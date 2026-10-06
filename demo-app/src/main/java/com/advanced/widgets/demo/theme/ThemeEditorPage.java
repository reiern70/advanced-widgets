/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.theme;

import java.util.List;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.advanced.widgets.demo.DemoPage;
import com.advanced.widgets.tabs.TabsStyle;
import com.advanced.widgets.tabs.TabsStyleBehavior;
import com.advanced.widgets.theme.ThemeEditorPanel;

/**
 * Generates a theme with a {@link ThemeEditorPanel}: the pickers of the theme's properties on top,
 * and below them tabs previewing every component in the edited colors. The editor starts from the
 * theme chosen for the whole page.
 */
public class ThemeEditorPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public ThemeEditorPage()
	{
		WebMarkupContainer preview = newThemedContainer("preview");
		add(new ThemeEditorPanel("editor", preview));
		add(preview);

		List<ITab> tabs = List.of(tab("tab.progress", ProgressPreview::new),
			tab("tab.jsonTable", JsonTablePreview::new), tab("tab.beanTable", BeanTablePreview::new),
			tab("tab.panels", PanelsPreview::new));
		preview.add(new AjaxTabbedPanel<>("tabs", tabs).add(new TabsStyleBehavior(TabsStyle.TABS)));
	}

	private static ITab tab(String titleKey, SerializableFunction<String, WebMarkupContainer> panel)
	{
		return new AbstractTab(new ResourceModel(titleKey))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId)
			{
				return panel.apply(panelId);
			}
		};
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(ThemeEditorPage.class, "ThemeEditorPage.css")));
	}
}
