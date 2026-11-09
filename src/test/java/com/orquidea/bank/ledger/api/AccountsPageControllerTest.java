package com.orquidea.bank.ledger.api;

import com.orquidea.bank.shared.infrastructure.BankDataFixture;
import com.orquidea.bank.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountsPageControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    BankDataFixture fixture;

    @Autowired
    TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
        fixture.seed("trc_accounts_page");
    }

    @Test
    void printsEverySeededAccount() throws Exception {
        mvc.perform(get("/ledger/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"account-count\">10<")))
                .andExpect(content().string(containsString("1000")))
                .andExpect(content().string(containsString("Cash")))
                .andExpect(content().string(containsString("ASSET")))
                .andExpect(content().string(containsString("2100")))
                .andExpect(content().string(containsString("Customer Deposits — Ada Lovelace")))
                .andExpect(content().string(containsString("2200")))
                .andExpect(content().string(containsString("Merchant Deposits — Café Orquídea")))
                .andExpect(content().string(containsString("2300")))
                .andExpect(content().string(containsString("Settlement Suspense")))
                .andExpect(content().string(containsString("4100")))
                .andExpect(content().string(containsString("Interchange Income")))
                .andExpect(content().string(containsString("INCOME")))
                .andExpect(content().string(containsString("3001")))
                .andExpect(content().string(containsString("5001")))
                .andExpect(content().string(containsString("1000.00")))
                .andExpect(content().string(containsString("5000.00")));
    }
}
