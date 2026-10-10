# The demo application on Jetty 11 and Java 21. deploy.sh builds the WAR on the developer's
# machine (Wicket 11 is a snapshot built there) and copies it next to this file as ROOT.war.
FROM jetty:11.0.26-jre21

# websockets for Wicket's native websockets; X-Forwarded-* from Caddy, so Wicket sees https
RUN java -jar "$JETTY_HOME/start.jar" --add-modules=websocket-jakarta,http-forwarded

# the Wicket page store and the accounts registered to the demos, on a volume of their own on
# the instance's SSD (see compose.yaml)
USER root
RUN mkdir -p /var/lib/antilia-widgets/page-store /var/lib/antilia-widgets/accounts \
	&& chown -R jetty:jetty /var/lib/antilia-widgets
USER jetty
ENV ANTILIA_WIDGETS_PAGE_STORE=/var/lib/antilia-widgets/page-store
ENV ANTILIA_WIDGETS_ACCOUNTS_FOLDER=/var/lib/antilia-widgets/accounts

COPY --chown=jetty:jetty ROOT.war /var/lib/jetty/webapps/ROOT.war
