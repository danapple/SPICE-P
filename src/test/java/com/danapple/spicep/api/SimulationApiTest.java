package com.danapple.spicep.api;

import com.danapple.spicep.coincap.CoinCapHistoricalPriceService;
import com.danapple.spicep.dtos.SimulateAssetRequest;
import com.danapple.spicep.dtos.SimulateWalletRequest;
import com.danapple.spicep.dtos.SimulateWalletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.danapple.spicep.common.TestConstants.SYMBOL_BTC;
import static com.danapple.spicep.common.TestConstants.SYMBOL_ETH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SimulationApiTest {
    @Mock(lenient = true)
    private CoinCapHistoricalPriceService coinCapHistoricalPriceService;

    private SimulationApi simulationApi;

    private final static LocalDate DATE_2025_03_03 = LocalDate.of(2025, 3, 3);

    @BeforeEach
    public void beforeEach() {
        when(coinCapHistoricalPriceService.getHistoricalPrice(eq(SYMBOL_BTC), any())).thenReturn(CompletableFuture.completedFuture(BigDecimal.TWO));
        when(coinCapHistoricalPriceService.getHistoricalPrice(eq(SYMBOL_ETH), any())).thenReturn(CompletableFuture.completedFuture(BigDecimal.TEN));

        simulationApi = new SimulationApi(coinCapHistoricalPriceService);
    }

    @Test
    void computesLosingPercentage() {
        List<SimulateAssetRequest> assets = new ArrayList<>();
        SimulateAssetRequest assetRequest = new SimulateAssetRequest();
        assetRequest.setSymbol("BTC");
        assetRequest.setValue(BigDecimal.valueOf(4));
        assetRequest.setQuantity(BigDecimal.ONE);
        assets.add(assetRequest);

        SimulateWalletRequest simluateWalletRequest = new SimulateWalletRequest();
        simluateWalletRequest.setAssets(assets);
        simluateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<? extends Object> responseEntity = simulationApi.simulateWallet(simluateWalletRequest);
        Object body = responseEntity.getBody();
        assertThat(body).isInstanceOf(SimulateWalletResponse.class);

        SimulateWalletResponse response = (SimulateWalletResponse) body;
        assertThat(response.best_asset()).isEqualTo(SYMBOL_BTC);
        assertThat(response.best_performance()).isEqualByComparingTo(BigDecimal.valueOf(-50));
    }

    @Test
    void computesGainingPercentage() {
        List<SimulateAssetRequest> assets = new ArrayList<>();
        SimulateAssetRequest assetRequest = new SimulateAssetRequest();
        assetRequest.setSymbol("BTC");
        assetRequest.setValue(BigDecimal.ONE);
        assetRequest.setQuantity(BigDecimal.ONE);
        assets.add(assetRequest);

        SimulateWalletRequest simluateWalletRequest = new SimulateWalletRequest();
        simluateWalletRequest.setAssets(assets);
        simluateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<? extends Object> responseEntity = simulationApi.simulateWallet(simluateWalletRequest);
        Object body = responseEntity.getBody();
        assertThat(body).isInstanceOf(SimulateWalletResponse.class);

        SimulateWalletResponse response = (SimulateWalletResponse) body;
        assertThat(response.best_asset()).isEqualTo(SYMBOL_BTC);
        assertThat(response.best_performance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void computesTotal() {
        List<SimulateAssetRequest> assets = new ArrayList<>();
        SimulateAssetRequest assetRequestBtc = new SimulateAssetRequest();
        assetRequestBtc.setSymbol("BTC");
        assetRequestBtc.setValue(BigDecimal.ONE);
        assetRequestBtc.setQuantity(BigDecimal.ONE);
        assets.add(assetRequestBtc);

        SimulateAssetRequest assetRequestEth = new SimulateAssetRequest();
        assetRequestEth.setSymbol("ETH");
        assetRequestEth.setValue(BigDecimal.ONE);
        assetRequestEth.setQuantity(BigDecimal.ONE);
        assets.add(assetRequestEth);

        SimulateWalletRequest simluateWalletRequest = new SimulateWalletRequest();
        simluateWalletRequest.setAssets(assets);
        simluateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<? extends Object> responseEntity = simulationApi.simulateWallet(simluateWalletRequest);
        Object body = responseEntity.getBody();
        assertThat(body).isInstanceOf(SimulateWalletResponse.class);

        SimulateWalletResponse response = (SimulateWalletResponse) body;
        assertThat(response.total()).isEqualByComparingTo(BigDecimal.valueOf(12));
    }
}
