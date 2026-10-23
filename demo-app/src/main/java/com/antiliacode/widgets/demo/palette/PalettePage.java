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
package com.antiliacode.widgets.demo.palette;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.palette.Palette;
import com.antiliacode.widgets.palette.PaletteBehavior;
import com.antiliacode.widgets.palette.PaletteMatcher;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link Palette}: the available choices on the left, the chosen ones on the right and arrows
 * between them, posting the choice like a native select. The skills are chosen in any order, with
 * a filter a check box takes away by Ajax; the
 * columns of a report keep the order they are chosen in, and are moved up and down, while the
 * order matters, which a check box switches by Ajax; their palette has titles of its own and
 * another check box numbers its rows. The countries, some 250 of them, are many choices: their
 * lists draw 30 rows at a time, more as they scroll, and the filter searches them all. Four more
 * palettes of the countries show how a filter can match: from the start of the names, anywhere
 * in them, at their end or at the start of their words, underlining what it matched. Each of
 * these is a case of {@link DemoCases}, one at a time. The choices are shown below the form as
 * Ajax reports them, and again when the form is sent. The React and Svelte tabs show the same
 * form in those frameworks.
 */
public class PalettePage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The skills, enough for a filter. */
	private static final List<String> SKILLS = List.of("java", "wicket", "javascript", "css",
		"sql", "react", "svelte", "docker");

	/** The columns of the report. */
	private static final List<String> COLUMNS = List.of("name", "email", "phone", "city",
		"country", "born", "company");

	private List<String> skills = new ArrayList<>(List.of("java", "wicket"));

	private List<String> countries = new ArrayList<>(List.of("DE", "ES", "UY"));

	private List<String> columns = new ArrayList<>(List.of("name", "country", "email"));

	private boolean ordered = true;

	private boolean numbered = true;

	private boolean filterable = true;

	private List<String> disabled = new ArrayList<>(List.of("sql"));

	/**
	 * Constructor.
	 */
	public PalettePage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		Form<Void> form = new Form<>("form")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				info(new StringResourceModel("sent", PalettePage.this).setParameters(
					describeChoices()).getString());
			}
		};
		themed.add(form);

		Label choices = new Label("choices", this::describeChoices);
		choices.setOutputMarkupId(true);
		themed.add(choices);

		DemoCases cases = new DemoCases("cases", this);
		form.add(cases);
		Fragment unorderedCase = cases.addCase("unordered", "unorderedCase");
		Fragment orderedCase = cases.addCase("orderedTitle", "orderedCase");
		Fragment manyCase = cases.addCase("manyTitle", "manyCase");
		Fragment matchCase = cases.addCase("matchTitle", "matchCase");
		Fragment disabledCase = cases.addCase("disabledTitle", "disabledCase");

		Palette<String> skillsChoice = new Palette<>("skills", new PropertyModel<>(this, "skills"),
			SKILLS, new NameRenderer("skill."))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setFilterable(filterable);
			}
		};
		unorderedCase.add(reportChanges(skillsChoice, choices));

		CheckBox filterableChoice = new CheckBox("filterable",
			new PropertyModel<>(this, "filterable"));
		filterableChoice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(skillsChoice);
			}
		});
		unorderedCase.add(filterableChoice);

		Palette<String> columnsChoice = new Palette<>("columns",
			new PropertyModel<>(this, "columns"), COLUMNS, new NameRenderer("column."))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setOrdered(ordered);
				setNumbered(numbered);
			}
		};
		columnsChoice.setAvailableTitle(new StringResourceModel("columnsAvailable", this))
			.setSelectedTitle(new StringResourceModel("columnsSelected", this));
		orderedCase.add(reportChanges(columnsChoice, choices));

		CheckBox orderedChoice = new CheckBox("ordered", new PropertyModel<>(this, "ordered"));
		orderedChoice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				if (!ordered)
				{
					// the columns take the order of the choices again
					columns.sort(Comparator.comparingInt(COLUMNS::indexOf));
				}
				target.add(columnsChoice, choices);
			}
		});
		orderedCase.add(orderedChoice);

		CheckBox numberedChoice = new CheckBox("numbered", new PropertyModel<>(this, "numbered"));
		numberedChoice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(columnsChoice);
			}
		});
		orderedCase.add(numberedChoice);

		IModel<List<String>> countryChoices = new LoadableDetachableModel<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected List<String> load()
			{
				return countryCodes(getLocale());
			}
		};
		Label countriesChosen = new Label("countriesChosen", this::describeCountries);
		countriesChosen.setOutputMarkupId(true);
		manyCase.add(countriesChosen);
		manyCase.add(reportChanges(new Palette<>("countries",
			new PropertyModel<>(this, "countries"), countryChoices, new CountryRenderer()),
			countriesChosen));

		matchCase.add(newMatchPalette("matchStart", countryChoices, PaletteMatcher.START));
		matchCase.add(newMatchPalette("matchAnywhere", countryChoices, PaletteMatcher.ANYWHERE));
		matchCase.add(newMatchPalette("matchEnd", countryChoices, PaletteMatcher.END));
		matchCase.add(newMatchPalette("matchWords", countryChoices, PaletteMatcher.WORDS));

		disabledCase.add(new Palette<>("disabled", new PropertyModel<>(this, "disabled"), SKILLS,
			new NameRenderer("skill.")).setEnabled(false));
	}

	/**
	 * @return a palette of the countries whose filter matches with the matcher
	 */
	private Palette<String> newMatchPalette(String id, IModel<List<String>> countryChoices,
		PaletteMatcher matcher)
	{
		return new Palette<>(id, new ListModel<>(new ArrayList<>()), countryChoices,
			new CountryRenderer()).setMatcher(matcher);
	}

	private static Palette<String> reportChanges(Palette<String> choice, Label choices)
	{
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(choices);
			}
		});
		return choice;
	}

	/**
	 * @param locale
	 *            the locale of the names
	 * @return the codes of the countries, in the order of their names in the locale
	 */
	static List<String> countryCodes(Locale locale)
	{
		Collator collator = Collator.getInstance(locale);
		return Arrays.stream(Locale.getISOCountries())
			.sorted(Comparator.comparing(code -> countryName(code, locale), collator))
			.toList();
	}

	private static String countryName(String code, Locale locale)
	{
		return Locale.of("", code).getDisplayCountry(locale);
	}

	/**
	 * @return how many countries are chosen, and which
	 */
	private String describeCountries()
	{
		return new StringResourceModel("countriesChosen", this).setParameters(countries.size(),
			countries.isEmpty() ? getString("nothing") : countries.stream()
				.map(code -> countryName(code, getLocale())).collect(Collectors.joining(", ")))
			.getString();
	}

	/**
	 * @return what is chosen, in words
	 */
	private String describeChoices()
	{
		String none = getString("nothing");
		return new StringResourceModel("choices", this).setParameters(
			skills.isEmpty() ? none : skills.stream().map(skill -> getString("skill." + skill))
				.collect(Collectors.joining(", ")),
			columns.isEmpty() ? none : columns.stream()
				.map(column -> getString("column." + column)).collect(Collectors.joining(", ")))
			.getString();
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "palette";
	}

	/**
	 * @return the texts, the choices and the chosen ones as the page has them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "cases", "unordered", "orderedTitle",
			"disabledTitle", "manyTitle", "countries", "countriesChosen", "skills", "columns", "ordered", "numbered",
			"filterable", "columnsAvailable", "columnsSelected", "matchTitle", "matchHint",
			"matchStart", "matchAnywhere", "matchEnd", "matchWords", "send", "nothing", "choices", "sent");
		JSONObject palette = new JSONObject();
		for (String text : List.of("availableTitle", "selectedTitle", "add", "addAll", "remove",
			"removeAll", "up", "down", "filter", "empty"))
		{
			palette.put(text, getString("Palette." + text));
		}
		JSONObject values = new JSONObject()
			.put("skills", new JSONArray(skills))
			.put("columns", new JSONArray(columns))
			.put("ordered", ordered)
			.put("numbered", numbered)
			.put("filterable", filterable)
			.put("countries", new JSONArray(countries))
			.put("disabled", new JSONArray(disabled));
		JSONArray countryChoices = new JSONArray();
		countryCodes(getLocale()).forEach(code -> countryChoices.put(new JSONObject()
			.put("value", code).put("label", countryName(code, getLocale()))));
		return new JSONObject()
			.put("texts", texts)
			.put("palette", palette)
			.put("skills", choices(SKILLS, "skill."))
			.put("columns", choices(COLUMNS, "column."))
			.put("countries", countryChoices)
			.put("values", values);
	}

	private JSONArray choices(List<String> values, String prefix)
	{
		JSONArray choices = new JSONArray();
		values.forEach(value -> choices.put(new JSONObject().put("value", value).put("label",
			getString(prefix + value))));
		return choices;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		PaletteBehavior.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(PalettePage.class, "PalettePage.css")));
	}

	/**
	 * Shows a value by its name from the page's bundle, under a prefix.
	 */
	private class NameRenderer implements IChoiceRenderer<String>
	{
		private static final long serialVersionUID = 1L;

		private final String prefix;

		NameRenderer(String prefix)
		{
			this.prefix = prefix;
		}

		@Override
		public Object getDisplayValue(String value)
		{
			return getString(prefix + value);
		}

		@Override
		public String getIdValue(String value, int index)
		{
			return value;
		}

		@Override
		public String getObject(String id, IModel<? extends List<? extends String>> choices)
		{
			return choices.getObject().contains(id) ? id : null;
		}
	}

	/**
	 * Shows a country by its name in the locale of the page.
	 */
	private class CountryRenderer implements IChoiceRenderer<String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Object getDisplayValue(String code)
		{
			return countryName(code, getLocale());
		}

		@Override
		public String getIdValue(String code, int index)
		{
			return code;
		}

		@Override
		public String getObject(String id, IModel<? extends List<? extends String>> choices)
		{
			return choices.getObject().contains(id) ? id : null;
		}
	}

	/**
	 * @return the countries, for the property model
	 */
	public List<String> getCountries()
	{
		return countries;
	}

	/**
	 * @param countries
	 *            the codes of the countries
	 */
	public void setCountries(List<String> countries)
	{
		this.countries = new ArrayList<>(countries);
	}

	/**
	 * @return the skills, for the property model
	 */
	public List<String> getSkills()
	{
		return skills;
	}

	/**
	 * @param skills
	 *            the skills
	 */
	public void setSkills(List<String> skills)
	{
		this.skills = new ArrayList<>(skills);
	}

	/**
	 * @return the columns of the report, in their order
	 */
	public List<String> getColumns()
	{
		return columns;
	}

	/**
	 * @param columns
	 *            the columns of the report, in their order
	 */
	public void setColumns(List<String> columns)
	{
		this.columns = new ArrayList<>(columns);
	}

	/**
	 * @return whether the order of the columns matters
	 */
	public boolean isOrdered()
	{
		return ordered;
	}

	/**
	 * @param ordered
	 *            whether the order of the columns matters
	 */
	public void setOrdered(boolean ordered)
	{
		this.ordered = ordered;
	}

	/**
	 * @return whether the rows of the columns' palette show their numbers
	 */
	public boolean isNumbered()
	{
		return numbered;
	}

	/**
	 * @param numbered
	 *            whether the rows of the columns' palette show their numbers
	 */
	public void setNumbered(boolean numbered)
	{
		this.numbered = numbered;
	}

	/**
	 * @return whether the skills have a filter
	 */
	public boolean isFilterable()
	{
		return filterable;
	}

	/**
	 * @param filterable
	 *            whether the skills have a filter
	 */
	public void setFilterable(boolean filterable)
	{
		this.filterable = filterable;
	}

	/**
	 * @return the skills of the disabled palette
	 */
	public List<String> getDisabled()
	{
		return disabled;
	}

	/**
	 * @param disabled
	 *            the skills of the disabled palette
	 */
	public void setDisabled(List<String> disabled)
	{
		this.disabled = new ArrayList<>(disabled);
	}
}
