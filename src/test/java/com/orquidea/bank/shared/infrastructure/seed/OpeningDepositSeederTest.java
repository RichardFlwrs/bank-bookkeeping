package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import com.orquidea.bank.ledger.infrastructure.JournalEntryRepository;
import com.orquidea.bank.shared.infrastructure.BankSeedProperties;
import com.orquidea.bank.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OpeningDepositSeederTest {

    @Autowired
    ChartOfAccountsSeeder chartSeeder;

    @Autowired
    OpeningDepositSeeder openingDepositSeeder;

    @Autowired
    JournalEntryRepository journalEntries;

    @Autowired
    AuditEventRepository auditEvents;

    @Autowired
    BankSeedProperties properties;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
    }

    @Test
    void postsOneDepositPerCustomer() {
        SeedChart chart = chartSeeder.seed("trc_deposits");
        var entries = openingDepositSeeder.seed("trc_deposits", chart.cash(), chart.customerDeposits());

        assertThat(entries).hasSize(properties.getCustomers());
        assertThat(journalEntries.count()).isEqualTo(properties.getCustomers());

        BigDecimal cashPosted = assetPostedBalance(chart.cash().getId());
        BigDecimal expectedCash = properties.getOpeningDeposit()
                .multiply(BigDecimal.valueOf(properties.getCustomers()));
        assertThat(cashPosted).isEqualByComparingTo(expectedCash);

        BigDecimal adaPosted = liabilityPostedBalance(chart.customerDeposits().get(0).getId());
        assertThat(adaPosted).isEqualByComparingTo(properties.getOpeningDeposit());

        long audits = auditEvents.findByTraceIdOrderByIdAsc("trc_deposits").stream()
                .filter(e -> e.getAction() == AuditAction.SEED_DEPOSIT)
                .count();
        assertThat(audits).isEqualTo(properties.getCustomers());
    }

    private BigDecimal assetPostedBalance(Integer accountId) {
        return signedBalance(accountId, true);
    }

    private BigDecimal liabilityPostedBalance(Integer accountId) {
        return signedBalance(accountId, false);
    }

    private BigDecimal signedBalance(Integer accountId, boolean asset) {
        String debitSign = asset ? "+" : "-";
        String creditSign = asset ? "-" : "+";
        return jdbc.queryForObject(
                """
                        SELECT COALESCE(SUM(CASE
                            WHEN side = 'DEBIT' THEN %sCAST(amount AS REAL)
                            WHEN side = 'CREDIT' THEN %sCAST(amount AS REAL)
                            ELSE 0
                        END), 0)
                        FROM journal_line
                        WHERE account_id = ?
                        """.formatted(debitSign, creditSign),
                BigDecimal.class,
                accountId
        );
    }
}
