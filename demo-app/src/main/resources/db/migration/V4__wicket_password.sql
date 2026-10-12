-- The account wicket gets the password wicket4321. The defaults are added only when missing and
-- never changed, so the databases that have it already get the new hash here.
-- Plain SQL that PostgreSQL and H2 (in its PostgreSQL mode) both run.

UPDATE users
SET password_hash = '6Fi23jr/izCqXM4Bn7yKxQ==:sLba9BDFyxARONbZ1H5Vi2KadTleKeMw/qqtJwg728M='
WHERE name = 'wicket';
