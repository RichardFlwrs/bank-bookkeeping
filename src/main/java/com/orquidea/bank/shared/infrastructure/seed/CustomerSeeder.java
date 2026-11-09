package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.application.AuditRecorder;
import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.customer.domain.Customer;
import com.orquidea.bank.customer.infrastructure.CustomerRepository;
import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.shared.infrastructure.BankSeedProperties;
import net.datafaker.Faker;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CustomerSeeder {

    public static final String ACTOR = "seeder:customer";
    public static final String BACKFILL_ACTOR = "seeder:customer-credentials";
    public static final String ADA_NAME = "Ada Lovelace";
    public static final String ADA_USERNAME = "ada";
    public static final String DEV_PASSWORD = "orquidea";

    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final AuditRecorder auditRecorder;
    private final BankSeedProperties properties;
    private final PasswordEncoder passwordEncoder;

    public CustomerSeeder(
            CustomerRepository customers,
            AccountRepository accounts,
            AuditRecorder auditRecorder,
            BankSeedProperties properties,
            PasswordEncoder passwordEncoder
    ) {
        this.customers = customers;
        this.accounts = accounts;
        this.auditRecorder = auditRecorder;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Customer> seed(String traceId, List<Account> customerDeposits) {
        if (customerDeposits.size() != properties.getCustomers()) {
            throw new IllegalArgumentException("customer deposit accounts must match customer count");
        }

        String passwordHash = passwordEncoder.encode(DEV_PASSWORD);
        Faker faker = new Faker(Locale.forLanguageTag("es"), new Random(properties.getSeed()));
        List<Customer> seeded = new ArrayList<>();

        for (int i = 0; i < customerDeposits.size(); i++) {
            Account deposit = customerDeposits.get(i);
            String name = i == 0 ? ADA_NAME : faker.name().fullName();
            String username = i == 0 ? ADA_USERNAME : usernameFor(deposit.getCode());
            Customer customer = customers.save(new Customer()
                    .setName(name)
                    .setDepositAccountId(deposit.getId())
                    .setUsername(username)
                    .setPasswordHash(passwordHash));
            auditRecorder.record(
                    ACTOR,
                    AuditAction.SEED_CUSTOMER,
                    "Customer",
                    String.valueOf(customer.getId()),
                    null,
                    traceId,
                    null,
                    auditSnapshot(customer)
            );
            seeded.add(customer);
        }
        return List.copyOf(seeded);
    }

    public int backfillCredentials(String traceId) {
        List<Customer> missing = customers.findByUsernameIsNull();
        if (missing.isEmpty()) {
            return 0;
        }

        Map<Integer, Account> depositsById = accounts.findAll().stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        String passwordHash = passwordEncoder.encode(DEV_PASSWORD);
        int updated = 0;

        for (Customer customer : missing) {
            Account deposit = depositsById.get(customer.getDepositAccountId());
            if (deposit == null) {
                continue;
            }
            String before = auditSnapshot(customer);
            String username = ChartOfAccountsSeeder.ADA_DEPOSIT_CODE.equals(deposit.getCode())
                    ? ADA_USERNAME
                    : usernameFor(deposit.getCode());
            customer.setUsername(username);
            customer.setPasswordHash(passwordHash);
            customers.save(customer);
            auditRecorder.record(
                    BACKFILL_ACTOR,
                    AuditAction.SEED_CUSTOMER,
                    "Customer",
                    String.valueOf(customer.getId()),
                    null,
                    traceId,
                    before,
                    auditSnapshot(customer)
            );
            updated++;
        }
        return updated;
    }

    public static String usernameFor(String accountCode) {
        return "c" + accountCode;
    }

    private static String auditSnapshot(Customer customer) {
        return SeedJson.of(
                "id", customer.getId(),
                "name", customer.getName(),
                "depositAccountId", customer.getDepositAccountId(),
                "username", customer.getUsername()
        );
    }
}
