/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import com.antilia.widgets.demo.data.Access;
import com.antilia.widgets.demo.data.AccessFilter;
import com.antilia.widgets.demo.data.Accesses;
import com.antilia.widgets.dynamicdatatable.IIdentifiableDynamicDataProvider;
import com.antilia.widgets.dynamicdatatable.SortableDynamicDataProvider;

/**
 * Who accessed the site, one row per session, the latest first: when on the server's clock, who,
 * from where, the time on the visitor's clock and whether the session opened the demos; filtered
 * by user, part and address (see {@link AccessesFilterPanel}).
 */
class AccessesPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final AccessFilter filter = new AccessFilter();

	AccessesPanel(String id)
	{
		super(id);
		List<IColumn<Access, String>> columns = List.of(
			new AdminColumn<>("accessed", "accessed",
				access -> AdminTables.format(access.getAccessed())),
			new AdminColumn<>("user", "userName", Access::getUserName),
			new AdminColumn<Access>("part", "privatePart",
				access -> getString(access.isPrivatePart() ? "part.private" : "part.public")),
			new AdminColumn<>("address", "address", Access::getAddress),
			new AdminColumn<>("localTime", "localTime",
				access -> AdminTables.format(access.getLocalTime())),
			new AdminColumn<>("timeZone", "timeZone", Access::getTimeZone),
			new AdminColumn<>("browser", null, Access::getUserAgent));

		AccessesProvider provider = new AccessesProvider(filter);
		add(new Label("count", () -> provider.size()).setOutputMarkupId(true));
		FilteredAdminTable<Access> table = new FilteredAdminTable<>("table", columns, provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected AdminFilterPanel newFilter(String id)
			{
				return new AccessesFilterPanel(id, filter)
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onFilter(AjaxRequestTarget target)
					{
						filtered(target);
						target.add(AccessesPanel.this.get("count"));
					}
				};
			}
		};
		// wide enough for the times, the addresses and the emails of the users, the rest for the
		// browser
		table.setColumnProportions(1.5, 2.2, 0.8, 1.4, 1.5, 1.4, 4);
		add(AdminTables.configure(table, "accesses.csv"));
	}

	/**
	 * The accesses a page at a time from the database, sorted there.
	 */
	private static class AccessesProvider extends SortableDynamicDataProvider<Access, Long, String>
		implements
			IIdentifiableDynamicDataProvider<Access, Long>
	{
		private static final long serialVersionUID = 1L;

		private final AccessFilter filter;

		AccessesProvider(AccessFilter filter)
		{
			super(Long.class);
			this.filter = filter;
			setSort("accessed", SortOrder.DESCENDING);
		}

		@Override
		public Iterator<? extends Access> iterator(long first, long count)
		{
			return Accesses.get()
				.page(filter, AdminTables.pageable(first, count, getSort()))
				.iterator();
		}

		@Override
		public long size()
		{
			return Accesses.get().count(filter);
		}

		@Override
		public Access findByKey(Long key)
		{
			return Accesses.get().find(key);
		}
	}
}
