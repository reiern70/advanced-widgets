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

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.string.StringValue;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Serves the rows of a {@link JsonBasedDynamicDataTable} as JSON from a resource the application
 * mounts, instead of from the page, for a table whose
 * {@link JsonBasedDynamicDataTable#setRowsSource(JsonBasedDynamicDataTable.RowsSource) rows
 * source} is {@link JsonBasedDynamicDataTable.RowsSource#RESOURCE RESOURCE}.
 * <p>
 * The resource is stateless: the table puts what it shows into the URL, as the parameters
 * {@value #FIRST} (the index of the first row), {@value #COUNT} (the number of rows),
 * {@value #SORT} and {@value #ASCENDING} (the sort property and order, if the table is sorted),
 * plus those of {@link JsonBasedDynamicDataTable#addRowsParameters(PageParameters)}, such as a
 * filter. {@link #newProvider(PageParameters)} turns them into the provider of the rows; the
 * answer is {@code {rows: [{key, number, data}], itemCount, currentPage}}, the rows as the
 * {@link #getJsonSerializer() serializer} writes them.
 * <p>
 * Mount it like any resource, for example
 * {@code mountResource("api/users", new ResourceReference("users") { ... getResource() ... })},
 * and give the table its reference with
 * {@link JsonBasedDynamicDataTable#setRowsResource(org.apache.wicket.request.resource.ResourceReference)}.
 * Anyone who knows the URL can read the rows, so a resource serving private data checks the
 * request, for example the session's user, in {@link #newProvider(PageParameters)}.
 * <p>
 * The resource can create, read, change and delete single rows too, for clients such as the React
 * and Svelte tables, which keep no page on the server:
 * <ul>
 * <li>{@code GET ?key=12} answers the row {@code {key, data}}, or 404 if there is none</li>
 * <li>{@code POST} with the row as JSON in its body creates it with
 * {@link #createRow(String)}, answering 201 and the row as it was saved</li>
 * <li>{@code PUT ?key=12} with the row as JSON changes it with {@link #updateRow(Object, String)},
 * answering the row as it was saved, or 404</li>
 * <li>{@code DELETE ?key=12} deletes it with {@link #deleteRow(Object)}, answering 204, or 404</li>
 * </ul>
 * A row that is not valid is answered with 400 and the message of the
 * {@link IllegalArgumentException} the method threw. The methods throw
 * {@link UnsupportedOperationException} by default, which answers 405: a resource only reads
 * unless it overrides them. Whoever may change rows has to be checked in those methods.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys of the rows
 * @since 11.0.0
 */
public abstract class JsonRowsResource<T, K extends Serializable> extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** The parameter holding the index of the first row, counting from 0. */
	public static final String FIRST = "first";

	/** The parameter holding the number of rows. */
	public static final String COUNT = "count";

	/** The parameter holding the sort property. */
	public static final String SORT = "sort";

	/** The parameter telling whether the rows are sorted ascending. */
	public static final String ASCENDING = "ascending";

	/** The most rows one request may ask for. */
	public static final int MAX_COUNT = 1000;

	/** The parameter naming the row a request reads, changes or deletes, as the table shows it. */
	public static final String KEY = "key";

	/** The longest body of a request creating or changing a row, in characters. */
	public static final int MAX_BODY = 64 * 1024;

	/**
	 * Creates the provider of the rows for a request, applying the parameters the table added,
	 * such as a filter. The sort is applied afterwards, if the provider is sortable.
	 *
	 * @param parameters
	 *            the parameters of the request
	 * @return the provider
	 */
	protected abstract IDynamicDataProvider<T, K> newProvider(PageParameters parameters);

	/**
	 * @return the serializer turning the rows into JSON, a {@link JacksonJsonSerializer} by
	 *         default
	 */
	protected IJsonSerializer getJsonSerializer()
	{
		return JacksonJsonSerializer.INSTANCE;
	}

	/**
	 * Creates a row from the JSON a client sent. Throws {@link UnsupportedOperationException} by
	 * default: the resource does not create rows.
	 *
	 * @param json
	 *            the row as JSON, as the client sent it
	 * @return the row as it was saved, with its key
	 * @throws IllegalArgumentException
	 *             if the row is not valid, its message telling why
	 */
	protected T createRow(String json)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Changes a row to the JSON a client sent. Throws {@link UnsupportedOperationException} by
	 * default: the resource does not change rows.
	 *
	 * @param row
	 *            the row as it is now
	 * @param json
	 *            the row as JSON, as the client sent it
	 * @return the row as it was saved
	 * @throws IllegalArgumentException
	 *             if the row is not valid, its message telling why
	 */
	protected T updateRow(T row, String json)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Deletes a row. Throws {@link UnsupportedOperationException} by default: the resource does
	 * not delete rows.
	 *
	 * @param row
	 *            the row
	 */
	protected void deleteRow(T row)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		String method = method(attributes);
		PageParameters parameters = attributes.getParameters();
		StringValue key = parameters.get(KEY);
		if ("GET".equals(method) || "HEAD".equals(method))
		{
			return key.isEmpty() ? rows(parameters) : row(attributes, parameters, key.toString());
		}
		try
		{
			switch (method)
			{
				case "POST":
					return created(parameters, createRow(readBody(attributes)));
				case "PUT":
					return key.isEmpty() ? error(attributes, 400, "The key of the row is missing")
						: change(parameters, key.toString(), attributes);
				case "DELETE":
					return key.isEmpty() ? error(attributes, 400, "The key of the row is missing")
						: delete(attributes, parameters, key.toString());
				default:
					return notAllowed(attributes);
			}
		}
		catch (UnsupportedOperationException e)
		{
			return notAllowed(attributes);
		}
		catch (IllegalArgumentException e)
		{
			return error(attributes, 400, e.getMessage());
		}
	}

	private ResourceResponse row(Attributes attributes, PageParameters parameters, String key)
	{
		IDynamicDataProvider<T, K> provider = newProvider(parameters);
		try
		{
			T row = find(provider, key);
			return row != null ? json(200, entry(provider, row, null))
				: error(attributes, 404, "No row " + key);
		}
		finally
		{
			provider.detach();
		}
	}

	private ResourceResponse created(PageParameters parameters, T row)
	{
		IDynamicDataProvider<T, K> provider = newProvider(parameters);
		try
		{
			return json(201, entry(provider, row, null));
		}
		finally
		{
			provider.detach();
		}
	}

	private ResourceResponse change(PageParameters parameters, String key, Attributes attributes)
	{
		IDynamicDataProvider<T, K> provider = newProvider(parameters);
		try
		{
			T row = find(provider, key);
			if (row == null)
			{
				return error(attributes, 404, "No row " + key);
			}
			return json(200, entry(provider, updateRow(row, readBody(attributes)), null));
		}
		finally
		{
			provider.detach();
		}
	}

	private ResourceResponse delete(Attributes attributes, PageParameters parameters, String key)
	{
		IDynamicDataProvider<T, K> provider = newProvider(parameters);
		try
		{
			T row = find(provider, key);
			if (row == null)
			{
				return error(attributes, 404, "No row " + key);
			}
			deleteRow(row);
			ResourceResponse response = new ResourceResponse();
			response.setStatusCode(204);
			response.disableCaching();
			response.setWriteCallback(new WriteCallback()
			{
				@Override
				public void writeData(Attributes attributes)
				{
				}
			});
			return response;
		}
		finally
		{
			provider.detach();
		}
	}

	private T find(IDynamicDataProvider<T, K> provider, String key)
	{
		K parsed = provider.parseKey(key);
		return parsed != null ? provider.findByKey(parsed) : null;
	}

	/**
	 * @return the row as the answer has it: its key, its number if it has one, and the row
	 */
	private Map<String, Object> entry(IDynamicDataProvider<T, K> provider, T row, Long number)
	{
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("key", provider.formatKey(provider.keyOf(row)));
		if (number != null)
		{
			entry.put("number", number);
		}
		entry.put("data", row);
		return entry;
	}

	private static String method(Attributes attributes)
	{
		if (attributes.getRequest().getContainerRequest() instanceof HttpServletRequest request)
		{
			return request.getMethod().toUpperCase(Locale.ROOT);
		}
		return "GET";
	}

	/**
	 * Reads the row a request creating or changing one sent.
	 *
	 * @param attributes
	 *            the attributes of the request
	 * @return the body of the request, at most {@value #MAX_BODY} characters
	 * @throws IllegalArgumentException
	 *             if it is longer or cannot be read
	 */
	protected String readBody(Attributes attributes)
	{
		if (attributes.getRequest().getContainerRequest() instanceof HttpServletRequest request)
		{
			try
			{
				StringBuilder body = new StringBuilder();
				char[] buffer = new char[4096];
				Reader reader = request.getReader();
				for (int read; (read = reader.read(buffer)) >= 0;)
				{
					body.append(buffer, 0, read);
					if (body.length() > MAX_BODY)
					{
						throw new IllegalArgumentException("The row is too long");
					}
				}
				return body.toString();
			}
			catch (IOException e)
			{
				throw new IllegalArgumentException("The row could not be read", e);
			}
		}
		return "";
	}

	private ResourceResponse notAllowed(Attributes attributes)
	{
		ResourceResponse response = error(attributes, 405, "Not allowed");
		response.getHeaders().addHeader("Allow", "GET, HEAD, POST, PUT, DELETE");
		return response;
	}

	/**
	 * @return the answer of an error: its status and {@code {error: message}}
	 */
	private ResourceResponse error(Attributes attributes, int status, String message)
	{
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("error", message != null ? message : "");
		byte[] bytes = getJsonSerializer().toJson(error).getBytes(StandardCharsets.UTF_8);
		ResourceResponse response = new ResourceResponse();
		response.setStatusCode(status);
		response.setContentType("application/json");
		response.setTextEncoding("UTF-8");
		response.disableCaching();
		// Wicket writes no body for a status from 300 on, so the error is written here
		attributes.getResponse().write(bytes);
		response.setWriteCallback(new WriteCallback()
		{
			@Override
			public void writeData(Attributes attributes)
			{
			}
		});
		return response;
	}

	private ResourceResponse json(int status, Object value)
	{
		byte[] bytes = getJsonSerializer().toJson(value).getBytes(StandardCharsets.UTF_8);
		ResourceResponse response = new ResourceResponse();
		response.setStatusCode(status);
		response.setContentType("application/json");
		response.setTextEncoding("UTF-8");
		response.setContentLength(bytes.length);
		response.disableCaching();
		response.setWriteCallback(new WriteCallback()
		{
			@Override
			public void writeData(Attributes attributes)
			{
				attributes.getResponse().write(bytes);
			}
		});
		return response;
	}

	private ResourceResponse rows(PageParameters parameters)
	{
		long first = Math.max(0, parameters.get(FIRST).toLong(0));
		long count = Math.min(MAX_COUNT, Math.max(1, parameters.get(COUNT).toLong(25)));

		IDynamicDataProvider<T, K> provider = newProvider(parameters);
		applySort(provider, parameters.get(SORT), parameters.get(ASCENDING));
		try
		{
			long itemCount = provider.size();
			List<Map<String, Object>> rows = new ArrayList<>();
			long number = first + 1;
			for (Iterator<? extends T> it = provider.iterator(first, count); it.hasNext();)
			{
				rows.add(entry(provider, it.next(), number++));
			}
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("rows", rows);
			data.put("itemCount", itemCount);
			data.put("currentPage", first / count);
			return json(200, data);
		}
		finally
		{
			provider.detach();
		}
	}

	@SuppressWarnings("unchecked")
	private static void applySort(IDynamicDataProvider<?, ?> provider, StringValue sort,
		StringValue ascending)
	{
		if (sort.isEmpty() || !(provider instanceof ISortStateLocator<?> locator))
		{
			return;
		}
		ISortState<Object> state = (ISortState<Object>)locator.getSortState();
		state.setPropertySortOrder(sort.toString(),
			ascending.toBoolean(true) ? SortOrder.ASCENDING : SortOrder.DESCENDING);
	}
}
