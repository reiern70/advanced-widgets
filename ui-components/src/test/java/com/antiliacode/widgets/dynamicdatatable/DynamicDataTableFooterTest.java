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
package com.antiliacode.widgets.dynamicdatatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.antiliacode.widgets.table.IMovableColumn;
import com.github.openjson.JSONObject;

/**
 * The footer row of the dynamic data tables: the cells of {@link IFooterColumn}s in a bean based
 * table, and those of columns with a footer template in a JSON based one.
 */
class DynamicDataTableFooterTest extends WicketTestCase
{
	private static final ResourceReference LINES = new ResourceReference(
		DynamicDataTableFooterTest.class, "lines")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new LinesResource();
		}
	};

	private LinesProvider provider;

	private BeanBasedDynamicDataTable<Line, Long, String> beanTable;

	private JsonBasedDynamicDataTable<Line, Long> jsonTable;

	/** The CSV export the next table started gets, if any. */
	private CsvExportToolbarAction<Line, Long> csv;

	@Override
	protected WicketTester newWicketTester(WebApplication app)
	{
		WicketTester tester = super.newWicketTester(app);
		app.mountResource("api/lines", LINES);
		return tester;
	}

	@Test
	void aFooterColumnRendersAFooterRowWithACellPerShownColumn()
	{
		startBeanTable(true);

		TagTester footer = tester.getTagByWicketId("footerRow");
		assertEquals("tr", footer.getName());
		assertTrue(footer.getAttribute("class").contains("ant-data-table-footer"),
			footer.getAttribute("class"));
		List<TagTester> cells = footerCells();
		assertEquals(3, cells.size(), "one cell per shown column");
		assertEquals("amount", cells.get(2).getAttribute("class"));
		assertTrue(cells.get(2).getValue().contains("Total 60"), cells.get(2).getValue());
		assertFalse(cells.get(1).getValue().contains("Total"), "the other cells are empty");
		assertFalse(footer.getValue().contains("ant-data-table-filler"),
			"no empty cell at the end, as the rows have none");
	}

	@Test
	void theFooterRowIsInTheFooterOfTheTableAboveTheBottomToolbars()
	{
		provider = new LinesProvider();
		beanTable = newBeanTable(true);
		beanTable.setNavigationPosition(ToolbarPosition.BOTTOM);
		tester.startPage(new TablePage(beanTable));

		String page = tester.getLastResponseAsString();
		int footer = page.indexOf("wicket:id=\"footerRow\"");
		assertTrue(page.indexOf("<tfoot>") < footer, page);
		assertTrue(footer < page.indexOf("wicket:id=\"bottomNavigation\""), page);
	}

	@Test
	void theFooterRowEndsWithTheEmptyCellOfTheRows()
	{
		provider = new LinesProvider();
		beanTable = newBeanTable(true);
		beanTable.setBodyHeight(200, BodyHeightUnits.PIXELS);
		tester.startPage(new TablePage(beanTable));

		assertTrue(tester.getTagByWicketId("footerRow").getValue()
			.contains("class=\"ant-data-table-filler\""), tester.getLastResponseAsString());
		assertEquals(beanTable.getCellCount(),
			tester.getTagByWicketId("footerRow").getValue().split("<td ", -1).length - 1);
	}

	@Test
	void movingOrHidingAColumnMovesOrHidesItsFooterCell()
	{
		startBeanTable(true);
		IColumn<Line, String> item = beanTable.getColumns().get(1);
		IColumn<Line, String> amount = beanTable.getColumns().get(2);

		beanTable.moveColumn(amount, 1);
		tester.startPage(tester.getLastRenderedPage());
		assertTrue(footerCells().get(1).getValue().contains("Total 60"),
			tester.getLastResponseAsString());

		beanTable.setColumnVisible(item, false);
		tester.startPage(tester.getLastRenderedPage());
		assertEquals(2, footerCells().size());
		assertTrue(footerCells().get(1).getValue().contains("Total 60"));

		beanTable.setColumnVisible(amount, false);
		tester.startPage(tester.getLastRenderedPage());
		assertFalse(beanTable.hasFooter());
		tester.assertContainsNot(Pattern.quote("ant-data-table-footer"));
	}

	@Test
	void aTableWithoutFooterColumnsRendersNoFooterRow()
	{
		startBeanTable(false);

		assertFalse(beanTable.hasFooter());
		tester.assertContainsNot(Pattern.quote("ant-data-table-footer"));
		tester.assertContainsNot(Pattern.quote("Total"));

		tester.clickLink("table:topNavigation:span:paging:navigator:next");
		assertFalse(tester.getLastResponseAsString()
			.contains("<component id=\"" + beanTable.getFooterRow().getMarkupId() + "\""),
			"refreshing leaves the footer alone");
	}

	@Test
	void refreshingRendersTheFooterAgainWithTheRows()
	{
		startBeanTable(true);
		provider.lines.add(new Line(4, "Pen", 40));

		tester.clickLink("table:topNavigation:span:paging:navigator:next");

		tester.assertComponentOnAjaxResponse(beanTable.getRowsBody());
		tester.assertComponentOnAjaxResponse(beanTable.getFooterRow());
		assertTrue(tester.getLastResponseAsString().contains("Total 100"),
			tester.getLastResponseAsString());
	}

	@Test
	void aColumnWithAFooterTemplateGetsAnEmptyFooterCellTheBrowserFills()
	{
		startJsonTable(true);

		assertTrue(jsonTable.hasFooter());
		List<TagTester> cells = footerCells();
		assertEquals(2, cells.size());
		assertEquals("amount", cells.get(1).getAttribute("class"));
		assertFalse(cells.get(1).getValue().contains("60"), "the browser fills the cell");
		tester.assertContains(Pattern.quote(
			"\"footerTemplate\":\"\\u003cb\\u003e{{total}}\\u003c\\/b\\u003e\""));
	}

	@Test
	void theFooterDataOfTheProviderComesWithTheRows()
	{
		startJsonTable(true);

		tester.executeBehavior(dataBehavior());

		JSONObject json = new JSONObject(tester.getLastResponseAsString());
		assertEquals(60, json.getJSONObject("footer").getInt("total"));
	}

	@Test
	void noFooterDataComesWithoutFooterTemplates()
	{
		startJsonTable(false);

		assertFalse(jsonTable.hasFooter());
		tester.assertContainsNot(Pattern.quote("ant-data-table-footer"));
		tester.assertContainsNot(Pattern.quote("footerTemplate"));
		tester.executeBehavior(dataBehavior());
		assertFalse(new JSONObject(tester.getLastResponseAsString()).has("footer"));
	}

	@Test
	void refreshingSendsTheFooterDataWithTheRowsNotTheFooterRow()
	{
		startJsonTable(true);
		provider.lines.add(new Line(4, "Pen", 40));

		tester.clickLink("table:topNavigation:span:paging:navigator:next");

		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("\"footer\":{\"total\":100}"), response);
		assertFalse(response.contains(
			"<component id=\"" + jsonTable.getFooterRow().getMarkupId() + "\""), response);
	}

	@Test
	void theRowsResourceSendsTheFooterDataOfItsProvider()
	{
		// as the browser fetches the rows; a POST would create one
		tester.getRequest().setMethod("GET");
		tester.executeUrl("api/lines?first=0&count=2");

		JSONObject json = new JSONObject(tester.getLastResponseAsString());
		assertEquals(2, json.getJSONArray("rows").length());
		assertEquals(60, json.getJSONObject("footer").getInt("total"));
	}

	@Test
	void aCsvExportEndsWithTheFooterOfABeanTableWhenAskedTo()
	{
		csv = new CsvExportToolbarAction<Line, Long>().setExportFooter(true);
		startBeanTable(true);

		assertTrue(csv.isExportFooter());
		assertEquals("\"Item\",\"Amount\"\r\n\"Paper\",\"10\"\r\n\"Ink\",\"20\"\r\n" +
			"\"Stamps\",\"30\"\r\n\"\",\"60\"\r\n", exportCsv());
	}

	@Test
	void aCsvExportHasNoFooterByDefault()
	{
		csv = new CsvExportToolbarAction<>();
		startBeanTable(true);

		assertFalse(csv.isExportFooter());
		assertTrue(exportCsv().endsWith("\"Stamps\",\"30\"\r\n"), tester.getLastResponseAsString());
	}

	@Test
	void aCsvExportOfTheSelectedRowsHasNoFooterAsItSumsUpAllRows()
	{
		csv = new CsvExportToolbarAction<Line, Long>().setExportFooter(true);
		startBeanTable(true);
		select(beanTable, "2");

		assertEquals("\"Item\",\"Amount\"\r\n\"Ink\",\"20\"\r\n", exportCsv());
	}

	@Test
	void aCsvExportWritesNoFooterForATableWithoutOne()
	{
		csv = new CsvExportToolbarAction<Line, Long>().setExportFooter(true);
		startBeanTable(false);

		assertTrue(exportCsv().endsWith("\"Stamps\",\"30\"\r\n"), tester.getLastResponseAsString());
	}

	@Test
	void aCsvExportWritesTheFooterOfAJsonTableFromItsFooterData()
	{
		csv = new CsvExportToolbarAction<Line, Long>().setExportFooter(true);
		startJsonTable(true);

		assertEquals("\"Item\",\"Amount\"\r\n\"Paper\",\"10\"\r\n\"Ink\",\"20\"\r\n" +
			"\"Stamps\",\"30\"\r\n\"\",\"60\"\r\n", exportCsv());
	}

	@Test
	void aJsonColumnWithAFooterTemplateButNoFooterExportValueLeavesItsFieldEmpty()
	{
		provider = new LinesProvider();
		List<IDynamicColumn<Line>> columns = new ArrayList<>();
		columns.add(new DynamicColumn<Line>(Model.of("Item"), "{{item}}")
			.setExportValue(Line::getItem)
			.setFooterTemplate("{{count}}"));
		columns.add(new SortableDynamicColumn<>(new DynamicColumn<Line>(Model.of("Amount"),
			"{{amount}}").setExportValue(Line::getAmount)
				.setFooterTemplate("<b>{{total}}</b>")
				.setFooterExportValue(footer -> ((Map<?, ?>)footer).get("total")), "amount"));
		jsonTable = new JsonBasedDynamicDataTable<>("table", columns, provider, 2);
		jsonTable.addToolbarAction(new CsvExportToolbarAction<Line, Long>().setExportFooter(true));
		tester.startPage(new TablePage(jsonTable));

		assertTrue(exportCsv().endsWith("\"Stamps\",\"30\"\r\n\"\",\"60\"\r\n"),
			"a sortable column gives the footer export value of the column it sorts: " +
				tester.getLastResponseAsString());
	}

	private String exportCsv()
	{
		Component button = tester.getLastRenderedPage()
			.visitChildren(CsvExportButton.class, (component, visit) -> visit.stop(component));
		tester.clickLink(((MarkupContainer)button).get("link"));
		Behavior download = button.getBehaviors()
			.stream()
			.filter(behavior -> behavior.getClass().getName().endsWith("$ResourceBehavior"))
			.findFirst()
			.orElseThrow();
		tester.invokeListener(button, download);
		return tester.getLastResponseAsString();
	}

	private void select(BaseDynamicDataTable<Line, Long, ?> table, String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action",
			BaseDynamicDataTable.SELECT_ACTION);
		tester.getRequest().getPostParameters().setParameterValue("key", key);
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private void startBeanTable(boolean footer)
	{
		provider = new LinesProvider();
		beanTable = newBeanTable(footer);
		tester.startPage(new TablePage(beanTable));
	}

	private BeanBasedDynamicDataTable<Line, Long, String> newBeanTable(boolean footer)
	{
		List<IColumn<Line, String>> columns = new ArrayList<>();
		columns.add(new BeanRowNumberColumn<>(Model.of("#")));
		columns.add(new LineColumn("Item", "item"));
		columns.add(footer ? new AmountColumn(provider) : new LineColumn("Amount", "amount"));
		BeanBasedDynamicDataTable<Line, Long, String> table = new BeanBasedDynamicDataTable<>(
			"table", columns, provider, 2);
		if (csv != null)
		{
			table.addToolbarAction(csv);
		}
		return table;
	}

	private void startJsonTable(boolean footer)
	{
		provider = new LinesProvider();
		List<IDynamicColumn<Line>> columns = new ArrayList<>();
		columns.add(new DynamicColumn<Line>(Model.of("Item"), "{{item}}")
			.setExportValue(Line::getItem));
		columns.add(new DynamicColumn<Line>(Model.of("Amount"), "{{amount}}")
			.setFooterTemplate(footer ? "<b>{{total}}</b>" : null)
			.setFooterExportValue(data -> ((Map<?, ?>)data).get("total"))
			.setExportValue(Line::getAmount)
			.setCssClass("amount"));
		jsonTable = new JsonBasedDynamicDataTable<>("table", columns, provider, 2);
		if (csv != null)
		{
			jsonTable.addToolbarAction(csv);
		}
		tester.startPage(new TablePage(jsonTable));
	}

	private List<TagTester> footerCells()
	{
		return TagTester.createTagsByAttribute(tester.getLastResponseAsString(), "wicket:id",
			"footers", false);
	}

	private AbstractAjaxBehavior dataBehavior()
	{
		return jsonTable.getBehaviors(AbstractAjaxBehavior.class)
			.stream()
			.filter(behavior -> !(behavior instanceof AbstractDefaultAjaxBehavior))
			.findFirst()
			.orElseThrow();
	}

	public static class Line implements Serializable, IIdentifiable<Long>
	{
		private static final long serialVersionUID = 1L;

		private final long id;

		private final String item;

		private final int amount;

		Line(long id, String item, int amount)
		{
			this.id = id;
			this.item = item;
			this.amount = amount;
		}

		@Override
		public Long getId()
		{
			return id;
		}

		public String getItem()
		{
			return item;
		}

		public int getAmount()
		{
			return amount;
		}
	}

	private static class LinesProvider extends SortableDynamicDataProvider<Line, Long, String>
		implements
			IIdentifiableDynamicDataProvider<Line, Long>,
			IFooterDataProvider
	{
		private static final long serialVersionUID = 1L;

		final List<Line> lines = new ArrayList<>(List.of(new Line(1, "Paper", 10),
			new Line(2, "Ink", 20), new Line(3, "Stamps", 30)));

		LinesProvider()
		{
			super(Long.class);
		}

		@Override
		public Iterator<Line> iterator(long first, long count)
		{
			return lines.subList((int)first, (int)Math.min(lines.size(), first + count))
				.iterator();
		}

		@Override
		public long size()
		{
			return lines.size();
		}

		@Override
		public Line findByKey(Long key)
		{
			return lines.stream().filter(line -> line.getId().equals(key)).findFirst()
				.orElse(null);
		}

		int total()
		{
			return lines.stream().mapToInt(Line::getAmount).sum();
		}

		@Override
		public Object footer()
		{
			return Map.of("total", total());
		}
	}

	private static class LineColumn extends PropertyColumn<Line, String>
		implements
			IMovableColumn,
			IHideableColumn
	{
		private static final long serialVersionUID = 1L;

		LineColumn(String title, String property)
		{
			super(Model.of(title), property);
		}
	}

	private static class AmountColumn extends LineColumn implements IFooterColumn<Line>
	{
		private static final long serialVersionUID = 1L;

		private final LinesProvider provider;

		AmountColumn(LinesProvider provider)
		{
			super("Amount", "amount");
			this.provider = provider;
		}

		@Override
		public String getCssClass()
		{
			return "amount";
		}

		@Override
		public Component newFooter(String id)
		{
			return new Label(id, () -> "Total " + provider.total());
		}

		@Override
		public Object getFooterExportValue()
		{
			return provider.total();
		}
	}

	private static class LinesResource extends JsonRowsResource<Line, Long>
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected IDynamicDataProvider<Line, Long> newProvider(PageParameters parameters)
		{
			return new LinesProvider();
		}
	}

	public static class TablePage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TablePage(BaseDynamicDataTable<Line, Long, ?> table)
		{
			add(table);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream(
				"<html><head></head><body><table wicket:id=\"table\"></table></body></html>");
		}
	}
}
