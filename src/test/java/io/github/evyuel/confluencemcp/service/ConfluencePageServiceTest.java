package io.github.evyuel.confluencemcp.service;

import io.github.evyuel.confluencemcp.client.ConfluenceClient;
import io.github.evyuel.confluencemcp.converter.ConfluenceContentConverter;
import io.github.evyuel.confluencemcp.converter.ConversionResult;
import io.github.evyuel.confluencemcp.dto.PageFormat;
import io.github.evyuel.confluencemcp.exception.ConfluenceToolException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.evyuel.confluencemcp.TestData.json;
import static io.github.evyuel.confluencemcp.TestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConfluencePageServiceTest {
    @Test
    void passesStorageThroughAndReturnsFirstMiddleAndFinalChunks() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.getPage("1", "body.storage,version,space,history.lastUpdated"))
                .thenReturn(page(7, "abcdefghijk"));
        var service = new ConfluencePageService(client, properties(), mock(ConfluenceContentConverter.class));

        var first = service.getPage("1", PageFormat.STORAGE, 0, 4);
        var middle = service.getPage("1", PageFormat.STORAGE, first.nextOffset(), 4);
        var last = service.getPage("1", PageFormat.STORAGE, middle.nextOffset(), 4);

        assertThat(first.content()).isEqualTo("abcd");
        assertThat(first.nextOffset()).isEqualTo(4);
        assertThat(middle.content()).isEqualTo("efgh");
        assertThat(last.content()).isEqualTo("ijk");
        assertThat(last.hasMore()).isFalse();
        assertThat(last.nextOffset()).isNull();
        assertThat(last.conversionStatus()).isEqualTo("NOT_APPLICABLE");
    }

    @Test
    void defaultsToMarkdownAndClampsMaxChars() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        ConfluenceContentConverter converter = mock(ConfluenceContentConverter.class);
        when(client.getPage(anyString(), anyString())).thenReturn(page(1, "storage"));
        when(converter.toMarkdown("storage")).thenReturn(new ConversionResult("123456789012", "SUCCESS", List.of()));
        var service = new ConfluencePageService(client, properties(), converter);

        var response = service.getPage("1", null, null, 999);

        assertThat(response.format()).isEqualTo(PageFormat.MARKDOWN);
        assertThat(response.content()).hasSize(10);
        assertThat(response.hasMore()).isTrue();
    }

    @Test
    void validatesOffsetBeforeCallingClient() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        var service = new ConfluencePageService(client, properties(), mock(ConfluenceContentConverter.class));

        assertThatThrownBy(() -> service.getPage("1", PageFormat.STORAGE, -1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    @Test
    void reportsConversionFailureWithStorageFallbackInstruction() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        ConfluenceContentConverter converter = mock(ConfluenceContentConverter.class);
        when(client.getPage(anyString(), anyString())).thenReturn(page(1, "broken"));
        when(converter.toMarkdown("broken")).thenReturn(new ConversionResult(
                "Markdown conversion failed. Retry get_page with format=STORAGE.", "FAILED", List.of("bad xml")));
        var service = new ConfluencePageService(client, properties(), converter);

        var response = service.getPage("1", PageFormat.MARKDOWN, 0, 50);

        assertThat(response.conversionStatus()).isEqualTo("FAILED");
        assertThat(response.conversionWarnings()).containsExactly("bad xml");
        assertThat(response.content()).contains("format=STORAGE");
    }

    @Test
    void detectsVersionChangeBetweenChunks() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.getPage(anyString(), anyString())).thenReturn(page(10, "abcdef"), page(11, "abcdef"));
        var service = new ConfluencePageService(client, properties(), mock(ConfluenceContentConverter.class));
        service.getPage("1", PageFormat.STORAGE, 0, 3);

        assertThatThrownBy(() -> service.getPage("1", PageFormat.STORAGE, 3, 3))
                .isInstanceOf(ConfluenceToolException.class).hasMessageContaining("changed from 10 to 11");
    }

    private static tools.jackson.databind.JsonNode page(int version, String storage) {
        return json("""
                {"id":"1","title":"Page","space":{"key":"ENG"},"version":{"number":%d},
                "history":{"lastUpdated":{"when":"2026-01-01"}},"body":{"storage":{"value":"%s"}},
                "_links":{"webui":"/pages/1"}}
                """.formatted(version, storage));
    }
}
