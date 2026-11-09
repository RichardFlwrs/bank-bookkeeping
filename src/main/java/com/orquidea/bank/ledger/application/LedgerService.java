package com.orquidea.bank.ledger.application;

import com.orquidea.bank.ledger.domain.JournalEntry;
import com.orquidea.bank.ledger.infrastructure.JournalEntryRepository;
import com.orquidea.bank.shared.domain.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LedgerService {

    private final JournalEntryRepository journalEntries;

    public LedgerService(JournalEntryRepository journalEntries) {
        this.journalEntries = journalEntries;
    }

    @Transactional
    public JournalEntry postBalancedEntry(JournalEntry entry) {
        if (entry == null || !entry.isBalanced()) {
            throw ApiException.unprocessable("unbalanced_journal_entry");
        }
        if (entry.getPostedAt() == null) {
            entry.setPostedAt(Instant.now());
        }
        return journalEntries.save(entry);
    }
}
