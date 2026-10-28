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
package com.antiliacode.widgets.breadcrumb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.menu.AjaxMenuItem;
import com.antiliacode.widgets.menu.BookmarkableMenuItem;
import com.antiliacode.widgets.menu.IMenuItem;
import com.antiliacode.widgets.menu.LinkMenuItem;
import com.antiliacode.widgets.menu.MenuItem;
import com.antiliacode.widgets.menu.SubMenu;
import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.Looks;

class BreadcrumbTest extends WicketTestCase
{
	private static final IIcon ICON = () -> "<svg class=\"icon\"></svg>";

	private static String crumb(int place)
	{
		return "breadcrumb:element:list:crumbs:" + place + ":crumb";
	}

	/**
	 * @return the links of the crumbs, in order
	 */
	private List<TagTester> links()
	{
		return TagTester.createTags(tester.getLastResponseAsString(), tag -> !tag.isClose() &&
			String.valueOf(tag.getAttribute("class")).contains("antilia-breadcrumb-link"), false);
	}

	private List<TagTester> items()
	{
		return TagTester.createTags(tester.getLastResponseAsString(), tag -> "li".equals(
			tag.getName()) && !tag.isClose() &&
			String.valueOf(tag.getAttribute("class")).contains("antilia-breadcrumb-item"), false);
	}

	/**
	 * @return a trail of the names, each crumb calling the server via Ajax
	 */
	private static List<IMenuItem> trail(List<String> calls, String... names)
	{
		List<IMenuItem> items = new ArrayList<>();
		for (String name : names)
		{
			items.add(AjaxMenuItem.of(Model.of(name), target -> calls.add(name)));
		}
		return items;
	}

	@Test
	void rendersTheCrumbsTheLastOneThePageShown()
	{
		List<String> calls = new ArrayList<>();
		Breadcrumb breadcrumb = new Breadcrumb("breadcrumb", List.of(
			new BookmarkableMenuItem(Model.of("Home"), TestPage.class).setIcon(ICON)
				.setTooltip(Model.of("The home page")),
			new SubMenu(Model.of("Products")),
			LinkMenuItem.of(Model.of("Laptops"), () -> calls.add("laptops")),
			AjaxMenuItem.of(Model.of("Gaming"), target -> calls.add("gaming")).setEnabled(false),
			AjaxMenuItem.of(Model.of("Hidden"), target -> calls.add("hidden")).setVisible(false),
			AjaxMenuItem.of(Model.of("Model X"), target -> calls.add("x"))
				.setTooltip(Model.of("You are here"))));
		tester.startPage(new TestPage(breadcrumb));

		TagTester nav = tester.getTagByWicketId("element");
		assertEquals("nav", nav.getName());
		assertEquals("antilia-breadcrumb antilia-breadcrumb-chevron", nav.getAttribute("class"));
		assertEquals("Breadcrumb", nav.getAttribute("aria-label"));
		assertEquals(breadcrumb.getBreadcrumbMarkupId(), nav.getAttribute("id"));
		TagTester list = tester.getTagByWicketId("list");
		assertEquals("ol", list.getName());
		assertEquals("antilia-breadcrumb-list", list.getAttribute("class"));
		assertEquals(5, items().size(), "the item that does not show is left out");
		assertEquals(5, breadcrumb.getShownItems().size());

		List<TagTester> links = links();
		assertEquals(3, links.size(), tester.getLastResponseAsString());
		TagTester home = links.get(0);
		assertEquals("a", home.getName());
		assertEquals("antilia-breadcrumb-link", home.getAttribute("class"));
		assertEquals("The home page", home.getAttribute("title"));
		assertTrue(home.getAttribute("href").contains("TestPage"), home.getAttribute("href"));
		assertTrue(home.getValue().contains("<svg class=\"icon\"></svg>"));
		assertTrue(home.getValue().contains("class=\"antilia-breadcrumb-label\">Home</span>"));

		// a submenu has no link: a crumb leading nowhere
		String html = tester.getLastResponseAsString();
		TagTester products = TagTester.createTagByAttribute(html, "class", "antilia-breadcrumb-text");
		assertEquals("span", products.getName());
		assertTrue(products.getValue().contains(">Products</span>"), products.getValue());

		TagTester gaming = links.get(2);
		assertEquals("true", gaming.getAttribute("aria-disabled"));
		assertNull(gaming.getAttribute("href"));

		TagTester current = TagTester.createTagByAttribute(html, "aria-current", "page");
		assertEquals("span", current.getName());
		assertEquals("antilia-breadcrumb-page", current.getAttribute("class"));
		assertEquals("You are here", current.getAttribute("title"));
		assertTrue(current.getValue().contains(">Model X</span>"));
		assertEquals("antilia-breadcrumb-item antilia-breadcrumb-current",
			items().get(4).getAttribute("class"));
		assertEquals("antilia-breadcrumb-item", items().get(0).getAttribute("class"));
		assertFalse(html.contains("antilia-breadcrumb-more"), "a short trail does not collapse");

		tester.clickLink(crumb(2) + ":link");
		assertEquals(List.of("laptops"), calls);
	}

	@Test
	void collapsesALongTrailTheServerHidingTheCrumbsInTheMiddle()
	{
		List<String> calls = new ArrayList<>();
		Breadcrumb breadcrumb = new Breadcrumb("breadcrumb", trail(calls, "Home", "Products",
			"Computers", "Laptops", "Gaming", "Model X")).setMaxItems(4);
		tester.startPage(new TestPage(breadcrumb));

		List<TagTester> items = items();
		assertEquals(7, items.size(), "six crumbs and the button");
		TagTester more = items.get(1);
		assertEquals("antilia-breadcrumb-item antilia-breadcrumb-more", more.getAttribute("class"));
		TagTester button = TagTester.createTagByAttribute(tester.getLastResponseAsString(), "class",
			"antilia-breadcrumb-more-button");
		assertEquals("button", button.getName());
		assertEquals("button", button.getAttribute("type"));
		assertEquals("…", button.getValue());
		assertEquals("Show the whole path (2 more)", button.getAttribute("aria-label"));
		assertEquals("Show the whole path (2 more)", button.getAttribute("title"));
		for (int place = 0; place < 7; place++)
		{
			boolean hidden = place == 2 || place == 3;
			assertEquals(hidden ? "hidden" : null, items.get(place).getAttribute("hidden"),
				"crumb " + place);
			assertEquals(hidden ? "" : null,
				items.get(place).getAttribute(Breadcrumb.COLLAPSED_ATTRIBUTE), "crumb " + place);
		}
		tester.assertContains("Wicket.Breadcrumb.attach\\(\"" + breadcrumb.getBreadcrumbMarkupId() +
			"\", \\{\"separator\":\"chevron\",\"maxItems\":4,\"texts\":\\{\"label\":\"Breadcrumb\"," +
			"\"more\":\"Show the whole path \\(\\{0\\} more\\)\"\\}\\}\\);");

		// the crumbs hidden still call the server
		tester.clickLink(crumb(2) + ":link");
		assertEquals(List.of("Products"), calls);
	}

	@Test
	void collapsesAsTheScriptDoes()
	{
		assertArrayEquals(new int[] { 1, 3 }, Breadcrumb.collapsed(6, 4));
		assertNull(Breadcrumb.collapsed(4, 4));
		assertNull(Breadcrumb.collapsed(6, 0));
		assertArrayEquals(new int[] { 1, 5 }, Breadcrumb.collapsed(6, 1));
		assertEquals(0, new Breadcrumb("b", List.of()).setMaxItems(-3).getMaxItems());
	}

	@Test
	void readsTheModelEachTimeItRenders()
	{
		List<String> calls = new ArrayList<>();
		ListModel<IMenuItem> model = new ListModel<>(new ArrayList<>(trail(calls, "Home",
			"Countries")));
		tester.startPage(new TestPage(new Breadcrumb("breadcrumb", model).setMaxItems(3)));
		assertEquals(2, items().size());

		model.getObject().addAll(trail(calls, "Europe", "Spain", "Madrid"));
		tester.startPage(tester.getLastRenderedPage());

		assertEquals(6, items().size(), "five crumbs and the button");
		tester.assertContains(">Madrid</span></span>");
	}

	@Test
	void drawsTheSeparatorChosenAndTakesTheNameGiven()
	{
		Breadcrumb breadcrumb = new Breadcrumb("breadcrumb", trail(new ArrayList<>(), "Home",
			"Here")).setSeparator(BreadcrumbSeparator.SLASH).setLabel(Model.of("You are here"));
		tester.startPage(new TestPage(breadcrumb));

		TagTester nav = tester.getTagByWicketId("element");
		assertEquals("antilia-breadcrumb antilia-breadcrumb-slash", nav.getAttribute("class"));
		assertEquals("You are here", nav.getAttribute("aria-label"));
		assertEquals(BreadcrumbSeparator.SLASH, breadcrumb.getSeparator());
		assertEquals(BreadcrumbSeparator.CHEVRON,
			breadcrumb.setSeparator(null).getSeparator());
		assertEquals("antilia-breadcrumb-arrow", BreadcrumbSeparator.ARROW.getCssClass());
		assertEquals("arrow", BreadcrumbSeparator.ARROW.cssName());
	}

	@Test
	void speaksTheLanguageOfThePage()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(new TestPage(new Breadcrumb("breadcrumb", trail(new ArrayList<>(), "A", "B",
			"C", "D")).setMaxItems(2)));

		assertEquals("Brotkrümelnavigation", tester.getTagByWicketId("element")
			.getAttribute("aria-label"));
		tester.assertContains("aria-label=\"Den ganzen Pfad zeigen \\(2 weitere\\)\"");

		tester.getSession().setLocale(new Locale("es"));
		tester.startPage(new TestPage(new Breadcrumb("breadcrumb", trail(new ArrayList<>(), "A", "B",
			"C", "D")).setMaxItems(2)));
		assertEquals("Ruta de navegación", tester.getTagByWicketId("element")
			.getAttribute("aria-label"));
		tester.assertContains("aria-label=\"Mostrar toda la ruta \\(2 más\\)\"");
	}

	@Test
	void takesTheClassesOfBootstrapsLook()
	{
		Looks.setSessionLook(Look.BOOTSTRAP);
		tester.startPage(new TestPage(new Breadcrumb("breadcrumb", trail(new ArrayList<>(), "Home",
			"Products", "Laptops")).setMaxItems(2)));

		assertEquals("antilia-breadcrumb-list breadcrumb",
			tester.getTagByWicketId("list").getAttribute("class"));
		List<TagTester> items = items();
		assertEquals("antilia-breadcrumb-item breadcrumb-item", items.get(0).getAttribute("class"));
		assertEquals("antilia-breadcrumb-item breadcrumb-item antilia-breadcrumb-more",
			items.get(1).getAttribute("class"));
		assertEquals("antilia-breadcrumb-item breadcrumb-item antilia-breadcrumb-current active",
			items.get(3).getAttribute("class"));
	}

	@Test
	void rendersWhatItNeedsInTheBrowser()
	{
		tester.startPage(new TestPage(new Breadcrumb("breadcrumb", List.of())));

		tester.assertContains("antilia-breadcrumb.js");
		tester.assertContains("antilia-breadcrumb.css");
	}

	@Test
	void wantsALinkThatHoldsTheIconAndTheLabel()
	{
		IMenuItem item = new MenuItem(Model.of("Bad"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getLink(String id)
			{
				return new Label(id, "no container");
			}
		};
		List<IMenuItem> items = List.of(item, AjaxMenuItem.of(Model.of("Here"), target -> {
		}));
		Exception thrown = assertThrows(Exception.class,
			() -> tester.startPage(new TestPage(new Breadcrumb("breadcrumb", items))));
		Throwable cause = thrown;
		while (cause.getCause() != null && !(cause instanceof IllegalArgumentException))
		{
			cause = cause.getCause();
		}
		assertTrue(cause instanceof IllegalArgumentException, thrown.toString());
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		public TestPage()
		{
			this(new Breadcrumb("breadcrumb", List.of()));
		}

		TestPage(Breadcrumb breadcrumb)
		{
			add(breadcrumb);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"breadcrumb\"></div></body></html>");
		}
	}
}
