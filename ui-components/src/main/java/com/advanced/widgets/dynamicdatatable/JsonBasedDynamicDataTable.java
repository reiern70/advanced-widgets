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
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.lang.Args;

import com.advanced.widgets.license.Licensing;
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
 * By default the endpoint is the page itself, an Ajax behavior of the table
 * ({@link RowsSource#PAGE}). With {@link #setRowsSource(RowsSource) RowsSource.RESOURCE} the
 * browser fetches the rows from a {@link JsonRowsResource} the application mounted and gave the
 * table with {@link #setRowsResource(ResourceReference)}: a stateless request that does not touch
 * the page, with the paging, the sort and the parameters of
 * {@link #addRowsParameters(PageParameters)} in its URL. Paging, sorting and selecting still go
 * through the page; scrolling infinitely loads the further rows from the page.
 * <p>
 * With an {@link ISortableDynamicDataProvider}, the header of every {@link ISortableDynamicColumn}
 * sorts the rows. A {@link SortableDynamicColumn} is hideable and movable if the column it wraps
 * is.
 * <p>
 * The column templates are written into the page as markup and have to be authored by the
 * developer; see {@link IDynamicColumn}. Every property the {@link #setJsonSerializer serializer}
 * produces for a row is sent to the browser.
 * <p>
 * A private component: it renders only when the {@link Licensing license key} of the application
 * activates it, else a placeholder telling so.
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

	/** The parameter asking the data endpoint for the next page of rows, scrolling infinitely. */
	private static final String MORE_PARAMETER = "more";

	/**
	 * Where the browser fetches the rows of the table from.
	 */
	public enum RowsSource
	{
		/** An Ajax behavior of the table, on the page: the default. */
		PAGE,

		/** A mounted {@link JsonRowsResource}, see {@link #setRowsResource(ResourceReference)}. */
		RESOURCE
	}

	private final Map<String, IWebSocketMessageType> messageTypes = new LinkedHashMap<>();

	private RowsSource rowsSource = RowsSource.PAGE;

	private ResourceReference rowsResource;

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

	/**
	 * @return where the browser fetches the rows from, {@link RowsSource#PAGE} by default
	 */
	public RowsSource getRowsSource()
	{
		return rowsSource;
	}

	/**
	 * @param rowsSource
	 *            where the browser fetches the rows from; {@link RowsSource#RESOURCE} needs
	 *            {@link #setRowsResource(ResourceReference) a resource}
	 * @return {@code this}
	 */
	public JsonBasedDynamicDataTable<T, K> setRowsSource(RowsSource rowsSource)
	{
		this.rowsSource = Args.notNull(rowsSource, "rowsSource");
		return this;
	}

	/**
	 * @return the mounted {@link JsonRowsResource} serving the rows with
	 *         {@link RowsSource#RESOURCE}, {@code null} if none was given
	 */
	public ResourceReference getRowsResource()
	{
		return rowsResource;
	}

	/**
	 * @param rowsResource
	 *            the mounted {@link JsonRowsResource} serving the rows with
	 *            {@link RowsSource#RESOURCE}
	 * @return {@code this}
	 */
	public JsonBasedDynamicDataTable<T, K> setRowsResource(ResourceReference rowsResource)
	{
		this.rowsResource = rowsResource;
		return this;
	}

	/**
	 * Adds the parameters the {@link JsonRowsResource} needs besides the paging and the sort, for
	 * example a filter, to the URL the browser fetches the rows from with
	 * {@link RowsSource#RESOURCE}. Adds nothing by default.
	 *
	 * @param parameters
	 *            the parameters of the URL
	 */
	protected void addRowsParameters(PageParameters parameters)
	{
	}

	/**
	 * @return whether the browser fetches the rows from the {@link #getRowsResource() resource}
	 */
	private boolean isFromResource()
	{
		if (rowsSource != RowsSource.RESOURCE)
		{
			return false;
		}
		if (rowsResource == null)
		{
			throw new IllegalStateException(
				"A table whose rows come from a resource needs setRowsResource(...): " + this);
		}
		return true;
	}

	/**
	 * @return the URL of the {@link #getRowsResource() resource} serving the rows of the current
	 *         page, sorted as the table is
	 */
	private String rowsResourceUrl()
	{
		PageParameters parameters = new PageParameters();
		parameters.set(JsonRowsResource.FIRST, getCurrentPage() * getItemsPerPage());
		parameters.set(JsonRowsResource.COUNT, getItemsPerPage());
		ISortState<Object> state = getSortState();
		if (state instanceof SingleSortState<Object> single && single.getSort() != null)
		{
			parameters.set(JsonRowsResource.SORT, single.getSort().getProperty());
			parameters.set(JsonRowsResource.ASCENDING, single.getSort().isAscending());
		}
		addRowsParameters(parameters);
		Url url = getRequestCycle().mapUrlFor(rowsResource, parameters);
		// a full URL, as the page and an Ajax response would resolve a relative one differently
		return getRequestCycle().getUrlRenderer().renderFullUrl(url);
	}

	/**
	 * @return the selected rows of the current page, which a {@link JsonRowsResource} does not
	 *         know, by their formatted keys
	 */
	private JSONArray selectedKeysOfPage()
	{
		JSONArray keys = new JSONArray();
		for (K key : getCurrentPageKeys())
		{
			if (isSelected(key))
			{
				keys.put(getProvider().formatKey(key));
			}
		}
		return keys;
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
		if (isFromResource())
		{
			target.appendJavaScript("if (" + getVarName() + ") { " + getVarName() + ".reload(" +
				resourceState() + "); }");
			onRowsSent(getCurrentPageKeys());
			return;
		}
		target.appendJavaScript("if (" + getVarName() + ") { " + getVarName() + ".update(" +
			currentPageJson() + "); }");
	}

	/**
	 * @return what the browser needs to fetch the rows of the current page from the resource:
	 *         its URL, the selected rows and whether to scroll back to the first row
	 */
	private JSONObject resourceState()
	{
		JSONObject state = new JSONObject();
		state.put("dataUrl", rowsResourceUrl());
		state.put("selectedKeys", selectedKeysOfPage());
		if (takeScrollToTop())
		{
			state.put("top", true);
		}
		return state;
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
		if (takeScrollToTop())
		{
			data.put("top", true);
		}
		String json = jsonSerializer.toJson(data);
		onRowsSent(getCurrentPageKeys());
		return json;
	}

	/**
	 * Scrolling infinitely, adds the next page of rows to those shown and calls
	 * {@link #onRowsSent(List)} with all of them.
	 *
	 * @return the added rows as the browser reads them, with the number of rows
	 */
	private String moreRowsJson()
	{
		long first = getShownRowCount() + 1;
		List<T> rows = loadMoreRows();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("rows", toRowEntries(rows, first));
		data.put("itemCount", getItemCount());
		data.put("currentPage", getCurrentPage());
		String json = jsonSerializer.toJson(data);
		if (rows.isEmpty() == false)
		{
			onRowsSent(getCurrentPageKeys());
		}
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
	 * Makes the table scroll infinitely: instead of a page at a time, it shows its rows in one list
	 * that grows by the next page of rows, {@link #getItemsPerPage() rows per page} of them, each
	 * time the user scrolls to its end, while there are more. The table blocks itself with its
	 * {@link #newVeilBehavior() veil} while it loads them. The paging links and the choice of the
	 * rows per page are left out of the navigation; the label tells how many of the rows are shown.
	 * Sorting, or filtering by {@link #setCurrentPage(long) setting the current page}, starts over
	 * from the first page of rows, scrolled to the top, while
	 * {@link #refresh(IPartialPageRequestHandler) refreshing} the table fetches all the rows shown
	 * again. Selecting the page selects every row shown.
	 * <p>
	 * The rows load as the user scrolls the {@link #setBodyHeight(int, BodyHeightUnits) body}, or,
	 * if it has no height, the page, to the last row. The table shows its first page of rows again
	 * after the setting changes; render it again for the change to show.
	 *
	 * @param infiniteScroll
	 *            whether the table scrolls infinitely, {@code false} by default
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public JsonBasedDynamicDataTable<T, K> setInfiniteScroll(boolean infiniteScroll)
	{
		setInfiniteScrollEnabled(infiniteScroll);
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
		if (isFromResource())
		{
			config.put("dataUrl", rowsResourceUrl());
			config.put("selectedKeys", selectedKeysOfPage());
			// scrolling infinitely, the further rows come from the page, which counts them
			config.put("moreUrl", dataBehavior.getCallbackUrl().toString());
			onRowsSent(getCurrentPageKeys());
		}
		else
		{
			config.put("dataUrl", dataBehavior.getCallbackUrl().toString());
		}
		config.put("engine", templateEngine.getName());
		config.put("columns", columnConfigs);
		config.put("push", pushEnabled);
		config.put("infinite", isInfiniteScroll());
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
		boolean partial = getRequestCycle().find(IPartialPageRequestHandler.class).isPresent() &&
			!isFromResource();
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
			boolean more = getComponent().getRequest().getRequestParameters()
				.getParameterValue(MORE_PARAMETER).toBoolean(false);
			String json = more ? moreRowsJson() : currentPageJson();
			getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(
				new TextRequestHandler("application/json", "UTF-8", json));
		}
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		Licensing.configurePrivate(this);
	}

	@Override
	protected void renderPlaceholderTag(ComponentTag tag, Response response)
	{
		if (!Licensing.renderPlaceholder(this, tag, response))
		{
			super.renderPlaceholderTag(tag, response);
		}
	}
}
