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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadByteCountLimitException;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.FileUploadSizeException;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

/**
 * What {@link RichTextImageUploadBehavior}, a behavior of a page, and {@link RichTextFiles}, the
 * resource of the application, share in taking the images users upload into the rich text
 * editors: the checks of a file before a store keeps it, by its size, its type as its bytes say
 * and its pixels as its header says, and the answers to the requests of the editor in the
 * browser, uploads, deletions, images kept permanently and searches among them.
 */
final class RichTextImageUploads
{
	private static final Logger LOG = LoggerFactory.getLogger(RichTextImageUploads.class);

	/** Room for the rest of the request besides the file: its boundaries and headers. */
	static final long OVERHEAD = 16 * 1024;

	/**
	 * The start of the name of an upload, for the progress Wicket keeps of uploads in the session:
	 * each upload has a name of its own, since uploads to the resource of the uploads run at once
	 * in a session, and the first one done would take the progress of the others away.
	 */
	private static final String UPLOAD_ID = "antilia-rich-text-upload-";

	/** Numbers the uploads, for their names. */
	private static final AtomicLong UPLOADS = new AtomicLong();

	/** The longest query of a search, in characters; the rest is cut. */
	private static final int LONGEST_QUERY = 100;

	private RichTextImageUploads()
	{
	}

	/**
	 * The rules of the uploads of an editor, or of an application.
	 *
	 * @param maxSize
	 *            the largest image
	 * @param types
	 *            the types taken, in the order the messages name them
	 * @param maxDimensions
	 *            the widest and the highest image, {@code null} for no limit
	 * @param siteOnly
	 *            whether the addresses must be the site's own
	 *            ({@link RichTextPolicy#isSiteUrl(String)})
	 * @param permanent
	 *            whether users keep images permanently, and find those kept, now
	 * @param maxFound
	 *            how many images kept permanently a search answers at most
	 */
	record Rules(Bytes maxSize, Set<IRichTextImageType> types, RichTextImageDimensions maxDimensions,
		boolean siteOnly, boolean permanent, int maxFound) implements IClusterable
	{
		Rules
		{
			Args.notNull(maxSize, "maxSize");
			types = checkedTypes(types);
			maxFound = Math.max(maxFound, 1);
		}

		/**
		 * @return the media types taken, as the editor in the browser takes them
		 */
		List<String> mimeTypes()
		{
			return types.stream().map(IRichTextImageType::getMimeType).distinct().toList();
		}
	}

	/** The texts of the answers, in the language of the user. */
	@FunctionalInterface
	interface Texts
	{
		/**
		 * @param name
		 *            the name of a text of the editor, {@code uploadFailed} for example
		 * @return its translation
		 */
		String text(String name);
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
	 * Answers a request of the editor in the browser: an upload, a form of files; the images to
	 * delete; the images to keep permanently; or a search among those kept.
	 *
	 * @param request
	 *            the request
	 * @param rules
	 *            the rules of the uploads
	 * @param store
	 *            what keeps the images
	 * @param uploaded
	 *            the images taken before, which the image taken joins
	 * @param allowed
	 *            whether the user uploads images now, and deletes those taken
	 * @param texts
	 *            the texts of the answers
	 * @return the answer
	 */
	static JSONObject answer(Request request, Rules rules, IRichTextImageStore store,
		RichTextUploadedImages uploaded, boolean allowed, Texts texts)
	{
		JSONObject answer = new JSONObject();
		if (isMultipart(request))
		{
			try
			{
				if (!allowed)
				{
					throw refusal(texts, "uploadFailed", "");
				}
				String[] taken = receive(request, rules, store, texts);
				uploaded.remember(taken[0], taken[1]);
				answer.put("url", taken[0]);
			}
			catch (Refusal refusal)
			{
				answer.put("error", refusal.getMessage());
			}
		}
		else if (parameter(request, RichTextImageUploadBehavior.REMOVED_PARAMETER) != null)
		{
			answer.put("deleted", new JSONArray(allowed ? uploaded.deleteUploaded(store,
				urlsOf(parameter(request, RichTextImageUploadBehavior.REMOVED_PARAMETER))) :
				Set.of()));
		}
		else if (parameter(request, RichTextImageUploadBehavior.PERMANENT_PARAMETER) != null)
		{
			answer.put("stored", new JSONArray(rules.permanent() ?
				uploaded.storePermanently(store, urlsOf(parameter(request,
					RichTextImageUploadBehavior.PERMANENT_PARAMETER))) : Set.of()));
		}
		else if (parameter(request, RichTextImageUploadBehavior.FIND_PARAMETER) != null)
		{
			JSONArray images = new JSONArray();
			if (rules.permanent())
			{
				findPermanent(store, parameter(request, RichTextImageUploadBehavior.FIND_PARAMETER),
					rules).forEach(image -> images.put(image.toJson()));
			}
			answer.put("images", images);
		}
		else
		{
			answer.put("error", texts.text("uploadFailed").replace("{0}", texts.text("theImage")));
		}
		return answer;
	}

	/**
	 * @return whether the request posts a form of files: an upload
	 */
	static boolean isMultipart(Request request)
	{
		if (request instanceof ServletWebRequest servlet)
		{
			String type = servlet.getContainerRequest().getContentType();
			return type != null && type.toLowerCase().startsWith("multipart/");
		}
		return false;
	}

	/**
	 * @return a parameter posted, {@code null} for none
	 */
	private static String parameter(Request request, String name)
	{
		return request.getPostParameters().getParameterValue(name).toOptionalString();
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
			for (int i = 0; i < array.length() && i < RichTextUploadedImages.REMEMBERED; i++)
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
	 * Deletes an image by a store, logging what went wrong; never one it keeps permanently.
	 *
	 * @return whether the store did not fail
	 */
	static boolean delete(IRichTextImageStore store, String url)
	{
		try
		{
			if (store.isPermanent(url))
			{
				return false;
			}
			store.delete(url);
			return true;
		}
		catch (IOException | RuntimeException e)
		{
			LOG.error("Could not delete the image {}", url, e);
			return false;
		}
	}

	/**
	 * Finds the images kept permanently, by the store: those of an address the editor takes,
	 * {@link Rules#maxFound() so many} at most.
	 */
	static List<RichTextStoredImage> findPermanent(IRichTextImageStore store, String query,
		Rules rules)
	{
		String text = query == null ? "" : query.trim();
		if (text.length() > LONGEST_QUERY)
		{
			text = text.substring(0, LONGEST_QUERY);
		}
		List<RichTextStoredImage> found;
		try
		{
			found = store.findPermanent(text, rules.maxFound());
		}
		catch (IOException | RuntimeException e)
		{
			LOG.error("Could not find the images kept permanently for {}", text, e);
			return List.of();
		}
		if (found == null)
		{
			return List.of();
		}
		List<RichTextStoredImage> taken = new ArrayList<>();
		for (RichTextStoredImage image : found)
		{
			if (taken.size() >= rules.maxFound())
			{
				break;
			}
			if (image != null && isTaken(image.url(), rules.siteOnly()))
			{
				taken.add(image);
			}
			else if (image != null)
			{
				LOG.warn("The address {} of an image kept permanently is not one the editor takes",
					image.url());
			}
		}
		return taken;
	}

	/**
	 * Takes the image of the request, checks it and has the store keep it.
	 *
	 * @return the address the image is served at, and the name of its file
	 */
	private static String[] receive(Request request, Rules rules, IRichTextImageStore store,
		Texts texts) throws Refusal
	{
		if (!(request instanceof ServletWebRequest servlet))
		{
			throw refusal(texts, "uploadFailed", "");
		}
		Bytes maxSize = rules.maxSize();
		List<FileItem> items = List.of();
		try
		{
			// the whole request no larger than the file and its boundaries: commons-fileupload
			// cuts it as it reads, and refuses one telling a larger length before reading it
			MultipartServletWebRequest multipart = servlet.newMultipartWebRequest(
				Bytes.bytes(maxSize.bytes() + OVERHEAD), UPLOAD_ID + UPLOADS.incrementAndGet());
			multipart.setFileMaxSize(maxSize);
			multipart.setFileCountMax(2);
			multipart.parseFileParts();
			Map<String, List<FileItem>> files = multipart.getFiles();
			items = files.getOrDefault(RichTextImageUploadBehavior.FILE_PARAMETER, List.of());
			if (items.size() != 1 || items.get(0).isFormField())
			{
				throw refusal(texts, "uploadFailed", "");
			}
			FileUpload upload = new FileUpload(items.get(0));
			return new String[] { keep(upload, rules, store, texts), upload.getClientFileName() };
		}
		catch (FileUploadSizeException tooLarge)
		{
			throw refusal(texts, "uploadTooLarge", fileName(tooLarge), sizeText(maxSize.bytes()));
		}
		catch (FileUploadException e)
		{
			LOG.warn("Could not read an image uploaded", e);
			throw refusal(texts, "uploadFailed", "");
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
	private static String keep(FileUpload upload, Rules rules, IRichTextImageStore store,
		Texts texts) throws Refusal
	{
		String name = upload.getClientFileName();
		if (upload.getSize() > rules.maxSize().bytes())
		{
			throw refusal(texts, "uploadTooLarge", name, sizeText(rules.maxSize().bytes()));
		}
		IRichTextImageType type;
		try (InputStream input = upload.getInputStream())
		{
			type = IRichTextImageType.detect(input.readNBytes(IRichTextImageType.HEAD),
				rules.types());
		}
		catch (IOException e)
		{
			LOG.warn("Could not read the image {} uploaded", name, e);
			throw refusal(texts, "uploadFailed", name);
		}
		if (type == null)
		{
			throw refusal(texts, "uploadType", name, rules.types().stream()
				.map(IRichTextImageType::getLabel).distinct().collect(Collectors.joining(", ")));
		}
		checkDimensions(upload, type, name, rules.maxDimensions(), texts);
		String url;
		try
		{
			url = store.store(upload, type);
		}
		catch (IOException | RuntimeException e)
		{
			LOG.error("Could not keep the image {} uploaded", name, e);
			throw refusal(texts, "uploadFailed", name);
		}
		finally
		{
			upload.closeStreams();
		}
		if (!isTaken(url, rules.siteOnly()))
		{
			LOG.error("The address {} of the image {} uploaded is not one the editor takes", url,
				name);
			throw refusal(texts, "uploadFailed", name);
		}
		return url.trim();
	}

	/**
	 * @return whether the editor takes an image of the address: a safe one, of the site when the
	 *         editor keeps the images of the site alone
	 */
	static boolean isTaken(String url, boolean siteOnly)
	{
		return RichTextPolicy.isSafeUrl(url) && (!siteOnly || RichTextPolicy.isSiteUrl(url));
	}

	/**
	 * Checks the width and the height of an image against the limit, if any, as the header of its
	 * file says them.
	 */
	private static void checkDimensions(FileUpload upload, IRichTextImageType type, String name,
		RichTextImageDimensions max, Texts texts) throws Refusal
	{
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
			throw refusal(texts, "uploadFailed", name);
		}
		if (dimensions == null)
		{
			LOG.warn("The header of the image {} uploaded, of the type {}, does not tell its " +
				"dimensions", name, type.getLabel());
			throw refusal(texts, "uploadFailed", name);
		}
		if (!dimensions.fits(max))
		{
			throw refusal(texts, "uploadTooBig", name, dimensions.toString(), max.toString());
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
	 * @return the reason the image was not taken, in the language of the user: one of the texts
	 *         of the editor, {0}, {1} standing for the values
	 */
	private static Refusal refusal(Texts texts, String key, String... values)
	{
		String text = texts.text(key);
		for (int i = 0; i < values.length; i++)
		{
			String value = i == 0 && Strings.isEmpty(values[i]) ? texts.text("theImage") :
				values[i];
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
