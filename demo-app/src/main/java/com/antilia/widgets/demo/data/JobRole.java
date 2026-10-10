/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

/**
 * What a visitor who registers does; with {@link #OTHER} the visitor says it in words, see
 * {@link User#getJobRoleOther()}. Not to be confused with the {@link Role} of the account.
 */
public enum JobRole
{
	DEVELOPER,

	ARCHITECT,

	DESIGNER,

	MANAGER,

	STUDENT,

	/** Something else, told in words. */
	OTHER
}
