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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONObject;

class RichTextProofreadBehaviorTest extends WicketTestCase
{
	/** The languages the proofreader of the tests was asked in. */
	private static final List<Locale> LANGUAGES = new ArrayList<>();

	/**
	 * A proofreader of the tests: "teh" is spelled wrong; it answers one issue out of its text too,
	 * which the behavior drops.
	 */
	private static List<List<RichTextIssue>> words(List<String> texts, Locale language)
	{
		LANGUAGES.add(language);
		List<List<RichTextIssue>> found = new ArrayList<>();
		for (String text : texts)
		{
			List<RichTextIssue> issues = new ArrayList<>();
			Matcher matcher = Pattern.compile("\\bteh\\b").matcher(text);
			while (matcher.find())
			{
				issues.add(new RichTextIssue(matcher.start(), matcher.end(),
					RichTextIssueKind.SPELLING, "Possible spelling mistake", List.of("the", "ten"),
					"SPELLING"));
			}
			issues.add(RichTextIssue.spelling(text.length(), text.length() + 4, "out"));
			found.add(issues);
		}
		return found;
	}

	@Test
	void answersTheIssuesOfEachTextWithinIt()
	{
		LANGUAGES.clear();
		TestPage page = new TestPage(RichTextProofreadBehaviorTest::words);
		tester.startPage(page);

		JSONObject answer = check(page.proofreading, "[\"I saw teh cat\",\"Fine\",\"teh\"]", "en-GB");
		assertEquals("{\"issues\":[[{\"from\":6,\"to\":9,\"kind\":\"spelling\"," +
			"\"message\":\"Possible spelling mistake\",\"replacements\":[\"the\",\"ten\"]," +
			"\"rule\":\"SPELLING\"}],[],[{\"from\":0,\"to\":3,\"kind\":\"spelling\"," +
			"\"message\":\"Possible spelling mistake\",\"replacements\":[\"the\",\"ten\"]," +
			"\"rule\":\"SPELLING\"}]]}", answer.toString());
		assertEquals(List.of(Locale.UK), LANGUAGES);
		assertTrue(tester.getLastResponse().getContentType().startsWith("application/json"));

		// no language, or none of BCP 47: the proofreader finds it
		check(page.proofreading, "[\"a\"]", null);
		check(page.proofreading, "[\"a\"]", "<script>");
		assertEquals(List.of(Locale.UK), LANGUAGES.subList(0, 1));
		assertNull(LANGUAGES.get(1));
		assertNull(LANGUAGES.get(2));

		assertEquals("{\"issues\":[]}", check(page.proofreading, "[]", "en").toString());
		assertEquals(3, LANGUAGES.size());
	}

	@Test
	void refusesWhatIsNoListOfTextsOrTooMuch()
	{
		TestPage page = new TestPage(RichTextProofreadBehaviorTest::words);
		tester.startPage(page);

		assertEquals("No texts", check(page.proofreading, null, "en").getString("error"));
		assertEquals("The texts are no JSON array", check(page.proofreading, "{\"a\":1}", "en")
			.getString("error"));
		assertEquals("A text is no string", check(page.proofreading, "[\"a\",1]", "en")
			.getString("error"));
		page.proofreading.setMaxTexts(2).setMaxLength(5);
		assertEquals("Too many texts: 3, at most 2", check(page.proofreading, "[\"a\",\"b\",\"c\"]",
			"en").getString("error"));
		assertEquals("The texts are too long: at most 5 characters", check(page.proofreading,
			"[\"abc\",\"def\"]", "en").getString("error"));
		assertTrue(check(page.proofreading, "[\"abc\",\"de\"]", "en").has("issues"));

		tester.getRequest().setMethod("GET");
		tester.getRequest().setParameter(RichTextProofreadBehavior.TEXTS_PARAMETER, "[\"a\"]");
		tester.executeBehavior(page.proofreading);
		assertEquals("Post the texts", new JSONObject(tester.getLastResponseAsString())
			.getString("error"));
	}

	@Test
	void tellsOfAProofreaderFailing()
	{
		TestPage failing = new TestPage((texts, language) -> {
			throw new IOException("LanguageTool answered 429");
		});
		tester.startPage(failing);
		assertEquals("The texts could not be checked", check(failing.proofreading, "[\"a\"]", "en")
			.getString("error"));

		TestPage wrong = new TestPage((texts, language) -> List.of());
		tester.startPage(wrong);
		assertEquals("The texts could not be checked", check(wrong.proofreading, "[\"a\"]", "en")
			.getString("error"));
	}

	@Test
	void checksTheTextsOfTheEditorItIsOfInItsLanguage()
	{
		LANGUAGES.clear();
		RichTextEditor editor = new RichTextEditor("editor", Model.of("<p>Text</p>"));
		assertNull(editor.getProofreadBehavior());
		assertFalse(editor.isProofreading());
		editor.setProofreader(RichTextProofreadBehaviorTest::words);
		EditorPage page = new EditorPage(editor);
		tester.startPage(page);
		RichTextProofreadBehavior behavior = editor.getProofreadBehavior();
		assertSame(behavior, editor.getBehaviors(RichTextProofreadBehavior.class).get(0));
		assertTrue(editor.isProofreading());

		assertEquals(1, check(behavior, "[\"teh\"]", "de").getJSONArray("issues").getJSONArray(0)
			.length());
		editor.setTextLanguage(Locale.of("es"));
		check(behavior, "[\"teh\"]", "de");
		assertEquals(List.of(Locale.GERMAN, Locale.of("es")), LANGUAGES);

		// read only, or without a proofreader, the editor checks nothing
		editor.setReadOnly(true);
		assertEquals("The editor checks no texts now", check(behavior, "[\"teh\"]", "en")
			.getString("error"));
		editor.setReadOnly(false);
		editor.setProofreader(null);
		assertTrue(check(behavior, "[\"teh\"]", "en").has("error"));
		assertEquals(2, LANGUAGES.size());

		// one behavior for any proofreader
		editor.setProofreader((texts, language) -> List.of(List.of()));
		assertSame(behavior, editor.getProofreadBehavior());
		assertEquals("{\"issues\":[[]]}", check(behavior, "[\"teh\"]", "en").toString());
	}

	@Test
	void hasLimitsOfItsOwn()
	{
		RichTextProofreadBehavior behavior = new RichTextProofreadBehavior((texts, language) ->
			List.of());
		assertEquals(RichTextProofreadBehavior.DEFAULT_MAX_TEXTS, behavior.getMaxTexts());
		assertEquals(RichTextProofreadBehavior.DEFAULT_MAX_LENGTH, behavior.getMaxLength());
		assertFalse(behavior.rendersPage());
		assertThrows(IllegalArgumentException.class, () -> behavior.setMaxTexts(0));
		assertThrows(IllegalArgumentException.class, () -> behavior.setMaxLength(0));
		assertThrows(IllegalArgumentException.class, () -> new RichTextProofreadBehavior(null));
	}

	@Test
	void writesAnIssueAsTheBrowserTakesIt()
	{
		assertEquals("{\"from\":1,\"to\":2,\"kind\":\"style\"}", new RichTextIssue(1, 2,
			RichTextIssueKind.STYLE, null, null, null).toJson().toString());
		assertEquals(List.of(), new RichTextIssue(1, 2, RichTextIssueKind.GRAMMAR, null, null, null)
			.replacements());
		assertThrows(IllegalArgumentException.class, () -> RichTextIssue.spelling(2, 2));
		assertThrows(IllegalArgumentException.class, () -> RichTextIssue.spelling(-1, 2));
		assertThrows(IllegalArgumentException.class, () -> new RichTextIssue(0, 1, null, null,
			null, null));
	}

	/**
	 * Posts texts to a behavior, as the editor does, and reads the answer.
	 */
	private JSONObject check(RichTextProofreadBehavior behavior, String texts, String language)
	{
		tester.getRequest().setMethod("POST");
		if (texts != null)
		{
			tester.getRequest().getPostParameters().setParameterValue(
				RichTextProofreadBehavior.TEXTS_PARAMETER, texts);
		}
		if (language != null)
		{
			tester.getRequest().getPostParameters().setParameterValue(
				RichTextProofreadBehavior.LANGUAGE_PARAMETER, language);
		}
		tester.executeBehavior(behavior);
		return new JSONObject(tester.getLastResponseAsString());
	}

	/** A page checking texts for the editors of React and Svelte in it. */
	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final RichTextProofreadBehavior proofreading;

		TestPage(IRichTextProofreader proofreader)
		{
			proofreading = new RichTextProofreadBehavior(proofreader);
			add(proofreading);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body></body></html>");
		}
	}

	/** A page with an editor checking its text. */
	public static class EditorPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		EditorPage(RichTextEditor editor)
		{
			Form<Void> form = new Form<>("form");
			form.add(editor);
			add(form);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id=\"form\">" +
				"<textarea wicket:id=\"editor\"></textarea></form></body></html>");
		}
	}
}
