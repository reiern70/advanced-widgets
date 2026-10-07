/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.lang.Args;

import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;

/**
 * The navigation of a {@link BaseDynamicDataTable}: from left to right an optional
 * {@link #newPrefix(String) prefix}, such as a filter, the label telling which rows are shown,
 * the paging links, the {@link IDynamicToolbarAction actions} of the table and the choice of the
 * number of rows per page.
 * <p>
 * The toolbar is shown when there is more than one page, a choice of rows per page, an action or
 * a visible prefix. Override {@link BaseDynamicDataTable#newNavigationToolbar(String,
 * ToolbarPosition)} to use a subclass.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys of the rows
 * @since 11.0.0
 */
public class DynamicNavigationToolbar<T, K extends Serializable> extends AbstractDynamicToolbar
{
	private static final long serialVersionUID = 1L;

	private final BaseDynamicDataTable<T, K, ?> table;

	private final ToolbarPosition position;

	private Component prefix;

	private WebMarkupContainer paging;

	private RepeatingView actions;

	/**
	 * @param id
	 *            component id
	 * @param table
	 *            the table
	 * @param position
	 *            whether the toolbar is above or below the rows, {@link ToolbarPosition#TOP} or
	 *            {@link ToolbarPosition#BOTTOM}
	 */
	public DynamicNavigationToolbar(String id, BaseDynamicDataTable<T, K, ?> table,
		ToolbarPosition position)
	{
		super(id, table);
		this.table = table;
		this.position = Args.notNull(position, "position");
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();

		WebMarkupContainer span = new WebMarkupContainer("span");
		span.add(newColspan());
		add(span);

		prefix = newPrefix("prefix");
		span.add(prefix);

		paging = new WebMarkupContainer("paging");
		paging.setOutputMarkupId(true);
		span.add(paging);
		paging.add(new NavigatorLabel("navigatorLabel", new ShownRows(table)));
		paging.add(new AjaxPagingNavigator("navigator", table)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(table.getPageCount() > 1 && table.isInfiniteScroll() == false);
			}

			@Override
			protected void onAjaxEvent(AjaxRequestTarget target)
			{
				table.refresh(target);
			}
		});

		actions = new RepeatingView("actions");
		populateActions(actions);
		WebMarkupContainer actionsContainer = new WebMarkupContainer("actionsContainer");
		actionsContainer.setVisible(actions.size() > 0);
		actionsContainer.add(actions);
		span.add(actionsContainer);

		span.add(newItemsPerPageChoice());
	}

	/**
	 * Creates the component shown before the label telling which rows are shown, for example a
	 * filter. Returns an invisible placeholder by default; a visible prefix keeps the toolbar
	 * shown even on a single page, and is not re-rendered when the rows change, so that a text
	 * field keeps its focus while the user types.
	 *
	 * @param id
	 *            the id the component must have; it is attached to a {@code <div>}
	 * @return the component
	 */
	protected Component newPrefix(String id)
	{
		return new WebMarkupContainer(id).setVisible(false);
	}

	/**
	 * Adds the components of the actions shown after the paging links: those of the table's
	 * {@link BaseDynamicDataTable#getToolbarActions() actions} by default.
	 *
	 * @param actions
	 *            the view to add the components to, with ids from
	 *            {@link RepeatingView#newChildId()}
	 */
	protected void populateActions(RepeatingView actions)
	{
		for (IDynamicToolbarAction<T, K> action : table.getToolbarActions())
		{
			actions.add(action.newComponent(actions.newChildId(), table));
		}
	}

	/**
	 * @return whether the toolbar is above or below the rows
	 */
	public ToolbarPosition getPosition()
	{
		return position;
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		prefix.configure();
		setVisible(table.getPageCount() > 1 || table.getItemsPerPageOptions().isEmpty() == false ||
			prefix.isVisible() || actions.size() > 0);
	}

	/**
	 * Re-renders the paging part only when there is a visible prefix, which keeps the toolbar
	 * shown, and the whole toolbar otherwise.
	 */
	@Override
	public void refresh(IPartialPageRequestHandler target)
	{
		if (prefix.isVisible() && isVisibleInHierarchy())
		{
			target.add(paging);
		}
		else
		{
			target.add(this);
		}
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		tag.append("class", "navigation", " ");
	}

	/**
	 * The rows the browser shows, for the label: the current page, or, scrolling infinitely, all
	 * rows loaded so far, as if they were one page.
	 */
	private static class ShownRows implements IPageableItems
	{
		private static final long serialVersionUID = 1L;

		private final BaseDynamicDataTable<?, ?, ?> table;

		ShownRows(BaseDynamicDataTable<?, ?, ?> table)
		{
			this.table = table;
		}

		@Override
		public long getCurrentPage()
		{
			return table.getCurrentPage();
		}

		@Override
		public void setCurrentPage(long page)
		{
			table.setCurrentPage(page);
		}

		@Override
		public long getPageCount()
		{
			return table.getPageCount();
		}

		@Override
		public long getItemsPerPage()
		{
			return table.isInfiniteScroll() ? Math.max(1, table.getShownRowCount())
				: table.getItemsPerPage();
		}

		@Override
		public void setItemsPerPage(long itemsPerPage)
		{
			table.setItemsPerPage(itemsPerPage);
		}

		@Override
		public long getItemCount()
		{
			return table.getItemCount();
		}
	}

	private WebMarkupContainer newItemsPerPageChoice()
	{
		WebMarkupContainer pageSize = new WebMarkupContainer("pageSize")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				// scrolling infinitely, the rows come as they are scrolled to, not by pages
				setVisible(table.getItemsPerPageOptions().isEmpty() == false &&
					table.isInfiniteScroll() == false);
			}
		};
		DropDownChoice<Long> choice = new DropDownChoice<>("itemsPerPage", new IModel<Long>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Long getObject()
			{
				return table.getItemsPerPage();
			}

			@Override
			public void setObject(Long object)
			{
				long first = table.getCurrentPage() * table.getItemsPerPage();
				table.setItemsPerPage(object);
				table.setCurrentPage(first / object);
			}
		}, table::getItemsPerPageOptions);
		choice.setRequired(true);
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				table.refresh(target);
			}
		});
		pageSize.add(choice);
		pageSize.add(new Label("label", new ResourceModel("DynamicDataTable.rowsPerPage",
			"Rows per page")));
		return pageSize;
	}
}
