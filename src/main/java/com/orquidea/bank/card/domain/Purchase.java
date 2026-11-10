package com.orquidea.bank.card.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId = "";

    @Column(name = "trace_id", nullable = false, unique = true, length = 64)
    private String traceId = "";

    @Column(name = "idempotency_key", unique = true, length = 120)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private BigDecimal amount;

    @Column(length = 160)
    private String label;

    @Column(name = "merchant_id")
    private Integer merchantId;

    @OneToOne(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AuthorizationHold hold;

    public Integer getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Purchase setPublicId(String publicId) {
        this.publicId = publicId;
        return this;
    }

    public String getTraceId() {
        return traceId;
    }

    public Purchase setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Purchase setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public Card getCard() {
        return card;
    }

    public Purchase setCard(Card card) {
        this.card = card;
        return this;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public Purchase setStatus(PurchaseStatus status) {
        this.status = status;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Purchase setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public Purchase setLabel(String label) {
        this.label = label;
        return this;
    }

    public Integer getMerchantId() {
        return merchantId;
    }

    public Purchase setMerchantId(Integer merchantId) {
        this.merchantId = merchantId;
        return this;
    }

    public AuthorizationHold getHold() {
        return hold;
    }

    public Purchase setHold(AuthorizationHold hold) {
        this.hold = hold;
        if (hold != null) {
            hold.setPurchase(this);
        }
        return this;
    }
}
