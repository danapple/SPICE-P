package com.danapple.spicep.api;

import com.danapple.spicep.coincap.CoinCapPriceService;
import com.danapple.spicep.dao.TokenDao;
import com.danapple.spicep.dao.WalletDao;
import com.danapple.spicep.dtos.AddAssetRequest;
import com.danapple.spicep.dtos.CreateWalletRequest;
import com.danapple.spicep.dtos.CreateWalletResponse;
import com.danapple.spicep.entities.Position;
import com.danapple.spicep.entities.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static com.danapple.spicep.common.TestConstants.SYMBOL_BTC;
import static com.danapple.spicep.common.TestConstants.TOKEN_KEY_BTC;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WalletApiTest {

    private static final String POSITION_KEY = "a";
    private static final String WALLET_KEY = "b";
    private static final String EMAIL_ADDRESS = "foo@foo.com";

    @Mock(lenient = true)
    private WalletDao walletDao;
    @Mock(lenient = true)
    private TokenDao tokenDao;
    @Mock(lenient = true)
    private CoinCapPriceService coinCapPriceService;

    private WalletApi walletApi;

    @BeforeEach
    public void beforeEach() {
        when(walletDao.getWallet(WALLET_KEY)).thenReturn(new Wallet(WALLET_KEY, EMAIL_ADDRESS));
        when(walletDao.getWallet(null)).thenReturn(null);

        walletApi = new WalletApi(walletDao,
                tokenDao,
                coinCapPriceService);
    }

    @Test
    void rejectsNullEmailAddress() {
        CreateWalletRequest request = new CreateWalletRequest();

        ResponseEntity<?> response = walletApi.createWallet(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Email address must be supplied");
    }

    @Test
    void rejectsEmptyEmailAddress() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setEmailAddress("");

        ResponseEntity<?> response = walletApi.createWallet(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Email address must be between 1 and 200 characters");
    }

    @Test
    void rejectsLongEmailAddress() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setEmailAddress("a".repeat(500));

        ResponseEntity<?> response = walletApi.createWallet(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Email address must be between 1 and 200 characters");
    }

    @Test
    void acceptsProperEmailAddress() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setEmailAddress(EMAIL_ADDRESS);

        ResponseEntity<?> response = walletApi.createWallet(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(CreateWalletResponse.class);
        CreateWalletResponse responseBody = (CreateWalletResponse) response.getBody();
        assertThat(responseBody.emailAddress()).isEqualTo(EMAIL_ADDRESS);
        assertThat(responseBody.id()).isNotBlank();
    }

    @Test
    void rejectsMissingWalletId() {
        String walletKey = null;
        AddAssetRequest addAssetRequest = new AddAssetRequest();
        addAssetRequest.setPrice(BigDecimal.ONE);
        addAssetRequest.setQuantity(BigDecimal.ONE);
        addAssetRequest.setSymbol(SYMBOL_BTC);

        ResponseEntity<?> response = walletApi.adjustPosition(walletKey, addAssetRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Wallet not found");
    }

    @Test
    void rejectsMissingQuantity() {
        AddAssetRequest addAssetRequest = new AddAssetRequest();
        addAssetRequest.setPrice(BigDecimal.ONE);
        addAssetRequest.setSymbol(SYMBOL_BTC);

        ResponseEntity<?> response = walletApi.adjustPosition(WALLET_KEY, addAssetRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Quantity must be supplied");
    }

    @Test
    void rejectsQuantityZero() {
        AddAssetRequest addAssetRequest = new AddAssetRequest();
        addAssetRequest.setPrice(BigDecimal.ONE);
        addAssetRequest.setQuantity(BigDecimal.ZERO);
        addAssetRequest.setSymbol(SYMBOL_BTC);

        ResponseEntity<?> response = walletApi.adjustPosition(WALLET_KEY, addAssetRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Quantity of 0 is not allowed");
    }

    @Test
    void rejectsMissingPrice() {
        AddAssetRequest addAssetRequest = new AddAssetRequest();
        addAssetRequest.setQuantity(BigDecimal.ZERO);
        addAssetRequest.setSymbol(SYMBOL_BTC);

        ResponseEntity<?> response = walletApi.adjustPosition(WALLET_KEY, addAssetRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Price must be supplied");
    }

    @Test
    void rejectsMissingSymbol() {
        AddAssetRequest addAssetRequest = new AddAssetRequest();
        addAssetRequest.setPrice(BigDecimal.ONE);
        addAssetRequest.setQuantity(BigDecimal.ZERO);

        ResponseEntity<?> response = walletApi.adjustPosition(WALLET_KEY, addAssetRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Symbol must be supplied");
    }

    @Test
    void rejectsShortSymbol() {
        AddAssetRequest addAssetRequest = new AddAssetRequest();
        addAssetRequest.setPrice(BigDecimal.ONE);
        addAssetRequest.setQuantity(BigDecimal.ZERO);
        addAssetRequest.setSymbol("");

        ResponseEntity<?> response = walletApi.adjustPosition(WALLET_KEY, addAssetRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Symbol must be between 1 and 200 characters");
    }

    @Test
    void rejectsBlankSymbol() {
        AddAssetRequest addAssetRequest = new AddAssetRequest();
        addAssetRequest.setPrice(BigDecimal.ONE);
        addAssetRequest.setQuantity(BigDecimal.ZERO);
        addAssetRequest.setSymbol("   ");

        ResponseEntity<?> response = walletApi.adjustPosition(WALLET_KEY, addAssetRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Symbol must be between 1 and 200 characters");
    }

    @Test
    void rejectsLongSymbol() {
        AddAssetRequest addAssetRequest = new AddAssetRequest();
        addAssetRequest.setPrice(BigDecimal.ONE);
        addAssetRequest.setQuantity(BigDecimal.ZERO);
        addAssetRequest.setSymbol("a".repeat(500));

        ResponseEntity<?> response = walletApi.adjustPosition(WALLET_KEY, addAssetRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isInstanceOf(String.class);
        String errorMessage = (String) response.getBody();
        assertThat(errorMessage).contains("Symbol must be between 1 and 200 characters");
    }

    @Test
    void opensPosition() {
        Position existingPosition = new Position(POSITION_KEY, WALLET_KEY, TOKEN_KEY_BTC,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        Position result = walletApi.adjustPosition(existingPosition, BigDecimal.ONE, BigDecimal.TWO);
        assertThat(result.quantity()).isEqualTo(BigDecimal.ONE);
        assertThat(result.cost()).isEqualTo(BigDecimal.TWO);
        assertThat(result.closedGain()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void increasesPosition() {
        Position existingPosition = new Position(POSITION_KEY, WALLET_KEY, TOKEN_KEY_BTC,
                BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ZERO, 0);
        Position result = walletApi.adjustPosition(existingPosition, BigDecimal.TWO, BigDecimal.TEN);
        assertThat(result.quantity()).isEqualTo(BigDecimal.valueOf(3));
        assertThat(result.cost()).isEqualTo(BigDecimal.valueOf(22));
        assertThat(result.closedGain()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void reducesPositionWithNoClosedGain() {
        Position existingPosition = new Position(POSITION_KEY, WALLET_KEY, TOKEN_KEY_BTC,
                BigDecimal.TWO, BigDecimal.TWO, BigDecimal.ZERO, 0);
        Position result = walletApi.adjustPosition(existingPosition, BigDecimal.ONE.negate(), BigDecimal.ONE);
        assertThat(result.quantity()).isEqualTo(BigDecimal.ONE);
        assertThat(result.cost()).isEqualTo(BigDecimal.ONE);
        assertThat(result.closedGain()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void reducesPositionWithClosedGain() {
        Position existingPosition = new Position(POSITION_KEY, WALLET_KEY, TOKEN_KEY_BTC,
                BigDecimal.TWO, BigDecimal.TWO, BigDecimal.ZERO, 0);
        Position result = walletApi.adjustPosition(existingPosition, BigDecimal.ONE.negate(), BigDecimal.TWO);
        assertThat(result.quantity()).isEqualTo(BigDecimal.ONE);
        assertThat(result.cost()).isEqualTo(BigDecimal.ONE);
        assertThat(result.closedGain()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void flattensPosition() {
        Position existingPosition = new Position(POSITION_KEY, WALLET_KEY, TOKEN_KEY_BTC,
                BigDecimal.TWO, BigDecimal.TWO, BigDecimal.ZERO, 0);
        Position result = walletApi.adjustPosition(existingPosition, BigDecimal.TWO.negate(), BigDecimal.ONE);
        assertThat(result.quantity()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.cost()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.closedGain()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void overClosesPosition() {
        Position existingPosition = new Position(POSITION_KEY, WALLET_KEY, TOKEN_KEY_BTC,
                BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ZERO, 0);
        Position result = walletApi.adjustPosition(existingPosition, BigDecimal.TWO.negate(), BigDecimal.ONE);
        assertThat(result.quantity()).isEqualTo(BigDecimal.ONE.negate());
        assertThat(result.cost()).isEqualTo(BigDecimal.ONE.negate());
        assertThat(result.closedGain()).isEqualTo(BigDecimal.ONE.negate());
    }
}
