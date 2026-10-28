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
package com.antiliacode.widgets.demo.richtext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.antiliacode.widgets.richtext.IRichTextProofreader;
import com.antiliacode.widgets.richtext.RichTextIssue;
import com.antiliacode.widgets.richtext.RichTextIssueKind;
import com.antiliacode.widgets.richtext.RichTextSuggester;
import com.antiliacode.widgets.richtext.RichTextSuggestion;

/**
 * What the writing case of the rich text page checks and suggests with, without a service: a
 * proofreader of a few words spelled wrong in English, German and Spanish, words repeated and two
 * blanks; the people to mention after {@code @}, the topics after {@code #} and the terms
 * completing a word on Ctrl+Space.
 */
final class RichTextWriting
{
	/** The words spelled wrong the proofreader knows, and their replacements. */
	private static final Map<String, List<String>> MISSPELLED = Map.ofEntries(
		Map.entry("teh", List.of("the")),
		Map.entry("recieve", List.of("receive")),
		Map.entry("seperate", List.of("separate")),
		Map.entry("definately", List.of("definitely")),
		Map.entry("occured", List.of("occurred")),
		Map.entry("wich", List.of("which", "witch")),
		Map.entry("accomodate", List.of("accommodate")),
		Map.entry("tommorow", List.of("tomorrow")),
		Map.entry("vieleicht", List.of("vielleicht")),
		Map.entry("nähmlich", List.of("nämlich")),
		Map.entry("wiederspiegeln", List.of("widerspiegeln")),
		Map.entry("aver", List.of("a ver", "haber")),
		Map.entry("nesesario", List.of("necesario")),
		Map.entry("ojala", List.of("ojalá")));

	private static final Pattern WORD = Pattern.compile("\\p{L}+");

	/** A word said twice. */
	private static final Pattern REPEATED = Pattern.compile("\\b(\\p{L}+) \\1\\b",
		Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern TWO_BLANKS = Pattern.compile("(?<=\\S)  +(?=\\S)");

	/** The people to mention: pioneers of computing, at example.org. */
	static final List<RichTextSuggestion> PEOPLE = List.of(
		person("ada", "Ada Lovelace"), person("alan", "Alan Turing"),
		person("grace", "Grace Hopper"), person("edsger", "Edsger Dijkstra"),
		person("barbara", "Barbara Liskov"), person("donald", "Donald Knuth"),
		person("margaret", "Margaret Hamilton"), person("dennis", "Dennis Ritchie"),
		person("frances", "Frances Allen"), person("tim", "Tim Berners-Lee"),
		person("radia", "Radia Perlman"), person("john", "John von Neumann"));

	/** The topics to mention after #. */
	static final List<String> TOPICS = List.of("accessibility", "design", "release", "review",
		"security", "testing", "wicket", "tiptap");

	/** The terms completing a word on Ctrl+Space. */
	static final List<String> TERMS = List.of("Apache Wicket", "accessibility", "component",
		"ProseMirror", "proofreader", "rich text editor", "suggestion", "Svelte", "React",
		"Tiptap", "TypeScript", "WicketTester");

	private RichTextWriting()
	{
	}

	private static RichTextSuggestion person(String id, String name)
	{
		return new RichTextSuggestion(id, name, id + "@example.org", null);
	}

	/**
	 * @return the people whose name, or a word of it, starts with the query
	 */
	static RichTextSuggester people()
	{
		return RichTextSuggester.mentions("people", "@", (query, max) -> {
			String start = query.toLowerCase(Locale.ROOT);
			return PEOPLE.stream().filter(person -> List.of(person.label().toLowerCase(Locale.ROOT)
				.split("[ -]")).stream().anyMatch(word -> word.startsWith(start)))
				.limit(max).toList();
		});
	}

	/**
	 * @return the topics starting with the query
	 */
	static RichTextSuggester topics()
	{
		return RichTextSuggester.mentions("topics", "#", (query, max) -> TOPICS.stream()
			.filter(topic -> topic.startsWith(query.toLowerCase(Locale.ROOT))).limit(max)
			.map(topic -> RichTextSuggestion.of(topic, topic)).toList());
	}

	/**
	 * @return the terms starting with the word before the cursor
	 */
	static RichTextSuggester terms()
	{
		return RichTextSuggester.words("terms", TERMS);
	}

	/**
	 * @param misspelled
	 *            what the proofreader says of a word spelled wrong
	 * @param repeated
	 *            of a word said twice
	 * @param blanks
	 *            of two blanks
	 * @return the proofreader of the demo
	 */
	static IRichTextProofreader proofreader(String misspelled, String repeated, String blanks)
	{
		return (texts, language) -> {
			List<List<RichTextIssue>> found = new ArrayList<>();
			for (String text : texts)
			{
				List<RichTextIssue> issues = new ArrayList<>();
				Matcher word = WORD.matcher(text);
				while (word.find())
				{
					List<String> replacements = MISSPELLED.get(word.group().toLowerCase(Locale.ROOT));
					if (replacements != null && !replacements.contains(word.group()))
					{
						issues.add(new RichTextIssue(word.start(), word.end(),
							RichTextIssueKind.SPELLING, misspelled, replacements, "DEMO_SPELLING"));
					}
				}
				Matcher twice = REPEATED.matcher(text);
				while (twice.find())
				{
					issues.add(new RichTextIssue(twice.start(), twice.end(), RichTextIssueKind.GRAMMAR,
						repeated, List.of(twice.group(1)), "DEMO_REPEATED"));
				}
				Matcher wide = TWO_BLANKS.matcher(text);
				while (wide.find())
				{
					issues.add(new RichTextIssue(wide.start(), wide.end(), RichTextIssueKind.STYLE,
						blanks, List.of(" "), "DEMO_BLANKS"));
				}
				found.add(issues);
			}
			return found;
		};
	}
}
