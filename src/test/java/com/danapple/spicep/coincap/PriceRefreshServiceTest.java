package com.danapple.spicep.coincap;

import com.danapple.spicep.dao.TokenDao;
import com.danapple.spicep.entities.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import static com.danapple.spicep.common.TestConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PriceRefreshServiceTest {

    @Mock(lenient = true)
    private TokenDao tokenDao;
    @Mock(lenient = true)
    private CoinCapPriceService coinCapPriceService;
    @Mock(lenient = true)
    private ScheduledExecutorService executorService;

    private PriceRefreshService priceRefreshService;

    @BeforeEach
    public void beforeEach() {
        when(tokenDao
                .getAllTokens())
                .thenReturn(Map.of(TOKEN_KEY_BTC, new Token(TOKEN_KEY_BTC, SYMBOL_BTC),
                        TOKEN_KEY_ETH, new Token(TOKEN_KEY_ETH, SYMBOL_ETH)));
        when(coinCapPriceService.getPrice(SYMBOL_BTC)).thenReturn(CompletableFuture.completedFuture(BigDecimal.ONE));
        when(coinCapPriceService.getPrice(SYMBOL_ETH)).thenReturn(CompletableFuture.completedFuture(BigDecimal.TWO));

        priceRefreshService = new PriceRefreshService(executorService,
                tokenDao,
                coinCapPriceService);
        priceRefreshService.refreshPeriod = 7;
    }

    @Test
    void schedulesRefreshesWithCorrectPeriod() {
        priceRefreshService.refreshPeriod = 7;
        priceRefreshService.schedulePriceRefreshes();
        verify(executorService).scheduleWithFixedDelay(any(Runnable.class),
                eq(7L),
                eq(7L),
                eq(java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    void callsCoinCapForAllTokens() {
        priceRefreshService.refreshPrices();
        verify(coinCapPriceService).getPrice(eq(SYMBOL_BTC));
        verify(coinCapPriceService).getPrice(eq(SYMBOL_ETH));
    }

    @Test
    void savesAllPrices() {
        priceRefreshService.refreshPrices();
        verify(tokenDao).savePrice(eq(TOKEN_KEY_BTC), eq(BigDecimal.ONE));
        verify(tokenDao).savePrice(eq(TOKEN_KEY_ETH), eq(BigDecimal.TWO));

    }

}
