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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * Validates that a text is well-formed JSON, by the grammar of RFC 8259: strict, without comments,
 * single quotes or commas before a closing bracket. The error it reports tells where the first
 * problem is, by the variables {@value CodeEditor#LINE} and {@value CodeEditor#COLUMN}, which a
 * {@link CodeEditor} reads to underline it in the text, and what is found there, by
 * {@code found}: {@code 'x'}, or {@code U+000A} for a control character.
 * <p>
 * The messages have the keys {@code JsonValidator.} followed by the {@link Problem#key() key} of
 * the problem: {@code JsonValidator.end} when the text ends too early, {@code JsonValidator.value}
 * when a value was expected and so on.
 *
 * <pre>
 * CodeEditor editor = new CodeEditor("settings", model);
 * editor.setLanguage(CodeEditorLanguage.JSON).setSaveOnBlur(true).add(new JsonValidator());
 * </pre>
 *
 * The validator reads the text in one pass, without recursion: however deep the text nests, it
 * does not overflow the stack. {@code findJsonError} of the frontend's core finds the same errors
 * in the browser.
 *
 * @since 11.0.0
 */
public class JsonValidator implements IValidator<String>
{
	private static final long serialVersionUID = 1L;

	/** The variable of the error telling what is found where the problem is. */
	public static final String FOUND = "found";

	/**
	 * What is wrong where the first problem of a text is.
	 */
	public enum Problem
	{
		/** The text ends too early. */
		END("end"),

		/** A value was expected. */
		VALUE("value"),

		/** The name of a member of an object, in double quotes, was expected. */
		NAME("name"),

		/** A colon was expected after the name of a member. */
		COLON("colon"),

		/** A comma, or the brace closing the object, was expected. */
		OBJECT_NEXT("objectNext"),

		/** A comma, or the bracket closing the array, was expected. */
		ARRAY_NEXT("arrayNext"),

		/** Something follows the value. */
		TRAILING("trailing"),

		/** A string has a control character or an escape that is none. */
		STRING("string"),

		/** A number is not written as JSON writes them. */
		NUMBER("number");

		private final String key;

		Problem(String key)
		{
			this.key = key;
		}

		/**
		 * @return the key of the problem, which the key of its message ends with
		 */
		public String key()
		{
			return key;
		}
	}

	/**
	 * The first problem of a text.
	 *
	 * @param problem
	 *            what is wrong
	 * @param offset
	 *            where, in characters from the start of the text
	 * @param line
	 *            the line where it is, counted from 1
	 * @param column
	 *            the column where it is, counted from 1
	 * @param found
	 *            what is found there, as a message shows it: {@code 'x'}, or {@code U+000A} for a
	 *            control character; {@code null} at the end of the text
	 */
	public record JsonError(Problem problem, int offset, int line, int column, String found)
	{
	}

	@Override
	public void validate(IValidatable<String> validatable)
	{
		String text = validatable.getValue();
		JsonError error = text == null ? null : findError(text);
		if (error != null)
		{
			// the key of this class, also for a subclass
			ValidationError validationError = new ValidationError()
				.addKey("JsonValidator." + error.problem().key())
				.setVariable(CodeEditor.LINE, error.line())
				.setVariable(CodeEditor.COLUMN, error.column());
			if (error.found() != null)
			{
				validationError.setVariable(FOUND, error.found());
			}
			validatable.error(validationError);
		}
	}

	/**
	 * @param text
	 *            the text
	 * @return its first problem, {@code null} when it is well-formed JSON
	 */
	public static JsonError findError(String text)
	{
		return new Reader(text).read();
	}

	/** What comes next in the text. */
	private enum Expected
	{
		VALUE, VALUE_OR_CLOSE, NAME, NAME_OR_CLOSE, COLON, AFTER
	}

	/**
	 * Reads a text from its start until its end or its first problem.
	 */
	private static final class Reader
	{
		private final String text;

		private final int length;

		private int position;

		private Reader(String text)
		{
			this.text = text;
			length = text.length();
		}

		private char at(int index)
		{
			return index < length ? text.charAt(index) : '\0';
		}

		private boolean isDigit(int index)
		{
			return index < length && text.charAt(index) >= '0' && text.charAt(index) <= '9';
		}

		private boolean isHex(int index)
		{
			return index < length && Character.digit(text.charAt(index), 16) >= 0;
		}

		private JsonError read()
		{
			Deque<Character> open = new ArrayDeque<>();
			Expected expected = Expected.VALUE;
			while (true)
			{
				while (position < length && " \t\n\r".indexOf(text.charAt(position)) >= 0)
				{
					position++;
				}
				if (expected == Expected.AFTER && open.isEmpty())
				{
					return position < length ? fail(Problem.TRAILING) : null;
				}
				if (position >= length)
				{
					return fail(Problem.END);
				}
				char character = text.charAt(position);
				JsonError error = null;
				switch (expected)
				{
					case VALUE, VALUE_OR_CLOSE -> {
						if (expected == Expected.VALUE_OR_CLOSE && character == ']')
						{
							open.pop();
							position++;
						}
						else if (character == '{' || character == '[')
						{
							open.push(character);
							position++;
							expected = character == '{' ? Expected.NAME_OR_CLOSE :
								Expected.VALUE_OR_CLOSE;
							continue;
						}
						else if (character == '"')
						{
							error = string();
						}
						else if (character == '-' || isDigit(position))
						{
							error = number();
						}
						else if (text.startsWith("true", position) ||
							text.startsWith("null", position))
						{
							position += 4;
						}
						else if (text.startsWith("false", position))
						{
							position += 5;
						}
						else
						{
							return fail(Problem.VALUE);
						}
						expected = Expected.AFTER;
					}
					case NAME, NAME_OR_CLOSE -> {
						if (expected == Expected.NAME_OR_CLOSE && character == '}')
						{
							open.pop();
							position++;
							expected = Expected.AFTER;
						}
						else if (character == '"')
						{
							error = string();
							expected = Expected.COLON;
						}
						else
						{
							return fail(Problem.NAME);
						}
					}
					case COLON -> {
						if (character != ':')
						{
							return fail(Problem.COLON);
						}
						position++;
						expected = Expected.VALUE;
					}
					case AFTER -> {
						boolean object = open.peek() == '{';
						if (character == ',')
						{
							position++;
							expected = object ? Expected.NAME : Expected.VALUE;
						}
						else if (character == (object ? '}' : ']'))
						{
							open.pop();
							position++;
						}
						else
						{
							return fail(object ? Problem.OBJECT_NEXT : Problem.ARRAY_NEXT);
						}
					}
				}
				if (error != null)
				{
					return error;
				}
			}
		}

		/**
		 * Reads a string from its opening quote.
		 *
		 * @return its problem, if any
		 */
		private JsonError string()
		{
			position++;
			while (position < length)
			{
				char character = text.charAt(position);
				if (character == '"')
				{
					position++;
					return null;
				}
				if (character == '\\')
				{
					position++;
					char escape = at(position);
					if (escape == 'u')
					{
						position++;
						for (int digit = 0; digit < 4; digit++, position++)
						{
							if (!isHex(position))
							{
								return fail(Problem.STRING);
							}
						}
					}
					else if (position < length && "\"\\/bfnrt".indexOf(escape) >= 0)
					{
						position++;
					}
					else
					{
						return fail(Problem.STRING);
					}
				}
				else if (character < 0x20)
				{
					return fail(Problem.STRING);
				}
				else
				{
					position++;
				}
			}
			return fail(Problem.END);
		}

		/**
		 * Reads a number.
		 *
		 * @return its problem, if any
		 */
		private JsonError number()
		{
			if (at(position) == '-')
			{
				position++;
			}
			if (at(position) == '0')
			{
				position++;
			}
			else if (isDigit(position))
			{
				skipDigits();
			}
			else
			{
				return fail(Problem.NUMBER);
			}
			if (at(position) == '.')
			{
				position++;
				if (!isDigit(position))
				{
					return fail(Problem.NUMBER);
				}
				skipDigits();
			}
			if (at(position) == 'e' || at(position) == 'E')
			{
				position++;
				if (at(position) == '+' || at(position) == '-')
				{
					position++;
				}
				if (!isDigit(position))
				{
					return fail(Problem.NUMBER);
				}
				skipDigits();
			}
			return null;
		}

		private void skipDigits()
		{
			while (isDigit(position))
			{
				position++;
			}
		}

		/**
		 * @return the problem where the reader is: the end of the text there, whatever was
		 *         expected
		 */
		private JsonError fail(Problem problem)
		{
			int offset = Math.min(position, length);
			int line = 1;
			int lineStart = 0;
			for (int index = 0; index < offset; index++)
			{
				if (text.charAt(index) == '\n')
				{
					line++;
					lineStart = index + 1;
				}
			}
			return new JsonError(offset >= length ? Problem.END : problem, offset, line,
				offset - lineStart + 1, found(offset));
		}

		/**
		 * @return what a message shows of the character at a position: {@code 'x'},
		 *         {@code U+000A} for a control or a space character, {@code null} at the end
		 */
		private String found(int offset)
		{
			if (offset >= length)
			{
				return null;
			}
			int code = text.codePointAt(offset);
			if (code <= 0x20 || (code >= 0x7f && code <= 0xa0) || code == 0x2028 || code == 0x2029)
			{
				return String.format(Locale.ROOT, "U+%04X", code);
			}
			return "'" + new String(Character.toChars(code)) + "'";
		}
	}
}
