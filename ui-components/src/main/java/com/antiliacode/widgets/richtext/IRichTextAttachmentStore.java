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

import com.antiliacode.widgets.upload.IUploadFileFormat;

/**
 * Keeps the files users attach to the texts of a {@link RichTextEditor}, a PDF or a spreadsheet,
 * which go into the text as links, and tells the address each is served at: in a folder, a
 * database or a store of files. It deletes those the users took out of their texts again, if it
 * wants to: the editor tells it which, as {@link RichTextEditor#setAttachmentDeletion(
 * RichTextImageDeletion) its mode} says, at once or once the text is saved.
 * <p>
 * {@link RichTextFiles The resource of the uploads}, mounted once by the application, takes the
 * files too, and serves those a store keeps: the store then answers the addresses the resource
 * gives ({@link RichTextFiles#addressOf(String)}) and opens a file by its name
 * ({@link #open(String)}), as the format its bytes were found to be, inline only when a browser may
 * open it, as an attachment otherwise.
 *
 * @see RichTextEditor#setAttachmentStore(IRichTextAttachmentStore)
 * @see RichTextAttachmentUploadBehavior
 * @since 11.0.0
 */
@FunctionalInterface
public interface IRichTextAttachmentStore extends IClusterable
{
	/**
	 * Keeps a file a user uploaded. The file is checked already: one of the formats allowed, as
	 * its bytes say, no larger than allowed, and scanned when the editor has a scanner. The file
	 * the upload holds is deleted after.
	 *
	 * @param file
	 *            the file uploaded; its name and its content type are the browser's, not to be
	 *            trusted
	 * @param format
	 *            its format, as its bytes say, which it is to be served as
	 * @return the address the file is served at: an http or https one, or a relative one, as the
	 *         editor and its cleaning ({@link RichTextPolicy#isSafeUrl(String)}) take
	 * @throws IOException
	 *             when the file could not be kept
	 */
	String store(FileUpload file, IUploadFileFormat format) throws IOException;

	/**
	 * Deletes a file a user took out of a text, if the store wants to: it may be in another text
	 * still. Only the addresses the editor knows come here: those of the files it was shown with,
	 * and those the store answered for the uploads of the page; never one the browser made up.
	 * Does nothing by default.
	 *
	 * @param url
	 *            the address of the file, as {@link #store(FileUpload, IUploadFileFormat)}
	 *            answered it
	 * @throws IOException
	 *             when the file could not be deleted
	 */
	default void delete(String url) throws IOException
	{
	}

	/**
	 * Tells whether an address is that of a file the store keeps. A file is a plain link in
	 * Markdown, {@code [report.pdf](url)}: the editor gives the browser the links of a text whose
	 * addresses the store keeps, which come back as files there, listed by the tool of the files and
	 * told of when the user takes them out. None by default: in Markdown, the links of a text read
	 * again stay links, but for the files uploaded since the text was last saved, and those of
	 * {@link RichTextFiles the resource of the uploads}.
	 *
	 * @param url
	 *            the address of a link of a text
	 * @return whether it is that of a file the store keeps
	 * @since 11.0.0
	 */
	default boolean isStored(String url)
	{
		return false;
	}

	/**
	 * Opens a file for {@link RichTextFiles the resource of the uploads} to serve, by the name its
	 * address ends with, one the user of the request may see. None by default: the application
	 * serves its files itself.
	 *
	 * @param name
	 *            the name of the file, as the address ends, decoded; never a path
	 * @return the file, with the format it was found to be as it was uploaded; {@code null} for
	 *         none of that name, or one the user may not see
	 * @throws IOException
	 *             when the file could not be read
	 */
	default RichTextServedFile open(String name) throws IOException
	{
		return null;
	}
}
