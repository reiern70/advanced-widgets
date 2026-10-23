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
package com.antiliacode.widgets.demo.data;

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
