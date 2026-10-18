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
package com.antilia.widgets.demo.datatable;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.util.io.IClusterable;

import com.antilia.widgets.dynamicdatatable.IIdentifiable;

/**
 * A user shown by the table demos. A user not saved in the {@link UsersDatabase} yet has the id
 * {@code 0}.
 */
public class User implements IClusterable, IIdentifiable<Long>
{
	private static final long serialVersionUID = 1L;

	/** The language of the names of the countries outside a request. */
	private static final ThreadLocal<Locale> LANGUAGE = new ThreadLocal<>();

	private long id;

	private String firstName;

	private String lastName;

	private String homePhone;

	private String cellPhone;

	private Date bornDate;

	private String address;

	private String city;

	/** ISO 3166 code of the country. */
	private String country;

	/** How far the simulated task of the user is, in percent; changed by another thread. */
	private volatile int progress;

	public User()
	{
	}

	/**
	 * @param firstName
	 *            the first name
	 * @param lastName
	 *            the last name
	 */
	public User(String firstName, String lastName)
	{
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Override
	public Long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public String getLastName()
	{
		return lastName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public String getHomePhone()
	{
		return homePhone;
	}

	public void setHomePhone(String homePhone)
	{
		this.homePhone = homePhone;
	}

	public String getCellPhone()
	{
		return cellPhone;
	}

	public void setCellPhone(String cellPhone)
	{
		this.cellPhone = cellPhone;
	}

	public Date getBornDate()
	{
		return bornDate;
	}

	public void setBornDate(Date bornDate)
	{
		this.bornDate = bornDate;
	}

	/**
	 * @return the day the user was born as {@code yyyy-MM-dd}, {@code null} if it is not known;
	 *         the JSON based table shows it
	 */
	public String getBorn()
	{
		return bornDate == null ? null
			: bornDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
	}

	/**
	 * @return the first letters of the first and the last name, as on the cards of the infinite
	 *         list example
	 */
	public String getInitials()
	{
		return initial(firstName) + initial(lastName);
	}

	private static String initial(String name)
	{
		return name == null || name.isBlank() ? ""
			: name.strip().substring(0, name.strip().offsetByCodePoints(0, 1)).toUpperCase();
	}

	public String getAddress()
	{
		return address;
	}

	public void setAddress(String address)
	{
		this.address = address;
	}

	public String getCity()
	{
		return city;
	}

	public void setCity(String city)
	{
		this.city = city;
	}

	/**
	 * @return the ISO 3166 code of the country
	 */
	public String getCountry()
	{
		return country;
	}

	/**
	 * @param country
	 *            the ISO 3166 code of the country
	 */
	public void setCountry(String country)
	{
		this.country = country;
	}

	/**
	 * @return the name of the country in the language of the session, or of
	 *         {@link #inLanguage(Locale, IOSupplier)}, in English outside both; {@code null} if it is
	 *         not known
	 */
	public String getCountryName()
	{
		if (country == null)
		{
			return null;
		}
		Locale language = LANGUAGE.get();
		if (language == null)
		{
			language = Session.exists() ? Session.get().getLocale() : Locale.ENGLISH;
		}
		return Locale.of("", country).getDisplayCountry(language);
	}

	/**
	 * Runs code outside a request, such as pushing rows from a timer, with the names of the
	 * countries in a language.
	 *
	 * @param language
	 *            the language of the names of the countries
	 * @param code
	 *            the code to run
	 * @return what the code returned
	 * @throws IOException
	 *             if the code threw it
	 */
	public static <R> R inLanguage(Locale language, IOSupplier<R> code) throws IOException
	{
		Locale previous = LANGUAGE.get();
		LANGUAGE.set(language);
		try
		{
			return code.get();
		}
		finally
		{
			LANGUAGE.set(previous);
		}
	}

	/**
	 * Supplies a value, possibly failing with an {@link IOException}.
	 */
	@FunctionalInterface
	public interface IOSupplier<R>
	{
		/**
		 * @return the value
		 * @throws IOException
		 *             if supplying failed
		 */
		R get() throws IOException;
	}

	public int getProgress()
	{
		return progress;
	}

	public void setProgress(int progress)
	{
		this.progress = progress;
	}

	/**
	 * Users are the same when they have the same id; a user not saved yet is only equal to itself.
	 */
	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		return other instanceof User user && id != 0 && id == user.id;
	}

	@Override
	public int hashCode()
	{
		return id == 0 ? System.identityHashCode(this) : Long.hashCode(id);
	}

	@Override
	public String toString()
	{
		return "User " + id + ": " + firstName + " " + lastName;
	}
}
