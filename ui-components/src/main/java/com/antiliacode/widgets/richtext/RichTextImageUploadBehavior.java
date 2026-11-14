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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;

import com.github.openjson.JSONObject;

/**
 * Takes the images a user uploads into a rich text editor, and answers the address each is served
 * at: the resource of a page's own the editor in the browser posts a file to, in a form under
 * {@value #FILE_PARAMETER}. It answers JSON, {@code {"url": "..."}}, or {@code {"error": "..."}}
 * saying why not, in the language of the page, which the editor shows.
 * <p>
 * It checks the file before a {@link IRichTextImageStore} keeps it: no larger than
 * {@link #setMaxSize(Bytes) the size allowed}, which the request is cut at, of
 * {@link #setTypes(Collection) a type allowed} as its first bytes say, whatever its name and the
 * type the browser gave, and, with {@link #setMaxDimensions(RichTextImageDimensions) a limit of
 * pixels}, no wider and no higher than it, as the header of the file says, read without decoding
 * the picture. The address the store answers must be one the editor and its cleaning take
 * ({@link RichTextPolicy#isSafeUrl(String)}): never {@code data:} nor {@code javascript:}; and
 * the site's own ({@link RichTextPolicy#isSiteUrl(String)}) when the behavior is on an editor that
 * {@link AbstractRichTextEditor#setImagesFromSiteOnly(boolean) keeps the images of the site
 * alone}.
 * <p>
 * It deletes the images it took again when asked to, by a POST of the addresses as a JSON array
 * under {@value #REMOVED_PARAMETER}, which {@code deleteImagesAt(}{@link #getUploadUrl()}{@code )}
 * of the editor in the browser sends: only those it took on this page, by the store's
 * {@link IRichTextImageStore#delete(String)}, and answers {@code {"deleted": [...]}}.
 * <p>
 * With {@link #setPermanentImages(boolean) permanent images}, it keeps images it took permanently
 * when asked to, by a POST of their addresses as a JSON array under {@value #PERMANENT_PARAMETER},
 * which {@code storePermanentlyAt(}{@link #getUploadUrl()}{@code )} sends: only those it took on
 * this page, by the store's {@link IRichTextImageStore#storePermanently(String, String)} with the
 * name of their files, and answers {@code {"stored": [...]}}. It finds those kept, by a POST of
 * what the user typed under {@value #FIND_PARAMETER}, which {@code findImagesAt(}
 * {@link #getUploadUrl()}{@code )} sends: by the store's
 * {@link IRichTextImageStore#findPermanent(String, int)}, {@link #setMaxFound(int) so many} at
 * most, and answers {@code {"images": [{"url": ..., "name": ...}]}}, those of an address the
 * editor takes.
 * <p>
 * A {@link RichTextEditor} with {@link RichTextEditor#setImageStore(IRichTextImageStore) a store}
 * has one already. Add one to a page for the editors of React and Svelte in it, and give them
 * {@code uploadImageTo(}{@link #getUploadUrl()}{@code )} as their {@code onUpload}.
 * <p>
 * {@link RichTextFiles The resource of the uploads}, mounted once by the application and not tied
 * to a page, takes the images with the same checks and serves them, for the editors of Wicket,
 * React and Svelte alike.
 *
 * @since 11.0.0
 */
public class RichTextImageUploadBehavior extends AbstractAjaxBehavior
{
	private static final long serialVersionUID = 1L;

	/** The name the editor posts the file under. */
	public static final String FILE_PARAMETER = "file";

	/**
	 * The name the editor posts the addresses of the images to delete under, as a JSON array.
	 *
	 * @since 10.11.0
	 */
	public static final String REMOVED_PARAMETER = "removed";

	/**
	 * The name the editor posts the addresses of the images to keep permanently under, as a JSON
	 * array.
	 *
	 * @since 10.11.0
	 */
	public static final String PERMANENT_PARAMETER = "permanent";

	/**
	 * The name the editor posts what the user typed to find the images kept permanently under.
	 *
	 * @since 10.11.0
	 */
	public static final String FIND_PARAMETER = "find";

	/**
	 * The most images kept permanently a search answers, unless set otherwise.
	 *
	 * @since 10.11.0
	 */
	public static final int DEFAULT_MAX_FOUND = 20;

	/** The largest image a user uploads, unless set otherwise. */
	public static final Bytes DEFAULT_MAX_SIZE = Bytes.megabytes(5);

	private final IRichTextImageStore store;

	private Bytes maxSize = DEFAULT_MAX_SIZE;

	private Set<IRichTextImageType> types = new LinkedHashSet<>(RichTextImageType.ALL);

	private RichTextImageDimensions maxDimensions;

	private boolean permanentImages;

	private int maxFound = DEFAULT_MAX_FOUND;

	/**
	 * The addresses of the images taken on this page, which it deletes when asked to, with the
	 * names of their files.
	 */
	private final RichTextUploadedImages uploaded = new RichTextUploadedImages();

	/**
	 * @param store
	 *            what keeps the images and tells their addresses
	 */
	public RichTextImageUploadBehavior(IRichTextImageStore store)
	{
		this.store = Args.notNull(store, "store");
	}

	/**
	 * @return what keeps the images
	 */
	public IRichTextImageStore getStore()
	{
		return store;
	}

	/**
	 * @return the largest image a user uploads
	 */
	public Bytes getMaxSize()
	{
		return maxSize;
	}

	/**
	 * @param maxSize
	 *            the largest image a user uploads, {@link #DEFAULT_MAX_SIZE} by default
	 * @return {@code this}
	 */
	public RichTextImageUploadBehavior setMaxSize(Bytes maxSize)
	{
		this.maxSize = Args.notNull(maxSize, "maxSize");
		return this;
	}

	/**
	 * @return the types of the images a user uploads, in their order
	 */
	public Set<IRichTextImageType> getTypes()
	{
		return Collections.unmodifiableSet(types);
	}

	/**
	 * @param types
	 *            the types of the images a user uploads, in the order the messages name them: those
	 *            of {@link RichTextImageType} and those of the application's own; all those built
	 *            in by default
	 * @return {@code this}
	 */
	public RichTextImageUploadBehavior setTypes(Collection<? extends IRichTextImageType> types)
	{
		this.types = checkedTypes(types);
		return this;
	}

	/**
	 * @return the types given, each once, in their order
	 * @throws IllegalArgumentException
	 *             for none, or one missing
	 */
	static Set<IRichTextImageType> checkedTypes(Collection<? extends IRichTextImageType> types)
	{
		return RichTextImageUploads.checkedTypes(types);
	}

	/**
	 * @return the widest and the highest image a user uploads, {@code null} for no limit
	 * @since 10.11.0
	 */
	public RichTextImageDimensions getMaxDimensions()
	{
		return maxDimensions;
	}

	/**
	 * @param maxDimensions
	 *            the widest and the highest image a user uploads, in pixels; {@code null} for no
	 *            limit, as by default. The header of the file says it, read without decoding the
	 *            picture: an image of a type that cannot tell it is refused.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public RichTextImageUploadBehavior setMaxDimensions(RichTextImageDimensions maxDimensions)
	{
		this.maxDimensions = maxDimensions;
		return this;
	}

	/**
	 * @return whether users keep images permanently, and find those kept
	 * @since 10.11.0
	 */
	public boolean isPermanentImages()
	{
		return permanentImages;
	}

	/**
	 * @param permanentImages
	 *            whether users keep the images they upload permanently, those they ticked, and find
	 *            those kept, by the store ({@link IRichTextImageStore#storePermanently(String,
	 *            String)}, {@link IRichTextImageStore#findPermanent(String, int)}); {@code false}
	 *            by default
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public RichTextImageUploadBehavior setPermanentImages(boolean permanentImages)
	{
		this.permanentImages = permanentImages;
		return this;
	}

	/**
	 * @return how many images kept permanently a search answers at most
	 * @since 10.11.0
	 */
	public int getMaxFound()
	{
		return maxFound;
	}

	/**
	 * @param maxFound
	 *            how many images kept permanently a search answers at most, at least one;
	 *            {@link #DEFAULT_MAX_FOUND} by default
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public RichTextImageUploadBehavior setMaxFound(int maxFound)
	{
		this.maxFound = Math.max(maxFound, 1);
		return this;
	}

	/**
	 * @return the media types of the images a user uploads, as the editor in the browser takes them
	 */
	public List<String> getMimeTypes()
	{
		return rules().mimeTypes();
	}

	/**
	 * @return the URL the editor posts the images to
	 */
	public String getUploadUrl()
	{
		return getCallbackUrl().toString();
	}

	/**
	 * Answers the request with what it answers, not by rendering the page.
	 */
	@Override
	public boolean rendersPage()
	{
		return false;
	}

	/**
	 * @return whether the component takes images now; {@code true} by default
	 */
	protected boolean isUploadAllowed()
	{
		return true;
	}

	/**
	 * @return whether users keep images permanently now, and find those kept: with
	 *         {@link #setPermanentImages(boolean) permanent images}, while
	 *         {@link #isUploadAllowed() uploads are allowed}
	 * @since 10.11.0
	 */
	protected boolean isPermanentAllowed()
	{
		return permanentImages && isUploadAllowed();
	}

	/**
	 * @param url
	 *            the address of an image
	 * @return whether this behavior took the image on this page, and has not deleted it since
	 * @since 10.11.0
	 */
	public boolean isUploaded(String url)
	{
		return uploaded.isUploaded(url);
	}

	/**
	 * @return the addresses of the images this behavior took on this page, and has not deleted
	 *         since, the oldest first
	 * @since 10.11.0
	 */
	public Set<String> getUploaded()
	{
		return Collections.unmodifiableSet(uploaded.getUploaded());
	}

	/**
	 * Forgets images this behavior took, as the text that holds them was saved: it no longer
	 * deletes them when asked to.
	 *
	 * @param urls
	 *            the addresses of the images
	 * @since 10.11.0
	 */
	public void forget(Collection<String> urls)
	{
		uploaded.forget(urls);
	}

	/**
	 * Keeps images this behavior took on this page permanently, by the store, with the names of
	 * their files, and forgets them: it no longer deletes them when asked to. It leaves the others
	 * alone.
	 *
	 * @param urls
	 *            the addresses of the images
	 * @return the addresses of the images kept
	 * @since 10.11.0
	 */
	public Set<String> storePermanently(Collection<String> urls)
	{
		return uploaded.storePermanently(store, urls);
	}

	/**
	 * Finds the images kept permanently, by the store: those of an address the editor takes,
	 * {@link #getMaxFound() so many} at most.
	 *
	 * @param query
	 *            what the user typed
	 * @return the images found
	 * @since 10.11.0
	 */
	public List<RichTextStoredImage> findPermanent(String query)
	{
		return RichTextImageUploads.findPermanent(store, query, rules());
	}

	/**
	 * Deletes images this behavior took on this page, by the store, and forgets them; it leaves
	 * the others alone.
	 *
	 * @param urls
	 *            the addresses of the images
	 * @return the addresses of the images deleted
	 * @since 10.11.0
	 */
	public Set<String> deleteUploaded(Collection<String> urls)
	{
		return uploaded.deleteUploaded(store, urls);
	}

	/**
	 * Deletes an image by a store, logging what went wrong.
	 *
	 * @return whether the store did not fail
	 */
	static boolean delete(IRichTextImageStore store, String url)
	{
		return RichTextImageUploads.delete(store, url);
	}

	/**
	 * @return the rules the behavior checks the uploads against now
	 */
	private RichTextImageUploads.Rules rules()
	{
		return new RichTextImageUploads.Rules(maxSize, types, maxDimensions,
			isImagesFromSiteOnly(), isPermanentAllowed(), maxFound);
	}

	@Override
	public void onRequest()
	{
		Component component = getComponent();
		JSONObject answer = RichTextImageUploads.answer(RequestCycle.get().getRequest(), rules(),
			store, uploaded, isUploadAllowed(), name -> RichTextEditor.text(component, name));
		RequestCycle.get().scheduleRequestHandlerAfterCurrent(new TextRequestHandler(
			"application/json", "UTF-8", answer.toString()));
		if (answer.has("url"))
		{
			onUploaded(answer.getString("url"));
		}
	}

	/**
	 * @param json
	 *            a JSON array of addresses
	 * @return the addresses, those that are strings; none for no such array
	 */
	static List<String> urlsOf(String json)
	{
		return RichTextImageUploads.urlsOf(json);
	}

	/**
	 * Called once the store kept an image, with the address it answered. Does nothing by default.
	 *
	 * @param url
	 *            the address of the image
	 * @since 10.11.0
	 */
	protected void onUploaded(String url)
	{
	}

	/**
	 * @return whether the behavior is on an editor that keeps the images of the site alone
	 */
	private boolean isImagesFromSiteOnly()
	{
		return getComponent() instanceof AbstractRichTextEditor<?> editor &&
			editor.isImagesFromSiteOnly();
	}

	/**
	 * @return a size as the messages say it, as the editor in the browser does: 2 MB, 1.5 MB, 300 KB
	 */
	static String sizeText(long bytes)
	{
		return RichTextImageUploads.sizeText(bytes);
	}
}
