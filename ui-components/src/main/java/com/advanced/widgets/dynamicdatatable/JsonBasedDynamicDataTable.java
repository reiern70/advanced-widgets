/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.lang.Args;

import com.advanced.widgets.table.IMovableColumn;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * A dynamic data table whose body is rendered in the browser from JSON, instead of by Wicket
 * components.
 * <p>
 * Wicket renders only the {@code <table>} and its header. The rows come as JSON, either pulled
 * from the table's own endpoint (on load, on {@link #refresh(IPartialPageRequestHandler)} and,
 * optionally, {@link #setPollInterval(Duration) periodically}) or pushed by the application, for
 * example over a web socket with an {@link IWebSocketLightWeightMessage}. Each cell is the
 * column's template evaluated against the row's JSON by the {@link #setTemplateEngine template
 * engine}. Links in the cells are plain markup, not components, whose {@code data-dt-action}
 * attributes the table dispatches; see {@link BaseDynamicDataTable}.
 * <p>
 * Action contributors are registered from the {@link CompoundDynamicColumn}s the table is given,
 * and with {@link #addActionContributor(IAjaxActionColumnContributor)} for contributors used in
 * other columns.
 * <p>
 * The endpoint serves the rows of the current page. When a reload finds that the number of rows or
 * the current page has changed, the browser has the toolbars re-rendered. Rows pushed by the
 * application are shown as they are.
 * <p>
 * With an {@link ISortableDynamicDataProvider}, the header of every {@link ISortableDynamicColumn}
 * sorts the rows. A {@link SortableDynamicColumn} is hideable and movable if the column it wraps
 * is.
 * <p>
 * The column templates are written into the page as markup and have to be authored by the
 * developer; see {@link IDynamicColumn}. Every property the {@link #setJsonSerializer serializer}
 * produces for a row is sent to the browser.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys of the rows
 * @since 11.0.0
 */
public class JsonBasedDynamicDataTable<T, K extends Serializable>
	extends
		BaseDynamicDataTable<T, K, IDynamicColumn<T>>
{
	private static final long serialVersionUID = 1L;

	private final Map<String, IWebSocketMessageType> messageTypes = new LinkedHashMap<>();

	private final DataBehavior dataBehavior;

	private ITemplateEngine templateEngine = BuiltInTemplateEngine.INSTANCE;

	private IJsonSerializer jsonSerializer = JacksonJsonSerializer.INSTANCE;

	private Duration pollInterval;

	private boolean pushEnabled;

	/**
	 * @param id
	 *            component id
	 * @param columns
	 *            the columns, in display order, all of them shown
	 * @param provider
	 *            supplies the rows and resolves them back from their keys
	 * @param rowsPerPage
	 *            the number of rows shown on a page
	 */
	public JsonBasedDynamicDataTable(String id, List<? extends IDynamicColumn<T>> columns,
		IDynamicDataProvider<T, K> provider, long rowsPerPage)
	{
		super(id, columns, provider, rowsPerPage);

		for (IDynamicColumn<T> column : getColumns())
		{
			IDynamicColumn<T> unwrapped = unwrap(column);
			if (unwrapped instanceof CompoundDynamicColumn<T> compound)
			{
				for (IDynamicColumnContributor<T> contributor : compound.getContributors())
				{
					if (contributor instanceof IAjaxActionColumnContributor<T> actionContributor)
					{
						addActionContributor(actionContributor);
					}
				}
			}
		}

		addMessageType(UpdateRowsMessageType.INSTANCE);
		addMessageType(RefreshMessageType.INSTANCE);
		addMessageType(UpdateRowMessageType.INSTANCE);

		add(dataBehavior = new DataBehavior());
	}

	private static <T> IDynamicColumn<T> unwrap(IDynamicColumn<T> column)
	{
		return column instanceof SortableDynamicColumn<T, ?> sortable ? sortable.getColumn()
			: column;
	}

	/**
	 * Registers the browser-side function handling the messages of a type pushed to this table.
	 * {@link UpdateRowsMessageType}, {@link UpdateRowMessageType} and {@link RefreshMessageType}
	 * are registered already.
	 *
	 * @param messageType
	 *            the message type
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             if another message type is registered with the same id
	 */
	public JsonBasedDynamicDataTable<T, K> addMessageType(IWebSocketMessageType messageType)
	{
		Args.notNull(messageType, "messageType");
		String typeId = Args.notEmpty(messageType.getTypeId(), "typeId");
		IWebSocketMessageType registered = messageTypes.putIfAbsent(typeId, messageType);
		if (registered != null && registered != messageType)
		{
			throw new IllegalArgumentException(
				"Another message type is already registered as '" + typeId + "'");
		}
		return this;
	}

	@Override
	protected Component newRowsBody(String id)
	{
		return new WebMarkupContainer(id);
	}

	@Override
	protected void refreshRows(IPartialPageRequestHandler target)
	{
		target.appendJavaScript("if (" + getVarName() + ") { " + getVarName() + ".update(" +
			currentPageJson() + "); }");
	}

	/**
	 * @return whether the user can hide the column: whether it, or the column a
	 *         {@link SortableDynamicColumn} wraps, implements {@link IHideableColumn}
	 */
	@Override
	public boolean isHideable(IDynamicColumn<T> column)
	{
		return unwrap(column) instanceof IHideableColumn;
	}

	/**
	 * @return whether the user can move the column: whether it, or the column a
	 *         {@link SortableDynamicColumn} wraps, implements {@link IMovableColumn}
	 */
	@Override
	public boolean isMovable(IDynamicColumn<T> column)
	{
		return unwrap(column) instanceof IMovableColumn;
	}

	@Override
	protected String getDeclaredColumnId(IDynamicColumn<T> column)
	{
		return column.getId();
	}

	@Override
	protected Component newHeader(String id, IDynamicColumn<T> column)
	{
		return column.newHeader(id);
	}

	@Override
	protected IModel<String> getColumnTitle(IDynamicColumn<T> column)
	{
		return column.getHeader();
	}

	@Override
	protected String getColumnCssClass(IDynamicColumn<T> column)
	{
		return column.getCssClass();
	}

	@Override
	protected boolean isColumnResizable(IDynamicColumn<T> column)
	{
		return column.isResizable();
	}

	@Override
	protected Object getSortProperty(IDynamicColumn<T> column)
	{
		return column instanceof ISortableDynamicColumn<?, ?> sortable && sortable.isSortable()
			? sortable.getSortProperty() : null;
	}

	@Override
	protected boolean isColumnExportable(IDynamicColumn<T> column)
	{
		return column.isExportable();
	}

	@Override
	protected Object getColumnExportValue(IDynamicColumn<T> column, T row)
	{
		return column.getExportValue(row);
	}

	/**
	 * @return the rows of the current page as the browser reads them, with the paging state, and
	 *         calls {@link #onRowsSent(List)}
	 */
	private String currentPageJson()
	{
		List<T> rows = getCurrentPageRowList();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("rows", toRowEntries(rows, getFirstRowNumber()));
		data.put("itemCount", getItemCount());
		data.put("currentPage", getCurrentPage());
		String json = jsonSerializer.toJson(data);
		onRowsSent(getCurrentPageKeys());
		return json;
	}

	/**
	 * Called after the rows of the current page were sent to the browser, which shows them from
	 * now on, for example to push updates only for these rows. Does nothing by default.
	 *
	 * @param keys
	 *            the keys of the rows sent, in display order
	 */
	protected void onRowsSent(List<K> keys)
	{
	}

	/**
	 * Makes the browser fetch the rows of the current page again by pushing a
	 * {@link #newRefreshMessage() refresh message} over the page's web socket connection. The
	 * table has to {@link #setPushEnabled(boolean) accept pushes}.
	 *
	 * @return {@code false} if the page has no open web socket connection, {@code true} if the
	 *         message was sent
	 * @throws IOException
	 *             if the message cannot be sent
	 * @see IWebSocketLightWeightMessage#send(org.apache.wicket.Component)
	 */
	public boolean refreshViaWebSockets() throws IOException
	{
		return newRefreshMessage().send(this);
	}

	/**
	 * Builds a message of the {@link UpdateRowsMessageType update rows} type, replacing the rows of
	 * this table with the given ones. The rows are shown as they are, regardless of the current
	 * page.
	 *
	 * @param rows
	 *            the rows to show
	 * @return the message
	 */
	public IWebSocketLightWeightMessage newUpdateRowsMessage(List<T> rows)
	{
		return new UpdateRowsMessage(getMarkupId(),
			toRowEntries(rows, getFirstRowNumber()));
	}

	/**
	 * Builds a message of the {@code refresh} type, making this table fetch the rows of its
	 * current page again.
	 *
	 * @return the message
	 */
	public IWebSocketLightWeightMessage newRefreshMessage()
	{
		return new RefreshMessage(getMarkupId());
	}

	/**
	 * Builds a message of the {@link UpdateRowMessageType update row} type, repainting one row of
	 * this table, if the browser shows it, from the given row.
	 *
	 * @param row
	 *            the row in its current state
	 * @return the message
	 * @see UpdateRowMessageType#newMessage(String, String, Object)
	 */
	public IWebSocketLightWeightMessage newUpdateRowMessage(T row)
	{
		return UpdateRowMessageType.newMessage(getMarkupId(), getRowKey(row), row);
	}

	/**
	 * @param templateEngine
	 *            the engine evaluating the column templates in the browser
	 * @return {@code this}
	 */
	public JsonBasedDynamicDataTable<T, K> setTemplateEngine(ITemplateEngine templateEngine)
	{
		this.templateEngine = Args.notNull(templateEngine, "templateEngine");
		return this;
	}

	/**
	 * @param jsonSerializer
	 *            the serializer turning rows into JSON
	 * @return {@code this}
	 */
	public JsonBasedDynamicDataTable<T, K> setJsonSerializer(IJsonSerializer jsonSerializer)
	{
		this.jsonSerializer = Args.notNull(jsonSerializer, "jsonSerializer");
		return this;
	}

	/**
	 * @param pollInterval
	 *            how often the browser fetches the rows again, or {@code null} not to poll
	 * @return {@code this}
	 */
	public JsonBasedDynamicDataTable<T, K> setPollInterval(Duration pollInterval)
	{
		this.pollInterval = pollInterval;
		return this;
	}

	/**
	 * @param pushEnabled
	 *            whether the table picks up the {@link IWebSocketLightWeightMessage}s addressed to
	 *            it that arrive over Wicket's web socket connection
	 * @return {@code this}
	 */
	public JsonBasedDynamicDataTable<T, K> setPushEnabled(boolean pushEnabled)
	{
		this.pushEnabled = pushEnabled;
		return this;
	}

	/**
	 * @return the serializer turning rows into JSON
	 */
	public IJsonSerializer getJsonSerializer()
	{
		return jsonSerializer;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		templateEngine.renderHead(response);
	}

	@Override
	protected void configure(JSONObject config)
	{
		super.configure(config);
		JSONArray columnConfigs = new JSONArray();
		for (IDynamicColumn<T> column : getVisibleColumns())
		{
			JSONObject columnConfig = new JSONObject();
			columnConfig.put("template", column.getTemplate(this));
			columnConfig.put("cssClass", column.getCssClass());
			columnConfigs.put(columnConfig);
		}
		config.put("dataUrl", dataBehavior.getCallbackUrl().toString());
		config.put("engine", templateEngine.getName());
		config.put("columns", columnConfigs);
		config.put("push", pushEnabled);
		if (pollInterval != null)
		{
			config.put("pollMillis", pollInterval.toMillis());
		}
	}

	/**
	 * @return the handlers of the pushed messages and, in an Ajax request, the rows of the current
	 *         page
	 */
	@Override
	protected String getScriptArguments()
	{
		boolean partial = getRequestCycle().find(IPartialPageRequestHandler.class).isPresent();
		return ", " + buildMessageHandlers() +
			(partial ? ", " + escapeForScript(currentPageJson()) : "");
	}

	/**
	 * @return the JSON with {@code <}, {@code >} and {@code &} escaped, so it cannot end the script
	 *         or the section of the Ajax response it is written into
	 */
	private static String escapeForScript(String json)
	{
		return json.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
	}

	private String buildMessageHandlers()
	{
		StringJoiner handlers = new StringJoiner(", ", "{", "}");
		for (IWebSocketMessageType messageType : messageTypes.values())
		{
			handlers.add(JSONObject.quote(messageType.getTypeId()) + ": " +
				messageType.getTypeFunction());
		}
		return handlers.toString();
	}

	private List<Map<String, Object>> toRowEntries(List<T> rows, long firstNumber)
	{
		List<Map<String, Object>> entries = new ArrayList<>(rows.size());
		long number = firstNumber;
		for (T row : rows)
		{
			K key = getProvider().keyOf(row);
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("key", getProvider().formatKey(key));
			entry.put("number", number++);
			entry.put("selected", isSelected(key));
			entry.put("data", row);
			entries.add(entry);
		}
		return entries;
	}

	abstract static class TableMessage implements IWebSocketLightWeightMessage
	{
		private final String tableId;

		private final String typeId;

		TableMessage(String tableId, String typeId)
		{
			this.tableId = tableId;
			this.typeId = typeId;
		}

		@Override
		public String getTableId()
		{
			return tableId;
		}

		@Override
		public String getTypeId()
		{
			return typeId;
		}
	}

	private static class RefreshMessage extends TableMessage
	{
		RefreshMessage(String tableId)
		{
			super(tableId, RefreshMessageType.TYPE_ID);
		}
	}

	private static class UpdateRowsMessage extends TableMessage
	{
		private final List<Map<String, Object>> rows;

		UpdateRowsMessage(String tableId, List<Map<String, Object>> rows)
		{
			super(tableId, UpdateRowsMessageType.TYPE_ID);
			this.rows = rows;
		}

		public List<Map<String, Object>> getRows()
		{
			return rows;
		}
	}

	static class UpdateRowMessage extends TableMessage
	{
		private final String key;

		private final Object data;

		UpdateRowMessage(String tableId, String key, Object data)
		{
			super(tableId, UpdateRowMessageType.TYPE_ID);
			this.key = key;
			this.data = data;
		}

		public String getKey()
		{
			return key;
		}

		public Object getData()
		{
			return data;
		}
	}

	private class DataBehavior extends AbstractAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void onRequest()
		{
			String json = currentPageJson();
			getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(
				new TextRequestHandler("application/json", "UTF-8", json));
		}
	}
}
