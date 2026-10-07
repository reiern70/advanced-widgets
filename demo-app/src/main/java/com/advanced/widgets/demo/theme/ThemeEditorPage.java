/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.theme;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.string.Strings;
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

		List<ITab> tabs = List.of(tab("tab.progress", ProgressPreview::new, null),
			tab("tab.jsonTable", JsonTablePreview::new, false),
			tab("tab.beanTable", BeanTablePreview::new, true),
			tab("tab.panels", PanelsPreview::new, null));
		preview.add(new AjaxTabbedPanel<>("tabs", tabs)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newTitle(String titleId, IModel<?> titleModel, int index)
			{
				return new Label(titleId, titleModel).setEscapeModelStrings(false);
			}
		}.add(new TabsStyleBehavior(TabsStyle.TABS)));
	}

	/**
	 * @param publicComponent
	 *            whether the component of the tab is public, false if it is private for now, or
	 *            {@code null} for no badge
	 */
	private ITab tab(String titleKey, SerializableFunction<String, WebMarkupContainer> panel,
		Boolean publicComponent)
	{
		IModel<String> title = () -> {
			String text = Strings.escapeMarkup(getString(titleKey)).toString();
			if (publicComponent == null)
			{
				return text;
			}
			String key = publicComponent ? "component.public" : "component.private";
			return text + "<span class=\"badge tab-badge " +
				(publicComponent ? "badge--public" : "badge--private") + "\">" +
				Strings.escapeMarkup(getString(key)) + "</span>";
		};
		return new AbstractTab(title)
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
