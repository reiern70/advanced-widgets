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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListDynamicDataProviderTest extends WicketTestCase
{
	private final List<Line> lines = new ArrayList<>();

	private ListDynamicDataProvider<Line, Long, String> provider;

	@BeforeEach
	void lines()
	{
		lines.add(new Line(1, "Paper", new BigDecimal("10.50")));
		lines.add(new Line(2, "ink", null));
		lines.add(new Line(3, "Stamps", new BigDecimal("2")));
		lines.add(new Line(4, "Émile's envelopes", new BigDecimal("30")));
		lines.add(new Line(5, null, new BigDecimal("2")));
		provider = new ListDynamicDataProvider<Line, Long, String>(Long.class, Model.ofList(lines),
			Line::getId)
			.sortByText("description", Line::getDescription)
			.sortBy("amount", Line::getAmount)
			.sortWith("id", (one, other) -> Long.compare(one.getId(), other.getId()));
	}

	@Test
	void withoutASortTheRowsAreInTheOrderOfTheList()
	{
		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids(provider.iterator(0, 10)));
		assertEquals(5, provider.size());
	}

	@Test
	void sortsByAValueInItsNaturalOrderNullsLastBothWays()
	{
		provider.setSort("amount", SortOrder.ASCENDING);
		assertEquals(List.of(3L, 5L, 1L, 4L, 2L), ids(provider.iterator(0, 10)),
			"equal amounts keep the order of the list, the null amount is last");

		provider.setSort("amount", SortOrder.DESCENDING);
		assertEquals(List.of(4L, 1L, 3L, 5L, 2L), ids(provider.iterator(0, 10)),
			"the null amount is last descending too");
	}

	@Test
	void sortsTextsInTheAlphabeticalOrderOfTheSessionsLanguage()
	{
		tester.getSession().setLocale(Locale.FRENCH);

		provider.setSort("description", SortOrder.ASCENDING);
		assertEquals(List.of(4L, 2L, 1L, 3L, 5L), ids(provider.iterator(0, 10)),
			"Émile among the Es, ink regardless of its case, the null description last");

		provider.setSort("description", SortOrder.DESCENDING);
		assertEquals(List.of(3L, 1L, 2L, 4L, 5L), ids(provider.iterator(0, 10)));
	}

	@Test
	void sortsWithAComparatorOfTheRows()
	{
		lines.add(0, lines.remove(4));

		provider.setSort("id", SortOrder.ASCENDING);
		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids(provider.iterator(0, 10)));
		provider.setSort("id", SortOrder.DESCENDING);
		assertEquals(List.of(5L, 4L, 3L, 2L, 1L), ids(provider.iterator(0, 10)));
	}

	@Test
	void aPropertyWithoutAnOrderIsNotSortable()
	{
		assertTrue(provider.isSortable("amount"));
		assertFalse(provider.isSortable("color"));
		assertEquals(List.of("description", "amount", "id"),
			List.copyOf(provider.getSortProperties()));

		provider.setSort("color", SortOrder.DESCENDING);
		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids(provider.iterator(0, 10)),
			"the order of the list");
		assertThrows(IllegalArgumentException.class,
			() -> provider.defaultSort("color", SortOrder.ASCENDING));
	}

	@Test
	void theDefaultSortSortsUntilTheTableSortsOtherwise()
	{
		provider.defaultSort("amount", SortOrder.DESCENDING);

		assertEquals("amount", provider.getSort().getProperty());
		assertFalse(provider.getSort().isAscending());
		assertEquals(4L, provider.iterator(0, 1).next().getId());
	}

	@Test
	void pagingReturnsTheSliceOfTheSortedRows()
	{
		provider.setSort("amount", SortOrder.ASCENDING);

		assertEquals(List.of(5L, 1L), ids(provider.iterator(1, 2)));
		assertEquals(List.of(2L), ids(provider.iterator(4, 2)), "the last page is short");
		assertEquals(List.of(), ids(provider.iterator(7, 2)), "past the end");
		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids(lines.iterator()),
			"the list of the model is left alone");
	}

	@Test
	void aKeyFindsItsRowAfterTheListChangedOrder()
	{
		Line stamps = lines.get(2);
		provider.setSort("amount", SortOrder.ASCENDING);
		provider.iterator(0, 10);

		lines.clear();
		lines.addAll(List.of(new Line(4, "Envelopes", null), stamps, new Line(1, "Paper", null)));
		provider.detach();

		assertSame(stamps, provider.findByKey(3L));
		assertEquals("Envelopes", provider.findByKey(4L).getDescription());
		assertNull(provider.findByKey(2L), "gone");
		assertNull(provider.findByKey(null));
	}

	@Test
	void aRowsModelResolvesToTheCurrentVersionOfTheRow()
	{
		IModel<Line> model = provider.model(lines.get(0));
		model.detach();
		provider.detach();
		lines.set(0, new Line(1, "Paper, recycled", BigDecimal.ONE));

		assertEquals("Paper, recycled", model.getObject().getDescription());
	}

	@Test
	void theRowsAreReadOncePerRequest()
	{
		AtomicInteger reads = new AtomicInteger();
		IModel<List<Line>> model = new LoadableDetachableModel<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected List<Line> load()
			{
				reads.incrementAndGet();
				return lines;
			}
		};
		ListDynamicDataProvider<Line, Long, String> provider = new ListDynamicDataProvider<>(
			Long.class, model, Line::getId);

		provider.size();
		provider.iterator(0, 2);
		provider.findByKey(3L);
		assertEquals(1, reads.get());

		provider.detach();
		provider.size();
		assertEquals(2, reads.get(), "read again after detaching");
	}

	@Test
	void aFilterReducesTheSizeAndTheRows()
	{
		IModel<String> search = Model.of("p");
		provider.filterBy(search, (line, text) -> line.getDescription() != null &&
			line.getDescription().toLowerCase(Locale.ROOT).contains(text));
		provider.setSort("amount", SortOrder.DESCENDING);

		assertEquals(3, provider.size());
		assertEquals(List.of(4L, 1L, 3L), ids(provider.iterator(0, 10)));
		assertEquals(2L, provider.findByKey(2L).getId(), "a row filtered out is still found");

		search.setObject(null);
		provider.detach();
		assertEquals(5, provider.size(), "a null value keeps every row");

		provider.filterBy(null, null);
		search.setObject("p");
		assertEquals(5, provider.size(), "no filter");
	}

	@Test
	void rowsThatAreIdentifiableGiveTheirKeys()
	{
		List<Item> items = List.of(new Item("b"), new Item("a"));
		ListDynamicDataProvider<Item, String, String> byId = new ListDynamicDataProvider<Item,
			String, String>(String.class, Model.ofList(items)).sortBy("id", Item::getId);

		assertEquals("b", byId.keyOf(items.get(0)));
		assertSame(items.get(1), byId.findByKey("a"));

		ListDynamicDataProvider<Line, Long, String> notIdentifiable = new ListDynamicDataProvider<>(
			Long.class, Model.ofList(lines));
		assertThrows(ClassCastException.class, () -> notIdentifiable.keyOf(lines.get(0)));
	}

	@Test
	void aModelOfNoListGivesNoRows()
	{
		ListDynamicDataProvider<Line, Long, String> empty = new ListDynamicDataProvider<>(
			Long.class, Model.ofList(null), Line::getId);

		assertEquals(0, empty.size());
		assertFalse(empty.iterator(0, 10).hasNext());
		assertNull(empty.findByKey(1L));
	}

	@Test
	void theProviderIsSerializableWithItsOrdersAndFilter()
	{
		provider.filterBy(Model.of("p"), (line, text) -> line.getDescription() != null &&
			line.getDescription().contains(text));
		provider.setSort("description", SortOrder.ASCENDING);

		ListDynamicDataProvider<Line, Long, String> copy = WicketObjects.cloneObject(provider);

		assertEquals(List.of(4L, 1L, 3L), ids(copy.iterator(0, 10)));
	}

	@Test
	void aBeanTableSortsThroughTheProvider()
	{
		List<IColumn<Line, String>> columns = new ArrayList<>();
		columns.add(new PropertyColumn<>(Model.of("Description"), "description", "description"));
		columns.add(new PropertyColumn<>(Model.of("Amount"), "amount", "amount"));
		BeanBasedDynamicDataTable<Line, Long, String> table = new BeanBasedDynamicDataTable<>(
			"table", columns, provider, 2);
		tester.startPage(new TablePage(table));
		assertTrue(tester.getLastResponseAsString().contains("data-key=\"1\""));

		tester.clickLink("table:headersRow:headers:1:sort");

		assertEquals("amount", provider.getSort().getProperty());
		String rows = tester.getLastResponseAsString();
		assertTrue(rows.contains("data-key=\"3\"") && rows.contains("data-key=\"5\""), rows);
		assertFalse(rows.contains("data-key=\"1\""), rows);
	}

	private static List<Long> ids(Iterator<? extends Line> rows)
	{
		List<Long> ids = new ArrayList<>();
		rows.forEachRemaining(row -> ids.add(row.getId()));
		return ids;
	}

	public static class Line implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private final long id;

		private final String description;

		private final BigDecimal amount;

		Line(long id, String description, BigDecimal amount)
		{
			this.id = id;
			this.description = description;
			this.amount = amount;
		}

		public Long getId()
		{
			return id;
		}

		public String getDescription()
		{
			return description;
		}

		public BigDecimal getAmount()
		{
			return amount;
		}
	}

	public static class Item implements Serializable, IIdentifiable<String>
	{
		private static final long serialVersionUID = 1L;

		private final String id;

		Item(String id)
		{
			this.id = id;
		}

		@Override
		public String getId()
		{
			return id;
		}
	}

	public static class TablePage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TablePage(BaseDynamicDataTable<Line, Long, ?> table)
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
