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
 * The template engines rendering JSON into markup in the browser, by name. An engine is an object
 * whose compile(template) returns function(data, meta) -> markup, meta holding what the caller
 * adds, such as the number of a row, as {{@name}}.
 *
 * The built-in engine replaces {{path}} with the escaped value at the path of the data, and
 * {{{path}}} with the raw value; a path is a dotted chain of properties.
 *
 * The components look the engines up in templates(), the registry set with useTemplates(): in a
 * Wicket page the page's Wicket.Templates, so that every script sees the engines registered there,
 * such as Handlebars.
 */

import { getHost } from './host.js';

/** Renders data into markup, meta holding what the caller adds, as {{@name}}. */
export type CompiledTemplate = (data: unknown, meta?: Record<string, unknown>) => string;

/** A template engine. */
export interface TemplateEngine {
	compile(template: string): CompiledTemplate;
}

/** The template engines, by name, and their helpers. */
export interface TemplateRegistry {
	engines: Record<string, TemplateEngine>;
	escapeHtml(value: unknown): string;
	lookup(data: unknown, path: string): unknown;
	engine(name?: string | null): TemplateEngine | null;
}

const HTML_ESCAPES: Record<string, string> = {
	'&': '&amp;',
	'<': '&lt;',
	'>': '&gt;',
	'"': '&quot;',
	"'": '&#39;'
};

function escapeHtml(value: unknown): string {
	return String(value).replace(/[&<>"']/g, (c) => HTML_ESCAPES[c]);
}

function lookup(data: unknown, path: string): unknown {
	return path.split('.').reduce<unknown>((value, name) =>
		(value === null || value === undefined) ? undefined :
			(value as Record<string, unknown>)[name], data);
}

const builtinEngine: TemplateEngine = {
	compile(template) {
		const placeholder = /\{\{\{\s*(@?[\w$.]+)\s*\}\}\}|\{\{\s*(@?[\w$.]+)\s*\}\}/g;
		const parts: Array<string | { path: string; raw: boolean }> = [];
		let last = 0;
		let match;
		while ((match = placeholder.exec(template)) !== null) {
			parts.push(template.substring(last, match.index));
			parts.push({ path: match[1] || match[2], raw: match[1] !== undefined });
			last = placeholder.lastIndex;
		}
		parts.push(template.substring(last));

		return (data, meta) => parts.map((part) => {
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
	}
};

/**
 * The registry of this script: the built-in engine, and those registered into it.
 */
export const Templates: TemplateRegistry = {

	engines: {
		builtin: builtinEngine
	},

	escapeHtml,

	lookup,

	/**
	 * @returns the engine of the name, the built-in one when none is given; null (and logged) if
	 *          none is registered under the name
	 */
	engine(name) {
		const engine = Templates.engines[name || 'builtin'];
		if (!engine) {
			getHost().log.error('Wicket.Templates: no template engine registered as "' + name + '"');
		}
		return engine || null;
	}
};

let current: TemplateRegistry = Templates;

/**
 * @returns the registry the components look the engines up in
 */
export function templates(): TemplateRegistry {
	return current;
}

/**
 * Makes the components look the engines up in another registry.
 *
 * @param registry the registry, the one of this script when left out
 */
export function useTemplates(registry?: TemplateRegistry | null): void {
	current = registry ?? Templates;
}
