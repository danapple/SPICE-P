package com.danapple.spicep.api;

import com.danapple.spicep.testdtos.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WalletApiRestTest extends AbstractRestTest {

    @Test
    void respondsWithNotFoundForAbsentWallet() {
        ResponseEntity<String> response = template.getForEntity("/wallets/333", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createNewWallet() {
        ResponseEntity<TestCreateWalletResponse> response =
                template.postForEntity("/wallets",
                                       new TestCreateWalletRequest("createNewWallet@WalletApiTest.com"),
                                       TestCreateWalletResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<TestGetWalletResponse> getResponse =
                template.getForEntity("/wallets/%s".formatted(response.getBody().id()),
                                      TestGetWalletResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    void returnsAlreadyCreatedWallet() {
        ResponseEntity<TestCreateWalletResponse> response =
                template.postForEntity("/wallets",
                                       new TestCreateWalletRequest("returnsAlreadyCreatedWallet@WalletApiTest.com"),
                                       TestCreateWalletResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<TestCreateWalletResponse> response2 =
                template.postForEntity("/wallets",
                                       new TestCreateWalletRequest("returnsAlreadyCreatedWallet@WalletApiTest.com"),
                                       TestCreateWalletResponse.class);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addsPosition() {
        ResponseEntity<TestCreateWalletResponse> createResponse =
                template.postForEntity("/wallets",
                                       new TestCreateWalletRequest("addsPosition@WalletApiTest.com"),
                                       TestCreateWalletResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String walletKey = createResponse.getBody().id();
        TestAddAssetRequest addAssetRequest = new TestAddAssetRequest();
        addAssetRequest.setSymbol("BTC");
        addAssetRequest.setPrice(BigDecimal.TWO);
        addAssetRequest.setQuantity(BigDecimal.TEN);

        ResponseEntity<String> addAssetResponse =
                template.postForEntity("/wallets/%s/assets".formatted(walletKey),
                                       addAssetRequest,
                                       String.class);
        assertThat(addAssetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<TestGetWalletResponse> getResponse =
                template.getForEntity("/wallets/%s".formatted(createResponse.getBody().id()),
                                      TestGetWalletResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestGetWalletResponse getWalletResponse = getResponse.getBody();
        assertThat(getWalletResponse.assets().size()).isEqualTo(1);
        TestAssetPosition assetPosition = getWalletResponse.assets().getFirst();
        assertThat(assetPosition.symbol()).isEqualTo("BTC");
        assertThat(assetPosition.quantity()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(assetPosition.cost()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(assetPosition.closedGain()).isEqualByComparingTo(BigDecimal.ZERO);

    }

    @Test
    void reducesPosition() {
        ResponseEntity<TestCreateWalletResponse> createResponse =
                template.postForEntity("/wallets",
                        new TestCreateWalletRequest("reducesPosition@WalletApiTest.com"),
                        TestCreateWalletResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String walletKey = createResponse.getBody().id();
        TestAddAssetRequest addAssetRequest = new TestAddAssetRequest();
        addAssetRequest.setSymbol("BTC");
        addAssetRequest.setPrice(BigDecimal.TWO);
        addAssetRequest.setQuantity(BigDecimal.TEN);

        ResponseEntity<String> addAssetResponse =
                template.postForEntity("/wallets/%s/assets".formatted(walletKey),
                        addAssetRequest,
                        String.class);
        assertThat(addAssetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        TestAddAssetRequest reduceAssetRequest = new TestAddAssetRequest();
        reduceAssetRequest.setSymbol("BTC");
        reduceAssetRequest.setPrice(BigDecimal.ONE);
        reduceAssetRequest.setQuantity(BigDecimal.ONE.negate());

        ResponseEntity<String> reduceAssetResponse =
                template.postForEntity("/wallets/%s/assets".formatted(walletKey),
                        reduceAssetRequest,
                        String.class);
        assertThat(reduceAssetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<TestGetWalletResponse> getResponse =
                template.getForEntity("/wallets/%s".formatted(createResponse.getBody().id()),
                        TestGetWalletResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestGetWalletResponse getWalletResponse = getResponse.getBody();
        assertThat(getWalletResponse.assets().size()).isEqualTo(1);
        TestAssetPosition assetPosition = getWalletResponse.assets().getFirst();
        assertThat(assetPosition.symbol()).isEqualTo("BTC");
        assertThat(assetPosition.quantity()).isEqualByComparingTo(BigDecimal.valueOf(9));
        assertThat(assetPosition.cost()).isEqualByComparingTo(BigDecimal.valueOf(18));
        assertThat(assetPosition.closedGain()).isEqualByComparingTo(BigDecimal.ONE.negate());

    }
}