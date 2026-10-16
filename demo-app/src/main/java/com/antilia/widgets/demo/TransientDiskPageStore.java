/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import java.io.File;

import org.apache.wicket.pageStore.DiskPageStore;
import org.apache.wicket.util.file.Files;
import org.apache.wicket.util.lang.Bytes;

/**
 * Wicket's page store on disk, for one run of the server: it starts empty and takes its pages
 * along when the application stops.
 * <p>
 * Wicket's own {@link DiskPageStore} writes an index of its pages when the application stops and
 * reads it back at the next start, for sessions that outlive a restart. Jetty keeps ours in memory
 * only, so those pages would never be asked for again, and no session ending would remove them.
 * This store removes the folder Wicket's keeps them in, within the file store folder, before it
 * reads the index, also after a crash that wrote none, and when it is destroyed. The rest of the
 * file store folder, the servlet container's temporary folder by default, is left alone.
 */
public class TransientDiskPageStore extends DiskPageStore
{
	private final File pages;

	/**
	 * @param applicationName
	 *            the name of the application
	 * @param fileStoreFolder
	 *            the folder in which the store keeps the folder of its pages
	 * @param maxSizePerSession
	 *            the most the pages of a session take on disk
	 */
	public TransientDiskPageStore(String applicationName, File fileStoreFolder,
		Bytes maxSizePerSession)
	{
		super(applicationName, withoutPages(fileStoreFolder, applicationName), maxSizePerSession);
		pages = pagesFolder(fileStoreFolder, applicationName);
	}

	@Override
	public void destroy()
	{
		super.destroy();
		Files.removeFolder(pages);
	}

	/**
	 * @return the folder {@link DiskPageStore} keeps the pages of an application in, within the
	 *         file store folder
	 */
	static File pagesFolder(File fileStoreFolder, String applicationName)
	{
		return new File(fileStoreFolder, applicationName + "-filestore");
	}

	private static File withoutPages(File fileStoreFolder, String applicationName)
	{
		Files.removeFolder(pagesFolder(fileStoreFolder, applicationName));
		return fileStoreFolder;
	}
}
