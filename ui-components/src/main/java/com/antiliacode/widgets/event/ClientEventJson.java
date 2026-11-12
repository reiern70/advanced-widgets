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
package com.antiliacode.widgets.event;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.json.JSONFunction;
import org.apache.wicket.util.string.Strings;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.github.openjson.JSONString;

/**
 * Writes an event as JSON, by its public getters: {@link IClientEvent#toJson()}.
 */
final class ClientEventJson
{
	/** The property of the code of the event, which goes as code. */
	static final String CODE = "eventHandlerClientSideCode";

	/**
	 * The property of the attributes of the requests of an event going to the server, which the
	 * {@link ClientEventsBehavior} writes: no event's own.
	 */
	static final String AJAX = "ajax";

	private ClientEventJson()
	{
	}

	/**
	 * @param event
	 *            an event
	 * @return its properties, as JSON, without those that are {@code null} and those of the
	 *         attributes of its requests, which {@link AbstractAjaxRoundTripEvent} keeps, the code as
	 *         code
	 */
	static JSONObject of(IClientEvent event)
	{
		BeanInfo info;
		try
		{
			info = Introspector.getBeanInfo(event.getClass(), Object.class);
		}
		catch (IntrospectionException e)
		{
			throw new WicketRuntimeException("Cannot read the properties of the event " +
				event.getEventName(), e);
		}
		JSONObject json = new JSONObject();
		for (PropertyDescriptor property : info.getPropertyDescriptors())
		{
			Method getter = property.getReadMethod();
			if (getter != null && getter.getDeclaringClass() == AbstractAjaxRoundTripEvent.class)
			{
				// the method, the extra parameters and the rest go in the attributes of the
				// requests, which the ClientEventsBehavior writes
				continue;
			}
			Object value = getter == null ? null : read(event, getter, property);
			if (value != null)
			{
				json.put(property.getName(), json(value));
			}
		}
		String code = event.getEventHandlerClientSideCode();
		if (Strings.isEmpty(code))
		{
			json.remove(CODE);
		}
		else
		{
			json.put(CODE, new JSONFunction(code));
		}
		return json;
	}

	/**
	 * @param value
	 *            a value, not {@code null}
	 * @return it as JSON: a text, a number, a boolean or JSON as it is, an enum by its name, a
	 *         collection, a map or an array as JSON of their values, anything else, a component
	 *         for example, as its {@code toString()}, never walked through
	 */
	private static Object json(Object value)
	{
		if (value instanceof String || value instanceof Number || value instanceof Boolean ||
			value instanceof JSONObject || value instanceof JSONArray || value instanceof JSONString)
		{
			return value;
		}
		if (value instanceof Enum<?> constant)
		{
			return constant.name();
		}
		if (value instanceof Map<?, ?> map)
		{
			JSONObject object = new JSONObject();
			map.forEach((key, item) -> object.put(String.valueOf(key), item == null ?
				JSONObject.NULL : json(item)));
			return object;
		}
		if (value instanceof Collection<?> items)
		{
			JSONArray array = new JSONArray();
			items.forEach(item -> array.put(item == null ? JSONObject.NULL : json(item)));
			return array;
		}
		if (value.getClass().isArray())
		{
			JSONArray array = new JSONArray();
			for (int index = 0; index < Array.getLength(value); index++)
			{
				Object item = Array.get(value, index);
				array.put(item == null ? JSONObject.NULL : json(item));
			}
			return array;
		}
		return value.toString();
	}

	private static Object read(IClientEvent event, Method getter, PropertyDescriptor property)
	{
		try
		{
			return accessible(event, getter).invoke(event);
		}
		catch (IllegalAccessException e)
		{
			throw new WicketRuntimeException("Cannot read the property " + property.getName() +
				" of the event " + event.getEventName(), e);
		}
		catch (InvocationTargetException e)
		{
			throw new WicketRuntimeException("Reading the property " + property.getName() +
				" of the event " + event.getEventName() + " failed", e.getCause());
		}
	}

	/**
	 * @return the getter, or, when it is of a class not public, an anonymous one for example, the
	 *         same method of a public class or interface above it, which runs the same code
	 */
	private static Method accessible(Object bean, Method getter)
	{
		if (getter.canAccess(bean))
		{
			return getter;
		}
		Deque<Class<?>> types = new ArrayDeque<>();
		Set<Class<?>> seen = new HashSet<>();
		types.add(bean.getClass());
		while (!types.isEmpty())
		{
			Class<?> type = types.poll();
			if (!seen.add(type))
			{
				continue;
			}
			if (Modifier.isPublic(type.getModifiers()))
			{
				try
				{
					Method method = type.getDeclaredMethod(getter.getName());
					if (method.canAccess(bean))
					{
						return method;
					}
				}
				catch (NoSuchMethodException e)
				{
					// not declared there: above it then
				}
			}
			if (type.getSuperclass() != null)
			{
				types.add(type.getSuperclass());
			}
			types.addAll(List.of(type.getInterfaces()));
		}
		// declared by the class alone, a getter an anonymous class adds for example: when the
		// module of the class does not open it, invoking the getter tells
		getter.trySetAccessible();
		return getter;
	}
}
