/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

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
