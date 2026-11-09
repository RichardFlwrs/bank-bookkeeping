package com.orquidea.bank.audit.application;

import com.orquidea.bank.audit.api.dto.AuditEventResponse;
import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.audit.domain.AuditEvent;
import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditRecorder {

    private final AuditEventRepository auditEvents;

    public AuditRecorder(AuditEventRepository auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Transactional
    public void record(
            String actor,
            AuditAction action,
            String aggregateType,
            String aggregateId,
            String purchaseId,
            String traceId,
            String beforeJson,
            String afterJson
    ) {
        AuditEvent event = new AuditEvent()
                .setOccurredAt(Instant.now())
                .setActor(actor)
                .setAction(action)
                .setAggregateType(aggregateType)
                .setAggregateId(aggregateId)
                .setPurchaseId(purchaseId)
                .setTraceId(traceId)
                .setBeforeJson(beforeJson)
                .setAfterJson(afterJson);
        auditEvents.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> list(String traceId, String purchaseId, String actor) {
        List<AuditEvent> events;
        if (traceId != null && !traceId.isBlank()) {
            events = auditEvents.findByTraceIdOrderByIdAsc(traceId);
        } else if (purchaseId != null && !purchaseId.isBlank()) {
            events = auditEvents.findByPurchaseIdOrderByIdAsc(purchaseId);
        } else if (actor != null && !actor.isBlank()) {
            events = auditEvents.findByActorOrderByIdAsc(actor);
        } else {
            events = auditEvents.findAll(Sort.by(Sort.Direction.ASC, "id"));
        }
        return events.stream().map(this::toResponse).toList();
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getOccurredAt() == null ? null : event.getOccurredAt().toString(),
                event.getActor(),
                event.getAction() == null ? null : event.getAction().name(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getPurchaseId(),
                event.getTraceId(),
                event.getBeforeJson(),
                event.getAfterJson()
        );
    }
}
