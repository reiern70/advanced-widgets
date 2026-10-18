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
package com.antilia.widgets.demo;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * The countries of the drop-downs of the demos: their ISO codes, shown by their names in the
 * language of the session.
 */
public final class Countries
{
	private Countries()
	{
	}

	/**
	 * @return the codes of all countries, sorted by their names in the language of the session
	 */
	public static IModel<List<String>> codes()
	{
		return new LoadableDetachableModel<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected List<String> load()
			{
				Locale locale = Session.get().getLocale();
				Collator collator = Collator.getInstance(locale);
				return Arrays.stream(Locale.getISOCountries())
					.sorted(Comparator.comparing((String code) -> name(code, locale), collator))
					.toList();
			}
		};
	}

	/**
	 * @return the renderer showing a country by its name in the language of the session
	 */
	public static IChoiceRenderer<String> renderer()
	{
		return new ChoiceRenderer<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Object getDisplayValue(String code)
			{
				return name(code, Session.get().getLocale());
			}

			@Override
			public String getIdValue(String code, int index)
			{
				return code;
			}
		};
	}

	/**
	 * @return the name of the country in the language
	 */
	public static String name(String code, Locale locale)
	{
		return Locale.of("", code).getDisplayCountry(locale);
	}
}
