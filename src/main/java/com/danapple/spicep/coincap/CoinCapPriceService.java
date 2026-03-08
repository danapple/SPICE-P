package com.danapple.spicep.coincap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
public class CoinCapPriceService  extends AbstractCoinCapService {
    private final Logger logger = LoggerFactory.getLogger(CoinCapPriceService.class);

    private final ExecutorService executorService;

    CoinCapPriceService(@Qualifier("coinCapExecutorService") final ExecutorService executorService,
                        @Value("${spicep.coinCapPriceService.apiKey}") String apiKey,
                        @Value( "${spicep.coinCapPriceService.url}") String url) {
        super(apiKey, url);
        this.executorService = executorService;
    }

    public Future<BigDecimal> getPrice(final String symbol) {
        return executorService.submit(() -> getTokenPrice(symbol));
    }

    private BigDecimal getTokenPrice(final String symbol) {
        try {
            logger.debug("getTokenPrice = {}",
                         symbol);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>("parameters",
                                                         getHeaders());
            String thisUrl = "%s/price/bysymbol/%s".formatted(getUrl(),
                                                  symbol);
            ResponseEntity<PriceResponse> result =
                    restTemplate.exchange(thisUrl,
                                          HttpMethod.GET,
                                          entity,
                                          PriceResponse.class);
            logger.trace("Result = {}",
                        result);
            PriceResponse body = result.getBody();
            BigDecimal price = body.getData().getFirst();
            logger.debug("Got price {} for symbol {}",
                        price,
                        symbol);
            return price;

        }
        catch (Throwable t) {
            logger.warn("Got Throwable", t);
            return null;
        }
    }

    private static class PriceResponse {
        private List<BigDecimal> data;

        public List<BigDecimal> getData() {
            return data;
        }
    }
}
