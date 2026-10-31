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

/*
 * Finds the first error of a JSON text, by the grammar of RFC 8259, and where it is, for a code
 * editor to show it (CodeEditorError). It finds what the JsonValidator of ui-components finds, so
 * that a page in React or Svelte tells of the errors of a text as a Wicket page does. It reads the
 * text in one pass, without recursion: however deep the text nests, it does not overflow the stack.
 */

/**
 * What is wrong where the error is: the text ends too early (end); a value (value), a name in
 * double quotes (name), a colon (colon), a comma or the end of the object (objectNext) or of the
 * array (arrayNext) was expected; something follows the value (trailing); a string has a control
 * character or a wrong escape (string); a number is not one (number).
 */
export type JsonProblem = 'end' | 'value' | 'name' | 'colon' | 'objectNext' | 'arrayNext' |
	'trailing' | 'string' | 'number';

/** The first error of a JSON text. */
export interface JsonError {
	problem: JsonProblem;
	/** Where it is, in characters from the start of the text. */
	from: number;
	/** The line where it is, counted from 1. */
	line: number;
	/** The column where it is, counted from 1. */
	column: number;
	/** What is found there, as a message shows it: 'x', or U+000A for a control character; null at the end. */
	found: string | null;
}

/** What comes next. */
type Expected = 'value' | 'valueOrClose' | 'name' | 'nameOrClose' | 'colon' | 'after';

function isDigit(character: string | undefined): boolean {
	return character !== undefined && character >= '0' && character <= '9';
}

function isHex(character: string | undefined): boolean {
	return character !== undefined && /^[0-9a-fA-F]$/.test(character);
}

/**
 * @returns what a message shows of the character at a position: 'x', U+000A for a control or a
 *          space character, null at the end of the text
 */
function foundAt(text: string, position: number): string | null {
	const code = text.codePointAt(position);
	if (code === undefined) {
		return null;
	}
	if (code <= 0x20 || (code >= 0x7f && code <= 0xa0) || code === 0x2028 || code === 0x2029) {
		return 'U+' + code.toString(16).toUpperCase().padStart(4, '0');
	}
	return "'" + String.fromCodePoint(code) + "'";
}

/**
 * @param text the text
 * @returns its first error, null when it is well-formed JSON
 */
export function findJsonError(text: string): JsonError | null {
	const length = text.length;
	const open: string[] = [];
	let position = 0;
	let expected: Expected = 'value';

	const fail = (problem: JsonProblem): JsonError => {
		const at = Math.min(position, length);
		const lineStart = text.lastIndexOf('\n', at - 1) + 1;
		let line = 1;
		for (let index = text.indexOf('\n'); index >= 0 && index < at;
			index = text.indexOf('\n', index + 1)) {
			line++;
		}
		return { problem: at >= length ? 'end' : problem, from: at, line,
			column: at - lineStart + 1, found: foundAt(text, at) };
	};

	/** Reads a string from its opening quote; @returns its error, if any */
	const string = (): JsonError | null => {
		position++;
		while (position < length) {
			const character = text[position];
			if (character === '"') {
				position++;
				return null;
			}
			if (character === '\\') {
				position++;
				const escape = text[position];
				if (escape === 'u') {
					position++;
					for (let digit = 0; digit < 4; digit++, position++) {
						if (!isHex(text[position])) {
							return fail('string');
						}
					}
				} else if (escape !== undefined && '"\\/bfnrt'.includes(escape)) {
					position++;
				} else {
					return fail('string');
				}
			} else if (character.charCodeAt(0) < 0x20) {
				return fail('string');
			} else {
				position++;
			}
		}
		return fail('end');
	};

	/** Reads a number; @returns its error, if any */
	const number = (): JsonError | null => {
		if (text[position] === '-') {
			position++;
		}
		if (text[position] === '0') {
			position++;
		} else if (isDigit(text[position])) {
			while (isDigit(text[position])) {
				position++;
			}
		} else {
			return fail('number');
		}
		if (text[position] === '.') {
			position++;
			if (!isDigit(text[position])) {
				return fail('number');
			}
			while (isDigit(text[position])) {
				position++;
			}
		}
		if (text[position] === 'e' || text[position] === 'E') {
			position++;
			if (text[position] === '+' || text[position] === '-') {
				position++;
			}
			if (!isDigit(text[position])) {
				return fail('number');
			}
			while (isDigit(text[position])) {
				position++;
			}
		}
		return null;
	};

	for (;;) {
		while (position < length && ' \t\n\r'.includes(text[position])) {
			position++;
		}
		const character = text[position];
		if (expected === 'after' && open.length === 0) {
			return position < length ? fail('trailing') : null;
		}
		if (character === undefined) {
			return fail('end');
		}
		let error: JsonError | null = null;
		switch (expected) {
			case 'value':
			case 'valueOrClose':
				if (expected === 'valueOrClose' && character === ']') {
					open.pop();
					position++;
				} else if (character === '{') {
					open.push('{');
					position++;
					expected = 'nameOrClose';
					continue;
				} else if (character === '[') {
					open.push('[');
					position++;
					expected = 'valueOrClose';
					continue;
				} else if (character === '"') {
					error = string();
				} else if (character === '-' || isDigit(character)) {
					error = number();
				} else {
					const literal = ['true', 'false', 'null'].find((each) =>
						text.startsWith(each, position));
					if (!literal) {
						return fail('value');
					}
					position += literal.length;
				}
				expected = 'after';
				break;
			case 'name':
			case 'nameOrClose':
				if (expected === 'nameOrClose' && character === '}') {
					open.pop();
					position++;
					expected = 'after';
				} else if (character === '"') {
					error = string();
					expected = 'colon';
				} else {
					return fail('name');
				}
				break;
			case 'colon':
				if (character !== ':') {
					return fail('colon');
				}
				position++;
				expected = 'value';
				break;
			case 'after': {
				const object = open[open.length - 1] === '{';
				if (character === ',') {
					position++;
					expected = object ? 'name' : 'value';
				} else if (character === (object ? '}' : ']')) {
					open.pop();
					position++;
				} else {
					return fail(object ? 'objectNext' : 'arrayNext');
				}
				break;
			}
		}
		if (error) {
			return error;
		}
	}
}
