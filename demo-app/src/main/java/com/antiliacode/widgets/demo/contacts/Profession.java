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
package com.antiliacode.widgets.demo.contacts;

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
