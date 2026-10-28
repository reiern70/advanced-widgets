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
 * Suggestions in the rich text editor: what the page's suggesters (RichTextSuggester) offer for
 * what the user types, in a list under the cursor.
 *
 * A suggester with a character, @ for the users or # for the tags, offers its items once the user
 * types it at the start of a word, for what follows it, the query, until a blank or another word.
 * Ctrl+Space offers the items of the suggesters without a character, which complete the word
 * before the cursor (e and Ctrl+Space: what starts with e), and opens again the list of a query
 * being typed. The items are asked a moment after the last key, the request before cancelled; at
 * most ten show.
 *
 * Up and Down move in the list, Enter or Tab choose, Escape closes it; a click chooses too. The
 * text keeps the focus, and tells of the list as a combobox does: aria-expanded, aria-controls,
 * aria-activedescendant, aria-autocomplete. Choosing an item of a mention inserts a mention, an
 * atomic node keeping the suggester's name and the item's id,
 * <span class="antilia-mention" data-mention="users" data-id="42">@Ann</span>; choosing one of a
 * completion, or a mention where the text has no mentions (Markdown), its text. A blank follows.
 */

import { Extension, Node as TiptapNode } from '@tiptap/core';
import { Plugin, PluginKey, type EditorState } from '@tiptap/pm/state';
import type { EditorView } from '@tiptap/pm/view';

import { partClasses, type Look } from './look.js';
import type { RichTextEditorTexts } from './rich-text-toolbar.js';

/** An item a suggester offers. */
export interface RichTextSuggestion {
	/** What a mention keeps: the id of a user, for example. */
	id: string;
	/** What the list shows, and a mention after its character. */
	label: string;
	/** More, shown after the label: an e-mail address, a description. */
	detail?: string;
	/** What a completion inserts; its label when left out. */
	text?: string;
}

/** What choosing an item inserts: a mention, or its text. */
export type RichTextSuggestionInsert = 'mention' | 'text';

/** What offers items for what the user types: users to mention, words to complete. */
export interface RichTextSuggester {
	/** Its name: the kind of its mentions (data-mention), users for example. */
	name: string;
	/**
	 * The character starting its queries, at the start of a word: @, #, :. None, when left out or
	 * null: it completes the word before the cursor, on Ctrl+Space.
	 */
	char?: string | null;
	/** What choosing an item inserts: a mention with a character, its text without, when left out. */
	insert?: RichTextSuggestionInsert;
	/**
	 * @param query what the user typed after the character, or the word before the cursor
	 * @param signal aborted once the answer is no longer wanted
	 * @returns the items, the best first
	 */
	items(query: string, signal: AbortSignal): Promise<readonly RichTextSuggestion[]>;
}

/** The classes of the mentions and of the list. */
export const RICH_TEXT_SUGGESTION_CLASSES = Object.freeze({
	mention: 'antilia-mention',
	list: 'antilia-rich-text-suggestions',
	detail: 'antilia-rich-text-suggestion-detail',
	status: 'antilia-rich-text-suggestion-status'
});

/** The attributes of a mention in the HTML. */
export const MENTION_ATTRIBUTES = Object.freeze({ kind: 'data-mention', id: 'data-id' });

/** The items shown, at most. */
const SHOWN = 10;

/** The longest query, in characters. */
const LONGEST = 50;

/** A name of a suggester, as the mentions keep it. */
const NAME = /^[A-Za-z0-9_-]{1,40}$/;

declare module '@tiptap/core' {
	interface Commands<ReturnType> {
		mention: {
			/** Inserts a mention at the cursor, or in place of a range. */
			insertMention: (mention: { kind: string; id: string; label: string },
				range?: { from: number; to: number }) => ReturnType;
		};
	}
}

/**
 * A mention: an atomic inline node, written as a span of its own the cleaning knows, its label as
 * its text.
 */
export const Mention = TiptapNode.create({
	name: 'mention',
	group: 'inline',
	inline: true,
	atom: true,
	selectable: false,

	addAttributes() {
		return {
			kind: {
				default: null,
				parseHTML: (element: HTMLElement) => element.getAttribute(MENTION_ATTRIBUTES.kind),
				renderHTML: (attributes: Record<string, unknown>) =>
					({ [MENTION_ATTRIBUTES.kind]: attributes.kind })
			},
			id: {
				default: null,
				parseHTML: (element: HTMLElement) => element.getAttribute(MENTION_ATTRIBUTES.id),
				renderHTML: (attributes: Record<string, unknown>) =>
					({ [MENTION_ATTRIBUTES.id]: attributes.id })
			},
			label: {
				default: '',
				parseHTML: (element: HTMLElement) => element.textContent ?? '',
				rendered: false
			}
		};
	},

	parseHTML() {
		return [{
			tag: 'span[' + MENTION_ATTRIBUTES.kind + ']',
			// before the colors of the text, also spans
			priority: 100,
			getAttrs: (element: HTMLElement) => NAME.test(element.getAttribute(
				MENTION_ATTRIBUTES.kind) ?? '') && element.hasAttribute(MENTION_ATTRIBUTES.id) &&
				element.textContent ? null : false
		}];
	},

	renderHTML({ node, HTMLAttributes }) {
		return ['span', { class: RICH_TEXT_SUGGESTION_CLASSES.mention, ...HTMLAttributes },
			node.attrs.label as string];
	},

	renderText({ node }) {
		return node.attrs.label as string;
	},

	addCommands() {
		return {
			insertMention: (mention, range) => ({ state, tr, dispatch }) => {
				const type = state.schema.nodes.mention;
				if (!type || !NAME.test(mention.kind) || !mention.label) {
					return false;
				}
				const from = range?.from ?? state.selection.from;
				const to = range?.to ?? state.selection.to;
				if (dispatch) {
					tr.replaceWith(from, to, [type.create({ kind: mention.kind, id: mention.id,
						label: mention.label }), state.schema.text(' ')]);
				}
				return true;
			}
		};
	}
});

/** What the plugin of the suggestions asks the editor. */
export interface SuggestionOptions {
	/** The suggesters, none read only. */
	suggesters: () => readonly RichTextSuggester[];
	/** Whether the text takes mentions: it has them, and is not Markdown. */
	mentions: () => boolean;
	/** How long after the last key the items are asked, in milliseconds. */
	delay: () => number;
	/** The element the list shows in, positioned. */
	container: () => HTMLElement;
	texts: () => RichTextEditorTexts;
	look: () => Look;
	log: (message: string, error: unknown) => void;
}

/** A query being typed: of a suggester with a character, or the word completed. */
interface Query {
	/** The suggesters asked. */
	suggesters: readonly RichTextSuggester[];
	/** Where the character, or the word, starts, and where the cursor is. */
	from: number;
	to: number;
	query: string;
	/** The character, empty for a completion. */
	char: string;
}

/** An item shown, and its suggester. */
interface Shown {
	item: RichTextSuggestion;
	suggester: RichTextSuggester;
}

/** What the text says of the list, as a combobox does. */
interface ListState {
	listId: string | null;
	optionId: string | null;
}

const KEY = new PluginKey<ListState>('antiliaSuggestions');

/** The meta of a transaction telling of the list. */
const LIST = 'antilia-rich-text-suggestions:list';

let counter = 0;

/** What opens the list of an editor on Ctrl+Space, by its view. */
const completing = new WeakMap<EditorView, () => boolean>();

/**
 * @returns the items of a suggester as the editor takes them: an id and a label each, at most ten
 */
export function suggestionItems(found: unknown): readonly RichTextSuggestion[] {
	if (!Array.isArray(found)) {
		return Object.freeze([]);
	}
	const items: RichTextSuggestion[] = [];
	for (const entry of found as unknown[]) {
		if (items.length >= SHOWN) {
			break;
		}
		if (!entry || typeof entry !== 'object') {
			continue;
		}
		const { id, label, detail, text } = entry as Record<string, unknown>;
		const name = typeof label === 'string' ? label.trim() : '';
		if ((typeof id !== 'string' && typeof id !== 'number') || !name) {
			continue;
		}
		const item: RichTextSuggestion = { id: String(id), label: name };
		if (typeof detail === 'string' && detail.trim()) {
			item.detail = detail.trim();
		}
		if (typeof text === 'string' && text) {
			item.text = text;
		}
		items.push(Object.freeze(item));
	}
	return Object.freeze(items);
}

/**
 * @returns the query being typed at the cursor for a suggester with a character, if any
 */
function queryAt(state: EditorState, suggesters: readonly RichTextSuggester[]): Query | null {
	const selection = state.selection;
	if (!selection.empty) {
		return null;
	}
	const $cursor = selection.$from;
	const parent = $cursor.parent;
	if (!parent.isTextblock || parent.type.spec.code ||
		$cursor.marks().some((mark) => mark.type.spec.code || mark.type.name === 'code')) {
		return null;
	}
	const before = parent.textBetween(Math.max(0, $cursor.parentOffset - LONGEST - 1),
		$cursor.parentOffset, undefined, '￼');
	for (const suggester of suggesters) {
		const char = suggester.char;
		if (!char) {
			continue;
		}
		const at = before.lastIndexOf(char);
		if (at < 0) {
			continue;
		}
		const query = before.slice(at + char.length);
		const start = $cursor.parentOffset - before.length + at;
		const previous = at > 0 ? before[at - 1]! : start > 0 ? '￼' : '';
		if (/\s/u.test(query) || query.includes('￼') || query.length > LONGEST ||
			(previous && !/[\s([{"'«“]/u.test(previous))) {
			continue;
		}
		const from = $cursor.pos - $cursor.parentOffset + start;
		return { suggesters: suggesters.filter((one) => one.char === char), from,
			to: $cursor.pos, query, char };
	}
	return null;
}

/**
 * @returns the word before the cursor, completed by the suggesters without a character
 */
function wordAt(state: EditorState, suggesters: readonly RichTextSuggester[]): Query | null {
	const completers = suggesters.filter((suggester) => !suggester.char);
	const selection = state.selection;
	if (!completers.length || !selection.empty) {
		return null;
	}
	const $cursor = selection.$from;
	const parent = $cursor.parent;
	if (!parent.isTextblock || parent.type.spec.code) {
		return null;
	}
	const before = parent.textBetween(Math.max(0, $cursor.parentOffset - LONGEST),
		$cursor.parentOffset, undefined, '￼');
	const word = /[\p{L}\p{N}_-]*$/u.exec(before)?.[0] ?? '';
	return { suggesters: completers, from: $cursor.pos - word.length, to: $cursor.pos,
		query: word, char: '' };
}

/**
 * Opens the list of the completions of the word before the cursor, or of the query being typed.
 *
 * @returns whether the editor has suggestions
 */
export function openSuggestions(view: EditorView): boolean {
	return completing.get(view)?.() ?? false;
}

/**
 * Offers the items of the suggesters for what the user types, in a list under the cursor.
 */
export const Suggestions = Extension.create<SuggestionOptions>({
	name: 'antiliaSuggestions',

	addOptions() {
		return {
			suggesters: () => [],
			mentions: () => false,
			delay: () => 150,
			container: () => document.body,
			texts: () => ({}) as RichTextEditorTexts,
			look: () => ({ name: 'antilia' }) as Look,
			log: () => undefined
		};
	},

	addKeyboardShortcuts() {
		return {
			'Ctrl-Space': () => openSuggestions(this.editor.view)
		};
	},

	addProseMirrorPlugins() {
		const options = this.options;
		const listId = 'antilia-rich-text-suggestions-' + (++counter);
		let view: EditorView | null = null;
		let list: HTMLElement | null = null;
		/** The query of the list open, null while closed. */
		let open: Query | null = null;
		/** A completion asked for, by where its word starts. */
		let completion: number | null = null;
		/** Where a query the user closed starts: not opened again until it goes. */
		let dismissed: number | null = null;
		let shown: Shown[] = [];
		let active = 0;
		let loading = false;
		let timer: ReturnType<typeof setTimeout> | null = null;
		let controller: AbortController | null = null;
		/** The query last asked, so that moving the cursor alone does not ask again. */
		let asked = '';

		/** Tells the text of the list, after the update it may come from. */
		const tell = () => queueMicrotask(() => {
			if (view && !view.isDestroyed) {
				const option = open && shown.length ? listId + '-' + active : null;
				const state = KEY.getState(view.state);
				const listed = open ? listId : null;
				if (state?.listId !== listed || state?.optionId !== option) {
					view.dispatch(view.state.tr.setMeta(LIST, { listId: listed, optionId: option })
						.setMeta('addToHistory', false));
				}
			}
		});

		const cancel = () => {
			if (timer !== null) {
				clearTimeout(timer);
				timer = null;
			}
			controller?.abort();
			controller = null;
		};

		const close = () => {
			cancel();
			open = null;
			completion = null;
			shown = [];
			asked = '';
			loading = false;
			list?.remove();
			list = null;
			tell();
		};

		const choose = (index: number) => {
			const chosen = shown[index];
			const query = open;
			if (!chosen || !query || !view) {
				return;
			}
			const { item, suggester } = chosen;
			const mention = (suggester.insert ?? (suggester.char ? 'mention' : 'text')) === 'mention';
			const range = { from: query.from, to: query.to };
			close();
			if (mention && options.mentions() && NAME.test(suggester.name)) {
				const type = view.state.schema.nodes.mention!;
				view.dispatch(view.state.tr.replaceWith(range.from, range.to, [type.create({
					kind: suggester.name, id: item.id, label: query.char + item.label }),
				view.state.schema.text(' ')]).scrollIntoView());
			} else {
				const text = mention ? query.char + item.label : item.text ?? item.label;
				view.dispatch(view.state.tr.insertText(text + ' ', range.from, range.to)
					.scrollIntoView());
			}
			view.focus();
		};

		const place = (element: HTMLElement) => {
			if (!view || !open) {
				return;
			}
			const container = options.container();
			const box = container.getBoundingClientRect();
			let at = { left: box.left, bottom: box.top };
			try {
				at = view.coordsAtPos(open.from);
			} catch {
				// not laid out
			}
			element.style.setProperty('left', Math.max(0, at.left - box.left) + 'px');
			element.style.setProperty('top', (at.bottom - box.top + 4) + 'px');
		};

		const render = () => {
			if (!view || !open) {
				return;
			}
			const texts = options.texts();
			const look = options.look();
			if (!list) {
				list = document.createElement('div');
				list.id = listId;
				list.className = partClasses(look, 'rich-text-menu') + ' ' +
					RICH_TEXT_SUGGESTION_CLASSES.list;
				list.setAttribute('role', 'listbox');
				list.setAttribute('aria-label', texts.suggestions);
				// the text keeps the focus
				list.addEventListener('mousedown', (event) => event.preventDefault());
				options.container().append(list);
			}
			list.replaceChildren();
			shown.forEach(({ item }, index) => {
				const option = document.createElement('div');
				option.id = listId + '-' + index;
				option.setAttribute('role', 'option');
				option.setAttribute('aria-selected', String(index === active));
				option.className = partClasses(look, 'rich-text-menu-item', ...(index === active ?
					['rich-text-menu-item-active' as const] : []));
				option.textContent = item.label;
				if (item.detail) {
					const detail = document.createElement('span');
					detail.className = RICH_TEXT_SUGGESTION_CLASSES.detail;
					detail.textContent = item.detail;
					option.append(' ', detail);
				}
				option.addEventListener('click', () => choose(index));
				list!.append(option);
			});
			if (!shown.length) {
				const status = document.createElement('div');
				status.className = RICH_TEXT_SUGGESTION_CLASSES.status;
				status.setAttribute('role', 'status');
				status.textContent = loading ? texts.suggestLoading : texts.suggestNone;
				list.append(status);
			}
			list.querySelector('[aria-selected="true"]')?.scrollIntoView?.({ block: 'nearest' });
			place(list);
			tell();
		};

		const ask = (query: Query) => {
			cancel();
			const key = query.from + '\n' + query.char + '\n' + query.query;
			if (key === asked) {
				render();
				return;
			}
			asked = key;
			loading = true;
			const request = new AbortController();
			controller = request;
			timer = setTimeout(() => {
				timer = null;
				Promise.all(query.suggesters.map((suggester) => {
					let answer: Promise<readonly RichTextSuggestion[]>;
					try {
						answer = Promise.resolve(suggester.items(query.query, request.signal));
					} catch (error) {
						answer = Promise.reject(error);
					}
					return answer.then((items) => suggestionItems(items).map((item) =>
						({ item, suggester })), (error: unknown) => {
						if (!request.signal.aborted) {
							options.log('A suggester of the rich text editor failed', error);
						}
						return [] as Shown[];
					});
				})).then((found) => {
					if (controller !== request || request.signal.aborted) {
						return;
					}
					controller = null;
					loading = false;
					shown = found.flat().slice(0, SHOWN);
					active = 0;
					render();
				});
			}, Math.max(0, options.delay()));
			render();
		};

		/** Opens, follows or closes the list as the text and the cursor change. */
		const follow = (state: EditorState) => {
			const suggesters = options.suggesters();
			let query = suggesters.length && view?.editable ? queryAt(state, suggesters) : null;
			if (!query && completion !== null && view?.editable) {
				const word = wordAt(state, suggesters);
				query = word && word.from === completion ? word : null;
			}
			if (query && dismissed === query.from) {
				query = null;
			} else if (!query || dismissed !== query.from) {
				dismissed = null;
			}
			if (!query) {
				if (open) {
					close();
				}
				return;
			}
			const same = open && open.from === query.from && open.query === query.query;
			open = query;
			if (!same) {
				ask(query);
			}
		};

		const complete = (): boolean => {
			if (!view || !view.editable) {
				return false;
			}
			const suggesters = options.suggesters();
			const query = queryAt(view.state, suggesters) ?? wordAt(view.state, suggesters);
			if (!query) {
				return false;
			}
			dismissed = null;
			completion = query.char ? null : query.from;
			asked = '';
			open = query;
			ask(query);
			return true;
		};

		return [new Plugin<ListState>({
			key: KEY,
			state: {
				init: () => ({ listId: null, optionId: null }),
				apply: (transaction, value) => transaction.getMeta(LIST) as ListState | undefined ??
					value
			},
			props: {
				attributes: (state): Record<string, string> => {
					const value = KEY.getState(state);
					return value?.listId ? { 'aria-expanded': 'true', 'aria-controls': value.listId,
						'aria-autocomplete': 'list',
						...(value.optionId ? { 'aria-activedescendant': value.optionId } : {}) } : {};
				},
				handleKeyDown: (_view, event) => {
					if (!open) {
						return false;
					}
					const count = shown.length;
					if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
						if (!count) {
							return true;
						}
						active = (active + (event.key === 'ArrowDown' ? 1 : count - 1)) % count;
						render();
						return true;
					}
					if ((event.key === 'Enter' || event.key === 'Tab') && !event.shiftKey &&
						count) {
						choose(active);
						return true;
					}
					if (event.key === 'Escape') {
						dismissed = open.from;
						close();
						return true;
					}
					return false;
				},
				handleDOMEvents: {
					blur: () => {
						if (open) {
							close();
						}
						return false;
					}
				}
			},
			view: (editorView) => {
				view = editorView;
				completing.set(editorView, complete);
				return {
					update: (updated, previous) => {
						if (updated.state.doc !== previous.doc ||
							!updated.state.selection.eq(previous.selection)) {
							follow(updated.state);
						} else if (open && list) {
							place(list);
						}
					},
					destroy: () => {
						completing.delete(editorView);
						cancel();
						list?.remove();
						list = null;
						open = null;
						view = null;
					}
				};
			}
		})];
	}
});

/**
 * @returns a suggester asking the server: Wicket's RichTextSuggestBehavior, whose URL the editors
 *          of Wicket get, and which the React and Svelte editors of a Wicket page take too. It
 *          posts the name of the suggester and the query; the server answers its items,
 *          {"items": [...]}, or {"error": "..."}.
 */
export function suggestAt(url: string, suggester: Omit<RichTextSuggester, 'items'>):
	RichTextSuggester {
	return Object.freeze({
		...suggester,
		async items(query: string, signal: AbortSignal): Promise<readonly RichTextSuggestion[]> {
			const body = new URLSearchParams({ name: suggester.name, query });
			const response = await fetch(url, { method: 'POST', body, signal,
				credentials: 'same-origin', headers: { Accept: 'application/json' } });
			let answer: { items?: unknown; error?: unknown } = {};
			try {
				answer = await response.json() as typeof answer;
			} catch {
				// no JSON: the server failed
			}
			if (typeof answer.error === 'string' && answer.error) {
				throw new Error(answer.error);
			}
			if (!response.ok || !Array.isArray(answer.items)) {
				throw new Error('The server answered ' + response.status);
			}
			return suggestionItems(answer.items);
		}
	});
}
