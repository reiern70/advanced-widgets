/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import com.antilia.widgets.button.AjaxActionButton;
import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.dynamicdatatable.ColumnState;
import com.antilia.widgets.dynamicdatatable.SessionColumnStateStore;
import com.antilia.widgets.icon.SvgIcon;
import com.antilia.widgets.modal.Modal;

/**
 * What the dynamic table examples share: the {@link UsersTablePanel table of the users}, its
 * settings above it and the details of the selected user below it. A subclass decides which
 * table: rendered in the browser from JSON, or on the server by Wicket components, and whether the
 * edit form and the confirmation of a deletion show in the table's overlay or in a {@link Modal}.
 */
public abstract class DynamicTableExamplePage extends UsersPage implements IUsersTableHost
{
	private static final long serialVersionUID = 1L;

	private final UserDetailsPanel details;

	private final UsersTablePanel users;

	private final ColumnState initialColumnState;

	/** The dialog of the edit form and of the confirmation, when the page uses a {@link Modal}. */
	private final Modal dialog;

	private ActionIcons icons = ActionIcons.BUILT_IN;

	/**
	 * Constructor.
	 */
	protected DynamicTableExamplePage()
	{
		details = new UserDetailsPanel("details", new PropertyModel<>(this, "selected"));
		add(details);

		if (isModal())
		{
			dialog = new Modal("dialog");
			dialog.confirmCloseIfModified(new ResourceModel("discard"));
			add(dialog);
		}
		else
		{
			dialog = null;
			add(new WebMarkupContainer("dialog").setVisible(false));
		}

		WebMarkupContainer themed = new WebMarkupContainer("themed");
		themed.setOutputMarkupId(true);
		add(themed);
		users = newUsersTable("tableParent");
		BaseDynamicDataTable<User, Long, ?> table = users.getTable();
		initialColumnState = table.getColumnState();
		table.setColumnStateStore(SessionColumnStateStore.INSTANCE);
		users.add(new AttributeAppender("class",
			() -> table.getBodyHeightUnits() == BodyHeightUnits.PERCENT ? "ant-dataview-sized"
				: null));
		themed.add(users);

		DynamicTableOptionsPanel options = new DynamicTableOptionsPanel("options", table,
			users.getResizableColumns(), new PropertyModel<>(this, "icons"),
			users.getColumnProportions(), users.getSelectionCount())
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
				target.add(users);
			}

			@Override
			protected boolean isOverlayUsed()
			{
				return !isModal();
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
	 * Creates the table of the users, with its columns, settings, toolbar and row actions.
	 *
	 * @param id
	 *            the id of the panel of the table
	 * @return the panel of the table, this page its host
	 */
	protected abstract UsersTablePanel newUsersTable(String id);

	/**
	 * @return whether the edit form and the confirmation of a deletion show in a {@link Modal}
	 *         instead of the table's overlay; {@code false} by default
	 */
	protected boolean isModal()
	{
		return false;
	}

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
		return users.getTable();
	}

	/**
	 * @return the panel of the table of the users
	 */
	protected final UsersTablePanel getUsersTable()
	{
		return users;
	}

	@Override
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

	@Override
	public Modal getDialog()
	{
		return dialog;
	}

	/**
	 * Selects the user and shows it in the details below the table.
	 */
	@Override
	public void select(User user, AjaxRequestTarget target)
	{
		setSelected(user);
		target.add(details);
	}

	/**
	 * @return the text of the page's bundle under the key, with the parameters put in
	 */
	protected final String message(String key, Object... parameters)
	{
		return new StringResourceModel(key, this).setParameters(parameters).getString();
	}
}
