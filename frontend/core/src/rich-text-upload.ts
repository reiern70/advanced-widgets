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
 * trusted. While a file uploads, a mark stands where its image goes; the images take their places
 * once the page answered, in the order of the files, or a message says why one did not.
 *
 * deleteImagesAt() asks Wicket's RichTextImageUploadBehavior to delete images it took, those the
 * user took out of the text (onImagesRemoved).
 */

import { Extension, type Editor } from '@tiptap/core';
import { Plugin, PluginKey, type EditorState, type Transaction } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

import { dimensionsText, fits, readImageDimensions, scaledToFit,
	type RichTextImageDimensions } from './rich-text-image-headers.js';

/** Uploads an image, answering the address it is served at. */
export type RichTextUploader = (file: File) => Promise<string>;

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
 * @returns an uploader posting a file to a URL, in a form under a name, and reading the address
 *          from the JSON of the answer, {"url": "..."}; {"error": "..."} says why the server
 *          refused it, which the promise is rejected with
 */
export function uploadImageTo(url: string, name = 'file'): RichTextUploader {
	return async (file) => {
		const body = new FormData();
		body.append(name, file, file.name);
		const response = await fetch(url, { method: 'POST', body, credentials: 'same-origin',
			headers: { Accept: 'application/json' } });
		let answer: { url?: unknown; error?: unknown } = {};
		try {
			answer = await response.json() as typeof answer;
		} catch {
			// no JSON: the server failed
		}
		if (typeof answer.error === 'string' && answer.error) {
			throw new Error(answer.error);
		}
		if (!response.ok || typeof answer.url !== 'string') {
			throw new Error('');
		}
		return answer.url;
	};
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
	 * @returns the address of the image; rejected with the message saying why not
	 */
	upload(file: File): Promise<string>;
	/** @returns the text of the mark standing where the image of a file goes */
	uploading(file: File): string;
	/** Tells the user something went wrong. */
	alert(message: string): void;
	/** Tells the page images were uploaded that never went into the text, or no longer are. */
	discard(urls: readonly string[]): void;
}

/** A file uploading: the mark of where its image goes. */
interface Uploading {
	id: object;
	position: number;
	text: string;
}

/**
 * What a transaction does to the marks: adds one, or takes one away, maybe moving others, those
 * of the files after it, to where its image ends.
 */
type UploadMeta = { add: Uploading } |
	{ remove: object; move?: { ids: readonly object[]; position: number } };

const KEY = new PluginKey<DecorationSet>('antiliaRichTextUploads');

/**
 * @returns where the mark of a file uploading stands, null when it is gone
 */
function markOf(state: EditorState, id: object): number | null {
	const found = KEY.getState(state)?.find(undefined, undefined, (spec) => spec.id === id);
	return found?.length ? found[0]!.from : null;
}

/**
 * @returns the mark of a file uploading, before what goes in where it stands
 */
function mark(uploading: Uploading): Decoration {
	return Decoration.widget(uploading.position, () => {
		const element = document.createElement('span');
		element.className = 'antilia-rich-text-uploading';
		element.setAttribute('role', 'status');
		element.textContent = uploading.text;
		return element;
	}, { id: uploading.id, text: uploading.text, side: -1 });
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
 * changes meanwhile; those of the files the check leaves out go.
 *
 * @param position where the images go
 */
export async function uploadFiles(host: UploadHost, files: readonly File[],
	position: number): Promise<void> {
	const start = host.editor();
	const at = Math.min(Math.max(position, 0), start.state.doc.content.size);
	const ids = new Map<File, object>();
	for (const file of files) {
		const id = {};
		ids.set(file, id);
		start.view.dispatch(start.state.tr.setMeta(KEY, { add: { id, position: at,
			text: host.uploading(file) } }).setMeta('addToHistory', false));
	}
	const unmark = (id: object) => {
		const current = host.editor();
		if (!current.isDestroyed) {
			current.view.dispatch(current.state.tr.setMeta(KEY, { remove: id })
				.setMeta('addToHistory', false));
		}
	};
	let check: UploadCheck;
	try {
		check = await host.check(files);
	} catch {
		check = { files: [], from: [], refused: [] };
	}
	const { files: checked, from, refused } = check;
	ids.forEach((id, file) => {
		if (!from.includes(file)) {
			unmark(id);
		}
	});
	if (refused.length) {
		host.alert(refused.join(' '));
	}
	// the image before goes in first, or fails, so that the images keep their order
	let before: Promise<unknown> = Promise.resolve();
	checked.forEach((file, index) => {
		const id = ids.get(from[index]!)!;
		const uploaded = host.editor().isDestroyed ? Promise.reject(new Error('')) :
			host.upload(file);
		// failing before the image before it is in: told once that one is
		uploaded.catch(() => undefined);
		// the marks of the files after it, which go after its image
		const later = checked.slice(index + 1).map((_each, next) => ids.get(from[index + 1 + next]!)!);
		before = before.then(() => uploaded).then((src) => {
			const current = host.editor();
			const where = current.isDestroyed ? null : markOf(current.state, id);
			if (where === null) {
				// the editor went, or the text where the image went: the image is not wanted
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
		}, (error: unknown) => {
			unmark(id);
			const message = error instanceof Error ? error.message : String(error);
			if (message) {
				host.alert(message);
			}
		});
	});
	await before;
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
								const specs = moved.map((each) => each.spec as Uploading);
								next = next.remove(moved);
								next = next.add(transaction.doc, move.ids.map((id) =>
									specs.find((spec) => spec.id === id)).filter(
									(spec): spec is Uploading => !!spec).map((spec) =>
									mark({ id: spec.id, text: spec.text,
										position: Math.min(move.position,
											transaction.doc.content.size) })));
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
