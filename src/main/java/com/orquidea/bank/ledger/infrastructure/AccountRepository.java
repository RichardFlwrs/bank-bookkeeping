package com.orquidea.bank.ledger.infrastructure;

import com.orquidea.bank.ledger.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    Optional<Account> findByCode(String code);
}
