/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

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
