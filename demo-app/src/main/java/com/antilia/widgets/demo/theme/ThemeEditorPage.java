/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.theme;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.string.Strings;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.dropdown.DropdownBehavior;
import com.antilia.widgets.tabs.TabsStyle;
import com.antilia.widgets.tabs.TabsStyleBehavior;
import com.antilia.widgets.theme.ThemeChoice;
import com.antilia.widgets.theme.ThemeEditorPanel;

/**
 * Generates a theme with a {@link ThemeEditorPanel}: the pickers of the theme's properties on top,
 * and below them tabs as wide as the page previewing every component in the edited colors. The
 * editor starts from the theme chosen for the whole page, here or in the top bar.
 */
public class ThemeEditorPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public ThemeEditorPage()
	{
		// the theme the pickers start from: the page's, as in the top bar
		Form<Void> start = new Form<>("start");
		add(start);
		start.add(new ThemeChoice("theme", getThemeModel())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onThemeChanged(AjaxRequestTarget target)
			{
				setResponsePage(getPage());
			}
		}.add(new DropdownBehavior()));

		WebMarkupContainer preview = newThemedContainer("preview");
		add(new ThemeEditorPanel("editor", preview));
		add(preview);

		List<ITab> tabs = List.of(tab("tab.progress", ProgressPreview::new, true),
			tab("tab.jsonTable", JsonTablePreview::new, false),
			tab("tab.beanTable", BeanTablePreview::new, true),
			tab("tab.panels", PanelsPreview::new, true));
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
	 *            whether the component of the tab is public, false if it is private for now
	 */
	private ITab tab(String titleKey, SerializableFunction<String, WebMarkupContainer> panel,
		boolean publicComponent)
	{
		IModel<String> title = () -> Strings.escapeMarkup(getString(titleKey)) +
			"<span class=\"visibility tab-visibility " +
			(publicComponent ? "visibility--public" : "visibility--private") + "\" title=\"" +
			Strings.escapeMarkup(getString(publicComponent ? "component.public.help"
				: "component.private.help")) + "\">" +
			visibilityIcon(publicComponent, this) + "</span>";
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
