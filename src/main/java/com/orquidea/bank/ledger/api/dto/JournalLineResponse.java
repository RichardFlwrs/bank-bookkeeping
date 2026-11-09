package com.orquidea.bank.ledger.api.dto;

public record JournalLineResponse(
        Integer journalEntryId,
        String side,
        String amount,
        String accountCode
) {
}
