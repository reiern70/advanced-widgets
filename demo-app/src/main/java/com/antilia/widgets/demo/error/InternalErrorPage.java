/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.error;

/**
 * A page failed on the server.
 */
public class InternalErrorPage extends ErrorPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Construct.
	 */
	public InternalErrorPage()
	{
		super("internalError", 500);
	}
}
