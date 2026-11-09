package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.audit.domain.AuditEvent;
import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import com.orquidea.bank.customer.domain.Customer;
import com.orquidea.bank.customer.infrastructure.CustomerRepository;
import com.orquidea.bank.shared.infrastructure.BankSeedProperties;
import com.orquidea.bank.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    PasswordEncoder passwordEncoder;

    @Autowired
    TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
    }

    @Test
    void seedsAdaFirstWithLoginAndMatchesDepositAccounts() {
        SeedChart chart = chartSeeder.seed("trc_customers");
        var seeded = customerSeeder.seed("trc_customers", chart.customerDeposits());

        assertThat(seeded).hasSize(properties.getCustomers());
        assertThat(customers.count()).isEqualTo(properties.getCustomers());

        Customer ada = seeded.get(0);
        assertThat(ada.getName()).isEqualTo(CustomerSeeder.ADA_NAME);
        assertThat(ada.getDepositAccountId()).isEqualTo(chart.customerDeposits().get(0).getId());
        assertThat(ada.getUsername()).isEqualTo(CustomerSeeder.ADA_USERNAME);
        assertThat(passwordEncoder.matches(CustomerSeeder.DEV_PASSWORD, ada.getPasswordHash())).isTrue();

        Customer second = seeded.get(1);
        assertThat(second.getUsername()).isEqualTo(
                CustomerSeeder.usernameFor(chart.customerDeposits().get(1).getCode())
        );
        assertThat(passwordEncoder.matches(CustomerSeeder.DEV_PASSWORD, second.getPasswordHash())).isTrue();

        long audits = auditEvents.findByTraceIdOrderByIdAsc("trc_customers").stream()
                .filter(e -> e.getAction() == AuditAction.SEED_CUSTOMER)
                .count();
        assertThat(audits).isEqualTo(properties.getCustomers());

        AuditEvent adaAudit = auditEvents.findByTraceIdOrderByIdAsc("trc_customers").stream()
                .filter(e -> e.getAction() == AuditAction.SEED_CUSTOMER)
                .findFirst()
                .orElseThrow();
        assertThat(adaAudit.getAfterJson()).contains("\"username\":\"" + CustomerSeeder.ADA_USERNAME + "\"");
        assertThat(adaAudit.getAfterJson()).doesNotContain("password");
        assertThat(adaAudit.getAfterJson()).doesNotContain(ada.getPasswordHash());
    }
}
