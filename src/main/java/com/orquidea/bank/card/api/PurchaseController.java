package com.orquidea.bank.card.api;

import com.orquidea.bank.card.api.dto.CreatePurchaseRequest;
import com.orquidea.bank.card.api.dto.PurchaseDetailResponse;
import com.orquidea.bank.card.api.dto.PurchaseTrailResponse;
import com.orquidea.bank.card.application.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Purchases")
public class PurchaseController {

    private final PurchaseService purchases;

    public PurchaseController(PurchaseService purchases) {
        this.purchases = purchases;
    }

    @PostMapping("/api/purchases")
    @Operation(summary = "Run authorize, clear, post customer, settle merchant")
    public PurchaseTrailResponse create(
            @Valid @RequestBody CreatePurchaseRequest request,
            @Parameter(description = "Replay returns the original trail")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return purchases.purchase(request, idempotencyKey);
    }

    @GetMapping("/api/purchases/{purchaseId}")
    @Operation(summary = "Purchase with hold, clearing item, and settlement")
    public PurchaseDetailResponse show(@PathVariable String purchaseId) {
        return purchases.getPurchase(purchaseId);
    }
}
