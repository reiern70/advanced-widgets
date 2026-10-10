/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * The {@link User}s in the table {@code users}, also filtered, see {@link UserFilter}.
 */
public interface IUserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>
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

	/**
	 * @param validationToken
	 *            the SHA-256 of the token in the link of a confirmation email, in hex
	 * @return the account the link confirms
	 */
	Optional<User> findByValidationToken(String validationToken);
}
