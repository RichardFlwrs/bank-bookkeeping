package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import com.orquidea.bank.card.infrastructure.CardRepository;
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
class CardSeederTest {

    @Autowired
    ChartOfAccountsSeeder chartSeeder;

    @Autowired
    CustomerSeeder customerSeeder;

    @Autowired
    CardSeeder cardSeeder;

    @Autowired
    CardRepository cards;

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
    void seedsOneCardPerCustomerWithAdaToken() {
        SeedChart chart = chartSeeder.seed("trc_cards");
        var customers = customerSeeder.seed("trc_cards", chart.customerDeposits());
        var seeded = cardSeeder.seed("trc_cards", customers);

        assertThat(seeded).hasSize(properties.getCustomers());
        assertThat(cards.count()).isEqualTo(properties.getCustomers());
        assertThat(seeded.get(0).getToken()).isEqualTo(CardSeeder.ADA_TOKEN);
        assertThat(cards.findByToken(CardSeeder.ADA_TOKEN)).isPresent();

        long audits = auditEvents.findByTraceIdOrderByIdAsc("trc_cards").stream()
                .filter(e -> e.getAction() == AuditAction.SEED_CARD)
                .count();
        assertThat(audits).isEqualTo(properties.getCustomers());
    }
}
