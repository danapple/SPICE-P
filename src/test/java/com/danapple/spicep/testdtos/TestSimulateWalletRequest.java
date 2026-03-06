package com.danapple.spicep.testdtos;

import java.time.LocalDate;
import java.util.List;

public class TestSimulateWalletRequest {
    private LocalDate date;
    private List<TestSimulateAssetRequest> assets;

    public List<TestSimulateAssetRequest> getAssets() {
        return assets;
    }

    public void setAssets(List<TestSimulateAssetRequest> assets) {
        this.assets = assets;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
