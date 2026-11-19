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
package com.antiliacode.widgets.demo.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;

import com.antiliacode.widgets.filter.FilterBehavior;

/**
 * The filter of a table of the administration, shown in front of its navigation (see
 * {@link FilteredAdminTable}): its fields filter while the administrator types or chooses, the
 * rows of the table veiled while a filter's request runs.
 */
abstract class AdminFilterPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final List<FilterBehavior> filters = new ArrayList<>();

	private final List<Component> veiled = new ArrayList<>();

	AdminFilterPanel(String id)
	{
		super(id);
	}

	/**
	 * Veils the components while the request of a field's filter runs, the fields left usable.
	 *
	 * @param components
	 *            what the filters fill, outside this panel: the rows of the table
	 * @return {@code this}
	 */
	AdminFilterPanel veil(Component... components)
	{
		veiled.addAll(List.of(components));
		filters.forEach(filter -> filter.veil(components));
		return this;
	}

	/**
	 * Makes the field filter on the event, once the administrator paused.
	 *
	 * @param field
	 *            a field of the filter
	 * @param event
	 *            {@code input} for a text, {@code change} for a choice
	 * @return the field
	 */
	protected <T> FormComponent<T> filtering(FormComponent<T> field, String event)
	{
		FilterBehavior filter = FilterBehavior.onFilter(event, this::onFilter);
		filter.veil(veiled.toArray(Component[]::new));
		filters.add(filter);
		field.add(filter);
		return field;
	}

	/**
	 * Called after the filter changed.
	 */
	protected abstract void onFilter(AjaxRequestTarget target);
}
