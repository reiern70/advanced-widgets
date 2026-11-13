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

import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antiliacode.widgets.progress.ProgressBar;

/**
 * A column of a {@link BeanBasedDynamicDataTable} showing a {@link ProgressBar} for a percentage
 * of each row: how much of something is done, collected, paid or used. It is the counterpart of
 * {@link ProgressBarColumn}, whose bar the browser renders from the row's JSON in a
 * {@link JsonBasedDynamicDataTable}.
 *
 * <pre>
 * new BeanProgressBarColumn&lt;Row, String&gt;(Model.of("Collected"), Row::percentCollected)
 * 	.setSortProperty("percentCollected")
 * 	.setLabel(row -&gt; row.collected() + " of " + row.total());
 * </pre>
 * <p>
 * The bar is in the look of the table, its percentage rounded and clamped to 0..100 on the
 * server; a row whose percentage is {@code null} shows an indeterminate bar. The bar shows its
 * percentage, or the {@link #setLabel(SerializableFunction) label} of the row instead, and is
 * named for screen readers after the column's header, which read its value from the
 * {@code <progress>} element. The column contributes the style sheet of the bar, also to a table
 * without rows yet.
 * <p>
 * The column is exportable: a CSV export writes the percentage of the row as it is, not rounded
 * nor clamped. It is sortable once it has a {@link #setSortProperty(Object) sort property}.
 *
 * @param <T>
 *            the type of the rows
 * @param <S>
 *            the type of the sort properties
 * @since 11.0.0
 */
public class BeanProgressBarColumn<T, S> extends AbstractColumn<T, S>
	implements
		IExportableColumn<T, S>,
		IHeaderContributor
{
	private static final long serialVersionUID = 1L;

	private final SerializableFunction<? super T, ? extends Number> percentage;

	private S sortProperty;

	private SerializableFunction<? super T, String> label;

	/**
	 * @param header
	 *            the header text, rendered escaped, also the accessible name of the bars
	 * @param percentage
	 *            returns the percentage of a row, from 0 to 100, such as
	 *            {@code Row::percentCollected}; {@code null} for an indeterminate bar
	 */
	public BeanProgressBarColumn(IModel<String> header,
		SerializableFunction<? super T, ? extends Number> percentage)
	{
		super(header);
		this.percentage = Args.notNull(percentage, "percentage");
	}

	/**
	 * @param header
	 *            the header text, rendered escaped, also the accessible name of the bars
	 * @param property
	 *            the property expression of the percentage of a row, from 0 to 100, such as
	 *            {@code percentCollected}, read as a {@link PropertyResolver} reads it
	 */
	public BeanProgressBarColumn(IModel<String> header, String property)
	{
		this(header, percentageOf(Args.notEmpty(property, "property")));
	}

	private static <T> SerializableFunction<T, Number> percentageOf(String property)
	{
		return row -> (Number)PropertyResolver.getValue(property, row);
	}

	/**
	 * @param sortProperty
	 *            the property the column sorts the rows by, or {@code null} for a column that does
	 *            not sort them
	 * @return {@code this}
	 */
	public BeanProgressBarColumn<T, S> setSortProperty(S sortProperty)
	{
		this.sortProperty = sortProperty;
		return this;
	}

	@Override
	public S getSortProperty()
	{
		return sortProperty;
	}

	/**
	 * Shows a text of the row on its bar instead of the percentage, such as {@code 3 of 4}.
	 *
	 * @param label
	 *            returns the text of a row, written escaped; {@code null} for the percentage
	 * @return {@code this}
	 */
	public BeanProgressBarColumn<T, S> setLabel(SerializableFunction<? super T, String> label)
	{
		this.label = label;
		return this;
	}

	@Override
	public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
		IModel<T> rowModel)
	{
		cellItem.add(new ProgressBarCell(componentId, getDataModel(rowModel),
			label != null ? rowModel.map(label) : null, getDisplayModel()));
	}

	/**
	 * @return the percentage of the row, as it is, for example for a CSV export
	 */
	@Override
	public IModel<? extends Number> getDataModel(IModel<T> rowModel)
	{
		return rowModel.map(percentage);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		response.render(CssHeaderItem.forReference(ProgressBar.CSS));
	}
}
