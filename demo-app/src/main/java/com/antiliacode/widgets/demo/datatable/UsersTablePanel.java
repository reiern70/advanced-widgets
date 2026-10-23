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
package com.antiliacode.widgets.demo.datatable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior.Location;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.antiliacode.widgets.dynamicdatatable.AjaxDownloadActionColumnContributor;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.OverlayPlacement;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.antiliacode.widgets.dynamicdatatable.ColumnChooserToolbarAction;
import com.antiliacode.widgets.dynamicdatatable.CsvExportToolbarAction;
import com.antiliacode.widgets.dynamicdatatable.DynamicNavigationToolbar;
import com.antiliacode.widgets.dynamicdatatable.IAjaxActionColumnContributor;
import com.antiliacode.widgets.dynamicdatatable.IconBasedRowAction;
import com.antiliacode.widgets.dynamicdatatable.IconBasedToolbarAction;
import com.antiliacode.widgets.dynamicdatatable.SelectionCountToolbarAction;
import com.antiliacode.widgets.floating.FloatingPanel;
import com.antiliacode.widgets.icon.FontAwesomeIcon;
import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.modal.IModalButtonFactory;
import com.antiliacode.widgets.modal.Modal;
import com.antiliacode.widgets.table.MovableColumnsBehavior;
import com.antiliacode.widgets.table.ResizableColumnsBehavior;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The table of the users the dynamic table examples share, on any page: a thousand users with the
 * same settings, the same toolbar and row actions and the same edit form. Its columns move by drag
 * and drop and resize. A subclass decides how the rows are rendered: in the browser from JSON,
 * {@link JsonUsersTablePanel}, or on the server by Wicket components, {@link BeanUsersTablePanel}.
 * The edit form and the confirmation of a deletion show in the {@link IUsersTableHost#getDialog()
 * dialog of the page}, or in the table's overlay; the messages in the notifications of the page.
 */
public abstract class UsersTablePanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The number of rows a page shows at first. */
	public static final int ROWS_PER_PAGE = 25;

	private static final Duration SLOW_ACTION = Duration.ofSeconds(2);

	private final IUsersTableHost host;

	private final UsersProvider provider = new UsersProvider();

	private final BaseDynamicDataTable<User, Long, ?> table;

	private final ResizableColumnsBehavior resizableColumns =
		new ResizableColumnsBehavior(ResizableColumnsBehavior.Mode.STRETCH);

	private final SelectionCountToolbarAction<User, Long> selectionCount =
		new SelectionCountToolbarAction<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public IIcon getClearIcon()
			{
				return icons().pick(IconBasedRowAction.Icon.DELETE, SvgIcon.TRASH_CAN,
					FontAwesomeIcon.TRASH_CAN);
			}
		};

	/**
	 * @param id
	 *            component id
	 * @param host
	 *            the page the table is on
	 */
	protected UsersTablePanel(String id, IUsersTableHost host)
	{
		super(id);
		this.host = host;
		setOutputMarkupId(true);

		table = newTable("table", provider);
		table.setNavigationPosition(ToolbarPosition.TOP);
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
		// searching, paging, selecting and choosing columns lose nothing when a window of a
		// desktop holding the table closes: only the form of a user does, see showUserForm
		table.add(AttributeModifier.replace("data-modified", "false"));
		add(table);
	}

	/**
	 * Creates the table of the users with its columns. The panel adds the settings, toolbar
	 * actions and behaviors the examples share.
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
	 * @return the proportions of the columns of the table the settings offer, one per column
	 */
	public abstract double[] getColumnProportions();

	/**
	 * @return the table of the users
	 */
	public BaseDynamicDataTable<User, Long, ?> getTable()
	{
		return table;
	}

	/**
	 * @return the users
	 */
	public UsersProvider getProvider()
	{
		return provider;
	}

	/**
	 * @return the behavior resizing the columns, which the settings add to the table or take away
	 */
	public ResizableColumnsBehavior getResizableColumns()
	{
		return resizableColumns;
	}

	/**
	 * @return the action counting the selected users in the navigation
	 */
	public SelectionCountToolbarAction<User, Long> getSelectionCount()
	{
		return selectionCount;
	}

	private ActionIcons icons()
	{
		return host.getIcons();
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
	}

	/**
	 * @return a text of the panel's bundle, for the dialog of the page, which does not see the
	 *         bundle
	 */
	private IModel<String> text(String key)
	{
		return new StringResourceModel(key, this);
	}

	/**
	 * @return the text of the panel's bundle under the key, with the parameters put in
	 */
	private String message(String key, Object... parameters)
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
	 * Edits the user in the table's overlay or the dialog, and selects it.
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
			return icons().pick(Icon.EDIT, SvgIcon.PEN, FontAwesomeIcon.PEN);
		}

		@Override
		protected void onRowAction(User user, AjaxRequestTarget target)
		{
			host.select(user, target);
			showUserForm(user, new StringResourceModel("editUser", UsersTablePanel.this,
				Model.of(user)), target);
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
			return icons().pick(IconBasedRowAction.Icon.ADD, SvgIcon.USER_PLUS,
				FontAwesomeIcon.USER_PLUS);
		}

		@Override
		protected void onClick(BaseDynamicDataTable<User, Long, ?> table,
			AjaxRequestTarget target)
		{
			showUserForm(new User(), text("newUser"), target);
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
			return icons().pick(HOURGLASS, SvgIcon.HOURGLASS_HALF, FontAwesomeIcon.HOURGLASS_HALF);
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
		}
	}

	/**
	 * Shows the form of a user in the table's overlay or the dialog; the saved user is selected.
	 */
	private void showUserForm(User user, IModel<String> title, AjaxRequestTarget target)
	{
		Modal dialog = host.getDialog();
		if (dialog != null)
		{
			dialog.open(title, new UserEditPanel(Modal.BODY_ID, user)
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSaved(User saved, AjaxRequestTarget target)
				{
					dialog.close(target);
					table.refresh(target);
					host.select(saved, target);
				}

				@Override
				protected void onCancel(AjaxRequestTarget target)
				{
					dialog.close(target);
				}
			}, target);
			return;
		}
		table.showOverlay(new FloatingPanel(BaseDynamicDataTable.OVERLAY_CONTENT_ID, title)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				UserEditPanel form = new UserEditPanel(id, user)
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onSaved(User saved, AjaxRequestTarget target)
					{
						table.closeOverlay(target);
						table.refresh(target);
						host.select(saved, target);
					}

					@Override
					protected void onCancel(AjaxRequestTarget target)
					{
						table.closeOverlay(target);
					}
				};
				// what the user typed in it is lost when a window of a desktop holding the table
				// closes, which asks first
				return form.add(AttributeModifier.replace("data-modified", "auto"));
			}

			@Override
			protected void onClose(AjaxRequestTarget target)
			{
				table.closeOverlay(target);
			}
		}, target);
	}

	/**
	 * Deletes the user once the user confirmed it in the table's overlay or the dialog.
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
			return icons().pick(Icon.DELETE, SvgIcon.TRASH_CAN, FontAwesomeIcon.TRASH_CAN);
		}

		@Override
		protected IModel<String> getConfirmation(User user)
		{
			// the dialog asks itself, see onRowAction
			return host.getDialog() != null ? null : confirmation(user);
		}

		@Override
		protected void onRowAction(User user, AjaxRequestTarget target)
		{
			if (host.getDialog() != null)
			{
				confirmDeletion(user, target);
			}
			else
			{
				delete(user, target);
			}
		}
	}

	private IModel<String> confirmation(User user)
	{
		return new StringResourceModel("delete.confirm", this, Model.of(user));
	}

	/**
	 * Asks in the dialog to confirm the deletion of a user, then deletes it if it still exists.
	 */
	private void confirmDeletion(User user, AjaxRequestTarget target)
	{
		Modal dialog = host.getDialog();
		String key = table.getRowKey(user);
		dialog.open(text("delete"), new Label(Modal.BODY_ID, confirmation(user)),
			target, IModalButtonFactory.close(new ResourceModel("DynamicDataTable.cancel")),
			IModalButtonFactory.primary(text("delete"), confirmed -> {
				dialog.close(confirmed);
				User current = table.findRow(key);
				if (current == null)
				{
					onUserNotFound(confirmed);
				}
				else
				{
					delete(current, confirmed);
				}
			}));
	}

	private void delete(User user, AjaxRequestTarget target)
	{
		if (Objects.equals(host.getSelected(), user))
		{
			host.select(null, target);
		}
		DatabaseLocator.getLargeDatabase().delete(user);
		info(message("deleted", user.getFirstName(), user.getLastName()));
		table.refresh(target);
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
			return icons().pick(IconBasedRowAction.Icon.DOWNLOAD, SvgIcon.DOWNLOAD,
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
