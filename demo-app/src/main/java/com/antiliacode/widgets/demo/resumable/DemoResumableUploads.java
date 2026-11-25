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
package com.antiliacode.widgets.demo.resumable;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.resumable.ResumableUpload;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.resumable.ResumableUploadResourceReference;
import org.apache.wicket.markup.html.form.upload.resource.FolderUploadsFileManager;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.lang.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The resumable uploads of the demo, Wicket's resource of them: files of 100 MB at most, five at
 * once and 300 MB together of a session, 1 GB of all sessions, each deleted 30 minutes after the
 * browser last sent something. The whole files go to a folder, from which the page deletes each
 * once it told what it received. For a session that asks for it, the server breaks off a chunk now
 * and then, to show how the browser goes on.
 */
public class DemoResumableUploads extends ResumableUploadResourceReference
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(DemoResumableUploads.class);

	/** Where the uploads are mounted. */
	public static final String PATH = "resumable-uploads";

	/** Whether the server breaks off the chunks of a session now and then. */
	private static final MetaDataKey<Boolean> BREAKING = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	/** The id the React and Svelte apps of a session store their files under. */
	private static final MetaDataKey<String> CLIENT = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	/** One chunk in so many breaks off, while the session asks for it. */
	static final int ONE_IN = 3;

	/**
	 * @param folder
	 *            the folder of the uploads: the parts of those not finished in one folder, the
	 *            files stored whole in another
	 */
	public DemoResumableUploads(Path folder)
	{
		super(new FolderUploadsFileManager(new File(folder.resolve("stored").toString())),
			folder.resolve("parts").toFile());
		setMaxFileSize(Bytes.megabytes(100));
		// less than the proxy before production lets a request carry
		setMaxChunkSize(Bytes.megabytes(8));
		setMaxUploadsPerSession(5);
		setMaxBytesPerSession(Bytes.megabytes(300));
		setMaxTotalBytes(Bytes.gigabytes(1));
		setExpiry(Duration.ofMinutes(30));
	}

	/**
	 * @return the folder of the uploads of this process in the temporary folder of the JVM: several
	 *         Jettys running side by side each empty their own; the folders of processes no longer
	 *         running are deleted
	 */
	public static Path defaultFolder()
	{
		Path all = Path.of(System.getProperty("java.io.tmpdir"), "antilia-widgets-resumable-uploads");
		try (Stream<Path> folders = Files.isDirectory(all) ? Files.list(all) : Stream.empty())
		{
			folders.filter(DemoResumableUploads::isLeft).forEach(DemoResumableUploads::deleteAll);
		}
		catch (IOException e)
		{
			LOG.warn("Could not look for the resumable uploads left in {}", all, e);
		}
		return all.resolve(String.valueOf(ProcessHandle.current().pid()));
	}

	/**
	 * @return whether a folder is that of a process no longer running
	 */
	private static boolean isLeft(Path folder)
	{
		String name = folder.getFileName().toString();
		if (!name.matches("[0-9]{1,18}"))
		{
			return false;
		}
		return ProcessHandle.of(Long.parseLong(name)).map(process -> !process.isAlive())
			.orElse(true);
	}

	private static void deleteAll(Path folder)
	{
		try (Stream<Path> paths = Files.walk(folder))
		{
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
		catch (IOException e)
		{
			LOG.warn("Could not delete the resumable uploads left in {}", folder, e);
		}
	}

	/**
	 * @return the uploads of the application of the current thread
	 */
	public static DemoResumableUploads get()
	{
		return (DemoResumableUploads)ResumableUploadResourceReference.getInstance();
	}

	/**
	 * @return the id the React and Svelte apps of the session of the request store their files
	 *         under, a random one, made for the session the first time
	 */
	public static String clientId()
	{
		Session session = Session.get();
		String id = session.getMetaData(CLIENT);
		if (id == null)
		{
			id = "demo-" + UUID.randomUUID();
			session.setMetaData(CLIENT, id);
			session.bind();
		}
		return id;
	}

	/**
	 * @return the id the React and Svelte apps of the session of the request store their files
	 *         under, {@code null} before {@link #clientId()} made one
	 */
	public static String existingClientId()
	{
		return Session.exists() ? Session.get().getMetaData(CLIENT) : null;
	}

	/**
	 * @return whether the server breaks off the chunks of the session of the request now and then
	 */
	public static boolean isBreaking()
	{
		return Session.exists() && Boolean.TRUE.equals(Session.get().getMetaData(BREAKING));
	}

	/**
	 * @param breaking
	 *            whether the server breaks off the chunks of the session of the request now and
	 *            then
	 */
	public static void setBreaking(boolean breaking)
	{
		Session.get().setMetaData(BREAKING, breaking ? Boolean.TRUE : null);
	}

	@Override
	protected InputStream wrapBody(InputStream body, ResumableUpload upload)
	{
		if (!isBreaking() || ThreadLocalRandom.current().nextInt(ONE_IN) != 0)
		{
			return body;
		}
		// somewhere in the first half of a chunk, after 64 kB at least
		long left = upload.getLength() - upload.getOffset();
		long most = Math.max(64 * 1024, Math.min(left, getMaxChunkSize().bytes()) / 2);
		return new BreakingStream(body, ThreadLocalRandom.current().nextLong(64 * 1024, most + 1));
	}

	/**
	 * A body breaking off after some bytes, as a connection does.
	 */
	static final class BreakingStream extends FilterInputStream
	{
		private long left;

		BreakingStream(InputStream in, long left)
		{
			super(in);
			this.left = left;
		}

		@Override
		public int read() throws IOException
		{
			byte[] one = new byte[1];
			return read(one, 0, 1) < 0 ? -1 : one[0] & 0xff;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException
		{
			if (left <= 0)
			{
				throw new IOException("The demo broke the connection off");
			}
			int read = super.read(buffer, offset, (int)Math.min(length, left));
			if (read > 0)
			{
				left -= read;
			}
			return read;
		}
	}
}
