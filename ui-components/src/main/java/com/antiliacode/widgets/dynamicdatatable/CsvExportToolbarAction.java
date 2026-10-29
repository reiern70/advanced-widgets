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
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior.Location;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.lang.Args;

/**
 * An action exporting the rows of a {@link BaseDynamicDataTable} as a CSV file, shown as an icon
 * button.
 * <p>
 * The file holds the {@link BaseDynamicDataTable#getSelection() selected} rows if there are any,
 * and every row of the provider otherwise, in the provider's order. Its columns are those the table
 * shows that are {@link IDynamicColumn#isExportable() exportable}, in display order, headed by
 * their header text. Dates
 * and times of {@code java.time}, such as {@code LocalDate}, are written in ISO-8601, for example
 * {@code 1957-10-17}, so other tools read them unambiguously; other values are converted with the
 * table's converters for its locale. Every field is quoted.
 * <p>
 * A spreadsheet reads a field starting with {@code =}, {@code +}, {@code -} or {@code @} as a
 * formula, and some read one starting with a tab or a line break the same way once they trim it.
 * Values entered by users, written as they are, could then run as formulas on the computer of
 * whoever opens the file. So, by default, such a field gets an apostrophe before it, which
 * spreadsheets take as the mark of a text: see {@link #escapeFormula(String)}. Numbers, such as
 * {@code -12.5}, stay as they are. {@link #setEscapeFormulas(boolean)} turns this off, for files
 * read by tools that take no formulas.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys of the rows
 * @since 11.0.0
 */
public class CsvExportToolbarAction<T, K extends Serializable>
	implements
		IDynamicToolbarAction<T, K>
{
	private static final long serialVersionUID = 1L;

	/** The characters a spreadsheet may start a formula with, or trims before one. */
	private static final String FORMULA_START = "=+-@\t\r\n";

	/** A number: a sign, digits in groups, maybe a fraction, an exponent and a percent sign. */
	private static final Pattern NUMBER = Pattern
		.compile("[+-]?\\d+(?:[.,' \\x{00A0}\\x{202F}]\\d+)*(?:[eE][+-]?\\d+)?%?");

	private IModel<String> label = new ResourceModel("DynamicDataTable.exportCsv", "Export to CSV");

	private String fileName = "export.csv";

	private char delimiter = ',';

	private Location location = Location.Blob;

	private boolean escapeFormulas = true;

	@Override
	public Component newComponent(String id, BaseDynamicDataTable<T, K, ?> table)
	{
		return new CsvExportButton<>(id, table, this);
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
	 *            {@code DynamicDataTable.exportCsv} by default
	 * @return {@code this}
	 */
	public CsvExportToolbarAction<T, K> setLabel(IModel<String> label)
	{
		this.label = Args.notNull(label, "label");
		return this;
	}

	/**
	 * @return the name of the file, {@code export.csv} by default
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * @param fileName
	 *            the name of the file
	 * @return {@code this}
	 */
	public CsvExportToolbarAction<T, K> setFileName(String fileName)
	{
		this.fileName = Args.notEmpty(fileName, "fileName");
		return this;
	}

	/**
	 * @return the character separating the fields, a comma by default
	 */
	public char getDelimiter()
	{
		return delimiter;
	}

	/**
	 * @param delimiter
	 *            the character separating the fields
	 * @return {@code this}
	 */
	public CsvExportToolbarAction<T, K> setDelimiter(char delimiter)
	{
		this.delimiter = delimiter;
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
	public CsvExportToolbarAction<T, K> setLocation(Location location)
	{
		this.location = Args.notNull(location, "location");
		return this;
	}

	/**
	 * @return whether a field a spreadsheet could read as a formula gets an apostrophe before it,
	 *         {@code true} by default
	 * @see #escapeFormula(String)
	 */
	public boolean isEscapeFormulas()
	{
		return escapeFormulas;
	}

	/**
	 * @param escapeFormulas
	 *            whether a field a spreadsheet could read as a formula gets an apostrophe before
	 *            it, see {@link #escapeFormula(String)}; turn it off only when no value of the file
	 *            comes from users, or when no spreadsheet reads the file
	 * @return {@code this}
	 */
	public CsvExportToolbarAction<T, K> setEscapeFormulas(boolean escapeFormulas)
	{
		this.escapeFormulas = escapeFormulas;
		return this;
	}

	/**
	 * Keeps a spreadsheet from reading a field of a CSV file as a formula: a value starting with
	 * {@code =}, {@code +}, {@code -}, {@code @}, a tab, a carriage return or a line feed gets an
	 * apostrophe before it, which spreadsheets take as the mark of a text. A number, such as
	 * {@code -12.5}, {@code +1,234.50} or {@code -3%}, stays as it is: it cannot be a formula. For
	 * other writers of CSV files too.
	 *
	 * @param value
	 *            the value of a field, may be {@code null}
	 * @return the value, with an apostrophe before it when a spreadsheet could read it as a formula
	 */
	public static String escapeFormula(String value)
	{
		if (value == null || value.isEmpty() || FORMULA_START.indexOf(value.charAt(0)) < 0 ||
			NUMBER.matcher(value).matches())
		{
			return value;
		}
		return "'" + value;
	}
}
