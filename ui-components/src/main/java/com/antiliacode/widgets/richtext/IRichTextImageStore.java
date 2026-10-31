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
import java.util.List;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.io.IClusterable;

/**
 * Keeps the images users upload into a {@link RichTextEditor}, and tells the address each is served
 * at, which the image of the text then takes: in a folder, a database or a store of files, served
 * by a resource of the application's own for example. It deletes those the users took out of their
 * texts again, if it wants to: the editor tells it which, as {@link RichTextEditor#setImageDeletion(
 * RichTextImageDeletion) its mode} says, at once or once the text is saved.
 * <p>
 * A store may keep images permanently too, for an editor that
 * {@link RichTextEditor#setPermanentImages(boolean) says so}: the user ticks the images to keep so
 * as they upload them ({@link #storePermanently(String, String)}), and inserts those kept into
 * any text later, found by their names ({@link #findPermanent(String, int)}) with the tool
 * {@link RichTextTool#STORED_IMAGE insert image from store}. The editor never deletes an image
 * kept permanently ({@link #isPermanent(String)}). Which images a user finds is the store's
 * business: those of the user alone, or of everybody.
 *
 * @see RichTextEditor#setImageStore(IRichTextImageStore)
 * @see RichTextImageUploadBehavior
 * @since 11.0.0
 */
@FunctionalInterface
public interface IRichTextImageStore extends IClusterable
{
	/**
	 * Keeps an image a user uploaded. The image is checked already: one of the types allowed, as
	 * its bytes say, no larger than allowed, and no wider and no higher than allowed. The file the
	 * upload holds is deleted after.
	 *
	 * @param image
	 *            the file uploaded; its name and its content type are the browser's, not to be
	 *            trusted
	 * @param type
	 *            its type, as its bytes say
	 * @return the address the image is served at: an http or https one, or a relative one, as the
	 *         editor and its cleaning ({@link RichTextPolicy#isSafeUrl(String)}) take
	 * @throws IOException
	 *             when the image could not be kept
	 */
	String store(FileUpload image, IRichTextImageType type) throws IOException;

	/**
	 * Deletes an image a user took out of a text, if the store wants to: it may be in another text
	 * still, pasted there, or kept for the versions of a text. Only the addresses the editor knows
	 * come here: those of the images it was shown with, and those the store answered for the
	 * uploads of the page; never one the browser made up, and never one
	 * {@link #isPermanent(String) kept permanently}. Does nothing by default.
	 *
	 * @param url
	 *            the address of the image, as {@link #store(FileUpload, IRichTextImageType)}
	 *            answered it
	 * @throws IOException
	 *             when the image could not be deleted
	 * @since 11.0.0
	 */
	default void delete(String url) throws IOException
	{
	}

	/**
	 * Keeps an image a user uploaded permanently, as the user ticked it: among the images
	 * {@link #findPermanent(String, int)} finds, and no longer deleted when taken out of a text.
	 * Only the addresses of the images uploaded on the page come here, never one the browser made
	 * up. Not supported by default.
	 *
	 * @param url
	 *            the address of the image, as {@link #store(FileUpload, IRichTextImageType)}
	 *            answered it
	 * @param name
	 *            the name of the file uploaded, the browser's, not to be trusted: the name the user
	 *            finds the image by
	 * @throws IOException
	 *             when the image could not be kept
	 * @throws UnsupportedOperationException
	 *             for a store that keeps no images permanently
	 * @since 11.0.0
	 */
	default void storePermanently(String url, String name) throws IOException
	{
		throw new UnsupportedOperationException("The store keeps no images permanently");
	}

	/**
	 * @param url
	 *            the address of an image
	 * @return whether the store keeps the image permanently: the editor then never deletes it;
	 *         {@code false} by default
	 * @since 11.0.0
	 */
	default boolean isPermanent(String url)
	{
		return false;
	}

	/**
	 * Finds the images kept permanently that the user of the request may insert, for the tool
	 * {@link RichTextTool#STORED_IMAGE insert image from store}: those whose names hold the query,
	 * the latest first for example. The addresses must be ones the editor takes, as those
	 * {@link #store(FileUpload, IRichTextImageType)} answers; the others are left out. None by
	 * default.
	 *
	 * @param query
	 *            what the user typed, trimmed; empty for all, the latest first
	 * @param max
	 *            how many to answer at most
	 * @return the images found, in the order the list shows them
	 * @throws IOException
	 *             when the images could not be searched
	 * @since 11.0.0
	 */
	default List<RichTextStoredImage> findPermanent(String query, int max) throws IOException
	{
		return List.of();
	}
}
