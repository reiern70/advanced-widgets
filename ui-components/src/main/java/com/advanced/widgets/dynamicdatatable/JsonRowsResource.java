/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.string.StringValue;

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

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		PageParameters parameters = attributes.getParameters();
		long first = Math.max(0, parameters.get(FIRST).toLong(0));
		long count = Math.min(MAX_COUNT, Math.max(1, parameters.get(COUNT).toLong(25)));

		IDynamicDataProvider<T, K> provider = newProvider(parameters);
		applySort(provider, parameters.get(SORT), parameters.get(ASCENDING));
		String json;
		try
		{
			long itemCount = provider.size();
			List<Map<String, Object>> rows = new ArrayList<>();
			long number = first + 1;
			for (Iterator<? extends T> it = provider.iterator(first, count); it.hasNext();)
			{
				T row = it.next();
				Map<String, Object> entry = new LinkedHashMap<>();
				entry.put("key", provider.formatKey(provider.keyOf(row)));
				entry.put("number", number++);
				entry.put("data", row);
				rows.add(entry);
			}
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("rows", rows);
			data.put("itemCount", itemCount);
			data.put("currentPage", first / count);
			json = getJsonSerializer().toJson(data);
		}
		finally
		{
			provider.detach();
		}

		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		ResourceResponse response = new ResourceResponse();
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
