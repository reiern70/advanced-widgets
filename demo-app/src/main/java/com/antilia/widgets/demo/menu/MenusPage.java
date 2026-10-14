/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antilia.widgets.demo.DemoCases;
import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.HomePage;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.antilia.widgets.dropdown.Dropdown;
import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.icon.SvgIcon;
import com.antilia.widgets.menu.AjaxMenuItem;
import com.antilia.widgets.menu.BookmarkableMenuItem;
import com.antilia.widgets.menu.IMenuItem;
import com.antilia.widgets.menu.LinkMenuItem;
import com.antilia.widgets.menu.Menu;
import com.antilia.widgets.menu.MenuDesign;
import com.antilia.widgets.menu.MenuItem;
import com.antilia.widgets.menu.MenuOrientation;
import com.antilia.widgets.menu.MenuSeparator;
import com.antilia.widgets.menu.SubMenu;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link Menu}: the menu bar of an application, its items calling the server via Ajax, one with a
 * request of its own and one leading to the home page, some disabled, with icons, tooltips and
 * submenus in submenus; the navigation of a site, its items bookmarkable pages, this one with the
 * parameter {@value #SECTION}, the one shown marked as current, its submenus opening on hover; the
 * side bar of an application, its submenus opening in place, the server marking the item chosen
 * via Ajax as current; a menu whose orientation, design and ways of opening its submenus controls
 * switch; and a gallery of the designs, along the top and down the side. The React and Svelte
 * menus show the same in those frameworks, their links leading to anchors of the page.
 */
public class MenusPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The parameter naming the section of the site the navigation leads to. */
	public static final String SECTION = "section";

	/** The separator of the labels of an item and of the items holding it, in what the page says. */
	static final String PATH_SEPARATOR = " › ";

	/** The menu bar of an application: its items call the server via Ajax unless told otherwise. */
	static final List<Entry> APPLICATION = List.of(
		Entry.menu("file", SvgIcon.FOLDER_OPEN,
			Entry.item("new", SvgIcon.FILE),
			Entry.menu("recent", null, Entry.item("notes", SvgIcon.FILE_LINES),
				Entry.item("todo", SvgIcon.FILE_LINES), Entry.item("report", SvgIcon.FILE_LINES)),
			Entry.SEPARATOR,
			Entry.item("save", SvgIcon.FLOPPY_DISK),
			Entry.menu("export", SvgIcon.FILE_EXPORT, Entry.item("pdf", SvgIcon.FILE_PDF),
				Entry.item("csv", SvgIcon.FILE_CSV)),
			Entry.SEPARATOR,
			Entry.link("startAgain", SvgIcon.ARROWS_ROTATE)),
		Entry.menu("edit", null,
			Entry.item("undo", SvgIcon.ROTATE_LEFT),
			Entry.item("redo", SvgIcon.ROTATE_RIGHT).disable(),
			Entry.SEPARATOR,
			Entry.item("cut", SvgIcon.SCISSORS),
			Entry.item("copy", SvgIcon.COPY),
			Entry.item("paste", SvgIcon.PASTE).disable()),
		Entry.menu("view", null,
			Entry.item("zoomIn", SvgIcon.MAGNIFYING_GLASS_PLUS),
			Entry.item("zoomOut", SvgIcon.MAGNIFYING_GLASS_MINUS)),
		Entry.menu("help", SvgIcon.CIRCLE_QUESTION,
			Entry.item("about", SvgIcon.CIRCLE_INFO),
			Entry.SEPARATOR,
			Entry.page("homePage", SvgIcon.HOUSE)));

	/** The navigation of a site: each item leads to this page, with the parameter of its section. */
	static final List<Entry> NAVIGATION = List.of(
		Entry.page("welcome", SvgIcon.HOUSE),
		Entry.menu("products", SvgIcon.STORE,
			Entry.page("laptops", SvgIcon.LAPTOP),
			Entry.page("phones", SvgIcon.MOBILE_SCREEN),
			Entry.menu("accessories", SvgIcon.PLUG, Entry.page("chargers", null),
				Entry.page("covers", null))),
		Entry.menu("services", SvgIcon.HEADSET,
			Entry.page("support", SvgIcon.HEADSET),
			Entry.page("training", SvgIcon.GRADUATION_CAP)),
		Entry.page("company", SvgIcon.CIRCLE_INFO));

	/** The section of the navigation a page without the parameter is. */
	static final String FIRST_SECTION = "welcome";

	/** The side bar of an application: its items show their views, chosen via Ajax. */
	static final List<Entry> SIDEBAR = List.of(
		Entry.item("dashboard", SvgIcon.GAUGE),
		Entry.menu("reports", SvgIcon.CHART_PIE,
			Entry.item("sales", SvgIcon.CHART_LINE),
			Entry.item("inventory", SvgIcon.BOX_ARCHIVE),
			Entry.menu("archive", SvgIcon.CALENDAR, Entry.item("lastYear", null),
				Entry.item("thisYear", null))),
		Entry.menu("people", SvgIcon.USERS,
			Entry.item("members", SvgIcon.USER),
			Entry.item("roles", SvgIcon.USER_SHIELD)),
		Entry.item("preferences", SvgIcon.GEAR));

	/** The view of the side bar shown first. */
	static final String FIRST_VIEW = "sales";

	/** The designs the gallery shows, along the top and down the side. */
	static final List<MenuDesign> DESIGNS = Arrays.asList(MenuDesign.values());

	private final Label applicationStatus;

	private final Label settingsStatus;

	private final WebMarkupContainer content;

	private final Menu sidebar;

	private String applicationMessage;

	private String settingsMessage;

	private String view = FIRST_VIEW;

	private boolean vertical;

	private MenuDesign design = MenuDesign.PILLS;

	private boolean inline;

	private boolean hover;

	/**
	 * @param parameters
	 *            the section of the site the navigation is at, {@value #SECTION}
	 */
	public MenusPage(PageParameters parameters)
	{
		super(parameters);
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		DemoCases cases = new DemoCases("cases", this);
		themed.add(cases);
		Fragment applicationCase = cases.addCase("applicationTitle", "applicationCase");
		Fragment navigationCase = cases.addCase("navigationTitle", "navigationCase");
		Fragment sidebarCase = cases.addCase("sidebarTitle", "sidebarCase");
		Fragment settingsCase = cases.addCase("settingsTitle", "settingsCase");
		Fragment galleryCase = cases.addCase("galleryTitle", "galleryCase");
		// a section of the site: the page the navigation led to
		if (!parameters.get(SECTION).isEmpty())
		{
			cases.setModelObject("navigationTitle");
		}

		applicationStatus = new Label("applicationStatus", () -> applicationMessage != null ?
			applicationMessage : getString("applicationStart"));
		applicationStatus.setOutputMarkupId(true);
		applicationCase.add(applicationStatus);
		applicationCase.add(new Menu("application", items(APPLICATION, this::applicationItem))
			.setLabel(new ResourceModel("application")));

		navigationCase.add(new Menu("navigation", items(NAVIGATION, this::navigationItem))
			.setDesign(MenuDesign.UNDERLINE).setOpenOnHover(true)
			.setLabel(new ResourceModel("navigation")));
		navigationCase.add(new Label("where", this::describeSection));
		navigationCase.add(new Label("address", () -> getString("address")
			.replace("{0}", section())));

		content = new WebMarkupContainer("content");
		content.setOutputMarkupId(true);
		content.add(new Label("heading", () -> path(pathOf(SIDEBAR, view))));
		sidebarCase.add(content);
		sidebar = new Menu("sidebar", items(SIDEBAR, (entry, label, path) ->
			new ViewItem(label, entry.name(), true)));
		sidebar.setOrientation(MenuOrientation.VERTICAL).setDesign(MenuDesign.SIDEBAR)
			.setInline(true).setLabel(new ResourceModel("sidebar")).setOutputMarkupId(true);
		sidebarCase.add(sidebar);

		settingsStatus = new Label("settingsStatus", () -> settingsMessage != null ?
			settingsMessage : getString("settingsStart"));
		settingsStatus.setOutputMarkupId(true);
		settingsCase.add(settingsStatus);
		WebMarkupContainer preview = new WebMarkupContainer("preview");
		preview.setOutputMarkupId(true);
		settingsCase.add(preview);
		preview.add(new Menu("settings", items(APPLICATION, this::settingsItem))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setOrientation(vertical ? MenuOrientation.VERTICAL : MenuOrientation.HORIZONTAL);
				setDesign(design).setInline(inline).setOpenOnHover(hover);
			}
		}.setLabel(new ResourceModel("application")));
		Form<Void> form = new Form<>("form");
		settingsCase.add(form);
		form.add(new CheckBox("vertical", new PropertyModel<>(this, "vertical"))
			.add(rerender(preview)));
		form.add(new Dropdown<>("design", new PropertyModel<>(this, "design"), DESIGNS,
			new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(MenuDesign shown)
				{
					return getString("design." + shown.name());
				}

				@Override
				public String getIdValue(MenuDesign shown, int index)
				{
					return shown.name();
				}
			}).setRequired(true).add(rerender(preview)));
		form.add(new CheckBox("inline", new PropertyModel<>(this, "inline")).add(rerender(preview)));
		form.add(new CheckBox("hover", new PropertyModel<>(this, "hover")).add(rerender(preview)));

		galleryCase.add(gallery("topDesigns", MenuOrientation.HORIZONTAL));
		galleryCase.add(gallery("sideDesigns", MenuOrientation.VERTICAL));
	}

	/**
	 * @return the menus of the gallery in each design, under its name
	 */
	private ListView<MenuDesign> gallery(String id, MenuOrientation orientation)
	{
		return new ListView<>(id, DESIGNS)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<MenuDesign> item)
			{
				MenuDesign shown = item.getModelObject();
				String name = getString("design." + shown.name());
				item.add(new Label("name", name));
				item.add(new Menu("menu", items(SIDEBAR, (entry, label, path) ->
					new ViewItem(label, entry.name(), false)
					{
						private static final long serialVersionUID = 1L;

						@Override
						public boolean isCurrent(Page page)
						{
							return "dashboard".equals(entry.name());
						}
					})).setDesign(shown).setOrientation(orientation).setLabel(Model.of(name)));
			}
		};
	}

	private static AjaxFormComponentUpdatingBehavior rerender(Component component)
	{
		return new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(component);
			}
		};
	}

	/**
	 * @return the items of a menu of entries, those of submenus made here, the others by a leaf
	 */
	private List<IMenuItem> items(List<Entry> entries, Leaf leaf)
	{
		return entries.stream().map(entry -> item(entry, List.of(), leaf)).toList();
	}

	private IMenuItem item(Entry entry, List<String> path, Leaf leaf)
	{
		if (entry.isSeparator())
		{
			return MenuSeparator.INSTANCE;
		}
		List<String> here = new ArrayList<>(path);
		here.add(entry.name());
		IModel<String> label = new ResourceModel(entry.name());
		MenuItem item = entry.items().isEmpty() ? leaf.create(entry, label, here) :
			new SubMenu(label).add(entry.items().stream().map(each -> item(each, here, leaf))
				.toArray(IMenuItem[]::new));
		item.setIcon(entry.icon()).setEnabled(!entry.disabled());
		if (hasTooltip(entry.name()))
		{
			item.setTooltip(new ResourceModel(entry.name() + ".tooltip"));
		}
		return item;
	}

	private boolean hasTooltip(String name)
	{
		return !getString(name + ".tooltip", null, "").isEmpty();
	}

	/**
	 * @return an item of the application: via Ajax, with a link, or to the home page
	 */
	private MenuItem applicationItem(Entry entry, IModel<String> label, List<String> path)
	{
		return switch (entry.kind())
		{
			case LINK -> LinkMenuItem.of(label, () -> applicationMessage = getString("linked")
				.replace("{0}", path(path)));
			case PAGE -> new BookmarkableMenuItem(label, HomePage.class);
			default -> AjaxMenuItem.of(label, target -> {
				applicationMessage = getString("chose").replace("{0}", path(path));
				target.add(applicationStatus);
			});
		};
	}

	/**
	 * @return an item of the settings, saying what was chosen below them
	 */
	private MenuItem settingsItem(Entry entry, IModel<String> label, List<String> path)
	{
		return switch (entry.kind())
		{
			case LINK -> LinkMenuItem.of(label, () -> settingsMessage = getString("linked")
				.replace("{0}", path(path)));
			case PAGE -> new BookmarkableMenuItem(label, HomePage.class);
			default -> AjaxMenuItem.of(label, target -> {
				settingsMessage = getString("chose").replace("{0}", path(path));
				target.add(settingsStatus);
			});
		};
	}

	/**
	 * @return an item of the navigation: this page, at its section
	 */
	private MenuItem navigationItem(Entry entry, IModel<String> label, List<String> path)
	{
		return new BookmarkableMenuItem(label, MenusPage.class, FIRST_SECTION.equals(entry.name()) ?
			null : new PageParameters().add(SECTION, entry.name()));
	}

	/**
	 * @return the section of the site the page is at, that of the parameter or the first one
	 */
	String section()
	{
		String section = getPageParameters().get(SECTION).toString(FIRST_SECTION);
		return pathOf(NAVIGATION, section).isEmpty() ? FIRST_SECTION : section;
	}

	/**
	 * @return where the navigation is, in words
	 */
	private String describeSection()
	{
		return getString("at").replace("{0}", path(pathOf(NAVIGATION, section())));
	}

	/**
	 * @return the labels of an item and of those holding it, separated
	 */
	String path(List<String> names)
	{
		return names.stream().map(this::getString).collect(Collectors.joining(PATH_SEPARATOR));
	}

	/**
	 * @return the names of an entry and of those holding it, from the bar down; empty when there
	 *         is no entry of the name
	 */
	static List<String> pathOf(List<Entry> entries, String name)
	{
		for (Entry entry : entries)
		{
			if (entry.isSeparator())
			{
				continue;
			}
			if (entry.name().equals(name))
			{
				return List.of(name);
			}
			List<String> inside = pathOf(entry.items(), name);
			if (!inside.isEmpty())
			{
				List<String> path = new ArrayList<>();
				path.add(entry.name());
				path.addAll(inside);
				return path;
			}
		}
		return List.of();
	}

	/**
	 * @return the names of the entries, those of their submenus too
	 */
	static Set<String> namesOf(List<Entry> entries)
	{
		Set<String> names = new LinkedHashSet<>();
		entries.stream().filter(entry -> !entry.isSeparator()).forEach(entry -> {
			names.add(entry.name());
			names.addAll(namesOf(entry.items()));
		});
		return names;
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "menus";
	}

	/**
	 * @return the texts, the menus and their icons, and the settings as the page has them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "cases", "applicationTitle", "application",
			"applicationIntro", "applicationStart", "documentText", "chose", "linked",
			"navigationTitle", "navigation", "navigationIntro", "at", "address", "sidebarTitle",
			"sidebar", "sidebarText", "settingsTitle", "settingsStart", "vertical", "design",
			"inline", "hover", "galleryTitle", "alongTheTop", "downTheSide", "applicationAppIntro",
			"picked", "navigationAppIntro", "routed", "sidebarAppText", "settingsAppStart");
		for (MenuDesign each : MenuDesign.values())
		{
			texts.put("design." + each.name(), getString("design." + each.name()));
		}
		Set<String> names = namesOf(APPLICATION);
		names.addAll(namesOf(NAVIGATION));
		names.addAll(namesOf(SIDEBAR));
		for (String name : names)
		{
			texts.put(name, getString(name));
			if (hasTooltip(name))
			{
				texts.put(name + ".tooltip", getString(name + ".tooltip"));
			}
		}
		return new JSONObject()
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("application", Entry.toJson(APPLICATION))
			.put("navigation", Entry.toJson(NAVIGATION))
			.put("sidebar", Entry.toJson(SIDEBAR))
			.put("homeUrl", urlFor(HomePage.class, null).toString())
			.put("section", FIRST_SECTION)
			.put("view", FIRST_VIEW)
			.put("vertical", vertical)
			.put("design", design.cssName())
			.put("inline", inline)
			.put("hover", hover)
			.put("designs", new JSONArray(DESIGNS.stream().map(MenuDesign::cssName).toList()));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Menu.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(MenusPage.class, "MenusPage.css")));
	}

	public boolean isVertical()
	{
		return vertical;
	}

	public void setVertical(boolean vertical)
	{
		this.vertical = vertical;
	}

	public MenuDesign getDesign()
	{
		return design;
	}

	public void setDesign(MenuDesign design)
	{
		this.design = design;
	}

	public boolean isInline()
	{
		return inline;
	}

	public void setInline(boolean inline)
	{
		this.inline = inline;
	}

	public boolean isHover()
	{
		return hover;
	}

	public void setHover(boolean hover)
	{
		this.hover = hover;
	}

	/**
	 * @return the view of the side bar shown, the name of its item
	 */
	public String getView()
	{
		return view;
	}

	/**
	 * Makes the item of an entry without a submenu.
	 */
	@FunctionalInterface
	interface Leaf extends Serializable
	{
		/**
		 * @param entry
		 *            the entry
		 * @param label
		 *            its label
		 * @param path
		 *            its name and those of the entries holding it, from the bar down
		 * @return its item
		 */
		MenuItem create(Entry entry, IModel<String> label, List<String> path);
	}

	/**
	 * An item of the side bar: it shows its view via Ajax, the server rendering the side bar again
	 * with it as the current item. Those of the gallery do nothing.
	 */
	private class ViewItem extends AjaxMenuItem
	{
		private static final long serialVersionUID = 1L;

		private final String name;

		private final boolean shows;

		ViewItem(IModel<String> label, String name, boolean shows)
		{
			super(label);
			this.name = name;
			this.shows = shows;
		}

		@Override
		protected void onClick(AjaxRequestTarget target)
		{
			if (shows)
			{
				view = name;
				target.add(content, sidebar);
			}
		}

		@Override
		public boolean isCurrent(Page page)
		{
			return name.equals(view);
		}
	}

	/** What the items of the demo do: call the server via Ajax, with a link, or lead to a page. */
	enum Kind
	{
		AJAX, LINK, PAGE
	}

	/**
	 * An item of the menus of the demo, which the page and the apps make theirs of: its name, the
	 * key of its label, and of its tooltip under name.tooltip; its icon, what it does, whether it is
	 * disabled and the items of its submenu. A separator has no name.
	 */
	record Entry(String name, IIcon icon, Kind kind, boolean disabled, List<Entry> items)
		implements Serializable
	{
		/** A separator. */
		static final Entry SEPARATOR = new Entry(null, null, Kind.AJAX, false, List.of());

		static Entry item(String name, IIcon icon)
		{
			return new Entry(name, icon, Kind.AJAX, false, List.of());
		}

		static Entry link(String name, IIcon icon)
		{
			return new Entry(name, icon, Kind.LINK, false, List.of());
		}

		static Entry page(String name, IIcon icon)
		{
			return new Entry(name, icon, Kind.PAGE, false, List.of());
		}

		static Entry menu(String name, IIcon icon, Entry... items)
		{
			return new Entry(name, icon, Kind.AJAX, false, List.of(items));
		}

		/**
		 * @return the entry, disabled
		 */
		Entry disable()
		{
			return new Entry(name, icon, kind, true, items);
		}

		boolean isSeparator()
		{
			return name == null;
		}

		/**
		 * @return the entries as the apps take them: the name, the markup of the icon, what the
		 *         item does, whether it is disabled and its items
		 */
		static JSONArray toJson(List<Entry> entries)
		{
			JSONArray array = new JSONArray();
			for (Entry entry : entries)
			{
				JSONObject json = new JSONObject();
				if (entry.isSeparator())
				{
					json.put("separator", true);
				}
				else
				{
					json.put("name", entry.name()).put("kind", entry.kind().name().toLowerCase());
					if (entry.icon() != null)
					{
						json.put("icon", entry.icon().getMarkup());
					}
					if (entry.disabled())
					{
						json.put("disabled", true);
					}
					if (!entry.items().isEmpty())
					{
						json.put("items", toJson(entry.items()));
					}
				}
				array.put(json);
			}
			return array;
		}
	}
}
