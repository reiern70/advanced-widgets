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
package com.antiliacode.widgets.demo.contacts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior.Location;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.chips.Chips;
import com.antiliacode.widgets.collapsible.CollapsiblePanel;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.antiliacode.widgets.dynamicdatatable.AjaxDownloadActionColumnContributor;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.antiliacode.widgets.dynamicdatatable.ColumnChooserToolbarAction;
import com.antiliacode.widgets.dynamicdatatable.CompoundDynamicColumn;
import com.antiliacode.widgets.dynamicdatatable.CsvExportToolbarAction;
import com.antiliacode.widgets.dynamicdatatable.DynamicColumn;
import com.antiliacode.widgets.dynamicdatatable.DynamicNavigationToolbar;
import com.antiliacode.widgets.dynamicdatatable.IDynamicColumn;
import com.antiliacode.widgets.dynamicdatatable.IDynamicColumnContributor;
import com.antiliacode.widgets.dynamicdatatable.IHideableColumn;
import com.antiliacode.widgets.dynamicdatatable.IconBasedRowAction;
import com.antiliacode.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.antiliacode.widgets.dynamicdatatable.RowNumberColumn;
import com.antiliacode.widgets.dynamicdatatable.SelectionColumn;
import com.antiliacode.widgets.dynamicdatatable.SelectionCountToolbarAction;
import com.antiliacode.widgets.dynamicdatatable.SortableDynamicColumn;
import com.antiliacode.widgets.filter.FilterBehavior;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.modal.IModalButtonFactory;
import com.antiliacode.widgets.modal.Modal;
import com.antiliacode.widgets.split.SplitPanel;
import com.antiliacode.widgets.split.SplitSize;
import com.antiliacode.widgets.table.IMovableColumn;
import com.antiliacode.widgets.table.MovableColumnsBehavior;
import com.antiliacode.widgets.table.ResizableColumnsBehavior;

/**
 * A realistic example: an address book searched by facets. A {@link SplitPanel} holds the filters
 * on the left, a {@link CollapsiblePanel collapsible} {@link FacetPanel} for each {@link Facet},
 * and the contacts they leave on the right, in a {@link JsonBasedDynamicDataTable} with
 * everything it offers: selection, sorting, paging, a search in its navigation, columns to
 * resize, move, hide and export, and row actions showing a contact's card and saving it as a
 * vCard.
 */
public class ContactsPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The number of rows a page shows at first. */
	static final int ROWS_PER_PAGE = 25;

	/** The height of the body of the table, in pixels. */
	static final int BODY_HEIGHT = 560;

	private final ContactFilter filter = new ContactFilter();

	private final ContactsProvider contacts = new ContactsProvider(filter);

	private final List<FacetPanel> facets = new ArrayList<>();

	private final Modal dialog;

	private WebMarkupContainer filtersHead;

	private WebMarkupContainer summary;

	private JsonBasedDynamicDataTable<Contact, Long> table;

	/**
	 * Constructor.
	 */
	public ContactsPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		SplitPanel split = new SplitPanel("split")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newFirstPane(String id)
			{
				return newFilters(id);
			}

			@Override
			protected Component newSecondPane(String id)
			{
				return newResults(id);
			}
		};
		split.setPosition(SplitSize.pixels(300))
			.setMin(SplitSize.pixels(220))
			.setMax(50)
			.setMinSecond(SplitSize.pixels(420))
			.setCollapsible(true);
		themed.add(split);

		dialog = new Modal("dialog");
		dialog.setSize(Modal.Size.MEDIUM);
		add(dialog);
	}

	/**
	 * @return the left pane: a heading with a button clearing all filters, and the facets
	 */
	private Component newFilters(String id)
	{
		Fragment pane = new Fragment(id, "filtersPane", this);

		filtersHead = new WebMarkupContainer("head");
		filtersHead.setOutputMarkupId(true);
		pane.add(filtersHead);
		filtersHead.add(new AjaxActionButton("clearAll", new ResourceModel("clearAll"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				filter.clear();
				onFilterChanged(target, null, false);
			}

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(filter.isEmpty() == false);
			}
		}.setIcon(SvgIcon.FILTER_CIRCLE_XMARK).setVariant(Variant.GHOST).setSize(Size.SMALL));

		Form<Void> form = new Form<>("form");
		pane.add(form);
		RepeatingView facetViews = new RepeatingView("facet");
		form.add(facetViews);
		for (Facet facet : Facet.values())
		{
			FacetPanel panel = new FacetPanel(facetViews.newChildId(), facet, filter)
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onFilterChanged(AjaxRequestTarget target, boolean fromSearch)
				{
					ContactsPage.this.onFilterChanged(target, this, fromSearch);
				}
			};
			facets.add(panel);
			facetViews.add(panel);
		}
		return pane;
	}

	/**
	 * @return the right pane: how many contacts pass, the values chosen as chips, and the table
	 */
	private Component newResults(String id)
	{
		Fragment pane = new Fragment(id, "resultsPane", this);

		summary = new WebMarkupContainer("summary");
		summary.setOutputMarkupId(true);
		pane.add(summary);
		summary.add(new Label("count", () -> new StringResourceModel("count", this)
			.setParameters(contacts.size(), ContactsDatabase.SIZE).getString()));
		summary.add(new ListView<>("chip", this::chosenValues)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Chosen> item)
			{
				Chosen chosen = item.getModelObject();
				String label = chosen.facet().label(chosen.value(), getLocale());
				item.add(new Label("facet", new ResourceModel("facet." + chosen.facet().key())));
				item.add(new Label("text", label));
				item.add(new AjaxLink<Void>("remove")
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						filter.choose(chosen.facet(), chosen.value(), false);
						onFilterChanged(target, null, false);
					}
				}.add(AttributeModifier.replace("aria-label",
					new StringResourceModel("chip.remove", this).setParameters(label))));
			}
		}.setReuseItems(false));

		table = newTable("table");
		pane.add(table);
		return pane;
	}

	/**
	 * @return the table of the contacts
	 */
	private JsonBasedDynamicDataTable<Contact, Long> newTable(String id)
	{
		List<IDynamicColumn<Contact>> columns = new ArrayList<>();
		columns.add(new SelectionColumn<Contact>().setId("selection"));
		columns.add(new RowNumberColumn<Contact>(Model.of("#")).setId("number"));
		List<IDynamicColumnContributor<Contact>> actions = List.of(new CardAction(),
			new VCardDownload());
		columns.add(new CompoundDynamicColumn<>(new ResourceModel("actions"), actions)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isResizable()
			{
				return false;
			}
		}.setId("actions"));
		columns.add(sortable(new ContactColumn("name",
			"<span class=\"contacts-nowrap\"><span class=\"contacts-avatar\" " +
				"aria-hidden=\"true\">{{initials}}</span>{{firstName}} {{lastName}}</span>",
			Contact::getName), ContactsProvider.NAME));
		columns.add(sortable(new ContactColumn("email",
			"<a href=\"mailto:{{email}}\">{{email}}</a>", Contact::getEmail),
			ContactsProvider.EMAIL));
		columns.add(new ContactColumn("phone",
			"<a class=\"contacts-nowrap\" href=\"tel:{{phone}}\">{{phone}}</a>",
			Contact::getPhone));
		columns.add(sortable(new ContactColumn("profession", "{{professionName}}",
			Contact::getProfessionName), ContactsProvider.PROFESSION));
		columns.add(sortable(new ContactColumn("country",
			"{{countryName}} <small class=\"contacts-muted\">{{city}}</small>",
			Contact::getCountryName), ContactsProvider.COUNTRY));
		columns.add(sortable(new ContactColumn("born",
			"<span class=\"contacts-nowrap\">{{born}} <small class=\"contacts-muted\">" +
				"({{age}})</small></span>",
			contact -> contact.getBirthDate().toString()), ContactsProvider.BIRTH_DATE));
		ContactColumn city = new ContactColumn("city", "{{city}}", Contact::getCity);
		columns.add(city);

		JsonBasedDynamicDataTable<Contact, Long> contactsTable = new JsonBasedDynamicDataTable<>(
			id, columns, contacts, ROWS_PER_PAGE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected AbstractDynamicToolbar newNavigationToolbar(String id,
				ToolbarPosition position)
			{
				return new DynamicNavigationToolbar<>(id, this, position)
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected Component newPrefix(String id)
					{
						return position == ToolbarPosition.TOP ? newSearch(id)
							: super.newPrefix(id);
					}
				};
			}

			@Override
			protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
				Contact contact)
			{
				warn(getString("gone"));
				target.add(ContactsPage.this.get("feedback"));
			}
		};
		contactsTable.setColumnVisible(city, false);
		// selection, number, actions, name, email, phone, profession, country, born and city
		contactsTable.setColumnProportions(0.35, 0.45, 0.75, 1.7, 2.2, 1.3, 1.3, 1.5, 1.5, 1.0);
		contactsTable.setNavigationPosition(ToolbarPosition.TOP);
		contactsTable.setItemsPerPageOptions(25L, 50L, 100L);
		contactsTable.setAdjustToParentWidth(true);
		contactsTable.setBodyHeight(BODY_HEIGHT, BodyHeightUnits.PIXELS);
		contactsTable.add(new ResizableColumnsBehavior(ResizableColumnsBehavior.Mode.STRETCH));
		contactsTable.add(new MovableColumnsBehavior());
		contactsTable.addToolbarAction(new ColumnChooserToolbarAction<>());
		contactsTable.addToolbarAction(new CsvExportToolbarAction<Contact, Long>()
			.setFileName("contacts.csv"));
		contactsTable.addToolbarAction(new SelectionCountToolbarAction<>());
		return contactsTable;
	}

	private static IDynamicColumn<Contact> sortable(ContactColumn column, String property)
	{
		return new SortableDynamicColumn<>(column, property);
	}

	/**
	 * @return the search in front of the top navigation: the contacts whose name or email
	 *         contains what the user types
	 */
	private Component newSearch(String id)
	{
		Fragment search = new Fragment(id, "search", this);
		TextField<String> text = new TextField<>("text", new PropertyModel<>(filter, "text"));
		text.add(FilterBehavior.onFilter(target -> onFilterChanged(target, null, false)));
		search.add(text);
		return search;
	}

	/**
	 * Shows the contacts the filters leave, from the first page, and counts the values of the
	 * facets again.
	 *
	 * @param source
	 *            the facet the user changed, {@code null} if it was the search or a chip
	 * @param fromSearch
	 *            whether the user changed it in its autocomplete, which is not rendered again
	 */
	private void onFilterChanged(AjaxRequestTarget target, FacetPanel source, boolean fromSearch)
	{
		table.setCurrentPage(0);
		table.refresh(target);
		for (FacetPanel facet : facets)
		{
			facet.update(target, facet == source && fromSearch);
		}
		target.add(summary, filtersHead);
	}

	/**
	 * @return the values chosen, of all facets, facet by facet
	 */
	private List<Chosen> chosenValues()
	{
		List<Chosen> chosen = new ArrayList<>();
		for (Facet facet : Facet.values())
		{
			filter.get(facet).forEach(value -> chosen.add(new Chosen(facet, value)));
		}
		return chosen;
	}

	/**
	 * @return the filters of the contacts
	 */
	ContactFilter getFilter()
	{
		return filter;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(Chips.CSS));
		response.render(CssHeaderItem.forReference(new CssResourceReference(ContactsPage.class,
			"ContactsPage.css")));
	}

	/**
	 * A value chosen of a facet, shown as a chip above the table.
	 */
	private record Chosen(Facet facet, String value) implements Serializable
	{
	}

	/**
	 * A column the user can hide and move, its export value read from the contact.
	 */
	private static class ContactColumn extends DynamicColumn<Contact>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		ContactColumn(String key, String template, SerializableFunction<Contact, ?> exportValue)
		{
			super(new ResourceModel(key), template);
			setId(key);
			setExportValue(exportValue);
		}
	}

	/**
	 * Shows the card of the contact in the dialog.
	 */
	private class CardAction extends IconBasedRowAction<Contact>
	{
		private static final long serialVersionUID = 1L;

		CardAction()
		{
			super("card", SvgIcon.ADDRESS_CARD, new ResourceModel("card"));
		}

		@Override
		protected void onRowAction(Contact contact, AjaxRequestTarget target)
		{
			dialog.open(Model.of(contact.getName()),
				new ContactCardPanel(Modal.BODY_ID, Model.of(contact)), target,
				IModalButtonFactory.close(new ResourceModel("close"), SvgIcon.XMARK));
		}
	}

	/**
	 * Saves the contact as a vCard, the file address books exchange contacts in.
	 */
	private static class VCardDownload extends AjaxDownloadActionColumnContributor<Contact>
	{
		private static final long serialVersionUID = 1L;

		VCardDownload()
		{
			super("vcard", new ResourceModel("vcard"));
			setIcon(SvgIcon.DOWNLOAD);
			setLocation(Location.SameWindow);
		}

		@Override
		protected String getFileName(Contact contact)
		{
			return "contact-" + contact.getId() + ".vcf";
		}

		@Override
		protected String getContentType()
		{
			return "text/vcard";
		}

		@Override
		protected String getContent(Contact contact)
		{
			return VCard.of(contact);
		}
	}
}
