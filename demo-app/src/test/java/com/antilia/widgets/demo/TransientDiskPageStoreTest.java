/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.wicket.mock.MockPageContext;
import org.apache.wicket.pageStore.DiskPageStore;
import org.apache.wicket.pageStore.SerializedPage;
import org.apache.wicket.util.lang.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The pages on disk last one run of the server, as the sessions do: a store starts without the
 * pages of an earlier run, also of one that crashed, and takes its own along when destroyed.
 */
class TransientDiskPageStoreTest
{
	private static final String APPLICATION = "transient-page-store-test";

	private static final Bytes SIZE = Bytes.megabytes(1);

	private static final byte[] DATA = { 1, 2, 3 };

	@TempDir
	File folder;

	@Test
	void wicketsStoreReadsThePagesOfAnEarlierRunBack()
	{
		// a session the servlet container kept across the restart, with the attribute naming
		// its pages
		MockPageContext session = new MockPageContext("session");
		DiskPageStore earlier = new DiskPageStore(APPLICATION, folder, SIZE);
		earlier.addPage(session, new SerializedPage(1, "Page", DATA));
		earlier.destroy();

		DiskPageStore store = new DiskPageStore(APPLICATION, folder, SIZE);
		try
		{
			assertEquals(Set.of("session"), store.getSessionIdentifiers());
			assertNotNull(store.getPage(session, 1));
		}
		finally
		{
			store.destroy();
		}
	}

	@Test
	void startsWithoutThePagesOfAnEarlierRun() throws IOException
	{
		DiskPageStore earlier = new DiskPageStore(APPLICATION, folder, SIZE);
		earlier.addPage(new MockPageContext("session"), new SerializedPage(1, "Page", DATA));
		earlier.destroy();

		TransientDiskPageStore store = new TransientDiskPageStore(APPLICATION, folder, SIZE);
		try
		{
			assertEquals(Set.of(), store.getSessionIdentifiers());
			assertEquals(List.of(), files(TransientDiskPageStore.pagesFolder(folder, APPLICATION)));
		}
		finally
		{
			store.destroy();
		}
	}

	@Test
	void startsWithoutThePagesOfARunThatCrashed() throws IOException
	{
		// a run that crashed wrote no index: nothing would ever remove its pages
		File pages = new File(TransientDiskPageStore.pagesFolder(folder, APPLICATION),
			"1/2/session/data");
		Files.createDirectories(pages.getParentFile().toPath());
		Files.write(pages.toPath(), DATA);

		TransientDiskPageStore store = new TransientDiskPageStore(APPLICATION, folder, SIZE);
		try
		{
			assertFalse(pages.exists());
		}
		finally
		{
			store.destroy();
		}
	}

	@Test
	void takesItsPagesAlongWhenDestroyed() throws IOException
	{
		File other = new File(folder, "other");
		Files.write(other.toPath(), DATA);

		TransientDiskPageStore store = new TransientDiskPageStore(APPLICATION, folder, SIZE);
		MockPageContext context = new MockPageContext("session");
		store.addPage(context, new SerializedPage(1, "Page", DATA));
		assertArrayEquals(DATA, ((SerializedPage)store.getPage(context, 1)).getData());
		store.destroy();

		assertFalse(TransientDiskPageStore.pagesFolder(folder, APPLICATION).exists());
		// the rest of the folder, the servlet container's temporary folder by default, stays
		assertTrue(other.exists());
	}

	private static List<Path> files(File folder) throws IOException
	{
		try (Stream<Path> paths = Files.walk(folder.toPath()))
		{
			return paths.filter(Files::isRegularFile).toList();
		}
	}
}
