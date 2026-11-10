package com.orquidea.bank.card.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(name = "CreatePurchaseRequest", example = """
        {"cardToken":"card_ada_orquidea","amount":"45.00","label":"coffee","merchantId":1}
        """)
public record CreatePurchaseRequest(
        @NotBlank
        @Schema(example = "card_ada_orquidea", description = "Public card token")
        String cardToken,
        @NotNull
        @Schema(example = "45.00", type = "string", description = "Purchase amount in MXN")
        BigDecimal amount,
        @Schema(example = "coffee", description = "Optional payment label")
        String label,
        @Schema(example = "1", description = "Optional merchant id; defaults to Café Orquídea")
        Integer merchantId
) {
}
