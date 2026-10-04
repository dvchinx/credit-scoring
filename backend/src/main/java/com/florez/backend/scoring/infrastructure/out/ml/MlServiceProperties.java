package com.florez.backend.scoring.infrastructure.out.ml;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ml-service")
public record MlServiceProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
