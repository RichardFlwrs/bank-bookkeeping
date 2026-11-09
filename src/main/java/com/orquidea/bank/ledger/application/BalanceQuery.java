package com.orquidea.bank.ledger.application;

import com.orquidea.bank.ledger.api.dto.AccountBalanceResponse;
import com.orquidea.bank.ledger.api.dto.AccountDetailResponse;
import com.orquidea.bank.ledger.api.dto.JournalEntryResponse;
import com.orquidea.bank.ledger.api.dto.JournalLineResponse;
import com.orquidea.bank.ledger.domain.Account;
import com.orquidea.bank.ledger.domain.JournalEntry;
import com.orquidea.bank.ledger.domain.JournalLine;
import com.orquidea.bank.ledger.infrastructure.AccountRepository;
import com.orquidea.bank.ledger.infrastructure.JournalEntryRepository;
import com.orquidea.bank.shared.domain.ApiException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class BalanceQuery {

    private final AccountRepository accounts;
    private final JournalEntryRepository journalEntries;
    private final BalanceCalculator balances;

    public BalanceQuery(
            AccountRepository accounts,
            JournalEntryRepository journalEntries,
            BalanceCalculator balances
    ) {
        this.accounts = accounts;
        this.journalEntries = journalEntries;
        this.balances = balances;
    }

    @Transactional(readOnly = true)
    public List<AccountBalanceResponse> listAccounts() {
        return accounts.findAll(Sort.by("code")).stream()
                .map(this::toBalance)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountDetailResponse getAccount(String code) {
        Account account = accounts.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("account_not_found"));
        return toDetail(account);
    }

    @Transactional(readOnly = true)
    public AccountDetailResponse getAccountById(Integer accountId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> ApiException.notFound("account_not_found"));
        return toDetail(account);
    }

    private AccountDetailResponse toDetail(Account account) {
        List<JournalLineResponse> lines = journalEntries.findAll(Sort.by("id")).stream()
                .flatMap(entry -> entry.getLines().stream()
                        .filter(line -> line.getAccount().getId().equals(account.getId()))
                        .map(line -> toLine(entry, line)))
                .toList();
        return new AccountDetailResponse(
                account.getCode(),
                account.getName(),
                account.getType().name(),
                balances.format(balances.posted(account)),
                balances.format(balances.available(account)),
                lines
        );
    }

    @Transactional(readOnly = true)
    public List<JournalEntryResponse> listJournal() {
        List<JournalEntry> entries = new ArrayList<>(journalEntries.findAll(Sort.by(Sort.Direction.ASC, "id")));
        return entries.stream()
                .map(entry -> new JournalEntryResponse(
                        entry.getId(),
                        entry.getPostedAt() == null ? null : entry.getPostedAt().toString(),
                        entry.getLines().stream()
                                .sorted(Comparator.comparing(JournalLine::getId, Comparator.nullsLast(Integer::compareTo)))
                                .map(line -> toLine(entry, line))
                                .toList()
                ))
                .toList();
    }

    private AccountBalanceResponse toBalance(Account account) {
        return new AccountBalanceResponse(
                account.getCode(),
                account.getName(),
                account.getType().name(),
                balances.format(balances.posted(account)),
                balances.format(balances.available(account))
        );
    }

    private JournalLineResponse toLine(JournalEntry entry, JournalLine line) {
        return new JournalLineResponse(
                entry.getId(),
                line.getSide().name(),
                balances.format(line.getAmount()),
                line.getAccount().getCode()
        );
    }
}
