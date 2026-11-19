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
import { TextSelection } from '@tiptap/pm/state';

import { attachmentsIn, attachmentUrlTest, checkAttachments, DEFAULT_RICH_TEXT_ATTACH_TYPES,
	DEFAULT_RICH_TEXT_EDITOR_TEXTS, linksToAttachments, Popover, RichTextEditor, setHost,
	standaloneHost } from '../src/index.js';

const tick = (ms = 20) => new Promise((resolve) => setTimeout(resolve, ms));

beforeAll(() => {
	const rect = { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0,
		toJSON: () => ({}) } as DOMRect;
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => rect;
	document.elementFromPoint = () => null;
});

afterEach(() => {
	Popover.closeAll();
	document.querySelectorAll<HTMLElement>('.modal-dialog-close').forEach((close) => close.click());
	document.body.innerHTML = '';
	setHost(standaloneHost);
	vi.restoreAllMocks();
	vi.unstubAllGlobals();
});

function textArea(html = '<p>Text</p>'): HTMLTextAreaElement {
	document.body.innerHTML = '<form><textarea id="notes"></textarea></form>';
	const field = document.getElementById('notes') as HTMLTextAreaElement;
	field.defaultValue = html;
	return field;
}

function tool(editor: RichTextEditor, name: string): HTMLButtonElement | null {
	return editor.element.querySelector<HTMLButtonElement>(
		`.antilia-rich-text-toolbar [data-tool="${name}"]`);
}

function select(editor: RichTextEditor, anchor: number): void {
	const state = editor.editor.state;
	editor.editor.view.dispatch(state.tr.setSelection(TextSelection.create(state.doc, anchor)));
}

function pdf(name = 'report.pdf', size = 1536): File {
	return new File([new Uint8Array(size)], name, { type: 'application/pdf' });
}

function png(name = 'bird.png'): File {
	return new File([new Uint8Array(4)], name, { type: 'image/png' });
}

function drop(editor: RichTextEditor, files: File[]): boolean {
	const event = new Event('drop', { cancelable: true });
	Object.assign(event, { dataTransfer: { files, types: ['Files'], getData: () => '' },
		clientX: 0, clientY: 0 });
	const view = editor.editor.view;
	vi.spyOn(view, 'posAtCoords').mockReturnValue(null);
	return !!view.someProp('handleDrop', (handle) => handle(view, event as DragEvent, Slice.empty,
		false));
}

function message(editor: RichTextEditor): string | null {
	const element = editor.element.querySelector('.antilia-rich-text-message');
	return element ? element.textContent : null;
}

const LINK = '<a class="antilia-attachment antilia-attachment-pdf" href="/files/r.pdf" ' +
	'target="_blank" data-size="1.5 KB" rel="noopener noreferrer nofollow">report.pdf</a>';

describe('the files attached', () => {
	it('are links of their own, their kind in their class and their size in data-size', () => {
		const editor = new RichTextEditor(textArea('<p>See ' + LINK + ' now</p>'));
		expect(editor.getValue()).toBe('<p>See ' + LINK + ' now</p>');
		const node = editor.editor.state.doc.firstChild!.child(1);
		expect(node.type.name).toBe('attachment');
		expect(node.attrs).toEqual({ href: '/files/r.pdf', name: 'report.pdf', size: '1.5 KB',
			kind: 'pdf' });
	});

	it('take no unsafe address, nor a size or a kind the editor does not write', () => {
		const editor = new RichTextEditor(textArea('<p><a class="antilia-attachment" ' +
			'href="javascript:steal()">x</a> <a class="antilia-attachment antilia-attachment-Bad!" ' +
			'href="/f" data-size="huge">y</a></p>'));
		expect(editor.getValue()).toBe('<p>x <a class="antilia-attachment" href="/f" ' +
			'target="_blank" rel="noopener noreferrer nofollow">y</a></p>');
	});

	it('are plain links in Markdown', () => {
		const editor = new RichTextEditor(textArea(''), { format: 'markdown' });
		editor.editor.commands.insertContent({ type: 'attachment', attrs: { href: '/files/r.pdf',
			name: 'report [final].pdf', size: '2 MB', kind: 'pdf' } });
		expect(editor.getValue()).toBe('[report \\[final\\].pdf](/files/r.pdf)');
	});

	it('come back as files from Markdown when the page tells their addresses', async () => {
		vi.useFakeTimers();
		try {
			const onAttachmentsRemoved = vi.fn();
			const markdown = 'See [report.pdf](/files/r.pdf), [site](/about) and ' +
				'[**bold.pdf**](/files/b.pdf)';
			const editor = new RichTextEditor(textArea(markdown), { format: 'markdown',
				onAttach: () => Promise.resolve('/f'), onAttachmentsRemoved,
				attachmentUrls: ['/files/r.pdf', '/files/b.pdf'] });
			const paragraph = editor.editor.state.doc.firstChild!;
			expect(paragraph.child(1).type.name).toBe('attachment');
			expect(paragraph.child(1).attrs).toEqual({ href: '/files/r.pdf', name: 'report.pdf',
				size: null, kind: 'pdf' });
			expect(attachmentsIn(editor.editor.state.doc)).toEqual(new Map([['/files/r.pdf', 1]]));
			// the same Markdown, not modified, nothing to undo
			expect(editor.getValue()).toBe(markdown);
			expect(editor.isModified()).toBe(false);
			expect(editor.editor.can().undo()).toBe(false);

			// a text the page gives too, by a test of the addresses
			editor.setValue('[a.csv](/files/a.csv?v=2) [b](/files/b)');
			expect(attachmentsIn(editor.editor.state.doc).size).toBe(0);
			const tested = new RichTextEditor(textArea('[a.csv](/files/a.csv?v=2) [b](/files/b)'),
				{ format: 'markdown', onAttach: () => Promise.resolve('/f'), onAttachmentsRemoved,
					attachmentUrls: (href) => href.startsWith('/files/') });
			const attrs = tested.editor.state.doc.firstChild!.child(0).attrs;
			expect(attrs).toEqual({ href: '/files/a.csv?v=2', name: 'a.csv', size: null,
				kind: 'csv' });
			expect(tested.editor.state.doc.firstChild!.child(2).attrs.kind).toBeNull();
			tested.setValue('[r.pdf](/files/r.pdf)');
			expect(attachmentsIn(tested.editor.state.doc)).toEqual(new Map([['/files/r.pdf', 1]]));

			// taken out, the page hears of it
			tested.editor.commands.setContent('');
			await vi.advanceTimersByTimeAsync(300);
			expect(onAttachmentsRemoved).toHaveBeenCalledWith(['/files/r.pdf']);
		} finally {
			vi.useRealTimers();
		}
	});

	it('come back as links where no file is told, or the schema has none', () => {
		const markdown = '[report.pdf](/files/r.pdf)';
		const none = new RichTextEditor(textArea(markdown), { format: 'markdown',
			onAttach: () => Promise.resolve('/f') });
		expect(attachmentsIn(none.editor.state.doc).size).toBe(0);
		const noFeature = new RichTextEditor(textArea(markdown), { format: 'markdown',
			attachmentUrls: ['/files/r.pdf'] });
		expect(noFeature.getValue()).toBe(markdown);
	});

	it('are links of the JSON turned into files: a text of their own at a safe address', () => {
		const isFile = (href: string) => href.startsWith('/files/') || href.startsWith('javascript');
		const link = (text: string, href: string, more: { type: string }[] = []) => ({ type: 'text', text,
			marks: [{ type: 'link', attrs: { href } }, ...more] });
		const json = { type: 'doc', content: [{ type: 'bulletList', content: [{ type: 'listItem',
			content: [{ type: 'paragraph', content: [link('r.pdf', '/files/r.pdf'),
				link('b.pdf', '/files/b.pdf', [{ type: 'bold' }]), link('x', 'javascript:steal()'),
				link('site', '/about')] }] }] }] };
		const converted = linksToAttachments(json, isFile, DEFAULT_RICH_TEXT_ATTACH_TYPES);
		const content = converted.content![0]!.content![0]!.content![0]!.content!;
		expect(content[0]).toEqual({ type: 'attachment', attrs: { href: '/files/r.pdf',
			name: 'r.pdf', size: null, kind: 'pdf' } });
		expect(content.slice(1)).toEqual(json.content[0]!.content[0]!.content[0]!.content.slice(1));
		// the JSON given stays as it was
		expect(json.content[0]!.content[0]!.content[0]!.content[0]!.type).toBe('text');
		const plain = { type: 'doc', content: [{ type: 'paragraph', content: [link('site', '/a')] }] };
		expect(linksToAttachments(plain, isFile, [])).toBe(plain);
		expect(attachmentUrlTest(null)).toBeNull();
		expect(attachmentUrlTest([])).toBeNull();
		expect(attachmentUrlTest(['/a'])!('/a')).toBe(true);
		expect(attachmentUrlTest(() => {
			throw new Error('no');
		})!('/a')).toBe(false);
	});

	it('are checked before they upload: their types, their sizes and how many', () => {
		const texts = DEFAULT_RICH_TEXT_EDITOR_TEXTS;
		const exe = new File([new Uint8Array(4)], 'setup.exe', { type: 'application/x-msdownload' });
		// a browser may give no type: the extension tells it
		const csv = new File(['a,b'], 'table.csv', { type: '' });
		const check = checkAttachments([pdf(), exe, csv, pdf('big.pdf', 4096)],
			{ types: DEFAULT_RICH_TEXT_ATTACH_TYPES, maxSize: 2048, maxCount: 3 }, texts);
		expect(check.files.map((file) => file.name)).toEqual(['report.pdf', 'table.csv']);
		expect(check.refused).toEqual(['3 files at most at a time: 1 left out.',
			'setup.exe is not a file of a type the editor takes: PDF, DOCX, XLSX, PPTX, ODT, ODS, ' +
			'ZIP, TXT, CSV, Markdown.']);
	});
});

describe('the files dropped', () => {
	it('upload by onAttach and go in as links with their sizes, the images by onUpload', async () => {
		const onAttach = vi.fn(() => Promise.resolve('/files/r.pdf'));
		const onUpload = vi.fn(() => Promise.resolve('/images/b.png'));
		const editor = new RichTextEditor(textArea(), { onAttach, onUpload });
		select(editor, 5);
		expect(drop(editor, [pdf(), png()])).toBe(true);
		await tick();
		expect(onAttach).toHaveBeenCalledTimes(1);
		expect(onUpload).toHaveBeenCalledTimes(1);
		const value = editor.getValue();
		expect(value).toContain('<img src="/images/b.png">');
		expect(value).toContain(LINK);
	});

	it('are refused, with a message, when not of the types taken', async () => {
		vi.useFakeTimers();
		try {
			const onAttach = vi.fn(() => Promise.resolve('/files/x'));
			const editor = new RichTextEditor(textArea(), { onAttach,
				attachTypes: [{ type: 'application/pdf', extension: 'pdf', label: 'PDF' }] });
			drop(editor, [new File(['x'], 'notes.txt', { type: 'text/plain' })]);
			await vi.advanceTimersByTimeAsync(0);
			expect(message(editor)).toBe('notes.txt is not a file of a type the editor takes: PDF.');
			expect(onAttach).not.toHaveBeenCalled();
		} finally {
			vi.useRealTimers();
		}
	});

	it('are left to the browser while the page attaches none and uploads no images', () => {
		const editor = new RichTextEditor(textArea());
		expect(drop(editor, [pdf()])).toBe(false);
	});
});

describe('the tools of the files attached', () => {
	it('show while the page attaches files', () => {
		expect(tool(new RichTextEditor(textArea()), 'attachment')).toBeNull();
		const editor = new RichTextEditor(textArea(), { onAttach: () => Promise.resolve('/f') });
		expect(tool(editor, 'attachment')!.getAttribute('aria-label')).toBe('Attach a file');
		expect(tool(editor, 'attachments')).not.toBeNull();
	});

	it('attach the files dropped on the zone of the form, where the cursor is', async () => {
		const onAttach = vi.fn(() => Promise.resolve('/files/r.pdf'));
		const editor = new RichTextEditor(textArea(), { onAttach });
		select(editor, 5);
		tool(editor, 'attachment')!.click();
		const zone = document.querySelector('.antilia-popover .antilia-rich-text-dropzone')!;
		expect(zone.querySelector('.antilia-rich-text-dropzone-text')!.textContent)
			.toBe('Drop files here');
		const picker = zone.querySelector<HTMLInputElement>('input[type="file"]')!;
		expect(picker.accept.split(',')).toContain('.pdf');
		Object.defineProperty(picker, 'files', { value: [pdf()], configurable: true });
		picker.dispatchEvent(new Event('change', { bubbles: true }));
		await tick();
		expect(document.querySelector('.antilia-popover')).toBeNull();
		expect(editor.getValue()).toBe('<p>Text' + LINK + '</p>');
	});

	it('open the dialog of the uploads, when told, titled for the files', () => {
		const editor = new RichTextEditor(textArea(), { onAttach: () => Promise.resolve('/f'),
			attachDialog: true });
		tool(editor, 'attachment')!.click();
		const button = document.querySelector<HTMLButtonElement>(
			'.antilia-popover .antilia-rich-text-form-upload button')!;
		expect(button.textContent).toBe('Attach files…');
		button.click();
		const dialog = document.querySelector('.antilia-rich-text-upload-dialog')!;
		expect(dialog.textContent).toContain('Attach files');
		expect(dialog.querySelector('.antilia-rich-text-dropzone-text')!.textContent)
			.toBe('Drop files here');
	});

	it('list the files of the text, and take one out, which the page hears of', async () => {
		vi.useFakeTimers();
		try {
			const onAttachmentsRemoved = vi.fn();
			const editor = new RichTextEditor(textArea('<p>See ' + LINK + '</p>'),
				{ onAttach: () => Promise.resolve('/f'), onAttachmentsRemoved });
			tool(editor, 'attachments')!.click();
			const item = editor.element.querySelector('.antilia-rich-text-attachments-item')!;
			expect(item.querySelector('.antilia-rich-text-attachments-name')!.textContent)
				.toBe('report.pdf');
			expect(item.querySelector('.antilia-rich-text-attachments-size')!.textContent)
				.toBe('1.5 KB');
			const remove = item.querySelector<HTMLButtonElement>(
				'.antilia-rich-text-attachments-delete')!;
			expect(remove.getAttribute('aria-label')).toBe('Delete report.pdf');
			remove.click();
			expect(editor.getValue()).toBe('<p>See </p>');
			await vi.advanceTimersByTimeAsync(300);
			expect(onAttachmentsRemoved).toHaveBeenCalledWith(['/files/r.pdf']);
		} finally {
			vi.useRealTimers();
		}
	});
});

describe('Wicket.RichTextEditor and the files attached', () => {
	it('tells the server of the files taken out, via Ajax with the attributes it rendered',
		async () => {
			await import('../src/wicket/rich-text-editor.js');
			const global = window as unknown as { Wicket: Record<string, unknown> };
			const Wicket = global.Wicket.RichTextEditor as { attach(id: string,
				options?: Record<string, unknown>): RichTextEditor | null };
			const heard: unknown[] = [];
			const ajax = global.Wicket.Ajax;
			global.Wicket.Ajax = { Call: class {
				ajax(attributes: unknown) {
					heard.push(attributes);
				}
			} };
			vi.useFakeTimers();
			try {
				textArea('<p>Text ' + LINK + '</p>');
				const editor = Wicket.attach('notes', { attachUrl: 'files?3',
					imagesAjax: { u: 'images?5', m: 'POST', c: 'notes' } })!;
				expect(tool(editor, 'attachment')).not.toBeNull();
				const state = editor.editor.state;
				editor.editor.view.dispatch(state.tr.delete(6, 7));
				await vi.advanceTimersByTimeAsync(300);
				expect(heard).toEqual([{ u: 'images?5', m: 'POST', c: 'notes',
					ep: [{ name: 'removedAttachments', value: '["/files/r.pdf"]' }] }]);
			} finally {
				vi.useRealTimers();
				global.Wicket.Ajax = ajax;
			}
		});
});
