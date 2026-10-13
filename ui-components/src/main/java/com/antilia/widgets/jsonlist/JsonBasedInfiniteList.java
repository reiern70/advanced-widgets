/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.jsonlist;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxUtils;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes.Method;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.veil.LocalVeilBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;

import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.dynamicdatatable.BuiltInTemplateEngine;
import com.antilia.widgets.dynamicdatatable.IDynamicDataProvider;
import com.antilia.widgets.dynamicdatatable.IJsonSerializer;
import com.antilia.widgets.dynamicdatatable.ITemplateEngine;
import com.antilia.widgets.dynamicdatatable.IWebSocketLightWeightMessage;
import com.antilia.widgets.dynamicdatatable.IWebSocketMessageType;
import com.antilia.widgets.dynamicdatatable.JacksonJsonSerializer;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.RefreshMessageType;
import com.antilia.widgets.dynamicdatatable.ResourceTemplate;
import com.antilia.widgets.dynamicdatatable.UpdateRowMessageType;
import com.antilia.widgets.license.Licensing;
import com.antilia.widgets.scrollbar.ScrollbarBehavior;
import com.antilia.widgets.template.Templates;
import com.github.openjson.JSONObject;
import com.antilia.widgets.theme.LookPart;
import com.antilia.widgets.theme.Looks;

/**
 * A list that scrolls infinitely, whose items are rendered in the browser from JSON, the way the
 * rows of a {@link JsonBasedDynamicDataTable} are.
 * <p>
 * The list is a single component without children: Wicket renders only its {@code <div>}, empty.
 * The items come as JSON, a page of {@link #getItemsPerPage() items per page} at a time, from the
 * list's own endpoint: the first page when the list is shown, the next one each time the user
 * scrolls to the last item, while there are more. Each item is the {@link #setItemTemplate(String)
 * item template} evaluated against the item's JSON by the {@link #setTemplateEngine template
 * engine}; templates refer to the item's position as {@code {{@number}}} and to its key as
 * {@code {{@key}}}. The items load as the user scrolls the list, if it has a
 * {@link #setHeight(int, BodyHeightUnits) height}, or else the page.
 * <p>
 * The {@link IDynamicDataProvider provider} knows the key of every item, which ties an item in
 * the browser to one on the server. Links and buttons in the items are plain markup, not
 * components: a click on an element with a {@code data-dt-action} attribute sends the action and
 * the item's key to the list, which looks the item up through
 * {@link IDynamicDataProvider#findByKey(Serializable)} and hands it to the {@link IItemAction}
 * registered for the action with {@link #addAction(IItemAction)}. A click on an item that has
 * changed or gone since it was rendered therefore still reaches the list, rather than a component
 * that no longer exists, and ends up in
 * {@link #onNotFoundAction(AjaxRequestTarget, String, Serializable, Object)}. Whatever the action
 * shows, such as a form in a dialog, lives elsewhere on the page.
 * <p>
 * The application can have the browser fetch the items again, with
 * {@link #refresh(IPartialPageRequestHandler)}, repaint one item with
 * {@link #updateItem(IPartialPageRequestHandler, Object)}, or start over from the first page with
 * {@link #reset()}. It can also push messages over Wicket's web socket connection, which the list
 * picks up once it {@link #setPushEnabled(boolean) accepts pushes}: a
 * {@link #newRefreshMessage() refresh} and an {@link #newUpdateItemMessage(Object) update of an
 * item}, both of the same types as those of a {@link JsonBasedDynamicDataTable}, or messages of a
 * type of its own, {@link #addMessageType(IWebSocketMessageType) registered} with the list.
 * <p>
 * The list is a feed, as screen readers know it: its items are articles, telling their position
 * and the number of items, which Page Down and Page Up move the focus between; Enter on an item
 * carrying an action sends it. Below the items the list says that it is loading more, that it
 * shows all of them, or that there are none, in texts of the bundle of ui-components:
 * {@code JsonBasedInfiniteList.loading}, {@code JsonBasedInfiniteList.end}, where {@code ${count}}
 * stands for the number of items, and {@code JsonBasedInfiniteList.empty}.
 * <p>
 * The list carries the CSS class {@value #CSS_CLASS}, lays its items out in a column and takes its
 * colors from the {@link com.antilia.widgets.theme.Theme theme} it is rendered in, if any; a grid
 * of cards, for example, is a matter of CSS.
 * <p>
 * The item template is written into the page as markup and has to be authored by the developer.
 * Every property the {@link #setJsonSerializer serializer} produces for an item is sent to the
 * browser.
 * <p>
 * A private component: it renders only when the {@link Licensing license key} of the application
 * activates it, else a placeholder telling so.
 *
 * @param <T>
 *            the type of the items
 * @param <K>
 *            the type of the keys of the items
 * @since 11.0.0
 */
public class JsonBasedInfiniteList<T, K extends Serializable> extends WebComponent
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the list. */
	public static final String CSS_CLASS = "json-infinite-list";

	private static final ResourceReference JAVASCRIPT = new JavaScriptResourceReference(
		JsonBasedInfiniteList.class, "json-infinite-list.js");

	private static final ResourceReference CSS = new CssResourceReference(
		JsonBasedInfiniteList.class, "json-infinite-list.css");

	/** The parameter asking the data endpoint for the next page of items. */
	private static final String MORE_PARAMETER = "more";

	private static final String ACTION_PARAMETER = "action";

	private static final String KEY_PARAMETER = "key";

	/** Has the list fetch its items again. */
	private static final IWebSocketMessageType REFRESH_TYPE = new MessageType(
		RefreshMessageType.TYPE_ID,
		"function (message) { Wicket.JsonInfiniteList.instances[message.tableId].load(); }");

	/** Repaints one item of the list. */
	private static final IWebSocketMessageType UPDATE_ITEM_TYPE = new MessageType(
		UpdateRowMessageType.TYPE_ID,
		"function (message) { Wicket.JsonInfiniteList.instances[message.tableId]" +
			".renderItem(message.key, message.data); }");

	private final IDynamicDataProvider<T, K> provider;

	private final Map<String, IItemAction<T>> actions = new LinkedHashMap<>();

	private final Map<String, IWebSocketMessageType> messageTypes = new LinkedHashMap<>();

	private final ActionBehavior actionBehavior;

	private final DataBehavior dataBehavior;

	private long itemsPerPage;

	/** The number of items the browser shows, at most; -1 for the first page. */
	private long shownLimit = -1;

	private boolean scrollToTop;

	private String itemTemplate;

	private ResourceTemplate resourceTemplate;

	private ITemplateEngine templateEngine = BuiltInTemplateEngine.INSTANCE;

	private IJsonSerializer jsonSerializer = JacksonJsonSerializer.INSTANCE;

	private Duration pollInterval;

	private boolean pushEnabled;

	private int height = -1;

	private BodyHeightUnits heightUnits = BodyHeightUnits.PIXELS;

	private transient Long itemCount;

	private transient List<T> shownItems;

	private transient Map<String, String> templates;

	/**
	 * @param id
	 *            component id
	 * @param provider
	 *            supplies the items and resolves them back from their keys
	 * @param itemsPerPage
	 *            the number of items fetched at a time
	 */
	public JsonBasedInfiniteList(String id, IDynamicDataProvider<T, K> provider,
		long itemsPerPage)
	{
		super(id);
		this.provider = Args.notNull(provider, "provider");
		setItemsPerPage(itemsPerPage);
		setOutputMarkupId(true);

		addMessageType(REFRESH_TYPE);
		addMessageType(UPDATE_ITEM_TYPE);

		add(actionBehavior = new ActionBehavior());
		add(dataBehavior = new DataBehavior());
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		Behavior veil = newVeilBehavior();
		if (veil != null)
		{
			add(veil);
		}
	}

	/**
	 * Creates the behavior blocking the list while one of its Ajax requests runs, for example
	 * while an action is carried out: a {@link LocalVeilBehavior} by default, which shows a
	 * spinner over the list when a request takes a while. Loading more items does not block it.
	 *
	 * @return the behavior, or {@code null} to leave the list usable during its requests
	 */
	protected Behavior newVeilBehavior()
	{
		return new LocalVeilBehavior();
	}

	/**
	 * Registers the action handling the clicks on the elements of the items whose
	 * {@code data-dt-action} attribute is the action's id.
	 *
	 * @param action
	 *            the action
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             if another action is registered with the same id
	 */
	public JsonBasedInfiniteList<T, K> addAction(IItemAction<T> action)
	{
		Args.notNull(action, "action");
		String actionId = Args.notEmpty(action.getActionId(), "actionId");
		IItemAction<T> registered = actions.putIfAbsent(actionId, action);
		if (registered != null && registered != action)
		{
			throw new IllegalArgumentException(
				"Another action is already registered as '" + actionId + "'");
		}
		return this;
	}

	/**
	 * Registers the browser-side function handling the messages of a type pushed to this list.
	 * The function finds the list as {@code Wicket.JsonInfiniteList.instances[message.tableId]}.
	 * Refreshing the list, of the type {@link RefreshMessageType#TYPE_ID}, and repainting an item,
	 * of the type {@link UpdateRowMessageType#TYPE_ID}, are registered already.
	 *
	 * @param messageType
	 *            the message type
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             if another message type is registered with the same id
	 */
	public JsonBasedInfiniteList<T, K> addMessageType(IWebSocketMessageType messageType)
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

	/**
	 * Dispatches a click on an element with a {@code data-dt-action} attribute to the
	 * {@link IItemAction} registered for the action, or to
	 * {@link #onNotFoundAction(AjaxRequestTarget, String, Serializable, Object)} if there is none
	 * or the item no longer exists.
	 */
	private void onAction(AjaxRequestTarget target, String action, K key, T item)
	{
		IItemAction<T> handler = actions.get(action);
		if (handler == null || item == null)
		{
			onNotFoundAction(target, action, key, item);
		}
		else
		{
			handler.onAction(item, target);
		}
	}

	/**
	 * Called when a click cannot be dispatched: the item no longer exists, for example because it
	 * was deleted after it was rendered, or no action is registered for it. Does nothing by
	 * default.
	 * <p>
	 * Both the action and the key come from the browser.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param action
	 *            the value of the clicked element's {@code data-dt-action} attribute
	 * @param key
	 *            the key the item was looked up by, or {@code null} if the element is not in an
	 *            item or the browser sent no valid key
	 * @param item
	 *            the item, or {@code null} if it no longer exists
	 */
	protected void onNotFoundAction(AjaxRequestTarget target, String action, K key, T item)
	{
	}

	/**
	 * Queries the items the browser shows again, as many as it shows, and has the browser repaint
	 * them all, for example after one of them was added or deleted.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 */
	public void refresh(IPartialPageRequestHandler target)
	{
		forgetItems();
		target.appendJavaScript("if (" + getVarName() + ") { " + getVarName() + ".update(" +
			escapeForScript(shownItemsJson()) + "); }");
	}

	/**
	 * Has the browser repaint one item, if it shows it, from its current state, leaving the other
	 * items alone, for example after it was edited.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 * @param item
	 *            the item in its current state
	 */
	public void updateItem(IPartialPageRequestHandler target, T item)
	{
		Args.notNull(item, "item");
		target.appendJavaScript("if (" + getVarName() + ") { " + getVarName() + ".renderItem(" +
			JSONObject.quote(getItemKey(item)) + ", " + escapeForScript(jsonSerializer.toJson(item)) +
			"); }");
	}

	/**
	 * Starts over from the first page of items, scrolled to the top, for example after the items
	 * were filtered or sorted otherwise. {@link #refresh(IPartialPageRequestHandler) Refresh} the
	 * list, or render it again, for the browser to show it.
	 *
	 * @return {@code this}
	 */
	public JsonBasedInfiniteList<T, K> reset()
	{
		shownLimit = -1;
		scrollToTop = true;
		forgetItems();
		return this;
	}

	/**
	 * Makes the browser fetch the items it shows again by pushing a
	 * {@link #newRefreshMessage() refresh message} over the page's web socket connection. The list
	 * has to {@link #setPushEnabled(boolean) accept pushes}.
	 *
	 * @return {@code false} if the page has no open web socket connection, {@code true} if the
	 *         message was sent
	 * @throws IOException
	 *             if the message cannot be sent
	 */
	public boolean refreshViaWebSockets() throws IOException
	{
		return newRefreshMessage().send(this);
	}

	/**
	 * Builds a message making this list fetch the items it shows again.
	 *
	 * @return the message
	 */
	public IWebSocketLightWeightMessage newRefreshMessage()
	{
		return new RefreshMessage(getMarkupId());
	}

	/**
	 * Builds a message repainting one item of this list, if the browser shows it, from the given
	 * item. {@link UpdateRowMessageType#newMessage(String, String, Object)} builds the same message
	 * without the list, for example on a background thread, from the {@link #getMarkupId() markup
	 * id} of the list.
	 *
	 * @param item
	 *            the item in its current state
	 * @return the message
	 */
	public IWebSocketLightWeightMessage newUpdateItemMessage(T item)
	{
		return UpdateRowMessageType.newMessage(getMarkupId(), getItemKey(item), item);
	}

	/**
	 * Called after items were sent to the browser, which shows them from now on, for example to
	 * push updates only for these items. Does nothing by default.
	 *
	 * @param keys
	 *            the keys of all the items the browser shows, in display order
	 */
	protected void onItemsSent(List<K> keys)
	{
	}

	/**
	 * @param item
	 *            an item
	 * @return the key of the item as the browser shows it, {@link IDynamicDataProvider#formatKey
	 *         formatted} by the provider
	 */
	public String getItemKey(T item)
	{
		return provider.formatKey(provider.keyOf(item));
	}

	/**
	 * Resolves an item from a key as the browser shows it, for example one sent with a request.
	 *
	 * @param key
	 *            the key as the browser shows it, possibly not a key at all
	 * @return the item, or {@code null} if there is none for the key
	 */
	public T findItem(String key)
	{
		K parsed = provider.parseKey(key);
		return parsed != null ? provider.findByKey(parsed) : null;
	}

	/**
	 * @return the provider of the items
	 */
	public IDynamicDataProvider<T, K> getProvider()
	{
		return provider;
	}

	/**
	 * @return the number of items of the provider, queried once per request
	 */
	public long getItemCount()
	{
		if (itemCount == null)
		{
			itemCount = provider.size();
		}
		return itemCount;
	}

	/**
	 * @return the number of items the browser shows: the first page of them, and every item
	 *         loaded since
	 */
	public long getShownItemCount()
	{
		return Math.max(0, Math.min(shownLimit < 0 ? itemsPerPage : shownLimit, getItemCount()));
	}

	/**
	 * @return the number of items fetched at a time
	 */
	public long getItemsPerPage()
	{
		return itemsPerPage;
	}

	/**
	 * @param itemsPerPage
	 *            the number of items fetched at a time, positive
	 * @return {@code this}
	 */
	public JsonBasedInfiniteList<T, K> setItemsPerPage(long itemsPerPage)
	{
		this.itemsPerPage = Args.withinRange(1L, Long.MAX_VALUE, itemsPerPage, "itemsPerPage");
		shownItems = null;
		return this;
	}

	/**
	 * Sets the template of the items, trusted markup written into the page as is. Without one,
	 * the template is read from an {@code .html} file named after the class of the list, or one of
	 * its superclasses, as the markup of a panel is.
	 *
	 * @param template
	 *            the template, evaluated by the {@link #setTemplateEngine(ITemplateEngine) template
	 *            engine} against the JSON of each item
	 * @return {@code this}
	 */
	public JsonBasedInfiniteList<T, K> setItemTemplate(String template)
	{
		itemTemplate = Args.notEmpty(template, "template");
		resourceTemplate = null;
		return this;
	}

	/**
	 * Reads the template of the items from {@code name.html} in the package of {@code scope}, for
	 * the style, variation and locale of the list, as the markup of a panel is: a German locale
	 * picks {@code name_de.html}. Like in a panel, the template is the content of the file's
	 * {@code <wicket:panel>}, if it has one.
	 *
	 * @param scope
	 *            the class whose package holds the template
	 * @param name
	 *            the name of the template, without the extension
	 * @return {@code this}
	 */
	public JsonBasedInfiniteList<T, K> setItemTemplate(Class<?> scope, String name)
	{
		resourceTemplate = ResourceTemplate.named(scope, name);
		itemTemplate = null;
		return this;
	}

	/**
	 * @return the template of the items as rendered in this list, for its style, variation and
	 *         locale
	 */
	public String getItemTemplate()
	{
		if (itemTemplate != null)
		{
			return itemTemplate;
		}
		ResourceTemplate template = resourceTemplate != null ? resourceTemplate
			: ResourceTemplate.forClass(getClass(), JsonBasedInfiniteList.class);
		String style = getStyle();
		String variation = getVariation();
		Locale locale = getLocale();
		if (getApplication().usesDevelopmentConfig())
		{
			return template.load(style, variation, locale);
		}
		if (templates == null)
		{
			templates = new HashMap<>();
		}
		return templates.computeIfAbsent(template.getCacheKey(style, variation, locale),
			key -> template.load(style, variation, locale));
	}

	/**
	 * @param templateEngine
	 *            the engine evaluating the item template in the browser
	 * @return {@code this}
	 */
	public JsonBasedInfiniteList<T, K> setTemplateEngine(ITemplateEngine templateEngine)
	{
		this.templateEngine = Args.notNull(templateEngine, "templateEngine");
		return this;
	}

	/**
	 * @return the serializer turning items into JSON
	 */
	public IJsonSerializer getJsonSerializer()
	{
		return jsonSerializer;
	}

	/**
	 * @param jsonSerializer
	 *            the serializer turning items into JSON
	 * @return {@code this}
	 */
	public JsonBasedInfiniteList<T, K> setJsonSerializer(IJsonSerializer jsonSerializer)
	{
		this.jsonSerializer = Args.notNull(jsonSerializer, "jsonSerializer");
		return this;
	}

	/**
	 * @param pollInterval
	 *            how often the browser fetches the items it shows again, or {@code null} not to
	 *            poll
	 * @return {@code this}
	 */
	public JsonBasedInfiniteList<T, K> setPollInterval(Duration pollInterval)
	{
		this.pollInterval = pollInterval;
		return this;
	}

	/**
	 * @param pushEnabled
	 *            whether the list picks up the {@link IWebSocketLightWeightMessage}s addressed to
	 *            it that arrive over Wicket's web socket connection
	 * @return {@code this}
	 */
	public JsonBasedInfiniteList<T, K> setPushEnabled(boolean pushEnabled)
	{
		this.pushEnabled = pushEnabled;
		return this;
	}

	/**
	 * @return the height of the list in {@link #getHeightUnits()}, or a negative number,
	 *         {@code -1} by default, when the list is as high as its items
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * @return the unit of {@link #getHeight()}, {@link BodyHeightUnits#PIXELS} by default
	 */
	public BodyHeightUnits getHeightUnits()
	{
		return heightUnits;
	}

	/**
	 * Gives the list a fixed height, within which it scrolls, with a slim scroll bar in the
	 * colors of the theme, drawn by a {@link ScrollbarBehavior} script. Without a height, the list
	 * is as high as its items and grows as the user scrolls the page.
	 *
	 * @param height
	 *            the height of the list in the given units; any negative number, such as
	 *            {@code -1}, to let the list take the height of its items, as by default
	 * @param units
	 *            the units of the height
	 * @return {@code this}
	 */
	public JsonBasedInfiniteList<T, K> setHeight(int height, BodyHeightUnits units)
	{
		Args.isTrue(height != 0, "the height has to be positive, or negative for none");
		heightUnits = Args.notNull(units, "units");
		this.height = height;
		return this;
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

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		checkComponentTag(tag, "div");
		super.onComponentTag(tag);
		Looks.addClasses(tag, Looks.of(this), LookPart.LIST);
		tag.put("role", "feed");
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		CoreLibrariesContributor.contributeAjax(getApplication(), response);
		if (height > 0)
		{
			ScrollbarBehavior.renderHeadItems(response);
		}
		response.render(JavaScriptHeaderItem.forReference(Templates.JS));
		response.render(JavaScriptHeaderItem.forReference(JAVASCRIPT));
		response.render(CssHeaderItem.forReference(CSS));
		templateEngine.renderHead(response);

		boolean partial = getRequestCycle().find(IPartialPageRequestHandler.class).isPresent();
		response.render(OnDomReadyHeaderItem.forScript("new Wicket.JsonInfiniteList(" +
			buildConfig() + ", " +
			actionBehavior.getCallbackFunction(CallbackParameter.explicit(ACTION_PARAMETER),
				CallbackParameter.explicit(KEY_PARAMETER)) +
			", " + buildMessageHandlers() +
			(partial ? ", " + escapeForScript(shownItemsJson()) : "") + ");"));
	}

	private JSONObject buildConfig()
	{
		JSONObject config = new JSONObject();
		config.put("id", getMarkupId());
		config.put("dataUrl", dataBehavior.getCallbackUrl().toString());
		config.put("engine", templateEngine.getName());
		config.put("template", getItemTemplate());
		config.put("itemCount", getItemCount());
		config.put("push", pushEnabled);
		config.put("locale", getLocale().toLanguageTag());
		if (pollInterval != null)
		{
			config.put("pollMillis", pollInterval.toMillis());
		}
		if (height > 0)
		{
			JSONObject heightConfig = new JSONObject();
			heightConfig.put("value", height);
			heightConfig.put("unit", heightUnits.getCssUnit());
			config.put("height", heightConfig);
		}
		JSONObject texts = new JSONObject();
		texts.put("loading", getString("JsonBasedInfiniteList.loading", null, "Loading…"));
		texts.put("end", getString("JsonBasedInfiniteList.end", null, "All ${count} shown"));
		texts.put("empty", getString("JsonBasedInfiniteList.empty", null, "Nothing to show"));
		config.put("texts", texts);
		return config;
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

	/**
	 * @return the JSON with {@code <}, {@code >} and {@code &} escaped, so it cannot end the script
	 *         or the section of the Ajax response it is written into
	 */
	private static String escapeForScript(String json)
	{
		return json.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
	}

	/**
	 * @return all the items the browser shows, as it reads them, and calls
	 *         {@link #onItemsSent(List)}
	 */
	private String shownItemsJson()
	{
		List<T> items = getShownItems();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("items", toEntries(items, 1));
		data.put("itemCount", getItemCount());
		if (scrollToTop)
		{
			scrollToTop = false;
			data.put("top", true);
		}
		String json = jsonSerializer.toJson(data);
		onItemsSent(keysOf(items));
		return json;
	}

	/**
	 * Adds the next page of items to those the browser shows, if there is one, and calls
	 * {@link #onItemsSent(List)} with all of them.
	 *
	 * @return the added items as the browser reads them, with the number of items
	 */
	private String moreItemsJson()
	{
		long shown = getShownItemCount();
		List<T> items = List.of();
		if (shown < getItemCount())
		{
			items = items(shown, Math.min(itemsPerPage, getItemCount() - shown));
			shownLimit = shown + items.size();
			shownItems = null;
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("items", toEntries(items, shown + 1));
		data.put("itemCount", getItemCount());
		String json = jsonSerializer.toJson(data);
		if (items.isEmpty() == false)
		{
			onItemsSent(keysOf(getShownItems()));
		}
		return json;
	}

	/**
	 * @return the items of every page loaded so far, queried once per request
	 */
	private List<T> getShownItems()
	{
		if (shownItems == null)
		{
			long count = getShownItemCount();
			shownItems = count > 0 ? items(0, count) : List.of();
		}
		return shownItems;
	}

	private List<T> items(long first, long count)
	{
		List<T> items = new ArrayList<>();
		provider.iterator(first, count).forEachRemaining(items::add);
		return items;
	}

	private List<K> keysOf(List<T> items)
	{
		List<K> keys = new ArrayList<>(items.size());
		for (T item : items)
		{
			keys.add(provider.keyOf(item));
		}
		return keys;
	}

	private List<Map<String, Object>> toEntries(List<T> items, long firstNumber)
	{
		List<Map<String, Object>> entries = new ArrayList<>(items.size());
		long number = firstNumber;
		for (T item : items)
		{
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("key", getItemKey(item));
			entry.put("number", number++);
			entry.put("data", item);
			entries.add(entry);
		}
		return entries;
	}

	/**
	 * Forgets the items and their count, so they are queried again.
	 */
	private void forgetItems()
	{
		itemCount = null;
		shownItems = null;
	}

	/**
	 * @return the expression evaluating to the list's client-side instance
	 */
	private String getVarName()
	{
		return "Wicket.JsonInfiniteList.instances[" + JSONObject.quote(getMarkupId()) + "]";
	}

	@Override
	protected void onRemove()
	{
		super.onRemove();
		AjaxUtils.executeIfAjaxOrWebSockets(target -> target.appendJavaScript(
			"if (" + getVarName() + ") { " + getVarName() + ".destroy(); }"));
	}

	@Override
	protected void onDetach()
	{
		forgetItems();
		provider.detach();
		for (IItemAction<T> action : actions.values())
		{
			action.detach();
		}
		super.onDetach();
	}

	private static final class MessageType implements IWebSocketMessageType
	{
		private static final long serialVersionUID = 1L;

		private final String typeId;

		private final String typeFunction;

		MessageType(String typeId, String typeFunction)
		{
			this.typeId = typeId;
			this.typeFunction = typeFunction;
		}

		@Override
		public String getTypeId()
		{
			return typeId;
		}

		@Override
		public String getTypeFunction()
		{
			return typeFunction;
		}
	}

	/**
	 * A message making the list fetch its items again, sent as {@code tableId} and
	 * {@code typeId}.
	 */
	private static final class RefreshMessage implements IWebSocketLightWeightMessage
	{
		private final String tableId;

		private final String typeId = RefreshMessageType.TYPE_ID;

		RefreshMessage(String tableId)
		{
			this.tableId = tableId;
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

	private class DataBehavior extends AbstractAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void onRequest()
		{
			boolean more = getComponent().getRequest().getRequestParameters()
				.getParameterValue(MORE_PARAMETER).toBoolean(false);
			String json = more ? moreItemsJson() : shownItemsJson();
			getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(
				new TextRequestHandler("application/json", "UTF-8", json));
		}
	}

	private class ActionBehavior extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			attributes.setMethod(Method.POST);
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			IRequestParameters parameters = getComponent().getRequest().getRequestParameters();
			String action = parameters.getParameterValue(ACTION_PARAMETER).toOptionalString();
			if (action == null)
			{
				return;
			}
			String key = parameters.getParameterValue(KEY_PARAMETER).toOptionalString();
			K parsedKey = provider.parseKey(key);
			T item = parsedKey != null ? provider.findByKey(parsedKey) : null;
			onAction(target, action, parsedKey, item);
		}
	}
}
