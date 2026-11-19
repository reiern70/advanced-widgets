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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.WicketRuntimeException;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Jackson 2 writes the rows as Jackson 3 does.
 */
class Jackson2JsonSerializerTest
{
	@Test
	void writesTheRowsAsJackson3Does()
	{
		Map<String, Object> rows = new LinkedHashMap<>();
		rows.put("rows", List.of(new Person("1", "Ada", 36), new Person("2", "Linus", 55)));
		rows.put("itemCount", 2);
		rows.put("nothing", null);
		rows.put("when", new Date(0));
		rows.put("record", new Point(3, 4));

		JSONObject jackson2 = new JSONObject(Jackson2JsonSerializer.INSTANCE.toJson(rows));
		JSONObject jackson3 = new JSONObject(JacksonJsonSerializer.INSTANCE.toJson(rows));

		// the same names and values; Jackson 3 sorts a bean's properties, Jackson 2 does not
		assertEquals(jackson3.keySet(), jackson2.keySet());
		for (JSONObject json : List.of(jackson2, jackson3))
		{
			JSONArray people = json.getJSONArray("rows");
			assertEquals(2, people.length());
			assertEquals("1", people.getJSONObject(0).getString("id"));
			assertEquals("Ada", people.getJSONObject(0).getString("name"));
			assertEquals(36, people.getJSONObject(0).getInt("age"));
			assertEquals("Linus", people.getJSONObject(1).getString("name"));
			assertEquals(2, json.getInt("itemCount"));
			assertTrue(json.isNull("nothing"));
			// a date as text, not as a number: "+00:00" by Jackson 2, "Z" by Jackson 3
			assertEquals(Instant.EPOCH,
				OffsetDateTime.parse(json.getString("when")).toInstant());
			assertEquals(3, json.getJSONObject("record").getInt("x"));
			assertEquals(4, json.getJSONObject("record").getInt("y"));
		}
	}

	@Test
	void aRowJacksonCannotWriteFailsTelling()
	{
		WicketRuntimeException e = assertThrows(WicketRuntimeException.class,
			() -> Jackson2JsonSerializer.INSTANCE.toJson(List.of(new Broken())));

		assertEquals("Jackson 2 cannot write java.util.ImmutableCollections$List12 as JSON",
			e.getMessage());
	}

	/** A row with a getter for each property. */
	public static class Person implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private final String id;

		private final String name;

		private final int age;

		Person(String id, String name, int age)
		{
			this.id = id;
			this.name = name;
			this.age = age;
		}

		public String getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}

		public int getAge()
		{
			return age;
		}
	}

	/** A record row. */
	public record Point(int x, int y) implements Serializable
	{
	}

	/** A row whose getter fails. */
	public static class Broken implements Serializable
	{
		private static final long serialVersionUID = 1L;

		public String getValue()
		{
			throw new IllegalStateException("broken");
		}
	}
}
