package com.orquidea.bank.ledger.api.dto;

import java.util.List;

public record AccountDetailResponse(
        String code,
        String name,
        String type,
        String posted,
        String available,
        List<JournalLineResponse> lines
) {
}
