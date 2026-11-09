package com.orquidea.bank.audit.application;

import com.orquidea.bank.audit.api.dto.AuditEventResponse;
import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import com.orquidea.bank.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuditRecorderTest {

    @Autowired
    AuditRecorder auditRecorder;

    @Autowired
    AuditEventRepository auditEvents;

    @Autowired
    TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
    }

    @Test
    void recordPersistsAppendOnlyEvent() {
        auditRecorder.record(
                "seeder:chart-of-accounts",
                AuditAction.SEED_ACCOUNT,
                "Account",
                "1",
                null,
                "trc_seed_1",
                null,
                "{\"code\":\"1000\"}"
        );

        assertThat(auditEvents.count()).isEqualTo(1);
        var saved = auditEvents.findAll().get(0);
        assertThat(saved.getActor()).isEqualTo("seeder:chart-of-accounts");
        assertThat(saved.getAction()).isEqualTo(AuditAction.SEED_ACCOUNT);
        assertThat(saved.getAggregateType()).isEqualTo("Account");
        assertThat(saved.getAggregateId()).isEqualTo("1");
        assertThat(saved.getPurchaseId()).isNull();
        assertThat(saved.getTraceId()).isEqualTo("trc_seed_1");
        assertThat(saved.getBeforeJson()).isNull();
        assertThat(saved.getAfterJson()).isEqualTo("{\"code\":\"1000\"}");
        assertThat(saved.getOccurredAt()).isNotNull();
    }

    @Test
    void listFiltersByTraceIdPurchaseIdAndActor() {
        auditRecorder.record("purchase", AuditAction.PURCHASE_AUTHORIZED, "Purchase", "pur_1", "pur_1", "trc_a", null, "{}");
        auditRecorder.record("purchase", AuditAction.PURCHASE_CLEARED, "Purchase", "pur_1", "pur_1", "trc_a", null, "{}");
        auditRecorder.record("seeder:card", AuditAction.SEED_CARD, "Card", "1", null, "trc_b", null, "{}");

        List<AuditEventResponse> byTrace = auditRecorder.list("trc_a", null, null);
        assertThat(byTrace).hasSize(2);
        assertThat(byTrace).allMatch(e -> "trc_a".equals(e.traceId()));

        List<AuditEventResponse> byPurchase = auditRecorder.list(null, "pur_1", null);
        assertThat(byPurchase).hasSize(2);

        List<AuditEventResponse> byActor = auditRecorder.list(null, null, "seeder:card");
        assertThat(byActor).hasSize(1);
        assertThat(byActor.get(0).action()).isEqualTo("SEED_CARD");
    }
}
