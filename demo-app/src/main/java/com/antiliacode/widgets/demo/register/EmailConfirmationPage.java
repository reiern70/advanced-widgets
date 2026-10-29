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
package com.antiliacode.widgets.demo.register;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.demo.HomePage;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Tells a visitor who registered on the {@link RegisterPage} to open the link of the
 * {@link ConfirmationMail}, which leads to the {@link EmailValidationPage}. It is shown only after
 * registering: asked for by its address, without an email, it answers 400.
 */
public class EmailConfirmationPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param email
	 *            the address the confirmation was sent to
	 */
	public EmailConfirmationPage(String email)
	{
		// kept in the session, so that it is not made anew from its class, without the email
		setStatelessHint(false);
		add(new Label("sent", new StringResourceModel("sent", this).setParameters(Model.of(email))));
		add(new BookmarkablePageLink<>("backHome", HomePage.class));
	}

	/**
	 * The page asked for by its address, which it has nothing to show for.
	 *
	 * @param parameters
	 *            the parameters of the request
	 * @throws AbortWithHttpErrorCodeException
	 *             always, with 400
	 */
	public EmailConfirmationPage(PageParameters parameters)
	{
		throw new AbortWithHttpErrorCodeException(HttpServletResponse.SC_BAD_REQUEST);
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}
}
