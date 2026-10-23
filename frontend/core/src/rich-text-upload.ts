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

/*
 * The images a user uploads into the rich text editor: chosen in the form of the image, dropped
 * into the text or pasted. The page uploads them (the option onUpload): it keeps the file and
 * answers the address it is served at, which goes through isSafeUrl as any other, and the server's
 * cleaning; the editor never writes a data: address. uploadImageTo() posts a file to a URL and
 * reads the address from the answer, as Wicket's RichTextEditor answers.
 *
 * Before uploading, the editor checks the type of the file and its size against the page's limits
 * (uploadTypes, uploadMaxSize): the server checks them again, the browser's word is not to be
 * trusted. While a file uploads, a mark stands where its image goes; the image takes its place
 * once the page answered, or a message says why it did not.
 */

import { Extension, type Editor } from '@tiptap/core';
import { Plugin, PluginKey, type EditorState } from '@tiptap/pm/state';
import { Decoration, DecorationSet, type EditorView } from '@tiptap/pm/view';

/** Uploads an image, answering the address it is served at. */
export type RichTextUploader = (file: File) => Promise<string>;

/** The types of the images a user may upload unless the page says otherwise. */
export const DEFAULT_RICH_TEXT_UPLOAD_TYPES: readonly string[] = Object.freeze(['image/png',
	'image/jpeg', 'image/gif', 'image/webp']);

/** What the types are called in the messages. */
const TYPE_NAMES: ReadonlyMap<string, string> = new Map([['image/png', 'PNG'],
	['image/jpeg', 'JPEG'], ['image/gif', 'GIF'], ['image/webp', 'WebP'], ['image/avif', 'AVIF'],
	['image/svg+xml', 'SVG']]);

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

/** What the uploads of an editor ask of it. */
export interface UploadHost {
	/** The editor of Tiptap, whose text gets the images. */
	editor(): Editor;
	/** Whether the user may upload images now: the page uploads them, and the text is not read only. */
	enabled(): boolean;
	/** The types of the images the user may upload. */
	types(): readonly string[];
	/**
	 * Checks a file and uploads it.
	 *
	 * @returns the address of the image; rejected with the message saying why not
	 */
	upload(file: File): Promise<string>;
	/** @returns why a file cannot be uploaded, before it is sent; null when it can */
	refusal(file: File): string | null;
	/** @returns the text of the mark standing where the image of a file goes */
	uploading(file: File): string;
	/** Tells the user something went wrong. */
	alert(message: string): void;
}

/** A file uploading: the mark of where its image goes. */
interface Uploading {
	id: object;
	position: number;
	text: string;
}

type UploadMeta = { add: Uploading } | { remove: object };

const KEY = new PluginKey<DecorationSet>('antiliaRichTextUploads');

/**
 * @returns where the mark of a file uploading stands, null when it is gone
 */
function markOf(state: EditorState, id: object): number | null {
	const found = KEY.getState(state)?.find(undefined, undefined, (spec) => spec.id === id);
	return found?.length ? found[0]!.from : null;
}

/**
 * @returns the mark of a file uploading
 */
function mark(uploading: Uploading): Decoration {
	return Decoration.widget(uploading.position, () => {
		const element = document.createElement('span');
		element.className = 'antilia-rich-text-uploading';
		element.setAttribute('role', 'status');
		element.textContent = uploading.text;
		return element;
	}, { id: uploading.id, side: -1 });
}

/**
 * Uploads files, each image taking the place of its mark once the page answered.
 *
 * @param position where the images go
 */
export function uploadFiles(host: UploadHost, view: EditorView, files: readonly File[],
	position: number): void {
	for (const file of files) {
		const refused = host.refusal(file);
		if (refused) {
			host.alert(refused);
			continue;
		}
		const id = {};
		view.dispatch(view.state.tr.setMeta(KEY, { add: { id, position, text: host.uploading(file) } })
			.setMeta('addToHistory', false));
		host.upload(file).then((src) => {
			const editor = host.editor();
			if (editor.isDestroyed) {
				return;
			}
			const at = markOf(editor.state, id);
			if (at === null) {
				return;
			}
			editor.chain().command(({ tr }) => {
				tr.setMeta(KEY, { remove: id });
				return true;
			}).insertContentAt(at, { type: 'image', attrs: { src, alt: null } }).run();
		}, (error: unknown) => {
			const editor = host.editor();
			if (!editor.isDestroyed) {
				editor.view.dispatch(editor.state.tr.setMeta(KEY, { remove: id })
					.setMeta('addToHistory', false));
			}
			host.alert(error instanceof Error ? error.message : String(error));
		});
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
						uploadFiles(host, view, files, at ? at.pos : view.state.selection.from);
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
						uploadFiles(host, view, files, view.state.selection.from);
						return true;
					}
				}
			})];
		}
	});
}
