package com.orquidea.bank.shared.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.bank-seed")
public class BankSeedProperties {

    /**
     * Fixed RNG seed so local reloads are reproducible.
     */
    private long seed = 42L;

    private int customers = 1000;

    private int merchants = 50;

    private BigDecimal openingDeposit = new BigDecimal("1000.00");

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public int getCustomers() {
        return customers;
    }

    public void setCustomers(int customers) {
        this.customers = customers;
    }

    public int getMerchants() {
        return merchants;
    }

    public void setMerchants(int merchants) {
        this.merchants = merchants;
    }

    public BigDecimal getOpeningDeposit() {
        return openingDeposit;
    }

    public void setOpeningDeposit(BigDecimal openingDeposit) {
        this.openingDeposit = openingDeposit;
    }
}
