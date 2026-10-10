/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import org.apache.wicket.Component;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.HomePage;
import com.antilia.widgets.demo.TestDatabase;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.data.JobRole;
import com.antilia.widgets.demo.data.Language;
import com.antilia.widgets.demo.data.Role;

/**
 * The {@link AdminPage}: the accounts in a table filtered by country, name and email, and a tab
 * with the accesses.
 */
class AdminPageTest
{
	private WicketTester tester;

	@BeforeEach
	void start()
	{
		TestDatabase.clear();
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return RuntimeConfigurationType.DEPLOYMENT;
			}
		});
		register("ada@example.com", "Ada", "Lovelace", "GB");
		register("grace@example.org", "Grace", "Hopper", "US");
		tester.startPage(HomePage.class);
		DemoSession.get().signIn("admin", Role.ADMIN);
		tester.startPage(AdminPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void theUsersAreTheFirstTab()
	{
		tester.assertRenderedPage(AdminPage.class);
		tester.assertComponent("tabs:panel", UsersPanel.class);
		tester.assertLabel("tabs:panel:count", "5");
		tester.assertContains("ada@example.com");
		tester.assertContains("Lovelace");
		tester.assertContains("Hopper");
		tester.assertContains("United States");
		tester.assertContains(">Administrator<");
		tester.assertContains("Other: Mathematician");
	}

	@Test
	void theUsersAreFiltered()
	{
		filter("country", "US", "change");
		tester.assertLabel("tabs:panel:count", "1");
		tester.assertContains("grace@example.org");
		tester.assertContainsNot("ada@example.com");

		filter("country", "", "change");
		filter("name", "love", "input");
		tester.assertLabel("tabs:panel:count", "1");
		tester.assertContains("ada@example.com");

		filter("name", "", "input");
		filter("email", "example", "input");
		tester.assertLabel("tabs:panel:count", "2");
	}

	@Test
	void theAccessesAreTheSecondTab()
	{
		tester.clickLink("tabs:tabs-container:tabs:1:link");

		tester.assertComponent("tabs:panel", AccessesPanel.class);
		tester.assertContains(">admin<");
	}

	private void filter(String field, String value, String event)
	{
		FormComponent<?> component = find(field);
		tester.getRequest().getPostParameters().setParameterValue(component.getInputName(), value);
		tester.executeAjaxEvent(component, event);
	}

	private FormComponent<?> find(String field)
	{
		UsersFilterPanel panel = tester.getLastRenderedPage().visitChildren(UsersFilterPanel.class,
			(Component component, org.apache.wicket.util.visit.IVisit<UsersFilterPanel> visit) -> visit
				.stop((UsersFilterPanel)component));
		return (FormComponent<?>)panel.get(field);
	}

	private static void register(String email, String firstName, String lastName, String country)
	{
		Accounts accounts = TestDatabase.accounts();
		accounts.register(email, "Analytical-Engine-1843", user -> {
			user.setFirstName(firstName);
			user.setLastName(lastName);
			user.setCountry(country);
			user.setLanguage(Language.ENGLISH);
			user.setJobRole(JobRole.OTHER);
			user.setJobRoleOther("Mathematician");
		});
	}
}
