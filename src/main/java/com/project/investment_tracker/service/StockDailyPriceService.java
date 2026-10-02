package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockDailyPriceResponse;
import com.project.investment_tracker.external.kis.KisDailyPriceClient;
import com.project.investment_tracker.global.error.BadRequestException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.TreeMap;

@Service
public class StockDailyPriceService {
    private final KisDailyPriceClient client;

    public StockDailyPriceService(KisDailyPriceClient client) { this.client = client; }

    public List<StockDailyPriceResponse> getDailyPrices(String symbol, LocalDate start, LocalDate end) {
        if (!symbol.matches("[0-9A-Z]{6}") || start == null || end == null || start.isAfter(end)
                || !end.isBefore(LocalDate.now(ZoneId.of("Asia/Seoul"))) || end.isAfter(start.plusYears(1))) {
            throw new BadRequestException("종목코드와 조회 기간을 확인해주세요. 어제까지 최대 1년을 조회할 수 있습니다.");
        }
        var prices = new TreeMap<LocalDate, StockDailyPriceResponse>();
        // 달력 기준 100일씩 조회하여 한 번의 응답이 최대 100개 일봉을 넘지 않게 한다.
        for (LocalDate from = start; !from.isAfter(end); from = from.plusDays(100)) {
            LocalDate to = from.plusDays(99).isAfter(end) ? end : from.plusDays(99);
            for (var price : client.getDailyPrices(symbol, from, to)) {
                if (!price.date().isBefore(from) && !price.date().isAfter(to)) {
                    prices.put(price.date(), price);
                }
            }
        }
        return List.copyOf(prices.values());
    }
}
