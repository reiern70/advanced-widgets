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

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * What is kept of an account once it expired and was deleted, for the statistics: nothing telling
 * who it was, no name, email, address, user agent or text typed, only the month of registering,
 * the country, the language, the {@link JobRole}, how many accesses it had and how many of them
 * opened the demos. See {@link com.antiliacode.widgets.demo.account.ExpiredAccounts}.
 */
@Entity
@Table(name = "former_accounts")
public class FormerAccount implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** The first day of the month of registering. */
	@Column(nullable = false)
	private LocalDate registered;

	@Column(nullable = false)
	private LocalDate deleted;

	/** ISO 3166 code. */
	@Column(length = 2)
	private String country;

	@Convert(converter = LanguageConverter.class)
	@Column(length = 5)
	private Language language;

	@Enumerated(EnumType.STRING)
	@Column(name = "job_role", length = 20)
	private JobRole jobRole;

	@Column(nullable = false)
	private int accesses;

	@Column(name = "demo_accesses", nullable = false)
	private int demoAccesses;

	protected FormerAccount()
	{
	}

	/**
	 * @param user
	 *            the account deleted
	 * @param deleted
	 *            the day it is deleted
	 * @param accesses
	 *            how many accesses it had
	 * @param demoAccesses
	 *            how many of them opened the demos
	 */
	public FormerAccount(User user, LocalDate deleted, int accesses, int demoAccesses)
	{
		this.registered = LocalDate.ofInstant(user.getCreated(), ZoneOffset.UTC)
			.withDayOfMonth(1);
		this.deleted = deleted;
		this.country = user.getCountry();
		this.language = user.getLanguage();
		this.jobRole = user.getJobRole();
		this.accesses = accesses;
		this.demoAccesses = demoAccesses;
	}

	public Long getId()
	{
		return id;
	}

	/**
	 * @return the first day of the month the account registered in
	 */
	public LocalDate getRegistered()
	{
		return registered;
	}

	public LocalDate getDeleted()
	{
		return deleted;
	}

	public String getCountry()
	{
		return country;
	}

	public Language getLanguage()
	{
		return language;
	}

	public JobRole getJobRole()
	{
		return jobRole;
	}

	public int getAccesses()
	{
		return accesses;
	}

	public int getDemoAccesses()
	{
		return demoAccesses;
	}
}
