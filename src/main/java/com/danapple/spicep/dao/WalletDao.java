package com.danapple.spicep.dao;

import com.danapple.spicep.entities.Position;
import com.danapple.spicep.entities.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WalletDao {
    private final JdbcClient jdbcClient;
    private final static String BASIC_POSITON_QUERY = """
            SELECT
            positionKey, walletKey, tokenKey, quantity, cost, closedGain, versionNumber
            FROM position""";


    private final Logger logger = LoggerFactory.getLogger(WalletDao.class);

    WalletDao(@Qualifier("spicepJdbcClient") final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void saveWallet(final Wallet wallet) {
        logger.debug("Saving wallet {}", wallet);

        JdbcClient.StatementSpec updateStatement = jdbcClient.sql("""
            INSERT INTO wallet
            (walletKey, emailAddress)
            VALUES
            (:walletKey, :emailAddress)
            """)
                .param("walletKey", wallet.walletKey())
                .param("emailAddress", wallet.emailAddress());
        updateStatement.update();
    }

    public Wallet getWallet(final String walletKey) {
        JdbcClient.StatementSpec queryStatement = jdbcClient.sql("""
            SELECT
            emailAddress
            FROM wallet
            WHERE walletKey = :walletKey
            """)
                .param("walletKey", walletKey);
        JdbcClient.ResultQuerySpec query = queryStatement.query();
        List<Map<String, Object>> rows = query.listOfRows();
        if (!rows.isEmpty()) {
            Wallet wallet = new Wallet(walletKey, (String) rows.getFirst().get("emailAddress"));
            logger.debug("Retrieved wallet {} by walletKey", wallet);
            return wallet;
        }
        return null;
    }

    public Wallet getWalletByEmailAddress(final String emailAddress) {
        JdbcClient.StatementSpec queryStatement = jdbcClient.sql("""
            SELECT
            walletKey
            FROM wallet
            WHERE emailAddress = :emailAddress
            """)
                .param("emailAddress", emailAddress);
        JdbcClient.ResultQuerySpec query = queryStatement.query();
        List<Map<String, Object>> rows = query.listOfRows();
        if (!rows.isEmpty()) {
            String walletKey = (String)rows.getFirst().get("walletKey");
            Wallet wallet = new Wallet(walletKey, emailAddress);
            logger.debug("Retrieved wallet {} by emailAddress", wallet);
            return wallet;
        }
        return null;
    }

    public List<Position> getPositions(final String walletKey) {
        JdbcClient.StatementSpec queryStatement = jdbcClient.sql("""
            %s
            WHERE walletKey = :walletKey
            """.formatted(BASIC_POSITON_QUERY))
                .param("walletKey", walletKey);
        JdbcClient.ResultQuerySpec query = queryStatement.query();
        List<Map<String, Object>> rows = query.listOfRows();
        List<Position> positions = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            positions.add(createPositionFromRow(row));
        }
        logger.debug("Retrieved {} positions for wallet with walletKey {}",
                     positions.size(),
                     walletKey);

        return positions;
    }

    public Position getPosition(final String walletKey,
                                final String tokenKey) {
        JdbcClient.StatementSpec queryStatement = jdbcClient.sql("""
            %s
            WHERE walletKey = :walletKey AND tokenKey = :tokenKey
            """.formatted(BASIC_POSITON_QUERY))
                .param("walletKey", walletKey)
                .param("tokenKey", tokenKey);
        JdbcClient.ResultQuerySpec query = queryStatement.query();
        List<Map<String, Object>> rows = query.listOfRows();
        if (rows.isEmpty()) {
            return null;
        }
        if (rows.size() > 1) {
            throw new RuntimeException("Expected 1 row, returned " + rows.size() + " rows");
        }
        Map<String, Object> row = rows.getFirst();
        Position position = createPositionFromRow(row);
        logger.debug("Retrieved position {} for tokenKey {} in wallet {}",
                     position,
                     walletKey,
                     tokenKey);

        return position;
    }

    private static Position createPositionFromRow(final Map<String, Object> row) {
        return new Position((String) row.get("positionKey"),
                            (String) row.get("walletKey"),
                            (String) row.get("tokenKey"),
                            (BigDecimal) row.get("quantity"),
                            (BigDecimal) row.get("cost"),
                            (BigDecimal) row.get("closedGain"),
                            (Long) row.get("versionNumber"));
    }

    public void savePosition(final Position position) {
        logger.debug("Saving Position {}", position);

        JdbcClient.StatementSpec updateStatement = jdbcClient.sql("""
               INSERT INTO position
               (walletKey, positionKey, tokenKey, quantity, cost, closedGain, versionNumber)
               VALUES
               (:walletKey, :positionKey, :tokenKey, :quantity, :cost, :closedGain, :versionNumber)
               """);

        updateStatement.param("walletKey", position.walletKey())
                .param("positionKey", position.positionKey())
                .param("tokenKey",
                       position.tokenKey())
                .param("quantity", position.quantity())
                .param("cost", position.cost())
                .param("closedGain",
                       position.closedGain())
                .param("versionNumber", position.versionNumber());
        int rowsUpdated = updateStatement.update();
        if (rowsUpdated == 0) {
            throw new OptimisticLockingFailureException("Could not insert Position");
        }
    }


    public void updatePosition(final Position position) {
        logger.debug("Updating Position {}", position);

        JdbcClient.StatementSpec updateStatement = jdbcClient.sql("""
                UPDATE position SET quantity = :quantity,
                    cost = :cost,
                    closedGain = :closedGain,
                    versionNumber = :versionNumber + 1
                    WHERE positionKey = :positionKey
                    AND versionNumber = :versionNumber
                """);

        updateStatement.param("walletKey", position.walletKey())
                .param("positionKey", position.positionKey())
                .param("tokenKey",
                       position.tokenKey())
                .param("quantity", position.quantity())
                .param("cost", position.cost())
                .param("closedGain",
                       position.closedGain())
                .param("versionNumber", position.versionNumber());
        int rowsUpdated = updateStatement.update();
        if (rowsUpdated == 0) {
            throw new OptimisticLockingFailureException("Could not modify Position");
        }
    }
}
