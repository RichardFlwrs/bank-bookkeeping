package com.orquidea.bank.card.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "card")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 64)
    private String token = "";

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    public Integer getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Card setToken(String token) {
        this.token = token;
        return this;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public Card setCustomerId(Integer customerId) {
        this.customerId = customerId;
        return this;
    }
}
