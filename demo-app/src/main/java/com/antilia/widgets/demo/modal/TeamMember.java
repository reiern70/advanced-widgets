/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.modal;

import java.io.Serializable;

/**
 * A member of the team of the modal dialog example, edited in a dialog.
 */
public class TeamMember implements Serializable
{
	private static final long serialVersionUID = 1L;

	private long id;

	private String name;

	private String email;

	private String role = TeamStore.ROLES.get(0);

	public TeamMember()
	{
	}

	TeamMember(long id, String name, String email, String role)
	{
		this.id = id;
		this.name = name;
		this.email = email;
		this.role = role;
	}

	/**
	 * @return a copy, edited by a form without changing the member until it is saved
	 */
	public TeamMember copy()
	{
		return new TeamMember(id, name, email, role);
	}

	/**
	 * @return the id, {@code 0} for a member not saved yet
	 */
	public long getId()
	{
		return id;
	}

	void setId(long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	/**
	 * @return the role, one of {@link TeamStore#ROLES}
	 */
	public String getRole()
	{
		return role;
	}

	public void setRole(String role)
	{
		this.role = role;
	}
}
