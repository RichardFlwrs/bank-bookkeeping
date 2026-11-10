package com.orquidea.bank.card.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orquidea.bank.audit.application.AuditRecorder;
import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.card.api.dto.CreatePurchaseRequest;
import com.orquidea.bank.card.api.dto.PurchaseDetailResponse;
import com.orquidea.bank.card.api.dto.PurchaseStepResponse;
import com.orquidea.bank.card.api.dto.PurchaseTrailResponse;
import com.orquidea.bank.card.domain.AuthorizationHold;
import com.orquidea.bank.card.domain.Card;
import com.orquidea.bank.card.domain.HoldStatus;
import com.orquidea.bank.card.domain.Purchase;
import com.orquidea.bank.card.domain.PurchaseStatus;
import com.orquidea.bank.card.infrastructure.CardRepository;
import com.orquidea.bank.card.infrastructure.PurchaseRepository;
import com.orquidea.bank.customer.domain.Customer;
import com.orquidea.bank.customer.infrastructure.CustomerRepository;
import com.orquidea.bank.ledger.application.BalanceCalculator;
import com.orquidea.bank.ledger.application.LedgerService;
import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.ledger.domain.EntrySide;
import com.orquidea.bank.ledger.domain.JournalEntry;
import com.orquidea.bank.ledger.domain.JournalLine;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.merchant.domain.Merchant;
import com.orquidea.bank.merchant.infrastructure.MerchantRepository;
import com.orquidea.bank.settlement.application.SettlementService;
import com.orquidea.bank.settlement.domain.ClearingItem;
import com.orquidea.bank.settlement.domain.Settlement;
import com.orquidea.bank.settlement.infrastructure.ClearingItemRepository;
import com.orquidea.bank.settlement.infrastructure.SettlementRepository;
import com.orquidea.bank.shared.domain.ApiException;
import com.orquidea.bank.shared.infrastructure.seed.ChartOfAccountsSeeder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PurchaseService {

    private final PurchaseService self;
    private final CardRepository cards;
    private final PurchaseRepository purchases;
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final MerchantRepository merchants;
    private final ClearingItemRepository clearingItems;
    private final SettlementRepository settlements;
    private final LedgerService ledgerService;
    private final SettlementService settlementService;
    private final BalanceCalculator balances;
    private final AuditRecorder auditRecorder;
    private final ObjectMapper objectMapper;

    public PurchaseService(
            @Lazy PurchaseService self,
            CardRepository cards,
            PurchaseRepository purchases,
            CustomerRepository customers,
            AccountRepository accounts,
            MerchantRepository merchants,
            ClearingItemRepository clearingItems,
            SettlementRepository settlements,
            LedgerService ledgerService,
            SettlementService settlementService,
            BalanceCalculator balances,
            AuditRecorder auditRecorder,
            ObjectMapper objectMapper
    ) {
        this.self = self;
        this.cards = cards;
        this.purchases = purchases;
        this.customers = customers;
        this.accounts = accounts;
        this.merchants = merchants;
        this.clearingItems = clearingItems;
        this.settlements = settlements;
        this.ledgerService = ledgerService;
        this.settlementService = settlementService;
        this.balances = balances;
        this.auditRecorder = auditRecorder;
        this.objectMapper = objectMapper;
    }

    public PurchaseTrailResponse purchase(CreatePurchaseRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = purchases.findDetailedByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return rebuildTrail(existing.get());
            }
        }

        Card card = cards.findByToken(request.cardToken())
                .orElseThrow(() -> ApiException.notFound("card_not_found"));
        Merchant merchant = resolveMerchant(request.merchantId());
        Account deposit = depositAccountFor(card);
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (balances.available(deposit).compareTo(amount) < 0) {
            throw ApiException.unprocessable("insufficient_available");
        }

        BigDecimal fee = PurchaseFee.forAmount(amount);
        List<PurchaseStepResponse> steps = new ArrayList<>();

        Purchase purchase = self.authorize(
                card,
                amount,
                idempotencyKey,
                blankToNull(request.label()),
                merchant.getId()
        );
        steps.add(step("authorize", purchase, deposit));

        settlementService.clear(purchase);
        purchase = purchases.findById(purchase.getId()).orElseThrow();
        steps.add(step("clear", purchase, deposit));

        self.postCustomer(purchase);
        purchase = purchases.findById(purchase.getId()).orElseThrow();
        steps.add(step("postCustomer", purchase, deposit));

        settlementService.settleMerchant(purchase, fee);
        purchase = purchases.findById(purchase.getId()).orElseThrow();
        steps.add(step("settleMerchant", purchase, deposit));

        return new PurchaseTrailResponse(
                purchase.getPublicId(),
                purchase.getTraceId(),
                purchase.getStatus().name(),
                List.copyOf(steps)
        );
    }

    @Transactional(readOnly = true)
    public PurchaseDetailResponse getPurchase(String purchaseId) {
        Purchase purchase = purchases.findDetailedByPublicId(purchaseId)
                .orElseThrow(() -> ApiException.notFound("purchase_not_found"));
        AuthorizationHold hold = purchase.getHold();
        ClearingItem clearing = clearingItems.findByPurchaseId(purchase.getId()).orElse(null);
        Settlement settlement = settlements.findByPurchaseId(purchase.getId()).orElse(null);

        return new PurchaseDetailResponse(
                purchase.getPublicId(),
                purchase.getTraceId(),
                purchase.getStatus().name(),
                money(purchase.getAmount()),
                purchase.getCard().getToken(),
                hold == null ? null : new PurchaseDetailResponse.HoldResponse(
                        hold.getId(),
                        money(hold.getAmount()),
                        hold.getStatus().name(),
                        hold.getReleasedAt() == null ? null : hold.getReleasedAt().toString()
                ),
                clearing == null ? null : new PurchaseDetailResponse.ClearingItemResponse(
                        clearing.getId(),
                        money(clearing.getAmount())
                ),
                settlement == null ? null : new PurchaseDetailResponse.SettlementResponse(
                        settlement.getId(),
                        money(settlement.getAmount()),
                        money(settlement.getFeeAmount())
                )
        );
    }

    @Transactional
    public Purchase authorize(
            Card card,
            BigDecimal amount,
            String idempotencyKey,
            String label,
            Integer merchantId
    ) {
        Purchase purchase = new Purchase()
                .setPublicId(newId("pur"))
                .setTraceId(newId("trc"))
                .setIdempotencyKey(blankToNull(idempotencyKey))
                .setCard(card)
                .setStatus(PurchaseStatus.REQUESTED)
                .setAmount(amount)
                .setLabel(label)
                .setMerchantId(merchantId);
        purchase.setHold(new AuthorizationHold()
                .setAmount(amount)
                .setStatus(HoldStatus.OPEN));
        purchase.setStatus(PurchaseStatus.AUTHORIZED);
        Purchase saved = purchases.save(purchase);

        Map<String, Object> purchaseAfter = new LinkedHashMap<>();
        purchaseAfter.put("publicId", saved.getPublicId());
        purchaseAfter.put("status", saved.getStatus().name());
        purchaseAfter.put("amount", money(saved.getAmount()));
        purchaseAfter.put("holdId", saved.getHold().getId());
        purchaseAfter.put("merchantId", saved.getMerchantId());
        if (saved.getLabel() != null) {
            purchaseAfter.put("label", saved.getLabel());
        }

        auditRecorder.record(
                "purchase",
                AuditAction.PURCHASE_AUTHORIZED,
                "Purchase",
                saved.getPublicId(),
                saved.getPublicId(),
                saved.getTraceId(),
                null,
                json(purchaseAfter)
        );
        auditRecorder.record(
                "purchase",
                AuditAction.PURCHASE_AUTHORIZED,
                "AuthorizationHold",
                String.valueOf(saved.getHold().getId()),
                saved.getPublicId(),
                saved.getTraceId(),
                null,
                json(Map.of(
                        "id", saved.getHold().getId(),
                        "amount", money(saved.getHold().getAmount()),
                        "status", saved.getHold().getStatus().name()
                ))
        );
        return saved;
    }

    @Transactional
    public void postCustomer(Purchase purchase) {
        Purchase managed = purchases.findById(purchase.getId()).orElseThrow();
        Account deposit = depositAccountFor(managed.getCard());
        Account suspense = accounts.findByCode(ChartOfAccountsSeeder.SUSPENSE_CODE).orElseThrow();

        JournalEntry entry = new JournalEntry().setPostedAt(Instant.now());
        entry.addLine(new JournalLine().setAccount(deposit).setSide(EntrySide.DEBIT).setAmount(managed.getAmount()));
        entry.addLine(new JournalLine().setAccount(suspense).setSide(EntrySide.CREDIT).setAmount(managed.getAmount()));
        JournalEntry savedEntry = ledgerService.postBalancedEntry(entry);

        AuthorizationHold hold = managed.getHold();
        String beforeHold = json(Map.of(
                "id", hold.getId(),
                "status", hold.getStatus().name(),
                "releasedAt", hold.getReleasedAt() == null ? "" : hold.getReleasedAt().toString()
        ));
        hold.setStatus(HoldStatus.RELEASED);
        hold.setReleasedAt(Instant.now());
        managed.setStatus(PurchaseStatus.SETTLED);
        purchases.save(managed);

        auditRecorder.record(
                "purchase",
                AuditAction.PURCHASE_CUSTOMER_POSTED,
                "JournalEntry",
                String.valueOf(savedEntry.getId()),
                managed.getPublicId(),
                managed.getTraceId(),
                null,
                json(Map.of(
                        "id", savedEntry.getId(),
                        "from", deposit.getCode(),
                        "to", suspense.getCode(),
                        "amount", money(managed.getAmount())
                ))
        );
        auditRecorder.record(
                "purchase",
                AuditAction.PURCHASE_CUSTOMER_POSTED,
                "AuthorizationHold",
                String.valueOf(hold.getId()),
                managed.getPublicId(),
                managed.getTraceId(),
                beforeHold,
                json(Map.of(
                        "id", hold.getId(),
                        "status", hold.getStatus().name(),
                        "releasedAt", hold.getReleasedAt().toString()
                ))
        );
    }

    private PurchaseTrailResponse rebuildTrail(Purchase purchase) {
        Account deposit = depositAccountFor(purchase.getCard());
        BigDecimal postedNow = balances.posted(deposit);
        BigDecimal postedBefore = postedNow.add(purchase.getAmount());
        String beforePosted = balances.format(postedBefore);
        String beforeAvailable = balances.format(postedBefore.subtract(purchase.getAmount()));
        String afterPosted = balances.format(postedNow);
        String afterAvailable = balances.format(balances.available(deposit));

        List<PurchaseStepResponse> steps = List.of(
                new PurchaseStepResponse("authorize", PurchaseStatus.AUTHORIZED.name(), beforeAvailable, beforePosted),
                new PurchaseStepResponse("clear", PurchaseStatus.CLEARED.name(), beforeAvailable, beforePosted),
                new PurchaseStepResponse("postCustomer", PurchaseStatus.SETTLED.name(), afterAvailable, afterPosted),
                new PurchaseStepResponse("settleMerchant", PurchaseStatus.POSTED.name(), afterAvailable, afterPosted)
        );
        return new PurchaseTrailResponse(
                purchase.getPublicId(),
                purchase.getTraceId(),
                purchase.getStatus().name(),
                steps
        );
    }

    private PurchaseStepResponse step(String stage, Purchase purchase, Account deposit) {
        return new PurchaseStepResponse(
                stage,
                purchase.getStatus().name(),
                balances.format(balances.available(deposit)),
                balances.format(balances.posted(deposit))
        );
    }

    private Merchant resolveMerchant(Integer merchantId) {
        if (merchantId != null) {
            return merchants.findById(merchantId)
                    .orElseThrow(() -> ApiException.notFound("merchant_not_found"));
        }
        Account cafe = accounts.findByCode(ChartOfAccountsSeeder.CAFE_DEPOSIT_CODE)
                .orElseThrow(() -> ApiException.notFound("account_not_found"));
        return merchants.findByDepositAccountId(cafe.getId())
                .orElseThrow(() -> ApiException.notFound("merchant_not_found"));
    }

    private Account depositAccountFor(Card card) {
        Customer customer = customers.findById(card.getCustomerId())
                .orElseThrow(() -> ApiException.notFound("customer_not_found"));
        return accounts.findById(customer.getDepositAccountId())
                .orElseThrow(() -> ApiException.notFound("account_not_found"));
    }

    private static String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String json(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(values));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
