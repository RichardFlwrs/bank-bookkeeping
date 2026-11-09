CREATE TABLE account (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    code VARCHAR(16) NOT NULL,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(20) NOT NULL
);

CREATE UNIQUE INDEX UNIQ_account_code ON account (code);

CREATE TABLE journal_entry (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    posted_at TEXT NOT NULL
);

CREATE TABLE journal_line (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    journal_entry_id INTEGER NOT NULL,
    account_id INTEGER NOT NULL,
    side VARCHAR(10) NOT NULL,
    amount TEXT NOT NULL,
    CONSTRAINT FK_journal_line_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entry (id) ON DELETE CASCADE,
    CONSTRAINT FK_journal_line_account FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE INDEX IDX_journal_line_entry ON journal_line (journal_entry_id);
CREATE INDEX IDX_journal_line_account ON journal_line (account_id);

CREATE TABLE customer (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name VARCHAR(160) NOT NULL,
    deposit_account_id INTEGER NOT NULL
);

CREATE INDEX IDX_customer_deposit_account ON customer (deposit_account_id);

CREATE TABLE merchant (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name VARCHAR(160) NOT NULL,
    deposit_account_id INTEGER NOT NULL
);

CREATE INDEX IDX_merchant_deposit_account ON merchant (deposit_account_id);

CREATE TABLE card (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    token VARCHAR(64) NOT NULL,
    customer_id INTEGER NOT NULL
);

CREATE UNIQUE INDEX UNIQ_card_token ON card (token);
CREATE INDEX IDX_card_customer ON card (customer_id);

CREATE TABLE purchase (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    public_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(120) DEFAULT NULL,
    card_id INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount TEXT NOT NULL,
    CONSTRAINT FK_purchase_card FOREIGN KEY (card_id) REFERENCES card (id)
);

CREATE UNIQUE INDEX UNIQ_purchase_public_id ON purchase (public_id);
CREATE UNIQUE INDEX UNIQ_purchase_trace_id ON purchase (trace_id);
CREATE UNIQUE INDEX UNIQ_purchase_idempotency_key ON purchase (idempotency_key);
CREATE INDEX IDX_purchase_card ON purchase (card_id);

CREATE TABLE authorization_hold (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    purchase_id INTEGER NOT NULL,
    amount TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    released_at TEXT DEFAULT NULL,
    CONSTRAINT FK_authorization_hold_purchase FOREIGN KEY (purchase_id) REFERENCES purchase (id)
);

CREATE UNIQUE INDEX UNIQ_authorization_hold_purchase ON authorization_hold (purchase_id);

CREATE TABLE clearing_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    purchase_id INTEGER NOT NULL,
    authorization_hold_id INTEGER NOT NULL,
    amount TEXT NOT NULL
);

CREATE UNIQUE INDEX UNIQ_clearing_item_purchase ON clearing_item (purchase_id);
CREATE UNIQUE INDEX UNIQ_clearing_item_hold ON clearing_item (authorization_hold_id);

CREATE TABLE settlement (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    purchase_id INTEGER NOT NULL,
    amount TEXT NOT NULL,
    fee_amount TEXT NOT NULL
);

CREATE UNIQUE INDEX UNIQ_settlement_purchase ON settlement (purchase_id);

CREATE TABLE audit_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    occurred_at TEXT NOT NULL,
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    aggregate_type TEXT NOT NULL,
    aggregate_id TEXT NOT NULL,
    purchase_id TEXT DEFAULT NULL,
    trace_id TEXT NOT NULL,
    before_json TEXT DEFAULT NULL,
    after_json TEXT NOT NULL
);

CREATE INDEX IDX_audit_event_trace ON audit_event (trace_id);
CREATE INDEX IDX_audit_event_purchase ON audit_event (purchase_id);
CREATE INDEX IDX_audit_event_actor ON audit_event (actor);
