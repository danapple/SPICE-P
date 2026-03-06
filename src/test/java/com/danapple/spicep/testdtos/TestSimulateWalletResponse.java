package com.danapple.spicep.testdtos;

import java.math.BigDecimal;

public record TestSimulateWalletResponse(BigDecimal total,
                                         String best_asset,
                                         BigDecimal best_performance,
                                         String worst_asset,
                                         BigDecimal worst_performance) {
}
