package com.orquidea.bank.card.api.dto;

public record PurchaseDetailResponse(
        String purchaseId,
        String traceId,
        String status,
        String amount,
        String cardToken,
        HoldResponse hold,
        ClearingItemResponse clearingItem,
        SettlementResponse settlement
) {
    public record HoldResponse(
            Integer id,
            String amount,
            String status,
            String releasedAt
    ) {
    }

    public record ClearingItemResponse(
            Integer id,
            String amount
    ) {
    }

    public record SettlementResponse(
            Integer id,
            String amount,
            String feeAmount
    ) {
    }
}
