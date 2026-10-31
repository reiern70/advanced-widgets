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

/**
 * Reads the width and the height of the images of {@link RichTextImageType} from the headers of
 * their files, without decoding the pictures: a small file may hold a picture of many millions of
 * pixels, which decoding would take the memory of. Each answers {@code null} for a file whose
 * header does not tell them, or tells nonsense, a width of 0 for example.
 */
final class RichTextImageHeaders
{
	/** The most bytes of the box of the metadata of an AVIF read, besides its pictures. */
	private static final int MAX_AVIF_META = 1024 * 1024;

	/** The most bytes of a segment of EXIF read for the orientation of a JPEG. */
	private static final int MAX_EXIF = 64 * 1024;

	private RichTextImageHeaders()
	{
	}

	/**
	 * @return the dimensions of a PNG, from its first chunk, IHDR
	 */
	static RichTextImageDimensions png(InputStream input) throws IOException
	{
		byte[] head = input.readNBytes(24);
		if (head.length < 24 || !ascii(head, 12, "IHDR"))
		{
			return null;
		}
		return dimensions(int32(head, 16), int32(head, 20));
	}

	/**
	 * @return the dimensions of a GIF: of its screen, or of a frame going beyond it, which the
	 *         browser makes room for
	 */
	static RichTextImageDimensions gif(InputStream input) throws IOException
	{
		byte[] head = input.readNBytes(13);
		if (head.length < 13)
		{
			return null;
		}
		long width = uint16le(head, 6);
		long height = uint16le(head, 8);
		int flags = head[10] & 0xff;
		if ((flags & 0x80) != 0)
		{
			skip(input, 3L << ((flags & 0x07) + 1));
		}
		while (true)
		{
			int block = input.read();
			if (block == 0x2c)
			{
				byte[] descriptor = input.readNBytes(9);
				if (descriptor.length < 9)
				{
					return null;
				}
				width = Math.max(width, uint16le(descriptor, 0) + uint16le(descriptor, 4));
				height = Math.max(height, uint16le(descriptor, 2) + uint16le(descriptor, 6));
				int local = descriptor[8] & 0xff;
				if ((local & 0x80) != 0)
				{
					skip(input, 3L << ((local & 0x07) + 1));
				}
				// the size of the codes of the picture, then the picture in blocks
				if (input.read() < 0 || !skipBlocks(input))
				{
					return null;
				}
			}
			else if (block == 0x21)
			{
				// an extension: its label, then its blocks
				if (input.read() < 0 || !skipBlocks(input))
				{
					return null;
				}
			}
			else if (block == 0x3b || block < 0)
			{
				// the end, or a file cut short: what was read
				return dimensions(width, height);
			}
			else
			{
				return null;
			}
		}
	}

	/**
	 * @return the dimensions of a WebP: of its canvas, or of its picture, lossy or lossless
	 */
	static RichTextImageDimensions webp(InputStream input) throws IOException
	{
		byte[] head = input.readNBytes(30);
		if (head.length < 30 || !ascii(head, 0, "RIFF") || !ascii(head, 8, "WEBP"))
		{
			return null;
		}
		if (ascii(head, 12, "VP8X"))
		{
			return dimensions(uint24le(head, 24) + 1, uint24le(head, 27) + 1);
		}
		if (ascii(head, 12, "VP8L"))
		{
			if ((head[20] & 0xff) != 0x2f)
			{
				return null;
			}
			long bits = uint16le(head, 21) | (uint16le(head, 23) << 16);
			return dimensions((bits & 0x3fff) + 1, ((bits >> 14) & 0x3fff) + 1);
		}
		if (ascii(head, 12, "VP8 "))
		{
			if ((head[23] & 0xff) != 0x9d || (head[24] & 0xff) != 0x01 || (head[25] & 0xff) != 0x2a)
			{
				return null;
			}
			return dimensions(uint16le(head, 26) & 0x3fff, uint16le(head, 28) & 0x3fff);
		}
		return null;
	}

	/**
	 * @return the dimensions of a JPEG, from its frame, turned when its EXIF says the camera was
	 */
	static RichTextImageDimensions jpeg(InputStream input) throws IOException
	{
		if (input.read() != 0xff || input.read() != 0xd8)
		{
			return null;
		}
		boolean turned = false;
		while (true)
		{
			int marker = input.read();
			if (marker != 0xff)
			{
				return null;
			}
			while (marker == 0xff)
			{
				marker = input.read();
			}
			if (marker < 0 || marker == 0xd9 || marker == 0xda)
			{
				// the end, or the picture, before any frame
				return null;
			}
			if (marker == 0x01 || marker == 0xd8 || marker >= 0xd0 && marker <= 0xd7)
			{
				continue;
			}
			byte[] length = input.readNBytes(2);
			if (length.length < 2 || uint16be(length, 0) < 2)
			{
				return null;
			}
			int size = (int)uint16be(length, 0) - 2;
			if (marker >= 0xc0 && marker <= 0xcf && marker != 0xc4 && marker != 0xc8 &&
				marker != 0xcc)
			{
				byte[] frame = input.readNBytes(5);
				if (frame.length < 5)
				{
					return null;
				}
				RichTextImageDimensions dimensions = dimensions(uint16be(frame, 3),
					uint16be(frame, 1));
				return dimensions != null && turned ? dimensions.turned() : dimensions;
			}
			if (marker == 0xe1 && size <= MAX_EXIF)
			{
				byte[] segment = input.readNBytes(size);
				if (segment.length < size)
				{
					return null;
				}
				turned = turned || exifTurned(segment);
			}
			else
			{
				skip(input, size);
			}
		}
	}

	/**
	 * @return whether the EXIF of a segment says the camera was turned a quarter: orientations 5
	 *         to 8, which the browser shows turned
	 */
	private static boolean exifTurned(byte[] segment)
	{
		if (segment.length < 14 || !ascii(segment, 0, "Exif") || segment[4] != 0 ||
			segment[5] != 0)
		{
			return false;
		}
		byte[] tiff = Arrays.copyOfRange(segment, 6, segment.length);
		boolean little;
		if (ascii(tiff, 0, "II"))
		{
			little = true;
		}
		else if (ascii(tiff, 0, "MM"))
		{
			little = false;
		}
		else
		{
			return false;
		}
		long ifd = little ? uint32le(tiff, 4) : int32(tiff, 4) & 0xffffffffL;
		if (ifd < 8 || ifd + 2 > tiff.length)
		{
			return false;
		}
		int entries = (int)(little ? uint16le(tiff, (int)ifd) : uint16be(tiff, (int)ifd));
		for (int i = 0; i < entries; i++)
		{
			int entry = (int)ifd + 2 + i * 12;
			if (entry + 12 > tiff.length)
			{
				return false;
			}
			long tag = little ? uint16le(tiff, entry) : uint16be(tiff, entry);
			if (tag == 0x0112)
			{
				long orientation = little ? uint16le(tiff, entry + 8) : uint16be(tiff, entry + 8);
				return orientation >= 5 && orientation <= 8;
			}
		}
		return false;
	}

	/**
	 * @return the dimensions of an AVIF: the largest its properties give a picture of it, turned
	 *         when one of them says so
	 */
	static RichTextImageDimensions avif(InputStream input) throws IOException
	{
		// the boxes of the file, up to the metadata, which tells the dimensions
		while (true)
		{
			byte[] header = input.readNBytes(8);
			if (header.length < 8)
			{
				return null;
			}
			long size = int32(header, 0) & 0xffffffffL;
			int read = 8;
			if (size == 1)
			{
				byte[] large = input.readNBytes(8);
				if (large.length < 8)
				{
					return null;
				}
				size = ((int32(large, 0) & 0xffffffffL) << 32) | (int32(large, 4) & 0xffffffffL);
				read = 16;
			}
			if (size == 0 || size < read)
			{
				// to the end of the file, or nonsense: no metadata after it
				return null;
			}
			long body = size - read;
			if (ascii(header, 4, "meta"))
			{
				if (body > MAX_AVIF_META)
				{
					return null;
				}
				byte[] meta = input.readNBytes((int)body);
				if (meta.length < body || meta.length < 4)
				{
					return null;
				}
				Avif found = new Avif();
				// a full box: its version and flags first
				found.read(meta, 4, meta.length);
				RichTextImageDimensions dimensions = dimensions(found.width, found.height);
				return dimensions != null && found.turned ? dimensions.turned() : dimensions;
			}
			skip(input, body);
		}
	}

	/** What the properties of an AVIF tell: the largest picture, and whether it is turned. */
	private static final class Avif
	{
		long width;

		long height;

		boolean turned;

		/**
		 * Reads the boxes between two offsets, and those in the boxes holding others.
		 */
		void read(byte[] bytes, int from, int to)
		{
			int at = from;
			while (at + 8 <= to)
			{
				long size = int32(bytes, at) & 0xffffffffL;
				int header = 8;
				if (size == 1)
				{
					if (at + 16 > to)
					{
						return;
					}
					size = ((int32(bytes, at + 8) & 0xffffffffL) << 32) |
						(int32(bytes, at + 12) & 0xffffffffL);
					header = 16;
				}
				else if (size == 0)
				{
					size = to - at;
				}
				if (size < header || at + size > to)
				{
					return;
				}
				int end = (int)(at + size);
				int body = at + header;
				if (ascii(bytes, at + 4, "iprp") || ascii(bytes, at + 4, "ipco"))
				{
					read(bytes, body, end);
				}
				else if (ascii(bytes, at + 4, "ispe") && body + 12 <= end)
				{
					// a full box: its version and flags, then the width and the height
					width = Math.max(width, int32(bytes, body + 4) & 0xffffffffL);
					height = Math.max(height, int32(bytes, body + 8) & 0xffffffffL);
				}
				else if (ascii(bytes, at + 4, "irot") && body < end)
				{
					turned = turned || (bytes[body] & 0x01) != 0;
				}
				at = end;
			}
		}
	}

	/**
	 * @return the dimensions, {@code null} for a width or a height of 0, or beyond an int
	 */
	private static RichTextImageDimensions dimensions(long width, long height)
	{
		if (width <= 0 || height <= 0 || width > Integer.MAX_VALUE || height > Integer.MAX_VALUE)
		{
			return null;
		}
		return new RichTextImageDimensions((int)width, (int)height);
	}

	/**
	 * Skips the blocks of a GIF, each its length and its bytes, up to the empty one.
	 *
	 * @return whether the empty block came
	 */
	private static boolean skipBlocks(InputStream input) throws IOException
	{
		while (true)
		{
			int length = input.read();
			if (length < 0)
			{
				return false;
			}
			if (length == 0)
			{
				return true;
			}
			skip(input, length);
		}
	}

	private static void skip(InputStream input, long count) throws IOException
	{
		long left = count;
		while (left > 0)
		{
			long skipped = input.skip(left);
			if (skipped <= 0)
			{
				if (input.read() < 0)
				{
					return;
				}
				skipped = 1;
			}
			left -= skipped;
		}
	}

	private static boolean ascii(byte[] bytes, int at, String text)
	{
		byte[] expected = text.getBytes(StandardCharsets.US_ASCII);
		return at >= 0 && at + expected.length <= bytes.length &&
			Arrays.equals(Arrays.copyOfRange(bytes, at, at + expected.length), expected);
	}

	private static int int32(byte[] bytes, int at)
	{
		return ((bytes[at] & 0xff) << 24) | ((bytes[at + 1] & 0xff) << 16) |
			((bytes[at + 2] & 0xff) << 8) | (bytes[at + 3] & 0xff);
	}

	private static long uint32le(byte[] bytes, int at)
	{
		return uint16le(bytes, at) | (uint16le(bytes, at + 2) << 16);
	}

	private static long uint24le(byte[] bytes, int at)
	{
		return (bytes[at] & 0xff) | ((bytes[at + 1] & 0xff) << 8) | ((bytes[at + 2] & 0xff) << 16);
	}

	private static long uint16le(byte[] bytes, int at)
	{
		return (bytes[at] & 0xff) | ((bytes[at + 1] & 0xff) << 8);
	}

	private static long uint16be(byte[] bytes, int at)
	{
		return ((bytes[at] & 0xff) << 8) | (bytes[at + 1] & 0xff);
	}
}
