package com.danapple.spicep.api;

import com.danapple.spicep.testdtos.TestSimulateAssetRequest;
import com.danapple.spicep.testdtos.TestSimulateWalletRequest;
import com.danapple.spicep.testdtos.TestSimulateWalletResponse;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SimulationApiRestTest extends AbstractRestTest {
    private final static BigDecimal WALLET_BTC_VALUE_2025_03_03 = BigDecimal.valueOf(43087.50);
    private final static BigDecimal WALLET_BTC_ETH_VALUE_2025_03_03 = BigDecimal.valueOf(52222.80);

    private final static LocalDate DATE_2025_03_03 = LocalDate.of(2025, 3, 3);

    @Test
    void simulateWithOneAssetReturnsSameBestAndWorst() {
        List<TestSimulateAssetRequest> assets = new ArrayList<>();
        TestSimulateAssetRequest assetRequest = new TestSimulateAssetRequest();
        assetRequest.setSymbol("BTC");
        assetRequest.setValue(BigDecimal.valueOf(35000));
        assetRequest.setQuantity(BigDecimal.valueOf(.5));
        assets.add(assetRequest);

        TestSimulateWalletRequest simluateWalletRequest = new TestSimulateWalletRequest();
        simluateWalletRequest.setAssets(assets);
        simluateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<TestSimulateWalletResponse> response =
                template.postForEntity("/simulate",
                        simluateWalletRequest,
                        TestSimulateWalletResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestSimulateWalletResponse simulateWalletResponse = response.getBody();
        assertThat(simulateWalletResponse.best_asset()).isEqualTo("BTC");
        assertThat(simulateWalletResponse.best_asset()).isEqualTo(simulateWalletResponse.worst_asset());

        assertThat(simulateWalletResponse.best_performance())
                .isCloseTo(BigDecimal.valueOf(23.11), Percentage.withPercentage(1));
        assertThat(simulateWalletResponse.best_performance())
                .isEqualByComparingTo(simulateWalletResponse.worst_performance());

        assertThat(simulateWalletResponse.total())
                .isCloseTo(WALLET_BTC_VALUE_2025_03_03, Percentage.withPercentage(1));
    }

    @Test
    void simulateWithTwoAssetsReturnsDifferentBestAndWorst() {
        List<TestSimulateAssetRequest> assets = new ArrayList<>();
        TestSimulateAssetRequest assetRequestBtc = new TestSimulateAssetRequest();
        assetRequestBtc.setSymbol("BTC");
        assetRequestBtc.setValue(BigDecimal.valueOf(35000));
        assetRequestBtc.setQuantity(BigDecimal.valueOf(.5));
        assets.add(assetRequestBtc);

        TestSimulateAssetRequest assetRequestEth = new TestSimulateAssetRequest();
        assetRequestEth.setSymbol("ETH");
        assetRequestEth.setValue(BigDecimal.valueOf(15310.71));
        assetRequestEth.setQuantity(BigDecimal.valueOf(4.25));
        assets.add(assetRequestEth);

        TestSimulateWalletRequest simluateWalletRequest = new TestSimulateWalletRequest();
        simluateWalletRequest.setAssets(assets);
        simluateWalletRequest.setDate(DATE_2025_03_03);

        ResponseEntity<TestSimulateWalletResponse> response =
                template.postForEntity("/simulate",
                        simluateWalletRequest,
                        TestSimulateWalletResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestSimulateWalletResponse simulateWalletResponse = response.getBody();

        assertThat(simulateWalletResponse.best_asset()).isEqualTo("BTC");
        assertThat(simulateWalletResponse.worst_asset()).isEqualTo("ETH");
        assertThat(simulateWalletResponse.best_performance())
                .isCloseTo(BigDecimal.valueOf(23.11), Percentage.withPercentage(1));
        assertThat(simulateWalletResponse.worst_performance())
                .isCloseTo(BigDecimal.valueOf(-40.33), Percentage.withPercentage(1));
        assertThat(simulateWalletResponse.total())
                .isCloseTo(WALLET_BTC_ETH_VALUE_2025_03_03, Percentage.withPercentage(1));

    }
}
