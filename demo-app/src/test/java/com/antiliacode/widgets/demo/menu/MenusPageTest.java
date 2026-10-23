/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antiliacode.widgets.demo.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.menu.Menu;
import com.antiliacode.widgets.menu.MenuDesign;
import com.antiliacode.widgets.menu.MenuOrientation;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Checks {@link MenusPage}: the items of the application calling the server via Ajax or with a
 * request of their own, disabled ones and tooltips; the navigation leading to the page with its
 * section, marked as current; the side bar showing the view chosen via Ajax; the menu the form
 * switches; the gallery; and the configuration of the React and Svelte apps.
 */
class MenusPageTest
{
	private static final String APPLICATION = inCase("applicationTitle", "application");

	private static final String NAVIGATION = inCase("navigationTitle", "navigation");

	private static final String SIDEBAR = inCase("sidebarTitle", "sidebar");

	private static final String SETTINGS = inCase("settingsTitle", "preview:settings");

	private WicketTester tester;

	/**
	 * @return the path of a component of a case
	 */
	private static String inCase(String name, String path)
	{
		return "themed:cases:" + DemoCases.pathOf(name) + ":" + path;
	}

	/**
	 * @return the path of an item of a menu, by its place in the bar and in its submenus
	 */
	private static String item(String menu, int... places)
	{
		StringBuilder path = new StringBuilder(menu).append(":element:bar");
		for (int i = 0; i < places.length; i++)
		{
			path.append(i == 0 ? "" : ":list").append(":items:").append(places[i]).append(":item");
		}
		return path.toString();
	}

	private static String link(String menu, int... places)
	{
		return item(menu, places) + ":link";
	}

	private static String trigger(String menu, int... places)
	{
		return item(menu, places) + ":trigger";
	}

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(MenusPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private TagTester tagAt(String path)
	{
		return TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			tester.getComponentFromLastRenderedPage(path).getMarkupId());
	}

	private Menu menu(String path)
	{
		return (Menu)tester.getComponentFromLastRenderedPage(path);
	}

	@Test
	void callsTheServerViaAjaxOrWithARequestOfItsOwn()
	{
		String status = inCase("applicationTitle", "applicationStatus");
		tester.assertLabel(status, "Choose an item: those with an arrow open a submenu.");
		tester.assertContains("role=\"menubar\" aria-orientation=\"horizontal\" " +
			"aria-label=\"Application\"");

		// File › Save
		tester.executeAjaxEvent(link(APPLICATION, 0, 3), "click");
		tester.assertLabel(status, "You chose File › Save, via Ajax.");
		tester.assertComponentOnAjaxResponse(status);
		// File › Export › PDF
		tester.executeAjaxEvent(link(APPLICATION, 0, 4, 0), "click");
		tester.assertLabel(status, "You chose File › Export › PDF, via Ajax.");
		// File › Start again
		tester.clickLink(link(APPLICATION, 0, 6));
		tester.assertRenderedPage(MenusPage.class);
		tester.assertLabel(status, "You chose File › Start again, with a request of its own: " +
			"the page rendered anew.");
	}

	@Test
	void hasDisabledItemsTooltipsIconsAndALinkToTheHomePage()
	{
		assertEquals("Save the document on the server", tagAt(link(APPLICATION, 0, 3))
			.getAttribute("title"));
		assertTrue(tagAt(link(APPLICATION, 0, 3)).getValue().contains("<svg"));
		// Edit › Redo and Edit › Paste
		assertEquals("true", tagAt(link(APPLICATION, 1, 1)).getAttribute("aria-disabled"));
		assertEquals("true", tagAt(link(APPLICATION, 1, 5)).getAttribute("aria-disabled"));
		assertNull(tagAt(link(APPLICATION, 1, 3)).getAttribute("aria-disabled"));
		// Help › Home page
		TagTester home = tagAt(link(APPLICATION, 3, 2));
		assertTrue(home.getAttribute("href") != null && !home.getAttribute("href").isEmpty());
		assertEquals("A bookmarkable page: the home page of the demos", home.getAttribute("title"));
		assertEquals("menu", tagAt(trigger(APPLICATION, 0)).getAttribute("aria-haspopup"));
		assertEquals("hidden", tagAt(item(APPLICATION, 0) + ":list").getAttribute("hidden"));
	}

	@Test
	void leadsToTheSectionsOfTheSiteMarkingTheOneShown()
	{
		String where = inCase("navigationTitle", "where");
		tester.assertLabel(where, "You are at Welcome.");
		assertEquals("page", tagAt(link(NAVIGATION, 0)).getAttribute("aria-current"));
		Menu navigation = menu(NAVIGATION);
		assertEquals(MenuDesign.UNDERLINE, navigation.getDesign());
		assertTrue(navigation.isOpenOnHover());

		// Products › Laptops
		tester.clickLink(link(NAVIGATION, 1, 0));
		tester.assertRenderedPage(MenusPage.class);
		assertEquals("laptops", tester.getLastRenderedPage().getPageParameters()
			.get(MenusPage.SECTION).toString());
		assertEquals("navigationTitle", ((DemoCases)tester.getComponentFromLastRenderedPage(
			"themed:cases")).getModelObject(), "the case of the navigation shown");
		tester.assertLabel(where, "You are at Products › Laptops.");
		tester.assertContains("section=laptops");
		assertEquals("page", tagAt(link(NAVIGATION, 1, 0)).getAttribute("aria-current"));
		assertNull(tagAt(link(NAVIGATION, 0)).getAttribute("aria-current"));
		assertTrue(tagAt(trigger(NAVIGATION, 1)).getAttribute("class")
			.contains("antilia-menu-current-parent"));

		tester.startPage(MenusPage.class, new PageParameters().add(MenusPage.SECTION, "covers"));
		tester.assertLabel(where, "You are at Products › Accessories › Covers.");
		tester.startPage(MenusPage.class, new PageParameters().add(MenusPage.SECTION, "nothing"));
		tester.assertLabel(where, "You are at Welcome.");
	}

	@Test
	void showsTheViewChosenInTheSideBarMarkingIt()
	{
		String heading = inCase("sidebarTitle", "content:heading");
		Menu sidebar = menu(SIDEBAR);
		assertEquals(MenuOrientation.VERTICAL, sidebar.getOrientation());
		assertTrue(sidebar.isInline());
		tester.assertLabel(heading, "Reports › Sales");
		// Reports holds the current view: open in place from the start
		assertEquals("true", tagAt(trigger(SIDEBAR, 1)).getAttribute("aria-expanded"));
		assertEquals("false", tagAt(trigger(SIDEBAR, 2)).getAttribute("aria-expanded"));
		assertEquals("page", tagAt(link(SIDEBAR, 1, 0)).getAttribute("aria-current"));

		// People › Members
		String members = link(SIDEBAR, 2, 0);
		String id = tester.getComponentFromLastRenderedPage(members).getMarkupId();
		tester.executeAjaxEvent(members, "click");
		assertEquals("members", ((MenusPage)tester.getLastRenderedPage()).getView());
		tester.assertLabel(heading, "People › Members");
		tester.assertComponentOnAjaxResponse(SIDEBAR);
		tester.assertComponentOnAjaxResponse(inCase("sidebarTitle", "content"));
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("id=\"" + id + "\""), "the same id, the focus kept");
		assertTrue(response.contains("aria-current=\"page\""));
	}

	@Test
	void switchesTheMenuOfTheSettings()
	{
		Menu settings = menu(SETTINGS);
		assertEquals(MenuDesign.PILLS, settings.getDesign());
		assertEquals(MenuOrientation.HORIZONTAL, settings.getOrientation());

		String form = inCase("settingsTitle", "form");
		FormTester tester1 = tester.newFormTester(form);
		tester1.setValue("vertical", true);
		tester.executeAjaxEvent(form + ":vertical", "change");
		FormTester tester2 = tester.newFormTester(form);
		tester2.select("design", 3);
		tester.executeAjaxEvent(form + ":design", "change");
		FormTester tester3 = tester.newFormTester(form);
		tester3.setValue("inline", true);
		tester.executeAjaxEvent(form + ":inline", "change");
		FormTester tester4 = tester.newFormTester(form);
		tester4.setValue("hover", true);
		tester.executeAjaxEvent(form + ":hover", "change");

		assertEquals(MenuOrientation.VERTICAL, settings.getOrientation());
		assertEquals(MenuDesign.SIDEBAR, settings.getDesign());
		assertTrue(settings.isInline());
		assertTrue(settings.isOpenOnHover());
		tester.assertComponentOnAjaxResponse(inCase("settingsTitle", "preview"));
		tester.assertContains("antilia-menu antilia-menu-vertical antilia-menu-sidebar " +
			"antilia-menu-inline");
		tester.assertContains("\"inline\":true,\"openOnHover\":true");

		// View › Zoom in
		tester.executeAjaxEvent(link(SETTINGS, 2, 0), "click");
		tester.assertLabel(inCase("settingsTitle", "settingsStatus"), "You chose View › Zoom in, " +
			"via Ajax.");
	}

	@Test
	void showsTheDesignsAlongTheTopAndDownTheSide()
	{
		tester.assertContains("antilia-menu antilia-menu-horizontal antilia-menu-bar");
		tester.assertContains("antilia-menu antilia-menu-horizontal antilia-menu-underline");
		tester.assertContains("antilia-menu antilia-menu-vertical antilia-menu-pills");
		tester.assertContains("antilia-menu antilia-menu-vertical antilia-menu-sidebar\"");
		tester.assertContains("<figcaption>Side bar</figcaption>");
		Menu first = menu(inCase("galleryTitle", "sideDesigns:0:menu"));
		assertEquals(MenuOrientation.VERTICAL, first.getOrientation());
		assertEquals(MenuDesign.BAR, first.getDesign());
		assertFalse(first.isInline());
		// the dashboard current in each
		assertEquals("page", tagAt(link(inCase("galleryTitle", "topDesigns:3:menu"), 0))
			.getAttribute("aria-current"));
	}

	@Test
	void givesTheAppsTheMenusTheirIconsAndTheTexts()
	{
		JSONObject config = new JSONObject(TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "data-framework-app", "react")
			.getAttribute("data-config"));

		JSONArray application = config.getJSONArray("application");
		JSONObject file = application.getJSONObject(0);
		assertEquals("file", file.getString("name"));
		assertTrue(file.getString("icon").contains("<svg"));
		assertTrue(file.getJSONArray("items").getJSONObject(2).getBoolean("separator"));
		assertEquals("link", file.getJSONArray("items").getJSONObject(6).getString("kind"));
		assertTrue(application.getJSONObject(1).getJSONArray("items").getJSONObject(1)
			.getBoolean("disabled"));
		assertEquals("page", config.getJSONArray("navigation").getJSONObject(0).getString("kind"));
		assertEquals("dashboard", config.getJSONArray("sidebar").getJSONObject(0).getString("name"));
		JSONObject texts = config.getJSONObject("texts");
		assertEquals("Save", texts.getString("save"));
		assertEquals("Save the document on the server", texts.getString("save.tooltip"));
		assertEquals("Covers", texts.getString("covers"));
		assertEquals("You chose {0}.", texts.getString("picked"));
		assertEquals("Side bar", texts.getString("design.SIDEBAR"));
		assertFalse(texts.has("dashboard.tooltip"));
		assertEquals("[\"bar\",\"pills\",\"underline\",\"sidebar\"]",
			config.getJSONArray("designs").toString());
		assertEquals("welcome", config.getString("section"));
		assertEquals("sales", config.getString("view"));
		assertEquals("pills", config.getString("design"));
		assertFalse(config.getString("homeUrl").isEmpty());
	}

	@Test
	void speaksGermanAndSpanish()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(MenusPage.class);
		tester.executeAjaxEvent(link(APPLICATION, 0, 3), "click");
		tester.assertLabel(inCase("applicationTitle", "applicationStatus"),
			"Sie haben Datei › Speichern gewählt, per Ajax.");

		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(MenusPage.class, new PageParameters().add(MenusPage.SECTION, "training"));
		tester.assertLabel(inCase("navigationTitle", "where"), "Está en Servicios › Formación.");
	}
}
