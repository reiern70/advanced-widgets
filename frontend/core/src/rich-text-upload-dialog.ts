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

/*
 * What the uploads of images show besides the marks in the text: the zone to drop images on or
 * choose them, in the form of the image or in the dialog of the uploads; the dialog of the uploads,
 * a zone and the list of the files, each uploading, uploaded or refused and why, which the user
 * takes out before the images go into the text; and the dialog asking which pictures too large to
 * scale down. The dialogs have the markup of modal.ts, appended to the body of the page, in the look
 * of the editor.
 */

import { buttonClasses } from './button.js';
import { partClasses, type Look } from './look.js';
import { ModalDialog, CLOSE_ATTRIBUTE } from './modal.js';
import { dimensionsText, type RichTextImageDimensions } from './rich-text-image-headers.js';
import type { RichTextEditorTexts } from './rich-text-toolbar.js';
import type { Oversize, UploadHost } from './rich-text-upload.js';

/** Counts the dialogs, for the ids of their parts. */
let dialogs = 0;

/**
 * @returns {0}, {1}... of a text replaced by the values
 */
function fill(text: string, ...values: (string | number)[]): string {
	return text.replace(/\{(\d)\}/g, (all, index: string) => {
		const value = values[Number(index)];
		return value === undefined ? all : String(value);
	});
}

/**
 * @returns whether a drag carries files
 */
function carriesFiles(event: DragEvent): boolean {
	return Array.from(event.dataTransfer?.types ?? []).includes('Files');
}

/**
 * @returns a zone to drop images on, with a button choosing them, several at once, by the file
 *          field it opens, hidden
 * @param types the media types of the images taken, which the file field accepts
 * @param onFiles called with the files dropped or chosen
 */
export function dropZone(texts: RichTextEditorTexts, look: Look, types: readonly string[],
	onFiles: (files: File[]) => void): { element: HTMLElement; button: HTMLButtonElement;
		picker: HTMLInputElement } {
	const element = document.createElement('div');
	element.className = 'antilia-rich-text-dropzone';
	const text = document.createElement('p');
	text.className = 'antilia-rich-text-dropzone-text';
	text.textContent = texts.dropImages;
	const button = document.createElement('button');
	button.type = 'button';
	button.className = buttonClasses({ variant: 'secondary', size: 'small', look });
	button.textContent = texts.chooseImages;
	const picker = document.createElement('input');
	picker.type = 'file';
	picker.multiple = true;
	picker.accept = types.join(',');
	picker.hidden = true;
	picker.tabIndex = -1;
	picker.className = 'antilia-rich-text-form-file';
	button.addEventListener('click', () => picker.click());
	picker.addEventListener('change', () => {
		const files = Array.from(picker.files ?? []);
		picker.value = '';
		if (files.length) {
			onFiles(files);
		}
	});
	const over = (event: Event) => {
		if (carriesFiles(event as DragEvent)) {
			event.preventDefault();
			element.classList.add('antilia-rich-text-dropzone-over');
		}
	};
	element.addEventListener('dragenter', over);
	element.addEventListener('dragover', over);
	element.addEventListener('dragleave', (event) => {
		if (!element.contains((event as DragEvent).relatedTarget as Node | null)) {
			element.classList.remove('antilia-rich-text-dropzone-over');
		}
	});
	element.addEventListener('drop', (event) => {
		element.classList.remove('antilia-rich-text-dropzone-over');
		const files = Array.from((event as DragEvent).dataTransfer?.files ?? []);
		if (!files.length) {
			return;
		}
		event.preventDefault();
		event.stopPropagation();
		onFiles(files);
	});
	element.append(text, button, picker);
	return { element, button, picker };
}

/** A dialog of the markup of modal.ts, while it is open. */
interface Frame {
	wrapper: HTMLElement;
	overlay: HTMLElement;
	body: HTMLElement;
	footer: HTMLElement;
}

/**
 * @returns the markup of a dialog, appended to the body of the page: its title, a button closing
 *          it, its body and its footer
 */
function frame(look: Look, title: string, close: string, className: string): Frame {
	const id = 'antilia-rich-text-dialog-' + (++dialogs);
	const wrapper = document.createElement('div');
	wrapper.className = partClasses(look, 'dialog-theme') + ' ' + className;
	const overlay = document.createElement('div');
	overlay.className = partClasses(look, 'dialog-overlay');
	const dialog = document.createElement('div');
	dialog.className = partClasses(look, 'dialog');
	dialog.setAttribute('role', 'dialog');
	dialog.setAttribute('aria-modal', 'true');
	dialog.setAttribute('aria-labelledby', id + '-title');
	const content = document.createElement('div');
	content.className = partClasses(look, 'dialog-content');
	const header = document.createElement('div');
	header.className = partClasses(look, 'dialog-header');
	const heading = document.createElement('h1');
	heading.className = partClasses(look, 'dialog-title');
	heading.id = id + '-title';
	heading.textContent = title;
	const closing = document.createElement('button');
	closing.type = 'button';
	closing.className = partClasses(look, 'dialog-close');
	closing.setAttribute('aria-label', close);
	closing.title = close;
	header.append(heading, closing);
	const body = document.createElement('div');
	body.className = partClasses(look, 'dialog-body');
	const footer = document.createElement('div');
	footer.className = partClasses(look, 'dialog-footer');
	content.append(header, body, footer);
	dialog.append(content);
	overlay.append(dialog);
	wrapper.append(overlay);
	document.body.append(wrapper);
	return { wrapper, overlay, body, footer };
}

function footerButton(look: Look, label: string, variant: 'primary' | 'secondary',
	closes: boolean): HTMLButtonElement {
	const button = document.createElement('button');
	button.type = 'button';
	button.className = buttonClasses({ variant, size: 'small', look });
	button.textContent = label;
	if (closes) {
		button.setAttribute(CLOSE_ATTRIBUTE, '');
	}
	return button;
}

/**
 * Asks the user which pictures too large to scale down, each by a box to tick, in a dialog; the
 * others are left out. Cancel, Escape or closing the dialog leaves them all out.
 *
 * @param max the widest and the highest a picture may be
 * @returns the files of the pictures to scale down
 */
export function askToScale(oversize: readonly Oversize[], max: RichTextImageDimensions,
	texts: RichTextEditorTexts, look: Look): Promise<ReadonlySet<File>> {
	return new Promise((resolve) => {
		const { wrapper, overlay, body, footer } = frame(look, texts.scaleTitle, texts.close,
			'antilia-rich-text-scale-dialog');
		const text = document.createElement('p');
		text.textContent = fill(texts.scaleText, dimensionsText(max));
		const list = document.createElement('ul');
		list.className = 'antilia-rich-text-scale-list';
		const boxes = oversize.map((each) => {
			const item = document.createElement('li');
			const label = document.createElement('label');
			label.className = partClasses(look, 'rich-text-form-check');
			const box = document.createElement('input');
			box.type = 'checkbox';
			box.className = partClasses(look, 'rich-text-form-checkbox');
			const name = each.file.name || texts.theImage;
			const size = document.createElement('span');
			size.className = 'antilia-rich-text-scale-size';
			size.textContent = ' (' + dimensionsText(each.dimensions) + ')';
			label.append(box, document.createTextNode(' ' + fill(texts.scaleDown, name,
				dimensionsText(each.scaled))), size);
			item.append(label);
			list.append(item);
			return box;
		});
		body.append(text, list);
		const upload = footerButton(look, texts.upload, 'primary', false);
		const cancel = footerButton(look, texts.cancel, 'secondary', true);
		footer.append(upload, cancel);
		let modal: ModalDialog | null = null;
		let done = false;
		const finish = (files: ReadonlySet<File>) => {
			if (done) {
				return;
			}
			done = true;
			modal?.destroy();
			wrapper.remove();
			resolve(files);
		};
		upload.addEventListener('click', () => finish(new Set(oversize.filter((_each, index) =>
			boxes[index]!.checked).map((each) => each.file))));
		modal = new ModalDialog(overlay, { size: 'small', foldable: false, look,
			onClose: () => finish(new Set()) });
	});
}

/** A file of the dialog of the uploads. */
interface Row {
	element: HTMLElement;
	state: 'uploading' | 'done' | 'refused';
	/** The address of its image, once uploaded. */
	src: string | null;
}

/**
 * Opens the dialog of the uploads: a zone to drop images on or choose them, and the list of the
 * files, each uploading, uploaded, or refused and why, with a button taking it out. Insert puts the
 * images uploaded into the text, where the cursor is, in the order of the list; Cancel, Escape or
 * closing the dialog puts none, and tells the page of those uploaded, which the text never got.
 *
 * @param insert puts the images of the addresses into the text
 */
export function openUploadDialog(host: UploadHost, texts: RichTextEditorTexts, look: Look,
	insert: (srcs: readonly string[]) => void): void {
	const { wrapper, overlay, body, footer } = frame(look, texts.uploadImagesTitle, texts.close,
		'antilia-rich-text-upload-dialog');
	const list = document.createElement('ul');
	list.className = 'antilia-rich-text-upload-list';
	list.setAttribute('aria-live', 'polite');
	const rows: Row[] = [];
	const ok = footerButton(look, texts.insertImages, 'primary', false);
	const cancel = footerButton(look, texts.cancel, 'secondary', true);
	const update = () => {
		ok.disabled = !rows.some((row) => row.state === 'done') ||
			rows.some((row) => row.state === 'uploading');
	};
	let open = true;
	const row = (name: string, status: string, state: Row['state']): Row => {
		const element = document.createElement('li');
		element.className = 'antilia-rich-text-upload-file antilia-rich-text-upload-' + state;
		const label = document.createElement('span');
		label.className = 'antilia-rich-text-upload-name';
		label.textContent = name;
		const said = document.createElement('span');
		said.className = 'antilia-rich-text-upload-status';
		said.textContent = status;
		const remove = document.createElement('button');
		remove.type = 'button';
		remove.className = buttonClasses({ variant: 'ghost', size: 'small', iconOnly: true, look }) +
			' antilia-rich-text-upload-remove';
		const removeLabel = fill(texts.removeFile, name || texts.theImage);
		remove.setAttribute('aria-label', removeLabel);
		remove.title = removeLabel;
		remove.textContent = '×';
		element.append(label, said, remove);
		list.append(element);
		const made: Row = { element, state, src: null };
		remove.addEventListener('click', () => {
			rows.splice(rows.indexOf(made), 1);
			element.remove();
			if (made.src) {
				host.discard([made.src]);
			}
			made.state = 'refused';
			update();
		});
		rows.push(made);
		return made;
	};
	const add = async (files: File[]) => {
		const { files: checked, refused } = await host.check(files);
		if (!open) {
			return;
		}
		refused.forEach((message) => row('', message, 'refused'));
		for (const file of checked) {
			const name = file.name || texts.theImage;
			const made = row(name, host.uploading(file), 'uploading');
			update();
			host.upload(file).then((src) => {
				if (!open || !rows.includes(made)) {
					host.discard([src]);
					return;
				}
				made.src = src;
				made.state = 'done';
				made.element.className = 'antilia-rich-text-upload-file antilia-rich-text-upload-done';
				made.element.querySelector('.antilia-rich-text-upload-status')!.textContent =
					texts.uploadDone;
				update();
			}, (error: unknown) => {
				if (!open || !rows.includes(made)) {
					return;
				}
				made.state = 'refused';
				made.element.className =
					'antilia-rich-text-upload-file antilia-rich-text-upload-refused';
				made.element.querySelector('.antilia-rich-text-upload-status')!.textContent =
					error instanceof Error && error.message ? error.message :
						fill(texts.uploadFailed, name);
				update();
			});
		}
		update();
	};
	const zone = dropZone(texts, look, host.types(), (files) => void add(files));
	body.append(zone.element, list);
	footer.append(ok, cancel);
	update();
	let modal: ModalDialog | null = null;
	const finish = (srcs: readonly string[]) => {
		if (!open) {
			return;
		}
		open = false;
		modal?.destroy();
		wrapper.remove();
		const left = rows.map((each) => each.src).filter((src): src is string => !!src &&
			!srcs.includes(src));
		if (left.length) {
			host.discard(left);
		}
		if (srcs.length) {
			insert(srcs);
		}
	};
	ok.addEventListener('click', () => {
		if (!ok.disabled) {
			finish(rows.filter((each) => each.state === 'done' && each.src).map((each) =>
				each.src!));
		}
	});
	modal = new ModalDialog(overlay, { size: 'medium', foldable: false, look,
		onClose: () => finish([]) });
	zone.button.focus();
}
