package io.github.evyuel.confluencemcp.service;

import io.github.evyuel.confluencemcp.client.ConfluenceClient;
import org.junit.jupiter.api.Test;

import static io.github.evyuel.confluencemcp.TestData.json;
import static io.github.evyuel.confluencemcp.TestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfluenceNavigationServiceTest {
    @Test
    void mapsDirectChildrenPositionAndPaginationAndClampsLimit() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.listChildren("1", 0, 50)).thenReturn(json("""
                {"start":0,"limit":50,"totalSize":2,"results":[
                {"id":"2","title":"Child","status":"current","extensions":{"position":7},"_links":{"webui":"/2"}}]}
                """));
        var service = new ConfluenceNavigationService(client, properties());

        var response = service.children("1", 500, null);

        assertThat(response.pages()).hasSize(1);
        assertThat(response.pages().get(0).position()).isEqualTo(7);
        assertThat(response.pagination().hasMore()).isTrue();
        verify(client).listChildren("1", 0, 50);
    }

    @Test
    void ancestorsKeepRootToParentOrderAndExcludeTarget() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.getPage("3", "ancestors,space")).thenReturn(json("""
                {"id":"3","space":{"key":"ENG"},"ancestors":[
                {"id":"1","title":"Root","status":"current"},{"id":"2","title":"Parent","status":"current"},
                {"id":"3","title":"Target","status":"current"}]}
                """));
        var response = new ConfluenceNavigationService(client, properties()).ancestors("3");

        assertThat(response.ancestors()).extracting(a -> a.pageId()).containsExactly("1", "2");
        assertThat(response.spaceKey()).isEqualTo("ENG");
    }

    @Test
    void mapsSpacesAndHomepageWithDefaultPagination() {
        ConfluenceClient client = mock(ConfluenceClient.class);
        when(client.listSpaces(eq("ENG"), eq(0), eq(25))).thenReturn(json("""
                {"results":[{"id":"9","key":"ENG","name":"Engineering","type":"global","status":"current",
                "homepage":{"id":"1","title":"Home"},"_links":{"webui":"/spaces/ENG"}}]}
                """));
        var response = new ConfluenceNavigationService(client, properties()).spaces("ENG", null, null);

        assertThat(response.spaces().get(0).homepageId()).isEqualTo("1");
        verify(client).listSpaces("ENG", 0, 25);
    }
}
