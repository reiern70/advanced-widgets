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
package com.antiliacode.widgets.demo.datatable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.ws.api.message.AbortedMessage;
import org.apache.wicket.protocol.ws.api.message.ClosedMessage;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;

import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.demo.DemoWebSocketBehavior;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antiliacode.widgets.dynamicdatatable.IWebSocketLightWeightMessage;
import com.antiliacode.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.antiliacode.widgets.icon.SvgIcon;
import com.github.openjson.JSONObject;

/**
 * A table rendered in the browser from JSON, whose links are handled by the table itself, and
 * whose rows the server pushes over the page's web socket connection.
 */
public class DynamicDataTablePage extends DynamicTableExamplePage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public DynamicDataTablePage()
	{
		add(new DemoWebSocketBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConnect(ConnectedMessage message)
			{
				ProgressSimulator.start(message.getApplication(), message.getSessionId(),
					getPageId(), message.getKey(), getTable().getMarkupId(),
					DatabaseLocator.getLargeDatabase(), getLocale());
			}

			@Override
			protected void onClose(ClosedMessage message)
			{
				ProgressSimulator.stop(message.getSessionId(), getPageId());
			}

			@Override
			protected void onAbort(AbortedMessage message)
			{
				ProgressSimulator.stop(message.getSessionId(), getPageId());
			}
		});
	}

	@Override
	protected UsersTablePanel newUsersTable(String id)
	{
		JsonUsersTablePanel users = new JsonUsersTablePanel(id, this)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onRowsSent(List<Long> keys)
			{
				ProgressSimulator.show(getSession().getId(), getPage().getPageId(), keys);
			}
		};
		users.getTable().setPushEnabled(true);
		return users;
	}

	private JsonBasedDynamicDataTable<User, Long> getJsonTable()
	{
		return ((JsonUsersTablePanel)getUsersTable()).getTable();
	}

	/**
	 * Pushes the rows of the page in reverse order over the web socket connection; the browser
	 * repaints them in place.
	 */
	@Override
	protected Component newPushRowsButton(String id)
	{
		return new AjaxActionButton(id, new ResourceModel("pushRows"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				List<User> rows = currentPageRows();
				Collections.reverse(rows);

				push(target, getJsonTable().newUpdateRowsMessage(rows),
					getString("pushed"));
			}
		}.setIcon(SvgIcon.ARROW_DOWN_UP_ACROSS_LINE).setSize(Size.SMALL);
	}

	private List<User> currentPageRows()
	{
		BaseDynamicDataTable<User, Long, ?> table = getTable();
		long first = table.getCurrentPage() * table.getItemsPerPage();
		long count = Math.min(table.getItemsPerPage(), table.getItemCount() - first);
		List<User> rows = new ArrayList<>();
		if (count > 0)
		{
			table.getProvider().iterator(first, count).forEachRemaining(rows::add);
		}
		return rows;
	}

	private void push(AjaxRequestTarget target, IWebSocketLightWeightMessage message,
		String success)
	{
		try
		{
			if (message.send(getTable()))
			{
				info(success);
			}
			else
			{
				warn(getString("noWebSocket"));
			}
		}
		catch (IOException e)
		{
			error(message("pushFailed", e.getMessage()));
		}
	}

	/**
	 * The edit form and the confirmation of a deletion show in the
	 * {@link com.antiliacode.widgets.modal.Modal Modal} the React and Svelte tabs use too.
	 */
	@Override
	protected boolean isModal()
	{
		return true;
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "jsonTable";
	}

	/**
	 * @return the configuration of the apps' grids, made from this table, see
	 *         {@link JsonTableApps#config(Component, JsonBasedDynamicDataTable)}
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		return JsonTableApps.config(this, getJsonTable());
	}
}
