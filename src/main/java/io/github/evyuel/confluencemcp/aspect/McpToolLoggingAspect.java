package io.github.evyuel.confluencemcp.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Aspect
@Component
public class McpToolLoggingAspect {

    @AfterThrowing(
            pointcut = "@annotation(mcpTool)",
            throwing = "exception"
    )
    public void logToolError(
            McpTool mcpTool,
            Throwable exception
    ) {
        String toolName = StringUtils.hasText(mcpTool.name())
                ? mcpTool.name()
                : "unknown";

        Throwable rootCause = getRootCause(exception);

        log.error(
                "MCP tool '{}' failed. Root cause: {}: {}",
                toolName,
                rootCause.getClass().getSimpleName(),
                rootCause.getMessage(),
                exception
        );
    }

    private Throwable getRootCause(Throwable exception) {
        Throwable result = exception;

        while (result.getCause() != null
                && result.getCause() != result) {
            result = result.getCause();
        }

        return result;
    }
}