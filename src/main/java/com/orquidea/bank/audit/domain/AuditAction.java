package com.orquidea.bank.audit.domain;

public enum AuditAction {
    SEED_ACCOUNT,
    SEED_CUSTOMER,
    SEED_MERCHANT,
    SEED_DEPOSIT,
    SEED_CARD,
    PURCHASE_AUTHORIZED,
    PURCHASE_CLEARED,
    PURCHASE_CUSTOMER_POSTED,
    PURCHASE_SETTLED
}
