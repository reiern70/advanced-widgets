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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.lang.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.richtext.RichTextImageStore;
import com.antiliacode.widgets.richtext.RichTextImageType;

/**
 * Keeps the images users upload into the rich text editors of the demo in a folder, served by
 * {@link RichTextImageResource} at {@value #PATH}: for a day, and at most {@value #MAX_IMAGES} of
 * them, the oldest going first, so that the public demo cannot fill the disk. The articles live in
 * the sessions, so an image is not needed for longer.
 * <p>
 * Each image is named anew, by a random name and the extension of its type, which the resource
 * serves it as: the name the browser gave is never a path on the server.
 */
public final class RichTextImages implements RichTextImageStore
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(RichTextImages.class);

	/** The path the images are served at, below the application. */
	public static final String PATH = "rich-text-images";

	/** The largest image a user uploads in the demo. */
	public static final Bytes MAX_SIZE = Bytes.megabytes(2);

	/** How long an image stays. */
	public static final Duration MAX_AGE = Duration.ofDays(1);

	/** How many images stay at most. */
	public static final int MAX_IMAGES = 100;

	/** The name of an image the store kept. */
	private static final Pattern NAME = Pattern.compile("[0-9a-f]{32}\\.(png|jpg|gif|webp)");

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
	public String store(FileUpload image, RichTextImageType type) throws IOException
	{
		Path folder = getFolder();
		String name = UUID.randomUUID().toString().replace("-", "") + "." + type.getExtension();
		synchronized (RichTextImages.class)
		{
			Files.createDirectories(folder);
			prune(MAX_IMAGES - 1);
			try (InputStream input = image.getInputStream())
			{
				Files.copy(input, folder.resolve(name), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		return addressOf(name);
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
		for (int i = 0; i < images.size(); i++)
		{
			Path image = images.get(i);
			if (i >= Math.max(keep, 0) || isOld(image, old))
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
