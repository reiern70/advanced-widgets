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
package com.antilia.widgets.demo.register;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * Accepts a strong password only: {@value #MINIMUM_LENGTH} to {@value #MAXIMUM_LENGTH}
 * characters, with a lower case and an upper case letter, a digit and another character, as a
 * symbol or a space. The message is the one of the key {@code StrongPasswordValidator}, with the
 * variables {@code minimum} and {@code maximum}.
 */
public class StrongPasswordValidator implements IValidator<String>
{
	private static final long serialVersionUID = 1L;

	/** The fewest characters of a password. */
	public static final int MINIMUM_LENGTH = 10;

	/** The most characters of a password. */
	public static final int MAXIMUM_LENGTH = 128;

	@Override
	public void validate(IValidatable<String> validatable)
	{
		if (!isStrong(validatable.getValue()))
		{
			validatable.error(new ValidationError(this)
				.setVariable("minimum", MINIMUM_LENGTH)
				.setVariable("maximum", MAXIMUM_LENGTH));
		}
	}

	/**
	 * @param password
	 *            a password
	 * @return whether it is strong enough
	 */
	public static boolean isStrong(String password)
	{
		if (password == null || password.length() < MINIMUM_LENGTH ||
			password.length() > MAXIMUM_LENGTH)
		{
			return false;
		}
		boolean lower = false;
		boolean upper = false;
		boolean digit = false;
		boolean other = false;
		for (int i = 0; i < password.length(); i++)
		{
			char c = password.charAt(i);
			if (Character.isLowerCase(c))
			{
				lower = true;
			}
			else if (Character.isUpperCase(c))
			{
				upper = true;
			}
			else if (Character.isDigit(c))
			{
				digit = true;
			}
			else
			{
				other = true;
			}
		}
		return lower && upper && digit && other;
	}
}
