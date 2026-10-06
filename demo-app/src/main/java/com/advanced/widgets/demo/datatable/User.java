/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import java.util.Date;
import java.util.Locale;

import org.apache.wicket.util.io.IClusterable;

import com.advanced.widgets.dynamicdatatable.IIdentifiable;

/**
 * A user shown by the table demos. A user not saved in the {@link UsersDatabase} yet has the id
 * {@code 0}.
 */
public class User implements IClusterable, IIdentifiable<Long>
{
	private static final long serialVersionUID = 1L;

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
	 * @return the English name of the country, {@code null} if it is not known
	 */
	public String getCountryName()
	{
		return country == null ? null : Locale.of("", country).getDisplayCountry(Locale.ENGLISH);
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
