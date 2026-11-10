package com.orquidea.bank.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "journal_line")
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntrySide side;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    public Integer getId() {
        return id;
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public JournalLine setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
        return this;
    }

    public Account getAccount() {
        return account;
    }

    public JournalLine setAccount(Account account) {
        this.account = account;
        return this;
    }

    public EntrySide getSide() {
        return side;
    }

    public JournalLine setSide(EntrySide side) {
        this.side = side;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public JournalLine setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }
}
