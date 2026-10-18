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
package com.antilia.widgets.tabs;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

import com.antilia.widgets.theme.LookBehavior;
import com.antilia.widgets.theme.LookPart;
import com.antilia.widgets.theme.Looks;

/**
 * Gives a {@link TabbedPanel} one of the looks of {@link TabsStyle}, by a CSS class only, so it
 * needs no inline style. The look comes from a model, so re-rendering the panel switches it.
 * Without this behavior a tabbed panel has no style sheet of its own and looks as the
 * application's CSS makes it look.
 * <p>
 * The looks mark the selected tab by the CSS class {@code selected}, the default of
 * {@link TabbedPanel#getSelectedTabCssClass()}; a panel configured with another class shows no
 * selected tab.
 *
 * @since 11.0.0
 */
public class TabsStyleBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The style sheet of the looks. */
	public static final ResourceReference CSS = new CssResourceReference(TabsStyleBehavior.class,
		"antilia-tabs.css");

	private final IModel<TabsStyle> style;

	/**
	 * @param style
	 *            the look of the tabs
	 */
	public TabsStyleBehavior(TabsStyle style)
	{
		this(Model.of(Args.notNull(style, "style")));
	}

	/**
	 * @param style
	 *            the look of the tabs; {@code null} as its object for none
	 */
	public TabsStyleBehavior(IModel<TabsStyle> style)
	{
		this.style = Args.notNull(style, "style");
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		TabsStyle current = style.getObject();
		if (current != null)
		{
			Looks.addClasses(tag, Looks.of(component), LookPart.TABS, current.getPart());
		}
	}

	/**
	 * Gives the row of the tabs of a {@link TabbedPanel} the classes of its part in the look of
	 * the panel, such as Bootstrap's {@code nav nav-tabs}. The tabs themselves, which the panel
	 * makes anew each time it renders, keep Wicket's markup: the style sheet of a look styles
	 * them inside the row.
	 */
	@Override
	public void bind(Component component)
	{
		if (component instanceof TabbedPanel<?> panel && panel.get("tabs-container") != null)
		{
			panel.get("tabs-container").add(new LookBehavior()
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected LookPart[] getParts(Component container)
				{
					TabsStyle current = style.getObject();
					return new LookPart[] { LookPart.TAB_ROW,
						current != null ? current.getRowPart() : null };
				}
			});
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		response.render(CssHeaderItem.forReference(CSS));
	}

	@Override
	public void detach(Component component)
	{
		style.detach();
	}
}
