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
package com.antiliacode.widgets.tabs;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class TabsOverflowMenuBehaviorTest extends WicketTestCase
{
	private static TabbedPanel tabs()
	{
		TabbedPanel tabs = new TabbedPanel("tabs");
		for (String name : new String[] { "one", "two", "three" })
		{
			tabs.addTab(new Tab(name, Model.of("Title " + name))
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected Component newPanel(String id)
				{
					return new Label(id, "Panel " + name);
				}
			});
		}
		return tabs;
	}

	@Test
	void attachesTheMenuOfAllTabsAfterTheTabs()
	{
		TabbedPanel tabs = tabs();
		tabs.add(new TabsOverflowMenuBehavior());
		tester.startPage(new TabbedPanelTest.TestPage(tabs));

		String html = tester.getLastResponseAsString();
		tester.assertContains("TabsOverflowMenuBehavior/antilia-tabs-overflow-menu.*\\.js");
		tester.assertContains("menu.Menu/antilia-menu.*\\.css");
		tester.assertContains("TabsStyleBehavior/antilia-tabs.*\\.css");
		String attach = "Wicket.TabsOverflowMenu.attach(\"" + tabs.getTabsMarkupId() +
			"\", {\"label\":\"All tabs\"});";
		assertTrue(html.contains(attach), html);
		assertTrue(html.indexOf("Wicket.Tabs.attach(") < html.indexOf(attach), "after the tabs");
	}

	@Test
	void saysItsLabelInTheSessionsLanguage()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		TabbedPanel tabs = tabs();
		tabs.add(new TabsOverflowMenuBehavior());
		tester.startPage(new TabbedPanelTest.TestPage(tabs));
		tester.assertContains("\\{\"label\":\"Alle Tabs\"\\}");
	}

	@Test
	void goesOnTabbedPanelsAlone()
	{
		Label label = new Label("label", "text");
		assertThrows(IllegalArgumentException.class, () -> label.add(
			new TabsOverflowMenuBehavior()));
	}
}
