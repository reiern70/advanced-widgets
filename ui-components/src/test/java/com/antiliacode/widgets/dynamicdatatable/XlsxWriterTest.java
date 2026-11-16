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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * The writer of the Excel workbooks: what it writes, read back as XML and with Apache POI.
 */
class XlsxWriterTest
{
	private static final ZoneId ZONE = ZoneId.of("America/Panama");

	@Test
	void everyPartIsWellFormedXml() throws Exception
	{
		byte[] file = write(writer -> {
			writer.caption(List.of("A <title> & more"));
			writer.header(List.of("Name", "Amount"), List.of("@", "#,##0.00"));
			writer.row(List.of("Ana & \"Bo\"", new BigDecimal("1.5")));
			writer.footer(Arrays.asList(null, new BigDecimal("1.5")));
		});

		List<String> names = new ArrayList<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(file)))
		{
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
			{
				names.add(entry.getName());
				DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
					new ByteArrayInputStream(zip.readAllBytes()));
			}
		}
		assertEquals(List.of("[Content_Types].xml", "_rels/.rels", "xl/_rels/workbook.xml.rels",
			"xl/worksheets/sheet1.xml", "xl/styles.xml", "xl/workbook.xml"), names);
		// a format only for the numbers and dates that have one
		assertTrue(part(file, "xl/styles.xml").contains("<numFmts count=\"1\"><numFmt " +
			"numFmtId=\"164\" formatCode=\"#,##0.00\"/></numFmts>"), part(file, "xl/styles.xml"));
	}

	@Test
	void textsKeepWhatXmlCannotHold() throws IOException
	{
		List<String> texts = List.of("a\u0001b", "_x0041_ stays", "line\r\nbreak", " spaced ",
			"emoji 😀", "half \uD800 pair", "tab\there", "￾");
		byte[] file = write(writer -> {
			writer.header(List.of("Text"), null);
			for (String text : texts)
			{
				writer.row(List.of(text));
			}
		});

		String sheet = part(file, "xl/worksheets/sheet1.xml");
		assertTrue(sheet.contains("a_x0001_b"), sheet);
		assertTrue(sheet.contains("_x005F_x0041_ stays"), sheet);
		assertTrue(sheet.contains("line_x000D_\nbreak"), sheet);
		assertTrue(sheet.contains("<t xml:space=\"preserve\"> spaced </t>"), sheet);
		try (XSSFWorkbook workbook = XlsxExportToolbarActionTest.read(file))
		{
			XSSFSheet read = workbook.getSheetAt(0);
			for (int i = 0; i < texts.size(); i++)
			{
				assertEquals(texts.get(i), read.getRow(i + 1).getCell(0).getStringCellValue(),
					texts.get(i));
			}
		}
	}

	@Test
	void aTextIsCutAtTheCharactersACellHolds() throws IOException
	{
		String longText = "x".repeat(XlsxWriter.MAX_CELL_LENGTH - 1) + "😀" + "rest";
		byte[] file = write(writer -> {
			writer.header(List.of("Text"), null);
			writer.row(List.of(longText));
			writer.row(List.of("y".repeat(XlsxWriter.MAX_CELL_LENGTH + 10)));
		});

		try (XSSFWorkbook workbook = XlsxExportToolbarActionTest.read(file))
		{
			String first = workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue();
			assertEquals(XlsxWriter.MAX_CELL_LENGTH - 1, first.length(),
				"without half of the emoji");
			assertEquals(XlsxWriter.MAX_CELL_LENGTH,
				workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue().length());
		}
	}

	@Test
	void datesAndTimesAreExcelsDays()
	{
		assertEquals("1", XlsxWriter.serial(LocalDate.of(1900, 1, 1).atStartOfDay()));
		assertEquals("59", XlsxWriter.serial(LocalDate.of(1900, 2, 28).atStartOfDay()));
		assertEquals("61", XlsxWriter.serial(LocalDate.of(1900, 3, 1).atStartOfDay()),
			"after the 29 February 1900 Excel counts");
		assertEquals("46341", XlsxWriter.serial(LocalDate.of(2026, 11, 15).atStartOfDay()));
		assertEquals("46341.5", XlsxWriter.serial(LocalDateTime.of(2026, 11, 15, 12, 0)));
		assertEquals("2958465", XlsxWriter.serial(LocalDate.of(9999, 12, 31).atStartOfDay()));
		assertNull(XlsxWriter.serial(LocalDate.of(1899, 12, 31).atStartOfDay()));
		assertNull(XlsxWriter.serial(LocalDate.of(10000, 1, 1).atStartOfDay()));
	}

	@Test
	void everyKindOfDateReadsBackAsADate() throws IOException
	{
		Instant instant = Instant.parse("2026-11-15T17:30:00Z");
		byte[] file = write(writer -> {
			writer.header(List.of("Date", "Own format", "Offset", "Zoned", "Date", "Timestamp",
				"Old"), Arrays.asList(null, "dd/mm/yyyy", null, null, null, null, null));
			writer.row(List.of(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 15),
				OffsetDateTime.of(2026, 11, 15, 9, 0, 0, 0, ZoneOffset.ofHours(2)),
				ZonedDateTime.of(2026, 11, 15, 10, 0, 0, 0, ZoneId.of("Europe/Madrid")),
				Date.from(instant), new Timestamp(instant.toEpochMilli()),
				LocalDate.of(1850, 6, 1)));
		});

		try (XSSFWorkbook workbook = XlsxExportToolbarActionTest.read(file))
		{
			var row = workbook.getSheetAt(0).getRow(1);
			for (int i = 0; i < 6; i++)
			{
				assertTrue(DateUtil.isCellDateFormatted(row.getCell(i)), String.valueOf(i));
			}
			assertEquals(LocalDateTime.of(2026, 11, 15, 0, 0), row.getCell(0)
				.getLocalDateTimeCellValue());
			assertEquals("dd/mm/yyyy", row.getCell(1).getCellStyle().getDataFormatString());
			assertEquals(LocalDateTime.of(2026, 11, 15, 9, 0), row.getCell(2)
				.getLocalDateTimeCellValue(), "at its own offset");
			assertEquals(LocalDateTime.of(2026, 11, 15, 10, 0), row.getCell(3)
				.getLocalDateTimeCellValue(), "in its own zone");
			assertEquals(LocalDateTime.of(2026, 11, 15, 12, 30), row.getCell(4)
				.getLocalDateTimeCellValue(), "in the zone of the writer");
			assertEquals(LocalDateTime.of(2026, 11, 15, 12, 30), row.getCell(5)
				.getLocalDateTimeCellValue());
			assertEquals(CellType.STRING, row.getCell(6).getCellType());
			assertEquals("1850-06-01", row.getCell(6).getStringCellValue(),
				"before Excel's first day");
		}
	}

	@Test
	void numbersAreWrittenAsExcelReadsThem()
	{
		assertEquals("5", XlsxWriter.number(5));
		assertEquals("-7", XlsxWriter.number((short)-7));
		assertEquals("3", XlsxWriter.number((byte)3));
		assertEquals("3", XlsxWriter.number(new AtomicInteger(3)));
		assertEquals("999999999999999", XlsxWriter.number(999_999_999_999_999L));
		assertEquals("-999999999999999", XlsxWriter.number(-999_999_999_999_999L));
		assertNull(XlsxWriter.number(1_000_000_000_000_000L));
		assertNull(XlsxWriter.number(new AtomicLong(Long.MIN_VALUE)));
		assertEquals("12", XlsxWriter.number(BigInteger.valueOf(12)));
		assertNull(XlsxWriter.number(BigInteger.TEN.pow(20)));
		assertEquals("1234.50", XlsxWriter.number(new BigDecimal("1234.50")));
		assertEquals("0.07", XlsxWriter.number(new BigDecimal("0.07")));
		assertEquals("1000", XlsxWriter.number(new BigDecimal("1E+3")));
		assertEquals("1E-40", XlsxWriter.number(new BigDecimal("1E-40")));
		assertEquals("0.33333333333333333",
			XlsxWriter.number(BigDecimal.ONE.divide(BigDecimal.valueOf(3), 40,
				java.math.RoundingMode.HALF_UP)), "the digits a double holds");
		assertNull(XlsxWriter.number(new BigDecimal("1E+400")));
		assertEquals("0.1", XlsxWriter.number(0.1));
		assertEquals("0.1", XlsxWriter.number(0.1f), "the digits of the float");
		assertNull(XlsxWriter.number(Double.NaN));
		assertNull(XlsxWriter.number(Float.POSITIVE_INFINITY));
		DoubleAdder adder = new DoubleAdder();
		adder.add(2.5);
		assertEquals("2.5", XlsxWriter.number(adder));
		assertEquals("4.25", XlsxWriter.number(new OddNumber(4.25)));
		assertNull(XlsxWriter.number(new OddNumber(Double.NaN)));
	}

	@Test
	void numbersBooleansAndTextsHaveTheirTypes() throws IOException
	{
		byte[] file = write(writer -> {
			writer.header(List.of("Number", "Boolean", "Not a number", "Other", "Nothing"),
				null);
			writer.row(Arrays.asList(12.5, true, Double.NaN, new StringBuilder("built"),
				new Object()));
			writer.footer(Arrays.asList(12.5, false, null, null, null));
		});

		try (XSSFWorkbook workbook = XlsxExportToolbarActionTest.read(file))
		{
			var row = workbook.getSheetAt(0).getRow(1);
			assertEquals(12.5, row.getCell(0).getNumericCellValue());
			assertTrue(row.getCell(1).getBooleanCellValue());
			assertEquals("NaN", row.getCell(2).getStringCellValue());
			assertEquals("built", row.getCell(3).getStringCellValue());
			assertNull(row.getCell(4), "a value the converter has no text for");
			var footer = workbook.getSheetAt(0).getRow(2);
			assertFalse(footer.getCell(1).getBooleanCellValue());
			assertTrue(workbook.getFontAt(footer.getCell(1).getCellStyle().getFontIndex())
				.getBold());
		}
	}

	@Test
	void theWidthsComeFromTheHeaderAndTheFirstRows() throws IOException
	{
		byte[] file = write(writer -> {
			writer.header(List.of("Amount", "Rate", "Note"),
				Arrays.asList("#,##0.00", "0.00%", "General"));
			writer.row(List.of(new BigDecimal("1234567.5"), new BigDecimal("0.07"), "short"));
			for (int i = 1; i < XlsxWriter.MEASURED_ROWS; i++)
			{
				writer.row(List.of(BigDecimal.ONE, BigDecimal.ONE, "short"));
			}
			// beyond the rows measured
			writer.row(List.of(BigDecimal.ONE, BigDecimal.ONE, "x".repeat(50)));
		});

		try (XSSFWorkbook workbook = XlsxExportToolbarActionTest.read(file))
		{
			XSSFSheet sheet = workbook.getSheetAt(0);
			// 1,234,567.50: twelve characters and some room
			assertEquals(13 * 256, sheet.getColumnWidth(0));
			assertTrue(sheet.getColumnWidth(1) >= 6 * 256);
			assertEquals(7 * 256, sheet.getColumnWidth(2), "Note, the filter's button and room");
			assertEquals("General", sheet.getRow(1).getCell(2).getCellStyle()
				.getDataFormatString());
			assertEquals(XlsxWriter.MEASURED_ROWS + 1, sheet.getLastRowNum());
		}
	}

	@Test
	void aSheetWithoutColumnsOrHeaderIsAWorkbookStill() throws IOException
	{
		byte[] noColumns = write(writer -> {
			writer.header(List.of(), null);
			writer.row(List.of("ignored"));
		});
		String sheet = part(noColumns, "xl/worksheets/sheet1.xml");
		assertFalse(sheet.contains("autoFilter"), sheet);
		assertFalse(sheet.contains("pane"), sheet);
		assertFalse(part(noColumns, "xl/workbook.xml").contains("definedName"));

		byte[] nothing = write(writer -> writer.caption(null));
		try (XSSFWorkbook workbook = XlsxExportToolbarActionTest.read(nothing))
		{
			assertEquals("Sheet", workbook.getSheetName(0));
			assertEquals(-1, workbook.getSheetAt(0).getLastRowNum());
		}
	}

	@Test
	void theWriterKeepsItsOrderAndLimits()
	{
		write(writer -> {
			writer.header(List.of("A"), null);
			assertThrows(IllegalStateException.class, () -> writer.header(List.of("A"), null));
			assertThrows(IllegalStateException.class, () -> writer.caption(List.of("late")));
			writer.finish();
			writer.finish();
		});

		write(3, writer -> {
			writer.header(List.of("A"), null);
			assertTrue(writer.hasRoom(1));
			writer.row(List.of(1));
			assertFalse(writer.hasRoom(1));
			assertTrue(writer.hasRoom(0));
			writer.row(List.of(2));
			assertFalse(writer.hasRoom(0));
			assertThrows(IllegalStateException.class, () -> writer.footer(List.of(3)));
		});
		assertThrows(IllegalArgumentException.class,
			() -> new XlsxWriter(new ByteArrayOutputStream(), "x", ZONE, String::valueOf, 0));
	}

	@Test
	void theSheetIsNamedAsExcelAcceptsIt()
	{
		assertEquals("Sheet1", XlsxWriter.sheetName(null));
		assertEquals("Sheet1", XlsxWriter.sheetName("  "));
		assertEquals("Sheet1", XlsxWriter.sheetName("'''"));
		assertEquals("a b c d e f g h", XlsxWriter.sheetName("a/b\\c?d*e[f]g:h"));
		assertEquals("quoted", XlsxWriter.sheetName("' quoted '"));
		assertEquals("it's", XlsxWriter.sheetName("it's"));
		assertEquals("tab here", XlsxWriter.sheetName("tab\there"));
		assertEquals("Cuentas por cobrar por antigüed",
			XlsxWriter.sheetName("Cuentas por cobrar por antigüedad al 15 de noviembre"));
		assertEquals("x".repeat(30), XlsxWriter.sheetName("x".repeat(30) + "😀"),
			"without half of the emoji");
		assertEquals("history 1", XlsxWriter.sheetName("history"));
		assertEquals("Sheet", new XlsxWriter(new ByteArrayOutputStream(), "Sheet", ZONE,
			String::valueOf).getSheetName());
	}

	@Test
	void columnsAreNamedAsExcelNamesThem()
	{
		assertEquals("A", XlsxWriter.columnName(0));
		assertEquals("Z", XlsxWriter.columnName(25));
		assertEquals("AA", XlsxWriter.columnName(26));
		assertEquals("ZZ", XlsxWriter.columnName(701));
		assertEquals("AAA", XlsxWriter.columnName(702));
		assertEquals("XFD", XlsxWriter.columnName(XlsxWriter.MAX_COLUMNS - 1));
	}

	@Test
	void aSheetNameWithAnApostropheIsQuotedInTheFilter() throws IOException
	{
		byte[] file = write("it's", 100, writer -> {
			writer.header(List.of("A"), null);
			writer.row(List.of(1));
		});

		try (XSSFWorkbook workbook = XlsxExportToolbarActionTest.read(file))
		{
			assertEquals("'it''s'!$A$1:$A$2",
				workbook.getName("_xlnm._FilterDatabase").getRefersToFormula());
		}
	}

	@Test
	void theStreamIsLeftOpen() throws IOException
	{
		ClosingStream out = new ClosingStream();
		XlsxWriter writer = new XlsxWriter(out, "x", ZONE, String::valueOf);
		writer.header(List.of("A"), null);
		writer.finish();

		assertFalse(out.closed);
		assertTrue(out.size() > 0);
	}

	/** Reads a part of a workbook as text. */
	static String part(byte[] file, String name)
	{
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(file)))
		{
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
			{
				if (entry.getName().equals(name))
				{
					return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
				}
			}
			throw new IllegalArgumentException("No " + name);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private static byte[] write(Writing writing)
	{
		return write("Sheet", XlsxWriter.MAX_ROWS, writing);
	}

	private static byte[] write(int maxRows, Writing writing)
	{
		return write("Sheet", maxRows, writing);
	}

	private static byte[] write(String sheetName, int maxRows, Writing writing)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try
		{
			XlsxWriter writer = new XlsxWriter(out, sheetName, ZONE,
				value -> value.getClass() == Object.class ? null : value.toString(), maxRows);
			writing.write(writer);
			writer.finish();
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return out.toByteArray();
	}

	@FunctionalInterface
	private interface Writing
	{
		void write(XlsxWriter writer) throws IOException;
	}

	/** A number whose text is no decimal. */
	private static class OddNumber extends Number
	{
		private static final long serialVersionUID = 1L;

		private final double value;

		OddNumber(double value)
		{
			this.value = value;
		}

		@Override
		public int intValue()
		{
			return (int)value;
		}

		@Override
		public long longValue()
		{
			return (long)value;
		}

		@Override
		public float floatValue()
		{
			return (float)value;
		}

		@Override
		public double doubleValue()
		{
			return value;
		}

		@Override
		public String toString()
		{
			return "about " + value;
		}
	}

	private static class ClosingStream extends ByteArrayOutputStream
	{
		boolean closed;

		@Override
		public void close()
		{
			closed = true;
		}
	}
}
