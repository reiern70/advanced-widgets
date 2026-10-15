/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.error;

/**
 * The page asked for is no longer kept: its session ended, or it left the page store.
 */
public class PageExpiredPage extends ErrorPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Construct.
	 */
	public PageExpiredPage()
	{
		super("pageExpired", 0);
	}
}
