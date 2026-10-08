/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Handlebars as a template engine of templates(), for the tables, the lists and the
 * autocompletes of any framework. Handlebars itself is the page's: the core does not bundle it.
 */

import { templates, type TemplateEngine, type TemplateRegistry } from './templates.js';

/** What the engine needs of Handlebars: compiling a template. */
export interface HandlebarsLike {
	compile(template: string): (data: unknown, options?: { data?: unknown }) => string;
}

declare global {
	interface Window {
		Handlebars?: HandlebarsLike;
	}
}

/**
 * @param handlebars Handlebars; the page's window.Handlebars, when a template is compiled, if left
 *            out
 * @returns the engine rendering templates with Handlebars, the meta of a row or item, such as its
 *          key and number, as @key and @number
 */
export function handlebarsEngine(handlebars?: HandlebarsLike): TemplateEngine {
	return {
		compile(template) {
			const engine = handlebars ?? window.Handlebars;
			if (!engine) {
				throw new Error('Handlebars is not loaded');
			}
			const compiled = engine.compile(template);
			return (data, meta) => compiled(data, { data: meta });
		}
	};
}

/**
 * Registers Handlebars as a template engine.
 *
 * @param handlebars Handlebars; the page's window.Handlebars if left out
 * @param registry the registry, templates() if left out
 * @param name the name tables, lists and autocompletes refer to the engine by
 */
export function registerHandlebars(handlebars?: HandlebarsLike,
	registry: TemplateRegistry = templates(), name = 'handlebars'): void {
	registry.engines[name] = handlebarsEngine(handlebars);
}
