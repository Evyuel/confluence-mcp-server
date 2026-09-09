package io.github.evyuel.confluencemcp.converter;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ConfluenceContentConverter {
    private static final Set<String> PANEL_MACROS = Set.of("info", "note", "warning", "tip");
    private final FlexmarkHtmlConverter htmlConverter = FlexmarkHtmlConverter.builder().build();

    public ConversionResult toMarkdown(String storage) {
        var warnings = new ArrayList<String>();
        try {
            Document document = Jsoup.parse(storage, "", Parser.xmlParser());
            preprocessMacros(document, warnings);
            preprocessConfluenceLinks(document);
            String markdown = htmlConverter.convert(document.html()).strip();
            return new ConversionResult(markdown, warnings.isEmpty() ? "SUCCESS" : "SUCCESS_WITH_WARNINGS",
                    List.copyOf(warnings));
        } catch (RuntimeException ex) {
            return new ConversionResult(
                    "Markdown conversion failed. Retry get_page with format=STORAGE.",
                    "FAILED", List.of("Markdown conversion failed: " + safeMessage(ex)));
        }
    }

    private void preprocessMacros(Document document, List<String> warnings) {
        for (Element macro : new ArrayList<>(document.getAllElements())) {
            if (!macro.tagName().equalsIgnoreCase("ac:structured-macro")) continue;
            String name = macro.attr("ac:name").toLowerCase(Locale.ROOT);
            Element body = directChild(macro, "ac:rich-text-body");
            Element plainBody = directChild(macro, "ac:plain-text-body");

            if ("code".equals(name)) {
                String language = parameter(macro, "language");
                Element pre = new Element(Tag.valueOf("pre"), "");
                Element code = pre.appendElement("code");
                if (language != null && !language.isBlank()) code.addClass("language-" + safeLanguage(language));
                code.text(plainBody != null ? plainBody.wholeText() : body != null ? body.text() : "");
                macro.replaceWith(pre);
            } else if (PANEL_MACROS.contains(name)) {
                Element blockquote = new Element(Tag.valueOf("blockquote"), "");
                blockquote.appendElement("p").appendElement("strong").text(capitalize(name) + ":");
                appendBody(blockquote, body, plainBody);
                macro.replaceWith(blockquote);
            } else if ("expand".equals(name)) {
                Element details = new Element(Tag.valueOf("details"), "");
                String title = parameter(macro, "title");
                details.appendElement("summary").text(title == null || title.isBlank() ? "Details" : title);
                appendBody(details, body, plainBody);
                macro.replaceWith(details);
            } else {
                String displayName = name.isBlank() ? "unknown" : name;
                warnings.add("Unsupported Confluence macro: " + displayName);
                Element fallback = new Element(Tag.valueOf("div"), "");
                fallback.appendElement("p").appendElement("strong")
                        .text("[Confluence macro: " + displayName + "]");
                appendBody(fallback, body, plainBody);
                macro.replaceWith(fallback);
            }
        }
    }

    private static void preprocessConfluenceLinks(Document document) {
        for (Element link : new ArrayList<>(document.getAllElements())) {
            if (!link.tagName().equalsIgnoreCase("ac:link")) continue;
            Element page = directChild(link, "ri:page");
            Element label = directChild(link, "ac:plain-text-link-body");
            if (page != null) {
                String title = page.attr("ri:content-title");
                Element replacement = new Element(Tag.valueOf("span"), "");
                replacement.text(label != null ? label.wholeText() : title);
                link.replaceWith(replacement);
            }
        }
    }

    private static void appendBody(Element target, Element richBody, Element plainBody) {
        if (richBody != null) target.append(richBody.html());
        else if (plainBody != null && !plainBody.wholeText().isBlank()) target.appendElement("p").text(plainBody.wholeText());
    }

    private static Element directChild(Element parent, String tagName) {
        return parent.children().stream().filter(e -> e.tagName().equalsIgnoreCase(tagName)).findFirst().orElse(null);
    }

    private static String parameter(Element macro, String parameterName) {
        return macro.children().stream()
                .filter(e -> e.tagName().equalsIgnoreCase("ac:parameter"))
                .filter(e -> parameterName.equalsIgnoreCase(e.attr("ac:name")))
                .map(Element::text).findFirst().orElse(null);
    }

    private static String safeLanguage(String language) {
        return language.replaceAll("[^A-Za-z0-9_+.-]", "");
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null ? ex.getClass().getSimpleName() : message.substring(0, Math.min(300, message.length()));
    }
}
