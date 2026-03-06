package com.danapple.spicep.coincap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
public class CoinCapPriceService {
    private final Logger logger = LoggerFactory.getLogger(CoinCapPriceService.class);

    @Value( "${spicep.coinCapPriceService.apiKey}" )
    private String apiKey;
    @Value( "${spicep.coinCapPriceService.url}" )
    private String url;

    private final ExecutorService executorService;

    CoinCapPriceService(@Qualifier("coinCapExecutorService") final ExecutorService executorService) {
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

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.add("user-agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
            headers.add("Authorization", "Bearer %s".formatted(apiKey));

            HttpEntity<String> entity = new HttpEntity<>("parameters",
                                                         headers);
            String thisUrl = "%s/%s/%s".formatted(url,
                                                  "/price/bysymbol/",
                                                  symbol);
            ResponseEntity<PriceResponse> result =
                    restTemplate.exchange(thisUrl,
                                          HttpMethod.GET,
                                          entity,
                                          PriceResponse.class);
            logger.debug("Result = {}",
                        result);
            PriceResponse body = result.getBody();
            BigDecimal price = body.getData().get(0);
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

    public static class PriceResponse {
        public long timestamp;
        public List<BigDecimal> data;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(final long timestamp) {
            this.timestamp = timestamp;
        }

        public List<BigDecimal> getData() {
            return data;
        }

        public void setData(final List<BigDecimal> data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "PriceResponse{" +
                    "timestamp=" + timestamp +
                    ", data=" + data +
                    '}';
        }
    }
}
