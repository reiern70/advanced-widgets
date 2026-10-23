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
package com.antiliacode.widgets.demo.modal;

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
