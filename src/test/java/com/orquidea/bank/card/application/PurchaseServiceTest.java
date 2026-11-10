package com.orquidea.bank.card.application;

import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import com.orquidea.bank.card.api.dto.CreatePurchaseRequest;
import com.orquidea.bank.card.api.dto.PurchaseTrailResponse;
import com.orquidea.bank.card.domain.PurchaseStatus;
import com.orquidea.bank.card.infrastructure.PurchaseRepository;
import com.orquidea.bank.ledger.application.BalanceCalculator;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.merchant.domain.Merchant;
import com.orquidea.bank.merchant.infrastructure.MerchantRepository;
import com.orquidea.bank.shared.domain.ApiException;
import com.orquidea.bank.shared.infrastructure.BankDataFixture;
import com.orquidea.bank.shared.infrastructure.seed.ChartOfAccountsSeeder;
import com.orquidea.bank.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PurchaseServiceTest {

    @Autowired
    PurchaseService purchaseService;

    @Autowired
    BankDataFixture fixture;

    @Autowired
    PurchaseRepository purchases;

    @Autowired
    AccountRepository accounts;

    @Autowired
    MerchantRepository merchants;

    @Autowired
    BalanceCalculator balances;

    @Autowired
    AuditEventRepository auditEvents;

    @Autowired
    TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
        fixture.seed("trc_purchase_seed");
    }

    @Test
    void purchaseWalkPostsAdaAndPaysCafe() {
        PurchaseTrailResponse trail = purchaseService.purchase(
                new CreatePurchaseRequest("card_ada_orquidea", new BigDecimal("45.00"), null, null),
                "coffee-1"
        );

        assertThat(trail.status()).isEqualTo(PurchaseStatus.POSTED.name());
        assertThat(trail.steps()).hasSize(4);
        assertThat(trail.steps().get(0).stage()).isEqualTo("authorize");
        assertThat(trail.steps().get(0).purchaseStatus()).isEqualTo("AUTHORIZED");
        assertThat(trail.steps().get(0).available()).isEqualTo("955.00");
        assertThat(trail.steps().get(0).posted()).isEqualTo("1000.00");
        assertThat(trail.steps().get(1).purchaseStatus()).isEqualTo("CLEARED");
        assertThat(trail.steps().get(2).purchaseStatus()).isEqualTo("SETTLED");
        assertThat(trail.steps().get(2).available()).isEqualTo("955.00");
        assertThat(trail.steps().get(2).posted()).isEqualTo("955.00");
        assertThat(trail.steps().get(3).purchaseStatus()).isEqualTo("POSTED");

        var ada = accounts.findByCode(ChartOfAccountsSeeder.ADA_DEPOSIT_CODE).orElseThrow();
        var cafe = accounts.findByCode(ChartOfAccountsSeeder.CAFE_DEPOSIT_CODE).orElseThrow();
        var suspense = accounts.findByCode(ChartOfAccountsSeeder.SUSPENSE_CODE).orElseThrow();
        var income = accounts.findByCode(ChartOfAccountsSeeder.INCOME_CODE).orElseThrow();

        assertThat(balances.posted(ada)).isEqualByComparingTo("955.00");
        assertThat(balances.available(ada)).isEqualByComparingTo("955.00");
        assertThat(balances.posted(cafe)).isEqualByComparingTo("43.50");
        assertThat(balances.posted(suspense)).isEqualByComparingTo("0.00");
        assertThat(balances.posted(income)).isEqualByComparingTo("1.50");

        assertThat(purchases.count()).isEqualTo(1);
        long purchaseAudits = auditEvents.findByTraceIdOrderByIdAsc(trail.traceId()).stream()
                .filter(e -> e.getAction().name().startsWith("PURCHASE_"))
                .count();
        assertThat(purchaseAudits).isGreaterThanOrEqualTo(4);
    }

    @Test
    void purchaseWithLabelPaysSelectedMerchant() {
        Merchant other = merchants.findAll().stream()
                .filter(m -> !m.getName().contains("Café"))
                .findFirst()
                .orElseThrow();

        PurchaseTrailResponse trail = purchaseService.purchase(
                new CreatePurchaseRequest(
                        "card_ada_orquidea",
                        new BigDecimal("40.00"),
                        "mug",
                        other.getId()
                ),
                "mug-1"
        );

        assertThat(trail.status()).isEqualTo(PurchaseStatus.POSTED.name());
        var purchase = purchases.findAll().get(0);
        assertThat(purchase.getLabel()).isEqualTo("mug");
        assertThat(purchase.getMerchantId()).isEqualTo(other.getId());

        var ada = accounts.findByCode(ChartOfAccountsSeeder.ADA_DEPOSIT_CODE).orElseThrow();
        var cafe = accounts.findByCode(ChartOfAccountsSeeder.CAFE_DEPOSIT_CODE).orElseThrow();
        var merchantAccount = accounts.findById(other.getDepositAccountId()).orElseThrow();

        assertThat(balances.posted(ada)).isEqualByComparingTo("960.00");
        assertThat(balances.posted(cafe)).isEqualByComparingTo("0.00");
        assertThat(balances.posted(merchantAccount)).isEqualByComparingTo("38.50");
    }

    @Test
    void unknownCardIsNotFound() {
        assertThatThrownBy(() -> purchaseService.purchase(
                new CreatePurchaseRequest("card_missing", new BigDecimal("10.00"), null, null),
                null
        ))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("card_not_found");
        assertThat(purchases.count()).isZero();
    }

    @Test
    void insufficientAvailableIsUnprocessable() {
        assertThatThrownBy(() -> purchaseService.purchase(
                new CreatePurchaseRequest("card_ada_orquidea", new BigDecimal("1000.01"), null, null),
                null
        ))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("insufficient_available");
        assertThat(purchases.count()).isZero();
    }

    @Test
    void idempotencyKeyReplaysOriginalTrail() {
        CreatePurchaseRequest request = new CreatePurchaseRequest("card_ada_orquidea", new BigDecimal("45.00"), null, null);
        PurchaseTrailResponse first = purchaseService.purchase(request, "coffee-replay");
        PurchaseTrailResponse second = purchaseService.purchase(request, "coffee-replay");

        assertThat(second.purchaseId()).isEqualTo(first.purchaseId());
        assertThat(second.traceId()).isEqualTo(first.traceId());
        assertThat(second.steps()).isEqualTo(first.steps());
        assertThat(purchases.count()).isEqualTo(1);
    }

    @Test
    void getPurchaseReturnsDetail() {
        PurchaseTrailResponse trail = purchaseService.purchase(
                new CreatePurchaseRequest("card_ada_orquidea", new BigDecimal("45.00"), null, null),
                "coffee-detail"
        );
        var detail = purchaseService.getPurchase(trail.purchaseId());
        assertThat(detail.purchaseId()).isEqualTo(trail.purchaseId());
        assertThat(detail.status()).isEqualTo("POSTED");
        assertThat(detail.hold()).isNotNull();
        assertThat(detail.hold().status()).isEqualTo("RELEASED");
        assertThat(detail.clearingItem()).isNotNull();
        assertThat(detail.settlement()).isNotNull();
        assertThat(detail.settlement().feeAmount()).isEqualTo("1.50");
    }
}
