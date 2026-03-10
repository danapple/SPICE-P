package com.danapple.spicep.api;

import com.danapple.spicep.coincap.CoinCapPriceService;
import com.danapple.spicep.dao.TokenDao;
import com.danapple.spicep.dao.WalletDao;
import com.danapple.spicep.dtos.*;
import com.danapple.spicep.entities.Position;
import com.danapple.spicep.entities.Token;
import com.danapple.spicep.entities.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Transactional
@RestController
@RequestMapping("/wallets")
class WalletApi extends AbstractApi {
    private final WalletDao walletDao;
    private final TokenDao tokenDao;
    private final CoinCapPriceService coinCapPriceService;
    private final Logger logger = LoggerFactory.getLogger(WalletApi.class);

    WalletApi(final WalletDao walletDao,
              final TokenDao tokenDao,
              final CoinCapPriceService coinCapPriceService) {
        this.walletDao = walletDao;
        this.tokenDao = tokenDao;
        this.coinCapPriceService = coinCapPriceService;
    }

    @PostMapping
    ResponseEntity<?> createWallet(@RequestBody CreateWalletRequest request) {
        logger.debug("createWallet {}", request);
        try {
            String emailAddress = validateEmailAddress(request);
            Wallet wallet = new Wallet(UUID.randomUUID().toString(), emailAddress);
            logger.debug("Trying to save wallet {}", wallet);
            HttpStatus status;
            try {
                walletDao.saveWallet(wallet);
                status = HttpStatus.CREATED;
            } catch (Exception e) {
                logger.debug("Failed to save wallet, trying load existing wallet", e);
                wallet = walletDao.getWalletByEmailAddress(emailAddress);
                logger.debug("Wallet already {} already exists", wallet);
                status = HttpStatus.OK;
            }

            return new ResponseEntity<>(new CreateWalletResponse(wallet.walletKey(),
                    wallet.emailAddress()),
                    status);
        }
        catch (ValidationException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.PRECONDITION_FAILED);
        }
    }

    @GetMapping("/{walletKey}")
    ResponseEntity<?> getWallet(@PathVariable("walletKey") String walletKey) {
        logger.debug("getWallet for walletKey {}", walletKey);

        Wallet wallet = walletDao.getWallet(walletKey);
        if (wallet == null) {
            return createErrorResponse("Wallet not found", HttpStatus.NOT_FOUND);
        }
        List<Position> positions = walletDao.getPositions(walletKey);
        Collection<String> tokenKeys = positions.stream().map(Position::tokenKey).collect(Collectors.toSet());
        Map<String, BigDecimal> prices = tokenDao.getPrices(tokenKeys);
        Map<String, Token> tokens = tokenDao.getTokens(tokenKeys);

        List<AssetPosition> assetPositions = new ArrayList<>();
        for (Position position : positions) {
            AssetPosition assetPosition = convertPositionToAssetPosition(position, prices, tokens);
            assetPositions.add(assetPosition);
            
        }
        BigDecimal total = assetPositions
                .stream()
                .map(AssetPosition::value)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        GetWalletResponse response = new GetWalletResponse(walletKey,
                                                           total,
                                                           assetPositions);
        return new ResponseEntity<>(response,
                HttpStatus.OK);
    }

    @PostMapping("/{walletKey}/assets")
    ResponseEntity<?> adjustPosition(@PathVariable("walletKey") String walletKey,
                                                    @RequestBody AddAssetRequest request) {
        logger.debug("addAsset for walletKey {}: {}", walletKey, request);

        try {
            String symbol = validateAssetRequest(request);
            Wallet wallet = walletDao.getWallet(walletKey);
            if (wallet == null) {
                logger.debug("Wallet {} does not exist", walletKey);
                return createErrorResponse("Wallet not found", HttpStatus.NOT_FOUND);
            }
            String tokenKey = setupToken(symbol);
            if (tokenKey == null) {
                return createErrorResponse("Token %s was not found".formatted(symbol),
                        HttpStatus.NOT_FOUND);
            }

            adjustPosition(walletKey, tokenKey, request);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch (ValidationException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.PRECONDITION_FAILED);
        }
    }

    private void adjustPosition(String walletKey, String tokenKey, AddAssetRequest request) {
        Position existingPosition = walletDao.getPosition(walletKey,
                tokenKey);
        if (existingPosition == null) {
            String positionKey = UUID.randomUUID().toString();
            Position newPosition = new Position(positionKey,
                    walletKey,
                    tokenKey,
                    request.getQuantity(),
                    request.getQuantity().multiply(request.getPrice()),
                    BigDecimal.ZERO,
                    0);
            walletDao.savePosition(newPosition);
        }
        else {
            Position newPosition = adjustPosition(existingPosition,
                    request.getQuantity(),
                    request.getPrice());
            walletDao.updatePosition(newPosition);
        }
    }

    Position adjustPosition(Position position, BigDecimal quantity, BigDecimal price) {
        logger.info("adjustPosition BEFORE position {}, quantity {}, price {}",
                    position, quantity, price);
        BigDecimal openingQuantity = quantity;
        BigDecimal newClosedGain = position.closedGain();
        BigDecimal newQuantity = position.quantity();
        BigDecimal newCost = position.cost();

        // closing
        if (position.quantity().compareTo(BigDecimal.ZERO) != 0 &&
            quantity.signum() != position.quantity().signum()) {
            BigDecimal closingQuantity = quantity;
            BigDecimal excess = quantity.abs().subtract(position.quantity().abs());

            if (excess.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal quantityMultiplier = quantity.signum() >= 0 ? BigDecimal.ONE : BigDecimal.ONE.negate();
                closingQuantity = closingQuantity.subtract(excess.multiply(quantityMultiplier));
                openingQuantity = excess.multiply(quantityMultiplier);
            }
            else {
                openingQuantity = BigDecimal.ZERO;
            }

            BigDecimal costBasis = position.cost().divide(position.quantity(),
                                           RoundingMode.HALF_UP);

            BigDecimal closingCost = closingQuantity.multiply(costBasis);
            newQuantity = newQuantity.add(closingQuantity);
            newCost = newCost.add(closingCost);
            newClosedGain = newClosedGain.add(closingQuantity.negate().multiply(price.subtract(costBasis)));
        }
        newQuantity = newQuantity.add(openingQuantity);
        newCost = newCost.add(openingQuantity.multiply(price));

        Position newPosition = new Position(position.positionKey(),
                                            position.walletKey(),
                                            position.tokenKey(),
                                            newQuantity,
                                            newCost,
                                            newClosedGain,
                                            position.versionNumber());

        logger.info("adjustPosition BEFORE position {} AFTER position {}", position, newPosition);

        return newPosition;
    }

    String setupToken(final String symbol) {
        logger.debug("Setting up token for symbol {}", symbol);

        Token token = getOrCreateToken(symbol);
        BigDecimal price = getPriceFromDatabase(token);
        if (price != null) {
            return token.tokenKey();
        }
        logger.debug("No price in database for token {}, checking CoinCap", token);
        try {
            Future<BigDecimal> priceFuture = coinCapPriceService.getPrice(symbol);
            price = priceFuture.get();
            if (price == null) {
                logger.debug("No price from CoinCap for token {}", token);
                return null;
            }
            logger.debug("Got price {} from CoinCap for token {}", price, token);
            tokenDao.saveToken(token);
            tokenDao.savePrice(token.tokenKey(), price);
            return token.tokenKey();
        }
        catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    private Token getOrCreateToken(final String symbol) {
        Map<String, Token> tokens = tokenDao.getTokensBySymbol(Set.of(symbol));
        Token token = tokens.get(symbol);
        if (token == null) {
            token = new Token(UUID.randomUUID().toString(), symbol);
            logger.debug("Token does not exist, creating new token {}", token);
        }
        return token;
    }

    private BigDecimal getPriceFromDatabase(Token token) {
        String tokenKey = token.tokenKey();

        Map<String, BigDecimal> prices = tokenDao.getPrices(Set.of(tokenKey));
        BigDecimal price = prices.get(tokenKey);
        if (price != null) {
            logger.debug("Got price {} in database for token {}", price, token);
            return price;
        }
        return null;
    }

    private static AssetPosition convertPositionToAssetPosition(Position position,
                                                                Map<String, BigDecimal> prices,
                                                                Map<String, Token> tokens) {
        BigDecimal price = prices.get(position.tokenKey());
        BigDecimal value = null;
        BigDecimal gain = null;
        if (price != null) {
            value = price.multiply(position.quantity());
            gain = value.subtract(position.cost());
        }
        Token token = tokens.get(position.tokenKey());
        BigDecimal costBasis = position.quantity().compareTo(BigDecimal.ZERO) == 0 ?
                BigDecimal.ZERO :
                position.cost().divide(position.quantity(),
                        RoundingMode.HALF_UP);
        return new AssetPosition(token.symbol(),
                position.quantity(),
                costBasis,
                position.cost(),
                price,
                value,
                gain,
                position.closedGain());
    }

    private static String validateEmailAddress(CreateWalletRequest request) throws ValidationException {
        String emailAddress = request.getEmailAddress();

        if (emailAddress == null) {
            throw new ValidationException("Email address must be supplied");
        }
        emailAddress = emailAddress.trim();
        if (emailAddress.isEmpty() ||
                emailAddress.length() > 200) {
            throw new ValidationException("Email address must be between 1 and 200 characters");
        }
        return emailAddress;
    }

    private static String validateAssetRequest(AddAssetRequest addAssetRequest) throws ValidationException {
        String symbol = addAssetRequest.getSymbol();
        if (symbol == null) {
            throw new ValidationException("Symbol must be supplied");
        }
        symbol = symbol.trim().toUpperCase();
        if (symbol.isEmpty() ||
                symbol.length() > 200) {
            throw new ValidationException("Symbol must be between 1 and 200 characters");
        }
        if (addAssetRequest.getPrice() == null) {
            throw new ValidationException("Price must be supplied");
        }
        if (addAssetRequest.getQuantity() == null) {
            throw new ValidationException("Quantity must be supplied");
        }
        if (addAssetRequest.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Quantity of 0 is not allowed");
        }
        return symbol;
    }
}
