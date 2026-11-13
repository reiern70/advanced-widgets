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
 * An {@link IDynamicDataProvider} that also gives the data of the footer row of a
 * {@link JsonBasedDynamicDataTable}, such as the totals of its columns. The table sends it along
 * with the rows, as its {@link JsonBasedDynamicDataTable#getJsonSerializer() serializer} writes
 * it, and the browser fills the {@link IDynamicColumn#getFooterTemplate() footer templates} of the
 * columns with it, as it fills the cell templates with a row: a footer template
 * {@code {{total}}} writes the property {@code total} of the footer data. A
 * {@link JsonRowsResource} sends it too, when the provider it creates is one.
 *
 * <pre>
 * class LinesProvider extends SortableDynamicDataProvider&lt;Line, Long, String&gt;
 * 	implements IFooterDataProvider
 * {
 * 	&#64;Override
 * 	public Object footer()
 * 	{
 * 		return Map.of("total", format(invoice.total()));
 * 	}
 * }
 * </pre>
 *
 * @since 11.0.0
 */
public interface IFooterDataProvider
{
	/**
	 * Called whenever the table sends rows to the browser, so the footer follows a change of the
	 * data.
	 *
	 * @return the data of the footer, for example a map or a bean of the totals; {@code null} to
	 *         fill the footer templates with nothing
	 */
	Object footer();
}
