package com.orquidea.bank.shared.infrastructure.seed;

import com.orquidea.bank.audit.application.AuditRecorder;
import com.orquidea.bank.audit.domain.AuditAction;
import com.orquidea.bank.card.domain.Card;
import com.orquidea.bank.card.infrastructure.CardRepository;
import com.orquidea.bank.customer.domain.Customer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CardSeeder {

    public static final String ACTOR = "seeder:card";
    public static final String ADA_TOKEN = "card_ada_orquidea";

    private final CardRepository cards;
    private final AuditRecorder auditRecorder;

    public CardSeeder(CardRepository cards, AuditRecorder auditRecorder) {
        this.cards = cards;
        this.auditRecorder = auditRecorder;
    }

    public List<Card> seed(String traceId, List<Customer> customers) {
        List<Card> seeded = new ArrayList<>();
        for (int i = 0; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            String token = i == 0 ? ADA_TOKEN : "card_" + (i + 1);
            Card card = cards.save(new Card()
                    .setToken(token)
                    .setCustomerId(customer.getId()));
            auditRecorder.record(
                    ACTOR,
                    AuditAction.SEED_CARD,
                    "Card",
                    String.valueOf(card.getId()),
                    null,
                    traceId,
                    null,
                    SeedJson.of(
                            "id", card.getId(),
                            "token", card.getToken(),
                            "customerId", card.getCustomerId()
                    )
            );
            seeded.add(card);
        }
        return List.copyOf(seeded);
    }
}
