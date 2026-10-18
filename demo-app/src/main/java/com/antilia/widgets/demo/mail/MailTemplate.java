/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.demo.mail;

import java.util.function.Function;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.response.StringResponse;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;

/**
 * Renders a Wicket component, with its markup and bundles, to the HTML of an email, during the
 * request that sends the email. Only the component is written, not a page around it, to a
 * buffer instead of the response.
 */
public final class MailTemplate
{
	/** The id the component of the email has to have. */
	public static final String ID = "mail";

	private MailTemplate()
	{
	}

	/**
	 * @param newComponent
	 *            makes the component, given the id {@value #ID}
	 * @return the component as rendered, and its HTML
	 */
	public static <C extends Component> Rendered<C> render(Function<String, C> newComponent)
	{
		C component = newComponent.apply(ID);
		MailPage page = new MailPage();
		page.add(component);

		RequestCycle cycle = RequestCycle.get();
		Response original = cycle.getResponse();
		StringResponse buffer = new StringResponse();
		try
		{
			cycle.setResponse(buffer);
			page.internalInitialize();
			component.render();
		}
		finally
		{
			cycle.setResponse(original);
		}
		return new Rendered<>(component, buffer.getBuffer().toString());
	}

	/**
	 * A component and its HTML.
	 *
	 * @param component
	 *            the component, in its page, to read texts from its bundles
	 * @param html
	 *            the HTML
	 */
	public record Rendered<C extends Component>(C component, String html)
	{
	}

	/**
	 * The page the component is in while it is rendered, never rendered itself.
	 */
	private static class MailPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<wicket:container wicket:id=\"" + ID +
				"\"></wicket:container>");
		}

		@Override
		public boolean isBookmarkable()
		{
			return false;
		}
	}
}
