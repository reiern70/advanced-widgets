/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.error;

/**
 * No page is at the address asked for, mounted at not-found for the 404 of web.xml.
 */
public class NotFoundPage extends ErrorPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Construct.
	 */
	public NotFoundPage()
	{
		super("notFound", 404);
	}
}
