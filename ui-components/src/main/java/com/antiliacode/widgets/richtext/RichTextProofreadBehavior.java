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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Checks the texts of a rich text editor with a {@link IRichTextProofreader}, and answers what it
 * finds: the behavior the editor in the browser posts the texts to, as a JSON array under
 * {@value #TEXTS_PARAMETER}, with their language under {@value #LANGUAGE_PARAMETER}. It answers
 * JSON, {@code {"issues": [[...], ...]}}, the issues of each text, or {@code {"error": "..."}}.
 * <p>
 * The texts are the user's: there are at most {@link #setMaxTexts(int) so many} in a request, of
 * {@link #setMaxLength(int) so many characters} in all, and the issues the proofreader answers are
 * kept only within their text.
 * <p>
 * A {@link RichTextEditor} with {@link RichTextEditor#setProofreader(IRichTextProofreader) a
 * proofreader} has one already. Add one to a page for the editors of React and Svelte in it, and
 * give them {@code proofreadAt(}{@link #getProofreadUrl()}{@code )} as their {@code proofreader}.
 *
 * @since 11.0.0
 */
public class RichTextProofreadBehavior extends AbstractAjaxBehavior
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(RichTextProofreadBehavior.class);

	/** The name the editor posts the texts under, a JSON array of strings. */
	public static final String TEXTS_PARAMETER = "texts";

	/** The name the editor posts the language of the texts under, a tag of BCP 47. */
	public static final String LANGUAGE_PARAMETER = "language";

	/** The most texts in a request, unless set otherwise. */
	public static final int DEFAULT_MAX_TEXTS = 100;

	/** The most characters of the texts of a request, unless set otherwise. */
	public static final int DEFAULT_MAX_LENGTH = 20_000;

	/** A language tag of BCP 47: en, en-GB, zh-Hant-TW. */
	private static final Pattern LANGUAGE_TAG = Pattern.compile("[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*");

	private final IRichTextProofreader proofreader;

	private int maxTexts = DEFAULT_MAX_TEXTS;

	private int maxLength = DEFAULT_MAX_LENGTH;

	/**
	 * @param proofreader
	 *            what checks the texts
	 */
	public RichTextProofreadBehavior(IRichTextProofreader proofreader)
	{
		this.proofreader = Args.notNull(proofreader, "proofreader");
	}

	/**
	 * @return what checks the texts
	 */
	public IRichTextProofreader getProofreader()
	{
		return proofreader;
	}

	/**
	 * @return the most texts in a request
	 */
	public int getMaxTexts()
	{
		return maxTexts;
	}

	/**
	 * @param maxTexts
	 *            the most texts in a request, {@value #DEFAULT_MAX_TEXTS} by default
	 * @return {@code this}
	 */
	public RichTextProofreadBehavior setMaxTexts(int maxTexts)
	{
		this.maxTexts = Args.withinRange(1, Integer.MAX_VALUE, maxTexts, "maxTexts");
		return this;
	}

	/**
	 * @return the most characters of the texts of a request
	 */
	public int getMaxLength()
	{
		return maxLength;
	}

	/**
	 * @param maxLength
	 *            the most characters of the texts of a request, {@value #DEFAULT_MAX_LENGTH} by
	 *            default: the editor asks about 6000 at most, but for a block longer alone
	 * @return {@code this}
	 */
	public RichTextProofreadBehavior setMaxLength(int maxLength)
	{
		this.maxLength = Args.withinRange(1, Integer.MAX_VALUE, maxLength, "maxLength");
		return this;
	}

	/**
	 * @return the URL the editor posts the texts to
	 */
	public String getProofreadUrl()
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
	 * @return whether the component has its texts checked now; {@code true} by default
	 */
	protected boolean isProofreadAllowed()
	{
		return true;
	}

	/**
	 * @param requested
	 *            the language the browser asks for, {@code null} for none
	 * @return the language the texts are checked in: the one asked for; {@code null} for the
	 *         proofreader to find
	 */
	protected Locale getLanguage(Locale requested)
	{
		return requested;
	}

	@Override
	public void onRequest()
	{
		JSONObject answer = new JSONObject();
		try
		{
			answer.put("issues", check());
		}
		catch (Refusal refusal)
		{
			answer.put("error", refusal.getMessage());
		}
		RequestCycle.get().scheduleRequestHandlerAfterCurrent(new TextRequestHandler(
			"application/json", "UTF-8", answer.toString()));
	}

	/**
	 * Takes the texts of the request, checks them and keeps what the proofreader found within them.
	 *
	 * @return the issues of each text
	 */
	private JSONArray check() throws Refusal
	{
		if (!isProofreadAllowed())
		{
			throw new Refusal("The editor checks no texts now");
		}
		Request request = RequestCycle.get().getRequest();
		if (request.getContainerRequest() instanceof HttpServletRequest servlet &&
			!"POST".equalsIgnoreCase(servlet.getMethod()))
		{
			throw new Refusal("Post the texts");
		}
		IRequestParameters parameters = request.getPostParameters();
		List<String> texts = texts(parameters.getParameterValue(TEXTS_PARAMETER).toOptionalString());
		Locale language = getLanguage(language(
			parameters.getParameterValue(LANGUAGE_PARAMETER).toOptionalString()));
		List<List<RichTextIssue>> found;
		try
		{
			found = texts.isEmpty() ? List.of() : proofreader.check(texts, language);
		}
		catch (IOException | RuntimeException failure)
		{
			LOG.warn("The proofreader of a rich text editor failed", failure);
			throw new Refusal("The texts could not be checked");
		}
		if (found == null || found.size() != texts.size())
		{
			LOG.warn("The proofreader of a rich text editor answered {} lists of issues for {} texts",
				found == null ? null : found.size(), texts.size());
			throw new Refusal("The texts could not be checked");
		}
		JSONArray issues = new JSONArray();
		for (int index = 0; index < texts.size(); index++)
		{
			int length = texts.get(index).length();
			JSONArray ofText = new JSONArray();
			List<RichTextIssue> ofProofreader = found.get(index);
			if (ofProofreader != null)
			{
				ofProofreader.stream()
					.filter(issue -> issue != null && issue.to() <= length)
					.forEach(issue -> ofText.put(issue.toJson()));
			}
			issues.put(ofText);
		}
		return issues;
	}

	private List<String> texts(String json) throws Refusal
	{
		if (json == null)
		{
			throw new Refusal("No texts");
		}
		JSONArray array;
		try
		{
			array = new JSONArray(json);
		}
		catch (JSONException e)
		{
			throw new Refusal("The texts are no JSON array");
		}
		if (array.length() > maxTexts)
		{
			throw new Refusal("Too many texts: " + array.length() + ", at most " + maxTexts);
		}
		List<String> texts = new ArrayList<>(array.length());
		long length = 0;
		for (int index = 0; index < array.length(); index++)
		{
			if (!(array.opt(index) instanceof String text))
			{
				throw new Refusal("A text is no string");
			}
			length += text.length();
			if (length > maxLength)
			{
				throw new Refusal("The texts are too long: at most " + maxLength + " characters");
			}
			texts.add(text);
		}
		return texts;
	}

	/**
	 * @return the language of a tag of BCP 47, {@code null} for none or no such tag
	 */
	private static Locale language(String tag)
	{
		if (tag == null || !LANGUAGE_TAG.matcher(tag.trim()).matches())
		{
			return null;
		}
		Locale locale = Locale.forLanguageTag(tag.trim());
		return locale.getLanguage().isEmpty() ? null : locale;
	}

	/**
	 * Why the texts were not checked, which the answer tells.
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
