-- What a visitor tells when registering, and whether the email was confirmed.
-- Plain SQL that PostgreSQL and H2 (in its PostgreSQL mode) both run.

ALTER TABLE users ADD COLUMN first_name VARCHAR(60);
ALTER TABLE users ADD COLUMN last_name VARCHAR(60);
-- ISO 3166 code
ALTER TABLE users ADD COLUMN country VARCHAR(2);
-- the language the emails are written in: en, es or de
ALTER TABLE users ADD COLUMN language VARCHAR(5);
-- whether the email was confirmed; registered visitors open the demos only after it
ALTER TABLE users ADD COLUMN validated BOOLEAN DEFAULT FALSE NOT NULL;
-- the SHA-256, in hex, of the token in the link of the confirmation email, and until when it works
ALTER TABLE users ADD COLUMN validation_token VARCHAR(64);
ALTER TABLE users ADD COLUMN validation_expires TIMESTAMP WITH TIME ZONE;

-- the accounts made before were configured ones or registered without confirming: all trusted
UPDATE users SET validated = TRUE;

CREATE UNIQUE INDEX users_validation_token ON users (validation_token);
CREATE INDEX users_country ON users (country);
