package com.danapple.spicep.dtos;

import java.math.BigDecimal;

public record AssetPosition(String symbol,
                            BigDecimal quantity,
                            BigDecimal price,
                            BigDecimal cost,
                            BigDecimal mark,
                            BigDecimal value,
                            BigDecimal openGain,
                            BigDecimal closedGain) {
}