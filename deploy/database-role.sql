-- The role the application logs in to PostgreSQL with: no superuser, it can create neither
-- databases nor roles, and it owns the schema public with its tables and sequences, so that
-- Flyway's migrations still run. The superuser the image creates (POSTGRES_USER) keeps the
-- database itself, and makes the backups.
--
-- PostgreSQL runs this file when it creates the database (compose.yaml mounts it into
-- /docker-entrypoint-initdb.d), and start.sh runs it again before every start of the
-- application, on the database as it is: so it only adds. It makes the role when it is missing,
-- gives it the password of .env, and hands it what in the schema public it does not own yet, one
-- table and one sequence at a time (REASSIGN OWNED cannot take things from the superuser the
-- image made). It never drops, deletes or empties anything.
--
-- The role's name and password come from the environment of the container of the database,
-- DATABASE_APP_USER (widgets_app unless set) and DATABASE_APP_PASSWORD.

\set ON_ERROR_STOP on
\getenv app_user DATABASE_APP_USER
\getenv app_password DATABASE_APP_PASSWORD
\if :{?app_user}
\else
\set app_user widgets_app
\endif
\if :{?app_password}
\else
\set app_password ''
\endif

SELECT :'app_password' = '' AS no_password, :'app_user' = current_user AS same_user,
	current_database() AS db_name \gset
\if :no_password
DO $$ BEGIN RAISE EXCEPTION 'DATABASE_APP_PASSWORD is empty: set it in .env'; END $$;
\endif
\if :same_user
DO $$ BEGIN RAISE EXCEPTION 'DATABASE_APP_USER must not be the superuser POSTGRES_USER'; END $$;
\endif

SELECT format('CREATE ROLE %I', :'app_user')
	WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'app_user') \gexec
ALTER ROLE :"app_user" WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS
	PASSWORD :'app_password';
GRANT CONNECT, TEMPORARY ON DATABASE :"db_name" TO :"app_user";

ALTER SCHEMA public OWNER TO :"app_user";

-- the tables, views and the like, with their indexes and the sequences of their identity columns
SELECT format('ALTER %s %s OWNER TO %I',
		CASE c.relkind
			WHEN 'v' THEN 'VIEW'
			WHEN 'm' THEN 'MATERIALIZED VIEW'
			WHEN 'f' THEN 'FOREIGN TABLE'
			ELSE 'TABLE'
		END, c.oid::regclass, :'app_user')
	FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
	WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p', 'v', 'm', 'f')
		AND pg_get_userbyid(c.relowner) <> :'app_user'
	ORDER BY c.oid \gexec

-- the sequences of their own, which no table took along
SELECT format('ALTER SEQUENCE %s OWNER TO %I', c.oid::regclass, :'app_user')
	FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
	WHERE n.nspname = 'public' AND c.relkind = 'S'
		AND pg_get_userbyid(c.relowner) <> :'app_user'
	ORDER BY c.oid \gexec
