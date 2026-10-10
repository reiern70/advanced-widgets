-- What a visitor who registers does: one of the JobRole values, and the free text of OTHER.
-- Plain SQL that PostgreSQL and H2 (in its PostgreSQL mode) both run.

ALTER TABLE users ADD COLUMN job_role VARCHAR(20);
ALTER TABLE users ADD COLUMN job_role_other VARCHAR(100);
