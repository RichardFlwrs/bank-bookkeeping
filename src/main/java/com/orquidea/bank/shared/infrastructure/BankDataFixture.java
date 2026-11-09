package com.orquidea.bank.shared.infrastructure;

import com.orquidea.bank.shared.infrastructure.seed.CardSeeder;
import com.orquidea.bank.shared.infrastructure.seed.ChartOfAccountsSeeder;
import com.orquidea.bank.shared.infrastructure.seed.CustomerSeeder;
import com.orquidea.bank.shared.infrastructure.seed.MerchantSeeder;
import com.orquidea.bank.shared.infrastructure.seed.OpeningDepositSeeder;
import com.orquidea.bank.shared.infrastructure.seed.SeedChart;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BankDataFixture {

    private final ChartOfAccountsSeeder chartOfAccountsSeeder;
    private final CustomerSeeder customerSeeder;
    private final MerchantSeeder merchantSeeder;
    private final OpeningDepositSeeder openingDepositSeeder;
    private final CardSeeder cardSeeder;

    public BankDataFixture(
            ChartOfAccountsSeeder chartOfAccountsSeeder,
            CustomerSeeder customerSeeder,
            MerchantSeeder merchantSeeder,
            OpeningDepositSeeder openingDepositSeeder,
            CardSeeder cardSeeder
    ) {
        this.chartOfAccountsSeeder = chartOfAccountsSeeder;
        this.customerSeeder = customerSeeder;
        this.merchantSeeder = merchantSeeder;
        this.openingDepositSeeder = openingDepositSeeder;
        this.cardSeeder = cardSeeder;
    }

    @Transactional
    public SeedChart seed(String traceId) {
        SeedChart chart = chartOfAccountsSeeder.seed(traceId);
        var customers = customerSeeder.seed(traceId, chart.customerDeposits());
        merchantSeeder.seed(traceId, chart.merchantDeposits());
        openingDepositSeeder.seed(traceId, chart.cash(), chart.customerDeposits());
        cardSeeder.seed(traceId, customers);
        return chart;
    }
}
