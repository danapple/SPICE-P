package com.danapple.spicep.testdtos;

import java.util.List;

public class TestSimulateWalletRequest {
    private List<TestSimulateAssetRequest> assets;

    public List<TestSimulateAssetRequest> getAssets() {
        return assets;
    }

    public void setAssets(List<TestSimulateAssetRequest> assets) {
        this.assets = assets;
    }
}
