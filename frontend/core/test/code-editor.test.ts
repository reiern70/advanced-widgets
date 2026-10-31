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

import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { diagnosticCount, forEachDiagnostic } from '@codemirror/lint';
import { EditorState } from '@codemirror/state';
import { EditorView } from '@codemirror/view';

import { CODE_EDITOR_EVENTS, CODE_EDITOR_KIT, CODE_EDITOR_LANGUAGES, CodeEditor, codeEditor,
	codeEditorLanguages, loadLanguage, registerLanguage, setHost, standaloneHost, type Host }
	from '../src/index.js';

/** A moment, for the grammars loading and the state reported after a delay of 0. */
const tick = () => new Promise((resolve) => setTimeout(resolve, 10));

/** CodeMirror tells of the focus 10 ms after it moved. */
const focusTick = () => new Promise((resolve) => setTimeout(resolve, 30));

beforeAll(() => {
	// jsdom lays nothing out: CodeMirror measures nothing
	const rect = { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0,
		toJSON: () => ({}) } as DOMRect;
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => rect;
});

/**
 * @returns a text area holding some text, with a label
 */
function textArea(text = 'let a = 1;\nlet b = 2;', attributes = ''): HTMLTextAreaElement {
	document.body.innerHTML = '<form><label for="code">Script</label><textarea id="code" ' +
		'name="code"' + attributes + '>' + text + '</textarea><input id="other"></form>';
	return document.getElementById('code') as HTMLTextAreaElement;
}

/**
 * Changes the text as the user would.
 */
function type(editor: CodeEditor, text: string, at = 0): void {
	editor.view.dispatch({ changes: { from: at, insert: text }, userEvent: 'input.type' });
}

function select(editor: CodeEditor, anchor: number, head = anchor): void {
	editor.view.dispatch({ selection: { anchor, head }, userEvent: 'select' });
}

afterEach(() => {
	document.body.innerHTML = '';
	setHost(standaloneHost);
	vi.restoreAllMocks();
});

describe('CodeEditor', () => {
	it('takes the nonce of the page for the style element of CodeMirror', () => {
		const script = document.createElement('script');
		script.setAttribute('nonce', 'n0nce');
		document.head.append(script);
		try {
			const editor = new CodeEditor(textArea());

			expect(editor.view.state.facet(EditorView.cspNonce)).toBe('n0nce');
			expect(new CodeEditor(textArea(), { nonce: 'given' }).view.state.facet(
				EditorView.cspNonce)).toBe('given');
		} finally {
			script.remove();
		}
	});

	it('wraps a text area, hiding it, and keeps the text in it', () => {
		const field = textArea();
		const editor = new CodeEditor(field);
		const element = editor.element;

		expect(element.parentElement?.tagName).toBe('FORM');
		expect(element.className).toBe('antilia-code-editor antilia-resizable ' +
			'antilia-resizable-vertical');
		expect(element.firstElementChild).toBe(field);
		expect(element.querySelector('.cm-editor')).not.toBeNull();
		expect(element.lastElementChild?.className).toBe('antilia-resizable-handle');
		expect(field.hidden).toBe(true);
		expect(field.tabIndex).toBe(-1);
		expect(field.getAttribute('aria-hidden')).toBe('true');
		expect(field.className).toBe('antilia-code-editor-field');
		expect(editor.getValue()).toBe('let a = 1;\nlet b = 2;');
		expect(editor.view.contentDOM.getAttribute('aria-label')).toBe('Script');
		expect(element.getAttribute('data-modified')).toBe('false');
		expect(editor.isModified()).toBe(false);
		expect(CodeEditor.of('code')).toBe(editor);
		expect(CodeEditor.of(element)).toBe(editor);

		type(editor, '// ');
		expect(field.value).toBe('// let a = 1;\nlet b = 2;');
		expect(field.defaultValue).toBe('let a = 1;\nlet b = 2;');
	});

	it('works in an element of its own, keeping the text in a text area in it, if any', () => {
		document.body.innerHTML = '<div id="editor"></div><div id="posted"><textarea name="t">' +
			'</textarea></div>';
		const plain = new CodeEditor(document.getElementById('editor')!, { value: 'one',
			label: 'Notes' });

		expect(plain.element.id).toBe('editor');
		expect(plain.field).toBeNull();
		expect(plain.getValue()).toBe('one');
		expect(plain.view.contentDOM.getAttribute('aria-label')).toBe('Notes');

		const posted = new CodeEditor(document.getElementById('posted')!, { value: 'two' });
		expect(posted.field?.name).toBe('t');
		expect(posted.field?.value).toBe('two');
	});

	it('highlights the text with the grammar of its language, loaded as it is first shown', async () => {
		const editor = new CodeEditor(textArea(), { language: 'javascript' });
		await tick();

		expect(editor.view.contentDOM.getAttribute('data-language')).toBe('javascript');
		expect(editor.element.querySelector('.tok-keyword')?.textContent).toBe('let');
		expect(editor.element.querySelector('.tok-number')?.textContent).toBe('1');

		// loaded, a grammar shows from the start
		const again = new CodeEditor(textArea(), { language: 'JS' });
		expect(again.element.querySelector('.tok-keyword')?.textContent).toBe('let');
	});

	it('knows the grammars of Java, JavaScript, TypeScript, JSON and Markdown', async () => {
		expect(CODE_EDITOR_LANGUAGES).toEqual(['java', 'javascript', 'typescript', 'json',
			'markdown']);
		for (const name of ['java', 'typescript', 'json', 'markdown', 'md', 'ts']) {
			expect(await loadLanguage(name)).not.toBeNull();
		}
		expect(await loadLanguage('cobol')).toBeNull();

		const java = new CodeEditor(textArea('public class A {}'), { language: 'java' });
		await tick();
		expect(java.element.querySelector('.tok-keyword')?.textContent).toBe('public');
	});

	it('highlights the code blocks of Markdown in their languages', async () => {
		const editor = new CodeEditor(textArea('# Title\n\n```java\nclass A {}\n```\n'),
			{ language: 'markdown' });
		await tick();
		await tick();

		expect(Array.from(editor.element.querySelectorAll('.tok-heading'))
			.map((token) => token.textContent).join('')).toBe('# Title');
		// once CodeMirror parses the block again, with the grammar of Java
		await vi.waitFor(() => expect(Array.from(editor.element.querySelectorAll('.tok-keyword'))
			.map((token) => token.textContent)).toContain('class'), { timeout: 3000 });
	});

	it('takes grammars of the page, made from the kit or given as extensions', async () => {
		registerLanguage('log', (kit) => kit.StreamLanguage.define({
			token(stream) {
				if (stream.match(/^ERROR\b/)) {
					return 'keyword';
				}
				stream.next();
				return null;
			}
		}), ['logs']);
		expect(codeEditorLanguages()).toContain('log');

		const log = new CodeEditor(textArea('ERROR boom'), { language: 'logs' });
		await tick();
		expect(log.element.querySelector('.tok-keyword')?.textContent).toBe('ERROR');

		const given = new CodeEditor(textArea('ERROR boom'), {
			language: CODE_EDITOR_KIT.StreamLanguage.define({ token(stream) {
				stream.skipToEnd();
				return 'comment';
			} })
		});
		expect(given.element.querySelector('.tok-comment')?.textContent).toBe('ERROR boom');

		// a grammar that does not load leaves the text plain, and says so
		const error = vi.fn();
		setHost({ ...standaloneHost, log: { error } });
		registerLanguage('broken', () => Promise.reject(new Error('gone')));
		const broken = new CodeEditor(textArea('ERROR'), { language: 'broken' });
		await tick();
		expect(broken.element.querySelector('.tok-keyword')).toBeNull();
		expect(error).toHaveBeenCalledWith('The grammar of broken did not load',
			expect.any(Error));
	});

	it('tells of the user changing the text: callbacks, events and the text area', () => {
		const field = textArea();
		const onChange = vi.fn();
		const onModifiedChange = vi.fn();
		const editor = new CodeEditor(field, { onChange, onModifiedChange });
		const events: string[] = [];
		document.body.addEventListener('input', () => events.push('input'));
		document.body.addEventListener(CODE_EDITOR_EVENTS.change, () => events.push('change'));
		document.body.addEventListener(CODE_EDITOR_EVENTS.modified, (event) => events.push(
			'modified ' + (event as CustomEvent).detail.modified));

		type(editor, 'x');
		expect(onChange).toHaveBeenCalledWith('xlet a = 1;\nlet b = 2;');
		expect(onModifiedChange).toHaveBeenCalledWith(true);
		expect(editor.isModified()).toBe(true);
		expect(editor.element.getAttribute('data-modified')).toBe('true');
		expect(events).toEqual(['modified true', 'input', 'change']);

		editor.view.dispatch({ changes: { from: 0, to: 1 }, userEvent: 'delete' });
		expect(onModifiedChange).toHaveBeenLastCalledWith(false);
		expect(editor.element.getAttribute('data-modified')).toBe('false');
	});

	it('changes the text for the page without calling back', () => {
		const onChange = vi.fn();
		const onModifiedChange = vi.fn();
		const editor = new CodeEditor(textArea(), { onChange, onModifiedChange });

		editor.setValue('const c = 3;');
		expect(editor.getValue()).toBe('const c = 3;');
		expect(editor.field?.value).toBe('const c = 3;');
		expect(onChange).not.toHaveBeenCalled();
		expect(editor.isModified()).toBe(true);

		editor.setValue('const d = 4;', true);
		expect(editor.isModified()).toBe(false);
		expect(editor.field?.defaultValue).toBe('const d = 4;');
		expect(onModifiedChange.mock.calls).toEqual([[true], [false]]);
	});

	it('takes the text as saved, telling the tabs and the dialogs of the page', () => {
		const field = textArea();
		const editor = new CodeEditor(field, { saved: 'old' });

		expect(editor.isModified()).toBe(true);
		expect(field.defaultValue).toBe('old');
		expect(field.value).toBe('let a = 1;\nlet b = 2;');

		editor.markSaved();
		expect(editor.isModified()).toBe(false);
		expect(field.defaultValue).toBe('let a = 1;\nlet b = 2;');
		expect(editor.element.getAttribute('data-modified')).toBe('false');
		editor.markSaved('else');
		expect(editor.isModified()).toBe(true);
	});

	it('puts the saved text back when a tab or a window holding it is closed anyway', () => {
		document.body.innerHTML = '<div id="panel"><textarea id="t">saved</textarea></div>' +
			'<div id="elsewhere"></div>';
		const editor = new CodeEditor(document.getElementById('t')!);
		type(editor, 'not ');

		document.getElementById('elsewhere')!.dispatchEvent(new CustomEvent(
			'antilia-tabs:discard', { bubbles: true }));
		expect(editor.getValue()).toBe('not saved');
		document.getElementById('panel')!.dispatchEvent(new CustomEvent('antilia-desktop:discard',
			{ bubbles: true }));
		expect(editor.getValue()).toBe('saved');
		expect(editor.isModified()).toBe(false);
	});

	it('takes the text a page put back into the text area', () => {
		const field = textArea('one');
		const editor = new CodeEditor(field);
		type(editor, 'x');

		field.value = field.defaultValue;
		field.dispatchEvent(new Event('input', { bubbles: true }));
		expect(editor.getValue()).toBe('one');
	});

	it('tells of the focus, and of the text changed once it is lost', async () => {
		const field = textArea();
		const onFocus = vi.fn();
		const onBlur = vi.fn();
		const editor = new CodeEditor(field, { onFocus, onBlur });
		const changes = vi.fn();
		field.addEventListener('change', changes);
		const events: string[] = [];
		for (const name of ['focus', 'blur'] as const) {
			field.addEventListener(CODE_EDITOR_EVENTS[name], () => events.push(name));
		}

		editor.focus();
		await focusTick();
		expect(onFocus).toHaveBeenCalledTimes(1);
		document.getElementById('other')!.focus();
		await focusTick();
		expect(onBlur).toHaveBeenCalledWith('let a = 1;\nlet b = 2;');
		expect(changes).not.toHaveBeenCalled();

		editor.focus();
		await focusTick();
		type(editor, '!');
		document.getElementById('other')!.focus();
		await focusTick();
		expect(changes).toHaveBeenCalledTimes(1);
		expect(events).toEqual(['focus', 'blur', 'focus', 'blur']);
	});

	it('puts the focus in the text on a click on the label of the text area', () => {
		const editor = new CodeEditor(textArea());

		document.querySelector('label')!.click();
		expect(editor.view.hasFocus).toBe(true);
	});

	it('tells the page to save on Ctrl+S', () => {
		const onSave = vi.fn();
		const editor = new CodeEditor(textArea(), { onSave });
		const saved = vi.fn();
		editor.field!.addEventListener(CODE_EDITOR_EVENTS.save, saved);

		const event = new KeyboardEvent('keydown', { key: 's', ctrlKey: true, bubbles: true,
			cancelable: true });
		editor.view.contentDOM.dispatchEvent(event);

		expect(event.defaultPrevented).toBe(true);
		expect(onSave).toHaveBeenCalledWith('let a = 1;\nlet b = 2;');
		expect(saved).toHaveBeenCalledTimes(1);
	});

	it('starts in the state given: the selection, the scroll position and the size', () => {
		const editor = new CodeEditor(textArea(), { state: {
			selection: { ranges: [{ anchor: 4, head: 5 }, { anchor: 15, head: 15 }], main: 1 },
			scrollTop: 40, scrollLeft: 0, height: 240, width: 500 } });

		expect(editor.getState()).toEqual({
			selection: { ranges: [{ anchor: 4, head: 5 }, { anchor: 15, head: 15 }], main: 1 },
			scrollTop: 40, scrollLeft: 0, width: null, height: 240 });
		expect(editor.element.style.getPropertyValue('--antilia-resizable-height')).toBe('240px');
	});

	it('keeps a selection given within the text, the start for none', () => {
		const editor = new CodeEditor(textArea('short'), { state: {
			selection: { ranges: [{ anchor: 2, head: 99 }], main: 7 } } });
		expect(editor.getState().selection).toEqual({ ranges: [{ anchor: 2, head: 5 }], main: 0 });

		const none = new CodeEditor(textArea('short'), { state: {
			selection: { ranges: [{ anchor: Number.NaN, head: 1 }], main: 0 } } });
		expect(none.getState().selection).toEqual({ ranges: [{ anchor: 0, head: 0 }], main: 0 });
	});

	it('reports the state a moment after the user changed it, and as it loses the focus', async () => {
		const onStateChange = vi.fn();
		const onSelectionChange = vi.fn();
		const editor = new CodeEditor(textArea(), { stateDelay: 0, onStateChange,
			onSelectionChange });
		const selects = vi.fn();
		editor.field!.addEventListener(CODE_EDITOR_EVENTS.select, selects);

		select(editor, 2, 6);
		expect(onSelectionChange).toHaveBeenCalledWith({ ranges: [{ anchor: 2, head: 6 }],
			main: 0 });
		expect(selects).toHaveBeenCalledTimes(1);
		expect(onStateChange).not.toHaveBeenCalled();
		// a moment, longer when the machine is busy
		await vi.waitFor(() => expect(onStateChange).toHaveBeenCalledTimes(1));
		expect(onStateChange).toHaveBeenCalledWith(expect.objectContaining({
			selection: { ranges: [{ anchor: 2, head: 6 }], main: 0 } }),
		{ leaving: false, changed: false });

		// the same state again is not reported
		select(editor, 2, 6);
		await tick();
		expect(onStateChange).toHaveBeenCalledTimes(1);

		editor.focus();
		await focusTick();
		type(editor, 'x');
		document.getElementById('other')!.focus();
		await focusTick();
		expect(onStateChange).toHaveBeenCalledTimes(2);
		expect(onStateChange.mock.calls[1][1]).toEqual({ leaving: false, changed: true });
		await tick();
		expect(onStateChange).toHaveBeenCalledTimes(2);
	});

	it('reports the state as the page goes, when it has not yet', () => {
		const onStateChange = vi.fn();
		const editor = new CodeEditor(textArea(), { onStateChange });

		window.dispatchEvent(new Event('pagehide'));
		expect(onStateChange).not.toHaveBeenCalled();
		select(editor, 3);
		window.dispatchEvent(new Event('pagehide'));
		expect(onStateChange).toHaveBeenCalledWith(expect.objectContaining({
			selection: { ranges: [{ anchor: 3, head: 3 }], main: 0 } }),
		{ leaving: true, changed: false });
	});

	it('reports the size the user gave it', async () => {
		const onResize = vi.fn();
		const onStateChange = vi.fn();
		const editor = new CodeEditor(textArea(), { stateDelay: 0, onResize, onStateChange,
			minHeight: 100 });
		const resized = vi.fn();
		editor.field!.addEventListener(CODE_EDITOR_EVENTS.resize, resized);
		const handle = editor.element.querySelector('.antilia-resizable-handle')!;

		// laid out nowhere, the editor is as high as it may be at least
		handle.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
		expect(onResize).toHaveBeenCalledWith({ width: null, height: 100 });
		handle.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
		expect(onResize).toHaveBeenLastCalledWith({ width: null, height: 110 });
		expect(resized).toHaveBeenCalledTimes(2);
		await tick();
		expect(onStateChange).toHaveBeenCalledWith(expect.objectContaining({ height: 110 }),
			{ leaving: false, changed: false });
	});

	it('scrolls back where it was once shown again', () => {
		let observed: ResizeObserverCallback | undefined;
		vi.stubGlobal('ResizeObserver', class {
			constructor(callback: ResizeObserverCallback) {
				observed = callback;
			}

			observe(): void {
				return undefined;
			}

			disconnect(): void {
				return undefined;
			}
		});
		try {
			const editor = new CodeEditor(textArea(), { state: { scrollTop: 120, scrollLeft: 8 } });
			const scroller = editor.view.scrollDOM;
			let height = 0;
			const set: number[] = [];
			Object.defineProperty(scroller, 'clientHeight', { get: () => height });
			Object.defineProperty(scroller, 'scrollTop', { get: () => 0, set: (value: number) =>
				set.push(value) });

			// hidden, a scroller at its start does not lose where the editor was
			scroller.dispatchEvent(new Event('scroll'));
			expect(editor.getState().scrollTop).toBe(120);

			height = 200;
			observed!([], {} as ResizeObserver);
			expect(set).toEqual([120]);
			expect(editor.getState()).toEqual(expect.objectContaining({ scrollTop: 120,
				scrollLeft: 8 }));
		} finally {
			vi.unstubAllGlobals();
		}
	});

	it('takes another state from the page', () => {
		const onStateChange = vi.fn();
		const editor = new CodeEditor(textArea(), { onStateChange });

		editor.setState({ selection: { ranges: [{ anchor: 1, head: 3 }], main: 0 }, height: 300 });
		expect(editor.getState()).toEqual(expect.objectContaining({
			selection: { ranges: [{ anchor: 1, head: 3 }], main: 0 }, height: 300 }));
		editor.setState({ scrollTop: 10 });
		expect(editor.getState().scrollTop).toBe(10);
		expect(onStateChange).not.toHaveBeenCalled();
	});

	it('lets the user read, but not change, a text read only', () => {
		const editor = new CodeEditor(textArea(), { readOnly: true });

		expect(editor.view.state.facet(EditorState.readOnly)).toBe(true);
		expect(editor.view.state.facet(EditorView.editable)).toBe(true);
		expect(editor.element.classList.contains('antilia-code-editor-readonly')).toBe(true);

		const field = new CodeEditor(textArea('x', ' readonly'));
		expect(field.view.state.facet(EditorState.readOnly)).toBe(true);
	});

	it('shows a text not selectable as a picture, and one disabled dimmed', () => {
		const picture = new CodeEditor(textArea(), { selectable: false });
		expect(picture.view.state.facet(EditorView.editable)).toBe(false);
		expect(picture.element.classList.contains('antilia-code-editor-unselectable')).toBe(true);

		const disabled = new CodeEditor(textArea('x', ' disabled'));
		expect(disabled.view.state.facet(EditorView.editable)).toBe(false);
		expect(disabled.view.state.facet(EditorState.readOnly)).toBe(true);
		expect(disabled.element.classList.contains('antilia-code-editor-disabled')).toBe(true);
	});

	it('shows line numbers and fold arrows unless told otherwise', () => {
		const editor = new CodeEditor(textArea(), { placeholder: 'Type' });
		expect(editor.element.querySelector('.cm-lineNumbers')).not.toBeNull();
		expect(editor.element.querySelector('.cm-foldGutter')).not.toBeNull();

		editor.configure({ lineNumbers: false, folding: false });
		expect(editor.element.querySelector('.cm-gutters')).toBeNull();

		editor.setValue('');
		expect(editor.element.querySelector('.cm-placeholder')?.textContent).toBe('Type');
	});

	it('takes other options, keeping the text and the state', async () => {
		const editor = new CodeEditor(textArea(), { state: { selection: {
			ranges: [{ anchor: 3, head: 3 }], main: 0 } } });

		editor.configure({ language: 'javascript', lineWrapping: true, tabSize: 2,
			readOnly: true, label: 'Code', texts: { phrases: { Find: 'Suchen' } } });
		await tick();
		expect(editor.element.querySelector('.tok-keyword')).not.toBeNull();
		expect(editor.view.contentDOM.classList.contains('cm-lineWrapping')).toBe(true);
		expect(editor.view.state.tabSize).toBe(2);
		expect(editor.view.state.phrase('Find')).toBe('Suchen');
		expect(editor.view.contentDOM.getAttribute('aria-label')).toBe('Code');
		expect(editor.element.classList.contains('antilia-code-editor-readonly')).toBe(true);
		expect(editor.getState().selection?.ranges[0].head).toBe(3);

		editor.configure({ resizable: false });
		expect(editor.element.querySelector('.antilia-resizable-handle')).toBeNull();
		expect(editor.element.classList.contains('antilia-resizable')).toBe(false);
		editor.configure({ resizable: 'both' });
		expect(editor.element.classList.contains('antilia-resizable-both')).toBe(true);
	});

	it('takes the dark look of CodeMirror in a dark theme', () => {
		document.body.innerHTML = '<div id="dark"></div>';
		const element = document.getElementById('dark')!;
		element.style.setProperty('--antilia-theme-color-scheme', 'dark');

		const editor = new CodeEditor(element);
		expect(editor.view.state.facet(EditorView.darkTheme)).toBe(true);
	});

	it('takes itself away, showing the text area again where it was', () => {
		const field = textArea();
		const editor = new CodeEditor(field);
		type(editor, '1');

		editor.destroy();
		expect(field.parentElement?.tagName).toBe('FORM');
		expect(field.hidden).toBe(false);
		expect(field.value).toBe('1let a = 1;\nlet b = 2;');
		expect(field.hasAttribute('class')).toBe(false);
		expect(field.hasAttribute('aria-hidden')).toBe(false);
		expect(document.querySelector('.antilia-code-editor')).toBeNull();
		expect(CodeEditor.of('code')).toBeNull();
		editor.destroy();

		document.body.innerHTML = '<div id="own" class="box"></div>';
		const own = new CodeEditor(document.getElementById('own')!);
		own.destroy();
		expect(document.body.innerHTML).toBe('<div id="own" class="box"></div>');
	});

	it('attached again to its text area, gives way to the new editor', () => {
		const field = textArea();
		const first = CodeEditor.attach('code')!;
		select(first, 4);
		const second = CodeEditor.attach('code', { language: 'javascript' })!;

		expect(second.element).toBe(first.element);
		expect(second.element.querySelectorAll('.cm-editor')).toHaveLength(1);
		expect(second.element.querySelectorAll('.antilia-resizable-handle')).toHaveLength(1);
		expect(second.getState().selection?.ranges[0].head).toBe(4);
		second.destroy();
		expect(field.parentElement?.tagName).toBe('FORM');
		expect(CodeEditor.attach('nothing')).toBeNull();
	});

	it('rendered anew by the page with the same text, goes on as it was', () => {
		let removing: ((element: Element) => void) | undefined;
		const host: Host = { ...standaloneHost, onNodeRemoving: (listener) => {
			removing = listener;
			return () => undefined;
		} };
		setHost(host);
		const onStateChange = vi.fn();
		const field = textArea();
		const editor = new CodeEditor(field, { onStateChange });
		type(editor, 'x');
		select(editor, 5);
		window.dispatchEvent(new Event('pagehide'));
		select(editor, 7);

		// the page replaces the text area, which the editor gives back first
		removing!(field);
		expect(field.parentElement?.tagName).toBe('FORM');
		expect(document.querySelector('.antilia-code-editor')).toBeNull();
		const rendered = document.createElement('textarea');
		rendered.id = 'code';
		rendered.textContent = 'xlet a = 1;\nlet b = 2;';
		field.replaceWith(rendered);

		// the server's state is the one it was told, as Wicket renders it without the sizes of
		// the page: the editor goes on where the user was
		const told = onStateChange.mock.calls[0][0];
		const again = new CodeEditor(rendered, { state: { selection: told.selection,
			scrollTop: told.scrollTop, scrollLeft: told.scrollLeft } });
		expect(again.getState().selection?.ranges[0].head).toBe(7);
		expect(again.isModified()).toBe(true);
		expect(rendered.defaultValue).toBe('let a = 1;\nlet b = 2;');
	});

	it('rendered anew with another text or state from the page, takes them', () => {
		let removing: ((element: Element) => void) | undefined;
		setHost({ ...standaloneHost, onNodeRemoving: (listener) => {
			removing = listener;
			return () => undefined;
		} });
		const field = textArea();
		const editor = new CodeEditor(field, { onStateChange: () => undefined });
		type(editor, 'x');
		select(editor, 5);
		window.dispatchEvent(new Event('pagehide'));

		removing!(field);
		textArea('another text');
		const other = new CodeEditor(document.getElementById('code')!);
		expect(other.isModified()).toBe(false);
		expect(other.getState().selection?.ranges[0].head).toBe(0);

		const editorAgain = other;
		select(editorAgain, 2);
		const moved = CodeEditor.attach('code', { state: { selection: {
			ranges: [{ anchor: 9, head: 9 }], main: 0 } } })!;
		expect(moved.getState().selection?.ranges[0].head).toBe(2);
	});

	it('works as a Svelte action', () => {
		document.body.innerHTML = '<div id="e"></div>';
		const element = document.getElementById('e')!;
		const action = codeEditor(element, { value: 'text', lineNumbers: false });

		expect(element.querySelector('.cm-lineNumbers')).toBeNull();
		action.update({ lineNumbers: true });
		expect(element.querySelector('.cm-lineNumbers')).not.toBeNull();
		action.destroy();
		expect(element.innerHTML).toBe('');
	});
});

/**
 * Moves the focus out of an element, as the browser tells it: focusout with the element taking it.
 */
function leave(element: HTMLElement, to: Element | null): void {
	element.dispatchEvent(new FocusEvent('focusout', { bubbles: true, relatedTarget: to }));
}

/**
 * @returns the diagnostics of CodeMirror: their ranges and messages
 */
function diagnostics(editor: CodeEditor): Array<[number, number, string]> {
	const found: Array<[number, number, string]> = [];
	forEachDiagnostic(editor.view.state, (diagnostic, from, to) => {
		found.push([from, to, diagnostic.message]);
	});
	return found;
}

describe('CodeEditor scroll bars', () => {
	it('scrolls the text with the scroll bars of the theme, around CodeMirror\'s scroller', () => {
		const editor = new CodeEditor(textArea());
		const scroller = editor.view.scrollDOM;
		const wrapper = scroller.parentElement!;

		expect(wrapper.classList.contains('antilia-scrollbar')).toBe(true);
		expect(wrapper.parentElement).toBe(editor.view.dom);
		expect(wrapper.parentElement?.classList.contains('cm-editor')).toBe(true);
		expect(scroller.classList.contains('antilia-scrollbar-host')).toBe(true);
		expect(wrapper.querySelectorAll(':scope > .antilia-scrollbar-track')).toHaveLength(2);
		expect(wrapper.classList.contains('antilia-scrollbar--both')).toBe(true);
	});

	it('takes the scroll bars away with the editor', () => {
		const field = textArea();
		const editor = new CodeEditor(field);
		const scroller = editor.view.scrollDOM;
		const wrapper = scroller.parentElement!;

		editor.destroy();
		expect(wrapper.querySelector('.antilia-scrollbar-track')).toBeNull();
		expect(scroller.classList.contains('antilia-scrollbar-host')).toBe(false);
		expect(document.querySelector('.antilia-scrollbar')).toBeNull();
		expect(document.querySelector('.cm-editor')).toBeNull();
		expect(field.parentElement?.tagName).toBe('FORM');
	});

	it('rendered anew in the same place, leaves one wrapper of scroll bars', () => {
		textArea();
		CodeEditor.attach('code');
		const again = CodeEditor.attach('code')!;

		expect(again.element.querySelectorAll('.antilia-scrollbar')).toHaveLength(1);
		expect(again.view.scrollDOM.parentElement?.classList.contains('antilia-scrollbar'))
			.toBe(true);
	});
});

describe('CodeEditor saving on blur', () => {
	it('tells the page to save when the user leaves it with changes not saved', () => {
		const onSave = vi.fn();
		const editor = new CodeEditor(textArea(), { saveOnBlur: true, onSave });
		const saved = vi.fn();
		editor.field!.addEventListener(CODE_EDITOR_EVENTS.save, saved);
		const other = document.getElementById('other')!;

		// nothing to save yet
		leave(editor.view.contentDOM, other);
		expect(onSave).not.toHaveBeenCalled();

		type(editor, 'x');
		leave(editor.view.contentDOM, other);
		expect(onSave).toHaveBeenCalledWith('xlet a = 1;\nlet b = 2;');
		expect(saved).toHaveBeenCalledTimes(1);

		// the focus going nowhere, to another window for example, leaves the editor too
		leave(editor.view.contentDOM, null);
		expect(onSave).toHaveBeenCalledTimes(2);

		// saved, nothing more to save
		editor.markSaved();
		leave(editor.view.contentDOM, other);
		expect(onSave).toHaveBeenCalledTimes(2);
	});

	it('tells the page to save again while it shows errors, the text being as saved', () => {
		const onSave = vi.fn();
		const editor = new CodeEditor(textArea(), { saveOnBlur: true, onSave });
		const other = document.getElementById('other')!;
		editor.setErrors([{ message: 'Not valid', line: 1, column: 1 }]);

		// the user put the text back as it was saved: the errors are no longer true
		leave(editor.view.contentDOM, other);
		expect(onSave).toHaveBeenCalledWith('let a = 1;\nlet b = 2;');

		editor.setErrors([]);
		leave(editor.view.contentDOM, other);
		expect(onSave).toHaveBeenCalledTimes(1);
	});

	it('does not save while the focus stays in the editor, in its search panel for example', () => {
		const onSave = vi.fn();
		const editor = new CodeEditor(textArea(), { saveOnBlur: true, onSave });
		type(editor, 'x');
		const inside = document.createElement('input');
		editor.element.append(inside);

		leave(editor.view.contentDOM, inside);
		expect(onSave).not.toHaveBeenCalled();
	});

	it('does not save on blur unless told to, nor a text read only', () => {
		const onSave = vi.fn();
		const editor = new CodeEditor(textArea(), { onSave });
		const other = document.getElementById('other')!;
		type(editor, 'x');

		leave(editor.view.contentDOM, other);
		expect(onSave).not.toHaveBeenCalled();

		editor.configure({ saveOnBlur: true, readOnly: true });
		leave(editor.view.contentDOM, other);
		expect(onSave).not.toHaveBeenCalled();

		editor.configure({ readOnly: false });
		leave(editor.view.contentDOM, other);
		expect(onSave).toHaveBeenCalledTimes(1);
	});

	it('saves when the focus really leaves it', async () => {
		const onSave = vi.fn();
		const editor = new CodeEditor(textArea(), { saveOnBlur: true, onSave });
		editor.focus();
		await focusTick();
		type(editor, 'x');
		document.getElementById('other')!.focus();
		await focusTick();

		expect(onSave).toHaveBeenCalledTimes(1);
	});
});

describe('CodeEditor errors', () => {
	const text = '{\n  "a": 1,\n  "b": ]\n}';

	it('shows the errors the page gives: underlined, listed and the editor invalid', () => {
		const editor = new CodeEditor(textArea(text), {
			errors: [{ message: 'A value was expected', line: 3, column: 8 },
				{ message: 'Not saved' }],
			texts: { errors: 'Errors in the text' }
		});
		const element = editor.element;
		const bar = element.querySelector<HTMLElement>('.antilia-code-editor-errors')!;

		expect(element.classList.contains('antilia-code-editor-invalid')).toBe(true);
		expect(bar.getAttribute('role')).toBe('alert');
		expect(bar.getAttribute('aria-label')).toBe('Errors in the text');
		expect(bar.closest('.cm-panels-bottom')).not.toBeNull();
		const items = Array.from(bar.children) as HTMLElement[];
		expect(items.map((item) => item.textContent)).toEqual(['A value was expected',
			'Not saved']);
		expect(items.map((item) => item.tagName)).toEqual(['BUTTON', 'DIV']);
		expect(items[0].className).toBe('antilia-code-editor-error');
		expect((items[0] as HTMLButtonElement).type).toBe('button');
		expect(editor.view.contentDOM.getAttribute('aria-invalid')).toBe('true');
		expect(editor.view.contentDOM.getAttribute('aria-describedby')).toBe(bar.id);
		// line 3, column 8: the ] after "b":
		const at = text.indexOf(']');
		expect(diagnosticCount(editor.view.state)).toBe(1);
		expect(diagnostics(editor)).toEqual([[at, at + 1, 'A value was expected']]);
	});

	it('goes to an error the user clicks in the bar', () => {
		const editor = new CodeEditor(textArea(text));
		const focus = vi.spyOn(editor.view, 'focus');
		editor.setErrors([{ message: 'Here', line: 3, column: 8 }]);

		editor.element.querySelector<HTMLButtonElement>('.antilia-code-editor-error')!.click();
		expect(editor.view.state.selection.main.head).toBe(text.indexOf(']'));
		expect(focus).toHaveBeenCalled();
	});

	it('places an error by its range, by its line alone, and within the text', () => {
		const editor = new CodeEditor(textArea(text));
		const lines = text.split('\n');
		editor.setErrors([
			{ message: 'range', from: 2, to: 5 },
			{ message: 'from', from: 4 },
			{ message: 'line', line: 2 },
			{ message: 'beyond', line: 99, column: 99 },
			{ message: 'before', line: -3, column: -3 },
			{ message: 'past the end', from: 1000, to: 2000 }
		]);

		const line2 = lines[0].length + 1;
		const last = text.length - lines[3].length;
		expect(diagnostics(editor)).toEqual([
			[0, 1, 'before'],
			[2, 5, 'range'],
			[4, 5, 'from'],
			[line2, line2 + 1, 'line'],
			// the last line holds one character: the end of the text, a point there
			[text.length, text.length, 'beyond'],
			[text.length, text.length, 'past the end']
		].sort((one, other) => (one[0] as number) - (other[0] as number) ||
			(one[1] as number) - (other[1] as number)) as Array<[number, number, string]>);
		expect(last).toBe(text.length - 1);
		expect(editor.element.querySelectorAll('button.antilia-code-editor-error')).toHaveLength(6);
	});

	it('takes the errors away when the page gives none', () => {
		const editor = new CodeEditor(textArea(text), { errors: [{ message: 'x', line: 1 }] });

		editor.setErrors([]);
		expect(editor.element.classList.contains('antilia-code-editor-invalid')).toBe(false);
		expect(editor.element.querySelector('.antilia-code-editor-errors')).toBeNull();
		expect(editor.view.contentDOM.hasAttribute('aria-invalid')).toBe(false);
		expect(editor.view.contentDOM.hasAttribute('aria-describedby')).toBe(false);
		expect(diagnosticCount(editor.view.state)).toBe(0);
		expect(editor.getErrors()).toEqual([]);

		editor.setErrors([{ message: 'y' }]);
		editor.setErrors();
		expect(editor.element.querySelector('.antilia-code-editor-errors')).toBeNull();
	});

	it('leaves out errors without a message', () => {
		const editor = new CodeEditor(textArea(text));
		editor.setErrors([{ message: '' }, null as unknown as { message: string },
			{ line: 1 } as unknown as { message: string }]);

		expect(editor.getErrors()).toEqual([]);
		expect(editor.element.classList.contains('antilia-code-editor-invalid')).toBe(false);
		editor.setErrors('none' as unknown as []);
		expect(editor.getErrors()).toEqual([]);
	});

	it('keeps the bar when given the same errors again, underlining them anew', () => {
		const editor = new CodeEditor(textArea(text));
		editor.setErrors([{ message: 'Here', line: 3, column: 8 }]);
		const bar = editor.element.querySelector('.antilia-code-editor-errors');

		// the user typed before the error, which moves with the text
		type(editor, 'xx');
		const at = text.indexOf(']') + 2;
		expect(diagnostics(editor)).toEqual([[at, at + 1, 'Here']]);

		editor.setErrors([{ message: 'Here', line: 3, column: 8 }]);
		expect(editor.element.querySelector('.antilia-code-editor-errors')).toBe(bar);
		// line 3, column 8 of the text as it is now
		const now = editor.getValue();
		const line3 = now.indexOf('\n', now.indexOf('\n') + 1) + 1;
		expect(diagnostics(editor)).toEqual([[line3 + 7, line3 + 8, 'Here']]);

		editor.setErrors([{ message: 'Other' }]);
		const other = editor.element.querySelector('.antilia-code-editor-errors');
		expect(other).not.toBe(bar);
		expect(other?.textContent).toBe('Other');
		expect(diagnosticCount(editor.view.state)).toBe(0);
	});

	it('keeps the bar, and the view, while other options change, the phrases too', () => {
		const editor = new CodeEditor(textArea(text), { errors: [{ message: 'x', line: 1 }],
			texts: { phrases: { Find: 'Suchen' } } });
		const bar = editor.element.querySelector('.antilia-code-editor-errors');
		const content = editor.view.contentDOM.firstElementChild;

		editor.configure({ lineNumbers: false, texts: { phrases: { Find: 'Suchen' } } });
		expect(editor.element.querySelector('.antilia-code-editor-errors')).toBe(bar);
		expect(editor.view.contentDOM.firstElementChild).toBe(content);

		// other phrases: CodeMirror draws the view anew, in them
		editor.configure({ texts: { phrases: { Find: 'Buscar' } } });
		expect(editor.view.state.phrase('Find')).toBe('Buscar');
		expect(editor.element.querySelector('.antilia-code-editor-errors')?.textContent).toBe('x');
	});

	it('takes errors with other options, and gives copies of them', () => {
		const editor = new CodeEditor(textArea(text));
		const given = [{ message: 'From the options', line: 1, column: 1 }];

		editor.configure({ errors: given });
		expect(editor.element.querySelector('.antilia-code-editor-errors')?.textContent)
			.toBe('From the options');
		given[0].message = 'changed';
		const errors = editor.getErrors();
		expect(errors).toEqual([{ message: 'From the options', line: 1, column: 1 }]);
		errors[0].message = 'changed too';
		expect(editor.getErrors()[0].message).toBe('From the options');

		// other options leave the errors
		editor.configure({ lineNumbers: false });
		expect(editor.getErrors()).toHaveLength(1);
		editor.configure({ errors: [] });
		expect(editor.getErrors()).toEqual([]);
	});

	it('keeps the class of an invalid editor when a framework wrote other classes over it', () => {
		const editor = new CodeEditor(textArea(text), { errors: [{ message: 'x' }] });

		editor.element.className = 'other';
		editor.configure({});
		expect(editor.element.classList.contains('antilia-code-editor-invalid')).toBe(true);
		expect(editor.element.classList.contains('other')).toBe(true);
	});

	it('names the bar in English unless told otherwise, and each bar by its own id', () => {
		const first = new CodeEditor(textArea(text), { errors: [{ message: 'x' }] });
		const bar = first.element.querySelector('.antilia-code-editor-errors')!;
		expect(bar.getAttribute('aria-label')).toBe('Errors');

		document.body.insertAdjacentHTML('beforeend', '<div id="second"></div>');
		const second = new CodeEditor(document.getElementById('second')!, { value: 'y',
			errors: [{ message: 'y' }] });
		const other = second.element.querySelector('.antilia-code-editor-errors')!;
		expect(other.id).not.toBe(bar.id);
	});

	it('takes the class of an invalid editor away with the editor', () => {
		document.body.innerHTML = '<div id="own"></div>';
		const editor = new CodeEditor(document.getElementById('own')!, { value: 'x',
			errors: [{ message: 'x' }] });

		editor.destroy();
		expect(document.body.innerHTML).toBe('<div id="own"></div>');
	});
});

