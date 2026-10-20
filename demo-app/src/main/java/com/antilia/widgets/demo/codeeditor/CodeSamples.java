/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.demo.codeeditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.request.resource.JavaScriptResourceReference;

import com.antilia.widgets.codeeditor.CodeEditorLanguage;

/**
 * What the code editor demo shows: a sample in each language, notes in Markdown beside their
 * preview, and the files of a small project.
 */
final class CodeSamples
{
	/** The grammar of a log, which the page plugs in with a script of its own. */
	static final CodeEditorLanguage LOG = CodeEditorLanguage.of("log",
		new JavaScriptResourceReference(CodeSamples.class, "log-grammar.js"));

	/** The languages of the editor, in the order of the drop-down. */
	static final List<CodeEditorLanguage> LANGUAGES = List.of(CodeEditorLanguage.JAVA,
		CodeEditorLanguage.JAVASCRIPT, CodeEditorLanguage.TYPESCRIPT, CodeEditorLanguage.JSON,
		CodeEditorLanguage.MARKDOWN, LOG);

	private static final String JAVA = """
		package com.example.shop;

		import java.math.BigDecimal;
		import java.util.List;

		/**
		 * An order: its lines, and what they come to.
		 */
		public record Order(String customer, List<Line> lines)
		{
		    public record Line(String product, int quantity, BigDecimal price)
		    {
		        BigDecimal total()
		        {
		            return price.multiply(BigDecimal.valueOf(quantity));
		        }
		    }

		    public BigDecimal total()
		    {
		        return lines.stream()
		            .map(Line::total)
		            .reduce(BigDecimal.ZERO, BigDecimal::add);
		    }

		    public boolean isEmpty()
		    {
		        return lines.isEmpty(); // nothing ordered yet
		    }
		}
		""";

	private static final String JAVASCRIPT = """
		// Counts the words of a text, the most frequent first.
		export function countWords(text, top = 5) {
		    const counts = new Map();
		    for (const word of text.toLowerCase().match(/\\p{L}+/gu) ?? []) {
		        counts.set(word, (counts.get(word) ?? 0) + 1);
		    }
		    return [...counts]
		        .sort(([, a], [, b]) => b - a)
		        .slice(0, top)
		        .map(([word, count]) => `${word}: ${count}`);
		}

		console.log(countWords('the cat and the hat and the bat'));
		""";

	private static final String TYPESCRIPT = """
		/** A temperature, in the unit it was measured in. */
		interface Reading {
		    value: number;
		    unit: 'C' | 'F';
		    at: Date;
		}

		export function toCelsius(reading: Reading): number {
		    return reading.unit === 'C' ? reading.value : (reading.value - 32) * 5 / 9;
		}

		export function average(readings: readonly Reading[]): number | null {
		    if (!readings.length) {
		        return null;
		    }
		    const sum = readings.reduce((total, each) => total + toCelsius(each), 0);
		    return Math.round(sum / readings.length * 10) / 10;
		}
		""";

	private static final String JSON = """
		{
		    "name": "shop-front",
		    "version": "1.4.0",
		    "private": true,
		    "scripts": {
		        "build": "vite build",
		        "test": "vitest run"
		    },
		    "dependencies": {
		        "@antilia/react": "^1.0.0",
		        "react": "^19.0.0"
		    },
		    "browserslist": ["defaults", "not dead"],
		    "sideEffects": false
		}
		""";

	private static final String MARKDOWN = """
		# Shop front

		A **small** shop, built with *Antilia's Advanced Widgets*.

		## Getting started

		1. Install the dependencies: `npm install`
		2. Run it: `npm run dev`
		3. Open [the shop](http://localhost:5173)

		## An order in Java

		```java
		Order order = new Order("Ada", List.of(new Line("tea", 2, price)));
		BigDecimal total = order.total();
		```

		## Its settings

		```json
		{ "currency": "EUR", "taxes": true }
		```

		> Fenced code blocks take the grammar of their language.
		""";

	private static final String NOTES = """
		# Release notes

		The **shop** now shows *what you type* as you type it: edit on the left, read on the
		right, and find [the other demos](./) in a new tab.

		## What changed

		- [x] A code editor, in any language
		- [x] Markdown with a ~~separate~~ live preview
		- [ ] Coffee, still to do

		| Widget | Wicket | React | Svelte |
		|:--|:-:|:-:|:-:|
		| Code editor | ✓ | ✓ | ✓ |
		| Preview | ✓ | ✓ | ✓ |

		## The total of an order

		```java
		BigDecimal total = order.lines().stream()
		    .map(Line::total)
		    .reduce(BigDecimal.ZERO, BigDecimal::add);
		```

		> HTML such as <b>this</b> shows as text: the preview never runs what it shows.

		---

		1. Edit on the left
		2. Read on the right
		3. Save with `Ctrl+S`

		## Keeping the preview in step

		Tick *Keep the preview in step* and the preview follows the place you are at: move the
		cursor, type or scroll the editor, and the part of the preview it matches comes into view.
		Untick it, and each side scrolls on its own.

		### In a long text

		The longer the notes, the more it helps: on its own, the preview often shows another part
		than the one being edited; in step, the heading you write under stays in sight beside you.
		""";

	private static final String LOG_TEXT = """
		2026-10-14 08:12:03.114 INFO  [main] Starting the shop on port 8080
		2026-10-14 08:12:04.502 INFO  [main] Connected to "jdbc:postgresql://db/shop"
		2026-10-14 08:13:17.020 WARN  [http-3] Slow request: GET /orders took 1840 ms
		2026-10-14 08:14:45.866 ERROR [http-7] Order 4711 failed: IllegalStateException
		2026-10-14 08:14:45.867 DEBUG [http-7] Rolled back the transaction
		2026-10-14 08:15:00.000 INFO  [scheduler] Sent 12 confirmation emails
		""";

	private CodeSamples()
	{
	}

	/**
	 * @return a sample in each language, by the names of the languages
	 */
	static Map<String, CodeDocument> samples()
	{
		Map<String, CodeDocument> samples = new LinkedHashMap<>();
		add(samples, "Order.java", CodeEditorLanguage.JAVA, JAVA);
		add(samples, "words.js", CodeEditorLanguage.JAVASCRIPT, JAVASCRIPT);
		add(samples, "readings.ts", CodeEditorLanguage.TYPESCRIPT, TYPESCRIPT);
		add(samples, "package.json", CodeEditorLanguage.JSON, JSON);
		add(samples, "README.md", CodeEditorLanguage.MARKDOWN, MARKDOWN);
		add(samples, "shop.log", LOG, LOG_TEXT);
		return samples;
	}

	private static void add(Map<String, CodeDocument> samples, String name,
		CodeEditorLanguage language, String text)
	{
		samples.put(language.getName(), new CodeDocument(name, language, text));
	}

	/**
	 * @return the notes in Markdown the editor beside its preview shows
	 */
	static CodeDocument notes()
	{
		return new CodeDocument("NOTES.md", CodeEditorLanguage.MARKDOWN, NOTES);
	}

	/**
	 * @return the files of the project the tabs show
	 */
	static List<CodeDocument> files()
	{
		return List.of(new CodeDocument("Order.java", CodeEditorLanguage.JAVA, JAVA),
			new CodeDocument("words.js", CodeEditorLanguage.JAVASCRIPT, JAVASCRIPT),
			new CodeDocument("package.json", CodeEditorLanguage.JSON, JSON),
			new CodeDocument("README.md", CodeEditorLanguage.MARKDOWN, MARKDOWN));
	}
}
