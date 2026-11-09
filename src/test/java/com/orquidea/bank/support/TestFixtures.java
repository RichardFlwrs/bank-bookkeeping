package com.orquidea.bank.support;

import com.orquidea.bank.audit.infrastructure.AuditEventRepository;
import com.orquidea.bank.card.infrastructure.AuthorizationHoldRepository;
import com.orquidea.bank.card.infrastructure.CardRepository;
import com.orquidea.bank.card.infrastructure.PurchaseRepository;
import com.orquidea.bank.customer.infrastructure.CustomerRepository;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.ledger.infrastructure.JournalEntryRepository;
import com.orquidea.bank.merchant.infrastructure.MerchantRepository;
import com.orquidea.bank.settlement.infrastructure.ClearingItemRepository;
import com.orquidea.bank.settlement.infrastructure.SettlementRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestFixtures {

    private final JdbcTemplate jdbc;
    private final AccountRepository accounts;
    private final JournalEntryRepository journalEntries;
    private final CustomerRepository customers;
    private final MerchantRepository merchants;
    private final CardRepository cards;
    private final PurchaseRepository purchases;
    private final AuthorizationHoldRepository holds;
    private final ClearingItemRepository clearingItems;
    private final SettlementRepository settlements;
    private final AuditEventRepository auditEvents;

    public TestFixtures(
            JdbcTemplate jdbc,
            AccountRepository accounts,
            JournalEntryRepository journalEntries,
            CustomerRepository customers,
            MerchantRepository merchants,
            CardRepository cards,
            PurchaseRepository purchases,
            AuthorizationHoldRepository holds,
            ClearingItemRepository clearingItems,
            SettlementRepository settlements,
            AuditEventRepository auditEvents
    ) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.journalEntries = journalEntries;
        this.customers = customers;
        this.merchants = merchants;
        this.cards = cards;
        this.purchases = purchases;
        this.holds = holds;
        this.clearingItems = clearingItems;
        this.settlements = settlements;
        this.auditEvents = auditEvents;
    }

    public void clearDatabase() {
        jdbc.execute("PRAGMA foreign_keys = OFF");
        jdbc.update("DELETE FROM audit_event");
        jdbc.update("DELETE FROM settlement");
        jdbc.update("DELETE FROM clearing_item");
        jdbc.update("DELETE FROM authorization_hold");
        jdbc.update("DELETE FROM purchase");
        jdbc.update("DELETE FROM card");
        jdbc.update("DELETE FROM customer");
        jdbc.update("DELETE FROM merchant");
        jdbc.update("DELETE FROM journal_line");
        jdbc.update("DELETE FROM journal_entry");
        jdbc.update("DELETE FROM account");
        jdbc.execute("PRAGMA foreign_keys = ON");
        auditEvents.flush();
        settlements.flush();
        clearingItems.flush();
        holds.flush();
        purchases.flush();
        cards.flush();
        customers.flush();
        merchants.flush();
        journalEntries.flush();
        accounts.flush();
    }
}
