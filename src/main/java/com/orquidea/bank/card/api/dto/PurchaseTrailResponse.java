package com.orquidea.bank.card.api.dto;

import java.util.List;

public record PurchaseTrailResponse(
        String purchaseId,
        String traceId,
        String status,
        List<PurchaseStepResponse> steps
) {
}
