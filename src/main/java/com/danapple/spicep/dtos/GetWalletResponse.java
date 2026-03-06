package com.danapple.spicep.dtos;

import java.math.BigDecimal;
import java.util.List;

public record GetWalletResponse(String id,
                                BigDecimal total,
                                List<AssetPosition> assets) {
}