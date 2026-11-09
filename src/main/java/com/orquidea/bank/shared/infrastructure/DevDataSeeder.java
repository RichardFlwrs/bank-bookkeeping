package com.orquidea.bank.shared.infrastructure;

import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DevDataSeeder {

    @Bean
    @Profile("!test")
    CommandLineRunner seedBankData(AccountRepository accounts, BankDataFixture bankDataFixture) {
        return args -> {
            if (accounts.count() > 0) {
                return;
            }
            bankDataFixture.seed("trc_seed_run");
        };
    }
}
