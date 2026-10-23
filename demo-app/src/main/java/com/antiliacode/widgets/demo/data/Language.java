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
package com.antiliacode.widgets.demo.data;

import java.util.Locale;

/**
 * A language a {@link User} prefers, the one the emails to the user are written in.
 */
public enum Language
{
	/** English. */
	ENGLISH("en"),

	/** Spanish. */
	SPANISH("es"),

	/** German. */
	GERMAN("de");

	private final String code;

	Language(String code)
	{
		this.code = code;
	}

	/**
	 * @return the ISO 639 code, as kept in the table {@code users}
	 */
	public String getCode()
	{
		return code;
	}

	/**
	 * @return the locale of the language
	 */
	public Locale getLocale()
	{
		return Locale.of(code);
	}

	/**
	 * @param locale
	 *            a locale, may be {@code null}
	 * @return the language of the locale, English for one that is none of them
	 */
	public static Language of(Locale locale)
	{
		return locale == null ? ENGLISH : ofCode(locale.getLanguage());
	}

	/**
	 * @param code
	 *            an ISO 639 code, may be {@code null}
	 * @return the language of the code, English for one that is none of them
	 */
	public static Language ofCode(String code)
	{
		for (Language language : values())
		{
			if (language.code.equals(code))
			{
				return language;
			}
		}
		return ENGLISH;
	}
}
