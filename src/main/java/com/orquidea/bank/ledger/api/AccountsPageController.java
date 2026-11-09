package com.orquidea.bank.ledger.api;

import com.orquidea.bank.ledger.application.BalanceQuery;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Hidden
public class AccountsPageController {

    private final BalanceQuery balances;

    public AccountsPageController(BalanceQuery balances) {
        this.balances = balances;
    }

    @GetMapping("/ledger/accounts")
    public String accounts(Model model) {
        model.addAttribute("accounts", balances.listAccounts());
        return "ledger/accounts";
    }
}
