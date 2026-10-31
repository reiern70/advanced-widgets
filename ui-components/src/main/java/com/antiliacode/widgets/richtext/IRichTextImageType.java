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
import java.io.InputStream;
import java.util.Collection;

import org.apache.wicket.util.io.IClusterable;

/**
 * A type of the images a user uploads into a {@link RichTextEditor}, told by the first bytes of the
 * file, never by its name or the type the browser gives, which anyone can set. The built-in ones
 * are {@link RichTextImageType}'s: PNG, JPEG, GIF, WebP and AVIF. An application adds one of its
 * own by implementing this interface and giving it to
 * {@link RichTextEditor#setImageTypes(Collection)} with the others it takes.
 * <p>
 * Mind what a browser does with a file of the type when it is opened by itself: an SVG is a
 * document running scripts, which is why there is none built in.
 *
 * @see RichTextImageType
 * @since 10.11.0
 */
public interface IRichTextImageType extends IClusterable
{
	/** How many of the first bytes of a file {@link #matches(byte[])} is given, at most. */
	int HEAD = 32;

	/**
	 * @return the media type the image is served as, {@code image/png} for example, which the
	 *         editor in the browser checks the files against too
	 */
	String getMimeType();

	/**
	 * @return the extension a file of the type takes, without the dot
	 */
	String getExtension();

	/**
	 * @return what the messages call the type: the name of the format, PNG for example, the same
	 *         in every language. A type of an application's own may translate it, in the language
	 *         of the session.
	 */
	String getLabel();

	/**
	 * @param head
	 *            the first bytes of a file, up to {@link #HEAD} of them: fewer for a shorter file
	 * @return whether the file is an image of this type
	 */
	boolean matches(byte[] head);

	/**
	 * Reads the width and the height of an image of this type from the header of its file,
	 * without decoding the picture, for the limit of {@link RichTextEditor#setImageMaxDimensions(
	 * RichTextImageDimensions)}. Unknown by default: with a limit, the editor then refuses the images of
	 * the type.
	 *
	 * @param input
	 *            the file, from its first byte
	 * @return the width and the height, as the browser shows the image; {@code null} when the
	 *         header does not tell them
	 * @throws IOException
	 *             when the file could not be read
	 */
	default RichTextImageDimensions readDimensions(InputStream input) throws IOException
	{
		return null;
	}

	/**
	 * @param head
	 *            the first bytes of a file, up to {@link #HEAD} of them
	 * @param types
	 *            the types the file may be of
	 * @return the first of the types the file is of, {@code null} for none
	 */
	static <T extends IRichTextImageType> T detect(byte[] head, Collection<T> types)
	{
		if (head == null || types == null)
		{
			return null;
		}
		for (T type : types)
		{
			if (type != null && type.matches(head))
			{
				return type;
			}
		}
		return null;
	}
}
