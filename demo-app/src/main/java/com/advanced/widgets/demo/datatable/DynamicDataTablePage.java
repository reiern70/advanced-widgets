/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.message.AbortedMessage;
import org.apache.wicket.protocol.ws.api.message.ClosedMessage;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.advanced.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.CompoundDynamicColumn;
import com.advanced.widgets.dynamicdatatable.DynamicColumn;
import com.advanced.widgets.dynamicdatatable.IDynamicColumn;
import com.advanced.widgets.dynamicdatatable.IHideableColumn;
import com.advanced.widgets.dynamicdatatable.IWebSocketLightWeightMessage;
import com.advanced.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.ProgressBarColumn;
import com.advanced.widgets.dynamicdatatable.ResourceDynamicColumn;
import com.advanced.widgets.dynamicdatatable.RowNumberColumn;
import com.advanced.widgets.dynamicdatatable.SelectionColumn;
import com.advanced.widgets.dynamicdatatable.SortableDynamicColumn;
import com.advanced.widgets.table.IMovableColumn;

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
		add(new WebSocketBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConnect(ConnectedMessage message)
			{
				ProgressSimulator.start(message.getApplication(), message.getSessionId(),
					getPageId(), message.getKey(), getTable().getMarkupId(),
					DatabaseLocator.getLargeDatabase());
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
	protected BaseDynamicDataTable<Contact, Long, ?> newTable(String id,
		ContactsProvider provider)
	{
		List<IDynamicColumn<Contact>> columns = new ArrayList<>();
		columns.add(new SelectionColumn<Contact>().setId("selection"));
		columns.add(new RowNumberColumn<Contact>(Model.of("#")).setId("number"));
		columns.add(new CompoundDynamicColumn<>(new ResourceModel("actions"), newRowActions())
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isResizable()
			{
				return false;
			}
		}.setId("actions"));
		columns.add(new ResourceDynamicColumn<Contact>(new ResourceModel("id"),
			DynamicDataTablePage.class, "IdColumn")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isResizable()
			{
				return false;
			}
		}.setExportValue(Contact::getId).setId("id"));
		columns.add(new SortableDynamicColumn<>(
			new TemplateColumn("firstName", "FirstNameColumn", Contact::getFirstName), "firstName"));
		columns.add(new SortableDynamicColumn<>(
			new TemplateColumn("lastName", "LastNameColumn", Contact::getLastName), "lastName"));
		columns.add(new SortableDynamicColumn<>(
			new TemplateColumn("bornDate", "BornDateColumn", DynamicTableExamplePage::bornOn),
			"bornDate"));
		columns.add(new TemplateColumn("address", "AddressColumn",
			contact -> contact.getAddress() + ", " + contact.getCity() + ", " +
				contact.getCountryName()));
		columns.add(new PhonesColumn(new ResourceModel("phones"))
			.setExportValue(contact -> contact.getHomePhone() + " / " + contact.getCellPhone())
			.setId("phones"));
		columns.add(new ProgressColumn());
		List<IDynamicColumn<Contact>> hidden = List.of(
			new ValueColumn("homePhone", "{{homePhone}}", Contact::getHomePhone),
			new ValueColumn("cellPhone", "{{cellPhone}}", Contact::getCellPhone),
			new ValueColumn("city", "{{city}}", Contact::getCity),
			new ValueColumn("country", "{{countryName}}", Contact::getCountryName));
		columns.addAll(hidden);

		JsonBasedDynamicDataTable<Contact, Long> table = new JsonBasedDynamicDataTable<>(id,
			columns, provider, ROWS_PER_PAGE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected AbstractDynamicToolbar newNavigationToolbar(String id,
				ToolbarPosition position)
			{
				return DynamicDataTablePage.this.newNavigationToolbar(this, id, position);
			}

			@Override
			protected void onRowsSent(List<Long> keys)
			{
				ProgressSimulator.show(getSession().getId(), getPage().getPageId(), keys);
			}

			@Override
			protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
				Contact contact)
			{
				onContactNotFound(target);
			}

			@Override
			protected void onSelectionChanged(AjaxRequestTarget target)
			{
				DynamicDataTablePage.this.onSelectionChanged(target);
			}
		};
		hidden.forEach(column -> table.setColumnVisible(column, false));
		table.setPushEnabled(true);
		return table;
	}

	private JsonBasedDynamicDataTable<Contact, Long> getJsonTable()
	{
		return (JsonBasedDynamicDataTable<Contact, Long>)getTable();
	}

	/**
	 * Has the browser fetch the rows again, told so over the web socket connection.
	 */
	@Override
	protected void onContactAdded(Contact contact, AjaxRequestTarget target)
	{
		push(target, getJsonTable().newRefreshMessage(), "Added " + contact.getFirstName() + " " +
			contact.getLastName() + "; the table was refreshed via web sockets.");
	}

	@Override
	protected Component newPushRowsLink(String id)
	{
		return new AjaxLink<Void>(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				List<Contact> rows = currentPageRows();
				Collections.reverse(rows);

				push(target, getJsonTable().newUpdateRowsMessage(rows),
					"Pushed the rows of this page in reverse order via web sockets.");
			}
		};
	}

	private List<Contact> currentPageRows()
	{
		BaseDynamicDataTable<Contact, Long, ?> table = getTable();
		long first = table.getCurrentPage() * table.getItemsPerPage();
		long count = Math.min(table.getItemsPerPage(), table.getItemCount() - first);
		List<Contact> rows = new ArrayList<>();
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
				warn("The page has no open web socket connection.");
			}
		}
		catch (IOException e)
		{
			error("Pushing to the table via web sockets failed: " + e.getMessage());
		}
		target.add(get("feedback"));
	}

	/**
	 * A column the user can hide and move, reading its template from {@code templateName.html}
	 * next to this page.
	 */
	private static class TemplateColumn extends ResourceDynamicColumn<Contact>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		TemplateColumn(String headerKey, String templateName,
			SerializableFunction<Contact, ?> exportValue)
		{
			super(new ResourceModel(headerKey), DynamicDataTablePage.class, templateName);
			setId(headerKey);
			setExportValue(exportValue);
		}
	}

	/**
	 * A column the user can hide and move, showing one value of the contact.
	 */
	private static class ValueColumn extends DynamicColumn<Contact>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		ValueColumn(String headerKey, String template, SerializableFunction<Contact, ?> exportValue)
		{
			super(new ResourceModel(headerKey), template);
			setId(headerKey);
			setExportValue(exportValue);
		}
	}

	/**
	 * The progress of the contact, a column the user can hide and move.
	 */
	private static class ProgressColumn extends ProgressBarColumn<Contact>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		ProgressColumn()
		{
			super(new ResourceModel("progress"), "progress");
			setId("progress");
		}
	}
}
