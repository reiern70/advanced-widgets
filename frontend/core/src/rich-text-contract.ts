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
 * What the rich text editors share, Tiptap's and CKEditor's, without either: the state they keep
 * besides their text and the events they fire, which the events of Wicket's editors listen to,
 * for both.
 */

/** What is selected: from where the selection starts to where its head, the cursor, is. */
export interface RichTextEditorSelection {
	anchor: number;
	head: number;
}

/**
 * The state of an editor besides its text: what is selected, how far it is scrolled, in pixels,
 * and the size the user gave it, null for the size the page's style sheets give it. The positions
 * of the selection are those of ProseMirror, which count the places between the nodes too; those
 * of CKEditor are counted the same way.
 */
export interface RichTextEditorState {
	selection?: RichTextEditorSelection;
	scrollTop?: number;
	scrollLeft?: number;
	width?: number | null;
	height?: number | null;
}

/** What onStateChange is told besides the state. */
export interface RichTextEditorStateChange {
	/** Whether the page is going: the last chance to keep the state. */
	leaving: boolean;
	/** Whether the text changed since the state was last reported. */
	changed: boolean;
}

/** The events the editors fire, on their text area or their element, which bubble. */
export const RICH_TEXT_EDITOR_EVENTS = Object.freeze({
	/** The user changed the text. */
	change: 'antilia-rich-text-editor:change',
	/** The editor got the focus. */
	focus: 'antilia-rich-text-editor:focus',
	/** The editor lost the focus. */
	blur: 'antilia-rich-text-editor:blur',
	/** The user moved the cursor or selected text. */
	select: 'antilia-rich-text-editor:select',
	/** The user pressed Ctrl+S, or Cmd+S. */
	save: 'antilia-rich-text-editor:save',
	/** The user resized the editor. */
	resize: 'antilia-rich-text-editor:resize',
	/** The text came to have changes not saved, or no longer has. */
	modified: 'antilia-rich-text-editor:modified',
	/** The state was reported: what onStateChange is given. */
	state: 'antilia-rich-text-editor:state',
	/** The editor has a text: it started, or its text changed, by the user or by the page. */
	text: 'antilia-rich-text-editor:text'
});

/**
 * @returns a state in one shape whatever its source: no selection is a cursor at the start, no
 *          scrolling none and no size the page's
 */
export function canonicalState(state: RichTextEditorState | null | undefined): string {
	if (!state) {
		return 'null';
	}
	return JSON.stringify([state.selection?.anchor ?? 0, state.selection?.head ?? 0,
		Math.round(state.scrollTop ?? 0), Math.round(state.scrollLeft ?? 0), state.width ?? null,
		state.height ?? null]);
}

/**
 * @returns whether two states are the same, whatever their source
 */
export function sameState(one: RichTextEditorState | null | undefined,
	other: RichTextEditorState | null | undefined): boolean {
	return canonicalState(one) === canonicalState(other);
}
