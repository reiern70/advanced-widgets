/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.Page;

import com.antilia.widgets.demo.autocomplete.AutocompletePage;
import com.antilia.widgets.demo.button.ButtonsPage;
import com.antilia.widgets.demo.datatable.BeanDynamicDataTablePage;
import com.antilia.widgets.demo.datatable.DynamicDataTablePage;
import com.antilia.widgets.demo.datatable.MasterDetailPage;
import com.antilia.widgets.demo.dropdown.DropdownPage;
import com.antilia.widgets.demo.list.JsonListPage;
import com.antilia.widgets.demo.modal.ModalDialogPage;
import com.antilia.widgets.demo.progress.ProgressBarPage;
import com.antilia.widgets.demo.scrollbar.ScrollbarPage;
import com.antilia.widgets.demo.theme.ThemeEditorPage;
import com.antilia.widgets.demo.tree.TreePage;
import com.antilia.widgets.demo.tree.TreeGridPage;

/**
 * The demo pages, in the order the home page lists them and the previous and next links of a demo
 * follow.
 */
public final class Demos
{
	/**
	 * A demo page. Its texts are in the bundle of {@link BasePage}: the name under
	 * {@code demo.<id>}, what it shows under {@code demo.<id>.description} and its category under
	 * {@code category.<category>}.
	 *
	 * @param id
	 *            the id of the demo in the keys of its texts
	 * @param category
	 *            the category shown on the home page, a key under {@code category.}
	 * @param page
	 *            the page of the demo
	 * @param publicComponent
	 *            whether the component the demo shows is public, or private for now
	 * @param frameworkApps
	 *            whether the demo shows the component in React and Svelte too, not only in Wicket
	 */
	public record Demo(String id, String category, Class<? extends Page> page,
		boolean publicComponent, boolean frameworkApps) implements Serializable
	{
		/**
		 * @return the path of the screenshot of the demo, in the web application
		 */
		public String imagePath()
		{
			return "images/demos/" + id + ".png";
		}

		/**
		 * @return the key of the name shown on the home page and in the links of the demos
		 */
		public String nameKey()
		{
			return "demo." + id;
		}

		/**
		 * @return the key of what the demo shows
		 */
		public String descriptionKey()
		{
			return "demo." + id + ".description";
		}

		/**
		 * @return the key of the category
		 */
		public String categoryKey()
		{
			return "category." + category;
		}
	}

	private static final List<Demo> ALL = List.of(
		new Demo("jsonTable", "data", DynamicDataTablePage.class, false, true),
		new Demo("beanTable", "data", BeanDynamicDataTablePage.class, true, false),
		new Demo("jsonList", "data", JsonListPage.class, false, true),
		new Demo("tree", "data", TreePage.class, false, true),
		new Demo("treeGrid", "data", TreeGridPage.class, false, true),
		new Demo("masterDetail", "data", MasterDetailPage.class, false, false),
		new Demo("progressBar", "feedback", ProgressBarPage.class, true, true),
		new Demo("dropdown", "forms", DropdownPage.class, true, true),
		new Demo("autocomplete", "forms", AutocompletePage.class, true, true),
		new Demo("buttons", "forms", ButtonsPage.class, true, true),
		new Demo("scrollbar", "layout", ScrollbarPage.class, true, true),
		new Demo("modalDialog", "layout", ModalDialogPage.class, true, true),
		new Demo("themeEditor", "themes", ThemeEditorPage.class, true, false));

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
