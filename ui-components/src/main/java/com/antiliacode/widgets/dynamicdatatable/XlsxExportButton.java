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
import java.io.OutputStream;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.convert.IConverter;

import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;

/**
 * The icon button of an {@link XlsxExportToolbarAction}.
 */
class XlsxExportButton<T, K extends Serializable, C extends IDetachable> extends Panel
{
	private static final long serialVersionUID = 1L;

	private final BaseDynamicDataTable<T, K, C> table;

	private final XlsxExportToolbarAction<T, K> action;

	XlsxExportButton(String id, BaseDynamicDataTable<T, K, C> table,
		XlsxExportToolbarAction<T, K> action)
	{
		super(id);
		this.table = table;
		this.action = action;
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();

		AjaxDownloadBehavior download = new AjaxDownloadBehavior(new XlsxResource());
		download.setLocation(action.getLocation());
		add(download);

		IModel<String> label = wrap(action.getLabel());
		AjaxLink<Void> link = new AjaxLink<>("link")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				download.initiate(target);
			}
		};
		link.add(AttributeModifier.replace("title", label),
			AttributeModifier.replace("aria-label", label),
			new LookBehavior(LookPart.TABLE_ACTION, LookPart.TABLE_EXPORT_XLSX));
		link.add(new Label("icon", () -> action.getIcon().getMarkup()).setEscapeModelStrings(false)
			.setRenderBodyOnly(true));
		add(link);
	}

	@Override
	protected void onDetach()
	{
		detach(action.getTitle());
		detach(action.getCaption());
		super.onDetach();
	}

	private static void detach(IModel<?> model)
	{
		if (model != null)
		{
			model.detach();
		}
	}

	/**
	 * @return the name of the sheet: the action's title, else the name of the file without its
	 *         extension
	 */
	String sheetName()
	{
		String title = read(action.getTitle());
		if (title != null)
		{
			return title;
		}
		String file = action.getFileName();
		int dot = file.lastIndexOf('.');
		return dot > 0 ? file.substring(0, dot) : file;
	}

	private <M> M read(IModel<M> model)
	{
		if (model == null)
		{
			return null;
		}
		IModel<M> wrapped = wrap(model);
		try
		{
			return wrapped.getObject();
		}
		finally
		{
			if (wrapped != model)
			{
				wrapped.detach();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private String format(Object value)
	{
		if (value instanceof TemporalAccessor)
		{
			return value.toString();
		}
		IConverter<Object> converter = (IConverter<Object>)table.getConverter(value.getClass());
		return converter.convertToString(value, table.getLocale());
	}

	private void write(OutputStream out) throws IOException
	{
		ZoneId zone = action.getZoneId() != null ? action.getZoneId() : ZoneId.systemDefault();
		XlsxWriter writer = new XlsxWriter(out, sheetName(), zone, this::format,
			action.getMaxRows());
		writer.caption(read(action.getCaption()));

		List<C> columns = table.getExportColumns();
		List<String> headers = new ArrayList<>();
		List<String> formats = new ArrayList<>();
		for (C column : columns)
		{
			headers.add(table.getExportHeader(column));
			formats.add(table.getColumnExportFormat(column));
		}
		writer.header(headers, formats);

		ISelection<T, K> selection = table.getSelection();
		// the footer sums up all the rows: not under the selected ones only
		boolean footer = action.isExportFooter() && selection.isEmpty() &&
			columns.stream().anyMatch(table::hasFooter);
		Iterator<T> rows = (selection.isEmpty() ? table.fetchAllRows() : selection.fetch())
			.iterator();
		writeRows(writer, columns, rows, footer ? 1 : 0);
		if (rows.hasNext())
		{
			// the sheet cannot hold all the rows: as many as it can, and no footer, which would
			// not sum up those shown
			footer = false;
			writeRows(writer, columns, rows, 0);
		}
		if (footer)
		{
			writer.footer(table.getFooterExportValues(columns));
		}
		writer.finish();
	}

	private void writeRows(XlsxWriter writer, List<C> columns, Iterator<T> rows, int reserved)
		throws IOException
	{
		List<Object> values = new ArrayList<>();
		while (rows.hasNext() && writer.hasRoom(reserved))
		{
			T row = rows.next();
			values.clear();
			for (C column : columns)
			{
				values.add(table.getColumnExportValue(column, row));
			}
			writer.row(values);
		}
	}

	private class XlsxResource extends AbstractResource
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected ResourceResponse newResourceResponse(Attributes attributes)
		{
			ResourceResponse response = new ResourceResponse();
			response.setContentType(XlsxExportToolbarAction.CONTENT_TYPE);
			response.setFileName(action.getFileName());
			response.setContentDisposition(ContentDisposition.ATTACHMENT);
			response.disableCaching();
			response.setWriteCallback(new WriteCallback()
			{
				@Override
				public void writeData(Attributes attributes) throws IOException
				{
					OutputStream out = attributes.getResponse().getOutputStream();
					write(out);
					out.flush();
				}
			});
			return response;
		}
	}
}
