package com.danapple.spicep.dtos;

import java.time.LocalDate;
import java.util.List;

public class SimulateWalletRequest {
    private LocalDate date;
    private List<SimulateAssetRequest> assets;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<SimulateAssetRequest> getAssets() {
        return assets;
    }

    public void setAssets(List<SimulateAssetRequest> assets) {
        this.assets = assets;
    }
}
