CREATE TABLE account (
    id INT NOT NULL AUTO_INCREMENT,
    code VARCHAR(16) NOT NULL,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX UNIQ_account_code ON account (code);

CREATE TABLE journal_entry (
    id INT NOT NULL AUTO_INCREMENT,
    posted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journal_line (
    id INT NOT NULL AUTO_INCREMENT,
    journal_entry_id INT NOT NULL,
    account_id INT NOT NULL,
    side VARCHAR(10) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_journal_line_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entry (id) ON DELETE CASCADE,
    CONSTRAINT FK_journal_line_account FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IDX_journal_line_entry ON journal_line (journal_entry_id);
CREATE INDEX IDX_journal_line_account ON journal_line (account_id);

CREATE TABLE customer (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    deposit_account_id INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IDX_customer_deposit_account ON customer (deposit_account_id);

CREATE TABLE merchant (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    deposit_account_id INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IDX_merchant_deposit_account ON merchant (deposit_account_id);

CREATE TABLE card (
    id INT NOT NULL AUTO_INCREMENT,
    token VARCHAR(64) NOT NULL,
    customer_id INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX UNIQ_card_token ON card (token);
CREATE INDEX IDX_card_customer ON card (customer_id);

CREATE TABLE purchase (
    id INT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(120) DEFAULT NULL,
    card_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_purchase_card FOREIGN KEY (card_id) REFERENCES card (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX UNIQ_purchase_public_id ON purchase (public_id);
CREATE UNIQUE INDEX UNIQ_purchase_trace_id ON purchase (trace_id);
CREATE UNIQUE INDEX UNIQ_purchase_idempotency_key ON purchase (idempotency_key);
CREATE INDEX IDX_purchase_card ON purchase (card_id);

CREATE TABLE authorization_hold (
    id INT NOT NULL AUTO_INCREMENT,
    purchase_id INT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    released_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_authorization_hold_purchase FOREIGN KEY (purchase_id) REFERENCES purchase (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX UNIQ_authorization_hold_purchase ON authorization_hold (purchase_id);

CREATE TABLE clearing_item (
    id INT NOT NULL AUTO_INCREMENT,
    purchase_id INT NOT NULL,
    authorization_hold_id INT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX UNIQ_clearing_item_purchase ON clearing_item (purchase_id);
CREATE UNIQUE INDEX UNIQ_clearing_item_hold ON clearing_item (authorization_hold_id);

CREATE TABLE settlement (
    id INT NOT NULL AUTO_INCREMENT,
    purchase_id INT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    fee_amount DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX UNIQ_settlement_purchase ON settlement (purchase_id);

CREATE TABLE audit_event (
    id INT NOT NULL AUTO_INCREMENT,
    occurred_at DATETIME(6) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    action VARCHAR(40) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    purchase_id VARCHAR(64) DEFAULT NULL,
    trace_id VARCHAR(64) NOT NULL,
    before_json VARCHAR(4000) DEFAULT NULL,
    after_json VARCHAR(4000) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IDX_audit_event_trace ON audit_event (trace_id);
CREATE INDEX IDX_audit_event_purchase ON audit_event (purchase_id);
CREATE INDEX IDX_audit_event_actor ON audit_event (actor);
