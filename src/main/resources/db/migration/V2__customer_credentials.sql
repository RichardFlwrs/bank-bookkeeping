ALTER TABLE customer ADD COLUMN username VARCHAR(64);
ALTER TABLE customer ADD COLUMN password_hash VARCHAR(100);

CREATE UNIQUE INDEX UQ_customer_username ON customer (username);
