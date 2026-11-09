package com.orquidea.bank.customer.api;

import com.orquidea.bank.customer.application.CardholderPrincipal;
import com.orquidea.bank.ledger.application.BalanceQuery;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Hidden
public class MyLedgerPageController {

    private final BalanceQuery balances;

    public MyLedgerPageController(BalanceQuery balances) {
        this.balances = balances;
    }

    /**
     * Cardholder view of posted balance, available balance, and posted journal lines.
     * Real-world limbo (auth waiting for capture, estimated amounts, settlement lag, merchant
     * delay, network pending, fraud/review pause, expired unused auth) would show as
     * available &lt; posted, or as pending items that are not journal lines yet.
     * Candidates for later: list open holds and mid-walk purchases on this page.
     */
    @GetMapping("/ledger/me")
    public String myLedger(
            @AuthenticationPrincipal CardholderPrincipal cardholder,
            Model model
    ) {
        model.addAttribute("account", balances.getAccountById(cardholder.getDepositAccountId()));
        model.addAttribute("cardholderName", cardholder.getUsername());
        return "ledger/me";
    }
}
