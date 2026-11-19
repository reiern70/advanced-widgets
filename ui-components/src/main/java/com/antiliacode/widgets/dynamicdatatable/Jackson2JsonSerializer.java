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

import org.apache.wicket.WicketRuntimeException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Serializes rows with Jackson 2, for an application on Jackson 2 rather than Jackson 3
 * ({@link JacksonJsonSerializer}). Jackson 2 is an optional dependency of ui-components: an
 * application using this serializer has {@code com.fasterxml.jackson.core:jackson-databind}
 * itself. {@link JsonSerializers#getDefault()} picks this serializer when Jackson 2 is on the
 * class path and Jackson 3 is not. Every property Jackson sees on a row is sent to the browser.
 * <p>
 * The mapper registers the modules Jackson finds on the class path, such as
 * {@code jackson-datatype-jsr310} for the {@code java.time} values, and writes dates as text, as
 * Jackson 3 does, rather than as numbers.
 *
 * @since 11.0.0
 */
public class Jackson2JsonSerializer implements IJsonSerializer
{
	private static final long serialVersionUID = 1L;

	/** The shared instance. */
	public static final Jackson2JsonSerializer INSTANCE = new Jackson2JsonSerializer();

	private static final class Holder
	{
		private static final ObjectMapper MAPPER = JsonMapper.builder()
			.findAndAddModules()
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.build();
	}

	@Override
	public String toJson(Object value)
	{
		try
		{
			return Holder.MAPPER.writeValueAsString(value);
		}
		catch (JsonProcessingException e)
		{
			throw new WicketRuntimeException("Jackson 2 cannot write " +
				(value == null ? "null" : value.getClass().getName()) + " as JSON", e);
		}
		catch (NoClassDefFoundError e)
		{
			throw new IllegalStateException(
				"Jackson2JsonSerializer needs com.fasterxml.jackson.core:jackson-databind on the " +
					"class path, or set another IJsonSerializer, see JsonSerializers",
				e);
		}
	}
}
