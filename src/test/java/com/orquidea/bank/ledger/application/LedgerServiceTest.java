package com.orquidea.bank.ledger.application;

import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.ledger.domain.AccountType;
import com.orquidea.bank.ledger.domain.EntrySide;
import com.orquidea.bank.ledger.domain.JournalEntry;
import com.orquidea.bank.ledger.domain.JournalLine;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.ledger.infrastructure.JournalEntryRepository;
import com.orquidea.bank.shared.domain.ApiException;
import com.orquidea.bank.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class LedgerServiceTest {

    @Autowired
    LedgerService ledgerService;

    @Autowired
    AccountRepository accounts;

    @Autowired
    JournalEntryRepository journalEntries;

    @Autowired
    TestFixtures fixtures;

    private Account cash;
    private Account deposits;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
        cash = accounts.save(new Account().setCode("1000").setName("Cash").setType(AccountType.ASSET));
        deposits = accounts.save(new Account().setCode("2100").setName("Customer Deposits").setType(AccountType.LIABILITY));
    }

    @Test
    void rejectsUnbalancedEntry() {
        JournalEntry entry = new JournalEntry().setPostedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entry.addLine(new JournalLine().setAccount(cash).setSide(EntrySide.DEBIT).setAmount(new BigDecimal("1000.00")));
        entry.addLine(new JournalLine().setAccount(deposits).setSide(EntrySide.CREDIT).setAmount(new BigDecimal("999.00")));

        assertThatThrownBy(() -> ledgerService.postBalancedEntry(entry))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("unbalanced_journal_entry");
        assertThat(journalEntries.count()).isZero();
    }

    @Test
    void postsBalancedEntryWithLines() {
        JournalEntry entry = new JournalEntry().setPostedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entry.addLine(new JournalLine().setAccount(cash).setSide(EntrySide.DEBIT).setAmount(new BigDecimal("1000.00")));
        entry.addLine(new JournalLine().setAccount(deposits).setSide(EntrySide.CREDIT).setAmount(new BigDecimal("1000.00")));

        JournalEntry saved = ledgerService.postBalancedEntry(entry);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLines()).hasSize(2);
        assertThat(saved.isBalanced()).isTrue();
        assertThat(journalEntries.count()).isEqualTo(1);
        assertThat(journalEntries.findById(saved.getId())).isPresent();
    }
}
