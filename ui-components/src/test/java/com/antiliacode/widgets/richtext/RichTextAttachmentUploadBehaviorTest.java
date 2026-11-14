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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antiliacode.widgets.upload.IUploadFileFormat;
import com.antiliacode.widgets.upload.UploadFileFormat;
import com.antiliacode.widgets.upload.UploadScan;
import com.github.openjson.JSONObject;

class RichTextAttachmentUploadBehaviorTest extends WicketTestCase
{
	private static final byte[] PDF = "%PDF-1.7\n1 0 obj".getBytes(StandardCharsets.US_ASCII);

	@TempDir
	Path folder;

	/** What the store of the tests kept: the name, the format and the bytes of each file. */
	private static final List<Object[]> KEPT = new ArrayList<>();

	private static final List<String> DELETED = new ArrayList<>();

	private static String keep(FileUpload file, IUploadFileFormat format)
	{
		KEPT.add(new Object[] { file.getClientFileName(), format, file.getBytes() });
		return "/files/" + KEPT.size() + "." + format.getExtension();
	}

	@BeforeEach
	void forget()
	{
		KEPT.clear();
		DELETED.clear();
	}

	@Test
	void keepsAFileOfAFormatItsBytesSay() throws IOException
	{
		TestPage page = new TestPage(RichTextAttachmentUploadBehaviorTest::keep);
		tester.startPage(page);

		assertEquals("{\"url\":\"/files/1.pdf\"}", upload(page, "report.txt", PDF).toString());
		assertEquals(UploadFileFormat.PDF, KEPT.get(0)[1]);
		assertArrayEquals(PDF, (byte[])KEPT.get(0)[2]);
		assertEquals("/files/2.csv", upload(page, "table.csv", "a,b\n".getBytes(
			StandardCharsets.UTF_8)).getString("url"));
		assertTrue(page.upload.isUploaded("/files/2.csv"));
	}

	@Test
	void refusesAFileOfAnotherFormatOrTooLarge() throws IOException
	{
		TestPage page = new TestPage(RichTextAttachmentUploadBehaviorTest::keep);
		page.upload.setFormats(EnumSet.of(UploadFileFormat.PDF, UploadFileFormat.TXT))
			.setMaxSize(Bytes.kilobytes(1));
		tester.startPage(page);

		assertEquals("setup.exe is not a file of a type the editor takes: PDF, TXT.",
			upload(page, "setup.exe", new byte[] { 'M', 'Z', 0, 0 }).getString("error"));
		// a text holding a NUL past its head
		byte[] text = new byte[IUploadFileFormat.HEAD + 10];
		java.util.Arrays.fill(text, (byte)'a');
		text[text.length - 1] = 0;
		page.upload.setMaxSize(Bytes.kilobytes(10));
		assertEquals("notes.txt is not a file of a type the editor takes: PDF, TXT.",
			upload(page, "notes.txt", text).getString("error"));
		page.upload.setMaxSize(Bytes.kilobytes(1));
		byte[] large = new byte[4096];
		System.arraycopy(PDF, 0, large, 0, PDF.length);
		assertEquals("large.pdf is larger than 1 KB.", upload(page, "large.pdf", large)
			.getString("error"));

		tester.getSession().setLocale(Locale.of("es"));
		assertEquals("setup.exe no es un archivo de un tipo que el editor acepte: PDF, TXT.",
			upload(page, "setup.exe", new byte[] { 'M', 'Z', 0, 0 }).getString("error"));
		assertTrue(KEPT.isEmpty());
	}

	@Test
	void keepsWhatTheScannerAcceptsOrCleaned() throws IOException
	{
		TestPage page = new TestPage(RichTextAttachmentUploadBehaviorTest::keep);
		page.upload.setScanner((file, format, locale) -> {
			String name = file.getClientFileName();
			if (name.startsWith("virus"))
			{
				return UploadScan.refuse(name + " holds a virus (" + locale.getLanguage() + ").");
			}
			if (name.startsWith("macro"))
			{
				return UploadScan.clean("%PDF-1.7 clean".getBytes(StandardCharsets.US_ASCII));
			}
			if (name.startsWith("other"))
			{
				// not a PDF any more
				return UploadScan.clean("plain".getBytes(StandardCharsets.US_ASCII));
			}
			return UploadScan.accept();
		});
		tester.startPage(page);

		assertEquals("virus.pdf holds a virus (en).", upload(page, "virus.pdf", PDF)
			.getString("error"));
		assertEquals("/files/1.pdf", upload(page, "macro.pdf", PDF).getString("url"));
		assertEquals("%PDF-1.7 clean", new String((byte[])KEPT.get(0)[2],
			StandardCharsets.US_ASCII));
		assertEquals("other.pdf could not be uploaded.", upload(page, "other.pdf", PDF)
			.getString("error"));
		assertEquals("/files/2.pdf", upload(page, "fine.pdf", PDF).getString("url"));
		assertEquals(2, KEPT.size());
	}

	@Test
	void deletesOnlyTheFilesItTook() throws IOException
	{
		TestPage page = new TestPage(new IRichTextAttachmentStore()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String store(FileUpload file, IUploadFileFormat format)
			{
				return keep(file, format);
			}

			@Override
			public void delete(String url)
			{
				DELETED.add(url);
			}
		});
		tester.startPage(page);
		String url = upload(page, "report.pdf", PDF).getString("url");

		tester.getRequest().getPostParameters().setParameterValue(
			RichTextImageUploadBehavior.REMOVED_PARAMETER, "[\"" + url + "\", \"/files/other\"]");
		tester.executeBehavior(page.upload);
		assertEquals("{\"deleted\":[\"" + url + "\"]}", tester.getLastResponseAsString());
		assertEquals(List.of(url), DELETED);
	}

	private JSONObject upload(TestPage page, String name, byte[] bytes) throws IOException
	{
		Path file = folder.resolve(name);
		Files.write(file, bytes);
		tester.getRequest().setUseMultiPartContentType(true);
		tester.getRequest().addFile(RichTextImageUploadBehavior.FILE_PARAMETER,
			new org.apache.wicket.util.file.File(file.toFile()), "application/octet-stream");
		tester.executeBehavior(page.upload);
		return new JSONObject(tester.getLastResponseAsString());
	}

	/** A page taking files for the editors of React and Svelte in it. */
	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final RichTextAttachmentUploadBehavior upload;

		TestPage(IRichTextAttachmentStore store)
		{
			upload = new RichTextAttachmentUploadBehavior(store);
			add(upload);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body></body></html>");
		}
	}
}
