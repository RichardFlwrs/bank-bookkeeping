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

    /**
     * Posted = sum of journal lines on the books. Available = posted minus open authorization holds.
     * When purchases stay in limbo (auth without capture, delayed clear/settle, pending review),
     * available drops while posted stays put until customer posting runs. Later work may keep
     * holds open across requests so this list shows that gap for longer than one purchase walk.
     */
    @GetMapping("/api/ledger/accounts")
    @Operation(summary = "Chart of accounts with posted and available balances")
    public List<AccountBalanceResponse> accounts() {
        return balances.listAccounts();
    }

    /**
     * One account and its posted journal lines. Limbo payments do not appear here until posted;
     * open holds only affect available. Future: surface pending/authorized activity alongside lines.
     */
    @GetMapping("/api/ledger/accounts/{code}")
    @Operation(summary = "One account plus its journal lines")
    public AccountDetailResponse account(
            @Parameter(example = "2100", description = "Account code")
            @PathVariable String code
    ) {
        return balances.getAccount(code);
    }

    /**
     * Only posted journal entries. Auth holds, uncleared captures, and expired unused auths
     * are out of scope here until those cases write (or deliberately omit) journal rows.
     */
    @GetMapping("/api/ledger/journal")
    @Operation(summary = "Every journal entry and its lines, oldest first")
    public List<JournalEntryResponse> journal() {
        return balances.listJournal();
    }
}
