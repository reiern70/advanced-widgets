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

import java.nio.charset.StandardCharsets;

import org.apache.wicket.Application;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * A script that takes its time, for the browser tests of the veil over a page while it loads
 * ({@link com.antiliacode.widgets.veil.PageLoadingVeilBehavior}): when the web application has the
 * init parameter {@value #PARAMETER}, a number of milliseconds, every page loads deferred a script
 * the server answers only after that long. The browser shows the page, but runs its domready
 * handlers, which initialize the components, only once that script has run. The browser tests set
 * it ({@code JettyTestCase}); production's {@code web.xml} never does, and without it the script is
 * neither mounted nor loaded.
 */
public final class SlowScript extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** The init parameter of the web application: how long the script takes, in milliseconds. */
	public static final String PARAMETER = "antilia-widgets.slow-script";

	/** Where the script is mounted, when it is. */
	public static final String PATH = "slow-script.js";

	/** The script, as mounted at {@value #PATH}. */
	static final ResourceReference REFERENCE = new ResourceReference(SlowScript.class,
		"slow-script.js")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new SlowScript();
		}
	};

	private SlowScript()
	{
	}

	/**
	 * @param application
	 *            the application
	 * @return how long the script takes, 0 when the application has no such script
	 */
	static long millis(Application application)
	{
		if (application instanceof WebApplication web && web.getServletContext() != null)
		{
			String millis = web.getServletContext().getInitParameter(PARAMETER);
			return millis == null ? 0 : Long.parseLong(millis.trim());
		}
		return 0;
	}

	/**
	 * Mounts the script, when the application has one.
	 *
	 * @param application
	 *            the application starting
	 */
	static void mount(WebApplication application)
	{
		if (millis(application) > 0)
		{
			application.mountResource(PATH, REFERENCE);
		}
	}

	/**
	 * Has the page load the script deferred, when the application has one.
	 *
	 * @param response
	 *            the header response of a page
	 */
	static void renderHead(IHeaderResponse response)
	{
		if (millis(Application.get()) > 0)
		{
			response.render(JavaScriptHeaderItem.forReference(REFERENCE).setDefer(true));
		}
	}

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		try
		{
			Thread.sleep(millis(Application.get()));
		}
		catch (InterruptedException stopped)
		{
			// the server stops: answer at once
			Thread.currentThread().interrupt();
		}
		ResourceResponse response = new ResourceResponse();
		response.disableCaching();
		response.setContentType("text/javascript");
		response.setTextEncoding(StandardCharsets.UTF_8.name());
		response.setWriteCallback(new WriteCallback()
		{
			@Override
			public void writeData(Attributes attributes)
			{
				attributes.getResponse().write("/* the slow script of the tests */\n");
			}
		});
		return response;
	}
}
