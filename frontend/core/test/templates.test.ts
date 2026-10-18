/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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

import { afterEach, describe, expect, it, vi } from 'vitest';

import { setHost, standaloneHost, Templates, templates, useTemplates,
	type TemplateRegistry } from '../src/index.js';

afterEach(() => {
	useTemplates();
	setHost();
});

describe('Templates', () => {
	it('escapes {{path}}, leaves {{{path}}} raw and reads {{@meta}}', () => {
		const render = Templates.engine()!.compile(
			'<b>{{name.first}}</b> {{{html}}} #{{@number}} {{missing}}');

		expect(render({ name: { first: '<Ann>' }, html: '<i>x</i>' }, { number: 3 }))
			.toBe('<b>&lt;Ann&gt;</b> <i>x</i> #3 ');
	});

	it('logs an engine that is not registered', () => {
		const error = vi.fn();
		setHost({ ...standaloneHost, log: { error } });

		expect(Templates.engine('mustache')).toBeNull();
		expect(error).toHaveBeenCalledWith(
			'Wicket.Templates: no template engine registered as "mustache"');
	});

	it('lets the components use another registry', () => {
		const other: TemplateRegistry = { ...Templates, engines: {} };
		useTemplates(other);
		expect(templates()).toBe(other);

		useTemplates();
		expect(templates()).toBe(Templates);
	});
});
