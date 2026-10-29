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
package com.antiliacode.widgets.menu;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;
import com.github.openjson.JSONObject;

/**
 * A menu: a bar of {@link IMenuItem items}, {@link #setOrientation(MenuOrientation) along the top of
 * a page or down its side}, each leading somewhere or doing something by its link, a
 * {@link BookmarkableMenuItem bookmarkable}, an {@link AjaxMenuItem Ajax} or a plain
 * {@link LinkMenuItem} one, or opening a submenu of more items, an {@link IMenu} such as a
 * {@link SubMenu}, nested as deep as one likes. Items may have an icon before their label and a
 * tooltip, and {@link MenuSeparator separators} go between them. The menu renders each item with a
 * {@link MenuLinkPanel}, marks the selected item as current ({@link IMenuItem#getSelected()}),
 * and those whose submenus hold it, and leaves out the items that do not show and the submenus none
 * of whose items do.
 * <p>
 * A WAI-ARIA menu bar, which {@code Wicket.Menu} of {@link #JS} drives in the browser: a click opens
 * a submenu, and while one of the bar is open the pointer opens the others; in a submenu the pointer
 * resting on an item opens its submenu. The keys move along the bar and through the submenus,
 * Escape closes them, a letter goes to the next item starting with it. The submenus pop up below
 * the items of a bar along the top and beside the others, on the side with room; those of a bar down
 * the side may {@link #setInline(boolean) open in place} instead, as in the side bar of an
 * application, the submenus holding the selected item open from the start. The submenus of the bar
 * may {@link #setOpenOnHover(boolean) open as the pointer rests on their items} too, as those of
 * the menus of web sites do.
 * <p>
 * The design is one of {@link MenuDesign}, in the colors of the
 * {@link com.antiliacode.widgets.theme.Theme theme} the menu is in and with the classes of its
 * {@link com.antiliacode.widgets.theme.ILook look}: in Bootstrap's, the bar is a nav, its submenus
 * dropdown-menus. The items are those of the model, read each time the menu renders:
 *
 * <pre>
 * add(new Menu("menu", List.of(
 * 	new BookmarkableMenuItem(Model.of("Home"), HomePage.class).setIcon(SvgIcon.HOUSE),
 * 	new SubMenu(Model.of("File")).add(
 * 		AjaxMenuItem.of(Model.of("Save"), target -&gt; save(target)),
 * 		MenuSeparator.INSTANCE,
 * 		LinkMenuItem.of(Model.of("Sign out"), () -&gt; signOut())))));
 * </pre>
 *
 * @since 11.0.0
 */
public class Menu extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The script of the menus, {@code Wicket.Menu}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Menu.class,
		"antilia-menu.js");

	/** The style sheet of the menus, with the designs of {@link MenuDesign}. */
	public static final ResourceReference CSS = new CssResourceReference(Menu.class,
		"antilia-menu.css");

	private final WebMarkupContainer element;

	private MenuOrientation orientation = MenuOrientation.HORIZONTAL;

	private MenuDesign design = MenuDesign.BAR;

	private boolean inline;

	private boolean openOnHover;

	private IModel<String> label;

	/**
	 * @param id
	 *            component id
	 * @param items
	 *            the items of the bar, copied
	 */
	public Menu(String id, List<? extends IMenuItem> items)
	{
		this(id, new ListModel<IMenuItem>(new ArrayList<>(items)));
	}

	/**
	 * A menu of the items of a submenu, whose label is the accessible name of the bar.
	 *
	 * @param id
	 *            component id
	 * @param menu
	 *            the menu, its items those of the bar
	 */
	public Menu(String id, IMenu menu)
	{
		this(id, menuItems(menu));
		setLabel(menu.getLabel());
	}

	/**
	 * @param id
	 *            component id
	 * @param items
	 *            the items of the bar, read each time the menu renders
	 */
	public Menu(String id, IModel<? extends List<? extends IMenuItem>> items)
	{
		super(id, items);

		element = new WebMarkupContainer("element");
		element.setOutputMarkupId(true);
		element.add(new LookBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected LookPart[] getParts(Component component)
			{
				return new LookPart[] { LookPart.MENU, orientation.getPart(), design.getPart(),
					isInline() ? LookPart.MENU_INLINE : null };
			}
		});
		add(element);

		WebMarkupContainer bar = new WebMarkupContainer("bar");
		bar.add(AttributeModifier.replace("role", "menubar"));
		bar.add(AttributeModifier.replace("aria-orientation", () -> orientation.cssName()));
		bar.add(AttributeModifier.replace("aria-label", () -> label != null ? label.getObject() :
			null));
		bar.add(new LookBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected LookPart[] getParts(Component component)
			{
				return new LookPart[] { LookPart.MENU_LIST, design.getListPart() };
			}
		});
		element.add(bar);
		bar.add(newItems("items", items, 0, ""));
	}

	private static IModel<List<? extends IMenuItem>> menuItems(IMenu menu)
	{
		return menu::getItems;
	}

	/**
	 * @return the items of a list, those of the bar at depth 0, those of a submenu below; the path
	 *         of the list is where it is, the places of the items holding it
	 */
	@SuppressWarnings("unchecked")
	private ListView<IMenuItem> newItems(String id, IModel<? extends List<? extends IMenuItem>> items,
		int depth, String path)
	{
		// the list is only read
		return new ListView<>(id, (IModel<? extends List<IMenuItem>>)(IModel<?>)items)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<IMenuItem> listItem)
			{
				populate(listItem, listItem.getModelObject(), depth,
					path + "-" + listItem.getIndex());
			}
		};
	}

	/**
	 * Fills the element of an item: its link, its submenu or nothing for a separator.
	 *
	 * @param path
	 *            where the item is, the places of the items holding it and its own
	 */
	private void populate(ListItem<IMenuItem> listItem, IMenuItem item, int depth, String path)
	{
		boolean top = depth == 0;
		if (item instanceof MenuSeparator)
		{
			listItem.add(new WebMarkupContainer("item").setVisible(false));
			listItem.add(new LookBehavior(LookPart.MENU_SEPARATOR));
			listItem.add(AttributeModifier.replace("role", "separator"));
			listItem.add(AttributeModifier.replace("aria-orientation", () -> top &&
				orientation == MenuOrientation.HORIZONTAL ? "vertical" : "horizontal"));
			return;
		}
		listItem.add(new LookBehavior(top ? LookPart.MENU_ITEM : LookPart.MENU_SUBMENU_ITEM));
		listItem.add(AttributeModifier.replace("role", "none"));
		listItem.add(new Behavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onConfigure(Component component)
			{
				component.setVisible(isShown(item));
			}
		});
		listItem.add(item instanceof IMenu submenu ? newSubmenu("item", submenu, depth, path) :
			newLink("item", item, top, path));
	}

	/**
	 * @return the markup id of an element of the menu, the same each time the menu renders, so
	 *         that the item with the focus gets it back when Ajax renders the menu again
	 */
	private String markupIdOf(String kind, String path)
	{
		return element.getMarkupId() + "-" + kind + path;
	}

	/**
	 * @return the link of an item, as {@link MenuLinkPanel} renders it, an item of the menu
	 */
	private Component newLink(String id, IMenuItem item, boolean top, String path)
	{
		MenuLinkPanel panel = new MenuLinkPanel(id, item);
		Component link = panel.getLink();
		link.setMarkupId(markupIdOf("item", path));
		link.add(new LookBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected LookPart[] getParts(Component component)
			{
				return new LookPart[] { top ? LookPart.MENU_LINK : LookPart.MENU_SUBMENU_LINK,
					item.isSelected() ? LookPart.MENU_CURRENT : null };
			}
		});
		link.add(new Behavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onComponentTag(Component component, ComponentTag tag)
			{
				tag.put("role", "menuitem");
				if (item.isSelected())
				{
					tag.put("aria-current", "page");
				}
				if (!component.isEnabledInHierarchy())
				{
					tag.put("aria-disabled", "true");
				}
			}
		});
		return panel;
	}

	/**
	 * @return the item opening a submenu, and the submenu
	 */
	private Component newSubmenu(String id, IMenu submenu, int depth, String path)
	{
		Fragment fragment = new Fragment(id, "submenu", this);
		WebMarkupContainer trigger = new WebMarkupContainer("trigger")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				tag.put("role", "menuitem");
				tag.put("aria-haspopup", "menu");
				tag.put("aria-controls", fragment.get("list").getMarkupId());
				tag.put("aria-expanded", String.valueOf(isExpanded(submenu)));
				if (!submenu.isEnabled())
				{
					tag.put("aria-disabled", "true");
				}
			}
		};
		trigger.setMarkupId(markupIdOf("item", path));
		trigger.add(new LookBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected LookPart[] getParts(Component component)
			{
				return new LookPart[] { depth == 0 ? LookPart.MENU_LINK : LookPart.MENU_SUBMENU_LINK,
					holdsSelected(submenu) ? LookPart.MENU_CURRENT_PARENT : null };
			}
		});
		IModel<String> tooltip = submenu.getTooltip();
		if (tooltip != null)
		{
			trigger.add(AttributeModifier.replace("title", MenuLinkPanel.assigned(tooltip, trigger)));
		}
		trigger.add(MenuLinkPanel.newIcon("icon", submenu));
		trigger.add(MenuLinkPanel.newLabel("label", submenu));
		fragment.add(trigger);

		WebMarkupContainer list = new WebMarkupContainer("list")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				tag.put("role", "menu");
				tag.put("aria-labelledby", trigger.getMarkupId());
				if (!isExpanded(submenu))
				{
					tag.put("hidden", "hidden");
				}
			}
		};
		list.setMarkupId(markupIdOf("menu", path));
		list.add(new LookBehavior(LookPart.MENU_SUBMENU));
		list.add(newItems("items", menuItems(submenu), depth + 1, path));
		fragment.add(list);
		return fragment;
	}

	/**
	 * @return whether the menu shows an item: it says so, and, for a submenu, one of its items
	 *         shows
	 */
	private static boolean isShown(IMenuItem item)
	{
		return item.isVisible() && (!(item instanceof IMenu submenu) || submenu.getItems().stream()
			.anyMatch(each -> !(each instanceof MenuSeparator) && isShown(each)));
	}

	/**
	 * @return whether an item is selected, or holds one that is in its submenus
	 */
	private static boolean holdsSelected(IMenuItem item)
	{
		return isShown(item) && (item.isSelected() || item instanceof IMenu submenu &&
			submenu.getItems().stream().anyMatch(Menu::holdsSelected));
	}

	/**
	 * @return whether a submenu is open from the start: one holding the selected item, open in
	 *         place
	 */
	private boolean isExpanded(IMenu submenu)
	{
		return isInline() && submenu.isEnabled() && holdsSelected(submenu);
	}

	/**
	 * @return the items of the bar
	 */
	@SuppressWarnings("unchecked")
	public List<? extends IMenuItem> getItems()
	{
		List<? extends IMenuItem> items = (List<? extends IMenuItem>)getDefaultModelObject();
		return items != null ? items : List.of();
	}

	/**
	 * @return where the bar is, {@link MenuOrientation#HORIZONTAL} by default
	 */
	public MenuOrientation getOrientation()
	{
		return orientation;
	}

	/**
	 * @param orientation
	 *            where the bar is: along the top or down the side
	 * @return {@code this}
	 */
	public Menu setOrientation(MenuOrientation orientation)
	{
		this.orientation = orientation == null ? MenuOrientation.HORIZONTAL : orientation;
		return this;
	}

	/**
	 * @return the design of the menu, {@link MenuDesign#BAR} by default
	 */
	public MenuDesign getDesign()
	{
		return design;
	}

	/**
	 * @param design
	 *            the design of the menu
	 * @return {@code this}
	 */
	public Menu setDesign(MenuDesign design)
	{
		this.design = design == null ? MenuDesign.BAR : design;
		return this;
	}

	/**
	 * @return whether the submenus open in place: those of a bar down the side asked to
	 */
	public boolean isInline()
	{
		return inline && orientation == MenuOrientation.VERTICAL;
	}

	/**
	 * Whether the submenus of a bar down the side open in place, pushing the items after them down,
	 * as in the side bar of an application, rather than popping up beside their items. The
	 * submenus of a bar along the top pop up always.
	 *
	 * @param inline
	 *            whether the submenus open in place
	 * @return {@code this}
	 */
	public Menu setInline(boolean inline)
	{
		this.inline = inline;
		return this;
	}

	/**
	 * @return whether the submenus of the bar open as the pointer rests on their items,
	 *         {@code false} by default
	 */
	public boolean isOpenOnHover()
	{
		return openOnHover;
	}

	/**
	 * Whether the submenus of the bar open as the pointer rests on their items, not only on a
	 * click, and close as it leaves the menu. Those of submenus open so always.
	 *
	 * @param openOnHover
	 *            whether the submenus of the bar open on hover
	 * @return {@code this}
	 */
	public Menu setOpenOnHover(boolean openOnHover)
	{
		this.openOnHover = openOnHover;
		return this;
	}

	/**
	 * @param label
	 *            the accessible name of the bar, {@code null} for none
	 * @return {@code this}
	 */
	public Menu setLabel(IModel<String> label)
	{
		this.label = MenuLinkPanel.assigned(label, this);
		return this;
	}

	/**
	 * @return the markup id of the element of the menu, which the script works on
	 */
	public String getMenuMarkupId()
	{
		return element.getMarkupId();
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderHeadItems(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Menu.attach(" +
			ScriptJson.quote(element.getMarkupId()) + ", " + getOptions() + ");"));
	}

	/**
	 * @return the options of {@code Wicket.Menu.attach}, as a JavaScript object
	 */
	private String getOptions()
	{
		JSONObject options = new JSONObject()
			.put("orientation", orientation.cssName())
			.put("design", design.cssName())
			.put("inline", isInline())
			.put("openOnHover", openOnHover);
		String text = label != null ? label.getObject() : null;
		if (!Strings.isEmpty(text))
		{
			options.put("label", text);
		}
		return ScriptJson.of(options);
	}

	@Override
	protected void onDetach()
	{
		if (label != null)
		{
			label.detach();
		}
		// the items of a model already detached would load again
		if (!(getDefaultModel() instanceof LoadableDetachableModel<?> loadable) ||
			loadable.isAttached())
		{
			getItems().forEach(IMenuItem::detach);
		}
		super.onDetach();
	}

	/**
	 * Renders what the menus need in the browser: {@link #JS} and {@link #CSS}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
