/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The template engines rendering JSON into markup in the browser, by name. An engine is an object
 * whose compile(template) returns function(data, meta) -> markup, meta holding what the caller
 * adds, such as the number of a row, as {{@name}}.
 *
 * The built-in engine replaces {{path}} with the escaped value at the path of the data, and
 * {{{path}}} with the raw value; a path is a dotted chain of properties.
 */
;(function (undefined) {
	'use strict';

	if (typeof(Wicket) === "undefined") {
		window.Wicket = { };
	}

	if (typeof(Wicket.Templates) !== "undefined") {
		return;
	}

	const HTML_ESCAPES = {
		'&': '&amp;',
		'<': '&lt;',
		'>': '&gt;',
		'"': '&quot;',
		"'": '&#39;'
	};

	const escapeHtml = function (value) {
		return String(value).replace(/[&<>"']/g, function (c) {
			return HTML_ESCAPES[c];
		});
	};

	const lookup = function (data, path) {
		return path.split('.').reduce(function (value, name) {
			return (value === null || value === undefined) ? undefined : value[name];
		}, data);
	};

	const builtinEngine = {
		compile: function (template) {
			const placeholder = /\{\{\{\s*(@?[\w$.]+)\s*\}\}\}|\{\{\s*(@?[\w$.]+)\s*\}\}/g;
			const parts = [];
			let last = 0;
			let match;
			while ((match = placeholder.exec(template)) !== null) {
				parts.push(template.substring(last, match.index));
				parts.push({ path: match[1] || match[2], raw: match[1] !== undefined });
				last = placeholder.lastIndex;
			}
			parts.push(template.substring(last));

			return function (data, meta) {
				return parts.map(function (part) {
					if (typeof part === 'string') {
						return part;
					}
					const value = part.path.charAt(0) === '@' ?
						lookup(meta, part.path.substring(1)) : lookup(data, part.path);
					if (value === null || value === undefined) {
						return '';
					}
					return part.raw ? String(value) : escapeHtml(value);
				}).join('');
			};
		}
	};

	Wicket.Templates = {

		/** The engines, by name. */
		engines : {
			builtin : builtinEngine
		},

		escapeHtml : escapeHtml,

		lookup : lookup,

		/**
		 * @return the engine of the name, null (and logged) if none is registered
		 */
		engine : function (name) {
			const engine = Wicket.Templates.engines[name || 'builtin'];
			if (!engine && Wicket.Log) {
				Wicket.Log.error('Wicket.Templates: no template engine registered as "' + name + '"');
			}
			return engine || null;
		}
	};
})();
