package com.danapple.spicep.api;

import com.danapple.spicep.coincap.CoinCapPriceService;
import com.danapple.spicep.dao.TokenDao;
import com.danapple.spicep.dao.WalletDao;
import com.danapple.spicep.entities.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.danapple.spicep.common.TestConstants.TOKEN_KEY_BTC;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class WalletApiTest {

    public static final String POSITION_KEY = "a";
    public static final String WALLET_KEY = "b";
    @Mock(lenient = true)
    private WalletDao walletDao;
    @Mock(lenient = true)
    private TokenDao tokenDao;
    @Mock(lenient = true)
    private CoinCapPriceService coinCapPriceService;

    private WalletApi walletApi;

    @BeforeEach
    public void beforeEach() {
        walletApi = new WalletApi(walletDao,
                tokenDao,
                coinCapPriceService);
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
