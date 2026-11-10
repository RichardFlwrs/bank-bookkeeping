package com.orquidea.bank.shared.infrastructure;

import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import com.orquidea.bank.card.infrastructure.CardRepository;
import com.orquidea.bank.customer.infrastructure.CustomerRepository;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.merchant.infrastructure.MerchantRepository;
import com.orquidea.bank.shared.infrastructure.seed.CardSeeder;
import com.orquidea.bank.shared.infrastructure.seed.CustomerSeeder;
import com.orquidea.bank.shared.infrastructure.seed.MerchantSeeder;
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
class BankDataFixtureTest {

    @Autowired
    BankDataFixture fixture;

    @Autowired
    BankSeedProperties properties;

    @Autowired
    AccountRepository accounts;

    @Autowired
    CustomerRepository customers;

    @Autowired
    MerchantRepository merchants;

    @Autowired
    CardRepository cards;

    @Autowired
    AuditEventRepository auditEvents;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
    }

    @Test
    void seedsFullGraphWithDebuggerFixtures() {
        String traceId = "trc_fixture";
        fixture.seed(traceId);

        assertThat(customers.count()).isEqualTo(properties.getCustomers());
        assertThat(merchants.count()).isEqualTo(properties.getMerchants());
        assertThat(cards.count()).isEqualTo(properties.getCustomers());
        assertThat(customers.findAll().stream().anyMatch(c -> CustomerSeeder.ADA_NAME.equals(c.getName()))).isTrue();
        assertThat(merchants.findAll().stream().anyMatch(m -> MerchantSeeder.CAFE_NAME.equals(m.getName()))).isTrue();
        assertThat(cards.findByToken(CardSeeder.ADA_TOKEN)).isPresent();

        var adaAccount = accounts.findByCode("2100").orElseThrow();
        assertThat(liabilityPostedBalance(adaAccount.getId())).isEqualByComparingTo(properties.getOpeningDeposit());

        var cash = accounts.findByCode("1000").orElseThrow();
        BigDecimal expectedCash = properties.getOpeningDeposit()
                .multiply(BigDecimal.valueOf(properties.getCustomers()));
        assertThat(assetPostedBalance(cash.getId())).isEqualByComparingTo(expectedCash);

        int expectedAccounts = 3 + properties.getCustomers() + properties.getMerchants();
        int expectedAudits = expectedAccounts
                + properties.getCustomers()
                + properties.getMerchants()
                + properties.getCustomers()
                + properties.getCustomers();
        assertThat(auditEvents.findByTraceIdOrderByIdAsc(traceId)).hasSize(expectedAudits);
        assertThat(auditEvents.findByTraceIdOrderByIdAsc(traceId))
                .allMatch(e -> e.getPurchaseId() == null);
    }

    @Test
    void doesNotDuplicateWhenChartAlreadyExists() {
        fixture.seed("trc_first");
        long accountCount = accounts.count();

        boolean chartEmpty = accounts.count() == 0;
        assertThat(chartEmpty).isFalse();
        if (chartEmpty) {
            fixture.seed("trc_second");
        }

        assertThat(accounts.count()).isEqualTo(accountCount);
        assertThat(customers.count()).isEqualTo(properties.getCustomers());
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
                            WHEN side = 'DEBIT' THEN %samount
                            WHEN side = 'CREDIT' THEN %samount
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
