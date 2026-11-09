package com.orquidea.bank.shared.infrastructure;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BankSeedPropertiesTest {

    @Test
    void defaultsMatchScaledEcosystem() {
        BankSeedProperties properties = new BankSeedProperties();

        assertThat(properties.getSeed()).isEqualTo(42L);
        assertThat(properties.getCustomers()).isEqualTo(1000);
        assertThat(properties.getMerchants()).isEqualTo(50);
        assertThat(properties.getOpeningDeposit()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}
