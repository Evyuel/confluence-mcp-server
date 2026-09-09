package io.github.evyuel.confluencemcp.converter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfluenceContentConverterTest {
    private final ConfluenceContentConverter converter = new ConfluenceContentConverter();

    @Test
    void convertsStandardHtmlAndSupportedMacros() {
        String storage = "<h1>Title</h1><p><strong>Bold</strong> and <em>italic</em></p>"
                + "<ul><li>One</li></ul><table><tr><th>A</th></tr><tr><td>B</td></tr></table>"
                + "<ac:structured-macro ac:name=\"code\"><ac:parameter ac:name=\"language\">java</ac:parameter>"
                + "<ac:plain-text-body>int x = 1;</ac:plain-text-body></ac:structured-macro>"
                + "<ac:structured-macro ac:name=\"info\"><ac:rich-text-body><p>Heads up</p></ac:rich-text-body></ac:structured-macro>"
                + "<ac:structured-macro ac:name=\"expand\"><ac:parameter ac:name=\"title\">More</ac:parameter>"
                + "<ac:rich-text-body><p>Details</p></ac:rich-text-body></ac:structured-macro>";

        ConversionResult result = converter.toMarkdown(storage);

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.content()).contains("Title", "**Bold**", "*italic*", "int x = 1", "Heads up", "More", "Details");
    }

    @Test
    void preservesUnknownMacroContentAndAddsWarning() {
        var result = converter.toMarkdown("<ac:structured-macro ac:name=\"corporate-diagram\">"
                + "<ac:rich-text-body><p>Important diagram caption</p></ac:rich-text-body></ac:structured-macro>");

        assertThat(result.status()).isEqualTo("SUCCESS_WITH_WARNINGS");
        assertThat(result.content()).contains("Confluence macro: corporate-diagram", "Important diagram caption");
        assertThat(result.warnings()).containsExactly("Unsupported Confluence macro: corporate-diagram");
    }
}
