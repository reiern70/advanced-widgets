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
package com.antiliacode.widgets.dynamicdatatable;

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
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookContainer;
import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;

/**
 * The navigation of a {@link BaseDynamicDataTable}: from left to right an optional
 * {@link #newPrefix(String) prefix}, such as a filter, the choice of the number of rows per page,
 * the label telling which rows are shown, the paging links and the
 * {@link IDynamicToolbarAction actions} of the table. The choice comes early, so that it stays in
 * sight when the columns make the table wider than the window.
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
		span.add(new LookContainer("navigation", LookPart.TABLE_NAVIGATION));

		prefix = newPrefix("prefix");
		span.add(prefix.add(new LookBehavior(LookPart.TABLE_NAVIGATION_PREFIX)));

		span.add(newItemsPerPageChoice());

		paging = new WebMarkupContainer("paging");
		paging.setOutputMarkupId(true);
		span.add(paging.add(new LookBehavior(LookPart.TABLE_PAGING)));
		paging.add(new LookContainer("labelFrame", LookPart.TABLE_NAVIGATOR_LABEL));
		paging.add(new NavigatorLabel("navigatorLabel", new ShownRows(table)));
		paging.add(new LookContainer("navigatorFrame", LookPart.TABLE_NAVIGATOR));
		paging.add(new AjaxPagingNavigator("navigator", table)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onBeforeRender()
			{
				super.onBeforeRender();
				// Wicket's markup of the links: the pages, made anew each time, and every link
				visitChildren((child, visit) -> {
					if (child.getBehaviors(LookBehavior.class).isEmpty())
					{
						if (child instanceof LoopItem)
						{
							child.add(new LookBehavior(LookPart.TABLE_PAGE));
						}
						else if (child instanceof AbstractLink)
						{
							child.add(new LookBehavior(LookPart.TABLE_PAGE_LINK));
						}
					}
				});
			}

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(table.getPageCount() > 1 && table.isInfiniteScroll() == false);
			}

			@Override
			protected void onAjaxEvent(AjaxRequestTarget target)
			{
				table.onNavigation(target);
				table.refresh(target);
			}
		}.add(new LookBehavior(LookPart.TABLE_PAGES)));

		actions = new RepeatingView("actions");
		populateActions(actions);
		WebMarkupContainer actionsContainer = new WebMarkupContainer("actionsContainer");
		actionsContainer.setVisible(actions.size() > 0);
		actionsContainer.add(new LookBehavior(LookPart.TABLE_ACTIONS));
		actionsContainer.add(actions);
		span.add(actionsContainer);
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
		Looks.addClasses(tag, Looks.of(this), LookPart.TABLE_NAVIGATION_ROW);
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
		Dropdown<Long> choice = new Dropdown<>("itemsPerPage", new IModel<Long>()
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
				table.onNavigation(target);
				table.refresh(target);
			}
		});
		pageSize.add(choice);
		pageSize.add(new LookBehavior(LookPart.TABLE_PAGE_SIZE));
		pageSize.add(new Label("label", new ResourceModel("DynamicDataTable.rowsPerPage",
			"Rows per page")));
		return pageSize;
	}
}
