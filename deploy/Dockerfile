# The demo application on Jetty 12, in its Jakarta EE 11 environment (ee11, Servlet 6.1, which
# Wicket 11 is built for), and Java 21: the Jetty of jetty:run and of the tests. deploy.sh builds
# the WAR on the developer's machine and copies it next to this file as ROOT.war. Jetty 12.1 comes
# with a JDK only, there is no image of it with a JRE. The image is pinned by its digest, so a
# build takes the same one until Dependabot proposes the next (see .github/dependabot.yml).
FROM jetty:12.1.15-jdk21@sha256:fe52928bbddac1bf31b0227dff3864cacdcc2e31686c3674f652d98a4041d68b AS base

# The WAR unpacked here, so that Jetty serves the web application from a folder of the image it
# cannot change, rather than unpacking it into the temporary folder at every start
FROM base AS unpacked
COPY ROOT.war /tmp/ROOT.war
RUN mkdir /tmp/ROOT && cd /tmp/ROOT && jar -xf /tmp/ROOT.war

FROM base

# the web applications of webapps deployed as ee11 web applications; websockets for Wicket's
# native websockets; X-Forwarded-* from Caddy, so Wicket sees https; no version of Jetty in the
# Server header and on its error pages
RUN java -jar "$JETTY_HOME/start.jar" --add-modules=ee11-deploy,ee11-websocket-jakarta,forwarded \
	&& echo "jetty.httpConfig.sendServerVersion=false" > "$JETTY_BASE/start.d/server-version.ini"

# Jetty's base and the web application belong to root, so that the user jetty, which runs Jetty,
# can change neither the configuration nor the code; compose.yaml makes the whole file system
# read-only besides. The Wicket page store and the visits counted (a file a day, kept 13 months)
# are on a volume of their own on the instance's SSD (see compose.yaml); the page store is emptied
# at every start, its pages last as long as the sessions, which Jetty keeps in memory (no
# session-store module). The command line Jetty starts with, jetty.start, is made at every start
# from JAVA_OPTIONS, in /run/jetty, a tmpfs in compose.yaml.
USER root
RUN chown -R root:root "$JETTY_BASE" && chmod -R go-w "$JETTY_BASE" \
	&& mkdir -p /var/lib/antilia-widgets/page-store /var/lib/antilia-widgets/stats /run/jetty \
	&& chown -R jetty:jetty /var/lib/antilia-widgets /run/jetty
COPY --from=unpacked /tmp/ROOT /var/lib/jetty/webapps/ROOT
USER jetty
ENV ANTILIA_WIDGETS_PAGE_STORE=/var/lib/antilia-widgets/page-store
ENV ANTILIA_WIDGETS_STATS_FOLDER=/var/lib/antilia-widgets/stats
ENV JETTY_START=/run/jetty/jetty.start
# a JVM out of memory ends, whatever JAVA_OPTIONS of .env say, so that Docker starts it again
# (restart: unless-stopped) instead of leaving it up but broken; the java launcher reads this
# variable by itself
ENV JDK_JAVA_OPTIONS=-XX:+ExitOnOutOfMemoryError
