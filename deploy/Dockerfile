# The demo application on Jetty 11 and Java 21. deploy.sh builds the WAR on the developer's
# machine and copies it next to this file as ROOT.war.
FROM jetty:11.0.26-jre21

# websockets for Wicket's native websockets; X-Forwarded-* from Caddy, so Wicket sees https;
# no version of Jetty in the Server header and on its error pages
RUN java -jar "$JETTY_HOME/start.jar" --add-modules=websocket-jakarta,http-forwarded \
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
