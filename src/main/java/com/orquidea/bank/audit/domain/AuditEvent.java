package com.orquidea.bank.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "TEXT")
    private Instant occurredAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String actor = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "TEXT")
    private AuditAction action;

    @Column(name = "aggregate_type", nullable = false, columnDefinition = "TEXT")
    private String aggregateType = "";

    @Column(name = "aggregate_id", nullable = false, columnDefinition = "TEXT")
    private String aggregateId = "";

    @Column(name = "purchase_id", columnDefinition = "TEXT")
    private String purchaseId;

    @Column(name = "trace_id", nullable = false, columnDefinition = "TEXT")
    private String traceId = "";

    @Column(name = "before_json", columnDefinition = "TEXT")
    private String beforeJson;

    @Column(name = "after_json", nullable = false, columnDefinition = "TEXT")
    private String afterJson = "";

    public Integer getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public AuditEvent setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
        return this;
    }

    public String getActor() {
        return actor;
    }

    public AuditEvent setActor(String actor) {
        this.actor = actor;
        return this;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditEvent setAction(AuditAction action) {
        this.action = action;
        return this;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public AuditEvent setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
        return this;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public AuditEvent setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
        return this;
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public AuditEvent setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
        return this;
    }

    public String getTraceId() {
        return traceId;
    }

    public AuditEvent setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public AuditEvent setBeforeJson(String beforeJson) {
        this.beforeJson = beforeJson;
        return this;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public AuditEvent setAfterJson(String afterJson) {
        this.afterJson = afterJson;
        return this;
    }
}
