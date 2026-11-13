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

import org.apache.wicket.Component;

/**
 * A column of a {@link BeanBasedDynamicDataTable} with a value in the table's footer, under its
 * cells: a total, a count, an average. As soon as a column the table shows is one, the table
 * renders a footer row under its rows, with a cell for every shown column, in their order and with
 * their CSS classes; the cells of the other columns stay empty. The cell moves, hides and resizes
 * with its column, and is rendered again whenever the table
 * {@link BaseDynamicDataTable#refresh(org.apache.wicket.core.request.handler.IPartialPageRequestHandler)
 * refreshes} its rows, so a total computed by a model follows a change of the data.
 *
 * <pre>
 * class AmountColumn extends PropertyColumn&lt;Line, String&gt; implements IFooterColumn&lt;Line&gt;
 * {
 * 	&#64;Override
 * 	public Component newFooter(String id)
 * 	{
 * 		return new Label(id, () -&gt; format(invoice.getObject().total()));
 * 	}
 * }
 * </pre>
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public interface IFooterColumn<T>
{
	/**
	 * Creates the content of the footer's cell of the column, attached to a
	 * {@code <wicket:container>}: a label, or a panel, for example of several lines such as a
	 * subtotal, the tax and the total.
	 *
	 * @param id
	 *            the id the component must have
	 * @return the component, or {@code null} for an empty cell
	 */
	Component newFooter(String id);

	/**
	 * The value a CSV export {@link CsvExportToolbarAction#setExportFooter(boolean) writing the
	 * footer} writes under the column, in the last line of the file: the total, the count or the
	 * average of {@link #newFooter(String)}, for example as a number rather than as the text the
	 * footer shows. It is converted like the values of the rows.
	 *
	 * @return the value, or {@code null} for an empty field; {@code null} by default
	 * @since 11.0.0
	 */
	default Object getFooterExportValue()
	{
		return null;
	}
}
