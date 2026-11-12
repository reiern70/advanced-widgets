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

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.json.JSONFunction;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.IComponentAwareHeaderContributor;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.ScriptJson;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

import com.antiliacode.widgets.resource.AlreadyCompressedJavaScriptResourceReference;

/**
 * Listens to the {@link IClientEvent events} of its component in the browser, any number of them,
 * of any names: those the browser fires and those the scripts of the component fire, the events of
 * a rich text editor for example. It keeps them by their names, as many as needed for each name.
 * Each event runs its handler in the browser, and that is all the browser knows of it: an
 * {@link IAjaxRoundTripEvent} comes with the attributes of its requests, those of Wicket's Ajax,
 * which its handler sends by calling {@code send(values)}, a round trip of Wicket's Ajax; any other
 * stays in the browser, its handler doing what it does there. The requests go to one of two Ajax
 * behaviors the behavior adds to the component, whatever the number of events: one for the
 * {@link IAjaxEvent}s and the {@link IAjaxUpdateEvent}s, without the form, one for the
 * {@link IAjaxSubmitEvent}s, submitting the form the component is in. They post, unless an event
 * sets GET: when the component is a field, Wicket sends its value with the request too, as the
 * text a {@link com.antiliacode.widgets.richtext.RichTextEditorChangeEvent} reads.
 *
 * <pre>
 * ClientEventsBehavior events = new ClientEventsBehavior();
 * events.add(AjaxEvent.of("focusin", (values, target) -&gt; target.add(status)));
 * events.add(new ClientEvent("keydown", "function(event) { ... }"));
 * events.add(ClientEvent.handledBy("keyup", "Search.suggest"));
 * component.add(events);
 * </pre>
 *
 * The events of one name each run, in their order, with their own handler and delay, and each
 * going to the server sends its own request, which only it hears of. One event keeps the size of a
 * resized editor in the browser, for example, and another sends it to the server; or a single
 * {@link IAjaxEvent} does both, its handler keeping the size before it calls {@code send}.
 * {@link #add(IClientEvent)} adds an event after those of its name, {@link #set(IClientEvent)}
 * puts it in their place and {@link #removeEvents(String...)} takes them away.
 * <p>
 * The attributes of the requests of each event are those of Wicket's Ajax:
 * {@link #updateAjaxAttributes(AjaxRequestAttributes)} changes those of all the events of the
 * component, then {@link IAjaxRoundTripEvent#updateAjaxAttributes(AjaxRequestAttributes)} those of
 * the event alone, its method, its extra parameters, which a page veil leaving some requests alone
 * sees in their {@code ep}, its dynamic extra parameters and the listeners of its calls, on
 * success and on failure for example.
 * <p>
 * An event telling {@code leaving: true} in its {@code detail}, as the editors' events as the page
 * goes do, goes to the server in a beacon, which the browser sends even then.
 * <p>
 * The events go to the browser as the component renders, in the order they are in, by which the
 * server finds them again: a page changes them before it renders the component, not between. A
 * component keeping events of its own, or of its parts, overrides {@link #getEvents()};
 * {@link #getClientObject()} gives the handlers what the component is in the browser,
 * {@link #getClientValues()} what each request carries besides, which
 * {@link #onEventSent(AjaxRequestTarget, IRequestParameters)} takes first, and
 * {@link #getClientPendingValues()} what the component has not sent yet, which the page's other
 * Ajax requests carry, and {@link #onPendingValuesSent(AjaxRequestTarget, IRequestParameters)}
 * takes: an editor the user scrolled without ever focusing it sends its state so, with the request
 * redrawing its form, without a request of its own.
 * <p>
 * The events listen once the page, or the Ajax response, is ready, in the order of the scripts:
 * Wicket renders the head of a component before the heads of its behaviors. A component whose
 * script fires events as it starts, the text an editor has, renders the behavior's head itself,
 * before its script, by {@link #renderHead(Component, IHeaderResponse)}, so that the events hear of
 * them too: the behavior renders its head once a response.
 *
 * @since 11.0.0
 */
public class ClientEventsBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(ClientEventsBehavior.class);

	/** {@code Wicket.ClientEvents}, which listens to the events in the browser. */
	public static final ResourceReference JS = new AlreadyCompressedJavaScriptResourceReference(
		ClientEventsBehavior.class, "antilia-client-events.js");

	/** The parameter of a request naming the event. */
	public static final String EVENT_PARAMETER = "antilia-event";

	/** The parameter of a request telling the place of the event among those of the component. */
	public static final String INDEX_PARAMETER = "antilia-event-index";

	/** The parameter of a request holding the values the browser sent, as JSON. */
	public static final String VALUES_PARAMETER = "antilia-event-values";

	/**
	 * The name of the event of Wicket's behavior submitting the form, which needs one: the browser
	 * never fires it, the requests going as the events' handlers send them.
	 */
	private static final String SUBMIT_EVENT = "antilia-client-events:submit";

	/**
	 * The prefix of the parameter of the page's Ajax requests holding the values a component has
	 * not sent yet, {@link #getClientPendingValues()}, as JSON: followed by the markup id of the
	 * component.
	 */
	public static final String PENDING_PARAMETER_PREFIX = "antilia-pending:";

	/** What the name of a function of the page is: a global, or a path to one. */
	private static final Pattern FUNCTION_NAME = Pattern.compile(
		"[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)*");

	/** The behaviors that took their pending values in a request: once each. */
	private static final MetaDataKey<Set<ClientEventsBehavior>> PENDING_TAKEN =
		new MetaDataKey<>()
		{
			private static final long serialVersionUID = 1L;
		};

	private final Map<String, List<IClientEvent>> events = new LinkedHashMap<>();

	private AjaxEvents ajaxEvents;

	private SubmitEvents submitEvents;

	/**
	 * @param event
	 *            an event the component listens to, after those it listens to already, of its name
	 *            and of the others
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             when the event goes to the server more than one way, or has no handler
	 */
	public ClientEventsBehavior add(IClientEvent event)
	{
		check(event);
		events.computeIfAbsent(event.getEventName(), name -> new ArrayList<>()).add(event);
		return this;
	}

	/**
	 * @param event
	 *            an event the component listens to in place of those of its name it listens to
	 *            already, if any
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             when the event goes to the server more than one way, or has no handler
	 */
	public ClientEventsBehavior set(IClientEvent event)
	{
		check(event);
		List<IClientEvent> named = new ArrayList<>();
		named.add(event);
		events.put(event.getEventName(), named);
		return this;
	}

	/**
	 * @param event
	 *            an event the component no longer listens to
	 * @return whether the component listened to it
	 */
	public boolean remove(IClientEvent event)
	{
		if (event == null)
		{
			return false;
		}
		List<IClientEvent> named = events.get(event.getEventName());
		if (named == null || !named.remove(event))
		{
			return false;
		}
		if (named.isEmpty())
		{
			events.remove(event.getEventName());
		}
		return true;
	}

	/**
	 * @param eventNames
	 *            the names of events
	 * @return the events of the names the component no longer listens to, in the order of the
	 *         names, an empty list for none
	 */
	public List<IClientEvent> removeEvents(String... eventNames)
	{
		List<IClientEvent> removed = new ArrayList<>();
		for (String eventName : Args.notNull(eventNames, "eventNames"))
		{
			List<IClientEvent> named = events.remove(eventName);
			if (named != null)
			{
				removed.addAll(named);
			}
		}
		return Collections.unmodifiableList(removed);
	}

	/**
	 * @param eventNames
	 *            the names of events
	 * @return the events of the names the component no longer listens to, in the order of the
	 *         names, an empty list for none
	 */
	public List<IClientEvent> removeEvents(IClientEventName... eventNames)
	{
		Args.notNull(eventNames, "eventNames");
		String[] names = new String[eventNames.length];
		for (int index = 0; index < names.length; index++)
		{
			names[index] = AbstractClientEvent.nameOf(eventNames[index]);
		}
		return removeEvents(names);
	}

	/**
	 * @param eventName
	 *            the name of events
	 * @return the events of the name added, in their order, an empty list for none
	 */
	public List<IClientEvent> getEvents(String eventName)
	{
		List<IClientEvent> named = events.get(eventName);
		return named == null ? List.of() : Collections.unmodifiableList(named);
	}

	/**
	 * @param eventName
	 *            the name of events
	 * @return the events of the name added, in their order, an empty list for none
	 */
	public List<IClientEvent> getEvents(IClientEventName eventName)
	{
		return getEvents(AbstractClientEvent.nameOf(eventName));
	}

	/**
	 * @return the events added by their names, in the order the names came, those of each name in
	 *         theirs
	 */
	public Map<String, List<IClientEvent>> getEventsByName()
	{
		Map<String, List<IClientEvent>> byName = new LinkedHashMap<>();
		events.forEach((name, named) -> byName.put(name, Collections.unmodifiableList(named)));
		return Collections.unmodifiableMap(byName);
	}

	/**
	 * @return the events added, in their order: by their names, in the order the names came, those
	 *         of each name in theirs
	 */
	public List<IClientEvent> getAddedEvents()
	{
		List<IClientEvent> all = new ArrayList<>();
		events.values().forEach(all::addAll);
		return Collections.unmodifiableList(all);
	}

	/**
	 * @return the events the component listens to, in the order the server finds them by: those
	 *         added, unless a subclass adds others
	 */
	public List<IClientEvent> getEvents()
	{
		return getAddedEvents();
	}

	/**
	 * The function goes into the page as it is, as code: it is the component's own, never made of
	 * what a user entered.
	 *
	 * @return JavaScript, {@code function(element) {...}}, giving what the handlers of the events
	 *         get as the component from its element; {@code null}, as by default, for the element
	 */
	protected String getClientObject()
	{
		return null;
	}

	/**
	 * The function goes into the page as it is, as code: it is the component's own, never made of
	 * what a user entered. What it gives comes back from the browser: the server checks it before
	 * it trusts it.
	 *
	 * @return JavaScript, {@code function(component) {...}}, giving the parameters, an object of
	 *         strings by their names, the request of each event going to the server carries
	 *         besides its own, the state of an editor for example, which
	 *         {@link #onEventSent(AjaxRequestTarget, IRequestParameters)} takes;
	 *         {@code null}, as by default, for none
	 */
	protected String getClientValues()
	{
		return null;
	}

	/**
	 * Called when the browser sent an event, before the event hears of it and, for an event
	 * submitting the form, before the form takes what the user entered: the parameters hold what
	 * {@link #getClientValues()} gave. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param parameters
	 *            the parameters of the request
	 */
	protected void onEventSent(AjaxRequestTarget target, IRequestParameters parameters)
	{
	}

	/**
	 * The function goes into the page as it is, as code: it is the component's own, never made of
	 * what a user entered. What it gives comes back from the browser: the server checks it before
	 * it trusts it.
	 * <p>
	 * Each Ajax request of the page but those of the component's own events, as it goes, asks the
	 * function, and carries what it gives, which the server gives
	 * {@link #onPendingValuesSent(AjaxRequestTarget, IRequestParameters)} before the components
	 * of the request render: an editor the user scrolled without focusing it, and so without
	 * sending its state, sends it so with the request redrawing its form. The function gives only
	 * what it did not give before, and takes it as sent: the request redrawing the form needs it,
	 * the next ones not.
	 *
	 * @return JavaScript, {@code function(component) {...}}, giving the parameters, an object of
	 *         strings by their names, the component has not sent yet; {@code null}, an empty
	 *         object or nothing for none. {@code null}, as by default, for a component never
	 *         sending values so
	 */
	protected String getClientPendingValues()
	{
		return null;
	}

	/**
	 * Called when another Ajax request of the page carried what
	 * {@link #getClientPendingValues()} gave, once the request was handled, before the components
	 * it updates render. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param values
	 *            the values the component had not sent, as {@link #getClientPendingValues()} gave
	 *            them, by their names
	 */
	protected void onPendingValuesSent(AjaxRequestTarget target, IRequestParameters values)
	{
	}

	/**
	 * Changes the attributes of the requests of all the events of the component going to the
	 * server, those via Ajax and those submitting the form: the channel, the method, the
	 * {@link AjaxRequestAttributes#getExtraParameters() extra parameters} a page veil leaves the
	 * requests alone by, or the {@link AjaxRequestAttributes#getAjaxCallListeners() listeners},
	 * for example. Each event changes those of its own after, in
	 * {@link IAjaxRoundTripEvent#updateAjaxAttributes(AjaxRequestAttributes)}. Does nothing by
	 * default.
	 * <p>
	 * The attributes have the URL, the method, POST, and the form the events need already: keep
	 * them.
	 *
	 * @param attributes
	 *            the attributes of the requests of the events, those submitting the form telling
	 *            its id ({@link AjaxRequestAttributes#getFormId()})
	 */
	protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
	{
	}

	/**
	 * @param event
	 *            an event
	 * @return the event, when it is one way or the other and has a handler
	 */
	static IClientEvent check(IClientEvent event)
	{
		Args.notNull(event, "event");
		Args.notEmpty(event.getEventName(), "event.eventName");
		int ways = (event instanceof IAjaxEvent ? 1 : 0) + (event instanceof IAjaxUpdateEvent ?
			1 : 0) + (event instanceof IAjaxSubmitEvent ? 1 : 0);
		if (ways > 1)
		{
			throw new IllegalArgumentException("The event " + event.getEventName() +
				" goes to the server more than one way: it is one of IAjaxEvent," +
				" IAjaxUpdateEvent and IAjaxSubmitEvent");
		}
		if (ways == 0 && event instanceof IAjaxRoundTripEvent)
		{
			throw new IllegalArgumentException("The event " + event.getEventName() +
				" goes to the server, but none of IAjaxEvent, IAjaxUpdateEvent and" +
				" IAjaxSubmitEvent tells what the server does with it");
		}
		String handler = event.getEventClientHandlerName();
		if (Strings.isEmpty(event.getEventHandlerClientSideCode()))
		{
			if (Strings.isEmpty(handler))
			{
				throw new IllegalArgumentException("The event " + event.getEventName() +
					" has no handler: neither the name of a function nor code");
			}
			if (!FUNCTION_NAME.matcher(handler).matches())
			{
				throw new IllegalArgumentException("The handler of the event " +
					event.getEventName() + ", " + handler +
					", is not the name of a function: a global or a path to one");
			}
		}
		return event;
	}

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		if (ajaxEvents != null)
		{
			throw new IllegalStateException("A ClientEventsBehavior listens to one component");
		}
		component.setOutputMarkupId(true);
		ajaxEvents = new AjaxEvents();
		submitEvents = new SubmitEvents();
		component.add(ajaxEvents, submitEvents);
	}

	@Override
	public void unbind(Component component)
	{
		component.remove(ajaxEvents, submitEvents);
		ajaxEvents = null;
		submitEvents = null;
		super.unbind(component);
	}

	/**
	 * Renders the script listening to the events and what the events render, once a response: a
	 * component may call it before its own script starts, Wicket's call coming after it then
	 * rendering nothing.
	 */
	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		if (response.wasRendered(this))
		{
			return;
		}
		response.markRendered(this);
		List<IClientEvent> all = getEvents();
		if (all.isEmpty())
		{
			return;
		}
		response.render(JavaScriptHeaderItem.forReference(JS));
		StringBuilder script = new StringBuilder("Wicket.ClientEvents.attach(")
			.append(ScriptJson.quote(component.getMarkupId())).append(",[");
		for (int index = 0; index < all.size(); index++)
		{
			IClientEvent event = check(all.get(index));
			if (event instanceof IAjaxUpdateEvent && !(component instanceof FormComponent))
			{
				throw new IllegalStateException("The event " + event.getEventName() +
					" updates the model of its component, which " + component +
					" is not a form component to");
			}
			event.renderHead(component, response);
			if (index > 0)
			{
				script.append(',');
			}
			script.append(ScriptJson.withFunctions(describe(component, response, event, index)));
		}
		script.append("],");
		String clientObject = getClientObject();
		script.append(Strings.isEmpty(clientObject) ? "null" : clientObject).append(",{");
		List<String> options = new ArrayList<>();
		String values = getClientValues();
		if (!Strings.isEmpty(values))
		{
			options.add("values:" + values);
		}
		String pending = getClientPendingValues();
		if (!Strings.isEmpty(pending))
		{
			options.add("pending:" + pending);
		}
		script.append(String.join(",", options)).append("});");
		response.render(OnDomReadyHeaderItem.forScript(script));
	}

	/**
	 * @param component
	 *            the component
	 * @param response
	 *            the response, which the listeners of the calls of the event render their head to
	 * @param event
	 *            an event of the component
	 * @param index
	 *            its place among them
	 * @return what the browser gets of it: its name and its place first, which the
	 *         {@link IClientEvent#toJson() JSON} of the event does not change, then the JSON, then,
	 *         for an event going to the server, the attributes of its requests as {@code ajax}
	 */
	private JSONObject describe(Component component, IHeaderResponse response, IClientEvent event,
		int index)
	{
		JSONObject described = new JSONObject();
		described.put("eventName", event.getEventName());
		described.put("index", index);
		JSONObject json = event.toJson();
		if (json != null)
		{
			for (String key : json.keySet())
			{
				// what goes to the server is the behavior's to tell
				if (!described.has(key) && !ClientEventJson.AJAX.equals(key))
				{
					described.put(key, json.opt(key));
				}
			}
		}
		if (event instanceof IAjaxRoundTripEvent roundTrip)
		{
			described.put(ClientEventJson.AJAX, new JSONFunction(
				attributesOf(component, response, roundTrip)));
		}
		return described;
	}

	/**
	 * @return the attributes of the requests of the event, as Wicket's Ajax takes them: those of
	 *         the behavior it goes by, of all the events of the component, then of the event
	 */
	private CharSequence attributesOf(Component component, IHeaderResponse response,
		IAjaxRoundTripEvent event)
	{
		return event instanceof IAjaxSubmitEvent ? submitEvents.attributesOf(component, response,
			event) : ajaxEvents.attributesOf(component, response, event);
	}

	/**
	 * Renders the head of the listeners of the calls that have one, as Wicket's Ajax behaviors do
	 * with theirs.
	 */
	private static void renderListeners(Component component, IHeaderResponse response,
		AjaxRequestAttributes attributes)
	{
		for (IAjaxCallListener listener : attributes.getAjaxCallListeners())
		{
			if (listener instanceof IComponentAwareHeaderContributor contributor)
			{
				contributor.renderHead(component, response);
			}
		}
	}

	/**
	 * Takes the values the component had not sent, {@link #getClientPendingValues()}, which the
	 * page's Ajax requests carry: Wicket's Ajax tells each component of the page of the target of
	 * the request, once the request was handled, before the components of the target render, and
	 * a page may tell them before. Once a request.
	 */
	@Override
	public void onEvent(Component component, IEvent<?> event)
	{
		super.onEvent(component, event);
		if (event.getPayload() instanceof AjaxRequestTarget target &&
			!Strings.isEmpty(getClientPendingValues()) && takesPendingValues())
		{
			String sent = parameters().getParameterValue(PENDING_PARAMETER_PREFIX +
				component.getMarkupId()).toOptionalString();
			IRequestParameters values = Strings.isEmpty(sent) ? null : pendingValues(sent);
			if (values != null && !values.getParameterNames().isEmpty())
			{
				onPendingValuesSent(target, values);
			}
		}
	}

	/**
	 * @return whether the behavior did not take its pending values in this request yet, taking
	 *         them now
	 */
	private boolean takesPendingValues()
	{
		RequestCycle cycle = RequestCycle.get();
		Set<ClientEventsBehavior> taken = cycle.getMetaData(PENDING_TAKEN);
		if (taken == null)
		{
			taken = Collections.newSetFromMap(new IdentityHashMap<>());
			cycle.setMetaData(PENDING_TAKEN, taken);
		}
		return taken.add(this);
	}

	/**
	 * @param sent
	 *            the values a component had not sent, as the browser sent them: JSON
	 * @return them as parameters, none for what is not a JSON object
	 */
	private static IRequestParameters pendingValues(String sent)
	{
		Map<String, List<StringValue>> values = new LinkedHashMap<>();
		try
		{
			JSONObject json = new JSONObject(sent);
			for (String name : json.keySet())
			{
				Object value = json.opt(name);
				if (value != null && value != JSONObject.NULL)
				{
					values.put(name, List.of(StringValue.valueOf(String.valueOf(value))));
				}
			}
		}
		catch (JSONException e)
		{
			LOG.debug("The values pending are not a JSON object: {}", sent);
		}
		return new IRequestParameters()
		{
			@Override
			public Set<String> getParameterNames()
			{
				return Collections.unmodifiableSet(values.keySet());
			}

			@Override
			public StringValue getParameterValue(String name)
			{
				List<StringValue> named = values.get(name);
				return named == null ? StringValue.valueOf((String)null) : named.get(0);
			}

			@Override
			public List<StringValue> getParameterValues(String name)
			{
				return values.get(name);
			}
		};
	}

	/**
	 * Gives a test the Ajax behavior to execute for an event, with the request parameters
	 * {@link #EVENT_PARAMETER}, {@link #INDEX_PARAMETER}, the place of the event in
	 * {@link #getEvents()}, and {@link #VALUES_PARAMETER}, which the browser sends with it.
	 *
	 * @param event
	 *            an event going to the server
	 * @return the Ajax behavior it goes by
	 * @throws IllegalArgumentException
	 *             when the event stays in the browser
	 */
	public AbstractDefaultAjaxBehavior getAjaxBehavior(IClientEvent event)
	{
		if (event instanceof IAjaxSubmitEvent)
		{
			return submitEvents;
		}
		if (event instanceof IAjaxEvent || event instanceof IAjaxUpdateEvent)
		{
			return ajaxEvents;
		}
		throw new IllegalArgumentException("The event " + event.getEventName() +
			" stays in the browser");
	}

	/**
	 * @return whether the component listens to an event of the type
	 */
	private boolean hasEvents(Class<?> type)
	{
		return getEvents().stream().anyMatch(type::isInstance);
	}

	/**
	 * @return the event the request is of, {@code null} when the component no longer has it, or
	 *         when it is of none of the types, which the behavior hears of
	 */
	private IClientEvent eventOf(IRequestParameters parameters, Class<?>... types)
	{
		String name = parameters.getParameterValue(EVENT_PARAMETER).toOptionalString();
		int index = parameters.getParameterValue(INDEX_PARAMETER).toInt(-1);
		List<IClientEvent> all = getEvents();
		if (index < 0 || index >= all.size())
		{
			LOG.debug("No event {} at {} among the events of the component", name, index);
			return null;
		}
		IClientEvent event = all.get(index);
		if (List.of(types).stream().noneMatch(type -> type.isInstance(event)) ||
			!event.getEventName().equals(name))
		{
			LOG.debug("The event at {} is {}, not {}", index, event.getEventName(), name);
			return null;
		}
		return event;
	}

	/**
	 * @return the values the request holds, an empty object for none or for what is not JSON
	 */
	private static JSONObject valuesOf(IRequestParameters parameters)
	{
		String values = parameters.getParameterValue(VALUES_PARAMETER).toOptionalString();
		if (Strings.isEmpty(values))
		{
			return new JSONObject();
		}
		try
		{
			return new JSONObject(values);
		}
		catch (JSONException e)
		{
			LOG.debug("The values sent are not a JSON object: {}", values);
			return new JSONObject();
		}
	}

	private static IRequestParameters parameters()
	{
		return RequestCycle.get().getRequest().getRequestParameters();
	}

	/**
	 * Gives the model of the component what the user entered, validated, then tells the event, as
	 * Wicket's {@link org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior} does.
	 */
	private static void update(IAjaxUpdateEvent event, JSONObject values,
		FormComponent<?> component, AjaxRequestTarget target)
	{
		try
		{
			component.inputChanged();
			component.validate();
			if (component.isValid())
			{
				component.valid();
				component.updateModel();
				event.onEvent(values, component, target);
			}
			else
			{
				component.invalid();
				event.onError(values, component, target, null);
			}
			component.updateAutoLabels(target);
		}
		catch (RuntimeException e)
		{
			event.onError(values, component, target, e);
		}
	}

	/**
	 * Hears of the {@link IAjaxEvent}s and the {@link IAjaxUpdateEvent}s, by the requests their
	 * handlers send. It renders the libraries of Wicket's Ajax for all the events going to the
	 * server.
	 */
	private final class AjaxEvents extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void renderHead(Component component, IHeaderResponse response)
		{
			if (hasEvents(IAjaxRoundTripEvent.class))
			{
				super.renderHead(component, response);
			}
		}

		@Override
		public boolean getStatelessHint(Component component)
		{
			// without events going by it, its URL never renders
			return !hasEvents(IAjaxEvent.class) && !hasEvents(IAjaxUpdateEvent.class);
		}

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			// in the body: Wicket sends the value of a field with it, a long text for example,
			// and the values may be long too
			attributes.setMethod(AjaxRequestAttributes.Method.POST);
			ClientEventsBehavior.this.updateAjaxAttributes(attributes);
		}

		/**
		 * @return the attributes of the requests of the event, as JSON
		 */
		CharSequence attributesOf(Component component, IHeaderResponse response,
			IAjaxRoundTripEvent event)
		{
			AjaxRequestAttributes attributes = getAttributes();
			event.updateAjaxAttributes(attributes);
			renderListeners(component, response, attributes);
			return renderAjaxAttributes(component, attributes);
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			IRequestParameters parameters = parameters();
			onEventSent(target, parameters);
			IClientEvent event = eventOf(parameters, IAjaxEvent.class, IAjaxUpdateEvent.class);
			if (event instanceof IAjaxUpdateEvent update)
			{
				if (getComponent() instanceof FormComponent<?> component)
				{
					update(update, valuesOf(parameters), component, target);
				}
			}
			else if (event instanceof IAjaxEvent ajax)
			{
				ajax.onEvent(valuesOf(parameters), target);
			}
		}
	}

	/**
	 * Hears of the {@link IAjaxSubmitEvent}s, by the requests their handlers send, submitting the
	 * form. Wicket's behavior submitting a form binds to an event, which the browser never fires
	 * here: it renders no script, the events coming with the attributes of their requests.
	 */
	private final class SubmitEvents extends AjaxFormSubmitBehavior
	{
		private static final long serialVersionUID = 1L;

		SubmitEvents()
		{
			super(SUBMIT_EVENT);
		}

		@Override
		public void renderHead(Component component, IHeaderResponse response)
		{
			// the libraries come with the other behavior, the attributes with the events
		}

		@Override
		public boolean getStatelessHint(Component component)
		{
			// without events going by it, its URL never renders
			return !hasEvents(IAjaxSubmitEvent.class);
		}

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			// the browser fires no event of it: the handlers of the events send the requests
			attributes.setEventNames();
			ClientEventsBehavior.this.updateAjaxAttributes(attributes);
		}

		/**
		 * @return the attributes of the requests of the event, as JSON
		 */
		CharSequence attributesOf(Component component, IHeaderResponse response,
			IAjaxRoundTripEvent event)
		{
			AjaxRequestAttributes attributes = getAttributes();
			event.updateAjaxAttributes(attributes);
			renderListeners(component, response, attributes);
			return renderAjaxAttributes(component, attributes);
		}

		@Override
		protected void onEvent(AjaxRequestTarget target)
		{
			// what the component sent besides, before the form takes what the user entered
			onEventSent(target, parameters());
			super.onEvent(target);
		}

		@Override
		protected void onSubmit(AjaxRequestTarget target)
		{
			IRequestParameters parameters = parameters();
			if (eventOf(parameters, IAjaxSubmitEvent.class) instanceof IAjaxSubmitEvent event)
			{
				event.onEvent(valuesOf(parameters), getForm(), target);
			}
		}

		@Override
		protected void onError(AjaxRequestTarget target)
		{
			IRequestParameters parameters = parameters();
			if (eventOf(parameters, IAjaxSubmitEvent.class) instanceof IAjaxSubmitEvent event)
			{
				event.onError(valuesOf(parameters), getForm(), target);
			}
		}
	}
}
