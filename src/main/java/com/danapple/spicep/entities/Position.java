package com.danapple.spicep.entities;

import java.math.BigDecimal;

public record Position(String positionKey,
                       String walletKey,
                       String tokenKey,
                       BigDecimal quantity,
                       BigDecimal cost,
                       BigDecimal closedGain,
                       long versionNumber) {}