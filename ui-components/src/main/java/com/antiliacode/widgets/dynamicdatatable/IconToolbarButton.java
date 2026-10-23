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

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;

class IconToolbarButton<T, K extends Serializable> extends Panel
{
	private static final long serialVersionUID = 1L;

	IconToolbarButton(String id, BaseDynamicDataTable<T, K, ?> table,
		IconBasedToolbarAction<T, K> action)
	{
		super(id);

		IModel<String> tooltip = action.getTooltip();
		IModel<String> assigned = tooltip instanceof IComponentAssignedModel<String> resource
			? resource.wrapOnAssignment(this) : tooltip;
		AjaxLink<Void> button = new AjaxLink<>("button")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				action.onClick(table, target);
			}
		};
		button.add(AttributeModifier.replace("title", assigned),
			AttributeModifier.replace("aria-label", assigned));
		button.add(new Label("icon", () -> action.getIcon().getMarkup())
			.setEscapeModelStrings(false).setRenderBodyOnly(true));
		button.add(new LookBehavior(LookPart.TABLE_ACTION, LookPart.TABLE_ICON_ACTION));
		add(button);
	}
}
