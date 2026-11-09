package com.orquidea.bank.card.api.dto;

public record PurchaseStepResponse(
        String stage,
        String purchaseStatus,
        String available,
        String posted
) {
}
