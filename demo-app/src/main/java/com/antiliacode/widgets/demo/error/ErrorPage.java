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
package com.antiliacode.widgets.demo.error;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.http.WebResponse;

import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.demo.HomePage;

/**
 * A page telling what went wrong, in the look of the site: its header, its theme and its language,
 * a title, a sentence and the way back to the demos. Wicket shows the subclasses instead of its own
 * pages, and Jetty instead of its error page (see WicketApplication and web.xml): when no page is
 * at an address, when a page failed, when a page is no longer kept and when a page is not for the
 * user. They answer with the status of what went wrong, rendered at once rather than after a
 * redirect (see DirectPageRenderer).
 */
public abstract class ErrorPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	private final String kind;

	private final int status;

	/**
	 * @param kind
	 *            what went wrong, the prefix of the resources of the title and the message
	 * @param status
	 *            the HTTP status the page answers with, 0 to leave it as it is
	 */
	protected ErrorPage(String kind, int status)
	{
		this.kind = kind;
		this.status = status;
		add(new Label("errorTitle", new ResourceModel(kind + ".title")));
		add(new Label("errorMessage", new ResourceModel(kind + ".message")));
		add(new BookmarkablePageLink<>("backHome", HomePage.class));
	}

	@Override
	protected void setHeaders(WebResponse response)
	{
		super.setHeaders(response);
		if (status != 0)
		{
			response.setStatus(status);
		}
	}

	@Override
	public boolean isErrorPage()
	{
		return true;
	}

	@Override
	public boolean isVersioned()
	{
		return false;
	}

	@Override
	protected String getPageTitle()
	{
		return getString(kind + ".title") + " · " + super.getPageTitle();
	}
}
