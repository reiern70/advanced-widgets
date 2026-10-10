/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.antilia.widgets.dynamicdatatable.ColumnChooserToolbarAction;
import com.antilia.widgets.dynamicdatatable.CsvExportToolbarAction;
import com.antilia.widgets.table.MovableColumnsBehavior;
import com.antilia.widgets.table.ResizableColumnsBehavior;

/**
 * What the tables of the administration share: their settings, and the page of the database a
 * page of the table is.
 */
final class AdminTables
{
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private AdminTables()
	{
	}

	/**
	 * Sets the table up: the navigation on top, the rows per page, choosing the columns,
	 * exporting the rows to CSV, and columns that are resized and moved.
	 *
	 * @param table
	 *            the table
	 * @param fileName
	 *            the name of the CSV file
	 * @return the table
	 */
	static <C extends BaseDynamicDataTable<T, Long, ?>, T> C configure(C table, String fileName)
	{
		table.setAdjustToParentWidth(true);
		table.setNavigationPosition(ToolbarPosition.TOP);
		table.setItemsPerPageOptions(25L, 50L, 100L);
		table.addToolbarAction(new ColumnChooserToolbarAction<>());
		table.addToolbarAction(new CsvExportToolbarAction<T, Long>().setFileName(fileName));
		table.add(new ResizableColumnsBehavior(ResizableColumnsBehavior.Mode.STRETCH));
		table.add(new MovableColumnsBehavior());
		return table;
	}

	/**
	 * @param first
	 *            the first row of the page of the table
	 * @param count
	 *            the rows of the page
	 * @param sort
	 *            the sort of the table, {@code null} for none
	 * @return the page of the database: sorted as the table, then by the id, the latest first
	 */
	static Pageable pageable(long first, long count, SortParam<String> sort)
	{
		Sort byId = Sort.by(Sort.Direction.DESC, "id");
		Sort order = sort == null ? byId
			: Sort.by(sort.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC,
				sort.getProperty()).and(byId);
		int size = (int)Math.max(1, count);
		return PageRequest.of((int)(first / size), size, order);
	}

	/**
	 * @return the time as the tables show it, on the server's clock in UTC
	 */
	static String format(Instant instant)
	{
		return instant == null ? "" : TIME.format(instant.atOffset(ZoneOffset.UTC)) + " UTC";
	}

	/**
	 * @return the time as the tables show it
	 */
	static String format(TemporalAccessor time)
	{
		return time == null ? "" : TIME.format(time);
	}
}
