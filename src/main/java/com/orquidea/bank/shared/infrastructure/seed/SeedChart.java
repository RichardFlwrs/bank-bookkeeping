package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.ledger.domain.Account;

import java.util.List;

public record SeedChart(
        Account cash,
        Account suspense,
        Account income,
        List<Account> customerDeposits,
        List<Account> merchantDeposits
) {
}
