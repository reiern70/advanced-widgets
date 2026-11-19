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
 * The files a user attaches to a text, PDFs or spreadsheets, each a link of its own in the text,
 * the node attachment:
 *
 *   <a class="antilia-attachment antilia-attachment-pdf" href="..." data-size="1.2 MB"
 *     target="_blank" rel="noopener noreferrer nofollow">report.pdf</a>
 *
 * The class of its kind, the extension of its format, draws its icon, and the style sheet shows its
 * size after its name: no style attribute, which a strict Content Security Policy blocks. In
 * Markdown a file is a plain link, [report.pdf](url), which comes back as a link, unless the page
 * tells the addresses of its files (the option attachmentUrls): a link to one of them, a text of
 * its own, comes back as a file, its size unknown.
 *
 * The page uploads the files (the option onAttach, an uploader as onUpload is for the images,
 * uploadFileTo() posting to a URL), within its limits: the types (attachTypes, each a media type,
 * an extension and a label), the size (attachMaxSize) and how many one drop or choice takes
 * (attachMaxCount). The browser tells a type by the media type of the file or its extension, which
 * a browser often leaves out; the server tells it again by the bytes.
 */

import { Node, type Editor, type JSONContent } from '@tiptap/core';
import type { Node as ProseMirrorNode } from '@tiptap/pm/model';

import { getHost } from './host.js';
import { isSafeUrl } from './rich-text-html.js';
import { fileNameOf, sizeText, type UploadCheck } from './rich-text-upload.js';

/** A type of the files a user may attach: its media type, its extension and its label. */
export interface RichTextAttachmentType {
	/** The media type, application/pdf for example. */
	type: string;
	/** The extension, without the dot: pdf. */
	extension: string;
	/** What the messages call it: PDF. */
	label: string;
}

/** The class of the link of a file attached. */
export const ATTACHMENT_CLASS = 'antilia-attachment';

/** The types of the files a user may attach unless the page says otherwise, those of the server. */
export const DEFAULT_RICH_TEXT_ATTACH_TYPES: readonly RichTextAttachmentType[] = Object.freeze([
	{ type: 'application/pdf', extension: 'pdf', label: 'PDF' },
	{ type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
		extension: 'docx', label: 'DOCX' },
	{ type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
		extension: 'xlsx', label: 'XLSX' },
	{ type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
		extension: 'pptx', label: 'PPTX' },
	{ type: 'application/vnd.oasis.opendocument.text', extension: 'odt', label: 'ODT' },
	{ type: 'application/vnd.oasis.opendocument.spreadsheet', extension: 'ods', label: 'ODS' },
	{ type: 'application/zip', extension: 'zip', label: 'ZIP' },
	{ type: 'text/plain', extension: 'txt', label: 'TXT' },
	{ type: 'text/csv', extension: 'csv', label: 'CSV' },
	{ type: 'text/markdown', extension: 'md', label: 'Markdown' }
].map((each) => Object.freeze(each)));

/** What the kind of a file may be, as its class holds it. */
const KIND = /^[a-z0-9]{1,10}$/;

/** The size of a file, as the link says it. */
const SIZE = /^[0-9]{1,4}(?:\.[0-9])? (?:B|KB|MB|GB)$/;

/**
 * @returns the types given, checked: each with a media type and an extension, lower case
 */
export function attachmentTypes(types: readonly RichTextAttachmentType[] | null | undefined):
	RichTextAttachmentType[] {
	return (types ?? DEFAULT_RICH_TEXT_ATTACH_TYPES).filter((each) => each &&
		typeof each.type === 'string' && typeof each.extension === 'string')
		.map((each) => ({ type: each.type.trim().toLowerCase(),
			extension: each.extension.trim().toLowerCase().replace(/^\./, ''),
			label: String(each.label ?? each.extension).trim() }))
		.filter((each) => each.type && each.extension);
}

/**
 * @returns the extension of the name of a file, lower case, without the dot; empty for none
 */
export function extensionOf(name: string): string {
	const match = /\.([^./\\]+)$/.exec(name ?? '');
	return match ? match[1]!.toLowerCase() : '';
}

/**
 * @returns the type a file is of, by its media type or its extension: of those its media type
 *          names, the one its extension names, else the first; null for none
 */
export function attachmentTypeOf(file: File, types: readonly RichTextAttachmentType[]):
	RichTextAttachmentType | null {
	const extension = extensionOf(file.name);
	const media = (file.type ?? '').toLowerCase();
	return types.find((each) => each.extension === extension) ??
		types.find((each) => media && each.type === media) ?? null;
}

/**
 * @returns the kind of a file as its class says it, the extension of its type: pdf
 */
export function attachmentKind(file: File, types: readonly RichTextAttachmentType[]): string {
	const kind = attachmentTypeOf(file, types)?.extension ?? extensionOf(file.name);
	return KIND.test(kind) ? kind : 'file';
}

/**
 * The addresses of the files attached to a text, which a link to in Markdown is one of: a list of
 * them, or a test of an address.
 */
export type RichTextAttachmentUrls = readonly string[] | ((href: string) => boolean);

/**
 * @returns a test of the addresses of the files attached, as given; null for none
 */
export function attachmentUrlTest(urls: RichTextAttachmentUrls | null | undefined):
	((href: string) => boolean) | null {
	if (typeof urls === 'function') {
		return (href) => {
			try {
				return !!urls(href);
			} catch (error) {
				getHost().log.error('The test of the addresses of the files attached failed', error);
				return false;
			}
		};
	}
	if (!Array.isArray(urls) || !urls.length) {
		return null;
	}
	const known = new Set(urls.filter((each) => typeof each === 'string'));
	return (href) => known.has(href);
}

/**
 * @returns the kind of a file attached known by its link alone: the extension of its name, else
 *          of its address, of the types given or not; null for none a class may hold
 */
function kindOfLink(name: string, href: string, types: readonly RichTextAttachmentType[]):
	string | null {
	for (const extension of [extensionOf(name), extensionOf(href.split(/[?#]/)[0] ?? '')]) {
		const known = types.find((each) => each.extension === extension)?.extension ?? extension;
		if (known && KIND.test(known)) {
			return known;
		}
	}
	return null;
}

/**
 * Turns the links to files attached back into files attached, in Tiptap's JSON of a text: in
 * Markdown a file is a plain link, [report.pdf](url), which comes back as a link. A link is a file
 * when its address is safe and one of the files' (isAttachment), and it is a text of its own, with
 * no other mark: its text is the file's name, the extension its kind; its size is unknown.
 *
 * @returns the JSON, a copy of what changed, the same object when nothing did
 */
export function linksToAttachments(json: JSONContent, isAttachment: (href: string) => boolean,
	types: readonly RichTextAttachmentType[]): JSONContent {
	const content = json.content;
	if (!Array.isArray(content)) {
		return json;
	}
	let changed = false;
	const next = content.map((child): JSONContent => {
		if (child.type === 'text') {
			const marks = child.marks ?? [];
			const href = marks.length === 1 && marks[0]!.type === 'link' ?
				String(marks[0]!.attrs?.href ?? '') : '';
			if (!href || !isSafeUrl(href) || !isAttachment(href)) {
				return child;
			}
			changed = true;
			const name = (child.text ?? '').trim() || href;
			return { type: 'attachment',
				attrs: { href, name, size: null, kind: kindOfLink(name, href, types) } };
		}
		const converted = linksToAttachments(child, isAttachment, types);
		changed ||= converted !== child;
		return converted;
	});
	return changed ? { ...json, content: next } : json;
}

/** The limits of the files attached, as the page gives them. */
export interface AttachmentLimits {
	types: readonly RichTextAttachmentType[];
	/** The largest file, in bytes; none for null. */
	maxSize: number | null;
	/** How many files one drop or choice takes; none for null. */
	maxCount: number | null;
}

/** What the messages of the checks of the files attached say. */
export interface AttachmentCheckTexts {
	theFile: string;
	attachmentType: string;
	uploadTooLarge: string;
	attachTooMany: string;
}

function fill(text: string, ...values: (string | number)[]): string {
	return text.replace(/\{(\d)\}/g, (all, index: string) => {
		const value = values[Number(index)];
		return value === undefined ? all : String(value);
	});
}

/**
 * @returns why a file cannot be attached: not of the types taken, or too large; null when it can
 */
export function attachmentRefusal(file: File, limits: Pick<AttachmentLimits, 'types' | 'maxSize'>,
	texts: AttachmentCheckTexts): string | null {
	const name = fileNameOf(file, { theImage: texts.theFile });
	if (!attachmentTypeOf(file, limits.types)) {
		return fill(texts.attachmentType, name, limits.types.map((each) => each.label)
			.filter((label, index, all) => all.indexOf(label) === index).join(', '));
	}
	if (limits.maxSize != null && file.size > limits.maxSize) {
		return fill(texts.uploadTooLarge, name, sizeText(limits.maxSize));
	}
	return null;
}

/**
 * Checks files against the limits of the files attached: how many, and the type and the size of
 * each.
 *
 * @returns the files to upload, and why the others are not
 */
export function checkAttachments(files: readonly File[], limits: AttachmentLimits,
	texts: AttachmentCheckTexts): UploadCheck {
	const refused: string[] = [];
	let taken = files;
	if (limits.maxCount != null && files.length > limits.maxCount) {
		taken = files.slice(0, limits.maxCount);
		refused.push(fill(texts.attachTooMany, limits.maxCount, files.length - limits.maxCount));
	}
	const accepted: File[] = [];
	for (const file of taken) {
		const refusal = attachmentRefusal(file, limits, texts);
		if (refusal) {
			refused.push(refusal);
		} else {
			accepted.push(file);
		}
	}
	return { files: accepted, from: accepted, refused };
}

/**
 * @returns what Tiptap inserts for a file attached, at an address
 */
export function attachmentContent(href: string, file: File,
	types: readonly RichTextAttachmentType[]): { type: 'attachment'; attrs: AttachmentAttributes } {
	return { type: 'attachment', attrs: { href, name: file.name || 'file',
		size: sizeText(file.size), kind: attachmentKind(file, types) } };
}

/** What a file attached keeps. */
export interface AttachmentAttributes {
	href: string;
	/** The name of the file, which the link shows. */
	name: string;
	/** Its size, as the link says it: 1.2 MB; null for none. */
	size: string | null;
	/** The extension of its type, which its class holds. */
	kind: string | null;
}

/**
 * @returns the kind of a link of a file attached, by its class
 */
function kindOf(element: HTMLElement): string | null {
	for (const each of Array.from(element.classList)) {
		if (each.startsWith(ATTACHMENT_CLASS + '-')) {
			const kind = each.slice(ATTACHMENT_CLASS.length + 1);
			return KIND.test(kind) ? kind : null;
		}
	}
	return null;
}

/**
 * @returns a text as Markdown writes it in the brackets of a link: its brackets and backslashes
 *          escaped
 */
function markdownText(text: string): string {
	return text.replace(/[\\[\]]/g, (char) => '\\' + char).replace(/\n/g, ' ');
}

/** A file attached: a link of its own, inline, taken out or moved whole. */
export const Attachment = Node.create({
	name: 'attachment',
	inline: true,
	group: 'inline',
	atom: true,
	selectable: true,
	draggable: true,

	addAttributes() {
		return {
			href: { default: null },
			name: { default: '' },
			size: { default: null },
			kind: { default: null }
		};
	},

	parseHTML() {
		return [{
			tag: 'a.' + ATTACHMENT_CLASS + '[href]',
			// before the link, a mark of the same element
			priority: 100,
			getAttrs: (element: HTMLElement) => {
				const href = element.getAttribute('href');
				if (!href || !isSafeUrl(href)) {
					return false;
				}
				const size = element.getAttribute('data-size');
				return { href, name: (element.textContent ?? '').trim() || href,
					size: size && SIZE.test(size) ? size : null, kind: kindOf(element) };
			}
		}];
	},

	renderHTML({ node }) {
		const attrs = node.attrs as AttachmentAttributes;
		const kind = attrs.kind && KIND.test(attrs.kind) ? attrs.kind : null;
		// in the order the cleaning of the server writes them
		const html: Record<string, string> = {
			class: kind ? ATTACHMENT_CLASS + ' ' + ATTACHMENT_CLASS + '-' + kind : ATTACHMENT_CLASS,
			href: attrs.href,
			target: '_blank'
		};
		if (attrs.size && SIZE.test(attrs.size)) {
			html['data-size'] = attrs.size;
		}
		html.rel = 'noopener noreferrer nofollow';
		return ['a', html, attrs.name || attrs.href];
	},

	renderText({ node }) {
		return String((node.attrs as AttachmentAttributes).name ?? '');
	},

	renderMarkdown: (node: { attrs?: Partial<AttachmentAttributes> }) => {
		const href = node.attrs?.href ?? '';
		return href ? '[' + markdownText(node.attrs?.name || href) + '](' + href + ')' : '';
	}
});

/**
 * @returns the addresses of the files attached to a text, each with how many times it is in it
 */
export function attachmentsIn(doc: ProseMirrorNode): Map<string, number> {
	const found = new Map<string, number>();
	doc.descendants((node) => {
		if (node.type.name === 'attachment') {
			const href = String(node.attrs.href ?? '');
			if (href) {
				found.set(href, (found.get(href) ?? 0) + 1);
			}
		}
		return true;
	});
	return found;
}

/**
 * @returns the files attached to the text of an editor, each once, in their order: its address,
 *          its name and its size
 */
export function attachmentsOf(editor: Editor): AttachmentAttributes[] {
	const found: AttachmentAttributes[] = [];
	const seen = new Set<string>();
	editor.state.doc.descendants((node) => {
		if (node.type.name === 'attachment') {
			const attrs = node.attrs as AttachmentAttributes;
			if (attrs.href && !seen.has(attrs.href)) {
				seen.add(attrs.href);
				found.push({ ...attrs });
			}
		}
		return true;
	});
	return found;
}

/**
 * Takes a file out of the text of an editor, every place it is in.
 */
export function removeAttachment(editor: Editor, href: string): void {
	const ranges: { from: number; to: number }[] = [];
	editor.state.doc.descendants((node, pos) => {
		if (node.type.name === 'attachment' && node.attrs.href === href) {
			ranges.push({ from: pos, to: pos + node.nodeSize });
		}
		return true;
	});
	if (!ranges.length) {
		return;
	}
	editor.chain().focus().command(({ tr }) => {
		ranges.reverse().forEach(({ from, to }) => tr.delete(from, to));
		return true;
	}).run();
}
