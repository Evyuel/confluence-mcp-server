package io.github.evyuel.confluencemcp.service;

import io.github.evyuel.confluencemcp.client.ConfluenceClient;
import io.github.evyuel.confluencemcp.exception.ConfluenceHttpException;
import io.github.evyuel.confluencemcp.exception.ConfluenceToolException;
import io.github.evyuel.confluencemcp.mcp.ConfluenceWriteTools;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;

import static io.github.evyuel.confluencemcp.TestData.JSON;
import static io.github.evyuel.confluencemcp.TestData.json;
import static io.github.evyuel.confluencemcp.TestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfluenceWriteServiceTest {
    @Test
    void toolDoesNotExposeParentOrSpaceParameters() {
        var method = Arrays.stream(ConfluenceWriteTools.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("createPage")).findFirst().orElseThrow();
        assertThat(method.getParameterTypes()).containsExactly(String.class, String.class);
    }

    @Test
    void readsConfiguredParentEveryTimeAndUsesItsSpaceInPublishedStorageRequest() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.getPage("999", "space")).thenReturn(json("{\"space\":{\"key\":\"ENG\"}}"));
        when(client.createPage(any())).thenReturn(json("{\"id\":\"42\",\"title\":\"T\",\"_links\":{\"webui\":\"/42\"}}"));
        var service = new ConfluenceWriteService(client, properties(), JSON);

        service.createPage("T", "<p>x</p>");
        service.createPage("T2", "<p>y</p>");

        verify(client, times(2)).getPage("999", "space");
        ArgumentCaptor<JsonNode> requests = ArgumentCaptor.forClass(JsonNode.class);
        verify(client, times(2)).createPage(requests.capture());
        JsonNode request = requests.getAllValues().get(0);
        assertThat(request.at("/ancestors/0/id").asText()).isEqualTo("999");
        assertThat(request.at("/space/key").asText()).isEqualTo("ENG");
        assertThat(request.at("/status").asText()).isEqualTo("current");
        assertThat(request.at("/body/storage/representation").asText()).isEqualTo("storage");
    }

    @Test
    void countsUtf8BytesAndDoesNotPostOversizeContent() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        var service = new ConfluenceWriteService(client, properties(), JSON);

        assertThatThrownBy(() -> service.createPage("T", "€€€€"))
                .isInstanceOf(ConfluenceToolException.class).hasMessageContaining("12 bytes", "10 bytes");
        verify(client, times(0)).createPage(any());
    }

    @Test
    void retriesOnlyDuplicateTitlesUsingNumberedPrefix() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.getPage("999", "space")).thenReturn(json("{\"space\":{\"key\":\"ENG\"}}"));
        when(client.createPage(any()))
                .thenThrow(new ConfluenceHttpException(409, "page with same title already exists", true))
                .thenThrow(new ConfluenceHttpException(400, "title already exists", true))
                .thenReturn(json("{\"id\":\"42\",\"_links\":{\"webui\":\"/42\"}}"));
        var service = new ConfluenceWriteService(client, properties(), JSON);

        var response = service.createPage("Architecture", "<p>x</p>");

        ArgumentCaptor<JsonNode> requests = ArgumentCaptor.forClass(JsonNode.class);
        verify(client, times(3)).createPage(requests.capture());
        assertThat(requests.getAllValues()).extracting(node -> node.path("title").asText())
                .containsExactly("Architecture", "1 - Architecture", "2 - Architecture");
        assertThat(response.actualTitle()).isEqualTo("2 - Architecture");
        assertThat(response.renamedDueToConflict()).isTrue();
    }

    @Test
    void malformedStorageOrNetworkUncertaintyIsNotRetried() {
        ConfluenceClient malformed = mock(ConfluenceClient.class);
        when(malformed.getPage("999", "space")).thenReturn(json("{\"space\":{\"key\":\"ENG\"}}"));
        when(malformed.createPage(any())).thenThrow(new ConfluenceHttpException(400, "Malformed storage XHTML", false));
        var malformedService = new ConfluenceWriteService(malformed, properties(), JSON);

        assertThatThrownBy(() -> malformedService.createPage("T", "<bad>"))
                .isInstanceOf(ConfluenceToolException.class).hasMessageContaining("Malformed storage XHTML");
        verify(malformed, times(1)).createPage(any());

        ConfluenceClient uncertain = mock(ConfluenceClient.class);
        when(uncertain.getPage("999", "space")).thenReturn(json("{\"space\":{\"key\":\"ENG\"}}"));
        when(uncertain.createPage(any())).thenThrow(new ConfluenceToolException("may have been created"));
        var uncertainService = new ConfluenceWriteService(uncertain, properties(), JSON);

        assertThatThrownBy(() -> uncertainService.createPage("T", "<p>x</p>"))
                .isInstanceOf(ConfluenceToolException.class).hasMessageContaining("may have been created");
        verify(uncertain, times(1)).createPage(any());
    }

    @Test
    void stopsAfterDuplicateTitleSafetyLimit() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.getPage("999", "space")).thenReturn(json("{\"space\":{\"key\":\"ENG\"}}"));
        when(client.createPage(any())).thenThrow(new ConfluenceHttpException(409, "duplicate title", true));
        var service = new ConfluenceWriteService(client, properties(), JSON);

        assertThatThrownBy(() -> service.createPage("T", "<p>x</p>"))
                .isInstanceOf(ConfluenceToolException.class).hasMessageContaining("100");
        verify(client, times(101)).createPage(any());
    }
}
