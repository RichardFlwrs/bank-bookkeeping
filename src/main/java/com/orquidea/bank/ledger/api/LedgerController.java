package com.orquidea.bank.ledger.api;

import com.orquidea.bank.ledger.api.dto.AccountBalanceResponse;
import com.orquidea.bank.ledger.api.dto.AccountDetailResponse;
import com.orquidea.bank.ledger.api.dto.JournalEntryResponse;
import com.orquidea.bank.ledger.application.BalanceQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(summary = "Chart of accounts with posted and available balances")
    public List<AccountBalanceResponse> accounts() {
        return balances.listAccounts();
    }

    @GetMapping("/api/ledger/accounts/{code}")
    @Operation(summary = "One account plus its journal lines")
    public AccountDetailResponse account(
            @Parameter(example = "2100", description = "Account code")
            @PathVariable String code
    ) {
        return balances.getAccount(code);
    }

    @GetMapping("/api/ledger/journal")
    @Operation(summary = "Every journal entry and its lines, oldest first")
    public List<JournalEntryResponse> journal() {
        return balances.listJournal();
    }
}
