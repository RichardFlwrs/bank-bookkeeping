package com.orquidea.bank.ledger.application;

import com.orquidea.bank.card.domain.AuthorizationHold;
import com.orquidea.bank.card.domain.Card;
import com.orquidea.bank.card.domain.HoldStatus;
import com.orquidea.bank.card.domain.Purchase;
import com.orquidea.bank.card.domain.PurchaseStatus;
import com.orquidea.bank.card.infrastructure.CardRepository;
import com.orquidea.bank.card.infrastructure.PurchaseRepository;
import com.orquidea.bank.customer.domain.Customer;
import com.orquidea.bank.customer.infrastructure.CustomerRepository;
import com.orquidea.bank.ledger.api.dto.AccountBalanceResponse;
import com.orquidea.bank.ledger.api.dto.AccountDetailResponse;
import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.ledger.domain.AccountType;
import com.orquidea.bank.ledger.domain.EntrySide;
import com.orquidea.bank.ledger.domain.JournalEntry;
import com.orquidea.bank.ledger.domain.JournalLine;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.shared.domain.ApiException;
import com.orquidea.bank.shared.infrastructure.BankDataFixture;
import com.orquidea.bank.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BalanceQueryTest {

    @Autowired
    BalanceQuery balanceQuery;

    @Autowired
    BalanceCalculator balances;

    @Autowired
    BankDataFixture fixture;

    @Autowired
    AccountRepository accounts;

    @Autowired
    CustomerRepository customers;

    @Autowired
    CardRepository cards;

    @Autowired
    PurchaseRepository purchases;

    @Autowired
    LedgerService ledgerService;

    @Autowired
    TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
        fixture.seed("trc_balance");
    }

    @Test
    void listsAccountsWithPostedAndAvailable() {
        List<AccountBalanceResponse> list = balanceQuery.listAccounts();
        assertThat(list).isNotEmpty();

        AccountBalanceResponse ada = list.stream()
                .filter(a -> "2100".equals(a.code()))
                .findFirst()
                .orElseThrow();
        assertThat(ada.posted()).isEqualTo("1000.00");
        assertThat(ada.available()).isEqualTo("1000.00");

        AccountBalanceResponse cash = list.stream()
                .filter(a -> "1000".equals(a.code()))
                .findFirst()
                .orElseThrow();
        assertThat(new BigDecimal(cash.posted())).isEqualByComparingTo("5000.00");
    }

    @Test
    void availableDropsWhenHoldIsOpen() {
        Account adaDeposit = accounts.findByCode("2100").orElseThrow();
        Customer ada = customers.findAll().stream()
                .filter(c -> adaDeposit.getId().equals(c.getDepositAccountId()))
                .findFirst()
                .orElseThrow();
        Card card = cards.findByToken("card_ada_orquidea").orElseThrow();

        Purchase purchase = purchases.save(new Purchase()
                .setPublicId("pur_hold_test")
                .setTraceId("trc_hold_test")
                .setCard(card)
                .setStatus(PurchaseStatus.AUTHORIZED)
                .setAmount(new BigDecimal("45.00")));
        purchase.setHold(new AuthorizationHold()
                .setAmount(new BigDecimal("45.00"))
                .setStatus(HoldStatus.OPEN));
        purchases.save(purchase);

        assertThat(balances.posted(adaDeposit)).isEqualByComparingTo("1000.00");
        assertThat(balances.available(adaDeposit)).isEqualByComparingTo("955.00");
        assertThat(ada.getId()).isNotNull();
    }

    @Test
    void getAccountIncludesLines() {
        AccountDetailResponse detail = balanceQuery.getAccount("2100");
        assertThat(detail.code()).isEqualTo("2100");
        assertThat(detail.posted()).isEqualTo("1000.00");
        assertThat(detail.lines()).isNotEmpty();
        assertThat(detail.lines().get(0).side()).isEqualTo("CREDIT");
    }

    @Test
    void accountLinesPutOlderRecordsLast() {
        Account ada = accounts.findByCode("2100").orElseThrow();
        Account cash = accounts.findByCode("1000").orElseThrow();
        JournalEntry later = new JournalEntry().setPostedAt(Instant.parse("2026-03-01T00:00:00Z"));
        later.addLine(new JournalLine().setAccount(ada).setSide(EntrySide.DEBIT).setAmount(new BigDecimal("5.00")));
        later.addLine(new JournalLine().setAccount(cash).setSide(EntrySide.CREDIT).setAmount(new BigDecimal("5.00")));
        JournalEntry saved = ledgerService.postBalancedEntry(later);

        AccountDetailResponse detail = balanceQuery.getAccount("2100");
        assertThat(detail.lines()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(detail.lines().get(0).journalEntryId()).isEqualTo(saved.getId());
        assertThat(detail.lines().get(0).journalEntryId())
                .isGreaterThan(detail.lines().get(detail.lines().size() - 1).journalEntryId());
    }

    @Test
    void getAccountUnknownCodeNotFound() {
        assertThatThrownBy(() -> balanceQuery.getAccount("9999"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("account_not_found");
    }

    @Test
    void listJournalOldestFirst() {
        Account cash = accounts.findByCode("1000").orElseThrow();
        Account suspense = accounts.findByCode("2300").orElseThrow();
        JournalEntry second = new JournalEntry().setPostedAt(Instant.parse("2026-02-01T00:00:00Z"));
        second.addLine(new JournalLine().setAccount(cash).setSide(EntrySide.CREDIT).setAmount(new BigDecimal("1.00")));
        second.addLine(new JournalLine().setAccount(suspense).setSide(EntrySide.DEBIT).setAmount(new BigDecimal("1.00")));
        ledgerService.postBalancedEntry(second);

        var journal = balanceQuery.listJournal();
        assertThat(journal).hasSizeGreaterThanOrEqualTo(2);
        assertThat(journal.get(0).id()).isLessThan(journal.get(journal.size() - 1).id());
        assertThat(journal.get(0).lines()).isNotEmpty();
    }
}
