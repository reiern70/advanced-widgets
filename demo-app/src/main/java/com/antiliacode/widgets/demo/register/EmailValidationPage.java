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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.SignInPage;
import com.antiliacode.widgets.demo.data.User;

/**
 * Where the link of the {@link ConfirmationMail} leads: a button that validates the email of the
 * account, so that it opens the demos once the visitor signs in. Opening the link validates
 * nothing, nor signs in, as programs checking the links of emails open them too; only the button,
 * sent with POST, does. The link works once and for {@link Accounts#VALIDATION_TIME}; a link that
 * does not work says so.
 */
public class EmailValidationPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/** The page parameter holding the token of the link. */
	public static final String TOKEN = "token";

	/** What the page shows. */
	private enum State
	{
		/** The button that validates. */
		CONFIRM,
		/** That the email is validated. */
		VALIDATED,
		/** That the link does not work. */
		INVALID
	}

	private State state;

	/** The email the link validates, or validated. */
	private String email;

	/**
	 * @param parameters
	 *            the token of the link
	 */
	public EmailValidationPage(PageParameters parameters)
	{
		super(parameters);
		String token = parameters.get(TOKEN).toOptionalString();
		User user = Accounts.get().findValidation(token).orElse(null);
		state = user == null ? State.INVALID : State.CONFIRM;
		email = user == null ? "" : user.getName();

		WebMarkupContainer confirm = new StateContainer("confirm", State.CONFIRM);
		add(confirm);
		confirm.add(new Label("text", new StringResourceModel("confirm.text", this)
			.setParameters((IModel<String>)() -> email)));
		StatelessForm<Void> form = new StatelessForm<>("form")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				User validated = Accounts.get().validate(token).orElse(null);
				state = validated == null ? State.INVALID : State.VALIDATED;
				email = validated == null ? "" : validated.getName();
			}
		};
		confirm.add(form);

		WebMarkupContainer validated = new StateContainer("validated", State.VALIDATED);
		add(validated);
		validated.add(new Label("email", new StringResourceModel("validated.text", this)
			.setParameters((IModel<String>)() -> email)));
		validated.add(new BookmarkablePageLink<>("signIn", SignInPage.class));

		WebMarkupContainer invalid = new StateContainer("invalid", State.INVALID);
		add(invalid);
		invalid.add(new BookmarkablePageLink<>("signIn", SignInPage.class));
		invalid.add(new BookmarkablePageLink<>("home", HomePage.class));
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}

	/**
	 * A part of the page shown in one state.
	 */
	private class StateContainer extends WebMarkupContainer
	{
		private static final long serialVersionUID = 1L;

		private final State shownIn;

		StateContainer(String id, State shownIn)
		{
			super(id);
			this.shownIn = shownIn;
		}

		@Override
		protected void onConfigure()
		{
			super.onConfigure();
			setVisible(state == shownIn);
		}
	}
}
