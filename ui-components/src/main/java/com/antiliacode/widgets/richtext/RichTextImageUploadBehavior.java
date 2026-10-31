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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadByteCountLimitException;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.FileUploadSizeException;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
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
 * A {@link RichTextEditor} with {@link RichTextEditor#setImageStore(IRichTextImageStore) a store}
 * has one already. Add one to a page for the editors of React and Svelte in it, and give them
 * {@code uploadImageTo(}{@link #getUploadUrl()}{@code )} as their {@code onUpload}.
 *
 * @since 11.0.0
 */
public class RichTextImageUploadBehavior extends AbstractAjaxBehavior
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(RichTextImageUploadBehavior.class);

	/** The name the editor posts the file under. */
	public static final String FILE_PARAMETER = "file";

	/**
	 * The name the editor posts the addresses of the images to delete under, as a JSON array.
	 *
	 * @since 10.11.0
	 */
	public static final String REMOVED_PARAMETER = "removed";

	/** The largest image a user uploads, unless set otherwise. */
	public static final Bytes DEFAULT_MAX_SIZE = Bytes.megabytes(5);

	/** Room for the rest of the request besides the file: its boundaries and headers. */
	private static final long OVERHEAD = 16 * 1024;

	/** The name of the upload, for the progress Wicket keeps of uploads. */
	private static final String UPLOAD_ID = "antilia-rich-text-image";

	/** The most addresses of the images taken a behavior remembers, the oldest forgotten first. */
	private static final int REMEMBERED = 1000;

	private final IRichTextImageStore store;

	private Bytes maxSize = DEFAULT_MAX_SIZE;

	private Set<IRichTextImageType> types = new LinkedHashSet<>(RichTextImageType.ALL);

	private RichTextImageDimensions maxDimensions;

	/** The addresses of the images taken on this page, which it deletes when asked to. */
	private final Set<String> uploaded = new LinkedHashSet<>();

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
		Args.notEmpty(types, "types");
		Set<IRichTextImageType> checked = new LinkedHashSet<>();
		for (IRichTextImageType type : types)
		{
			Args.notNull(type, "type");
			Args.notEmpty(type.getMimeType(), "type.getMimeType()");
			checked.add(type);
		}
		return checked;
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
	 * @return the media types of the images a user uploads, as the editor in the browser takes them
	 */
	public List<String> getMimeTypes()
	{
		return types.stream().map(IRichTextImageType::getMimeType).distinct().toList();
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
	 * @param url
	 *            the address of an image
	 * @return whether this behavior took the image on this page, and has not deleted it since
	 * @since 10.11.0
	 */
	public boolean isUploaded(String url)
	{
		return url != null && uploaded.contains(url.trim());
	}

	/**
	 * @return the addresses of the images this behavior took on this page, and has not deleted
	 *         since, the oldest first
	 * @since 10.11.0
	 */
	public Set<String> getUploaded()
	{
		return Collections.unmodifiableSet(uploaded);
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
		// a copy: the addresses may be a view of those remembered
		List.copyOf(urls).forEach(uploaded::remove);
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
		Set<String> deleted = new LinkedHashSet<>();
		for (String url : List.copyOf(urls))
		{
			if (isUploaded(url))
			{
				String address = url.trim();
				if (delete(store, address))
				{
					deleted.add(address);
				}
				uploaded.remove(address);
			}
		}
		return deleted;
	}

	/**
	 * Deletes an image by a store, logging what went wrong.
	 *
	 * @return whether the store did not fail
	 */
	static boolean delete(IRichTextImageStore store, String url)
	{
		try
		{
			store.delete(url);
			return true;
		}
		catch (IOException | RuntimeException e)
		{
			LOG.error("Could not delete the image {}", url, e);
			return false;
		}
	}

	@Override
	public void onRequest()
	{
		JSONObject answer = new JSONObject();
		List<String> removed = removed();
		if (removed != null)
		{
			answer.put("deleted", new JSONArray(isUploadAllowed() ? deleteUploaded(removed) :
				Set.of()));
		}
		else
		{
			try
			{
				answer.put("url", receive());
			}
			catch (Refusal refusal)
			{
				answer.put("error", refusal.getMessage());
			}
		}
		RequestCycle.get().scheduleRequestHandlerAfterCurrent(new TextRequestHandler(
			"application/json", "UTF-8", answer.toString()));
	}

	/**
	 * @return the addresses of the images the request asks to delete, {@code null} for a request
	 *         that does not: an upload
	 */
	private static List<String> removed()
	{
		RequestCycle cycle = RequestCycle.get();
		if (cycle.getRequest() instanceof ServletWebRequest request)
		{
			String type = request.getContainerRequest().getContentType();
			if (type != null && type.toLowerCase().startsWith("multipart/"))
			{
				return null;
			}
		}
		String json = cycle.getRequest().getPostParameters().getParameterValue(REMOVED_PARAMETER)
			.toOptionalString();
		return json == null ? null : urlsOf(json);
	}

	/**
	 * @param json
	 *            a JSON array of addresses
	 * @return the addresses, those that are strings; none for no such array
	 */
	static List<String> urlsOf(String json)
	{
		List<String> urls = new ArrayList<>();
		try
		{
			JSONArray array = new JSONArray(json);
			for (int i = 0; i < array.length() && i < REMEMBERED; i++)
			{
				if (array.opt(i) instanceof String url && !Strings.isEmpty(url.trim()))
				{
					urls.add(url.trim());
				}
			}
		}
		catch (JSONException e)
		{
			LOG.warn("The images to delete are no JSON array: {}", Strings.isEmpty(json) ? json :
				json.substring(0, Math.min(json.length(), 200)));
		}
		return urls;
	}

	/**
	 * Takes the image of the request, checks it and has the store keep it.
	 *
	 * @return the address the image is served at
	 */
	private String receive() throws Refusal
	{
		if (!isUploadAllowed())
		{
			throw refusal("uploadFailed", "");
		}
		RequestCycle cycle = RequestCycle.get();
		if (!(cycle.getRequest() instanceof ServletWebRequest request))
		{
			throw refusal("uploadFailed", "");
		}
		List<FileItem> items = List.of();
		try
		{
			MultipartServletWebRequest multipart = request.newMultipartWebRequest(
				Bytes.bytes(maxSize.bytes() + OVERHEAD), UPLOAD_ID);
			multipart.setFileMaxSize(maxSize);
			multipart.setFileCountMax(2);
			multipart.parseFileParts();
			Map<String, List<FileItem>> files = multipart.getFiles();
			items = files.getOrDefault(FILE_PARAMETER, List.of());
			if (items.size() != 1 || items.get(0).isFormField())
			{
				throw refusal("uploadFailed", "");
			}
			String url = keep(new FileUpload(items.get(0)));
			remember(url);
			return url;
		}
		catch (FileUploadSizeException tooLarge)
		{
			throw refusal("uploadTooLarge", fileName(tooLarge), sizeText(maxSize.bytes()));
		}
		catch (FileUploadException e)
		{
			LOG.warn("Could not read an image uploaded", e);
			throw refusal("uploadFailed", "");
		}
		finally
		{
			delete(items);
		}
	}

	/**
	 * Remembers the address of an image taken, forgetting the oldest beyond {@link #REMEMBERED}.
	 */
	private void remember(String url)
	{
		uploaded.remove(url);
		uploaded.add(url);
		Iterator<String> oldest = uploaded.iterator();
		while (uploaded.size() > REMEMBERED && oldest.hasNext())
		{
			oldest.next();
			oldest.remove();
		}
		onUploaded(url);
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
	 * Deletes the files of an upload, which the store kept a copy of.
	 */
	private static void delete(List<FileItem> items)
	{
		for (FileItem item : items)
		{
			try
			{
				item.delete();
			}
			catch (IOException e)
			{
				LOG.warn("Could not delete the file of an image uploaded", e);
			}
		}
	}

	/**
	 * Checks an image and has the store keep it.
	 *
	 * @return the address the image is served at
	 */
	private String keep(FileUpload upload) throws Refusal
	{
		String name = upload.getClientFileName();
		if (upload.getSize() > maxSize.bytes())
		{
			throw refusal("uploadTooLarge", name, sizeText(maxSize.bytes()));
		}
		IRichTextImageType type;
		try (InputStream input = upload.getInputStream())
		{
			type = IRichTextImageType.detect(input.readNBytes(IRichTextImageType.HEAD), types);
		}
		catch (IOException e)
		{
			LOG.warn("Could not read the image {} uploaded", name, e);
			throw refusal("uploadFailed", name);
		}
		if (type == null)
		{
			throw refusal("uploadType", name, types.stream().map(IRichTextImageType::getLabel)
				.distinct().collect(Collectors.joining(", ")));
		}
		checkDimensions(upload, type, name);
		String url;
		try
		{
			url = store.store(upload, type);
		}
		catch (IOException | RuntimeException e)
		{
			LOG.error("Could not keep the image {} uploaded", name, e);
			throw refusal("uploadFailed", name);
		}
		finally
		{
			upload.closeStreams();
		}
		if (!RichTextPolicy.isSafeUrl(url) ||
			(isImagesFromSiteOnly() && !RichTextPolicy.isSiteUrl(url)))
		{
			LOG.error("The address {} of the image {} uploaded is not one the editor takes", url,
				name);
			throw refusal("uploadFailed", name);
		}
		return url.trim();
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
	 * Checks the width and the height of an image against the limit, if any, as the header of its
	 * file says them.
	 */
	private void checkDimensions(FileUpload upload, IRichTextImageType type, String name)
		throws Refusal
	{
		RichTextImageDimensions max = maxDimensions;
		if (max == null)
		{
			return;
		}
		RichTextImageDimensions dimensions;
		try (InputStream input = upload.getInputStream())
		{
			dimensions = type.readDimensions(input);
		}
		catch (IOException | RuntimeException e)
		{
			LOG.warn("Could not read the dimensions of the image {} uploaded", name, e);
			throw refusal("uploadFailed", name);
		}
		if (dimensions == null)
		{
			LOG.warn("The header of the image {} uploaded, of the type {}, does not tell its " +
				"dimensions", name, type.getLabel());
			throw refusal("uploadFailed", name);
		}
		if (!dimensions.fits(max))
		{
			throw refusal("uploadTooBig", name, dimensions.toString(), max.toString());
		}
	}

	/**
	 * @return the name of the file a limit refused, which commons-fileupload tells
	 */
	private static String fileName(FileUploadException e)
	{
		return e instanceof FileUploadByteCountLimitException limit && limit.getFileName() != null ?
			limit.getFileName() : "";
	}

	/**
	 * @return the reason the image was not taken, in the language of the component: one of the
	 *         texts of the editor, {0}, {1} standing for the values
	 */
	private Refusal refusal(String key, String... values)
	{
		Component component = getComponent();
		String text = RichTextEditor.text(component, key);
		for (int i = 0; i < values.length; i++)
		{
			String value = i == 0 && Strings.isEmpty(values[i]) ?
				RichTextEditor.text(component, "theImage") : values[i];
			text = text.replace("{" + i + "}", value);
		}
		return new Refusal(text);
	}

	/**
	 * @return a size as the messages say it, as the editor in the browser does: 2 MB, 1.5 MB, 300 KB
	 */
	static String sizeText(long bytes)
	{
		long[] sizes = { 1024L * 1024 * 1024, 1024L * 1024, 1024L };
		String[] units = { "GB", "MB", "KB" };
		for (int i = 0; i < sizes.length; i++)
		{
			if (bytes >= sizes[i])
			{
				double value = Math.round(bytes * 10.0 / sizes[i]) / 10.0;
				String number = value == Math.rint(value) ? String.valueOf((long)value) :
					String.valueOf(value);
				return number + " " + units[i];
			}
		}
		return bytes + " B";
	}

	/** Why an image was not taken, in words for the user. */
	private static final class Refusal extends Exception
	{
		private static final long serialVersionUID = 1L;

		Refusal(String message)
		{
			super(message, null, false, false);
		}
	}
}
