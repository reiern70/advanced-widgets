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
package com.antiliacode.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.antiliacode.widgets.demo.button.ButtonsPage;
import com.antiliacode.widgets.demo.looks.LooksPage;
import com.antiliacode.widgets.demo.progress.ProgressBarPage;
import com.antiliacode.widgets.demo.theme.ThemeEditorPage;
import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.LookAspect;
import com.antiliacode.widgets.theme.Looks;
import com.antiliacode.widgets.theme.Theme;
import com.antiliacode.widgets.theme.ThemeEditorPanel;

/**
 * Every page is in the theme and the look the user chooses in the top bar.
 */
class ThemedDemosTest extends WicketTestCase
{
	@Override
	protected WebApplication newApplication()
	{
		return new WicketApplication();
	}

	@ParameterizedTest
	@ValueSource(classes = { HomePage.class, ProgressBarPage.class, ThemeEditorPage.class })
	void theThemeChosenAppliesToTheWholePage(Class<? extends Page> page)
	{
		tester.startPage(page);
		assertTrue(bodyClass().contains(Theme.DEFAULT.getCssClass()), page.getSimpleName());

		choose("settings:theme", Theme.DARK.ordinal());

		assertEquals(Theme.DARK, DemoSession.get().getTheme());
		tester.startPage(page);
		assertTrue(bodyClass().contains(Theme.DARK.getCssClass()), page.getSimpleName());
	}

	@Test
	void theLookChosenAppliesToTheWholePageWithTheStyleSheetOfBootstrap()
	{
		tester.startPage(ButtonsPage.class);
		assertTrue(bodyClass().contains("antilia-look-antilia"), bodyClass());
		tester.assertContainsNot("bootstrap\\.min\\.css");

		choose("settings:look", Look.BOOTSTRAP.ordinal());

		assertEquals(Look.BOOTSTRAP, Looks.getSessionLook());
		// another page, in the look chosen on the first
		tester.startPage(ButtonsPage.class);
		TagTester body = TagTester.createTagByName(tester.getLastResponseAsString(), "body");
		assertTrue(bodyClass().contains("antilia-look-bootstrap"), bodyClass());
		assertEquals("bootstrap", body.getAttribute("data-antilia-look"));
		assertEquals("light", body.getAttribute("data-bs-theme"));
		tester.assertContains("bootstrap/5\\.[0-9.]+/bootstrap\\.min\\.css");
		tester.assertContains("antilia-look-bootstrap.*\\.css");
		// the buttons of the page are outside the <body>'s ThemeBehavior in Wicket's hierarchy,
		// in the look of the session all the same
		tester.assertContains("class=\"btn btn-primary btn-sm\"");
		tester.assertContainsNot("class=\"ant-button ");
	}

	@Test
	void aLookMadeOfBootstrapsWithTheColorsAndSizesOfAntilias()
	{
		tester.startPage(HomePage.class);

		choose("settings:look", 2);

		assertEquals(Look.BOOTSTRAP.with(LookAspect.COLOR, Look.ANTILIA).with(LookAspect.SIZE,
			Look.ANTILIA), Looks.getSessionLook());
		tester.startPage(ButtonsPage.class);
		TagTester body = TagTester.createTagByName(tester.getLastResponseAsString(), "body");
		assertEquals("bootstrap color:antilia size:antilia",
			body.getAttribute("data-antilia-look"));
		assertTrue(bodyClass().contains("antilia-look-bootstrap antilia-look-antilia"), bodyClass());
		tester.assertContains("antilia-look-bootstrap.*\\.css");
		tester.assertContains("class=\"btn ant-button--primary ant-button--small\"");
	}

	@Test
	void thePublicPagesChooseTheThemeTheLookAndTheLanguage()
	{
		tester.startPage(HomePage.class);

		tester.assertVisible("settings:theme");
		tester.assertVisible("settings:look");
		tester.assertVisible("settings:language");
	}

	@Test
	void theLooksSideBySideOnAPageWithBootstrap()
	{
		tester.startPage(LooksPage.class);

		tester.assertContains("bootstrap\\.min\\.css");
		String response = tester.getLastResponseAsString();
		assertEquals(2, response.split("data-antilia-look=\"bootstrap\"", -1).length - 1,
			"the Bootstrap look and Bootstrap's own markup");
		assertEquals(2, response.split("data-antilia-look=\"antilia\"", -1).length - 1,
			"the <body> and the Antilia look");
		tester.assertContains("class=\"btn btn-primary\"");
		tester.assertContains("class=\"ant-button ant-button--primary ant-button--medium\"");
	}

	@Test
	void theThemeEditorListsEveryProperty()
	{
		tester.startPage(ThemeEditorPage.class);

		String response = tester.getLastResponseAsString();
		for (String property : ThemeEditorPanel.PROPERTIES)
		{
			List<TagTester> fields = TagTester.createTagsByAttribute(response,
				"data-theme-property", "--antilia-theme-" + property, false);
			assertEquals(1, fields.size(), property);
		}
		assertEquals("color", TagTester.createTagByAttribute(response, "data-theme-property",
			"--antilia-theme-primary").getAttribute("type"));
		tester.assertContains("Wicket.ThemeEditor.init");
	}

	@Test
	void theThemeEditorPreviewsTheNotificationsInTheColorsOfTheTheme()
	{
		tester.startPage(ThemeEditorPage.class);

		tester.clickLink("preview:tabs:tabs-container:tabs:4:link");

		String response = tester.getLastResponseAsString();
		for (String type : List.of("success", "error", "warning", "info"))
		{
			assertTrue(response.contains("{\"type\":\"" + type + "\",\"title\":"), type);
			assertTrue(response.contains("--antilia-theme-" + type + " color of the theme"), type);
		}
		assertTrue(response.contains("Wicket.Notifications.attach("), response);

		tester.clickLink("preview:tabs:panel:again");
		tester.assertContains("Wicket.Notifications.closeAll");
		tester.assertContains("\\{\"type\":\"error\",\"title\":\"Error\"");
	}

	private String bodyClass()
	{
		return TagTester.createTagByName(tester.getLastResponseAsString(), "body")
			.getAttribute("class");
	}

	@SuppressWarnings("unchecked")
	private void choose(String path, int index)
	{
		DropDownChoice<Object> choice = (DropDownChoice<Object>)tester
			.getComponentFromLastRenderedPage(path);
		tester.getRequest().getPostParameters().setParameterValue(choice.getInputName(),
			choice.getChoiceRenderer().getIdValue(choice.getChoices().get(index), index));
		tester.executeAjaxEvent(choice, "change");
	}
}
