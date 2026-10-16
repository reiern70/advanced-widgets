-- The accesses older than some days move here once a day (see Accesses.archive), so that the
-- table of the accesses, which every session writes to, stays small. The rows keep their ids.
-- Plain SQL that PostgreSQL and H2 (in its PostgreSQL mode) both run.

CREATE TABLE old_accesses (
	id BIGINT PRIMARY KEY,
	session_id VARCHAR(100),
	user_name VARCHAR(254) NOT NULL,
	private_part BOOLEAN NOT NULL,
	address VARCHAR(100),
	user_agent VARCHAR(500),
	accessed TIMESTAMP WITH TIME ZONE NOT NULL,
	time_zone VARCHAR(100),
	local_time TIMESTAMP
);

CREATE INDEX old_accesses_accessed ON old_accesses (accessed);
CREATE INDEX old_accesses_user_name ON old_accesses (user_name, accessed);
CREATE INDEX old_accesses_address ON old_accesses (address);
