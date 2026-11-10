package com.orquidea.bank.customer.api;

import com.orquidea.bank.card.api.dto.CreatePurchaseRequest;
import com.orquidea.bank.card.domain.Card;
import com.orquidea.bank.card.application.PurchaseService;
import com.orquidea.bank.card.infrastructure.CardRepository;
import com.orquidea.bank.customer.application.CardholderPrincipal;
import com.orquidea.bank.ledger.api.dto.AccountDetailResponse;
import com.orquidea.bank.ledger.application.BalanceQuery;
import com.orquidea.bank.merchant.domain.Merchant;
import com.orquidea.bank.merchant.infrastructure.MerchantRepository;
import com.orquidea.bank.shared.domain.ApiException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Controller
@Hidden
@Validated
public class PayPageController {

    private final BalanceQuery balances;
    private final MerchantRepository merchants;
    private final CardRepository cards;
    private final PurchaseService purchases;

    public PayPageController(
            BalanceQuery balances,
            MerchantRepository merchants,
            CardRepository cards,
            PurchaseService purchases
    ) {
        this.balances = balances;
        this.merchants = merchants;
        this.cards = cards;
        this.purchases = purchases;
    }

    @GetMapping("/ledger/pay")
    public String payForm(
            @AuthenticationPrincipal CardholderPrincipal cardholder,
            Model model
    ) {
        populateForm(model, cardholder, null, null, null, null);
        return "ledger/pay";
    }

    @PostMapping("/ledger/pay")
    public String pay(
            @AuthenticationPrincipal CardholderPrincipal cardholder,
            @RequestParam @NotNull BigDecimal amount,
            @RequestParam @NotBlank String label,
            @RequestParam @NotNull Integer merchantId,
            Model model,
            RedirectAttributes redirect
    ) {
        try {
            Card card = cards.findByCustomerId(cardholder.getCustomerId())
                    .orElseThrow(() -> ApiException.notFound("card_not_found"));
            var trail = purchases.purchase(
                    new CreatePurchaseRequest(card.getToken(), amount, label.trim(), merchantId),
                    "web-" + UUID.randomUUID()
            );
            redirect.addFlashAttribute(
                    "flashOk",
                    "Paid " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                            + " for " + label.trim()
                            + " (" + trail.purchaseId() + ")."
            );
            return "redirect:/ledger/me";
        } catch (ApiException ex) {
            populateForm(model, cardholder, amount, label, merchantId, ex.getCode());
            return "ledger/pay";
        }
    }

    private void populateForm(
            Model model,
            CardholderPrincipal cardholder,
            BigDecimal amount,
            String label,
            Integer merchantId,
            String error
    ) {
        AccountDetailResponse account = balances.getAccountById(cardholder.getDepositAccountId());
        List<Merchant> merchantList = merchants.findAllByOrderByNameAsc();
        model.addAttribute("account", account);
        model.addAttribute("available", account.available());
        model.addAttribute("merchants", merchantList);
        model.addAttribute("amount", amount);
        model.addAttribute("label", label);
        model.addAttribute("merchantId", merchantId);
        model.addAttribute("error", error);
    }
}
