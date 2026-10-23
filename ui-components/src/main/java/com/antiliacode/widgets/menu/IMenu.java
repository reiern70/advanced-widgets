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

import java.util.List;

import org.apache.wicket.Component;

/**
 * An item of a {@link Menu} that opens a submenu of more items, which may be submenus too, as deep
 * as one likes. Its label, icon and tooltip are those of the item opening it; it has no link. A
 * menu takes an {@code IMenu} as its items too: its label is then the accessible name of the bar.
 * {@link SubMenu} is the one of ui-components.
 *
 * @since 11.0.0
 */
public interface IMenu extends IMenuItem
{
	/**
	 * @return the items of the submenu, in order
	 */
	List<? extends IMenuItem> getItems();

	/**
	 * A submenu has no link: its item opens it.
	 *
	 * @return {@code null}
	 */
	@Override
	default Component getLink(String id)
	{
		return null;
	}

	@Override
	default void detach()
	{
		getItems().forEach(IMenuItem::detach);
	}
}
