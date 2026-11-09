package com.orquidea.bank.ledger.infrastructure;

import com.orquidea.bank.ledger.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Integer> {
}
