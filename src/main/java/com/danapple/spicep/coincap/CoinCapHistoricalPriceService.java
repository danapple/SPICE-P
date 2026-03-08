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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
public class CoinCapHistoricalPriceService extends AbstractCoinCapService {
    private final Logger logger = LoggerFactory.getLogger(CoinCapHistoricalPriceService.class);

    private final ExecutorService executorService;

    CoinCapHistoricalPriceService(@Qualifier("coinCapExecutorService") final ExecutorService executorService,
                                  @Value("${spicep.coinCapPriceService.apiKey}") String apiKey,
                                  @Value( "${spicep.coinCapPriceService.url}") String url) {
        super(apiKey, url);
        this.executorService = executorService;
    }

    public Future<BigDecimal> getHistoricalPrice(final String symbol, final LocalDate date) {
        return executorService.submit(() -> getHistoricalTokenPrice(symbol, date));
    }

    private BigDecimal getHistoricalTokenPrice(final String symbol, final LocalDate date) {
        try {
            logger.debug("getHistoricalTokenPrice = {} on {}",
                    symbol,
                    date);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>("parameters",
                                                         getHeaders());

            String slug = searchForSlug(restTemplate, entity, symbol);
            if (slug == null) {
                logger.debug("No slug found for symbol {}", symbol);
                return null;
            }
            BigDecimal price = getPrice(restTemplate, entity, slug, date);
            logger.debug("Got price {} for symbol {} on {}",
                    price,
                    symbol,
                    date);
            return price;

        }
        catch (Throwable t) {
            logger.warn("Got Throwable", t);
            return null;
        }
    }

    private String searchForSlug(RestTemplate restTemplate, HttpEntity<String> entity, String symbol) {
        String thisUrl = "%s/assets?search=%s".formatted(getUrl(),
                symbol);
        ResponseEntity<SearchResponse> result =
                restTemplate.exchange(thisUrl,
                        HttpMethod.GET,
                        entity,
                        SearchResponse.class);
        logger.trace("Result = {}",
                result);
        SearchResponse body = result.getBody();
        if (body.getData().isEmpty()) {
            logger.debug("No slugs returned  for symbol {}", symbol);
            return null;
        }
        SearchData searchData = body.getData().getFirst();
        logger.debug("Got slug {} for symbol {}",
                searchData.getId(),
                symbol);
        return searchData.getId();
    }

    private BigDecimal getPrice(RestTemplate restTemplate, HttpEntity<String> entity, String slug, LocalDate date) {
        long startOfDay = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000;
        long endOfDay = date.plus(1, ChronoUnit.DAYS).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000;
        String thisUrl = "%s/assets/%s/history?interval=d1&start=%d&end=%d".formatted(getUrl(),
                slug,
                startOfDay,
                endOfDay);
        ResponseEntity<PriceResponse> result =
                restTemplate.exchange(thisUrl,
                        HttpMethod.GET,
                        entity,
                        PriceResponse.class);
        logger.trace("Result = {}",
                result);

        PriceResponse body = result.getBody();
        logger.debug("Got {} prices for slug {} on {}",
                body.getData().size(), slug, date);
        BigDecimal price = body.getData().getLast().getPriceUsd();
        String dateTime = body.getData().getLast().getDate();

        logger.debug("Got price {} for slug {} on {} ({})",
                slug,
                price,
                date,
                dateTime);
        return price;
    }

    private static class SearchData {
        private String id;

        public String getId() {
            return id;
        }
    }
    
    private static class SearchResponse {
        private List<SearchData> data;

        public List<SearchData> getData() {
            return data;
        }
    }

    private static class PriceData {
        private BigDecimal priceUsd;
        private String date;

        public BigDecimal getPriceUsd() {
            return priceUsd;
        }

        public String getDate() {
            return date;
        }
    }

    private static class PriceResponse {
        private List<PriceData> data;

        public List<PriceData> getData() {
            return data;
        }

        @Override
        public String toString() {
            return "PriceResponse{" +
                    ", data=" + data +
                    '}';
        }
    }
}
