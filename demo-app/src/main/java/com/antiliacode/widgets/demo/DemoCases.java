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
package com.antiliacode.widgets.demo;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.tabs.Tab;
import com.antiliacode.widgets.tabs.Tabs;
import com.antiliacode.widgets.tabs.TabsDesign;

/**
 * The cases of a demo that has too many to show at once, as pills under the Wicket, React and
 * Svelte tabs: one case at a time, at the full width of the page. Each case is a {@link Fragment}
 * of the page's markup, its title the text of the page's bundle under the case's name. All the
 * cases are rendered, so that a form around them posts them all. The browser tells the server the
 * case the user selects, so that the page shows it again when rendered anew, after the form is
 * sent for example. A case has no changes to save: its pill never shows the mark of them.
 */
public class DemoCases extends Tabs
{
	private static final long serialVersionUID = 1L;

	private final MarkupContainer markupProvider;

	/**
	 * @param id
	 *            component id
	 * @param markupProvider
	 *            the container whose markup has the fragments of the cases, the page usually
	 */
	public DemoCases(String id, MarkupContainer markupProvider)
	{
		super(id);
		this.markupProvider = markupProvider;
		setDesign(TabsDesign.PILLS);
		setLabel(new ResourceModel("cases"));
		setSendToServer(true);
	}

	/**
	 * Adds a case, after the others.
	 *
	 * @param name
	 *            the name of the case, the key of its title in the bundle
	 * @param fragmentId
	 *            the id of the fragment of the case in the markup
	 * @return the fragment of the case, for the components of the case
	 */
	public Fragment addCase(String name, String fragmentId)
	{
		Fragment fragment = new Fragment("panel", fragmentId, markupProvider);
		// a case has no changes to save, whatever its fields hold
		addTab(new Tab(name, new ResourceModel(name))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newPanel(String id)
			{
				return fragment;
			}
		}.setModified(false));
		return fragment;
	}

	/**
	 * @param name
	 *            the name of a case
	 * @return the path of the fragment of the case, relative to the cases
	 */
	public static String pathOf(String name)
	{
		return "element:panels:items:" + name + ":panel";
	}
}
