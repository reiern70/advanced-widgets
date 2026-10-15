-- The accesses are filtered and sorted by the user and the address they came from.
-- Plain SQL that PostgreSQL and H2 (in its PostgreSQL mode) both run.

CREATE INDEX accesses_user_name ON accesses (user_name, accessed);
CREATE INDEX accesses_address ON accesses (address);
