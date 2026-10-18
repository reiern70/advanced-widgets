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
package com.antilia.widgets.demo.datatable;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.IModel;

import com.antilia.widgets.clipboard.ClipboardCopyBehavior;
import com.antilia.widgets.dynamicdatatable.IHideableColumn;
import com.antilia.widgets.dynamicdatatable.ResourceDynamicColumn;
import com.antilia.widgets.table.IMovableColumn;

/**
 * Shows a user's phone numbers. The template is read from {@code PhonesColumn.html}, or from
 * {@code PhonesColumn_de.html} for German, the way a panel's markup is.
 */
public class PhonesColumn extends ResourceDynamicColumn<User>
	implements
		IHeaderContributor,
		IHideableColumn,
		IMovableColumn
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param header
	 *            the header text
	 */
	public PhonesColumn(IModel<String> header)
	{
		super(header);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		ClipboardCopyBehavior.renderHeadItems(response);
	}
}
