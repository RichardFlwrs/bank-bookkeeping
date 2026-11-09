# Bank Bookkeeping

A small Spring Boot backend that behaves like one bank's books. It is complex enough to show a card purchase move through authorization, clearing, settlement, and posting. It is small enough to read in a debugger.

Money enters only through seeders. Each seeder writes the ledger and the audit table. A purchase endpoint then spends some of that money and records every mutation.

This document names the models, the folders, the audit table, and the endpoints. Column-level props of the domain models come later. The audit table is declared here.

## What this simulates

One bank. The customer and the merchant both hold accounts here, so a card purchase never leaves the process. There is no Visa, no second bank, and no network call. The stages still happen in the real order, and each stage commits before the next one starts. A breakpoint between stages sees rows that are already in the database.

A balance is not a column that gets overwritten. It is the sum of journal lines.

- **Posted balance** is the sum of posted journal lines on an account.
- **Available balance** is the posted balance minus open authorization holds.
- **Posting** is the moment those journal lines become part of the official balance.

From the bank's point of view a customer deposit is a liability: the bank owes that money to the customer. Adding money with a seeder debits Cash and credits the customer's deposit account. A purchase debits the customer and credits the merchant, and a small interchange fee stays with the bank.

## Stack

Same shape as the Orquídea RSVP API.

- Java 17, Spring Boot 3.5
- Spring Web, Validation, Data JPA, Flyway, Security
- SQLite at `./var/bank_dev.db` (one writer is enough for a single debug session)
- springdoc-openapi at `/swagger-ui.html`
- Thymeleaf pages: `/ledger/accounts` (chart), `/login`, `/ledger/me` (signed-in cardholder)
- Session login for cardholders. The JSON `/api/**` routes stay open for the debugger.

Package root: `com.orquidea.bank`. `BankApplication` stays at that root for component scanning.

## Package layout (DDD)

Code is organized by bounded context, then by layer (`domain`, `application`, `infrastructure`, `api`). Empty layers are omitted.

| Context | Responsibility | Packages |
|---------|----------------|----------|
| `ledger` | Chart of accounts, journal, posted balances | `ledger.domain`, `ledger.application`, `ledger.infrastructure`, `ledger.api` |
| `customer` | The person who holds a deposit account and a card | `customer.domain`, `customer.application`, `customer.infrastructure`, `customer.api` |
| `merchant` | The shop that gets paid | `merchant.domain`, `merchant.infrastructure` |
| `card` | The card, the hold, and the purchase walk | `card.domain`, `card.application`, `card.infrastructure`, `card.api` |
| `settlement` | Clearing and the suspense-to-merchant movement | `settlement.domain`, `settlement.application`, `settlement.infrastructure` |
| `audit` | Append-only record of every mutation, including seeders | `audit.domain`, `audit.application`, `audit.infrastructure`, `audit.api` |
| `shared` | API errors, the seeder runner | `shared.domain`, `shared.infrastructure` |

```
com.orquidea.bank
├── BankApplication
├── ledger
│   ├── domain          Account, JournalEntry, JournalLine, AccountType
│   ├── application     LedgerService, BalanceQuery
│   ├── infrastructure  AccountRepository, JournalEntryRepository
│   └── api             LedgerController, AccountsPageController
├── customer
│   ├── domain          Customer
│   ├── application     CardholderUserDetailsService, CardholderPrincipal
│   ├── infrastructure  CustomerRepository
│   └── api             LoginPageController, MyLedgerPageController
├── merchant
│   ├── domain          Merchant
│   └── infrastructure  MerchantRepository
├── card
│   ├── domain          Card, AuthorizationHold, Purchase, PurchaseStatus
│   ├── application     PurchaseService
│   ├── infrastructure  CardRepository, AuthorizationHoldRepository, PurchaseRepository
│   └── api             PurchaseController
├── settlement
│   ├── domain          ClearingItem, Settlement
│   ├── application     SettlementService
│   └── infrastructure  ClearingItemRepository, SettlementRepository
├── audit
│   ├── domain          AuditEvent, AuditAction
│   ├── application     AuditRecorder
│   ├── infrastructure  AuditEventRepository
│   └── api             AuditController
└── shared
    ├── domain          ApiException
    └── infrastructure  SecurityConfig, DevDataSeeder, BankDataFixture, BankSeedProperties, seeders
```


`PurchaseService` is the only type that walks the whole purchase. It calls `card`, `settlement`, `ledger`, and `audit` in order. Contexts do not reach into each other's tables.

## Models

Names only. Props are a later task. Relationships that the purchase depends on are fixed now.

### ledger

| Model | Role |
|-------|------|
| `Account` | One bookkeeping account. Seeded accounts are listed below. |
| `AccountType` | `ASSET`, `LIABILITY`, `INCOME`. |
| `JournalEntry` | One balanced posting. The lines inside it commit together. |
| `JournalLine` | One side of an entry, tied to one `Account`. A debit or a credit. |

An entry is valid only when its debits equal its credits. `LedgerService` rejects anything else. Nothing outside `LedgerService` inserts a `JournalLine`.

Seeded chart (defaults: 1000 customers, 50 merchants):

| Code | Name | Type | Who uses it |
|------|------|------|-------------|
| `1000` | Cash | `ASSET` | The bank's cash. Opening deposits debit this. |
| `2100` | Customer Deposits — Ada Lovelace | `LIABILITY` | Debugger fixture. First customer. |
| `3001`–`3999` | Customer Deposits — … | `LIABILITY` | Remaining customer deposits. |
| `2200` | Merchant Deposits — Café Orquídea | `LIABILITY` | Debugger fixture. First merchant. |
| `5001`–`5049` | Merchant Deposits — … | `LIABILITY` | Remaining merchant deposits. |
| `2300` | Settlement Suspense | `LIABILITY` | Holds the purchase amount between posting the customer and paying the merchant. |
| `4100` | Interchange Income | `INCOME` | The fee the bank keeps. |

### customer

| Model | Role |
|-------|------|
| `Customer` | A deposit-account holder with a login. First seeded row is Ada Lovelace (`2100`, username `ada`). Password hash is BCrypt; never written to audit. |

### merchant

| Model | Role |
|-------|------|
| `Merchant` | A shop that gets paid. First seeded row is Café Orquídea (`2200`). |

### card

| Model | Role |
|-------|------|
| `Card` | Ada's card. The purchase endpoint addresses it by its public token, not by a raw number. |
| `AuthorizationHold` | A reservation against available balance. Open during the purchase, released when the customer is posted. Does not write the journal. |
| `Purchase` | The walk itself. Its status is the debugger's progress marker. |
| `PurchaseStatus` | `REQUESTED`, `AUTHORIZED`, `CLEARED`, `SETTLED`, `POSTED`. |

### settlement

| Model | Role |
|-------|------|
| `ClearingItem` | The merchant's confirmation of the authorized amount. Links the hold to the purchase. |
| `Settlement` | The movement from `2300` out to the merchant and to fee income. |

### audit

Declared in [Audit table](#audit-table). One model: `AuditEvent`.

## How money is recorded

**Opening deposit (seeder), 1,000.00 MXN.** The bank receives cash and now owes Ada that amount.

| Account | Debit | Credit |
|---------|------:|-------:|
| `1000` Cash | 1,000.00 | |
| `2100` Customer Deposits — Ada | | 1,000.00 |

**Purchase, 45.00 MXN, fee 1.50 MXN.** Two entries, so the debugger can see the money sit in suspense.

Customer posting (hold is released in the same transaction):

| Account | Debit | Credit |
|---------|------:|-------:|
| `2100` Customer Deposits — Ada | 45.00 | |
| `2300` Settlement Suspense | | 45.00 |

Merchant settlement:

| Account | Debit | Credit |
|---------|------:|-------:|
| `2300` Settlement Suspense | 45.00 | |
| `2200` Merchant Deposits — Café Orquídea | | 43.50 |
| `4100` Interchange Income | | 1.50 |

After both entries, suspense is zero. Ada's posted balance is 955.00. The shop is owed 43.50. The bank has earned 1.50. Cash is unchanged by the purchase, because both parties bank here: the purchase moved a liability, it did not move cash.

Available balance equals posted balance while no hold is open. Between authorize and customer posting, Ada's available balance is posted balance minus the open hold.

## Seeders

`DevDataSeeder` runs on startup when the chart is empty, same idea as the RSVP `CommandLineRunner`. If the books already exist, it backfills customer usernames and password hashes that are still null (so an older `./var/bank_dev.db` gets logins on restart). Profile `test` does not run it. Deleting `./var/bank_dev.db` and starting again reloads the fixture.

Configured by `app.bank-seed` (defaults: `customers=1000`, `merchants=50`, `opening-deposit=1000.00`, `seed=42`). The test profile uses `customers=5` and `merchants=2`. Ada Lovelace (`2100`, card token `card_ada_orquidea`, username `ada`, password `orquidea`) and Café Orquídea (`2200`) are always the first rows so the purchase curl and the login page still work. Other customers get username `c` plus their deposit code (for example `c3001`) and the same password.

Every seeder calls `AuditRecorder`. A seed that wrote accounts or journal lines and wrote no audit row is a bug. Customer seed and credential backfill record `username` in the snapshot and never the password hash.

| Seeder | What it creates | Audit action |
|--------|-----------------|--------------|
| `ChartOfAccountsSeeder` | Cash, suspense, income, plus one deposit account per customer and merchant | `SEED_ACCOUNT` per account |
| `CustomerSeeder` | One customer per deposit account (Ada first), with username and password hash | `SEED_CUSTOMER` |
| `MerchantSeeder` | One merchant per deposit account (Café first) | `SEED_MERCHANT` |
| `OpeningDepositSeeder` | One `1,000.00` journal entry per customer (debit Cash, credit that deposit) | `SEED_DEPOSIT` |
| `CardSeeder` | One card per customer (Ada's token `card_ada_orquidea`) | `SEED_CARD` |

`actor` on those rows is the seeder name, for example `seeder:opening-deposit`. `purchase_id` is null. One shared `trace_id` groups the whole seed run. After a full default seed, Cash posted balance is `1,000,000.00`.

Fee rule for the fixture: 1.50 MXN on a purchase of 40.00 or more, otherwise 0.00. The purchase body does not choose the fee.

## Purchase walk

`POST /api/purchases` runs all four stages inside `PurchaseService`. Each stage is its own method and its own transaction, in this order:

1. `authorize` — insert `Purchase` (`REQUESTED`, then `AUTHORIZED`) and an open `AuthorizationHold`. Journal unchanged. Available balance drops.
2. `clear` — insert `ClearingItem`. Status `CLEARED`. Journal still unchanged.
3. `postCustomer` — journal entry from `2100` to `2300`, release the hold, status `SETTLED`. Posted balance drops. Suspense holds the amount.
4. `settleMerchant` — journal entry from `2300` to `2200` and `4100`, insert `Settlement`, status `POSTED`. Suspense returns to zero.

`PurchaseStatus` uses `SETTLED` for "the customer is posted and the amount sits in suspense", then `POSTED` for "the merchant has been paid". The words match the two journal entries above.

Each method calls `AuditRecorder.record(...)` before it returns, with a snapshot of the rows it changed. Because the transaction commits at the end of the method, a breakpoint on the next method sees the previous stage in SQLite, and so does `GET /api/audit?traceId=...`.

The response body is the trail. Clients do not have to query after the fact, and the debugger can still stop on each method.

```json
{
  "purchaseId": "pur_...",
  "traceId": "trc_...",
  "status": "POSTED",
  "steps": [
    { "stage": "authorize", "purchaseStatus": "AUTHORIZED", "available": "955.00", "posted": "1000.00" },
    { "stage": "clear", "purchaseStatus": "CLEARED", "available": "955.00", "posted": "1000.00" },
    { "stage": "postCustomer", "purchaseStatus": "SETTLED", "available": "955.00", "posted": "955.00" },
    { "stage": "settleMerchant", "purchaseStatus": "POSTED", "available": "955.00", "posted": "955.00" }
  ]
}
```

Amounts in that example assume the seeded 1,000.00 and a 45.00 purchase. `available` and `posted` are Ada's deposit account after that stage.

Put breakpoints on the four methods. Step over a stage, then call the read endpoints (or look at SQLite) before continuing.

## Audit table

Append-only. No updates and no deletes, including from seeders. `AuditRecorder` is the only writer.

Table: `audit_event`

| Column | Type | Notes |
|--------|------|-------|
| `id` | integer, primary key | |
| `occurred_at` | text, ISO-8601 UTC | When the mutation committed. |
| `actor` | text | `seeder:chart-of-accounts`, `seeder:customer`, `seeder:customer-credentials`, `seeder:merchant`, `seeder:opening-deposit`, `seeder:card`, or `purchase`. |
| `action` | text | One `AuditAction`: `SEED_ACCOUNT`, `SEED_CUSTOMER`, `SEED_MERCHANT`, `SEED_DEPOSIT`, `SEED_CARD`, `PURCHASE_AUTHORIZED`, `PURCHASE_CLEARED`, `PURCHASE_CUSTOMER_POSTED`, `PURCHASE_SETTLED`. |
| `aggregate_type` | text | Model name: `Account`, `Customer`, `Merchant`, `JournalEntry`, `Card`, `Purchase`, `AuthorizationHold`, `ClearingItem`, `Settlement`. |
| `aggregate_id` | text | Id of that row. |
| `purchase_id` | text, nullable | Set for purchase stages. Null for seed rows. |
| `trace_id` | text | Groups one seed run, or one purchase. |
| `before_json` | text, nullable | Snapshot before the write. Null when the row is being created. |
| `after_json` | text | Snapshot after the write. |

A purchase of one card produces four purchase audit rows that share a `trace_id`, plus any extra rows when a stage writes more than one aggregate (the journal entry and the hold release are two rows inside `PURCHASE_CUSTOMER_POSTED`'s transaction, same `trace_id`).

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/purchases` | Run authorize, clear, post customer, settle merchant. Body: `{ "cardToken": "card_ada_orquidea", "amount": "45.00" }`. Returns the trail above. |
| `GET` | `/api/purchases/{purchaseId}` | The purchase, its hold, its clearing item, its settlement. |
| `GET` | `/api/ledger/accounts` | Every account with posted balance and available balance. |
| `GET` | `/ledger/accounts` | Thymeleaf page of that same list: code, name, type, posted, available. |
| `GET` | `/login` | Cardholder login form. |
| `POST` | `/login` | Session form login. Username and password. Success goes to `/ledger/me`. |
| `POST` | `/logout` | End the session. |
| `GET` | `/ledger/me` | Signed-in cardholder's deposit account and journal lines. Session only. |
| `GET` | `/api/ledger/accounts/{code}` | One account, `code` such as `2100`, plus its journal lines. |
| `GET` | `/api/ledger/journal` | Every journal entry and its lines, oldest first. |
| `GET` | `/api/audit` | Audit rows. Filters: `traceId`, `purchaseId`, `actor`. |

Declined cases, so the books can be checked when a purchase must not post:

| Case | Result |
|------|--------|
| Unknown `cardToken` | `404`, no rows written |
| Amount above available balance | `422`, purchase stays unwritten, journal untouched |
| Replay of the same `Idempotency-Key` header | The original trail again, no second hold and no second posting |

### Payment limbo (later)

Today `POST /api/purchases` runs the whole walk in one request. Real payments often sit between stages. Controller comments mark those as future work; short summary:

| Endpoint | Today | Later candidates |
|----------|--------|------------------|
| `POST /api/purchases` | Authorize, clear, post customer, settle merchant in one call. Available differs from posted only while a hold is open inside that walk. | Auth waiting for merchant capture; estimated amounts (hotels, fuel, tips); clearing/settlement batch lag; merchant delay; network/bank pending; fraud/review pause; auth expires unused (hold drops, no post). |
| `GET /api/purchases/{purchaseId}` | Purchase plus hold, clearing item, and settlement after the walk. | Mid-walk limbo status: `AUTHORIZED` / `CLEARED` with no customer posting yet, expired holds, paused reviews. |
| `GET /api/ledger/accounts` | Posted = journal lines; available = posted minus open holds. | Keep holds open across requests so the chart shows available lagging posted for longer than one purchase. |
| `GET /ledger/accounts` | Same chart as the JSON list, for the debugger. | Same as `/api/ledger/accounts` when delayed capture/settle keeps holds open. |
| `GET /api/ledger/accounts/{code}` | One account and its posted journal lines. Holds only change available. | Pending/authorized activity alongside posted lines. |
| `GET /api/ledger/journal` | Posted journal entries only. | Auth holds, uncleared captures, and expired unused auths stay out until those cases write (or omit) journal rows. |
| `GET /ledger/me` | Cardholder posted, available, and posted journal lines. | Open holds and mid-walk purchases on the page (available &lt; posted, or pending items that are not lines yet). |

## Run

```bash
./mvnw spring-boot:run
```

Or with Docker (SQLite file at `./var/bank_dev.db`):

```bash
docker compose up --build -d
```

Swagger: `http://localhost:8086/swagger-ui.html`. Accounts page: `http://localhost:8086/ledger/accounts`. Login: `http://localhost:8086/login` (`ada` / `orquidea`).

After startup, `GET /api/ledger/accounts/2100` shows a posted balance of 1,000.00 and `GET /api/audit?actor=seeder:opening-deposit` shows the seed row. Then:

```bash
curl -s -X POST http://localhost:8086/api/purchases \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: coffee-1' \
  -d '{"cardToken":"card_ada_orquidea","amount":"45.00"}'
```

Follow `traceId` with `GET /api/audit?traceId=...` and re-read account `2100`, `2200`, `2300`, and `4100`.

### Postman

Import [`postman/Bank.postman_collection.json`](postman/Bank.postman_collection.json) and [`postman/local.postman_environment.json`](postman/local.postman_environment.json). The collection is generated from Springdoc OpenAPI. To regenerate (API must be running):

```bash
./scripts/generate-postman.sh
```

To load the fixture again, stop the app, delete `./var/bank_dev.db`, and start it again.

## Out of scope

Other banks, card networks, cash moving in or out after the seed, interest, loans, chargebacks, end-of-day batch, merchant or staff logins, locking down `/api/**`, and the payment-limbo cases listed under [Payment limbo (later)](#payment-limbo-later). Those belong to a later spec if the cardholder walk above is already visible.
