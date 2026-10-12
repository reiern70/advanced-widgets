/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.desktop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antilia.widgets.button.AjaxActionButton;
import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.autocomplete.AutocompletePage;
import com.antilia.widgets.demo.button.ButtonsPage;
import com.antilia.widgets.demo.datatable.BeanUsersTablePanel;
import com.antilia.widgets.demo.datatable.IUsersTableHost;
import com.antilia.widgets.demo.datatable.JsonTableApps;
import com.antilia.widgets.demo.datatable.JsonUsersTablePanel;
import com.antilia.widgets.demo.datatable.User;
import com.antilia.widgets.demo.datatable.UsersTablePanel;
import com.antilia.widgets.demo.datepicker.DatePickerPage;
import com.antilia.widgets.demo.dropdown.DropdownExamplesPanel;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.antilia.widgets.demo.list.JsonListPage;
import com.antilia.widgets.demo.modal.ModalDialogPage;
import com.antilia.widgets.demo.progress.ProgressBarPage;
import com.antilia.widgets.desktop.Desktop;
import com.antilia.widgets.desktop.DesktopPane;
import com.antilia.widgets.desktop.DesktopWindow;
import com.antilia.widgets.desktop.TaskbarPosition;
import com.antilia.widgets.desktop.WindowState;
import com.antilia.widgets.desktop.WindowType;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.icon.SvgIcon;
import com.github.openjson.JSONObject;

/**
 * {@link Desktop}: floating windows that are not modal, holding the grids of the JSON Grid and
 * Bean Grid demos, {@link JsonUsersTablePanel} and {@link BeanUsersTablePanel}, with their
 * actions and their columns moving by drag and drop; the JSON grid in React and in Svelte too; the
 * drop-downs of the Dropdown demo, {@link DropdownExamplesPanel}, in Wicket, React and Svelte;
 * and a window with the keyboard shortcuts, which starts minimized. Every
 * window can be closed, and opened again from the menu of the taskbar; closing one with changes
 * not saved, a user edited or drop-downs not sent, asks first. The desktop sends the server the
 * states of its windows, the side of its taskbar and whether it fills the page, which the sentence
 * below it says by Ajax; another side for the taskbar renders the desktop again in those states.
 * The React and Svelte tabs show a desktop of their own, with the grid and the drop-downs of their
 * framework, notes, the keyboard shortcuts, and six more demos of the framework, each configured by
 * its page.
 * <p>
 * The windows are in two panes, the grids and the keyboard in the first, the drop-downs in the
 * second; the user adds, renames and removes panes and moves windows between them. The launcher of
 * the taskbar opens notes and Bean grids, types of windows the page registers.
 */
public class DesktopPage extends DemoPage implements IUsersTableHost
{
	private static final long serialVersionUID = 1L;

	/** The height of the body of the grids, in pixels, smaller than the windows they are in. */
	static final int BODY_HEIGHT = 260;

	/** The size the windows of the grids start with, in pixels. */
	static final int WINDOW_WIDTH = 900;

	static final int WINDOW_HEIGHT = 460;

	/** The width the windows of the drop-downs start with, in pixels, as high as the grids'. */
	static final int DROPDOWNS_WIDTH = 700;

	/** The icons of the windows: the first letter of each framework on its color, and a keyboard. */
	static final IIcon WICKET_ICON = badge("#e2711d", "W");

	static final IIcon BEAN_ICON = badge("#7a4fd6", "B");

	static final IIcon REACT_ICON = badge("#087ea4", "R");

	static final IIcon SVELTE_ICON = badge("#ff3e00", "S");

	static final IIcon KEYBOARD_ICON = SvgIcon.KEYBOARD;

	/** The icon of the notes: a sheet with lines, as the React and Svelte desktops have it. */
	static final IIcon NOTES_ICON = () -> "<svg viewBox=\"0 0 24 24\" width=\"16\" " +
		"height=\"16\" aria-hidden=\"true\" focusable=\"false\" fill=\"none\" " +
		"stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" " +
		"stroke-linejoin=\"round\"><path d=\"M6 3h9l4 4v14H6z M9 11h7 M9 15h7\"/></svg>";

	private final Desktop desktop;

	private final DesktopWindow keyboard;

	private JsonUsersTablePanel jsonUsers;

	private DropdownExamplesPanel dropdowns;

	private User selected;

	/**
	 * Constructor.
	 */
	public DesktopPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		Label states = new Label("states", this::describeStates);
		states.setOutputMarkupId(true);
		themed.add(states);

		Form<Void> form = new Form<>("form");
		form.setOutputMarkupId(true);
		themed.add(form);

		desktop = new Desktop("desktop")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onChanged(AjaxRequestTarget target)
			{
				// the side of the taskbar may have changed, dragged by the user
				target.add(states, form);
			}
		};
		desktop.setSendToServer(true).setOutputMarkupId(true);
		themed.add(desktop);
		desktop.getDefaultPane().setName(getString("pane.grids"));
		String dropdownsPane = desktop.addPane(getString("pane.dropdowns")).getId();
		desktop.addWindowType(new WindowType("notes", new ResourceModel("notes"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public DesktopWindow newWindow(String id)
			{
				return newNotesWindow(id);
			}
		}.setIcon(NOTES_ICON));
		desktop.addWindowType(new WindowType("beanGrid", new ResourceModel("beanUsers"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public DesktopWindow newWindow(String id)
			{
				return new DesktopWindow(id, new ResourceModel("beanUsers"))
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected Component newContent(String id)
					{
						return sized(new BeanUsersTablePanel(id, DesktopPage.this));
					}
				}.setIcon(BEAN_ICON).setPosition(96, 56).setSize(WINDOW_WIDTH, WINDOW_HEIGHT)
					.setMinSize(320, 160);
			}
		}.setIcon(BEAN_ICON));

		desktop.addWindow(new DesktopWindow(desktop.newWindowId(), new ResourceModel("jsonUsers"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newContent(String id)
			{
				jsonUsers = new JsonUsersTablePanel(id, DesktopPage.this);
				return sized(jsonUsers);
			}
		}.setIcon(WICKET_ICON).setPosition(16, 16).setSize(WINDOW_WIDTH, WINDOW_HEIGHT)
			.setMinSize(320, 160).setClosable(true));
		desktop.addWindow(new DesktopWindow(desktop.newWindowId(), new ResourceModel("beanUsers"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newContent(String id)
			{
				return sized(new BeanUsersTablePanel(id, DesktopPage.this));
			}
		}.setIcon(BEAN_ICON).setPosition(56, 56).setSize(WINDOW_WIDTH, WINDOW_HEIGHT)
			.setMinSize(320, 160).setClosable(true));
		// the grids of the apps keep nothing the user would lose
		desktop.addWindow(newAppWindow("reactUsers", "react", "jsonTable", this::getJsonTableConfig,
			REACT_ICON).setPosition(96, 96).setSize(WINDOW_WIDTH, WINDOW_HEIGHT).setModified(false));
		desktop.addWindow(newAppWindow("svelteUsers", "svelte", "jsonTable",
			this::getJsonTableConfig, SVELTE_ICON).setPosition(136, 136)
				.setSize(WINDOW_WIDTH, WINDOW_HEIGHT).setModified(false));
		desktop.addWindow(new DesktopWindow(desktop.newWindowId(),
			new ResourceModel("wicketDropdowns"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newContent(String id)
			{
				dropdowns = new DropdownExamplesPanel(id);
				return dropdowns;
			}
		}.setIcon(WICKET_ICON).setPosition(16, 16).setSize(DROPDOWNS_WIDTH, WINDOW_HEIGHT)
			.setMinSize(320, 160).setClosable(true).setPane(dropdownsPane));
		desktop.addWindow(newAppWindow("reactDropdowns", "react", "dropdown",
			this::getDropdownConfig, REACT_ICON).setPosition(56, 56)
				.setSize(DROPDOWNS_WIDTH, WINDOW_HEIGHT).setPane(dropdownsPane));
		desktop.addWindow(newAppWindow("svelteDropdowns", "svelte", "dropdown",
			this::getDropdownConfig, SVELTE_ICON).setPosition(96, 96)
				.setSize(DROPDOWNS_WIDTH, WINDOW_HEIGHT).setPane(dropdownsPane));
		keyboard = new DesktopWindow(desktop.newWindowId(), new ResourceModel("keyboard"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newContent(String id)
			{
				return new Fragment(id, "keyboardFragment", DesktopPage.this);
			}
		};
		keyboard.setIcon(KEYBOARD_ICON).setPosition(560, 16).setSize(640, 620).setMinSize(320, 200)
			.setClosable(true).setMinimized(true).setPane(DesktopPane.DEFAULT);
		desktop.addWindow(keyboard);

		DropDownChoice<TaskbarPosition> position = new DropDownChoice<>("taskbar",
			new PropertyModel<>(desktop, "taskbarPosition"), Arrays.asList(TaskbarPosition.values()),
			new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(TaskbarPosition object)
				{
					return getString("taskbar." + object.cssName());
				}

				@Override
				public String getIdValue(TaskbarPosition object, int index)
				{
					return object.cssName();
				}
			});
		position.setNullValid(false).setRequired(true);
		position.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(desktop);
			}
		});
		form.add(position);
		form.add(new AjaxActionButton("openKeyboard", new ResourceModel("openKeyboard"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				// in front, opened again when closed and restored when minimized
				target.appendJavaScript("Wicket.Desktop.of(" +
					JSONObject.quote(desktop.getDesktopMarkupId()) + ").activate(" +
					JSONObject.quote(keyboard.getMarkupId()) + ", true);");
			}
		}.setIcon(SvgIcon.KEYBOARD).setSize(Size.SMALL));
	}

	/**
	 * @return a window of notes, a field to write in, which the launcher of the taskbar opens
	 */
	private DesktopWindow newNotesWindow(String id)
	{
		return new DesktopWindow(id, new ResourceModel("notes"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newContent(String id)
			{
				return new Fragment(id, "notesFragment", DesktopPage.this);
			}
		}.setIcon(NOTES_ICON).setPosition(200, 40).setSize(360, 240);
	}

	/**
	 * @return a window holding an app of a framework, which mounts as soon as the bundle of the
	 *         framework runs; it can be closed
	 */
	private DesktopWindow newAppWindow(String titleKey, String framework, String demo,
		IModel<JSONObject> config, IIcon icon)
	{
		return new DesktopWindow(desktop.newWindowId(), new ResourceModel(titleKey))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newContent(String id)
			{
				WebMarkupContainer app = FrameworkTabs.newApp("app", framework, demo, config);
				app.add(AttributeModifier.replace("data-mount", "true"));
				return new Fragment(id, "appFragment", DesktopPage.this).add(app);
			}
		}.setIcon(icon).setMinSize(320, 160).setClosable(true);
	}

	/**
	 * @return the grid of a window, its body lower than the window, as wide as its columns: the
	 *         window scrolls sideways
	 */
	private static UsersTablePanel sized(UsersTablePanel users)
	{
		users.getTable().setBodyHeight(BODY_HEIGHT, BodyHeightUnits.PIXELS);
		users.getTable().setAdjustToParentWidth(false);
		return users;
	}

	/**
	 * @return the configuration of the React and Svelte grids: those of the JSON grid demo, made
	 *         from the Wicket grid of the first window, without the settings on top
	 */
	private JSONObject getJsonTableConfig()
	{
		JSONObject config = JsonTableApps.config(this, jsonUsers.getTable());
		config.put("compact", true).put("adjustToParent", false);
		config.getJSONObject("bodyHeight").put("value", BODY_HEIGHT);
		return config;
	}

	/**
	 * @return the configuration of the React and Svelte drop-downs: those of the Dropdown demo,
	 *         with the choices of the drop-downs in Wicket
	 */
	private JSONObject getDropdownConfig()
	{
		return dropdowns.getFrameworkConfig();
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "desktop";
	}

	/**
	 * @return the texts of the desktops of the apps, their keyboard shortcuts, and the
	 *         configuration of their grids
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "taskbar", "taskbar.bottom", "taskbar.top",
			"taskbar.left", "taskbar.right", "openKeyboard", "reactUsers", "svelteUsers",
			"reactDropdowns", "svelteDropdowns", "keyboard", "notes", "notesText", "key.f11",
			"key.next", "key.previous", "key.doubleClick", "key.fold", "key.taskbar",
			"key.taskbarHelp", "key.menu", "key.menuHelp", "key.close", "key.closeHelp",
			"key.dragTaskbar", "key.dragTaskbarHelp", "key.escape", "key.previousPane",
			"key.nextPane", "key.panes", "key.panesHelp", "key.moveToPane", "key.moveToPaneHelp",
			"key.launcher", "key.launcherHelp", "pane.grids", "pane.dropdowns", "pane.demos");
		JSONObject desktopTexts = new JSONObject();
		for (String name : new String[] { "minimize", "maximize", "restore", "close", "taskbar",
			"grid", "columns", "rows", "cascade", "expand", "collapse", "move", "menu",
			"openWindows", "closedWindows", "closeModifiedTitle", "closeModified", "discard",
			"keep", "panes", "defaultPane", "newPane", "newPaneName", "renamePane",
			"renamePaneTitle", "removePane", "paneName", "moveToPane", "moveToNewPane", "newWindow",
			"ok", "cancel" })
		{
			desktopTexts.put(name, getString("Desktop." + name));
		}
		return new JSONObject()
			.put("texts", texts)
			.put("desktop", desktopTexts)
			.put("jsonTable", getJsonTableConfig())
			.put("dropdown", getDropdownConfig())
			.put("demos", getDemosConfig());
	}

	/**
	 * @return the other demos the React and Svelte desktops show in windows, by the names of their
	 *         apps: the name of each demo, the title of its window, and the configuration its page
	 *         gives its apps
	 */
	private JSONObject getDemosConfig()
	{
		Map<String, Supplier<DemoPage>> pages = new LinkedHashMap<>();
		pages.put("autocomplete", AutocompletePage::new);
		pages.put("buttons", ButtonsPage::new);
		pages.put("datePicker", DatePickerPage::new);
		pages.put("jsonList", JsonListPage::new);
		pages.put("modalDialog", ModalDialogPage::new);
		pages.put("progressBar", ProgressBarPage::new);
		JSONObject demos = new JSONObject();
		pages.forEach((name, page) -> demos.put(name, new JSONObject()
			.put("title", getString("demo." + name))
			.put("config", getFrameworkConfig(page.get()))));
		return demos;
	}

	/**
	 * @return the side of the taskbar, whether the desktop fills the page and the states of the
	 *         windows as the server knows them, in words
	 */
	private String describeStates()
	{
		List<String> described = new ArrayList<>();
		String side = getString("taskbar." + desktop.getTaskbarPosition().cssName());
		String layout = new StringResourceModel("layout", this).setParameters(
			side.substring(0, 1).toLowerCase(getLocale()) + side.substring(1)).getString();
		described.add(desktop.isExpanded() ? layout + ", " + getString("filling") : layout);
		List<String> panes = new ArrayList<>();
		for (DesktopPane pane : desktop.getPanes())
		{
			panes.add(paneName(pane));
		}
		described.add(new StringResourceModel("panes", this).setParameters(String.join(", ", panes),
			paneName(desktop.getPane(desktop.getActivePane()))).getString());
		for (DesktopWindow window : desktop.getWindows())
		{
			WindowState state = window.getState();
			String title = window.get("title").getDefaultModelObjectAsString();
			StringBuilder text = new StringBuilder(state.getX() == null ? title + " " +
				getString("nowhere") : new StringResourceModel("state", this).setParameters(title,
					state.getX(), state.getY(), state.getWidth(), state.getHeight()).getString());
			DesktopPane pane = desktop.getPane(state.getPane());
			if (pane != null)
			{
				text.append(" ").append(new StringResourceModel("inPane", this)
					.setParameters(paneName(pane)).getString());
			}
			for (String flag : new String[] { "minimized", "maximized", "folded", "closed" })
			{
				boolean on = switch (flag)
				{
					case "minimized" -> state.isMinimized();
					case "maximized" -> state.isMaximized();
					case "folded" -> state.isFolded();
					default -> state.isClosed();
				};
				if (on)
				{
					text.append(", ").append(getString(flag));
				}
			}
			described.add(text.toString());
		}
		return new StringResourceModel("states", this).setParameters(String.join("; ", described))
			.getString();
	}

	/**
	 * @return the name of a pane, as the desktop names the default one while it has none
	 */
	private String paneName(DesktopPane pane)
	{
		return pane.getName() != null && !pane.getName().isBlank() ? pane.getName() :
			getString("Desktop.defaultPane");
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Desktop.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(DesktopPage.class, "DesktopPage.css")));
	}

	@Override
	public User getSelected()
	{
		return selected;
	}

	/**
	 * Selects the user, the one edited last; the desktop shows no details of it.
	 */
	@Override
	public void select(User user, AjaxRequestTarget target)
	{
		selected = user;
	}

	/**
	 * @return the desktop
	 */
	Desktop getDesktop()
	{
		return desktop;
	}

	/**
	 * @return the window of the keyboard shortcuts
	 */
	DesktopWindow getKeyboardWindow()
	{
		return keyboard;
	}

	/**
	 * @return an icon: a letter on a rounded square of a color
	 */
	private static IIcon badge(String color, String letter)
	{
		return () -> "<svg viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" aria-hidden=\"true\" " +
			"focusable=\"false\"><rect width=\"24\" height=\"24\" rx=\"6\" fill=\"" + color +
			"\"/><text x=\"12\" y=\"17\" text-anchor=\"middle\" font-size=\"14\" " +
			"font-weight=\"700\" font-family=\"sans-serif\" fill=\"#ffffff\">" + letter +
			"</text></svg>";
	}
}
