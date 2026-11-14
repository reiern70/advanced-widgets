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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The formats of the files users upload built in: PDF, the Office documents (DOCX, XLSX, PPTX),
 * OpenDocument (ODT, ODS), ZIP, and texts (TXT, CSV, Markdown), each told by the first bytes of
 * the file, not by the name or the type the browser gives, which anyone can set. A browser may
 * open a PDF in its window; the others it saves.
 * <p>
 * An Office document and an OpenDocument are ZIP files, told apart by the names of their first
 * entries, which {@link IUploadFileFormat#HEAD the head} holds: a document whose first entries say
 * nothing of its kind is a ZIP. A text has no signature ({@link #hasSignature()}): any head that is
 * UTF-8 without NUL may be one, a file of another format first, and the whole file is checked once uploaded ({@link #accepts(InputStream)}); its extension
 * chooses among TXT, CSV and Markdown, all served as text.
 *
 * @since 10.11.0
 */
public enum UploadFileFormat implements IUploadFileFormat
{
	/** Portable Document Format, which a browser opens. */
	PDF("application/pdf", "pdf", "PDF", true)
	{
		@Override
		public boolean matches(byte[] head)
		{
			return startsWith(head, ascii("%PDF-"));
		}
	},
	/** A document of Word, Office Open XML. */
	DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
		"DOCX", false)
	{
		@Override
		public boolean matches(byte[] head)
		{
			return isZip(head) && contains(head, ascii("word/"));
		}
	},
	/** A workbook of Excel, Office Open XML. */
	XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", "XLSX",
		false)
	{
		@Override
		public boolean matches(byte[] head)
		{
			return isZip(head) && contains(head, ascii("xl/"));
		}
	},
	/** A presentation of PowerPoint, Office Open XML. */
	PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx",
		"PPTX", false)
	{
		@Override
		public boolean matches(byte[] head)
		{
			return isZip(head) && contains(head, ascii("ppt/"));
		}
	},
	/** A text of OpenDocument. */
	ODT("application/vnd.oasis.opendocument.text", "odt", "ODT", false)
	{
		@Override
		public boolean matches(byte[] head)
		{
			return isOpenDocument(head, getMimeType());
		}
	},
	/** A spreadsheet of OpenDocument. */
	ODS("application/vnd.oasis.opendocument.spreadsheet", "ods", "ODS", false)
	{
		@Override
		public boolean matches(byte[] head)
		{
			return isOpenDocument(head, getMimeType());
		}
	},
	/** An archive, any ZIP but those of the formats above, which come first. */
	ZIP("application/zip", "zip", "ZIP", false)
	{
		@Override
		public boolean matches(byte[] head)
		{
			return isZip(head);
		}
	},
	/** Plain text, in UTF-8. */
	TXT("text/plain", "txt", "TXT", false)
	{
		@Override
		public boolean matches(byte[] head)
		{
			return isText(head);
		}

		@Override
		public boolean hasSignature()
		{
			return false;
		}

		@Override
		public boolean accepts(InputStream file) throws IOException
		{
			return isText(file);
		}
	},
	/** Values separated by commas, in UTF-8. */
	CSV("text/csv", "csv", "CSV", false)
	{
		@Override
		public boolean matches(byte[] head)
		{
			return isText(head);
		}

		@Override
		public boolean hasSignature()
		{
			return false;
		}

		@Override
		public boolean accepts(InputStream file) throws IOException
		{
			return isText(file);
		}
	},
	/** Markdown, in UTF-8. */
	MARKDOWN("text/markdown", "md", "Markdown", false)
	{
		@Override
		public boolean matches(byte[] head)
		{
			return isText(head);
		}

		@Override
		public boolean hasSignature()
		{
			return false;
		}

		@Override
		public boolean accepts(InputStream file) throws IOException
		{
			return isText(file);
		}
	};

	/**
	 * All of them, in the order a file is told by: the Office documents and the OpenDocuments
	 * before the ZIP they are, the texts last.
	 */
	public static final List<UploadFileFormat> ALL = List.of(values());

	private final String mimeType;

	private final String extension;

	private final String label;

	private final boolean inline;

	UploadFileFormat(String mimeType, String extension, String label, boolean inline)
	{
		this.mimeType = mimeType;
		this.extension = extension;
		this.label = label;
		this.inline = inline;
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

	@Override
	public boolean isInline()
	{
		return inline;
	}

	/**
	 * @param head
	 *            the first bytes of a file
	 * @return the first of the formats built in the file is of, its extension choosing among the
	 *         texts; {@code null} for none
	 */
	public static UploadFileFormat detect(byte[] head, String fileName)
	{
		return IUploadFileFormat.detect(head, fileName, ALL);
	}

	static boolean startsWith(byte[] head, byte[] prefix)
	{
		if (head == null || head.length < prefix.length)
		{
			return false;
		}
		for (int i = 0; i < prefix.length; i++)
		{
			if (head[i] != prefix[i])
			{
				return false;
			}
		}
		return true;
	}

	static boolean contains(byte[] head, byte[] part)
	{
		return indexOf(head, part, 0) >= 0;
	}

	private static int indexOf(byte[] head, byte[] part, int from)
	{
		if (head == null)
		{
			return -1;
		}
		outer: for (int i = Math.max(from, 0); i <= head.length - part.length; i++)
		{
			for (int j = 0; j < part.length; j++)
			{
				if (head[i + j] != part[j])
				{
					continue outer;
				}
			}
			return i;
		}
		return -1;
	}

	static byte[] ascii(String text)
	{
		return text.getBytes(StandardCharsets.US_ASCII);
	}

	/**
	 * @return whether the head is that of a ZIP: a local file header, or the end of an empty one
	 */
	static boolean isZip(byte[] head)
	{
		return startsWith(head, new byte[] { 'P', 'K', 3, 4 }) ||
			startsWith(head, new byte[] { 'P', 'K', 5, 6 });
	}

	/**
	 * @return whether the head is that of an OpenDocument of a media type: a ZIP whose first entry,
	 *         {@code mimetype}, stored as it is, holds the type, and nothing longer
	 */
	static boolean isOpenDocument(byte[] head, String mimeType)
	{
		if (!isZip(head))
		{
			return false;
		}
		byte[] wanted = ascii("mimetype" + mimeType);
		int at = indexOf(head, wanted, 0);
		if (at < 0)
		{
			return false;
		}
		int after = at + wanted.length;
		// text, not text-template nor text-master; the next entry follows, PK...
		return after >= head.length || head[after] != '-';
	}

	/**
	 * @return whether the head may be that of a text: UTF-8 without NUL, a character cut at its end
	 *         taken
	 */
	static boolean isText(byte[] head)
	{
		if (head == null)
		{
			return false;
		}
		for (byte b : head)
		{
			if (b == 0)
			{
				return false;
			}
		}
		CharsetDecoder decoder = decoder();
		CharBuffer out = CharBuffer.allocate(head.length + 1);
		CoderResult result = decoder.decode(ByteBuffer.wrap(head), out, false);
		// a character cut at the end of the head is underflow, not an error
		return !result.isError();
	}

	/**
	 * @return whether the whole file is a text: UTF-8 without NUL
	 */
	static boolean isText(InputStream file) throws IOException
	{
		CharsetDecoder decoder = decoder();
		byte[] buffer = new byte[8192];
		// room for what is left of a character cut at the end of a read, and as many chars
		ByteBuffer in = ByteBuffer.allocate(buffer.length + 8);
		CharBuffer out = CharBuffer.allocate(buffer.length + 8);
		int read;
		while ((read = file.read(buffer)) >= 0)
		{
			for (int i = 0; i < read; i++)
			{
				if (buffer[i] == 0)
				{
					return false;
				}
			}
			in.put(buffer, 0, read);
			in.flip();
			CoderResult result = decoder.decode(in, out, false);
			if (result.isError())
			{
				return false;
			}
			in.compact();
			out.clear();
		}
		in.flip();
		try
		{
			CoderResult result = decoder.decode(in, out, true);
			if (result.isError())
			{
				return false;
			}
			out.clear();
			return !decoder.flush(out).isError();
		}
		catch (IllegalStateException e)
		{
			return false;
		}
	}

	private static CharsetDecoder decoder()
	{
		return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);
	}
}
