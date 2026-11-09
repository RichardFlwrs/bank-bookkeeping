package com.orquidea.bank.card.application;

import com.orquidea.bank.card.api.dto.CreatePurchaseRequest;
import com.orquidea.bank.card.api.dto.PurchaseDetailResponse;
import com.orquidea.bank.card.api.dto.PurchaseTrailResponse;
import org.springframework.stereotype.Service;

@Service
public class PurchaseService {

    public PurchaseTrailResponse purchase(CreatePurchaseRequest request, String idempotencyKey) {
        throw new UnsupportedOperationException("PurchaseService.purchase is not implemented yet");
    }

    public PurchaseDetailResponse getPurchase(String purchaseId) {
        throw new UnsupportedOperationException("PurchaseService.getPurchase is not implemented yet");
    }

    public void authorize() {
        throw new UnsupportedOperationException("PurchaseService.authorize is not implemented yet");
    }

    public void clear() {
        throw new UnsupportedOperationException("PurchaseService.clear is not implemented yet");
    }

    public void postCustomer() {
        throw new UnsupportedOperationException("PurchaseService.postCustomer is not implemented yet");
    }

    public void settleMerchant() {
        throw new UnsupportedOperationException("PurchaseService.settleMerchant is not implemented yet");
    }
}
