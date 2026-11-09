package com.orquidea.bank.card.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "authorization_hold")
public class AuthorizationHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false, unique = true)
    private Purchase purchase;

    @Column(nullable = false, columnDefinition = "TEXT")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldStatus status;

    @Column(name = "released_at", columnDefinition = "TEXT")
    private Instant releasedAt;

    public Integer getId() {
        return id;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public AuthorizationHold setPurchase(Purchase purchase) {
        this.purchase = purchase;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public AuthorizationHold setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public AuthorizationHold setStatus(HoldStatus status) {
        this.status = status;
        return this;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public AuthorizationHold setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
        return this;
    }
}
