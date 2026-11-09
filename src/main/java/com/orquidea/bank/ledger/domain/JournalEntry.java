package com.orquidea.bank.ledger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entry")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "posted_at", nullable = false, columnDefinition = "TEXT")
    private Instant postedAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalLine> lines = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public JournalEntry setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
        return this;
    }

    public List<JournalLine> getLines() {
        return lines;
    }

    public JournalEntry addLine(JournalLine line) {
        lines.add(line);
        line.setJournalEntry(this);
        return this;
    }

    public boolean isBalanced() {
        if (lines.size() < 2) {
            return false;
        }
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            if (line.getAmount() == null || line.getSide() == null) {
                return false;
            }
            if (line.getSide() == EntrySide.DEBIT) {
                debits = debits.add(line.getAmount());
            } else {
                credits = credits.add(line.getAmount());
            }
        }
        return debits.compareTo(credits) == 0;
    }
}
