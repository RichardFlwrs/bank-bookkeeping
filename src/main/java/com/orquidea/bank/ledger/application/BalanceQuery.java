package com.orquidea.bank.ledger.application;

import com.orquidea.bank.ledger.api.dto.AccountBalanceResponse;
import com.orquidea.bank.ledger.api.dto.AccountDetailResponse;
import com.orquidea.bank.ledger.api.dto.JournalEntryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BalanceQuery {

    public List<AccountBalanceResponse> listAccounts() {
        throw new UnsupportedOperationException("BalanceQuery.listAccounts is not implemented yet");
    }

    public AccountDetailResponse getAccount(String code) {
        throw new UnsupportedOperationException("BalanceQuery.getAccount is not implemented yet");
    }

    public List<JournalEntryResponse> listJournal() {
        throw new UnsupportedOperationException("BalanceQuery.listJournal is not implemented yet");
    }
}
