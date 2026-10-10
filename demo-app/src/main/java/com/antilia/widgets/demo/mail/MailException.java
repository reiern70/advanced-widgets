/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.mail;

/**
 * A {@link Mail} could not be sent.
 */
public class MailException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param message
	 *            what went wrong
	 * @param cause
	 *            why
	 */
	public MailException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
