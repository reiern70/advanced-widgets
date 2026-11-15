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
package com.antiliacode.widgets.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Locale;

import org.apache.wicket.util.io.IClusterable;

/**
 * A format of the files users upload, told by the first bytes of the file, never by its name or
 * the type the browser gives, which anyone can set: PNG, PDF or a format of an application's own.
 * The format a file was found to be is the one it is served as, by its media type, and it says
 * whether a browser may open a file of it in its window ({@link #isInline()}) or only save it.
 * <p>
 * The images of the rich text editors are of the formats of
 * {@link com.antiliacode.widgets.richtext.IRichTextImageType}, which extends this interface; the
 * files attached to their texts of those of {@link UploadFileFormat} and of the application's own.
 * <p>
 * A format without a signature in its bytes, a text, matches any head that may be one, and checks
 * the whole file once uploaded ({@link #accepts(InputStream)}). A file is of the first format with
 * a signature its head matches; else, of the formats without one it matches, of the one its
 * extension names, else the first ({@link #detect(byte[], String, Collection)}): the extension only
 * chooses among the texts the bytes allow, and a PDF named {@code notes.txt} stays a PDF.
 *
 * @since 11.0.0
 */
public interface IUploadFileFormat extends IClusterable
{
	/**
	 * How many of the first bytes of a file {@link #matches(byte[])} is given, at most: enough to
	 * read the names of the first entries of a ZIP, which tell an Office document from another.
	 */
	int HEAD = 4096;

	/**
	 * @return the media type a file of the format is served as, {@code application/pdf} for
	 *         example, which the browser checks the files against too before sending them
	 */
	String getMimeType();

	/**
	 * @return the extension a file of the format takes, without the dot
	 */
	String getExtension();

	/**
	 * @return what the messages call the format: its name, PDF for example, the same in every
	 *         language. A format of an application's own may translate it, in the language of the
	 *         session.
	 */
	String getLabel();

	/**
	 * @param head
	 *            the first bytes of a file, up to {@link #HEAD} of them: fewer for a shorter file
	 * @return whether the file is of this format
	 */
	boolean matches(byte[] head);

	/**
	 * @return whether a browser may open a file of the format in its window, as an image or a PDF,
	 *         rather than only save it: the file is then served {@code inline}, else as an
	 *         {@code attachment}. Either way it is served as its media type, with
	 *         {@code X-Content-Type-Options: nosniff} and in a sandbox
	 *         ({@code Content-Security-Policy: sandbox}), so that it runs no script of the site's.
	 *         {@code false} by default.
	 */
	default boolean isInline()
	{
		return false;
	}

	/**
	 * @return whether the format has a signature in its first bytes, as PDF has, rather than
	 *         taking any head that may be of it, as a text does; {@code true} by default
	 */
	default boolean hasSignature()
	{
		return true;
	}

	/**
	 * Checks the whole file once its head matched, before it is kept: a text checks that it is
	 * UTF-8 and holds no NUL, for example. Takes any by default.
	 *
	 * @param file
	 *            the file, from its first byte
	 * @return whether the file is of the format
	 * @throws IOException
	 *             when the file could not be read
	 */
	default boolean accepts(InputStream file) throws IOException
	{
		return true;
	}

	/**
	 * @param head
	 *            the first bytes of a file, up to {@link #HEAD} of them
	 * @param fileName
	 *            the name the browser gave the file, not to be trusted: its extension only chooses
	 *            among the formats the bytes allow; {@code null} for none
	 * @param formats
	 *            the formats the file may be of, in their order
	 * @return the format of the file: the first with a signature its head matches; else, of those
	 *         without one it matches, the one its extension names, else the first; {@code null}
	 *         for none
	 */
	static <T extends IUploadFileFormat> T detect(byte[] head, String fileName,
		Collection<T> formats)
	{
		if (head == null || formats == null)
		{
			return null;
		}
		String name = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
		T first = null;
		for (T format : formats)
		{
			if (format != null && format.hasSignature() && format.matches(head))
			{
				return format;
			}
		}
		for (T format : formats)
		{
			if (format == null || format.hasSignature() || !format.matches(head))
			{
				continue;
			}
			if (first == null)
			{
				first = format;
			}
			String extension = format.getExtension();
			if (extension != null && !extension.isEmpty() &&
				name.endsWith("." + extension.toLowerCase(Locale.ROOT)))
			{
				return format;
			}
		}
		return first;
	}

	/**
	 * @param head
	 *            the first bytes of a file, up to {@link #HEAD} of them
	 * @param formats
	 *            the formats the file may be of
	 * @return the first of the formats the file is of, {@code null} for none
	 */
	static <T extends IUploadFileFormat> T detect(byte[] head, Collection<T> formats)
	{
		if (head == null || formats == null)
		{
			return null;
		}
		for (T format : formats)
		{
			if (format != null && format.matches(head))
			{
				return format;
			}
		}
		return null;
	}
}
