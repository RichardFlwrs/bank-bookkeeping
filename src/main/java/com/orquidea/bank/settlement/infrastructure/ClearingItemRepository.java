package com.orquidea.bank.settlement.infrastructure;

import com.orquidea.bank.settlement.domain.ClearingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClearingItemRepository extends JpaRepository<ClearingItem, Integer> {

    Optional<ClearingItem> findByPurchaseId(Integer purchaseId);
}
