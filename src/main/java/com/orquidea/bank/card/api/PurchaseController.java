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

    /**
     * Today this runs authorize → clear → postCustomer → settleMerchant in one request.
     * Real-world payments often sit in limbo between those stages; candidates for later work:
     * <ul>
     *   <li>Authorization hold waiting for merchant capture (auth done, no clear/post yet)</li>
     *   <li>Amount not final yet (hotels, rentals, fuel, tips) — estimate hold, later capture</li>
     *   <li>Clearing / settlement batch lag (overnight or business-day windows)</li>
     *   <li>Merchant delay submitting clearing (auth can sit days until capture or expiry)</li>
     *   <li>Network / bank pending processing before the ledger posts</li>
     *   <li>Exceptions and reviews (fraud, amount mismatch, RFI) that pause posting</li>
     *   <li>Auth expires unused — hold drops with no post (cancel, not settle)</li>
     * </ul>
     * Until those exist, available can differ from posted only while a hold is open inside this walk.
     */
    @PostMapping("/api/purchases")
    @Operation(summary = "Run authorize, clear, post customer, settle merchant")
    public PurchaseTrailResponse create(
            @Valid @RequestBody CreatePurchaseRequest request,
            @Parameter(description = "Replay returns the original trail")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return purchases.purchase(request, idempotencyKey);
    }

    /**
     * Returns the purchase plus hold, clearing item, and settlement.
     * Later, this is the natural place to expose mid-walk limbo status
     * (AUTHORIZED / CLEARED with no customer posting yet, expired holds, paused reviews).
     */
    @GetMapping("/api/purchases/{purchaseId}")
    @Operation(summary = "Purchase with hold, clearing item, and settlement")
    public PurchaseDetailResponse show(@PathVariable String purchaseId) {
        return purchases.getPurchase(purchaseId);
    }
}
