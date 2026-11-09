package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.application.AuditRecorder;
import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.customer.domain.Customer;
import com.orquidea.bank.customer.infrastructure.CustomerRepository;
import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.shared.infrastructure.BankSeedProperties;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Component
public class CustomerSeeder {

    public static final String ACTOR = "seeder:customer";
    public static final String ADA_NAME = "Ada Lovelace";

    private final CustomerRepository customers;
    private final AuditRecorder auditRecorder;
    private final BankSeedProperties properties;

    public CustomerSeeder(
            CustomerRepository customers,
            AuditRecorder auditRecorder,
            BankSeedProperties properties
    ) {
        this.customers = customers;
        this.auditRecorder = auditRecorder;
        this.properties = properties;
    }

    public List<Customer> seed(String traceId, List<Account> customerDeposits) {
        if (customerDeposits.size() != properties.getCustomers()) {
            throw new IllegalArgumentException("customer deposit accounts must match customer count");
        }

        Faker faker = new Faker(Locale.forLanguageTag("es"), new Random(properties.getSeed()));
        List<Customer> seeded = new ArrayList<>();

        for (int i = 0; i < customerDeposits.size(); i++) {
            Account deposit = customerDeposits.get(i);
            String name = i == 0 ? ADA_NAME : faker.name().fullName();
            Customer customer = customers.save(new Customer()
                    .setName(name)
                    .setDepositAccountId(deposit.getId()));
            auditRecorder.record(
                    ACTOR,
                    AuditAction.SEED_CUSTOMER,
                    "Customer",
                    String.valueOf(customer.getId()),
                    null,
                    traceId,
                    null,
                    SeedJson.of(
                            "id", customer.getId(),
                            "name", customer.getName(),
                            "depositAccountId", customer.getDepositAccountId()
                    )
            );
            seeded.add(customer);
        }
        return List.copyOf(seeded);
    }
}
