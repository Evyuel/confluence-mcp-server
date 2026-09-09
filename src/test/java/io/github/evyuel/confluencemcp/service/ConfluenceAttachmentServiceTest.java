package io.github.evyuel.confluencemcp.service;

import io.github.evyuel.confluencemcp.client.ConfluenceClient;
import org.junit.jupiter.api.Test;

import static io.github.evyuel.confluencemcp.TestData.json;
import static io.github.evyuel.confluencemcp.TestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfluenceAttachmentServiceTest {
    @Test
    void mapsMetadataAndPaginationWithoutReadingContent() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.listAttachments("1", 0, 20)).thenReturn(json("""
                {"totalSize":2,"results":[{"id":"10","title":"design.pdf",
                "metadata":{"mediaType":"application/pdf","comment":"diagram"},"extensions":{"fileSize":1234},
                "version":{"number":2,"when":"2026-02-02"},"history":{"createdDate":"2026-01-01"},
                "_links":{"download":"/download/10"}}]}
                """));
        var service = new ConfluenceAttachmentService(client, properties());

        var response = service.attachments("1", null, null);

        assertThat(response.attachments().get(0).fileName()).isEqualTo("design.pdf");
        assertThat(response.attachments().get(0).downloadUrl()).isEqualTo("https://wiki.example/download/10");
        assertThat(response.pagination().hasMore()).isTrue();
        verify(client).listAttachments("1", 0, 20);
    }
}
