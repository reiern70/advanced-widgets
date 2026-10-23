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
package com.antiliacode.widgets.dynamicdatatable;

import org.apache.wicket.model.IModel;

/**
 * A column showing the number of each row: its 1-based position across all pages, in the current
 * order, so the first row of the second page of ten is number 11. A row repainted on its own keeps
 * its number.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public class RowNumberColumn<T> extends AbstractDynamicColumn<T>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param header
	 *            the header text, rendered escaped
	 */
	public RowNumberColumn(IModel<String> header)
	{
		super(header);
	}

	@Override
	public String getTemplate()
	{
		return "{{@number}}";
	}

	/**
	 * @return {@code false}, the numbers needing no more than the width they are given
	 */
	@Override
	public boolean isResizable()
	{
		return false;
	}
}
