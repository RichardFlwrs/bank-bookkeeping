package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import com.orquidea.bank.customer.infrastructure.CustomerRepository;
import com.orquidea.bank.shared.infrastructure.BankSeedProperties;
import com.orquidea.bank.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CustomerSeederTest {

    @Autowired
    ChartOfAccountsSeeder chartSeeder;

    @Autowired
    CustomerSeeder customerSeeder;

    @Autowired
    CustomerRepository customers;

    @Autowired
    AuditEventRepository auditEvents;

    @Autowired
    BankSeedProperties properties;

    @Autowired
    TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
    }

    @Test
    void seedsAdaFirstAndMatchesDepositAccounts() {
        SeedChart chart = chartSeeder.seed("trc_customers");
        var seeded = customerSeeder.seed("trc_customers", chart.customerDeposits());

        assertThat(seeded).hasSize(properties.getCustomers());
        assertThat(customers.count()).isEqualTo(properties.getCustomers());
        assertThat(seeded.get(0).getName()).isEqualTo(CustomerSeeder.ADA_NAME);
        assertThat(seeded.get(0).getDepositAccountId()).isEqualTo(chart.customerDeposits().get(0).getId());

        long audits = auditEvents.findByTraceIdOrderByIdAsc("trc_customers").stream()
                .filter(e -> e.getAction() == AuditAction.SEED_CUSTOMER)
                .count();
        assertThat(audits).isEqualTo(properties.getCustomers());
    }
}
