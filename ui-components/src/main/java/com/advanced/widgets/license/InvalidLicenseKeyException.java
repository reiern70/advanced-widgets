/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.license;

/**
 * Thrown for a text that is not a valid {@link LicenseKey}.
 *
 * @since 11.0.0
 */
public class InvalidLicenseKeyException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	InvalidLicenseKeyException(String message)
	{
		super(message);
	}

	InvalidLicenseKeyException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
