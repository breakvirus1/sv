package com.example.calculatorservice.entity;

public enum UnitType {
    SQUARE_METER("м²"),
    LINEAR_METER("п.м."),
    PIECE("шт");

    private final String displayName;

    UnitType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
