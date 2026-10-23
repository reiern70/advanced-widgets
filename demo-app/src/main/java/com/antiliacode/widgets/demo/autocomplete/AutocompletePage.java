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
package com.antiliacode.widgets.demo.autocomplete;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.wicket.Session;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.danekja.java.util.function.serializable.SerializableSupplier;

import com.antiliacode.widgets.autocomplete.AbstractAutocomplete;
import com.antiliacode.widgets.autocomplete.Autocomplete;
import com.antiliacode.widgets.autocomplete.AutocompleteAction;
import com.antiliacode.widgets.autocomplete.FindAutocomplete;
import com.antiliacode.widgets.autocomplete.IAutocompleteProvider;
import com.antiliacode.widgets.autocomplete.MultiAutocomplete;
import com.antiliacode.widgets.demo.Countries;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.datatable.DatabaseLocator;
import com.antiliacode.widgets.demo.datatable.FontAwesomeResourceReference;
import com.antiliacode.widgets.demo.datatable.User;
import com.antiliacode.widgets.demo.datatable.UsersProvider;
import com.antiliacode.widgets.demo.datatable.UsersRowsResource;
import com.antiliacode.widgets.demo.frameworks.ChoicesResource;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link Autocomplete} and {@link MultiAutocomplete}: the users of the table demos and the
 * countries suggested as the user types, each row rendered in the browser from a template, the
 * chosen values shown as chips with an x. The choices are shown below the form as Ajax reports
 * them, and again when the form is sent. A {@link FindAutocomplete} adds the countries it finds to
 * a list below it instead of keeping them as chips. The rows of the users end with two actions: an
 * (i) whose tooltip, a template too, shows their address, phones and birthday, toggled by F1 for
 * the active row, and a phone reported to the page via Ajax. The React and Svelte tabs show the same form
 * in those frameworks, the users searched through the {@link UsersRowsResource}, the countries
 * filtered in the browser from the {@link ChoicesResource}.
 */
public class AutocompletePage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The row of a user: name, city and country. */
	static final String USER_TEMPLATE = "<span class=\"ac-user\"><b>{{firstName}} {{lastName}}</b>" +
		"<small>{{city}}, {{countryName}}</small></span>";

	/** The (i) at the end of the row of a user. */
	static final String INFO_TEMPLATE = "<i class=\"fa-solid fa-circle-info\" aria-hidden=\"true\"></i>";

	/** The tooltip of the (i) of a user: address, phones and birthday. */
	static final String USER_TOOLTIP = "<span class=\"ac-info\"><b>{{firstName}} {{lastName}}</b>" +
		"<span>{{address}}, {{city}}, {{countryName}}</span>" +
		"<span><i class=\"fa-solid fa-mobile-screen\" aria-hidden=\"true\"></i> {{cellPhone}}</span>" +
		"<span><i class=\"fa-solid fa-phone\" aria-hidden=\"true\"></i> {{homePhone}}</span>" +
		"<span><i class=\"fa-solid fa-cake-candles\" aria-hidden=\"true\"></i> {{born}}</span></span>";

	/** The phone at the end of the row of a user, reported to the page. */
	static final String CALL_TEMPLATE = "<i class=\"fa-solid fa-phone\" aria-hidden=\"true\"></i>";

	/** The row of a country: its flag and name. */
	static final String COUNTRY_TEMPLATE = "<span class=\"ac-country\"><span class=\"ac-flag\" " +
		"aria-hidden=\"true\">{{flag}}</span>{{name}}</span>";

	private User owner;

	private List<User> team = new ArrayList<>();

	private List<Country> countries = new ArrayList<>();

	/** The reviewer of the disabled autocomplete, which cannot be changed. */
	private User reviewer = DatabaseLocator.getLargeDatabase().get(1L);

	/** The markets of the disabled autocomplete, which cannot be changed. */
	private List<Country> markets = new ArrayList<>(List.of(Country.of("ES"), Country.of("JP"),
		Country.of("BR")));

	/** The countries found with the find bar, in the order they were found. */
	private final List<Country> visits = new ArrayList<>();

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

		// the user whose phone was clicked last, at the end of a suggested row
		Model<String> calling = Model.of("");
		Label call = new Label("calling", calling)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!calling.getObject().isEmpty());
			}
		};
		call.setOutputMarkupPlaceholderTag(true);
		themed.add(call);

		Autocomplete<User> ownerChoice = new Autocomplete<>("owner",
			new PropertyModel<>(this, "owner"), new UsersAutocompleteProvider())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onAction(AjaxRequestTarget target, String action, User user)
			{
				call(target, call, calling, user);
			}
		};
		withUserActions(ownerChoice).setTemplate(USER_TEMPLATE)
			.setPlaceholder(new ResourceModel("owner.placeholder"));
		ownerChoice.setRequired(true).setLabel(new ResourceModel("owner"));
		form.add(reportChanges(ownerChoice, choices));

		MultiAutocomplete<User> teamChoice = new MultiAutocomplete<>("team",
			new PropertyModel<>(this, "team"), new UsersAutocompleteProvider())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onAction(AjaxRequestTarget target, String action, User user)
			{
				call(target, call, calling, user);
			}
		};
		withUserActions(teamChoice).setTemplate(USER_TEMPLATE)
			.setPlaceholder(new ResourceModel("team.placeholder"));
		form.add(reportChanges(teamChoice, choices));

		MultiAutocomplete<Country> countriesChoice = new MultiAutocomplete<>("countries",
			new PropertyModel<>(this, "countries"), new CountriesProvider());
		countriesChoice.setTemplate(COUNTRY_TEMPLATE)
			.setMinLength(0)
			.setPlaceholder(new ResourceModel("countries.placeholder"));
		form.add(reportChanges(countriesChoice, choices));

		// disabled: the values show, greyed out, but cannot be changed
		form.add(new Autocomplete<>("reviewer", new PropertyModel<>(this, "reviewer"),
			new UsersAutocompleteProvider()).setTemplate(USER_TEMPLATE).setEnabled(false));
		form.add(new MultiAutocomplete<>("markets", new PropertyModel<>(this, "markets"),
			new CountriesProvider()).setTemplate(COUNTRY_TEMPLATE).setEnabled(false));

		WebMarkupContainer visitList = new WebMarkupContainer("visits");
		visitList.setOutputMarkupId(true);
		form.add(visitList);
		FindAutocomplete<Country> visitFind = new FindAutocomplete<>("visitFind",
			new CountriesProvider(() -> visits))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onPick(AjaxRequestTarget target, Country country)
			{
				if (!visits.contains(country))
				{
					visits.add(country);
				}
				target.add(visitList);
			}
		};
		visitFind.setTemplate(COUNTRY_TEMPLATE)
			.setMinLength(0)
			.setPlaceholder(new ResourceModel("visits.placeholder"));
		form.add(visitFind);
		visitList.add(new ListView<>("visit", visits)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Country> item)
			{
				Country country = item.getModelObject();
				item.add(new Label("flag", country.getFlag()));
				item.add(new Label("name", country::getName));
				item.add(new AjaxLink<Void>("remove")
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						visits.remove(country);
						target.add(visitList);
					}
				}.add(AttributeModifier.replace("aria-label", () -> getString("Dropdown.remove",
					null, "Remove {0}").replace("{0}", country.getName()))));
			}
		});
		visitList.add(new Label("none", new ResourceModel("visits.none"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(visits.isEmpty());
			}
		});
	}

	/**
	 * Ends the rows of the users with the (i) of their details and their phone.
	 */
	private static <A extends AbstractAutocomplete<User, ?>> A withUserActions(A autocomplete)
	{
		autocomplete.addAction(new AutocompleteAction("info", INFO_TEMPLATE)
			.setTooltip(USER_TOOLTIP).setLabel(new ResourceModel("info")));
		autocomplete.addAction(new AutocompleteAction("call", CALL_TEMPLATE)
			.setLabel(new ResourceModel("call")));
		return autocomplete;
	}

	/**
	 * Tells that a user is being called, as their phone at the end of a row was clicked.
	 */
	private void call(AjaxRequestTarget target, Label call, Model<String> calling, User user)
	{
		calling.setObject(new StringResourceModel("calling", this).setParameters(name(user),
			user.getCellPhone()).getString());
		target.add(call);
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
			"find", "visits", "visits.placeholder", "visits.none", "disabledTitle", "reviewer",
			"markets", "send", "nothing", "choices", "sent", "info", "call", "calling");
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
		JSONArray marketValues = new JSONArray();
		markets.forEach(country -> marketValues.put(value(country.getCode(), country.getName())));
		JSONArray visitValues = new JSONArray();
		visits.forEach(country -> visitValues.put(value(country.getCode(), country.getName())));
		JSONObject values = new JSONObject()
			.put("owner", owner != null ? new JSONArray().put(value(
				String.valueOf(owner.getId()), name(owner))) : new JSONArray())
			.put("team", teamValues)
			.put("countries", countryValues)
			.put("reviewer", new JSONArray().put(value(String.valueOf(reviewer.getId()),
				name(reviewer))))
			.put("markets", marketValues)
			.put("visits", visitValues);
		return new JSONObject()
			.put("texts", texts)
			.put("userTemplate", USER_TEMPLATE)
			.put("infoTemplate", INFO_TEMPLATE)
			.put("userTooltip", USER_TOOLTIP)
			.put("callTemplate", CALL_TEMPLATE)
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
		// the icons of the actions of the rows
		response.render(FontAwesomeResourceReference.styleSheet());
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
	 * Suggests the countries whose name, in the language of the session, contains the text, but
	 * those left out.
	 */
	private static class CountriesProvider implements IAutocompleteProvider<Country>
	{
		private static final long serialVersionUID = 1L;

		private final SerializableSupplier<Collection<Country>> excluded;

		CountriesProvider()
		{
			this(List::of);
		}

		/**
		 * @param excluded
		 *            the countries not suggested
		 */
		CountriesProvider(SerializableSupplier<Collection<Country>> excluded)
		{
			this.excluded = excluded;
		}

		@Override
		public List<Country> search(String term, int max)
		{
			String lower = term.toLowerCase(locale());
			Collection<Country> left = excluded.get();
			return Countries.codes().getObject().stream()
				.map(Country::of)
				.filter(country -> !left.contains(country))
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

	public User getReviewer()
	{
		return reviewer;
	}

	public void setReviewer(User reviewer)
	{
		this.reviewer = reviewer;
	}

	public List<Country> getMarkets()
	{
		return markets;
	}

	public void setMarkets(Collection<Country> markets)
	{
		this.markets = new ArrayList<>(markets);
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
