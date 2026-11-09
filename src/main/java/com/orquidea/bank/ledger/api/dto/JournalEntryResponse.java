package com.orquidea.bank.ledger.api.dto;

import java.util.List;

public record JournalEntryResponse(
        Integer id,
        String postedAt,
        List<JournalLineResponse> lines
) {
}
