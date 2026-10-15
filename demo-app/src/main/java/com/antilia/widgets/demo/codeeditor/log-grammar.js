/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
