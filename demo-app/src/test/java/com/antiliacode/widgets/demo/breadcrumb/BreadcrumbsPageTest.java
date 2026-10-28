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
package com.antiliacode.widgets.demo.breadcrumb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.breadcrumb.Breadcrumb;
import com.antiliacode.widgets.breadcrumb.BreadcrumbSeparator;
import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.menu.IMenuItem;
import com.github.openjson.JSONObject;

/**
 * Checks {@link BreadcrumbsPage}: the site leading to the page with its path, the drill-down going
 * deeper and back via Ajax, the trail the form changes, and the configuration of the React and
 * Svelte apps.
 */
class BreadcrumbsPageTest
{
	private static final String SITE = inCase("siteTitle", "siteTrail");

	private static final String FOLDERS = inCase("foldersTitle", "folderView:folderTrail");

	private static final String SETTINGS = inCase("settingsTitle", "preview:settingsTrail");

	private WicketTester tester;

	private static String inCase(String name, String path)
	{
		return "themed:cases:" + DemoCases.pathOf(name) + ":" + path;
	}

	/**
	 * @return the path of the link of a crumb, by its place among the crumbs and the button "…"
	 */
	private static String crumb(String breadcrumb, int place)
	{
		return breadcrumb + ":element:list:crumbs:" + place + ":crumb:link";
	}

	@BeforeEach
	void start()
	{
		tester = new WicketTester(new WicketApplication());
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private Breadcrumb breadcrumb(String path)
	{
		return (Breadcrumb)tester.getComponentFromLastRenderedPage(path);
	}

	private List<String> labels(String path)
	{
		return breadcrumb(path).getShownItems().stream().map(IMenuItem::getLabel)
			.map(label -> label instanceof IComponentAssignedModel<String> assigned ?
				assigned.wrapOnAssignment(tester.getLastRenderedPage()).getObject() :
				label.getObject())
			.toList();
	}

	@Test
	void theSiteLeadsToThisPageWithItsPath()
	{
		tester.startPage(BreadcrumbsPage.class, new PageParameters().add(BreadcrumbsPage.PATH,
			"products/computers"));

		assertEquals(List.of("Home", "Products", "Computers"), labels(SITE));
		tester.assertLabel(inCase("siteTitle", "siteHeading"), "Computers");
		tester.assertLabel(inCase("siteTitle", "siteBelow"), "The pages in Computers:");
		tester.assertContains("href=\"\\./breadcrumbs\\?path=products\"");
		tester.assertContains("href=\"\\./breadcrumbs\\?path=products/computers/laptops\"");
		tester.assertContains("aria-current=\"page\"");

		tester.clickLink(crumb(SITE, 1));
		tester.assertRenderedPage(BreadcrumbsPage.class);
		assertEquals(List.of("Home", "Products"), labels(SITE));
	}

	@Test
	void aPathLeadingNowhereIsTheHomePage()
	{
		tester.startPage(BreadcrumbsPage.class, new PageParameters().add(BreadcrumbsPage.PATH,
			"products/nothing"));

		assertEquals(List.of("Home"), labels(SITE));
		tester.assertLabel(inCase("siteTitle", "siteHeading"), "Home");
	}

	@Test
	void theDrillDownGoesDeeperAndBackViaAjax()
	{
		tester.startPage(BreadcrumbsPage.class);
		BreadcrumbsPage page = (BreadcrumbsPage)tester.getLastRenderedPage();

		assertEquals(List.of("My drive", "Documents", "Projects", "widgets", "src"),
			labels(FOLDERS));
		assertEquals(4, breadcrumb(FOLDERS).getMaxItems());
		tester.assertContains("Show the whole path \\(1 more\\)");

		tester.clickLink(inCase("foldersTitle", "folderView:subfolders:0:link"));
		assertEquals(List.of("documents", "projects", "widgets", "src", "main"), page.getFolder());
		tester.assertComponentOnAjaxResponse(inCase("foldersTitle", "folderView"));
		tester.assertLabel(inCase("foldersTitle", "folderView:folderHeading"), "main");

		// back to Projects, the third crumb, after the first and the button "…"
		tester.clickLink(crumb(FOLDERS, 3));
		assertEquals(List.of("documents", "projects"), page.getFolder());
		assertEquals(List.of("My drive", "Documents", "Projects"), labels(FOLDERS));

		// to Website, which is empty
		tester.clickLink(inCase("foldersTitle", "folderView:subfolders:1:link"));
		tester.assertLabel(inCase("foldersTitle", "folderView:emptyFolder"),
			"This folder is empty.");
	}

	@Test
	void theFormChangesTheLongTrail()
	{
		tester.startPage(BreadcrumbsPage.class);
		Breadcrumb trail = breadcrumb(SETTINGS);
		assertEquals(7, trail.getShownItems().size());
		assertEquals(4, trail.getMaxItems());
		assertEquals(BreadcrumbSeparator.CHEVRON, trail.getSeparator());

		FormTester form = tester.newFormTester(inCase("settingsTitle", "form"));
		form.select("separator", 2);
		tester.executeAjaxEvent(inCase("settingsTitle", "form:separator"), "change");
		form = tester.newFormTester(inCase("settingsTitle", "form"));
		form.select("maxItems", 0);
		tester.executeAjaxEvent(inCase("settingsTitle", "form:maxItems"), "change");

		tester.assertComponentOnAjaxResponse(inCase("settingsTitle", "preview"));
		trail = breadcrumb(SETTINGS);
		assertEquals(BreadcrumbSeparator.ARROW, trail.getSeparator());
		assertEquals(0, trail.getMaxItems());

		tester.clickLink(crumb(SETTINGS, 2));
		tester.assertLabel(inCase("settingsTitle", "settingsStatus"),
			"You chose Projects, via Ajax.");
	}

	@Test
	void speaksTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(BreadcrumbsPage.class);

		assertEquals(List.of("Mein Laufwerk", "Dokumente", "Projekte", "widgets", "src"),
			labels(FOLDERS));
		tester.assertContains("aria-label=\"Brotkrümelnavigation\"");
	}

	@Test
	void givesTheAppsTheirConfiguration()
	{
		tester.startPage(BreadcrumbsPage.class);
		JSONObject config = new JSONObject(TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "data-framework-app", "react")
			.getAttribute("data-config"));

		assertEquals("Site", config.getJSONObject("texts").getString("siteTitle"));
		assertEquals("Computers", config.getJSONObject("texts").getString("place.computers"));
		assertEquals("Breadcrumb", config.getJSONObject("breadcrumbTexts").getString("label"));
		assertEquals("home", config.getJSONObject("site").getString("name"));
		assertTrue(config.getJSONObject("files").getString("icon").startsWith("<svg"));
		assertEquals(4, config.getInt("foldersMaxItems"));
		assertEquals("chevron", config.getString("separator"));
		assertEquals(3, config.getJSONArray("separators").length());
		assertEquals(6, config.getJSONArray("longTrail").length());
	}
}
