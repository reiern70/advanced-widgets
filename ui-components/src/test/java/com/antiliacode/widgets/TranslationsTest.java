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
package com.antiliacode.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Every bundle of the components' texts speaks every language the library ships: each
 * translation has exactly the keys of the English one, none missing and none left over, and each
 * text keeps the placeholders of its English one. A text added in English only fails here, so
 * that it does not reach only some of the languages.
 */
class TranslationsTest
{
	/** The languages each bundle is translated to, besides the English of the default one. */
	static final List<String> LANGUAGES = List.of("es", "de", "fr");

	private static final String SUFFIX = ".properties.xml";

	/** A bundle's translation: its name ends in an underscore and a language. */
	private static final Pattern TRANSLATION = Pattern
		.compile(".*_([a-z]{2})(_[A-Z]{2})?\\" + SUFFIX);

	/**
	 * The placeholders a text is filled in with: ${name} by Wicket, {0} by the components, and the
	 * lone $ of CodeMirror's phrases.
	 */
	private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{[^}]+}|\\{\\d+}|\\$");

	private static final Path SOURCES = Path.of("src", "main", "java");

	@Test
	void theLibraryHasItsBundles() throws IOException
	{
		List<Path> bundles = bundles();

		assertTrue(bundles.size() >= 6, "bundles: " + bundles);
		assertTrue(bundles.contains(SOURCES.resolve(
			"com/antiliacode/widgets/Initializer" + SUFFIX)), "bundles: " + bundles);
	}

	@Test
	void everyBundleIsTranslatedToEveryLanguage() throws IOException
	{
		List<String> missing = new ArrayList<>();
		for (Path bundle : bundles())
		{
			for (String language : LANGUAGES)
			{
				if (!Files.exists(translation(bundle, language)))
				{
					missing.add(SOURCES.relativize(translation(bundle, language)).toString());
				}
			}
		}
		assertEquals(List.of(), missing);
	}

	@Test
	void noTranslationIsLeftWithoutItsBundle() throws IOException
	{
		try (Stream<Path> files = Files.walk(SOURCES))
		{
			List<Path> orphans = files
				.filter(file -> TRANSLATION.matcher(file.getFileName().toString()).matches())
				.filter(file -> !Files.exists(bundleOf(file)))
				.toList();
			assertEquals(List.of(), orphans);
		}
	}

	@Test
	void everyTranslationHasExactlyTheKeysOfItsBundle() throws IOException
	{
		List<String> problems = new ArrayList<>();
		for (Path bundle : bundles())
		{
			Set<String> keys = load(bundle).stringPropertyNames();
			for (String language : LANGUAGES)
			{
				Path translation = translation(bundle, language);
				Set<String> translated = load(translation).stringPropertyNames();

				Set<String> missing = new TreeSet<>(keys);
				missing.removeAll(translated);
				Set<String> extra = new TreeSet<>(translated);
				extra.removeAll(keys);
				if (!missing.isEmpty())
				{
					problems.add(SOURCES.relativize(translation) + " misses " + missing);
				}
				if (!extra.isEmpty())
				{
					problems.add(SOURCES.relativize(translation) + " has more than English: " + extra);
				}
			}
		}
		assertEquals(List.of(), problems);
	}

	@Test
	void everyTextKeepsThePlaceholdersOfItsEnglishOne() throws IOException
	{
		List<String> problems = new ArrayList<>();
		for (Path bundle : bundles())
		{
			Properties english = load(bundle);
			for (String language : LANGUAGES)
			{
				Path translation = translation(bundle, language);
				Properties translated = load(translation);
				for (String key : new TreeSet<>(english.stringPropertyNames()))
				{
					String text = translated.getProperty(key);
					if (text != null &&
						!placeholders(english.getProperty(key)).equals(placeholders(text)))
					{
						problems.add(SOURCES.relativize(translation) + " " + key + ": " +
							placeholders(text) + " instead of " +
							placeholders(english.getProperty(key)));
					}
				}
			}
		}
		assertEquals(List.of(), problems);
	}

	@Test
	void noTranslationIsLeftEmpty() throws IOException
	{
		List<String> empty = new ArrayList<>();
		for (Path bundle : bundles())
		{
			for (String language : LANGUAGES)
			{
				Path translation = translation(bundle, language);
				Properties translated = load(translation);
				translated.stringPropertyNames()
					.stream()
					.filter(key -> translated.getProperty(key).isBlank())
					.sorted()
					.forEach(key -> empty.add(SOURCES.relativize(translation) + " " + key));
			}
		}
		assertEquals(List.of(), empty);
	}

	@Test
	void findsThePlaceholders()
	{
		assertEquals(List.of("${a}", "${b}", "{0}", "{1}"),
			placeholders("{1} and ${b}, {0} and ${a}"));
		assertEquals(List.of("$"), placeholders("replaced $ matches"));
		assertFalse(placeholders("no placeholder {here}").contains("{here}"));
	}

	/**
	 * @return the default bundles of the library's sources: the English ones, without a language
	 *         in their name
	 */
	static List<Path> bundles() throws IOException
	{
		try (Stream<Path> files = Files.walk(SOURCES))
		{
			return files.filter(file -> file.getFileName().toString().endsWith(SUFFIX))
				.filter(file -> !TRANSLATION.matcher(file.getFileName().toString()).matches())
				.sorted()
				.toList();
		}
	}

	private static Path translation(Path bundle, String language)
	{
		String name = bundle.getFileName().toString();
		return bundle.resolveSibling(name.substring(0, name.length() - SUFFIX.length()) + "_" +
			language + SUFFIX);
	}

	private static Path bundleOf(Path translation)
	{
		String name = translation.getFileName().toString();
		return translation.resolveSibling(name.substring(0, name.lastIndexOf('_')) + SUFFIX);
	}

	private static Properties load(Path file) throws IOException
	{
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(file))
		{
			properties.loadFromXML(in);
		}
		catch (IOException e)
		{
			fail(file + " cannot be read: " + e.getMessage());
		}
		return properties;
	}

	private static List<String> placeholders(String text)
	{
		List<String> found = new ArrayList<>();
		Matcher matcher = PLACEHOLDER.matcher(text);
		while (matcher.find())
		{
			found.add(matcher.group());
		}
		found.sort(null);
		return found;
	}
}
