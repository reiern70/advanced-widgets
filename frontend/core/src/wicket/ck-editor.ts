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
 * antilia-ckeditor.js: registers Wicket.CKEditor for the CKEditor of ui-components, which calls
 * Wicket.CKEditor.attach(id, options). It holds no code of CKEditor: the page loads CKEditor's
 * browser build before, which defines the global CKEDITOR, with its license key in the options.
 * The editor sends the server its state, and its text when the user changed it, with each of its
 * events going to the server, which the events of the CKEditor, Wicket.ClientEvents, send
 * (Wicket.CKEditor.stateParameters): by default a moment after the user changed them, as the
 * editor loses the focus and, in a beacon, as the page goes. The core's own handlers sending the
 * state are off.
 *
 * With uploadUrl, the URL of the editor's RichTextImageUploadBehavior, or of the resource of the
 * uploads of the application (RichTextFiles) with the key of the editor's uploads, the editor
 * uploads the images the user chooses, pastes or drops there (uploadImageTo), as the Tiptap editor
 * of Wicket does; with permanentImages too, the user ticks the images to store permanently in the
 * dialog of the uploads, which the editor asks the same behavior to, and the tool storedImage
 * finds those stored there. With attachUrl, that of its behavior of the files attached or of the
 * resource, the files attached the same way. With imagesAjax, the attributes of the requests of
 * its Ajax behavior hearing of the images and the files taken out of the text, which the server
 * renders, the editor tells the server of them, via Ajax, a moment after the user took them out:
 * a request of Wicket's Ajax with those attributes, the extra parameters of the page going with
 * it; the server deletes those it knows, at once or once the form is sent.
 */

import { CKEditor, type CKEditorOptions } from '../ck-editor.js';
import { NO_STATE_EVENTS } from '../editor-events.js';
import { findImagesAt, storePermanentlyAt } from '../rich-text-stored-images.js';
import { uploadFileTo, uploadImageTo } from '../rich-text-upload.js';
import { useWicketHost, wicket } from './host.js';
import { useRichTextEvents } from './rich-text-events.js';
import { type AjaxAttributes, pendingParameters, roundTrip, stateParameters }
	from './send-state.js';

/** The options of Wicket's editor: those of the core, and the URLs of the behaviors of the server. */
interface WicketCKEditorOptions extends CKEditorOptions {
	/** The URL of the behavior taking the images the user uploads, and answering their addresses. */
	uploadUrl?: string;
	/**
	 * Whether the user stores images permanently, and inserts those stored, by the behavior of
	 * uploadUrl.
	 */
	permanentImages?: boolean;
	/**
	 * The attributes of the requests of the Ajax behavior hearing of the images and the files the
	 * user took out of the text, as the server renders them: its URL, its method, its extra
	 * parameters.
	 */
	imagesAjax?: AjaxAttributes;
	/**
	 * The URL of the behavior taking the files the user attaches, or of the resource of the
	 * uploads, and answering their addresses.
	 */
	attachUrl?: string;
}

const WicketCKEditor = {

	/**
	 * Makes an editor of a text area.
	 *
	 * @param id the markup id of the text area
	 * @param options CKEditor's license key, the text, its format, the features and the state;
	 *        the URLs of the behaviors of the server
	 * @returns the editor, null when there is no such element
	 */
	attach(id: string, options: WicketCKEditorOptions = {}): CKEditor | null {
		const { uploadUrl, permanentImages, imagesAjax, attachUrl, ...rest } = options;
		const stored = !!uploadUrl && !!permanentImages;
		const onImagesRemoved = imagesAjax ? (urls: string[]) => {
			roundTrip(imagesAjax, [{ name: 'removed', value: JSON.stringify(urls) }]);
			rest.onImagesRemoved?.(urls);
		} : rest.onImagesRemoved;
		const onAttachmentsRemoved = imagesAjax ? (urls: string[]) => {
			roundTrip(imagesAjax, [{ name: 'removedAttachments', value: JSON.stringify(urls) }]);
			rest.onAttachmentsRemoved?.(urls);
		} : rest.onAttachmentsRemoved;
		// the events of the server send the state
		return CKEditor.attach(id, { ...rest,
			onUpload: uploadUrl ? uploadImageTo(uploadUrl) : rest.onUpload,
			onStorePermanently: stored ? storePermanentlyAt(uploadUrl) : rest.onStorePermanently,
			onFindImages: stored ? findImagesAt(uploadUrl) : rest.onFindImages,
			onAttach: attachUrl ? uploadFileTo(attachUrl) : rest.onAttach,
			onImagesRemoved, onAttachmentsRemoved,
			events: { ...NO_STATE_EVENTS, ...rest.events } });
	},

	/** What the events of the server carry besides: see stateParameters(). */
	stateParameters,

	/** What the page's other Ajax requests carry for the editor: see pendingParameters(). */
	pendingParameters,

	/**
	 * @param id the markup id of the text area
	 * @returns the editor, if there is one
	 */
	of(id: string): CKEditor | null {
		return CKEditor.of(id);
	}
};

const Wicket = wicket();
if (Wicket.CKEditor === undefined) {
	useWicketHost();
	Wicket.CKEditor = WicketCKEditor;
	useRichTextEvents();
}
