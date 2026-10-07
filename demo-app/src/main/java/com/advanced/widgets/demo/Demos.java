/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.Page;

import com.advanced.widgets.demo.datatable.BeanDynamicDataTablePage;
import com.advanced.widgets.demo.datatable.DynamicDataTablePage;
import com.advanced.widgets.demo.progress.ProgressBarPage;
import com.advanced.widgets.demo.theme.ThemeEditorPage;

/**
 * The demo pages, in the order the home page lists them and the previous and next links of a demo
 * follow.
 */
public final class Demos
{
	/**
	 * A demo page.
	 *
	 * @param name
	 *            the name shown on the home page
	 * @param category
	 *            the category shown on the home page
	 * @param description
	 *            what the demo shows
	 * @param page
	 *            the page of the demo
	 * @param publicComponent
	 *            whether the component the demo shows is public, or private for now
	 */
	public record Demo(String name, String category, String description,
		Class<? extends Page> page, boolean publicComponent) implements Serializable
	{
	}

	private static final List<Demo> ALL = List.of(
		new Demo("JSON Dynamic Data Table", "Data",
			"A thousand users rendered in the browser from JSON, with live updates over websockets.",
			DynamicDataTablePage.class, false),
		new Demo("Bean Dynamic Data Table", "Data",
			"The same table with its rows rendered on the server by Wicket components.",
			BeanDynamicDataTablePage.class, true),
		new Demo("Progress Bar", "Feedback", "Known and unknown progress, in any of the themes.",
			ProgressBarPage.class, true),
		new Demo("Theme Editor", "Themes",
			"Generates a theme of the components, with a live preview.", ThemeEditorPage.class,
			true));

	private Demos()
	{
	}

	/**
	 * @return all demos, in order
	 */
	public static List<Demo> all()
	{
		return ALL;
	}

	/**
	 * @return the demo of the page, if it is one
	 */
	public static Optional<Demo> of(Class<? extends Page> page)
	{
		int index = indexOf(page);
		return index >= 0 ? Optional.of(ALL.get(index)) : Optional.empty();
	}

	/**
	 * @return the demo before the one of the page, if there is one
	 */
	public static Optional<Demo> previous(Class<? extends Page> page)
	{
		int index = indexOf(page);
		return index > 0 ? Optional.of(ALL.get(index - 1)) : Optional.empty();
	}

	/**
	 * @return the demo after the one of the page, if there is one
	 */
	public static Optional<Demo> next(Class<? extends Page> page)
	{
		int index = indexOf(page);
		return index >= 0 && index < ALL.size() - 1 ? Optional.of(ALL.get(index + 1))
			: Optional.empty();
	}

	private static int indexOf(Class<? extends Page> page)
	{
		for (int i = 0; i < ALL.size(); i++)
		{
			if (ALL.get(i).page().isAssignableFrom(page))
			{
				return i;
			}
		}
		return -1;
	}
}
