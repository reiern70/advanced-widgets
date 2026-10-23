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
 * The grammar of a log, which the code editor demo plugs in: built from the CodeMirror classes of
 * the editor, a StreamLanguage reading a line a token at a time. The times are numbers, the levels
 * keywords, errors in the color of what went wrong, warnings in that of what asks for attention,
 * the threads meta, the quoted texts strings and the exceptions types.
 */
Wicket.CodeEditor.registerLanguage('log', function (kit) {
	return kit.StreamLanguage.define({
		name: 'log',
		token: function (stream) {
			if (stream.eatSpace()) {
				return null;
			}
			if (stream.match(/^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}(\.\d+)?/)) {
				return 'number';
			}
			if (stream.match(/^(ERROR|FATAL)\b/)) {
				return 'invalid';
			}
			if (stream.match(/^WARN(ING)?\b/)) {
				return 'meta';
			}
			if (stream.match(/^(INFO|DEBUG|TRACE)\b/)) {
				return 'keyword';
			}
			if (stream.match(/^\[[^\]]*\]/)) {
				return 'comment';
			}
			if (stream.peek() === '"') {
				stream.next();
				if (stream.skipTo('"')) {
					stream.next();
				} else {
					stream.skipToEnd();
				}
				return 'string';
			}
			if (stream.match(/^[A-Z]\w*(Exception|Error)\b/)) {
				return 'typeName';
			}
			if (stream.match(/^\d+/)) {
				return 'number';
			}
			stream.next();
			return null;
		}
	});
}, ['logs']);
