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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;
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
 * Each event runs its handler in the browser; there the Ajax ones go to the server, through one of
 * two Ajax behaviors the behavior adds to the component, whatever the number of events: one for
 * the {@link IAjaxEvent}s and the {@link IAjaxUpdateEvent}s, without the form, one for the
 * {@link IAjaxSubmitEvent}s, submitting the form the component is in. Both post: when the
 * component is a field, Wicket sends its value with an Ajax event too, as the text a
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
 * going to the server sends its own request, which only it hears of. One event keeps the size of a
 * resized editor in the browser, for example, and another sends it to the server; or a single
 * {@link IAjaxEvent} does both, its handler keeping the size before it calls {@code send}.
 * {@link #add(IClientEvent)} adds an event after those of its name, {@link #set(IClientEvent)}
 * puts it in their place and {@link #removeEvents(String)} takes them away.
 * <p>
 * An event telling {@code leaving: true} in its {@code detail}, as the editors' events as the page
 * goes do, goes to the server in a beacon, which the browser sends even then.
 * <p>
 * The events go to the browser as the component renders, in the order they are in, by which the
 * server finds them again: a page changes them before it renders the component, not between. A
 * component keeping events of its own, or of its parts, overrides {@link #getEvents()};
 * {@link #getClientObject()} gives the handlers what the component is in the browser,
 * {@link #getClientValues()} what each request carries besides, which
 * {@link #onEventSent(AjaxRequestTarget, IRequestParameters)} takes first.
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

	/** The private event of the browser the Ajax events go to the server by. */
	static final String AJAX_EVENT = "antilia-client-events:ajax";

	/** The private event of the browser the events submitting the form go to the server by. */
	static final String SUBMIT_EVENT = "antilia-client-events:submit";

	/** What the name of a function of the page is: a global, or a path to one. */
	private static final Pattern FUNCTION_NAME = Pattern.compile(
		"[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)*");

	/**
	 * Reads the request of a private event from the event, as Wicket's Ajax wraps it: the
	 * parameters the component adds, then the event's own.
	 */
	private static final String PARAMETERS = "var e=attrs.event,d=e&&(e.detail||" +
		"(e.originalEvent&&e.originalEvent.detail))||{},x=d.extra||{},r={},k;" +
		"for(k in x){if(Object.prototype.hasOwnProperty.call(x,k)){r[k]=x[k];}}" +
		"r['" + EVENT_PARAMETER + "']=d.event||'';r['" + INDEX_PARAMETER +
		"']=d.index==null?-1:d.index;r['" + VALUES_PARAMETER + "']=d.values||'{}';return r;";

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
	 * @param eventName
	 *            the name of events
	 * @return the events of the name the component no longer listens to, an empty list for none
	 */
	public List<IClientEvent> removeEvents(String eventName)
	{
		List<IClientEvent> removed = events.remove(eventName);
		return removed == null ? List.of() : Collections.unmodifiableList(removed);
	}

	/**
	 * @param eventName
	 *            the name of events
	 * @return the events of the name the component no longer listens to, an empty list for none
	 */
	public List<IClientEvent> removeEvents(IClientEventName eventName)
	{
		return removeEvents(AbstractClientEvent.nameOf(eventName));
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

	private enum Kind
	{
		CLIENT("client"), AJAX("ajax"), UPDATE("ajax"), SUBMIT("submit");

		/** The kind in the browser: an update goes as an Ajax event does. */
		private final String script;

		Kind(String script)
		{
			this.script = script;
		}
	}

	private static Kind kind(IClientEvent event)
	{
		if (event instanceof IAjaxSubmitEvent)
		{
			return Kind.SUBMIT;
		}
		if (event instanceof IAjaxUpdateEvent)
		{
			return Kind.UPDATE;
		}
		return event instanceof IAjaxEvent ? Kind.AJAX : Kind.CLIENT;
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

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
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
			if (kind(event) == Kind.UPDATE && !(component instanceof FormComponent))
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
			script.append(ScriptJson.withFunctions(describe(event, index)));
		}
		script.append("],");
		String clientObject = getClientObject();
		script.append(Strings.isEmpty(clientObject) ? "null" : clientObject).append(",{");
		// the URLs a beacon goes to as the page goes
		List<String> options = new ArrayList<>();
		if (hasEvents(Kind.AJAX) || hasEvents(Kind.UPDATE))
		{
			options.add("ajaxUrl:" + ScriptJson.quote(ajaxEvents.getCallbackUrl().toString()));
		}
		if (hasEvents(Kind.SUBMIT))
		{
			options.add("submitUrl:" +
				ScriptJson.quote(submitEvents.getCallbackUrl().toString()));
		}
		String values = getClientValues();
		if (!Strings.isEmpty(values))
		{
			options.add("values:" + values);
		}
		script.append(String.join(",", options)).append("});");
		response.render(OnDomReadyHeaderItem.forScript(script));
	}

	/**
	 * @param event
	 *            an event of the component
	 * @param index
	 *            its place among them
	 * @return what the browser gets of it: its name, its place and its kind first, which the
	 *         {@link IClientEvent#toJson() JSON} of the event does not change, then the JSON
	 */
	private static JSONObject describe(IClientEvent event, int index)
	{
		JSONObject described = new JSONObject();
		described.put("eventName", event.getEventName());
		described.put("index", index);
		described.put("kind", kind(event).script);
		JSONObject json = event.toJson();
		if (json != null)
		{
			for (String key : json.keySet())
			{
				if (!described.has(key))
				{
					described.put(key, json.opt(key));
				}
			}
		}
		return described;
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
	public AjaxEventBehavior getAjaxBehavior(IClientEvent event)
	{
		return switch (kind(event))
		{
			case AJAX, UPDATE -> ajaxEvents;
			case SUBMIT -> submitEvents;
			case CLIENT -> throw new IllegalArgumentException("The event " +
				event.getEventName() + " stays in the browser");
		};
	}

	/**
	 * @return whether the component listens to an event of the kind
	 */
	private boolean hasEvents(Kind kind)
	{
		return getEvents().stream().anyMatch(event -> kind(event) == kind);
	}

	/**
	 * @return the event the request is of, {@code null} when the component no longer has it
	 */
	private IClientEvent eventOf(IRequestParameters parameters, Kind... kinds)
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
		if (!List.of(kinds).contains(kind(event)) || !event.getEventName().equals(name))
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
	 * Hears of the {@link IAjaxEvent}s and the {@link IAjaxUpdateEvent}s, by the private event
	 * their handlers fire.
	 */
	private final class AjaxEvents extends AjaxEventBehavior
	{
		private static final long serialVersionUID = 1L;

		AjaxEvents()
		{
			super(AJAX_EVENT);
		}

		@Override
		public void renderHead(Component component, IHeaderResponse response)
		{
			if (hasEvents(Kind.AJAX) || hasEvents(Kind.UPDATE))
			{
				super.renderHead(component, response);
			}
		}

		@Override
		public boolean getStatelessHint(Component component)
		{
			// without events of its kinds, it never renders
			return !hasEvents(Kind.AJAX) && !hasEvents(Kind.UPDATE);
		}

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			// in the body: Wicket sends the value of a field with it, a long text for example,
			// and the values may be long too
			attributes.setMethod(AjaxRequestAttributes.Method.POST);
			attributes.getDynamicExtraParameters().add(PARAMETERS);
		}

		@Override
		protected void onEvent(AjaxRequestTarget target)
		{
			IRequestParameters parameters = parameters();
			onEventSent(target, parameters);
			IClientEvent event = eventOf(parameters, Kind.AJAX, Kind.UPDATE);
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
	 * Hears of the {@link IAjaxSubmitEvent}s, by the private event their handlers fire, submitting
	 * the form.
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
			if (hasEvents(Kind.SUBMIT))
			{
				super.renderHead(component, response);
			}
		}

		@Override
		public boolean getStatelessHint(Component component)
		{
			// without events of its kind, it never renders
			return !hasEvents(Kind.SUBMIT);
		}

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			attributes.getDynamicExtraParameters().add(PARAMETERS);
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
			if (eventOf(parameters, Kind.SUBMIT) instanceof IAjaxSubmitEvent event)
			{
				event.onEvent(valuesOf(parameters), getForm(), target);
			}
		}

		@Override
		protected void onError(AjaxRequestTarget target)
		{
			IRequestParameters parameters = parameters();
			if (eventOf(parameters, Kind.SUBMIT) instanceof IAjaxSubmitEvent event)
			{
				event.onError(valuesOf(parameters), getForm(), target);
			}
		}
	}
}
