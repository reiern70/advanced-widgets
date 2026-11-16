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

/**
 * A column of a {@link BeanBasedDynamicDataTable} giving the number format of its cells in an
 * {@link XlsxExportToolbarAction Excel export}, as {@link IDynamicColumn#getExportFormat()} does
 * for a column of a {@link JsonBasedDynamicDataTable}. The column is exported if it is also an
 * {@link org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn}.
 *
 * <pre>
 * class AmountColumn extends PropertyColumn&lt;Invoice, String&gt; implements IExportFormatColumn
 * {
 * 	&#64;Override
 * 	public String getExportFormat()
 * 	{
 * 		return "#,##0.00";
 * 	}
 * }
 * </pre>
 *
 * @since 11.0.0
 */
public interface IExportFormatColumn
{
	/**
	 * @return the number format of the column's cells holding numbers or dates, in the codes of
	 *         Excel, such as {@code #,##0.00} for an amount with two decimals, {@code 0.00%} for a
	 *         rate such as {@code 0.07} or {@code dd/mm/yyyy} for a date; {@code null} for
	 *         Excel's own
	 */
	String getExportFormat();
}
