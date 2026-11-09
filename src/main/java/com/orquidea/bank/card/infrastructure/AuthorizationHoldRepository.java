package com.orquidea.bank.card.infrastructure;

import com.orquidea.bank.card.domain.AuthorizationHold;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationHoldRepository extends JpaRepository<AuthorizationHold, Integer> {
}
