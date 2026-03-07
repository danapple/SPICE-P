package com.danapple.spicep.testdtos;

import java.math.BigDecimal;

public record TestAssetPosition(String symbol,
                                BigDecimal quantity,
                                BigDecimal cost,
                                BigDecimal mark,
                                BigDecimal value,
                                BigDecimal openGain,
                                BigDecimal closedGain) {
}