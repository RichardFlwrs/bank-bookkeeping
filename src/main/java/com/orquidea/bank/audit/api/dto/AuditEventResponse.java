package com.orquidea.bank.audit.api.dto;

public record AuditEventResponse(
        Integer id,
        String occurredAt,
        String actor,
        String action,
        String aggregateType,
        String aggregateId,
        String purchaseId,
        String traceId,
        String beforeJson,
        String afterJson
) {
}
