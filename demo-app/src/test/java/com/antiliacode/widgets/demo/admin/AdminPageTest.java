/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antiliacode.widgets.demo.admin;

import org.apache.wicket.Component;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.data.JobRole;
import com.antiliacode.widgets.demo.data.Language;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.dropdown.DropdownPage;

/**
 * The {@link AdminPage}: the accounts in a table filtered by country, name and email, and tabs
 * with the recent accesses and the old ones.
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
		tester.assertContains(">Account<");
		tester.assertContains(">Email<");
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

		// the name of the account too, which the accounts the demo always has have alone
		filter("name", "wicket", "input");
		tester.assertLabel("tabs:panel:count", "1");

		// the email, which these accounts lack
		filter("name", "", "input");
		filter("email", "example", "input");
		tester.assertLabel("tabs:panel:count", "2");
		filter("email", "wicket", "input");
		tester.assertLabel("tabs:panel:count", "0");
	}

	@Test
	void theAccessesAreTheSecondTab()
	{
		tester.clickLink("tabs:tabs-container:tabs:1:link");

		tester.assertComponent("tabs:panel", AccessesPanel.class);
		// the private part at first, which the administrator did not open yet
		tester.assertLabel("tabs:panel:count", "0");
		tester.startPage(DropdownPage.class);
		tester.startPage(AdminPage.class);
		tester.clickLink("tabs:tabs-container:tabs:1:link");
		tester.assertLabel("tabs:panel:count", "1");
		tester.assertContains(">admin<");
	}

	@Test
	void theOldAccessesAreTheThirdTab()
	{
		tester.clickLink("tabs:tabs-container:tabs:2:link");

		tester.assertComponent("tabs:panel", AccessesPanel.class);
		tester.assertContains("Old accesses");
		tester.assertLabel("tabs:panel:count", "0");
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
