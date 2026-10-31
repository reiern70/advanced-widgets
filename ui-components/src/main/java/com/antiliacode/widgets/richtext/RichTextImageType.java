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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * The types of the images a user uploads into a {@link RichTextEditor} built in: PNG, JPEG, GIF,
 * WebP and AVIF, told by the first bytes of the file, not by the name or the type the browser
 * gives, which anyone can set. No SVG: an SVG is a document that runs scripts when opened by
 * itself. An application adds types of its own with {@link IRichTextImageType}.
 *
 * @since 11.0.0
 */
public enum RichTextImageType implements IRichTextImageType
{
	PNG("image/png", "png", "PNG")
	{
		@Override
		public boolean matches(byte[] head)
		{
			return startsWith(head, PNG_SIGNATURE);
		}

		@Override
		public RichTextImageDimensions readDimensions(InputStream input) throws IOException
		{
			return RichTextImageHeaders.png(input);
		}
	},
	JPEG("image/jpeg", "jpg", "JPEG")
	{
		@Override
		public boolean matches(byte[] head)
		{
			return head != null && head.length >= 3 && (head[0] & 0xff) == 0xff &&
				(head[1] & 0xff) == 0xd8 && (head[2] & 0xff) == 0xff;
		}

		@Override
		public RichTextImageDimensions readDimensions(InputStream input) throws IOException
		{
			return RichTextImageHeaders.jpeg(input);
		}
	},
	GIF("image/gif", "gif", "GIF")
	{
		@Override
		public boolean matches(byte[] head)
		{
			return startsWith(head, ascii("GIF87a")) || startsWith(head, ascii("GIF89a"));
		}

		@Override
		public RichTextImageDimensions readDimensions(InputStream input) throws IOException
		{
			return RichTextImageHeaders.gif(input);
		}
	},
	WEBP("image/webp", "webp", "WebP")
	{
		@Override
		public boolean matches(byte[] head)
		{
			return head != null && head.length >= 12 && startsWith(head, ascii("RIFF")) &&
				Arrays.equals(Arrays.copyOfRange(head, 8, 12), ascii("WEBP"));
		}

		@Override
		public RichTextImageDimensions readDimensions(InputStream input) throws IOException
		{
			return RichTextImageHeaders.webp(input);
		}
	},
	/**
	 * AVIF, a picture of AV1 in a file of ISO's media format: its first box, ftyp, names the brand
	 * avif, or avis for a sequence, as the main one or among the compatible ones.
	 *
	 * @since 10.11.0
	 */
	AVIF("image/avif", "avif", "AVIF")
	{
		@Override
		public boolean matches(byte[] head)
		{
			if (head == null || head.length < 12 || !Arrays.equals(Arrays.copyOfRange(head, 4, 8),
				ascii("ftyp")))
			{
				return false;
			}
			long size = ((head[0] & 0xffL) << 24) | ((head[1] & 0xff) << 16) |
				((head[2] & 0xff) << 8) | (head[3] & 0xff);
			int end = (int)Math.min(Math.max(size, 12), head.length);
			// the main brand at 8, its version at 12, then the compatible brands
			for (int at = 8; at + 4 <= end; at += at == 8 ? 8 : 4)
			{
				byte[] brand = Arrays.copyOfRange(head, at, at + 4);
				if (Arrays.equals(brand, ascii("avif")) || Arrays.equals(brand, ascii("avis")))
				{
					return true;
				}
			}
			return false;
		}

		@Override
		public RichTextImageDimensions readDimensions(InputStream input) throws IOException
		{
			return RichTextImageHeaders.avif(input);
		}
	};

	/** The types built in, in their order. */
	public static final List<RichTextImageType> ALL = List.of(values());

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

	@Override
	public String getMimeType()
	{
		return mimeType;
	}

	@Override
	public String getExtension()
	{
		return extension;
	}

	@Override
	public String getLabel()
	{
		return label;
	}

	/**
	 * @param head
	 *            the first bytes of a file, {@link IRichTextImageType#HEAD} or fewer of them
	 * @return the type built in of the image the bytes start, {@code null} for none of them
	 */
	public static RichTextImageType detect(byte[] head)
	{
		return IRichTextImageType.detect(head, ALL);
	}

	private static byte[] ascii(String text)
	{
		return text.getBytes(StandardCharsets.US_ASCII);
	}

	private static boolean startsWith(byte[] bytes, byte[] start)
	{
		return bytes != null && bytes.length >= start.length &&
			Arrays.equals(Arrays.copyOf(bytes, start.length), start);
	}
}
