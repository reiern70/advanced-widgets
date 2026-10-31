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
package com.antiliacode.widgets.richtext;

/**
 * When a {@link RichTextEditor} deletes the images the user took out of its text: deleted by a
 * key, cut, taken out by the list of the images of the toolbar or by the code of a page. The browser
 * tells the server at once, via Ajax, in either mode; the editor then calls
 * {@link RichTextEditor#onImagesRemoved(org.apache.wicket.ajax.AjaxRequestTarget, java.util.Set)}
 * and sends a {@link RichTextImagesRemovedEvent}. The deleting is
 * {@link RichTextEditor#deleteImages(java.util.Set)}, by the store's
 * {@link IRichTextImageStore#delete(String)} unless a page overrides it. Either way, saving the text
 * deletes the images uploaded since that it does not hold.
 *
 * @see RichTextEditor#setImageDeletion(RichTextImageDeletion)
 * @since 11.0.0
 */
public enum RichTextImageDeletion
{
	/**
	 * Each image is deleted as the browser tells it was taken out. An image Ctrl+Z, or a paste
	 * after a cut, brings back is then gone: for texts that are not saved, a chat for example, or
	 * stores that keep their images anyway.
	 */
	AT_ONCE,

	/**
	 * The server collects the images taken out, and deletes them once the page says the text was
	 * saved, by {@link RichTextEditor#markSaved(org.apache.wicket.ajax.AjaxRequestTarget)} or
	 * {@link RichTextEditor#deleteRemovedImages()}: those the text saved no longer holds, and
	 * those uploaded since it was shown that it does not hold either. An image Ctrl+Z brought back
	 * is in the text, and stays. The model taking the text, as an Ajax behavior of the page may
	 * have it do as the user types, deletes nothing; nor does leaving the page without saving. The
	 * default.
	 */
	ON_SAVE
}
