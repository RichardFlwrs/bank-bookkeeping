package com.orquidea.bank.card.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePurchaseRequest(
        @NotBlank String cardToken,
        @NotNull BigDecimal amount
) {
}
