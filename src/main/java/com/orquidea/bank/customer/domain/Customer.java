package com.orquidea.bank.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 160)
    private String name = "";

    @Column(name = "deposit_account_id", nullable = false)
    private Integer depositAccountId;

    @Column(length = 64)
    private String username;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Customer setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getDepositAccountId() {
        return depositAccountId;
    }

    public Customer setDepositAccountId(Integer depositAccountId) {
        this.depositAccountId = depositAccountId;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public Customer setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Customer setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }
}
