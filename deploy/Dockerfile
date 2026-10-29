# The demo application on Jetty 12, in its Jakarta EE 10 environment (ee10, Servlet 6), and Java
# 21: the Jetty of jetty:run and of the tests. deploy.sh builds the WAR on the developer's machine
# (Wicket 11 is a snapshot built there) and copies it next to this file as ROOT.war. Jetty 12.1
# comes with a JDK only, there is no image of it with a JRE.
FROM jetty:12.1.14-jdk21

# the WARs of webapps deployed as ee10 web applications; websockets for Wicket's native
# websockets; X-Forwarded-* from Caddy, so Wicket sees https; no version of Jetty in the Server
# header and on its error pages
RUN java -jar "$JETTY_HOME/start.jar" --add-modules=ee10-deploy,ee10-websocket-jakarta,forwarded \
	&& echo "jetty.httpConfig.sendServerVersion=false" > "$JETTY_BASE/start.d/server-version.ini"

# the Wicket page store and the accounts registered to the demos, on a volume of their own on
# the instance's SSD (see compose.yaml); the page store is emptied at every start, its pages
# last as long as the sessions, which Jetty keeps in memory (no session-store module)
USER root
RUN mkdir -p /var/lib/antilia-widgets/page-store /var/lib/antilia-widgets/accounts \
	&& chown -R jetty:jetty /var/lib/antilia-widgets
USER jetty
ENV ANTILIA_WIDGETS_PAGE_STORE=/var/lib/antilia-widgets/page-store
ENV ANTILIA_WIDGETS_ACCOUNTS_FOLDER=/var/lib/antilia-widgets/accounts

COPY --chown=jetty:jetty ROOT.war /var/lib/jetty/webapps/ROOT.war
