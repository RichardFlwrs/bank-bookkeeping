package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import com.orquidea.bank.ledger.domain.AccountType;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
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
class ChartOfAccountsSeederTest {

    @Autowired
    ChartOfAccountsSeeder seeder;

    @Autowired
    AccountRepository accounts;

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
    void seedsBankAndDepositAccountsWithAudit() {
        SeedChart chart = seeder.seed("trc_chart");

        int expectedAccounts = 3 + properties.getCustomers() + properties.getMerchants();
        assertThat(accounts.count()).isEqualTo(expectedAccounts);
        assertThat(chart.cash().getCode()).isEqualTo("1000");
        assertThat(chart.cash().getType()).isEqualTo(AccountType.ASSET);
        assertThat(chart.customerDeposits()).hasSize(properties.getCustomers());
        assertThat(chart.customerDeposits().get(0).getCode()).isEqualTo("2100");
        assertThat(chart.merchantDeposits().get(0).getCode()).isEqualTo("2200");
        assertThat(chart.suspense().getCode()).isEqualTo("2300");
        assertThat(chart.income().getCode()).isEqualTo("4100");

        long accountAudits = auditEvents.findByTraceIdOrderByIdAsc("trc_chart").stream()
                .filter(e -> e.getAction() == AuditAction.SEED_ACCOUNT)
                .count();
        assertThat(accountAudits).isEqualTo(expectedAccounts);
    }
}
