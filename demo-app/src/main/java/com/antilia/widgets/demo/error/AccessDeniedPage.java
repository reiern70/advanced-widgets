/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.error;

/**
 * The page is not for the user, such as the administration for a user who is no administrator.
 */
public class AccessDeniedPage extends ErrorPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Construct.
	 */
	public AccessDeniedPage()
	{
		super("accessDenied", 403);
	}
}
