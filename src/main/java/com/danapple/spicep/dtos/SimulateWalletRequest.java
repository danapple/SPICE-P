package com.danapple.spicep.dtos;

import java.util.List;

public class SimulateWalletRequest {
    private List<SimulateAssetRequest> assets;

    public List<SimulateAssetRequest> getAssets() {
        return assets;
    }

    public void setAssets(List<SimulateAssetRequest> assets) {
        this.assets = assets;
    }
}
