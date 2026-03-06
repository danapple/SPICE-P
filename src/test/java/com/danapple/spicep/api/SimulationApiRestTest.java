package com.danapple.spicep.api;

import com.danapple.spicep.testdtos.TestSimulateAssetRequest;
import com.danapple.spicep.testdtos.TestSimulateWalletRequest;
import com.danapple.spicep.testdtos.TestSimulateWalletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SimulationApiRestTest extends AbstractRestTest {
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

        ResponseEntity<TestSimulateWalletResponse> response =
                template.postForEntity("/simulate",
                        simluateWalletRequest,
                        TestSimulateWalletResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestSimulateWalletResponse simulateWalletResponse = response.getBody();
        assertThat(simulateWalletResponse.best_asset()).isEqualTo(simulateWalletResponse.worst_asset());
        assertThat(simulateWalletResponse.best_performance()).isEqualTo(simulateWalletResponse.worst_performance());
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

        ResponseEntity<TestSimulateWalletResponse> response =
                template.postForEntity("/simulate",
                        simluateWalletRequest,
                        TestSimulateWalletResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestSimulateWalletResponse simulateWalletResponse = response.getBody();

        assertThat(simulateWalletResponse.best_asset()).isNotEqualTo(simulateWalletResponse.worst_asset());
        assertThat(simulateWalletResponse.best_performance()).isNotEqualTo(simulateWalletResponse.worst_performance());
        assertThat(simulateWalletResponse.best_performance()).isGreaterThan(simulateWalletResponse.worst_performance());
    }
}
