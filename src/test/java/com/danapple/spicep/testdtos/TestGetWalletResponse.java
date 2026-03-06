package com.danapple.spicep.testdtos;

import java.math.BigDecimal;
import java.util.List;

public record TestGetWalletResponse(String id,
                                    BigDecimal total,
                                    List<TestAssetPosition> assets) {
}