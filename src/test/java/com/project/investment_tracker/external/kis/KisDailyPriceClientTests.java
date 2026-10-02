package com.project.investment_tracker.external.kis;

import com.project.investment_tracker.global.error.StockPriceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KisDailyPriceClientTests {
    @Test
    void parsesPricesAndReusesTokenProvider() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var tokens = mock(KisTokenClient.class);
        when(tokens.getAccessToken()).thenReturn("test-token");
        var client = new KisDailyPriceClient(new KisProperties("key", "secret", "http://localhost"),
                tokens, new KisQuoteRateLimiter(), builder);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("inquire-daily-itemchartprice")))
                .andExpect(queryParam("FID_INPUT_DATE_1", "20250901"))
                .andExpect(queryParam("FID_ORG_ADJ_PRC", "1"))
                .andExpect(header("authorization", "Bearer test-token"))
                .andRespond(withSuccess("""
                        {"rt_cd":"0","output2":[{"stck_bsop_date":"20250901",
                        "stck_oprc":"100","stck_hgpr":"120","stck_lwpr":"90","stck_clpr":"110"}]}
                        """, MediaType.APPLICATION_JSON));
        var result = client.getDailyPrices("005930", LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30));
        assertEquals(110, result.get(0).close());
        assertEquals(LocalDate.of(2025, 9, 1), result.get(0).date());
        verify(tokens).getAccessToken();
        server.verify();
    }

    @Test
    void rejectsBusinessErrorEvenWithHttp200() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new KisDailyPriceClient(new KisProperties("key", "secret", "http://localhost"),
                mock(KisTokenClient.class), new KisQuoteRateLimiter(), builder);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("inquire-daily-itemchartprice")))
                .andRespond(withSuccess("{\"rt_cd\":\"1\"}", MediaType.APPLICATION_JSON));
        assertThrows(StockPriceUnavailableException.class, () -> client.getDailyPrices("005930",
                LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30)));
        server.verify();
    }
}
