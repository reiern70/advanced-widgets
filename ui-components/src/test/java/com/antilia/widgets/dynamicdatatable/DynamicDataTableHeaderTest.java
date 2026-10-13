/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.antilia.widgets.table.ResizableColumnsBehavior;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

class DynamicDataTableHeaderTest extends WicketTestCase
{
	private static final List<Person> PEOPLE = List.of(new Person("2", "Linus"),
		new Person("1", "Ada"), new Person("3", "Grace"));

	private static final String NAME_SORT = "table:headersRow:headers:1:sort";

	@Test
	void sortableHeaderIsALinkWithBothArrows()
	{
		startTable(new SortablePeopleProvider(), 10);

		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("aria-sort=\"none\""), page);
		assertHeaderClass(1, "wicket_orderNone");
		assertTrue(page.contains("class=\"ant-data-table-sort-asc\">&#9650;</span>") &&
			page.contains("class=\"ant-data-table-sort-desc\">&#9660;</span>"), page);
		assertTrue(tester.getComponentFromLastRenderedPage(NAME_SORT).isEnabledInHierarchy());
		assertFalse(tester.getComponentFromLastRenderedPage("table:headersRow:headers:2:sort")
			.isEnabledInHierarchy(), "a column without sort property is not sortable");
		tester.assertInvisible("table:headersRow:headers:2:arrows");
	}

	@Test
	void onlyASortableHeaderLinkHasAMarkupId()
	{
		startTable(new SortablePeopleProvider(), 10);

		assertTrue(tester.getComponentFromLastRenderedPage(NAME_SORT).getOutputMarkupId());
		assertFalse(tester.getComponentFromLastRenderedPage("table:headersRow:headers:2:sort")
			.getOutputMarkupId(), "a header rendering its body only cannot carry an id");
	}

	@Test
	void clickingTheHeaderSortsAscendingThenDescending()
	{
		SortablePeopleProvider provider = new SortablePeopleProvider();
		JsonBasedDynamicDataTable<Person, String> table = startTable(provider, 10);

		tester.clickLink(NAME_SORT);

		assertEquals(new SortParam<>("name", true), provider.getSort());
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("aria-sort=\"ascending\""), response);
		assertHeaderClass(1, "wicket_orderUp");
		assertTrue(response.contains("ant-data-table-sort-desc"), response);
		assertTrue(response.contains(".update({"), response);
		assertEquals(List.of("Ada", "Grace", "Linus"), names(served(table)));

		tester.clickLink(NAME_SORT);

		assertEquals(new SortParam<>("name", false), provider.getSort());
		assertTrue(tester.getLastResponseAsString().contains("aria-sort=\"descending\""));
		assertHeaderClass(1, "wicket_orderDown");
		assertEquals(List.of("Linus", "Grace", "Ada"), names(served(table)));
	}

	@Test
	void sortingShowsTheFirstPage()
	{
		JsonBasedDynamicDataTable<Person, String> table =
			startTable(new SortablePeopleProvider(), 1);
		table.setCurrentPage(2);

		tester.clickLink(NAME_SORT);

		assertEquals(0, table.getCurrentPage());
	}

	@Test
	void headersAreNotSortableWithoutASortableProvider()
	{
		startTable(new PeopleProvider(), 10);

		assertFalse(tester.getComponentFromLastRenderedPage(NAME_SORT).isEnabledInHierarchy());
		assertFalse(tester.getLastResponseAsString().contains("aria-sort"));
	}

	@Test
	void rowsAreNumberedAcrossPages()
	{
		JsonBasedDynamicDataTable<Person, String> table =
			startTable(new SortablePeopleProvider(), 2);
		table.setCurrentPage(1);

		JSONArray rows = served(table);

		assertEquals(1, rows.length());
		assertEquals(3, rows.getJSONObject(0).getInt("number"));
	}

	@Test
	void rowNumberColumnShowsTheRowVariable()
	{
		assertEquals("{{@number}}", new RowNumberColumn<Person>(Model.of("#")).getTemplate());
	}

	@Test
	void navigationIsOnTopByDefault()
	{
		startTable(new PeopleProvider(), 1);

		tester.assertVisible("table:topNavigation");
		tester.assertInvisible("table:bottomNavigation");
	}

	@Test
	void navigationCanBeAtTheBottomOrOnBothEnds()
	{
		JsonBasedDynamicDataTable<Person, String> table = table(new PeopleProvider(), 1);
		table.setNavigationPosition(ToolbarPosition.BOTTOM);
		tester.startPage(new TablePage(table));
		tester.assertVisible("table:bottomNavigation");
		tester.assertInvisible("table:topNavigation");

		table.setNavigationPosition(ToolbarPosition.BOTH);
		tester.startPage(tester.getLastRenderedPage());
		tester.assertVisible("table:topNavigation");
		tester.assertVisible("table:bottomNavigation");
	}

	@Test
	void navigationIsHiddenOnASinglePageWherever()
	{
		JsonBasedDynamicDataTable<Person, String> table = table(new PeopleProvider(), 10);
		table.setNavigationPosition(ToolbarPosition.BOTH);
		tester.startPage(new TablePage(table));

		tester.assertInvisible("table:topNavigation");
		tester.assertInvisible("table:bottomNavigation");
	}

	@Test
	void pagingRerendersBothNavigations()
	{
		JsonBasedDynamicDataTable<Person, String> table = table(new PeopleProvider(), 1);
		table.setNavigationPosition(ToolbarPosition.BOTH);
		tester.startPage(new TablePage(table));

		tester.clickLink("table:topNavigation:span:paging:navigator:next");

		assertEquals(1, table.getCurrentPage());
		tester.assertComponentOnAjaxResponse("table:topNavigation");
		tester.assertComponentOnAjaxResponse("table:bottomNavigation");
	}

	@Test
	void sortableColumnWrapperDelegatesToTheColumn()
	{
		IDynamicColumn<Person> column = new DynamicColumn<Person>(Model.of("Name"), "{{name}}")
			.setCssClass("name");
		SortableDynamicColumn<Person, String> sortable = new SortableDynamicColumn<>(column,
			"name");

		assertEquals("name", sortable.getSortProperty());
		assertTrue(sortable.isSortable());
		assertEquals("Name", sortable.getHeader().getObject());
		assertEquals("{{name}}", sortable.getTemplate());
		assertEquals("name", sortable.getCssClass());
	}

	@Test
	void wrappedCompoundColumnRegistersItsActions()
	{
		RecordingAction select = new RecordingAction();
		JsonBasedDynamicDataTable<Person, String> table = new JsonBasedDynamicDataTable<>("table",
			List.of(new SortableDynamicColumn<>(
				new CompoundDynamicColumn<>(Model.of("Actions"), List.of(select)), "actions")),
			new SortablePeopleProvider(), 10);

		assertThrows(IllegalArgumentException.class,
			() -> table.addActionContributor(new RecordingAction()));
	}

	@Test
	void wrappedColumnContributesItsHeaderItems()
	{
		JsonBasedDynamicDataTable<Person, String> table = new JsonBasedDynamicDataTable<>("table",
			List.of(new SortableDynamicColumn<>(
				new ProgressBarColumn<Person>(Model.of("Progress"), "progress"), "progress")),
			new SortablePeopleProvider(), 10);
		tester.startPage(new TablePage(table));

		tester.assertContains("antilia-progress-bar.*\\.css");
	}

	@Test
	void columnsAreNotResizableWithoutTheBehavior()
	{
		tester.startPage(new TablePage(table(new PeopleProvider(), 10)));

		String page = tester.getLastResponseAsString();
		assertFalse(page.contains("\"resize\""), page);
		assertFalse(page.contains("antilia-resizable-columns"), page);
	}

	@Test
	void resizingIsConfiguredForTheBrowser()
	{
		JsonBasedDynamicDataTable<Person, String> table =
			resizable(table(new PeopleProvider(), 10));
		tester.startPage(new TablePage(table));

		tester.assertContains(Pattern.quote("\"resize\":{}"));
		tester.assertContains(Pattern.quote("Wicket.ResizableColumns.attach("));
		tester.assertContains("antilia-resizable-columns.*\\.js");
		tester.assertContains("antilia-resizable-columns.*\\.css");
		tester.assertContains(Pattern.quote("data-resizable=\"false\""));
	}

	@Test
	void resizedWidthsAreStoredAndRenderedAgain()
	{
		JsonBasedDynamicDataTable<Person, String> table =
			resizable(table(new PeopleProvider(), 10));
		tester.startPage(new TablePage(table));

		resize(table, "40,120.5,80");

		assertArrayEquals(new double[] { 40, 120.5, 80 }, table.getColumnWidths());
		tester.startPage(tester.getLastRenderedPage());
		tester.assertContains(Pattern.quote("\"widths\":[40,120.5,80]"));
		tester.assertContains(Pattern.quote("options.widths = [40,120.5,80];"));
	}

	@Test
	void invalidWidthsAreIgnored()
	{
		JsonBasedDynamicDataTable<Person, String> table =
			resizable(table(new PeopleProvider(), 10));
		tester.startPage(new TablePage(table));

		for (String widths : List.of("40,120", "a,b,c", "-1,2,3", "Infinity,1,1", "1,2,1e9"))
		{
			resize(table, widths);
			assertNull(table.getColumnWidths(), widths);
		}
	}

	private static JsonBasedDynamicDataTable<Person, String> resizable(
		JsonBasedDynamicDataTable<Person, String> table)
	{
		table.add(new ResizableColumnsBehavior(ResizableColumnsBehavior.Mode.STRETCH));
		return table;
	}

	private void resize(JsonBasedDynamicDataTable<Person, String> table, String widths)
	{
		tester.getRequest().getPostParameters().setParameterValue("widths", widths);
		tester.executeBehavior(table.getBehaviors(ResizableColumnsBehavior.class).get(0));
	}

	@Test
	void proportionsAndParentWidthAreSentToTheBrowser()
	{
		JsonBasedDynamicDataTable<Person, String> table = table(new PeopleProvider(), 10);
		table.setColumnProportions(1, 2.5, 1)
			.setAdjustToParentWidth(true);
		tester.startPage(new TablePage(table));

		tester.assertContains(
			Pattern.quote("\"layout\":{\"proportions\":[1,2.5,1],\"adjustToParent\":true}"));
		assertArrayEquals(new double[] { 1, 2.5, 1 }, table.getColumnProportions());
	}

	@Test
	void withoutProportionsOrParentWidthNoLayoutIsSent()
	{
		tester.startPage(new TablePage(table(new PeopleProvider(), 10)));

		assertFalse(tester.getLastResponseAsString().contains("\"layout\""));
	}

	@Test
	void proportionsHaveToBePositiveAndOnePerColumn()
	{
		JsonBasedDynamicDataTable<Person, String> table = table(new PeopleProvider(), 10);

		assertThrows(IllegalArgumentException.class, () -> table.setColumnProportions(1, 2));
		assertThrows(IllegalArgumentException.class, () -> table.setColumnProportions(1, 0, 1));
		assertThrows(IllegalArgumentException.class,
			() -> table.setColumnProportions(1, Double.NaN, 1));
		assertNull(table.setColumnProportions((double[])null).getColumnProportions());
	}

	@Test
	void actionsWithTheReservedPrefixAreRejected()
	{
		JsonBasedDynamicDataTable<Person, String> table = table(new PeopleProvider(), 10);

		assertThrows(IllegalArgumentException.class,
			() -> table.addActionContributor(action(JsonBasedDynamicDataTable.SELECT_ACTION)));
		assertThrows(IllegalArgumentException.class,
			() -> table.addActionContributor(action("_antiliaCustom")));
		assertThrows(IllegalArgumentException.class, () -> table.setColumnWidths(1, 2));
	}


	private static RecordingAction action(String actionId)
	{
		return new RecordingAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getActionId()
			{
				return actionId;
			}
		};
	}

	@Test
	void rowsPerPageCanBeChosen()
	{
		JsonBasedDynamicDataTable<Person, String> table = table(new PeopleProvider(), 10);
		table.setItemsPerPageOptions(1L, 2L, 10L);
		tester.startPage(new TablePage(table));
		tester.assertVisible("table:topNavigation:span:pageSize");
		tester.assertInvisible("table:topNavigation:span:paging:navigator");

		DropDownChoice<?> choice = (DropDownChoice<?>)tester.getComponentFromLastRenderedPage(
			"table:topNavigation:span:pageSize:itemsPerPage");
		tester.getRequest().getPostParameters().setParameterValue(choice.getInputName(), "0");
		tester.executeAjaxEvent(choice, "change");

		assertEquals(1, table.getItemsPerPage());
		tester.assertComponentOnAjaxResponse("table:topNavigation");
		assertThrows(IllegalArgumentException.class, () -> table.setItemsPerPageOptions(0L));
	}

	@Test
	void navigationIsHiddenWithoutChoiceOnASinglePage()
	{
		startTable(new PeopleProvider(), 10);

		tester.assertInvisible("table:topNavigation");
	}

	private void assertHeaderClass(int index, String cssClass)
	{
		String th = tester.getTagsByWicketId("headers").get(index).getAttribute("class");
		assertTrue(th.contains(cssClass), th);
	}

	private void executeAction(JsonBasedDynamicDataTable<Person, String> table, String action,
		String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		tester.getRequest().getPostParameters().setParameterValue("key", key);
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private JsonBasedDynamicDataTable<Person, String> startTable(
		IDynamicDataProvider<Person, String> provider, long rowsPerPage)
	{
		JsonBasedDynamicDataTable<Person, String> table = table(provider, rowsPerPage);
		tester.startPage(new TablePage(table));
		return table;
	}

	private static JsonBasedDynamicDataTable<Person, String> table(
		IDynamicDataProvider<Person, String> provider, long rowsPerPage)
	{
		List<IDynamicColumn<Person>> columns = new ArrayList<>();
		columns.add(new RowNumberColumn<>(Model.of("#")));
		columns.add(new SortableDynamicColumn<>(new DynamicColumn<>(Model.of("Name"), "{{name}}"),
			"name"));
		columns.add(new DynamicColumn<>(Model.of("Id"), "{{id}}"));
		return new JsonBasedDynamicDataTable<>("table", columns, provider, rowsPerPage);
	}

	private JSONArray served(JsonBasedDynamicDataTable<Person, String> table)
	{
		tester.executeBehavior(table.getBehaviors(AbstractAjaxBehavior.class)
			.stream()
			.filter(behavior -> behavior instanceof AbstractDefaultAjaxBehavior == false)
			.findFirst()
			.orElseThrow());
		return new JSONObject(tester.getLastResponseAsString()).getJSONArray("rows");
	}

	private static List<String> names(JSONArray rows)
	{
		List<String> names = new ArrayList<>();
		for (int i = 0; i < rows.length(); i++)
		{
			JSONObject row = rows.getJSONObject(i);
			assertEquals(i + 1, row.getInt("number"));
			names.add(row.getJSONObject("data").getString("name"));
		}
		return names;
	}

	public static class Person implements Serializable, IIdentifiable<String>
	{
		private static final long serialVersionUID = 1L;

		private final String id;

		private final String name;

		Person(String id, String name)
		{
			this.id = id;
			this.name = name;
		}

		@Override
		public String getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}
	}

	private static class PeopleProvider
		implements
			IIdentifiableDynamicDataProvider<Person, String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Iterator<Person> iterator(long first, long count)
		{
			return rows(first, count).iterator();
		}

		private List<Person> rows(long first, long count)
		{
			return PEOPLE.subList((int)first, (int)(first + count));
		}

		@Override
		public long size()
		{
			return PEOPLE.size();
		}

		@Override
		public Person findByKey(String key)
		{
			return PEOPLE.stream().filter(p -> p.getId().equals(key)).findFirst().orElse(null);
		}

		@Override
		public Class<String> getKeyType()
		{
			return String.class;
		}
	}

	private static class SortablePeopleProvider
		extends
			SortableDynamicDataProvider<Person, String, String>
		implements
			IIdentifiableDynamicDataProvider<Person, String>
	{
		private static final long serialVersionUID = 1L;

		private final PeopleProvider people = new PeopleProvider();

		SortablePeopleProvider()
		{
			super(String.class);
		}

		@Override
		public Iterator<Person> iterator(long first, long count)
		{
			return rows(first, count).iterator();
		}

		private List<Person> rows(long first, long count)
		{
			List<Person> sorted = new ArrayList<>(PEOPLE);
			SortParam<String> sort = getSort();
			if (sort != null)
			{
				Comparator<Person> byName = Comparator.comparing(Person::getName);
				sorted.sort(sort.isAscending() ? byName : byName.reversed());
			}
			return sorted.subList((int)first, (int)(first + count));
		}

		@Override
		public long size()
		{
			return people.size();
		}

		@Override
		public Person findByKey(String key)
		{
			return people.findByKey(key);
		}
	}

	private static class RecordingAction implements IAjaxActionColumnContributor<Person>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String getActionId()
		{
			return "select";
		}

		@Override
		public String getTemplate()
		{
			return "<a href=\"#\" data-dt-action=\"select\">select</a>";
		}

		@Override
		public void onAction(Person row, AjaxRequestTarget target)
		{
		}
	}

	public static class TablePage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TablePage(JsonBasedDynamicDataTable<Person, String> table)
		{
			add(table);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream(
				"<html><head></head><body><table wicket:id=\"table\"></table></body></html>");
		}
	}
}
