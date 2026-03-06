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

    @Value( "${spicep.coinCapPriceService.apiKey}" )
    private String apiKey;
    @Value( "${spicep.coinCapPriceService.url}" )
    private String url;

    private final ExecutorService executorService;

    CoinCapHistoricalPriceService(@Qualifier("coinCapExecutorService") final ExecutorService executorService) {
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
        String thisUrl = "%s/assets?search=%s".formatted(url,
                symbol);
        ResponseEntity<SearchResponse> result =
                restTemplate.exchange(thisUrl,
                        HttpMethod.GET,
                        entity,
                        SearchResponse.class);
        logger.trace("Result = {}",
                result);
        SearchResponse body = result.getBody();
        SearchData searchData = body.data.getFirst();
        logger.debug("Got slug {} for symbol {}",
                searchData.id,
                symbol);
        return searchData.id;
    }

    private BigDecimal getPrice(RestTemplate restTemplate, HttpEntity<String> entity, String slug, LocalDate date) {
        long startOfDay = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000;
        long endOfDay = date.plus(1, ChronoUnit.DAYS).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000;
        String thisUrl = "%s/assets/%s/history?interval=m1&start=%d&end=%d".formatted(url,
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
                body.data.size(), slug, date);
        BigDecimal price = body.data.getLast().priceUsd;
        String dateTime = body.data.getLast().date;

        logger.debug("Got price {} for slug {} on {} ({})",
                slug,
                price,
                date,
                dateTime);
        return price;
    }

    private static class SearchData {
        private String id;
        private String symbol;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
    }
    
    private static class SearchResponse {
        private long timestamp;
        private List<SearchData> data;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public List<SearchData> getData() {
            return data;
        }

        public void setData(List<SearchData> data) {
            this.data = data;
        }
    }

    private static class PriceData {
        private BigDecimal priceUsd;
        private String date;

        public BigDecimal getPriceUsd() {
            return priceUsd;
        }

        public void setPriceUsd(BigDecimal priceUsd) {
            this.priceUsd = priceUsd;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }


    }

    private static class PriceResponse {
        private long timestamp;
        private List<PriceData> data;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public List<PriceData> getData() {
            return data;
        }

        public void setData(List<PriceData> data) {
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
