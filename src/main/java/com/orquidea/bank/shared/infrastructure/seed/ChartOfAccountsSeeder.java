package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.application.AuditRecorder;
import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.ledger.domain.AccountType;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.shared.infrastructure.BankSeedProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChartOfAccountsSeeder {

    public static final String ACTOR = "seeder:chart-of-accounts";
    public static final String CASH_CODE = "1000";
    public static final String ADA_DEPOSIT_CODE = "2100";
    public static final String CAFE_DEPOSIT_CODE = "2200";
    public static final String SUSPENSE_CODE = "2300";
    public static final String INCOME_CODE = "4100";

    private final AccountRepository accounts;
    private final AuditRecorder auditRecorder;
    private final BankSeedProperties properties;

    public ChartOfAccountsSeeder(
            AccountRepository accounts,
            AuditRecorder auditRecorder,
            BankSeedProperties properties
    ) {
        this.accounts = accounts;
        this.auditRecorder = auditRecorder;
        this.properties = properties;
    }

    public SeedChart seed(String traceId) {
        Account cash = saveAccount(CASH_CODE, "Cash", AccountType.ASSET, traceId);
        Account suspense = saveAccount(SUSPENSE_CODE, "Settlement Suspense", AccountType.LIABILITY, traceId);
        Account income = saveAccount(INCOME_CODE, "Interchange Income", AccountType.INCOME, traceId);

        List<Account> customerDeposits = new ArrayList<>();
        customerDeposits.add(saveAccount(
                ADA_DEPOSIT_CODE,
                "Customer Deposits — Ada Lovelace",
                AccountType.LIABILITY,
                traceId
        ));
        for (int i = 1; i < properties.getCustomers(); i++) {
            String code = String.valueOf(3000 + i);
            customerDeposits.add(saveAccount(
                    code,
                    "Customer Deposits — " + code,
                    AccountType.LIABILITY,
                    traceId
            ));
        }

        List<Account> merchantDeposits = new ArrayList<>();
        merchantDeposits.add(saveAccount(
                CAFE_DEPOSIT_CODE,
                "Merchant Deposits — Café Orquídea",
                AccountType.LIABILITY,
                traceId
        ));
        for (int i = 1; i < properties.getMerchants(); i++) {
            String code = String.valueOf(5000 + i);
            merchantDeposits.add(saveAccount(
                    code,
                    "Merchant Deposits — " + code,
                    AccountType.LIABILITY,
                    traceId
            ));
        }

        return new SeedChart(cash, suspense, income, List.copyOf(customerDeposits), List.copyOf(merchantDeposits));
    }

    private Account saveAccount(String code, String name, AccountType type, String traceId) {
        Account account = accounts.save(new Account().setCode(code).setName(name).setType(type));
        auditRecorder.record(
                ACTOR,
                AuditAction.SEED_ACCOUNT,
                "Account",
                String.valueOf(account.getId()),
                null,
                traceId,
                null,
                SeedJson.of("id", account.getId(), "code", code, "name", name, "type", type.name())
        );
        return account;
    }
}
