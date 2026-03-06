package com.danapple.spicep.testdtos;

import java.math.BigDecimal;

public class TestSimulateAssetRequest {
    private String symbol;
    private BigDecimal value;
    private BigDecimal quantity;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }
}