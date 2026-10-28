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
 * Proofreading in the rich text editor. A proofreader the page gives (RichTextProofreader), such
 * as LanguageTool's (languageToolProofreader) or the server's (proofreadAt), finds the words
 * spelled wrong and the mistakes of grammar and of style, which the editor underlines: decorations,
 * never written into the text. A click on one, or Mod-. (the next one from the cursor), opens a
 * popover of what the proofreader says, its replacements, and ignoring it.
 *
 * The editor checks its text by its blocks, the paragraphs, headings, items and cells: those whose
 * text it has not checked yet, a moment after the user stopped typing, several in a request and
 * one request at a time. What a text was found to have is kept by its text, so that a block
 * changing back, or another of the same text, is not checked again; until its new text is
 * checked, a block keeps the issues whose words did not change. Code blocks and inline code are
 * not checked. Images and line breaks are one character each in the text a block is checked as,
 * so that the issues fall on the same places of the editor.
 *
 * The popover points at an element of its own over the issue, out of ProseMirror's, which would
 * draw its own again.
 */

import { Extension } from '@tiptap/core';
import type { Node as ProseMirrorNode } from '@tiptap/pm/model';
import { Plugin, PluginKey, TextSelection, type EditorState } from '@tiptap/pm/state';
import { Decoration, DecorationSet, type EditorView } from '@tiptap/pm/view';

import { lookClasses, type Look } from './look.js';
import { Popover } from './popover.js';
import type { RichTextEditorTexts } from './rich-text-toolbar.js';

/** What an issue is: a word spelled wrong, a mistake of grammar, or one of style. */
export type RichTextIssueKind = 'spelling' | 'grammar' | 'style';

export const RICH_TEXT_ISSUE_KINDS: readonly RichTextIssueKind[] =
	Object.freeze(['spelling', 'grammar', 'style']);

/** What a proofreader found in a text. */
export interface RichTextIssue {
	/** Where it starts and ends in the text, in its UTF-16 code units, as JavaScript counts them. */
	from: number;
	to: number;
	kind: RichTextIssueKind;
	/** What is wrong, as the proofreader says it. */
	message?: string;
	/** What may take its place, the best first. */
	replacements?: readonly string[];
	/** The rule that found it: ignoring an issue ignores those of its rule and its text. */
	rule?: string;
}

/** Checks texts: a service, the server, or a dictionary of the page's own. */
export interface RichTextProofreader {
	/**
	 * @param texts the texts of blocks of an editor, a paragraph each for example
	 * @param language the language of the texts, a tag of BCP 47; null for the proofreader to find
	 * @param signal aborted once the answer is no longer wanted
	 * @returns the issues of each text, in the order of the texts
	 */
	check(texts: readonly string[], language: string | null,
		signal: AbortSignal): Promise<readonly (readonly RichTextIssue[])[]>;
}

/** The classes of the issues in the text, and of their popover. */
export const RICH_TEXT_ISSUE_CLASSES = Object.freeze({
	/** Of the text of an issue, with antilia-rich-text-issue-<kind>. */
	issue: 'antilia-rich-text-issue',
	/** Of the element over an issue the popover points at. */
	anchor: 'antilia-rich-text-issue-anchor',
	popover: 'antilia-rich-text-issue-popover'
});

/** What the plugin of proofreading asks the editor. */
export interface ProofreadingOptions {
	/** The proofreader, null for none: the text is then not checked. */
	proofreader: () => RichTextProofreader | null;
	/** The language of the text, null for the proofreader to find. */
	language: () => string | null;
	/** How long after the user stopped typing the text is checked, in milliseconds. */
	delay: () => number;
	texts: () => RichTextEditorTexts;
	look: () => Look;
	/** Told when the popover of an issue opens and closes. */
	onPopover: (open: boolean) => void;
	log: (message: string, error: unknown) => void;
}

/** An issue in an editor: where it is in the document, its text, and what the proofreader said. */
export interface RichTextIssueFound {
	from: number;
	to: number;
	text: string;
	issue: RichTextIssue;
}

/** A block of text as it is checked: where its text starts, its text, and what is not checked. */
export interface ProofreadBlock {
	start: number;
	text: string;
	/** The parts of the text not checked, from and to: inline code, images, line breaks. */
	skipped: readonly (readonly [number, number])[];
}

const KEY = new PluginKey<DecorationSet>('antiliaProofreading');

/** The meta of a transaction drawing the issues again. */
const REDRAW = 'antilia-rich-text-proofreading:redraw';

/** The texts checked kept, at least, as long as the text has fewer blocks. */
const CACHED = 500;

/** The characters asked about in a request, at most, but for a block longer alone. */
const BATCH = 6000;

/** The replacements the popover offers, at most. */
const SHOWN = 6;

/** What is kept of an issue in its decoration. */
interface IssueSpec {
	issue: RichTextIssue;
	text: string;
}

/** What the editors do when asked to check again, by their views. */
const refreshers = new WeakMap<EditorView, () => void>();

/** What ignores an issue in an editor, by its view. */
const ignoring = new WeakMap<EditorView, (issue: RichTextIssue, text: string) => void>();

/** The popover of an issue open, and the view of its editor. */
let open: { popover: Popover; view: EditorView } | null = null;

/**
 * @returns the blocks of a document as they are checked, in their order: all but code blocks
 */
export function proofreadBlocks(doc: ProseMirrorNode): ProofreadBlock[] {
	const blocks: ProofreadBlock[] = [];
	doc.descendants((node, position) => {
		if (node.type.spec.code) {
			return false;
		}
		if (!node.isTextblock) {
			return true;
		}
		let text = '';
		const skipped: [number, number][] = [];
		node.forEach((child) => {
			const start = text.length;
			if (child.isText) {
				text += child.text ?? '';
				if (child.marks.some((mark) => mark.type.spec.code || mark.type.name === 'code')) {
					skipped.push([start, text.length]);
				}
			} else {
				// as many characters as the node takes places
				text += (child.type.name === 'hardBreak' ? '\n' : '￼')
					.padEnd(child.nodeSize, '￼');
				skipped.push([start, text.length]);
			}
		});
		blocks.push({ start: position + 1, text, skipped });
		return false;
	});
	return blocks;
}

/**
 * @returns the issues of a text as the editor takes them: within the text, of a kind known, the
 *          replacements texts; in their order in the text
 */
export function proofreadIssues(found: unknown, length: number): readonly RichTextIssue[] {
	if (!Array.isArray(found)) {
		return Object.freeze([]);
	}
	const issues: RichTextIssue[] = [];
	for (const item of found as unknown[]) {
		if (!item || typeof item !== 'object') {
			continue;
		}
		const { from, to, kind, message, replacements, rule } = item as Record<string, unknown>;
		if (!Number.isInteger(from) || !Number.isInteger(to) || (from as number) < 0 ||
			(to as number) <= (from as number) || (to as number) > length) {
			continue;
		}
		const issue: RichTextIssue = {
			from: from as number,
			to: to as number,
			kind: RICH_TEXT_ISSUE_KINDS.includes(kind as RichTextIssueKind) ?
				kind as RichTextIssueKind : 'grammar'
		};
		if (typeof message === 'string' && message.trim()) {
			issue.message = message.trim();
		}
		if (Array.isArray(replacements)) {
			const texts = [...new Set((replacements as unknown[]).filter((replacement):
				replacement is string => typeof replacement === 'string'))];
			if (texts.length) {
				issue.replacements = Object.freeze(texts);
			}
		}
		if (typeof rule === 'string' && rule) {
			issue.rule = rule;
		}
		issues.push(Object.freeze(issue));
	}
	return Object.freeze(issues.sort((one, other) => one.from - other.from || one.to - other.to));
}

/**
 * Checks the text of an editor again: after its proofreader, or the language of its text,
 * changed. A proofreader of another identity, or another language, forgets what was checked.
 */
export function refreshProofreading(view: EditorView): void {
	refreshers.get(view)?.();
}

/**
 * @returns the element of the popover of an issue open in an editor, if any
 */
export function issuePopoverOf(view: EditorView): HTMLElement | null {
	return open?.view === view && open.popover.isOpen() ? open.popover.element : null;
}

/**
 * @returns the issues the editor shows, in their order
 */
export function issuesOf(state: EditorState): RichTextIssueFound[] {
	const set = KEY.getState(state);
	return set ? set.find().map(foundOf).sort((one, other) => one.from - other.from) : [];
}

function foundOf(decoration: Decoration): RichTextIssueFound {
	const spec = decoration.spec as IssueSpec;
	return { from: decoration.from, to: decoration.to, text: spec.text, issue: spec.issue };
}

function keyOf(language: string | null, text: string): string {
	return (language ?? '') + '\n' + text;
}

function ignoredKey(issue: RichTextIssue, text: string): string {
	return (issue.rule ?? issue.kind) + '\u0000' + text;
}

/**
 * Underlines what a proofreader finds in the text, and offers its replacements.
 */
export const Proofreading = Extension.create<ProofreadingOptions>({
	name: 'antiliaProofreading',

	addOptions() {
		return {
			proofreader: () => null,
			language: () => null,
			delay: () => 600,
			texts: () => ({}) as RichTextEditorTexts,
			look: () => ({ name: 'antilia' }) as Look,
			onPopover: () => undefined,
			log: () => undefined
		};
	},

	addKeyboardShortcuts() {
		return {
			'Mod-.': () => openNext(this.editor.view, this.options)
		};
	},

	addProseMirrorPlugins() {
		const options = this.options;
		/** What the texts were found to have, by their language and text. */
		const cache = new Map<string, readonly RichTextIssue[]>();
		const ignored = new Set<string>();
		let checkedBy: RichTextProofreader | null = null;
		let checkedIn: string | null = null;
		let view: EditorView | null = null;
		let timer: ReturnType<typeof setTimeout> | null = null;
		let running: { controller: AbortController; keys: readonly string[] } | null = null;

		const stop = () => {
			if (timer !== null) {
				clearTimeout(timer);
				timer = null;
			}
			running?.controller.abort();
			running = null;
		};

		/**
		 * @returns the proofreader, and the language, of the editor now; what was checked by
		 *          another, or in another language, is forgotten
		 */
		const sync = (): RichTextProofreader | null => {
			const proofreader = options.proofreader();
			const language = proofreader ? options.language() : null;
			if (proofreader !== checkedBy || language !== checkedIn) {
				stop();
				cache.clear();
				checkedBy = proofreader;
				checkedIn = language;
			}
			return proofreader;
		};

		const redraw = () => {
			if (view && !view.isDestroyed) {
				view.dispatch(view.state.tr.setMeta(REDRAW, true).setMeta('addToHistory', false));
			}
		};

		const draw = (state: EditorState, before: DecorationSet | null): DecorationSet => {
			if (!sync()) {
				return DecorationSet.empty;
			}
			const decorations: Decoration[] = [];
			for (const block of proofreadBlocks(state.doc)) {
				const issues = cache.get(keyOf(checkedIn, block.text));
				if (issues) {
					for (const issue of issues) {
						if (block.skipped.some(([from, to]) => issue.from < to && issue.to > from)) {
							continue;
						}
						const text = block.text.slice(issue.from, issue.to);
						if (!ignored.has(ignoredKey(issue, text))) {
							decorations.push(decorationOf(block.start + issue.from,
								block.start + issue.to, { issue, text }));
						}
					}
				} else if (before) {
					// not checked yet: the issues whose words stayed as they were
					before.find(block.start, block.start + block.text.length).forEach((decoration) => {
						const spec = decoration.spec as IssueSpec;
						if (state.doc.textBetween(decoration.from, decoration.to) === spec.text &&
							!ignored.has(ignoredKey(spec.issue, spec.text))) {
							decorations.push(decoration);
						}
					});
				}
			}
			return DecorationSet.create(state.doc, decorations);
		};

		/** Forgets the texts no longer in the editor once too many are kept. */
		const forget = (present: ReadonlySet<string>) => {
			if (cache.size <= CACHED) {
				return;
			}
			for (const key of cache.keys()) {
				if (cache.size <= CACHED) {
					break;
				}
				if (!present.has(key)) {
					cache.delete(key);
				}
			}
		};

		const run = () => {
			timer = null;
			if (!view || view.isDestroyed) {
				return;
			}
			const proofreader = sync();
			if (!proofreader) {
				return;
			}
			const language = checkedIn;
			const blocks = proofreadBlocks(view.state.doc);
			const present = new Set(blocks.map((block) => keyOf(language, block.text)));
			if (running) {
				if (running.keys.some((key) => present.has(key))) {
					// its answer comes first, then the rest
					return;
				}
				running.controller.abort();
				running = null;
			}
			const texts: string[] = [];
			let size = 0;
			for (const block of blocks) {
				const key = keyOf(language, block.text);
				if (!block.text.trim() || cache.has(key) || texts.includes(block.text)) {
					continue;
				}
				if (texts.length > 0 && size + block.text.length > BATCH) {
					break;
				}
				texts.push(block.text);
				size += block.text.length;
			}
			if (!texts.length) {
				return;
			}
			const keys = texts.map((text) => keyOf(language, text));
			const request = { controller: new AbortController(), keys };
			running = request;
			let answer: Promise<readonly (readonly RichTextIssue[])[]>;
			try {
				answer = Promise.resolve(proofreader.check(texts, language, request.controller.signal));
			} catch (error) {
				answer = Promise.reject(error);
			}
			answer.then((found) => {
				if (running !== request) {
					return;
				}
				running = null;
				const answers: readonly unknown[] = Array.isArray(found) ? found : [];
				texts.forEach((text, index) =>
					cache.set(keys[index]!, proofreadIssues(answers[index], text.length)));
				forget(present);
				redraw();
				run();
			}, (error: unknown) => {
				if (running !== request) {
					return;
				}
				running = null;
				// checked again once the text changes
				if (!request.controller.signal.aborted) {
					options.log('The proofreader of the rich text editor failed', error);
				}
			});
		};

		const schedule = (delay: number) => {
			if (timer !== null) {
				clearTimeout(timer);
			}
			timer = setTimeout(run, delay);
		};

		const refresh = () => {
			if (open?.view === view && !options.proofreader()) {
				open.popover.close();
			}
			redraw();
			schedule(0);
		};

		const plugin: Plugin<DecorationSet> = new Plugin<DecorationSet>({
			key: KEY,
			state: {
				init: (_, state) => draw(state, null),
				apply: (transaction, decorations, _, state) =>
					transaction.docChanged || transaction.getMeta(REDRAW) ?
						draw(state, decorations.map(transaction.mapping, transaction.doc)) : decorations
			},
			props: {
				decorations: (state) => KEY.getState(state),
				handleClick: (editorView, position, event) => {
					const target = event.target instanceof Element ?
						event.target.closest('.' + RICH_TEXT_ISSUE_CLASSES.issue) : null;
					const found = target ? issuesOf(editorView.state).find((issue) =>
						issue.from <= position && position <= issue.to) : undefined;
					if (found) {
						// once the click is over, which would close it
						setTimeout(() => openIssue(editorView, found, options));
					}
					return false;
				}
			},
			view: (editorView) => {
				view = editorView;
				refreshers.set(editorView, refresh);
				ignoring.set(editorView, (issue, text) => {
					ignored.add(ignoredKey(issue, text));
					redraw();
				});
				schedule(options.delay());
				return {
					update: (updated, previous) => {
						if (updated.state.doc !== previous.doc) {
							schedule(options.delay());
						}
					},
					destroy: () => {
						stop();
						refreshers.delete(editorView);
						ignoring.delete(editorView);
						if (open?.view === editorView) {
							open.popover.close();
						}
						view = null;
					}
				};
			}
		});
		return [plugin];
	}
});

function decorationOf(from: number, to: number, spec: IssueSpec): Decoration {
	const kind = spec.issue.kind;
	const attributes: Record<string, string> = {
		class: RICH_TEXT_ISSUE_CLASSES.issue + ' ' + RICH_TEXT_ISSUE_CLASSES.issue + '-' + kind
	};
	if (kind !== 'style') {
		attributes['aria-invalid'] = kind;
	}
	return Decoration.inline(from, to, attributes, spec);
}

/**
 * Opens the popover of the issue at the cursor, or of the next one after it, the first one after
 * the last; the issue is selected.
 *
 * @returns whether there is an issue
 */
function openNext(view: EditorView, options: ProofreadingOptions): boolean {
	const issues = issuesOf(view.state);
	if (!issues.length) {
		return false;
	}
	const { from } = view.state.selection;
	const found = issues.find((issue) => issue.from <= from && from < issue.to) ??
		issues.find((issue) => issue.from > from) ?? issues[0]!;
	view.dispatch(view.state.tr.setSelection(TextSelection.create(view.state.doc, found.from,
		found.to)).scrollIntoView());
	openIssue(view, found, options);
	return true;
}

/**
 * @returns the element over an issue the popover points at, in the scroller of the text, made
 *          once
 */
function anchorOver(view: EditorView, found: RichTextIssueFound): HTMLElement | null {
	const container = view.dom.parentElement;
	if (!container) {
		return null;
	}
	let anchor = container.querySelector<HTMLElement>(':scope > .' +
		RICH_TEXT_ISSUE_CLASSES.anchor);
	if (!anchor) {
		anchor = document.createElement('span');
		anchor.className = RICH_TEXT_ISSUE_CLASSES.anchor;
		anchor.setAttribute('aria-hidden', 'true');
		container.append(anchor);
	}
	const box = container.getBoundingClientRect();
	let start: { left: number; right: number; top: number; bottom: number };
	let end: { left: number; right: number; top: number; bottom: number };
	try {
		start = view.coordsAtPos(found.from, 1);
		end = view.coordsAtPos(found.to, -1);
	} catch {
		start = end = box;
	}
	// the first line of the issue
	const right = end.top < start.bottom ? Math.max(end.right, start.left + 1) : start.left + 1;
	const style = anchor.style;
	style.setProperty('left', (start.left - box.left + container.scrollLeft) + 'px');
	style.setProperty('top', (start.top - box.top + container.scrollTop) + 'px');
	style.setProperty('width', (right - start.left) + 'px');
	style.setProperty('height', Math.max(1, start.bottom - start.top) + 'px');
	return anchor;
}

function openIssue(view: EditorView, found: RichTextIssueFound,
	options: ProofreadingOptions): void {
	if (view.isDestroyed) {
		return;
	}
	const anchor = anchorOver(view, found);
	if (!anchor) {
		return;
	}
	const texts = options.texts();
	const look = options.look();
	const issue = found.issue;
	const body = document.createElement('div');
	body.className = 'antilia-rich-text-issue-body';
	if (issue.message) {
		const message = document.createElement('p');
		message.className = 'antilia-rich-text-issue-message';
		message.textContent = issue.message;
		body.append(message);
	}
	let popover: Popover | null = null;
	let applied = false;
	const done = () => {
		applied = true;
		popover?.close();
		view.focus();
	};
	const replacements = (issue.replacements ?? []).slice(0, SHOWN);
	const buttons: HTMLButtonElement[] = [];
	if (replacements.length) {
		const group = document.createElement('div');
		group.className = 'antilia-rich-text-issue-replacements';
		group.setAttribute('role', 'group');
		group.setAttribute('aria-label', texts.proofreadReplacements);
		replacements.forEach((replacement) => {
			const button = document.createElement('button');
			button.type = 'button';
			button.className = lookClasses(look, 'button', 'button-small');
			// an empty replacement takes the text away
			button.textContent = replacement || '∅';
			button.addEventListener('click', () => {
				const doc = view.state.doc;
				if (found.to <= doc.content.size &&
					doc.textBetween(found.from, found.to) === found.text) {
					view.dispatch(view.state.tr.insertText(replacement, found.from, found.to)
						.scrollIntoView());
				}
				done();
			});
			buttons.push(button);
			group.append(button);
		});
		body.append(group);
	} else {
		const none = document.createElement('p');
		none.className = 'antilia-rich-text-issue-none';
		none.textContent = texts.proofreadNoSuggestions;
		body.append(none);
	}
	const footer = document.createElement('div');
	footer.className = 'antilia-rich-text-form-buttons';
	const ignore = document.createElement('button');
	ignore.type = 'button';
	ignore.className = lookClasses(look, 'button', 'button-ghost', 'button-small');
	ignore.textContent = texts.proofreadIgnore;
	ignore.addEventListener('click', () => {
		ignoring.get(view)?.(issue, found.text);
		done();
	});
	footer.append(ignore);
	body.append(footer);
	const title = issue.kind === 'spelling' ? texts.proofreadSpelling :
		issue.kind === 'style' ? texts.proofreadStyle : texts.proofreadGrammar;
	options.onPopover(true);
	popover = Popover.show(anchor, { title, body, placement: 'bottom-left', look,
		closeLabel: texts.close, maxWidth: '22rem',
		className: 'antilia-rich-text-popover ' + RICH_TEXT_ISSUE_CLASSES.popover,
		onClose: () => {
			if (open?.popover === popover) {
				open = null;
			}
			if (!applied && !view.isDestroyed) {
				view.focus();
			}
			options.onPopover(false);
		} });
	if (popover) {
		open = { popover, view };
		(buttons[0] ?? ignore).focus();
	}
}

/** How LanguageTool's server is asked. */
export interface LanguageToolOptions {
	/**
	 * The address of its check: https://api.languagetool.org/v2/check, or that of a server of the
	 * page's own, http://localhost:8081/v2/check for example, started with --allow-origin.
	 */
	url: string;
	/** The language when the editor has none, auto when left out: LanguageTool finds it. */
	language?: string;
	/** The language of the user, for the mistakes of those who speak it (motherTongue). */
	motherTongue?: string;
	/** picky for more rules, those of style among them; default when left out. */
	level?: 'default' | 'picky';
	/** More of LanguageTool's parameters: username and apiKey of Premium, enabledRules... */
	parameters?: Readonly<Record<string, string>>;
	/** Requests the server, fetch when left out. */
	fetch?: typeof fetch;
}

/** A match of LanguageTool's answer, what the editor reads of it. */
interface LanguageToolMatch {
	offset?: unknown;
	length?: unknown;
	message?: unknown;
	replacements?: unknown;
	rule?: { id?: unknown; issueType?: unknown };
}

/** LanguageTool's types of issues that are of style, the others being of grammar. */
const LANGUAGE_TOOL_STYLE = ['style', 'register', 'locale-violation', 'terminology',
	'typographical', 'whitespace', 'inconsistency'];

/**
 * @returns the kind of an issue of LanguageTool, by the type of its rule
 */
export function languageToolKind(issueType: unknown): RichTextIssueKind {
	return issueType === 'misspelling' ? 'spelling' :
		typeof issueType === 'string' && LANGUAGE_TOOL_STYLE.includes(issueType) ? 'style' :
			'grammar';
}

/**
 * @returns a proofreader asking LanguageTool, open source (LGPL), on its public server or one of
 *          the page's: the texts are checked in one request, apart by blank lines, as paragraphs.
 *          The public server takes about 20 requests a minute, of 20 KB at most, and hands the
 *          text to a third party.
 */
export function languageToolProofreader(options: LanguageToolOptions): RichTextProofreader {
	return Object.freeze({
		async check(texts: readonly string[], language: string | null,
			signal: AbortSignal): Promise<readonly (readonly RichTextIssue[])[]> {
			const starts: number[] = [];
			let text = '';
			texts.forEach((one, index) => {
				if (index > 0) {
					text += '\n\n';
				}
				starts.push(text.length);
				text += one;
			});
			const body = new URLSearchParams({ ...options.parameters, text,
				language: language ?? options.language ?? 'auto' });
			if (options.motherTongue) {
				body.set('motherTongue', options.motherTongue);
			}
			if (options.level) {
				body.set('level', options.level);
			}
			const request = options.fetch ?? fetch;
			const response = await request(options.url, { method: 'POST', body, signal,
				headers: { Accept: 'application/json' } });
			if (!response.ok) {
				throw new Error('LanguageTool answered ' + response.status);
			}
			const answer = await response.json() as { matches?: unknown };
			const found: Record<string, unknown>[][] = texts.map(() => []);
			const matches = Array.isArray(answer.matches) ? answer.matches as LanguageToolMatch[] : [];
			for (const match of matches) {
				const offset = match.offset;
				const length = match.length;
				if (!Number.isInteger(offset) || !Number.isInteger(length)) {
					continue;
				}
				// the text the match is in; none across texts
				let index = starts.length - 1;
				while (index > 0 && starts[index]! > (offset as number)) {
					index--;
				}
				const from = (offset as number) - starts[index]!;
				const replacements = Array.isArray(match.replacements) ?
					(match.replacements as { value?: unknown }[]).map((replacement) =>
						replacement?.value).filter((value) => typeof value === 'string') : [];
				found[index]!.push({ from, to: from + (length as number),
					kind: languageToolKind(match.rule?.issueType), message: match.message,
					replacements, rule: match.rule?.id });
			}
			return texts.map((one, index) => proofreadIssues(found[index], one.length));
		}
	});
}

/**
 * @returns a proofreader asking the server: Wicket's RichTextProofreadBehavior, whose URL the
 *          editors of Wicket get, and which the React and Svelte editors of a Wicket page take
 *          too. It posts the texts, as JSON, and the language; the server answers the issues of
 *          each text, {"issues": [[...], ...]}, or {"error": "..."}.
 */
export function proofreadAt(url: string): RichTextProofreader {
	return Object.freeze({
		async check(texts: readonly string[], language: string | null,
			signal: AbortSignal): Promise<readonly (readonly RichTextIssue[])[]> {
			const body = new URLSearchParams({ texts: JSON.stringify(texts) });
			if (language) {
				body.set('language', language);
			}
			const response = await fetch(url, { method: 'POST', body, signal,
				credentials: 'same-origin', headers: { Accept: 'application/json' } });
			let answer: { issues?: unknown; error?: unknown } = {};
			try {
				answer = await response.json() as typeof answer;
			} catch {
				// no JSON: the server failed
			}
			if (typeof answer.error === 'string' && answer.error) {
				throw new Error(answer.error);
			}
			if (!response.ok || !Array.isArray(answer.issues)) {
				throw new Error('The server answered ' + response.status);
			}
			const issues = answer.issues as unknown[];
			return texts.map((text, index) => proofreadIssues(issues[index], text.length));
		}
	});
}
