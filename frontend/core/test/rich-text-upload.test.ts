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

import { Slice } from '@tiptap/pm/model';
import { TextSelection } from '@tiptap/pm/state';
import type { EditorView } from '@tiptap/pm/view';

import { DEFAULT_RICH_TEXT_UPLOAD_TYPES, lezerHighlighter, Popover, registerRichTextHighlighter,
	RichTextEditor, setHost, sizeText, standaloneHost, typesText, uploadImageTo,
	type Host } from '../src/index.js';

const tick = (ms = 20) => new Promise((resolve) => setTimeout(resolve, ms));

beforeAll(() => {
	const rect = { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0,
		toJSON: () => ({}) } as DOMRect;
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => rect;
	document.elementFromPoint = () => null;
});

function textArea(html = '<p>Text</p>', attributes = ''): HTMLTextAreaElement {
	document.body.innerHTML = '<form><textarea id="notes"' + attributes + '></textarea></form>';
	const field = document.getElementById('notes') as HTMLTextAreaElement;
	field.defaultValue = html;
	return field;
}

function tool(editor: RichTextEditor, name: string): HTMLButtonElement {
	return editor.element.querySelector<HTMLButtonElement>(
		`.antilia-rich-text-toolbar [data-tool="${name}"]`)!;
}

function select(editor: RichTextEditor, anchor: number): void {
	const state = editor.editor.state;
	editor.editor.view.dispatch(state.tr.setSelection(TextSelection.create(state.doc, anchor)));
}

function png(name = 'bird.png', size = 4): File {
	return new File([new Uint8Array(size)], name, { type: 'image/png' });
}

/** A promise the test settles. */
function deferred<T>(): { promise: Promise<T>; resolve(value: T): void; reject(error: unknown): void } {
	let resolve!: (value: T) => void;
	let reject!: (error: unknown) => void;
	const promise = new Promise<T>((yes, no) => {
		resolve = yes;
		reject = no;
	});
	return { promise, resolve, reject };
}

/**
 * @returns the data of a drop or a paste, holding files and maybe HTML
 */
function transfer(files: File[], html?: string): DataTransfer {
	return { files, types: html ? ['text/html', 'Files'] : ['Files'],
		getData: (type: string) => type === 'text/html' ? html ?? '' : '' } as unknown as DataTransfer;
}

/**
 * Drops files into the text, as ProseMirror tells its plugins: jsdom lays nothing out, so the
 * drop goes where the test says, or nowhere, which the editor takes as where the cursor is.
 */
function drop(editor: RichTextEditor, files: File[], at?: number): { handled: boolean;
	event: Event } {
	const event = new Event('drop', { cancelable: true });
	Object.assign(event, { dataTransfer: transfer(files), clientX: 0, clientY: 0 });
	const view = editor.editor.view;
	vi.spyOn(view, 'posAtCoords').mockReturnValue(at === undefined ? null :
		{ pos: at, inside: -1 });
	const handled = !!view.someProp('handleDrop', (handle) =>
		handle(view, event as DragEvent, Slice.empty, false));
	return { handled, event };
}

function paste(editor: RichTextEditor, files: File[], html?: string): boolean {
	const event = new Event('paste', { cancelable: true });
	Object.assign(event, { clipboardData: transfer(files, html) });
	const view: EditorView = editor.editor.view;
	return !!view.someProp('handlePaste', (handle) =>
		handle(view, event as ClipboardEvent, Slice.empty));
}

function marks(editor: RichTextEditor): string[] {
	return Array.from(editor.element.querySelectorAll('.antilia-rich-text-uploading')).map(
		(mark) => mark.getAttribute('role') + ': ' + mark.textContent);
}

function message(editor: RichTextEditor): string | null {
	const element = editor.element.querySelector('.antilia-rich-text-message');
	return element ? element.getAttribute('role') + ': ' + element.textContent : null;
}

const form = () => document.querySelector<HTMLFormElement>('.antilia-popover form');

/**
 * Chooses a file in the field of the form of the image, as the user does.
 */
function choose(file: File): void {
	const picker = form()!.querySelector<HTMLInputElement>('input[type="file"]')!;
	Object.defineProperty(picker, 'files', { value: [file], configurable: true });
	picker.dispatchEvent(new Event('change', { bubbles: true }));
}

afterEach(() => {
	Popover.closeAll();
	registerRichTextHighlighter(null);
	document.body.innerHTML = '';
	setHost(standaloneHost);
	vi.restoreAllMocks();
	vi.unstubAllGlobals();
});

describe('the form of an image', () => {
	it('has no upload while the page uploads no images', () => {
		const editor = new RichTextEditor(textArea());
		tool(editor, 'image').click();
		expect(form()!.querySelector('input[type="file"]')).toBeNull();
	});

	it('uploads a file chosen, which the image then shows', async () => {
		const upload = deferred<string>();
		const onUpload = vi.fn(() => upload.promise);
		const editor = new RichTextEditor(textArea(), { onUpload });
		select(editor, 5);
		tool(editor, 'image').click();
		const button = form()!.querySelector<HTMLButtonElement>('.antilia-rich-text-form-upload ' +
			'button')!;
		expect(button.textContent).toBe('Upload an image…');
		expect(button.className).toBe('ant-button ant-button--secondary ant-button--small');
		const picker = form()!.querySelector<HTMLInputElement>('input[type="file"]')!;
		expect(picker.accept).toBe('image/png,image/jpeg,image/gif,image/webp');
		expect(picker.hidden).toBe(true);
		const click = vi.spyOn(picker, 'click').mockImplementation(() => undefined);
		button.click();
		expect(click).toHaveBeenCalled();

		form()!.querySelector<HTMLInputElement>('input[type="text"]')!.value = 'A bird';
		const file = png();
		choose(file);
		expect(onUpload).toHaveBeenCalledWith(file);
		const status = form()!.querySelector<HTMLElement>('[role="status"]')!;
		expect(status.hidden).toBe(false);
		expect(status.textContent).toBe('Uploading bird.png…');
		expect(button.disabled).toBe(true);
		expect(form()!.querySelector<HTMLButtonElement>('[type="submit"]')!.disabled).toBe(true);

		upload.resolve('/images/bird.png');
		await tick();
		expect(form()).toBeNull();
		expect(editor.getValue()).toBe('<p>Text</p><img src="/images/bird.png" alt="A bird">');
	});

	it('refuses a file of another type, or too large, before uploading it', () => {
		const onUpload = vi.fn(() => Promise.resolve('/a.png'));
		const editor = new RichTextEditor(textArea(), { onUpload, uploadMaxSize: 2048,
			uploadTypes: ['image/png', 'image/svg+xml'] });
		tool(editor, 'image').click();
		const error = () => form()!.querySelector<HTMLElement>('[role="alert"]')!;

		choose(new File(['x'], 'notes.txt', { type: 'text/plain' }));
		expect(error().hidden).toBe(false);
		expect(error().textContent).toBe('notes.txt is not an image of a type the editor takes: ' +
			'PNG, SVG.');
		choose(png('large.png', 4096));
		expect(error().textContent).toBe('large.png is larger than 2 KB.');
		expect(onUpload).not.toHaveBeenCalled();
	});

	it('says why the page did not upload a file, or gave an address the editor does not take',
		async () => {
			const log = vi.fn();
			setHost({ ...standaloneHost, log: { ...standaloneHost.log, error: log } } as Host);
			const onUpload = vi.fn<(file: File) => Promise<string>>()
				.mockRejectedValueOnce(new Error('The server is full.'))
				.mockRejectedValueOnce('no reason')
				.mockResolvedValueOnce('javascript:alert(1)');
			const editor = new RichTextEditor(textArea(), { onUpload });
			tool(editor, 'image').click();
			const error = () => form()!.querySelector<HTMLElement>('[role="alert"]')!;

			choose(png());
			await tick();
			expect(error().textContent).toBe('The server is full.');
			expect(form()!.querySelector<HTMLElement>('[role="status"]')!.hidden).toBe(true);
			expect(form()!.querySelector<HTMLButtonElement>('[type="submit"]')!.disabled).toBe(false);
			choose(png());
			await tick();
			expect(error().textContent).toBe('bird.png could not be uploaded.');
			choose(png());
			await tick();
			expect(error().textContent).toBe('bird.png could not be uploaded.');
			expect(log).toHaveBeenCalledWith('The address of an image uploaded is not one the ' +
				'editor takes: javascript:alert(1)');
			expect(editor.getValue()).toBe('<p>Text</p>');
		});

	it('drops an image uploaded once the user closed it', async () => {
		const upload = deferred<string>();
		const editor = new RichTextEditor(textArea(), { onUpload: () => upload.promise });
		tool(editor, 'image').click();
		choose(png());
		Popover.closeAll();
		upload.resolve('/a.png');
		await tick();
		expect(editor.getValue()).toBe('<p>Text</p>');
	});

	it('has no upload once the page uploads no more images', () => {
		const editor = new RichTextEditor(textArea(), { onUpload: () => Promise.resolve('/a.png') });
		editor.configure({ onUpload: null });
		select(editor, 5);
		tool(editor, 'image').click();
		expect(form()!.querySelector('input[type="file"]')).toBeNull();
	});
});

describe('the images dropped or pasted', () => {
	it('upload, a mark standing where each goes until it is there', async () => {
		const first = deferred<string>();
		const second = deferred<string>();
		const onUpload = vi.fn<(file: File) => Promise<string>>()
			.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
		const editor = new RichTextEditor(textArea(), { onUpload });
		select(editor, 5);
		const { handled, event } = drop(editor, [png('one.png'), png('two.png')]);
		expect(handled).toBe(true);
		expect(event.defaultPrevented).toBe(true);
		expect(onUpload).toHaveBeenCalledTimes(2);
		expect(marks(editor)).toEqual(['status: Uploading one.png…', 'status: Uploading two.png…']);
		// the marks are not in the text
		expect(editor.getValue()).toBe('<p>Text</p>');

		second.resolve('/two.png');
		await tick();
		expect(marks(editor)).toEqual(['status: Uploading one.png…']);
		first.resolve('/one.png');
		await tick();
		expect(marks(editor)).toEqual([]);
		expect(editor.getValue()).toBe('<p>Text</p><img src="/one.png"><img src="/two.png">');
	});

	it('go where they were dropped, the text typed meanwhile staying', async () => {
		const upload = deferred<string>();
		const editor = new RichTextEditor(textArea('<p>Hello world</p>'),
			{ onUpload: () => upload.promise });
		select(editor, 1);
		drop(editor, [png()], 6);
		editor.editor.view.dispatch(editor.editor.state.tr.insertText('Oh, '));
		upload.resolve('/bird.png');
		await tick();
		expect(editor.getValue()).toBe('<p>Oh, Hello</p><img src="/bird.png"><p> world</p>');
	});

	it('say why one was not uploaded, above the text, for a while', async () => {
		vi.useFakeTimers();
		try {
			const onUpload = vi.fn(() => Promise.reject(new Error('No room left.')));
			const editor = new RichTextEditor(textArea(), { onUpload, uploadMaxSize: 1024 });
			drop(editor, [new File(['x'], 'notes.txt', { type: 'text/plain' })]);
			expect(message(editor)).toBe('alert: notes.txt is not an image of a type the editor ' +
				'takes: PNG, JPEG, GIF, WebP.');
			expect(editor.element.querySelector('.antilia-rich-text-message')!.nextElementSibling)
				.toBe(editor.content);
			drop(editor, [png('large.png', 2048)]);
			expect(message(editor)).toBe('alert: large.png is larger than 1 KB.');
			expect(onUpload).not.toHaveBeenCalled();

			drop(editor, [png()]);
			await vi.advanceTimersByTimeAsync(10);
			expect(message(editor)).toBe('alert: No room left.');
			expect(marks(editor)).toEqual([]);
			await vi.advanceTimersByTimeAsync(8000);
			expect(message(editor)).toBeNull();
		} finally {
			vi.useRealTimers();
		}
	});

	it('are pasted as a screenshot is, not with HTML, which pastes as it is', async () => {
		const onUpload = vi.fn(() => Promise.resolve('/shot.png'));
		const editor = new RichTextEditor(textArea(), { onUpload });
		select(editor, 5);
		expect(paste(editor, [png('shot.png')], '<img src="https://example.org/a.png">')).toBe(false);
		expect(paste(editor, [])).toBe(false);
		expect(paste(editor, [png('shot.png')])).toBe(true);
		await tick();
		expect(editor.getValue()).toBe('<p>Text</p><img src="/shot.png">');
	});

	it('are left to the browser while the page uploads none, or the text is read only', () => {
		const plain = new RichTextEditor(textArea());
		expect(drop(plain, [png()]).handled).toBe(false);
		expect(paste(plain, [png()])).toBe(false);

		const onUpload = vi.fn(() => Promise.resolve('/a.png'));
		const readOnly = new RichTextEditor(textArea(), { onUpload, readOnly: true });
		expect(drop(readOnly, [png()]).handled).toBe(false);
		const field = new RichTextEditor(textArea('<p>Text</p>', ' readonly'), { onUpload });
		expect(drop(field, [png()]).handled).toBe(false);
		const noImages = new RichTextEditor(textArea(), { onUpload, features: ['bold'] });
		expect(drop(noImages, [png()]).handled).toBe(false);
		expect(onUpload).not.toHaveBeenCalled();
	});

	it('are not inserted once the editor is gone', async () => {
		const upload = deferred<string>();
		const editor = new RichTextEditor(textArea(), { onUpload: () => upload.promise });
		drop(editor, [png()]);
		editor.destroy();
		upload.resolve('/a.png');
		await tick();
		expect(editor.editor.isDestroyed).toBe(true);
	});
});

describe('uploadImageTo', () => {
	it('posts a file, and reads its address from the answer', async () => {
		const fetch = vi.fn(() => Promise.resolve(new Response('{"url": "/images/a.png"}',
			{ status: 200, headers: { 'Content-Type': 'application/json' } })));
		vi.stubGlobal('fetch', fetch);
		const file = png();
		expect(await uploadImageTo('upload?x=1')(file)).toBe('/images/a.png');
		const [url, init] = fetch.mock.calls[0] as unknown as [string, RequestInit];
		expect(url).toBe('upload?x=1');
		expect(init.method).toBe('POST');
		expect(init.credentials).toBe('same-origin');
		expect((init.body as FormData).get('file')).toBeInstanceOf(File);

		await uploadImageTo('upload', 'image')(file);
		expect(((fetch.mock.calls[1] as unknown as [string, RequestInit])[1].body as FormData)
			.get('image')).toBeInstanceOf(File);
	});

	it('rejects with what the server says, or with nothing', async () => {
		const answers = [new Response('{"error": "Too large."}', { status: 200 }),
			new Response('Server error', { status: 500 }), new Response('{}', { status: 200 })];
		vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(answers.shift()!)));
		const upload = uploadImageTo('upload');
		await expect(upload(png())).rejects.toThrow('Too large.');
		await expect(upload(png())).rejects.toThrow(/^$/);
		await expect(upload(png())).rejects.toThrow(/^$/);
	});
});

describe('sizeText and typesText', () => {
	it('say sizes and types as the messages do', () => {
		expect(sizeText(512)).toBe('512 B');
		expect(sizeText(2048)).toBe('2 KB');
		expect(sizeText(1536 * 1024)).toBe('1.5 MB');
		expect(sizeText(2 * 1024 * 1024 * 1024)).toBe('2 GB');
		expect(typesText(DEFAULT_RICH_TEXT_UPLOAD_TYPES)).toBe('PNG, JPEG, GIF, WebP');
		expect(typesText(['image/bmp', 'IMAGE/PNG'])).toBe('BMP, PNG');
	});
});

describe('Wicket.RichTextEditor', () => {
	interface WicketRichTextEditor {
		attach(id: string, options?: Record<string, unknown>): RichTextEditor | null;
		registerHighlighter: typeof registerRichTextHighlighter;
	}

	it('uploads to the URL of the editor, and highlights with the grammars of its script',
		async () => {
			await import('../src/wicket/rich-text-editor.js');
			const global = window as unknown as { Wicket: Record<string, unknown> };
			const Wicket = global.Wicket.RichTextEditor as WicketRichTextEditor;
			const fetch = vi.fn(() => Promise.resolve(new Response('{"url": "/images/a.png"}')));
			vi.stubGlobal('fetch', fetch);
			textArea('<pre><code class="language-java">class A {}</code></pre>');
			const editor = Wicket.attach('notes', { uploadUrl: 'upload?2', highlight: true })!;
			select(editor, 1);
			drop(editor, [png()]);
			await tick();
			expect((fetch.mock.calls[0] as unknown as [string])[0]).toBe('upload?2');
			expect(editor.getValue()).toBe('<img src="/images/a.png"><pre><code ' +
				'class="language-java">class A {}</code></pre>');

			expect(editor.editor.view.dom.querySelector('pre span.tok-keyword')).toBeNull();
			await import('../src/wicket/rich-text-highlight.js');
			expect(global.Wicket.RichTextHighlighter).toBe(lezerHighlighter);
			await lezerHighlighter.load('java');
			await tick();
			expect(editor.editor.view.dom.querySelector('pre span.tok-keyword')?.textContent)
				.toBe('class');
		});
});
