/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.theme;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.ajax.markup.html.modal.theme.DefaultTheme;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

import com.advanced.widgets.collapsible.CollapsiblePanel;
import com.advanced.widgets.floating.FloatingPanel;
import com.advanced.widgets.tabs.TabsStyle;
import com.advanced.widgets.tabs.TabsStyleBehavior;

/**
 * The panels of the theme editor's preview: a collapsible panel, a floating panel, tabs styled as
 * pills and a modal dialog.
 */
class PanelsPreview extends Panel
{
	private static final long serialVersionUID = 1L;

	private final ModalDialog dialog;

	PanelsPreview(String id)
	{
		super(id);
		add(new CollapsiblePanel("collapsible", new ResourceModel("collapsible.title"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				return new Label(id, new ResourceModel("collapsible.body"));
			}
		}.setExpanded(true));

		add(new FloatingPanel("floating", new ResourceModel("floating.title"))
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

		List<ITab> tabs = new ArrayList<>();
		for (String title : List.of("First", "Second", "Third"))
		{
			tabs.add(new AbstractTab(Model.of(title))
			{
				private static final long serialVersionUID = 1L;

				@Override
				public WebMarkupContainer getPanel(String panelId)
				{
					Fragment content = new Fragment(panelId, "tabContent", PanelsPreview.this);
					content.add(new Label("text", title));
					return content;
				}
			});
		}
		add(new AjaxTabbedPanel<>("tabs", tabs).add(new TabsStyleBehavior(TabsStyle.PILLS)));

		dialog = new ModalDialog("dialog");
		dialog.add(new DefaultTheme());
		dialog.closeOnEscape();
		add(dialog);
		add(new AjaxLink<Void>("openDialog")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				Fragment content = new Fragment(ModalDialog.CONTENT_ID, "dialogContent",
					PanelsPreview.this);
				content.add(new AjaxLink<Void>("close")
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						dialog.close(target);
					}
				});
				dialog.open(content, target);
			}
		});
	}
}
