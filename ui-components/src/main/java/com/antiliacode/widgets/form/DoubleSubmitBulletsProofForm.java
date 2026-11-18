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
package com.antiliacode.widgets.form;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * A form that is processed once however many times the browser sends it from one version: a
 * double click, Enter pressed twice or a submit queued behind another one run {@code onSubmit}
 * once, and the other times the form gets an error ({@link #onSentTwice()}). It is a {@link Form}
 * with a {@link FormVersionBehavior}, which tells what it covers and what it does not; a form that
 * cannot extend this class, such as one of a library, gets the behavior instead.
 *
 * @param <T>
 *            the type of the form's model object
 */
public class DoubleSubmitBulletsProofForm<T> extends Form<T>
{
	private static final long serialVersionUID = 1L;

	private final FormVersionBehavior formVersion;

	/**
	 * @param id
	 *            the id of the form
	 */
	public DoubleSubmitBulletsProofForm(String id)
	{
		this(id, null);
	}

	/**
	 * @param id
	 *            the id of the form
	 * @param model
	 *            its model, or null
	 */
	public DoubleSubmitBulletsProofForm(String id, IModel<T> model)
	{
		super(id, model);
		formVersion = new FormVersionBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onRefused(Form<?> form)
			{
				onSentTwice();
			}
		};
		add(formVersion);
	}

	/**
	 * @return the behavior keeping the form's version
	 */
	public final FormVersionBehavior getFormVersionBehavior()
	{
		return formVersion;
	}

	/**
	 * Called when a submit carried another version than the form's: the form was already sent from
	 * that version, or changed while it was being sent. By default the form gets the error
	 * {@value FormVersionBehavior#ALREADY_SENT_KEY}, which fails the validation; an override that
	 * reports nothing must still give the form an error, or the submit is processed.
	 */
	protected void onSentTwice()
	{
		error(getString(FormVersionBehavior.ALREADY_SENT_KEY, null,
			FormVersionBehavior.ALREADY_SENT));
	}
}
