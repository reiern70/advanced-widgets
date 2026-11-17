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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior.Location;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.icon.IIcon;

/**
 * The Excel export of the data tables, read back with Apache POI, as a spreadsheet reads it.
 */
class XlsxExportToolbarActionTest extends WicketTestCase
{
	private static final ZoneId ZONE = ZoneId.of("America/Panama");

	private ItemsProvider provider;

	private JsonBasedDynamicDataTable<Item, Long> table;

	private XlsxExportToolbarAction<Item, Long> xlsx;

	private void start(int items)
	{
		start(items, new XlsxExportToolbarAction<Item, Long>().setFileName("items.xlsx")
			.setZoneId(ZONE));
	}

	private void start(int items, XlsxExportToolbarAction<Item, Long> action)
	{
		tester.getSession().setLocale(Locale.US);
		provider = new ItemsProvider(items);
		xlsx = action;
		table = new JsonBasedDynamicDataTable<>("table", columns(), provider, 2);
		table.addToolbarAction(new CsvExportToolbarAction<>());
		table.addToolbarAction(xlsx);
		tester.startPage(new TablePage(table));
	}

	@Test
	void theButtonComesAfterTheCsvOneWithItsLabelAndIcon()
	{
		start(3);

		String page = tester.getLastResponseAsString();
		assertTrue(page.indexOf("ant-data-table-export-xlsx") > page.indexOf(
			"ant-data-table-export-csv"), page);
		TagTester link = TagTester.createTagByAttribute(page, "class",
			"ant-data-table-action ant-data-table-export-xlsx");
		assertEquals("Export to Excel", link.getAttribute("title"));
		assertEquals("Export to Excel", link.getAttribute("aria-label"));
		assertTrue(link.getValue().startsWith("<svg aria-hidden=\"true\""), link.getValue());
	}

	@Test
	void theLabelIsInTheLanguageOfTheSession()
	{
		for (Map.Entry<Locale, String> label : Map.of(Locale.forLanguageTag("es"), "Exportar a Excel",
			Locale.FRENCH, "Exporter vers Excel", Locale.GERMAN, "Nach Excel exportieren")
			.entrySet())
		{
			start(1);
			tester.getSession().setLocale(label.getKey());
			tester.startPage(tester.getLastRenderedPage());

			assertTrue(tester.getLastResponseAsString().contains("aria-label=\"" +
				label.getValue() + "\""), label.getKey().toString());
		}
	}

	@Test
	void theLabelAndTheIconCanBeOwn()
	{
		IIcon icon = () -> "<i class=\"own-icon\"></i>";
		start(1, new XlsxExportToolbarAction<Item, Long>().setLabel(Model.of("Excel"))
			.setIcon(icon));

		assertEquals("Excel", xlsx.getLabel().getObject());
		assertEquals(icon, xlsx.getIcon());
		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("aria-label=\"Excel\""), page);
		assertTrue(page.contains("<i class=\"own-icon\"></i>"), page);
	}

	@Test
	void theDefaultsAreThoseOfTheCsvExport()
	{
		XlsxExportToolbarAction<Item, Long> action = new XlsxExportToolbarAction<>();

		assertEquals("export.xlsx", action.getFileName());
		assertEquals(Location.Blob, action.getLocation());
		assertFalse(action.isExportFooter());
		assertNull(action.getTitle());
		assertNull(action.getCaption());
		assertNull(action.getZoneId());
		assertEquals(XlsxWriter.MAX_ROWS, action.getMaxRows());
		assertEquals(Location.IFrame, action.setLocation(Location.IFrame).getLocation());
		assertThrows(IllegalArgumentException.class, () -> action.setFileName(""));
		assertThrows(IllegalArgumentException.class, () -> action.setLabel(null));
		assertThrows(IllegalArgumentException.class, () -> action.setIcon(null));
		assertThrows(IllegalArgumentException.class, () -> action.setLocation(null));
	}

	@Test
	void theFileIsAWorkbookNamedAsTheActionSays() throws IOException
	{
		start(3);

		byte[] file = exportXlsx();

		assertEquals(XlsxExportToolbarAction.CONTENT_TYPE,
			tester.getLastResponse().getContentType());
		assertTrue(tester.getLastResponse().getHeader("Content-Disposition").contains(
			"items.xlsx"), tester.getLastResponse().getHeader("Content-Disposition"));
		try (XSSFWorkbook workbook = read(file))
		{
			assertEquals(1, workbook.getNumberOfSheets());
			assertEquals("items", workbook.getSheetName(0), "after the file, without .xlsx");
		}
	}

	@Test
	void theCellsHaveTheTypesOfTheirValues() throws IOException
	{
		start(3);

		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			assertEquals(List.of("Id", "Name", "Amount", "Rate", "Born", "Paid", "Seen", "Sent",
				"Kind", "Code"), texts(sheet.getRow(0)));
			Row first = sheet.getRow(1);

			assertEquals(CellType.NUMERIC, first.getCell(0).getCellType());
			assertEquals(1, first.getCell(0).getNumericCellValue());
			assertEquals("General", first.getCell(0).getCellStyle().getDataFormatString());

			assertEquals(CellType.STRING, first.getCell(1).getCellType());
			assertEquals("Ítem \"1\" — ñandú", first.getCell(1).getStringCellValue());

			assertEquals(CellType.NUMERIC, first.getCell(2).getCellType());
			assertEquals(1234.5, first.getCell(2).getNumericCellValue());
			assertEquals("#,##0.00", first.getCell(2).getCellStyle().getDataFormatString());

			assertEquals(0.07, first.getCell(3).getNumericCellValue());
			assertEquals("0.00%", first.getCell(3).getCellStyle().getDataFormatString());

			Cell born = first.getCell(4);
			assertEquals(CellType.NUMERIC, born.getCellType());
			assertTrue(DateUtil.isCellDateFormatted(born));
			assertEquals(LocalDate.of(2001, 1, 1), born.getLocalDateTimeCellValue().toLocalDate());

			assertEquals(CellType.BOOLEAN, first.getCell(5).getCellType());
			assertFalse(first.getCell(5).getBooleanCellValue());
			assertTrue(sheet.getRow(2).getCell(5).getBooleanCellValue());

			Cell seen = first.getCell(6);
			assertTrue(DateUtil.isCellDateFormatted(seen));
			assertEquals(LocalDateTime.of(2026, 11, 1, 8, 30), seen.getLocalDateTimeCellValue());

			Cell sent = first.getCell(7);
			assertTrue(DateUtil.isCellDateFormatted(sent));
			assertEquals(LocalDateTime.of(2026, 11, 1, 7, 0), sent.getLocalDateTimeCellValue(),
				"an instant in the zone of the action");

			assertEquals(CellType.STRING, first.getCell(8).getCellType());
			assertEquals("SMALL", first.getCell(8).getStringCellValue(),
				"converted to its text");

			assertEquals(CellType.STRING, first.getCell(9).getCellType());
			assertEquals("1234567890123456789", first.getCell(9).getStringCellValue(),
				"too long a whole number for Excel keeps its digits");
		}
	}

	@Test
	void amountsAddUpAsNumbers() throws IOException
	{
		start(3);

		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			double total = 0;
			for (int row = 1; row <= 3; row++)
			{
				total += sheet.getRow(row).getCell(2).getNumericCellValue();
			}
			assertEquals(1234.5 + 2469 + 3703.5, total, 0.001);
		}
	}

	@Test
	void aTextIsNeverAFormula() throws IOException
	{
		start(3);
		provider.items.get(0).name = "=1+2";
		provider.items.get(1).name = "@SUM(A1:A2)";
		provider.items.get(2).name = "-12.5";

		byte[] file = exportXlsx();

		String sheet = XlsxWriterTest.part(file, "xl/worksheets/sheet1.xml");
		assertFalse(sheet.contains("<f>"), sheet);
		try (XSSFWorkbook workbook = read(file))
		{
			Row first = workbook.getSheetAt(0).getRow(1);
			assertEquals(CellType.STRING, first.getCell(1).getCellType());
			assertEquals("=1+2", first.getCell(1).getStringCellValue());
			assertEquals("@SUM(A1:A2)",
				workbook.getSheetAt(0).getRow(2).getCell(1).getStringCellValue());
			assertEquals("-12.5", workbook.getSheetAt(0).getRow(3).getCell(1)
				.getStringCellValue(), "a text that looks like a number stays a text");
		}
	}

	@Test
	void theHeaderIsBoldFrozenAndFiltered() throws IOException
	{
		start(3);

		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			assertTrue(workbook.getFontAt(sheet.getRow(0).getCell(0).getCellStyle()
				.getFontIndex()).getBold());
			assertFalse(workbook.getFontAt(sheet.getRow(1).getCell(1).getCellStyle()
				.getFontIndex()).getBold());
			assertTrue(sheet.getPaneInformation().isFreezePane());
			assertEquals(1, sheet.getPaneInformation().getHorizontalSplitPosition());
			assertEquals("A1:J4", sheet.getCTWorksheet().getAutoFilter().getRef());
			assertEquals("'items'!$A$1:$J$4",
				workbook.getName("_xlnm._FilterDatabase").getRefersToFormula());
		}
	}

	@Test
	void theColumnsAreAsWideAsTheirHeaderAndValues() throws IOException
	{
		start(3);
		provider.items.get(1).name = "A name much longer than its header, Name";

		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			int id = sheet.getColumnWidth(0);
			int name = sheet.getColumnWidth(1);
			assertTrue(name > 40 * 256, String.valueOf(name));
			assertTrue(id < name / 3, id + " " + name);
		}
	}

	@Test
	void theSelectedRowsAreTheOnesWritten() throws IOException
	{
		start(3);
		select("2");

		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			assertEquals(1, sheet.getLastRowNum());
			assertEquals(2, sheet.getRow(1).getCell(0).getNumericCellValue());
		}
	}

	@Test
	void theColumnsAreThoseShownInTheirOrder() throws IOException
	{
		start(2);
		List<IDynamicColumn<Item>> columns = table.getColumns();
		// Name first, Amount hidden
		table.moveColumn(columns.get(2), 0);
		table.setColumnVisible(columns.get(3), false);
		tester.startPage(tester.getLastRenderedPage());

		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			assertEquals(List.of("Name", "Id", "Rate", "Born", "Paid", "Seen", "Sent", "Kind",
				"Code"), texts(workbook.getSheetAt(0).getRow(0)));
			assertEquals("Ítem \"1\" — ñandú",
				workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
		}
	}

	@Test
	void theFooterEndsTheSheetInBoldOutOfTheFilter() throws IOException
	{
		start(3, new XlsxExportToolbarAction<Item, Long>().setExportFooter(true));

		assertTrue(xlsx.isExportFooter());
		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			assertEquals(4, sheet.getLastRowNum());
			Row footer = sheet.getRow(4);
			assertEquals(7407, footer.getCell(2).getNumericCellValue());
			assertEquals("#,##0.00", footer.getCell(2).getCellStyle().getDataFormatString());
			assertTrue(workbook.getFontAt(footer.getCell(2).getCellStyle().getFontIndex())
				.getBold());
			assertEquals(CellType.STRING, footer.getCell(1).getCellType());
			assertEquals("3 items", footer.getCell(1).getStringCellValue());
			assertNull(footer.getCell(0), "an empty cell for a column without one");
			assertEquals("A1:J4", sheet.getCTWorksheet().getAutoFilter().getRef());
		}
	}

	@Test
	void theSheetHasNoFooterByDefaultNorUnderTheSelectedRows() throws IOException
	{
		start(3);
		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			assertEquals(3, workbook.getSheetAt(0).getLastRowNum());
		}

		start(3, new XlsxExportToolbarAction<Item, Long>().setExportFooter(true));
		select("1");
		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
		}
	}

	@Test
	void theSheetIsNamedAfterTheTitleWithACaptionAboveTheHeader() throws IOException
	{
		IModel<List<String>> caption = () -> List.of("Items: November", "Made today");
		start(2, new XlsxExportToolbarAction<Item, Long>().setTitle(Model.of("Items: November"))
			.setCaption(caption)
			.setExportFooter(true));

		assertEquals("Items: November", xlsx.getTitle().getObject());
		assertEquals(caption, xlsx.getCaption());
		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			assertEquals("Items  November", sheet.getSheetName(), "without its colon");
			assertEquals("Items: November", sheet.getRow(0).getCell(0).getStringCellValue());
			assertTrue(workbook.getFontAt(sheet.getRow(0).getCell(0).getCellStyle()
				.getFontIndex()).getBold());
			assertEquals("Made today", sheet.getRow(1).getCell(0).getStringCellValue());
			assertNull(sheet.getRow(2).getCell(0), "an empty row under the caption");
			assertEquals("Id", sheet.getRow(3).getCell(0).getStringCellValue());
			assertEquals(4, sheet.getPaneInformation().getHorizontalSplitPosition());
			assertEquals("A4:J6", sheet.getCTWorksheet().getAutoFilter().getRef());
			assertEquals(1, sheet.getRow(4).getCell(0).getNumericCellValue());
			assertEquals("2 items", sheet.getRow(6).getCell(1).getStringCellValue());
		}
	}

	@Test
	void theTitleAndTheCaptionAreDetachedWithTheButton()
	{
		DetachableText title = new DetachableText("Items");
		start(1, new XlsxExportToolbarAction<Item, Long>().setTitle(title));

		tester.startPage(tester.getLastRenderedPage());

		assertTrue(title.detached);
	}

	@Test
	void aSheetHoldsTheRowsExcelDoesAndLeavesTheFooterOutWhenItCannotHoldThemAll() throws IOException
	{
		start(5, new XlsxExportToolbarAction<Item, Long>().setExportFooter(true).setMaxRows(4));

		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			assertEquals(3, sheet.getLastRowNum(), "the header and three rows");
			assertEquals(3, sheet.getRow(3).getCell(0).getNumericCellValue());
		}

		start(3, new XlsxExportToolbarAction<Item, Long>().setExportFooter(true).setMaxRows(5));
		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			assertEquals(4, workbook.getSheetAt(0).getLastRowNum(), "room for all and the footer");
			assertEquals(7407, workbook.getSheetAt(0).getRow(4).getCell(2).getNumericCellValue());
		}
	}

	@Test
	void manyRowsAreReadAPageAtATime() throws IOException
	{
		start(30_000);

		byte[] file = exportXlsx();

		assertTrue(provider.largestCount <= 100, String.valueOf(provider.largestCount));
		try (XSSFWorkbook workbook = read(file))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			assertEquals(30_000, sheet.getLastRowNum());
			assertEquals(30_000, sheet.getRow(30_000).getCell(0).getNumericCellValue());
		}
	}

	@Test
	void aBeanTableWritesItsColumnsWithTheirFormatsAndFooter() throws IOException
	{
		tester.getSession().setLocale(Locale.US);
		provider = new ItemsProvider(2);
		List<IColumn<Item, String>> columns = new ArrayList<>();
		columns.add(new BeanRowNumberColumn<>(Model.of("#")));
		columns.add(new PropertyColumn<>(Model.of("Name"), "name"));
		columns.add(new AmountColumn(provider));
		BeanBasedDynamicDataTable<Item, Long, String> beanTable = new BeanBasedDynamicDataTable<>(
			"table", columns, provider, 2);
		beanTable.addToolbarAction(new XlsxExportToolbarAction<Item, Long>()
			.setExportFooter(true));
		tester.startPage(new TablePage(beanTable));

		try (XSSFWorkbook workbook = read(exportXlsx()))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			assertEquals(List.of("Name", "Amount"), texts(sheet.getRow(0)));
			assertEquals(2469, sheet.getRow(2).getCell(1).getNumericCellValue());
			assertEquals("#,##0.00", sheet.getRow(2).getCell(1).getCellStyle()
				.getDataFormatString());
			assertEquals(3703.5, sheet.getRow(3).getCell(1).getNumericCellValue());
			assertEquals("A1:B3", sheet.getCTWorksheet().getAutoFilter().getRef());
		}
	}

	@Test
	void sortableColumnsDelegateTheirExportFormat()
	{
		SortableDynamicColumn<Item, String> sortable = new SortableDynamicColumn<>(
			new DynamicColumn<Item>(Model.of("Amount"), "{{amount}}").setExportFormat("0.0"),
			"amount");

		assertEquals("0.0", sortable.getExportFormat());
		assertNull(new DynamicColumn<Item>(Model.of("Name"), "{{name}}").getExportFormat());
	}

	private static List<String> texts(Row row)
	{
		List<String> texts = new ArrayList<>();
		for (Cell cell : row)
		{
			texts.add(cell.getStringCellValue());
		}
		return texts;
	}

	static XSSFWorkbook read(byte[] file)
	{
		try
		{
			return new XSSFWorkbook(new ByteArrayInputStream(file));
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private byte[] exportXlsx()
	{
		Component button = tester.getLastRenderedPage()
			.visitChildren(XlsxExportButton.class, (component, visit) -> visit.stop(component));
		tester.clickLink(((MarkupContainer)button).get("link"));
		assertTrue(tester.getLastResponseAsString().contains("Wicket.AjaxDownload.initiate("));
		Behavior download = button.getBehaviors()
			.stream()
			.filter(behavior -> behavior.getClass().getName().endsWith("$ResourceBehavior"))
			.findFirst()
			.orElseThrow();
		tester.invokeListener(button, download);
		return tester.getLastResponse().getBinaryContent();
	}

	private void select(String key)
	{
		BaseDynamicDataTable<?, ?, ?> shown = (BaseDynamicDataTable<?, ?, ?>)tester
			.getComponentFromLastRenderedPage("table");
		tester.getRequest().getPostParameters().setParameterValue("action",
			BaseDynamicDataTable.SELECT_ACTION);
		tester.getRequest().getPostParameters().setParameterValue("key", key);
		tester.executeBehavior(shown.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
		tester.startPage(tester.getLastRenderedPage());
	}

	private List<IDynamicColumn<Item>> columns()
	{
		List<IDynamicColumn<Item>> columns = new ArrayList<>();
		columns.add(new SelectionColumn<>());
		columns.add(new DynamicColumn<Item>(Model.of("Id"), "{{id}}").setExportValue(Item::getId));
		columns.add(new SortableDynamicColumn<>(new DynamicColumn<Item>(Model.of("Name"),
			"{{name}}").setExportValue(Item::getName)
				.setFooterTemplate("{{count}}")
				.setFooterExportValue(footer -> ((Map<?, ?>)footer).get("count")), "name"));
		columns.add(new DynamicColumn<Item>(Model.of("Amount"), "{{amount}}")
			.setExportValue(Item::getAmount)
			.setExportFormat("#,##0.00")
			.setFooterTemplate("{{total}}")
			.setFooterExportValue(footer -> ((Map<?, ?>)footer).get("total")));
		columns.add(new DynamicColumn<Item>(Model.of("Rate"), "{{rate}}")
			.setExportValue(item -> new BigDecimal("0.07"))
			.setExportFormat("0.00%"));
		columns.add(new DynamicColumn<Item>(Model.of("Born"), "{{born}}")
			.setExportValue(Item::getBorn));
		columns.add(new DynamicColumn<Item>(Model.of("Paid"), "{{paid}}")
			.setExportValue(item -> item.getId() % 2 == 0));
		columns.add(new DynamicColumn<Item>(Model.of("Seen"), "{{seen}}")
			.setExportValue(item -> LocalDateTime.of(2026, 11, 1, 8, 30)
				.plusDays(item.getId() - 1)));
		columns.add(new DynamicColumn<Item>(Model.of("Sent"), "{{sent}}")
			.setExportValue(item -> Instant.parse("2026-11-01T12:00:00Z")
				.plus(Duration.ofDays(item.getId() - 1))));
		columns.add(new DynamicColumn<Item>(Model.of("Kind"), "{{kind}}")
			.setExportValue(item -> Kind.SMALL));
		columns.add(new DynamicColumn<Item>(Model.of("Code"), "{{code}}")
			.setExportValue(item -> 1234567890123456789L));
		columns.add(new DynamicColumn<>(Model.of("Not exported"), "-"));
		return columns;
	}

	enum Kind
	{
		SMALL
	}

	public static class Item implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private final long id;

		String name;

		Item(long id)
		{
			this.id = id;
		}

		public long getId()
		{
			return id;
		}

		public String getName()
		{
			return name != null ? name : "Ítem \"" + id + "\" — ñandú";
		}

		public BigDecimal getAmount()
		{
			return new BigDecimal("1234.50").multiply(BigDecimal.valueOf(id));
		}

		public LocalDate getBorn()
		{
			return LocalDate.of(2001, 1, (int)Math.min(28, id));
		}
	}

	private static class ItemsProvider extends SortableDynamicDataProvider<Item, Long, String>
		implements
			IFooterDataProvider
	{
		private static final long serialVersionUID = 1L;

		final List<Item> items = new ArrayList<>();

		long largestCount;

		ItemsProvider(int size)
		{
			super(Long.class);
			for (long id = 1; id <= size; id++)
			{
				items.add(new Item(id));
			}
		}

		@Override
		public Long keyOf(Item item)
		{
			return item.getId();
		}

		@Override
		public Iterator<Item> iterator(long first, long count)
		{
			largestCount = Math.max(largestCount, count);
			return new ArrayList<>(items.subList((int)first, (int)(first + count))).iterator();
		}

		@Override
		public long size()
		{
			return items.size();
		}

		@Override
		public Item findByKey(Long key)
		{
			return items.stream().filter(item -> item.getId() == key).findFirst().orElse(null);
		}

		@Override
		public Object footer()
		{
			BigDecimal total = BigDecimal.ZERO;
			for (Item item : items)
			{
				total = total.add(item.getAmount());
			}
			return Map.of("count", items.size() + " items", "total", total);
		}
	}

	/** A column of a bean table with the amounts, two decimals in Excel. */
	private static class AmountColumn extends PropertyColumn<Item, String>
		implements
			IExportFormatColumn,
			IFooterColumn<Item>
	{
		private static final long serialVersionUID = 1L;

		private final ItemsProvider provider;

		AmountColumn(ItemsProvider provider)
		{
			super(Model.of("Amount"), "amount");
			this.provider = provider;
		}

		@Override
		public String getExportFormat()
		{
			return "#,##0.00";
		}

		@Override
		public Component newFooter(String id)
		{
			return new Label(id, "total");
		}

		@Override
		public Object getFooterExportValue()
		{
			return ((Map<?, ?>)provider.footer()).get("total");
		}
	}

	private static class DetachableText implements IModel<String>
	{
		private static final long serialVersionUID = 1L;

		private final String text;

		boolean detached;

		DetachableText(String text)
		{
			this.text = text;
		}

		@Override
		public String getObject()
		{
			return text;
		}

		@Override
		public void detach()
		{
			detached = true;
		}
	}

	public static class TablePage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TablePage(BaseDynamicDataTable<Item, Long, ?> table)
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
