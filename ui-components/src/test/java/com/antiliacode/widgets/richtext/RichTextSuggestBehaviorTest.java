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

import java.util.ArrayList;
import java.util.List;
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

class RichTextSuggestBehaviorTest extends WicketTestCase
{
	private static final List<RichTextSuggestion> USERS = List.of(
		new RichTextSuggestion("1", "Ann", "ann@example.org", null),
		RichTextSuggestion.of("2", "Andrew"), RichTextSuggestion.of("3", "Bob"));

	/** What the suggester of users was asked: the query and the most items. */
	private static final List<String> ASKED = new ArrayList<>();

	private static List<RichTextSuggestion> users(String query, int max)
	{
		ASKED.add(query + " " + max);
		return USERS.stream().filter(user -> user.label().toLowerCase().startsWith(
			query.toLowerCase())).limit(max).toList();
	}

	@Test
	void answersTheItemsOfASuggesterByItsName()
	{
		ASKED.clear();
		TestPage page = new TestPage(RichTextSuggester.mentions("users", "@",
			RichTextSuggestBehaviorTest::users), RichTextSuggester.words("words",
			List.of("example", "Excellent", "editor")));
		tester.startPage(page);

		assertEquals("{\"items\":[{\"id\":\"1\",\"label\":\"Ann\",\"detail\":\"ann@example.org\"}," +
			"{\"id\":\"2\",\"label\":\"Andrew\"}]}", suggest(page.suggesting, "users", "an")
			.toString());
		assertEquals(List.of("an 10"), ASKED);
		assertTrue(tester.getLastResponse().getContentType().startsWith("application/json"));
		assertEquals("{\"items\":[{\"id\":\"0\",\"label\":\"example\",\"text\":\"example\"}," +
			"{\"id\":\"1\",\"label\":\"Excellent\",\"text\":\"Excellent\"}]}",
			suggest(page.suggesting, "words", "ex").toString());

		page.suggesting.setMaxItems(1);
		assertEquals(1, suggest(page.suggesting, "users", null).getJSONArray("items").length());
		assertEquals(List.of("an 10", " 1"), ASKED);

		assertEquals("[{\"name\":\"users\",\"insert\":\"mention\",\"char\":\"@\"}," +
			"{\"name\":\"words\",\"insert\":\"text\"}]", page.suggesting.toJson().toString());
	}

	@Test
	void refusesWhatItDoesNotKnow()
	{
		TestPage page = new TestPage(RichTextSuggester.mentions("users", "@",
			RichTextSuggestBehaviorTest::users), RichTextSuggester.mentions("broken", "#",
			(query, max) -> {
				throw new IllegalStateException("down");
			}));
		tester.startPage(page);

		assertEquals("No suggester is named tags", suggest(page.suggesting, "tags", "a")
			.getString("error"));
		assertEquals("The query is too long: at most 50 characters", suggest(page.suggesting,
			"users", "a".repeat(51)).getString("error"));
		assertEquals("Nothing could be found", suggest(page.suggesting, "broken", "a")
			.getString("error"));

		tester.getRequest().setMethod("GET");
		tester.getRequest().setParameter(RichTextSuggestBehavior.NAME_PARAMETER, "users");
		tester.executeBehavior(page.suggesting);
		assertEquals("Post the query", new JSONObject(tester.getLastResponseAsString())
			.getString("error"));

		assertThrows(IllegalArgumentException.class, () -> page.suggesting.add(
			RichTextSuggester.words("users", List.of())));
		assertThrows(IllegalArgumentException.class, () -> page.suggesting.setMaxItems(0));
	}

	@Test
	void offersTheItemsOfTheEditorItIsOf()
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of("<p>Text</p>"));
		assertNull(editor.getSuggestBehavior());
		assertFalse(editor.isSuggesting());
		RichTextSuggester users = RichTextSuggester.mentions("users", "@",
			RichTextSuggestBehaviorTest::users);
		editor.addSuggester(users).addSuggester(RichTextSuggester.words("words", List.of("a")));
		EditorPage page = new EditorPage(editor);
		tester.startPage(page);
		RichTextSuggestBehavior behavior = editor.getSuggestBehavior();
		assertSame(behavior, editor.getBehaviors(RichTextSuggestBehavior.class).get(0));
		assertTrue(editor.isSuggesting());
		JSONObject options = options(editor);
		assertTrue(options.getString("suggestUrl").endsWith("-form-editor"));
		assertEquals(behavior.toJson().toString(), options.getJSONArray("suggesters").toString());
		assertEquals("Nothing found", options.getJSONObject("texts").getString("suggestNone"));

		assertEquals(3, suggest(behavior, "users", "").getJSONArray("items").length());
		editor.setReadOnly(true);
		assertEquals("The editor offers nothing now", suggest(behavior, "users", "")
			.getString("error"));
		editor.setReadOnly(false);
		editor.removeSuggester(users);
		assertEquals(1, editor.getSuggesters().size());
		assertTrue(suggest(behavior, "users", "").has("error"));
	}

	@Test
	void makesSuggestersOfANameAndOneCharacter()
	{
		RichTextSuggester.Source none = (query, max) -> List.of();
		assertThrows(IllegalArgumentException.class, () -> RichTextSuggester.mentions("my users",
			"@", none));
		assertThrows(IllegalArgumentException.class, () -> RichTextSuggester.mentions("users",
			"@@", none));
		assertThrows(IllegalArgumentException.class, () -> RichTextSuggester.mentions("users", "a",
			none));
		assertThrows(IllegalArgumentException.class, () -> RichTextSuggester.mentions("users", " ",
			none));
		RichTextSuggester completions = RichTextSuggester.completions("words", none);
		assertNull(completions.getTrigger());
		assertEquals(RichTextSuggestionInsert.TEXT, completions.getInsert());
		IRichTextSuggester own = new IRichTextSuggester()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getName()
			{
				return "tags";
			}

			@Override
			public String getTrigger()
			{
				return "#";
			}

			@Override
			public List<RichTextSuggestion> suggest(String query, int max)
			{
				return List.of();
			}
		};
		assertEquals(RichTextSuggestionInsert.MENTION, own.getInsert());
		assertThrows(IllegalArgumentException.class, () -> RichTextSuggestion.of("a b", "A"));
		assertThrows(IllegalArgumentException.class, () -> RichTextSuggestion.of("1", ""));
		assertEquals("ann@example.org", RichTextSuggestion.of("ann@example.org", "Ann").id());
	}

	@Test
	void keepsMentionsInTheTextAndReadsThem()
	{
		String cleaned = RichTextPolicy.ALL.sanitize("<p>Hi <span class=\"antilia-mention\" " +
			"data-mention=\"users\" data-id=\"ann@example.org\">@Ann &amp; co</span>, " +
			"<span class=\"antilia-mention\" data-mention=\"bad kind\" data-id=\"2\" " +
			"onclick=\"steal()\">@Bob</span> and <span data-id=\"x y\">x</span></p>");
		assertEquals("<p>Hi <span class=\"antilia-mention\" data-mention=\"users\" " +
			"data-id=\"ann&#64;example.org\">&#64;Ann &amp; co</span>, <span " +
			"class=\"antilia-mention\" data-id=\"2\">&#64;Bob</span> and <span>x</span></p>",
			cleaned);
		assertEquals(List.of(new RichTextMention("users", "ann@example.org", "@Ann & co")),
			RichTextMention.of(cleaned));
		assertEquals(List.of(new RichTextMention("tags", "7", "#\u00e9t\u00e9")),
			RichTextMention.of("<span data-id='7' class='x antilia-mention' " +
				"data-mention='tags'>#&#xe9;t&eacute;</span>".replace("&eacute;", "&#233;")));
		assertEquals(List.of(), RichTextMention.of(null));

		// Markdown has no mentions
		assertFalse(RichTextFeature.MENTION.isInMarkdown());
		assertEquals("<p>Hi &#64;Ann</p>", RichTextPolicy.of(List.of(RichTextFeature.BOLD))
			.sanitize("<p>Hi <span class=\"antilia-mention\" data-mention=\"users\" " +
				"data-id=\"1\">@Ann</span></p>"));
	}

	/**
	 * Posts a query to a behavior, as the editor does, and reads the answer.
	 */
	private JSONObject suggest(RichTextSuggestBehavior behavior, String name, String query)
	{
		tester.getRequest().setMethod("POST");
		tester.getRequest().getPostParameters().setParameterValue(
			RichTextSuggestBehavior.NAME_PARAMETER, name);
		if (query != null)
		{
			tester.getRequest().getPostParameters().setParameterValue(
				RichTextSuggestBehavior.QUERY_PARAMETER, query);
		}
		tester.executeBehavior(behavior);
		return new JSONObject(tester.getLastResponseAsString());
	}

	private JSONObject options(RichTextEditor editor)
	{
		Matcher matcher = Pattern.compile("Wicket\\.RichTextEditor\\.attach\\(\"" +
			Pattern.quote(editor.getMarkupId()) + "\", (\\{.*\\})\\);").matcher(
				tester.getLastResponseAsString());
		assertTrue(matcher.find(), "the editor is attached");
		return new JSONObject(matcher.group(1));
	}

	/** A page offering items for the editors of React and Svelte in it. */
	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final RichTextSuggestBehavior suggesting;

		TestPage(IRichTextSuggester... suggesters)
		{
			suggesting = new RichTextSuggestBehavior(suggesters);
			add(suggesting);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body></body></html>");
		}
	}

	/** A page with an editor offering items. */
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
