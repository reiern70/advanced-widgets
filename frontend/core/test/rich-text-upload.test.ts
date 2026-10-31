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

import { Slice } from '@tiptap/pm/model';
import { NodeSelection, TextSelection } from '@tiptap/pm/state';
import type { EditorView } from '@tiptap/pm/view';

import { DEFAULT_RICH_TEXT_UPLOAD_TYPES, deleteImagesAt, lezerHighlighter, Popover,
	readImageDimensions, registerRichTextHighlighter, RichTextEditor, setHost, sizeText,
	standaloneHost, typesText, uploadImageTo, type Host } from '../src/index.js';

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

function selectImage(editor: RichTextEditor, position: number): void {
	const state = editor.editor.state;
	editor.editor.view.dispatch(state.tr.setSelection(NodeSelection.create(state.doc, position)));
}

function png(name = 'bird.png', size = 4): File {
	return new File([new Uint8Array(size)], name, { type: 'image/png' });
}

/**
 * @returns a PNG whose header says its width and height, without a picture
 */
function pngOf(width: number, height: number, name: string): File {
	const bytes = new Uint8Array(33);
	bytes.set([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52]);
	const view = new DataView(bytes.buffer);
	view.setUint32(16, width);
	view.setUint32(20, height);
	return new File([bytes], name, { type: 'image/png' });
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
 * Chooses files in the field of the form of the image, as the user does.
 */
function choose(...files: File[]): void {
	const picker = form()!.querySelector<HTMLInputElement>('input[type="file"]')!;
	Object.defineProperty(picker, 'files', { value: files, configurable: true });
	picker.dispatchEvent(new Event('change', { bubbles: true }));
}

afterEach(() => {
	Popover.closeAll();
	document.querySelectorAll<HTMLElement>('.modal-dialog-close').forEach((close) => close.click());
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

	it('has a zone to drop images on or choose them, which go into the text one after the other',
		async () => {
			const first = deferred<string>();
			const second = deferred<string>();
			const onUpload = vi.fn<(file: File) => Promise<string>>()
				.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
			const editor = new RichTextEditor(textArea(), { onUpload });
			select(editor, 5);
			tool(editor, 'image').click();
			const zone = form()!.querySelector<HTMLElement>('.antilia-rich-text-dropzone')!;
			expect(zone.querySelector('.antilia-rich-text-dropzone-text')!.textContent)
				.toBe('Drop images here');
			const button = zone.querySelector<HTMLButtonElement>('button')!;
			expect(button.textContent).toBe('Choose images…');
			expect(button.className).toBe('ant-button ant-button--secondary ant-button--small');
			const picker = zone.querySelector<HTMLInputElement>('input[type="file"]')!;
			expect(picker.accept).toBe('image/png,image/jpeg,image/gif,image/webp,image/avif');
			expect(picker.multiple).toBe(true);
			expect(picker.hidden).toBe(true);
			const click = vi.spyOn(picker, 'click').mockImplementation(() => undefined);
			button.click();
			expect(click).toHaveBeenCalled();

			choose(png('one.png'), png('two.png'));
			// the form closes, the marks stand where the cursor is
			expect(form()).toBeNull();
			expect(marks(editor)).toEqual(['status: Uploading one.png…',
				'status: Uploading two.png…']);
			await tick();
			expect(onUpload).toHaveBeenCalledTimes(2);
			second.resolve('/two.png');
			first.resolve('/one.png');
			await tick();
			expect(marks(editor)).toEqual([]);
			expect(editor.getValue()).toBe('<p>Text</p><img src="/one.png"><img src="/two.png">');
		});

	it('takes the files dropped on its zone', async () => {
		const onUpload = vi.fn(() => Promise.resolve('/dropped.png'));
		const editor = new RichTextEditor(textArea(), { onUpload });
		select(editor, 5);
		tool(editor, 'image').click();
		const zone = form()!.querySelector<HTMLElement>('.antilia-rich-text-dropzone')!;
		const over = new Event('dragover', { cancelable: true });
		Object.assign(over, { dataTransfer: transfer([png()]) });
		zone.dispatchEvent(over);
		expect(over.defaultPrevented).toBe(true);
		expect(zone.classList.contains('antilia-rich-text-dropzone-over')).toBe(true);
		const dropped = new Event('drop', { cancelable: true, bubbles: true });
		Object.assign(dropped, { dataTransfer: transfer([png('dropped.png')]) });
		zone.dispatchEvent(dropped);
		expect(dropped.defaultPrevented).toBe(true);
		expect(zone.classList.contains('antilia-rich-text-dropzone-over')).toBe(false);
		await tick();
		expect(editor.getValue()).toBe('<p>Text</p><img src="/dropped.png">');
	});

	it('uploads one file for the image selected, which takes its address', async () => {
		const upload = deferred<string>();
		const onUpload = vi.fn(() => upload.promise);
		const editor = new RichTextEditor(textArea('<p>Text</p><img src="/old.png" alt="Old">'),
			{ onUpload });
		selectImage(editor, 6);
		tool(editor, 'image').click();
		expect(form()!.querySelector<HTMLInputElement>('input[type="url"]')!.value)
			.toBe('/old.png');
		form()!.querySelector<HTMLInputElement>('input[type="text"]')!.value = 'A bird';
		choose(png());
		await tick();
		expect(onUpload).toHaveBeenCalledTimes(1);
		const status = form()!.querySelector<HTMLElement>('[role="status"]')!;
		expect(status.hidden).toBe(false);
		expect(status.textContent).toBe('Uploading bird.png…');
		expect(form()!.querySelector<HTMLButtonElement>('[type="submit"]')!.disabled).toBe(true);

		upload.resolve('/images/bird.png');
		await tick();
		expect(form()).toBeNull();
		expect(editor.getValue()).toBe('<p>Text</p><img src="/images/bird.png" alt="A bird">');
	});

	it('refuses a file of another type, or too large, before uploading it', async () => {
		const onUpload = vi.fn(() => Promise.resolve('/a.png'));
		const editor = new RichTextEditor(textArea('<p>Text</p><img src="/old.png">'),
			{ onUpload, uploadMaxSize: 2048, uploadTypes: ['image/png', 'image/svg+xml'] });
		selectImage(editor, 6);
		tool(editor, 'image').click();
		const error = () => form()!.querySelector<HTMLElement>('[role="alert"]')!;

		choose(new File(['x'], 'notes.txt', { type: 'text/plain' }));
		await tick();
		expect(error().hidden).toBe(false);
		expect(error().textContent).toBe('notes.txt is not an image of a type the editor takes: ' +
			'PNG, SVG.');
		choose(png('large.png', 4096));
		await tick();
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
			const editor = new RichTextEditor(textArea('<p>Text</p><img src="/old.png">'),
				{ onUpload });
			selectImage(editor, 6);
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
			expect(editor.getValue()).toBe('<p>Text</p><img src="/old.png">');
		});

	it('drops an image uploaded once the user closed it, and tells the page', async () => {
		const upload = deferred<string>();
		const onImagesRemoved = vi.fn();
		const editor = new RichTextEditor(textArea('<p>Text</p><img src="/old.png">'),
			{ onUpload: () => upload.promise, onImagesRemoved });
		selectImage(editor, 6);
		tool(editor, 'image').click();
		choose(png());
		await tick();
		Popover.closeAll();
		upload.resolve('/a.png');
		await tick();
		expect(editor.getValue()).toBe('<p>Text</p><img src="/old.png">');
		expect(onImagesRemoved).toHaveBeenCalledWith(['/a.png']);
	});

	it('has no upload once the page uploads no more images', () => {
		const editor = new RichTextEditor(textArea(), { onUpload: () => Promise.resolve('/a.png') });
		editor.configure({ onUpload: null });
		select(editor, 5);
		tool(editor, 'image').click();
		expect(form()!.querySelector('input[type="file"]')).toBeNull();
	});
});

describe('the dialog of the uploads', () => {
	const dialog = () => document.querySelector<HTMLElement>('.antilia-rich-text-upload-dialog');
	const rows = () => Array.from(dialog()!.querySelectorAll('.antilia-rich-text-upload-file'))
		.map((row) => row.className.replace('antilia-rich-text-upload-file antilia-rich-text-' +
			'upload-', '') + ': ' + row.querySelector('.antilia-rich-text-upload-name')!.textContent +
			' ' + row.querySelector('.antilia-rich-text-upload-status')!.textContent);
	const insert = () => Array.from(dialog()!.querySelectorAll<HTMLButtonElement>(
		'.modal-dialog-footer button'))[0]!;

	function chooseInDialog(...files: File[]): void {
		const picker = dialog()!.querySelector<HTMLInputElement>('input[type="file"]')!;
		Object.defineProperty(picker, 'files', { value: files, configurable: true });
		picker.dispatchEvent(new Event('change', { bubbles: true }));
	}

	it('lists the files as they upload, and inserts those uploaded once the user says so',
		async () => {
			const first = deferred<string>();
			const second = deferred<string>();
			const onImagesRemoved = vi.fn();
			const onUpload = vi.fn<(file: File) => Promise<string>>()
				.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
			const editor = new RichTextEditor(textArea(), { onUpload, uploadDialog: true,
				uploadMaxSize: 1024, onImagesRemoved });
			select(editor, 5);
			tool(editor, 'image').click();
			expect(form()!.querySelector('.antilia-rich-text-dropzone')).toBeNull();
			const open = form()!.querySelector<HTMLButtonElement>('.antilia-rich-text-form-upload ' +
				'button')!;
			expect(open.textContent).toBe('Upload images…');
			open.click();
			expect(form()).toBeNull();
			expect(dialog()!.querySelector('.modal-dialog-title')!.textContent)
				.toBe('Upload images');
			expect(dialog()!.querySelector('.modal-dialog')!.getAttribute('role')).toBe('dialog');
			expect(insert().textContent).toBe('Insert');
			expect(insert().disabled).toBe(true);

			chooseInDialog(png('one.png'), png('two.png'), png('large.png', 4096));
			await tick();
			expect(rows()).toEqual(['refused:  large.png is larger than 1 KB.',
				'uploading: one.png Uploading one.png…', 'uploading: two.png Uploading two.png…']);
			first.resolve('/one.png');
			await tick();
			expect(rows()[1]).toBe('done: one.png Uploaded');
			// waiting for the second
			expect(insert().disabled).toBe(true);
			const remove = dialog()!.querySelectorAll<HTMLButtonElement>(
				'.antilia-rich-text-upload-remove')[2]!;
			expect(remove.getAttribute('aria-label')).toBe('Remove two.png');
			remove.click();
			expect(insert().disabled).toBe(false);
			second.resolve('/two.png');
			await tick();
			// uploaded once taken out of the list: the text never gets it
			expect(onImagesRemoved).toHaveBeenCalledWith(['/two.png']);

			insert().click();
			expect(dialog()).toBeNull();
			expect(editor.getValue()).toBe('<p>Text</p><img src="/one.png">');
		});

	it('tells the page of the images uploaded once the user canceled', async () => {
		const onImagesRemoved = vi.fn();
		const editor = new RichTextEditor(textArea(), { onUpload: () => Promise.resolve('/a.png'),
			uploadDialog: true, onImagesRemoved });
		tool(editor, 'image').click();
		form()!.querySelector<HTMLButtonElement>('.antilia-rich-text-form-upload button')!.click();
		chooseInDialog(png());
		await tick();
		const cancel = Array.from(dialog()!.querySelectorAll<HTMLButtonElement>(
			'.modal-dialog-footer button'))[1]!;
		expect(cancel.textContent).toBe('Cancel');
		cancel.click();
		expect(dialog()).toBeNull();
		expect(onImagesRemoved).toHaveBeenCalledWith(['/a.png']);
		expect(editor.getValue()).toBe('<p>Text</p>');
	});
});

describe('the images dropped or pasted', () => {
	it('upload, a mark standing where each goes until it is there, in their order', async () => {
		const first = deferred<string>();
		const second = deferred<string>();
		const onUpload = vi.fn<(file: File) => Promise<string>>()
			.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
		const editor = new RichTextEditor(textArea(), { onUpload });
		select(editor, 5);
		const { handled, event } = drop(editor, [png('one.png'), png('two.png')]);
		expect(handled).toBe(true);
		expect(event.defaultPrevented).toBe(true);
		expect(marks(editor)).toEqual(['status: Uploading one.png…', 'status: Uploading two.png…']);
		await tick();
		expect(onUpload).toHaveBeenCalledTimes(2);
		// the marks are not in the text
		expect(editor.getValue()).toBe('<p>Text</p>');

		second.resolve('/two.png');
		await tick();
		// the second waits for the first, to keep their order
		expect(marks(editor)).toEqual(['status: Uploading one.png…', 'status: Uploading two.png…']);
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
			await vi.advanceTimersByTimeAsync(0);
			expect(message(editor)).toBe('alert: notes.txt is not an image of a type the editor ' +
				'takes: PNG, JPEG, GIF, WebP, AVIF.');
			expect(editor.element.querySelector('.antilia-rich-text-message')!.nextElementSibling)
				.toBe(editor.content);
			expect(marks(editor)).toEqual([]);
			drop(editor, [png('large.png', 2048)]);
			await vi.advanceTimersByTimeAsync(0);
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

	it('take as many at a time as the page says, the first ones', async () => {
		const onUpload = vi.fn((file: File) => Promise.resolve('/' + file.name));
		const editor = new RichTextEditor(textArea(), { onUpload, uploadMaxCount: 2 });
		select(editor, 5);
		drop(editor, [png('one.png'), png('two.png'), png('three.png')]);
		await tick();
		expect(message(editor)).toBe('alert: 2 images at most at a time: 1 left out.');
		expect(onUpload).toHaveBeenCalledTimes(2);
		expect(editor.getValue()).toBe('<p>Text</p><img src="/one.png"><img src="/two.png">');
	});

	it('refuse a picture wider or higher than the page takes, by the header of its file',
		async () => {
			const onUpload = vi.fn((file: File) => Promise.resolve('/' + file.name));
			const editor = new RichTextEditor(textArea(), { onUpload, uploadMaxWidth: 100,
				uploadMaxHeight: 100 });
			select(editor, 5);
			drop(editor, [pngOf(200, 50, 'wide.png'), pngOf(80, 100, 'small.png')]);
			await tick();
			expect(message(editor)).toBe('alert: wide.png is 200 × 50 pixels, larger than ' +
				'100 × 100.');
			expect(onUpload).toHaveBeenCalledTimes(1);
			expect(editor.getValue()).toBe('<p>Text</p><img src="/small.png">');
		});

	it('offer to scale down the pictures too large, those the user ticks', async () => {
		const close = vi.fn();
		const drawImage = vi.fn();
		vi.stubGlobal('createImageBitmap', vi.fn((file: File) => Promise.resolve(
			{ width: file.name === 'wide.png' ? 200 : 400, height: 50, close })));
		vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(
			{ drawImage } as unknown as CanvasRenderingContext2D);
		vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation(function (this:
			HTMLCanvasElement, callback: BlobCallback, type?: string) {
			callback(new Blob([new Uint8Array(10)], { type: type ?? 'image/png' }));
		});
		const onUpload = vi.fn((file: File) => Promise.resolve('/' + file.name));
		const editor = new RichTextEditor(textArea(), { onUpload, uploadMaxWidth: 100,
			uploadMaxHeight: 100, uploadScaling: true });
		select(editor, 5);
		drop(editor, [pngOf(200, 50, 'wide.png'), pngOf(400, 50, 'wider.png')]);
		await tick();
		const asked = document.querySelector<HTMLElement>('.antilia-rich-text-scale-dialog')!;
		expect(asked.querySelector('.modal-dialog-title')!.textContent).toBe('Images too large');
		expect(asked.querySelector('.modal-dialog-body p')!.textContent).toBe('These images are ' +
			'larger than 100 × 100 pixels. Tick those to scale down; the others are left out.');
		const labels = Array.from(asked.querySelectorAll('label')).map((label) =>
			label.textContent);
		expect(labels).toEqual([' Scale wide.png down to 100 × 25 (200 × 50)',
			' Scale wider.png down to 100 × 12 (400 × 50)']);
		asked.querySelector<HTMLInputElement>('input[type="checkbox"]')!.click();
		asked.querySelector<HTMLButtonElement>('.modal-dialog-footer button')!.click();
		await tick();
		expect(document.querySelector('.antilia-rich-text-scale-dialog')).toBeNull();
		expect(drawImage).toHaveBeenCalledWith(expect.anything(), 0, 0, 100, 25);
		expect(close).toHaveBeenCalled();
		expect(onUpload).toHaveBeenCalledTimes(1);
		const sent = onUpload.mock.calls[0]![0];
		expect([sent.name, sent.type, sent.size]).toEqual(['wide.png', 'image/png', 10]);
		expect(message(editor)).toBeNull();
		expect(editor.getValue()).toBe('<p>Text</p><img src="/wide.png">');
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
		await tick();
		editor.destroy();
		upload.resolve('/a.png');
		await tick();
		expect(editor.editor.isDestroyed).toBe(true);
	});
});

describe('the images taken out of the text', () => {
	const IMAGES = '<p>Text</p><img src="/a.png" alt="A bird"><img src="/b.png"><img src="/a.png">';

	function deleteNode(editor: RichTextEditor, position: number): void {
		const state = editor.editor.state;
		editor.editor.view.dispatch(state.tr.delete(position, position + 1));
	}

	it('are told of a moment after, unless the text holds them again by then', async () => {
		vi.useFakeTimers();
		try {
			const onImagesRemoved = vi.fn();
			const field = textArea(IMAGES);
			const heard = vi.fn();
			field.addEventListener('antilia-rich-text-editor:images-removed', (event) =>
				heard((event as CustomEvent).detail.urls));
			const editor = new RichTextEditor(field, { onImagesRemoved });
			// one of the two of /a.png: the text still holds it
			deleteNode(editor, 6);
			deleteNode(editor, 6);
			await vi.advanceTimersByTimeAsync(299);
			expect(onImagesRemoved).not.toHaveBeenCalled();
			await vi.advanceTimersByTimeAsync(1);
			expect(onImagesRemoved).toHaveBeenCalledExactlyOnceWith(['/b.png']);
			expect(heard).toHaveBeenCalledWith(['/b.png']);

			// taken out, then back by Ctrl+Z, before the moment is over: the three of them
			deleteNode(editor, 6);
			editor.editor.commands.undo();
			expect(editor.getValue()).toBe(IMAGES);
			await vi.advanceTimersByTimeAsync(400);
			expect(onImagesRemoved).toHaveBeenCalledTimes(1);

			// by the page's code
			editor.editor.commands.clearContent();
			await vi.advanceTimersByTimeAsync(400);
			expect(onImagesRemoved).toHaveBeenLastCalledWith(['/a.png', '/b.png']);
		} finally {
			vi.useRealTimers();
		}
	});

	it('are told of at once as the user saves', async () => {
		vi.useFakeTimers();
		try {
			const onImagesRemoved = vi.fn();
			const onSave = vi.fn(() => expect(onImagesRemoved).toHaveBeenCalledWith(['/b.png']));
			const editor = new RichTextEditor(textArea(IMAGES), { onImagesRemoved, onSave });
			deleteNode(editor, 7);
			editor.save();
			expect(onSave).toHaveBeenCalled();
			await vi.advanceTimersByTimeAsync(400);
			expect(onImagesRemoved).toHaveBeenCalledTimes(1);
		} finally {
			vi.useRealTimers();
		}
	});

	it('are not told of when the page gives another text, and are once the editor goes',
		async () => {
			vi.useFakeTimers();
			try {
				const onImagesRemoved = vi.fn();
				const editor = new RichTextEditor(textArea(IMAGES), { onImagesRemoved });
				editor.setValue('<p>Another text</p><img src="/c.png">');
				await vi.advanceTimersByTimeAsync(400);
				expect(onImagesRemoved).not.toHaveBeenCalled();
				deleteNode(editor, 14);
				editor.destroy();
				expect(onImagesRemoved).toHaveBeenCalledWith(['/c.png']);
			} finally {
				vi.useRealTimers();
			}
		});

	it('are listed by the menu of the images, which selects one and deletes one', async () => {
		vi.useFakeTimers();
		try {
			const onImagesRemoved = vi.fn();
			const editor = new RichTextEditor(textArea(IMAGES), { onImagesRemoved });
			const images = tool(editor, 'images');
			expect(images.getAttribute('aria-label')).toBe('Images in the text');
			expect(images.getAttribute('aria-haspopup')).toBe('menu');
			expect(images.getAttribute('aria-disabled')).toBe('false');
			images.click();
			const menu = editor.element.querySelector<HTMLElement>('.antilia-rich-text-menu:not([hidden])')!;
			const items = Array.from(menu.querySelectorAll('.antilia-rich-text-images-item'));
			expect(items.map((item) => item.querySelector('.antilia-rich-text-images-name')!
				.textContent)).toEqual(['A bird', 'b.png']);
			expect(items.map((item) => item.querySelector('img')!.getAttribute('src')))
				.toEqual(['/a.png', '/b.png']);
			const trash = items[0]!.querySelector<HTMLButtonElement>(
				'.antilia-rich-text-images-delete')!;
			expect(trash.getAttribute('aria-label')).toBe('Delete A bird');
			expect(trash.getAttribute('role')).toBe('menuitem');

			items[1]!.querySelector<HTMLButtonElement>('.antilia-rich-text-images-select')!.click();
			expect(menu.hidden).toBe(true);
			const selection = editor.editor.state.selection as NodeSelection;
			expect(selection.node.attrs.src).toBe('/b.png');

			images.click();
			editor.element.querySelector<HTMLButtonElement>(
				'.antilia-rich-text-menu:not([hidden]) .antilia-rich-text-images-delete')!.click();
			// every place the image is in
			expect(editor.getValue()).toBe('<p>Text</p><img src="/b.png">');
			await vi.advanceTimersByTimeAsync(300);
			expect(onImagesRemoved).toHaveBeenCalledExactlyOnceWith(['/a.png']);

			editor.setValue('<p>No images</p>');
			expect(images.getAttribute('aria-disabled')).toBe('true');
		} finally {
			vi.useRealTimers();
		}
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

describe('deleteImagesAt', () => {
	it('posts the addresses as a JSON array, and reads those deleted from the answer', async () => {
		const fetch = vi.fn(() => Promise.resolve(new Response('{"deleted": ["/a.png", 3]}')));
		vi.stubGlobal('fetch', fetch);
		expect(await deleteImagesAt('upload?4')(['/a.png', '/b.png'])).toEqual(['/a.png']);
		const [url, init] = fetch.mock.calls[0] as unknown as [string, RequestInit];
		expect(url).toBe('upload?4');
		expect(init.method).toBe('POST');
		expect(init.credentials).toBe('same-origin');
		expect((init.body as URLSearchParams).get('removed')).toBe('["/a.png","/b.png"]');

		fetch.mockReturnValueOnce(Promise.resolve(new Response('Server error', { status: 500 })));
		expect(await deleteImagesAt('upload', 'gone')(['/a.png'])).toEqual([]);
		expect(((fetch.mock.calls[1] as unknown as [string, RequestInit])[1].body as URLSearchParams)
			.get('gone')).toBe('["/a.png"]');
	});
});

describe('readImageDimensions', () => {
	const bytes = (...parts: (string | number[])[]) => new Blob(parts.map((part) =>
		typeof part === 'string' ? new TextEncoder().encode(part) : new Uint8Array(part)));
	const be32 = (value: number) => [value >>> 24, (value >> 16) & 0xff, (value >> 8) & 0xff,
		value & 0xff];
	const le16 = (value: number) => [value & 0xff, value >> 8];
	const le24 = (value: number) => [value & 0xff, (value >> 8) & 0xff, value >> 16];

	it('reads the header of a PNG, a GIF and a WebP', async () => {
		expect(await readImageDimensions(pngOf(640, 480, 'a.png'), 'image/png'))
			.toEqual({ width: 640, height: 480 });
		// a frame beyond the screen widens it
		const gif = bytes('GIF89a', [...le16(10), ...le16(10), 0, 0, 0, 0x2c, ...le16(5),
			...le16(0), ...le16(20), ...le16(8), 0, 2, 1, 0, 0, 0x3b]);
		expect(await readImageDimensions(gif, 'image/gif')).toEqual({ width: 25, height: 10 });
		const extended = bytes('RIFF', [0, 0, 0, 0], 'WEBPVP8X', [10, 0, 0, 0, 0, 0, 0, 0],
			[...le24(1919), ...le24(1079)]);
		expect(await readImageDimensions(extended, 'image/webp'))
			.toEqual({ width: 1920, height: 1080 });
		const bits = (300 - 1) + (200 - 1) * 0x4000;
		const lossless = bytes('RIFF', [0, 0, 0, 0], 'WEBPVP8L', [0, 0, 0, 0, 0x2f],
			[bits & 0xff, (bits >> 8) & 0xff, (bits >> 16) & 0xff, bits >>> 24], [0, 0, 0, 0, 0]);
		expect(await readImageDimensions(lossless, 'image/webp'))
			.toEqual({ width: 300, height: 200 });
		const lossy = bytes('RIFF', [0, 0, 0, 0], 'WEBPVP8 ', [0, 0, 0, 0, 0, 0, 0, 0x9d, 0x01,
			0x2a], [...le16(64), ...le16(32)]);
		expect(await readImageDimensions(lossy, 'image/webp')).toEqual({ width: 64, height: 32 });
	});

	it('reads a JPEG turned as its EXIF says, and an AVIF', async () => {
		const exif = bytes([0xff, 0xd8, 0xff, 0xe1, 0, 34], 'Exif', [0, 0], 'MM', [0, 42],
			be32(8), [0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, 6, 0, 0], be32(0),
			[0xff, 0xc0, 0, 17, 8, 0x01, 0xe0, 0x02, 0x80], new Array(10)
				.fill(0));
		// 640 wide, 480 high, the camera turned: 480 wide, 640 high
		expect(await readImageDimensions(exif, 'image/jpeg')).toEqual({ width: 480, height: 640 });
		const plain = bytes([0xff, 0xd8, 0xff, 0xdb, 0, 4, 0, 0, 0xff, 0xc2, 0, 17, 8, 0, 10, 0, 20],
			new Array(12).fill(0));
		expect(await readImageDimensions(plain, 'image/jpeg')).toEqual({ width: 20, height: 10 });
		const avif = bytes(be32(20), 'ftypavif', be32(0), 'mif1', be32(48), 'meta', be32(0),
			be32(36), 'iprp', be32(28), 'ipco', be32(20), 'ispe', be32(0), be32(4000), be32(3000));
		expect(await readImageDimensions(avif, 'image/avif'))
			.toEqual({ width: 4000, height: 3000 });
	});

	it('answers nothing for a header that tells nothing, or a type it does not know', async () => {
		expect(await readImageDimensions(png(), 'image/png')).toBeNull();
		expect(await readImageDimensions(pngOf(0, 10, 'a.png'), 'image/png')).toBeNull();
		expect(await readImageDimensions(bytes([0xff, 0xd8, 0xff, 0xda]), 'image/jpeg')).toBeNull();
		expect(await readImageDimensions(pngOf(10, 10, 'a.png'), 'image/bmp')).toBeNull();
	});
});

describe('sizeText and typesText', () => {
	it('say sizes and types as the messages do', () => {
		expect(sizeText(512)).toBe('512 B');
		expect(sizeText(2048)).toBe('2 KB');
		expect(sizeText(1536 * 1024)).toBe('1.5 MB');
		expect(sizeText(2 * 1024 * 1024 * 1024)).toBe('2 GB');
		expect(typesText(DEFAULT_RICH_TEXT_UPLOAD_TYPES)).toBe('PNG, JPEG, GIF, WebP, AVIF');
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

	it('tells the server of the images taken out of the text, via Ajax', async () => {
		await import('../src/wicket/rich-text-editor.js');
		const global = window as unknown as { Wicket: Record<string, unknown> };
		const Wicket = global.Wicket.RichTextEditor as WicketRichTextEditor;
		const post = vi.fn();
		const ajax = global.Wicket.Ajax;
		global.Wicket.Ajax = { post };
		vi.useFakeTimers();
		try {
			textArea('<p>Text</p><img src="/a.png">');
			const editor = Wicket.attach('notes', { imagesUrl: 'images?5' })!;
			const state = editor.editor.state;
			editor.editor.view.dispatch(state.tr.delete(6, 7));
			await vi.advanceTimersByTimeAsync(300);
			expect(post).toHaveBeenCalledWith({ u: 'images?5', c: 'notes',
				ep: [{ name: 'removed', value: '["/a.png"]' }] });
		} finally {
			vi.useRealTimers();
			global.Wicket.Ajax = ajax;
		}
	});
});
