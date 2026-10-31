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

import { CodeEditor } from '@antilia/core';

import BoundCodeEditor from './BoundCodeEditor.svelte';

beforeAll(() => {
	// jsdom lays nothing out: CodeMirror measures nothing
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => ({ left: 0, top: 0, right: 0, bottom: 0,
		width: 0, height: 0, x: 0, y: 0, toJSON: () => ({}) }) as DOMRect;
});

/** A moment, for a grammar loading and the state reported after a delay of 0. */
const tick = () => new Promise((resolve) => setTimeout(resolve, 20));

function editorOf(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-code-editor')!;
}

/**
 * @returns the editor of the core in the container
 */
function core(container: HTMLElement): CodeEditor {
	return CodeEditor.of(editorOf(container))!;
}

afterEach(cleanup);

describe('CodeEditor', () => {
	it('renders an element the core makes an editor of, resizable by its bottom edge', () => {
		const { container } = render(BoundCodeEditor);
		const element = editorOf(container);

		expect(element.id).toBe('e');
		expect(element.className.split(' ')).toEqual(['code', 'antilia-code-editor',
			'antilia-resizable', 'antilia-resizable-vertical']);
		expect(element.querySelectorAll('.cm-editor')).toHaveLength(1);
		expect(element.querySelector('.cm-content')?.getAttribute('aria-label')).toBe('Script');
		expect(element.querySelector('.cm-content')?.textContent).toBe('let a = 1;');
		expect(container.querySelector('.modified')?.textContent).toBe('false');
	});

	it('binds the text the user changes, and whether it has changes not saved', () => {
		const { container } = render(BoundCodeEditor);

		core(container).view.dispatch({ changes: { from: 0, insert: '// ' },
			userEvent: 'input.type' });
		flushSync();
		expect(container.querySelector('output')?.textContent).toBe('// let a = 1;');
		expect(container.querySelector('.modified')?.textContent).toBe('true');
		expect(editorOf(container).getAttribute('data-modified')).toBe('true');
	});

	it('shows the text the page gives it', async () => {
		const { container } = render(BoundCodeEditor);

		await fireEvent.click(container.querySelector('.reset')!);
		flushSync();

		expect(editorOf(container).querySelector('.cm-content')?.textContent).toBe('const b = 2;');
		expect(container.querySelector('.modified')?.textContent).toBe('true');
	});

	it('takes the text last saved from the page', () => {
		const { container } = render(BoundCodeEditor, { saved: 'other' });
		expect(container.querySelector('.modified')?.textContent).toBe('true');
	});

	it('starts in the state given and binds the state the user leaves it in', async () => {
		const onselectionchange = vi.fn();
		const { container } = render(BoundCodeEditor, { onselectionchange,
			state: { selection: { ranges: [{ anchor: 2, head: 4 }], main: 0 } } });
		const editor = core(container);

		expect(editor.getState().selection).toEqual({ ranges: [{ anchor: 2, head: 4 }], main: 0 });
		editor.view.dispatch({ selection: { anchor: 6 }, userEvent: 'select' });
		expect(onselectionchange).toHaveBeenCalledWith({ ranges: [{ anchor: 6, head: 6 }],
			main: 0 });
		await tick();
		flushSync();
		expect(container.querySelector('.head')?.textContent).toBe('6');
	});

	it('calls back on Ctrl+S, the focus and the size', async () => {
		const onsave = vi.fn();
		const onfocus = vi.fn();
		const onblur = vi.fn();
		const onresize = vi.fn();
		const { container } = render(BoundCodeEditor, { onsave, onfocus, onblur, onresize });

		await fireEvent.keyDown(container.querySelector('.cm-content')!, { key: 's',
			ctrlKey: true });
		expect(onsave).toHaveBeenCalledWith('let a = 1;');
		core(container).focus();
		await tick();
		expect(onfocus).toHaveBeenCalledTimes(1);
		(container.querySelector('#other') as HTMLInputElement).focus();
		await tick();
		expect(onblur).toHaveBeenCalledWith('let a = 1;');
		await fireEvent.keyDown(container.querySelector('.antilia-resizable-handle')!,
			{ key: 'ArrowDown' });
		expect(onresize).toHaveBeenCalledWith({ width: null, height: 100 });
	});

	it('takes other options, keeping the classes of the core', async () => {
		const { container, rerender } = render(BoundCodeEditor);

		await rerender({ lineNumbers: false, readOnly: true, language: 'javascript',
			className: 'other' });
		flushSync();
		await tick();
		const element = editorOf(container);
		expect(element.querySelector('.cm-lineNumbers')).toBeNull();
		expect(element.classList.contains('other')).toBe(true);
		expect(element.classList.contains('antilia-code-editor-readonly')).toBe(true);
		expect(element.classList.contains('antilia-resizable')).toBe(true);
		expect(element.querySelector('.tok-keyword')?.textContent).toBe('let');
	});

	it('posts the text in a hidden text area under a name', () => {
		const { container } = render(BoundCodeEditor, { name: 'script' });
		const field = container.querySelector('textarea')!;

		expect(field.hidden).toBe(true);
		core(container).view.dispatch({ changes: { from: 0, insert: 'x' },
			userEvent: 'input.type' });
		expect(new FormData(container.querySelector('form')!).get('script')).toBe('xlet a = 1;');
	});

	it('shows the errors the page gives, as they change, and none', async () => {
		const { container } = render(BoundCodeEditor, {
			errors: [{ message: 'Unexpected', line: 1, column: 5 }] });
		const element = editorOf(container);
		const editor = core(container);
		const bar = () => element.querySelector<HTMLElement>('.antilia-code-editor-errors');

		expect(element.classList.contains('antilia-code-editor-invalid')).toBe(true);
		expect(bar()?.textContent).toBe('Unexpected');
		expect(bar()?.querySelector('button')).not.toBeNull();

		// the same errors anew, in another array: the bar stays
		const shown = bar();
		await fireEvent.click(container.querySelector('.same-errors')!);
		flushSync();
		expect(bar()).toBe(shown);

		await fireEvent.click(container.querySelector('.other-errors')!);
		flushSync();
		expect(bar()?.textContent).toBe('Not saved');
		expect(bar()?.querySelector('button')).toBeNull();

		await fireEvent.click(container.querySelector('.no-errors')!);
		flushSync();
		expect(bar()).toBeNull();
		expect(element.classList.contains('antilia-code-editor-invalid')).toBe(false);
		// the same editor all along
		expect(core(container)).toBe(editor);
	});

	it('calls onsave when the user leaves it with changes not saved, with saveOnBlur', async () => {
		const onsave = vi.fn();
		const { container } = render(BoundCodeEditor, { onsave });
		const other = container.querySelector('#other')!;
		const leave = () => fireEvent.focusOut(container.querySelector('.cm-content')!,
			{ relatedTarget: other });

		core(container).view.dispatch({ changes: { from: 0, insert: 'x' },
			userEvent: 'input.type' });
		await leave();
		expect(onsave).not.toHaveBeenCalled();

		await fireEvent.click(container.querySelector('.save-on-blur')!);
		flushSync();
		await leave();
		expect(onsave).toHaveBeenCalledWith('xlet a = 1;');
	});

	it('starts saving on blur when told to', async () => {
		const onsave = vi.fn();
		const { container } = render(BoundCodeEditor, { onsave, saveOnBlur: true });

		core(container).view.dispatch({ changes: { from: 0, insert: 'y' },
			userEvent: 'input.type' });
		await fireEvent.focusOut(container.querySelector('.cm-content')!,
			{ relatedTarget: container.querySelector('#other') });
		expect(onsave).toHaveBeenCalledWith('ylet a = 1;');
	});

	it('takes itself away when unmounted', () => {
		const { container, unmount } = render(BoundCodeEditor);
		const editor = core(container);

		unmount();
		expect(editor.view.dom.isConnected).toBe(false);
	});
});
