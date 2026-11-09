package com.orquidea.bank.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "clearing_item")
public class ClearingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "purchase_id", nullable = false, unique = true)
    private Integer purchaseId;

    @Column(name = "authorization_hold_id", nullable = false, unique = true)
    private Integer authorizationHoldId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private BigDecimal amount;

    public Integer getId() {
        return id;
    }

    public Integer getPurchaseId() {
        return purchaseId;
    }

    public ClearingItem setPurchaseId(Integer purchaseId) {
        this.purchaseId = purchaseId;
        return this;
    }

    public Integer getAuthorizationHoldId() {
        return authorizationHoldId;
    }

    public ClearingItem setAuthorizationHoldId(Integer authorizationHoldId) {
        this.authorizationHoldId = authorizationHoldId;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ClearingItem setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }
}
