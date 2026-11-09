package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import com.orquidea.bank.merchant.infrastructure.MerchantRepository;
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
class MerchantSeederTest {

    @Autowired
    ChartOfAccountsSeeder chartSeeder;

    @Autowired
    MerchantSeeder merchantSeeder;

    @Autowired
    MerchantRepository merchants;

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
    void seedsCafeFirst() {
        SeedChart chart = chartSeeder.seed("trc_merchants");
        var seeded = merchantSeeder.seed("trc_merchants", chart.merchantDeposits());

        assertThat(seeded).hasSize(properties.getMerchants());
        assertThat(merchants.count()).isEqualTo(properties.getMerchants());
        assertThat(seeded.get(0).getName()).isEqualTo(MerchantSeeder.CAFE_NAME);

        long audits = auditEvents.findByTraceIdOrderByIdAsc("trc_merchants").stream()
                .filter(e -> e.getAction() == AuditAction.SEED_MERCHANT)
                .count();
        assertThat(audits).isEqualTo(properties.getMerchants());
    }
}
