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
package com.antiliacode.widgets.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.icon.IIcon;

/**
 * An item opening a submenu of the items {@link #add(IMenuItem...) added} to it, which may be
 * submenus too:
 *
 * <pre>
 * SubMenu file = new SubMenu(Model.of("File")).add(
 * 	AjaxMenuItem.of(Model.of("New"), target -&gt; ...),
 * 	new SubMenu(Model.of("Export")).add(...),
 * 	MenuSeparator.INSTANCE,
 * 	new BookmarkableMenuItem(Model.of("Help"), HelpPage.class));
 * </pre>
 *
 * The menu hides a submenu none of whose items shows.
 *
 * @since 11.0.0
 */
public class SubMenu extends MenuItem implements IMenu
{
	private static final long serialVersionUID = 1L;

	private final List<IMenuItem> items = new ArrayList<>();

	/**
	 * @param label
	 *            the label of the item opening the submenu
	 */
	public SubMenu(IModel<String> label)
	{
		super(label);
	}

	/**
	 * Adds items, after the others.
	 *
	 * @param items
	 *            the items
	 * @return {@code this}
	 */
	public SubMenu add(IMenuItem... items)
	{
		for (IMenuItem item : items)
		{
			this.items.add(Args.notNull(item, "item"));
		}
		return this;
	}

	@Override
	public List<IMenuItem> getItems()
	{
		return Collections.unmodifiableList(items);
	}

	@Override
	public SubMenu setTooltip(IModel<String> tooltip)
	{
		super.setTooltip(tooltip);
		return this;
	}

	@Override
	public SubMenu setIcon(IModel<IIcon> icon)
	{
		super.setIcon(icon);
		return this;
	}

	@Override
	public SubMenu setIcon(IIcon icon)
	{
		super.setIcon(icon);
		return this;
	}

	@Override
	public SubMenu setVisible(IModel<Boolean> visible)
	{
		super.setVisible(visible);
		return this;
	}

	@Override
	public SubMenu setVisible(boolean visible)
	{
		super.setVisible(visible);
		return this;
	}

	@Override
	public SubMenu setEnabled(IModel<Boolean> enabled)
	{
		super.setEnabled(enabled);
		return this;
	}

	@Override
	public SubMenu setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		return this;
	}

	@Override
	public SubMenu setSelected(IModel<Boolean> selected)
	{
		super.setSelected(selected);
		return this;
	}

	@Override
	public SubMenu setSelected(boolean selected)
	{
		super.setSelected(selected);
		return this;
	}

	@Override
	public void detach()
	{
		super.detach();
		items.forEach(IMenuItem::detach);
	}
}
