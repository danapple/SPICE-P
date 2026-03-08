package com.danapple.spicep.coincap;

import com.danapple.spicep.dao.TokenDao;
import com.danapple.spicep.entities.Token;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
class PriceRefreshService {
    private final Logger logger = LoggerFactory.getLogger(PriceRefreshService.class);

    private final TokenDao tokenDao;
    private final CoinCapPriceService coinCapPriceService;
    private final ScheduledExecutorService executorService;

    @Value( "${spicep.refreshService.refreshPeriod:10}" )
    int refreshPeriod;

    PriceRefreshService(@Qualifier("priceRefreshExecutorService") final ScheduledExecutorService executorService,
                        final TokenDao tokenDao,
                        final CoinCapPriceService coinCapPriceService) {
        this.executorService = executorService;
        this.tokenDao = tokenDao;
        this.coinCapPriceService = coinCapPriceService;
    }

    @PostConstruct
    void schedulePriceRefreshes() {
        executorService.scheduleWithFixedDelay(this::refreshPrices,
                                               refreshPeriod,
                                               refreshPeriod,
                                               TimeUnit.SECONDS);
    }

    void refreshPrices() {
        Map<String, Token> allTokens = tokenDao.getAllTokens();
        logger.debug("Refreshing {} tokens",
                     allTokens.size());
        Map<Token, Future<BigDecimal>> futures = new HashMap<>();
        for (Token token : allTokens.values()) {
            Future<BigDecimal> future = coinCapPriceService.getPrice(token.symbol());
            futures.put(token, future);
        }
        for (Map.Entry<Token, Future<BigDecimal>> entry : futures.entrySet()) {
            Token token = entry.getKey();
            Future<BigDecimal> future = entry.getValue();
            try {
                BigDecimal price = future.get();
                if (price != null) {
                    logger.debug("Saving price {} for token {}",
                                 price,
                                 token);
                    tokenDao.savePrice(token.tokenKey(),
                                       price);
                }
                else {
                    logger.warn("Got null price for token {}",
                                token);
                }
            }
            catch (Throwable t) {
                logger.warn("Got Throwable",
                            t);
            }
        }
    }
}
