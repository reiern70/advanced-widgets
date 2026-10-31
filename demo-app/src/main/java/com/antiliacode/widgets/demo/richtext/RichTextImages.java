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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.lang.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.richtext.IRichTextImageStore;
import com.antiliacode.widgets.richtext.IRichTextImageType;
import com.antiliacode.widgets.richtext.RichTextImageDimensions;
import com.antiliacode.widgets.richtext.RichTextImageType;

/**
 * Keeps the images users upload into the rich text editors of the demo in a folder, served by
 * {@link RichTextImageResource} at {@value #PATH}: for a day, and at most {@value #MAX_IMAGES} of
 * them, the oldest going first, so that the public demo cannot fill the disk. The articles live in
 * the sessions, so an image is not needed for longer. Each user, by the account signed in with or
 * else by the session, keeps at most {@value #MAX_IMAGES_PER_USER}, its own oldest going first:
 * one user uploading many does not push out the images of the others.
 * <p>
 * Each image is named anew, by a random name and the extension of its type, which the resource
 * serves it as: the name the browser gave is never a path on the server. The name starts with a
 * mark of its user, eight hex digits of a hash, which tells the images of a user apart.
 * <p>
 * It deletes an image a user took out of a text when the editor says so, only one of that user's
 * own: by its mark.
 */
public final class RichTextImages implements IRichTextImageStore
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(RichTextImages.class);

	/** The path the images are served at, below the application. */
	public static final String PATH = "rich-text-images";

	/** The largest image a user uploads in the demo. */
	public static final Bytes MAX_SIZE = Bytes.megabytes(2);

	/** The widest and the highest image a user uploads in the demo, in pixels. */
	public static final RichTextImageDimensions MAX_DIMENSIONS = new RichTextImageDimensions(2400,
		2400);

	/** How many images one drop or choice takes in the demo. */
	public static final int MAX_COUNT = 5;

	/** How long an image stays. */
	public static final Duration MAX_AGE = Duration.ofDays(1);

	/** How many images stay at most. */
	public static final int MAX_IMAGES = 100;

	/** How many images of one user stay at most. */
	public static final int MAX_IMAGES_PER_USER = 10;

	/** The random part of the names. */
	private static final SecureRandom RANDOM = new SecureRandom();

	/** The name of an image the store kept. */
	private static final Pattern NAME = Pattern.compile("[0-9a-f]{32}\\.(png|jpg|gif|webp|avif)");

	/** The store of the demo, in its folder of the temporary folder of the JVM. */
	private static final RichTextImages DEMO = new RichTextImages(Path.of(System.getProperty(
		"java.io.tmpdir"), "antilia-widgets-rich-text-images"));

	/** The folder, as a string: a Path is not serializable. */
	private final String folder;

	/**
	 * @param folder
	 *            the folder of the images
	 */
	RichTextImages(Path folder)
	{
		this.folder = folder.toAbsolutePath().toString();
	}

	/**
	 * @return the store of the demo
	 */
	public static RichTextImages get()
	{
		return DEMO;
	}

	/**
	 * @return the folder of the images
	 */
	public Path getFolder()
	{
		return Path.of(folder);
	}

	@Override
	public String store(FileUpload image, IRichTextImageType type) throws IOException
	{
		Path folder = getFolder();
		String user = markOf(user());
		byte[] random = new byte[12];
		RANDOM.nextBytes(random);
		String name = user + HexFormat.of().formatHex(random) + "." + type.getExtension();
		synchronized (RichTextImages.class)
		{
			Files.createDirectories(folder);
			prune(file -> file.getFileName().toString().startsWith(user),
				MAX_IMAGES_PER_USER - 1);
			prune(MAX_IMAGES - 1);
			try (InputStream input = image.getInputStream())
			{
				Files.copy(input, folder.resolve(name), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		return addressOf(name);
	}

	/**
	 * Deletes an image the user took out of a text: one of the user's own, the account's or the
	 * session's, never another's, which its name tells.
	 */
	@Override
	public void delete(String url) throws IOException
	{
		String name = url == null ? "" : url.substring(url.lastIndexOf('/') + 1);
		if (!NAME.matcher(name).matches() || !name.startsWith(markOf(user())))
		{
			return;
		}
		synchronized (RichTextImages.class)
		{
			Files.deleteIfExists(getFolder().resolve(name));
		}
	}

	/**
	 * @return who uploads an image: the account signed in with, else the session
	 */
	private static String user()
	{
		DemoSession session = DemoSession.get();
		return session.isSignedIn() ? "account:" + session.getAccount()
			: "session:" + session.getId();
	}

	/**
	 * @return the mark of a user that the names of its images start with: eight hex digits
	 */
	static String markOf(String user)
	{
		try
		{
			byte[] hash = MessageDigest.getInstance("SHA-256")
				.digest(String.valueOf(user).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash, 0, 4);
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("SHA-256 is missing", e);
		}
	}

	/**
	 * @return the address an image is served at, from the root of the server
	 */
	static String addressOf(String name)
	{
		Request request = RequestCycle.get().getRequest();
		return request.getContextPath() + request.getFilterPath() + "/" + PATH + "/" + name;
	}

	/**
	 * @param name
	 *            the name of an image, as its address ends
	 * @return the file of the image, {@code null} for a name the store did not give or an image
	 *         gone
	 */
	Path find(String name)
	{
		if (name == null || !NAME.matcher(name).matches())
		{
			return null;
		}
		Path file = getFolder().resolve(name);
		return Files.isRegularFile(file) && !isOld(file, Instant.now().minus(MAX_AGE)) ? file : null;
	}

	/**
	 * @return the type of an image by its name
	 */
	static RichTextImageType typeOf(Path file)
	{
		Matcher matcher = NAME.matcher(file.getFileName().toString());
		if (!matcher.matches())
		{
			return null;
		}
		for (RichTextImageType type : RichTextImageType.values())
		{
			if (type.getExtension().equals(matcher.group(1)))
			{
				return type;
			}
		}
		return null;
	}

	/**
	 * Deletes the images older than {@link #MAX_AGE}, then the oldest ones, so that as many as
	 * this stay at most.
	 */
	void prune(int keep)
	{
		prune(file -> true, keep);
	}

	/**
	 * Deletes the images older than {@link #MAX_AGE}, then the oldest of those chosen, so that as
	 * many of them as this stay at most.
	 */
	void prune(Predicate<Path> chosen, int keep)
	{
		Path folder = getFolder();
		if (!Files.isDirectory(folder))
		{
			return;
		}
		Instant old = Instant.now().minus(MAX_AGE);
		List<Path> images;
		try (Stream<Path> files = Files.list(folder))
		{
			images = files.filter(file -> NAME.matcher(file.getFileName().toString()).matches())
				.sorted(Comparator.comparing(RichTextImages::modified).reversed()).toList();
		}
		catch (IOException e)
		{
			LOG.warn("Could not list the images in {}", folder, e);
			return;
		}
		int kept = 0;
		for (Path image : images)
		{
			boolean counts = chosen.test(image);
			if (isOld(image, old) || counts && kept++ >= Math.max(keep, 0))
			{
				try
				{
					Files.deleteIfExists(image);
				}
				catch (IOException e)
				{
					LOG.warn("Could not delete the image {}", image, e);
				}
			}
		}
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
