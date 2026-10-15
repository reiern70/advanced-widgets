-- The email of an account, apart from its name: a visitor who registered has the email as name
-- too, a configured account may have one or none. The emails registered so far are copied from
-- the names; nothing is removed.
-- Plain SQL that PostgreSQL and H2 (in its PostgreSQL mode) both run.

ALTER TABLE users ADD COLUMN email VARCHAR(254);

UPDATE users SET email = name WHERE name LIKE '%@%';

CREATE INDEX users_email ON users (email);
