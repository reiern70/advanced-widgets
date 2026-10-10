/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.antilia.widgets.demo.BasePage;
import com.antilia.widgets.demo.data.Access;
import com.antilia.widgets.demo.data.Accesses;

/**
 * Who accessed the site, one row per session, the latest first: when on the server's clock, who,
 * from where, the time on the visitor's clock and whether the session opened the demos. Only for
 * administrators, see {@link com.antilia.widgets.demo.account.DemosAuthorization}.
 */
public class AccessesPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/** The rows on a page of the table. */
	static final int ROWS_PER_PAGE = 25;

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public AccessesPage()
	{
		List<IColumn<Access, String>> columns = List.of(
			new LambdaColumn<>(new ResourceModel("accessed"), "accessed",
				access -> TIME.format(access.getAccessed().atOffset(ZoneOffset.UTC)) + " UTC"),
			new LambdaColumn<>(new ResourceModel("user"), "userName", Access::getUserName),
			new AbstractColumn<Access, String>(new ResourceModel("part"), "privatePart")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void populateItem(Item<ICellPopulator<Access>> item, String componentId,
					IModel<Access> rowModel)
				{
					item.add(new Label(componentId, new ResourceModel(
						rowModel.getObject().isPrivatePart() ? "part.private" : "part.public")));
				}
			},
			new LambdaColumn<>(new ResourceModel("address"), "address", Access::getAddress),
			new LambdaColumn<>(new ResourceModel("localTime"), "localTime",
				access -> access.getLocalTime() == null ? "" : TIME.format(access.getLocalTime())),
			new LambdaColumn<>(new ResourceModel("timeZone"), "timeZone", Access::getTimeZone),
			new LambdaColumn<>(new ResourceModel("browser"), Access::getUserAgent));

		add(new Label("count", () -> Accesses.get().count()));
		add(new AjaxFallbackDefaultDataTable<>("table", columns, new AccessesProvider(),
			ROWS_PER_PAGE));
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}

	/**
	 * The accesses a page at a time from the database, sorted there.
	 */
	private static class AccessesProvider extends SortableDataProvider<Access, String>
	{
		private static final long serialVersionUID = 1L;

		AccessesProvider()
		{
			setSort("accessed", SortOrder.DESCENDING);
		}

		@Override
		public Iterator<? extends Access> iterator(long first, long count)
		{
			SortParam<String> sort = getSort();
			Sort order = Sort.by(sort.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC,
				sort.getProperty()).and(Sort.by(Sort.Direction.DESC, "id"));
			int size = (int)Math.max(1, count);
			return Accesses.get().page(PageRequest.of((int)(first / size), size, order)).iterator();
		}

		@Override
		public long size()
		{
			return Accesses.get().count();
		}

		@Override
		public IModel<Access> model(Access access)
		{
			return Model.of(access);
		}
	}
}
