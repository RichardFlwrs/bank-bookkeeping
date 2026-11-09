package com.orquidea.bank.card.infrastructure;

import com.orquidea.bank.card.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {

    Optional<Purchase> findByPublicId(String publicId);

    Optional<Purchase> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select p from Purchase p
            join fetch p.card
            left join fetch p.hold
            where p.idempotencyKey = :idempotencyKey
            """)
    Optional<Purchase> findDetailedByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Query("""
            select p from Purchase p
            join fetch p.card
            left join fetch p.hold
            where p.publicId = :publicId
            """)
    Optional<Purchase> findDetailedByPublicId(@Param("publicId") String publicId);
}
