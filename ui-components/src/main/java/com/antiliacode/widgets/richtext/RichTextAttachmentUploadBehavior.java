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
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;

import com.antiliacode.widgets.upload.IUploadFileFormat;
import com.antiliacode.widgets.upload.IUploadScanner;
import com.antiliacode.widgets.upload.UploadFileFormat;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Takes the files a user attaches to the text of a rich text editor, a PDF or a spreadsheet, and
 * answers the address each is served at, which goes into the text as a link: the resource of a
 * page's own the editor in the browser posts a file to, in a form under
 * {@value RichTextImageUploadBehavior#FILE_PARAMETER}. It answers JSON, {@code {"url": "..."}},
 * or {@code {"error": "..."}} saying why not, in the language of the page.
 * <p>
 * It checks the file before a {@link IRichTextAttachmentStore} keeps it: no larger than
 * {@link #setMaxSize(Bytes) the size allowed}, which the request is cut at, and of
 * {@link #setFormats(Collection) a format allowed} as its bytes say, whatever its name and the
 * type the browser gave, the whole of a text checked to be UTF-8 without NUL; then
 * {@link #setScanner(IUploadScanner) the scanner}, if any, accepts it, refuses it, or answers a
 * cleaned copy. The address the store answers must be one the editor and its cleaning take.
 * <p>
 * It deletes the files it took again when asked to, by a POST of the addresses as a JSON array
 * under {@value RichTextImageUploadBehavior#REMOVED_PARAMETER}: only those it took on this page,
 * by the store's {@link IRichTextAttachmentStore#delete(String)}, and answers
 * {@code {"deleted": [...]}}.
 * <p>
 * A {@link RichTextEditor} with {@link RichTextEditor#setAttachmentStore(IRichTextAttachmentStore)
 * a store} has one already. Add one to a page for the editors of React and Svelte in it, and give
 * them {@code uploadFileTo(}{@link #getUploadUrl()}{@code )} as their {@code onAttach}.
 * {@link RichTextFiles The resource of the uploads} takes the files with the same checks too.
 *
 * @since 10.11.0
 */
public class RichTextAttachmentUploadBehavior extends AbstractAjaxBehavior
{
	private static final long serialVersionUID = 1L;

	/** The largest file a user attaches, unless set otherwise. */
	public static final Bytes DEFAULT_MAX_SIZE = Bytes.megabytes(10);

	private final IRichTextAttachmentStore store;

	private Bytes maxSize = DEFAULT_MAX_SIZE;

	private Set<IUploadFileFormat> formats = new LinkedHashSet<>(UploadFileFormat.ALL);

	private IUploadScanner scanner;

	/** The addresses of the files taken on this page, with the names of their files. */
	private final RichTextUploadedImages uploaded = new RichTextUploadedImages();

	/**
	 * @param store
	 *            what keeps the files and tells their addresses
	 */
	public RichTextAttachmentUploadBehavior(IRichTextAttachmentStore store)
	{
		this.store = Args.notNull(store, "store");
	}

	/**
	 * @return what keeps the files
	 */
	public IRichTextAttachmentStore getStore()
	{
		return store;
	}

	/**
	 * @return the largest file a user attaches
	 */
	public Bytes getMaxSize()
	{
		return maxSize;
	}

	/**
	 * @param maxSize
	 *            the largest file a user attaches, {@link #DEFAULT_MAX_SIZE} by default
	 * @return {@code this}
	 */
	public RichTextAttachmentUploadBehavior setMaxSize(Bytes maxSize)
	{
		this.maxSize = Args.notNull(maxSize, "maxSize");
		return this;
	}

	/**
	 * @return the formats of the files a user attaches, in their order
	 */
	public Set<IUploadFileFormat> getFormats()
	{
		return Collections.unmodifiableSet(formats);
	}

	/**
	 * @param formats
	 *            the formats of the files a user attaches, in the order a file is told by and the
	 *            messages name them: those of {@link UploadFileFormat} and of the application's
	 *            own; all those built in by default
	 * @return {@code this}
	 */
	public RichTextAttachmentUploadBehavior setFormats(
		Collection<? extends IUploadFileFormat> formats)
	{
		this.formats = RichTextAttachmentUploads.checkedFormats(formats);
		return this;
	}

	/**
	 * @return what scans each file before the store keeps it, {@code null} for none
	 */
	public IUploadScanner getScanner()
	{
		return scanner;
	}

	/**
	 * @param scanner
	 *            what scans each file before the store keeps it, an antivirus for example;
	 *            {@code null} for none, as by default
	 * @return {@code this}
	 */
	public RichTextAttachmentUploadBehavior setScanner(IUploadScanner scanner)
	{
		this.scanner = scanner;
		return this;
	}

	/**
	 * @return the formats as the editor in the browser takes them: the media type, the extension
	 *         and the label of each
	 */
	public JSONArray getFormatsJson()
	{
		return rules().toJson();
	}

	/**
	 * @return the URL the editor posts the files to
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
	 * @return whether the component takes files now; {@code true} by default
	 */
	protected boolean isUploadAllowed()
	{
		return true;
	}

	/**
	 * @param url
	 *            the address of a file
	 * @return whether this behavior took the file on this page, and has not deleted it since
	 */
	public boolean isUploaded(String url)
	{
		return uploaded.isUploaded(url);
	}

	/**
	 * @return the addresses of the files this behavior took on this page, and has not deleted
	 *         since, the oldest first
	 */
	public Set<String> getUploaded()
	{
		return Collections.unmodifiableSet(uploaded.getUploaded());
	}

	/**
	 * Forgets files this behavior took, as the text that holds them was saved: it no longer
	 * deletes them when asked to.
	 *
	 * @param urls
	 *            the addresses of the files
	 */
	public void forget(Collection<String> urls)
	{
		uploaded.forget(urls);
	}

	/**
	 * Deletes files this behavior took on this page, by the store, and forgets them; it leaves the
	 * others alone.
	 *
	 * @param urls
	 *            the addresses of the files
	 * @return the addresses of the files deleted
	 */
	public Set<String> deleteUploaded(Collection<String> urls)
	{
		return uploaded.deleteUploaded(url -> RichTextAttachmentUploads.delete(store, url), urls);
	}

	/**
	 * @return the rules the behavior checks the files against now
	 */
	RichTextAttachmentUploads.Rules rules()
	{
		return new RichTextAttachmentUploads.Rules(maxSize, formats, isFromSiteOnly(), scanner);
	}

	@Override
	public void onRequest()
	{
		Component component = getComponent();
		JSONObject answer = RichTextAttachmentUploads.answer(RequestCycle.get().getRequest(),
			rules(), store, uploaded, isUploadAllowed(), name -> RichTextEditor.text(component,
				name));
		RequestCycle.get().scheduleRequestHandlerAfterCurrent(new TextRequestHandler(
			"application/json", "UTF-8", answer.toString()));
	}

	/**
	 * @return whether the behavior is on an editor that keeps the images, and the files, of the
	 *         site alone
	 */
	private boolean isFromSiteOnly()
	{
		return getComponent() instanceof AbstractRichTextEditor<?> editor &&
			editor.isImagesFromSiteOnly();
	}
}
