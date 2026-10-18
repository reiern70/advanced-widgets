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
package com.antilia.widgets.demo.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.util.string.Strings;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Which {@link User}s an administrator looks at: those of a country, those whose name, first name
 * or last name contains a text and those whose email contains another, ignoring case. What is
 * left empty does not filter.
 */
public class UserFilter implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String country;

	private String name;

	private String email;

	/**
	 * @return the ISO 3166 code of the country, {@code null} for all
	 */
	public String getCountry()
	{
		return country;
	}

	public void setCountry(String country)
	{
		this.country = country;
	}

	/**
	 * @return the text the name of the account, the first or the last name contains, {@code null}
	 *         for all
	 */
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the text the email contains, {@code null} for all
	 */
	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	/**
	 * @return the filter as the query of the repository
	 */
	public Specification<User> toSpecification()
	{
		// the fields of the filter, read now: the specification runs later
		String byCountry = country;
		String byName = pattern(name);
		String byEmail = pattern(email);
		return (user, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (!Strings.isEmpty(byCountry))
			{
				predicates.add(builder.equal(user.get("country"), byCountry));
			}
			if (byName != null)
			{
				Expression<String> account = builder.lower(user.get("name"));
				Expression<String> first = builder.lower(user.get("firstName"));
				Expression<String> last = builder.lower(user.get("lastName"));
				predicates.add(builder.or(builder.like(account, byName, '\\'),
					builder.like(first, byName, '\\'), builder.like(last, byName, '\\')));
			}
			if (byEmail != null)
			{
				predicates.add(builder.like(builder.lower(user.get("email")), byEmail, '\\'));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	/**
	 * @return the LIKE pattern of a text contained, its wildcards escaped; {@code null} for an
	 *         empty text
	 */
	static String pattern(String text)
	{
		if (Strings.isEmpty(text) || text.isBlank())
		{
			return null;
		}
		String escaped = text.strip()
			.toLowerCase(Locale.ROOT)
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_");
		return "%" + escaped + "%";
	}
}
