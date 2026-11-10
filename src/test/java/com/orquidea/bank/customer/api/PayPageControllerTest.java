package com.orquidea.bank.customer.api;

import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.merchant.domain.Merchant;
import com.orquidea.bank.merchant.infrastructure.MerchantRepository;
import com.orquidea.bank.shared.infrastructure.BankDataFixture;
import com.orquidea.bank.shared.infrastructure.seed.ChartOfAccountsSeeder;
import com.orquidea.bank.shared.infrastructure.seed.CustomerSeeder;
import com.orquidea.bank.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PayPageControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    BankDataFixture fixture;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    MerchantRepository merchants;

    @Autowired
    AccountRepository accounts;

    @Autowired
    com.orquidea.bank.ledger.application.BalanceCalculator balances;

    @BeforeEach
    void setUp() {
        fixtures.clearDatabase();
        fixture.seed("trc_pay_page");
    }

    @Test
    void payPageRequiresLogin() throws Exception {
        mvc.perform(get("/ledger/pay"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void payPageShowsBalanceAndMerchants() throws Exception {
        MockHttpSession session = loginAda();

        mvc.perform(get("/ledger/pay").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"available-balance\"")))
                .andExpect(content().string(containsString("1000.00")))
                .andExpect(content().string(containsString("Café Orquídea")))
                .andExpect(content().string(containsString("dice-amount")))
                .andExpect(content().string(containsString("dice-label")))
                .andExpect(content().string(containsString("dice-merchant")));
    }

    @Test
    void payPostsToSelectedMerchantAndUpdatesAda() throws Exception {
        MockHttpSession session = loginAda();
        Merchant cafe = merchants.findAll().stream()
                .filter(m -> m.getName().contains("Café"))
                .findFirst()
                .orElseThrow();

        mvc.perform(post("/ledger/pay").session(session).with(csrf())
                        .param("amount", "45.00")
                        .param("label", "coffee")
                        .param("merchantId", String.valueOf(cafe.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ledger/me"));

        var ada = accounts.findByCode(ChartOfAccountsSeeder.ADA_DEPOSIT_CODE).orElseThrow();
        var cafeAccount = accounts.findByCode(ChartOfAccountsSeeder.CAFE_DEPOSIT_CODE).orElseThrow();
        assertThat(balances.posted(ada)).isEqualByComparingTo(new BigDecimal("955.00"));
        assertThat(balances.posted(cafeAccount)).isEqualByComparingTo(new BigDecimal("43.50"));
    }

    private MockHttpSession loginAda() throws Exception {
        MvcResult login = mvc.perform(formLogin("/login")
                        .user(CustomerSeeder.ADA_USERNAME)
                        .password(CustomerSeeder.DEV_PASSWORD))
                .andExpect(authenticated().withUsername(CustomerSeeder.ADA_USERNAME))
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
