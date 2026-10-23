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
package com.antilia.widgets.demo.account;

import java.security.SecureRandom;
import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.io.IClusterable;

import com.antilia.widgets.demo.DemoSession;

/**
 * Asks a visitor to show being a person: by typing the letters of a {@link CaptchaImage}, or, for
 * those who cannot read it, by answering a question about two numbers written in words, which a
 * screen reader reads out. A link turns one into the other; another link shows other letters
 * without sending the form, so what the visitor typed in it stays.
 */
public class Captcha extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The letters of the image: none that look like another, as 0 and O, 1 and I, or 5 and S. */
	static final String LETTERS = "ACDEFHJKLMNPRTUWXY346789";

	/** How many letters the image has. */
	static final int LENGTH = 5;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final Model<String> letters = Model.of(newLetters());

	private final Model<String> answer = Model.of();

	private final CaptchaImage image = new CaptchaImage(letters,
		() -> DemoSession.get().getTheme().isDark());

	private final NonCachingImage picture;

	private final FormComponent<String> answerField;

	/** The question asked instead of the letters, {@code null} while they are asked. */
	private Question question;

	/**
	 * @param id
	 *            component id
	 */
	public Captcha(String id)
	{
		super(id);
		setOutputMarkupPlaceholderTag(true);

		add(new Label("lead",
			() -> getString(question == null ? "captchaLead" : "captchaQuestionLead")));

		WebMarkupContainer pictured = new WebMarkupContainer("pictured")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(question == null);
			}
		};
		add(pictured);
		picture = new NonCachingImage("image", image);
		pictured.add(picture.setOutputMarkupId(true));

		answerField = new RequiredTextField<>("answer", answer);
		answerField.setLabel(
			() -> getString(question == null ? "captchaLetters" : "captchaAnswer"));
		answerField.setOutputMarkupId(true);
		// capitals for the letters, the keyboard of digits for the answer to a question
		answerField.add(AttributeModifier.replace("autocapitalize",
			() -> question == null ? "characters" : null));
		answerField.add(AttributeModifier.replace("inputmode",
			() -> question == null ? null : "numeric"));

		pictured.add(new AjaxFallbackLink<Void>("another")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(Optional<AjaxRequestTarget> target)
			{
				renew();
				target.ifPresent(t -> t.add(picture, answerField));
			}
		});

		add(new Label("label",
			() -> question == null ? getString("captchaLetters") : word(question)));
		add(answerField);

		AjaxFallbackLink<Void> other = new AjaxFallbackLink<>("other")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(Optional<AjaxRequestTarget> target)
			{
				ask(question == null);
				target.ifPresent(t -> {
					t.add(Captcha.this);
					t.focusComponent(answerField);
				});
			}
		};
		add(other);
		other.add(new Label("label",
			() -> getString(question == null ? "captchaToQuestion" : "captchaToLetters")));
	}

	/**
	 * @return whether what the visitor typed solves the captcha: the letters of the image, in
	 *         capitals or not, or the number that answers the question, in digits
	 */
	public boolean isSolved()
	{
		String typed = answer.getObject() == null ? "" : answer.getObject().strip();
		return getSolution().equalsIgnoreCase(typed);
	}

	/**
	 * Shows other letters, or asks another question, and empties the field of the answer.
	 */
	public void renew()
	{
		ask(question != null);
	}

	/**
	 * Asks for other letters, which the image draws, or another question, with the field of the
	 * answer empty.
	 *
	 * @param byQuestion
	 *            whether to ask a question
	 */
	private void ask(boolean byQuestion)
	{
		question = byQuestion ? newQuestion() : null;
		if (!byQuestion)
		{
			letters.setObject(newLetters());
		}
		answer.setObject(null);
		answerField.clearInput();
	}

	/**
	 * @return the message telling the visitor that the answer was wrong
	 */
	public String getWrongMessage()
	{
		return getString(question == null ? "captchaLettersWrong" : "captchaAnswerWrong");
	}

	/**
	 * @return the field the visitor types the answer in
	 */
	public FormComponent<String> getAnswerField()
	{
		return answerField;
	}

	/**
	 * @return what solves the captcha: the letters of the image, or the number answering the
	 *         question
	 */
	String getSolution()
	{
		return question == null ? letters.getObject() : String.valueOf(question.answer());
	}

	/**
	 * @return the question asked instead of the letters, {@code null} while they are asked
	 */
	Question getQuestion()
	{
		return question;
	}

	/**
	 * Words a question in the language of the page, with its numbers in words, so that it is read
	 * aloud as it is shown.
	 */
	private String word(Question asked)
	{
		String[] numbers = getString("captchaNumbers").split(",");
		return new StringResourceModel(asked.sum() ? "captchaPlus" : "captchaMinus", this)
			.setParameters(numbers[asked.first()].strip(), numbers[asked.second()].strip())
			.getString();
	}

	private static String newLetters()
	{
		StringBuilder text = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++)
		{
			text.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
		}
		return text.toString();
	}

	/**
	 * @return the sum of two numbers from one to nine, or the difference of two up to ten
	 */
	static Question newQuestion()
	{
		if (RANDOM.nextBoolean())
		{
			return new Question(1 + RANDOM.nextInt(9), 1 + RANDOM.nextInt(9), true);
		}
		int first = 2 + RANDOM.nextInt(9);
		return new Question(first, 1 + RANDOM.nextInt(first - 1), false);
	}

	/**
	 * A question about two numbers, from zero to ten: how much is their sum, or their difference.
	 *
	 * @param first
	 *            the first number
	 * @param second
	 *            the second number
	 * @param sum
	 *            whether the question asks for the sum, else for the difference
	 */
	record Question(int first, int second, boolean sum) implements IClusterable
	{
		/**
		 * @return the number answering the question
		 */
		int answer()
		{
			return sum ? first + second : first - second;
		}
	}
}
