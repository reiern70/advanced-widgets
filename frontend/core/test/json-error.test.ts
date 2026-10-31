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

import { describe, expect, it } from 'vitest';

import { findJsonError, type JsonError } from '../src/index.js';

/**
 * @returns the error of a text without where it is in characters, for short expectations
 */
function problemOf(text: string): Omit<JsonError, 'from'> | null {
	const error = findJsonError(text);
	if (!error) {
		return null;
	}
	const { from, ...rest } = error;
	expect(from).toBeGreaterThanOrEqual(0);
	return rest;
}

describe('findJsonError', () => {
	it('finds nothing wrong with well-formed JSON', () => {
		for (const text of [
			'{}', '[]', '0', '-0', '1.5', '-12.25e+3', '3E-2', '2e10', 'true', 'false', 'null',
			'""', '"a \\" \\\\ \\/ \\b \\f \\n \\r \\t \\u00e9 \\uABCD"', '"é ✓ 😀"',
			' \t\r\n{ "a" : [ 1 , { "b" : null } , [ ] , "c" ] , "d" : { } } \n',
			'[[[[[[1]]]]]]', '{"a":{"b":{"c":[true,false,null]}}}'
		]) {
			expect(findJsonError(text), text).toBeNull();
		}
		expect(findJsonError(JSON.stringify({ deep: [1, [2, [3, { x: 'y' }]]], n: -1.5e-7 })))
			.toBeNull();
	});

	it('tells where the text ends too early', () => {
		expect(findJsonError('')).toEqual({ problem: 'end', from: 0, line: 1, column: 1,
			found: null });
		expect(findJsonError('  ')).toEqual({ problem: 'end', from: 2, line: 1, column: 3,
			found: null });
		expect(problemOf('{"a": 1')).toEqual({ problem: 'end', line: 1, column: 8, found: null });
		expect(problemOf('[1,\n2')).toEqual({ problem: 'end', line: 2, column: 2, found: null });
		expect(problemOf('{"a"')).toMatchObject({ problem: 'end' });
		expect(problemOf('{"a":')).toMatchObject({ problem: 'end' });
		expect(problemOf('"open')).toMatchObject({ problem: 'end', column: 6 });
		expect(problemOf('"\\')).toMatchObject({ problem: 'end' });
		expect(problemOf('"\\u12')).toMatchObject({ problem: 'end' });
		expect(problemOf('-')).toMatchObject({ problem: 'end' });
		expect(problemOf('1.')).toMatchObject({ problem: 'end' });
		expect(problemOf('1e+')).toMatchObject({ problem: 'end' });
	});

	it('tells where a value was expected', () => {
		expect(findJsonError('{"a": }')).toEqual({ problem: 'value', from: 6, line: 1, column: 7,
			found: "'}'" });
		expect(problemOf('[1, ]')).toEqual({ problem: 'value', line: 1, column: 5, found: "']'" });
		expect(problemOf("['a']")).toEqual({ problem: 'value', line: 1, column: 2, found: "'''" });
		expect(problemOf('tru')).toEqual({ problem: 'value', line: 1, column: 1, found: "'t'" });
		expect(problemOf('nul')).toMatchObject({ problem: 'value' });
		expect(problemOf('undefined')).toMatchObject({ problem: 'value', found: "'u'" });
		expect(problemOf('+1')).toMatchObject({ problem: 'value', found: "'+'" });
	});

	it('tells where a name in double quotes, or a colon, was expected', () => {
		expect(problemOf('{a: 1}')).toEqual({ problem: 'name', line: 1, column: 2, found: "'a'" });
		expect(problemOf('{"a": 1, }')).toEqual({ problem: 'name', line: 1, column: 10,
			found: "'}'" });
		expect(problemOf('{"a" 1}')).toEqual({ problem: 'colon', line: 1, column: 6,
			found: "'1'" });
	});

	it('tells where a comma or the end of an object or an array was expected', () => {
		expect(problemOf('{"a": 1 "b": 2}')).toEqual({ problem: 'objectNext', line: 1,
			column: 9, found: "'\"'" });
		expect(problemOf('{\n  "a": 1\n  ]')).toEqual({ problem: 'objectNext', line: 3,
			column: 3, found: "']'" });
		expect(problemOf('[1 2]')).toEqual({ problem: 'arrayNext', line: 1, column: 4,
			found: "'2'" });
		expect(problemOf('[1}')).toMatchObject({ problem: 'arrayNext', found: "'}'" });
		// a number starting with 0 ends there
		expect(problemOf('[01]')).toMatchObject({ problem: 'arrayNext', column: 3, found: "'1'" });
	});

	it('tells what follows the value', () => {
		expect(problemOf('{} {}')).toEqual({ problem: 'trailing', line: 1, column: 4,
			found: "'{'" });
		expect(problemOf('1 2')).toMatchObject({ problem: 'trailing', column: 3 });
		expect(problemOf('null,')).toMatchObject({ problem: 'trailing', found: "','" });
	});

	it('tells of a control character or a wrong escape in a string', () => {
		expect(problemOf('"a\nb"')).toEqual({ problem: 'string', line: 1, column: 3,
			found: 'U+000A' });
		expect(problemOf('"a\tb"')).toMatchObject({ problem: 'string', found: 'U+0009' });
		expect(problemOf('"\\x"')).toEqual({ problem: 'string', line: 1, column: 3,
			found: "'x'" });
		expect(problemOf('"\\u12G4"')).toEqual({ problem: 'string', line: 1, column: 6,
			found: "'G'" });
		expect(problemOf('["ok", "\\q"]')).toMatchObject({ problem: 'string', column: 10 });
	});

	it('tells of a number that is not one', () => {
		expect(problemOf('-a')).toEqual({ problem: 'number', line: 1, column: 2, found: "'a'" });
		expect(problemOf('1.e5')).toEqual({ problem: 'number', line: 1, column: 3, found: "'e'" });
		expect(problemOf('1e')).toMatchObject({ problem: 'end' });
		expect(problemOf('1ex')).toEqual({ problem: 'number', line: 1, column: 3, found: "'x'" });
		expect(problemOf('[-]')).toMatchObject({ problem: 'number', found: "']'" });
	});

	it('shows what it found as a message would', () => {
		expect(problemOf('[1  ]')).toMatchObject({ found: 'U+00A0' });
		expect(problemOf('[1  ]')).toMatchObject({ found: 'U+2028' });
		expect(problemOf('[1 \u007f]')).toMatchObject({ found: 'U+007F' });
		// a character beyond the basic plane, whole
		expect(problemOf('[1 😀]')).toMatchObject({ found: "'😀'" });
	});

	it('counts the lines by their line feeds, a carriage return before one in its line', () => {
		expect(findJsonError('{\r\n  "a": 1,\r\n  "b": ]\r\n}')).toEqual({ problem: 'value',
			from: 21, line: 3, column: 8, found: "']'" });
	});

	it('reads a text however deep it nests, without overflowing the stack', () => {
		const depth = 100000;
		expect(findJsonError('['.repeat(depth) + ']'.repeat(depth))).toBeNull();
		expect(findJsonError('['.repeat(depth))).toMatchObject({ problem: 'end', from: depth });
		expect(findJsonError('{"a":'.repeat(depth) + '1' + '}'.repeat(depth))).toBeNull();
	});
});
