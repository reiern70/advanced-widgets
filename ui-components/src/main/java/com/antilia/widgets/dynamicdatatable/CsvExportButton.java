/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.convert.IConverter;

import com.antilia.widgets.theme.LookBehavior;
import com.antilia.widgets.theme.LookPart;

/**
 * The icon button of a {@link CsvExportToolbarAction}.
 */
class CsvExportButton<T, K extends Serializable, C extends IDetachable> extends Panel
{
	private static final long serialVersionUID = 1L;

	private final BaseDynamicDataTable<T, K, C> table;

	private final CsvExportToolbarAction<T, K> action;

	CsvExportButton(String id, BaseDynamicDataTable<T, K, C> table,
		CsvExportToolbarAction<T, K> action)
	{
		super(id);
		this.table = table;
		this.action = action;
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();

		AjaxDownloadBehavior download = new AjaxDownloadBehavior(new CsvResource());
		download.setLocation(action.getLocation());
		add(download);

		IModel<String> label = label();
		add(new AjaxLink<Void>("link")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				download.initiate(target);
			}
		}.add(AttributeModifier.replace("title", label),
			AttributeModifier.replace("aria-label", label),
			new LookBehavior(LookPart.TABLE_ACTION, LookPart.TABLE_EXPORT)));
	}

	private IModel<String> label()
	{
		IModel<String> label = action.getLabel();
		return label instanceof IComponentAssignedModel<String> assigned
			? assigned.wrapOnAssignment(this) : label;
	}

	private String header(C column)
	{
		IModel<String> header = table.getColumnTitle(column);
		if (header instanceof IComponentAssignedModel<String> assigned)
		{
			IModel<String> wrapped = assigned.wrapOnAssignment(table);
			try
			{
				return wrapped.getObject();
			}
			finally
			{
				wrapped.detach();
			}
		}
		return header != null ? header.getObject() : null;
	}

	@SuppressWarnings("unchecked")
	private String format(Object value)
	{
		if (value == null)
		{
			return "";
		}
		if (value instanceof TemporalAccessor)
		{
			return value.toString();
		}
		IConverter<Object> converter = (IConverter<Object>)table.getConverter(value.getClass());
		return converter.convertToString(value, table.getLocale());
	}

	private String quote(String value)
	{
		return '"' + (value != null ? value.replace("\"", "\"\"") : "") + '"';
	}

	private void write(Writer writer) throws IOException
	{
		List<C> columns = new ArrayList<>();
		for (C column : table.getVisibleColumns())
		{
			if (table.isColumnExportable(column))
			{
				columns.add(column);
			}
		}
		List<String> fields = new ArrayList<>();
		for (C column : columns)
		{
			fields.add(quote(header(column)));
		}
		writeLine(writer, fields);

		ISelection<T, K> selection = table.getSelection();
		Iterable<T> rows = selection.isEmpty() ? table.fetchAllRows() : selection.fetch();
		for (T row : rows)
		{
			fields.clear();
			for (C column : columns)
			{
				fields.add(quote(format(table.getColumnExportValue(column, row))));
			}
			writeLine(writer, fields);
		}
	}

	private void writeLine(Writer writer, List<String> fields) throws IOException
	{
		writer.write(String.join(String.valueOf(action.getDelimiter()), fields));
		writer.write("\r\n");
	}

	private class CsvResource extends AbstractResource
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected ResourceResponse newResourceResponse(Attributes attributes)
		{
			ResourceResponse response = new ResourceResponse();
			response.setContentType("text/csv");
			response.setTextEncoding(StandardCharsets.UTF_8.name());
			response.setFileName(action.getFileName());
			response.setContentDisposition(ContentDisposition.ATTACHMENT);
			response.disableCaching();
			response.setWriteCallback(new WriteCallback()
			{
				@Override
				public void writeData(Attributes attributes) throws IOException
				{
					Writer writer = new OutputStreamWriter(
						attributes.getResponse().getOutputStream(), StandardCharsets.UTF_8);
					write(writer);
					writer.flush();
				}
			});
			return response;
		}
	}
}
