package io.github.evyuel.confluencemcp.client;

import io.github.evyuel.confluencemcp.exception.ConfluenceHttpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import static io.github.evyuel.confluencemcp.TestData.JSON;
import static io.github.evyuel.confluencemcp.TestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestConfluenceClientTest {
    private MockRestServiceServer server;
    private RestConfluenceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://wiki.example")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer secret-test-token")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestConfluenceClient(builder.build(), JSON, properties());
    }

    @Test
    void listSpacesAlwaysFixesGlobalCurrentAndUsesExactSpaceKey() {
        server.expect(request -> {
            var query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
            assertThat(query.getFirst("type")).isEqualTo("global");
            assertThat(query.getFirst("status")).isEqualTo("current");
            assertThat(query.getFirst("spaceKey")).isEqualTo("ENG");
            assertThat(query.getFirst("expand")).isEqualTo("homepage");
            assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Bearer secret-test-token");
        }).andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        client.listSpaces("ENG", 0, 25);
        server.verify();
    }

    @Test
    void childrenAlwaysRequestsCurrentStatusAndNoHeavyExpand() {
        server.expect(request -> {
            var query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
            assertThat(query.getFirst("status")).isEqualTo("current");
            assertThat(query).doesNotContainKey("expand");
        }).andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        client.listChildren("123", 0, 25);
        server.verify();
    }

    @Test
    void sanitizesTokenButKeepsUsefulCreateDiagnostic() {
        server.expect(request -> { }).andRespond(withBadRequest()
                .body("{\"message\":\"Malformed XHTML; Bearer secret-test-token\"}")
                .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createPage(JSON.createObjectNode()))
                .isInstanceOf(ConfluenceHttpException.class)
                .hasMessageContaining("Malformed XHTML", "[REDACTED]")
                .hasMessageNotContaining("secret-test-token");
        server.verify();
    }
}
