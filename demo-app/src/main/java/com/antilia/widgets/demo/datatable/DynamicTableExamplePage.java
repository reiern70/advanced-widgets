/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior.Location;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antilia.widgets.button.AjaxActionButton;
import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.antilia.widgets.dynamicdatatable.AjaxDownloadActionColumnContributor;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.OverlayPlacement;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.antilia.widgets.dynamicdatatable.ColumnChooserToolbarAction;
import com.antilia.widgets.dynamicdatatable.ColumnState;
import com.antilia.widgets.dynamicdatatable.CsvExportToolbarAction;
import com.antilia.widgets.dynamicdatatable.DynamicNavigationToolbar;
import com.antilia.widgets.dynamicdatatable.IAjaxActionColumnContributor;
import com.antilia.widgets.dynamicdatatable.IconBasedRowAction;
import com.antilia.widgets.dynamicdatatable.IconBasedToolbarAction;
import com.antilia.widgets.dynamicdatatable.SelectionCountToolbarAction;
import com.antilia.widgets.dynamicdatatable.SessionColumnStateStore;
import com.antilia.widgets.floating.FloatingPanel;
import com.antilia.widgets.icon.FontAwesomeIcon;
import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.icon.SvgIcon;
import com.antilia.widgets.table.MovableColumnsBehavior;
import com.antilia.widgets.table.ResizableColumnsBehavior;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * What the dynamic table examples share: a thousand users in a table with the same settings,
 * the same row actions and the same edit form. A subclass decides how the rows are rendered: in
 * the browser from JSON, or on the server by Wicket components.
 */
public abstract class DynamicTableExamplePage extends UsersPage
{
	private static final long serialVersionUID = 1L;

	/** The number of rows a page shows at first. */
	protected static final int ROWS_PER_PAGE = 25;

	private static final Duration SLOW_ACTION = Duration.ofSeconds(2);

	private final UserDetailsPanel details;

	private final UsersProvider provider = new UsersProvider();

	private final BaseDynamicDataTable<User, Long, ?> table;

	private final ResizableColumnsBehavior resizableColumns =
		new ResizableColumnsBehavior(ResizableColumnsBehavior.Mode.STRETCH);

	private final WebMarkupContainer themed;

	private final WebMarkupContainer tableParent;

	private final ColumnState initialColumnState;

	private final SelectionCountToolbarAction<User, Long> selectionCount =
		new SelectionCountToolbarAction<>();

	private ActionIcons icons = ActionIcons.BUILT_IN;

	/**
	 * Constructor.
	 */
	protected DynamicTableExamplePage()
	{
		get("feedback").setOutputMarkupId(true);

		details = new UserDetailsPanel("details", new PropertyModel<>(this, "selected"));
		add(details);

		table = newTable("table", provider);
		initialColumnState = table.getColumnState();
		table.setColumnStateStore(SessionColumnStateStore.INSTANCE);
		table.setNavigationPosition(ToolbarPosition.BOTH);
		table.setItemsPerPageOptions(25L, 50L, 100L, 500L);
		table.setAdjustToParentWidth(true);
		table.add(resizableColumns);
		table.add(new MovableColumnsBehavior());
		table.setBodyHeight(DynamicTableOptionsPanel.defaultHeight(BodyHeightUnits.PIXELS),
			BodyHeightUnits.PIXELS);
		table.setOverlayPlacement(OverlayPlacement.BODY);
		table.addToolbarAction(new AddUserAction());
		table.addToolbarAction(new SlowAction());
		table.addToolbarAction(new ColumnChooserToolbarAction<>());
		table.addToolbarAction(new CsvExportToolbarAction<User, Long>()
			.setFileName("users.csv"));
		table.addToolbarAction(selectionCount);

		themed = new WebMarkupContainer("themed");
		themed.setOutputMarkupId(true);
		add(themed);
		tableParent = new WebMarkupContainer("tableParent");
		tableParent.setOutputMarkupId(true);
		tableParent.add(new AttributeAppender("class",
			() -> table.getBodyHeightUnits() == BodyHeightUnits.PERCENT ? "ant-dataview-sized"
				: null));
		themed.add(tableParent);
		tableParent.add(table);

		DynamicTableOptionsPanel options = new DynamicTableOptionsPanel("options", table,
			resizableColumns, new PropertyModel<>(this, "icons"), getColumnProportions(),
			selectionCount)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected ColumnState getInitialColumnState()
			{
				return initialColumnState;
			}

			@Override
			protected void onTableChanged(AjaxRequestTarget target)
			{
				target.add(tableParent);
			}

		};
		add(options);

		WebMarkupContainer actions = new WebMarkupContainer("actions");
		add(actions);
		actions.add(new AjaxActionButton("resetColumns", new ResourceModel("resetColumns"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				options.resetColumns(target);
			}
		}.setIcon(SvgIcon.ROTATE_LEFT).setSize(Size.SMALL));
		actions.add(newPushRowsButton("pushRows"));
	}

	/**
	 * Creates the table of the users with its columns. The page adds the settings, toolbar
	 * actions and behaviors the demos share.
	 *
	 * @param id
	 *            the id of the table
	 * @param provider
	 *            the users
	 * @return the table
	 */
	protected abstract BaseDynamicDataTable<User, Long, ?> newTable(String id,
		UsersProvider provider);

	/**
	 * @return the proportions of the columns of the table offered by the settings, one per column
	 */
	protected abstract double[] getColumnProportions();

	/**
	 * @param id
	 *            the id of the button, attached to a {@code <button>}
	 * @return the button pushing the rows of the page to the browser, invisible by default
	 */
	protected Component newPushRowsButton(String id)
	{
		return new WebMarkupContainer(id).setVisible(false);
	}

	/**
	 * @return the table of the users
	 */
	protected final BaseDynamicDataTable<User, Long, ?> getTable()
	{
		return table;
	}

	/**
	 * @return the icons of the actions
	 */
	public ActionIcons getIcons()
	{
		return icons;
	}

	/**
	 * @param icons
	 *            the icons of the actions
	 */
	public void setIcons(ActionIcons icons)
	{
		this.icons = icons;
	}

	/**
	 * @return the row actions: edit, delete and download as a jCard
	 */
	protected final List<IAjaxActionColumnContributor<User>> newRowActions()
	{
		return List.of(new EditAction(), new DeleteAction(), new JCardDownload());
	}

	/**
	 * @return a navigation of the table with a filter by name in front of it, in the top
	 *         navigation unless there is only a bottom one
	 */
	protected final AbstractDynamicToolbar newNavigationToolbar(
		BaseDynamicDataTable<User, Long, ?> table, String id, ToolbarPosition position)
	{
		return new DynamicNavigationToolbar<>(id, table, position)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newPrefix(String id)
			{
				return new UserFilterPanel(id, new PropertyModel<>(provider, "filter"))
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onConfigure()
					{
						super.onConfigure();
						setVisible(isFilterShownAt(getPosition()));
					}

					@Override
					protected void onFilter(AjaxRequestTarget target)
					{
						table.setCurrentPage(0);
						table.refresh(target);
					}
				};
			}
		};
	}

	/**
	 * Tells the user that a clicked user no longer exists.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected final void onUserNotFound(AjaxRequestTarget target)
	{
		warn(getString("userGone"));
		target.add(get("feedback"));
	}

	/**
	 * @return the text of the page's bundle under the key, with the parameters put in
	 */
	protected final String message(String key, Object... parameters)
	{
		return new StringResourceModel(key, this).setParameters(parameters).getString();
	}

	/**
	 * @return the day the user was born, or {@code null} if it is not known
	 */
	static LocalDate bornOn(User user)
	{
		Date born = user.getBornDate();
		return born != null ? born.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
	}

	/**
	 * @return whether the filter is shown in the navigation at the position: in the top one,
	 *         unless there is only a bottom one
	 */
	private boolean isFilterShownAt(ToolbarPosition position)
	{
		return position == ToolbarPosition.TOP
			? table.getNavigationPosition() != ToolbarPosition.BOTTOM
			: table.getNavigationPosition() == ToolbarPosition.BOTTOM;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(
			DynamicTableExamplePage.class, "DynamicTableExamplePage.css")));
		response.render(FontAwesomeResourceReference.styleSheet());
	}


	/**
	 * Edits the user in the table's overlay, and selects it.
	 */
	private class EditAction extends IconBasedRowAction<User>
	{
		private static final long serialVersionUID = 1L;

		EditAction()
		{
			super("edit", Icon.EDIT, new ResourceModel("edit"));
		}

		@Override
		public IIcon getIcon()
		{
			return icons.pick(Icon.EDIT, SvgIcon.PEN, FontAwesomeIcon.PEN);
		}

		@Override
		protected void onRowAction(User user, AjaxRequestTarget target)
		{
			setSelected(user);
			target.add(details);
			showUserForm(user, new StringResourceModel("editUser",
				DynamicTableExamplePage.this, Model.of(user)), target);
		}
	}

	/**
	 * Adds a user, entered in the same form as an edited one, shown from an icon in the
	 * navigation.
	 */
	private class AddUserAction extends IconBasedToolbarAction<User, Long>
	{
		private static final long serialVersionUID = 1L;

		AddUserAction()
		{
			super(IconBasedRowAction.Icon.ADD, new ResourceModel("newUser"));
		}

		@Override
		public IIcon getIcon()
		{
			return icons.pick(IconBasedRowAction.Icon.ADD, SvgIcon.USER_PLUS,
				FontAwesomeIcon.USER_PLUS);
		}

		@Override
		protected void onClick(BaseDynamicDataTable<User, Long, ?> table,
			AjaxRequestTarget target)
		{
			showUserForm(new User(), new ResourceModel("newUser"), target);
		}
	}

	/**
	 * Takes a while on the server, so that the veil over the table shows its spinner.
	 */
	private class SlowAction extends IconBasedToolbarAction<User, Long>
	{
		private static final long serialVersionUID = 1L;

		private static final IIcon HOURGLASS = () -> "<svg aria-hidden=\"true\" " +
			"focusable=\"false\" viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" " +
			"fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" " +
			"stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M6 3h12 M6 21h12 " +
			"M7 3c0 5 10 6 10 9s-10 4-10 9 M17 3c0 5-10 6-10 9s10 4 10 9\"/></svg>";

		SlowAction()
		{
			super(HOURGLASS, new ResourceModel("slowAction"));
		}

		@Override
		public IIcon getIcon()
		{
			return icons.pick(HOURGLASS, SvgIcon.HOURGLASS_HALF, FontAwesomeIcon.HOURGLASS_HALF);
		}

		@Override
		protected void onClick(BaseDynamicDataTable<User, Long, ?> table,
			AjaxRequestTarget target)
		{
			try
			{
				Thread.sleep(SLOW_ACTION.toMillis());
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			info(getString("slowAction.done"));
			target.add(DynamicTableExamplePage.this.get("feedback"));
		}
	}

	/**
	 * Shows the form of a user in the table's overlay; the saved user is selected.
	 */
	private void showUserForm(User user, IModel<String> title, AjaxRequestTarget target)
	{
		table.showOverlay(new FloatingPanel(BaseDynamicDataTable.OVERLAY_CONTENT_ID, title)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				return new UserEditPanel(id, user)
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onSaved(User saved, AjaxRequestTarget target)
					{
						setSelected(saved);
						table.closeOverlay(target);
						table.refresh(target);
						target.add(details);
					}

					@Override
					protected void onCancel(AjaxRequestTarget target)
					{
						table.closeOverlay(target);
					}
				};
			}

			@Override
			protected void onClose(AjaxRequestTarget target)
			{
				table.closeOverlay(target);
			}
		}, target);
	}

	/**
	 * Deletes the user once the user confirmed it in the table's overlay.
	 */
	private class DeleteAction extends IconBasedRowAction<User>
	{
		private static final long serialVersionUID = 1L;

		DeleteAction()
		{
			super("delete", Icon.DELETE, new ResourceModel("delete"));
		}

		@Override
		public IIcon getIcon()
		{
			return icons.pick(Icon.DELETE, SvgIcon.TRASH_CAN, FontAwesomeIcon.TRASH_CAN);
		}

		@Override
		protected IModel<String> getConfirmation(User user)
		{
			return new StringResourceModel("delete.confirm", DynamicTableExamplePage.this,
				Model.of(user));
		}

		@Override
		protected void onRowAction(User user, AjaxRequestTarget target)
		{
			if (Objects.equals(getSelected(), user))
			{
				setSelected(null);
				target.add(details);
			}
			DatabaseLocator.getLargeDatabase().delete(user);
			info(message("deleted", user.getFirstName(), user.getLastName()));
			target.add(DynamicTableExamplePage.this.get("feedback"));
			table.refresh(target);
		}
	}

	/**
	 * Downloads the user as a jCard (RFC 7095), the JSON form of a vCard, which address books
	 * and user libraries read.
	 */
	private class JCardDownload extends AjaxDownloadActionColumnContributor<User>
	{
		private static final long serialVersionUID = 1L;

		JCardDownload()
		{
			super("download", new ResourceModel("download"));
			setIcon(IconBasedRowAction.Icon.DOWNLOAD);
			setLocation(Location.SameWindow);
		}

		@Override
		public IIcon getIcon()
		{
			return icons.pick(IconBasedRowAction.Icon.DOWNLOAD, SvgIcon.DOWNLOAD,
				FontAwesomeIcon.DOWNLOAD);
		}

		@Override
		protected String getFileName(User user)
		{
			return "user-" + user.getId() + ".json";
		}

		@Override
		protected String getContentType()
		{
			return "application/vcard+json";
		}

		@Override
		protected String getContent(User user)
		{
			return toJCard(user).toString(2);
		}
	}

	/**
	 * @return the user as a jCard: version, formatted and structured name, birthday, phones
	 *         and address
	 */
	static JSONArray toJCard(User user)
	{
		JSONArray properties = new JSONArray();
		properties.put(property("version", new JSONObject(), "text", "4.0"));
		properties.put(property("fn", new JSONObject(), "text",
			user.getFirstName() + " " + user.getLastName()));
		properties.put(property("n", new JSONObject(), "text",
			new JSONArray(List.of(user.getLastName(), user.getFirstName(), "", "", ""))));
		LocalDate born = bornOn(user);
		if (born != null)
		{
			properties.put(property("bday", new JSONObject(), "date", born.toString()));
		}
		if (user.getHomePhone() != null)
		{
			properties.put(property("tel", new JSONObject().put("type", new JSONArray(
				List.of("home", "voice"))), "uri", "tel:" + user.getHomePhone()));
		}
		if (user.getCellPhone() != null)
		{
			properties.put(property("tel", new JSONObject().put("type", new JSONArray(
				List.of("cell", "voice"))), "uri", "tel:" + user.getCellPhone()));
		}
		properties.put(property("adr", new JSONObject(), "text",
			new JSONArray(List.of("", "", Objects.toString(user.getAddress(), ""),
				Objects.toString(user.getCity(), ""), "", "",
				Objects.toString(user.getCountryName(), "")))));
		return new JSONArray().put("vcard").put(properties);
	}

	private static JSONArray property(String name, JSONObject parameters, String type,
		Object value)
	{
		return new JSONArray().put(name).put(parameters).put(type).put(value);
	}
}
