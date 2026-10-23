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
package com.antiliacode.widgets.demo.resizable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.resizable.ResizableBehavior;
import com.github.openjson.JSONObject;

/**
 * Checks {@link ResizablePage}: the sizes the server hears and keeps, in the session or in the
 * page, the card taken away and put back, and the configuration of the React and Svelte apps.
 */
class ResizablePageTest
{
	private WicketTester tester;

	private ResizablePage page;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		page = tester.startPage(ResizablePage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private void resize(String path, String width, String height)
	{
		Component component = tester.getComponentFromLastRenderedPage(path);
		tester.getRequest().setParameter("width", width);
		tester.getRequest().setParameter("height", height);
		tester.executeBehavior(component.getBehaviors(ResizableBehavior.class).get(0));
	}

	/**
	 * @return the classes of the element of a component, as rendered
	 */
	private String classes(String path)
	{
		return TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			tester.getComponentFromLastRenderedPage(path).getMarkupId()).getAttribute("class");
	}

	@Test
	void makesTheNoteTheSidePanelAndTheCardResizable()
	{
		assertEquals("resizable-demo__note antilia-resizable antilia-resizable-vertical",
			classes("themed:note"));
		assertEquals("resizable-demo__side antilia-resizable antilia-resizable-horizontal",
			classes("themed:side"));
		assertEquals("resizable-demo__card antilia-resizable antilia-resizable-both",
			classes("themed:cardPlace:card"));
		tester.assertContains("\"axis\":\"both\",\"enabled\":true,\"width\":320,\"height\":180");
		tester.assertLabel("themed:sizes", "The server knows: the note as the page makes it " +
			"high, the side panel as the page makes it wide, the card 320 px × 180 px.");
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
	}

	@Test
	void keepsTheSizesOfTheNoteAndTheSidePanelInTheSession()
	{
		resize("themed:note", "", "200");
		resize("themed:side", "260", "");
		tester.assertLabel("themed:sizes", "The server knows: the note 200 px high, the side " +
			"panel 260 px wide, the card 320 px × 180 px.");

		tester.startPage(ResizablePage.class);
		tester.assertContains("\"axis\":\"vertical\",\"enabled\":true,\"height\":200,");
		tester.assertContains("\"axis\":\"horizontal\",\"enabled\":true,\"width\":260,");
	}

	@Test
	void theCardTakenAwayComesBackInTheSizeItHad()
	{
		resize("themed:cardPlace:card", "400", "220");
		tester.clickLink("themed:toggleCard");
		assertFalse(page.isCardShown());
		tester.assertLabel("themed:sizes", "The server knows: the note as the page makes it " +
			"high, the side panel as the page makes it wide, the card taken away.");
		tester.assertContains("Put the card back");

		tester.clickLink("themed:toggleCard");
		assertTrue(page.isCardShown());
		tester.assertContains("\"axis\":\"both\",\"enabled\":true,\"width\":400,\"height\":220");
		tester.assertContains("the card 400 px × 220 px.");
	}

	@Test
	void givesTheAppsTheTextsAndTheSizes()
	{
		JSONObject config = new JSONObject(TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "data-framework-app", "react")
			.getAttribute("data-config"));

		assertEquals("[\"inbox\",\"drafts\",\"sent\",\"archive\"]",
			config.getJSONArray("items").toString());
		assertEquals(320, config.getJSONObject("card").getInt("width"));
		assertTrue(config.getJSONObject("texts").getString("sizes").startsWith("The app knows"));
		assertEquals("Drag, or use the arrow keys, to resize", config.getString("handle"));
	}

	@Test
	void speaksGermanAndSpanish()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(ResizablePage.class);
		tester.assertContains("Eine Notiz: ihre Höhe");

		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(ResizablePage.class);
		tester.assertContains("Una nota: su altura");
	}
}
