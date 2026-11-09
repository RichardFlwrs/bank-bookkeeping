package com.orquidea.bank.customer.infrastructure;

import com.orquidea.bank.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
}
