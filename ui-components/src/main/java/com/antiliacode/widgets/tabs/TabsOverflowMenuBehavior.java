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
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.menu.Menu;
import com.github.openjson.JSONObject;

/**
 * The menu of all tabs, an extension of {@link TabbedPanel} for more tabs than fit: add it to the
 * tabs, {@code tabs.add(new TabsOverflowMenuBehavior())}. While the list of tabs overflows,
 * scrolling sideways, or down for tabs down the side, a button at its end opens a menu of all the
 * tabs, in their order, each with its icon, its title and the mark of changes not saved, the tab
 * selected checked. Choosing one selects its tab as a click on it does, telling the server as the
 * tabs do, and scrolls it into view. The button stays at the end of the list as the tabs scroll
 * under it, and is hidden while all the tabs fit. The menu follows the tabs added and taken away,
 * their titles, as {@link TabbedPanel#updateTab(Tab, org.apache.wicket.ajax.AjaxRequestTarget)}
 * renders them, and their marks.
 * <p>
 * {@code Wicket.TabsOverflowMenu} from {@link #JS} does it in the browser; the menu is a menu of
 * {@link Menu}, in its style sheet, {@link Menu#CSS}. The button says
 * {@code TabsOverflowMenuBehavior.label}, "All tabs".
 *
 * @since 11.0.0
 */
public class TabsOverflowMenuBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The script of the menu of all tabs, {@code Wicket.TabsOverflowMenu}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		TabsOverflowMenuBehavior.class, "antilia-tabs-overflow-menu.js");

	@Override
	public void bind(Component component)
	{
		if (!(component instanceof TabbedPanel))
		{
			throw new IllegalArgumentException("A TabsOverflowMenuBehavior goes on a TabbedPanel, " +
				"not on " + component.getClass().getName());
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		TabbedPanel.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(Menu.CSS));
		response.render(JavaScriptHeaderItem.forReference(JS));
		JSONObject options = new JSONObject();
		String label = component.getString("TabsOverflowMenuBehavior.label", null, "");
		if (!Strings.isEmpty(label))
		{
			options.put("label", label);
		}
		response.render(OnDomReadyHeaderItem.forScript("Wicket.TabsOverflowMenu.attach(" +
			ScriptJson.quote(((TabbedPanel)component).getTabsMarkupId()) + ", " +
			ScriptJson.of(options) + ");"));
	}
}
