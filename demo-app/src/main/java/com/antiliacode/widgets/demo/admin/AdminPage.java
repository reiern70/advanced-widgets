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
package com.antiliacode.widgets.demo.admin;

import java.util.List;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.demo.account.DemosAuthorization;
import com.antiliacode.widgets.tabs.TabsStyle;
import com.antiliacode.widgets.tabs.TabsStyleBehavior;

/**
 * The administration, a tab on top for each section: the {@link UsersPanel registered users}, the
 * recent {@link AccessesPanel accesses} and the old ones. Only for administrators, see
 * {@link DemosAuthorization}.
 */
public class AdminPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/** The rows on a page of the tables at first. */
	static final int ROWS_PER_PAGE = 25;

	public AdminPage()
	{
		List<ITab> tabs = List.of(new AbstractTab(new ResourceModel("tab.users"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId)
			{
				return new UsersPanel(panelId);
			}
		}, new AbstractTab(new ResourceModel("tab.accesses"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId)
			{
				return new AccessesPanel(panelId, false);
			}
		}, new AbstractTab(new ResourceModel("tab.oldAccesses"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(String panelId)
			{
				return new AccessesPanel(panelId, true);
			}
		});
		add(new AjaxTabbedPanel<>("tabs", tabs).add(new TabsStyleBehavior(TabsStyle.TABS)));
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		// shown again after the session stopped being an administrator's, which Wicket refuses
		// without telling where to go
		if (!isActionAuthorized(RENDER))
		{
			DemosAuthorization.turnAway();
		}
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}
}
