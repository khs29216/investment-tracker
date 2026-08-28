package com.project.investment_tracker.external.kis;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class KisStockPriceClient {
    private static final String DOMESTIC_STOCK_MARKET_CODE = "J";
    private static final String INQUIRE_PRICE_TR_ID = "FHKST01010100";

    private final KisProperties kisProperties;
    private final KisTokenClient kisTokenClient;
    private final RestClient restClient;

    public KisStockPriceClient(
            KisProperties kisProperties,
            KisTokenClient kisTokenClient
    ) {
        this.kisProperties = kisProperties;
        this.kisTokenClient = kisTokenClient;
        this.restClient = RestClient.builder()
                .baseUrl(kisProperties.baseUrl())
                .build();
    }

    public Integer getCurrentPrice(String stockSymbol) {
        KisStockPriceApiResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", DOMESTIC_STOCK_MARKET_CODE)
                        .queryParam("FID_INPUT_ISCD", stockSymbol)
                        .build()
                )
                .header("authorization", "Bearer " + kisTokenClient.getAccessToken())
                .header("appkey", kisProperties.appKey())
                .header("appsecret", kisProperties.appSecret())
                .header("tr_id", INQUIRE_PRICE_TR_ID)
                .retrieve()
                .body(KisStockPriceApiResponse.class);

        Objects.requireNonNull(response);

        return response.output().currentPriceAsInteger();
    }

    private record KisStockPriceApiResponse(
            KisStockPriceOutput output
    ) {
    }

    private record KisStockPriceOutput(
            @JsonProperty("stck_prpr")
            String currentPrice
    ) {
        private Integer currentPriceAsInteger() {
            return Integer.valueOf(currentPrice.replace(",", ""));
        }
    }
}
