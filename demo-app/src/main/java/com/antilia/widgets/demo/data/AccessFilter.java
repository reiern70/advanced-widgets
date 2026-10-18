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
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * Which {@link Access}es, or {@link OldAccess}es, an administrator looks at: those whose user
 * contains a text, ignoring case, or is exactly a name; those of the public or of the private
 * part; those from an address that contains another text; and those between two times. What is
 * left empty does not filter.
 */
public class AccessFilter implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String user;

	private String exactUser;

	private Boolean privatePart;

	private String address;

	private Instant from;

	private Instant to;

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
	 * @return the first time of the accesses, {@code null} for all
	 */
	public Instant getFrom()
	{
		return from;
	}

	public void setFrom(Instant from)
	{
		this.from = from;
	}

	/**
	 * @return the time the accesses are before, {@code null} for all
	 */
	public Instant getTo()
	{
		return to;
	}

	public void setTo(Instant to)
	{
		this.to = to;
	}

	/**
	 * @param <A>
	 *            the accesses or the old accesses
	 * @return the filter as the query of the repository
	 */
	public <A extends AbstractAccess> Specification<A> toSpecification()
	{
		// the fields of the filter, read now: the specification runs later
		String byUser = UserFilter.pattern(user);
		String byExactUser = exactUser;
		Boolean byPart = privatePart;
		String byAddress = UserFilter.pattern(address);
		Instant byFrom = from;
		Instant byTo = to;
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
			if (byFrom != null)
			{
				predicates.add(builder.greaterThanOrEqualTo(access.get("accessed"), byFrom));
			}
			if (byTo != null)
			{
				predicates.add(builder.lessThan(access.get("accessed"), byTo));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
