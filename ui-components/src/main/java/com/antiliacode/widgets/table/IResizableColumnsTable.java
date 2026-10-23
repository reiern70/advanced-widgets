/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antiliacode.widgets.table;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * A table keeping on the server the widths the user resized its columns to, so that they survive
 * the table being rendered again. A {@link ResizableColumnsBehavior} added to such a table hands
 * the widths to the browser and reports every resize back; added to any other table, it keeps
 * the widths in the browser only.
 *
 * @since 11.0.0
 */
public interface IResizableColumnsTable
{
	/**
	 * @return the widths, in pixels, of the columns the table shows, one for each in display
	 *         order, or {@code null} while the user has not resized them
	 */
	double[] getShownColumnWidths();

	/**
	 * Called when the user resized a column.
	 *
	 * @param target
	 *            the handler of the request reporting the resize
	 * @param widths
	 *            the widths, in pixels, of the columns the table shows, one for each in display
	 *            order; each is positive and finite, but sent by the browser
	 */
	void columnsResized(AjaxRequestTarget target, double[] widths);
}
