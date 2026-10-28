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
package com.antiliacode.widgets.richtext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Answers the items of the {@link IRichTextSuggester suggesters} of a rich text editor: the
 * behavior the editor in the browser posts the name of a suggester to, under
 * {@value #NAME_PARAMETER}, with the query under {@value #QUERY_PARAMETER}. It answers JSON,
 * {@code {"items": [...]}}, or {@code {"error": "..."}}. The query is the user's: at most
 * {@value #LONGEST} characters, and the items {@link #setMaxItems(int) so many}.
 * <p>
 * A {@link RichTextEditor} with {@link RichTextEditor#addSuggester(IRichTextSuggester)
 * suggesters} has one already. Add one to a page for the editors of React and Svelte in it, and
 * give them {@code suggestAt(}{@link #getSuggestUrl()}{@code , { name, char, insert })} as their
 * suggesters.
 *
 * @since 11.0.0
 */
public class RichTextSuggestBehavior extends AbstractAjaxBehavior
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(RichTextSuggestBehavior.class);

	/** The name the editor posts the name of the suggester under. */
	public static final String NAME_PARAMETER = "name";

	/** The name the editor posts the query under. */
	public static final String QUERY_PARAMETER = "query";

	/** The longest query, in characters, as the editor asks. */
	public static final int LONGEST = 50;

	/** The most items answered, unless set otherwise: those the editor shows. */
	public static final int DEFAULT_MAX_ITEMS = 10;

	private final List<IRichTextSuggester> suggesters = new ArrayList<>();

	private int maxItems = DEFAULT_MAX_ITEMS;

	/**
	 * @param suggesters
	 *            the suggesters it answers for
	 */
	public RichTextSuggestBehavior(IRichTextSuggester... suggesters)
	{
		for (IRichTextSuggester suggester : suggesters)
		{
			add(suggester);
		}
	}

	/**
	 * @param suggester
	 *            a suggester more, of a name of its own
	 * @return {@code this}
	 */
	public RichTextSuggestBehavior add(IRichTextSuggester suggester)
	{
		Args.notNull(suggester, "suggester");
		if (getSuggester(suggester.getName()) != null)
		{
			throw new IllegalArgumentException("There is a suggester named " + suggester.getName());
		}
		suggesters.add(suggester);
		return this;
	}

	/**
	 * @param suggester
	 *            a suggester to take away
	 * @return {@code this}
	 */
	public RichTextSuggestBehavior remove(IRichTextSuggester suggester)
	{
		suggesters.remove(suggester);
		return this;
	}

	/**
	 * @return the suggesters it answers for
	 */
	public List<IRichTextSuggester> getSuggesters()
	{
		return Collections.unmodifiableList(suggesters);
	}

	/**
	 * @return the suggester of a name, {@code null} for none
	 */
	public IRichTextSuggester getSuggester(String name)
	{
		return suggesters.stream().filter(suggester -> suggester.getName().equals(name)).findFirst()
			.orElse(null);
	}

	/**
	 * @return the most items answered
	 */
	public int getMaxItems()
	{
		return maxItems;
	}

	/**
	 * @param maxItems
	 *            the most items answered, {@value #DEFAULT_MAX_ITEMS} by default
	 * @return {@code this}
	 */
	public RichTextSuggestBehavior setMaxItems(int maxItems)
	{
		this.maxItems = Args.withinRange(1, 100, maxItems, "maxItems");
		return this;
	}

	/**
	 * @return the suggesters as the editor in the browser takes them, without their items: their
	 *         names, characters and insertions
	 */
	public JSONArray toJson()
	{
		JSONArray json = new JSONArray();
		suggesters.forEach(suggester -> {
			JSONObject one = new JSONObject().put("name", suggester.getName())
				.put("insert", suggester.getInsert().getName());
			if (suggester.getTrigger() != null)
			{
				one.put("char", suggester.getTrigger());
			}
			json.put(one);
		});
		return json;
	}

	/**
	 * @return the URL the editor posts the queries to
	 */
	public String getSuggestUrl()
	{
		return getCallbackUrl().toString();
	}

	/**
	 * Answers the request with what it answers, not by rendering the page.
	 */
	@Override
	public boolean rendersPage()
	{
		return false;
	}

	/**
	 * @return whether the component offers items now; {@code true} by default
	 */
	protected boolean isSuggestAllowed()
	{
		return true;
	}

	@Override
	public void onRequest()
	{
		JSONObject answer = new JSONObject();
		try
		{
			answer.put("items", suggest());
		}
		catch (Refusal refusal)
		{
			answer.put("error", refusal.getMessage());
		}
		RequestCycle.get().scheduleRequestHandlerAfterCurrent(new TextRequestHandler(
			"application/json", "UTF-8", answer.toString()));
	}

	private JSONArray suggest() throws Refusal
	{
		if (!isSuggestAllowed())
		{
			throw new Refusal("The editor offers nothing now");
		}
		Request request = RequestCycle.get().getRequest();
		if (request.getContainerRequest() instanceof HttpServletRequest servlet &&
			!"POST".equalsIgnoreCase(servlet.getMethod()))
		{
			throw new Refusal("Post the query");
		}
		IRequestParameters parameters = request.getPostParameters();
		String name = parameters.getParameterValue(NAME_PARAMETER).toOptionalString();
		IRichTextSuggester suggester = getSuggester(name);
		if (suggester == null)
		{
			throw new Refusal("No suggester is named " + name);
		}
		String query = parameters.getParameterValue(QUERY_PARAMETER).toString("");
		if (query.length() > LONGEST)
		{
			throw new Refusal("The query is too long: at most " + LONGEST + " characters");
		}
		List<RichTextSuggestion> items;
		try
		{
			items = suggester.suggest(query, maxItems);
		}
		catch (RuntimeException failure)
		{
			LOG.warn("The suggester {} of a rich text editor failed", name, failure);
			throw new Refusal("Nothing could be found");
		}
		JSONArray json = new JSONArray();
		if (items != null)
		{
			items.stream().filter(item -> item != null).limit(maxItems)
				.forEach(item -> json.put(item.toJson()));
		}
		return json;
	}

	/**
	 * Why nothing was answered, which the answer tells.
	 */
	private static final class Refusal extends Exception
	{
		private static final long serialVersionUID = 1L;

		Refusal(String message)
		{
			super(message, null, false, false);
		}
	}
}
