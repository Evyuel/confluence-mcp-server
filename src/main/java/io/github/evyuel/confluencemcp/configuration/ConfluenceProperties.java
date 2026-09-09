package io.github.evyuel.confluencemcp.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "confluence")
public record ConfluenceProperties(
        @NotNull URI baseUrl,
        @NotBlank String token,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Valid @NotNull Read read,
        @Valid @NotNull Write write) {

    public record Read(@Positive int defaultMaxChars, @Positive int maxChars) {
        public Read {
            if (defaultMaxChars > maxChars) {
                throw new IllegalArgumentException("confluence.read.default-max-chars must not exceed max-chars");
            }
        }
    }

    public record Write(@NotBlank String parentPageId, @NotNull DataSize maxContentSize) {
        public Write {
            if (maxContentSize != null && maxContentSize.toBytes() <= 0) {
                throw new IllegalArgumentException("confluence.write.max-content-size must be positive");
            }
        }
    }
}
