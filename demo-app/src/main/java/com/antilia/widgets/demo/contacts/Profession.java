/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.contacts;

import java.util.Locale;

import org.apache.wicket.resource.loader.ClassStringResourceLoader;

/**
 * What a contact does for a living. The names are in the bundles of this class, in English,
 * Spanish and German.
 */
public enum Profession
{
	ACCOUNTANT,
	ARCHITECT,
	CHEF,
	DESIGNER,
	ELECTRICIAN,
	ENGINEER,
	JOURNALIST,
	LAWYER,
	MARKETING_SPECIALIST,
	NURSE,
	PHARMACIST,
	PHOTOGRAPHER,
	PHYSICIAN,
	PILOT,
	RESEARCHER,
	SALES_MANAGER,
	SOFTWARE_DEVELOPER,
	TEACHER;

	/**
	 * @param locale
	 *            the language of the name
	 * @return the name of the profession in the language, in English if it has none there
	 */
	public String getName(Locale locale)
	{
		String name = new ClassStringResourceLoader(Profession.class)
			.loadStringResource(Profession.class, name(), locale, null, null);
		return name != null ? name : name();
	}
}
