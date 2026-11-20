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

class TabbedPanelTest extends WicketTestCase
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
		TestTabbedPanel tabs = new TestTabbedPanel();
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
		TestTabbedPanel tabs = new TestTabbedPanel();
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
		TestTabbedPanel tabs = new TestTabbedPanel();
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
		assertNull(new TestTabbedPanel().getSelectedTab());

		tabs.setDesign(TabsDesign.STEPS).setOrientation(TabsOrientation.VERTICAL);
		tester.startPage(new TestPage(tabs));
		assertEquals("antilia-tabs antilia-tabs-vertical antilia-tabs-steps",
			tester.getTagByWicketId("element").getAttribute("class"));
		tester.assertContains("\"design\":\"steps\"");
	}

	@Test
	void callsBackOnlyWhenTheServerIsTold()
	{
		TestTabbedPanel tabs = new TestTabbedPanel();
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
		TestTabbedPanel tabs = new TestTabbedPanel();
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
		TestTabbedPanel tabs = new TestTabbedPanel();
		tabs.addTab(tab("one", "one").setClosable(true)).addTab(tab("two", "two"))
			.addTab(tab("three", "three").setClosable(true));
		tester.startPage(new TestPage(tabs));
		String header = tabs.getHeaders().get("one").getMarkupId();
		String item = tabs.getItems().get("one").getMarkupId();

		send(tabs, "close", "one");
		assertEquals(List.of("two", "three"), names(tabs));
		assertEquals(List.of("one"), tabs.closed);
		// out of the page in the browser too, as Wicket's Ajax removes elements: what is in the
		// panel, an editor, sends nothing more to the components gone
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.DOM.remove(e);}};r(\"" + header + "\");r(\"" + item +
			"\");"), response);
		assertFalse(response.contains(".select("), "the browser selected the next one already");
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
		TestTabbedPanel tabs = new TestTabbedPanel();
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
		assertTrue(response.contains("Wicket.DOM.remove(e);"), response);
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
	void addsATabAtAPlaceRenderedInTheOrderOfTheTabs()
	{
		TestTabbedPanel tabs = new TestTabbedPanel();
		tabs.addTab(tab("one", "one")).addTab(tab("three", "three")).addTab(tab("zero", "zero"), 0)
			.addTab(tab("two", "two"), 2);
		assertEquals(List.of("zero", "one", "two", "three"), names(tabs));
		assertThrows(IndexOutOfBoundsException.class, () -> tabs.addTab(tab("x", "x"), 5));
		assertThrows(IndexOutOfBoundsException.class, () -> tabs.addTab(tab("x", "x"), -1));
		tester.startPage(new TestPage(tabs));

		String html = tester.getLastResponseAsString();
		assertTrue(html.indexOf("Title zero") < html.indexOf("Title one") &&
			html.indexOf("Title one") < html.indexOf("Title two") &&
			html.indexOf("Title two") < html.indexOf("Title three"), "the tabs in order");
		assertTrue(html.indexOf("Panel zero") < html.indexOf("Panel one") &&
			html.indexOf("Panel one") < html.indexOf("Panel two") &&
			html.indexOf("Panel two") < html.indexOf("Panel three"), "the panels in order");
		assertSame(tabs.getTab("zero"), tabs.getSelectedTab(), "the first one");
	}

	@Test
	void addsATabViaAjaxBeforeTheOneAtItsPlace()
	{
		TestTabbedPanel tabs = new TestTabbedPanel();
		tabs.addTab(tab("one", "one")).addTab(tab("three", "three"));
		tester.startPage(new TestPage(tabs));
		String header = tabs.getHeaders().get("three").getMarkupId();
		String item = tabs.getItems().get("three").getMarkupId();

		runAjax(tabs, t -> tabs.addTab(tab("two", "two"), 1, t));
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("b=document.getElementById(\"" + header +
			"\");p.insertBefore(e,b&&b.parentNode===p?b:null);"), response);
		assertTrue(response.contains("b=document.getElementById(\"" + item + "\");"), response);
		assertTrue(response.contains("Panel two"));
		assertFalse(response.contains("Panel one"), "the other panels stay as they are");
		assertEquals(List.of("one", "two", "three"), names(tabs));

		// after the others, as addTab(tab, target)
		runAjax(tabs, t -> tabs.addTab(tab("four", "four"), 3, t));
		assertTrue(tester.getLastResponseAsString().contains(".appendChild(e);"));
		assertEquals(List.of("one", "two", "three", "four"), names(tabs));
	}

	@Test
	void rendersTheHeaderOfATabAgainViaAjax()
	{
		List<String> clicks = new ArrayList<>();
		Model<String> title = Model.of("Inbox");
		Tab inbox = new Tab("inbox", title)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newPanel(String id)
			{
				return new Label(id, "Panel inbox");
			}
		}.setClosable(true);
		TestTabbedPanel tabs = new TestTabbedPanel();
		tabs.addTab(inbox).addTab(tab("two", "two"));
		tester.startPage(new TestPage(tabs));
		String html = tester.getLastResponseAsString();
		Component icon = tabs.getHeaders().get("inbox:button:icon");
		Component actions = tabs.getHeaders().get("inbox:actions");
		assertTrue(html.contains("id=\"" + icon.getMarkupId() + "\" hidden=\"\""),
			"a placeholder for an icon to come");
		assertTrue(html.contains("id=\"" + actions.getMarkupId() + "\" hidden=\"\""),
			"a placeholder for actions to come");

		title.setObject("Inbox (3)");
		inbox.setIcon(ICON).addAction(new TabAction(ICON, Model.of("Mark all read"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClick(AjaxRequestTarget target, Tab tab, Component button)
			{
				clicks.add(tab.getName());
			}
		});
		runAjax(tabs, t -> tabs.updateTab(inbox, t));
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Inbox (3)"), response);
		assertTrue(response.contains("<svg class=\"icon\"></svg>"));
		assertTrue(response.contains("title=\"Mark all read\""));
		assertTrue(response.contains("id=\"" + tabs.getHeaders().get("inbox:button:title")
			.getMarkupId() + "\""));
		assertFalse(response.contains("Panel inbox"), "the panel stays as it is");
		assertFalse(response.contains("id=\"" + tabs.getHeaders().get("inbox").getMarkupId() +
			"\""), "the tab element stays, with the close button and the mark of the browser");
		assertFalse(response.contains("Title two"), "the other tabs stay as they are");

		tester.executeAjaxEvent(tabs.getHeaders().get("inbox:actions:action:1"), "click");
		assertEquals(List.of("inbox"), clicks);

		// a tab the tabs no longer have changes nothing
		tabs.removeTab(inbox);
		runAjax(tabs, t -> tabs.updateTab(inbox, t));
		assertFalse(tester.getLastResponseAsString().contains("Inbox"));
	}

	/** The panels the lazy tabs of a test created, by the names of their tabs. */
	private static final List<String> CREATED = new ArrayList<>();

	private static LazyTab lazy(String name)
	{
		return new LazyTab(name, Model.of("Title " + name))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newPanel(String id)
			{
				CREATED.add(getName());
				return new Label(id, "Panel " + getName() + " " + CREATED.size());
			}
		};
	}

	@Test
	void createsThePanelOfALazyTabWhenItIsFirstSelectedAndKeepsIt()
	{
		CREATED.clear();
		TestTabbedPanel tabs = new TestTabbedPanel();
		LazyTab report = lazy("report");
		tabs.addTab(tab("one", "one")).addTab(report);
		assertTrue(CREATED.isEmpty(), "not when added");
		assertFalse(tabs.isLoaded(report));
		assertTrue(tabs.isLoaded(tabs.getTab("one")));
		tester.startPage(new TestPage(tabs));

		String html = tester.getLastResponseAsString();
		TagTester panel = tagOf(html, "id", tabs.getItems().get("report").getMarkupId());
		assertEquals("", panel.getAttribute("data-load"));
		assertFalse(panel.getValue().contains("Panel"), "empty");
		assertNull(tagOf(html, "id", tabs.getItems().get("one").getMarkupId())
			.getAttribute("data-load"));
		// no select callback, the load one after that of closing
		tester.assertContains("(?s)Wicket.Tabs.attach\\(\"" + tabs.getTabsMarkupId() +
			"\", \\{[^\\n]*\\}, null, function.*\"close\".*\"load\"");

		send(tabs, "load", "report");
		assertEquals(List.of("report"), CREATED);
		assertTrue(tabs.isLoaded(report));
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Panel report 1"), response);
		assertFalse(response.contains("data-load"), "rendered");
		assertFalse(response.contains("Panel one"), "the other panels stay as they are");
		assertNull(tabs.getModelObject(), "the server is not told the tab selected");
		assertTrue(tabs.selected.isEmpty());

		// kept, without a request for it, and rendered with the others
		send(tabs, "load", "report");
		assertEquals(List.of("report"), CREATED, "created once");
		tester.startPage(tester.getLastRenderedPage());
		assertTrue(tester.getLastResponseAsString().contains("Panel report 1"));
		assertEquals(List.of("report"), CREATED);
	}

	@Test
	void createsThePanelOfALazyTabSelectedAtOnce()
	{
		CREATED.clear();
		TestTabbedPanel tabs = new TestTabbedPanel();
		LazyTab first = lazy("first");
		LazyTab other = lazy("other");
		tabs.addTab(first).addTab(other).addTab(tab("plain", "plain"));
		tester.startPage(new TestPage(tabs));
		assertEquals(List.of("first"), CREATED, "the tab selected");
		assertTrue(tester.getLastResponseAsString().contains("Panel first 1"));

		// selected via Ajax: created in the same request
		runAjax(tabs, t -> tabs.select(other, t));
		assertEquals(List.of("first", "other"), CREATED);
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Panel other 2"), response);
		assertTrue(response.contains(".select(\"other\")"));
		assertTrue(response.indexOf("Panel other 2") < response.indexOf(".select(\"other\")"),
			"the panel first");

		// added via Ajax and selected
		LazyTab added = lazy("added");
		runAjax(tabs, t -> tabs.addTab(added, t).select(added, t));
		assertTrue(tester.getLastResponseAsString().contains("Panel added 3"));
		assertTrue(tabs.isLoaded(first), "kept");
	}

	@Test
	void dropsThePanelOfALazyTabNotCachingWhenAnotherIsSelected()
	{
		CREATED.clear();
		TestTabbedPanel tabs = new TestTabbedPanel();
		LazyTab live = lazy("live").setCaching(false);
		tabs.addTab(tab("one", "one")).addTab(live).addTab(lazy("kept"));
		assertFalse(live.isCaching());
		tester.startPage(new TestPage(tabs));
		// the browser tells the tabs selected, to drop the panel
		tester.assertContains("(?s)Wicket.Tabs.attach\\(\"" + tabs.getTabsMarkupId() +
			"\", \\{[^\\n]*\\}, function.*\"select\".*\"close\".*\"load\"");

		send(tabs, "load", "live");
		assertEquals(List.of("live"), CREATED);
		assertEquals("live", tabs.getModelObject(), "told the tab selected");
		assertTrue(tabs.selected.isEmpty(), "onSelected only with setSendToServer");

		send(tabs, "select", "one");
		assertFalse(tabs.isLoaded(live));
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("data-load"), response);
		assertEquals("one", tabs.getModelObject());

		send(tabs, "load", "kept");
		send(tabs, "load", "live");
		assertEquals(List.of("live", "kept", "live"), CREATED, "anew");
		assertTrue(tester.getLastResponseAsString().contains("Panel live 3"));
		send(tabs, "select", "kept");
		assertFalse(tabs.isLoaded(live));
		assertTrue(tabs.isLoaded(tabs.getTab("kept")));

		// rendered whole on another tab: not there
		send(tabs, "load", "live");
		assertTrue(tabs.isLoaded(live));
		tabs.select(tabs.getTab("one"));
		tester.startPage(tester.getLastRenderedPage());
		assertFalse(tabs.isLoaded(live));
	}

	@Test
	void tellsTheServerTheTabSelectedWithTheLoadOfItsPanel()
	{
		CREATED.clear();
		TestTabbedPanel tabs = new TestTabbedPanel();
		tabs.setSendToServer(true);
		tabs.addTab(tab("one", "one")).addTab(lazy("report"));
		tester.startPage(new TestPage(tabs));

		send(tabs, "load", "report");
		assertEquals(List.of("report"), tabs.selected);
		assertEquals("report", tabs.getModelObject());
		send(tabs, "select", "report");
		assertEquals(List.of("report"), tabs.selected, "once");
	}

	@Test
	void callsBackForLoadingOnlyWithLazyTabs()
	{
		TestTabbedPanel tabs = new TestTabbedPanel();
		tabs.addTab(tab("one", "one").setClosable(true));
		tester.startPage(new TestPage(tabs));
		assertFalse(tester.getLastResponseAsString().contains("\"load\""));
		assertFalse(tester.getLastResponseAsString().contains("data-load"));
	}

	@Test
	void tellsTheBrowserAPanelHasChanges()
	{
		TestTabbedPanel tabs = new TestTabbedPanel();
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
		TestTabbedPanel tabs = new TestTabbedPanel();
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
		TestTabbedPanel tabs = new TestTabbedPanel();
		tabs.addTab(tab("one", "one"));
		tester.startPage(new TestPage(tabs));
		tester.assertContains("\"close\":\"Schließen\"");
		tester.assertContains("\"keep\":\"Geöffnet lassen\"");

		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(new TestPage(tabs));
		tester.assertContains("\"closeModifiedTitle\":\"¿Cerrar \\{0\\}\\?\"");
	}

	private void send(TestTabbedPanel tabs, String event, String name)
	{
		tester.getRequest().setParameter("event", event);
		tester.getRequest().setParameter("tab", name);
		tester.executeBehavior(tabs.getHeaders().getParent().getParent()
			.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private void runAjax(TestTabbedPanel tabs, Consumer<AjaxRequestTarget> work)
	{
		tabs.work = work;
		tester.executeBehavior(tabs.getBehaviors(WorkBehavior.class).get(0));
	}

	private static List<String> names(TabbedPanel tabs)
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
			((TestTabbedPanel)getComponent()).work.accept(target);
		}
	}

	private static class TestTabbedPanel extends TabbedPanel
	{
		private static final long serialVersionUID = 1L;

		final List<String> selected = new ArrayList<>();

		final List<String> closed = new ArrayList<>();

		transient Consumer<AjaxRequestTarget> work;

		TestTabbedPanel()
		{
			super("tabs");
			add(new WorkBehavior());
		}

		MarkupContainer getHeaders()
		{
			return (MarkupContainer)get("element:list:headers");
		}

		MarkupContainer getItems()
		{
			return (MarkupContainer)get("element:panels:items");
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

		TestPage(TabbedPanel tabs)
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
