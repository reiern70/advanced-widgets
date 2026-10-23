/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * The types of the images uploaded, told by their first bytes.
 */
class RichTextImageTypeTest
{
	private static byte[] bytes(int... values)
	{
		byte[] bytes = new byte[values.length];
		for (int i = 0; i < values.length; i++)
		{
			bytes[i] = (byte)values[i];
		}
		return bytes;
	}

	private static byte[] ascii(String text)
	{
		return text.getBytes(StandardCharsets.ISO_8859_1);
	}

	@Test
	void tellsTheTypeOfAnImageByItsFirstBytes()
	{
		assertEquals(RichTextImageType.PNG, RichTextImageType.detect(bytes(0x89, 'P', 'N', 'G',
			'\r', '\n', 0x1a, '\n', 0, 0, 0, 13)));
		assertEquals(RichTextImageType.JPEG, RichTextImageType.detect(bytes(0xff, 0xd8, 0xff, 0xe0,
			0, 16, 'J', 'F', 'I', 'F', 0, 1)));
		assertEquals(RichTextImageType.GIF, RichTextImageType.detect(ascii("GIF87a")));
		assertEquals(RichTextImageType.GIF, RichTextImageType.detect(ascii("GIF89a and more")));
		assertEquals(RichTextImageType.WEBP, RichTextImageType.detect(
			ascii("RIFF\u0010\u0000\u0000\u0000WEBPVP8 ")));
	}

	@Test
	void tellsNoTypeOfOtherFiles()
	{
		assertNull(RichTextImageType.detect(null));
		assertNull(RichTextImageType.detect(new byte[0]));
		assertNull(RichTextImageType.detect(ascii("<svg xmlns=\"http://www.w3.org/2000/svg\"/>")));
		assertNull(RichTextImageType.detect(ascii("RIFF\u0010\u0000\u0000\u0000WAVEfmt ")));
		assertNull(RichTextImageType.detect(ascii("RIFF")));
		assertNull(RichTextImageType.detect(bytes(0x89, 'P', 'N', 'G')));
		assertNull(RichTextImageType.detect(bytes(0xff, 0xd8)));
		assertNull(RichTextImageType.detect(ascii("GIF90a")));
	}

	@Test
	void namesItsMediaTypeAndExtension()
	{
		assertEquals("image/png", RichTextImageType.PNG.getMimeType());
		assertEquals("jpg", RichTextImageType.JPEG.getExtension());
		assertEquals("WebP", RichTextImageType.WEBP.getLabel());
		assertEquals("image/webp", RichTextImageType.WEBP.getMimeType());
		assertEquals(12, RichTextImageType.HEAD);
	}
}
