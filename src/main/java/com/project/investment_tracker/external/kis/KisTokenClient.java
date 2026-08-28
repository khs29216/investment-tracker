package com.project.investment_tracker.external.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
public class KisTokenClient {

    private final KisProperties kisProperties;
    private final RestClient restClient;

    private String accessToken;
    private LocalDateTime expiresAt;

    public KisTokenClient(KisProperties kisProperties) {
        this.kisProperties = kisProperties;
        this.restClient = RestClient.builder()
                .baseUrl(kisProperties.baseUrl())
                .build();
    }

    public String getAccessToken() {
        if (accessToken == null || expiresAt == null || LocalDateTime.now().isAfter(expiresAt)) {
            requestAccessToken();
        }

        return accessToken;
    }

    private void requestAccessToken() {
        KisTokenResponse response = restClient.post()
                .uri("/oauth2/tokenP")
                .body(new KisTokenRequest(
                        "client_credentials",
                        kisProperties.appKey(),
                        kisProperties.appSecret()
                ))
                .retrieve()
                .body(KisTokenResponse.class);

        Objects.requireNonNull(response);

        this.accessToken = response.accessToken();
        this.expiresAt = LocalDateTime.now().plusSeconds(response.expiresIn() - 60);
    }

    private record KisTokenRequest(
            @JsonProperty("grant_type")
            String grantType,

            String appkey,

            String appsecret
    ) {
    }

    private record KisTokenResponse(
            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("token_type")
            String tokenType,

            @JsonProperty("expires_in")
            Long expiresIn
    ) {
    }
}
