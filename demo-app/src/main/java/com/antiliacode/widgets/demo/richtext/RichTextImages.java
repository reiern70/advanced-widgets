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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
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
import com.antiliacode.widgets.richtext.RichTextStoredImage;

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
 * <p>
 * It keeps the images a user ticked permanently, those of the user alone, in a folder of their own,
 * {@value #PERMANENT}, with the name of the file uploaded beside each: at the same address, never
 * deleted when taken out of a text, and found by their names for the tool insert image from store,
 * each user finding its own. Permanently, in the demo, is for {@link #PERMANENT_MAX_AGE} and
 * {@value #MAX_PERMANENT_PER_USER} images of a user, its oldest going first.
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

	/** The folder of the images kept permanently, in the folder of the images. */
	public static final String PERMANENT = "permanent";

	/** How long an image kept permanently stays in the demo. */
	public static final Duration PERMANENT_MAX_AGE = Duration.ofDays(30);

	/** How many images of one user stay permanently at most. */
	public static final int MAX_PERMANENT_PER_USER = 20;

	/** How many images stay permanently at most. */
	public static final int MAX_PERMANENT = 500;

	/** The extension of the file beside an image kept permanently, holding its name. */
	private static final String NAME_EXTENSION = ".name";

	/** The longest name of an image kept permanently. */
	private static final int LONGEST_NAME = 100;

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
	 * Keeps an image of the user's own permanently: moves it to the folder of those kept, with the
	 * name of its file beside it. Its address stays.
	 */
	@Override
	public void storePermanently(String url, String name) throws IOException
	{
		String file = nameOf(url);
		String user = markOf(user());
		if (!NAME.matcher(file).matches() || !file.startsWith(user))
		{
			throw new IOException("Not an image of the user: " + url);
		}
		Path permanent = getFolder().resolve(PERMANENT);
		synchronized (RichTextImages.class)
		{
			Path image = getFolder().resolve(file);
			if (!Files.isRegularFile(image))
			{
				throw new IOException("No such image: " + url);
			}
			Files.createDirectories(permanent);
			prunePermanent(each -> each.getFileName().toString().startsWith(user),
				MAX_PERMANENT_PER_USER - 1);
			prunePermanent(each -> true, MAX_PERMANENT - 1);
			Path kept = permanent.resolve(file);
			Files.move(image, kept, StandardCopyOption.REPLACE_EXISTING);
			Files.setLastModifiedTime(kept, FileTime.from(Instant.now()));
			Files.writeString(permanent.resolve(file + NAME_EXTENSION), cleanName(name, file),
				StandardCharsets.UTF_8);
		}
	}

	@Override
	public boolean isPermanent(String url)
	{
		String file = nameOf(url);
		return NAME.matcher(file).matches() && Files.isRegularFile(getFolder().resolve(PERMANENT)
			.resolve(file));
	}

	/**
	 * Finds the images the user kept permanently whose names hold the query, the latest first.
	 */
	@Override
	public List<RichTextStoredImage> findPermanent(String query, int max)
	{
		String user = markOf(user());
		String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		List<RichTextStoredImage> found = new ArrayList<>();
		for (Path image : permanentImages(each -> each.getFileName().toString().startsWith(user)))
		{
			if (found.size() >= max)
			{
				break;
			}
			String file = image.getFileName().toString();
			String name = nameOfPermanent(image);
			if (name.toLowerCase(Locale.ROOT).contains(text))
			{
				found.add(new RichTextStoredImage(addressOf(file), name));
			}
		}
		return found;
	}

	/**
	 * @return the name of the file of an address, as it ends
	 */
	private static String nameOf(String url)
	{
		return url == null ? "" : url.substring(url.lastIndexOf('/') + 1);
	}

	/**
	 * @return the name of an image kept permanently, as the file beside it says, else that of its
	 *         file
	 */
	private static String nameOfPermanent(Path image)
	{
		try
		{
			return Files.readString(image.resolveSibling(image.getFileName() + NAME_EXTENSION),
				StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			return image.getFileName().toString();
		}
	}

	/**
	 * @return the name the browser gave, without what a line or a path would make of it, and not
	 *         too long; that of the file for none
	 */
	static String cleanName(String name, String file)
	{
		String clean = name == null ? "" : name.replaceAll("[\\p{Cntrl}]", "").trim();
		clean = clean.substring(Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\')) + 1)
			.trim();
		if (clean.length() > LONGEST_NAME)
		{
			clean = clean.substring(0, LONGEST_NAME);
		}
		return clean.isEmpty() ? file : clean;
	}

	/**
	 * @return the images kept permanently of those chosen, the latest first, those older than
	 *         {@link #PERMANENT_MAX_AGE} left out
	 */
	private List<Path> permanentImages(Predicate<Path> chosen)
	{
		Path permanent = getFolder().resolve(PERMANENT);
		if (!Files.isDirectory(permanent))
		{
			return List.of();
		}
		Instant old = Instant.now().minus(PERMANENT_MAX_AGE);
		try (Stream<Path> files = Files.list(permanent))
		{
			return files.filter(file -> NAME.matcher(file.getFileName().toString()).matches())
				.filter(chosen).filter(file -> !isOld(file, old))
				.sorted(Comparator.comparing(RichTextImages::modified).reversed()).toList();
		}
		catch (IOException e)
		{
			LOG.warn("Could not list the images in {}", permanent, e);
			return List.of();
		}
	}

	/**
	 * Deletes the images kept permanently older than {@link #PERMANENT_MAX_AGE}, then the oldest
	 * of those chosen, so that as many of them as this stay at most, with the files of their names.
	 */
	void prunePermanent(Predicate<Path> chosen, int keep)
	{
		Path permanent = getFolder().resolve(PERMANENT);
		if (!Files.isDirectory(permanent))
		{
			return;
		}
		Instant old = Instant.now().minus(PERMANENT_MAX_AGE);
		List<Path> images;
		try (Stream<Path> files = Files.list(permanent))
		{
			images = files.filter(file -> NAME.matcher(file.getFileName().toString()).matches())
				.sorted(Comparator.comparing(RichTextImages::modified).reversed()).toList();
		}
		catch (IOException e)
		{
			LOG.warn("Could not list the images in {}", permanent, e);
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
					Files.deleteIfExists(image.resolveSibling(image.getFileName() +
						NAME_EXTENSION));
				}
				catch (IOException e)
				{
					LOG.warn("Could not delete the image {}", image, e);
				}
			}
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
		if (Files.isRegularFile(file))
		{
			return isOld(file, Instant.now().minus(MAX_AGE)) ? null : file;
		}
		Path permanent = getFolder().resolve(PERMANENT).resolve(name);
		return Files.isRegularFile(permanent) && !isOld(permanent, Instant.now().minus(
			PERMANENT_MAX_AGE)) ? permanent : null;
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
