package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.application.AuditRecorder;
import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.ledger.application.LedgerService;
import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.ledger.domain.EntrySide;
import com.orquidea.bank.ledger.domain.JournalEntry;
import com.orquidea.bank.ledger.domain.JournalLine;
import com.orquidea.bank.shared.infrastructure.BankSeedProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpeningDepositSeeder {

    public static final String ACTOR = "seeder:opening-deposit";

    private final LedgerService ledgerService;
    private final AuditRecorder auditRecorder;
    private final BankSeedProperties properties;

    public OpeningDepositSeeder(
            LedgerService ledgerService,
            AuditRecorder auditRecorder,
            BankSeedProperties properties
    ) {
        this.ledgerService = ledgerService;
        this.auditRecorder = auditRecorder;
        this.properties = properties;
    }

    public List<JournalEntry> seed(String traceId, Account cash, List<Account> customerDeposits) {
        BigDecimal amount = properties.getOpeningDeposit();
        Instant postedAt = Instant.parse("2026-01-01T00:00:00Z");
        List<JournalEntry> entries = new ArrayList<>();

        for (Account deposit : customerDeposits) {
            JournalEntry entry = new JournalEntry().setPostedAt(postedAt);
            entry.addLine(new JournalLine().setAccount(cash).setSide(EntrySide.DEBIT).setAmount(amount));
            entry.addLine(new JournalLine().setAccount(deposit).setSide(EntrySide.CREDIT).setAmount(amount));
            JournalEntry saved = ledgerService.postBalancedEntry(entry);
            auditRecorder.record(
                    ACTOR,
                    AuditAction.SEED_DEPOSIT,
                    "JournalEntry",
                    String.valueOf(saved.getId()),
                    null,
                    traceId,
                    null,
                    SeedJson.of(
                            "id", saved.getId(),
                            "cashCode", cash.getCode(),
                            "depositCode", deposit.getCode(),
                            "amount", amount.toPlainString()
                    )
            );
            entries.add(saved);
        }
        return List.copyOf(entries);
    }
}
