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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.ConnectionRejected;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

/**
 * The websockets connect from the site's own pages only, see {@link WebSocketOriginFilter}.
 */
class WebSocketOriginFilterTest
{
	@Test
	void aPageOfTheSiteConnectsWhereverTheSiteRuns()
	{
		assertTrue(WebSocketOriginFilter.isAllowed("https://antiliacode.com", "antiliacode.com",
			"https://antiliacode.com"));
		assertTrue(WebSocketOriginFilter.isAllowed("http://localhost:8194", "localhost", null));
		assertTrue(WebSocketOriginFilter.isAllowed("https://192.168.0.152:8494", "192.168.0.152",
			null));
		assertTrue(WebSocketOriginFilter.isAllowed("https://ANTILIACODE.com", "app",
			"https://antiliacode.com/"), "the site's address, whatever host the proxy passes");
	}

	@Test
	void aPageOfAnotherSiteDoesNot()
	{
		assertFalse(WebSocketOriginFilter.isAllowed("https://elsewhere.example",
			"antiliacode.com", "https://antiliacode.com"));
		assertFalse(WebSocketOriginFilter.isAllowed("https://antiliacode.com.elsewhere.example",
			"antiliacode.com", null));
		assertFalse(WebSocketOriginFilter.isAllowed(null, "antiliacode.com", null));
		assertFalse(WebSocketOriginFilter.isAllowed("null", "antiliacode.com", null),
			"a sandboxed frame");
		assertFalse(WebSocketOriginFilter.isAllowed("not an address", "antiliacode.com", null));
	}

	@Test
	void filtersTheHandshakesOfTheApplication()
	{
		WicketTester tester = new WicketTester(new WicketApplication());
		try
		{
			WebSocketOriginFilter filter = (WebSocketOriginFilter)WebSocketSettings.Holder
				.get(tester.getApplication()).getConnectionFilter();
			MockHttpServletRequest request = tester.getRequest();
			assertSame(WebSocketOriginFilter.class, filter.getClass());

			request.setHeader("Origin", "http://" + request.getServerName() + ":8194");
			assertNull(filter.doFilter(request), "by the name of the server");
			request.setHeader("Host", "192.168.0.152:8494");
			request.setHeader("Origin", "https://192.168.0.152:8494");
			assertNull(filter.doFilter(request), "by the host the handshake went to");

			request.setHeader("Origin", "https://elsewhere.example");
			ConnectionRejected rejected = filter.doFilter(request);
			assertEquals(1008, rejected.getCode());
		}
		finally
		{
			tester.destroy();
		}
	}
}
