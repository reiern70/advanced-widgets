/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import com.antilia.widgets.demo.data.Access;
import com.antilia.widgets.demo.data.Accesses;
import com.antilia.widgets.dynamicdatatable.BeanBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.IIdentifiableDynamicDataProvider;
import com.antilia.widgets.dynamicdatatable.SortableDynamicDataProvider;

/**
 * Who accessed the site, one row per session, the latest first: when on the server's clock, who,
 * from where, the time on the visitor's clock and whether the session opened the demos.
 */
class AccessesPanel extends Panel
{
	private static final long serialVersionUID = 1L;

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

		add(new Label("count", () -> Accesses.get().count()));
		add(AdminTables.configure(new BeanBasedDynamicDataTable<>("table", columns,
			new AccessesProvider(), AdminPage.ROWS_PER_PAGE), "accesses.csv"));
	}

	/**
	 * The accesses a page at a time from the database, sorted there.
	 */
	private static class AccessesProvider extends SortableDynamicDataProvider<Access, Long, String>
		implements
			IIdentifiableDynamicDataProvider<Access, Long>
	{
		private static final long serialVersionUID = 1L;

		AccessesProvider()
		{
			super(Long.class);
			setSort("accessed", SortOrder.DESCENDING);
		}

		@Override
		public Iterator<? extends Access> iterator(long first, long count)
		{
			return Accesses.get().page(AdminTables.pageable(first, count, getSort())).iterator();
		}

		@Override
		public long size()
		{
			return Accesses.get().count();
		}

		@Override
		public Access findByKey(Long key)
		{
			return Accesses.get().find(key);
		}
	}
}
