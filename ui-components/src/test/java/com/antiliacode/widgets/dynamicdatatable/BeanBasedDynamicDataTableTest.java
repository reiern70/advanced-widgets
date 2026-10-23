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
package com.antiliacode.widgets.dynamicdatatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import com.antiliacode.widgets.table.IMovableColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class BeanBasedDynamicDataTableTest extends WicketTestCase
{
	private PeopleProvider provider;

	private TestTable table;

	private void start(int people)
	{
		provider = new PeopleProvider(people);
		table = new TestTable(provider);
		tester.startPage(new TablePage(table));
	}

	@Test
	void rendersTheRowsOfThePageOnTheServer()
	{
		start(5);

		String page = tester.getLastResponseAsString();
		TagTester first = TagTester.createTagByAttribute(page, "data-key", "1");
		assertEquals("tr", first.getName());
		assertEquals(table.getMarkupId() + "-row-1", first.getAttribute("id"));
		assertEquals("1", first.getAttribute("data-number"));
		assertEquals("even", first.getAttribute("class"));
		assertEquals("odd", TagTester.createTagByAttribute(page, "data-key", "2")
			.getAttribute("class"));
		assertNull(TagTester.createTagByAttribute(page, "data-key", "3"), "one page of 2 rows");
		assertTrue(page.contains("Person &lt;1&gt;"), "cells are escaped: " + page);
		assertTrue(page.contains("\"serverRows\":true"), page);
		assertFalse(page.contains("dataUrl"), page);
	}

	@Test
	void theRowsAreInTheBodyOfTheMarkup()
	{
		start(5);

		TagTester body = tester.getTagByWicketId("body");
		assertEquals("tbody", body.getName());
		assertEquals(table.getRowsBody().getMarkupId(), body.getAttribute("id"));
	}

	@Test
	void rowNumbersAndCheckboxesFollowThePageAndTheSelection()
	{
		start(5);
		executeAction(BaseDynamicDataTable.SELECT_ACTION, "4");

		table.setCurrentPage(1);
		tester.startPage(tester.getLastRenderedPage());

		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("<input type=\"checkbox\" class=\"ant-data-table-select\" " +
			"data-dt-action=\"_antilia-select\" data-dt-action-unchecked=\"_antilia-deselect\" " +
			"aria-label=\"Select row\" checked>"), page);
		TagTester fourth = TagTester.createTagByAttribute(page, "data-key", "4");
		assertEquals("odd selected", fourth.getAttribute("class"));
		assertTrue(page.contains("<wicket:container wicket:id=\"cell\">3</wicket:container>"),
			page);
		assertEquals("3", TagTester.createTagByAttribute(page, "data-key", "3")
			.getAttribute("data-number"));
	}

	@Test
	void pagingRendersTheRowsAgainNotTheTable()
	{
		start(5);

		tester.clickLink("table:topNavigation:span:paging:navigator:next");

		tester.assertComponentOnAjaxResponse(table.getRowsBody());
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains(".rowsReplaced();"), response);
		assertTrue(response.contains("data-key=\"3\""), response);
		assertFalse(response.contains("<component id=\"" + table.getMarkupId() + "\""),
			response);
	}

	@Test
	void sortingByAPropertyColumnSortsTheProvider()
	{
		start(5);

		tester.clickLink("table:headersRow:headers:2:sort");

		assertEquals(new SortParam<>("name", true), provider.getSort());
		tester.clickLink("table:headersRow:headers:2:sort");
		assertEquals(SortOrder.DESCENDING,
			provider.getSortState().getPropertySortOrder("name"));
		assertTrue(tester.getLastResponseAsString().contains("data-key=\"5\""));
	}

	@Test
	void actionsOfTheActionsColumnGetTheRow()
	{
		start(5);

		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("data-dt-action=\"greet\""), page);
		executeAction("greet", "2");

		assertEquals(List.of(2L), table.greet.greeted);
	}

	@Test
	void fixedWidthColumnsAreNotResizable()
	{
		start(5);

		String page = tester.getLastResponseAsString();
		assertEquals("false", TagTester.createTagsByAttribute(page, "wicket:id", "headers", false)
			.get(0).getAttribute("data-resizable"));
		assertNull(TagTester.createTagsByAttribute(page, "wicket:id", "headers", false).get(2)
			.getAttribute("data-resizable"));
	}

	@Test
	void columnsCanBeMovedAndHiddenByIndex()
	{
		start(5);
		IColumn<Person, String> name = table.getColumns().get(2);

		table.moveColumn(name, 4);
		table.setColumnVisible(table.getColumns().get(2), false);
		tester.startPage(tester.getLastRenderedPage());

		assertEquals(List.of("0", "1", "3", "4", "2"), table.getColumnState().getOrder());
		assertEquals(Set.of("3"), table.getColumnState().getHidden());
		assertTrue(table.isMovable(name));
	}

	@Test
	void exportsTheTitleAndValueOfExportableColumns()
	{
		start(5);
		IColumn<Person, String> name = table.getColumns().get(2);

		assertTrue(table.isColumnExportable(name));
		assertEquals("Name", table.getColumnTitle(name).getObject());
		assertEquals("Person <3>", table.getColumnExportValue(name, provider.people.get(2)));
		assertFalse(table.isColumnExportable(table.getColumns().get(0)));
	}

	@Test
	void selectionModeNoneRendersNoSelectionColumn()
	{
		provider = new PeopleProvider(5);
		table = new TestTable(provider);
		table.setSelectionMode(SelectionMode.NONE);
		tester.startPage(new TablePage(table));

		String page = tester.getLastResponseAsString();
		assertFalse(page.contains("ant-data-table-select"), page);
		assertFalse(page.contains("ant-data-table-selection"), page);

		executeAction(BaseDynamicDataTable.SELECT_ACTION, "1");
		assertTrue(table.getSelection().isEmpty());
	}

	@Test
	void selectionModeOneRendersOneCheckedRowAndNoPageCheckbox()
	{
		provider = new PeopleProvider(5);
		table = new TestTable(provider);
		table.setSelectionMode(SelectionMode.ONE);
		tester.startPage(new TablePage(table));
		assertFalse(tester.getLastResponseAsString().contains("ant-data-table-select-page"));

		executeAction(BaseDynamicDataTable.SELECT_ACTION, "1");
		executeAction(BaseDynamicDataTable.SELECT_ACTION, "2");
		tester.startPage(tester.getLastRenderedPage());

		String page = tester.getLastResponseAsString();
		assertEquals("even", TagTester.createTagByAttribute(page, "data-key", "1")
			.getAttribute("class"));
		assertEquals("odd selected", TagTester.createTagByAttribute(page, "data-key", "2")
			.getAttribute("class"));
		assertEquals(1, page.split(" checked>", -1).length - 1, page);
	}

	private void executeAction(String action, String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		tester.getRequest().getPostParameters().setParameterValue("key", key);
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	public static class Person implements Serializable, IIdentifiable<Long>
	{
		private static final long serialVersionUID = 1L;

		private final long id;

		Person(long id)
		{
			this.id = id;
		}

		@Override
		public Long getId()
		{
			return id;
		}

		public String getName()
		{
			return "Person <" + id + ">";
		}
	}

	private static class PeopleProvider extends SortableDynamicDataProvider<Person, Long, String>
		implements
			IIdentifiableDynamicDataProvider<Person, Long>
	{
		private static final long serialVersionUID = 1L;

		final List<Person> people = new ArrayList<>();

		PeopleProvider(int size)
		{
			super(Long.class);
			for (long id = 1; id <= size; id++)
			{
				people.add(new Person(id));
			}
		}

		@Override
		public Iterator<Person> iterator(long first, long count)
		{
			List<Person> sorted = new ArrayList<>(people);
			if (getSort() != null && getSort().isAscending() == false)
			{
				sorted.sort(Comparator.comparing(Person::getId).reversed());
			}
			return sorted.subList((int)first, (int)(first + count)).iterator();
		}

		@Override
		public long size()
		{
			return people.size();
		}

		@Override
		public Person findByKey(Long key)
		{
			return people.stream().filter(person -> person.getId().equals(key)).findFirst()
				.orElse(null);
		}
	}

	private static class MovableColumn extends PropertyColumn<Person, String>
		implements
			IMovableColumn,
			IHideableColumn
	{
		private static final long serialVersionUID = 1L;

		MovableColumn(String title, String property, String sort)
		{
			super(Model.of(title), sort, property);
		}
	}

	private static class GreetAction implements IAjaxActionColumnContributor<Person>
	{
		private static final long serialVersionUID = 1L;

		final List<Long> greeted = new ArrayList<>();

		@Override
		public String getTemplate()
		{
			return "<button data-dt-action=\"greet\">Hi</button>";
		}

		@Override
		public String getActionId()
		{
			return "greet";
		}

		@Override
		public void onAction(Person row, AjaxRequestTarget target)
		{
			greeted.add(row.getId());
		}
	}

	private static class TestTable extends BeanBasedDynamicDataTable<Person, Long, String>
	{
		private static final long serialVersionUID = 1L;

		final GreetAction greet;

		TestTable(PeopleProvider provider)
		{
			this(provider, new GreetAction());
		}

		private TestTable(PeopleProvider provider, GreetAction greet)
		{
			super("table", List.of(new BeanSelectionColumn<>(),
				new BeanRowNumberColumn<>(Model.of("#")),
				new MovableColumn("Name", "name", "name"),
				new MovableColumn("Id", "id", null),
				new BeanActionsColumn<>(Model.of("Actions"), List.of(greet))), provider, 2);
			this.greet = greet;
		}
	}

	public static class TablePage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TablePage(TestTable table)
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
