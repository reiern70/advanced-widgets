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

import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { TextSelection } from '@tiptap/pm/state';

import { BOOTSTRAP_LOOK, DEFAULT_RICH_TEXT_TOOLBAR, isSafeUrl, Popover, quietHtml,
	registerRichTextExtension, RICH_TEXT_EDITOR_CLASSES,
	RICH_TEXT_EDITOR_EVENTS, RICH_TEXT_EDITOR_KIT, RICH_TEXT_FEATURES, RichTextEditor,
	richTextEditor, richTextExtensions, setHost, shortcutText, standaloneHost,
	type Host } from '../src/index.js';

/** A moment, for the state reported after a delay of 0 and the focus Tiptap gives in a frame. */
const tick = (ms = 20) => new Promise((resolve) => setTimeout(resolve, ms));

beforeAll(() => {
	// jsdom lays nothing out: ProseMirror measures nothing
	const rect = { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0,
		toJSON: () => ({}) } as DOMRect;
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => rect;
	document.elementFromPoint = () => null;
});

const ARTICLE = '<h2>Title</h2><p>Hello <strong>world</strong></p>';

/**
 * @returns a text area holding some HTML, with a label, in a form
 */
function textArea(html = ARTICLE, attributes = ''): HTMLTextAreaElement {
	document.body.innerHTML = '<form><label for="notes">Notes</label><textarea id="notes" ' +
		'name="notes"' + attributes + '></textarea><input id="other"></form>';
	const field = document.getElementById('notes') as HTMLTextAreaElement;
	field.defaultValue = html;
	return field;
}

/**
 * @returns the element of a tool of the editor's toolbar
 */
function tool(editor: RichTextEditor, name: string): HTMLButtonElement {
	return editor.element.querySelector<HTMLButtonElement>(
		`.antilia-rich-text-toolbar [data-tool="${name}"]`)!;
}

/**
 * Types as the user does: a transaction of the user's, at the cursor.
 */
function type(editor: RichTextEditor, text: string): void {
	editor.editor.view.dispatch(editor.editor.state.tr.insertText(text));
}

/**
 * Selects as the user does.
 */
function select(editor: RichTextEditor, anchor: number, head = anchor): void {
	const state = editor.editor.state;
	editor.editor.view.dispatch(state.tr.setSelection(TextSelection.create(state.doc, anchor, head)));
}

function key(target: Element, key: string, init: KeyboardEventInit = {}): KeyboardEvent {
	const event = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true, ...init });
	target.dispatchEvent(event);
	return event;
}

afterEach(() => {
	Popover.closeAll();
	document.body.innerHTML = '';
	setHost(standaloneHost);
	vi.restoreAllMocks();
});

describe('RichTextEditor', () => {
	it('wraps a text area, hiding it, and keeps the HTML in it', () => {
		const field = textArea();
		const editor = new RichTextEditor(field);
		const element = editor.element;

		expect(element.parentElement?.tagName).toBe('FORM');
		expect(element.className).toBe('antilia-rich-text-editor antilia-resizable ' +
			'antilia-resizable-vertical');
		expect(element.firstElementChild).toBe(field);
		expect(element.children[1]?.getAttribute('role')).toBe('toolbar');
		expect(element.children[2]?.className).toBe('antilia-rich-text-content');
		expect(element.lastElementChild?.className).toBe('antilia-resizable-handle');
		expect(field.hidden).toBe(true);
		expect(field.tabIndex).toBe(-1);
		expect(field.getAttribute('aria-hidden')).toBe('true');
		expect(field.className).toBe('antilia-rich-text-editor-field');
		expect(editor.getValue()).toBe(ARTICLE);
		expect(editor.getText()).toContain('Hello world');
		const text = editor.editor.view.dom;
		expect(text.getAttribute('aria-label')).toBe('Notes');
		expect(text.getAttribute('role')).toBe('textbox');
		expect(text.getAttribute('aria-multiline')).toBe('true');
		expect(text.id).toBe('notes-text');
		expect(element.querySelector('[role="toolbar"]')?.getAttribute('aria-controls'))
			.toBe('notes-text');
		expect(element.getAttribute('data-modified')).toBe('false');
		expect(RichTextEditor.of('notes')).toBe(editor);
		expect(RichTextEditor.of(element)).toBe(editor);
		expect(RichTextEditor.of(null)).toBeNull();

		editor.editor.commands.setTextSelection(4);
		type(editor, 'My ');
		expect(field.value).toBe('<h2>Tit<!---->My le</h2><p>Hello <strong>world</strong></p>'
			.replace('<!---->', ''));
		expect(field.defaultValue).toBe(ARTICLE);
		expect(editor.isModified()).toBe(true);
		expect(element.getAttribute('data-modified')).toBe('true');
	});

	it('takes away the class of a text area waiting for it, which then stays shown', () => {
		const field = textArea(ARTICLE, ' class="mine antilia-rich-text-editor-loading"');
		expect(RICH_TEXT_EDITOR_CLASSES.loading).toBe('antilia-rich-text-editor-loading');

		const editor = new RichTextEditor(field);
		expect(field.className).toBe('mine antilia-rich-text-editor-field');

		editor.destroy();
		expect(field.className).toBe('mine');
		expect(field.hidden).toBe(false);
	});

	it('writes the HTML its own way, and has no changes for that', () => {
		const editor = new RichTextEditor(textArea('<b>bold</b> and <i>italic</i>'));

		expect(editor.getValue()).toBe('<p><strong>bold</strong> and <em>italic</em></p>');
		expect(editor.isModified()).toBe(false);
		expect(editor.element.getAttribute('data-modified')).toBe('false');
	});

	it('is empty as an empty string, showing its placeholder', () => {
		const editor = new RichTextEditor(textArea(''), { placeholder: 'Write…' });

		expect(editor.getValue()).toBe('');
		const paragraph = editor.editor.view.dom.querySelector('p')!;
		expect(paragraph.getAttribute('data-placeholder')).toBe('Write…');
		expect(paragraph.classList.contains('is-editor-empty')).toBe(true);

		editor.configure({ placeholder: 'Another' });
		expect(editor.editor.view.dom.querySelector('p')!.getAttribute('data-placeholder'))
			.toBe('Another');
	});

	it('works in an element of its own, keeping the text in a text area in it, if any', () => {
		document.body.innerHTML = '<div id="editor"></div><div id="posted"><textarea name="t">' +
			'</textarea></div>';
		const plain = new RichTextEditor(document.getElementById('editor')!, { value: '<p>one</p>',
			label: 'Notes' });

		expect(plain.element.id).toBe('editor');
		expect(plain.field).toBeNull();
		expect(plain.getValue()).toBe('<p>one</p>');
		expect(plain.editor.view.dom.getAttribute('aria-label')).toBe('Notes');

		const posted = new RichTextEditor(document.getElementById('posted')!, { value: 'two' });
		expect(posted.field?.name).toBe('t');
		expect(posted.field?.value).toBe('<p>two</p>');
		expect(posted.element.id).toBe('posted');
	});

	it('tells the page of what the user changes, with its callbacks and events', async () => {
		const field = textArea();
		const changes: string[] = [];
		const modified: boolean[] = [];
		const selections: string[] = [];
		const events: string[] = [];
		Object.values(RICH_TEXT_EDITOR_EVENTS).forEach((name) =>
			document.addEventListener(name, () => events.push(name)));
		document.addEventListener('input', (event) => events.push('input:' +
			(event.target as Element).id));
		const editor = new RichTextEditor(field, {
			onChange: (value) => changes.push(value),
			onModifiedChange: (value) => modified.push(value),
			onSelectionChange: (value) => selections.push(value.anchor + '-' + value.head)
		});

		select(editor, 8);
		type(editor, '!');

		expect(changes).toEqual(['<h2>Title</h2><p>!Hello <strong>world</strong></p>']);
		expect(modified).toEqual([true]);
		expect(selections).toEqual(['8-8', '9-9']);
		expect(events).toContain(RICH_TEXT_EDITOR_EVENTS.change);
		expect(events).toContain(RICH_TEXT_EDITOR_EVENTS.modified);
		expect(events).toContain(RICH_TEXT_EDITOR_EVENTS.select);
		expect(events).toContain(RICH_TEXT_EDITOR_EVENTS.text);
		expect(events).toContain('input:notes');

		editor.editor.commands.undo();
		expect(modified).toEqual([true, false]);
		expect(editor.getValue()).toBe(ARTICLE);
	});

	it('replaces its text for the page, without calling back, the selection staying', () => {
		const field = textArea();
		const onChange = vi.fn();
		const onModifiedChange = vi.fn();
		const editor = new RichTextEditor(field, { onChange, onModifiedChange });
		select(editor, 9, 12);

		editor.setValue('<p>Hello there, world</p>');
		expect(editor.getValue()).toBe('<p>Hello there, world</p>');
		expect(field.value).toBe('<p>Hello there, world</p>');
		expect(onChange).not.toHaveBeenCalled();
		expect(onModifiedChange).toHaveBeenCalledWith(true);
		expect(editor.getState().selection).toEqual({ anchor: 9, head: 12 });
		// not for the user to undo
		expect(editor.editor.can().undo()).toBe(false);

		editor.setValue('<p>Saved now</p>', true);
		expect(editor.isModified()).toBe(false);
		expect(field.defaultValue).toBe('<p>Saved now</p>');

		// the same text again changes nothing
		editor.setValue('<p>Saved now</p>');
		expect(editor.isModified()).toBe(false);
	});

	it('takes a text as saved: no longer modified', () => {
		const field = textArea();
		const onModifiedChange = vi.fn();
		const editor = new RichTextEditor(field, { onModifiedChange, saved: '<p>Other</p>' });
		expect(editor.isModified()).toBe(true);
		expect(field.defaultValue).toBe('<p>Other</p>');

		editor.markSaved();
		expect(editor.isModified()).toBe(false);
		expect(field.defaultValue).toBe(ARTICLE);
		expect(onModifiedChange).toHaveBeenCalledWith(false);

		editor.markSaved('<p>An<b>other</b></p>');
		expect(field.defaultValue).toBe('<p>An<strong>other</strong></p>');
		expect(editor.isModified()).toBe(true);
	});

	it('takes up again the text the page puts back into the text area', () => {
		const field = textArea();
		const editor = new RichTextEditor(field);

		field.value = '<p>From the page</p>';
		field.dispatchEvent(new Event('input', { bubbles: true }));
		expect(editor.getValue()).toBe('<p>From the page</p>');
	});

	it('reports its state a moment after the user changed it, and as the page goes', async () => {
		const field = textArea();
		const states: string[] = [];
		const editor = new RichTextEditor(field, { stateDelay: 0, onStateChange: (state, change) =>
			states.push(JSON.stringify(state.selection) + (change.changed ? ' changed' : '') +
				(change.leaving ? ' leaving' : '')) });

		select(editor, 3, 5);
		await tick();
		expect(states).toEqual(['{"anchor":3,"head":5}']);

		type(editor, 'x');
		await tick();
		expect(states[1]).toBe('{"anchor":4,"head":4} changed');

		// nothing new: nothing reported
		window.dispatchEvent(new Event('pagehide'));
		expect(states).toHaveLength(2);

		select(editor, 2);
		window.dispatchEvent(new Event('pagehide'));
		expect(states[2]).toBe('{"anchor":2,"head":2} leaving');
	});

	it('starts in a state, and is put in one', () => {
		const editor = new RichTextEditor(textArea(), { state: { selection: { anchor: 9, head: 14 },
			scrollTop: 40, height: 300 } });

		expect(editor.getState()).toEqual({ selection: { anchor: 9, head: 14 }, scrollTop: 40,
			scrollLeft: 0, width: null, height: 300 });
		expect(editor.element.style.getPropertyValue('--antilia-resizable-height')).toBe('300px');

		editor.setState({ selection: { anchor: 2, head: 2 }, scrollLeft: 5, width: 500 });
		expect(editor.getState()).toEqual({ selection: { anchor: 2, head: 2 }, scrollTop: 40,
			scrollLeft: 5, width: null, height: 300 });

		// a selection beyond the text ends at its end
		editor.setState({ selection: { anchor: 999, head: 999 } });
		const end = editor.editor.state.doc.content.size - 1;
		expect(editor.getState().selection).toEqual({ anchor: end, head: end });
	});

	it('resizes as the options say', () => {
		const onResize = vi.fn();
		const editor = new RichTextEditor(textArea(), { resizable: 'both', onResize, minHeight: 50 });
		const handle = editor.element.querySelector<HTMLElement>('.antilia-resizable-handle')!;
		expect(editor.element.classList.contains('antilia-resizable-both')).toBe(true);
		expect(handle.getAttribute('aria-label')).toBe(
			'Drag, or use the arrow keys, to resize the editor');

		handle.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
		expect(onResize).toHaveBeenCalled();
		expect(editor.getState().height).not.toBeNull();

		editor.configure({ resizable: false });
		expect(editor.element.querySelector('.antilia-resizable-handle')).toBeNull();
	});

	it('goes on as it was when rendered anew in the same place, as Wicket does', () => {
		let removing: ((element: Element) => void) | undefined;
		const host: Host = { ...standaloneHost, onNodeRemoving: (listener) => {
			removing = listener;
			return () => undefined;
		} };
		setHost(host);
		const field = textArea();
		const editor = new RichTextEditor(field, { saved: '<p>Saved</p>' });
		select(editor, 9, 12);
		expect(editor.isModified()).toBe(true);

		// Wicket replaces the text area with one rendered anew
		removing!(field);
		expect(RichTextEditor.of('notes')).toBeNull();
		const again = textArea();
		const next = new RichTextEditor(again);

		expect(next.getState().selection).toEqual({ anchor: 9, head: 12 });
		expect(next.isModified()).toBe(true);
	});

	it('gives way to an editor made anew on its element', () => {
		const field = textArea();
		const first = new RichTextEditor(field);
		select(first, 9, 11);

		const second = new RichTextEditor(field);
		expect(RichTextEditor.of(field)).toBe(second);
		expect(second.element).toBe(first.element);
		expect(second.getState().selection).toEqual({ anchor: 9, head: 11 });
		expect(second.element.querySelectorAll('.antilia-rich-text-content')).toHaveLength(1);
	});

	it('tells of the focus got and lost, and of a change once it lost it', async () => {
		const field = textArea();
		const changes: string[] = [];
		field.addEventListener('change', () => changes.push(field.value));
		const onFocus = vi.fn();
		const onBlur = vi.fn();
		const editor = new RichTextEditor(field, { onFocus, onBlur });

		editor.editor.view.dom.focus();
		editor.editor.view.dom.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
		expect(onFocus).toHaveBeenCalledTimes(1);
		expect(editor.element.classList.contains('antilia-rich-text-editor-focused')).toBe(true);
		type(editor, 'x');

		// to the toolbar: still in the editor
		tool(editor, 'bold').focus();
		await tick();
		expect(onBlur).not.toHaveBeenCalled();

		document.getElementById('other')!.focus();
		await tick();
		expect(onBlur).toHaveBeenCalledWith(editor.getValue());
		expect(changes).toEqual([editor.getValue()]);
		expect(editor.element.classList.contains('antilia-rich-text-editor-focused')).toBe(false);
	});

	it('puts the focus in the text when its label is clicked', async () => {
		const editor = new RichTextEditor(textArea());
		document.querySelector<HTMLLabelElement>('label[for="notes"]')!.click();
		await tick();
		expect(editor.editor.isFocused).toBe(true);
	});

	it('tells the page to save on Ctrl+S', () => {
		const field = textArea();
		const onSave = vi.fn();
		const saved: Event[] = [];
		field.addEventListener(RICH_TEXT_EDITOR_EVENTS.save, (event) => saved.push(event));
		const editor = new RichTextEditor(field, { onSave });

		key(editor.editor.view.dom, 's', { ctrlKey: true });
		expect(onSave).toHaveBeenCalledWith(ARTICLE);
		expect(saved).toHaveLength(1);
	});

	it('leaves a list with Escape, then Tab, which otherwise indents', () => {
		const editor = new RichTextEditor(textArea('<ul><li><p>One</p></li><li><p>Two</p></li></ul>'));
		const text = editor.editor.view.dom;
		select(editor, 10);

		key(text, 'Tab');
		expect(editor.getValue()).toBe('<ul><li><p>One</p><ul><li><p>Two</p></li></ul></li></ul>');

		key(text, 'Escape');
		const event = key(text, 'Tab');
		expect(event.defaultPrevented).toBe(false);
		expect(editor.getValue()).toBe('<ul><li><p>One</p><ul><li><p>Two</p></li></ul></li></ul>');

		// Escape counts once
		key(text, 'Tab', { shiftKey: true });
		expect(editor.getValue()).toBe('<ul><li><p>One</p></li><li><p>Two</p></li></ul>');
	});

	it('puts the saved text back when its tab or window is closed anyway', () => {
		document.body.innerHTML = '<div id="tab"><textarea id="t"></textarea></div>';
		const field = document.getElementById('t') as HTMLTextAreaElement;
		field.defaultValue = '<p>Saved</p>';
		const editor = new RichTextEditor(field);
		type(editor, 'x');
		expect(editor.isModified()).toBe(true);

		document.getElementById('tab')!.dispatchEvent(new Event('antilia-tabs:discard',
			{ bubbles: true }));
		expect(editor.getValue()).toBe('<p>Saved</p>');
		expect(editor.isModified()).toBe(false);
	});

	it('is read only as told, or as its text area is, without its toolbar', () => {
		const editor = new RichTextEditor(textArea(ARTICLE, ' readonly'));
		const toolbar = editor.element.querySelector<HTMLElement>('[role="toolbar"]')!;

		expect(editor.editor.isEditable).toBe(false);
		expect(toolbar.hidden).toBe(true);
		expect(editor.editor.view.dom.getAttribute('aria-readonly')).toBe('true');
		expect(editor.element.classList.contains('antilia-rich-text-editor-readonly')).toBe(true);

		editor.field!.readOnly = false;
		editor.configure({});
		expect(editor.editor.isEditable).toBe(true);
		expect(toolbar.hidden).toBe(false);
		expect(editor.editor.view.dom.hasAttribute('aria-readonly')).toBe(false);

		editor.configure({ readOnly: true });
		expect(editor.editor.isEditable).toBe(false);

		const disabled = new RichTextEditor(textArea(ARTICLE, ' disabled'));
		expect(disabled.element.classList.contains('antilia-rich-text-editor-disabled')).toBe(true);
		expect(disabled.editor.isEditable).toBe(false);
	});

	it('counts the characters and the words, against a limit', () => {
		const editor = new RichTextEditor(textArea('<p>One two three</p>'), { count: true });
		const status = () => editor.element.querySelector('.antilia-rich-text-status');
		expect(status()?.textContent).toBe('13 characters · 3 words');

		editor.configure({ limit: 15 });
		expect(status()?.textContent).toBe('13 of 15 characters · 3 words');
		select(editor, 14);
		type(editor, ' four');
		// the limit stops what goes beyond
		expect(editor.getText()).toBe('One two three');
		type(editor, ' f');
		expect(status()?.textContent).toBe('15 of 15 characters · 4 words');
		expect(status()?.classList.contains('antilia-rich-text-status-full')).toBe(true);

		editor.configure({ count: false, limit: null });
		expect(status()).toBeNull();
	});

	it('takes the texts of the page', () => {
		const editor = new RichTextEditor(textArea(), { count: true, texts: { bold: 'Fett',
			characters: '{0} Zeichen', words: '{0} Wörter', toolbar: 'Formatierung' } });

		expect(tool(editor, 'bold').getAttribute('aria-label')).toBe('Fett');
		expect(tool(editor, 'bold').title).toBe('Fett (Ctrl+B)');
		expect(editor.element.querySelector('[role="toolbar"]')?.getAttribute('aria-label'))
			.toBe('Formatierung');
		expect(editor.element.querySelector('.antilia-rich-text-status')?.textContent)
			.toBe('16 Zeichen · 3 Wörter');
	});

	it('takes the look of the page', () => {
		document.body.innerHTML = '<div data-antilia-look="bootstrap"><textarea id="t"></textarea>' +
			'</div>';
		const editor = new RichTextEditor(document.getElementById('t')!);
		expect(editor.element.classList.contains('form-control')).toBe(true);
		expect(tool(editor, 'bold').className).toBe('antilia-rich-text-tool btn ' +
			'btn-outline-secondary');

		const given = new RichTextEditor(textArea(), { look: BOOTSTRAP_LOOK });
		expect(given.element.classList.contains('form-control')).toBe(true);
	});

	it('takes the editor away, showing the text area again with the text', () => {
		const field = textArea();
		const editor = new RichTextEditor(field);
		type(editor, 'x');
		const wrapper = editor.element;

		editor.destroy();
		expect(field.hidden).toBe(false);
		expect(field.hasAttribute('tabindex')).toBe(false);
		expect(field.hasAttribute('class')).toBe(false);
		expect(field.value).toContain('x');
		expect(wrapper.isConnected).toBe(false);
		expect(field.parentElement?.tagName).toBe('FORM');
		expect(RichTextEditor.of(field)).toBeNull();
		editor.destroy();

		document.body.innerHTML = '<div id="own" class="mine"></div>';
		const own = new RichTextEditor(document.getElementById('own')!);
		own.destroy();
		expect(document.getElementById('own')?.className).toBe('mine');
		expect(document.getElementById('own')?.childElementCount).toBe(0);
	});

	it('works as a Svelte action', () => {
		document.body.innerHTML = '<div id="e"></div>';
		const action = richTextEditor(document.getElementById('e')!, { value: '<p>a</p>' });
		const editor = RichTextEditor.of('e')!;
		action.update({ readOnly: true });
		expect(editor.editor.isEditable).toBe(false);
		action.destroy();
		expect(RichTextEditor.of('e')).toBeNull();
		expect(RichTextEditor.attach('missing')).toBeNull();
	});
});

describe('the formats', () => {
	it('keeps Markdown, without underlined text and alignment', () => {
		const editor = new RichTextEditor(textArea('# Title\n\nSome **bold** text\n\n- one\n- two'),
			{ format: 'markdown' });

		expect(editor.format()).toBe('markdown');
		expect(editor.getValue()).toBe('# Title\n\nSome **bold** text\n\n- one\n- two');
		expect(editor.isModified()).toBe(false);
		expect(editor.features().has('underline')).toBe(false);
		expect(editor.features().has('textAlign')).toBe(false);
		expect(tool(editor, 'underline')).toBeNull();
		expect(tool(editor, 'alignCenter')).toBeNull();
		expect(tool(editor, 'bold')).not.toBeNull();

		select(editor, 1);
		type(editor, 'Big ');
		expect(editor.getValue()).toBe('# Big Title\n\nSome **bold** text\n\n- one\n- two');

		editor.setValue('');
		expect(editor.getValue()).toBe('');
	});

	it('keeps Tiptap\'s JSON', () => {
		const json = JSON.stringify({ type: 'doc', content: [{ type: 'paragraph',
			content: [{ type: 'text', text: 'Hi' }] }] });
		const editor = new RichTextEditor(textArea(json), { format: 'json' });

		expect(JSON.parse(editor.getValue()).content[0].content).toEqual([{ type: 'text',
			text: 'Hi' }]);
		expect(editor.isModified()).toBe(false);
		editor.setValue('');
		expect(editor.getValue()).toBe('');

		const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);
		const broken = new RichTextEditor(textArea('{not json'), { format: 'json' });
		expect(broken.getValue()).toBe('');
		expect(error).toHaveBeenCalled();
	});

	it('takes another format, reading the text anew', () => {
		const editor = new RichTextEditor(textArea('<p>Some <strong>bold</strong></p>'));
		editor.configure({ format: 'markdown' });

		expect(editor.getValue()).toBe('Some **bold**');
		expect(editor.field?.value).toBe('Some **bold**');
		// what it had is saved in the new format
		expect(editor.isModified()).toBe(false);
	});
});

describe('the features', () => {
	it('are all of them by default', () => {
		const editor = new RichTextEditor(textArea());
		expect([...editor.features()]).toEqual([...RICH_TEXT_FEATURES]);
		const tools = Array.from(editor.element.querySelectorAll<HTMLElement>('[data-tool]'))
			.map((each) => each.dataset.tool);
		expect(tools).toEqual(DEFAULT_RICH_TEXT_TOOLBAR.flat());
	});

	it('leave out what the page leaves out, from the text and the toolbar', () => {
		const editor = new RichTextEditor(textArea('<h1>Title</h1><p><u>under</u> <a href="/x">' +
			'link</a></p><table><tr><td>cell</td></tr></table>'),
		{ features: ['bold', 'italic', 'unknown' as 'bold'] });

		expect(editor.getValue()).toBe('<p>Title</p><p>under link</p><p>cell</p>');
		expect(Array.from(editor.element.querySelectorAll<HTMLElement>('[data-tool]'))
			.map((each) => each.dataset.tool)).toEqual(['bold', 'italic', 'clearFormatting', 'undo',
			'redo']);
	});

	it('take other features, keeping the text and the selection', () => {
		const editor = new RichTextEditor(textArea('<p><u>under</u> and <strong>bold</strong></p>'));
		select(editor, 2, 4);
		editor.configure({ features: ['bold'] });

		expect(editor.getValue()).toBe('<p>under and <strong>bold</strong></p>');
		expect(editor.getState().selection).toEqual({ anchor: 2, head: 4 });
		expect(tool(editor, 'underline')).toBeNull();
		// the same again makes nothing anew
		const tiptap = editor.editor;
		editor.configure({ features: ['bold'] });
		expect(editor.editor).toBe(tiptap);
	});

	it('have headings of the levels asked for', () => {
		const editor = new RichTextEditor(textArea('<h5>Five</h5><h2>Two</h2>'),
			{ headingLevels: [5, 2, 2, 9 as 1] });
		expect(editor.getValue()).toBe('<h5>Five</h5><h2>Two</h2>');
		tool(editor, 'heading').click();
		const items = Array.from(editor.element.querySelectorAll('[role="menuitemradio"]'))
			.map((item) => item.textContent);
		expect(items).toEqual(['Paragraph', 'Heading 2', 'Heading 5']);
	});

	it('write no style attribute: tables have no widths, text aligns by classes', () => {
		const editor = new RichTextEditor(textArea('<p style="text-align: center">Centered</p>' +
			'<p align="right">Right</p><p class="antilia-text-justify">Justified</p>' +
			'<p style="text-align: start">Start</p>' +
			'<table style="width: 300px"><tr><th style="text-align: right">A</th>' +
			'<td align="center">B</td></tr></table>'));

		expect(editor.getValue()).toBe('<p class="antilia-text-center">Centered</p>' +
			'<p class="antilia-text-right">Right</p><p class="antilia-text-justify">Justified</p>' +
			'<p>Start</p><table><tbody><tr><th class="antilia-text-right"><p>A</p></th>' +
			'<td class="antilia-text-center"><p>B</p></td></tr></tbody></table>');
		expect(editor.editor.view.dom.querySelector('[style*="text-align"]')).toBeNull();
	});

	it('take only safe addresses for links and images', () => {
		const editor = new RichTextEditor(textArea('<p><a href="javascript:alert(1)">bad</a> ' +
			'<a href="https://example.org">good</a> <a href="/relative">relative</a></p>' +
			'<img src="javascript:alert(1)"><img src="data:image/png;base64,AAAA">' +
			'<img src="https://example.org/a.png" alt="A">'));

		expect(editor.getValue()).toBe('<p>bad <a rel="noopener noreferrer nofollow" ' +
			'href="https://example.org">good</a> <a rel="noopener noreferrer nofollow" ' +
			'href="/relative">relative</a></p><img src="https://example.org/a.png" alt="A">');
	});

	it('have task lists, code blocks in a language, quotes and rules', () => {
		const html = '<ul data-type="taskList"><li data-checked="true" data-type="taskItem"><label>' +
			'<input type="checkbox" checked="checked"><span></span></label><div><p>Done</p></div>' +
			'</li></ul><pre><code class="language-java">int a;</code></pre><blockquote><p>Q</p>' +
			'</blockquote><hr>';
		const editor = new RichTextEditor(textArea(html));
		expect(editor.getValue()).toBe(html);
	});
});

describe('a paste', () => {
	it('moves the styles out of the way, keeping what they say the editor has', () => {
		const editor = new RichTextEditor(textArea('<p>Start</p>'));
		select(editor, 6);
		// Google Docs: a b of a normal weight around it all, and spans of styles
		editor.editor.view.pasteHTML('<meta charset="utf-8"><b style="font-weight:normal;" ' +
			'id="docs-internal-guid-1"><p dir="ltr" style="line-height:1.38;text-align: center;">' +
			'<span style="font-weight:700;">Bold</span> <span style="font-style:italic;">italic' +
			'</span> <span style="text-decoration:underline;">under</span> <span style="text-' +
			'decoration-line: line-through">struck</span> plain</p></b>',
		// jsdom has no ClipboardEvent
		new Event('paste') as ClipboardEvent);

		// one paragraph pasted joins the one of the cursor
		expect(editor.getValue()).toBe('<p>Start<strong>Bold</strong> <em>italic</em> ' +
			'<u>under</u> <s>struck</s> plain</p>');

		// the alignment of a paragraph of its own comes along
		editor.editor.view.pasteHTML('<p>One</p><h2 style="text-align: right">Two</h2>',
			new Event('paste') as ClipboardEvent);
		expect(editor.getValue()).toContain('<h2 class="antilia-text-right">Two</h2>');
	});

	it('of HTML with no style is read as it is', () => {
		expect(quietHtml('<p>plain</p>')).toBe('<p>plain</p>');
		expect(quietHtml('<p title="a style=b">x</p>')).toBe('<p title="a style=b">x</p>');
		expect(quietHtml('<p title="my style=x" style="color: red">x</p>')).toBe(
			'<p title="my style=x" data-antilia-style="color: red">x</p>');
		expect(quietHtml('<h2 style="font-weight: 700">Heading</h2><b style="font-weight: bold">' +
			'b</b>')).toBe('<h2 data-antilia-style="font-weight: 700">Heading</h2>' +
			'<b data-antilia-style="font-weight: bold"><strong>b</strong></b>');
	});
});

describe('isSafeUrl', () => {
	it('takes http, https, mailto and relative addresses only', () => {
		expect(isSafeUrl('https://example.org')).toBe(true);
		expect(isSafeUrl('HTTP://example.org')).toBe(true);
		expect(isSafeUrl('mailto:a@example.org')).toBe(true);
		expect(isSafeUrl('/page')).toBe(true);
		expect(isSafeUrl('page.html#part')).toBe(true);
		expect(isSafeUrl('#top')).toBe(true);
		expect(isSafeUrl('javascript:alert(1)')).toBe(false);
		expect(isSafeUrl(' java\tscript:alert(1)')).toBe(false);
		expect(isSafeUrl('data:text/html,x')).toBe(false);
		expect(isSafeUrl('vbscript:x')).toBe(false);
		expect(isSafeUrl('')).toBe(false);
		expect(isSafeUrl(' ')).toBe(false);
		expect(isSafeUrl(null)).toBe(false);
	});
});

describe('the toolbar', () => {
	it('shows the formats where the cursor is', () => {
		const editor = new RichTextEditor(textArea());
		expect(tool(editor, 'heading').textContent).toBe('Heading 2');
		expect(tool(editor, 'bold').getAttribute('aria-pressed')).toBe('false');

		select(editor, 16);
		expect(tool(editor, 'heading').textContent).toBe('Paragraph');
		expect(tool(editor, 'bold').getAttribute('aria-pressed')).toBe('true');
		expect(tool(editor, 'bold').classList.contains('antilia-rich-text-tool-active')).toBe(true);
		expect(tool(editor, 'alignLeft').getAttribute('aria-pressed')).toBe('true');
		expect(tool(editor, 'undo').getAttribute('aria-disabled')).toBe('true');
		expect(tool(editor, 'link').getAttribute('aria-haspopup')).toBeNull();
		expect(tool(editor, 'table').getAttribute('aria-haspopup')).toBe('menu');
	});

	it('formats the text, keeping the focus there', () => {
		const editor = new RichTextEditor(textArea('<p>Hello world</p>'));
		select(editor, 1, 6);

		const down = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
		tool(editor, 'bold').dispatchEvent(down);
		expect(down.defaultPrevented).toBe(true);
		tool(editor, 'bold').click();
		expect(editor.getValue()).toBe('<p><strong>Hello</strong> world</p>');
		expect(tool(editor, 'undo').getAttribute('aria-disabled')).toBe('false');

		for (const [name, html] of [['italic', '<em>'], ['underline', '<u>'], ['strike', '<s>'],
			['code', '<code>']]) {
			tool(editor, name!).click();
			expect(editor.getValue()).toContain(html);
		}
		tool(editor, 'clearFormatting').click();
		expect(editor.getValue()).toBe('<p>Hello world</p>');

		tool(editor, 'alignCenter').click();
		expect(editor.getValue()).toBe('<p class="antilia-text-center">Hello world</p>');
		expect(tool(editor, 'alignCenter').getAttribute('aria-pressed')).toBe('true');
		tool(editor, 'alignRight').click();
		tool(editor, 'alignJustify').click();
		expect(editor.getValue()).toBe('<p class="antilia-text-justify">Hello world</p>');
		tool(editor, 'alignLeft').click();
		expect(editor.getValue()).toBe('<p>Hello world</p>');

		tool(editor, 'bulletList').click();
		expect(editor.getValue()).toBe('<ul><li><p>Hello world</p></li></ul>');
		tool(editor, 'orderedList').click();
		expect(editor.getValue()).toBe('<ol><li><p>Hello world</p></li></ol>');
		tool(editor, 'taskList').click();
		expect(editor.getValue()).toContain('data-type="taskList"');
		tool(editor, 'taskList').click();
		tool(editor, 'blockquote').click();
		expect(editor.getValue()).toBe('<blockquote><p>Hello world</p></blockquote>');
		tool(editor, 'blockquote').click();
		tool(editor, 'codeBlock').click();
		expect(editor.getValue()).toBe('<pre><code>Hello world</code></pre>');
		tool(editor, 'codeBlock').click();
		tool(editor, 'horizontalRule').click();
		expect(editor.getValue()).toContain('<hr>');

		tool(editor, 'undo').click();
		expect(editor.getValue()).not.toContain('<hr>');
		tool(editor, 'redo').click();
		expect(editor.getValue()).toContain('<hr>');
	});

	it('does nothing for a tool that cannot act', () => {
		const editor = new RichTextEditor(textArea('<p>a</p>'));
		const before = editor.getValue();
		tool(editor, 'redo').click();
		expect(editor.getValue()).toBe(before);
	});

	it('is one stop of Tab, the arrow keys moving along it', () => {
		const editor = new RichTextEditor(textArea());
		const tools = Array.from(editor.element.querySelectorAll<HTMLButtonElement>(
			'.antilia-rich-text-toolbar [data-tool]'));
		expect(tools.filter((each) => each.tabIndex === 0)).toEqual([tools[0]]);

		tools[0]!.focus();
		key(tools[0]!, 'ArrowRight');
		expect(document.activeElement).toBe(tools[1]);
		expect(tools[1]!.tabIndex).toBe(0);
		expect(tools[0]!.tabIndex).toBe(-1);
		key(tools[1]!, 'ArrowLeft');
		expect(document.activeElement).toBe(tools[0]);
		key(tools[0]!, 'ArrowLeft');
		expect(document.activeElement).toBe(tools[tools.length - 1]);
		key(tools[tools.length - 1]!, 'Home');
		expect(document.activeElement).toBe(tools[0]);
		key(tools[0]!, 'End');
		expect(document.activeElement).toBe(tools[tools.length - 1]);
		key(tools[tools.length - 1]!, 'x');
		expect(document.activeElement).toBe(tools[tools.length - 1]);
	});

	it('is the page\'s, in groups, or none', () => {
		const editor = new RichTextEditor(textArea(), { toolbar: [['bold', 'italic'], [], ['link',
			'bold']] });
		const groups = Array.from(editor.element.querySelectorAll('.antilia-rich-text-group'))
			.map((group) => Array.from(group.querySelectorAll<HTMLElement>('[data-tool]'))
				.map((each) => each.dataset.tool));
		expect(groups).toEqual([['bold', 'italic'], ['link']]);
		expect(editor.element.querySelector('.antilia-rich-text-group')?.getAttribute('role'))
			.toBe('group');

		editor.configure({ toolbar: false });
		expect(editor.element.querySelector('.antilia-rich-text-toolbar')).toBeNull();
		editor.configure({ toolbar: [['undo']] });
		expect(editor.element.querySelectorAll('[data-tool]')).toHaveLength(1);
		expect(editor.element.children[1]?.className).toBe('antilia-rich-text-toolbar');

		const none = new RichTextEditor(textArea(), { toolbar: false });
		expect(none.element.querySelector('.antilia-rich-text-toolbar')).toBeNull();
	});

	it('shows the keys of a tool as a Mac does, or as others do', () => {
		expect(shortcutText('Mod-Shift-8', false)).toBe('Ctrl+Shift+8');
		expect(shortcutText('Mod-Alt-C', true)).toBe('⌘⌥C');
		expect(shortcutText('Mod-Shift-Z', true)).toBe('⌘⇧Z');
	});
});

describe('the menu of the text style', () => {
	it('opens on its item in force, and chooses a style', () => {
		const editor = new RichTextEditor(textArea());
		select(editor, 10);
		const button = tool(editor, 'heading');
		button.click();

		const menu = editor.element.querySelector<HTMLElement>('[role="menu"]')!;
		expect(menu.hidden).toBe(false);
		expect(button.getAttribute('aria-expanded')).toBe('true');
		expect(button.getAttribute('aria-controls')).toBe(menu.id);
		const items = Array.from(menu.querySelectorAll<HTMLButtonElement>('[role="menuitemradio"]'));
		expect(items.map((item) => item.textContent)).toEqual(['Paragraph', 'Heading 1',
			'Heading 2', 'Heading 3', 'Heading 4']);
		expect(items.map((item) => item.getAttribute('aria-checked'))).toEqual(['true', 'false',
			'false', 'false', 'false']);
		expect(document.activeElement).toBe(items[0]);
		expect(items[1]!.dataset.level).toBe('1');

		key(items[0]!, 'ArrowDown');
		expect(document.activeElement).toBe(items[1]);
		key(items[1]!, 'ArrowUp');
		key(items[0]!, 'ArrowUp');
		expect(document.activeElement).toBe(items[4]);
		key(items[4]!, 'Home');
		expect(document.activeElement).toBe(items[0]);
		key(items[0]!, 'End');
		expect(document.activeElement).toBe(items[4]);
		key(items[4]!, 'h');
		expect(document.activeElement).toBe(items[1]);
		key(items[1]!, 'q');
		expect(document.activeElement).toBe(items[1]);

		items[3]!.click();
		expect(menu.hidden).toBe(true);
		expect(button.getAttribute('aria-expanded')).toBe('false');
		expect(editor.getValue()).toBe('<h2>Title</h2><h3>Hello <strong>world</strong></h3>');
		expect(button.textContent).toBe('Heading 3');
	});

	it('opens from the keys, and closes with Escape, Tab or a click elsewhere', () => {
		const editor = new RichTextEditor(textArea());
		const button = tool(editor, 'heading');
		const menu = () => editor.element.querySelector<HTMLElement>('[role="menu"]')!;
		button.focus();

		key(button, 'ArrowDown');
		expect(menu().hidden).toBe(false);
		expect(document.activeElement?.textContent).toBe('Paragraph');
		key(document.activeElement!, 'Escape');
		expect(menu().hidden).toBe(true);
		expect(document.activeElement).toBe(button);

		key(button, 'ArrowUp');
		expect(document.activeElement?.textContent).toBe('Heading 4');
		key(document.activeElement!, 'Tab');
		expect(menu().hidden).toBe(true);

		button.click();
		expect(menu().hidden).toBe(false);
		button.click();
		expect(menu().hidden).toBe(true);

		button.click();
		document.getElementById('other')!.dispatchEvent(new Event('pointerdown', { bubbles: true }));
		expect(menu().hidden).toBe(true);
	});

	it('keeps the focus on its item as the editor changes under it', () => {
		const editor = new RichTextEditor(textArea());
		tool(editor, 'heading').click();
		const item = document.activeElement;
		// the editor loses the focus to the menu: a transaction of Tiptap's
		editor.editor.view.dispatch(editor.editor.state.tr.setMeta('blur', true));
		expect(document.activeElement).toBe(item);
	});
});

describe('the menu of the table', () => {
	it('inserts a table, then changes its rows and columns', () => {
		const editor = new RichTextEditor(textArea('<p>Before</p>'));
		select(editor, 7);
		const button = tool(editor, 'table');
		const item = (name: string) => editor.element.querySelector<HTMLButtonElement>(
			`[data-item="${name}"]`)!;

		button.click();
		expect(item('insertTable').getAttribute('aria-disabled')).toBe('false');
		expect(item('addRowAfter').getAttribute('aria-disabled')).toBe('true');
		expect(editor.element.querySelectorAll('[role="menu"] [role="separator"]')).toHaveLength(3);
		item('addRowAfter').click();
		expect(editor.getValue()).toBe('<p>Before</p>');

		item('insertTable').click();
		const html = editor.getValue();
		expect(html.match(/<tr>/g)).toHaveLength(3);
		expect(html.match(/<th/g)).toHaveLength(3);
		expect(html).not.toContain('style');

		const steps: Array<[string, (value: string, before: string) => void]> = [
			['addRowAfter', (value) => expect(value.match(/<tr>/g)).toHaveLength(4)],
			['addRowBefore', (value) => expect(value.match(/<tr>/g)).toHaveLength(5)],
			['addColumnAfter', (value) => expect(value.match(/<th/g)).toHaveLength(4)],
			['addColumnBefore', (value) => expect(value.match(/<th/g)).toHaveLength(5)],
			['deleteColumn', (value) => expect(value.match(/<th/g)).toHaveLength(4)],
			['deleteRow', (value) => expect(value.match(/<tr>/g)).toHaveLength(4)],
			['toggleHeaderRow', (value, before) => expect(value.match(/<th/g)?.length ?? 0)
				.not.toBe(before.match(/<th/g)?.length ?? 0)],
			['deleteTable', (value) => expect(value).not.toContain('<table')]
		];
		for (const [name, check] of steps) {
			button.click();
			expect(item(name).getAttribute('aria-disabled')).toBe('false');
			const before = editor.getValue();
			item(name).click();
			check(editor.getValue(), before);
		}
	});
});

describe('the form of a link', () => {
	function form(): HTMLFormElement {
		return document.querySelector<HTMLFormElement>('.antilia-popover .antilia-rich-text-form')!;
	}

	function submit(): void {
		form().dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
	}

	it('links the text selected, in a new tab or not', () => {
		const editor = new RichTextEditor(textArea('<p>Hello world</p>'));
		select(editor, 1, 6);
		tool(editor, 'link').click();

		const popover = document.querySelector('.antilia-popover')!;
		expect(popover.getAttribute('role')).toBe('dialog');
		expect(popover.textContent).toContain('Link');
		const [url, newTab] = Array.from(form().querySelectorAll('input'));
		expect(document.activeElement).toBe(url);
		expect(url!.type).toBe('url');
		expect(form().querySelector('label')?.textContent).toBe('Address');

		url!.value = 'javascript:alert(1)';
		submit();
		const error = form().querySelector<HTMLElement>('[role="alert"]')!;
		expect(error.hidden).toBe(false);
		expect(url!.getAttribute('aria-invalid')).toBe('true');
		expect(url!.getAttribute('aria-describedby')).toBe(error.id);
		expect(editor.getValue()).toBe('<p>Hello world</p>');

		url!.value = ' https://example.org ';
		newTab!.checked = true;
		submit();
		expect(document.querySelector('.antilia-popover')).toBeNull();
		expect(editor.getValue()).toBe('<p><a target="_blank" rel="noopener noreferrer nofollow" ' +
			'href="https://example.org">Hello</a> world</p>');

		// the link again: its form knows it, and removes it
		select(editor, 3);
		expect(tool(editor, 'link').classList.contains('antilia-rich-text-tool-active')).toBe(true);
		tool(editor, 'link').click();
		const [again, tab] = Array.from(form().querySelectorAll('input'));
		expect(again!.value).toBe('https://example.org');
		expect(tab!.checked).toBe(true);
		Array.from(form().querySelectorAll('button')).find((button) =>
			button.textContent === 'Remove the link')!.click();
		expect(editor.getValue()).toBe('<p>Hello world</p>');
	});

	it('inserts a link at the cursor, and takes it away when the address is left empty', () => {
		const editor = new RichTextEditor(textArea('<p>See</p>'));
		select(editor, 4);
		tool(editor, 'link').click();
		form().querySelector('input')!.value = '/page';
		submit();
		expect(editor.getValue()).toBe('<p>See<a rel="noopener noreferrer nofollow" ' +
			'href="/page">/page</a></p>');

		select(editor, 6);
		tool(editor, 'link').click();
		form().querySelector('input')!.value = '';
		submit();
		expect(editor.getValue()).toBe('<p>See/page</p>');
	});

	it('opens with Ctrl+K, and not without the feature', () => {
		const editor = new RichTextEditor(textArea('<p>Hi</p>'));
		select(editor, 1, 3);
		key(editor.editor.view.dom, 'k', { ctrlKey: true });
		expect(form()).not.toBeNull();
		Popover.closeAll();

		const plain = new RichTextEditor(textArea('<p>Hi</p>'), { features: ['bold'] });
		expect(plain.openLink()).toBe(false);
		const readOnly = new RichTextEditor(textArea('<p>Hi</p>'), { readOnly: true });
		expect(readOnly.openLink()).toBe(false);
	});

	it('counts as in the editor while it is open', async () => {
		const onBlur = vi.fn();
		const editor = new RichTextEditor(textArea('<p>Hi</p>'), { onBlur });
		editor.editor.view.dom.focus();
		editor.editor.view.dom.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
		select(editor, 1, 3);
		tool(editor, 'link').click();
		editor.element.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
		await tick();
		expect(onBlur).not.toHaveBeenCalled();

		Popover.closeAll();
		document.getElementById('other')!.focus();
		await tick();
		expect(onBlur).toHaveBeenCalled();
	});
});

describe('the form of an image', () => {
	it('inserts an image, and changes the one selected', () => {
		const editor = new RichTextEditor(textArea('<p>Text</p>'));
		select(editor, 5);
		tool(editor, 'image').click();
		const form = () => document.querySelector<HTMLFormElement>('.antilia-popover form')!;
		const submit = () => form().dispatchEvent(new Event('submit', { bubbles: true,
			cancelable: true }));
		const [src, alt] = Array.from(form().querySelectorAll('input'));

		src!.value = 'data:image/png;base64,AAAA';
		submit();
		expect(form().querySelector<HTMLElement>('[role="alert"]')!.hidden).toBe(false);
		src!.value = '';
		submit();
		expect(editor.getValue()).toBe('<p>Text</p>');

		src!.value = 'https://example.org/a.png';
		alt!.value = 'A bird';
		submit();
		expect(editor.getValue()).toBe('<p>Text</p><img src="https://example.org/a.png" ' +
			'alt="A bird">');

		// the image selected
		editor.editor.commands.setNodeSelection(6);
		expect(tool(editor, 'image').classList.contains('antilia-rich-text-tool-active')).toBe(true);
		tool(editor, 'image').click();
		const [again, text] = Array.from(form().querySelectorAll('input'));
		expect(again!.value).toBe('https://example.org/a.png');
		expect(text!.value).toBe('A bird');
		again!.value = '/b.png';
		text!.value = '';
		submit();
		expect(editor.getValue()).toBe('<p>Text</p><img src="/b.png">');
	});
});

describe('the bubble menu', () => {
	it('has the tools of the inline formats, in a toolbar of its own', async () => {
		const editor = new RichTextEditor(textArea('<p>Hello world</p>'), { bubbleMenu: true });
		const shown = () => editor.element.querySelector<HTMLElement>('.antilia-rich-text-bubble');
		expect(shown()).toBeNull();
		editor.editor.view.dom.focus();
		select(editor, 1, 6);
		await tick(300);
		const bubble = shown();
		expect(bubble).not.toBeNull();
		expect(bubble!.getAttribute('role')).toBe('toolbar');
		expect(bubble!.getAttribute('aria-label')).toBe('Formatting of the selection');
		expect(Array.from(bubble!.querySelectorAll<HTMLElement>('[data-tool]'))
			.map((each) => each.dataset.tool)).toEqual(['bold', 'italic', 'underline', 'strike',
			'code', 'link']);

		bubble!.querySelector<HTMLButtonElement>('[data-tool="italic"]')!.click();
		expect(editor.getValue()).toBe('<p><em>Hello</em> world</p>');

		editor.configure({ bubbleMenu: false });
		expect(editor.element.querySelector('.antilia-rich-text-bubble')).toBeNull();
	});
});

describe('the extensions', () => {
	it('are Tiptap\'s, or registered by a name with the classes of the kit', () => {
		registerRichTextExtension('Highlight', (kit) => kit.Mark.create({
			name: 'highlight',
			parseHTML: () => [{ tag: 'mark' }],
			renderHTML: ({ HTMLAttributes }) => ['mark', kit.mergeAttributes(HTMLAttributes), 0]
		}));
		expect(richTextExtensions()).toContain('highlight');
		const editor = new RichTextEditor(textArea('<p><mark>marked</mark></p>'),
			{ extensions: ['highlight'] });
		expect(editor.getValue()).toBe('<p><mark>marked</mark></p>');

		const own = RICH_TEXT_EDITOR_KIT.Node.create({ name: 'aside', group: 'block',
			content: 'paragraph+', parseHTML: () => [{ tag: 'aside' }],
			renderHTML: () => ['aside', 0] });
		const other = new RichTextEditor(textArea('<aside><p>Aside</p></aside>'),
			{ extensions: [own] });
		expect(other.getValue()).toBe('<aside><p>Aside</p></aside>');
	});

	it('of a name not known are left out, said in the log', () => {
		const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);
		registerRichTextExtension('failing', () => {
			throw new Error('no');
		});
		const editor = new RichTextEditor(textArea('<p>a</p>'), { extensions: ['missing',
			'failing'] });
		expect(editor.getValue()).toBe('<p>a</p>');
		expect(error).toHaveBeenCalledTimes(2);

		// new extensions make the editor anew
		const tiptap = editor.editor;
		registerRichTextExtension('plain', [RICH_TEXT_EDITOR_KIT.Extension.create({ name: 'plain' })]);
		editor.configure({ extensions: ['plain'] });
		expect(editor.editor).not.toBe(tiptap);
		expect(editor.getValue()).toBe('<p>a</p>');
	});
});

describe('Wicket.RichTextEditor', () => {
	interface WicketRichTextEditor {
		attach(id: string, options?: Record<string, unknown>): RichTextEditor | null;
		of(id: string): RichTextEditor | null;
		registerExtension: typeof registerRichTextExtension;
		extensions(): string[];
		kit: typeof RICH_TEXT_EDITOR_KIT;
	}

	async function wicket(): Promise<WicketRichTextEditor> {
		await import('../src/wicket/rich-text-editor.js');
		return (window as unknown as { Wicket: { RichTextEditor: WicketRichTextEditor } }).Wicket
			.RichTextEditor;
	}

	it('makes an editor of a text area, sending the server its state and its text', async () => {
		const Wicket = await wicket();
		const post = vi.fn();
		(window as unknown as { Wicket: Record<string, unknown> }).Wicket.Ajax = { post,
			baseUrl: 'page?1' };
		textArea();
		const editor = Wicket.attach('notes', { stateUrl: 'state?x=1', inputName: 'notes',
			stateDelay: 0 })!;
		expect(Wicket.of('notes')).toBe(editor);
		expect(Wicket.attach('missing')).toBeNull();
		expect(Wicket.kit).toBe(RICH_TEXT_EDITOR_KIT);

		select(editor, 3);
		await tick();
		expect(post).toHaveBeenCalledWith({ u: 'state?x=1', c: 'notes', ep: [{ name: 'state',
			value: JSON.stringify(editor.getState()) }] });

		type(editor, 'x');
		await tick();
		expect(post.mock.calls[1]![0].ep).toEqual([{ name: 'state',
			value: JSON.stringify(editor.getState()) }, { name: 'notes', value: editor.getValue() }]);

		const beacon = vi.fn(() => true);
		Object.defineProperty(navigator, 'sendBeacon', { value: beacon, configurable: true });
		select(editor, 2);
		window.dispatchEvent(new Event('pagehide'));
		expect(beacon).toHaveBeenCalledTimes(1);
		const [url, body] = beacon.mock.calls[0] as unknown as [string, URLSearchParams];
		expect(url).toBe('state?x=1&wicket-ajax=true');
		expect(body.get('wicket-ajax-baseurl')).toBe('page?1');
		expect(JSON.parse(body.get('state')!).selection).toEqual({ anchor: 2, head: 2 });
	});

	it('plugs in the extensions of the page', async () => {
		const Wicket = await wicket();
		Wicket.registerExtension('callout', (kit) => kit.Node.create({ name: 'callout',
			group: 'block', content: 'paragraph+', parseHTML: () => [{ tag: 'div.callout' }],
			renderHTML: () => ['div', { class: 'callout' }, 0] }));
		expect(Wicket.extensions()).toContain('callout');
		textArea('<div class="callout"><p>Note</p></div>');
		const editor = Wicket.attach('notes', { extensions: ['callout'] })!;
		expect(editor.getValue()).toBe('<div class="callout"><p>Note</p></div>');

		// without a URL of the state, the page's callback alone
		const onStateChange = vi.fn();
		textArea();
		const plain = Wicket.attach('notes', { onStateChange, stateDelay: 0 })!;
		select(plain, 3);
		await tick();
		expect(onStateChange).toHaveBeenCalled();
	});
});
