/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.autocomplete;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antilia.widgets.autocomplete.Autocomplete;
import com.antilia.widgets.autocomplete.IAutocompleteProvider;
import com.antilia.widgets.autocomplete.MultiAutocomplete;
import com.antilia.widgets.demo.Countries;
import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.datatable.DatabaseLocator;
import com.antilia.widgets.demo.datatable.User;
import com.antilia.widgets.demo.datatable.UsersProvider;
import com.antilia.widgets.demo.datatable.UsersRowsResource;
import com.antilia.widgets.demo.frameworks.ChoicesResource;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link Autocomplete} and {@link MultiAutocomplete}: the users of the table demos and the
 * countries suggested as the user types, each row rendered in the browser from a template, the
 * chosen values shown as chips with an x. The choices are shown below the form as Ajax reports
 * them, and again when the form is sent. The React and Svelte tabs show the same form in those
 * frameworks, the users searched through the {@link UsersRowsResource}, the countries filtered in
 * the browser from the {@link ChoicesResource}.
 */
public class AutocompletePage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The row of a user: name, city and country. */
	static final String USER_TEMPLATE = "<span class=\"ac-user\"><b>{{firstName}} {{lastName}}</b>" +
		"<small>{{city}}, {{countryName}}</small></span>";

	/** The row of a country: its flag and name. */
	static final String COUNTRY_TEMPLATE = "<span class=\"ac-country\"><span class=\"ac-flag\" " +
		"aria-hidden=\"true\">{{flag}}</span>{{name}}</span>";

	private User owner;

	private List<User> team = new ArrayList<>();

	private List<Country> countries = new ArrayList<>();

	/**
	 * Constructor.
	 */
	public AutocompletePage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		Form<Void> form = new Form<>("form")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				info(new StringResourceModel("sent", AutocompletePage.this)
					.setParameters(describeChoices()).getString());
			}
		};
		themed.add(form);

		Label choices = new Label("choices", this::describeChoices);
		choices.setOutputMarkupId(true);
		themed.add(choices);

		Autocomplete<User> ownerChoice = new Autocomplete<>("owner",
			new PropertyModel<>(this, "owner"), new UsersAutocompleteProvider());
		ownerChoice.setTemplate(USER_TEMPLATE)
			.setPlaceholder(new ResourceModel("owner.placeholder"));
		ownerChoice.setRequired(true).setLabel(new ResourceModel("owner"));
		form.add(reportChanges(ownerChoice, choices));

		MultiAutocomplete<User> teamChoice = new MultiAutocomplete<>("team",
			new PropertyModel<>(this, "team"), new UsersAutocompleteProvider());
		teamChoice.setTemplate(USER_TEMPLATE)
			.setPlaceholder(new ResourceModel("team.placeholder"));
		form.add(reportChanges(teamChoice, choices));

		MultiAutocomplete<Country> countriesChoice = new MultiAutocomplete<>("countries",
			new PropertyModel<>(this, "countries"), new CountriesProvider());
		countriesChoice.setTemplate(COUNTRY_TEMPLATE)
			.setMinLength(0)
			.setPlaceholder(new ResourceModel("countries.placeholder"));
		form.add(reportChanges(countriesChoice, choices));

		themed.add(new FeedbackPanel("feedback"));
	}

	private static <C extends FormComponent<?>> C reportChanges(C choice, Label choices)
	{
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(choices);
			}

			@Override
			protected void onError(AjaxRequestTarget target, RuntimeException e)
			{
				target.add(choices);
			}
		});
		return choice;
	}

	/**
	 * @return what is chosen, in words
	 */
	private String describeChoices()
	{
		String none = getString("nothing");
		return new StringResourceModel("choices", this).setParameters(
			owner != null ? name(owner) : none,
			team.isEmpty() ? none : team.stream().map(AutocompletePage::name)
				.collect(Collectors.joining(", ")),
			countries.isEmpty() ? none : countries.stream().map(Country::getName)
				.collect(Collectors.joining(", ")))
			.getString();
	}

	private static String name(User user)
	{
		return user.getFirstName() + " " + user.getLastName();
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "autocomplete";
	}

	/**
	 * @return the texts, the templates of the rows, the URLs of the users and of the countries,
	 *         and the values as the page has them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "single", "multiple", "owner",
			"owner.placeholder", "team", "team.placeholder", "countries", "countries.placeholder",
			"send", "nothing", "choices", "sent");
		texts.put("required", getString("Required", Model.ofMap(Map.of("label",
			getString("owner")))));
		texts.put("noMatch", getString("Autocomplete.noMatch", null, "No matches"));
		texts.put("searching", getString("Autocomplete.searching", null, "Searching…"));
		texts.put("remove", getString("Dropdown.remove", null, "Remove {0}"));
		JSONArray teamValues = new JSONArray();
		team.forEach(user -> teamValues.put(value(String.valueOf(user.getId()), name(user))));
		JSONArray countryValues = new JSONArray();
		countries.forEach(country -> countryValues.put(value(country.getCode(),
			country.getName())));
		JSONObject values = new JSONObject()
			.put("owner", owner != null ? new JSONArray().put(value(
				String.valueOf(owner.getId()), name(owner))) : new JSONArray())
			.put("team", teamValues)
			.put("countries", countryValues);
		return new JSONObject()
			.put("texts", texts)
			.put("userTemplate", USER_TEMPLATE)
			.put("countryTemplate", COUNTRY_TEMPLATE)
			.put("values", values)
			.put("usersUrl", FrameworkTabs.urlOf(this, UsersRowsResource.REFERENCE))
			.put("choicesUrl", FrameworkTabs.urlOf(this, ChoicesResource.REFERENCE));
	}

	private static JSONObject value(String id, String label)
	{
		return new JSONObject().put("id", id).put("label", label);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(AutocompletePage.class, "AutocompletePage.css")));
	}

	/**
	 * Suggests the users whose first or last name contains the text, as the table's filter
	 * finds them.
	 */
	private static class UsersAutocompleteProvider implements IAutocompleteProvider<User>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public List<User> search(String term, int max)
		{
			UsersProvider provider = new UsersProvider();
			provider.setFilter(term);
			List<User> users = new ArrayList<>();
			provider.iterator(0, max).forEachRemaining(users::add);
			return users;
		}

		@Override
		public String getId(User user)
		{
			return String.valueOf(user.getId());
		}

		@Override
		public User fromId(String id)
		{
			try
			{
				return DatabaseLocator.getLargeDatabase().get(Long.parseLong(id));
			}
			catch (RuntimeException gone)
			{
				return null;
			}
		}

		@Override
		public String getLabel(User user)
		{
			return name(user);
		}
	}

	/**
	 * Suggests the countries whose name, in the language of the session, contains the text.
	 */
	private static class CountriesProvider implements IAutocompleteProvider<Country>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public List<Country> search(String term, int max)
		{
			String lower = term.toLowerCase(locale());
			return Countries.codes().getObject().stream()
				.map(Country::of)
				.filter(country -> country.getName().toLowerCase(locale()).contains(lower))
				.limit(max)
				.toList();
		}

		@Override
		public String getId(Country country)
		{
			return country.getCode();
		}

		@Override
		public Country fromId(String id)
		{
			return Arrays.asList(Locale.getISOCountries()).contains(id) ? Country.of(id) : null;
		}

		@Override
		public String getLabel(Country country)
		{
			return country.getName();
		}

		private static Locale locale()
		{
			return Session.get().getLocale();
		}
	}

	/**
	 * A country as the template of its row reads it: code, name in the language of the session
	 * and flag.
	 */
	public static final class Country implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private final String code;

		private Country(String code)
		{
			this.code = code;
		}

		static Country of(String code)
		{
			return new Country(code);
		}

		public String getCode()
		{
			return code;
		}

		public String getName()
		{
			return Countries.name(code, Session.get().getLocale());
		}

		/**
		 * @return the flag of the country: its two letters as regional indicator symbols
		 */
		public String getFlag()
		{
			return code.chars()
				.map(letter -> 0x1F1E6 + letter - 'A')
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
		}

		@Override
		public boolean equals(Object other)
		{
			return other instanceof Country country && country.code.equals(code);
		}

		@Override
		public int hashCode()
		{
			return code.hashCode();
		}
	}

	public User getOwner()
	{
		return owner;
	}

	public void setOwner(User owner)
	{
		this.owner = owner;
	}

	public List<User> getTeam()
	{
		return team;
	}

	public void setTeam(Collection<User> team)
	{
		this.team = new ArrayList<>(team);
	}

	public List<Country> getCountries()
	{
		return countries;
	}

	public void setCountries(Collection<Country> countries)
	{
		this.countries = new ArrayList<>(countries);
	}
}
