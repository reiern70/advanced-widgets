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
 * The images a user uploads into the rich text editor: dropped on the zone of the form of the image
 * or chosen there, several at once, or in the dialog of the uploads (see
 * rich-text-upload-dialog.ts); dropped into the text or pasted. The page uploads them (the option
 * onUpload): it keeps the file and answers the address it is served at, which goes through
 * isSafeUrl as any other, and the server's cleaning; the editor never writes a data: address.
 * uploadImageTo() posts a file to a URL and reads the address from the answer, as Wicket's
 * RichTextEditor answers.
 *
 * Before uploading, the editor checks the files against the page's limits: how many one drop or
 * choice takes (uploadMaxCount), the type of each and its size (uploadTypes, uploadMaxSize), and
 * its width and height (uploadMaxWidth, uploadMaxHeight), read from the header of the file without
 * decoding the picture. A picture too large is refused, or, while the page allows it
 * (uploadScaling), offered in a dialog to scale down, which the user ticks: the browser then
 * scales it down before sending it. The server checks them again, the browser's word is not to be
 * trusted. While a file uploads, a mark stands where its image goes, with the share of the file
 * sent and a button canceling the upload; the images take their places once the page answered, in
 * the order of the files, or a message says why one did not. Canceling takes the mark away and
 * inserts nothing.
 *
 * deleteImagesAt() asks Wicket's RichTextImageUploadBehavior to delete images it took, those the
 * user took out of the text (onImagesRemoved). The images the user ticks to store permanently go
 * to the page (onStorePermanently, see rich-text-stored-images.ts) once they are in the text.
 */

import { Extension, type Editor } from '@tiptap/core';
import { Plugin, PluginKey, type EditorState, type Transaction } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

import { dimensionsText, fits, readImageDimensions, scaledToFit,
	type RichTextImageDimensions } from './rich-text-image-headers.js';
import type { RichTextEditorTexts } from './rich-text-toolbar.js';

/** What the editor gives an upload besides the file. */
export interface RichTextUploadOptions {
	/** Called as the file is sent, with the bytes sent and those of the whole request. */
	onProgress?: (sent: number, total: number) => void;
	/** Aborted as the user cancels the upload: the uploader may stop it then. */
	signal?: AbortSignal;
}

/**
 * Uploads an image, answering the address it is served at. It may tell how much of the file it
 * sent, and stop as the user cancels: the editor ignores what an upload canceled answers anyway.
 */
export type RichTextUploader = (file: File, options?: RichTextUploadOptions) => Promise<string>;

/** The types of the images a user may upload unless the page says otherwise. */
export const DEFAULT_RICH_TEXT_UPLOAD_TYPES: readonly string[] = Object.freeze(['image/png',
	'image/jpeg', 'image/gif', 'image/webp', 'image/avif']);

/** What the types are called in the messages. */
const TYPE_NAMES: ReadonlyMap<string, string> = new Map([['image/png', 'PNG'],
	['image/jpeg', 'JPEG'], ['image/gif', 'GIF'], ['image/webp', 'WebP'], ['image/avif', 'AVIF'],
	['image/svg+xml', 'SVG']]);

/** The extensions of the files of the types a picture scaled down is written in. */
const EXTENSIONS: ReadonlyMap<string, string> = new Map([['image/png', 'png'],
	['image/jpeg', 'jpg'], ['image/webp', 'webp']]);

/**
 * @returns an error telling an upload was canceled, as fetch() rejects an aborted request
 */
export function abortError(): Error {
	return new DOMException('The upload was canceled', 'AbortError');
}

/**
 * @returns whether an error tells of an upload canceled
 */
export function isAbort(error: unknown): boolean {
	return error instanceof Error && error.name === 'AbortError';
}

/**
 * @returns an uploader posting a file to a URL, in a form under a name, and reading the address
 *          from the JSON of the answer, {"url": "..."}; {"error": "..."} says why the server
 *          refused it, which the promise is rejected with. It tells the progress of the file sent,
 *          and stops as the signal aborts, rejecting with an AbortError: by XMLHttpRequest, as
 *          fetch() tells no progress of what it sends.
 */
export function uploadImageTo(url: string, name = 'file'): RichTextUploader {
	return (file, options = {}) => new Promise<string>((resolve, reject) => {
		const { signal, onProgress } = options;
		if (signal?.aborted) {
			reject(abortError());
			return;
		}
		const body = new FormData();
		body.append(name, file, file.name);
		const request = new XMLHttpRequest();
		const abort = () => request.abort();
		const settle = () => signal?.removeEventListener('abort', abort);
		request.open('POST', url);
		request.setRequestHeader('Accept', 'application/json');
		request.responseType = 'text';
		if (onProgress) {
			request.upload.addEventListener('progress', (event) => {
				if (event.lengthComputable && event.total > 0) {
					onProgress(event.loaded, event.total);
				}
			});
		}
		request.addEventListener('load', () => {
			settle();
			let answer: { url?: unknown; error?: unknown } = {};
			try {
				answer = JSON.parse(request.responseText) as typeof answer;
			} catch {
				// no JSON: the server failed
			}
			if (typeof answer.error === 'string' && answer.error) {
				reject(new Error(answer.error));
			} else if (request.status < 200 || request.status >= 300 ||
				typeof answer.url !== 'string') {
				reject(new Error(''));
			} else {
				resolve(answer.url);
			}
		});
		request.addEventListener('error', () => {
			settle();
			reject(new Error(''));
		});
		request.addEventListener('abort', () => {
			settle();
			reject(abortError());
		});
		signal?.addEventListener('abort', abort);
		request.send(body);
	});
}

/**
 * @returns a function asking a URL to delete images, by their addresses: it posts them as a JSON
 *          array under a name, as Wicket's RichTextImageUploadBehavior takes them, which deletes
 *          those it took alone, and resolves to the addresses deleted, as the answer says,
 *          {"deleted": [...]}
 */
export function deleteImagesAt(url: string,
	name = 'removed'): (urls: readonly string[]) => Promise<string[]> {
	return async (urls) => {
		const body = new URLSearchParams();
		body.append(name, JSON.stringify(urls));
		const response = await fetch(url, { method: 'POST', body, credentials: 'same-origin',
			headers: { Accept: 'application/json' } });
		let answer: { deleted?: unknown } = {};
		try {
			answer = await response.json() as typeof answer;
		} catch {
			// no JSON: the server failed
		}
		return Array.isArray(answer.deleted) ? answer.deleted.filter((each): each is string =>
			typeof each === 'string') : [];
	};
}

/**
 * @returns a size as the messages say it: 2 MB, 1.5 MB, 300 KB
 */
export function sizeText(bytes: number): string {
	const units: [number, string][] = [[1024 * 1024 * 1024, 'GB'], [1024 * 1024, 'MB'],
		[1024, 'KB']];
	for (const [size, unit] of units) {
		if (bytes >= size) {
			const value = Math.round(bytes / size * 10) / 10;
			return value + ' ' + unit;
		}
	}
	return bytes + ' B';
}

/**
 * @returns the types of files as the messages name them: PNG, JPEG, GIF, WebP
 */
export function typesText(types: readonly string[]): string {
	return types.map((type) => TYPE_NAMES.get(type.toLowerCase()) ??
		type.replace(/^image\//i, '').toUpperCase()).join(', ');
}

/** What files to upload a check left: those to upload, maybe scaled down, and why the others not. */
export interface UploadCheck {
	/** The files to upload, in their order. */
	files: File[];
	/** The file each of those comes from: itself, or the one it scales down. */
	from: File[];
	/** Why the others are not uploaded, a message for each; none for those the user left out. */
	refused: string[];
}

/** The limits of the uploads, as the page gives them. */
export interface UploadLimits {
	/** The media types taken, lower case. */
	types: readonly string[];
	/** The largest file, in bytes; none for null. */
	maxSize: number | null;
	/** How many files one drop or choice takes; none for null. */
	maxCount: number | null;
	/** The widest and the highest picture; none for null. */
	maxDimensions: RichTextImageDimensions | null;
	/** Whether the user may have a picture too large scaled down rather than refused. */
	scaling: boolean;
}

/** What the messages of the checks say. */
export interface UploadCheckTexts {
	/** What stands for the name of a file that has none. */
	theImage: string;
	uploadType: string;
	uploadTooLarge: string;
	uploadTooMany: string;
	uploadTooBig: string;
	uploadFailed: string;
}

/** A picture too large, which the user may have scaled down. */
export interface Oversize {
	file: File;
	/** Its width and height. */
	dimensions: RichTextImageDimensions;
	/** Its width and height scaled down to fit. */
	scaled: RichTextImageDimensions;
}

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
 * @returns the name of a file as the messages say it, which a file pasted may not have
 */
export function fileNameOf(file: File, texts: Pick<UploadCheckTexts, 'theImage'>): string {
	return file.name || texts.theImage;
}

/**
 * @returns why a file cannot be uploaded, not by its pixels: not of the types taken, or too large
 *          in bytes; null when it can
 */
export function refusalOf(file: File, limits: Pick<UploadLimits, 'types' | 'maxSize'>,
	texts: UploadCheckTexts): string | null {
	if (!limits.types.includes(file.type.toLowerCase())) {
		return fill(texts.uploadType, fileNameOf(file, texts), typesText(limits.types));
	}
	if (limits.maxSize != null && file.size > limits.maxSize) {
		return fill(texts.uploadTooLarge, fileNameOf(file, texts), sizeText(limits.maxSize));
	}
	return null;
}

/**
 * Checks files against the limits: how many, the type and the size of each, and its pixels, read
 * from its header. Those too large in pixels the user may have scaled down, while the limits
 * allow it: ask() shows them, and resolves to those the user ticked, which are scaled down then.
 *
 * @param ask asks the user which pictures too large to scale down
 * @returns the files to upload, scaled down maybe, and why the others are not
 */
export async function checkFiles(files: readonly File[], limits: UploadLimits,
	texts: UploadCheckTexts,
	ask: (oversize: readonly Oversize[]) => Promise<ReadonlySet<File>>): Promise<UploadCheck> {
	const refused: string[] = [];
	let taken = files;
	if (limits.maxCount != null && files.length > limits.maxCount) {
		taken = files.slice(0, limits.maxCount);
		refused.push(fill(texts.uploadTooMany, limits.maxCount, files.length - limits.maxCount));
	}
	const max = limits.maxDimensions;
	const accepted: (File | Oversize)[] = [];
	for (const file of taken) {
		const name = fileNameOf(file, texts);
		const typeRefused = refusalOf(file, { types: limits.types, maxSize: null }, texts);
		if (typeRefused) {
			refused.push(typeRefused);
			continue;
		}
		const dimensions = max ? await readImageDimensions(file, file.type) : null;
		if (dimensions && !fits(dimensions, max)) {
			if (limits.scaling) {
				accepted.push({ file, dimensions, scaled: scaledToFit(dimensions, max!) });
			} else {
				refused.push(fill(texts.uploadTooBig, name, dimensionsText(dimensions),
					dimensionsText(max!)));
			}
			continue;
		}
		// a size the browser cannot read is the server's to check
		const sizeRefused = refusalOf(file, limits, texts);
		if (sizeRefused) {
			refused.push(sizeRefused);
			continue;
		}
		accepted.push(file);
	}
	const oversize = accepted.filter((each): each is Oversize => !(each instanceof File));
	const ticked = oversize.length ? await ask(oversize) : new Set<File>();
	const result: File[] = [];
	const from: File[] = [];
	for (const each of accepted) {
		if (each instanceof File) {
			result.push(each);
			from.push(each);
			continue;
		}
		if (!ticked.has(each.file)) {
			continue;
		}
		try {
			const scaled = await scaleImage(each.file, max!, limits.types);
			const tooLarge = refusalOf(scaled, limits, texts);
			if (tooLarge) {
				refused.push(tooLarge);
			} else {
				result.push(scaled);
				from.push(each.file);
			}
		} catch {
			refused.push(fill(texts.uploadFailed, fileNameOf(each.file, texts)));
		}
	}
	return { files: result, from, refused };
}

/**
 * Scales a picture down to fit a limit, its proportions kept: in its type, when the browser can
 * write it and the page takes it, else in PNG or JPEG. An animated GIF loses its animation.
 *
 * @param types the media types the page takes
 * @returns the picture scaled down, as a file named as the first, its extension that of its type
 */
export async function scaleImage(file: File, max: RichTextImageDimensions,
	types: readonly string[]): Promise<File> {
	let bitmap: ImageBitmap;
	try {
		bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
	} catch {
		bitmap = await createImageBitmap(file);
	}
	try {
		const size = scaledToFit({ width: bitmap.width, height: bitmap.height }, max);
		const canvas = document.createElement('canvas');
		canvas.width = size.width;
		canvas.height = size.height;
		const context = canvas.getContext('2d');
		if (!context) {
			throw new Error('No canvas');
		}
		context.imageSmoothingEnabled = true;
		context.imageSmoothingQuality = 'high';
		context.drawImage(bitmap, 0, 0, size.width, size.height);
		const wanted = [file.type.toLowerCase(), 'image/png', 'image/jpeg', 'image/webp']
			.filter((type, index, all) => EXTENSIONS.has(type) && types.includes(type) &&
				all.indexOf(type) === index);
		for (const type of wanted) {
			const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, type,
				0.9));
			// a browser that cannot write the type writes PNG
			if (blob && blob.type === type) {
				const base = (file.name || 'image').replace(/\.[^./\\]*$/, '');
				return new File([blob], base + '.' + EXTENSIONS.get(type)!, { type });
			}
		}
		throw new Error('The browser writes none of the types taken');
	} finally {
		bitmap.close();
	}
}

/** What the uploads of an editor ask of it. */
export interface UploadHost {
	/** The editor of Tiptap, whose text gets the images. */
	editor(): Editor;
	/** Whether the user may upload images now: the page uploads them, and the text is not read only. */
	enabled(): boolean;
	/** The types of the images the user may upload. */
	types(): readonly string[];
	/** Whether the form of the image opens the dialog of the uploads, rather than a zone of its own. */
	dialog(): boolean;
	/**
	 * Checks files before they are uploaded: how many, their types, sizes and pixels, asking the
	 * user which pictures too large to scale down while the page allows it.
	 */
	check(files: readonly File[]): Promise<UploadCheck>;
	/**
	 * Uploads a file, checked already.
	 *
	 * @param options the progress, and the signal canceling the upload
	 * @returns the address of the image; rejected with the message saying why not, or with an
	 *          AbortError once canceled
	 */
	upload(file: File, options?: RichTextUploadOptions): Promise<string>;
	/** @returns the text of the mark standing where the image of a file goes */
	uploading(file: File): string;
	/** What the editor says. */
	texts(): RichTextEditorTexts;
	/** Tells the user something went wrong. */
	alert(message: string): void;
	/** Tells the page images were uploaded that never went into the text, or no longer are. */
	discard(urls: readonly string[]): void;
	/** Whether the user may store the images uploaded permanently, by a box to tick. */
	permanent(): boolean;
	/**
	 * Has the page store images uploaded permanently.
	 *
	 * @returns the addresses of those stored
	 */
	storePermanently(urls: readonly string[]): Promise<string[]>;
}

/**
 * Has the page store images permanently, telling the user of those it did not store.
 *
 * @param images the address of each image, and the name of its file
 */
export async function keepPermanently(host: UploadHost,
	images: readonly { src: string; name: string }[]): Promise<void> {
	if (!images.length) {
		return;
	}
	let stored: readonly string[];
	try {
		stored = await host.storePermanently(images.map((each) => each.src));
	} catch {
		stored = [];
	}
	const texts = host.texts();
	const failed = images.filter((each) => !stored.includes(each.src));
	if (failed.length) {
		host.alert(failed.map((each) => fill(texts.permanentFailed, each.name || texts.theImage))
			.join(' '));
	}
}

/** A file uploading: the mark of where its image goes, with its progress and its cancel. */
interface Uploading {
	id: object;
	position: number;
	text: string;
	/** The label of the button canceling the upload. */
	cancelLabel: string;
	/** The share of the file sent, from 0 to 1; null before the upload tells it. */
	progress: number | null;
	/** Cancels the upload, taking the mark away. */
	cancel: () => void;
	/** The element of the mark the editor shows, if any, which the progress updates. */
	element: HTMLElement | null;
}

/** Counts the marks, for the keys of their widgets. */
let marks = 0;

/** The icon of the button canceling an upload: a cross. */
const CROSS = '<svg viewBox="0 0 24 24" width="1em" height="1em" fill="none" ' +
	'stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true" ' +
	'focusable="false"><path d="M6 6l12 12M18 6 6 18"/></svg>';

/**
 * @returns the share of a file sent as the mark and the dialog say it: 42 %
 */
export function percentText(progress: number): string {
	return Math.round(Math.min(Math.max(progress, 0), 1) * 100) + ' %';
}

/**
 * @returns a bar of progress, its share set by showProgress(), with no share until then
 * @param label what the bar is the progress of
 */
export function progressBar(label: string): HTMLElement {
	const bar = document.createElement('span');
	bar.className = 'antilia-rich-text-progress';
	bar.setAttribute('role', 'progressbar');
	bar.setAttribute('aria-label', label);
	bar.setAttribute('aria-valuemin', '0');
	bar.setAttribute('aria-valuemax', '100');
	const fill = document.createElement('span');
	fill.className = 'antilia-rich-text-progress-fill';
	bar.append(fill);
	return bar;
}

/**
 * Shows the share sent on a bar of progressBar(): its length, by a custom property the script
 * sets through the CSSOM, which a strict policy allows, and its value.
 */
export function showProgress(bar: HTMLElement, progress: number | null): void {
	if (progress === null) {
		bar.removeAttribute('aria-valuenow');
		bar.removeAttribute('aria-valuetext');
		return;
	}
	const share = Math.min(Math.max(progress, 0), 1);
	bar.style.setProperty('--antilia-rich-text-progress', String(share));
	bar.setAttribute('aria-valuenow', String(Math.round(share * 100)));
	bar.setAttribute('aria-valuetext', percentText(share));
}

/**
 * @returns the element of a mark: what uploads, its progress and the button canceling it
 */
function markElement(uploading: Uploading): HTMLElement {
	const element = document.createElement('span');
	element.className = 'antilia-rich-text-uploading';
	element.setAttribute('contenteditable', 'false');
	const text = document.createElement('span');
	text.className = 'antilia-rich-text-uploading-text';
	text.setAttribute('role', 'status');
	text.textContent = uploading.text;
	const bar = progressBar(uploading.text);
	const percent = document.createElement('span');
	percent.className = 'antilia-rich-text-uploading-percent';
	percent.setAttribute('aria-hidden', 'true');
	const cancel = document.createElement('button');
	cancel.type = 'button';
	cancel.className = 'antilia-rich-text-uploading-cancel';
	cancel.setAttribute('aria-label', uploading.cancelLabel);
	cancel.title = uploading.cancelLabel;
	cancel.insertAdjacentHTML('beforeend', CROSS);
	cancel.addEventListener('mousedown', (event) => event.preventDefault());
	cancel.addEventListener('click', (event) => {
		event.preventDefault();
		uploading.cancel();
	});
	element.append(text, bar, percent, cancel);
	uploading.element = element;
	updateMark(uploading);
	return element;
}

/**
 * Shows the progress of an upload on its mark.
 */
function updateMark(uploading: Uploading): void {
	const element = uploading.element;
	if (!element) {
		return;
	}
	showProgress(element.querySelector<HTMLElement>('.antilia-rich-text-progress')!,
		uploading.progress);
	element.querySelector('.antilia-rich-text-uploading-percent')!.textContent =
		uploading.progress === null ? '' : percentText(uploading.progress);
}

/**
 * What a transaction does to the marks: adds one, or takes one away, maybe moving others, those
 * of the files after it, to where its image ends.
 */
type UploadMeta = { add: Uploading } |
	{ remove: object; move?: { ids: readonly object[]; position: number } };

/** What the spec of the widget of a mark holds. */
interface MarkSpec {
	id: object;
	uploading: Uploading;
}

const KEY = new PluginKey<DecorationSet>('antiliaRichTextUploads');

/**
 * @returns where the mark of a file uploading stands, null when it is gone
 */
function markOf(state: EditorState, id: object): number | null {
	const found = KEY.getState(state)?.find(undefined, undefined, (spec) => spec.id === id);
	return found?.length ? found[0]!.from : null;
}

/**
 * @returns the mark of a file uploading, before what goes in where it stands; the editor leaves
 *          the events of its button to it
 */
function mark(uploading: Uploading): Decoration {
	return Decoration.widget(uploading.position, () => markElement(uploading),
		{ id: uploading.id, uploading, side: -1, key: 'antilia-upload-' + (++marks),
			ignoreSelection: true, stopEvent: () => true });
}

/**
 * @returns where what the last step of a transaction inserted ends, in the text it made: after an
 *          image inserted, wherever the editor put it; the position given without a step
 */
function endOfLastStep(tr: Transaction, position: number): number {
	const maps = tr.mapping.maps;
	let end = tr.mapping.map(position, 1);
	maps[maps.length - 1]?.forEach((_from, _to, _newFrom, newTo) => {
		end = newTo;
	});
	return end;
}

/**
 * Checks files and uploads them, each image taking the place of its mark once the page answered,
 * in the order of the files. The marks stand from the start, where the images go as the text
 * changes meanwhile, each showing the share of its file sent and a button canceling it; those of
 * the files the check leaves out go, and so do those the user cancels, their images not inserted.
 *
 * @param position where the images go
 * @param permanent whether the page stores the images permanently once they are in the text
 */
export async function uploadFiles(host: UploadHost, files: readonly File[],
	position: number, permanent = false): Promise<void> {
	const start = host.editor();
	const at = Math.min(Math.max(position, 0), start.state.doc.content.size);
	const texts = host.texts();
	const unmark = (id: object) => {
		const current = host.editor();
		if (!current.isDestroyed && markOf(current.state, id) !== null) {
			current.view.dispatch(current.state.tr.setMeta(KEY, { remove: id })
				.setMeta('addToHistory', false));
		}
	};
	const uploads = new Map<File, { uploading: Uploading; controller: AbortController }>();
	for (const file of files) {
		const controller = new AbortController();
		const uploading: Uploading = { id: {}, position: at, text: host.uploading(file),
			cancelLabel: fill(texts.cancelUpload, fileNameOf(file, texts)), progress: null,
			element: null, cancel: () => {
				controller.abort();
				unmark(uploading.id);
			} };
		uploads.set(file, { uploading, controller });
		start.view.dispatch(start.state.tr.setMeta(KEY, { add: uploading })
			.setMeta('addToHistory', false));
	}
	let check: UploadCheck;
	try {
		check = await host.check(files);
	} catch {
		check = { files: [], from: [], refused: [] };
	}
	const { files: checked, from, refused } = check;
	uploads.forEach(({ uploading }, file) => {
		if (!from.includes(file)) {
			unmark(uploading.id);
		}
	});
	if (refused.length) {
		host.alert(refused.join(' '));
	}
	const inserted: { src: string; name: string }[] = [];
	// the image before goes in first, or fails, so that the images keep their order
	let before: Promise<unknown> = Promise.resolve();
	checked.forEach((file, index) => {
		const { uploading, controller } = uploads.get(from[index]!)!;
		const id = uploading.id;
		const signal = controller.signal;
		const uploaded = signal.aborted ? Promise.reject(abortError()) :
			host.editor().isDestroyed ? Promise.reject(new Error('')) :
				host.upload(file, { signal, onProgress: (sent, total) => {
					uploading.progress = sent / total;
					updateMark(uploading);
				} });
		// failing before the image before it is in: told once that one is
		uploaded.catch(() => undefined);
		// the marks of the files after it, which go after its image
		const later = checked.slice(index + 1).map((_each, next) =>
			uploads.get(from[index + 1 + next]!)!.uploading.id);
		before = before.then(() => uploaded).then((src) => {
			const current = host.editor();
			const where = current.isDestroyed || signal.aborted ? null :
				markOf(current.state, id);
			if (where === null) {
				// the editor went, the text where the image went, or the user canceled it: the
				// image is not wanted
				unmark(id);
				host.discard([src]);
				return;
			}
			// the cursor stays where the user is typing meanwhile
			current.chain().insertContentAt(where, { type: 'image', attrs: { src, alt: null } },
				{ updateSelection: false }).command(({ tr }) => {
				tr.setMeta(KEY, { remove: id, move: { ids: later, position: endOfLastStep(tr,
					where) } });
				return true;
			}).run();
			inserted.push({ src, name: fileNameOf(file, texts) });
		}, (error: unknown) => {
			unmark(id);
			const message = error instanceof Error ? error.message : String(error);
			if (message && !isAbort(error) && !signal.aborted) {
				host.alert(message);
			}
		});
	});
	await before;
	if (permanent) {
		await keepPermanently(host, inserted);
	}
}

/**
 * @returns the files of a drop or a paste
 */
function filesOf(data: DataTransfer | null): File[] {
	return data ? Array.from(data.files ?? []) : [];
}

/**
 * Uploads the files dropped into the text, or pasted without HTML, as a screenshot is, while the
 * page uploads images; otherwise the editor leaves them to the browser, as before.
 */
export function uploadExtension(host: UploadHost): Extension {
	return Extension.create({
		name: 'antiliaRichTextUploads',

		addProseMirrorPlugins() {
			return [new Plugin<DecorationSet>({
				key: KEY,
				state: {
					init: () => DecorationSet.empty,
					apply: (transaction, marks) => {
						let next = marks.map(transaction.mapping, transaction.doc);
						const meta = transaction.getMeta(KEY) as UploadMeta | undefined;
						if (meta && 'add' in meta) {
							next = next.add(transaction.doc, [mark(meta.add)]);
						} else if (meta) {
							next = next.remove(next.find(undefined, undefined, (spec) =>
								spec.id === meta.remove));
							const move = meta.move;
							if (move?.ids.length) {
								const moved = next.find(undefined, undefined, (spec) =>
									move.ids.includes(spec.id as object));
								// before remove(), which empties the array it is given
								const specs = moved.map((each) => each.spec as MarkSpec);
								next = next.remove(moved);
								next = next.add(transaction.doc, move.ids.map((id) =>
									specs.find((spec) => spec.id === id)).filter(
									(spec): spec is MarkSpec => !!spec).map((spec) => {
									spec.uploading.position = Math.min(move.position,
										transaction.doc.content.size);
									return mark(spec.uploading);
								}));
							}
						}
						return next;
					}
				},
				props: {
					decorations: (state) => KEY.getState(state),
					handleDrop: (view, event, _slice, moved) => {
						const files = filesOf((event as DragEvent).dataTransfer);
						if (moved || !files.length || !host.enabled()) {
							return false;
						}
						event.preventDefault();
						const drag = event as DragEvent;
						const at = view.posAtCoords({ left: drag.clientX, top: drag.clientY });
						void uploadFiles(host, files, at ? at.pos : view.state.selection.from);
						return true;
					},
					handlePaste: (view, event) => {
						const data = event.clipboardData;
						const files = filesOf(data);
						if (!files.length || !host.enabled() || data!.types.includes('text/html')) {
							return false;
						}
						event.preventDefault();
						const tr = view.state.tr.deleteSelection();
						view.dispatch(tr);
						void uploadFiles(host, files, view.state.selection.from);
						return true;
					}
				}
			})];
		}
	});
}
