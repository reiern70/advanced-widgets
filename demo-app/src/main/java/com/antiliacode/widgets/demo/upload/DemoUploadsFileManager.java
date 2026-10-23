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
package com.antiliacode.widgets.demo.upload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.resource.FolderUploadsFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the files uploaded to the resource of the demo in a folder, a folder of each field in it,
 * only until the page told what it received: the page deletes them then ({@link #delete(File)}),
 * and every upload deletes the folders older than {@link #MAX_AGE} that a page left behind, such
 * as when its user went away during an upload.
 */
public class DemoUploadsFileManager extends FolderUploadsFileManager
{
	/** How long the files of an upload may stay. */
	public static final Duration MAX_AGE = Duration.ofMinutes(30);

	private static final Logger LOG = LoggerFactory.getLogger(DemoUploadsFileManager.class);

	/**
	 * @param folder
	 *            the folder of the uploads
	 */
	public DemoUploadsFileManager(File folder)
	{
		super(new org.apache.wicket.util.file.File(folder));
	}

	/**
	 * @return the folder of the uploads in the temporary folder of the JVM
	 */
	public static File defaultFolder()
	{
		return new File(System.getProperty("java.io.tmpdir"), "antilia-widgets-uploads");
	}

	@Override
	public String store(FileUpload fileItem, String uploadFieldId)
	{
		deleteOlderThan(Instant.now().minus(MAX_AGE));
		return super.store(fileItem, uploadFieldId);
	}

	/**
	 * Deletes an uploaded file, and the folder of its field once empty.
	 *
	 * @param file
	 *            the file, {@code null} for none
	 */
	public void delete(File file)
	{
		if (file == null)
		{
			return;
		}
		try
		{
			Files.deleteIfExists(file.toPath());
			Path field = file.toPath().getParent();
			if (field != null && Files.isDirectory(field) && !field.equals(getFolder().toPath()))
			{
				try (Stream<Path> rest = Files.list(field))
				{
					if (rest.findAny().isEmpty())
					{
						Files.deleteIfExists(field);
					}
				}
			}
		}
		catch (IOException e)
		{
			LOG.warn("Could not delete the upload {}", file, e);
		}
	}

	/**
	 * Deletes the folders of the fields changed last before a time.
	 *
	 * @param time
	 *            the time
	 */
	void deleteOlderThan(Instant time)
	{
		try (Stream<Path> fields = Files.list(getFolder().toPath()))
		{
			fields.filter(field -> isOlder(field, time)).forEach(DemoUploadsFileManager::deleteAll);
		}
		catch (IOException e)
		{
			LOG.warn("Could not list the uploads in {}", getFolder(), e);
		}
	}

	private static boolean isOlder(Path path, Instant time)
	{
		try
		{
			return Files.getLastModifiedTime(path).toInstant().isBefore(time);
		}
		catch (IOException e)
		{
			return false;
		}
	}

	private static void deleteAll(Path folder)
	{
		try (Stream<Path> paths = Files.walk(folder))
		{
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
		catch (IOException e)
		{
			LOG.warn("Could not delete the uploads in {}", folder, e);
		}
	}
}
