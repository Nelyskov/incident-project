package com.example.incedent_processor.services;

import lombok.Getter;

public enum Service {
    PAYMENT_SERVICE("payment-service", "payment-team"),
    AUTH_SERVICE("auth-service", "auth-team"),
    ORDER_SERVICE("order-service", "order-team"),
    INVENTORY_SERVICE("inventory-service", "inventory-team"),
    UNKNOWN(null, "oncall-team");

    private final String serviceName;
    @Getter
    private final String responsibleGroup;

    Service(String serviceName, String responsibleGroup) {
        this.serviceName = serviceName;
        this.responsibleGroup = responsibleGroup;
    }

    public static Service fromServiceName(String serviceName) {
        for (Service service : values()) {
            if (service.serviceName != null && service.serviceName.equals(serviceName)) {
                return service;
            }
        }
        return UNKNOWN;
    }
}
