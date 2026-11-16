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

import java.io.Serializable;
import java.time.ZoneId;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior.Location;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.icon.IIcon;

/**
 * An action exporting the rows of a {@link BaseDynamicDataTable} as an Excel workbook
 * ({@code .xlsx}), shown as an icon button, next to a {@link CsvExportToolbarAction} and with the
 * same rules. Excel, LibreOffice and Google Sheets open it with the numbers as numbers and the
 * dates as dates, in any locale, and the accents right.
 * <p>
 * The sheet holds the {@link BaseDynamicDataTable#getSelection() selected} rows if there are any,
 * and every row of the provider otherwise, in the provider's order. Its columns are those the
 * table shows that are {@link IDynamicColumn#isExportable() exportable}, in display order, headed
 * by their header text, in bold, frozen above the rows and with an auto-filter. The columns are as
 * wide as their header and the values of the first rows.
 * <p>
 * Each cell gets the type of its {@link IDynamicColumn#getExportValue(Object) export value}: a
 * {@link Number}, such as a {@code BigDecimal}, an {@code Integer}, a {@code Long} or a
 * {@code Double}, is a number; a {@code LocalDate} a date; a {@code LocalDateTime}, an
 * {@code OffsetDateTime} or a {@code ZonedDateTime} a date and time at its own offset, an
 * {@code Instant} or a {@code java.util.Date} one in the {@link #setZoneId(ZoneId) zone} of the
 * action; a {@code Boolean} a boolean. A date is shown as the computer opening the file shows
 * dates, unless its column gives a format. Any other value is a text, converted by the table's
 * converters for its locale; other dates and times of {@code java.time} in ISO-8601, as a CSV
 * export writes them. A text is always a text, never a formula, whatever it starts with: there is
 * nothing to escape.
 * <p>
 * A column may give the number format of its cells, {@link IDynamicColumn#getExportFormat()} or
 * {@link IExportFormatColumn} in a {@link BeanBasedDynamicDataTable}: {@code #,##0.00} keeps two
 * decimals to an amount, {@code 0.00%} shows a rate of {@code 0.07} as {@code 7.00%}.
 * <p>
 * With {@link #setExportFooter(boolean)}, the sheet ends with the table's
 * {@link BaseDynamicDataTable#hasFooter() footer}, its totals for example, in bold and out of the
 * auto-filter's range, so sorting leaves it last: a row with the
 * {@link IFooterColumn#getFooterExportValue() footer export value} of each column, typed as the
 * rows are. A sheet of the selected rows only has no footer, as the footer sums up all the rows.
 * <p>
 * The sheet is named after the {@link #setTitle(IModel) title}; a {@link #setCaption(IModel)
 * caption} may go above the header, such as what the sheet covers and when it was made.
 * <p>
 * The rows are written to the response as the provider gives them, a page of
 * {@link BaseDynamicDataTable#fetchAllRows()} at a time, so a table of tens of thousands of rows
 * is exported without holding them in memory. Excel's limits apply: a sheet holds 1,048,576 rows,
 * the header, the caption and the footer among them, so the rows beyond are left out, and the
 * footer with them, as it would not sum up the rows shown; a cell holds 32,767 characters, the
 * rest of a longer text being left out. A date before 1900 or after 9999, which Excel cannot hold,
 * is written as an ISO-8601 text, and so is a whole number of more than 15 digits, such as a long
 * id, which Excel would round.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys of the rows
 * @since 11.0.0
 */
public class XlsxExportToolbarAction<T, K extends Serializable>
	implements
		IDynamicToolbarAction<T, K>
{
	private static final long serialVersionUID = 1L;

	/** The media type of an Excel workbook. */
	public static final String CONTENT_TYPE =
		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	/** A sheet: a grid of cells. */
	private static final IIcon SHEET = () -> "<svg aria-hidden=\"true\" focusable=\"false\" " +
		"viewBox=\"0 0 24 24\" width=\"18\" height=\"18\"><path fill=\"currentColor\" " +
		"d=\"M20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM8 20H4v-4h4" +
		"v4zm0-6H4v-4h4v4zm0-6H4V4h4v4zm6 12h-4v-4h4v4zm0-6h-4v-4h4v4zm0-6h-4V4h4v4zm6 12h-4v-4h4v4" +
		"zm0-6h-4v-4h4v4zm0-6h-4V4h4v4z\"/></svg>";

	private IModel<String> label = new ResourceModel("DynamicDataTable.exportXlsx",
		"Export to Excel");

	private IIcon icon = SHEET;

	private String fileName = "export.xlsx";

	private IModel<String> title;

	private IModel<List<String>> caption;

	private Location location = Location.Blob;

	private boolean exportFooter;

	private ZoneId zoneId;

	/** The rows a sheet holds, fewer than Excel's in a test. */
	private int maxRows = XlsxWriter.MAX_ROWS;

	@Override
	public Component newComponent(String id, BaseDynamicDataTable<T, K, ?> table)
	{
		return new XlsxExportButton<>(id, table, this);
	}

	/**
	 * @return the label of the button, its tooltip and accessible name
	 */
	public IModel<String> getLabel()
	{
		return label;
	}

	/**
	 * @param label
	 *            the label of the button, its tooltip and accessible name, written escaped;
	 *            {@code DynamicDataTable.exportXlsx} by default
	 * @return {@code this}
	 */
	public XlsxExportToolbarAction<T, K> setLabel(IModel<String> label)
	{
		this.label = Args.notNull(label, "label");
		return this;
	}

	/**
	 * @return the icon of the button
	 */
	public IIcon getIcon()
	{
		return icon;
	}

	/**
	 * @param icon
	 *            the icon of the button, a grid of cells by default, for example
	 *            {@code SvgIcon.FILE_EXCEL}
	 * @return {@code this}
	 */
	public XlsxExportToolbarAction<T, K> setIcon(IIcon icon)
	{
		this.icon = Args.notNull(icon, "icon");
		return this;
	}

	/**
	 * @return the name of the file, {@code export.xlsx} by default
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * @param fileName
	 *            the name of the file, for example {@code customers.xlsx}
	 * @return {@code this}
	 */
	public XlsxExportToolbarAction<T, K> setFileName(String fileName)
	{
		this.fileName = Args.notEmpty(fileName, "fileName");
		return this;
	}

	/**
	 * @return the title the sheet is named after, {@code null} by default
	 */
	public IModel<String> getTitle()
	{
		return title;
	}

	/**
	 * @param title
	 *            the title the sheet is named after, such as {@code Customers}, read when the file
	 *            is written, without the characters Excel refuses in the name of a sheet
	 *            ({@code \ / ? * [ ] :}) and cut to its 31 characters; {@code null}, the default,
	 *            names it after the {@link #setFileName(String) file}, without its extension
	 * @return {@code this}
	 */
	public XlsxExportToolbarAction<T, K> setTitle(IModel<String> title)
	{
		this.title = title;
		return this;
	}

	/**
	 * @return the lines above the header, {@code null} by default
	 */
	public IModel<List<String>> getCaption()
	{
		return caption;
	}

	/**
	 * @param caption
	 *            the lines written above the header, the first in bold, and an empty row under
	 *            them, such as a title, the company the sheet is about and when it was made; read
	 *            when the file is written. The header stays frozen and filtered under them.
	 *            {@code null}, the default, or no lines put the header in the first row.
	 * @return {@code this}
	 */
	public XlsxExportToolbarAction<T, K> setCaption(IModel<List<String>> caption)
	{
		this.caption = caption;
		return this;
	}

	/**
	 * @return where the browser downloads the file, {@link Location#Blob} by default
	 */
	public Location getLocation()
	{
		return location;
	}

	/**
	 * @param location
	 *            where the browser downloads the file
	 * @return {@code this}
	 */
	public XlsxExportToolbarAction<T, K> setLocation(Location location)
	{
		this.location = Args.notNull(location, "location");
		return this;
	}

	/**
	 * @return whether the sheet ends with a row of the table's footer, {@code false} by default
	 * @see #setExportFooter(boolean)
	 */
	public boolean isExportFooter()
	{
		return exportFooter;
	}

	/**
	 * @param exportFooter
	 *            whether the sheet ends with a row of the table's
	 *            {@link BaseDynamicDataTable#hasFooter() footer}, in bold, under the rows: the
	 *            value of {@link IFooterColumn#getFooterExportValue()} for each column of a
	 *            {@link BeanBasedDynamicDataTable}, of
	 *            {@link IDynamicColumn#getFooterExportValue(Object)} for each column of a
	 *            {@link JsonBasedDynamicDataTable}, empty for the columns without one. Written
	 *            only when the table has a footer and the sheet holds all the rows, not only the
	 *            selected ones, which the footer does not sum up.
	 * @return {@code this}
	 */
	public XlsxExportToolbarAction<T, K> setExportFooter(boolean exportFooter)
	{
		this.exportFooter = exportFooter;
		return this;
	}

	/**
	 * @return the zone an {@code Instant} or a {@code java.util.Date} is shown in, {@code null}
	 *         by default for the system's
	 */
	public ZoneId getZoneId()
	{
		return zoneId;
	}

	/**
	 * @param zoneId
	 *            the zone an {@code Instant} or a {@code java.util.Date} is shown in, as a date
	 *            and time has none in Excel; {@code null}, the default, for the system's
	 * @return {@code this}
	 */
	public XlsxExportToolbarAction<T, K> setZoneId(ZoneId zoneId)
	{
		this.zoneId = zoneId;
		return this;
	}

	/**
	 * @return the rows a sheet holds
	 */
	int getMaxRows()
	{
		return maxRows;
	}

	/**
	 * @param maxRows
	 *            the rows a sheet holds, fewer than Excel's 1,048,576 for a test
	 * @return {@code this}
	 */
	XlsxExportToolbarAction<T, K> setMaxRows(int maxRows)
	{
		this.maxRows = maxRows;
		return this;
	}
}
