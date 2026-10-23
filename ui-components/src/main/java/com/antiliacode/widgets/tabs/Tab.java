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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.icon.IIcon;

/**
 * A tab of {@link Tabs}: its title, maybe with an icon and {@link TabAction actions}, and its
 * panel, the component {@link #newPanel(String)} returns. {@link Tabs#addTab(Tab) Add it} to the
 * tabs, which create its panel then and render it with the others, hidden until the tab is
 * selected.
 * <p>
 * The tabs know a tab by its name, which they give it when it has none; one of the page's own
 * suits a page that finds its tabs again by it, the name of a document for example. A name is a
 * Wicket component id: letters, digits, dashes and underscores.
 * <p>
 * A {@link #setClosable(boolean) closable} tab has a button closing it. Closing a tab whose panel
 * has changes not saved asks the user first: the panel has changes when a field in it holds
 * another value than the one it was rendered with, or as {@link #setModified(Boolean)} says,
 * which {@link Tabs#setModified(Tab, Boolean, org.apache.wicket.ajax.AjaxRequestTarget)} changes
 * via Ajax too. Its tab shows a mark meanwhile.
 *
 * @since 11.0.0
 */
public abstract class Tab implements IClusterable, IDetachable
{
	private static final long serialVersionUID = 1L;

	private String name;

	private final IModel<String> title;

	private IIcon icon;

	private boolean closable;

	private Boolean modified;

	private final List<TabAction> actions = new ArrayList<>();

	/**
	 * A tab the tabs give a name when it is added.
	 *
	 * @param title
	 *            the title of the tab
	 */
	public Tab(IModel<String> title)
	{
		this(null, title);
	}

	/**
	 * @param name
	 *            the name of the tab, a Wicket component id; {@code null} for the tabs to give it
	 *            one
	 * @param title
	 *            the title of the tab
	 */
	public Tab(String name, IModel<String> title)
	{
		this.name = name;
		this.title = Args.notNull(title, "title");
	}

	/**
	 * Creates the panel of the tab.
	 *
	 * @param id
	 *            the id the component must have
	 * @return the component
	 */
	protected abstract Component newPanel(String id);

	/**
	 * @return the name of the tab, {@code null} until it is added to tabs when it was given none
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name
	 *            the name the tabs give the tab
	 */
	void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the title of the tab
	 */
	public IModel<String> getTitle()
	{
		return title;
	}

	/**
	 * @return the icon before the title, {@code null} for none
	 */
	public IIcon getIcon()
	{
		return icon;
	}

	/**
	 * @param icon
	 *            the icon before the title, {@code null} for none
	 * @return {@code this}
	 */
	public Tab setIcon(IIcon icon)
	{
		this.icon = icon;
		return this;
	}

	/**
	 * @return whether the tab has a button closing it, {@code false} by default
	 */
	public boolean isClosable()
	{
		return closable;
	}

	/**
	 * @param closable
	 *            whether the tab has a button closing it
	 * @return {@code this}
	 */
	public Tab setClosable(boolean closable)
	{
		this.closable = closable;
		return this;
	}

	/**
	 * @return whether the panel has changes not saved, {@code null}, as by default, for the
	 *         browser to tell by its fields
	 */
	public Boolean getModified()
	{
		return modified;
	}

	/**
	 * Says whether the panel has changes not saved, which the user is asked about before the tab
	 * closes, the next time it is rendered. Left {@code null}, the browser tells by the fields of
	 * the panel: whether one holds another value than the one it was rendered with. A page that
	 * hears of the changes, by the Ajax behaviors of its fields, says so itself.
	 *
	 * @param modified
	 *            whether the panel has changes not saved; {@code null} for the browser to tell
	 * @return {@code this}
	 * @see Tabs#setModified(Tab, Boolean, org.apache.wicket.ajax.AjaxRequestTarget)
	 */
	public Tab setModified(Boolean modified)
	{
		this.modified = modified;
		return this;
	}

	/**
	 * Adds an action, a button after the title before the one closing the tab.
	 *
	 * @param action
	 *            the action
	 * @return {@code this}
	 */
	public Tab addAction(TabAction action)
	{
		actions.add(Args.notNull(action, "action"));
		return this;
	}

	/**
	 * @return the actions, in order
	 */
	public List<TabAction> getActions()
	{
		return Collections.unmodifiableList(actions);
	}

	@Override
	public void detach()
	{
		title.detach();
		actions.forEach(TabAction::detach);
	}
}
