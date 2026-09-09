package io.github.evyuel.confluencemcp;

import io.github.evyuel.confluencemcp.configuration.ConfluenceProperties;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;

public final class TestData {
    public static final ObjectMapper JSON = new ObjectMapper();

    private TestData() { }

    public static ConfluenceProperties properties() {
        return new ConfluenceProperties(URI.create("https://wiki.example"), "secret-test-token",
                Duration.ofSeconds(1), Duration.ofSeconds(2),
                new ConfluenceProperties.Read(5, 10),
                new ConfluenceProperties.Write("999", DataSize.ofBytes(10)));
    }

    public static JsonNode json(String value) {
        return JSON.readTree(value);
    }
}
