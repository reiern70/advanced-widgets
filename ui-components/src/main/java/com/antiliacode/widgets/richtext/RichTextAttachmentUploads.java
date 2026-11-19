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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.Request;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.richtext.RichTextImageUploads.Refusal;
import com.antiliacode.widgets.richtext.RichTextImageUploads.Texts;
import com.antiliacode.widgets.upload.IUploadFileFormat;
import com.antiliacode.widgets.upload.IUploadScanner;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * What {@link RichTextAttachmentUploadBehavior}, a behavior of a page, and {@link RichTextFiles},
 * the resource of the application, share in taking the files users attach to the texts of the
 * rich text editors: the checks of a file before a store keeps it, by its size and its format as
 * its bytes say, the scanner, and the answers to the requests of the editor in the browser,
 * uploads and deletions.
 */
final class RichTextAttachmentUploads
{
	private static final Logger LOG = LoggerFactory.getLogger(RichTextAttachmentUploads.class);

	/** The text standing for a file without a name. */
	private static final String THE_FILE = "theFile";

	private RichTextAttachmentUploads()
	{
	}

	/**
	 * The rules of the files attached by an editor, or by an application.
	 *
	 * @param maxSize
	 *            the largest file
	 * @param formats
	 *            the formats taken, in the order a file is told by
	 * @param siteOnly
	 *            whether the addresses must be the site's own
	 * @param scanner
	 *            what scans each file before the store keeps it, {@code null} for none
	 */
	record Rules(Bytes maxSize, Set<IUploadFileFormat> formats, boolean siteOnly,
		IUploadScanner scanner) implements IClusterable
	{
		Rules
		{
			Args.notNull(maxSize, "maxSize");
			formats = checkedFormats(formats);
		}

		/**
		 * @return the formats as the editor in the browser takes them: the media type, the
		 *         extension and the label of each
		 */
		JSONArray toJson()
		{
			JSONArray array = new JSONArray();
			formats.forEach(format -> array.put(new JSONObject().put("type", format.getMimeType())
				.put("extension", format.getExtension()).put("label", format.getLabel())));
			return array;
		}
	}

	/**
	 * @return the formats given, each once, in their order
	 * @throws IllegalArgumentException
	 *             for none, or one missing
	 */
	static Set<IUploadFileFormat> checkedFormats(Collection<? extends IUploadFileFormat> formats)
	{
		Args.notEmpty(formats, "formats");
		Set<IUploadFileFormat> checked = new LinkedHashSet<>();
		for (IUploadFileFormat format : formats)
		{
			Args.notNull(format, "format");
			Args.notEmpty(format.getMimeType(), "format.getMimeType()");
			Args.notEmpty(format.getExtension(), "format.getExtension()");
			checked.add(format);
		}
		return checked;
	}

	/**
	 * Answers a request of the editor in the browser: an upload, a form of files, or the files to
	 * delete.
	 *
	 * @param allowed
	 *            whether the user attaches files now, and deletes those taken
	 * @return the answer
	 */
	static JSONObject answer(Request request, Rules rules, IRichTextAttachmentStore store,
		RichTextUploadedImages uploaded, boolean allowed, Texts texts)
	{
		JSONObject answer = new JSONObject();
		if (RichTextImageUploads.isMultipart(request))
		{
			try
			{
				if (!allowed)
				{
					throw RichTextImageUploads.refusalOf(texts, THE_FILE, "uploadFailed", "");
				}
				String[] taken = RichTextImageUploads.receive(request, rules.maxSize(), texts,
					THE_FILE, upload -> keep(upload, rules, store, texts));
				uploaded.remember(taken[0], taken[1]);
				answer.put("url", taken[0]);
			}
			catch (Refusal refusal)
			{
				answer.put("error", refusal.getMessage());
			}
		}
		else if (request.getPostParameters().getParameterValue(
			RichTextImageUploadBehavior.REMOVED_PARAMETER).toOptionalString() != null)
		{
			answer.put("deleted", new JSONArray(allowed ? uploaded.deleteUploaded(
				url -> delete(store, url), RichTextImageUploads.urlsOf(request.getPostParameters()
					.getParameterValue(RichTextImageUploadBehavior.REMOVED_PARAMETER)
					.toOptionalString())) : Set.of()));
		}
		else
		{
			answer.put("error", texts.text("uploadFailed").replace("{0}", texts.text(THE_FILE)));
		}
		return answer;
	}

	/**
	 * Deletes a file by a store, logging what went wrong.
	 *
	 * @return whether the store did not fail
	 */
	static boolean delete(IRichTextAttachmentStore store, String url)
	{
		try
		{
			store.delete(url);
			return true;
		}
		catch (IOException | RuntimeException e)
		{
			LOG.error("Could not delete the file {}", url, e);
			return false;
		}
	}

	/**
	 * Checks a file, has the scanner scan it and the store keep it.
	 *
	 * @return the address the file is served at
	 */
	private static String keep(FileUpload upload, Rules rules, IRichTextAttachmentStore store,
		Texts texts) throws Refusal
	{
		String name = upload.getClientFileName();
		IUploadFileFormat format = check(upload, rules, texts);
		FileUpload kept = RichTextImageUploads.scanned(upload, format, rules.scanner(), texts,
			THE_FILE, copy -> {
				if (check(copy, rules, texts) != format)
				{
					throw RichTextImageUploads.refusalOf(texts, THE_FILE, "uploadFailed", name);
				}
				return null;
			});
		String url;
		try
		{
			url = store.store(kept, format);
		}
		catch (IOException | RuntimeException e)
		{
			LOG.error("Could not keep the file {} uploaded", name, e);
			throw RichTextImageUploads.refusalOf(texts, THE_FILE, "uploadFailed", name);
		}
		finally
		{
			upload.closeStreams();
			if (kept != upload)
			{
				RichTextImageUploads.discard(kept);
			}
		}
		if (!RichTextImageUploads.isTaken(url, rules.siteOnly()))
		{
			LOG.error("The address {} of the file {} uploaded is not one the editor takes", url,
				name);
			throw RichTextImageUploads.refusalOf(texts, THE_FILE, "uploadFailed", name);
		}
		return url.trim();
	}

	/**
	 * Checks a file: its size and its format as its bytes say, the whole file for a format
	 * checking it so.
	 *
	 * @return its format
	 */
	private static IUploadFileFormat check(FileUpload upload, Rules rules, Texts texts)
		throws Refusal
	{
		String name = upload.getClientFileName();
		if (upload.getSize() > rules.maxSize().bytes())
		{
			throw RichTextImageUploads.refusalOf(texts, THE_FILE, "uploadTooLarge", name,
				RichTextImageUploads.sizeText(rules.maxSize().bytes()));
		}
		IUploadFileFormat format;
		try (InputStream input = upload.getInputStream())
		{
			// the whole file again for a head that leaves its format open: an Office document
			format = IUploadFileFormat.detect(input.readNBytes(IUploadFileFormat.HEAD), name,
				rules.formats(), upload::getInputStream);
		}
		catch (IOException e)
		{
			LOG.warn("Could not read the file {} uploaded", name, e);
			throw RichTextImageUploads.refusalOf(texts, THE_FILE, "uploadFailed", name);
		}
		boolean accepted;
		try (InputStream input = upload.getInputStream())
		{
			accepted = format != null && format.accepts(input);
		}
		catch (IOException | RuntimeException e)
		{
			LOG.warn("Could not read the file {} uploaded", name, e);
			throw RichTextImageUploads.refusalOf(texts, THE_FILE, "uploadFailed", name);
		}
		if (!accepted)
		{
			throw RichTextImageUploads.refusalOf(texts, THE_FILE, "attachmentType", name,
				labels(rules.formats()));
		}
		return format;
	}

	/**
	 * @return the labels of formats, as the messages name them
	 */
	static String labels(Collection<? extends IUploadFileFormat> formats)
	{
		return formats.stream().map(IUploadFileFormat::getLabel).distinct()
			.collect(Collectors.joining(", "));
	}

	/**
	 * @return the addresses of a JSON array, as the editor posts them
	 */
	static List<String> urlsOf(String json)
	{
		return RichTextImageUploads.urlsOf(json);
	}
}
