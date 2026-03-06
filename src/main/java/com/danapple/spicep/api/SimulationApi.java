package com.danapple.spicep.api;

import com.danapple.spicep.coincap.CoinCapHistoricalPriceService;
import com.danapple.spicep.dtos.SimulateAssetRequest;
import com.danapple.spicep.dtos.SimulateWalletRequest;
import com.danapple.spicep.dtos.SimulateWalletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@Transactional
@RestController
@RequestMapping("/simulate")
class SimulationApi extends AbstractApi {
    private final static BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private final CoinCapHistoricalPriceService coinCapHistoricalPriceService;
    private final Logger logger = LoggerFactory.getLogger(SimulationApi.class);

    SimulationApi(final CoinCapHistoricalPriceService coinCapHistoricalPriceService) {
        this.coinCapHistoricalPriceService = coinCapHistoricalPriceService;
    }

    @PostMapping
    ResponseEntity<? extends Object> simulateWallet(@RequestBody SimulateWalletRequest request) {
        try {
            List<SimulateAssetRequest> simulateAssetRequests = request.getAssets();
            Map<String, Future<BigDecimal>> futures = new HashMap<>();
            for (SimulateAssetRequest requestAsset : simulateAssetRequests) {
                String symbol = requestAsset.getSymbol();
                futures.put(symbol, coinCapHistoricalPriceService.getHistoricalPrice(symbol, request.getDate()));
            }
            BigDecimal totalValue = BigDecimal.ZERO;
            String bestSymbol = null;
            BigDecimal bestGain = BigDecimal.ZERO;
            BigDecimal bestPerformance = BigDecimal.ZERO;

            String worstSymbol = null;
            BigDecimal worstGain = BigDecimal.ZERO;
            BigDecimal worstPerformance = BigDecimal.ZERO;

            for (SimulateAssetRequest requestAsset : simulateAssetRequests) {
                String symbol = requestAsset.getSymbol();
                Future<BigDecimal> future = futures.get(requestAsset.getSymbol());
                BigDecimal price = future.get();
                BigDecimal currentValue = requestAsset.getQuantity().multiply(price);
                totalValue = totalValue.add(currentValue);
                BigDecimal gain = currentValue.subtract(requestAsset.getValue());
                BigDecimal currentPerformance = gain.multiply(ONE_HUNDRED)
                        .divide(requestAsset.getValue(),
                                2,
                                RoundingMode.HALF_UP);

                logger.debug("symbol {}, value {}, quantity {}, price {}, currentValue {}, gain {}, currentPerformance {}",
                        symbol,
                        requestAsset.getValue(),
                        requestAsset.getQuantity(),
                        price,
                        currentValue,
                        gain,
                        currentPerformance);

                if (bestSymbol == null || gain.compareTo(bestGain) > 0) {
                    bestSymbol = symbol;
                    bestGain = gain;
                    bestPerformance = currentPerformance;
                }
                if (worstSymbol == null || gain.compareTo(worstGain) < 0) {
                    worstSymbol = symbol;
                    worstGain = gain;
                    worstPerformance = currentPerformance;
                }
            }

            SimulateWalletResponse response = new SimulateWalletResponse(totalValue,
                    bestSymbol,
                    bestPerformance,
                    worstSymbol,
                    worstPerformance,
                    request.getDate());
            HttpStatus status = HttpStatus.OK;

            return new ResponseEntity<>(response,
                    status);
        }
        catch (Exception ex) {
            logger.warn("Could not simulate", ex);
            return createErrorResponse("Could not simulate", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
