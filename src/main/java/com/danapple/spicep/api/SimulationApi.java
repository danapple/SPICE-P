package com.danapple.spicep.api;

import com.danapple.spicep.coincap.CoinCapHistoricalPriceService;
import com.danapple.spicep.coincap.CoinCapPriceService;
import com.danapple.spicep.dtos.SimulateAssetRequest;
import com.danapple.spicep.dtos.SimulateWalletRequest;
import com.danapple.spicep.dtos.SimulateWalletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/simulate")
class SimulationApi extends AbstractApi {
    private final static BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private final CoinCapHistoricalPriceService coinCapHistoricalPriceService;
    private final CoinCapPriceService coinCapPriceService;
    private final Logger logger = LoggerFactory.getLogger(SimulationApi.class);

    SimulationApi(final CoinCapPriceService coinCapPriceService,
                  final CoinCapHistoricalPriceService coinCapHistoricalPriceService) {
        this.coinCapPriceService = coinCapPriceService;
        this.coinCapHistoricalPriceService = coinCapHistoricalPriceService;
    }

    @PostMapping
    ResponseEntity<?> simulateWallet(@RequestBody SimulateWalletRequest request) {
        try {
            LocalDate date = request.getDate();
            LocalDate nowDate = LocalDate.now();
            List<SimulateAssetRequest> simulateAssetRequests = request.getAssets();
            Map<String, Future<BigDecimal>> futures = validateAndSetupPriceFutures(nowDate, date, simulateAssetRequests);
            return generateWalletPerformance(simulateAssetRequests, futures, date, nowDate);
        }
        catch (ValidationException ve) {
            return createErrorResponse(ve.getMessage(), HttpStatus.PRECONDITION_FAILED);
        }
        catch (Exception ex) {
            logger.warn("Could not simulate", ex);
            return createErrorResponse("Could not simulate", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<?> generateWalletPerformance(List<SimulateAssetRequest> simulateAssetRequests,
                                                Map<String, Future<BigDecimal>> futures,
                                                LocalDate date,
                                                LocalDate nowDate) throws InterruptedException, ExecutionException {
        BigDecimal totalValue = BigDecimal.ZERO;
        AssetPerformance bestAssetPerformance = null;
        AssetPerformance worstAssetPerformance = null;

        for (SimulateAssetRequest requestAsset : simulateAssetRequests) {
            String symbol = requestAsset.getSymbol().trim().toUpperCase();
            Future<BigDecimal> future = futures.get(symbol);
            BigDecimal price = future.get();
            if (price == null) {
                return createErrorResponse("No price found for token %s".formatted(symbol),
                        HttpStatus.NOT_FOUND);
            }
            BigDecimal currentValue = requestAsset.getQuantity().multiply(price);
            totalValue = totalValue.add(currentValue);
            BigDecimal gain = currentValue.subtract(requestAsset.getValue());
            BigDecimal currentPerformance = gain.multiply(ONE_HUNDRED)
                    .divide(requestAsset.getValue(),
                            2,
                            RoundingMode.HALF_UP);

            if (bestAssetPerformance == null || currentPerformance.compareTo(bestAssetPerformance.gain()) > 0) {
                bestAssetPerformance = new AssetPerformance(symbol, gain, currentPerformance);
            }
            if (worstAssetPerformance == null || currentPerformance.compareTo(worstAssetPerformance.gain()) < 0) {
                worstAssetPerformance = new AssetPerformance(symbol, gain, currentPerformance);
            }
        }
        LocalDate responseDate = date;
        if (responseDate == null) {
            responseDate = nowDate;
        }
        SimulateWalletResponse response = new SimulateWalletResponse(totalValue,
                bestAssetPerformance != null ? bestAssetPerformance.symbol() : null,
                bestAssetPerformance != null ? bestAssetPerformance.performance() : null,
                worstAssetPerformance != null ? worstAssetPerformance.symbol() : null,
                worstAssetPerformance != null ? worstAssetPerformance.performance() : null,
                responseDate);

        return new ResponseEntity<>(response,
                HttpStatus.OK);
    }

    private record AssetPerformance(String symbol, BigDecimal gain, BigDecimal performance) {}

    private Map<String, Future<BigDecimal>> validateAndSetupPriceFutures(LocalDate nowDate,
                                                                         LocalDate requestDate,
                                                                         List<SimulateAssetRequest> simulateAssetRequests)
            throws ValidationException {
        Map<String, Future<BigDecimal>> futures = new HashMap<>();
        for (SimulateAssetRequest requestAsset : simulateAssetRequests) {
            String symbol = validateSimulateAssetRequest(requestAsset);
            if (requestDate == null || requestDate.equals(nowDate)) {
                futures.put(symbol, coinCapPriceService.getPrice(symbol));
            }
            else {
                futures.put(symbol, coinCapHistoricalPriceService.getHistoricalPrice(symbol, requestDate));
            }
        }
        return futures;
    }

    private String validateSimulateAssetRequest(SimulateAssetRequest assetRequest) throws ValidationException {
        String symbol = assetRequest.getSymbol();
        if (symbol == null) {
            throw new ValidationException("Symbol must be supplied");
        }
        symbol = symbol.trim().toUpperCase();
        if (symbol.isEmpty() ||
                symbol.length() > 200) {
            throw new ValidationException("Symbol must be between 1 and 200 characters");
        }
        if (assetRequest.getQuantity() == null) {
            throw new ValidationException("Quantity must be supplied");
        }
        if (assetRequest.getValue() == null) {
            throw new ValidationException("Value must be supplied");
        }
        if (assetRequest.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Quantity of 0 is not allowed");
        }
        if (assetRequest.getValue().compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Value of 0 is not allowed");
        }
        return symbol;
    }
}
