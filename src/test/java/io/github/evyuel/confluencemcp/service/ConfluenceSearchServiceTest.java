package io.github.evyuel.confluencemcp.service;

import io.github.evyuel.confluencemcp.client.ConfluenceClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.github.evyuel.confluencemcp.TestData.json;
import static io.github.evyuel.confluencemcp.TestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfluenceSearchServiceTest {
    @Test
    void appliesDefaultsEscapesCqlMapsExcerptAndPaginationWithoutBody() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.search(anyString(), eq(0), eq(10))).thenReturn(json("""
                {"start":0,"limit":10,"totalSize":2,"results":[{"excerpt":"match",
                "lastModified":"2026-01-01","content":{"id":"1","title":"T","space":{"key":"ENG","name":"Engineering"},
                "body":{"storage":{"value":"must-not-leak"}},"_links":{"webui":"/pages/1"}}}]}
                """));
        var service = new ConfluenceSearchService(client, properties());

        var response = service.search("a\" OR type=blogpost \\", "ENG\" OR space=X", null, null);

        ArgumentCaptor<String> cql = ArgumentCaptor.forClass(String.class);
        verify(client).search(cql.capture(), eq(0), eq(10));
        assertThat(cql.getValue()).isEqualTo("type=page AND text ~ \"a\\\" OR type=blogpost \\\\\" AND space=\"ENG\\\" OR space=X\"");
        assertThat(response.pages().get(0).excerpt()).isEqualTo("match");
        assertThat(response.toString()).doesNotContain("must-not-leak");
        assertThat(response.pagination().hasMore()).isTrue();
    }

    @Test
    void clampsLimitToServerMaximumAndOmitsSpaceClause() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.search(anyString(), eq(4), eq(20))).thenReturn(json("{\"results\":[]}"));
        var service = new ConfluenceSearchService(client, properties());

        service.search("hello", null, 999, 4);

        ArgumentCaptor<String> cql = ArgumentCaptor.forClass(String.class);
        verify(client).search(cql.capture(), eq(4), eq(20));
        assertThat(cql.getValue()).isEqualTo("type=page AND text ~ \"hello\"");
    }
}
