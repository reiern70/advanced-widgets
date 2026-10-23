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

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serializes rows with Jackson, which is an optional dependency of ui-components: an
 * application using this serializer has to add {@code tools.jackson.core:jackson-databind}
 * itself. Every property Jackson sees on a row is sent to the browser.
 *
 * @since 11.0.0
 */
public class JacksonJsonSerializer implements IJsonSerializer
{
	private static final long serialVersionUID = 1L;

	/** The shared instance. */
	public static final JacksonJsonSerializer INSTANCE = new JacksonJsonSerializer();

	private static final class Holder
	{
		private static final ObjectMapper MAPPER = JsonMapper.builder().build();
	}

	@Override
	public String toJson(Object value)
	{
		try
		{
			return Holder.MAPPER.writeValueAsString(value);
		}
		catch (NoClassDefFoundError e)
		{
			throw new IllegalStateException(
				"JacksonJsonSerializer needs tools.jackson.core:jackson-databind on the classpath, " +
					"or set another IJsonSerializer on the JsonBasedDynamicDataTable",
				e);
		}
	}
}
