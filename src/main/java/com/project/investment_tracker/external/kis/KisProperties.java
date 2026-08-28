package com.project.investment_tracker.external.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kis")
public record KisProperties(
        String appKey,
        String appSecret,
        String baseUrl
) {
}
