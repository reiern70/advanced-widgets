/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.wicket.Page;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.datatable.DynamicDataTablePage;
import com.antilia.widgets.demo.dropdown.DropdownPage;
import com.antilia.widgets.demo.tree.TreePage;
import com.antilia.widgets.license.Licensing;
import com.antilia.widgets.tree.Tree;

/**
 * The demo application shows every private component, whatever key the system property holds, and
 * hands its key to the React and Svelte apps of every demo.
 */
class DemoLicenseTest
{
	@Test
	void theKeyOfTheDemoWinsOverAWrongOneOfTheSystem()
	{
		String previous = System.getProperty(Licensing.PROPERTY);
		System.setProperty(Licensing.PROPERTY, "AW1.not.valid");
		WicketTester tester = new WicketTester(new WicketApplication());
		try
		{
			assertTrue(Licensing.get(tester.getApplication()).activates(Tree.class));
			tester.startPage(TreePage.class);
			tester.assertContainsNot(Pattern.quote(Licensing.PLACEHOLDER_CSS_CLASS));
		}
		finally
		{
			tester.destroy();
			if (previous == null)
			{
				System.clearProperty(Licensing.PROPERTY);
			}
			else
			{
				System.setProperty(Licensing.PROPERTY, previous);
			}
		}
	}

	@Test
	void everyAppGetsTheKeyOfTheDemo()
	{
		WicketTester tester = new WicketTester(new WicketApplication());
		try
		{
			for (Class<? extends Page> page : List.of(TreePage.class,
				DynamicDataTablePage.class, DropdownPage.class))
			{
				tester.startPage(page);
				tester.assertContains(Pattern.quote("&quot;licenseKey&quot;:&quot;" +
					DemoLicense.key().substring(0, 12)));
			}
		}
		finally
		{
			tester.destroy();
		}
	}
}
