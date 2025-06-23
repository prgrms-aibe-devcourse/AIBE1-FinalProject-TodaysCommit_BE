package com.team5.catdogeats.addresses.domain.enums;

public enum AddressType {
    PERSONAL("개인주소"),
    BUSINESS("사업자주소");

    private final String description;

    AddressType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}