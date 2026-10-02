package com.project.investment_tracker.external.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.investment_tracker.dto.StockDailyPriceResponse;
import com.project.investment_tracker.global.error.StockPriceUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class KisDailyPriceClient {
    private final KisProperties properties;
    private final KisTokenClient tokens;
    private final KisQuoteRateLimiter limiter;
    private final RestClient client;

    @org.springframework.beans.factory.annotation.Autowired
    public KisDailyPriceClient(KisProperties properties, KisTokenClient tokens, KisQuoteRateLimiter limiter) {
        this(properties, tokens, limiter, createBuilder());
    }

    KisDailyPriceClient(KisProperties properties, KisTokenClient tokens, KisQuoteRateLimiter limiter,
                       RestClient.Builder builder) {
        this.properties = properties;
        this.tokens = tokens;
        this.limiter = limiter;
        client = builder.baseUrl(properties.baseUrl()).build();
    }

    private static RestClient.Builder createBuilder() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().requestFactory(factory);
    }

    public List<StockDailyPriceResponse> getDailyPrices(String symbol, LocalDate start, LocalDate end) {
        try {
            ApiResponse response = limiter.execute(() -> client.get().uri(builder -> builder
                    .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_INPUT_ISCD", symbol)
                    .queryParam("FID_INPUT_DATE_1", start.format(DateTimeFormatter.BASIC_ISO_DATE))
                    .queryParam("FID_INPUT_DATE_2", end.format(DateTimeFormatter.BASIC_ISO_DATE))
                    .queryParam("FID_PERIOD_DIV_CODE", "D")
                    .queryParam("FID_ORG_ADJ_PRC", "1").build())
                    .header("authorization", "Bearer " + tokens.getAccessToken())
                    .header("appkey", properties.appKey()).header("appsecret", properties.appSecret())
                    .header("tr_id", "FHKST03010100").retrieve().body(ApiResponse.class));
            if (response == null || !"0".equals(response.code()) || response.output() == null) {
                throw new IllegalStateException("Invalid KIS daily price response");
            }
            return response.output().stream()
                    .filter(row -> row.date() != null && !row.date().isBlank())
                    .map(row -> new StockDailyPriceResponse(symbol,
                            LocalDate.parse(row.date(), DateTimeFormatter.BASIC_ISO_DATE),
                            Long.parseLong(row.open()), Long.parseLong(row.high()),
                            Long.parseLong(row.low()), Long.parseLong(row.close())))
                    .toList();
        } catch (RestClientException | IllegalArgumentException | IllegalStateException
                 | java.time.format.DateTimeParseException exception) {
            throw new StockPriceUnavailableException(exception);
        }
    }

    private record ApiResponse(@JsonProperty("rt_cd") String code,
                               @JsonProperty("output2") List<DailyRow> output) {}
    private record DailyRow(@JsonProperty("stck_bsop_date") String date,
                            @JsonProperty("stck_oprc") String open,
                            @JsonProperty("stck_hgpr") String high,
                            @JsonProperty("stck_lwpr") String low,
                            @JsonProperty("stck_clpr") String close) {}
}
