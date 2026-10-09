# Production deployment

The demo site runs on **Amazon Lightsail**: one Linux instance with Docker, where Caddy serves
HTTPS and passes the requests, websockets included, to Jetty 11 with the WAR. The domain is
registered in Lightsail too, and PostgreSQL is a Lightsail managed database once the application
needs one.

| File | What it does |
|---|---|
| `Dockerfile` | Jetty 11 on Java 21 with the WAR as `ROOT.war`, websockets and `X-Forwarded-*` on |
| `compose.yaml` | Jetty and Caddy, the page store and the certificates on volumes |
| `Caddyfile` | HTTPS for `$DOMAIN` (Let's Encrypt), `www.` redirected to the bare domain |
| `.env.example` | The settings; copied to `.env` on the server, never committed |
| `setup-server.sh` | Prepares a fresh instance, once: Docker, swap, `/opt/antilia-widgets` |
| `deploy.sh` | Builds the WAR here and starts it on the instance: `deploy/deploy.sh ubuntu@<ip>` |

The WAR is built on the developer's machine, not on the server, because Wicket 11 is a snapshot
built locally. Each deployment uploads the WAR (about 17 MB) and rebuilds the small image on the
instance.

## Why Lightsail

| Requirement | Lightsail |
|---|---|
| About $20/month | $7 instance (1 GB) now; $22 with the managed PostgreSQL |
| PostgreSQL + backups | Managed PostgreSQL, $15/month: 1 GB RAM, 40 GB SSD, daily backups and 7 days of point-in-time restore included |
| Buy and manage domains in the site | *Domains & DNS* registers domains (through Route 53) and hosts the DNS zone for free |
| SSL / HTTPS | Caddy gets and renews Let's Encrypt certificates by itself, free |
| File system for Wicket's disk store | The instance's SSD (40 GB); the page store is a Docker volume on it |
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

## 4. PostgreSQL, once the application uses it

1. *Databases → Create database*: the same region, **PostgreSQL 17**, *Standard* $15, name
   `widgets`, master user `widgets`; keep the generated password.
2. Leave *public mode* off: only the instances of the account reach it.
3. Put its endpoint into `.env` as `DATABASE_URL` and deploy again.
4. Backups are on by themselves (daily, point-in-time for 7 days). A weekly `pg_dump` kept
   outside AWS protects against losing the account.

## 5. Showing the demo only after registering

Today every page is public. Hiding them until a visitor registers is application work (a login
or a confirmed registration in the session, checked by the pages); it needs the database, so it
comes with step 4.
