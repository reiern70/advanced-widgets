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
package com.antiliacode.widgets.demo.dropdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.demo.Countries;
import com.antiliacode.widgets.demo.frameworks.ChoicesResource;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.dropdown.DropdownBehavior;
import com.antiliacode.widgets.dropdown.MultiDropdown;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The examples of {@link Dropdown} and {@link MultiDropdown}: a form of drop-downs, one choice and
 * several, which post their choice like native selects. The choices are shown below the form as
 * Ajax reports them, and again when the form is sent. The {@link DropdownPage} shows it, and a
 * window of the desktop demo; {@link #getFrameworkConfig()} gives the React and Svelte apps the
 * same form.
 */
public class DropdownExamplesPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The sizes of the first example, too few for a filter. */
	private static final List<String> SIZES = List.of("S", "M", "L", "XL");

	/** The skills of the multiple example. */
	private static final List<String> SKILLS = List.of("java", "wicket", "javascript", "css",
		"sql", "react", "svelte", "docker");

	private String size = "M";

	private String country = "DE";

	private List<String> skills = new ArrayList<>(List.of("java", "wicket"));

	private List<String> languages = new ArrayList<>();

	private String disabled = "M";

	/**
	 * @param id
	 *            component id
	 */
	public DropdownExamplesPanel(String id)
	{
		super(id);

		Form<Void> form = new Form<>("form")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				info(new StringResourceModel("sent", DropdownExamplesPanel.this).setParameters(
					describeChoices()).getString());
			}
		};
		add(form);

		Label choices = new Label("choices", this::describeChoices);
		choices.setOutputMarkupId(true);
		add(choices);

		Dropdown<String> sizeChoice = new Dropdown<>("size", new PropertyModel<>(this, "size"),
			SIZES);
		form.add(reportChanges(sizeChoice, choices));

		Dropdown<String> countryChoice = new Dropdown<>("country",
			new PropertyModel<>(this, "country"), Countries.codes(), Countries.renderer());
		countryChoice.getDropdownBehavior().setPlaceholder(new ResourceModel("country.none"));
		form.add(reportChanges(countryChoice, choices));

		MultiDropdown<String> skillsChoice = new MultiDropdown<>("skills",
			new PropertyModel<>(this, "skills"), SKILLS, new SkillRenderer());
		form.add(reportChanges(skillsChoice, choices));

		MultiDropdown<String> languagesChoice = new MultiDropdown<>("languages",
			new PropertyModel<>(this, "languages"), languages(), new LanguageRenderer());
		languagesChoice.getDropdownBehavior()
			.setPlaceholder(new ResourceModel("languages.none"))
			.setFilterThreshold(0);
		form.add(reportChanges(languagesChoice, choices));

		form.add(new Dropdown<>("disabled", new PropertyModel<>(this, "disabled"), SIZES)
			.setEnabled(false));
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
		});
		return choice;
	}

	/**
	 * @return what is chosen, in words
	 */
	private String describeChoices()
	{
		Locale locale = getLocale();
		String none = getString("nothing");
		return new StringResourceModel("choices", this).setParameters(size,
			country != null ? Countries.name(country, locale) : none,
			skills.isEmpty() ? none : skills.stream().map(this::skillName)
				.collect(Collectors.joining(", ")),
			languages.isEmpty() ? none : languages.stream()
				.map(language -> Locale.forLanguageTag(language).getDisplayLanguage(locale))
				.collect(Collectors.joining(", ")))
			.getString();
	}

	private String skillName(String skill)
	{
		return getString("skill." + skill);
	}

	/**
	 * @return the codes of some languages
	 */
	private static List<String> languages()
	{
		return List.of("en", "de", "es", "fr", "it", "pt", "nl", "sv", "pl", "cs", "el", "tr",
			"ja", "zh", "ko", "ar");
	}

	/**
	 * @return the configuration of the React and Svelte apps of the drop-downs, {@code dropdown} in
	 *         the bundles built from {@code frontend/demo}: the texts, the options but the
	 *         countries, which come from the {@link ChoicesResource}, and the choices as the panel
	 *         has them
	 */
	public JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "single", "multiple", "disabledTitle",
			"size", "country", "country.none", "skills", "languages", "languages.none", "send",
			"nothing", "choices", "sent");
		JSONArray skillChoices = new JSONArray();
		SKILLS.forEach(skill -> skillChoices.put(choice(skill, skillName(skill))));
		JSONArray languageChoices = new JSONArray();
		LanguageRenderer languageRenderer = new LanguageRenderer();
		languages().forEach(language -> languageChoices.put(choice(language,
			languageRenderer.getDisplayValue(language).toString())));
		JSONObject values = new JSONObject()
			.put("size", size)
			.put("country", country != null ? country : "")
			.put("skills", new JSONArray(skills))
			.put("languages", new JSONArray(languages))
			.put("disabled", disabled);
		return new JSONObject()
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("sizes", new JSONArray(SIZES))
			.put("skills", skillChoices)
			.put("languages", languageChoices)
			.put("values", values)
			.put("choicesUrl", FrameworkTabs.urlOf(this, ChoicesResource.REFERENCE));
	}

	private static JSONObject choice(String value, String label)
	{
		return new JSONObject().put("value", value).put("label", label);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		DropdownBehavior.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(DropdownExamplesPanel.class, "DropdownExamplesPanel.css")));
	}

	/**
	 * Shows a language by its own name, as language pickers do.
	 */
	private static class LanguageRenderer extends ChoiceRenderer<String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Object getDisplayValue(String code)
		{
			Locale language = Locale.forLanguageTag(code);
			String name = language.getDisplayLanguage(language);
			return name.substring(0, 1).toUpperCase(language) + name.substring(1);
		}

		@Override
		public String getIdValue(String code, int index)
		{
			return code;
		}
	}

	/**
	 * Shows a skill by its name from the page's bundle.
	 */
	private class SkillRenderer implements IChoiceRenderer<String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Object getDisplayValue(String skill)
		{
			return skillName(skill);
		}

		@Override
		public String getIdValue(String skill, int index)
		{
			return skill;
		}

		@Override
		public String getObject(String id, IModel<? extends List<? extends String>> choices)
		{
			return choices.getObject().contains(id) ? id : null;
		}
	}

	/**
	 * @return the size, for the property model
	 */
	public String getSize()
	{
		return size;
	}

	/**
	 * @param size
	 *            the size
	 */
	public void setSize(String size)
	{
		this.size = size;
	}

	/**
	 * @return the code of the country
	 */
	public String getCountry()
	{
		return country;
	}

	/**
	 * @param country
	 *            the code of the country
	 */
	public void setCountry(String country)
	{
		this.country = country;
	}

	/**
	 * @return the skills
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
	 * @return the codes of the languages
	 */
	public List<String> getLanguages()
	{
		return languages;
	}

	/**
	 * @param languages
	 *            the codes of the languages
	 */
	public void setLanguages(List<String> languages)
	{
		this.languages = new ArrayList<>(languages);
	}

	/**
	 * @return the size of the disabled drop-down
	 */
	public String getDisabled()
	{
		return disabled;
	}

	/**
	 * @param disabled
	 *            the size of the disabled drop-down
	 */
	public void setDisabled(String disabled)
	{
		this.disabled = disabled;
	}
}
