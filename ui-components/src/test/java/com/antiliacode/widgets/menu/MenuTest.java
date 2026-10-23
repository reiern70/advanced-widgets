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
package com.antiliacode.widgets.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;

class MenuTest extends WicketTestCase
{
	private static final IIcon ICON = () -> "<svg class=\"icon\"></svg>";

	/** The paths of the items of the bar, and of those of their submenus, from the page. */
	private static String bar(int index)
	{
		return "menu:element:bar:items:" + index + ":item";
	}

	private static String sub(String parent, int index)
	{
		return parent + ":list:items:" + index + ":item";
	}

	private static TagTester tagOf(String html, String attribute, String value)
	{
		return TagTester.createTagByAttribute(html, attribute, value);
	}

	private TagTester tagAt(String path)
	{
		return tagOf(tester.getLastResponseAsString(), "id",
			tester.getComponentFromLastRenderedPage(path).getMarkupId());
	}

	/**
	 * @return the items: Save (Ajax), File (New, Export (PDF), separator, Reset (a link)), a
	 *         separator and Help (an external link)
	 */
	private static List<IMenuItem> items(List<String> calls)
	{
		return List.of(
			AjaxMenuItem.of(Model.of("Save"), target -> calls.add("save")).setIcon(ICON)
				.setTooltip(Model.of("Save the document")),
			new SubMenu(Model.of("File")).setIcon(ICON).setTooltip(Model.of("Files")).add(
				AjaxMenuItem.of(Model.of("New"), target -> calls.add("new")),
				new SubMenu(Model.of("Export")).add(
					AjaxMenuItem.of(Model.of("PDF"), target -> calls.add("pdf"))),
				MenuSeparator.INSTANCE,
				LinkMenuItem.of(Model.of("Reset"), () -> calls.add("reset"))),
			MenuSeparator.INSTANCE,
			new ExternalItem(Model.of("Help"), "https://example.com/help"));
	}

	@Test
	void rendersTheBarItsItemsAndSubmenus()
	{
		List<String> calls = new ArrayList<>();
		Menu menu = new Menu("menu", items(calls)).setLabel(Model.of("Main"));
		tester.startPage(new TestPage(menu));
		String html = tester.getLastResponseAsString();

		TagTester element = tester.getTagByWicketId("element");
		assertEquals("antilia-menu antilia-menu-horizontal antilia-menu-bar",
			element.getAttribute("class"));
		assertEquals(menu.getMenuMarkupId(), element.getAttribute("id"));
		TagTester barTag = tester.getTagByWicketId("bar");
		assertEquals("antilia-menu-list", barTag.getAttribute("class"));
		assertEquals("menubar", barTag.getAttribute("role"));
		assertEquals("horizontal", barTag.getAttribute("aria-orientation"));
		assertEquals("Main", barTag.getAttribute("aria-label"));

		TagTester save = tagAt(bar(0) + ":link");
		assertEquals("a", save.getName());
		assertEquals("antilia-menu-link", save.getAttribute("class"));
		assertEquals("menuitem", save.getAttribute("role"));
		assertEquals("Save the document", save.getAttribute("title"));
		assertTrue(save.getValue().contains("<svg class=\"icon\"></svg>"));
		assertTrue(save.getValue().contains("class=\"antilia-menu-label\">Save</span>"));
		assertNull(save.getAttribute("aria-current"));
		assertNull(save.getAttribute("aria-disabled"));

		TagTester file = tagAt(bar(1) + ":trigger");
		assertEquals("button", file.getName());
		assertEquals("antilia-menu-link", file.getAttribute("class"));
		assertEquals("menuitem", file.getAttribute("role"));
		assertEquals("menu", file.getAttribute("aria-haspopup"));
		assertEquals("false", file.getAttribute("aria-expanded"));
		assertEquals("Files", file.getAttribute("title"));
		assertEquals(menu.getMenuMarkupId() + "-item-1", file.getAttribute("id"));
		TagTester list = tagAt(bar(1) + ":list");
		assertEquals(menu.getMenuMarkupId() + "-menu-1", list.getAttribute("id"));
		assertEquals(menu.getMenuMarkupId() + "-item-1-1-0",
			tagAt(sub(sub(bar(1), 1), 0) + ":link").getAttribute("id"));
		assertEquals(list.getAttribute("id"), file.getAttribute("aria-controls"));
		assertEquals(file.getAttribute("id"), list.getAttribute("aria-labelledby"));
		assertEquals("antilia-menu-submenu", list.getAttribute("class"));
		assertEquals("menu", list.getAttribute("role"));
		assertEquals("hidden", list.getAttribute("hidden"));
		assertEquals("antilia-menu-submenu-link",
			tagAt(sub(bar(1), 0) + ":link").getAttribute("class"));
		assertEquals("antilia-menu-submenu-link",
			tagAt(sub(bar(1), 1) + ":trigger").getAttribute("class"));
		assertTrue(tagAt(sub(sub(bar(1), 1), 0) + ":link").getValue().contains("PDF"));

		List<TagTester> separators = TagTester.createTagsByAttribute(html, "role", "separator",
			false);
		assertEquals(2, separators.size());
		assertEquals("antilia-menu-separator", separators.get(0).getAttribute("class"));
		assertEquals("horizontal", separators.get(0).getAttribute("aria-orientation"));
		assertEquals("vertical", separators.get(1).getAttribute("aria-orientation"));
		// the elements of Save, File, New, Export, PDF, Reset and Help
		assertEquals(7, html.split("role=\"none\"", -1).length - 1);
		assertEquals("https://example.com/help", tagAt(bar(3) + ":link").getAttribute("href"));

		tester.assertContains("antilia-menu.*\\.js");
		tester.assertContains("antilia-menu.*\\.css");
		tester.assertContains("Wicket.Menu.attach\\(\"" + menu.getMenuMarkupId() +
			"\", \\{\"orientation\":\"horizontal\",\"design\":\"bar\",\"inline\":false," +
			"\"openOnHover\":false,\"label\":\"Main\"\\}\\);");
		assertFalse(html.contains("style="), "no inline style");
	}

	@Test
	void givesItsPartsTheClassesOfTheLookTheyAreIn()
	{
		Looks.setSessionLook(Look.BOOTSTRAP);
		Menu menu = new Menu("menu", items(new ArrayList<>())).setDesign(MenuDesign.PILLS);
		tester.startPage(new TestPage(menu));
		String html = tester.getLastResponseAsString();

		assertEquals("antilia-menu antilia-menu-horizontal antilia-menu-pills",
			tester.getTagByWicketId("element").getAttribute("class"));
		assertEquals("antilia-menu-list nav nav-pills",
			tester.getTagByWicketId("bar").getAttribute("class"));
		assertEquals("antilia-menu-link nav-link", tagAt(bar(0) + ":link").getAttribute("class"));
		assertEquals("antilia-menu-link nav-link", tagAt(bar(1) + ":trigger").getAttribute("class"));
		assertEquals("antilia-menu-submenu dropdown-menu",
			tagAt(bar(1) + ":list").getAttribute("class"));
		assertEquals("antilia-menu-submenu-link dropdown-item",
			tagAt(sub(bar(1), 0) + ":link").getAttribute("class"));
		assertEquals("antilia-menu-separator dropdown-divider", TagTester.createTagsByAttribute(html,
			"role", "separator", false).get(0).getAttribute("class"));
		assertEquals("antilia-menu-item nav-item", TagTester.createTagsByAttribute(html, "role",
			"none", false).get(0).getAttribute("class"));

		menu.setDesign(MenuDesign.SIDEBAR).setOrientation(MenuOrientation.VERTICAL).setInline(true);
		tester.startPage(new TestPage(menu));
		assertEquals("antilia-menu antilia-menu-vertical antilia-menu-sidebar antilia-menu-inline",
			tester.getTagByWicketId("element").getAttribute("class"));
		assertEquals("antilia-menu-list nav nav-pills bg-body-tertiary rounded",
			tester.getTagByWicketId("bar").getAttribute("class"));
		assertEquals("vertical", tester.getTagByWicketId("bar").getAttribute("aria-orientation"));
		tester.assertContains("\"orientation\":\"vertical\",\"design\":\"sidebar\",\"inline\":true");

		assertEquals("antilia-menu-underline", MenuDesign.UNDERLINE.getCssClass());
		assertEquals(LookPart.MENU_LIST_BAR, MenuDesign.BAR.getListPart());
		assertEquals(LookPart.MENU_VERTICAL, MenuOrientation.VERTICAL.getPart());
		assertEquals("vertical", MenuOrientation.VERTICAL.cssName());
		menu.setDesign(null).setOrientation(null);
		assertEquals(MenuDesign.BAR, menu.getDesign());
		assertEquals(MenuOrientation.HORIZONTAL, menu.getOrientation());
		assertFalse(menu.isInline(), "only down the side");
	}

	@Test
	void callsTheServerViaAjaxOrWithALinkClosingNothingItself()
	{
		List<String> calls = new ArrayList<>();
		tester.startPage(new TestPage(new Menu("menu", items(calls))));

		tester.executeAjaxEvent(bar(0) + ":link", "click");
		// the same ids each time the menu renders
		String id = tester.getComponentFromLastRenderedPage(bar(0) + ":link").getMarkupId();
		tester.startPage(tester.getLastRenderedPage());
		assertEquals(id, tester.getComponentFromLastRenderedPage(bar(0) + ":link").getMarkupId());
		tester.executeAjaxEvent(sub(sub(bar(1), 1), 0) + ":link", "click");
		tester.clickLink(sub(bar(1), 3) + ":link");
		assertEquals(List.of("save", "pdf", "reset"), calls);
	}

	@Test
	void leadsToBookmarkablePagesMarkingTheCurrentOneAndThoseHoldingIt()
	{
		tester.startPage(CurrentPage.class, new PageParameters().add("section", "laptops"));
		String html = tester.getLastResponseAsString();

		TagTester home = tagAt(bar(0) + ":link");
		assertTrue(home.getAttribute("href").contains("CurrentPage"), home.getAttribute("href"));
		assertNull(home.getAttribute("aria-current"));
		assertEquals("antilia-menu-link", home.getAttribute("class"));
		TagTester products = tagAt(bar(1) + ":trigger");
		assertEquals("antilia-menu-link antilia-menu-current-parent", products.getAttribute("class"));
		// popping up, it starts closed
		assertEquals("false", products.getAttribute("aria-expanded"));
		TagTester laptops = tagAt(sub(bar(1), 0) + ":link");
		assertEquals("page", laptops.getAttribute("aria-current"));
		assertEquals("antilia-menu-submenu-link antilia-menu-current", laptops.getAttribute("class"));
		assertTrue(laptops.getAttribute("href").contains("section=laptops"),
			laptops.getAttribute("href"));
		assertNull(tagAt(sub(bar(1), 1) + ":link").getAttribute("aria-current"));
		assertTrue(html.contains("aria-current=\"page\""));

		tester.startPage(CurrentPage.class);
		assertEquals("page", tagAt(bar(0) + ":link").getAttribute("aria-current"));
		assertEquals("antilia-menu-link", tagAt(bar(1) + ":trigger").getAttribute("class"));
	}

	@Test
	void marksTheItemsItsModelsSaySelected()
	{
		Model<Boolean> second = Model.of(false);
		MenuItem one = LinkMenuItem.of(Model.of("One"), () -> {}).setSelected(true);
		SubMenu more = new SubMenu(Model.of("More")).add(LinkMenuItem.of(Model.of("Two"), () -> {})
			.setSelected(second));
		Menu menu = new Menu("menu", List.of(one, more));
		tester.startPage(new TestPage(menu));

		assertEquals("page", tagAt(bar(0) + ":link").getAttribute("aria-current"));
		assertEquals("antilia-menu-link", tagAt(bar(1) + ":trigger").getAttribute("class"));
		assertNull(tagAt(sub(bar(1), 0) + ":link").getAttribute("aria-current"));

		one.setSelected(false);
		second.setObject(true);
		tester.startPage(new TestPage(menu));

		assertNull(tagAt(bar(0) + ":link").getAttribute("aria-current"));
		assertEquals("antilia-menu-link antilia-menu-current-parent",
			tagAt(bar(1) + ":trigger").getAttribute("class"));
		assertEquals("page", tagAt(sub(bar(1), 0) + ":link").getAttribute("aria-current"));
		assertFalse(MenuSeparator.INSTANCE.isSelected());
	}

	@Test
	void opensTheSubmenusHoldingTheCurrentItemInPlaceDownTheSide()
	{
		CurrentPage.inline = true;
		try
		{
			tester.startPage(CurrentPage.class, new PageParameters().add("section", "laptops"));
			assertEquals("true", tagAt(bar(1) + ":trigger").getAttribute("aria-expanded"));
			assertNull(tagAt(bar(1) + ":list").getAttribute("hidden"));
			assertEquals("false", tagAt(bar(2) + ":trigger").getAttribute("aria-expanded"));
			assertEquals("hidden", tagAt(bar(2) + ":list").getAttribute("hidden"));
		}
		finally
		{
			CurrentPage.inline = false;
		}
	}

	@Test
	void showsDisabledItemsThatDoNothing()
	{
		List<String> calls = new ArrayList<>();
		MenuItem save = AjaxMenuItem.of(Model.of("Save"), target -> calls.add("save"))
			.setEnabled(false);
		SubMenu file = new SubMenu(Model.of("File")).add(LinkMenuItem.of(Model.of("Reset"),
			() -> calls.add("reset")).setEnabled(false)).setEnabled(false);
		tester.startPage(new TestPage(new Menu("menu", List.of(save, file))));

		TagTester link = tagAt(bar(0) + ":link");
		assertEquals("true", link.getAttribute("aria-disabled"));
		assertNull(link.getAttribute("href"));
		assertEquals("true", tagAt(bar(1) + ":trigger").getAttribute("aria-disabled"));
		assertEquals("true", tagAt(sub(bar(1), 0) + ":link").getAttribute("aria-disabled"));
		assertFalse(save.isEnabled());
	}

	@Test
	void readsTheIconAndTheFlagsOfItsItemsEachTimeItRenders()
	{
		Model<IIcon> icon = Model.of(ICON);
		Model<Boolean> enabled = Model.of(true);
		Model<Boolean> visible = Model.of(true);
		MenuItem save = AjaxMenuItem.of(Model.of("Save"), target -> {}).setIcon(icon)
			.setEnabled(enabled);
		MenuItem help = LinkMenuItem.of(Model.of("Help"), () -> {}).setVisible(visible);
		Menu menu = new Menu("menu", List.of(save, help));
		tester.startPage(new TestPage(menu));

		TagTester link = tagAt(bar(0) + ":link");
		assertTrue(link.getValue().contains("<svg class=\"icon\"></svg>"));
		assertNull(link.getAttribute("aria-disabled"));
		tester.assertContains(">Help<");

		icon.setObject(() -> "<svg class=\"other\"></svg>");
		enabled.setObject(false);
		visible.setObject(false);
		tester.startPage(new TestPage(menu));

		link = tagAt(bar(0) + ":link");
		assertTrue(link.getValue().contains("<svg class=\"other\"></svg>"));
		assertEquals("true", link.getAttribute("aria-disabled"));
		assertFalse(tester.getLastResponseAsString().contains(">Help<"));
		assertFalse(save.isEnabled());
		assertFalse(help.isVisible());

		icon.setObject(null);
		tester.startPage(new TestPage(menu));

		assertFalse(tagAt(bar(0) + ":link").getValue().contains("<svg"));
	}

	@Test
	void leavesOutTheItemsThatDoNotShowAndTheSubmenusNoneOfWhoseItemsDo()
	{
		SubMenu empty = new SubMenu(Model.of("Empty")).add(MenuSeparator.INSTANCE,
			LinkMenuItem.of(Model.of("Gone"), () -> {}).setVisible(false));
		IMenuItem hidden = LinkMenuItem.of(Model.of("Hidden"), () -> {}).setVisible(false);
		IMenuItem shown = LinkMenuItem.of(Model.of("Shown"), () -> {});
		tester.startPage(new TestPage(new Menu("menu", List.of(empty, hidden, shown))));
		String html = tester.getLastResponseAsString();

		assertFalse(html.contains("Empty"));
		assertFalse(html.contains("Gone"));
		assertFalse(html.contains("Hidden"));
		assertTrue(html.contains("Shown"));
		assertEquals(1, html.split("role=\"menuitem\"", -1).length - 1);
		assertFalse(empty.setVisible(false).isVisible());
	}

	@Test
	void takesItsItemsFromAModelOrASubmenu()
	{
		ListModel<IMenuItem> model = new ListModel<>(new ArrayList<>());
		model.getObject().add(LinkMenuItem.of(Model.of("One"), () -> {}));
		Menu menu = new Menu("menu", model);
		tester.startPage(new TestPage(menu));
		tester.assertContains(">One<");
		model.getObject().add(LinkMenuItem.of(Model.of("Two"), () -> {}));
		tester.startPage(new TestPage(menu));
		tester.assertContains(">Two<");
		assertEquals(2, menu.getItems().size());

		SubMenu main = new SubMenu(Model.of("Main menu")).add(LinkMenuItem.of(Model.of("Three"),
			() -> {}));
		tester.startPage(new TestPage(new Menu("menu", main)));
		tester.assertContains(">Three<");
		assertEquals("Main menu", tester.getTagByWicketId("bar").getAttribute("aria-label"));
		assertSame(main.getItems().get(0), main.getItems().get(0));
		assertThrows(UnsupportedOperationException.class, () -> main.getItems().clear());
	}

	@Test
	void detachesItsItemsAndTheirModels()
	{
		CountingModel label = new CountingModel("Counted");
		CountingModel tooltip = new CountingModel("Tip");
		SubMenu file = new SubMenu(Model.of("File")).add(LinkMenuItem.of(label, () -> {}));
		file.setTooltip(tooltip);
		tester.startPage(new TestPage(new Menu("menu", List.of(file))));

		assertTrue(label.detached > 0);
		assertTrue(tooltip.detached > 0);
	}

	@Test
	void wantsALinkFromAnItemOfMenuLinkPanel()
	{
		assertThrows(IllegalArgumentException.class, () -> new MenuLinkPanel("link",
			MenuSeparator.INSTANCE));
		assertNull(MenuSeparator.INSTANCE.getLabel());
		assertNull(new SubMenu(Model.of("File")).getLink("link"));
		MenuLinkPanel panel = new MenuLinkPanel("link", new ExternalItem(Model.of("Help"), "/help"));
		assertTrue(panel.getLink() instanceof ExternalLink);
		assertEquals("Help", panel.getItem().getLabel().getObject());
		assertThrows(IllegalArgumentException.class, () -> new MenuLinkPanel("link",
			new MenuItem(Model.of("Text"))
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Component getLink(String id)
				{
					return new Label(id, "not a container");
				}
			}));
	}

	@Test
	void comparesTheParametersOfABookmarkableItemWithThoseOfThePage()
	{
		BookmarkableMenuItem item = new BookmarkableMenuItem(Model.of("Laptops"), CurrentPage.class,
			new PageParameters().add("section", "laptops"));
		assertEquals(CurrentPage.class, item.getPageClass());
		assertEquals("laptops", item.getParameters().get("section").toString());
		// no page is requested
		assertFalse(item.isSelected());
		assertTrue(item.setSelected(true).isSelected());
	}

	/** A model counting the times it is detached. */
	private static class CountingModel extends Model<String>
	{
		private static final long serialVersionUID = 1L;

		int detached;

		CountingModel(String text)
		{
			super(text);
		}

		@Override
		public void detach()
		{
			detached++;
			super.detach();
		}
	}

	/** An item of one's own: an external link. */
	private static class ExternalItem extends MenuItem
	{
		private static final long serialVersionUID = 1L;

		private final String url;

		ExternalItem(IModel<String> label, String url)
		{
			super(label);
			this.url = url;
		}

		@Override
		public AbstractLink getLink(String id)
		{
			return new ExternalLink(id, url);
		}
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(Menu menu)
		{
			add(menu);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<nav wicket:id=\"menu\"></nav></body></html>");
		}
	}

	/**
	 * A page with a menu leading to it: Home, Products (Laptops, Phones) and More (About), by the
	 * parameter section.
	 */
	public static class CurrentPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		/** Whether the menu goes down the side, opening its submenus in place. */
		static boolean inline;

		public CurrentPage(PageParameters parameters)
		{
			super(parameters);
			add(new Menu("menu", List.of(
				new BookmarkableMenuItem(Model.of("Home"), CurrentPage.class),
				new SubMenu(Model.of("Products")).add(section("Laptops"), section("Phones")),
				new SubMenu(Model.of("More")).add(section("About"))))
				.setOrientation(inline ? MenuOrientation.VERTICAL : MenuOrientation.HORIZONTAL)
				.setInline(inline));
		}

		private static IMenuItem section(String name)
		{
			return new BookmarkableMenuItem(Model.of(name), CurrentPage.class,
				new PageParameters().add("section", name.toLowerCase()));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<nav wicket:id=\"menu\"></nav></body></html>");
		}
	}
}
