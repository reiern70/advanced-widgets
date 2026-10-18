# Production deployment

The demo site runs on **Amazon Lightsail**: one Linux instance with Docker, where Caddy serves
HTTPS and passes the requests, websockets included, to Jetty 11 with the WAR, and PostgreSQL
keeps the users and the accesses. The domain is registered in Lightsail too.

| File | What it does |
|---|---|
| `Dockerfile` | Jetty 11 on Java 21 with the WAR as `ROOT.war`, websockets and `X-Forwarded-*` on |
| `compose.yaml` | Jetty, Caddy and PostgreSQL; the page store, the database and the certificates on volumes |
| `postgres-local.yaml` | PostgreSQL on a development machine, see below |
| `Caddyfile` | HTTPS for `$DOMAIN` (Let's Encrypt), `www.` redirected to the bare domain, the security headers (HSTS, `nosniff`, `Referrer-Policy`) and no `Server` |
| `.env.example` | The settings; copied to `.env` on the server, never committed |
| `setup-server.sh` | Prepares a fresh instance, once: Docker, swap, `/opt/antilia-widgets` |
| `deploy.sh` | Builds the WAR here and starts it on the instance: `deploy/deploy.sh ubuntu@<ip>` |

The WAR is built on the developer's machine, not on the server, because Wicket 11 is a snapshot
built locally. Each deployment uploads the WAR (about 55 MB, Hibernate and the JDBC drivers
included) and rebuilds the small image on the instance; `rsync` only sends what changed.

## Why Lightsail

| Requirement | Lightsail |
|---|---|
| About $20/month | $7 instance (1 GB) now; $22 with the managed PostgreSQL |
| PostgreSQL + backups | Managed PostgreSQL, $15/month: 1 GB RAM, 40 GB SSD, daily backups and 7 days of point-in-time restore included |
| Buy and manage domains in the site | *Domains & DNS* registers domains (through Route 53) and hosts the DNS zone for free |
| SSL / HTTPS | Caddy gets and renews Let's Encrypt certificates by itself, free |
| File system for Wicket's disk store | The instance's SSD (40 GB); the page store is a Docker volume on it, emptied at every start: pages last as long as the sessions, which Jetty keeps in memory |
| Web sockets | Nothing in between but Caddy, which passes them through |
| Publish with a script or Docker | `deploy.sh`, or any Docker image |
| Scale | Bigger instance from a snapshot in minutes (2 GB $12, 4 GB $24…); later a Lightsail load balancer ($18) with sticky sessions in front of several instances, or a move to EC2 |

Render, Railway and Fly.io deploy from Git more comfortably but sell no domains, and a Render
service with a disk can neither run two instances nor deploy without downtime. DigitalOcean
sells no domains either. A plain VPS (Hetzner) is cheaper but has no managed PostgreSQL.

## 1. Register

1. Create an AWS account at <https://portal.aws.amazon.com/billing/signup>: email, password,
   a credit card, a phone check; choose the *Basic* (free) support plan.
2. Secure it: in *IAM*, turn on MFA for the root user; then create an IAM user for daily work
   (`AdministratorAccess`, its own MFA) and sign in with that one from now on.
3. *Billing → Budgets*: a monthly budget of $25 that emails at 80 %.
4. Open Lightsail at <https://lightsail.aws.amazon.com>.

## 2. Domain

1. Lightsail → *Domains & DNS* → *Register domain*; pick the name. A `.com` costs about
   $15/year. Check the spelling: a wrong registration cannot be changed or refunded.
2. Lightsail creates the DNS zone by itself.

## 3. Instance

1. *Create instance*: region **Frankfurt** (or Virginia/Ohio for American users), platform
   *Linux*, blueprint *OS Only → Ubuntu 24.04 LTS*, plan **$7 (1 GB)**, name
   `antilia-widgets`. Download or upload an SSH key.
2. *Networking*: attach a **static IP**; in the IPv4 firewall add **HTTPS (443)** next to SSH
   (22) and HTTP (80). Restrict SSH to your own IP if it is fixed.
3. *Snapshots*: turn on **automatic snapshots** (daily, about $2/month).
4. *Domains & DNS* → the zone: an **A record** `@` → the static IP, and one for `www`.
5. From the repository:

   ```sh
   ssh ubuntu@<static-ip> 'sh -s' < deploy/setup-server.sh
   deploy/deploy.sh ubuntu@<static-ip>      # the first time it stops: fill in .env
   ssh ubuntu@<static-ip> nano /opt/antilia-widgets/.env
   deploy/deploy.sh ubuntu@<static-ip>
   ```

6. Open `https://<domain>`: the certificate is there within a minute, the home page shows the
   websocket status *Live*.

Logs: `ssh ubuntu@<ip> 'cd /opt/antilia-widgets && docker compose logs -f app'`.

The demos are private: visitors sign in to open them, the home page is public. Registering is
open once an SMTP server sends the confirmation emails, see section 5. The accounts are in the table `users`.
The demo always has `semedy`, `wicket` and `admin` (the administrator, who sees the users and the accesses on
`/admin`), with the passwords `.env` gives them in `ANTILIA_WIDGETS_SEMEDY_PASSWORD`,
`ANTILIA_WIDGETS_WICKET_PASSWORD` and `ANTILIA_WIDGETS_ADMIN_PASSWORD`: at every start an account is added when
missing and its password changed when `.env` gives another. Their passwords are never in the source. An account
without a password in `.env` is not added, and one still holding a password the source once gave it is locked.
Accounts for people such as a client go into `.env` as
`ANTILIA_WIDGETS_ACCOUNTS=name:password` (several separated by commas), then deploy again. The
accounts registered with earlier versions, in `/var/lib/antilia-widgets/accounts`, are copied into
the table at start.

### The administration's API

The resources below answer JSON to administrators, leaving out the fields without a value: requests
send the account and password of an administrator with HTTP Basic authentication
(`curl -u admin:...`), or, to read only, come from a browser signed in as one. After three wrong
passwords for an account they answer 429 until it signs in on the site.

| Request | What it does |
|---|---|
| `GET /api/admin/users?name=&email=&country=&page=0&size=100` | The accounts, the latest first, filtered as the table of the users |
| `GET /api/admin/users/<name>` | One account |
| `POST /api/admin/users` with `{"name", "password", "email", "role", "firstName", "lastName", "country", "language"}` | Adds an account that opens the demos at once; `name` and a strong `password` are needed |
| `DELETE /api/admin/users/<name>` | Removes an account, but an administrator's; its accesses stay |
| `GET /api/admin/accesses?user=&userLike=&part=public&address=&from=&to=&page=0&size=100` | The accesses, the latest first; `user` is exact, `from` and `to` are times as `2026-10-15T10:00:00Z` |
| `GET /api/admin/old-accesses?...` | The old accesses, filtered alike |

```sh
curl -u admin:... 'https://<domain>/api/admin/accesses?user=robot&size=10'
curl -u admin:... -H 'Content-Type: application/json' \
	-d '{"name": "client", "password": "...", "email": "client@example.com"}' https://<domain>/api/admin/users
curl -u admin:... -X DELETE https://<domain>/api/admin/users/client
```

The accesses show who: the account signed in with, `robot` for search engines and other
robots, `unknown` for the other visitors, and `user-<name>` for a visitor who tells who it is
with `?visitor=<name>` on any page, such as the checks of a developer with `?visitor=claude-dev`.
Every night at 03:30 UTC the accesses older than 10 days move from the table `accesses` to
`old_accesses`, keeping their ids, so that the table every session writes to stays small: each is
copied, then removed from `accesses` once it is in `old_accesses`, in one transaction. The
administration shows them in the tab *Old accesses*.

## 4. PostgreSQL

The application keeps its data with Spring Data JPA on Hibernate; Flyway makes the tables from
`demo-app/src/main/resources/db/migration` at start, and Hibernate only checks them. Three
settings, environment variables or system properties of the same meaning, choose the database:

| Variable | System property | Meaning |
|---|---|---|
| `DATABASE_URL` | `antilia-widgets.database.url` | JDBC URL, or `postgres://user:password@host:5432/db` |
| `DATABASE_USER` | `antilia-widgets.database.user` | The user, unless the URL has it |
| `DATABASE_PASSWORD` | `antilia-widgets.database.password` | The password, unless the URL has it |

Without them the demo keeps its data in an H2 file (PostgreSQL mode) in
`$ANTILIA_WIDGETS_DATA_FOLDER/antilia-widgets-db`, the temporary folder by default, so that it
runs without installing anything; the tests use H2 in memory.

### In production

`compose.yaml` runs **PostgreSQL 17 in a container** next to Jetty, on a volume of the instance's
SSD, reachable only by the application (no port is published). It costs nothing more. Its
settings are sized for the 2 GB instance next to a JVM of 1 GB: `shared_buffers` 192 MB,
`effective_cache_size` 512 MB, at most 20 connections (the application's pool uses 5); queries
slower than a second are logged. The logs of every container are rotated, 3 files of 10 MB.

Before the first deployment with it, add a password to `.env` on the server:

```sh
ssh ubuntu@<static-ip> "echo DATABASE_PASSWORD=$(openssl rand -hex 24) >> /opt/antilia-widgets/.env"
deploy/deploy.sh ubuntu@<static-ip>
```

Flyway makes the tables at the first start; the demo adds `semedy`, `wicket` and `admin`.

**Backups.** `deploy.sh` installs `/etc/cron.d/antilia-widgets`: every night at 03:30 (the
server's clock, UTC) `backup.sh` dumps the database to `/var/backups/antilia-widgets`, keeps it
once `pg_restore` reads it, and deletes dumps older than 14 days; it logs to `backup.log` there.
Those dumps are on the instance's disk: the **automatic snapshots** of the instance (Lightsail
console → the instance → *Snapshots* → *Automatic snapshots*, about $2/month) keep the disk too,
for 7 days. For a copy outside AWS, now and then:

```sh
scp 'ubuntu@<static-ip>:/var/backups/antilia-widgets/widgets-*.dump' .
```

To restore a dump:

```sh
ssh ubuntu@<static-ip> 'cd /opt/antilia-widgets && docker compose exec -T db sh -c '\''pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists'\'' < /var/backups/antilia-widgets/widgets-<date>.dump'
```

**Upgrading keeps the data.** The data in production is never thrown away: no volume is removed
and Flyway never cleans (`cleanDisabled`). The migrations in `db/migration` only add: new tables,
columns and indexes, and rows filled in from what is there; none drops a table or a column or
deletes rows, which `MigrationsTest` checks along with a database of the first version keeping
its users and accesses through all of them. A migration, once deployed, is never changed: a
change is a new one. Before starting the new version, `deploy.sh` dumps the database with
`backup.sh`, so the data from before its migrations can be restored as below.

**Going back.** Every deployment keeps the image that ran before as `antilia-widgets:previous`:

```sh
ssh ubuntu@<static-ip> 'cd /opt/antilia-widgets && docker tag antilia-widgets:previous antilia-widgets:latest && docker compose up -d --no-build app'
```

**Later, a managed database** ($15/month: daily backups and 7 days of point-in-time restore,
updates done by AWS): *Databases → Create database*, the same region, PostgreSQL 17, *Standard*,
name `widgets`, master user `widgets`, public mode off. Put its endpoint into `.env` as
`DATABASE_URL` (with `DATABASE_USER` and `DATABASE_PASSWORD`), move the data over with
`pg_dump | psql`, and deploy again; the `db` container may then be removed from `compose.yaml`.

### On a development machine

With Docker (the user in the `docker` group):

```sh
docker compose -f deploy/postgres-local.yaml up -d
```

or from Ubuntu's packages:

```sh
sudo apt install postgresql
sudo -u postgres psql -c "CREATE USER widgets PASSWORD 'widgets'" -c "CREATE DATABASE widgets OWNER widgets"
```

Then start the demo against it:

```sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/widgets DATABASE_USER=widgets DATABASE_PASSWORD=widgets
mvn -pl demo-app jetty:run -Dwicket.configuration=deployment -Dwicket.demo=true
```

## 5. Email

Visitors who register get an email with a link confirming their address, and open the demos only
after following it. The emails go over SMTP; without an SMTP server registering stays closed,
unless `-Dantilia-widgets.open-registration=true` opens it (locally `jetty:run` does, and the
emails go to the log, where the link is taken from). Set in `.env`:

| Environment variable | System property | |
|---|---|---|
| `SMTP_HOST` | `antilia-widgets.smtp.host` | The SMTP server, e.g. `email-smtp.eu-central-1.amazonaws.com` (Amazon SES) |
| `SMTP_PORT` | `antilia-widgets.smtp.port` | 587 (STARTTLS) if not set, or 465 (TLS) |
| `SMTP_USER` | `antilia-widgets.smtp.user` | The SMTP user, if the server asks for one |
| `SMTP_PASSWORD` | `antilia-widgets.smtp.password` | Its password |
| `MAIL_FROM` | `antilia-widgets.mail.from` | The sender, e.g. `Antilia Widgets <no-reply@antiliacode.com>`; the user if not set |
| `SITE_URL` | `antilia-widgets.site-url` | The address the links point at; `compose.yaml` sets `https://${DOMAIN}` |

`-Dantilia-widgets.open-registration=false` closes registering even with a server.
