# Production deployment

The demo site runs on **Amazon Lightsail**: one Linux instance with Docker, where Caddy serves
HTTPS and passes the requests, websockets included, to Jetty 12 with the WAR. There is no
database: the site keeps no record of the people who visit it. The domain is registered in
Lightsail too.

| File | What it does |
|---|---|
| `Dockerfile` | Jetty 12 (ee10, Servlet 6) on Java 21 with the WAR unpacked as `webapps/ROOT`, owned by root, websockets and `X-Forwarded-*` on; the Jetty of `jetty:run` and of the tests |
| `.dockerignore` | Only `Dockerfile` and `ROOT.war` go to the build of the image, never `.env` |
| `compose.yaml` | Jetty and Caddy, each on a read-only file system, without capabilities and with a limit of memory; the page store, the temporary folder and the certificates on volumes |
| `Caddyfile` | HTTPS for `$DOMAIN` (Let's Encrypt), `www.` redirected to the bare domain, request bodies up to 25 MB, the client's address passed on in `X-Forwarded-For` alone, the security headers (HSTS for the subdomains too, `nosniff`, `Referrer-Policy`, `X-Frame-Options`, `Permissions-Policy`), no `Server`, and no access log |
| `.env.example` | The settings, without any password; copied to `.env` (mode 600) on the server, never committed |
| `setup-server.sh` | Prepares an instance: Docker, swap, `/opt/antilia-widgets`, the firewall `ufw`, SSH with keys only, security updates that restart when needed; runs again on an instance set up before |
| `deploy.sh` | Builds the WAR here, copies it with this folder to the instance and runs `start.sh` there: `deploy/deploy.sh ubuntu@<ip>` |
| `start.sh` | On the instance: checks `.env`, pulls and builds the images and starts them; deletes once what earlier versions kept, see section 4 |

The WAR is built on the developer's machine, not on the server, because Wicket 11 is a snapshot
built locally. Each deployment uploads the WAR and rebuilds the small image on the instance; `rsync` only sends what changed.

The images are pinned by their digests, the tag kept to be read: `jetty:12.1.14-jdk21@sha256:…`
in `Dockerfile` and `caddy:2.11.7@sha256:…` in `compose.yaml`. Dependabot
(`.github/dependabot.yml`) proposes the next ones every week as pull requests; every deployment
pulls the images again (`docker compose pull`, `docker compose build --pull`), so a merged update
goes online with the next deployment. To move one by hand, look its digest up and replace both:

```sh
docker buildx imagetools inspect caddy:2.11.10 | grep -m1 Digest
```

## Why Lightsail

| Requirement | Lightsail |
|---|---|
| About $20/month | $7 instance (1 GB) now, $12 with 2 GB |
| Buy and manage domains in the site | *Domains & DNS* registers domains (through Route 53) and hosts the DNS zone for free |
| SSL / HTTPS | Caddy gets and renews Let's Encrypt certificates by itself, free |
| File system for Wicket's disk store | The instance's SSD (40 GB); the page store is a Docker volume on it, emptied at every start: pages last as long as the sessions, which Jetty keeps in memory |
| Web sockets | Nothing in between but Caddy, which passes them through |
| Publish with a script or Docker | `deploy.sh`, or any Docker image |
| Scale | Bigger instance from a snapshot in minutes (2 GB $12, 4 GB $24…); later a Lightsail load balancer ($18) with sticky sessions in front of several instances, or a move to EC2 |

Render, Railway and Fly.io deploy from Git more comfortably but sell no domains, and a Render
service with a disk can neither run two instances nor deploy without downtime. DigitalOcean
sells no domains either. A plain VPS (Hetzner) is cheaper, but sells no domains either.

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

6. Open `https://<domain>`: the certificate is there within a minute, the home page invites to
   sign in as `user` and says how many sessions are in use.

`.env` is made from `.env.example` with `install -m 600`, readable by its owner alone from the
start, and holds no password until one is made up. `start.sh`, which `deploy.sh` runs on the
instance, checks it before touching anything, without printing any of its values, and stops when:

- a value is still `change-me` (the placeholder of earlier examples);
- the last `JAVA_OPTIONS` lack `-Dwicket.configuration=deployment`, or set another
  `wicket.configuration`: in DEVELOPMENT mode every demo is open to anonymous visitors and errors
  show their stack traces;
- they set `-Dantilia-widgets.sign-in-as` (every visitor signed in as that account) or
  `-Dantilia-widgets.open-demos`: switches for a developer's machine.

Logs: `ssh ubuntu@<ip> 'cd /opt/antilia-widgets && docker compose logs -f app'`.

**The instance.** Besides Lightsail's firewall, `setup-server.sh` turns on `ufw`, letting in only
SSH (22/tcp), HTTP (80/tcp) and HTTPS (443/tcp, and 443/udp for HTTP/3); it allows SSH before it
turns the firewall on, so the session running it goes on. Docker opens the ports it publishes, 80
and 443, past `ufw` by itself; no other container publishes a port. SSH takes keys only and no
root (`/etc/ssh/sshd_config.d/10-antilia-widgets.conf`: `PasswordAuthentication no`,
`PermitRootLogin no`), checked with `sshd -t` before SSH reloads, and set only while the user has a
key in `~/.ssh/authorized_keys`. The security updates install every day, and when one needs a
restart (a new kernel), the instance restarts at 04:00 UTC; the containers start again by
themselves.

**Headers, and no access log.** Every answer, the redirects of `www` too, carries HSTS for a
year with `includeSubDomains` (so every subdomain of the domain has to serve HTTPS),
`X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`,
`X-Frame-Options: SAMEORIGIN` (no framing by other sites; the application's CSP says
`frame-ancestors 'self'` too) and a `Permissions-Policy` that turns off the browser's features the
site does not use: camera, microphone, geolocation, payment, USB, serial, HID, MIDI, the sensors,
fullscreen, reading the clipboard and more. Writing to the clipboard, which the click-to-copy of
the demos does, stays allowed to the site's own pages. Caddy logs no requests, so that nothing
records who visited; its own messages, such as the renewal of a certificate, are in
`docker compose logs caddy`.

**The client's address.** Jetty takes the address of the client, the host and the scheme
(https) from the `X-Forwarded-For`, `-Host` and `-Proto` headers Caddy sets, replacing those a
client sends. Caddy drops the other headers of the kind that Jetty's `forwarded` module reads
(`Forwarded`, `X-Forwarded-Port`, `X-Forwarded-Server`, `X-Proxied-Https`, `Proxy-Auth-Cert`,
`Proxy-Ssl-Id`), so the address that the limits of the sign-in and of the requests count, in
memory only, is the one Caddy saw.

**Containers.** Each container runs on a read-only file system and writes only into its volumes
and a few small tmpfs; it has no capabilities of Linux (Caddy keeps `NET_BIND_SERVICE`, for the
ports 80 and 443), cannot gain privileges (`no-new-privileges`), and has a limit of memory sized
for the 2 GB instance: Jetty 1400 MB (a heap of 1 GB and what the JVM needs besides), Caddy
128 MB. Jetty runs as the user `jetty`, which owns neither Jetty's base
nor the web application (unpacked from the WAR into the image, owned by root), so it can change
neither even without the read-only file system. Its temporary folder, with the uploads and the
images of the rich text editors, is the volume `temporary`, on the disk rather than in memory.
Only `Dockerfile` and `ROOT.war` go to the build of the image (`.dockerignore`), never `.env`. The
settings of `.env` stay environment variables of the containers: Docker secrets would need the
application to read files, which is left for later.

**Memory.** Caddy answers 413 to a request body over 25 MB (the uploads of the demo take 20 MB at
most), so such a body never reaches Jetty. A JVM that runs out of memory ends
(`-XX:+ExitOnOutOfMemoryError`, set by the image in `JDK_JAVA_OPTIONS` whatever `JAVA_OPTIONS`
say), and Docker starts it again (`restart: unless-stopped`) instead of leaving it up but broken.

**Accounts and sessions.** The demos are private: visitors sign in to open them, the home page
is public. There are two accounts, kept in memory: `user`, shared by everybody, whose password is
in the source and shown on the sign-in page and the home page, and `admin`, the administrator,
whose password `.env` gives in `ANTILIA_WIDGETS_ADMIN_PASSWORD` (without one there is no
administrator). Every sign-in asks the letters of an image (a captcha, its letters carried in an
encrypted token, not in a session). After 10 failures an address is blocked for 30 minutes. At
most 10 sessions are signed in as `user` at once, each sure of its place for 30 minutes and then
giving it up to a newcomer; the pages say how many are in use, and `/admin` shows them to the
administrator, who takes no place and always signs in.

**Robots.** The pages a visitor sees without signing in are stateless and make no session, so
looking costs the server nothing to keep. Each address gets 600 requests at once and 10 more a
second; beyond, the application answers 429 with `Retry-After` (`RequestLimitFilter`). The
failures of the sign-in and the buckets of the requests are kept in memory only, by address, and
forgotten within 30 minutes.

## 4. Nothing kept

The site keeps no record of the people who visit it: no database, no registrations, no visits
recorded, no access log. Earlier versions kept the users and the accesses in PostgreSQL, dumped
every night to `/var/backups/antilia-widgets`. The first deployment of issue #174 deleted them for
good: `start.sh` removes the database's container (`docker compose up --remove-orphans`), its
volume `antilia-widgets_database`, the dumps with their nightly job (`/etc/cron.d/antilia-widgets`)
and the files they ran on; each step only when there is something left, so later deployments skip
them. `antilia-widgets:previous` of that deployment was the last image that needed the database:
going back to it would start without one.

`SITE_URL` (`antilia-widgets.site-url`, set by `compose.yaml` to `https://${DOMAIN}`) is the
address of the site, whose pages the websockets connect from.

## 5. One-time steps on the existing server

The settings that earlier versions read and this one does not can go from `.env` by hand:
`DATABASE_*`, `BACKUP_AGE_RECIPIENT`, `ANTILIA_WIDGETS_ACCOUNTS`, `ANTILIA_WIDGETS_SEMEDY_PASSWORD`
and `ANTILIA_WIDGETS_WICKET_PASSWORD`; left there they do nothing. `JAVA_OPTIONS` must still hold
`-Dwicket.configuration=deployment`, and no value may be `change-me`; `start.sh` refuses to deploy
otherwise. Check `.env` without showing the values:

```sh
ssh ubuntu@<static-ip> 'cd /opt/antilia-widgets && grep -v "^ *#" .env | grep -c change-me; grep -c "^ *JAVA_OPTIONS=.*-Dwicket.configuration=deployment" .env'
```

The first count must be 0, the second at least 1.
