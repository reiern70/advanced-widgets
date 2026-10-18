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
import java.time.Instant;

import com.antilia.widgets.dynamicdatatable.IIdentifiable;

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
 * An account that may open the demos: its name, a salted hash of its password, its
 * {@link Role} and an email, which a configured account may lack. A visitor who registered has the
 * email as name too, tells the first and last name, the country, the {@link JobRole} and the
 * {@link Language} of the emails, and opens the demos once the email is
 * {@link #isValidated() validated}.
 */
@Entity
@Table(name = "users")
public class User implements Serializable, IIdentifiable<Long>
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 254)
	private String name;

	@Column(name = "password_hash", nullable = false, length = 200)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false)
	private Instant created;

	@Column(length = 254)
	private String email;

	@Column(name = "first_name", length = 60)
	private String firstName;

	@Column(name = "last_name", length = 60)
	private String lastName;

	/** ISO 3166 code. */
	@Column(length = 2)
	private String country;

	@Convert(converter = LanguageConverter.class)
	@Column(length = 5)
	private Language language;

	@Enumerated(EnumType.STRING)
	@Column(name = "job_role", length = 20)
	private JobRole jobRole;

	/** What the visitor does, in words, with {@link JobRole#OTHER}. */
	@Column(name = "job_role_other", length = 100)
	private String jobRoleOther;

	@Column(nullable = false)
	private boolean validated;

	/** The SHA-256 of the token in the link of the confirmation email, in hex. */
	@Column(name = "validation_token", length = 64)
	private String validationToken;

	@Column(name = "validation_expires")
	private Instant validationExpires;

	protected User()
	{
	}

	/**
	 * @param name
	 *            the name of the account, in lower case
	 * @param passwordHash
	 *            the salt and the hash of the password
	 * @param role
	 *            what the account may do
	 */
	public User(String name, String passwordHash, Role role)
	{
		this.name = name;
		this.passwordHash = passwordHash;
		this.role = role;
		this.created = Instant.now();
	}

	@Override
	public Long getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getPasswordHash()
	{
		return passwordHash;
	}

	/**
	 * @param passwordHash
	 *            the salt and the hash of the new password
	 */
	public void setPasswordHash(String passwordHash)
	{
		this.passwordHash = passwordHash;
	}

	public Role getRole()
	{
		return role;
	}

	public Instant getCreated()
	{
		return created;
	}

	/**
	 * @return the email, in lower case; {@code null} for a configured account without one
	 */
	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
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

	/**
	 * @return the ISO 3166 code of the country, {@code null} for an account that was not
	 *         registered
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
	 * @return the language of the emails, {@code null} for an account that was not registered
	 */
	public Language getLanguage()
	{
		return language;
	}

	public void setLanguage(Language language)
	{
		this.language = language;
	}

	/**
	 * @return what the visitor does, {@code null} for an account that was not registered
	 */
	public JobRole getJobRole()
	{
		return jobRole;
	}

	public void setJobRole(JobRole jobRole)
	{
		this.jobRole = jobRole;
	}

	/**
	 * @return what the visitor does, in words, with {@link JobRole#OTHER}; else {@code null}
	 */
	public String getJobRoleOther()
	{
		return jobRoleOther;
	}

	public void setJobRoleOther(String jobRoleOther)
	{
		this.jobRoleOther = jobRoleOther;
	}

	/**
	 * @return whether the email was confirmed, or the account was not registered by a visitor
	 */
	public boolean isValidated()
	{
		return validated;
	}

	public void setValidated(boolean validated)
	{
		this.validated = validated;
	}

	public String getValidationToken()
	{
		return validationToken;
	}

	public Instant getValidationExpires()
	{
		return validationExpires;
	}

	/**
	 * @param validationToken
	 *            the SHA-256 of the token in the link of the confirmation email, in hex;
	 *            {@code null} once there is none
	 * @param validationExpires
	 *            until when the link works
	 */
	public void setValidation(String validationToken, Instant validationExpires)
	{
		this.validationToken = validationToken;
		this.validationExpires = validationExpires;
	}
}
