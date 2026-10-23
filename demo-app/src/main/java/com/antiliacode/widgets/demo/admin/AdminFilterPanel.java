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

import java.time.Duration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * The filter of a table of the administration, shown in front of its navigation (see
 * {@link FilteredAdminTable}): its fields filter while the administrator types or chooses.
 */
abstract class AdminFilterPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	AdminFilterPanel(String id)
	{
		super(id);
	}

	/**
	 * Makes the field filter on the event, a moment after the last one.
	 *
	 * @param field
	 *            a field of the filter
	 * @param event
	 *            {@code input} for a text, {@code change} for a choice
	 * @return the field
	 */
	protected <T> FormComponent<T> filtering(FormComponent<T> field, String event)
	{
		field.add(new AjaxFormComponentUpdatingBehavior(event)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				attributes.setThrottlingSettings(
					new ThrottlingSettings(Duration.ofMillis(300), true));
			}

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				onFilter(target);
			}
		});
		return field;
	}

	/**
	 * Called after the filter changed.
	 */
	protected abstract void onFilter(AjaxRequestTarget target);
}
