DROP TABLE IF EXISTS position;
DROP TABLE IF EXISTS wallet;
DROP TABLE IF EXISTS price;
DROP TABLE IF EXISTS asset;

CREATE TABLE IF NOT EXISTS token (
    tokenKey VARCHAR PRIMARY KEY,
    symbol VARCHAR NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS price (
    tokenKey VARCHAR UNIQUE REFERENCES token,
    price NUMERIC(20, 10) NOT NULL
);

CREATE TABLE IF NOT EXISTS wallet (
    walletKey VARCHAR PRIMARY KEY,
    emailAddress VARCHAR NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS position (
    positionKey VARCHAR PRIMARY KEY,
    walletKey VARCHAR REFERENCES wallet,
    tokenKey VARCHAR REFERENCES token,
    quantity NUMERIC(20, 10) NOT NULL,
    cost NUMERIC(20, 10) NOT NULL,
    closedGain NUMERIC(20, 10) NOT NULL,
    versionNumber BIGINT NOT NULL
);

CREATE UNIQUE INDEX unq_position_wallet_symbol ON position (walletKey, tokenKey);