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
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
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
 * a rich text editor for example. Each event runs its function in the browser; there the Ajax ones
 * go to the server, through one of two Ajax behaviors the behavior adds to the component, whatever
 * the number of events: one for the {@link IAjaxEvent}s, without the form, one for the
 * {@link IAjaxSubmitEvent}s, submitting the form the component is in. Both post: when the
 * component is a field, Wicket sends its value with an Ajax event too, as the text a
 * {@link com.antiliacode.widgets.richtext.RichTextEditorChangeEvent} reads.
 *
 * <pre>
 * ClientEventsBehavior events = new ClientEventsBehavior();
 * events.add(AjaxEvent.of("focusin", (eventId, values, target) -&gt; target.add(status)));
 * events.add(new ClientEvent("keydown", "function(event) { ... }"));
 * component.add(events);
 * </pre>
 *
 * Several events may have one name: each runs, in their order, with its own function and delay,
 * and each going to the server sends its own request, which only it hears of. One event keeps the
 * size of a resized editor in the browser, for example, and another sends it to the server; or a
 * single {@link IAjaxEvent} does both, its function keeping the size before it calls
 * {@code send}.
 * <p>
 * The events go to the browser as the component renders, in the order they are in, by which the
 * server finds them again: a page changes them before it renders the component, not between. A
 * component keeping events of its own, or of its parts, overrides {@link #getEvents()};
 * {@link #getClientObject()} gives the functions what the component is in the browser.
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

	/** Reads the request of a private event from the event, as Wicket's Ajax wraps it. */
	private static final String PARAMETERS = "var e=attrs.event,d=e&&(e.detail||" +
		"(e.originalEvent&&e.originalEvent.detail))||{};return {'" + EVENT_PARAMETER +
		"':d.event||'','" + INDEX_PARAMETER + "':d.index==null?-1:d.index,'" + VALUES_PARAMETER +
		"':d.values||'{}'};";

	private final List<IClientEvent> events = new ArrayList<>();

	private AjaxEvents ajaxEvents;

	private SubmitEvents submitEvents;

	/**
	 * @param event
	 *            an event the component listens to, after those it listens to already
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             when the event goes to the server both ways, or stays in the browser without a
	 *             function
	 */
	public ClientEventsBehavior add(IClientEvent event)
	{
		events.add(check(event));
		return this;
	}

	/**
	 * @param event
	 *            an event the component no longer listens to
	 * @return whether the component listened to it
	 */
	public boolean remove(IClientEvent event)
	{
		return events.remove(event);
	}

	/**
	 * @return the events added, in their order
	 */
	public List<IClientEvent> getAddedEvents()
	{
		return Collections.unmodifiableList(events);
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
	 * @return JavaScript, {@code function(element) {...}}, giving what the functions of the events
	 *         get as the component from its element; {@code null}, as by default, for the element
	 */
	protected String getClientObject()
	{
		return null;
	}

	/**
	 * @param event
	 *            an event
	 * @return the event, when it is one way or the other
	 */
	static IClientEvent check(IClientEvent event)
	{
		Args.notNull(event, "event");
		Args.notEmpty(event.getId(), "event.id");
		if (event instanceof IAjaxEvent && event instanceof IAjaxSubmitEvent)
		{
			throw new IllegalArgumentException("The event " + event.getId() +
				" is both an IAjaxEvent and an IAjaxSubmitEvent: it is one or the other");
		}
		if (kind(event) == Kind.CLIENT && Strings.isEmpty(event.getClientFunction()))
		{
			throw new IllegalArgumentException("The event " + event.getId() +
				" stays in the browser but has no function to run there");
		}
		return event;
	}

	private enum Kind
	{
		CLIENT("client"), AJAX("ajax"), SUBMIT("submit");

		/** The kind in the browser. */
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
			event.renderHead(component, response);
			if (index > 0)
			{
				script.append(',');
			}
			script.append("{id:").append(ScriptJson.quote(event.getId())).append(",index:")
				.append(index).append(",kind:'").append(kind(event).script).append("',delay:")
				.append(event.getDelay() < 1 ? IClientEvent.NO_DELAY : event.getDelay());
			String function = event.getClientFunction();
			if (!Strings.isEmpty(function))
			{
				script.append(",fn:").append(function);
			}
			script.append('}');
		}
		script.append(']');
		String clientObject = getClientObject();
		if (!Strings.isEmpty(clientObject))
		{
			script.append(',').append(clientObject);
		}
		script.append(");");
		response.render(OnDomReadyHeaderItem.forScript(script));
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
			case AJAX -> ajaxEvents;
			case SUBMIT -> submitEvents;
			case CLIENT -> throw new IllegalArgumentException("The event " + event.getId() +
				" stays in the browser");
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
	private <E extends IClientEvent> E eventOf(IRequestParameters parameters, Class<E> type)
	{
		String id = parameters.getParameterValue(EVENT_PARAMETER).toOptionalString();
		int index = parameters.getParameterValue(INDEX_PARAMETER).toInt(-1);
		List<IClientEvent> all = getEvents();
		if (index < 0 || index >= all.size())
		{
			LOG.debug("No event {} at {} among the events of the component", id, index);
			return null;
		}
		IClientEvent event = all.get(index);
		if (!type.isInstance(event) || !event.getId().equals(id))
		{
			LOG.debug("The event at {} is {}, not {}", index, event.getId(), id);
			return null;
		}
		return type.cast(event);
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
	 * Hears of the {@link IAjaxEvent}s, by the private event their functions fire.
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
			if (hasEvents(Kind.AJAX))
			{
				super.renderHead(component, response);
			}
		}

		@Override
		public boolean getStatelessHint(Component component)
		{
			// without events of its kind, it never renders
			return !hasEvents(Kind.AJAX);
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
			IAjaxEvent event = eventOf(parameters, IAjaxEvent.class);
			if (event != null)
			{
				event.onEvent(event.getId(), valuesOf(parameters), target);
			}
		}
	}

	/**
	 * Hears of the {@link IAjaxSubmitEvent}s, by the private event their functions fire, submitting
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
		protected void onSubmit(AjaxRequestTarget target)
		{
			IRequestParameters parameters = parameters();
			IAjaxSubmitEvent event = eventOf(parameters, IAjaxSubmitEvent.class);
			if (event != null)
			{
				event.onEvent(event.getId(), valuesOf(parameters), getForm(), target);
			}
		}

		@Override
		protected void onError(AjaxRequestTarget target)
		{
			IRequestParameters parameters = parameters();
			IAjaxSubmitEvent event = eventOf(parameters, IAjaxSubmitEvent.class);
			if (event != null)
			{
				event.onError(event.getId(), valuesOf(parameters), getForm(), target);
			}
		}
	}
}
