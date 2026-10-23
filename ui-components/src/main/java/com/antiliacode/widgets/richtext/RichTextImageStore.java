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

import java.io.IOException;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.io.IClusterable;

/**
 * Keeps the images users upload into a {@link RichTextEditor}, and tells the address each is served
 * at, which the image of the text then takes: in a folder, a database or a store of files, served
 * by a resource of the application's own for example.
 *
 * @see RichTextEditor#setImageStore(RichTextImageStore)
 * @see RichTextImageUploadBehavior
 * @since 11.0.0
 */
@FunctionalInterface
public interface RichTextImageStore extends IClusterable
{
	/**
	 * Keeps an image a user uploaded. The image is checked already: one of the types allowed, as
	 * its bytes say, and no larger than allowed. The file the upload holds is deleted after.
	 *
	 * @param image
	 *            the file uploaded; its name and its content type are the browser's, not to be
	 *            trusted
	 * @param type
	 *            its type, as its bytes say
	 * @return the address the image is served at: an http or https one, or a relative one, as the
	 *         editor and its cleaning ({@link RichTextPolicy#isSafeUrl(String)}) take
	 * @throws IOException
	 *             when the image could not be kept
	 */
	String store(FileUpload image, RichTextImageType type) throws IOException;
}
