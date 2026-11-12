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
 * antilia-rich-text-editor.js: registers Wicket.RichTextEditor for the RichTextEditor of
 * ui-components, which calls Wicket.RichTextEditor.attach(id, options). The editor sends the
 * server its state, and its text when the user changed it, with each of its events going to the
 * server, which the events of the RichTextEditor, Wicket.ClientEvents, send
 * (Wicket.RichTextEditor.stateParameters): by default a moment after the user changed them, as the
 * editor loses the focus and, in a beacon, as the page goes. The core's own handlers sending the
 * state are off.
 *
 * Wicket.RichTextEditor.registerExtension(name, function (kit) {...}) plugs in an extension of the
 * page's own, built from the Tiptap classes of the kit, a Mark or a Node for example.
 * Wicket.RichTextEditor.registerFeature(name, { extensions, tools }) plugs in a feature of the
 * page's own, which an IRichTextFeature of that name renders the script of: its extensions, made
 * from the kit, and the tools of the toolbar acting on it (see rich-text-features.ts).
 * Wicket.RichTextEvents.register(name, { from, init, fire }) plugs in a type of events, shared
 * with CKEditor's script (see rich-text-events.ts).
 *
 * With uploadUrl, the URL of the editor's upload behavior, the editor uploads the images the user
 * chooses, drops or pastes there, telling the share sent, and the server answers their addresses;
 * with permanentImages too, the user ticks the images to store permanently, which the editor asks
 * the same behavior to, and the tool storedImage finds those stored there. With imagesUrl, that of
 * its Ajax behavior hearing of the images taken out of the text, the editor tells the server of
 * them, via Ajax, a moment after the user took them out; the server deletes those it knows, at once
 * or once the form is sent. With proofreadUrl, that
 * of its proofreading behavior, the server checks the text, which the editor underlines. With
 * suggestUrl and suggesters, the names, characters and insertions of the editor's suggesters, the
 * server offers what the user types: the users to mention, the words to complete. The grammars of the code
 * blocks are not here: antilia-rich-text-highlight.js, which a page loads only where an editor
 * highlights, registers them as Wicket.RichTextHighlighter, before this script or after it.
 */

import { registerRichTextHighlighter } from '../rich-text-code-blocks.js';
import { registerRichTextFeature, richTextFeatures } from '../rich-text-features.js';
import { NO_STATE_EVENTS } from '../editor-events.js';
import { RICH_TEXT_EDITOR_KIT, RichTextEditor, registerRichTextExtension, richTextExtensions,
	type RichTextEditorOptions } from '../rich-text-editor.js';
import type { RichTextHighlighter } from '../rich-text-languages.js';
import { proofreadAt } from '../rich-text-proofreading.js';
import { suggestAt, type RichTextSuggester } from '../rich-text-suggestions.js';
import { findImagesAt, storePermanentlyAt } from '../rich-text-stored-images.js';
import { uploadImageTo } from '../rich-text-upload.js';
import { useWicketHost, wicket } from './host.js';
import { useRichTextEvents } from './rich-text-events.js';
import { pendingParameters, sendState, stateParameters } from './send-state.js';

/** The options of Wicket's editor: those of the core, and the URLs of the behaviors of the server. */
interface WicketRichTextEditorOptions extends Omit<RichTextEditorOptions, 'suggesters'> {
	/** The URL of the behavior taking the images the user uploads, and answering their addresses. */
	uploadUrl?: string;
	/**
	 * Whether the user stores images permanently, and inserts those stored, by the behavior of
	 * uploadUrl.
	 */
	permanentImages?: boolean;
	/** The URL of the Ajax behavior hearing of the images the user took out of the text. */
	imagesUrl?: string;
	/** The URL of the behavior checking the text, and answering its issues. */
	proofreadUrl?: string;
	/** The URL of the behavior answering the items of the suggesters. */
	suggestUrl?: string;
	/** The suggesters of the server, without their items, which suggestUrl answers. */
	suggesters?: readonly (RichTextSuggester | Omit<RichTextSuggester, 'items'>)[];
}

const WicketRichTextEditor = {

	/**
	 * Makes an editor of a text area.
	 *
	 * @param id the markup id of the text area
	 * @param options the text, its format, the features, the toolbar and the state; the URLs of
	 *        the behaviors of the server
	 * @returns the editor, null when there is no such element
	 */
	attach(id: string, options: WicketRichTextEditorOptions = {}): RichTextEditor | null {
		const { uploadUrl, permanentImages, imagesUrl, proofreadUrl, suggestUrl, suggesters,
			...rest } = options;
		const onUpload = uploadUrl ? uploadImageTo(uploadUrl) : rest.onUpload;
		const stored = !!uploadUrl && !!permanentImages;
		const onStorePermanently = stored ? storePermanentlyAt(uploadUrl) : rest.onStorePermanently;
		const onFindImages = stored ? findImagesAt(uploadUrl) : rest.onFindImages;
		const onImagesRemoved = imagesUrl ? (urls: string[]) => {
			sendState(id, imagesUrl, [{ name: 'removed', value: JSON.stringify(urls) }], false);
			rest.onImagesRemoved?.(urls);
		} : rest.onImagesRemoved;
		const proofreader = proofreadUrl ? proofreadAt(proofreadUrl) : rest.proofreader;
		const asked = suggesters?.map((suggester) => 'items' in suggester ? suggester :
			suggestUrl ? suggestAt(suggestUrl, suggester) : null)
			.filter((suggester): suggester is RichTextSuggester => !!suggester);
		// the events of the server send the state
		return RichTextEditor.attach(id, { ...rest, events: { ...NO_STATE_EVENTS, ...rest.events },
			onUpload, onImagesRemoved, onStorePermanently, onFindImages, proofreader,
			suggesters: asked });
	},

	/** What the events of the server carry besides: see stateParameters(). */
	stateParameters,

	/** What the page's other Ajax requests carry for the editor: see pendingParameters(). */
	pendingParameters,

	/**
	 * @param id the markup id of the text area
	 * @returns the editor, if there is one
	 */
	of(id: string): RichTextEditor | null {
		return RichTextEditor.of(id);
	},

	/** Plugs in an extension: see registerRichTextExtension() of the core. */
	registerExtension: registerRichTextExtension,

	/**
	 * Plugs in a feature, its extensions and its tools, which IRichTextFeature names: see
	 * registerRichTextFeature() of the core.
	 */
	registerFeature: registerRichTextFeature,

	/** @returns the names of the features registered */
	features: richTextFeatures,

	/** @returns the names of the extensions registered */
	extensions: richTextExtensions,

	/** The Tiptap classes of the editor, for the extensions of a page. */
	kit: RICH_TEXT_EDITOR_KIT,

	/** Highlights the code blocks of the editors told so: see registerRichTextHighlighter(). */
	registerHighlighter: registerRichTextHighlighter
};

const Wicket = wicket();
if (Wicket.RichTextEditor === undefined) {
	useWicketHost();
	Wicket.RichTextEditor = WicketRichTextEditor;
	useRichTextEvents();
	// antilia-rich-text-highlight.js came first
	const highlighter = Wicket.RichTextHighlighter as RichTextHighlighter | undefined;
	if (highlighter) {
		registerRichTextHighlighter(highlighter);
	}
}
