package com.orquidea.bank.ledger.api.dto;

public record AccountBalanceResponse(
        String code,
        String name,
        String type,
        String posted,
        String available
) {
}
