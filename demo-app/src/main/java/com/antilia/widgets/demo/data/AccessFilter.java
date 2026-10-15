/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * Which {@link Access}es an administrator looks at: those whose user contains a text, ignoring
 * case, or is exactly a name; those of the public or of the private part; and those from an
 * address that contains another text. What is left empty does not filter.
 */
public class AccessFilter implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String user;

	private String exactUser;

	private Boolean privatePart;

	private String address;

	/**
	 * @return the text the user contains, {@code null} for all
	 */
	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	/**
	 * @return the user exactly, as kept, {@code null} for all
	 */
	public String getExactUser()
	{
		return exactUser;
	}

	public void setExactUser(String exactUser)
	{
		this.exactUser = exactUser;
	}

	/**
	 * @return {@code true} for the accesses that opened the demos, {@code false} for those that
	 *         did not, {@code null} for all
	 */
	public Boolean getPrivatePart()
	{
		return privatePart;
	}

	public void setPrivatePart(Boolean privatePart)
	{
		this.privatePart = privatePart;
	}

	/**
	 * @return the text the address contains, {@code null} for all
	 */
	public String getAddress()
	{
		return address;
	}

	public void setAddress(String address)
	{
		this.address = address;
	}

	/**
	 * @return the filter as the query of the repository
	 */
	public Specification<Access> toSpecification()
	{
		// the fields of the filter, read now: the specification runs later
		String byUser = UserFilter.pattern(user);
		String byExactUser = exactUser;
		Boolean byPart = privatePart;
		String byAddress = UserFilter.pattern(address);
		return (access, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (byUser != null)
			{
				predicates.add(builder.like(builder.lower(access.get("userName")), byUser, '\\'));
			}
			if (byExactUser != null)
			{
				predicates.add(builder.equal(access.get("userName"), byExactUser));
			}
			if (byPart != null)
			{
				predicates.add(builder.equal(access.get("privatePart"), byPart));
			}
			if (byAddress != null)
			{
				predicates.add(builder.like(builder.lower(access.get("address")), byAddress, '\\'));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
