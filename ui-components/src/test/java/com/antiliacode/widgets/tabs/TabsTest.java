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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;

class TabsTest extends WicketTestCase
{
	private static final IIcon ICON = () -> "<svg class=\"icon\"></svg>";

	private static Tab tab(String name, String text)
	{
		return new Tab(name, Model.of("Title " + text))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newPanel(String id)
			{
				return new Label(id, "Panel " + text);
			}
		};
	}

	private static TagTester tagOf(String html, String attribute, String value)
	{
		return TagTester.createTagByAttribute(html, attribute, value);
	}

	@Test
	void rendersTheTabsAndTheirPanelsTheFirstOneShown()
	{
		TestTabs tabs = new TestTabs();
		tabs.addTab(tab("one", "one").setIcon(ICON)).addTab(tab(null, "two"));
		tabs.setLabel(Model.of("Documents"));
		tester.startPage(new TestPage(tabs));
		String html = tester.getLastResponseAsString();

		TagTester element = tester.getTagByWicketId("element");
		assertEquals("antilia-tabs antilia-tabs-horizontal antilia-tabs-tabs",
			element.getAttribute("class"));
		TagTester list = tester.getTagByWicketId("list");
		assertEquals("tablist", list.getAttribute("role"));
		assertEquals("horizontal", list.getAttribute("aria-orientation"));
		assertEquals("Documents", list.getAttribute("aria-label"));

		Tab second = tabs.getTabs().get(1);
		assertEquals("1", second.getName(), "named by the tabs");
		TagTester one = tagOf(html, "data-tab", "one");
		assertEquals("antilia-tab", one.getAttribute("class"));
		assertEquals("presentation", one.getAttribute("role"));
		assertEquals("", one.getAttribute("data-selected"));
		assertNull(one.getAttribute("data-closable"));
		assertTrue(html.contains("<svg class=\"icon\"></svg>"));

		TagTester button = tagOf(html, "id", tabs.button("one").getMarkupId());
		assertEquals("tab", button.getAttribute("role"));
		assertEquals("true", button.getAttribute("aria-selected"));
		assertEquals("0", button.getAttribute("tabindex"));
		TagTester panel = tagOf(html, "id", button.getAttribute("aria-controls"));
		assertEquals("antilia-tabs-panel", panel.getAttribute("class"));
		assertEquals("tabpanel", panel.getAttribute("role"));
		assertEquals(button.getAttribute("id"), panel.getAttribute("aria-labelledby"));
		assertNull(panel.getAttribute("hidden"));
		assertTrue(panel.getValue().contains("Panel one"));

		assertEquals("false", tagOf(html, "id", tabs.button("1").getMarkupId())
			.getAttribute("aria-selected"));
		assertEquals("hidden", tagOf(html, "aria-labelledby", tabs.button("1").getMarkupId())
			.getAttribute("hidden"));

		tester.assertContains("antilia-tabs.*\\.js");
		tester.assertContains("TabsStyleBehavior/antilia-tabs.*\\.css");
		tester.assertContains("antilia-modal.*\\.css");
		tester.assertContains("Wicket.Tabs.attach\\(\"" + tabs.getTabsMarkupId() +
			"\", \\{\"orientation\":\"horizontal\",\"design\":\"tabs\",\"texts\":\\{\"close\":" +
			"\"Close\",");
		tester.assertContains("\"selected\":\"one\",\"label\":\"Documents\"\\}\\);");
		assertFalse(html.contains("style="), "no inline style");
		assertSame(tabs.getTabs().get(0), tabs.getSelectedTab());
		assertNull(tabs.getModelObject());
	}

	@Test
	void givesItsPartsTheClassesOfTheLookTheyAreIn()
	{
		Looks.setSessionLook(Look.BOOTSTRAP);
		TestTabs tabs = new TestTabs();
		tabs.addTab(tab("one", "one").setIcon(ICON)).addTab(tab("two", "two"));
		tester.startPage(new TestPage(tabs));
		String html = tester.getLastResponseAsString();

		assertEquals("antilia-tabs antilia-tabs-horizontal antilia-tabs-tabs",
			tester.getTagByWicketId("element").getAttribute("class"));
		assertEquals("antilia-tabs-list nav nav-tabs",
			tester.getTagByWicketId("list").getAttribute("class"));
		assertEquals("antilia-tab nav-item", tagOf(html, "data-tab", "one").getAttribute("class"));
		assertEquals("antilia-tabs-panel", tagOf(html, "aria-labelledby",
			tabs.button("one").getMarkupId()).getAttribute("class"));

		// a design of the components' own is no nav
		tabs.setDesign(TabsDesign.FOLDER);
		tester.startPage(new TestPage(tabs));
		assertEquals("antilia-tabs-list", tester.getTagByWicketId("list").getAttribute("class"));
		assertEquals("antilia-tabs-folder", TabsDesign.FOLDER.getCssClass());
		assertNull(TabsDesign.FOLDER.getListPart());
		assertEquals(LookPart.TABS_LIST_PILLS, TabsDesign.PILLS.getListPart());
		assertEquals(LookPart.TABS_VERTICAL, TabsOrientation.VERTICAL.getPart());
	}

	@Test
	void rendersTheTabTheModelNamesSelectedVerticalInTheDesignGiven()
	{
		TestTabs tabs = new TestTabs();
		tabs.addTab(tab("one", "one")).addTab(tab("two", "two"))
			.setOrientation(TabsOrientation.VERTICAL).setDesign(TabsDesign.PILLS)
			.setModelObject("two");
		tester.startPage(new TestPage(tabs));
		String html = tester.getLastResponseAsString();

		assertEquals("antilia-tabs antilia-tabs-vertical antilia-tabs-pills",
			tester.getTagByWicketId("element").getAttribute("class"));
		assertEquals("vertical", tester.getTagByWicketId("list").getAttribute("aria-orientation"));
		assertEquals("", tagOf(html, "data-tab", "two").getAttribute("data-selected"));
		assertNull(tagOf(html, "data-tab", "one").getAttribute("data-selected"));
		tester.assertContains("\"orientation\":\"vertical\",\"design\":\"pills\"");
		tester.assertContains("\"selected\":\"two\"");

		// a name the tabs do not have is the first tab
		tabs.setModelObject("nothing");
		assertEquals("one", tabs.getSelectedTab().getName());
		tabs.setOrientation(null).setDesign(null);
		assertEquals(TabsOrientation.HORIZONTAL, tabs.getOrientation());
		assertEquals(TabsDesign.TABS, tabs.getDesign());
		assertNull(new TestTabs().getSelectedTab());

		tabs.setDesign(TabsDesign.STEPS).setOrientation(TabsOrientation.VERTICAL);
		tester.startPage(new TestPage(tabs));
		assertEquals("antilia-tabs antilia-tabs-vertical antilia-tabs-steps",
			tester.getTagByWicketId("element").getAttribute("class"));
		tester.assertContains("\"design\":\"steps\"");
	}

	@Test
	void callsBackOnlyWhenTheServerIsTold()
	{
		TestTabs tabs = new TestTabs();
		tabs.addTab(tab("one", "one"));
		tester.startPage(new TestPage(tabs));
		assertFalse(tester.getLastResponseAsString().contains("function (tab)"),
			"neither selections nor closable tabs");
		tester.assertContains("\"selected\":\"one\"\\}\\);");

		tabs.addTab(tab("two", "two").setClosable(true));
		tester.startPage(new TestPage(tabs));
		tester.assertContains("\\}, null, function \\(tab\\)");
		assertEquals("true", tagOf(tester.getLastResponseAsString(), "data-tab", "two")
			.getAttribute("data-closable"));
	}

	@Test
	void takesTheTabsTheUserSelectsWhenTold()
	{
		TestTabs tabs = new TestTabs();
		tabs.addTab(tab("one", "one")).addTab(tab("two", "two")).setSendToServer(true);
		tester.startPage(new TestPage(tabs));
		tester.assertContains("\\}, function \\(tab\\)");

		send(tabs, "select", "two");
		assertEquals("two", tabs.getModelObject());
		assertEquals(List.of("two"), tabs.selected);

		send(tabs, "select", "two");
		send(tabs, "select", "nothing");
		assertEquals(List.of("two"), tabs.selected);

		// called back for the tabs closed alone, the selections are left out
		tabs.addTab(tab("three", "three").setClosable(true)).setSendToServer(false);
		send(tabs, "select", "one");
		assertEquals("two", tabs.getModelObject());
	}

	@Test
	void takesAwayATabTheUserCloses()
	{
		TestTabs tabs = new TestTabs();
		tabs.addTab(tab("one", "one").setClosable(true)).addTab(tab("two", "two"))
			.addTab(tab("three", "three").setClosable(true));
		tester.startPage(new TestPage(tabs));

		send(tabs, "close", "one");
		assertEquals(List.of("two", "three"), names(tabs));
		assertEquals(List.of("one"), tabs.closed);
		assertEquals("two", tabs.getModelObject(), "the next one is selected, as in the browser");
		assertNull(tabs.getHeaders().get("one"));

		// one that is not closable stays
		send(tabs, "close", "two");
		assertEquals(List.of("two", "three"), names(tabs));

		tabs.setModelObject("three");
		send(tabs, "close", "three");
		assertEquals("two", tabs.getModelObject(), "the previous one, when the last one closes");
		assertEquals(List.of("one", "three"), tabs.closed);
	}

	@Test
	void addsAndRemovesTabsViaAjaxWithoutRenderingTheOthers()
	{
		TestTabs tabs = new TestTabs();
		Tab one = tab("one", "one");
		tabs.addTab(one);
		tester.startPage(new TestPage(tabs));

		Tab two = tab("two", "two").setClosable(true);
		runAjax(tabs, t -> tabs.addTab(two, t).select(two, t));
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("document.getElementById(\"" +
			tabs.getHeaders().getParent().getMarkupId() + "\").appendChild(e);"), response);
		assertTrue(response.contains("Panel two"));
		assertFalse(response.contains("Panel one"), "the other panels stay as they are");
		assertTrue(response.contains(".select(\"two\")"));
		assertEquals("two", tabs.getModelObject());

		runAjax(tabs, t -> tabs.removeTab(two, t));
		response = tester.getLastResponseAsString();
		assertTrue(response.contains(".remove();"));
		assertTrue(response.contains(".select(\"one\")"));
		assertEquals(List.of("one"), names(tabs));
		assertEquals("one", tabs.getModelObject());
		tabs.removeTab(two);
		runAjax(tabs, t -> tabs.removeTab(two, t));
		tabs.removeTab(one);
		assertNull(tabs.getModelObject());

		assertThrows(IllegalArgumentException.class, () -> tabs.addTab(tab("x", "x"))
			.addTab(tab("x", "y")));
	}

	@Test
	void tellsTheBrowserAPanelHasChanges()
	{
		TestTabs tabs = new TestTabs();
		Tab one = tab("one", "one").setClosable(true).setModified(false);
		tabs.addTab(one);
		tester.startPage(new TestPage(tabs));
		assertEquals("false", tagOf(tester.getLastResponseAsString(), "role", "tabpanel")
			.getAttribute("data-modified"));

		runAjax(tabs, t -> tabs.setModified(one, true, t));
		assertTrue(tester.getLastResponseAsString().contains(".setModified(\"one\",true)"));
		assertEquals(Boolean.TRUE, one.getModified());
		runAjax(tabs, t -> tabs.setModified(one, null, t));
		assertTrue(tester.getLastResponseAsString().contains(".setModified(\"one\",null)"));
	}

	@Test
	void rendersTheActionsOfATabCallingThemBack()
	{
		List<String> clicks = new ArrayList<>();
		TestTabs tabs = new TestTabs();
		Tab one = tab("one", "one").setClosable(true).addAction(new TabAction(ICON,
			Model.of("About it"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClick(AjaxRequestTarget target, Tab tab, Component button)
			{
				clicks.add(tab.getName() + " " + button.getId());
			}
		});
		tabs.addTab(one).addTab(tab("two", "two"));
		tester.startPage(new TestPage(tabs));

		TagTester action = tagOf(tester.getLastResponseAsString(), "title", "About it");
		assertEquals("button", action.getName());
		assertEquals("About it", action.getAttribute("aria-label"));
		assertEquals("0", action.getAttribute("tabindex"));
		assertNotNull(action.getAttribute("id"), "for a popover to point at");
		assertEquals(1, one.getActions().size());
		assertEquals(1, tester.getLastResponseAsString().split("antilia-tab-actions", -1).length - 1,
			"only the tab with actions has them");

		tester.executeAjaxEvent(tabs.getHeaders().get("one:actions:action:1"), "click");
		assertEquals(List.of("one 1"), clicks);
	}

	@Test
	void saysTheTextsInTheSessionsLanguage()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		TestTabs tabs = new TestTabs();
		tabs.addTab(tab("one", "one"));
		tester.startPage(new TestPage(tabs));
		tester.assertContains("\"close\":\"Schließen\"");
		tester.assertContains("\"keep\":\"Geöffnet lassen\"");

		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(new TestPage(tabs));
		tester.assertContains("\"closeModifiedTitle\":\"¿Cerrar \\{0\\}\\?\"");
	}

	private void send(TestTabs tabs, String event, String name)
	{
		tester.getRequest().setParameter("event", event);
		tester.getRequest().setParameter("tab", name);
		tester.executeBehavior(tabs.getHeaders().getParent().getParent()
			.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private void runAjax(TestTabs tabs, Consumer<AjaxRequestTarget> work)
	{
		tabs.work = work;
		tester.executeBehavior(tabs.getBehaviors(WorkBehavior.class).get(0));
	}

	private static List<String> names(Tabs tabs)
	{
		return tabs.getTabs().stream().map(Tab::getName).toList();
	}

	/** Does the work of a test in an Ajax request. */
	private static class WorkBehavior extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			((TestTabs)getComponent()).work.accept(target);
		}
	}

	private static class TestTabs extends Tabs
	{
		private static final long serialVersionUID = 1L;

		final List<String> selected = new ArrayList<>();

		final List<String> closed = new ArrayList<>();

		transient Consumer<AjaxRequestTarget> work;

		TestTabs()
		{
			super("tabs");
			add(new WorkBehavior());
		}

		MarkupContainer getHeaders()
		{
			return (MarkupContainer)get("element:list:headers");
		}

		Component button(String name)
		{
			return getHeaders().get(name + ":button");
		}

		@Override
		protected void onSelected(AjaxRequestTarget target, Tab tab)
		{
			selected.add(tab.getName());
		}

		@Override
		protected void onClosed(AjaxRequestTarget target, Tab tab)
		{
			closed.add(tab.getName());
		}
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(Tabs tabs)
		{
			add(tabs);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"tabs\"></div></body></html>");
		}
	}
}
