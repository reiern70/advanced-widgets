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

import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { lezerHighlighter, RichTextEditor } from '@antilia/core';
import { Slice } from '@tiptap/pm/model';

import BoundRichTextEditor from './BoundRichTextEditor.svelte';

beforeAll(() => {
	// jsdom lays nothing out: ProseMirror measures nothing
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => ({ left: 0, top: 0, right: 0, bottom: 0,
		width: 0, height: 0, x: 0, y: 0, toJSON: () => ({}) }) as DOMRect;
	document.elementFromPoint = () => null;
});

/** A moment, for the state reported after a delay of 0. */
const tick = () => new Promise((resolve) => setTimeout(resolve, 20));

function editorOf(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-rich-text-editor')!;
}

/**
 * @returns the editor of the core in the container
 */
function core(container: HTMLElement): RichTextEditor {
	return RichTextEditor.of(editorOf(container))!;
}

afterEach(cleanup);

describe('RichTextEditor', () => {
	it('renders an element the core makes an editor of, with its toolbar', () => {
		const { container } = render(BoundRichTextEditor);
		const element = editorOf(container);

		expect(element.id).toBe('e');
		expect(element.className.split(' ')).toEqual(['rich', 'antilia-rich-text-editor',
			'antilia-resizable', 'antilia-resizable-vertical']);
		expect(element.querySelectorAll('.ProseMirror')).toHaveLength(1);
		expect(element.querySelectorAll('[role="toolbar"]')).toHaveLength(1);
		expect(element.querySelector('.ProseMirror')?.getAttribute('aria-label')).toBe('Notes');
		expect(element.querySelector('.ProseMirror')?.textContent).toBe('Hello world');
		expect(container.querySelector('.modified')?.textContent).toBe('false');
	});

	it('binds the text the user changes, and whether it has changes not saved', () => {
		const { container } = render(BoundRichTextEditor);
		const tiptap = core(container).editor;
		tiptap.view.dispatch(tiptap.state.tr.insertText('Oh, '));
		flushSync();
		expect(container.querySelector('output')?.textContent).toBe('<p>Oh, Hello world</p>');
		expect(container.querySelector('.modified')?.textContent).toBe('true');
		expect(editorOf(container).getAttribute('data-modified')).toBe('true');
	});

	it('shows the text the page gives it, and takes the text saved', async () => {
		const { container } = render(BoundRichTextEditor);
		await fireEvent.click(container.querySelector('.reset')!);
		flushSync();
		expect(editorOf(container).querySelector('.ProseMirror')?.textContent).toBe('Reset');
		expect(container.querySelector('.modified')?.textContent).toBe('true');

		const other = render(BoundRichTextEditor, { saved: '<p>Other</p>' });
		expect(other.container.querySelector('.modified')?.textContent).toBe('true');
	});

	it('starts in the state given and binds the state the user leaves it in', async () => {
		const onselectionchange = vi.fn();
		const { container } = render(BoundRichTextEditor, { onselectionchange,
			state: { selection: { anchor: 2, head: 4 } } });
		const editor = core(container);

		expect(editor.getState().selection).toEqual({ anchor: 2, head: 4 });
		editor.editor.commands.setTextSelection(6);
		expect(onselectionchange).toHaveBeenCalledWith({ anchor: 6, head: 6 });
		await tick();
		flushSync();
		expect(container.querySelector('.head')?.textContent).toBe('6');
	});

	it('calls back on Ctrl+S, the focus and the size', async () => {
		const onsave = vi.fn();
		const onfocus = vi.fn();
		const onblur = vi.fn();
		const onresize = vi.fn();
		const { container } = render(BoundRichTextEditor, { onsave, onfocus, onblur, onresize });

		await fireEvent.keyDown(container.querySelector('.ProseMirror')!, { key: 's',
			ctrlKey: true });
		expect(onsave).toHaveBeenCalledWith('<p>Hello world</p>');
		core(container).focus();
		await tick();
		expect(onfocus).toHaveBeenCalledTimes(1);
		(container.querySelector('#other') as HTMLInputElement).focus();
		await tick();
		expect(onblur).toHaveBeenCalledWith('<p>Hello world</p>');
		await fireEvent.keyDown(container.querySelector('.antilia-resizable-handle')!,
			{ key: 'ArrowDown' });
		expect(onresize).toHaveBeenCalledWith({ width: null, height: 100 });
	});

	it('takes other options, keeping the classes of the core', async () => {
		const { container, rerender } = render(BoundRichTextEditor);
		await rerender({ readOnly: true, toolbar: [['bold']], count: true, className: 'other' });
		flushSync();
		const element = editorOf(container);
		expect(element.classList.contains('other')).toBe(true);
		expect(element.classList.contains('antilia-rich-text-editor-readonly')).toBe(true);
		expect(element.classList.contains('antilia-resizable')).toBe(true);
		expect(element.querySelector('.antilia-rich-text-status')?.textContent)
			.toBe('11 characters · 2 words');
	});

	it('keeps Markdown', () => {
		const { container } = render(BoundRichTextEditor, { format: 'markdown',
			value: '# Title\n\nSome **bold**' });
		const tiptap = core(container).editor;
		tiptap.commands.setTextSelection(1);
		tiptap.view.dispatch(tiptap.state.tr.insertText('A '));
		flushSync();
		expect(container.querySelector('output')?.textContent).toBe('# A Title\n\nSome **bold**');
	});

	it('posts the text in a hidden text area under a name', () => {
		const { container } = render(BoundRichTextEditor, { name: 'body' });
		const field = container.querySelector<HTMLTextAreaElement>('textarea[name="body"]')!;
		expect(field.hidden).toBe(true);
		expect(field.value).toBe('<p>Hello world</p>');
	});

	it('highlights its code blocks with the grammars of the code editor, or none', async () => {
		await lezerHighlighter.load('java');
		const { container, rerender } = render(BoundRichTextEditor, { highlight: true,
			value: '<pre><code class="language-java">class A {}</code></pre>' });
		const keyword = () => container.querySelector('pre span.tok-keyword')?.textContent;
		expect(keyword()).toBe('class');
		const codeBlock = () => container.querySelector('[data-tool="codeBlock"]')!;
		expect(codeBlock().getAttribute('aria-haspopup')).toBe('menu');

		await rerender({ highlight: false, codeLanguages: ['java'] });
		flushSync();
		expect(keyword()).toBeUndefined();
		expect(container.querySelector('pre')?.getAttribute('data-antilia-language')).toBe('Java');
		await rerender({ codeLanguages: false });
		flushSync();
		expect(codeBlock().getAttribute('aria-haspopup')).toBeNull();
	});

	it('uploads the images the user drops, by onupload', async () => {
		const onupload = vi.fn(() => Promise.resolve('/bird.png'));
		const { container, rerender } = render(BoundRichTextEditor, { onupload });
		// the editor the props make, anew after a rerender of the test's
		const drop = () => {
			const view = core(container).editor.view;
			const event = new Event('drop', { cancelable: true });
			Object.assign(event, { clientX: 0, clientY: 0, dataTransfer: { types: ['Files'],
				files: [new File(['x'], 'bird.png', { type: 'image/png' })], getData: () => '' } });
			view.posAtCoords = () => null;
			return view.someProp('handleDrop', (handle) => handle(view, event as DragEvent,
				Slice.empty, false));
		};
		core(container).editor.commands.setTextSelection(12);
		expect(drop()).toBe(true);
		await tick();
		flushSync();
		expect(onupload).toHaveBeenCalledTimes(1);
		expect(container.querySelector('output')?.textContent)
			.toBe('<p>Hello world</p><img src="/bird.png">');

		await rerender({ onupload: undefined });
		flushSync();
		expect(drop()).toBeFalsy();
	});
});
