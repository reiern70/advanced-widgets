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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.upload.IUploadFileFormat;

/**
 * A file a store gives {@link RichTextFiles the resource of the uploads} to serve: its format, the
 * one its bytes were found to be when it was uploaded, which it is served as, never the type the
 * browser gave; the name of the file the user uploaded, which a browser saving it takes; its size
 * and when it last changed, for the caches; and its bytes.
 *
 * @see IRichTextImageStore#open(String)
 * @since 10.11.0
 */
public final class RichTextServedFile
{
	/** Opens the bytes of a file. */
	@FunctionalInterface
	public interface Content
	{
		/**
		 * @return the bytes of the file, from the first; closed by the resource
		 * @throws IOException
		 *             when the file could not be read
		 */
		InputStream open() throws IOException;
	}

	private final IUploadFileFormat format;

	private final String fileName;

	private final long size;

	private final Instant modified;

	private final Content content;

	/**
	 * @param format
	 *            the format of the file, as its bytes said when it was uploaded
	 * @param fileName
	 *            the name of the file the user uploaded, the browser's, which is cleaned before it
	 *            goes into a header; {@code null} for the name the file is served at
	 * @param size
	 *            its size in bytes, {@code -1} for unknown
	 * @param modified
	 *            when it last changed, {@code null} for unknown
	 * @param content
	 *            opens its bytes
	 */
	public RichTextServedFile(IUploadFileFormat format, String fileName, long size,
		Instant modified, Content content)
	{
		this.format = Args.notNull(format, "format");
		this.fileName = fileName;
		this.size = size;
		this.modified = modified;
		this.content = Args.notNull(content, "content");
	}

	/**
	 * @param file
	 *            a file on disk
	 * @param format
	 *            its format, as its bytes said when it was uploaded
	 * @param fileName
	 *            the name of the file the user uploaded, {@code null} for the name it is served at
	 * @return the file, its size and its time those of the file on disk
	 * @throws IOException
	 *             when the file could not be read
	 */
	public static RichTextServedFile of(Path file, IUploadFileFormat format, String fileName)
		throws IOException
	{
		Args.notNull(file, "file");
		String path = file.toAbsolutePath().toString();
		return new RichTextServedFile(format, fileName, Files.size(file),
			Files.getLastModifiedTime(file).toInstant(),
			() -> Files.newInputStream(Path.of(path)));
	}

	/**
	 * @return the format of the file, which it is served as
	 */
	public IUploadFileFormat getFormat()
	{
		return format;
	}

	/**
	 * @return the name of the file the user uploaded, {@code null} for the name it is served at
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * @return its size in bytes, {@code -1} for unknown
	 */
	public long getSize()
	{
		return size;
	}

	/**
	 * @return when it last changed, {@code null} for unknown
	 */
	public Instant getModified()
	{
		return modified;
	}

	/**
	 * @return opens its bytes
	 */
	public Content getContent()
	{
		return content;
	}
}
