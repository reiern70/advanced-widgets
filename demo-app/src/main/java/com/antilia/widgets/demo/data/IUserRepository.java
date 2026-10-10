/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The {@link User}s in the table {@code users}.
 */
public interface IUserRepository extends JpaRepository<User, Long>
{
	/**
	 * @param name
	 *            the name of the account, in lower case
	 * @return the account of that name
	 */
	Optional<User> findByName(String name);

	/**
	 * @param name
	 *            the name of the account, in lower case
	 * @return whether there is an account of that name
	 */
	boolean existsByName(String name);
}
