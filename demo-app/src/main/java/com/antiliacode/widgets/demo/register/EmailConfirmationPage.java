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

import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.demo.HomePage;

/**
 * Tells a visitor who registered on the {@link RegisterPage} to open the link of the
 * {@link ConfirmationMail}, which leads to the {@link EmailValidationPage}.
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
		add(new Label("sent", new StringResourceModel("sent", this).setParameters(Model.of(email))));
		add(new BookmarkablePageLink<>("backHome", HomePage.class));
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}
}
