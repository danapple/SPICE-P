package com.danapple.spicep.api;

import com.danapple.spicep.coincap.CoinCapHistoricalPriceService;
import com.danapple.spicep.coincap.CoinCapPriceService;
import com.danapple.spicep.dtos.SimulateAssetRequest;
import com.danapple.spicep.dtos.SimulateWalletRequest;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
public class SimulationApiValidationTest {
    @Mock(lenient = true)
    private CoinCapPriceService coinCapPriceService;
    @Mock(lenient = true)
    private CoinCapHistoricalPriceService coinCapHistoricalPriceService;

    private SimulationApi simulationApi;

    private final static LocalDate DATE_2025_03_03 = LocalDate.of(2025, 3, 3);

    @BeforeEach
    public void beforeEach() {
        when(coinCapHistoricalPriceService.getHistoricalPrice(eq(SYMBOL_BTC), any())).thenReturn(CompletableFuture.completedFuture(BigDecimal.TWO));
        when(coinCapHistoricalPriceService.getHistoricalPrice(eq(SYMBOL_ETH), any())).thenReturn(CompletableFuture.completedFuture(BigDecimal.TEN));

        when(coinCapPriceService.getPrice(eq(SYMBOL_BTC))).thenReturn(CompletableFuture.completedFuture(BigDecimal.TWO));
        when(coinCapPriceService.getPrice(eq(SYMBOL_ETH))).thenReturn(CompletableFuture.completedFuture(BigDecimal.TEN));

        simulationApi = new SimulationApi(coinCapPriceService, coinCapHistoricalPriceService);
    }

    @Test
    void rejectsMissingValue() {
        List<SimulateAssetRequest> assets = new ArrayList<>();
        SimulateAssetRequest assetRequest = new SimulateAssetRequest();
        assetRequest.setSymbol("BTC");
        assetRequest.setQuantity(BigDecimal.valueOf(.5));
        assets.add(assetRequest);

        SimulateWalletRequest simulateWalletRequest = new SimulateWalletRequest();
        simulateWalletRequest.setAssets(assets);
        simulateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<?> response = simulationApi.simulateWallet(simulateWalletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        AssertionsForClassTypes.assertThat(response.getBody()).isInstanceOf(String.class);
        assertThat(response.getBody()).isEqualTo("Value must be supplied");
    }

    @Test
    void rejectsValueOfZero() {
        List<SimulateAssetRequest> assets = new ArrayList<>();
        SimulateAssetRequest assetRequest = new SimulateAssetRequest();
        assetRequest.setSymbol("BTC");
        assetRequest.setValue(BigDecimal.ZERO);
        assetRequest.setQuantity(BigDecimal.valueOf(.5));
        assets.add(assetRequest);

        SimulateWalletRequest simulateWalletRequest = new SimulateWalletRequest();
        simulateWalletRequest.setAssets(assets);
        simulateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<?> response = simulationApi.simulateWallet(simulateWalletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        AssertionsForClassTypes.assertThat(response.getBody()).isInstanceOf(String.class);
        assertThat(response.getBody()).isEqualTo("Value of 0 is not allowed");
    }

    @Test
    void rejectsMissingQuantity() {
        List<SimulateAssetRequest> assets = new ArrayList<>();
        SimulateAssetRequest assetRequest = new SimulateAssetRequest();
        assetRequest.setSymbol("BTC");
        assets.add(assetRequest);

        SimulateWalletRequest simulateWalletRequest = new SimulateWalletRequest();
        simulateWalletRequest.setAssets(assets);
        simulateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<?> response = simulationApi.simulateWallet(simulateWalletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        AssertionsForClassTypes.assertThat(response.getBody()).isInstanceOf(String.class);
        assertThat(response.getBody()).isEqualTo("Quantity must be supplied");
    }

    @Test
    void rejectsQuantityOfZero() {
        List<SimulateAssetRequest> assets = new ArrayList<>();
        SimulateAssetRequest assetRequest = new SimulateAssetRequest();
        assetRequest.setSymbol("BTC");
        assetRequest.setValue(BigDecimal.ZERO);
        assetRequest.setQuantity(BigDecimal.ZERO);
        assets.add(assetRequest);

        SimulateWalletRequest simulateWalletRequest = new SimulateWalletRequest();
        simulateWalletRequest.setAssets(assets);
        simulateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<?> response = simulationApi.simulateWallet(simulateWalletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        AssertionsForClassTypes.assertThat(response.getBody()).isInstanceOf(String.class);
        assertThat(response.getBody()).isEqualTo("Quantity of 0 is not allowed");
    }

    @Test
    void rejectsMissingSymbol() {
        List<SimulateAssetRequest> assets = new ArrayList<>();
        SimulateAssetRequest assetRequest = new SimulateAssetRequest();
        assetRequest.setValue(BigDecimal.ONE);
        assetRequest.setQuantity(BigDecimal.valueOf(.5));
        assets.add(assetRequest);

        SimulateWalletRequest simulateWalletRequest = new SimulateWalletRequest();
        simulateWalletRequest.setAssets(assets);
        simulateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<?> response = simulationApi.simulateWallet(simulateWalletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        AssertionsForClassTypes.assertThat(response.getBody()).isInstanceOf(String.class);
        assertThat(response.getBody()).isEqualTo("Symbol must be supplied");
    }

    @Test
    void rejectsBlankSymbol() {
        List<SimulateAssetRequest> assets = new ArrayList<>();
        SimulateAssetRequest assetRequest = new SimulateAssetRequest();
        assetRequest.setSymbol("");
        assetRequest.setQuantity(BigDecimal.valueOf(.5));
        assets.add(assetRequest);

        SimulateWalletRequest simulateWalletRequest = new SimulateWalletRequest();
        simulateWalletRequest.setAssets(assets);
        simulateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<?> response = simulationApi.simulateWallet(simulateWalletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        AssertionsForClassTypes.assertThat(response.getBody()).isInstanceOf(String.class);
        assertThat(response.getBody()).isEqualTo("Symbol must be between 1 and 200 characters");
    }

    @Test
    void rejectsLongSymbol() {
        List<SimulateAssetRequest> assets = new ArrayList<>();
        SimulateAssetRequest assetRequest = new SimulateAssetRequest();
        assetRequest.setSymbol("f".repeat(403));
        assetRequest.setQuantity(BigDecimal.valueOf(.5));
        assets.add(assetRequest);

        SimulateWalletRequest simulateWalletRequest = new SimulateWalletRequest();
        simulateWalletRequest.setAssets(assets);
        simulateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<?> response = simulationApi.simulateWallet(simulateWalletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        AssertionsForClassTypes.assertThat(response.getBody()).isInstanceOf(String.class);
        assertThat(response.getBody()).isEqualTo("Symbol must be between 1 and 200 characters");
    }
}
