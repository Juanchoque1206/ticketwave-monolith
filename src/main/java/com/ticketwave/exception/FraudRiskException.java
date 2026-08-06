package com.ticketwave.exception;

public class FraudRiskException extends RuntimeException {

    public FraudRiskException(String message) {
        super(message);
    }
}