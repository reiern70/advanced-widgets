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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.wicket.util.io.IClusterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The images, or the files attached, taken for an editor and not deleted since, with the names of
 * their files: those of a page, kept by {@link RichTextImageUploadBehavior} and
 * {@link RichTextAttachmentUploadBehavior}, or those of an editor in a session, kept by
 * {@link RichTextFiles}. Only these the browser may have deleted or kept permanently: never an
 * address it made up. Safe for requests of one session coming at once.
 */
final class RichTextUploadedImages implements IClusterable
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(RichTextUploadedImages.class);

	/** The most addresses remembered, the oldest forgotten first. */
	static final int REMEMBERED = 1000;

	/** The addresses of the images taken, with the names of their files, the oldest first. */
	private final Map<String, String> uploaded = new LinkedHashMap<>();

	/**
	 * Remembers the address of an image taken, and the name of its file, forgetting the oldest
	 * beyond {@link #REMEMBERED}.
	 */
	synchronized void remember(String url, String name)
	{
		uploaded.remove(url);
		uploaded.put(url, name == null ? "" : name);
		Iterator<String> oldest = uploaded.keySet().iterator();
		while (uploaded.size() > REMEMBERED && oldest.hasNext())
		{
			oldest.next();
			oldest.remove();
		}
	}

	/**
	 * @return whether the image of the address was taken, and not deleted since
	 */
	synchronized boolean isUploaded(String url)
	{
		return url != null && uploaded.containsKey(url.trim());
	}

	/**
	 * @return the addresses of the images taken, the oldest first: a copy
	 */
	synchronized Set<String> getUploaded()
	{
		return new LinkedHashSet<>(uploaded.keySet());
	}

	/**
	 * Forgets images taken: they are no longer deleted when asked to.
	 */
	synchronized void forget(Collection<String> urls)
	{
		// a copy: the addresses may be a view of those remembered
		List.copyOf(urls).forEach(url -> uploaded.remove(url == null ? null : url.trim()));
	}

	/**
	 * Keeps images taken permanently, by the store, with the names of their files, and forgets
	 * them; leaves the others alone.
	 *
	 * @return the addresses of the images kept
	 */
	Set<String> storePermanently(IRichTextImageStore store, Collection<String> urls)
	{
		Set<String> stored = new LinkedHashSet<>();
		for (String url : List.copyOf(urls))
		{
			String address = url == null ? null : url.trim();
			String name;
			synchronized (this)
			{
				name = address == null ? null : uploaded.get(address);
			}
			if (name == null)
			{
				continue;
			}
			try
			{
				store.storePermanently(address, name);
				synchronized (this)
				{
					uploaded.remove(address);
				}
				stored.add(address);
			}
			catch (IOException | RuntimeException e)
			{
				LOG.error("Could not keep the image {} permanently", address, e);
			}
		}
		return stored;
	}

	/**
	 * Deletes images taken, by the store, and forgets them; leaves the others alone.
	 *
	 * @return the addresses of the images deleted
	 */
	Set<String> deleteUploaded(IRichTextImageStore store, Collection<String> urls)
	{
		return deleteUploaded(url -> RichTextImageUploads.delete(store, url), urls);
	}

	/**
	 * Deletes files taken, and forgets them; leaves the others alone.
	 *
	 * @param delete
	 *            deletes a file by its address, answering whether it did not fail
	 * @return the addresses of the files deleted
	 */
	Set<String> deleteUploaded(Predicate<String> delete, Collection<String> urls)
	{
		Set<String> deleted = new LinkedHashSet<>();
		for (String url : List.copyOf(urls))
		{
			String address = url == null ? null : url.trim();
			boolean known;
			synchronized (this)
			{
				known = address != null && uploaded.remove(address) != null;
			}
			if (known && delete.test(address))
			{
				deleted.add(address);
			}
		}
		return deleted;
	}
}
