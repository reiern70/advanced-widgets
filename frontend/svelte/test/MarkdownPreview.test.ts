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

import { cleanup, render } from '@testing-library/svelte';
import { afterEach, describe, expect, it } from 'vitest';

import { CodeEditor as CoreCodeEditor, MarkdownPreview as CoreMarkdownPreview } from '@antilia/core';

import { BOOTSTRAP_LOOK, LOOK_CONTEXT, MarkdownPreview } from '../src/index.js';

function preview(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-markdown-preview')!;
}

afterEach(cleanup);

describe('MarkdownPreview', () => {
	it('renders the text, a region by its label, that takes the focus', () => {
		const { container } = render(MarkdownPreview, { id: 'p', class: 'notes', value: '# Notes',
			label: 'Preview' });
		const element = preview(container);

		expect(element.id).toBe('p');
		expect(element.className).toBe('antilia-markdown-preview notes');
		expect(element.getAttribute('role')).toBe('region');
		expect(element.getAttribute('aria-label')).toBe('Preview');
		expect(element.tabIndex).toBe(0);
		expect(element.innerHTML).toBe('<h1>Notes</h1>');
	});

	it('renders the text again as it changes, and its label and placeholder', async () => {
		const { container, rerender } = render(MarkdownPreview, { value: '*one*' });
		const element = preview(container);
		expect(element.hasAttribute('role')).toBe(false);

		await rerender({ value: '**two**', class: 'more' });
		expect(element.innerHTML).toBe('<p><strong>two</strong></p>');
		expect(element.className).toBe('antilia-markdown-preview more');

		await rerender({ value: '', placeholder: 'Nothing yet', label: 'Notes' });
		expect(element.innerHTML).toBe('<p class="antilia-markdown-preview-empty">Nothing yet</p>');
		expect(element.getAttribute('aria-label')).toBe('Notes');

		await rerender({ value: '', placeholder: undefined, label: undefined });
		expect(element.innerHTML).toBe('');
		expect(element.hasAttribute('aria-label')).toBe(false);
	});

	it('follows a code editor, and keeps in step with it when asked to', async () => {
		document.body.insertAdjacentHTML('beforeend', '<textarea id="code"># Notes</textarea>');
		const editor = new CoreCodeEditor(document.getElementById('code')!);
		const { container, rerender } = render(MarkdownPreview, { editor: 'code' });
		const element = preview(container);
		const core = CoreMarkdownPreview.of(element)!;
		expect(element.innerHTML).toBe('<h1>Notes</h1>');
		expect(core.options.scrollWithEditor).toBe(false);

		await rerender({ editor: 'code', scrollWithEditor: true });
		expect(CoreMarkdownPreview.of(element)).toBe(core);
		expect(core.options.scrollWithEditor).toBe(true);
		editor.setValue('## Changed');
		await new Promise((resolve) => setTimeout(resolve, 40));
		expect(element.innerHTML).toBe('<h2>Changed</h2>');

		await rerender({ editor: null, scrollWithEditor: false, value: 'own' });
		expect(core.options.editor).toBeNull();
		expect(core.options.scrollWithEditor).toBe(false);
		expect(element.innerHTML).toBe('<p>own</p>');
		editor.destroy();
		document.getElementById('code')!.remove();
	});

	it('gives its tables the classes of the look', () => {
		const { container } = render(MarkdownPreview, { props: { value: '| a |\n|---|\n| b |' },
			context: new Map([[LOOK_CONTEXT, BOOTSTRAP_LOOK]]) });

		expect(preview(container).querySelector('table')!.className).toBe(
			'antilia-markdown-preview-table table table-sm');
	});
});
