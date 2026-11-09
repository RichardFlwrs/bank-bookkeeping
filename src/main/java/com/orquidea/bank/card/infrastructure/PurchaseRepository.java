package com.orquidea.bank.card.infrastructure;

import com.orquidea.bank.card.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {

    Optional<Purchase> findByPublicId(String publicId);

    Optional<Purchase> findByIdempotencyKey(String idempotencyKey);
}
