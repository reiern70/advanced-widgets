/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An account that may open the demos: its name, a salted hash of its password and its
 * {@link Role}.
 */
@Entity
@Table(name = "users")
public class User implements Serializable
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

	public Role getRole()
	{
		return role;
	}

	public Instant getCreated()
	{
		return created;
	}
}
