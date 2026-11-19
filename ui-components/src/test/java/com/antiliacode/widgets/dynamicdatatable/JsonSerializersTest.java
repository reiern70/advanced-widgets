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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.wicket.ThreadContext;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

/**
 * The serializer of the components given none of their own: the application's default, or else
 * the one of the Jackson on the class path.
 */
class JsonSerializersTest extends WicketTestCase
{
	@Test
	void takesJackson3WhenItIsOnTheClassPath()
	{
		assertSame(JacksonJsonSerializer.INSTANCE, JsonSerializers.detect(name -> true));
		// the tests have both
		assertSame(JacksonJsonSerializer.INSTANCE, JsonSerializers.getDefault(null));
	}

	@Test
	void takesJackson2WithoutJackson3()
	{
		assertSame(Jackson2JsonSerializer.INSTANCE,
			JsonSerializers.detect(name -> !name.startsWith("tools.jackson.")));
	}

	@Test
	void tellsWhatIsMissingWithoutJackson()
	{
		IJsonSerializer serializer = JsonSerializers.detect(name -> false);

		IllegalStateException e = assertThrows(IllegalStateException.class,
			() -> serializer.toJson(List.of()));
		assertTrue(e.getMessage().contains("tools.jackson.core:jackson-databind"), e.getMessage());
		assertTrue(e.getMessage().contains("com.fasterxml.jackson.core:jackson-databind"),
			e.getMessage());
		assertTrue(e.getMessage().contains("JsonSerializers.setDefault"), e.getMessage());
	}

	@Test
	void theApplicationSetsItsDefaultOnce()
	{
		IJsonSerializer mine = value -> "mine";
		JsonSerializers.setDefault(tester.getApplication(), mine);

		assertSame(mine, JsonSerializers.getDefault());
		assertSame(mine, JsonSerializers.getDefault(tester.getApplication()));
		assertSame(mine, newTable().getJsonSerializer());

		JsonSerializers.setDefault(tester.getApplication(), null);
		assertSame(JacksonJsonSerializer.INSTANCE, JsonSerializers.getDefault());
	}

	@Test
	void aComponentsOwnSerializerComesFirst()
	{
		JsonSerializers.setDefault(tester.getApplication(), value -> "default");
		IJsonSerializer own = value -> "own";

		JsonBasedDynamicDataTable<String, String> table = newTable();
		table.setJsonSerializer(own);

		assertSame(own, table.getJsonSerializer());
	}

	@Test
	void outsideOfAnApplicationTheClassPathDecides()
	{
		JsonSerializers.setDefault(tester.getApplication(), value -> "default");
		ThreadContext.detach();
		try
		{
			assertSame(JacksonJsonSerializer.INSTANCE, JsonSerializers.getDefault());
		}
		finally
		{
			ThreadContext.setApplication(tester.getApplication());
		}
	}

	private static JsonBasedDynamicDataTable<String, String> newTable()
	{
		return new JsonBasedDynamicDataTable<>("table",
			List.of(new DynamicColumn<String>(Model.of("Letter"), "{{letter}}")),
			new ListDynamicDataProvider<>(String.class, new ListModel<>(List.of("a")), row -> row),
			10);
	}
}
