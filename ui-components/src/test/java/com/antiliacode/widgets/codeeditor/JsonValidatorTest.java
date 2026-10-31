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
package com.antiliacode.widgets.codeeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.ValidationError;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.codeeditor.CodeEditorTest.TestPage;
import com.antiliacode.widgets.codeeditor.JsonValidator.JsonError;
import com.antiliacode.widgets.codeeditor.JsonValidator.Problem;

class JsonValidatorTest extends WicketTestCase
{
	@Test
	void findsNoErrorInWellFormedJson()
	{
		for (String text : List.of("{}", "[]", "\"x\"", "0", "-0.5e+10", "1E-2", "true", "false",
			"null", "  [ ]  \r\n", "{\"a\": [1, 2.5, {\"b\": null}], \"c\": \"\\u00e9\\n\\\"\\/\"}",
			"[\"\uD83D\uDE00\", \"\u00e9\"]"))
		{
			assertNull(JsonValidator.findError(text), text);
		}
	}

	@Test
	void findsWhereTheTextEndsTooEarly()
	{
		assertEquals(new JsonError(Problem.END, 0, 1, 1, null), JsonValidator.findError(""));
		assertEquals(new JsonError(Problem.END, 3, 1, 4, null), JsonValidator.findError("   "));
		assertEquals(new JsonError(Problem.END, 6, 1, 7, null), JsonValidator.findError(
			"{\"a\":1"));
		for (String text : List.of("\"abc", "[1,", "{\"a\"", "{\"a\":", "\"\\u12", "\"\\", "-",
			"1.", "1e", "1e+", "[", "{"))
		{
			JsonError error = JsonValidator.findError(text);
			assertEquals(Problem.END, error.problem(), text);
			assertEquals(text.length(), error.offset(), text);
			assertNull(error.found(), text);
		}
	}

	@Test
	void findsWhatWasExpectedInstead()
	{
		assertEquals(new JsonError(Problem.VALUE, 3, 1, 4, "']'"), JsonValidator.findError(
			"[1,]"));
		assertEquals(new JsonError(Problem.VALUE, 5, 1, 6, "'}'"), JsonValidator.findError(
			"{\"a\":}"));
		assertEquals(new JsonError(Problem.VALUE, 0, 1, 1, "'t'"), JsonValidator.findError(
			"tru"));
		assertEquals(new JsonError(Problem.VALUE, 1, 1, 2, "'''"), JsonValidator.findError(
			"['a']"));
		assertEquals(new JsonError(Problem.NAME, 1, 1, 2, "'a'"), JsonValidator.findError(
			"{a:1}"));
		assertEquals(new JsonError(Problem.NAME, 7, 1, 8, "'}'"), JsonValidator.findError(
			"{\"a\":1,}"));
		assertEquals(new JsonError(Problem.COLON, 5, 1, 6, "'1'"), JsonValidator.findError(
			"{\"a\" 1}"));
		assertEquals(new JsonError(Problem.OBJECT_NEXT, 7, 1, 8, "'\"'"), JsonValidator.findError(
			"{\"a\":1 \"b\":2}"));
		assertEquals(new JsonError(Problem.ARRAY_NEXT, 3, 1, 4, "'2'"), JsonValidator.findError(
			"[1 2]"));
		// a number does not start with 0
		assertEquals(new JsonError(Problem.ARRAY_NEXT, 2, 1, 3, "'1'"), JsonValidator.findError(
			"[01]"));
		assertEquals(new JsonError(Problem.TRAILING, 3, 1, 4, "'x'"), JsonValidator.findError(
			"{} x"));
		assertEquals(new JsonError(Problem.TRAILING, 2, 1, 3, "'2'"), JsonValidator.findError(
			"1 2"));
	}

	@Test
	void findsWrongStringsAndNumbers()
	{
		assertEquals(new JsonError(Problem.STRING, 2, 1, 3, "U+000A"), JsonValidator.findError(
			"\"a\nb\""));
		assertEquals(new JsonError(Problem.STRING, 2, 1, 3, "U+0009"), JsonValidator.findError(
			"\"a\tb\""));
		assertEquals(new JsonError(Problem.STRING, 2, 1, 3, "'x'"), JsonValidator.findError(
			"\"\\x\""));
		assertEquals(new JsonError(Problem.STRING, 5, 1, 6, "'G'"), JsonValidator.findError(
			"\"\\u12G4\""));
		assertEquals(new JsonError(Problem.NUMBER, 1, 1, 2, "'a'"), JsonValidator.findError(
			"-a"));
		assertEquals(new JsonError(Problem.NUMBER, 2, 1, 3, "'x'"), JsonValidator.findError(
			"1.x"));
		assertEquals(new JsonError(Problem.NUMBER, 3, 1, 4, "'x'"), JsonValidator.findError(
			"1e+x"));
		// what is found shows control characters and those beyond the basic plane as they are
		assertEquals("U+007F", JsonValidator.findError("[\u007f]").found());
		assertEquals("'\uD83D\uDE00'", JsonValidator.findError("[\uD83D\uDE00]").found());
	}

	@Test
	void countsTheLinesByTheirNewLines()
	{
		assertEquals(new JsonError(Problem.OBJECT_NEXT, 10, 2, 9, "']'"), JsonValidator.findError(
			"{\n  \"a\": 1]\n}"));
		// as the browser posts a text area: the line ends in \r\n
		assertEquals(new JsonError(Problem.COLON, 7, 2, 5, "'1'"), JsonValidator.findError(
			"{\r\n\"a\" 1}"));
	}

	@Test
	void readsAnyDepthWithoutOverflowingTheStack()
	{
		int depth = 100_000;
		assertEquals(new JsonError(Problem.END, depth, 1, depth + 1, null),
			JsonValidator.findError("[".repeat(depth)));
		assertNull(JsonValidator.findError("[".repeat(depth) + "]".repeat(depth)));
		assertNull(JsonValidator.findError("{\"a\":".repeat(depth) + "1" + "}".repeat(depth)));
	}

	@Test
	void reportsTheProblemWithWhereItIs()
	{
		Validatable<String> validatable = new Validatable<>("[1,]");
		new JsonValidator().validate(validatable);
		List<IValidationError> errors = validatable.getErrors();
		assertEquals(1, errors.size());
		ValidationError error = (ValidationError)errors.get(0);
		assertEquals(List.of("JsonValidator.value"), error.getKeys());
		assertEquals(1, error.getVariables().get(CodeEditor.LINE));
		assertEquals(4, error.getVariables().get(CodeEditor.COLUMN));
		assertEquals("']'", error.getVariables().get(JsonValidator.FOUND));

		// the end has nothing to show; a subclass has the keys of the validator
		validatable = new Validatable<>("[1,");
		new JsonValidator()
		{
			private static final long serialVersionUID = 1L;
		}.validate(validatable);
		error = (ValidationError)validatable.getErrors().get(0);
		assertEquals(List.of("JsonValidator.end"), error.getKeys());
		assertFalse(error.getVariables().containsKey(JsonValidator.FOUND));

		validatable = new Validatable<>("{\"a\": true}");
		new JsonValidator().validate(validatable);
		assertTrue(validatable.isValid());
		validatable = new Validatable<>(null);
		new JsonValidator().validate(validatable);
		assertTrue(validatable.isValid());
	}

	@Test
	void saysWhatIsWrongInTheLanguageOfThePage()
	{
		assertEquals("editor, line 2, column 9: ',' or '}' was expected, not ']'.",
			messageOf("{\n  \"a\": 1]\n}", Locale.ENGLISH));
		assertEquals("editor, line 1, column 4: the JSON ends too early.",
			messageOf("[1,", Locale.ENGLISH));
		assertEquals("editor, Zeile 2, Spalte 9: ',' oder '}' wurde erwartet, nicht ']'.",
			messageOf("{\n  \"a\": 1]\n}", Locale.GERMAN));
		assertEquals("editor, línea 1, columna 3: U+000A no se admite aquí en una cadena.",
			messageOf("[\"\n\"]", Locale.of("es")));
	}

	/**
	 * @return the message of the error a form sent with a text finds, in a language
	 */
	private String messageOf(String text, Locale locale)
	{
		tester.getSession().setLocale(locale);
		CodeEditor editor = new CodeEditor("editor", Model.of("[]"));
		editor.add(new JsonValidator());
		tester.startPage(new TestPage(editor));
		FormTester form = tester.newFormTester("form");
		form.setValue("editor", text);
		form.submit();
		List<CodeEditorError> errors = editor.getErrors();
		assertEquals(1, errors.size(), text);
		return errors.get(0).message();
	}
}
