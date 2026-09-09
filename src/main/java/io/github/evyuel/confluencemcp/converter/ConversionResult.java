package io.github.evyuel.confluencemcp.converter;

import java.util.List;

public record ConversionResult(String content, String status, List<String> warnings) {
}
