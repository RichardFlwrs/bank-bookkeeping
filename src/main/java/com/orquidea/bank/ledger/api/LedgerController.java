package com.orquidea.bank.ledger.api;

import com.orquidea.bank.ledger.api.dto.AccountBalanceResponse;
import com.orquidea.bank.ledger.api.dto.AccountDetailResponse;
import com.orquidea.bank.ledger.api.dto.JournalEntryResponse;
import com.orquidea.bank.ledger.application.BalanceQuery;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Ledger")
public class LedgerController {

    private final BalanceQuery balances;

    public LedgerController(BalanceQuery balances) {
        this.balances = balances;
    }

    @GetMapping("/api/ledger/accounts")
    public List<AccountBalanceResponse> accounts() {
        return balances.listAccounts();
    }

    @GetMapping("/api/ledger/accounts/{code}")
    public AccountDetailResponse account(@PathVariable String code) {
        return balances.getAccount(code);
    }

    @GetMapping("/api/ledger/journal")
    public List<JournalEntryResponse> journal() {
        return balances.listJournal();
    }
}
