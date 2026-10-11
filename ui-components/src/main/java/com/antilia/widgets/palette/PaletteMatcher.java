/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.palette;

/**
 * The matchers the filter of a {@link PaletteBehavior palette} has built in. They ignore case and
 * underline the parts of the label they matched.
 *
 * @since 11.0.0
 */
public enum PaletteMatcher implements IPaletteMatcher
{
	/** The label starts with the text. */
	START("start"),

	/** The label contains the text, anywhere. */
	ANYWHERE("anywhere"),

	/** The label ends with the text. */
	END("end"),

	/**
	 * Each word of the text starts a word of the label, in their order: "s ge" matches "South
	 * Georgia", underlining "S" and "Ge".
	 */
	WORDS("words");

	private final String name;

	PaletteMatcher(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}
}
