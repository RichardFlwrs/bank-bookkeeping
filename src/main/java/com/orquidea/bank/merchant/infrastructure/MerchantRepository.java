package com.orquidea.bank.merchant.infrastructure;

import com.orquidea.bank.merchant.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {

    List<Merchant> findAllByOrderByNameAsc();

    Optional<Merchant> findByDepositAccountId(Integer depositAccountId);
}
