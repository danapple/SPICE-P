package com.danapple.spicep.dao;

public class DatabaseConfiguration {
    private boolean repair;
    private String migrationsLocation;

    public boolean isRepair() {
        return repair;
    }

    public void setRepair(final boolean repair) {
        this.repair = repair;
    }

    public String getMigrationsLocation() {
        return migrationsLocation;
    }

    public void setMigrationsLocation(final String migrationsLocation) {
        this.migrationsLocation = migrationsLocation;
    }
}
