package com.orquidea.bank.customer.application;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CardholderPrincipal implements UserDetails {

    private final Integer customerId;
    private final Integer depositAccountId;
    private final String username;
    private final String passwordHash;

    public CardholderPrincipal(
            Integer customerId,
            Integer depositAccountId,
            String username,
            String passwordHash
    ) {
        this.customerId = customerId;
        this.depositAccountId = depositAccountId;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public Integer getDepositAccountId() {
        return depositAccountId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CARDHOLDER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return username != null && passwordHash != null;
    }
}
