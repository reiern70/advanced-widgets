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
package com.antilia.widgets.demo.theme;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antilia.widgets.collapsible.CollapsiblePanel;
import com.antilia.widgets.floating.FloatingPanel;
import com.antilia.widgets.modal.IModalButtonFactory;
import com.antilia.widgets.modal.Modal;
import com.antilia.widgets.tabs.TabsStyle;
import com.antilia.widgets.tabs.TabsStyleBehavior;

/**
 * The panels of the theme editor's preview, each in a collapsible panel of its own: a collapsible
 * panel, collapsible panels nested two levels deep, a floating panel, tabs styled as pills and a
 * modal dialog.
 */
class PanelsPreview extends Panel
{
	private static final long serialVersionUID = 1L;

	private Modal dialog;

	PanelsPreview(String id)
	{
		super(id);
		add(collapsible("collapsible", "collapsible.title",
			bodyId -> new Label(bodyId, new ResourceModel("collapsible.body"))).setExpanded(true));
		add(collapsible("nested", "nested.title", this::newNested).setExpanded(true));
		add(collapsible("floating", "floating.section", this::newFloating));
		add(collapsible("tabs", "tabs.section", this::newTabs));
		add(collapsible("dialog", "dialog.section", this::newDialog));
	}

	/**
	 * @return a collapsible panel with the title from the bundle and the body of the function
	 */
	private static CollapsiblePanel collapsible(String id, String titleKey,
		SerializableFunction<String, Component> body)
	{
		return new CollapsiblePanel(id, new ResourceModel(titleKey))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				return body.apply(id);
			}
		};
	}

	/**
	 * @return collapsible panels in a collapsible panel, the first holding one more level
	 */
	private Component newNested(String id)
	{
		Fragment nested = new Fragment(id, "nestedContent", this);
		nested.add(new Label("text", new ResourceModel("nested.body")));
		nested.add(collapsible("first", "nested.first",
			bodyId -> {
				Fragment first = new Fragment(bodyId, "nestedContent", this);
				first.add(new Label("text", new ResourceModel("nested.first.body")));
				first.add(collapsible("first", "nested.deeper",
					deeperId -> new Label(deeperId, new ResourceModel("nested.deeper.body")))
					.setExpanded(true));
				first.add(new WebMarkupContainer("second").setVisible(false));
				return first;
			}).setExpanded(true));
		nested.add(collapsible("second", "nested.second",
			bodyId -> new Label(bodyId, new ResourceModel("nested.second.body"))));
		return nested;
	}

	private Component newFloating(String id)
	{
		Fragment content = new Fragment(id, "floatingContent", this);
		content.add(new FloatingPanel("floating", new ResourceModel("floating.title"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				return new Label(id, new ResourceModel("floating.body"));
			}

			@Override
			protected void onClose(AjaxRequestTarget target)
			{
			}
		});
		return content;
	}

	private Component newTabs(String id)
	{
		Fragment content = new Fragment(id, "tabsContent", this);
		List<ITab> tabs = new ArrayList<>();
		for (String title : List.of("tab.first", "tab.second", "tab.third"))
		{
			tabs.add(new AbstractTab(new ResourceModel(title))
			{
				private static final long serialVersionUID = 1L;

				@Override
				public WebMarkupContainer getPanel(String panelId)
				{
					Fragment tab = new Fragment(panelId, "tabContent", PanelsPreview.this);
					tab.add(new Label("text", new ResourceModel(title)));
					return tab;
				}
			});
		}
		content.add(new AjaxTabbedPanel<>("tabs", tabs).add(new TabsStyleBehavior(TabsStyle.PILLS)));
		return content;
	}

	private Component newDialog(String id)
	{
		Fragment content = new Fragment(id, "dialogSectionContent", this);
		dialog = new Modal("dialog");
		content.add(dialog);
		content.add(new AjaxLink<Void>("openDialog")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				dialog.open(new ResourceModel("dialog.title"),
					new Label(Modal.BODY_ID, new ResourceModel("dialog.body")), target,
					IModalButtonFactory.close(new ResourceModel("dialog.close")));
			}
		});
		return content;
	}
}
