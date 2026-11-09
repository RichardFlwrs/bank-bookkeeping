package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.application.AuditRecorder;
import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.merchant.domain.Merchant;
import com.orquidea.bank.merchant.infrastructure.MerchantRepository;
import com.orquidea.bank.shared.infrastructure.BankSeedProperties;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Component
public class MerchantSeeder {

    public static final String ACTOR = "seeder:merchant";
    public static final String CAFE_NAME = "Café Orquídea";

    private final MerchantRepository merchants;
    private final AuditRecorder auditRecorder;
    private final BankSeedProperties properties;

    public MerchantSeeder(
            MerchantRepository merchants,
            AuditRecorder auditRecorder,
            BankSeedProperties properties
    ) {
        this.merchants = merchants;
        this.auditRecorder = auditRecorder;
        this.properties = properties;
    }

    public List<Merchant> seed(String traceId, List<Account> merchantDeposits) {
        if (merchantDeposits.size() != properties.getMerchants()) {
            throw new IllegalArgumentException("merchant deposit accounts must match merchant count");
        }

        Faker faker = new Faker(Locale.forLanguageTag("es"), new Random(properties.getSeed() + 7));
        List<Merchant> seeded = new ArrayList<>();

        for (int i = 0; i < merchantDeposits.size(); i++) {
            Account deposit = merchantDeposits.get(i);
            String name = i == 0 ? CAFE_NAME : faker.company().name();
            Merchant merchant = merchants.save(new Merchant()
                    .setName(name)
                    .setDepositAccountId(deposit.getId()));
            auditRecorder.record(
                    ACTOR,
                    AuditAction.SEED_MERCHANT,
                    "Merchant",
                    String.valueOf(merchant.getId()),
                    null,
                    traceId,
                    null,
                    SeedJson.of(
                            "id", merchant.getId(),
                            "name", merchant.getName(),
                            "depositAccountId", merchant.getDepositAccountId()
                    )
            );
            seeded.add(merchant);
        }
        return List.copyOf(seeded);
    }
}
