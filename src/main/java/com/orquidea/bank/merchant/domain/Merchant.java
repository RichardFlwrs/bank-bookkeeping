package com.orquidea.bank.merchant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "merchant")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 160)
    private String name = "";

    @Column(name = "deposit_account_id", nullable = false)
    private Integer depositAccountId;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Merchant setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getDepositAccountId() {
        return depositAccountId;
    }

    public Merchant setDepositAccountId(Integer depositAccountId) {
        this.depositAccountId = depositAccountId;
        return this;
    }
}
