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
package com.antiliacode.widgets.autocomplete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.chips.Chips;
import com.antiliacode.widgets.dynamicdatatable.BuiltInTemplateEngine;
import com.antiliacode.widgets.dynamicdatatable.IJsonSerializer;
import com.antiliacode.widgets.dynamicdatatable.ITemplateEngine;
import com.antiliacode.widgets.dynamicdatatable.JacksonJsonSerializer;
import com.antiliacode.widgets.scrollbar.ScrollbarBehavior;
import com.antiliacode.widgets.template.Templates;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

/**
 * What {@link Autocomplete}, {@link MultiAutocomplete} and {@link FindAutocomplete} share: a text field suggesting values
 * as the user types, whose chosen values are shown as {@link Chips chips} with an x removing
 * them, as in a multiple {@link com.antiliacode.widgets.dropdown.Dropdown drop-down}.
 * <p>
 * Attach it to an {@code <input type="hidden">}. {@code Wicket.Autocomplete} from {@link #JS} puts
 * the chips, the text field and the list of suggestions next to it; the hidden input holds the ids
 * of the chosen values as a JSON array, which a form posts as always. A choice fires its
 * {@code change} event, so an {@code AjaxFormComponentUpdatingBehavior} on it is notified.
 * <p>
 * The suggestions come from the {@link IAutocompleteProvider provider}, asked by the browser via
 * Ajax once the user typed {@link #setMinLength(int) enough}, and are rendered in the browser:
 * each row is the {@link #setTemplate(String) template} evaluated against the value as the
 * {@link #setJsonSerializer(IJsonSerializer) serializer} writes it, by the
 * {@link #setTemplateEngine(ITemplateEngine) template engine}, just as the cells of a
 * {@link com.antiliacode.widgets.dynamicdatatable.JsonBasedDynamicDataTable}. {@code {{@label}}} is
 * the label of the value. The template is authored by the developer and written into the page as
 * it is; the values are escaped by {@code {{ }}}, and the labels written into the script of the
 * page cannot close it.
 * <p>
 * {@link #addAction(AutocompleteAction) Actions} are buttons at the end of every row, such as an
 * (i) whose tooltip, a template too, tells more about the value; F1 toggles the tooltip of the
 * active row. Up and Down move from row to row, Enter chooses the active one.
 *
 * @param <T>
 *            the type of the values
 * @param <M>
 *            the type of the model: a value, or a collection of them
 * @since 11.0.0
 */
public abstract class AbstractAutocomplete<T, M> extends FormComponent<M>
{
	private static final long serialVersionUID = 1L;

	/** The script of the autocomplete, {@code Wicket.Autocomplete}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		AbstractAutocomplete.class, "antilia-autocomplete.js");

	/** The style sheet of the autocomplete. */
	public static final ResourceReference CSS = new CssResourceReference(
		AbstractAutocomplete.class, "antilia-autocomplete.css");

	/** The template of a row unless set otherwise: the label of the value. */
	public static final String DEFAULT_TEMPLATE = "{{@label}}";

	private static final String TERM_PARAMETER = "term";

	private final IAutocompleteProvider<T> provider;

	private final SuggestBehavior suggestBehavior;

	private final List<AutocompleteAction> actions = new ArrayList<>();

	private String template = DEFAULT_TEMPLATE;

	private ITemplateEngine templateEngine = BuiltInTemplateEngine.INSTANCE;

	private IJsonSerializer jsonSerializer = JacksonJsonSerializer.INSTANCE;

	private int minLength = 1;

	private int maxSuggestions = 10;

	private IModel<String> placeholder;

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the chosen value or values
	 * @param provider
	 *            supplies the suggestions
	 */
	protected AbstractAutocomplete(String id, IModel<M> model, IAutocompleteProvider<T> provider)
	{
		super(id, model);
		this.provider = Args.notNull(provider, "provider");
		setOutputMarkupId(true);
		add(suggestBehavior = new SuggestBehavior());
		add(new ActionBehavior());
	}

	/**
	 * @return whether many values can be chosen
	 */
	protected abstract boolean isMultiple();

	/**
	 * @return whether a choice is only reported, not kept as a chip, as in a find bar
	 */
	protected boolean isPick()
	{
		return false;
	}

	/**
	 * @return the chosen values of the model, none if it is empty
	 */
	protected abstract List<T> getChosen();

	/**
	 * @return the model object holding the values the user chose
	 */
	protected abstract M toModelObject(List<T> values);

	/**
	 * @return the provider of the suggestions
	 */
	public IAutocompleteProvider<T> getProvider()
	{
		return provider;
	}

	/**
	 * @return the template of a row of the suggestions
	 */
	public String getTemplate()
	{
		return template;
	}

	/**
	 * @param template
	 *            the template of a row of the suggestions, evaluated against the value's JSON;
	 *            {@code {{@label}}} is its label
	 * @return {@code this}
	 */
	public AbstractAutocomplete<T, M> setTemplate(String template)
	{
		this.template = Args.notEmpty(template, "template");
		return this;
	}

	/**
	 * @param templateEngine
	 *            the engine evaluating the template in the browser
	 * @return {@code this}
	 */
	public AbstractAutocomplete<T, M> setTemplateEngine(ITemplateEngine templateEngine)
	{
		this.templateEngine = Args.notNull(templateEngine, "templateEngine");
		return this;
	}

	/**
	 * @param jsonSerializer
	 *            the serializer turning the values into the JSON the template reads
	 * @return {@code this}
	 */
	public AbstractAutocomplete<T, M> setJsonSerializer(IJsonSerializer jsonSerializer)
	{
		this.jsonSerializer = Args.notNull(jsonSerializer, "jsonSerializer");
		return this;
	}

	/**
	 * @return how many characters the user types before the suggestions are asked for
	 */
	public int getMinLength()
	{
		return minLength;
	}

	/**
	 * @param minLength
	 *            how many characters the user types before the suggestions are asked for
	 * @return {@code this}
	 */
	public AbstractAutocomplete<T, M> setMinLength(int minLength)
	{
		this.minLength = Math.max(0, minLength);
		return this;
	}

	/**
	 * @return the most suggestions shown
	 */
	public int getMaxSuggestions()
	{
		return maxSuggestions;
	}

	/**
	 * @param maxSuggestions
	 *            the most suggestions shown
	 * @return {@code this}
	 */
	public AbstractAutocomplete<T, M> setMaxSuggestions(int maxSuggestions)
	{
		this.maxSuggestions = Args.withinRange(1, 1000, maxSuggestions, "maxSuggestions");
		return this;
	}

	/**
	 * @param placeholder
	 *            the text of the empty text field, {@code null} for the default
	 * @return {@code this}
	 */
	public AbstractAutocomplete<T, M> setPlaceholder(IModel<String> placeholder)
	{
		// a ResourceModel looks its text up in the bundles of this component and its page
		this.placeholder = placeholder instanceof IComponentAssignedModel<String> assigned
			? assigned.wrapOnAssignment(this) : placeholder;
		return this;
	}

	/**
	 * Adds a button at the end of every row of the suggestions.
	 *
	 * @param action
	 *            the action
	 * @return {@code this}
	 */
	public AbstractAutocomplete<T, M> addAction(AutocompleteAction action)
	{
		Args.notNull(action, "action");
		// a ResourceModel looks its text up in the bundles of this component and its page
		if (action.getLabel() instanceof IComponentAssignedModel<String> assigned)
		{
			action.setLabel(assigned.wrapOnAssignment(this));
		}
		actions.add(action);
		return this;
	}

	/**
	 * @return the buttons at the end of every row of the suggestions
	 */
	public List<AutocompleteAction> getActions()
	{
		return actions;
	}

	/**
	 * Called when the user clicked an action without a tooltip on the row of a value.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param action
	 *            the name of the action
	 * @param value
	 *            the value of the row
	 */
	protected void onAction(AjaxRequestTarget target, String action, T value)
	{
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		if (!"input".equalsIgnoreCase(tag.getName()))
		{
			throw new WicketRuntimeException(
				"An autocomplete must be attached to an <input type=\"hidden\">, not to <" +
					tag.getName() + "> of " + this);
		}
		super.onComponentTag(tag);
		tag.put("type", "hidden");
		tag.put("value", getValue());
	}

	/**
	 * @return the ids of the chosen values of the model as a JSON array, empty if there are none,
	 *         so that a required autocomplete without a value is told so
	 */
	@Override
	protected String getModelValue()
	{
		List<T> chosen = getChosen();
		if (chosen.isEmpty())
		{
			return "";
		}
		JSONArray ids = new JSONArray();
		chosen.forEach(value -> ids.put(provider.getId(value)));
		return ids.toString();
	}

	@Override
	public void convertInput()
	{
		List<T> values = new ArrayList<>();
		for (String id : parseIds(getInput()))
		{
			T value = provider.fromId(id);
			if (value != null && !values.contains(value))
			{
				values.add(value);
			}
		}
		if (!isMultiple() && values.size() > 1)
		{
			values = values.subList(0, 1);
		}
		setConvertedInput(values.isEmpty() && !isMultiple() ? null : toModelObject(values));
	}

	/**
	 * @return the ids in the JSON array the browser sent, none if it sent none or something else
	 */
	static List<String> parseIds(String input)
	{
		List<String> ids = new ArrayList<>();
		if (input == null || input.isBlank())
		{
			return ids;
		}
		try
		{
			JSONArray array = new JSONArray(input);
			for (int i = 0; i < array.length(); i++)
			{
				String id = array.optString(i, null);
				if (id != null && !id.isEmpty())
				{
					ids.add(id);
				}
			}
		}
		catch (JSONException e)
		{
			// not sent by the script: nothing chosen
		}
		return ids;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		CoreLibrariesContributor.contribute(Application.get(), response);
		Chips.renderHeadItems(response);
		// the scroll bars of the list of suggestions
		ScrollbarBehavior.renderHeadItems(response);
		response.render(JavaScriptHeaderItem.forReference(Templates.JS));
		templateEngine.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Autocomplete.attach(" +
			JSONObject.quote(getMarkupId()) + ", " + scriptSafe(newOptions().toString()) + ");"));
	}

	/**
	 * Escapes every {@code <} of JSON, which only its strings hold, so that no text in it, such as a
	 * label, closes the script it is written into or opens a comment there.
	 *
	 * @param json
	 *            the JSON
	 * @return the same JSON, read the same by the script
	 */
	static String scriptSafe(String json)
	{
		return json.replace("<", "\\u003c");
	}

	/**
	 * @return the options of the script
	 */
	protected JSONObject newOptions()
	{
		JSONObject options = new JSONObject();
		options.put("url", suggestBehavior.getCallbackUrl().toString());
		options.put("multiple", isMultiple());
		options.put("pick", isPick());
		options.put("template", template);
		options.put("engine", templateEngine.getName());
		options.put("minLength", minLength);
		options.put("disabled", !isEnabledInHierarchy());
		String text = placeholder != null ? placeholder.getObject() : null;
		options.put("placeholder", text != null ? text
			: getString("Autocomplete.placeholder", null, "Type to search…"));
		options.put("noMatch", getString("Autocomplete.noMatch", null, "No matches"));
		options.put("searching", getString("Autocomplete.searching", null, "Searching…"));
		options.put("remove", getString("Dropdown.remove", null, "Remove {0}"));
		JSONArray chosen = new JSONArray();
		for (T value : getChosen())
		{
			chosen.put(new JSONObject().put("id", provider.getId(value))
				.put("label", provider.getLabel(value)));
		}
		options.put("chosen", chosen);
		JSONArray actionsJson = new JSONArray();
		actions.forEach(action -> actionsJson.put(action.toJson()));
		options.put("actions", actionsJson);
		return options;
	}

	@Override
	protected void onDetach()
	{
		provider.detach();
		actions.forEach(AutocompleteAction::detach);
		if (placeholder != null)
		{
			placeholder.detach();
		}
		super.onDetach();
	}

	/**
	 * @return the suggestions for the term as the browser reads them:
	 *         {@code [{id, label, data}]}
	 */
	String suggestionsJson(String term)
	{
		List<Map<String, Object>> suggestions = new ArrayList<>();
		String trimmed = term == null ? "" : term.strip();
		if (trimmed.length() >= minLength)
		{
			for (T value : provider.search(trimmed, maxSuggestions))
			{
				Map<String, Object> suggestion = new LinkedHashMap<>();
				suggestion.put("id", provider.getId(value));
				suggestion.put("label", provider.getLabel(value));
				suggestion.put("data", value);
				suggestions.add(suggestion);
			}
		}
		return jsonSerializer.toJson(suggestions);
	}

	/**
	 * @return the values, or none for {@code null}
	 */
	static <T> List<T> listOf(Collection<T> values)
	{
		return values != null ? new ArrayList<>(values) : new ArrayList<>();
	}

	/**
	 * Hands the actions the script reports, those without a tooltip, to
	 * {@link AbstractAutocomplete#onAction(AjaxRequestTarget, String, Object)}.
	 */
	private class ActionBehavior extends AjaxEventBehavior
	{
		private static final long serialVersionUID = 1L;

		private static final String ACTION_PARAMETER = "action";

		private static final String ID_PARAMETER = "id";

		ActionBehavior()
		{
			super("antilia-autocomplete-action");
		}

		@Override
		public boolean isEnabled(Component component)
		{
			return super.isEnabled(component) &&
				actions.stream().anyMatch(AutocompleteAction::isReported);
		}

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			// the detail of the event the script fires: the action and the id of the row
			attributes.getDynamicExtraParameters().add("var detail = (attrs.event.originalEvent " +
				"|| attrs.event).detail || {}; return {" + ACTION_PARAMETER + ": detail.action, " +
				ID_PARAMETER + ": detail.id};");
		}

		@Override
		protected void onEvent(AjaxRequestTarget target)
		{
			IRequestParameters parameters = getRequest().getRequestParameters();
			String name = parameters.getParameterValue(ACTION_PARAMETER).toOptionalString();
			String id = parameters.getParameterValue(ID_PARAMETER).toOptionalString();
			// only the actions there are, on values the provider knows
			boolean known = actions.stream().anyMatch(action -> action.isReported() &&
				action.getName().equals(name));
			T value = known && id != null ? provider.fromId(id) : null;
			if (value != null)
			{
				onAction(target, name, value);
			}
		}
	}

	/**
	 * Answers the browser's requests for suggestions with JSON.
	 */
	private class SuggestBehavior extends AbstractAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void onRequest()
		{
			String term = getComponent().getRequest().getRequestParameters()
				.getParameterValue(TERM_PARAMETER).toOptionalString();
			getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(
				new TextRequestHandler("application/json", "UTF-8", suggestionsJson(term)));
		}
	}
}
