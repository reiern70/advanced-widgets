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
 * The HTML of the rich text editors, Tiptap's and CKEditor's, without either: how text aligns, by
 * classes, which addresses links and images may go to, the HTML a page gives or the user pastes
 * made quiet under a Content Security Policy, its style attributes moved aside, and what a paste
 * lost to the editor, which the editor tells the page and, by a notice, the user. That is a
 * convenience: the server cleans what the browser sends, which it never trusts.
 */

/** How text aligns; left, the start, is the default. */
export type RichTextAlignment = 'left' | 'center' | 'right' | 'justify';

export const RICH_TEXT_ALIGNMENTS: readonly RichTextAlignment[] = Object.freeze(['left', 'center',
	'right', 'justify']);

/** The classes aligning text in the HTML of the editor: none for left, the default. */
export const RICH_TEXT_ALIGN_CLASSES: Readonly<Record<Exclude<RichTextAlignment, 'left'>, string>> =
	Object.freeze({
		center: 'antilia-text-center',
		right: 'antilia-text-right',
		justify: 'antilia-text-justify'
	});

/**
 * @returns whether a link or an image may go to an address: http, https, mailto or a relative
 *          one; not javascript:, data: and so on. Spaces and control characters the browser
 *          ignores do not hide a scheme.
 */
export function isSafeUrl(url: string | null | undefined): boolean {
	if (url == null) {
		return false;
	}
	const bare = url.replace(/[\u0000-\u0020\u007f-\u00a0\u1680\u180e\u2000-\u2029\u205f\u3000\ufeff]/g,
		'');
	if (!bare) {
		return false;
	}
	const scheme = /^([a-z][a-z0-9+.-]*):/i.exec(bare);
	return !scheme || ['http', 'https', 'mailto'].includes(scheme[1]!.toLowerCase());
}

/**
 * The attribute the style of pasted HTML moves to (see quietHtml), which the editor reads the
 * alignment of a paragraph from.
 */
export const PASTED_STYLE_ATTRIBUTE = 'data-antilia-style';

/** A style attribute in a tag, not yet moved: after the whole attributes before it, quoted or not. */
const STYLE_IN_TAG = /(<[a-zA-Z][^\s>/]*(?:\s+[^\s"'>/=]+(?:\s*=\s*(?:"[^"]*"|'[^']*'|[^\s"'>]+))?)*?\s+)style(?=\s*=)/;

/**
 * @returns HTML the editor reads without a style attribute: each moves to PASTED_STYLE_ATTRIBUTE.
 *          Under a Content Security Policy without 'unsafe-inline', the browser reports every
 *          style attribute it parses, even out of the page, and reads none: pasted from another
 *          page, the HTML would fill the console. What the styles say the editor has a mark or an
 *          alignment for comes along: bold, italic, underlined and struck text, superscript and
 *          subscript, as the spans of Google Docs have it, and alignment; the bold wrapper of all
 *          Google Docs pastes, a b of a normal weight, goes.
 */
export function quietHtml(html: string): string {
	if (!STYLE_IN_TAG.test(html) || typeof document === 'undefined') {
		return html;
	}
	let quiet = html;
	while (STYLE_IN_TAG.test(quiet)) {
		quiet = quiet.replace(STYLE_IN_TAG, '$1' + PASTED_STYLE_ATTRIBUTE);
	}
	const template = document.createElement('template');
	template.innerHTML = quiet;
	template.content.querySelectorAll<HTMLElement>(`[${PASTED_STYLE_ATTRIBUTE}]`).forEach(
		(element) => {
			const style = element.getAttribute(PASTED_STYLE_ATTRIBUTE) ?? '';
			const weight = /font-weight\s*:\s*([a-z0-9]+)/i.exec(style)?.[1]?.toLowerCase();
			if (element.tagName === 'B' && (weight === 'normal' || weight === '400')) {
				element.replaceWith(...Array.from(element.childNodes));
				return;
			}
			const wrap = (tag: string) => {
				const inner = document.createElement(tag);
				inner.append(...Array.from(element.childNodes));
				element.append(inner);
			};
			const inline = !/^(P|H[1-6]|LI|UL|OL|TD|TH|TABLE|TR|DIV|BLOCKQUOTE|PRE)$/
				.test(element.tagName);
			if (inline && weight && /^(bold|bolder|[6-9]00)$/.test(weight)) {
				wrap('strong');
			}
			if (inline && /font-style\s*:\s*italic/i.test(style)) {
				wrap('em');
			}
			if (inline && /text-decoration[a-z-]*\s*:[^;]*underline/i.test(style)) {
				wrap('u');
			}
			if (inline && /text-decoration[a-z-]*\s*:[^;]*line-through/i.test(style)) {
				wrap('s');
			}
			const raised = /vertical-align\s*:\s*(super|sub)\b/i.exec(style)?.[1]?.toLowerCase();
			if (inline && raised) {
				wrap(raised === 'super' ? 'sup' : 'sub');
			}
		});
	return template.innerHTML;
}

/**
 * What the cleaning of a paste left out, by name and count: the elements, by their names in small
 * letters, and the attributes, the handlers of events alone. Never what they held.
 */
export interface RichTextCleaningReport {
	elements: Record<string, number>;
	attributes: Record<string, number>;
}

/** The elements no feature writes, which a paste loses: they run, embed or ask for something. */
const NEVER_KEPT: readonly string[] = ['script', 'iframe', 'frame', 'frameset', 'object', 'embed',
	'applet', 'form', 'input', 'button', 'select', 'textarea', 'video', 'audio', 'canvas', 'svg',
	'math'];

/** The blocks and the nodes of the features, which the editors keep one for one. */
const NODES: readonly string[] = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'pre', 'hr',
	'table', 'img', 'ul', 'ol'];

/**
 * The marks of text, which the editors join and order as they write them: two bold words become
 * one, so only a mark kept nowhere was lost.
 */
const MARKS: readonly string[] = ['strong', 'em', 'u', 's', 'code', 'sup', 'sub', 'mark'];

/** The names a paste may have for a mark, by the name the editors write. */
const SAME_AS: Readonly<Record<string, string>> = Object.freeze({ b: 'strong', i: 'em',
	strike: 's', del: 's' });

interface Counted {
	elements: Map<string, number>;
	attributes: Map<string, number>;
	/** The links to addresses the editors take, and to those they do not. */
	safeLinks: number;
	unsafeLinks: number;
}

/**
 * @returns the elements, the handlers of events and the links of some HTML, counted
 */
function counted(html: string): Counted {
	const template = document.createElement('template');
	template.innerHTML = html;
	const found: Counted = { elements: new Map(), attributes: new Map(), safeLinks: 0,
		unsafeLinks: 0 };
	const add = (map: Map<string, number>, name: string) => map.set(name, (map.get(name) ?? 0) + 1);
	template.content.querySelectorAll('*').forEach((element) => {
		const tag = element.localName.toLowerCase();
		const name = SAME_AS[tag] ?? tag;
		if (name === 'a' && element.hasAttribute('href')) {
			if (isSafeUrl(element.getAttribute('href'))) {
				found.safeLinks++;
			} else {
				found.unsafeLinks++;
			}
		} else if (name !== 'code' || !element.closest('pre')) {
			add(found.elements, name);
		}
		for (const attribute of Array.from(element.attributes)) {
			const attributeName = attribute.name.toLowerCase();
			if (attributeName.startsWith('on')) {
				add(found.attributes, attributeName);
			}
		}
	});
	return found;
}

/**
 * @param pasted the HTML of a paste, made quiet (see quietHtml), as the editor read it
 * @param kept the HTML of what the editor kept of it, as it writes it
 * @returns what the paste lost, compared to what was kept: the elements no feature writes, the
 *          blocks, nodes and marks of features the editor has not, the links and images to
 *          addresses it does not take, and the handlers of events. The rest of a paste, its spans,
 *          its divisions and the like of a word processor, is no loss: the editor keeps their
 *          text, and writes it as its own.
 */
export function pasteCleaningReport(pasted: string, kept: string): RichTextCleaningReport {
	const before = counted(pasted);
	const after = counted(kept);
	const report: RichTextCleaningReport = { elements: {}, attributes: {} };
	const lost = (map: Record<string, number>, name: string, count: number) => {
		if (count > 0) {
			map[name] = (map[name] ?? 0) + count;
		}
	};
	before.elements.forEach((count, name) => {
		if (NEVER_KEPT.includes(name) || NODES.includes(name)) {
			lost(report.elements, name, count - (after.elements.get(name) ?? 0));
		} else if (MARKS.includes(name) && !after.elements.has(name)) {
			lost(report.elements, name, count);
		}
	});
	// the links to addresses the editor does not take that it left out, and all of them when it
	// kept none: without the feature link
	lost(report.elements, 'a', Math.max(before.unsafeLinks - after.unsafeLinks, 0) +
		(after.safeLinks + after.unsafeLinks === 0 ? before.safeLinks : 0));
	before.attributes.forEach((count, name) =>
		lost(report.attributes, name, count - (after.attributes.get(name) ?? 0)));
	return report;
}

/**
 * @returns whether a report tells of nothing lost
 */
export function isEmptyCleaningReport(report: RichTextCleaningReport): boolean {
	return Object.keys(report.elements).length === 0 && Object.keys(report.attributes).length === 0;
}

/** What a paste may have lost, as the notice of the editor tells it, in this order. */
export type RichTextPasteLoss = 'scripts' | 'embedded' | 'forms' | 'images' | 'tables' | 'links' |
	'formatting';

export const RICH_TEXT_PASTE_LOSSES: readonly RichTextPasteLoss[] = Object.freeze(['scripts',
	'embedded', 'forms', 'images', 'tables', 'links', 'formatting']);

/** What each element lost is, by its name; formatting for the others. */
const LOSS_OF: Readonly<Record<string, RichTextPasteLoss>> = Object.freeze({
	script: 'scripts',
	iframe: 'embedded', frame: 'embedded', frameset: 'embedded', object: 'embedded',
	embed: 'embedded', applet: 'embedded', video: 'embedded', audio: 'embedded',
	canvas: 'embedded', svg: 'embedded', math: 'embedded',
	form: 'forms', input: 'forms', button: 'forms', select: 'forms', textarea: 'forms',
	img: 'images',
	table: 'tables',
	a: 'links'
});

/**
 * @returns what a paste lost, as the notice of the editor tells it: the handlers of events are
 *          scripts; each once, in the order of RICH_TEXT_PASTE_LOSSES
 */
export function pasteLosses(report: RichTextCleaningReport): RichTextPasteLoss[] {
	const losses = new Set<RichTextPasteLoss>(Object.keys(report.elements).map((name) =>
		LOSS_OF[name] ?? 'formatting'));
	if (Object.keys(report.attributes).length) {
		losses.add('scripts');
	}
	return RICH_TEXT_PASTE_LOSSES.filter((loss) => losses.has(loss));
}

/** What the notice of a paste that lost something says: the line, then what was lost. */
export interface RichTextPasteTexts {
	/** The line of the notice, {0} what was lost, as a list. */
	pasteCleaned: string;
	pasteScripts: string;
	pasteEmbedded: string;
	pasteForms: string;
	pasteImages: string;
	pasteTables: string;
	pasteLinks: string;
	pasteFormatting: string;
}

export const DEFAULT_RICH_TEXT_PASTE_TEXTS: Readonly<RichTextPasteTexts> = Object.freeze({
	pasteCleaned: 'Part of the paste was left out: {0}.',
	pasteScripts: 'scripts',
	pasteEmbedded: 'embedded content',
	pasteForms: 'form fields',
	pasteImages: 'images',
	pasteTables: 'tables',
	pasteLinks: 'links',
	pasteFormatting: 'formatting'
});

/** The text of each loss, by the loss. */
const LOSS_TEXTS: Readonly<Record<RichTextPasteLoss, keyof RichTextPasteTexts>> = Object.freeze({
	scripts: 'pasteScripts',
	embedded: 'pasteEmbedded',
	forms: 'pasteForms',
	images: 'pasteImages',
	tables: 'pasteTables',
	links: 'pasteLinks',
	formatting: 'pasteFormatting'
});

/**
 * @returns what the notice says of a paste that lost something: "Part of the paste was left out:
 *          scripts, images."; '' for one that lost nothing
 */
export function pasteNoticeText(report: RichTextCleaningReport,
	texts: Readonly<RichTextPasteTexts>): string {
	const losses = pasteLosses(report);
	if (!losses.length) {
		return '';
	}
	const list = losses.map((loss) => texts[LOSS_TEXTS[loss]]).join(', ');
	return texts.pasteCleaned.replace(/\{0\}/g, () => list);
}

/** The class of the notice telling the user what a paste lost. */
export const RICH_TEXT_NOTICE_CLASS = 'antilia-rich-text-notice';

/**
 * The notice of an editor telling the user what a paste lost, a polite line under its toolbar: a
 * live region, there from the start and empty, which screen readers read once it has a text. The
 * editor empties it at the next change of the text.
 */
export class RichTextPasteNotice {
	readonly element: HTMLElement;

	constructor() {
		this.element = document.createElement('div');
		this.element.className = RICH_TEXT_NOTICE_CLASS;
		this.element.setAttribute('role', 'status');
	}

	/**
	 * Says what a paste lost, in place of what it said; nothing for a paste that lost nothing.
	 */
	show(text: string): void {
		if (this.element.textContent !== text) {
			this.element.textContent = text;
		}
	}

	/** Says nothing, as the text changed. */
	clear(): void {
		this.show('');
	}

	/** @returns what the notice says, '' for nothing */
	text(): string {
		return this.element.textContent ?? '';
	}

	destroy(): void {
		this.element.remove();
	}
}
