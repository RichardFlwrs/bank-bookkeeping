package com.orquidea.bank.merchant.infrastructure;

import com.orquidea.bank.merchant.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
}
