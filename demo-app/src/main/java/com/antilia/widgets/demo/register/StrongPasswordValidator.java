/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
	static boolean isStrong(String password)
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
