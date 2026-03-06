package com.danapple.spicep.dtos;

import java.math.BigDecimal;

public class AddAssetRequest {
    private String symbol;
    private BigDecimal price;
    private BigDecimal quantity;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }
}
