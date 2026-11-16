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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.wicket.util.lang.Args;

/**
 * Writes a workbook of one sheet in the format of Excel, SpreadsheetML (Office Open XML,
 * {@code .xlsx}): a zip of a few XML files, which Excel, LibreOffice and Google Sheets open. The
 * rows are written as they come, so a sheet of any length is never held in memory: only its first
 * {@value #MEASURED_ROWS} rows are, to measure the widths of the columns before writing them.
 * <p>
 * Lines of a caption may come first, then the header, in bold, frozen and with an auto-filter, the
 * rows, and a footer in bold, out of the filter's range so that sorting leaves it last. A cell
 * gets the type of its value: a {@link Number} is a number, a {@link LocalDate} a date, a
 * {@link LocalDateTime}, {@link Instant}, {@link Date}, {@link OffsetDateTime} or
 * {@link ZonedDateTime} a date and time, a {@link Boolean} a boolean, and anything else a text,
 * converted by the function given. A text is written as an inline string, never read as a formula.
 * <p>
 * Excel's limits are kept: {@value #MAX_ROWS} rows, see {@link #hasRoom(int)}, {@value #MAX_COLUMNS}
 * columns and {@value #MAX_CELL_LENGTH} characters a cell, the rest of a longer text being left
 * out. A date before 1900 or after 9999, which Excel cannot hold, is written as an ISO-8601 text; a
 * whole number of more than 15 digits, which Excel would round, as its digits.
 */
final class XlsxWriter
{
	/** The rows a sheet of Excel holds. */
	static final int MAX_ROWS = 1_048_576;

	/** The columns a sheet of Excel holds. */
	static final int MAX_COLUMNS = 16_384;

	/** The characters a cell of Excel holds. */
	static final int MAX_CELL_LENGTH = 32_767;

	/** The characters of the name of a sheet of Excel. */
	static final int MAX_SHEET_NAME_LENGTH = 31;

	/** The first rows, measured for the widths of the columns and kept until these are written. */
	static final int MEASURED_ROWS = 100;

	/** The greatest whole number Excel keeps every digit of: 15 nines. */
	private static final long MAX_EXACT_INTEGER = 999_999_999_999_999L;

	/** The narrowest and the widest column, in characters. */
	private static final double NARROWEST = 6;

	private static final double WIDEST = 60;

	/** Excel's own formats of a date and of a date and time, shown as the computer's locale says. */
	private static final int DATE_FORMAT = 14;

	private static final int DATE_TIME_FORMAT = 22;

	/** The id of the first format of a workbook's own. */
	private static final int FIRST_CUSTOM_FORMAT = 164;

	/** The day before Excel's first, 1900-01-01, which counts as day 1. */
	private static final LocalDate EPOCH = LocalDate.of(1899, 12, 31);

	/**
	 * The first day after the 29 February 1900 Excel counts, which never was, from which the
	 * serial numbers are one more than the days since {@link #EPOCH}.
	 */
	private static final LocalDate AFTER_LEAP_BUG = LocalDate.of(1900, 3, 1);

	private static final LocalDate LAST_DAY = LocalDate.of(9999, 12, 31);

	private static final String MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";

	private static final String RELATIONSHIPS =
		"http://schemas.openxmlformats.org/officeDocument/2006/relationships";

	private static final String PACKAGE_RELATIONSHIPS =
		"http://schemas.openxmlformats.org/package/2006/relationships";

	private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

	private final ZipOutputStream zip;

	private final String sheetName;

	private final ZoneId zone;

	private final Function<Object, String> text;

	private final int maxRows;

	/** The ids of the formats of the workbook's own, by their code. */
	private final Map<String, Integer> customFormats = new LinkedHashMap<>();

	/** The cell styles, by their format and whether they are bold; the first is the default. */
	private final Map<Style, Integer> styles = new LinkedHashMap<>();

	/** The rows not written yet, while the first rows are measured; {@code null} after. */
	private StringBuilder pending = new StringBuilder();

	private Writer sheet;

	private String[] formats = new String[0];

	private double[] widths = new double[0];

	private int columns;

	/** The rows written or pending. */
	private int rows;

	/** The row of the header, from 1; 0 until it is written. */
	private int headerRow;

	/** The last row under the header, in the auto-filter's range. */
	private int lastFilteredRow;

	/** The rows of the body measured for the widths. */
	private int measured;

	private boolean finished;

	/**
	 * @param out
	 *            where the workbook is written, left open
	 * @param sheetName
	 *            the name of the sheet, see {@link #sheetName(String)}
	 * @param zone
	 *            the zone an {@link Instant} or a {@link Date} is shown in
	 * @param text
	 *            converts a value of no other type to its text
	 */
	XlsxWriter(OutputStream out, String sheetName, ZoneId zone, Function<Object, String> text)
	{
		this(out, sheetName, zone, text, MAX_ROWS);
	}

	/**
	 * @param maxRows
	 *            the rows the sheet holds at most, fewer than Excel's for a test
	 */
	XlsxWriter(OutputStream out, String sheetName, ZoneId zone, Function<Object, String> text,
		int maxRows)
	{
		Args.notNull(out, "out");
		this.zip = new ZipOutputStream(new FilterOutputStream(out)
		{
			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				out.write(b, off, len);
			}

			@Override
			public void close() throws IOException
			{
				// the stream belongs to the caller
				flush();
			}
		}, StandardCharsets.UTF_8);
		this.sheetName = sheetName(sheetName);
		this.zone = Args.notNull(zone, "zone");
		this.text = Args.notNull(text, "text");
		this.maxRows = Args.withinRange(1, MAX_ROWS, maxRows, "maxRows");
		styles.put(new Style(0, false), 0);
	}

	/**
	 * @return the name of the sheet, as Excel accepts it
	 */
	String getSheetName()
	{
		return sheetName;
	}

	/**
	 * Writes lines above the header, the first in bold, such as a title and what the sheet covers,
	 * and an empty row under them. Only before the header.
	 *
	 * @param lines
	 *            the lines, each in the first cell of its row; none for no caption
	 * @throws IOException
	 *             if the workbook cannot be written
	 */
	void caption(List<String> lines) throws IOException
	{
		if (headerRow > 0)
		{
			throw new IllegalStateException("The caption goes above the header");
		}
		if (lines == null || lines.isEmpty())
		{
			return;
		}
		boolean first = true;
		for (String line : lines)
		{
			StringBuilder row = startRow();
			if (line != null && line.isEmpty() == false)
			{
				appendText(row, 0, line, style(0, first));
			}
			endRow(row);
			first = false;
		}
		endRow(startRow());
	}

	/**
	 * Writes the header, in bold, which stays in view and gets an auto-filter, and the formats of
	 * the columns.
	 *
	 * @param headers
	 *            the header of each column
	 * @param formats
	 *            the number format of each column, such as {@code #,##0.00}, {@code null} for a
	 *            column without one; or {@code null} for none
	 * @throws IOException
	 *             if the workbook cannot be written
	 */
	void header(List<String> headers, List<String> formats) throws IOException
	{
		if (headerRow > 0)
		{
			throw new IllegalStateException("The header is written once");
		}
		columns = Math.min(headers.size(), MAX_COLUMNS);
		this.formats = new String[columns];
		widths = new double[columns];
		StringBuilder row = startRow();
		for (int i = 0; i < columns; i++)
		{
			String format = formats != null && i < formats.size() ? formats.get(i) : null;
			this.formats[i] = format == null || format.isBlank() ||
				"General".equalsIgnoreCase(format) ? null : format;
			String header = headers.get(i) != null ? headers.get(i) : "";
			if (header.isEmpty() == false)
			{
				appendText(row, i, header, style(0, true));
			}
			// the button of the filter takes some two characters
			widths[i] = textWidth(header) + 2;
		}
		endRow(row);
		headerRow = rows;
		lastFilteredRow = rows;
	}

	/**
	 * @param reserved
	 *            the rows to keep for what comes after, such as a footer
	 * @return whether the sheet holds another row and the reserved ones
	 */
	boolean hasRoom(int reserved)
	{
		return rows + 1 + reserved <= maxRows;
	}

	/**
	 * Writes a row under the header, in the range of its auto-filter.
	 *
	 * @param values
	 *            the value of each column, {@code null} for an empty cell
	 * @throws IOException
	 *             if the workbook cannot be written
	 * @throws IllegalStateException
	 *             if the sheet is full
	 */
	void row(List<?> values) throws IOException
	{
		writeRow(values, false);
		lastFilteredRow = rows;
	}

	/**
	 * Writes the footer, in bold, under the rows and out of the auto-filter's range.
	 *
	 * @param values
	 *            the value of each column, {@code null} for an empty cell
	 * @throws IOException
	 *             if the workbook cannot be written
	 * @throws IllegalStateException
	 *             if the sheet is full
	 */
	void footer(List<?> values) throws IOException
	{
		writeRow(values, true);
	}

	/**
	 * Ends the sheet and writes the parts of the workbook left, leaving the stream open.
	 *
	 * @throws IOException
	 *             if the workbook cannot be written
	 */
	void finish() throws IOException
	{
		if (finished)
		{
			return;
		}
		finished = true;
		startSheet();
		sheet.write("</sheetData>");
		if (hasFilter())
		{
			sheet.write("<autoFilter ref=\"" + filterRange(false) + "\"/>");
		}
		sheet.write("</worksheet>");
		sheet.flush();
		zip.closeEntry();
		entry("xl/styles.xml", styles());
		entry("xl/workbook.xml", workbook());
		zip.finish();
		zip.flush();
	}

	private void writeRow(List<?> values, boolean bold) throws IOException
	{
		if (hasRoom(0) == false)
		{
			throw new IllegalStateException("A sheet holds " + maxRows + " rows");
		}
		boolean measure = pending != null && measured < MEASURED_ROWS;
		StringBuilder row = startRow();
		for (int i = 0; i < Math.min(values.size(), columns); i++)
		{
			double width = appendValue(row, i, values.get(i), bold);
			if (measure)
			{
				widths[i] = Math.max(widths[i], width);
			}
		}
		endRow(row);
		if (measure && ++measured == MEASURED_ROWS)
		{
			startSheet();
		}
	}

	private StringBuilder startRow()
	{
		rows++;
		StringBuilder row = pending != null ? pending : new StringBuilder();
		return row.append("<row r=\"").append(rows).append("\">");
	}

	private void endRow(StringBuilder row) throws IOException
	{
		row.append("</row>");
		if (pending == null)
		{
			sheet.append(row);
		}
	}

	/**
	 * Writes the start of the sheet, the widths of its columns among it, and the rows kept until
	 * then; once.
	 */
	private void startSheet() throws IOException
	{
		if (sheet != null)
		{
			return;
		}
		staticParts();
		zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
		sheet = new OutputStreamWriter(new FilterOutputStream(zip)
		{
			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				zip.write(b, off, len);
			}

			@Override
			public void close() throws IOException
			{
				flush();
			}
		}, StandardCharsets.UTF_8);
		StringBuilder xml = new StringBuilder(XML);
		xml.append("<worksheet xmlns=\"").append(MAIN).append("\" xmlns:r=\"").append(RELATIONSHIPS)
			.append("\">");
		xml.append("<sheetViews><sheetView workbookViewId=\"0\" tabSelected=\"1\">");
		if (headerRow > 0 && columns > 0)
		{
			String first = "A" + (headerRow + 1);
			xml.append("<pane ySplit=\"").append(headerRow).append("\" topLeftCell=\"").append(first)
				.append("\" activePane=\"bottomLeft\" state=\"frozen\"/>");
			xml.append("<selection pane=\"bottomLeft\" activeCell=\"").append(first)
				.append("\" sqref=\"").append(first).append("\"/>");
		}
		xml.append("</sheetView></sheetViews>");
		xml.append("<sheetFormatPr defaultRowHeight=\"15\"/>");
		if (columns > 0)
		{
			xml.append("<cols>");
			for (int i = 0; i < columns; i++)
			{
				double width = Math.clamp(widths[i] + 1, NARROWEST, WIDEST);
				xml.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
					.append("\" width=\"").append(String.format(Locale.ROOT, "%.2f", width))
					.append("\" customWidth=\"1\"/>");
			}
			xml.append("</cols>");
		}
		xml.append("<sheetData>");
		sheet.append(xml);
		sheet.append(pending);
		pending.setLength(0);
		pending = null;
	}

	private boolean hasFilter()
	{
		return headerRow > 0 && columns > 0;
	}

	/**
	 * @return the range of the auto-filter, the header and the rows under it, for example
	 *         {@code A1:C10}, or {@code $A$1:$C$10} when absolute
	 */
	private String filterRange(boolean absolute)
	{
		String dollar = absolute ? "$" : "";
		return dollar + "A" + dollar + headerRow + ":" + dollar + columnName(columns - 1) + dollar +
			lastFilteredRow;
	}

	/**
	 * Appends the cell of a value.
	 *
	 * @return the width the value takes, in characters
	 */
	private double appendValue(StringBuilder row, int column, Object value, boolean bold)
	{
		String format = formats[column];
		return switch (value)
		{
			case null -> 0;
			case CharSequence chars -> appendText(row, column, chars.toString(), style(0, bold));
			case Boolean bool -> {
				appendCell(row, column, style(0, bold), "b", bool ? "1" : "0");
				yield 5;
			}
			case Number number -> {
				String digits = number(number);
				if (digits == null)
				{
					yield appendText(row, column, String.valueOf(number), style(0, bold));
				}
				appendCell(row, column, style(format(format, 0), bold), null, digits);
				yield numberWidth(digits, format);
			}
			case LocalDate date -> appendDate(row, column, date.atStartOfDay(), format,
				DATE_FORMAT, bold, date.toString());
			case LocalDateTime dateTime -> appendDate(row, column, dateTime, format,
				DATE_TIME_FORMAT, bold, dateTime.toString());
			// shown at their own offset, which the application chose
			case OffsetDateTime dateTime -> appendDate(row, column, dateTime.toLocalDateTime(),
				format, DATE_TIME_FORMAT, bold, dateTime.toString());
			case ZonedDateTime dateTime -> appendDate(row, column, dateTime.toLocalDateTime(),
				format, DATE_TIME_FORMAT, bold, dateTime.toString());
			case Instant instant -> appendDate(row, column, LocalDateTime.ofInstant(instant, zone),
				format, DATE_TIME_FORMAT, bold, instant.toString());
			case Date date -> {
				// a java.sql.Date has no instant of its own
				Instant instant = Instant.ofEpochMilli(date.getTime());
				yield appendDate(row, column, LocalDateTime.ofInstant(instant, zone), format,
					DATE_TIME_FORMAT, bold, instant.toString());
			}
			default -> {
				String converted = text.apply(value);
				yield converted != null ? appendText(row, column, converted, style(0, bold)) : 0;
			}
		};
	}

	private double appendDate(StringBuilder row, int column, LocalDateTime dateTime, String format,
		int defaultFormat, boolean bold, String iso)
	{
		String serial = serial(dateTime);
		if (serial == null)
		{
			return appendText(row, column, iso, style(0, bold));
		}
		appendCell(row, column, style(format(format, defaultFormat), bold), null, serial);
		return defaultFormat == DATE_FORMAT ? 10 : 16;
	}

	private double appendText(StringBuilder row, int column, String value, int style)
	{
		String cell = value.length() > MAX_CELL_LENGTH
			? value.substring(0, Character.isHighSurrogate(value.charAt(MAX_CELL_LENGTH - 1))
				? MAX_CELL_LENGTH - 1 : MAX_CELL_LENGTH)
			: value;
		row.append("<c r=\"").append(columnName(column)).append(rows).append('"');
		if (style != 0)
		{
			row.append(" s=\"").append(style).append('"');
		}
		row.append(" t=\"inlineStr\"><is><t");
		if (cell.isEmpty() == false && (Character.isWhitespace(cell.charAt(0)) ||
			Character.isWhitespace(cell.charAt(cell.length() - 1)) || cell.indexOf('\n') >= 0))
		{
			row.append(" xml:space=\"preserve\"");
		}
		row.append('>');
		escape(row, cell, false);
		row.append("</t></is></c>");
		return textWidth(cell);
	}

	private void appendCell(StringBuilder row, int column, int style, String type, String value)
	{
		row.append("<c r=\"").append(columnName(column)).append(rows).append('"');
		if (style != 0)
		{
			row.append(" s=\"").append(style).append('"');
		}
		if (type != null)
		{
			row.append(" t=\"").append(type).append('"');
		}
		row.append("><v>").append(value).append("</v></c>");
	}

	/**
	 * @return the id of the number format of a code, the workbook's own; the default if none
	 */
	private int format(String code, int defaultFormat)
	{
		if (code == null)
		{
			return defaultFormat;
		}
		return customFormats.computeIfAbsent(code, c -> FIRST_CUSTOM_FORMAT + customFormats.size());
	}

	private int style(int format, boolean bold)
	{
		return styles.computeIfAbsent(new Style(format, bold), s -> styles.size());
	}

	private void staticParts() throws IOException
	{
		entry("[Content_Types].xml", XML +
			"<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
			"<Default Extension=\"rels\" " +
			"ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
			"<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
			"<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/" +
			"vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
			"<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/" +
			"vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
			"<Override PartName=\"/xl/styles.xml\" ContentType=\"application/" +
			"vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
			"</Types>");
		entry("_rels/.rels", XML + "<Relationships xmlns=\"" + PACKAGE_RELATIONSHIPS + "\">" +
			"<Relationship Id=\"rId1\" Type=\"" + RELATIONSHIPS + "/officeDocument\" " +
			"Target=\"xl/workbook.xml\"/></Relationships>");
		entry("xl/_rels/workbook.xml.rels", XML + "<Relationships xmlns=\"" +
			PACKAGE_RELATIONSHIPS + "\">" + "<Relationship Id=\"rId1\" Type=\"" + RELATIONSHIPS +
			"/worksheet\" Target=\"worksheets/sheet1.xml\"/>" + "<Relationship Id=\"rId2\" Type=\"" +
			RELATIONSHIPS + "/styles\" Target=\"styles.xml\"/></Relationships>");
	}

	private String workbook()
	{
		StringBuilder xml = new StringBuilder(XML);
		xml.append("<workbook xmlns=\"").append(MAIN).append("\" xmlns:r=\"").append(RELATIONSHIPS)
			.append("\"><bookViews><workbookView/></bookViews><sheets><sheet name=\"");
		escape(xml, sheetName, true);
		xml.append("\" sheetId=\"1\" r:id=\"rId1\"/></sheets>");
		if (hasFilter())
		{
			// as Excel names the range of a sheet's auto-filter
			xml.append("<definedNames><definedName name=\"_xlnm._FilterDatabase\" ")
				.append("localSheetId=\"0\" hidden=\"1\">");
			escape(xml, "'" + sheetName.replace("'", "''") + "'!" + filterRange(true), false);
			xml.append("</definedName></definedNames>");
		}
		return xml.append("</workbook>").toString();
	}

	private String styles()
	{
		StringBuilder xml = new StringBuilder(XML);
		xml.append("<styleSheet xmlns=\"").append(MAIN).append("\">");
		if (customFormats.isEmpty() == false)
		{
			xml.append("<numFmts count=\"").append(customFormats.size()).append("\">");
			customFormats.forEach((code, id) -> {
				xml.append("<numFmt numFmtId=\"").append(id).append("\" formatCode=\"");
				escape(xml, code, true);
				xml.append("\"/>");
			});
			xml.append("</numFmts>");
		}
		String font = "<sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>";
		xml.append("<fonts count=\"2\"><font>").append(font).append("<font><b/>").append(font)
			.append("</fonts>");
		xml.append("<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>")
			.append("<fill><patternFill patternType=\"gray125\"/></fill></fills>");
		xml.append("<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/>")
			.append("</border></borders>");
		xml.append("<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" ")
			.append("borderId=\"0\"/></cellStyleXfs>");
		xml.append("<cellXfs count=\"").append(styles.size()).append("\">");
		for (Style style : styles.keySet())
		{
			xml.append("<xf numFmtId=\"").append(style.format).append("\" fontId=\"")
				.append(style.bold ? 1 : 0).append("\" fillId=\"0\" borderId=\"0\" xfId=\"0\"");
			if (style.format != 0)
			{
				xml.append(" applyNumberFormat=\"1\"");
			}
			if (style.bold)
			{
				xml.append(" applyFont=\"1\"");
			}
			xml.append("/>");
		}
		xml.append("</cellXfs>");
		xml.append("<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" ")
			.append("builtinId=\"0\"/></cellStyles>");
		return xml.append("</styleSheet>").toString();
	}

	private void entry(String name, String content) throws IOException
	{
		zip.putNextEntry(new ZipEntry(name));
		zip.write(content.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	/**
	 * @return the number as Excel reads it, or {@code null} if it cannot be one: not finite, too
	 *         large for a double, or a whole number of more than 15 digits, which Excel would round
	 */
	static String number(Number number)
	{
		if (number instanceof Integer || number instanceof Long || number instanceof Short ||
			number instanceof Byte || number instanceof AtomicInteger ||
			number instanceof AtomicLong)
		{
			long value = number.longValue();
			return value >= -MAX_EXACT_INTEGER && value <= MAX_EXACT_INTEGER ? Long.toString(value)
				: null;
		}
		if (number instanceof BigInteger integer)
		{
			return integer.abs().compareTo(BigInteger.valueOf(MAX_EXACT_INTEGER)) <= 0
				? integer.toString() : null;
		}
		if (number instanceof BigDecimal decimal)
		{
			return decimal(decimal);
		}
		if (number instanceof Double || number instanceof Float)
		{
			// Float.toString gives the digits of the float, not those of its double
			return Double.isFinite(number.doubleValue())
				? decimal(new BigDecimal(number.toString())) : null;
		}
		try
		{
			return decimal(new BigDecimal(number.toString()));
		}
		catch (NumberFormatException e)
		{
			double value = number.doubleValue();
			return Double.isFinite(value) ? decimal(BigDecimal.valueOf(value)) : null;
		}
	}

	private static String decimal(BigDecimal decimal)
	{
		if (Double.isInfinite(decimal.doubleValue()))
		{
			return null;
		}
		// a double has 17 significant digits at most
		BigDecimal rounded = decimal.round(new MathContext(17));
		return Math.abs(rounded.scale()) > 30 ? rounded.toString() : rounded.toPlainString();
	}

	/**
	 * @return the date and time as Excel counts it, the days since 1900 and the fraction of the
	 *         day, or {@code null} for a date Excel cannot hold, before 1900 or after 9999
	 */
	static String serial(LocalDateTime dateTime)
	{
		LocalDate date = dateTime.toLocalDate();
		if (date.isBefore(EPOCH.plusDays(1)) || date.isAfter(LAST_DAY))
		{
			return null;
		}
		long days = ChronoUnit.DAYS.between(EPOCH, date);
		if (date.isBefore(AFTER_LEAP_BUG) == false)
		{
			// Excel counts a 29 February 1900, as Lotus 1-2-3 did
			days++;
		}
		long nanos = dateTime.toLocalTime().toNanoOfDay();
		if (nanos == 0)
		{
			return Long.toString(days);
		}
		BigDecimal fraction = BigDecimal.valueOf(nanos).divide(BigDecimal.valueOf(86_400_000_000_000L),
			new MathContext(12));
		return BigDecimal.valueOf(days).add(fraction).toPlainString();
	}

	/**
	 * @param title
	 *            a title, may be {@code null}
	 * @return the title as the name of a sheet of Excel: without the characters Excel refuses in
	 *         one ({@code \ / ? * [ ] :}, control characters, an apostrophe first or last), at
	 *         most {@value #MAX_SHEET_NAME_LENGTH} characters, {@code Sheet1} for none
	 */
	static String sheetName(String title)
	{
		if (title == null)
		{
			return "Sheet1";
		}
		StringBuilder name = new StringBuilder();
		title.codePoints().forEach(c -> name.appendCodePoint(
			"\\/?*[]:".indexOf(c) >= 0 || Character.isISOControl(c) ? ' ' : c));
		String cleaned = strip(name.toString());
		if (cleaned.length() > MAX_SHEET_NAME_LENGTH)
		{
			int end = Character.isHighSurrogate(cleaned.charAt(MAX_SHEET_NAME_LENGTH - 1))
				? MAX_SHEET_NAME_LENGTH - 1 : MAX_SHEET_NAME_LENGTH;
			cleaned = strip(cleaned.substring(0, end));
		}
		if (cleaned.isEmpty())
		{
			return "Sheet1";
		}
		// a name Excel keeps for itself
		return "History".equalsIgnoreCase(cleaned) ? cleaned + " 1" : cleaned;
	}

	/** @return the text without spaces or apostrophes at its ends */
	private static String strip(String name)
	{
		String stripped = name.strip();
		while (stripped.startsWith("'") || stripped.endsWith("'"))
		{
			stripped = (stripped.startsWith("'") ? stripped.substring(1)
				: stripped.substring(0, stripped.length() - 1)).strip();
		}
		return stripped;
	}

	/**
	 * @param column
	 *            a column, from 0
	 * @return its name, {@code A} to {@code XFD}
	 */
	static String columnName(int column)
	{
		StringBuilder name = new StringBuilder();
		for (int n = column + 1; n > 0; n = (n - 1) / 26)
		{
			name.insert(0, (char)('A' + (n - 1) % 26));
		}
		return name.toString();
	}

	/**
	 * Escapes a text for XML. What XML cannot hold, such as a control character or half of a
	 * surrogate pair, is written as SpreadsheetML escapes it, {@code _x0001_}, and so is the
	 * underscore of a text that would read as such an escape.
	 */
	private static void escape(StringBuilder xml, String text, boolean attribute)
	{
		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			switch (c)
			{
				case '&' -> xml.append("&amp;");
				case '<' -> xml.append("&lt;");
				case '>' -> xml.append("&gt;");
				case '"' -> xml.append(attribute ? "&quot;" : "\"");
				case '\t', '\n' -> xml.append(attribute ? "&#" + (int)c + ";" : String.valueOf(c));
				case '_' -> xml.append(isEscape(text, i) ? "_x005F_" : "_");
				default -> {
					if (Character.isHighSurrogate(c) && i + 1 < text.length() &&
						Character.isLowSurrogate(text.charAt(i + 1)))
					{
						xml.append(c).append(text.charAt(++i));
					}
					else if (c < 0x20 || Character.isSurrogate(c) || c == 0xFFFE || c == 0xFFFF)
					{
						xml.append(String.format(Locale.ROOT, "_x%04X_", (int)c));
					}
					else
					{
						xml.append(c);
					}
				}
			}
		}
	}

	/** @return whether the text has an escape such as {@code _x0041_} at the index */
	private static boolean isEscape(String text, int at)
	{
		if (at + 7 > text.length() || text.charAt(at + 1) != 'x' || text.charAt(at + 6) != '_')
		{
			return false;
		}
		for (int i = at + 2; i < at + 6; i++)
		{
			if (Character.digit(text.charAt(i), 16) < 0)
			{
				return false;
			}
		}
		return true;
	}

	/** @return the width of a text, its longest line, in characters */
	private static double textWidth(String text)
	{
		int widest = 0;
		for (String line : text.split("\n", -1))
		{
			widest = Math.max(widest, line.codePointCount(0, line.length()));
		}
		return widest;
	}

	/** @return about the width of a number shown in a format, in characters */
	private static double numberWidth(String digits, String format)
	{
		int point = digits.indexOf('.');
		int whole = (point < 0 ? digits.length() : point) - (digits.startsWith("-") ? 1 : 0);
		double width = digits.length();
		if (format != null)
		{
			if (format.contains(","))
			{
				width += (whole - 1) / 3;
			}
			if (format.contains("%"))
			{
				width += 3;
			}
			int formatPoint = format.indexOf('.');
			if (formatPoint >= 0)
			{
				int decimals = 0;
				while (formatPoint + 1 + decimals < format.length() &&
					"0#".indexOf(format.charAt(formatPoint + 1 + decimals)) >= 0)
				{
					decimals++;
				}
				width = width - (point < 0 ? 0 : digits.length() - point) + 1 + decimals;
			}
		}
		return width;
	}

	/** A cell style: a number format, bold or not. */
	private record Style(int format, boolean bold)
	{
	}
}
