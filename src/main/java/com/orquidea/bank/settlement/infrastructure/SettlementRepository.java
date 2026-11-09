package com.orquidea.bank.settlement.infrastructure;

import com.orquidea.bank.settlement.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Integer> {

    Optional<Settlement> findByPurchaseId(Integer purchaseId);
}
