package com.danapple.spicep.dao;

import com.danapple.spicep.entities.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TokenDao {
    private final JdbcClient jdbcClient;
    private final Logger logger = LoggerFactory.getLogger(TokenDao.class);

    TokenDao(@Qualifier("spicepJdbcClient") final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void saveToken(Token token) {
        logger.debug("Saving token {}", token);
        JdbcClient.StatementSpec updateStatement = jdbcClient.sql("""
            INSERT INTO token
            (tokenKey, symbol)
            VALUES
            (:tokenKey, :symbol)
            """)
                .param("tokenKey", token.tokenKey())
                .param("symbol", token.symbol());
        updateStatement.update();
    }

    public Map<String, Token> getAllTokens() {
        JdbcClient.StatementSpec queryStatement = jdbcClient.sql("""
            SELECT
            tokenKey, symbol
            FROM token
            """);
        JdbcClient.ResultQuerySpec query = queryStatement.query();
        List<Map<String, Object>> rows = query.listOfRows();
        Map<String, Token> tokens = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String tokenKey = (String)row.get("tokenKey");
            String symbol = (String)row.get("symbol");
            Token token = new Token(tokenKey, symbol);
            tokens.put(token.tokenKey(),
                       token);
        }
        logger.debug("Retrieved {} tokens",
                     tokens.size());
        return tokens;
    }

    public Map<String, Token> getTokens(final Collection<String> tokenKeys) {
        JdbcClient.StatementSpec queryStatement = jdbcClient.sql("""
            SELECT
            tokenKey, symbol
            FROM token
            WHERE tokenKey IN (:tokenKeys)
            """)
                .param("tokenKeys", tokenKeys);
        JdbcClient.ResultQuerySpec query = queryStatement.query();
        List<Map<String, Object>> rows = query.listOfRows();
        Map<String, Token> tokens = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String tokenKey = (String)row.get("tokenKey");
            String symbol = (String)row.get("symbol");
            Token token = new Token(tokenKey, symbol);
            tokens.put(token.tokenKey(),
                       token);
        }
        logger.debug("Retrieved {} tokens for {} tokenKeys",
                     tokens.size(),
                     tokenKeys.size());
        return tokens;
    }

    public Map<String, Token> getTokensBySymbol(final Collection<String> symbols) {
        JdbcClient.StatementSpec queryStatement = jdbcClient.sql("""
            SELECT
            tokenKey, symbol
            FROM token
            WHERE symbol IN (:symbols)
            """)
                .param("symbols", symbols);
        JdbcClient.ResultQuerySpec query = queryStatement.query();
        List<Map<String, Object>> rows = query.listOfRows();
        Map<String, Token> tokens = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String tokenKey = (String)row.get("tokenKey");
            String symbol = (String)row.get("symbol");
            Token token = new Token(tokenKey, symbol);
            tokens.put(token.symbol(),
                       token);
        }
        logger.debug("Retrieved {} tokens for {} symbols",
                     tokens.size(),
                     symbols.size());
        return tokens;
    }

    public Map<String, BigDecimal> getPrices(final Collection<String> tokenKeys) {
        JdbcClient.StatementSpec queryStatement = jdbcClient.sql("""
            SELECT
            tokenKey, price
            FROM price
            WHERE tokenKey IN (:tokenKeys)
            """)
                .param("tokenKeys", tokenKeys);
        JdbcClient.ResultQuerySpec query = queryStatement.query();
        List<Map<String, Object>> rows = query.listOfRows();
        Map<String, BigDecimal> prices = new HashMap<>();
        for (Map<String, Object> row : rows) {
            prices.put((String)row.get("tokenKey"),
                                       (BigDecimal)row.get("price"));
        }
        logger.debug("Retrieved {} prices for {} tokenKeys",
                     prices.size(),
                     tokenKeys.size());

        return prices;
    }

    public void savePrice(final String tokenKey,
                          final BigDecimal price) {
        logger.debug("Saving price for tokenKey {}: {}}", tokenKey, price);

        JdbcClient.StatementSpec updateStatement = jdbcClient.sql("""
                MERGE INTO price
                USING DUAL
                    ON ( tokenKey = :tokenKey)
                WHEN MATCHED THEN
                    UPDATE SET price = :price
                WHEN NOT MATCHED THEN
                        INSERT (tokenKey, price)
                        VALUES
                     (:tokenKey, :price)
                """);

        updateStatement.param("tokenKey", tokenKey)
                .param("price", price);
        updateStatement.update();
    }
}
