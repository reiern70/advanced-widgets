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

import java.util.function.Predicate;

import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.util.lang.Args;

/**
 * The serializer writing the JSON of the components that are given none of their own:
 * {@link JsonBasedDynamicDataTable}, the autocompletes, {@link JsonRowsResource},
 * {@link com.antiliacode.widgets.tree.JsonBasedTree JsonBasedTree},
 * {@link com.antiliacode.widgets.jsonlist.JsonBasedInfiniteList JsonBasedInfiniteList} and the
 * {@link IWebSocketLightWeightMessage}s.
 * <p>
 * That is the application's default, set once in its {@code init()}:
 *
 * <pre>
 * JsonSerializers.setDefault(this, new MySerializer());
 * </pre>
 *
 * or, without one, the serializer of the Jackson the application has on its class path:
 * {@link JacksonJsonSerializer} for Jackson 3 ({@code tools.jackson.core:jackson-databind}), or
 * else {@link Jackson2JsonSerializer} for Jackson 2
 * ({@code com.fasterxml.jackson.core:jackson-databind}). Both are optional dependencies of the
 * components. With neither, nor a default, a component without a serializer of its own fails as
 * it writes its JSON, telling so.
 *
 * @since 11.0.0
 */
public final class JsonSerializers
{
	private static final MetaDataKey<IJsonSerializer> DEFAULT = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	private JsonSerializers()
	{
	}

	/**
	 * Sets the serializer of the components that are given none of their own.
	 *
	 * @param application
	 *            the application
	 * @param serializer
	 *            the serializer, or {@code null} for the one of the Jackson on the class path
	 */
	public static void setDefault(Application application, IJsonSerializer serializer)
	{
		Args.notNull(application, "application");
		application.setMetaData(DEFAULT, serializer);
	}

	/**
	 * @return the default serializer of the application of the current thread, see
	 *         {@link #getDefault(Application)}
	 */
	public static IJsonSerializer getDefault()
	{
		return getDefault(Application.exists() ? Application.get() : null);
	}

	/**
	 * @param application
	 *            the application, or {@code null} outside of one
	 * @return the serializer {@link #setDefault(Application, IJsonSerializer) set} on the
	 *         application, or else the one of the Jackson on the class path: Jackson 3, or else
	 *         Jackson 2; with neither, one failing with an {@link IllegalStateException} that tells
	 *         what is missing
	 */
	public static IJsonSerializer getDefault(Application application)
	{
		IJsonSerializer serializer = application != null ? application.getMetaData(DEFAULT) : null;
		return serializer != null ? serializer : Detected.SERIALIZER;
	}

	/**
	 * @param present
	 *            tells whether a class is on the class path
	 * @return the serializer of the Jackson present
	 */
	static IJsonSerializer detect(Predicate<String> present)
	{
		if (present.test("tools.jackson.databind.json.JsonMapper"))
		{
			return JacksonJsonSerializer.INSTANCE;
		}
		if (present.test("com.fasterxml.jackson.databind.json.JsonMapper"))
		{
			return Jackson2JsonSerializer.INSTANCE;
		}
		return Missing.INSTANCE;
	}

	private static boolean isPresent(String className)
	{
		try
		{
			Class.forName(className, false, JsonSerializers.class.getClassLoader());
			return true;
		}
		catch (ClassNotFoundException | LinkageError e)
		{
			return false;
		}
	}

	private static final class Detected
	{
		private static final IJsonSerializer SERIALIZER = detect(JsonSerializers::isPresent);
	}

	/**
	 * The serializer without Jackson nor a default: it tells what is missing.
	 */
	private static final class Missing implements IJsonSerializer
	{
		private static final long serialVersionUID = 1L;

		private static final Missing INSTANCE = new Missing();

		@Override
		public String toJson(Object value)
		{
			throw new IllegalStateException("No JSON serializer: put Jackson 3 " +
				"(tools.jackson.core:jackson-databind) or Jackson 2 " +
				"(com.fasterxml.jackson.core:jackson-databind) on the class path, set a default " +
				"with JsonSerializers.setDefault(application, serializer) in the application's " +
				"init(), or give the component an IJsonSerializer of its own");
		}

		private Object readResolve()
		{
			return INSTANCE;
		}
	}
}
