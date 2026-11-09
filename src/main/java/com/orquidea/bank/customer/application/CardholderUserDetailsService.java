package com.orquidea.bank.customer.application;

import com.orquidea.bank.customer.domain.Customer;
import com.orquidea.bank.customer.infrastructure.CustomerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CardholderUserDetailsService implements UserDetailsService {

    private final CustomerRepository customers;

    public CardholderUserDetailsService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Customer customer = customers.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("customer_not_found"));
        return new CardholderPrincipal(
                customer.getId(),
                customer.getDepositAccountId(),
                customer.getUsername(),
                customer.getPasswordHash()
        );
    }
}
