package com.orquidea.bank.customer.api;

import com.orquidea.bank.shared.infrastructure.BankDataFixture;
import com.orquidea.bank.shared.infrastructure.seed.CustomerSeeder;
import com.orquidea.bank.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginAndLedgerPageTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    BankDataFixture fixture;

    @Autowired
    TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
        fixture.seed("trc_login_pages");
    }

    @Test
    void loginPageRenders() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cardholder login")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")));
    }

    @Test
    void wrongPasswordStaysOnLoginError() throws Exception {
        mvc.perform(formLogin("/login")
                        .user(CustomerSeeder.ADA_USERNAME)
                        .password("wrong-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void adaCanSignIn() throws Exception {
        mvc.perform(formLogin("/login")
                        .user(CustomerSeeder.ADA_USERNAME)
                        .password(CustomerSeeder.DEV_PASSWORD))
                .andExpect(authenticated().withUsername(CustomerSeeder.ADA_USERNAME))
                .andExpect(redirectedUrl("/ledger/me"));
    }

    @Test
    void myLedgerWithoutSessionRedirectsToLogin() throws Exception {
        mvc.perform(get("/ledger/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void myLedgerShowsAdaDepositAccount() throws Exception {
        MvcResult login = mvc.perform(formLogin("/login")
                        .user(CustomerSeeder.ADA_USERNAME)
                        .password(CustomerSeeder.DEV_PASSWORD))
                .andExpect(authenticated().withUsername(CustomerSeeder.ADA_USERNAME))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/ledger/me").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"account-code\">2100<")))
                .andExpect(content().string(containsString("Customer Deposits — Ada Lovelace")))
                .andExpect(content().string(containsString("1000.00")))
                .andExpect(content().string(not(containsString("name=\"accountCode\""))));
    }

    @Test
    void debuggerAccountsPageStaysPublic() throws Exception {
        mvc.perform(get("/ledger/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ledger accounts")));
    }

    @Test
    void purchaseApiIsNotBlockedByCsrf() throws Exception {
        mvc.perform(post("/api/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "login-csrf-check")
                        .content("{\"cardToken\":\"card_ada_orquidea\",\"amount\":\"45.00\"}"))
                .andExpect(status().isOk());
    }
}
