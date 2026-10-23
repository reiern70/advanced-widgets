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
package com.antiliacode.widgets.demo.datatable;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.StringValidator;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.dropdown.Dropdown;

/**
 * Edits the name and the address of a user, every field required, or enters a new user,
 * one without an id. Shown in the overlay of the dynamic table examples and in the dialog of the
 * infinite list example, it calls
 * {@link #onSaved(User, AjaxRequestTarget)} or {@link #onCancel(AjaxRequestTarget)} when the
 * user is done.
 */
public abstract class UserEditPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final long userId;

	private String firstName;

	private String lastName;

	private String country;

	private String city;

	private String address;

	/**
	 * @param id
	 *            component id
	 * @param user
	 *            the user to edit, or a new one
	 */
	public UserEditPanel(String id, User user)
	{
		super(id);
		userId = user.getId();
		firstName = user.getFirstName();
		lastName = user.getLastName();
		country = user.getCountry();
		city = user.getCity();
		address = user.getAddress();

		Form<Void> form = new Form<>("form");
		form.add(AttributeAppender.replace("data-dt-changed", () -> form.hasError() ? "true" : null));
		add(form);
		FeedbackPanel feedback = new FeedbackPanel("feedback");
		feedback.setOutputMarkupId(true);
		form.add(feedback);

		form.add(text("firstName", 60));
		form.add(text("lastName", 60));
		form.add(new Dropdown<>("country", new PropertyModel<>(this, "country"),
			countries(), new ChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(String code)
				{
					return countryName(code);
				}

				@Override
				public String getIdValue(String code, int index)
				{
					return code;
				}
			}).setRequired(true).setLabel(new ResourceModel("field.country")));
		form.add(text("city", 60));
		form.add(text("address", 120));

		form.add(new AjaxLink<Void>("cancel")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				onCancel(target);
			}
		}.add(new ButtonBehavior(Variant.SECONDARY, Size.MEDIUM)));
		form.add(new AjaxButton("save")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				UsersDatabase database = DatabaseLocator.getLargeDatabase();
				User user = userId == 0 ? new User() : database.get(userId);
				if (user == null)
				{
					error(getString("userGone"));
					target.add(feedback);
					return;
				}
				user.setFirstName(firstName);
				user.setLastName(lastName);
				user.setCountry(country);
				user.setCity(city);
				user.setAddress(address);
				if (userId == 0)
				{
					database.save(user);
				}
				else
				{
					database.update(user);
				}
				onSaved(user, target);
			}

			@Override
			protected void onError(AjaxRequestTarget target)
			{
				target.add(form);
			}
		}.add(new ButtonBehavior(Variant.PRIMARY, Size.MEDIUM)));
	}

	private FormComponent<String> text(String property, int maximumLength)
	{
		TextField<String> field = new TextField<>(property, new PropertyModel<>(this, property));
		field.setRequired(true);
		// field.*: the page's resources, the headers of its columns, would win over firstName
		field.setLabel(new ResourceModel("field." + property));
		field.add(StringValidator.maximumLength(maximumLength));
		return field;
	}

	private List<String> countries()
	{
		// by the language's order, not the code points': Åland and Österreich go to the A and O
		Collator collator = Collator.getInstance(getLocale());
		return Arrays.stream(Locale.getISOCountries())
			.sorted(Comparator.comparing(this::countryName, collator))
			.toList();
	}

	private String countryName(String code)
	{
		return Locale.of("", code).getDisplayCountry(getLocale());
	}

	/**
	 * Called after the user was saved.
	 *
	 * @param user
	 *            the saved user
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onSaved(User user, AjaxRequestTarget target);

	/**
	 * Called when the user cancels editing.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onCancel(AjaxRequestTarget target);
}
