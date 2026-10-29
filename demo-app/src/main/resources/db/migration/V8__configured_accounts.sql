-- Whether an account is one of those the server is started with (ANTILIA_WIDGETS_ACCOUNTS): at
-- every start they get the passwords the setting gives, and those it no longer lists are locked,
-- never deleted. The accounts are marked at the first start after this migration.
-- Plain SQL that PostgreSQL and H2 (in its PostgreSQL mode) both run.

ALTER TABLE users ADD COLUMN configured BOOLEAN DEFAULT FALSE NOT NULL;
