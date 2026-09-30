package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockSearchResponse;
import com.project.investment_tracker.external.kis.KisStockMaster;
import com.project.investment_tracker.external.kis.KisStockMasterClient;
import com.project.investment_tracker.global.error.StockSearchUnavailableException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class StockSearchService {
    private static final int MAX_RESULTS = 20;
    private final KisStockMasterClient stockMasterClient;
    private List<KisStockMaster> stockMasters;

    public StockSearchService(KisStockMasterClient stockMasterClient) {
        this.stockMasterClient = stockMasterClient;
    }

    public List<StockSearchResponse> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String query = keyword.strip().toLowerCase(Locale.ROOT);
        return getStockMasters().stream()
                .filter(stock -> stock.stockName().toLowerCase(Locale.ROOT).contains(query)
                        || stock.stockSymbol().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.<KisStockMaster>comparingInt(stock ->
                                stock.stockName().equalsIgnoreCase(query)
                                        || stock.stockSymbol().equalsIgnoreCase(query) ? 0 : 1)
                        .thenComparing(KisStockMaster::stockName)
                        .thenComparing(KisStockMaster::stockSymbol))
                .limit(MAX_RESULTS)
                .map(StockSearchResponse::from)
                .toList();
    }

    private synchronized List<KisStockMaster> getStockMasters() {
        if (stockMasters == null) {
            try {
                // 두 시장을 모두 불러온 경우에만 캐시하여 실패한 요청은 다음 검색에서 재시도한다.
                stockMasters = List.copyOf(stockMasterClient.loadStockMasters());
            } catch (IllegalStateException exception) {
                throw new StockSearchUnavailableException(exception);
            }
        }
        return stockMasters;
    }
}
