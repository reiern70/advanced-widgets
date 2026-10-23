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
package com.antiliacode.widgets.demo;

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
