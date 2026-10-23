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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.icon.IIcon;

/**
 * An action of a {@link Tab}: a button after its title, before the one closing it, showing an
 * icon, with a title telling what it does. A click on it calls {@link #onClick(AjaxRequestTarget,
 * Tab, Component)} via Ajax, without selecting the tab: an action telling about what the panel of
 * the tab holds, for example, in a {@link com.antiliacode.widgets.popover.Popover} pointing at the
 * button.
 *
 * @since 11.0.0
 */
public abstract class TabAction implements IClusterable, IDetachable
{
	private static final long serialVersionUID = 1L;

	private final IIcon icon;

	private final IModel<String> title;

	/**
	 * @param icon
	 *            the icon of the button
	 * @param title
	 *            what the action does, the title and the accessible name of the button
	 */
	public TabAction(IIcon icon, IModel<String> title)
	{
		this.icon = Args.notNull(icon, "icon");
		this.title = Args.notNull(title, "title");
	}

	/**
	 * Called via Ajax when the user clicks the button of the action.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param tab
	 *            the tab of the action
	 * @param button
	 *            the button of the action, which renders its markup id: a popover points at it
	 */
	protected abstract void onClick(AjaxRequestTarget target, Tab tab, Component button);

	/**
	 * @return the icon of the button
	 */
	public IIcon getIcon()
	{
		return icon;
	}

	/**
	 * @return what the action does, the title and the accessible name of the button
	 */
	public IModel<String> getTitle()
	{
		return title;
	}

	@Override
	public void detach()
	{
		title.detach();
	}
}
