package io.github.evyuel.confluencemcp.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpTool;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class McpToolContractTest {
    @Test
    void exposesExactlySevenExpectedTools() {
        Set<String> names = Stream.of(ConfluenceSearchTools.class, ConfluencePageTools.class,
                        ConfluenceNavigationTools.class, ConfluenceAttachmentTools.class, ConfluenceWriteTools.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .map(method -> method.getAnnotation(McpTool.class))
                .filter(annotation -> annotation != null)
                .map(McpTool::name)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder("search_pages", "get_page", "list_page_children",
                "get_page_ancestors", "list_spaces", "list_page_attachments", "create_page");
    }
}
