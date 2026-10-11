/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.contacts;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.apache.wicket.Session;

import com.antilia.widgets.demo.Countries;
import com.antilia.widgets.dynamicdatatable.IIdentifiable;

/**
 * A person of the address book: name, email, phone, where they live, when they were born and
 * what they do. The table renders its rows from the getters, as JSON; the names of the country
 * and of the profession and the day of birth are in the language of the session.
 */
public class Contact implements IIdentifiable<Long>, Serializable
{
	private static final long serialVersionUID = 1L;

	private final long id;

	private final String firstName;

	private final String lastName;

	private final String email;

	private final String phone;

	private final String city;

	private final String country;

	private final LocalDate birthDate;

	private final Profession profession;

	Contact(long id, String firstName, String lastName, String email, String phone, String city,
		String country, LocalDate birthDate, Profession profession)
	{
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phone = phone;
		this.city = city;
		this.country = country;
		this.birthDate = birthDate;
		this.profession = profession;
	}

	@Override
	public Long getId()
	{
		return id;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public String getLastName()
	{
		return lastName;
	}

	/**
	 * @return the first and the last name
	 */
	public String getName()
	{
		return firstName + " " + lastName;
	}

	/**
	 * @return the first letters of the first and the last name, shown in the avatar
	 */
	public String getInitials()
	{
		return firstName.substring(0, 1) + lastName.substring(0, 1);
	}

	public String getEmail()
	{
		return email;
	}

	public String getPhone()
	{
		return phone;
	}

	public String getCity()
	{
		return city;
	}

	/**
	 * @return the ISO 3166 code of the country the contact lives in
	 */
	public String getCountry()
	{
		return country;
	}

	/**
	 * @return the name of the country, in the language of the session
	 */
	public String getCountryName()
	{
		return Countries.name(country, locale());
	}

	public LocalDate getBirthDate()
	{
		return birthDate;
	}

	/**
	 * @return the year the contact was born, the value of the facet of birth years
	 */
	public int getBirthYear()
	{
		return birthDate.getYear();
	}

	/**
	 * @return the day of birth, written as the language of the session writes dates
	 */
	public String getBorn()
	{
		return birthDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
			.withLocale(locale()));
	}

	/**
	 * @return how old the contact is today
	 */
	public int getAge()
	{
		return Period.between(birthDate, LocalDate.now()).getYears();
	}

	public Profession getProfession()
	{
		return profession;
	}

	/**
	 * @return the name of the profession, in the language of the session
	 */
	public String getProfessionName()
	{
		return profession.getName(locale());
	}

	private static Locale locale()
	{
		return Session.exists() ? Session.get().getLocale() : Locale.ENGLISH;
	}
}
