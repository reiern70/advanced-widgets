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
import java.util.Iterator;
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
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.io.IClusterable;
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
 * stays in the browser, its handler doing what it does there. Each event going to the server has
 * an Ajax behavior of its own, which the behavior adds to the component: with its own URL, its own
 * attributes and its own response, as any Ajax behavior of Wicket. That of an {@link IAjaxEvent}
 * or an {@link IAjaxUpdateEvent} sends the request without the form, that of an
 * {@link IAjaxSubmitEvent} submits the form the component is in. They post, unless an event sets
 * GET: when the component is a field, Wicket sends its value with the request too, as the text a
 * {@link com.antiliacode.widgets.richtext.RichTextEditorChangeEvent} reads.
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
 * going to the server sends its own request, to its own behavior, which only it hears of. One
 * event keeps the size of a resized editor in the browser, for example, and another sends it to
 * the server; or a single {@link IAjaxEvent} does both, its handler keeping the size before it
 * calls {@code send}. {@link #add(IClientEvent)} adds an event after those of its name,
 * {@link #set(IClientEvent)} puts it in their place and {@link #removeEvents(String...)} takes
 * them away.
 * <p>
 * The attributes of the requests of each event are those of its behavior, Wicket's Ajax ones:
 * {@link #updateAjaxAttributes(AjaxRequestAttributes)} changes those of all the events of the
 * component, then {@link IAjaxRoundTripEvent#updateAjaxAttributes(AjaxRequestAttributes)} those of
 * the event alone, its method, its extra parameters, which a page veil leaving some requests alone
 * sees in their {@code ep}, its dynamic extra parameters and the listeners of its calls, on
 * success and on failure for example. Two events of one name may so go each its own way:
 *
 * <pre>
 * // on blur, the state goes to the server under the page's veil, and another request, which the
 * // veil leaves alone, tells the page
 * events.add(new SendStateEvent(RichTextEditorEvents.BLUR)
 * {
 * 	&#64;Override
 * 	public void updateAjaxAttributes(AjaxRequestAttributes attributes)
 * 	{
 * 		super.updateAjaxAttributes(attributes);
 * 		attributes.getAjaxCallListeners().add(new VeilListener());
 * 	}
 * });
 * events.add(AjaxEvent.of(RichTextEditorEvents.BLUR, (values, target) -&gt; ...)
 * 	.setExtraParameter("_nb", "true"));
 * </pre>
 * <p>
 * An event telling {@code leaving: true} in its {@code detail}, as the editors' events as the page
 * goes do, goes to the server in a beacon, which the browser sends even then.
 * <p>
 * The events go to the browser as the component renders, in the order they are in. A component
 * keeping events of its own, or of its parts, overrides {@link #getEvents()}, giving the same
 * events each time, each with its behavior;
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

	/**
	 * The parameter of a request naming the event, for the page's scripts and logs: the server
	 * knows the event by the behavior the request goes to, without reading it.
	 */
	public static final String EVENT_PARAMETER = "antilia-event";

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

	/**
	 * The Ajax behaviors of the events going to the server, one each, on the component, in the
	 * order they came.
	 */
	private final List<EventBehavior> eventBehaviors = new ArrayList<>();

	/** The component, once the behavior is bound to it. */
	private Component bound;

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
		addBehavior(event);
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
		List<IClientEvent> replaced = events.put(event.getEventName(), named);
		if (replaced != null)
		{
			replaced.forEach(this::dropBehavior);
		}
		addBehavior(event);
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
		dropBehavior(event);
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
		removed.forEach(this::dropBehavior);
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
	 * A subclass adding events gives the same events each time, not new ones: each event going to
	 * the server has its own Ajax behavior on the component, which the server finds it by.
	 *
	 * @return the events the component listens to, in their order: those added, unless a subclass
	 *         adds others
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
	 * for example. The Ajax behavior of each event calls it, then the event changes those of its
	 * own, in {@link IAjaxRoundTripEvent#updateAjaxAttributes(AjaxRequestAttributes)}: what
	 * differs from one event to another, the veil on blur but not as the user types for example,
	 * goes there. Does nothing by default.
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
		if (bound != null)
		{
			throw new IllegalStateException("A ClientEventsBehavior listens to one component");
		}
		component.setOutputMarkupId(true);
		bound = component;
		// those added: a subclass adding others may not be ready to give them yet
		getAddedEvents().forEach(this::addBehavior);
	}

	@Override
	public void unbind(Component component)
	{
		for (EventBehavior behavior : eventBehaviors)
		{
			component.remove(behavior.asBehavior());
		}
		eventBehaviors.clear();
		bound = null;
		super.unbind(component);
	}

	/**
	 * Gives each event going to the server its Ajax behavior before the component renders, those
	 * a subclass adds among them.
	 */
	@Override
	public void onConfigure(Component component)
	{
		super.onConfigure(component);
		syncBehaviors();
	}

	/**
	 * Gives each event of the component going to the server an Ajax behavior of its own, and takes
	 * away those of the events the component no longer has whose URL never went to the browser:
	 * the browser may still send a request to the others, which they leave alone.
	 */
	private void syncBehaviors()
	{
		if (bound == null)
		{
			return;
		}
		List<IClientEvent> all = getEvents();
		for (Iterator<EventBehavior> iterator = eventBehaviors.iterator(); iterator.hasNext();)
		{
			EventBehavior behavior = iterator.next();
			if (!behavior.isRendered() && !containsSame(all, behavior.getClientEvent()))
			{
				iterator.remove();
				bound.remove(behavior.asBehavior());
			}
		}
		all.forEach(this::addBehavior);
	}

	/**
	 * Adds the Ajax behavior of an event going to the server to the component, unless the event
	 * has one already.
	 */
	private void addBehavior(IClientEvent event)
	{
		if (bound == null || behaviorOf(event) != null)
		{
			return;
		}
		EventBehavior behavior;
		if (event instanceof IAjaxSubmitEvent submit)
		{
			behavior = new EventSubmitBehavior(submit);
		}
		else if (event instanceof IAjaxEvent || event instanceof IAjaxUpdateEvent)
		{
			behavior = new EventAjaxBehavior((IAjaxRoundTripEvent)event);
		}
		else
		{
			// it stays in the browser
			return;
		}
		eventBehaviors.add(behavior);
		bound.add(behavior.asBehavior());
	}

	/**
	 * Takes the Ajax behavior of an event the component no longer listens to away from the
	 * component, unless its URL went to the browser already, or the event was added again.
	 */
	private void dropBehavior(IClientEvent event)
	{
		EventBehavior behavior = behaviorOf(event);
		if (bound != null && behavior != null && !behavior.isRendered() &&
			!containsSame(getAddedEvents(), event))
		{
			eventBehaviors.remove(behavior);
			bound.remove(behavior.asBehavior());
		}
	}

	/**
	 * @return the Ajax behavior of the event, {@code null} for none
	 */
	private EventBehavior behaviorOf(IClientEvent event)
	{
		for (EventBehavior behavior : eventBehaviors)
		{
			if (behavior.getClientEvent() == event)
			{
				return behavior;
			}
		}
		return null;
	}

	/**
	 * @return whether the events hold the event itself, not merely one equal to it
	 */
	private static boolean containsSame(List<IClientEvent> all, IClientEvent event)
	{
		return all.stream().anyMatch(each -> each == event);
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
	 *            the response, which the libraries of Wicket's Ajax and the listeners of the calls
	 *            of the event render their head to
	 * @param event
	 *            an event of the component, checked
	 * @param index
	 *            its place among them
	 * @return what the browser gets of it: its name and its place first, which the
	 *         {@link IClientEvent#toJson() JSON} of the event does not change, then the JSON, then,
	 *         for an event going to the server, the attributes of the requests of its behavior as
	 *         {@code ajax}
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
		if (event instanceof IAjaxRoundTripEvent)
		{
			EventBehavior behavior = behaviorOf(event);
			if (behavior == null)
			{
				// too late to add one: Wicket takes no behaviors as the component renders
				throw new IllegalStateException("The event " + event.getEventName() +
					" came after " + component + " was configured: getEvents() gives the" +
					" same events each time, and a page changes them before it renders the" +
					" component");
			}
			described.put(ClientEventJson.AJAX, new JSONFunction(behavior.renderAttributes(
				component, response)));
		}
		return described;
	}

	/**
	 * Renders the libraries of Wicket's Ajax and the base URL of its requests, as its behaviors
	 * do.
	 */
	private static void renderAjaxLibraries(Component component, IHeaderResponse response)
	{
		CoreLibrariesContributor.contributeAjax(component.getApplication(), response);
		Url baseUrl = component.getRequestCycle().getUrlRenderer().getBaseUrl();
		response.render(JavaScriptHeaderItem.forScript("Wicket.Ajax.baseUrl=\"" +
			Strings.escapeMarkup(baseUrl.toString()) + "\";", "wicket-ajax-base-url"));
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
	 * Gives a test the Ajax behavior to execute for an event, with the request parameter
	 * {@link #VALUES_PARAMETER}, which the browser sends with it.
	 *
	 * @param event
	 *            an event going to the server, which the component listens to
	 * @return its own Ajax behavior on the component
	 * @throws IllegalArgumentException
	 *             when the event stays in the browser, or the component does not listen to it
	 */
	public AbstractDefaultAjaxBehavior getAjaxBehavior(IClientEvent event)
	{
		Args.notNull(event, "event");
		if (!(event instanceof IAjaxEvent || event instanceof IAjaxUpdateEvent ||
			event instanceof IAjaxSubmitEvent))
		{
			throw new IllegalArgumentException("The event " + event.getEventName() +
				" stays in the browser");
		}
		syncBehaviors();
		EventBehavior behavior = behaviorOf(event);
		if (behavior == null)
		{
			throw new IllegalArgumentException("The component does not listen to the event " +
				event.getEventName());
		}
		return behavior.asBehavior();
	}

	/**
	 * @return whether the component still listens to the event, which a request came for: the
	 *         browser may send one after the page took the event away
	 */
	private boolean listensTo(IClientEvent event)
	{
		if (containsSame(getEvents(), event))
		{
			return true;
		}
		LOG.debug("The component no longer listens to the event {}", event.getEventName());
		return false;
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

	/** The Ajax behavior of an event going to the server, on the component. */
	private interface EventBehavior extends IClusterable
	{
		/**
		 * @return the event, which the requests of the behavior are of
		 */
		IAjaxRoundTripEvent getClientEvent();

		/**
		 * @return whether the URL of the behavior went to the browser, which may send a request to
		 *         it then
		 */
		boolean isRendered();

		/**
		 * Renders what the requests need, the libraries of Wicket's Ajax and the head of the
		 * listeners of their calls.
		 *
		 * @param component
		 *            the component
		 * @param response
		 *            the header response
		 * @return the attributes of the requests of the event, as JSON
		 */
		CharSequence renderAttributes(Component component, IHeaderResponse response);

		/**
		 * @return the behavior itself
		 */
		AbstractDefaultAjaxBehavior asBehavior();
	}

	/**
	 * Hears of an {@link IAjaxEvent} or an {@link IAjaxUpdateEvent}, by the requests its handler
	 * sends.
	 */
	private final class EventAjaxBehavior extends AbstractDefaultAjaxBehavior
		implements EventBehavior
	{
		private static final long serialVersionUID = 1L;

		private final IAjaxRoundTripEvent event;

		private boolean rendered;

		EventAjaxBehavior(IAjaxRoundTripEvent event)
		{
			this.event = event;
		}

		@Override
		public IAjaxRoundTripEvent getClientEvent()
		{
			return event;
		}

		@Override
		public boolean isRendered()
		{
			return rendered;
		}

		@Override
		public AbstractDefaultAjaxBehavior asBehavior()
		{
			return this;
		}

		@Override
		public CharSequence renderAttributes(Component component, IHeaderResponse response)
		{
			rendered = true;
			AjaxRequestAttributes attributes = getAttributes();
			renderAjaxLibraries(component, response);
			renderListeners(component, response, attributes);
			return renderAjaxAttributes(component, attributes);
		}

		/**
		 * POST, then those of all the events of the component, then those of the event.
		 */
		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			// in the body: Wicket sends the value of a field with it, a long text for example,
			// and the values may be long too
			attributes.setMethod(AjaxRequestAttributes.Method.POST);
			ClientEventsBehavior.this.updateAjaxAttributes(attributes);
			event.updateAjaxAttributes(attributes);
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			IRequestParameters parameters = parameters();
			onEventSent(target, parameters);
			if (!listensTo(event))
			{
				return;
			}
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
	 * Hears of an {@link IAjaxSubmitEvent}, by the requests its handler sends, submitting the form.
	 * Wicket's behavior submitting a form binds to an event, which the browser never fires here: it
	 * renders no script, the event coming with the attributes of its requests.
	 */
	private final class EventSubmitBehavior extends AjaxFormSubmitBehavior
		implements EventBehavior
	{
		private static final long serialVersionUID = 1L;

		private final IAjaxSubmitEvent event;

		private boolean rendered;

		EventSubmitBehavior(IAjaxSubmitEvent event)
		{
			super(SUBMIT_EVENT);
			this.event = event;
		}

		@Override
		public IAjaxRoundTripEvent getClientEvent()
		{
			return event;
		}

		@Override
		public boolean isRendered()
		{
			return rendered;
		}

		@Override
		public AbstractDefaultAjaxBehavior asBehavior()
		{
			return this;
		}

		@Override
		public CharSequence renderAttributes(Component component, IHeaderResponse response)
		{
			rendered = true;
			AjaxRequestAttributes attributes = getAttributes();
			renderAjaxLibraries(component, response);
			renderListeners(component, response, attributes);
			return renderAjaxAttributes(component, attributes);
		}

		/**
		 * Renders what Wicket's Ajax behaviors render, without the script of Wicket's behavior
		 * binding to the event: without names of events, it would send the request as the page is
		 * ready.
		 */
		@Override
		public void renderHead(Component component, IHeaderResponse response)
		{
			renderAjaxLibraries(component, response);
			renderListeners(component, response, getAttributes());
		}

		/**
		 * Those of the form, then those of all the events of the component, then those of the
		 * event.
		 */
		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			// the browser fires no event of it: the handler of the event sends the requests
			attributes.setEventNames();
			ClientEventsBehavior.this.updateAjaxAttributes(attributes);
			event.updateAjaxAttributes(attributes);
		}

		@Override
		protected void onEvent(AjaxRequestTarget target)
		{
			// what the component sent besides, before the form takes what the user entered
			onEventSent(target, parameters());
			if (listensTo(event))
			{
				super.onEvent(target);
			}
		}

		@Override
		protected void onSubmit(AjaxRequestTarget target)
		{
			event.onEvent(valuesOf(parameters()), getForm(), target);
		}

		@Override
		protected void onError(AjaxRequestTarget target)
		{
			event.onError(valuesOf(parameters()), getForm(), target);
		}
	}
}
