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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * The width and the height of the images, read from the headers of their files, as the browser
 * reads them.
 */
class RichTextImageHeadersTest
{
	/** The bytes of a file, from strings in ASCII and from numbers each a byte. */
	private static byte[] bytes(Object... parts)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (Object part : parts)
		{
			if (part instanceof String text)
			{
				out.writeBytes(text.getBytes(StandardCharsets.US_ASCII));
			}
			else if (part instanceof int[] values)
			{
				for (int value : values)
				{
					out.write(value);
				}
			}
			else
			{
				out.write((Integer)part);
			}
		}
		return out.toByteArray();
	}

	private static int[] be32(int value)
	{
		return new int[] { value >>> 24, (value >> 16) & 0xff, (value >> 8) & 0xff, value & 0xff };
	}

	private static int[] le16(int value)
	{
		return new int[] { value & 0xff, value >> 8 };
	}

	private static int[] le24(int value)
	{
		return new int[] { value & 0xff, (value >> 8) & 0xff, value >> 16 };
	}

	private static RichTextImageDimensions read(RichTextImageType type, byte[] bytes)
		throws IOException
	{
		return type.readDimensions(new ByteArrayInputStream(bytes));
	}

	private static RichTextImageDimensions of(int width, int height)
	{
		return new RichTextImageDimensions(width, height);
	}

	@Test
	void readsAPngAndAGif() throws IOException
	{
		byte[] png = bytes(new int[] { 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n' }, be32(13),
			"IHDR", be32(640), be32(480), new int[9]);
		assertEquals(of(640, 480), read(RichTextImageType.PNG, png));
		// a frame beyond the screen widens it
		byte[] gif = bytes("GIF89a", le16(10), le16(10), 0x80, 0, 0, new int[6], 0x21, 0xf9, 4,
			new int[4], 0, 0x2c, le16(5), le16(0), le16(20), le16(8), 0, 2, 2, 0, 0, 0, 0x3b);
		assertEquals(of(25, 10), read(RichTextImageType.GIF, gif));
	}

	@Test
	void readsTheThreeKindsOfWebP() throws IOException
	{
		assertEquals(of(1920, 1080), read(RichTextImageType.WEBP, bytes("RIFF", new int[4],
			"WEBPVP8X", new int[8], le24(1919), le24(1079))));
		int bits = (300 - 1) | ((200 - 1) << 14);
		assertEquals(of(300, 200), read(RichTextImageType.WEBP, bytes("RIFF", new int[4],
			"WEBPVP8L", new int[4], 0x2f, bits & 0xff, (bits >> 8) & 0xff, (bits >> 16) & 0xff,
			bits >>> 24, new int[5])));
		assertEquals(of(64, 32), read(RichTextImageType.WEBP, bytes("RIFF", new int[4],
			"WEBPVP8 ", new int[7], 0x9d, 0x01, 0x2a, le16(64), le16(32))));
	}

	@Test
	void readsAJpegTurnedAsItsExifSays() throws IOException
	{
		byte[] turned = bytes(0xff, 0xd8, 0xff, 0xe1, 0, 34, "Exif", 0, 0, "MM", 0, 42, be32(8),
			new int[] { 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, 6, 0, 0 }, be32(0),
			new int[] { 0xff, 0xc0, 0, 17, 8, 0x01, 0xe0, 0x02, 0x80 }, new int[10]);
		assertEquals(of(480, 640), read(RichTextImageType.JPEG, turned));
		byte[] little = bytes(0xff, 0xd8, 0xff, 0xe1, 0, 34, "Exif", 0, 0, "II", 42, 0,
			new int[] { 8, 0, 0, 0 }, new int[] { 1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0, 1, 0, 0, 0 },
			new int[4], new int[] { 0xff, 0xc0, 0, 17, 8, 0x01, 0xe0, 0x02, 0x80 }, new int[10]);
		assertEquals(of(640, 480), read(RichTextImageType.JPEG, little), "orientation 1");
		byte[] progressive = bytes(0xff, 0xd8, 0xff, 0xdb, 0, 4, 0, 0, 0xff, 0xff, 0xc2, 0, 17, 8,
			0, 10, 0, 20, new int[12]);
		assertEquals(of(20, 10), read(RichTextImageType.JPEG, progressive));
	}

	@Test
	void readsAnAvifByItsProperties() throws IOException
	{
		byte[] avif = bytes(be32(20), "ftypavif", be32(0), "mif1", be32(8), "free", be32(57),
			"meta", be32(0), be32(45), "iprp", be32(37), "ipco", be32(20), "ispe", be32(0),
			be32(4000), be32(3000), be32(9), "irot", 1);
		assertEquals(of(3000, 4000), read(RichTextImageType.AVIF, avif));
	}

	@Test
	void readsNothingOfAHeaderThatTellsNothing() throws IOException
	{
		assertNull(read(RichTextImageType.PNG, bytes("not a png")));
		assertNull(read(RichTextImageType.PNG, bytes(new int[] { 0x89, 'P', 'N', 'G', '\r', '\n',
			0x1a, '\n' }, be32(13), "IHDR", be32(0), be32(480))));
		assertNull(read(RichTextImageType.GIF, bytes("GIF89a")));
		assertNull(read(RichTextImageType.GIF, bytes("GIF89a", le16(10), le16(10), 0, 0, 0, 7)));
		assertNull(read(RichTextImageType.WEBP, bytes("RIFF", new int[4], "WEBPVP8Z",
			new int[14])));
		assertNull(read(RichTextImageType.JPEG, bytes(0xff, 0xd8, 0xff, 0xda, 0, 2)));
		assertNull(read(RichTextImageType.JPEG, bytes(0xff, 0xd8, 0xff, 0xc0, 0, 17, 8, 0, 0, 0,
			20)), "a height of 0, told later");
		assertNull(read(RichTextImageType.AVIF, bytes(be32(20), "ftypavif", be32(0), "mif1")));
		assertNull(read(RichTextImageType.AVIF, bytes(be32(0x7fffffff), "meta")));
	}
}
