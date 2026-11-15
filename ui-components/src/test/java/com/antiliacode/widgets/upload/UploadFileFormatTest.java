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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class UploadFileFormatTest
{
	/**
	 * @return a ZIP of entries, each a name and its text; the first stored as it is, as the
	 *         mimetype of an OpenDocument is
	 */
	static byte[] zip(String... entries) throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes))
		{
			for (int i = 0; i < entries.length; i += 2)
			{
				byte[] data = entries[i + 1].getBytes(StandardCharsets.UTF_8);
				ZipEntry entry = new ZipEntry(entries[i]);
				if (i == 0)
				{
					CRC32 crc = new CRC32();
					crc.update(data);
					entry.setMethod(ZipEntry.STORED);
					entry.setSize(data.length);
					entry.setCompressedSize(data.length);
					entry.setCrc(crc.getValue());
				}
				zip.putNextEntry(entry);
				zip.write(data);
				zip.closeEntry();
			}
		}
		return bytes.toByteArray();
	}

	private static byte[] head(byte[] file)
	{
		return Arrays.copyOf(file, Math.min(file.length, IUploadFileFormat.HEAD));
	}

	@Test
	void tellsTheFormatsByTheirBytes() throws IOException
	{
		assertEquals(UploadFileFormat.PDF, UploadFileFormat.detect(
			"%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII), "report.txt"));
		assertEquals(UploadFileFormat.DOCX, UploadFileFormat.detect(head(zip(
			"[Content_Types].xml", "<Types/>", "word/document.xml", "<w:document/>")),
			"letter.zip"), "the bytes, not the name");
		assertEquals(UploadFileFormat.XLSX, UploadFileFormat.detect(head(zip(
			"[Content_Types].xml", "<Types/>", "xl/workbook.xml", "<workbook/>")), null));
		assertEquals(UploadFileFormat.PPTX, UploadFileFormat.detect(head(zip(
			"[Content_Types].xml", "<Types/>", "ppt/presentation.xml", "<p/>")), null));
		assertEquals(UploadFileFormat.ODT, UploadFileFormat.detect(head(zip(
			"mimetype", "application/vnd.oasis.opendocument.text", "content.xml", "<c/>")), null));
		assertEquals(UploadFileFormat.ODS, UploadFileFormat.detect(head(zip(
			"mimetype", "application/vnd.oasis.opendocument.spreadsheet", "content.xml", "<c/>")),
			null));
		// a template of a text is none of the formats but a ZIP
		assertEquals(UploadFileFormat.ZIP, UploadFileFormat.detect(head(zip(
			"mimetype", "application/vnd.oasis.opendocument.text-template", "c.xml", "<c/>")),
			null));
		assertEquals(UploadFileFormat.ZIP, UploadFileFormat.detect(head(zip("notes.txt", "a")),
			"notes.zip"));
	}

	@Test
	void takesTextsByTheirExtensionAmongThoseTheBytesAllow()
	{
		byte[] text = "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8);
		assertEquals(UploadFileFormat.CSV, UploadFileFormat.detect(text, "table.CSV"));
		assertEquals(UploadFileFormat.MARKDOWN, UploadFileFormat.detect(text, "notes.md"));
		assertEquals(UploadFileFormat.TXT, UploadFileFormat.detect(text, "notes"));
		assertEquals(UploadFileFormat.TXT, UploadFileFormat.detect(text, "evil.exe"),
			"the extension chooses among the formats the bytes allow alone");
		assertEquals(UploadFileFormat.TXT, UploadFileFormat.detect("Grüße".getBytes(
			StandardCharsets.UTF_8), "a.txt"));
		// a character cut at the end of the head
		byte[] cut = Arrays.copyOf("aé".getBytes(StandardCharsets.UTF_8), 2);
		assertEquals(UploadFileFormat.TXT, UploadFileFormat.detect(cut, "a.txt"));

		assertNull(UploadFileFormat.detect(new byte[] { 'a', 0, 'b' }, "a.txt"));
		assertNull(UploadFileFormat.detect(new byte[] { 'a', (byte)0xc3, 'b' }, "a.txt"));
		assertNull(UploadFileFormat.detect(new byte[] { 'M', 'Z', (byte)0x90, 0 }, "setup.exe"));
	}

	@Test
	void checksATextWhole() throws IOException
	{
		byte[] text = new byte[IUploadFileFormat.HEAD + 100];
		Arrays.fill(text, (byte)'a');
		assertTrue(UploadFileFormat.TXT.accepts(new ByteArrayInputStream(text)));
		// a NUL beyond the head
		text[IUploadFileFormat.HEAD + 50] = 0;
		assertTrue(UploadFileFormat.TXT.matches(head(text)));
		assertFalse(UploadFileFormat.TXT.accepts(new ByteArrayInputStream(text)));
		// a character cut at the end of the file
		byte[] cut = Arrays.copyOf("aé".getBytes(StandardCharsets.UTF_8), 2);
		assertFalse(UploadFileFormat.CSV.accepts(new ByteArrayInputStream(cut)));
		// characters across the reads
		byte[] long_ = "é".repeat(20000).getBytes(StandardCharsets.UTF_8);
		assertTrue(UploadFileFormat.MARKDOWN.accepts(new ByteArrayInputStream(long_)));
		assertTrue(UploadFileFormat.PDF.accepts(new ByteArrayInputStream(new byte[] { 0 })),
			"a format without a whole check takes any");
	}

	@Test
	void opensOnlyThePdfInTheBrowser()
	{
		assertEquals(List.of(UploadFileFormat.PDF), UploadFileFormat.ALL.stream()
			.filter(UploadFileFormat::isInline).toList());
		assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			UploadFileFormat.DOCX.getMimeType());
		assertEquals("md", UploadFileFormat.MARKDOWN.getExtension());
	}
}
