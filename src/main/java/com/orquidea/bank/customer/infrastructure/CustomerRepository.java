package com.orquidea.bank.customer.infrastructure;

import com.orquidea.bank.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByUsername(String username);

    List<Customer> findByUsernameIsNull();
}
