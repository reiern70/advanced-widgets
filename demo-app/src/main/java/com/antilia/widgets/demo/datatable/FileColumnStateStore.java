/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.wicket.WicketRuntimeException;

import com.antilia.widgets.dynamicdatatable.ColumnState;
import com.antilia.widgets.dynamicdatatable.IColumnStateStore;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Keeps the column states of tables in a properties file in the temporary directory, as JSON, so
 * they survive restarts of the server. A real application would rather keep them in its database,
 * by user; this one shares them among everybody using the demos.
 */
class FileColumnStateStore implements IColumnStateStore
{
	private static final long serialVersionUID = 1L;

	static final FileColumnStateStore INSTANCE = new FileColumnStateStore();

	private static final File FILE = new File(System.getProperty("java.io.tmpdir"),
		"antilia-widgets-column-states.properties");

	@Override
	public synchronized ColumnState load(String key)
	{
		String json = read().getProperty(key);
		if (json == null)
		{
			return null;
		}
		JSONObject object = new JSONObject(json);
		Map<String, Double> widths = new LinkedHashMap<>();
		JSONObject widthsObject = object.getJSONObject("widths");
		for (String id : widthsObject.keySet())
		{
			widths.put(id, widthsObject.getDouble(id));
		}
		return new ColumnState(strings(object.getJSONArray("order")),
			strings(object.getJSONArray("hidden")), widths);
	}

	@Override
	public synchronized void save(String key, ColumnState state)
	{
		JSONObject object = new JSONObject();
		object.put("order", new JSONArray(state.getOrder()));
		object.put("hidden", new JSONArray(state.getHidden()));
		object.put("widths", new JSONObject(state.getWidths()));
		Properties properties = read();
		properties.setProperty(key, object.toString());
		try (OutputStream out = Files.newOutputStream(FILE.toPath()))
		{
			properties.store(out, "Column states of the Advanced Widgets demos");
		}
		catch (IOException e)
		{
			throw new WicketRuntimeException("Cannot write " + FILE, e);
		}
	}

	private static Properties read()
	{
		Properties properties = new Properties();
		if (FILE.exists())
		{
			try (InputStream in = Files.newInputStream(FILE.toPath()))
			{
				properties.load(in);
			}
			catch (IOException e)
			{
				throw new WicketRuntimeException("Cannot read " + FILE, e);
			}
		}
		return properties;
	}

	private static List<String> strings(JSONArray array)
	{
		List<String> strings = new ArrayList<>(array.length());
		for (int i = 0; i < array.length(); i++)
		{
			strings.add(array.getString(i));
		}
		return strings;
	}
}
