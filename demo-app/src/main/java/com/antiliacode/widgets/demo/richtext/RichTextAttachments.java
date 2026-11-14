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
package com.antiliacode.widgets.demo.richtext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.lang.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.richtext.IRichTextAttachmentStore;
import com.antiliacode.widgets.richtext.RichTextFiles;
import com.antiliacode.widgets.richtext.RichTextServedFile;
import com.antiliacode.widgets.upload.IUploadFileFormat;
import com.antiliacode.widgets.upload.UploadFileFormat;

/**
 * Keeps the files users attach to the texts of the rich text editors of the demo in a folder, as
 * {@link RichTextImages} keeps the images, served by the library's resource of the uploads,
 * {@link RichTextFiles}, mounted at {@value RichTextImages#PATH}: for a day, and at most
 * {@value #MAX_FILES} of them, the oldest going first, so that the public demo cannot fill the
 * disk; each user, by the account signed in with or else by the session, keeps at most
 * {@value #MAX_FILES_PER_USER}.
 * <p>
 * Each file is named anew, by the mark of its user, a random name and the extension of the
 * format its bytes said, which the resource serves it as; the name the user gave goes beside it,
 * for the browser saving it. It deletes a file a user took out of a text only when it is the
 * user's own.
 */
public final class RichTextAttachments implements IRichTextAttachmentStore
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(RichTextAttachments.class);

	/** The largest file a user attaches in the demo. */
	public static final Bytes MAX_SIZE = Bytes.megabytes(5);

	/** How many files one drop or choice takes in the demo. */
	public static final int MAX_COUNT = 5;

	/** How long a file stays. */
	public static final Duration MAX_AGE = Duration.ofDays(1);

	/** How many files stay at most. */
	public static final int MAX_FILES = 100;

	/** How many files of one user stay at most. */
	public static final int MAX_FILES_PER_USER = 10;

	/** The extension of the file beside a file attached, holding the name the user gave it. */
	private static final String NAME_EXTENSION = ".name";

	private static final SecureRandom RANDOM = new SecureRandom();

	/** The name of a file the store kept: the mark of its user, a random part, its extension. */
	private static final Pattern NAME = Pattern.compile("[0-9a-f]{32}\\.(pdf|docx|xlsx|pptx|odt|ods|zip" +
		"|txt|csv|md)");

	private static final RichTextAttachments DEMO = new RichTextAttachments(Path.of(
		System.getProperty("java.io.tmpdir"), "antilia-widgets-rich-text-files"));

	/** The folder, as a string: a Path is not serializable. */
	private final String folder;

	RichTextAttachments(Path folder)
	{
		this.folder = folder.toAbsolutePath().toString();
	}

	/**
	 * @return the store of the demo
	 */
	public static RichTextAttachments get()
	{
		return DEMO;
	}

	/**
	 * @return the folder of the files
	 */
	public Path getFolder()
	{
		return Path.of(folder);
	}

	@Override
	public String store(FileUpload file, IUploadFileFormat format) throws IOException
	{
		Path folder = getFolder();
		String user = RichTextImages.markOf(user());
		byte[] random = new byte[12];
		RANDOM.nextBytes(random);
		String name = user + HexFormat.of().formatHex(random) + "." + format.getExtension();
		if (!NAME.matcher(name).matches())
		{
			throw new IOException("A format the demo does not keep: " + format.getExtension());
		}
		synchronized (RichTextAttachments.class)
		{
			Files.createDirectories(folder);
			prune(each -> each.getFileName().toString().startsWith(user), MAX_FILES_PER_USER - 1);
			prune(each -> true, MAX_FILES - 1);
			try (InputStream input = file.getInputStream())
			{
				Files.copy(input, folder.resolve(name), StandardCopyOption.REPLACE_EXISTING);
			}
			Files.writeString(folder.resolve(name + NAME_EXTENSION), RichTextImages.cleanName(
				file.getClientFileName(), name), StandardCharsets.UTF_8);
		}
		return RichTextFiles.get(RichTextImages.PATH).addressOf(name);
	}

	/**
	 * Deletes a file the user took out of a text: one of the user's own, never another's.
	 */
	@Override
	public void delete(String url) throws IOException
	{
		String name = url == null ? "" : url.substring(url.lastIndexOf('/') + 1);
		if (!NAME.matcher(name).matches() || !name.startsWith(RichTextImages.markOf(user())))
		{
			return;
		}
		synchronized (RichTextAttachments.class)
		{
			Files.deleteIfExists(getFolder().resolve(name));
			Files.deleteIfExists(getFolder().resolve(name + NAME_EXTENSION));
		}
	}

	/**
	 * Opens a file the store gave the name of, kept no longer than allowed, as the format its
	 * extension keeps, with the name the user gave it.
	 */
	@Override
	public RichTextServedFile open(String name) throws IOException
	{
		Matcher matcher = name == null ? null : NAME.matcher(name);
		if (matcher == null || !matcher.matches())
		{
			return null;
		}
		Path file = getFolder().resolve(name);
		if (!Files.isRegularFile(file) || isOld(file, Instant.now().minus(MAX_AGE)))
		{
			return null;
		}
		UploadFileFormat format = UploadFileFormat.ALL.stream().filter(each ->
			each.getExtension().equals(matcher.group(1))).findFirst().orElse(null);
		if (format == null)
		{
			return null;
		}
		Path named = getFolder().resolve(name + NAME_EXTENSION);
		String uploaded = Files.isRegularFile(named) ? Files.readString(named,
			StandardCharsets.UTF_8) : null;
		return RichTextServedFile.of(file, format, uploaded);
	}

	/**
	 * Deletes the files older than {@link #MAX_AGE}, then the oldest of those chosen, so that as
	 * many of them as this stay at most, with the files of their names.
	 */
	void prune(Predicate<Path> chosen, int keep)
	{
		Path folder = getFolder();
		if (!Files.isDirectory(folder))
		{
			return;
		}
		Instant old = Instant.now().minus(MAX_AGE);
		List<Path> files;
		try (Stream<Path> listed = Files.list(folder))
		{
			files = listed.filter(each -> NAME.matcher(each.getFileName().toString()).matches())
				.sorted(Comparator.comparing(RichTextAttachments::modified).reversed()).toList();
		}
		catch (IOException e)
		{
			LOG.warn("Could not list the files in {}", folder, e);
			return;
		}
		int kept = 0;
		for (Path file : files)
		{
			boolean counts = chosen.test(file);
			if (isOld(file, old) || counts && kept++ >= Math.max(keep, 0))
			{
				try
				{
					Files.deleteIfExists(file);
					Files.deleteIfExists(file.resolveSibling(file.getFileName() + NAME_EXTENSION));
				}
				catch (IOException e)
				{
					LOG.warn("Could not delete the file {}", file, e);
				}
			}
		}
	}

	/**
	 * @return who attaches a file: the account signed in with, else the session
	 */
	private static String user()
	{
		DemoSession session = DemoSession.get();
		return session.isSignedIn() ? "account:" + session.getAccount()
			: "session:" + session.getId();
	}

	private static boolean isOld(Path file, Instant time)
	{
		return modified(file).toInstant().isBefore(time);
	}

	private static FileTime modified(Path file)
	{
		try
		{
			return Files.getLastModifiedTime(file);
		}
		catch (IOException e)
		{
			return FileTime.fromMillis(0);
		}
	}
}
