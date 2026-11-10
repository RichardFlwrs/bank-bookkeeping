ALTER TABLE customer ADD COLUMN username VARCHAR(64) NULL;
ALTER TABLE customer ADD COLUMN password_hash VARCHAR(100) NULL;

CREATE UNIQUE INDEX UQ_customer_username ON customer (username);
