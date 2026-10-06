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
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.advanced.widgets.collapsible.CollapsiblePanel;
import com.advanced.widgets.demo.DemoPage;
import com.advanced.widgets.progress.ProgressBar;
import com.advanced.widgets.tabs.TabsStyle;
import com.advanced.widgets.tabs.TabsStyleBehavior;
import com.advanced.widgets.theme.ThemeEditorPanel;

/**
 * Generates a theme with a {@link ThemeEditorPanel}: a picker per theme property, a preview of
 * themed components following every change, and the CSS class defining the theme, to copy into
 * an application's style sheet.
 */
public class ThemeEditorPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private final ThemeEditorPanel editor;

	private final ModalDialog dialog;

	/**
	 * Constructor.
	 */
	public ThemeEditorPage()
	{
		WebMarkupContainer preview = newThemedContainer("preview");
		add(preview);
		add(newThemeChoice("theme", preview));

		editor = new ThemeEditorPanel("editor", preview);
		add(editor);

		preview.add(new ProgressBar("bar", Model.of(62)));
		preview.add(new ProgressBar("indeterminate", Model.of((Integer)null)));
		preview.add(new CollapsiblePanel("collapsible", new ResourceModel("collapsible.title"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				return new Label(id, new ResourceModel("collapsible.body"));
			}
		}.setExpanded(true));
		preview.add(newTabs("tabs"));

		dialog = new ModalDialog("dialog");
		dialog.add(new DefaultTheme());
		dialog.closeOnEscape();
		preview.add(dialog);
		preview.add(new AjaxLink<Void>("openDialog")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				dialog.open(new DialogContent(ModalDialog.CONTENT_ID), target);
			}
		});
	}

	@Override
	protected void onThemeChanged(AjaxRequestTarget target)
	{
		editor.reload(target);
	}

	private static Component newTabs(String id)
	{
		List<ITab> tabs = new ArrayList<>();
		for (String title : List.of("First", "Second", "Third"))
		{
			tabs.add(new AbstractTab(Model.of(title))
			{
				private static final long serialVersionUID = 1L;

				@Override
				public WebMarkupContainer getPanel(String panelId)
				{
					return new TabContent(panelId, title);
				}
			});
		}
		return new AjaxTabbedPanel<>(id, tabs).add(new TabsStyleBehavior(TabsStyle.TABS));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(ThemeEditorPage.class, "ThemeEditorPage.css")));
	}

	/**
	 * The content of a tab of the preview.
	 */
	private static class TabContent extends Panel
	{
		private static final long serialVersionUID = 1L;

		TabContent(String id, String title)
		{
			super(id);
			add(new Label("text", title));
		}
	}

	/**
	 * The content of the dialog of the preview.
	 */
	private class DialogContent extends Panel
	{
		private static final long serialVersionUID = 1L;

		DialogContent(String id)
		{
			super(id);
			add(new AjaxLink<Void>("close")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					dialog.close(target);
				}
			});
		}
	}
}
