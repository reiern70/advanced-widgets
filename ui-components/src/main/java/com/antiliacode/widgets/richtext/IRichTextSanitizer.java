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

import org.apache.wicket.util.io.IClusterable;

/**
 * Cleans the text of a {@link RichTextEditor}, or of a
 * {@link com.antiliacode.widgets.ckeditor.CKEditor}, which comes from the browser and is never to be
 * trusted: what it gives back is what the model gets and a page shows. The text is in the format of
 * the editor. For HTML, {@link RichTextPolicy} and
 * {@link com.antiliacode.widgets.ckeditor.CKEditorPolicy} keep what the features of each editor
 * write; for Markdown, {@link RichTextMarkdownSanitizer} cleans the addresses of the links and
 * images; for Tiptap's JSON, {@link RichTextJsonSanitizer} does.
 *
 * @since 11.0.0
 */
@FunctionalInterface
public interface IRichTextSanitizer extends IClusterable
{
	/**
	 * @param text
	 *            the text, HTML, Markdown or JSON as the editor keeps it, maybe {@code null}
	 * @return the text cleaned, {@code null} for {@code null}
	 */
	String sanitize(String text);
}
