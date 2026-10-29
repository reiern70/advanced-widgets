# The demo application on Jetty 12, in its Jakarta EE 10 environment (ee10, Servlet 6), and Java
# 21: the Jetty of jetty:run and of the tests. deploy.sh builds the WAR on the developer's machine
# (Wicket 11 is a snapshot built there) and copies it next to this file as ROOT.war. Jetty 12.1
# comes with a JDK only, there is no image of it with a JRE. The image is pinned by its digest, so
# a build takes the same one until Dependabot proposes the next (see .github/dependabot.yml).
FROM jetty:12.1.14-jdk21@sha256:c84e5d8ac021fe32b34382c40dd35846f9eb3beff36b76287607cd2bf57aaa73 AS base

# The WAR unpacked here, so that Jetty serves the web application from a folder of the image it
# cannot change, rather than unpacking it into the temporary folder at every start
FROM base AS unpacked
COPY ROOT.war /tmp/ROOT.war
RUN mkdir /tmp/ROOT && cd /tmp/ROOT && jar -xf /tmp/ROOT.war

FROM base

# the web applications of webapps deployed as ee10 web applications; websockets for Wicket's
# native websockets; X-Forwarded-* from Caddy, so Wicket sees https; no version of Jetty in the
# Server header and on its error pages
RUN java -jar "$JETTY_HOME/start.jar" --add-modules=ee10-deploy,ee10-websocket-jakarta,forwarded \
	&& echo "jetty.httpConfig.sendServerVersion=false" > "$JETTY_BASE/start.d/server-version.ini"

# Jetty's base and the web application belong to root, so that the user jetty, which runs Jetty,
# can change neither the configuration nor the code; compose.yaml makes the whole file system
# read-only besides. The Wicket page store and the accounts registered to the demos are on a
# volume of their own on the instance's SSD (see compose.yaml); the page store is emptied at
# every start, its pages last as long as the sessions, which Jetty keeps in memory (no
# session-store module). The command line Jetty starts with, jetty.start, is made at every start
# from JAVA_OPTIONS, in /run/jetty, a tmpfs in compose.yaml.
USER root
RUN chown -R root:root "$JETTY_BASE" && chmod -R go-w "$JETTY_BASE" \
	&& mkdir -p /var/lib/antilia-widgets/page-store /var/lib/antilia-widgets/accounts /run/jetty \
	&& chown -R jetty:jetty /var/lib/antilia-widgets /run/jetty
COPY --from=unpacked /tmp/ROOT /var/lib/jetty/webapps/ROOT
USER jetty
ENV ANTILIA_WIDGETS_PAGE_STORE=/var/lib/antilia-widgets/page-store
ENV ANTILIA_WIDGETS_ACCOUNTS_FOLDER=/var/lib/antilia-widgets/accounts
ENV JETTY_START=/run/jetty/jetty.start
# a JVM out of memory ends, whatever JAVA_OPTIONS of .env say, so that Docker starts it again
# (restart: unless-stopped) instead of leaving it up but broken; the java launcher reads this
# variable by itself
ENV JDK_JAVA_OPTIONS=-XX:+ExitOnOutOfMemoryError
