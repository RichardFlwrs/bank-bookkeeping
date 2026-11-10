package com.orquidea.bank.ledger.application;

import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.ledger.domain.AccountType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BalanceCalculator {

    private final JdbcTemplate jdbc;

    public BalanceCalculator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public BigDecimal posted(Account account) {
        if (account == null || account.getId() == null) {
            return money("0.00");
        }
        // ASSET is debit-normal. LIABILITY and INCOME are credit-normal.
        boolean assetStyle = account.getType() == AccountType.ASSET;
        String debitSign = assetStyle ? "+" : "-";
        String creditSign = assetStyle ? "-" : "+";
        BigDecimal raw = jdbc.queryForObject(
                """
                        SELECT COALESCE(SUM(CASE
                            WHEN side = 'DEBIT' THEN %samount
                            WHEN side = 'CREDIT' THEN %samount
                            ELSE 0
                        END), 0)
                        FROM journal_line
                        WHERE account_id = ?
                        """.formatted(debitSign, creditSign),
                BigDecimal.class,
                account.getId()
        );
        return money(raw);
    }

    public BigDecimal openHolds(Account account) {
        if (account == null || account.getId() == null) {
            return money("0.00");
        }
        BigDecimal raw = jdbc.queryForObject(
                """
                        SELECT COALESCE(SUM(h.amount), 0)
                        FROM authorization_hold h
                        JOIN purchase p ON p.id = h.purchase_id
                        JOIN card c ON c.id = p.card_id
                        JOIN customer cu ON cu.id = c.customer_id
                        WHERE cu.deposit_account_id = ?
                          AND h.status = 'OPEN'
                        """,
                BigDecimal.class,
                account.getId()
        );
        return money(raw);
    }

    public BigDecimal available(Account account) {
        return money(posted(account).subtract(openHolds(account)));
    }

    public String format(BigDecimal amount) {
        return money(amount).toPlainString();
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return new BigDecimal("0.00");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }
}
