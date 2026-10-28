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
package com.antiliacode.widgets.demo.datatable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.filter.FilterBehavior;

/**
 * A text field filtering users once the user pauses typing, shown in front of the navigation of the
 * dynamic table examples.
 */
public abstract class UserFilterPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	public UserFilterPanel(String id, IModel<String> filter)
	{
		super(id);

		TextField<String> field = new TextField<>("filter", filter);
		field.add(FilterBehavior.onFilter(this::onFilter));
		add(field);
	}

	/**
	 * Called after the filter changed.
	 */
	protected abstract void onFilter(AjaxRequestTarget target);
}
