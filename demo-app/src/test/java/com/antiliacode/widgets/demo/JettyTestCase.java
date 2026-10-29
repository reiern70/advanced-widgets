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
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.ee11.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.antiliacode.widgets.demo.account.DemosAuthorization;

/**
 * Runs the demo application in Jetty on a free port for each test, at the root context and with
 * websockets, as {@link Start} does, in deployment as production does, but with the demos open to
 * everybody.
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

		server = new Server();
		ServerConnector http = new ServerConnector(server);
		http.setPort(0);
		server.addConnector(http);

		WebAppContext web = new WebAppContext();
		web.setServer(server);
		web.setContextPath("/");
		// the browser tests open the demos without signing in, as production runs otherwise
		web.setInitParameter(DemosAuthorization.OPEN_PARAMETER, "true");
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
		// the tests run after this one in the same JVM start as without it: in development
		System.clearProperty("wicket.configuration");
	}
}
