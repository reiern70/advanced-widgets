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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.dynamicdatatable.DynamicDataTableHeaderTest.Person;
import com.antiliacode.widgets.dynamicdatatable.DynamicDataTableHeaderTest.TablePage;
import com.antiliacode.widgets.table.IMovableColumn;
import com.antiliacode.widgets.table.MovableColumnsBehavior;

/**
 * The rows per page and the sort the user chose, kept in the column state store with the
 * columns, and the store read once (#127).
 */
class DynamicDataTableStateTest extends WicketTestCase
{
	private static final List<Person> PEOPLE = List.of(new Person("2", "Linus"),
		new Person("1", "Ada"), new Person("3", "Grace"));

	private static final String NAME_SORT = "table:headersRow:headers:1:sort";

	private static final String ROWS_PER_PAGE = "table:topNavigation:span:pageSize:itemsPerPage";

	private TestTable table;

	private SortablePeopleProvider provider;

	@Test
	void theRowsPerPageChosenAreSavedInTheStore()
	{
		start(SessionColumnStateStore.INSTANCE);

		chooseRowsPerPage(0);

		assertEquals(1, table.getItemsPerPage());
		ColumnState state = SessionColumnStateStore.INSTANCE.load(table.getColumnStateKey());
		assertEquals(1L, state.getItemsPerPage());
		assertNull(state.getSortProperty(), "not sorted by the user");
		assertEquals(state, table.getColumnState());
		assertEquals(1, table.changes, "the hook of the changes is called");
	}

	@Test
	void aSortIsSavedInTheStore()
	{
		start(SessionColumnStateStore.INSTANCE);

		tester.clickLink(NAME_SORT);

		ColumnState state = SessionColumnStateStore.INSTANCE.load(table.getColumnStateKey());
		assertEquals("name", state.getSortProperty());
		assertTrue(state.isSortAscending());
		assertNull(state.getItemsPerPage(), "the rows per page of the page, not chosen");
		assertEquals(1, table.changes);

		tester.clickLink(NAME_SORT);

		state = SessionColumnStateStore.INSTANCE.load(table.getColumnStateKey());
		assertEquals("name", state.getSortProperty());
		assertFalse(state.isSortAscending());
		assertEquals(2, table.changes);
	}

	@Test
	void aNewTableShowsTheRowsPerPageAndTheSortTheUserLeft()
	{
		start(SessionColumnStateStore.INSTANCE);
		tester.clickLink(NAME_SORT);
		tester.clickLink(NAME_SORT);
		chooseRowsPerPage(1);
		moveNameToTheEnd();

		start(SessionColumnStateStore.INSTANCE);

		assertEquals(2, table.getItemsPerPage());
		assertEquals(new SortParam<>("name", false), provider.getSort());
		assertEquals(List.of("0", "2", "1"), table.getColumnState().getOrder(),
			"the columns as well");
		assertTrue(tester.getLastResponseAsString().contains("aria-sort=\"descending\""));
	}

	@Test
	void theStateIsAppliedWhenTheTableIsFirstRenderedNotWhenItIsAdded()
	{
		create(new MemoryStore(new ColumnState(List.of(), Set.of(), Map.of(), 2L, "name", true)));
		table.setItemsPerPageOptions();
		TablePage page = new TablePage(table);
		table.setItemsPerPageOptions(1L, 2L, 10L);

		tester.startPage(page);

		assertEquals(2, table.getItemsPerPage(),
			"the options set after the table was added count");
		assertEquals(new SortParam<>("name", true), provider.getSort());
	}

	@Test
	void rowsPerPageNoLongerOfferedAreIgnored()
	{
		start(new MemoryStore(new ColumnState(List.of(), Set.of(), Map.of(), 7L, null, false)));

		assertEquals(10, table.getItemsPerPage(), "the page's own");
		assertNull(table.getColumnState().getItemsPerPage(), "the value no longer offered is gone");
	}

	@Test
	void aSortByAColumnNoLongerSortableIsIgnored()
	{
		start(new MemoryStore(new ColumnState(List.of(), Set.of(), Map.of(), null, "id", true)));

		assertNull(provider.getSort());
		assertNull(table.getColumnState().getSortProperty());
	}

	@Test
	void aSortIsIgnoredWithoutAProviderThatSorts()
	{
		MemoryStore store = new MemoryStore(
			new ColumnState(List.of(), Set.of(), Map.of(), 2L, "name", true));
		table = new TestTable(new PlainPeopleProvider());
		table.setColumnStateStore(store);
		tester.startPage(new TablePage(table));

		assertNull(table.getColumnState().getSortProperty());
		assertEquals(2, table.getItemsPerPage(), "the rows per page apply all the same");
	}

	@Test
	void anOldStateWithoutRowsPerPageOrSortIsRead()
	{
		start(new MemoryStore(new ColumnState(List.of("0", "2", "1"), Set.of(), Map.of())));

		assertEquals(List.of("0", "2", "1"), table.getColumnState().getOrder());
		assertEquals(10, table.getItemsPerPage());
		assertNull(provider.getSort());
	}

	@Test
	void aStateSerializedBeforeItHadRowsPerPageOrSortIsRead() throws Exception
	{
		ColumnState old = new ColumnState(List.of("1", "0"), Set.of("2"), Map.of("0", 40d));

		ColumnState read = serializedAndRead(old);

		assertEquals(old, read);
		assertNull(read.getItemsPerPage());
		assertNull(read.getSortProperty());
		assertFalse(read.isSortAscending());
	}

	@Test
	void theStoreIsReadOnceAcrossAjaxRefreshes()
	{
		MemoryStore store = new MemoryStore(
			new ColumnState(List.of(), Set.of(), Map.of(), 2L, "name", true));
		start(store);

		tester.clickLink(NAME_SORT);
		chooseRowsPerPage(0);
		moveNameToTheEnd();
		tester.clickLink("table:topNavigation:span:paging:navigator:next");
		tester.startPage(tester.getLastRenderedPage());

		assertEquals(1, store.loads, "read when first rendered, and never again");
		assertEquals(3, store.saves, "the sort, the rows per page and the move");

		store.state = new ColumnState(List.of(), Set.of(), Map.of(), 10L, "name", true);
		table.loadColumnState();

		assertEquals(2, store.loads, "read again on demand");
		assertEquals(10, table.getItemsPerPage());
		assertEquals(new SortParam<>("name", true), provider.getSort());
	}

	@Test
	void theSessionStoreKeepsTheRowsPerPageAndTheSort()
	{
		ColumnState state = new ColumnState(List.of("1", "0"), Set.of(), Map.of(), 50L, "name",
			false);

		SessionColumnStateStore.INSTANCE.save("key", state);

		assertEquals(state, SessionColumnStateStore.INSTANCE.load("key"));
	}

	@Test
	void statesDifferByTheirRowsPerPageAndSort()
	{
		ColumnState columns = new ColumnState(List.of("0"), Set.of(), Map.of());

		assertEquals(columns, new ColumnState(List.of("0"), Set.of(), Map.of(), null, null, true),
			"no order without a sort");
		assertNotEquals(columns, new ColumnState(List.of("0"), Set.of(), Map.of(), 25L, null, false));
		assertNotEquals(new ColumnState(List.of("0"), Set.of(), Map.of(), null, "name", true),
			new ColumnState(List.of("0"), Set.of(), Map.of(), null, "name", false));
		assertEquals(new ColumnState(List.of("0"), Set.of(), Map.of(), 25L, "name", true).hashCode(),
			new ColumnState(List.of("0"), Set.of(), Map.of(), 25L, "name", true).hashCode());
		assertTrue(new ColumnState(List.of("0"), Set.of(), Map.of(), 25L, "name", true).toString()
			.contains("itemsPerPage=25, sortProperty=name, sortAscending=true"));
	}

	private void start(IColumnStateStore store)
	{
		create(store);
		tester.startPage(new TablePage(table));
	}

	private void create(IColumnStateStore store)
	{
		provider = new SortablePeopleProvider();
		table = new TestTable(provider);
		table.setColumnStateStore(store);
	}

	private void chooseRowsPerPage(int option)
	{
		DropDownChoice<?> choice = (DropDownChoice<?>)tester.getComponentFromLastRenderedPage(
			ROWS_PER_PAGE);
		tester.getRequest().getPostParameters().setParameterValue(choice.getInputName(),
			String.valueOf(option));
		tester.executeAjaxEvent(choice, "change");
	}

	private void moveNameToTheEnd()
	{
		tester.getRequest().getPostParameters().setParameterValue("from", "1");
		tester.getRequest().getPostParameters().setParameterValue("to", "3");
		tester.executeBehavior(table.getBehaviors(MovableColumnsBehavior.class).get(0));
	}

	private static ColumnState serializedAndRead(ColumnState state)
		throws IOException, ClassNotFoundException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes))
		{
			out.writeObject(state);
		}
		try (ObjectInputStream in = new ObjectInputStream(
			new ByteArrayInputStream(bytes.toByteArray())))
		{
			return (ColumnState)in.readObject();
		}
	}

	/**
	 * A store of an application, keeping one state, which counts how often the table asks it.
	 */
	private static class MemoryStore implements IColumnStateStore
	{
		private static final long serialVersionUID = 1L;

		ColumnState state;

		int loads;

		int saves;

		MemoryStore(ColumnState state)
		{
			this.state = state;
		}

		@Override
		public ColumnState load(String key)
		{
			loads++;
			return state;
		}

		@Override
		public void save(String key, ColumnState state)
		{
			saves++;
			this.state = state;
		}
	}

	private static class MovableColumn extends DynamicColumn<Person> implements IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		MovableColumn(String header, String template)
		{
			super(Model.of(header), template);
		}
	}

	private static class TestTable extends JsonBasedDynamicDataTable<Person, String>
	{
		private static final long serialVersionUID = 1L;

		int changes;

		TestTable(IIdentifiableDynamicDataProvider<Person, String> provider)
		{
			super("table", List.of(new RowNumberColumn<>(Model.of("#")),
				new SortableDynamicColumn<>(new MovableColumn("Name", "{{name}}"), "name"),
				new MovableColumn("Id", "{{id}}")), provider, 10);
			setNavigationPosition(ToolbarPosition.TOP);
			setItemsPerPageOptions(1L, 2L, 10L);
			add(new MovableColumnsBehavior());
		}

		@Override
		protected void onColumnsChanged(AjaxRequestTarget target)
		{
			changes++;
		}
	}

	private static class PlainPeopleProvider
		implements
			IIdentifiableDynamicDataProvider<Person, String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Iterator<Person> iterator(long first, long count)
		{
			return PEOPLE.subList((int)first, (int)Math.min(PEOPLE.size(), first + count))
				.iterator();
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

		SortablePeopleProvider()
		{
			super(String.class);
		}

		@Override
		public Iterator<Person> iterator(long first, long count)
		{
			List<Person> sorted = new ArrayList<>(PEOPLE);
			SortParam<String> sort = getSort();
			if (sort != null)
			{
				Comparator<Person> byName = Comparator.comparing(Person::getName);
				sorted.sort(sort.isAscending() ? byName : byName.reversed());
			}
			return sorted.subList((int)first, (int)Math.min(sorted.size(), first + count))
				.iterator();
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
	}
}
