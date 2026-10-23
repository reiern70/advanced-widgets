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
package com.antiliacode.widgets.demo.popover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.popover.PopoverDesign;
import com.antiliacode.widgets.popover.PopoverPlacement;
import com.antiliacode.widgets.popover.PopoverVariant;
import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.Looks;
import com.github.openjson.JSONObject;

/**
 * Checks {@link PopoversPage}: the popovers around the target in the design and the variant chosen,
 * the gallery following the variant, the facts the server renders as their popover opens, the
 * project renamed in a form in a popover, the help on the fields and the rows of the table, and
 * the configuration of the React and Svelte apps.
 */
class PopoversPageTest
{
	private WicketTester tester;

	private PopoversPage page;

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
		page = tester.startPage(PopoversPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	/**
	 * @return the options the last response shows a popover with, pointing at a component
	 */
	private JSONObject shown(String trigger)
	{
		String response = tester.getLastResponseAsString();
		String call = "Wicket.Popover.show(\"" + tester.getComponentFromLastRenderedPage(trigger)
			.getMarkupId() + "\", ";
		int start = response.indexOf(call);
		assertTrue(start >= 0, response);
		start += call.length();
		return new JSONObject(response.substring(start, response.indexOf("});", start) + 1));
	}

	@Test
	void opensAPopoverOnEachSideInTheDesignAndTheVariantChosen()
	{
		tester.assertContains("antilia-popover.*\\.js");
		tester.assertContains("popovers-demo__place popovers-demo__place--top-left");
		tester.assertContains("popovers-demo__place popovers-demo__place--auto");

		String topLeft = inCase("placementsTitle", "placements:0:button");
		tester.executeAjaxEvent(topLeft, "click");
		JSONObject options = shown(topLeft);
		assertEquals("top-left", options.getString("placement"));
		assertEquals("Top left", options.getString("title"));
		assertEquals("card", options.getString("design"));
		assertEquals("default", options.getString("variant"));
		assertTrue(options.getString("body").startsWith("Opened Top left,"));

		FormTester form = tester.newFormTester(inCase("placementsTitle", "controls"));
		form.select("design", PopoverDesign.NEON.ordinal());
		tester.executeAjaxEvent(inCase("placementsTitle", "controls:design"), "change");
		form = tester.newFormTester(inCase("placementsTitle", "controls"));
		form.select("variant", PopoverVariant.SUCCESS.ordinal());
		tester.executeAjaxEvent(inCase("placementsTitle", "controls:variant"), "change");
		assertEquals(PopoverDesign.NEON, page.getDesign());
		assertEquals(PopoverVariant.SUCCESS, page.getVariant());
		// the gallery in the variant chosen
		tester.assertContains("antilia-popover antilia-popover-success antilia-popover-comic");

		String auto = inCase("placementsTitle", "placements:" + PopoverPlacement.AUTO.ordinal() +
			":button");
		tester.executeAjaxEvent(auto, "click");
		options = shown(auto);
		assertEquals("auto", options.getString("placement"));
		assertEquals("neon", options.getString("design"));
		assertEquals("success", options.getString("variant"));
	}

	/** The classes of the glass popover of the gallery, in the default variant and look. */
	private static final String GLASS = "antilia-popover antilia-popover-default " +
		"antilia-popover-glass antilia-look-antilia";

	@Test
	void drawsTheGalleryInTheLookOfTheSession()
	{
		Looks.setSessionLook(Look.BOOTSTRAP);
		tester.startPage(PopoversPage.class);

		String html = tester.getLastResponseAsString();
		TagTester preview = TagTester.createTagByAttribute(html, "class", "antilia-popover " +
			"antilia-popover-default antilia-popover-card antilia-look-bootstrap");
		assertTrue(preview != null);
		assertTrue(html.contains("class=\"antilia-popover-header popover-header\""));
		assertTrue(html.contains("class=\"antilia-popover-body popover-body\""));
		assertTrue(html.contains("class=\"antilia-popover-close btn-close\""));
	}

	@Test
	void opensEachDesignOfTheGallery()
	{
		tester.assertContains(GLASS);
		TagTester preview = TagTester.createTagByAttribute(tester.getLastResponseAsString(), "class",
			GLASS);
		assertEquals("bottom", preview.getAttribute("data-side"));
		assertEquals("center", preview.getAttribute("data-align"));
		String glass = inCase("galleryTitle", "gallery:designs:" + PopoverDesign.GLASS.ordinal() +
			":try");
		tester.executeAjaxEvent(glass, "click");
		JSONObject options = shown(glass);
		assertEquals("glass", options.getString("design"));
		assertEquals("Glass", options.getString("title"));
		assertEquals("bottom-center", options.getString("placement"));
		assertEquals("14rem", options.getString("maxWidth"));

		// the gallery and what it opens where the drop-down says
		FormTester form = tester.newFormTester(inCase("galleryTitle", "galleryControls"));
		form.select("position", PopoversPage.POSITIONS.indexOf(PopoverPlacement.LEFT_TOP));
		tester.executeAjaxEvent(inCase("galleryTitle", "galleryControls:position"), "change");
		assertEquals(PopoverPlacement.LEFT_TOP, page.getPosition());
		preview = TagTester.createTagByAttribute(tester.getLastResponseAsString(), "class",
			GLASS);
		assertEquals("left", preview.getAttribute("data-side"));
		assertEquals("start", preview.getAttribute("data-align"));
		tester.executeAjaxEvent(glass, "click");
		assertEquals("left-top", shown(glass).getString("placement"));
		assertEquals(12, PopoversPage.POSITIONS.size());
		assertEquals("end", PopoversPage.alignmentOf(PopoverPlacement.BOTTOM_RIGHT));
		assertEquals("right", PopoversPage.sideOf(PopoverPlacement.RIGHT_CENTER));
	}

	@Test
	void rendersTheFactsAsTheirPopoverOpens()
	{
		Component content = tester.getComponentFromLastRenderedPage(inCase("richTitle",
			"statusContent"));
		TagTester hidden = TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			content.getMarkupId());
		assertEquals("antilia-popover-content", hidden.getAttribute("class"));

		tester.executeAjaxEvent(inCase("richTitle", "status"), "click");

		tester.assertComponentOnAjaxResponse(content);
		JSONObject options = shown(inCase("richTitle", "status"));
		assertEquals(content.getMarkupId(), options.getString("content"));
		assertEquals("How it is now", options.getString("title"));
		assertEquals("bottom-left", options.getString("placement"));
		tester.assertContains("<dt>Java</dt><dd>" + System.getProperty("java.version") + "</dd>");
	}

	@Test
	void renamesTheProjectInAFormInAPopover()
	{
		Component content = tester.getComponentFromLastRenderedPage(inCase("richTitle",
			"renameContent"));
		tester.executeAjaxEvent(inCase("richTitle", "rename"), "click");
		tester.assertComponentOnAjaxResponse(content);
		JSONObject options = shown(inCase("richTitle", "rename"));
		assertEquals(content.getMarkupId(), options.getString("content"));
		assertEquals("aurora", options.getString("design"));
		assertEquals("right-top", options.getString("placement"));
		assertEquals("Apollo", page.getDraft());

		// no name: the form says so, in the popover
		FormTester form = tester.newFormTester(inCase("richTitle", "renameContent:renameForm"));
		form.setValue("name", "  ");
		tester.executeAjaxEvent(inCase("richTitle", "renameContent:renameForm:save"), "click");
		assertEquals("Apollo", page.getProject());
		tester.assertComponentOnAjaxResponse(content);
		tester.assertContains("Give the project a name.");
		assertTrue(!tester.getLastResponseAsString().contains("Wicket.Popover.closeAll()"));

		form = tester.newFormTester(inCase("richTitle", "renameContent:renameForm"));
		form.setValue("name", " Artemis ");
		tester.executeAjaxEvent(inCase("richTitle", "renameContent:renameForm:save"), "click");
		assertEquals("Artemis", page.getProject());
		tester.assertComponentOnAjaxResponse(inCase("richTitle", "project"));
		tester.assertContains("Wicket.Popover.closeAll\\(\\);");

		tester.executeAjaxEvent(inCase("richTitle", "renameContent:renameForm:cancel"), "click");
		tester.assertContains("Wicket.Popover.closeAll\\(\\);");
	}

	@Test
	void helpsOnTheFieldsAndTellsMoreOfTheRows()
	{
		String password = inCase("fieldsTitle", "fields:field:1:help");
		TagTester help = TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			tester.getComponentFromLastRenderedPage(inCase("fieldsTitle", "fields:field:0:help"))
				.getMarkupId());
		assertEquals("Help on User name", help.getAttribute("aria-label"));
		tester.executeAjaxEvent(password, "click");
		JSONObject options = shown(password);
		assertEquals("Password", options.getString("title"));
		assertEquals("inverse", options.getString("design"));
		assertEquals("default", options.getString("variant"));
		assertEquals("right-center", options.getString("placement"));

		String japan = inCase("tableTitle", "countries:2:about");
		tester.executeAjaxEvent(japan, "click");
		options = shown(japan);
		assertEquals("Japan", options.getString("title"));
		assertEquals("In its own language: 日本. ISO codes JP / JPN; it pays in ￥ JPY.",
			options.getString("body"));
		assertEquals("callout", options.getString("design"));
		assertEquals("left-center", options.getString("placement"));
	}

	@Test
	void givesTheAppsThePlacementsTheDesignsAndTheTexts()
	{
		JSONObject config = new JSONObject(TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "data-framework-app", "react")
			.getAttribute("data-config"));

		assertEquals(13, config.getJSONArray("placements").length());
		assertEquals("top-left", config.getJSONArray("placements").getString(0));
		assertEquals(9, config.getJSONArray("designs").length());
		assertEquals(5, config.getJSONArray("variants").length());
		assertEquals("card", config.getString("design"));
		assertEquals(12, config.getJSONArray("positions").length());
		assertEquals("bottom-center", config.getString("position"));
		assertEquals("Position", config.getJSONObject("texts").getString("position"));
		assertEquals("Apollo", config.getString("project"));
		JSONObject texts = config.getJSONObject("texts");
		assertEquals("Left bottom", texts.getString("placement.LEFT_BOTTOM"));
		assertEquals("Top center", texts.getString("placement.TOP_CENTER"));
		assertEquals("Neon", texts.getString("design.NEON"));
		assertEquals("Warning", texts.getString("variant.WARNING"));
		assertEquals("Close", texts.getString("close"));
		assertEquals("password", config.getJSONArray("fields").getJSONObject(1).getString("key"));
		assertEquals("Germany", config.getJSONArray("countries").getJSONObject(0)
			.getString("name"));
		assertTrue(config.getJSONObject("icons").getString("rename").contains("<svg"));
	}

	@Test
	void speaksGermanAndSpanish()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(PopoversPage.class);
		tester.assertContains("Wo es sich öffnet");
		tester.assertContains("Deutschland");

		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(PopoversPage.class);
		tester.assertContains("Dónde se abre");
		tester.assertContains("Japón");
	}
}
