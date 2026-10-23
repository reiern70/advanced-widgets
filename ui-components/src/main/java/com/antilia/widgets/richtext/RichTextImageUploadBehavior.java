/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.richtext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
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

import com.github.openjson.JSONObject;

/**
 * Takes the images a user uploads into a rich text editor, and answers the address each is served
 * at: the resource of a page's own the editor in the browser posts a file to, in a form under
 * {@value #FILE_PARAMETER}. It answers JSON, {@code {"url": "..."}}, or {@code {"error": "..."}}
 * saying why not, in the language of the page, which the editor shows.
 * <p>
 * It checks the file before a {@link RichTextImageStore} keeps it: no larger than
 * {@link #setMaxSize(Bytes) the size allowed}, which the request is cut at, and of
 * {@link #setTypes(Set) a type allowed} as its first bytes say, whatever its name and the type the
 * browser gave. The address the store answers must be one the editor and its cleaning take
 * ({@link RichTextPolicy#isSafeUrl(String)}): never {@code data:} nor {@code javascript:}.
 * <p>
 * A {@link RichTextEditor} with {@link RichTextEditor#setImageStore(RichTextImageStore) a store}
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

	/** The largest image a user uploads, unless set otherwise. */
	public static final Bytes DEFAULT_MAX_SIZE = Bytes.megabytes(5);

	/** Room for the rest of the request besides the file: its boundaries and headers. */
	private static final long OVERHEAD = 16 * 1024;

	/** The name of the upload, for the progress Wicket keeps of uploads. */
	private static final String UPLOAD_ID = "antilia-rich-text-image";

	private final RichTextImageStore store;

	private Bytes maxSize = DEFAULT_MAX_SIZE;

	private Set<RichTextImageType> types = EnumSet.allOf(RichTextImageType.class);

	/**
	 * @param store
	 *            what keeps the images and tells their addresses
	 */
	public RichTextImageUploadBehavior(RichTextImageStore store)
	{
		this.store = Args.notNull(store, "store");
	}

	/**
	 * @return what keeps the images
	 */
	public RichTextImageStore getStore()
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
	 * @return the types of the images a user uploads
	 */
	public Set<RichTextImageType> getTypes()
	{
		return Collections.unmodifiableSet(types);
	}

	/**
	 * @param types
	 *            the types of the images a user uploads, all of them by default
	 * @return {@code this}
	 */
	public RichTextImageUploadBehavior setTypes(Set<RichTextImageType> types)
	{
		Args.notEmpty(types, "types");
		this.types = EnumSet.copyOf(types);
		return this;
	}

	/**
	 * @return the media types of the images a user uploads, as the editor in the browser takes them
	 */
	public List<String> getMimeTypes()
	{
		return types.stream().map(RichTextImageType::getMimeType).toList();
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

	@Override
	public void onRequest()
	{
		JSONObject answer = new JSONObject();
		try
		{
			answer.put("url", receive());
		}
		catch (Refusal refusal)
		{
			answer.put("error", refusal.getMessage());
		}
		RequestCycle.get().scheduleRequestHandlerAfterCurrent(new TextRequestHandler(
			"application/json", "UTF-8", answer.toString()));
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
			return keep(new FileUpload(items.get(0)));
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
		RichTextImageType type;
		try (InputStream input = upload.getInputStream())
		{
			type = RichTextImageType.detect(input.readNBytes(RichTextImageType.HEAD));
		}
		catch (IOException e)
		{
			LOG.warn("Could not read the image {} uploaded", name, e);
			throw refusal("uploadFailed", name);
		}
		if (type == null || !types.contains(type))
		{
			throw refusal("uploadType", name, types.stream().map(RichTextImageType::getLabel)
				.collect(Collectors.joining(", ")));
		}
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
		if (!RichTextPolicy.isSafeUrl(url))
		{
			LOG.error("The address {} of the image {} uploaded is not one the editor takes", url,
				name);
			throw refusal("uploadFailed", name);
		}
		return url.trim();
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
