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
package com.antiliacode.widgets.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;

/**
 * Puts a look on an element and gives the buttons inside it the classes of the look.
 */
class LooksTest extends WicketTestCase
{
	@Test
	void putsTheLookOnTheElementInTheColorModeOfTheTheme()
	{
		tester.startPage(new LookedPage(Model.of(Theme.NAVY), Model.of(Look.BOOTSTRAP)));

		TagTester themed = tester.getTagByWicketId("themed");
		assertEquals("box antilia-theme antilia-theme-navy antilia-look-bootstrap",
			themed.getAttribute("class"));
		assertEquals("bootstrap", themed.getAttribute("data-antilia-look"));
		assertEquals("light", themed.getAttribute("data-bs-theme"));
		tester.assertContains("antilia-look-bootstrap.*\\.css");

		tester.startPage(new LookedPage(Model.of(Theme.DARK), Model.of(Look.BOOTSTRAP)));

		assertEquals("dark", tester.getTagByWicketId("themed").getAttribute("data-bs-theme"));
	}

	@Test
	void theBootstrapThemesSetTheColorModeOfBootstrapInAnyLook()
	{
		tester.startPage(new LookedPage(Model.of(Theme.BOOTSTRAP_DARK), Model.of(Look.ANTILIA)));

		TagTester themed = tester.getTagByWicketId("themed");
		assertEquals("box antilia-theme antilia-theme-bootstrap-dark antilia-look-antilia",
			themed.getAttribute("class"));
		assertEquals("dark", themed.getAttribute("data-bs-theme"));
		tester.assertContainsNot("antilia-look-bootstrap.*\\.css");
	}

	@Test
	void theButtonsInsideHaveTheClassesOfTheLook()
	{
		tester.startPage(new LookedPage(Model.of(Theme.BLUE), Model.of(Look.BOOTSTRAP)));

		assertEquals("btn btn-link btn-sm", tester.getTagByWicketId("inside").getAttribute("class"));
		assertEquals("ant-button ant-button--primary ant-button--large",
			tester.getTagByWicketId("outside").getAttribute("class"),
			"outside the element, in the default look");
	}

	@Test
	void theButtonsOutsideHaveTheDefaultLookOfTheApplication()
	{
		Looks.setDefault(tester.getApplication(), () -> Look.BOOTSTRAP);
		tester.startPage(new LookedPage(Model.of(Theme.BLUE), null));

		TagTester themed = tester.getTagByWicketId("themed");
		assertNull(themed.getAttribute("data-antilia-look"));
		assertEquals("btn btn-link btn-sm", tester.getTagByWicketId("inside").getAttribute("class"));
		assertEquals("btn btn-primary btn-lg", tester.getTagByWicketId("outside").getAttribute(
			"class"));
		tester.assertContains("antilia-look-bootstrap.*\\.css");
	}

	@Test
	void aButtonShowingOnlyItsIconInBootstrapsLook()
	{
		ButtonBehavior button = new ButtonBehavior(Variant.DANGER, Size.MEDIUM).setIconOnly(true);

		assertEquals("btn btn-danger ant-button--icon-only", button.getCssClasses(Look.BOOTSTRAP));
		assertEquals(button.getCssClasses(), button.getCssClasses(Look.ANTILIA));
	}

	@Test
	void thePartsAreThoseOfTheBrowser()
	{
		Properties table = Look.Table.PROPERTIES;
		Set<String> parts = new HashSet<>();
		for (LookPart part : LookPart.values())
		{
			assertEquals(part.getAspect().getName(), table.getProperty("aspect." + part.getName()),
				part.name());
			parts.add(part.getName());
		}
		Set<String> browser = table.stringPropertyNames().stream()
			.filter(name -> name.startsWith("aspect."))
			.map(name -> name.substring("aspect.".length()))
			.collect(Collectors.toSet());
		assertEquals(browser, parts, "every part of look.ts is a LookPart");
		assertEquals("modal-dialog-header modal-header",
			Look.BOOTSTRAP.getPartClasses(LookPart.DIALOG_HEADER));
		assertEquals("modal-dialog-header", Look.ANTILIA.getPartClasses(LookPart.DIALOG_HEADER));
	}

	@Test
	void aLookMadeOfOthersTakesEachAspectFromItsLook()
	{
		ILook look = Look.BOOTSTRAP.with(LookAspect.COLOR, Look.ANTILIA).with(LookAspect.SIZE,
			Look.ANTILIA);

		assertEquals("bootstrap color:antilia size:antilia", look.getName());
		assertEquals(List.of(Look.BOOTSTRAP, Look.ANTILIA), look.getLooks());
		assertEquals("btn ant-button--primary ant-button--small", new ButtonBehavior(
			Variant.PRIMARY, Size.SMALL).getCssClasses(look));
		assertEquals(look, Look.BOOTSTRAP.with(LookAspect.SIZE, Look.ANTILIA).with(LookAspect.COLOR,
			Look.ANTILIA), "equal whatever the order");
		assertEquals(Look.BOOTSTRAP.getName(), Look.BOOTSTRAP.with(LookAspect.COLOR,
			Look.BOOTSTRAP).getName(), "an aspect from the base is no other look");

		tester.startPage(new LookedPage(Model.of(Theme.NAVY), Model.of(look)));

		TagTester themed = tester.getTagByWicketId("themed");
		assertEquals("box antilia-theme antilia-theme-navy antilia-look-bootstrap antilia-look-antilia",
			themed.getAttribute("class"));
		assertEquals("bootstrap color:antilia size:antilia", themed.getAttribute("data-antilia-look"));
		assertEquals("light", themed.getAttribute("data-bs-theme"));
		assertEquals("btn ant-button--ghost ant-button--small",
			tester.getTagByWicketId("inside").getAttribute("class"));
		tester.assertContains("antilia-look-bootstrap.*\\.css");
	}

	@Test
	void theLookOfTheSessionIsThatOfEveryPage()
	{
		tester.startPage(new LookedPage(Model.of(Theme.BLUE), null));
		assertEquals("ant-button ant-button--primary ant-button--large",
			tester.getTagByWicketId("outside").getAttribute("class"));

		Looks.setSessionLook(Look.BOOTSTRAP);

		assertTrue(tester.getSession().isTemporary() == false, "bound, so the look lasts");
		tester.startPage(new LookedPage(Model.of(Theme.BLUE), Looks.sessionLook()));
		assertEquals("bootstrap", tester.getTagByWicketId("themed").getAttribute(
			"data-antilia-look"));
		assertEquals("btn btn-primary btn-lg", tester.getTagByWicketId("outside").getAttribute(
			"class"));
		Looks.sessionLook().setObject(Look.ANTILIA);
		assertEquals(Look.ANTILIA, Looks.getSessionLook());
	}

	@Test
	void anElementGetsTheClassesOfItsPartsOnce()
	{
		ComponentTag tag = new ComponentTag("div", XmlTag.TagType.OPEN);
		tag.put("class", "modal-dialog-header extra");

		Looks.addClasses(tag, Look.BOOTSTRAP, LookPart.DIALOG_HEADER, null);

		assertEquals("modal-dialog-header extra modal-header", tag.getAttribute("class"));
	}

	@Test
	void theBootstrapLookGivesBootstrapTheChannelsOfTheTheme() throws Exception
	{
		String css = new String(
			Look.class.getResourceAsStream("antilia-look-bootstrap.css").readAllBytes(), "UTF-8");
		for (String[] pair : List.of(new String[] { "primary", "primary" },
			new String[] { "link-color", "accent" }, new String[] { "link-hover-color", "accent-hover" },
			new String[] { "body-color", "text" }, new String[] { "body-bg", "surface" },
			new String[] { "success", "success" }, new String[] { "danger", "error" },
			new String[] { "warning", "warning" }, new String[] { "info", "info" }))
		{
			String mapping = "--bs-" + pair[0] + "-rgb: var(--antilia-theme-" + pair[1] + "-rgb);";
			assertTrue(css.contains(mapping), mapping);
		}
		assertTrue(css.contains("--bs-secondary-color: var(--antilia-theme-muted);"));
	}

	@Test
	void theDaisyUiLookPutsItsStyleSheetAndItsClassesOnTheElement()
	{
		tester.startPage(new LookedPage(Model.of(Theme.DARK), Model.of(Look.DAISYUI)));

		TagTester themed = tester.getTagByWicketId("themed");
		assertEquals("box antilia-theme antilia-theme-dark antilia-look-daisyui",
			themed.getAttribute("class"));
		assertEquals("daisyui", themed.getAttribute("data-antilia-look"));
		assertNull(themed.getAttribute("data-bs-theme"), "no mode of Bootstrap's");
		tester.assertContains("antilia-look-daisyui.*\\.css");
		tester.assertContainsNot("antilia-look-bootstrap.*\\.css");
		assertEquals("btn btn-ghost btn-sm", tester.getTagByWicketId("inside").getAttribute("class"));
	}

	@Test
	void theDaisyUiLookGivesThePartsTheClassesOfDaisyUi()
	{
		assertEquals("daisyui", Look.DAISYUI.getName());
		assertEquals("btn btn-error btn-square", new ButtonBehavior(Variant.DANGER, Size.MEDIUM)
			.setIconOnly(true).getCssClasses(Look.DAISYUI));
		assertEquals("btn", new ButtonBehavior(Variant.SECONDARY, Size.MEDIUM).getCssClasses(
			Look.DAISYUI), "the neutral button of daisyUI");
		assertEquals("modal-dialog-content modal-box",
			Look.DAISYUI.getPartClasses(LookPart.DIALOG_CONTENT));
		assertEquals("ant-data-table table", Look.DAISYUI.getPartClasses(LookPart.TABLE));
		assertEquals("antilia-dropdown-toggle select",
			Look.DAISYUI.getPartClasses(LookPart.DROPDOWN_TOGGLE));
		assertNull(Look.DAISYUI.getCssClass(LookPart.DIALOG_HEADER), "no header in daisyUI");
		// every part daisyUI gives classes is one of the table, with those of look.ts
		Properties table = Look.Table.PROPERTIES;
		for (String name : table.stringPropertyNames())
		{
			if (name.startsWith("daisyui."))
			{
				assertTrue(table.containsKey("aspect." + name.substring("daisyui.".length())), name);
			}
		}

		ILook look = Look.DAISYUI.with(LookAspect.COLOR, Look.ANTILIA).with(LookAspect.SIZE,
			Look.ANTILIA);
		assertEquals("daisyui color:antilia size:antilia", look.getName());
		assertEquals("btn ant-button--primary ant-button--small", new ButtonBehavior(
			Variant.PRIMARY, Size.SMALL).getCssClasses(look));
	}

	@Test
	void theDaisyUiLookGivesDaisyUiTheColorsOfTheTheme() throws Exception
	{
		String css = new String(
			Look.class.getResourceAsStream("antilia-look-daisyui.css").readAllBytes(), "UTF-8");
		for (String[] pair : List.of(new String[] { "base-100", "surface" },
			new String[] { "base-200", "surface-alt" }, new String[] { "base-content", "text" },
			new String[] { "primary", "primary" }, new String[] { "primary-content", "on-primary" },
			new String[] { "success", "success" }, new String[] { "error", "error" },
			new String[] { "warning", "warning" }, new String[] { "info", "info" }))
		{
			String mapping = "--color-" + pair[0] + ": var(--antilia-theme-" + pair[1] + ");";
			assertTrue(css.contains(mapping), mapping);
		}
		assertTrue(css.contains("--antilia-look-radius: var(--radius-field"), "daisyUI's corners");
	}

	public static class LookedPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		LookedPage(IModel<Theme> theme, IModel<? extends ILook> look)
		{
			WebMarkupContainer themed = new WebMarkupContainer("themed");
			themed.add(new ThemeBehavior(theme, look));
			add(themed);
			themed.add(new ExternalLink("inside", "#in", "In")
				.add(new ButtonBehavior(Variant.GHOST, Size.SMALL)));
			add(new ExternalLink("outside", "#out", "Out")
				.add(new ButtonBehavior(Variant.PRIMARY, Size.LARGE)));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"themed\" class=\"box\"><a wicket:id=\"inside\"></a></div>" +
				"<a wicket:id=\"outside\"></a></body></html>");
		}
	}
}
