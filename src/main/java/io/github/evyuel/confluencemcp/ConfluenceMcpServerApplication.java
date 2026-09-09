package io.github.evyuel.confluencemcp;

import io.github.evyuel.confluencemcp.configuration.ConfluenceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ConfluenceProperties.class)
public class ConfluenceMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfluenceMcpServerApplication.class, args);
    }
}

