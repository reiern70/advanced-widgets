/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/* the grammar of a log, plugged in by a page: its levels are keywords */
Wicket.CodeEditor.registerLanguage('log', function (kit) {
	return kit.StreamLanguage.define({
		token: function (stream) {
			if (stream.match(/^(ERROR|WARN|INFO)\b/)) {
				return 'keyword';
			}
			stream.next();
			return null;
		}
	});
});
