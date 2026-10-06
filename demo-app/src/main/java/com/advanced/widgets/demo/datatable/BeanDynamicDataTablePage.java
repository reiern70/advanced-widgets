/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.string.Strings;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.advanced.widgets.clipboard.ClipboardCopyBehavior;
import com.advanced.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.BeanActionsColumn;
import com.advanced.widgets.dynamicdatatable.BeanBasedDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.BeanRowNumberColumn;
import com.advanced.widgets.dynamicdatatable.BeanSelectionColumn;
import com.advanced.widgets.dynamicdatatable.IFixedWidthColumn;
import com.advanced.widgets.dynamicdatatable.IHideableColumn;
import com.advanced.widgets.progress.ProgressBar;
import com.advanced.widgets.table.IMovableColumn;

/**
 * The same table as {@link DynamicDataTablePage}, with its rows rendered on the server by Wicket
 * components: a {@link BeanBasedDynamicDataTable} with Wicket's {@link IColumn}s.
 */
public class BeanDynamicDataTablePage extends DynamicTableExamplePage
{
	private static final long serialVersionUID = 1L;

	@Override
	protected BaseDynamicDataTable<Contact, Long, ?> newTable(String id,
		ContactsProvider provider)
	{
		List<IColumn<Contact, String>> columns = new ArrayList<>();
		columns.add(new BeanSelectionColumn<>());
		columns.add(new BeanRowNumberColumn<>(Model.of("#")));
		columns.add(new BeanActionsColumn<>(new ResourceModel("actions"), newRowActions()));
		columns.add(new IdColumn());
		columns.add(new ContactColumn("firstName", "firstName", Contact::getFirstName));
		columns.add(new ContactColumn("lastName", "lastName", Contact::getLastName));
		columns.add(new ContactColumn("bornDate", "bornDate",
			contact -> Objects.toString(DynamicTableExamplePage.bornOn(contact), "")));
		columns.add(new ContactColumn("address", null,
			contact -> contact.getAddress() + ", " + contact.getCity() + ", " +
				contact.getCountryName()));
		columns.add(new PhonesColumn());
		columns.add(new ProgressColumn());
		List<IColumn<Contact, String>> hidden = List.of(
			new ContactColumn("homePhone", null, Contact::getHomePhone),
			new ContactColumn("cellPhone", null, Contact::getCellPhone),
			new ContactColumn("city", null, Contact::getCity),
			new ContactColumn("country", null, Contact::getCountryName));
		columns.addAll(hidden);

		BeanBasedDynamicDataTable<Contact, Long, String> table = new BeanBasedDynamicDataTable<>(
			id, columns, provider, ROWS_PER_PAGE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected AbstractDynamicToolbar newNavigationToolbar(String id,
				ToolbarPosition position)
			{
				return BeanDynamicDataTablePage.this.newNavigationToolbar(this, id, position);
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
				BeanDynamicDataTablePage.this.onSelectionChanged(target);
			}
		};
		hidden.forEach(column -> table.setColumnVisible(column, false));
		return table;
	}

	/**
	 * Renders the rows again via Ajax.
	 */
	@Override
	protected void onContactAdded(Contact contact, AjaxRequestTarget target)
	{
		getTable().refresh(target);
		info("Added " + contact.getFirstName() + " " + contact.getLastName() +
			"; the rows were rendered again.");
		target.add(get("feedback"));
	}

	/**
	 * The id of the contact, a column the user cannot resize.
	 */
	private static class IdColumn extends LambdaColumn<Contact, String> implements IFixedWidthColumn
	{
		private static final long serialVersionUID = 1L;

		IdColumn()
		{
			super(new ResourceModel("id"), Contact::getId);
		}
	}

	/**
	 * A value of the contact, written escaped, in a column the user can hide and move.
	 */
	private static class ContactColumn extends LambdaColumn<Contact, String>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		ContactColumn(String headerKey, String sortProperty,
			SerializableFunction<Contact, ?> value)
		{
			super(new ResourceModel(headerKey), sortProperty, value);
		}
	}

	/**
	 * The phone numbers of the contact, each copied to the clipboard on a double click.
	 */
	private static class PhonesColumn extends LambdaColumn<Contact, String>
		implements
			IHeaderContributor,
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		PhonesColumn()
		{
			super(new ResourceModel("phones"),
				contact -> contact.getHomePhone() + " / " + contact.getCellPhone());
		}

		@Override
		public void populateItem(Item<ICellPopulator<Contact>> item, String componentId,
			IModel<Contact> rowModel)
		{
			item.add(new Label(componentId, rowModel.map(contact -> phones(contact, item)))
				.setEscapeModelStrings(false));
		}

		private static String phones(Contact contact, Component component)
		{
			return "<span class=\"contact-phones\"><b>" + component.getString("phones.home") +
				"</b> " + phone(contact.getHomePhone()) + "<b>" +
				component.getString("phones.cell") + "</b> " + phone(contact.getCellPhone()) +
				"</span>";
		}

		private static String phone(String number)
		{
			String escaped = Strings.escapeMarkup(number != null ? number : "").toString();
			return "<span data-wicket-copy=\"" + escaped + "\" data-wicket-copy-on=\"dblclick\">" +
				escaped + "</span>";
		}

		@Override
		public void renderHead(IHeaderResponse response)
		{
			ClipboardCopyBehavior.renderHeadItems(response);
		}
	}

	/**
	 * The progress of the contact as a progress bar, a column the user can hide and move.
	 */
	private static class ProgressColumn extends LambdaColumn<Contact, String>
		implements
			IHeaderContributor,
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		ProgressColumn()
		{
			super(new ResourceModel("progress"), Contact::getProgress);
		}

		@Override
		public void populateItem(Item<ICellPopulator<Contact>> item, String componentId,
			IModel<Contact> rowModel)
		{
			item.add(new Label(componentId, rowModel.map(contact -> ProgressBar.markup(
				String.valueOf(Math.max(0, Math.min(100, contact.getProgress()))))))
				.setEscapeModelStrings(false));
		}

		@Override
		public void renderHead(IHeaderResponse response)
		{
			response.render(CssHeaderItem.forReference(ProgressBar.CSS));
		}
	}
}
