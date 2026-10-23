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
package com.antiliacode.widgets.dynamicdatatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Serves the rows of a JSON table from a mounted {@link JsonRowsResource}, and has the table
 * fetch them from there.
 */
class JsonRowsResourceTest extends WicketTestCase
{
	private static final ResourceReference FRUITS = new ResourceReference(
		JsonRowsResourceTest.class, "fruits")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new FruitsResource();
		}
	};

	@Override
	protected WicketTester newWicketTester(WebApplication app)
	{
		WicketTester tester = super.newWicketTester(app);
		app.mountResource("api/fruits", FRUITS);
		return tester;
	}

	@Test
	void servesAPageOfRowsSortedAndFiltered()
	{
		JSONObject json = fetch("api/fruits?first=1&count=2&sort=name&ascending=false&filter=e");

		// the fruits with an e, by name descending: Pear, Melon, Cherry, Apple
		assertEquals(4, json.getInt("itemCount"));
		assertEquals(0, json.getInt("currentPage"));
		JSONArray rows = json.getJSONArray("rows");
		assertEquals(2, rows.length());
		assertEquals("Melon", rows.getJSONObject(0).getJSONObject("data").getString("name"));
		assertEquals("melon", rows.getJSONObject(0).getString("key"));
		assertEquals(2, rows.getJSONObject(0).getInt("number"));
		assertEquals("Cherry", rows.getJSONObject(1).getJSONObject("data").getString("name"));
		assertFalse(rows.getJSONObject(0).has("selected"), "the resource knows no selection");
	}

	@Test
	void limitsTheRowsOfARequest()
	{
		JSONObject json = fetch("api/fruits?first=-5&count=100000");

		assertEquals(6, json.getJSONArray("rows").length());
		assertEquals(1, json.getJSONArray("rows").getJSONObject(0).getInt("number"));
	}

	@Test
	void theTableFetchesItsRowsFromTheResource()
	{
		TestTable table = new TestTable();
		table.setRowsSource(JsonBasedDynamicDataTable.RowsSource.RESOURCE)
			.setRowsResource(FRUITS);
		table.setCurrentPage(1);
		tester.startPage(new TestPage(table));

		String page = tester.getLastResponseAsString();
		Matcher dataUrl = Pattern.compile("\"dataUrl\":\"([^\"]*)\"").matcher(page);
		assertTrue(dataUrl.find(), page);
		String url = dataUrl.group(1).replace("\\/", "/");
		assertTrue(url.matches("https?://.*/api/fruits\\?.*"), "a full URL: " + url);
		assertTrue(url.contains("first=2") && url.contains("count=2"), url);
		assertTrue(url.contains("filter=e"), "the parameters the table adds: " + url);
		assertTrue(page.contains("\"moreUrl\":"), "further rows come from the page: " + page);
		assertTrue(page.contains("\"selectedKeys\":[]"), page);
	}

	@Test
	void aTableFromAResourceNeedsOne()
	{
		TestTable table = new TestTable();
		table.setRowsSource(JsonBasedDynamicDataTable.RowsSource.RESOURCE);

		assertThrows(Exception.class, () -> tester.startPage(new TestPage(table)));
	}

	private JSONObject fetch(String path)
	{
		// as the browser fetches the rows; a POST would create one
		tester.getRequest().setMethod("GET");
		tester.executeUrl(path);
		assertEquals("application/json", tester.getLastResponse().getContentType().split(";")[0]);
		return new JSONObject(tester.getLastResponseAsString());
	}

	public static class Fruit implements Serializable, IIdentifiable<String>
	{
		private static final long serialVersionUID = 1L;

		private final String name;

		Fruit(String name)
		{
			this.name = name;
		}

		@Override
		public String getId()
		{
			return name.toLowerCase();
		}

		public String getName()
		{
			return name;
		}
	}

	private static class FruitsProvider extends SortableDynamicDataProvider<Fruit, String, String>
		implements
			IIdentifiableDynamicDataProvider<Fruit, String>
	{
		private static final long serialVersionUID = 1L;

		private final List<Fruit> fruits = new ArrayList<>();

		FruitsProvider(String filter)
		{
			super(String.class);
			for (String name : List.of("Apple", "Banana", "Cherry", "Kiwi", "Melon", "Pear"))
			{
				if (filter == null || name.toLowerCase().contains(filter))
				{
					fruits.add(new Fruit(name));
				}
			}
		}

		@Override
		public Iterator<Fruit> iterator(long first, long count)
		{
			List<Fruit> sorted = new ArrayList<>(fruits);
			Comparator<Fruit> byName = Comparator.comparing(Fruit::getName);
			if (getSort() != null && getSort().isAscending() == false)
			{
				byName = byName.reversed();
			}
			sorted.sort(byName);
			return sorted.subList((int)first, (int)Math.min(sorted.size(), first + count))
				.iterator();
		}

		@Override
		public long size()
		{
			return fruits.size();
		}

		@Override
		public Fruit findByKey(String key)
		{
			return fruits.stream().filter(fruit -> fruit.getId().equals(key)).findFirst()
				.orElse(null);
		}
	}

	private static class FruitsResource extends JsonRowsResource<Fruit, String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected IDynamicDataProvider<Fruit, String> newProvider(PageParameters parameters)
		{
			return new FruitsProvider(parameters.get("filter").toOptionalString());
		}
	}

	private static class TestTable extends JsonBasedDynamicDataTable<Fruit, String>
	{
		private static final long serialVersionUID = 1L;

		TestTable()
		{
			super("table", List.of(new DynamicColumn<>(Model.of("Name"), "{{name}}")),
				new FruitsProvider(null), 2);
		}

		@Override
		protected void addRowsParameters(PageParameters parameters)
		{
			parameters.set("filter", "e");
		}
	}

	private static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(TestTable table)
		{
			add(table);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream(
				"<html><body><table wicket:id='table'></table></body></html>");
		}
	}
}
