package com.project.investment_tracker.external.kis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class KisStockMasterClient {
    private static final String MASTER_URL = "https://new.real.download.dws.co.kr/common/master";
    private final RestClient restClient;
    private final KisStockMasterParser parser;

    @Autowired
    public KisStockMasterClient(KisStockMasterParser parser) {
        this(createRestClient(), parser);
    }

    KisStockMasterClient(RestClient restClient, KisStockMasterParser parser) {
        this.restClient = restClient;
        this.parser = parser;
    }

    public List<KisStockMaster> loadStockMasters() {
        List<KisStockMaster> stocks = new ArrayList<>();
        for (KisStockMaster.Market market : KisStockMaster.Market.values()) {
            stocks.addAll(loadMarket(market));
        }
        return List.copyOf(stocks);
    }

    private List<KisStockMaster> loadMarket(KisStockMaster.Market market) {
        try {
            byte[] archive = restClient.get()
                    .uri(MASTER_URL + "/" + market.fileName() + ".zip")
                    .retrieve()
                    .body(byte[].class);
            if (archive == null || archive.length == 0) {
                throw new IllegalStateException(market + " 종목 마스터파일 응답이 비어 있습니다.");
            }
            return parser.parse(archive, market);
        } catch (RestClientException exception) {
            throw new IllegalStateException(market + " 종목 마스터파일 다운로드에 실패했습니다.", exception);
        }
    }

    private static RestClient createRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().requestFactory(factory).build();
    }
}
