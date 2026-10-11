/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.danekja.java.util.function.serializable.SerializableSupplier;

import com.antilia.widgets.demo.Countries;
import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.dropdown.Dropdown;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.dynamicdatatable.BeanBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.BeanRowNumberColumn;
import com.antilia.widgets.dynamicdatatable.DynamicColumn;
import com.antilia.widgets.dynamicdatatable.IDynamicColumn;
import com.antilia.widgets.dynamicdatatable.IIdentifiable;
import com.antilia.widgets.dynamicdatatable.IIdentifiableDynamicDataProvider;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.SelectionColumn;
import com.antilia.widgets.dynamicdatatable.SelectionMode;
import com.antilia.widgets.table.ResizableColumnsBehavior;
import com.antilia.widgets.table.ResizableColumnsBehavior.Mode;

/**
 * Two tables of a page, master and detail: the countries the users live in, in a
 * {@link JsonBasedDynamicDataTable}, and the users of the country chosen there, in a
 * {@link BeanBasedDynamicDataTable}. Drop-downs choose how the columns of both are resized, how
 * wide the tables are and whether they are stacked or side by side; resizing the columns of one
 * table leaves the other alone.
 */
public class MasterDetailPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The number of rows a page of either table shows. */
	private static final int ROWS_PER_PAGE = 10;

	/** The height of the bodies of the tables, in pixels. */
	private static final int BODY_HEIGHT = 320;

	/** How the columns are resized: not, or in a mode of {@link ResizableColumnsBehavior}. */
	private static final List<String> RESIZING = List.of("none", "fit", "stretch");

	private static final List<String> WIDTHS = List.of("parent", "content");

	private static final List<String> LAYOUTS = List.of("stacked", "sideBySide");

	private final UsersProvider users = new UsersProvider();

	private final JsonBasedDynamicDataTable<CountryRow, String> countries;

	private final BeanBasedDynamicDataTable<User, Long, String> detail;

	private final WebMarkupContainer tables;

	private String resizing = "stretch";

	private String width = "parent";

	private String layout = "stacked";

	/**
	 * Constructor.
	 */
	public MasterDetailPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		themed.add(choice("resize", RESIZING, () -> resizing, value -> resizing = value));
		themed.add(choice("width", WIDTHS, () -> width, value -> width = value));
		themed.add(choice("layout", LAYOUTS, () -> layout, value -> layout = value));

		tables = new WebMarkupContainer("tables");
		tables.setOutputMarkupId(true);
		tables.add(new AttributeAppender("class",
			() -> "master-detail--" + layout, " "));
		themed.add(tables);

		countries = newCountries("countries");
		tables.add(countries);
		detail = newUsers("users");
		tables.add(new Label("usersTitle", () -> users.getCountry() == null
			? getString("users.all")
			: new StringResourceModel("users.of", this)
				.setParameters(Countries.name(users.getCountry(), getLocale())).getString())
			.setOutputMarkupId(true));
		tables.add(detail);
		configure(countries);
		configure(detail);
	}

	/**
	 * @return the master: the countries, of which the user selects one
	 */
	private JsonBasedDynamicDataTable<CountryRow, String> newCountries(String id)
	{
		List<IDynamicColumn<CountryRow>> columns = new ArrayList<>();
		columns.add(new SelectionColumn<CountryRow>().setId("selection"));
		columns.add(new DynamicColumn<CountryRow>(new ResourceModel("country"), "{{name}}")
			.setId("name"));
		columns.add(new DynamicColumn<CountryRow>(new ResourceModel("code"), "{{id}}")
			.setId("code"));
		columns.add(new DynamicColumn<CountryRow>(new ResourceModel("count"), "{{users}}")
			.setId("users"));
		JsonBasedDynamicDataTable<CountryRow, String> table = new JsonBasedDynamicDataTable<>(id,
			columns, new CountriesProvider(), ROWS_PER_PAGE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSelectionChanged(AjaxRequestTarget target)
			{
				List<String> keys = getSelection().getKeys();
				users.setCountry(keys.isEmpty() ? null : keys.get(0));
				detail.setCurrentPage(0);
				detail.refresh(target);
				target.add(tables.get("usersTitle"));
			}
		};
		table.setSelectionMode(SelectionMode.ONE);
		return table;
	}

	/**
	 * @return the detail: the users of the country selected in the master, or all of them
	 */
	private BeanBasedDynamicDataTable<User, Long, String> newUsers(String id)
	{
		List<IColumn<User, String>> columns = new ArrayList<>();
		columns.add(new BeanRowNumberColumn<>(Model.of("#")));
		columns.add(new LambdaColumn<>(new ResourceModel("firstName"), "firstName",
			User::getFirstName));
		columns.add(new LambdaColumn<>(new ResourceModel("lastName"), "lastName",
			User::getLastName));
		columns.add(new LambdaColumn<>(new ResourceModel("bornDate"),
			user -> Objects.toString(UsersTablePanel.bornOn(user), "")));
		columns.add(new LambdaColumn<>(new ResourceModel("city"), User::getCity));
		columns.add(new LambdaColumn<>(new ResourceModel("phones"),
			user -> user.getHomePhone() + " / " + user.getCellPhone()));
		BeanBasedDynamicDataTable<User, Long, String> table = new BeanBasedDynamicDataTable<>(id,
			columns, users, ROWS_PER_PAGE);
		table.setSelectionMode(SelectionMode.NONE);
		return table;
	}

	private void configure(BaseDynamicDataTable<?, ?, ?> table)
	{
		table.setNavigationPosition(BaseDynamicDataTable.ToolbarPosition.TOP);
		table.setBodyHeight(BODY_HEIGHT, BodyHeightUnits.PIXELS);
		applySettings(table);
	}

	/**
	 * Gives the table the resizing and width chosen, forgetting the widths the user gave its
	 * columns.
	 */
	private void applySettings(BaseDynamicDataTable<?, ?, ?> table)
	{
		table.getBehaviors(ResizableColumnsBehavior.class).forEach(table::remove);
		if ("none".equals(resizing) == false)
		{
			table.add(new ResizableColumnsBehavior(Mode.valueOf(resizing.toUpperCase(Locale.ROOT))));
		}
		table.setAdjustToParentWidth("parent".equals(width)).setColumnWidths((double[])null);
	}

	/**
	 * @return a drop-down of a setting of both tables, which renders them again when changed
	 */
	private Dropdown<String> choice(String id, List<String> values,
		SerializableSupplier<String> getter, SerializableConsumer<String> setter)
	{
		Dropdown<String> choice = new Dropdown<>(id, LambdaModel.of(getter, setter), values,
			new ChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(String value)
				{
					return getString(id + "." + value);
				}

				@Override
				public String getIdValue(String value, int index)
				{
					return value;
				}
			});
		choice.setRequired(true);
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				applySettings(countries);
				applySettings(detail);
				target.add(tables);
			}
		});
		return choice;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(
			MasterDetailPage.class, "MasterDetailPage.css")));
	}

	/**
	 * A country the users live in, with how many of them do; its key is its ISO 3166 code.
	 */
	public static class CountryRow implements IIdentifiable<String>, Serializable
	{
		private static final long serialVersionUID = 1L;

		private final String id;

		private final String name;

		private final int users;

		CountryRow(String id, String name, int users)
		{
			this.id = id;
			this.name = name;
			this.users = users;
		}

		/**
		 * @return the ISO 3166 code of the country
		 */
		@Override
		public String getId()
		{
			return id;
		}

		/**
		 * @return the name of the country in the language of the session
		 */
		public String getName()
		{
			return name;
		}

		/**
		 * @return how many users live in the country
		 */
		public int getUsers()
		{
			return users;
		}
	}

	/**
	 * The countries the users of the large database live in, sorted by their names in the
	 * language of the session.
	 */
	private static class CountriesProvider
		implements
			IIdentifiableDynamicDataProvider<CountryRow, String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Iterator<CountryRow> iterator(long first, long count)
		{
			List<CountryRow> rows = rows();
			return rows.subList((int)first, (int)Math.min(rows.size(), first + count))
				.iterator();
		}

		@Override
		public long size()
		{
			return rows().size();
		}

		@Override
		public CountryRow findByKey(String key)
		{
			return rows().stream().filter(row -> row.getId().equals(key)).findFirst()
				.orElse(null);
		}

		@Override
		public Class<String> getKeyType()
		{
			return String.class;
		}

		private static List<CountryRow> rows()
		{
			List<User> index = DatabaseLocator.getLargeDatabase().getIndex(null);
			Map<String, Integer> counts = new LinkedHashMap<>();
			synchronized (index)
			{
				index.stream().map(User::getCountry).filter(Objects::nonNull)
					.forEach(code -> counts.merge(code, 1, Integer::sum));
			}
			Locale locale = Session.get().getLocale();
			Collator collator = Collator.getInstance(locale);
			return counts.entrySet().stream()
				.map(entry -> new CountryRow(entry.getKey(),
					Countries.name(entry.getKey(), locale), entry.getValue()))
				.sorted(Comparator.comparing(CountryRow::getName, collator))
				.toList();
		}
	}
}
