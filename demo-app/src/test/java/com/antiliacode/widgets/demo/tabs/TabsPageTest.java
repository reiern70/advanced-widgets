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
package com.antiliacode.widgets.demo.tabs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.tabs.TabsPage.Document;
import com.antiliacode.widgets.tabs.Tab;
import com.antiliacode.widgets.tabs.Tabs;
import com.antiliacode.widgets.tabs.TabsOrientation;
import com.antiliacode.widgets.tabs.TabsDesign;
import com.github.openjson.JSONObject;

/**
 * Checks {@link TabsPage}: the documents whose changes the server hears and marks, saves and
 * describes, the tabs closed and opened, the second tabs switched by the form and telling the
 * server the tab selected, and the configuration of the React and Svelte apps.
 */
class TabsPageTest
{
	private static final String DOCUMENTS = inCase("documentsTitle", "documents");

	private static final String SETTINGS = inCase("settingsTitle", "settings");

	private WicketTester tester;

	private TabsPage page;

	/**
	 * @return the path of a component of a case
	 */
	private static String inCase(String name, String path)
	{
		return "themed:cases:" + DemoCases.pathOf(name) + ":" + path;
	}

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		page = tester.startPage(TabsPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private Tabs tabs(String path)
	{
		return (Tabs)tester.getComponentFromLastRenderedPage(path);
	}

	/**
	 * @return the path of a component in the panel of a tab of the documents
	 */
	private String inPanel(Tab tab, String path)
	{
		return DOCUMENTS + ":element:panels:items:" + tab.getName() + ":panel:" + path;
	}

	private void send(Tabs tabs, String event, String name)
	{
		tester.getRequest().setParameter("event", event);
		tester.getRequest().setParameter("tab", name);
		tester.executeBehavior(((MarkupContainer)tabs.get("element"))
			.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	@Test
	void showsTheWelcomeTabThenTheDocuments()
	{
		Tabs documents = tabs(DOCUMENTS);
		assertEquals(3, documents.getTabs().size());
		tester.assertContains("class=\"antilia-tab-title\">Welcome</span>");
		assertEquals(List.of("notes.txt", "todo.txt"), page.getDocuments().stream()
			.map(Document::getName).toList());
		assertFalse(documents.getTabs().get(0).isClosable());
		assertTrue(documents.getTabs().get(1).isClosable());
		assertEquals(Boolean.FALSE, documents.getTabs().get(1).getModified(), "the server tells");
		assertEquals("welcome", documents.getSelectedTab().getName());
		tester.assertContains("Wicket.Tabs.attach");
		tester.assertContains("Buy milk");
		tester.assertLabel(inCase("documentsTitle", "status"), "Close a document with its ×, " +
			"Delete or the middle button; one with changes asks first.");
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
	}

	@Test
	void marksADocumentWithChangesAndSavesIt()
	{
		Tab notes = tabs(DOCUMENTS).getTabs().get(1);

		tester.getRequest().setParameter(((FormComponent<?>)tester.getComponentFromLastRenderedPage(
			inPanel(notes, "form:text"))).getInputName(), "Buy milk\r\nCall the bank");
		tester.executeAjaxEvent(inPanel(notes, "form:text"), "input");

		assertTrue(page.getDocuments().get(0).isModified());
		assertEquals(Boolean.TRUE, notes.getModified());
		tester.assertContains("setModified\\(\"" + notes.getName() + "\",true\\)");

		FormTester form = tester.newFormTester(inPanel(notes, "form"));
		form.setValue("text", "Buy milk\nCall the bank");
		tester.executeAjaxEvent(inPanel(notes, "form:save"), "click");

		assertFalse(page.getDocuments().get(0).isModified());
		assertEquals(Boolean.FALSE, notes.getModified());
		tester.assertContains("setModified\\(\"" + notes.getName() + "\",false\\)");
		tester.assertContains("Saved notes.txt.");
	}

	@Test
	void describesADocumentInAPopoverPointingAtItsAction()
	{
		Tab todo = tabs(DOCUMENTS).getTabs().get(2);
		String action = DOCUMENTS + ":element:list:headers:" + todo.getName() +
			":actions:action:1";
		tester.assertContains("antilia-popover.*\\.js");

		tester.executeAjaxEvent(action, "click");

		tester.assertContains("Wicket.Popover.show\\(\"" +
			tester.getComponentFromLastRenderedPage(action).getMarkupId() + "\"");
		tester.assertContains("\"body\":\"todo.txt: 2 lines, 51 characters; all saved.\"");
		tester.assertContains("\"variant\":\"default\"");
		tester.assertContains("\"title\":\"todo.txt\"");
		tester.assertContains("\"placement\":\"top-center\"");
	}

	@Test
	void forgetsTheDocumentClosedAndOpensNewOnes()
	{
		Tabs documents = tabs(DOCUMENTS);
		Tab notes = documents.getTabs().get(1);

		send(documents, "close", notes.getName());
		assertEquals(List.of("todo.txt"), page.getDocuments().stream().map(Document::getName)
			.toList());
		tester.assertContains("The server forgot notes.txt, closed.");

		tester.clickLink(inCase("documentsTitle", "newDocument"));
		assertEquals(List.of("todo.txt", "untitled-1.txt"), page.getDocuments().stream()
			.map(Document::getName).toList());
		Tab added = documents.getTabs().get(2);
		assertEquals(added, documents.getSelectedTab());
		tester.assertContains("Opened untitled-1.txt.");
		tester.assertContains(".select\\(\"" + added.getName() + "\"\\)");
		assertFalse(tester.getLastResponseAsString().contains("Book the flights"),
			"the other documents stay as they are in the browser");
	}

	@Test
	void switchesTheSecondTabsAndHearsTheTabSelected()
	{
		Tabs settings = tabs(SETTINGS);
		assertEquals(TabsOrientation.VERTICAL, settings.getOrientation());
		assertEquals(TabsDesign.PILLS, settings.getDesign());
		tester.assertLabel(inCase("settingsTitle", "selected"),
			"The server knows the tab selected: Profile.");

		FormTester form = tester.newFormTester(inCase("settingsTitle", "form"));
		form.setValue("vertical", false);
		tester.executeAjaxEvent(inCase("settingsTitle", "form:vertical"), "change");
		form = tester.newFormTester(inCase("settingsTitle", "form"));
		form.select("design", 2);
		tester.executeAjaxEvent(inCase("settingsTitle", "form:design"), "change");
		assertEquals(TabsOrientation.HORIZONTAL, settings.getOrientation());
		assertEquals(TabsDesign.UNDERLINE, settings.getDesign());
		tester.assertContains("antilia-tabs-horizontal antilia-tabs-underline");

		send(settings, "select", "privacy");
		assertEquals("privacy", settings.getModelObject());
		tester.assertLabel(inCase("settingsTitle", "selected"),
			"The server knows the tab selected: Privacy.");
	}

	@Test
	void showsTheGalleryOfDesigns()
	{
		Tabs segmented = tabs(inCase("galleryTitle", "topDesigns:0:tabs"));
		assertEquals(TabsDesign.SEGMENTED, segmented.getDesign());
		assertEquals(TabsOrientation.HORIZONTAL, segmented.getOrientation());
		assertEquals("overview", segmented.getSelectedTab().getName());
		Tabs steps = tabs(inCase("galleryTitle", "sideDesigns:1:tabs"));
		assertEquals(TabsDesign.STEPS, steps.getDesign());
		assertEquals(TabsOrientation.VERTICAL, steps.getOrientation());
		assertEquals("payment", steps.getSelectedTab().getName());
		assertEquals(4, steps.getTabs().size());
		tester.assertContains("antilia-tabs-vertical antilia-tabs-cards");
		tester.assertContains("antilia-tabs-horizontal antilia-tabs-folder");
		tester.assertContains("<figcaption>Side bar</figcaption>");
	}

	@Test
	void givesTheAppsTheDocumentsTheSectionsAndTheTexts()
	{
		JSONObject config = new JSONObject(TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "data-framework-app", "react")
			.getAttribute("data-config"));

		assertEquals("notes.txt", config.getJSONArray("documents").getJSONObject(0)
			.getString("name"));
		assertEquals("[\"profile\",\"alerts\",\"privacy\"]",
			config.getJSONArray("sections").toString());
		assertEquals("Profile", config.getJSONObject("texts").getString("profile"));
		assertEquals("Design", config.getJSONObject("texts").getString("design"));
		assertEquals("Close {0}?", config.getJSONObject("tabs").getString("closeModifiedTitle"));
		assertTrue(config.getJSONObject("icons").getString("info").contains("<svg"));
		assertTrue(config.getBoolean("vertical"));
		assertEquals(9, config.getJSONArray("designs").length());
		assertEquals("[\"sidebar\",\"steps\",\"cards\"]", config.getJSONObject("gallery")
			.getJSONObject("side").getJSONArray("designs").toString());
		assertEquals("payment", config.getJSONObject("gallery").getJSONObject("side")
			.getString("selected"));
		assertEquals("Steps", config.getJSONObject("texts").getString("design.STEPS"));
		assertEquals("Shipping", config.getJSONObject("texts").getString("shipping"));
		assertEquals("pills", config.getString("design"));
	}

	@Test
	void speaksGermanAndSpanish()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(TabsPage.class);
		tester.assertContains("Dokumente, auf dem Server bearbeitet");

		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(TabsPage.class);
		tester.assertContains("Documentos, editados en el servidor");
		Component selected = tester.getComponentFromLastRenderedPage(inCase("settingsTitle",
			"selected"));
		assertEquals("El servidor conoce la pestaña seleccionada: Perfil.",
			selected.getDefaultModelObjectAsString());
	}
}
