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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A type of the images a user uploads into a {@link RichTextEditor}: PNG, JPEG, GIF or WebP, told by
 * the first bytes of the file, not by the name or the type the browser gives, which anyone can
 * set. No SVG: an SVG is a document that runs scripts when opened by itself.
 *
 * @since 11.0.0
 */
public enum RichTextImageType
{
	PNG("image/png", "png", "PNG"),
	JPEG("image/jpeg", "jpg", "JPEG"),
	GIF("image/gif", "gif", "GIF"),
	WEBP("image/webp", "webp", "WebP");

	/** How many bytes of a file {@link #detect(byte[])} reads at most. */
	public static final int HEAD = 12;

	private static final byte[] PNG_SIGNATURE = { (byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1a,
			'\n' };

	private final String mimeType;

	private final String extension;

	private final String label;

	RichTextImageType(String mimeType, String extension, String label)
	{
		this.mimeType = mimeType;
		this.extension = extension;
		this.label = label;
	}

	/**
	 * @return the media type the image is served as, {@code image/png} for example
	 */
	public String getMimeType()
	{
		return mimeType;
	}

	/**
	 * @return the extension a file of the type takes, without the dot
	 */
	public String getExtension()
	{
		return extension;
	}

	/**
	 * @return what the messages call the type
	 */
	public String getLabel()
	{
		return label;
	}

	/**
	 * @param head
	 *            the first bytes of a file, {@value #HEAD} or more of them
	 * @return the type of the image the bytes start, {@code null} for none of these types
	 */
	public static RichTextImageType detect(byte[] head)
	{
		if (head == null)
		{
			return null;
		}
		if (startsWith(head, PNG_SIGNATURE))
		{
			return PNG;
		}
		if (head.length >= 3 && (head[0] & 0xff) == 0xff && (head[1] & 0xff) == 0xd8 &&
			(head[2] & 0xff) == 0xff)
		{
			return JPEG;
		}
		if (startsWith(head, ascii("GIF87a")) || startsWith(head, ascii("GIF89a")))
		{
			return GIF;
		}
		if (head.length >= 12 && startsWith(head, ascii("RIFF")) &&
			Arrays.equals(Arrays.copyOfRange(head, 8, 12), ascii("WEBP")))
		{
			return WEBP;
		}
		return null;
	}

	private static byte[] ascii(String text)
	{
		return text.getBytes(StandardCharsets.US_ASCII);
	}

	private static boolean startsWith(byte[] bytes, byte[] start)
	{
		return bytes.length >= start.length &&
			Arrays.equals(Arrays.copyOf(bytes, start.length), start);
	}
}
