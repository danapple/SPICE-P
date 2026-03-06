package com.danapple.spicep.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SimulateWalletResponse(BigDecimal total,
                                     String best_asset,
                                     BigDecimal best_performance,
                                     String worst_asset,
                                     BigDecimal worst_performance,
                                     LocalDate date) {
}
