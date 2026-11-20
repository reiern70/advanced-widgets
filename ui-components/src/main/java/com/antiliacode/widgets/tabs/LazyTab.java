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
package com.antiliacode.widgets.tabs;

import org.apache.wicket.model.IModel;

/**
 * A {@link Tab} whose panel is created when the tab is first selected, rather than when it is
 * added: a panel heavy to render, a document, an editor or a large table, costs nothing until the
 * user goes to it. Until then {@link TabbedPanel} renders an empty panel; selecting the tab asks
 * the server via Ajax for the panel, which shows a spinner meanwhile. A lazy tab selected when the
 * tabs are rendered has its panel at once, as does one {@link TabbedPanel#select(Tab,
 * org.apache.wicket.ajax.AjaxRequestTarget) selected via Ajax}.
 * <p>
 * By default the tab keeps its panel once created, on the server and in the browser, as the panels
 * of other tabs are kept: switching back to it needs no request, and what the user typed in it
 * stays. {@link #setCaching(boolean) setCaching(false)} has the tab drop its panel when another tab
 * is selected, as Wicket's {@code AjaxTabbedPanel} does: the panel is created anew each time the
 * tab is selected, and what the user typed in it and did not send is lost. The browser then tells
 * the server each tab the user selects, which the model of the tabs takes.
 * <p>
 * The panels of the other tabs are created when they are added, as always: a page whose Ajax
 * updates reach components in the panels of tabs that are not selected keeps them there.
 *
 * @since 11.0.0
 */
public abstract class LazyTab extends Tab
{
	private static final long serialVersionUID = 1L;

	private boolean caching = true;

	/**
	 * A lazy tab the tabs give a name when it is added.
	 *
	 * @param title
	 *            the title of the tab
	 */
	public LazyTab(IModel<String> title)
	{
		super(title);
	}

	/**
	 * @param name
	 *            the name of the tab, a Wicket component id; {@code null} for the tabs to give it
	 *            one
	 * @param title
	 *            the title of the tab
	 */
	public LazyTab(String name, IModel<String> title)
	{
		super(name, title);
	}

	/**
	 * @return whether the tab keeps its panel once created, {@code true} by default
	 */
	public boolean isCaching()
	{
		return caching;
	}

	/**
	 * Whether the tab keeps its panel once created, or drops it when another tab is selected, to
	 * create it anew the next time it is selected.
	 *
	 * @param caching
	 *            whether the tab keeps its panel
	 * @return {@code this}
	 */
	public LazyTab setCaching(boolean caching)
	{
		this.caching = caching;
		return this;
	}
}
