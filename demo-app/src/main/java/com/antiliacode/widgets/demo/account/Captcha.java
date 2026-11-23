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
package com.antiliacode.widgets.demo.account;

import java.time.Clock;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.demo.DemoSession;

/**
 * Asks a visitor to show being a person by typing the letters of a {@link CaptchaImage}. Stateless:
 * the letters travel in a {@link CaptchaToken}, in a hidden field of the form and in the URL of
 * the image, never in the page or the session, and each token answers once. A link shows other
 * letters by loading the page anew.
 */
public class Captcha extends Panel
{
	private static final long serialVersionUID = 1L;

	private final Clock clock;

	private final Model<String> token;

	private final Model<String> answer = Model.of();

	private final HiddenField<String> tokenField;

	private final FormComponent<String> answerField;

	/**
	 * @param id
	 *            component id
	 */
	public Captcha(String id)
	{
		this(id, Clock.systemUTC());
	}

	/**
	 * @param id
	 *            component id
	 * @param clock
	 *            the clock of the tokens
	 */
	Captcha(String id, Clock clock)
	{
		super(id);
		this.clock = clock;
		token = Model.of(CaptchaToken.issue(clock.instant()));
		tokenField = new HiddenField<>("token", token);
		add(tokenField);
		add(new WebComponent("image").add(AttributeModifier.replace("src",
			() -> urlFor(CaptchaImage.REFERENCE, CaptchaImage.parameters(token.getObject(),
				DemoSession.get().getTheme().isDark())))));
		answerField = new RequiredTextField<>("answer", answer);
		answerField.setLabel(new ResourceModel("captchaLetters"));
		add(answerField);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		add(new BookmarkablePageLink<>("another", getPage().getPageClass(),
			getPage().getPageParameters()));
	}

	/**
	 * Answers the token the form sent, which then answers no more.
	 *
	 * @return whether what the visitor typed are the letters of the image
	 */
	public boolean isSolved()
	{
		return CaptchaToken.answer(token.getObject(), answer.getObject(), clock.instant());
	}

	/**
	 * Shows other letters, with the field of the answer empty.
	 */
	public void renew()
	{
		token.setObject(CaptchaToken.issue(clock.instant()));
		tokenField.clearInput();
		answer.setObject(null);
		answerField.clearInput();
	}

	/**
	 * @return the message telling the visitor that the letters were wrong
	 */
	public String getWrongMessage()
	{
		return getString("captchaLettersWrong");
	}

	/**
	 * @return the field the visitor types the letters in
	 */
	public FormComponent<String> getAnswerField()
	{
		return answerField;
	}

	/**
	 * @return the token of the letters asked, for tests
	 */
	String getToken()
	{
		return token.getObject();
	}
}
