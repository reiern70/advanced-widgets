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
 * classes, which addresses links and images may go to, and the HTML a page gives or the user
 * pastes made quiet under a Content Security Policy, its style attributes moved aside.
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
 *          alignment for comes along: bold, italic, underlined and struck text, as the spans of
 *          Google Docs have it, and alignment; the bold wrapper of all Google Docs pastes, a b of a
 *          normal weight, goes.
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
		});
	return template.innerHTML;
}
