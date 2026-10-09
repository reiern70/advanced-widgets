/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import com.antilia.widgets.table.ResizableColumnsBehavior.Mode;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class ResizableColumnsBehaviorTest extends WicketTestCase
{
	@Test
	void fitIsTheDefault()
	{
		assertEquals(Mode.FIT, new ResizableColumnsBehavior().getMode());
	}

	@Test
	void attachesTheScriptToTheTable()
	{
		TablePage page = new TablePage("table", new ResizableColumnsBehavior(Mode.STRETCH)
			.setMinColumnWidth(50));
		tester.startPage(page);

		String id = page.get("table").getMarkupId();
		tester.assertContains(Pattern.quote("var options = {\"mode\":\"stretch\",\"minWidth\":50};" +
			"Wicket.ResizableColumns.attach(\"" + id + "\", options);"));
		tester.assertContains("antilia-resizable-columns.*\\.js");
		tester.assertContains("antilia-resizable-columns.*\\.css");
		assertTrue(tester.getLastResponseAsString().contains("id=\"" + id + "\""));
	}

	@Test
	void fitModeIsWrittenInLowerCase()
	{
		tester.startPage(new TablePage("table", new ResizableColumnsBehavior()));

		tester.assertContains(Pattern.quote("{\"mode\":\"fit\",\"minWidth\":80}"));
	}

	@Test
	void aTableKeepingTheWidthsGetsThemAndIsToldOfEveryResize()
	{
		WidthsTable table = new WidthsTable();
		tester.startPage(new WidthsPage(table));
		tester.assertContainsNot(Pattern.quote("options.widths"));
		tester.assertContains(Pattern.quote("options.onResize = function (widths)"));
		tester.assertContains(Pattern.quote("\"m\":\"POST\""));

		ResizableColumnsBehavior behavior = table.getBehaviors(ResizableColumnsBehavior.class)
			.get(0);
		for (String widths : new String[] { "40,120.5", "a,b", "-1,2", "Infinity,1", "1,1e9" })
		{
			tester.getRequest().getPostParameters().setParameterValue("widths", widths);
			tester.executeBehavior(behavior);
		}

		assertEquals(1, table.resized.size());
		assertEquals(List.of(40d, 120.5), table.resized.get(0));
		tester.startPage(tester.getLastRenderedPage());
		tester.assertContains(Pattern.quote("options.widths = [40,120.5];"));
	}

	@Test
	void reportingTheWidthsIsNotVeiled()
	{
		tester.startPage(new WidthsPage(new WidthsTable()));

		tester.assertContains("\"ep\":.*\"wicket_nb\"");
	}

	@Test
	void onlyTablesCanBeResized()
	{
		assertThrows(Exception.class,
			() -> tester.startPage(new TablePage("div", new ResizableColumnsBehavior())));
	}

	@Test
	void minimumWidthCannotBeNegative()
	{
		assertThrows(IllegalArgumentException.class,
			() -> new ResizableColumnsBehavior().setMinColumnWidth(-1));
	}

	private static class WidthsTable extends WebMarkupContainer implements IResizableColumnsTable
	{
		private static final long serialVersionUID = 1L;

		final List<List<Double>> resized = new ArrayList<>();

		WidthsTable()
		{
			super("table");
			add(new ResizableColumnsBehavior(Mode.STRETCH));
		}

		@Override
		public double[] getShownColumnWidths()
		{
			return resized.isEmpty() ? null
				: resized.get(resized.size() - 1).stream().mapToDouble(Double::doubleValue)
					.toArray();
		}

		@Override
		public void columnsResized(AjaxRequestTarget target, double[] widths)
		{
			List<Double> list = new ArrayList<>();
			for (double width : widths)
			{
				list.add(width);
			}
			resized.add(list);
		}
	}

	public static class WidthsPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		WidthsPage(WidthsTable table)
		{
			add(table);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><table wicket:id=\"table\">" +
				"<thead><tr><th>A</th><th>B</th></tr></thead></table></body></html>");
		}
	}

	public static class TablePage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TablePage(String id, ResizableColumnsBehavior behavior)
		{
			add(new WebMarkupContainer(id).add(behavior));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			String id = get("table") != null ? "table" : "div";
			String tag = "table".equals(id) ? "table" : "div";
			return new StringResourceStream("<html><head></head><body><" + tag +
				" wicket:id=\"" + id + "\"><thead><tr><th>A</th></tr></thead></" + tag +
				"></body></html>");
		}
	}
}
