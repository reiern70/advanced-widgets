/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antiliacode.widgets.demo;

import org.apache.wicket.protocol.ws.javax.WicketServerEndpointConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Runs the demo application in Jetty on a free port for each test, at the root context and with
 * websockets, as {@link Start} does.
 */
public abstract class JettyTestCase
{
	private Server server;

	/** The port the demo application listens on. */
	protected int localPort;

	@BeforeEach
	public void before() throws Exception
	{
		System.setProperty("wicket.configuration", "deployment");
		// the browser tests open the demos without signing in
		System.setProperty("antilia-widgets.open-demos", "true");

		server = new Server();
		ServerConnector http = new ServerConnector(server);
		http.setPort(0);
		server.addConnector(http);

		WebAppContext web = new WebAppContext();
		web.setServer(server);
		web.setContextPath("/");
		String basedir = System.getProperty("basedir");
		web.setWar((basedir != null ? basedir + "/" : "") + "src/main/webapp");
		JakartaWebSocketServletContainerInitializer.configure(web,
			(servletContext, container) -> container.addEndpoint(new WicketServerEndpointConfig()));
		server.setHandler(web);

		server.start();
		localPort = http.getLocalPort();
	}

	@AfterEach
	public void after() throws Exception
	{
		server.stop();
		server.join();
		// the tests run after this one in the same JVM start as without it: in development, and
		// with the demos kept for the visitors who signed in, unless development opens them
		System.clearProperty("wicket.configuration");
		System.clearProperty("antilia-widgets.open-demos");
	}
}
