package com.orquidea.bank.settlement.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orquidea.bank.audit.application.AuditRecorder;
import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.card.domain.Purchase;
import com.orquidea.bank.card.domain.PurchaseStatus;
import com.orquidea.bank.card.infrastructure.PurchaseRepository;
import com.orquidea.bank.ledger.application.LedgerService;
import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.ledger.domain.EntrySide;
import com.orquidea.bank.ledger.domain.JournalEntry;
import com.orquidea.bank.ledger.domain.JournalLine;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.merchant.domain.Merchant;
import com.orquidea.bank.merchant.infrastructure.MerchantRepository;
import com.orquidea.bank.settlement.domain.ClearingItem;
import com.orquidea.bank.settlement.domain.Settlement;
import com.orquidea.bank.settlement.infrastructure.ClearingItemRepository;
import com.orquidea.bank.settlement.infrastructure.SettlementRepository;
import com.orquidea.bank.shared.domain.ApiException;
import com.orquidea.bank.shared.infrastructure.seed.ChartOfAccountsSeeder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SettlementService {

    private final ClearingItemRepository clearingItems;
    private final SettlementRepository settlements;
    private final PurchaseRepository purchases;
    private final AccountRepository accounts;
    private final MerchantRepository merchants;
    private final LedgerService ledgerService;
    private final AuditRecorder auditRecorder;
    private final ObjectMapper objectMapper;

    public SettlementService(
            ClearingItemRepository clearingItems,
            SettlementRepository settlements,
            PurchaseRepository purchases,
            AccountRepository accounts,
            MerchantRepository merchants,
            LedgerService ledgerService,
            AuditRecorder auditRecorder,
            ObjectMapper objectMapper
    ) {
        this.clearingItems = clearingItems;
        this.settlements = settlements;
        this.purchases = purchases;
        this.accounts = accounts;
        this.merchants = merchants;
        this.ledgerService = ledgerService;
        this.auditRecorder = auditRecorder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ClearingItem clear(Purchase purchase) {
        Purchase managed = purchases.findById(purchase.getId()).orElseThrow();
        ClearingItem item = clearingItems.save(new ClearingItem()
                .setPurchaseId(managed.getId())
                .setAuthorizationHoldId(managed.getHold().getId())
                .setAmount(managed.getAmount()));
        managed.setStatus(PurchaseStatus.CLEARED);
        purchases.save(managed);

        auditRecorder.record(
                "purchase",
                AuditAction.PURCHASE_CLEARED,
                "ClearingItem",
                String.valueOf(item.getId()),
                managed.getPublicId(),
                managed.getTraceId(),
                null,
                json(Map.of(
                        "id", item.getId(),
                        "purchaseId", item.getPurchaseId(),
                        "authorizationHoldId", item.getAuthorizationHoldId(),
                        "amount", item.getAmount().toPlainString()
                ))
        );
        return item;
    }

    @Transactional
    public Settlement settleMerchant(Purchase purchase, BigDecimal fee) {
        Purchase managed = purchases.findById(purchase.getId()).orElseThrow();
        Account suspense = accounts.findByCode(ChartOfAccountsSeeder.SUSPENSE_CODE).orElseThrow();
        Merchant merchant = resolveMerchant(managed.getMerchantId());
        Account merchantAccount = accounts.findById(merchant.getDepositAccountId())
                .orElseThrow(() -> ApiException.notFound("account_not_found"));
        Account income = accounts.findByCode(ChartOfAccountsSeeder.INCOME_CODE).orElseThrow();

        BigDecimal amount = managed.getAmount();
        BigDecimal feeAmount = fee == null ? BigDecimal.ZERO : fee;
        BigDecimal merchantAmount = amount.subtract(feeAmount);

        JournalEntry entry = new JournalEntry().setPostedAt(Instant.now());
        entry.addLine(new JournalLine().setAccount(suspense).setSide(EntrySide.DEBIT).setAmount(amount));
        entry.addLine(new JournalLine().setAccount(merchantAccount).setSide(EntrySide.CREDIT).setAmount(merchantAmount));
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            entry.addLine(new JournalLine().setAccount(income).setSide(EntrySide.CREDIT).setAmount(feeAmount));
        }
        JournalEntry savedEntry = ledgerService.postBalancedEntry(entry);

        Settlement settlement = settlements.save(new Settlement()
                .setPurchaseId(managed.getId())
                .setAmount(merchantAmount)
                .setFeeAmount(feeAmount));
        managed.setStatus(PurchaseStatus.POSTED);
        purchases.save(managed);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("id", settlement.getId());
        after.put("purchaseId", settlement.getPurchaseId());
        after.put("amount", settlement.getAmount().toPlainString());
        after.put("feeAmount", settlement.getFeeAmount().toPlainString());
        after.put("journalEntryId", savedEntry.getId());
        after.put("merchantId", merchant.getId());
        after.put("merchantAccount", merchantAccount.getCode());
        if (managed.getLabel() != null) {
            after.put("label", managed.getLabel());
        }

        auditRecorder.record(
                "purchase",
                AuditAction.PURCHASE_SETTLED,
                "Settlement",
                String.valueOf(settlement.getId()),
                managed.getPublicId(),
                managed.getTraceId(),
                null,
                json(after)
        );
        return settlement;
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

    private String json(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(values));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
