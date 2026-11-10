package com.orquidea.bank.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "settlement")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "purchase_id", nullable = false, unique = true)
    private Integer purchaseId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    public Integer getId() {
        return id;
    }

    public Integer getPurchaseId() {
        return purchaseId;
    }

    public Settlement setPurchaseId(Integer purchaseId) {
        this.purchaseId = purchaseId;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Settlement setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public Settlement setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
        return this;
    }
}
