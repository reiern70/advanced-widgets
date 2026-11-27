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
package com.antiliacode.widgets.upload;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxAttributeName;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField;
import org.apache.wicket.markup.html.form.upload.resource.IUploadsFileManager;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.theme.Looks;
import com.antiliacode.widgets.upload.FilePickerBehavior.Design;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

/**
 * A {@link FileUploadToResourceField} whose files go in chunks over the tus protocol to a
 * {@link ResumableUploadResourceReference}, each paused, resumed or canceled by the user, and going
 * on after a broken connection or a reload of the page without sending again what the server
 * holds. Around the field's input it draws the file picker of the theme, as a zone to drop files
 * on or a button, and a row of each file: its name, its size, its progress bar telling what was
 * sent, how fast and how long it still takes, and its buttons pausing, resuming and canceling it;
 * then buttons pausing and resuming all.
 * <p>
 * Wicket 11.0 has no resumable upload: this field and the reference are those of Wicket 11.1 and
 * 12, {@code ResumableUploadToResourceField} in wicket-extensions, with the same names and
 * settings. Moving to Wicket 11.1, an application keeps this field, whose widgets build on
 * Wicket's there, and imports the reference from Wicket.
 * <p>
 * The application mounts the reference in its {@code init()}, with the
 * {@link IUploadsFileManager} the whole files go to, and its limits; the field tells the browser
 * those it has to know. Once the server stored a whole file,
 * {@link #onUploadSuccess(AjaxRequestTarget, List)} gets it;
 * {@link #onUploadFailure(AjaxRequestTarget, String)} and
 * {@link #onUploadCanceled(AjaxRequestTarget)} hear of the others, as for any
 * {@link FileUploadToResourceField}. The field's markup is an {@code <input type="file">}.
 *
 * <pre>
 * add(new ResumableUploadField("files")
 * {
 * 	&#64;Override
 * 	protected void onUploadSuccess(AjaxRequestTarget target, List&lt;UploadInfo&gt; infos)
 * 	{
 * 		...
 * 	}
 * });
 * </pre>
 *
 * After a broken connection a file goes on by itself; once the connection is lost for longer than
 * {@link #setRetryTimeout(Duration) the retry timeout}, a VPN that went down for example, the field
 * waits for the server to answer again and then asks above its rows whether to resume the files or
 * cancel them, unless it is not {@link #setAskingToResume(boolean) asking}.
 * <p>
 * The browser keeps the address of each upload not finished in its {@code localStorage}, with the
 * place of the field, {@link #getRememberedAs()}: after a reload, the field in the same place shows
 * them, how far each got, and asks whether to go on with them, which they do once their files are
 * chosen again; {@link #setRemembering(boolean)} turns that off. With the uploads belonging to the
 * user rather than the session, see {@link ResumableUploadResourceReference#getCurrentOwner()},
 * that holds after the session expired too. Whether a page rendered before the session ended can
 * go on with them depends on the key that signs the grant of the field, see
 * {@link ResumableUploadResourceReference#setGrantSigner(IUploadGrantSigner)}: when the server
 * refuses the grant, the field tells the user to load the page again, and the page loaded offers
 * to go on with the uploads. {@link #getClientSideResumeQuestionCallBack()} lets
 * the page ask in a dialog of its own. A field rendered again via Ajax stops the files it was
 * sending: render it once, and update the components around it. The guard of the uploads asks
 * before the page is left while files are being sent, unless {@link #setKeepingOnPage(boolean)}
 * says otherwise.
 *
 * @since 11.0.0
 */
public abstract class ResumableUploadField extends FileUploadToResourceField
{
	private static final long serialVersionUID = 1L;

	/**
	 * How often the browser asks the server how many bytes it holds while a chunk is being sent,
	 * for the progress: the bytes the browser handed to the network may be megabytes ahead.
	 */
	public static final int PROGRESS_POLL_MILLIS = 1000;

	/** The size of a chunk by default. */
	public static final Bytes DEFAULT_CHUNK_SIZE = Bytes.megabytes(8);

	/** The number of files sent at the same time by default. */
	public static final int DEFAULT_PARALLEL_UPLOADS = 2;

	/**
	 * How long an upload keeps trying again by itself by default, before it takes the connection
	 * for lost.
	 */
	public static final Duration DEFAULT_RETRY_TIMEOUT = Duration.ofSeconds(40);

	/**
	 * How often at most an upload that lost the connection looks whether the server answers again,
	 * by default.
	 */
	public static final Duration DEFAULT_RECONNECT_CHECK_INTERVAL = Duration.ofSeconds(30);

	/** How long a request may send or get nothing before it is given up, by default. */
	public static final Duration DEFAULT_STALL_TIMEOUT = Duration.ofSeconds(30);

	/**
	 * How long after an upload last sent something another page takes it for in use, by default.
	 */
	public static final Duration DEFAULT_IN_USE_TIMEOUT = Duration.ofSeconds(20);

	/** The first waits before looking whether the server answers again, in milliseconds. */
	private static final long[] RECONNECT_CHECK_STEPS = { 5000, 10000, 20000 };

	/** The resource key of the text of a file larger than the limits allow. */
	public static final String UPLOAD_TOO_LARGE_RESOURCE_KEY = "resumableUploadTooLarge";

	/** The resource key of the text of a session that has too many uploads running at once. */
	public static final String TOO_MANY_UPLOADS_RESOURCE_KEY = "resumableUploadTooMany";

	/** The resource key of the text of a session whose uploads together are too large. */
	public static final String TOO_MUCH_RESOURCE_KEY = "resumableUploadTooMuch";

	/** The resource key of the text of a server that has no room for a file now. */
	public static final String NO_ROOM_RESOURCE_KEY = "resumableUploadNoRoom";

	/** The resource key of the text of an upload the server no longer has. */
	public static final String UPLOAD_LOST_RESOURCE_KEY = "resumableUploadLost";

	/**
	 * The resource key of the text of a grant the server refuses: one signed with a key it no
	 * longer has, that of an ended session for example, expired, or made for another owner. The
	 * page has to be loaded again.
	 */
	public static final String GRANT_REFUSED_RESOURCE_KEY = "resumableUploadGrantRefused";

	/** The resource key of the text of an upload that failed, for no reason the browser knows. */
	public static final String UPLOAD_FAILED_RESOURCE_KEY = "uploadToResourceFailed";

	/** The resource key of the text of an upload whose requests got no answer. */
	public static final String CONNECTION_LOST_RESOURCE_KEY = "uploadConnectionLost";

	/** The resource key of the text of an upload without files. */
	static final String NO_FILE_SELECTED_RESOURCE_KEY = "wicket.no.files.selected";

	/** The waits before the tries again, in milliseconds, the last one again and again. */
	private static final long[] RETRY_STEPS = { 0, 1000, 3000, 5000, 10000, 20000, 30000 };

	private static final Set<String> RESUMABLE_ERROR_KEYS = Set.of(UPLOAD_TOO_LARGE_RESOURCE_KEY,
		TOO_MANY_UPLOADS_RESOURCE_KEY, TOO_MUCH_RESOURCE_KEY, NO_ROOM_RESOURCE_KEY,
		UPLOAD_LOST_RESOURCE_KEY, GRANT_REFUSED_RESOURCE_KEY);

	private static final Set<String> ERROR_RESOURCE_KEYS = Set.of(NO_FILE_SELECTED_RESOURCE_KEY,
		CONNECTION_LOST_RESOURCE_KEY, Form.UPLOAD_TOO_LARGE_RESOURCE_KEY,
		Form.UPLOAD_SINGLE_FILE_TOO_LARGE_RESOURCE_KEY, Form.UPLOAD_TOO_MANY_FILES_RESOURCE_KEY);

	/** The texts of the uploader, under {@code ResumableUploader.} in the Initializer's bundles. */
	static final String[] TEXTS = { "list", "ready", "queued", "starting", "sent", "rate",
			"remaining", "paused", "retrying", "offline", "interrupted", "disconnected", "done",
			"failed", "lost", "serverError", "canceled", "tooLarge", "start", "pause", "resume",
			"cancel", "remove", "pauseAll", "resumeAll", "clearFinished", "reconnected", "recalled",
			"resumeUploads", "cancelUploads", "continueUploads", "discardUploads", "continueFile" };

	/** The texts of the picker, under {@code FilePicker.}. */
	static final String[] PICKER_TEXTS = { "choose", "chooseMany", "drop", "dropMany", "browse",
			"none", "chosen", "clear", "rejected", "tooLarge", "tooMany", "tooMuch", "remove" };

	/** The errors the server tells by their keys, which the rows of the files word. */
	static final String[] ERROR_KEYS = { UPLOAD_TOO_LARGE_RESOURCE_KEY,
			TOO_MANY_UPLOADS_RESOURCE_KEY, TOO_MUCH_RESOURCE_KEY, NO_ROOM_RESOURCE_KEY,
			UPLOAD_LOST_RESOURCE_KEY, GRANT_REFUSED_RESOURCE_KEY, UPLOAD_FAILED_RESOURCE_KEY };

	private Bytes chunkSize = DEFAULT_CHUNK_SIZE;

	private int parallelUploads = DEFAULT_PARALLEL_UPLOADS;

	private boolean remembering = true;

	private boolean askingToResume = true;

	private Duration retryTimeout = DEFAULT_RETRY_TIMEOUT;

	private Duration reconnectCheckInterval = DEFAULT_RECONNECT_CHECK_INTERVAL;

	private Duration stallTimeout = DEFAULT_STALL_TIMEOUT;

	private boolean recalling = true;

	private String uploadContext;

	private Duration inUseTimeout = DEFAULT_IN_USE_TIMEOUT;

	private boolean multiple = true;

	private String accept;

	private Design design = Design.DROP_ZONE;

	private boolean autoStart = true;

	private boolean keepingOnPage = true;

	private boolean overridingPatchMethod;

	private final Callback callback;

	/**
	 * @param id
	 *            component id
	 */
	public ResumableUploadField(String id)
	{
		super(id);
		callback = new Callback();
		add(callback);
	}

	/**
	 * @return the reference the files go to, by default the one mounted in the application, see
	 *         {@link ResumableUploadResourceReference#getInstance()}
	 */
	protected ResumableUploadResourceReference getResumableUploadResourceReference()
	{
		return ResumableUploadResourceReference.getInstance();
	}

	/**
	 * @return the file manager of the reference
	 */
	@Override
	protected IUploadsFileManager fileManager()
	{
		return getResumableUploadResourceReference().getFileManager();
	}

	/**
	 * @return the size of each file at most: the lowest of {@link #setFileMaxSize(Bytes)},
	 *         {@link #setMaxSize(Bytes)} and the {@link ResumableUploadResourceReference#getMaxFileSize()
	 *         limit of the reference}
	 */
	@Override
	public Bytes getFileMaxSize()
	{
		long most = Math.min(getMaxSize().bytes(),
			getResumableUploadResourceReference().getMaxFileSize().bytes());
		Bytes fileMaxSize = super.getFileMaxSize();
		return Bytes.bytes(fileMaxSize != null ? Math.min(most, fileMaxSize.bytes()) : most);
	}

	/**
	 * @return the size of a chunk
	 */
	public Bytes getChunkSize()
	{
		return chunkSize;
	}

	/**
	 * Sets the size of the chunks the files are sent in: the smaller they are, the less is sent
	 * again after the connection broke off, and the more requests are made. The chunks are never
	 * larger than the reference takes, see
	 * {@link ResumableUploadResourceReference#setMaxChunkSize(Bytes)}.
	 *
	 * @param chunkSize
	 *            the size, {@link #DEFAULT_CHUNK_SIZE} by default
	 * @return {@code this}
	 */
	public ResumableUploadField setChunkSize(Bytes chunkSize)
	{
		Args.notNull(chunkSize, "chunkSize");
		Args.isTrue(chunkSize.bytes() > 0, "chunkSize must be positive");
		this.chunkSize = chunkSize;
		return this;
	}

	/**
	 * @return the number of files sent at the same time
	 */
	public int getParallelUploads()
	{
		return parallelUploads;
	}

	/**
	 * @param parallelUploads
	 *            the number of files sent at the same time, {@link #DEFAULT_PARALLEL_UPLOADS} by
	 *            default
	 * @return {@code this}
	 */
	public ResumableUploadField setParallelUploads(int parallelUploads)
	{
		Args.isTrue(parallelUploads > 0, "parallelUploads must be positive");
		this.parallelUploads = parallelUploads;
		return this;
	}

	/**
	 * @return whether the browser remembers the uploads that did not end
	 */
	public boolean isRemembering()
	{
		return remembering;
	}

	/**
	 * Sets whether the browser remembers the URLs of the uploads that did not end, in its local
	 * storage, with the place of the field, {@link #getRememberedAs()}, and the name, size and
	 * date of their files: after a reload of the page or a restart of the browser, the field lists
	 * those the server still has and asks whether to go on with them, and an upload goes on once
	 * the user chooses the same file again. The server keeps an upload as long as its owner lasts,
	 * the session by default, see {@link ResumableUploadResourceReference#getCurrentOwner()}, and
	 * the upload does not expire. The browser remembers no content of the files.
	 *
	 * @param remembering
	 *            whether to remember, {@code true} by default
	 * @return {@code this}
	 */
	public ResumableUploadField setRemembering(boolean remembering)
	{
		this.remembering = remembering;
		return this;
	}

	/**
	 * Names the place the uploads of the field are remembered for: the field in the same place of a
	 * later page offers to go on with them. By default it is the class of the page and the path of
	 * the field in it, since the id of the field is a new one on every page.
	 * <p>
	 * A page that edits one of several records, a document for example, adds the record, as
	 * {@code "document:" + id}, so that an upload started for one document is not offered on the
	 * page of another, where its file would go to the wrong record. Where what the file was for does
	 * not come back, the unsaved steps of an Ajax wizard after its session ended for example, the
	 * uploads should belong to the session, see
	 * {@link ResumableUploadResourceReference#getCurrentOwner()}, or not be
	 * {@link #setRecalling(boolean) recalled}.
	 *
	 * @return the place of the field
	 */
	protected String getRememberedAs()
	{
		return getPage().getClass().getName() + ':' + getPageRelativePath();
	}

	/**
	 * @return whether the field asks the user before it resumes the uploads, once the server
	 *         answers again after the connection was lost for long
	 */
	public boolean isAskingToResume()
	{
		return askingToResume;
	}

	/**
	 * Sets whether the field asks the user before it resumes the uploads once the server answers
	 * again, after the connection was lost for longer than {@link #setRetryTimeout(Duration) the
	 * retry timeout}: the user may no longer want them, or want to look at what was sent first.
	 * Else the uploads go on by themselves. Either way an upload goes on by itself after a shorter
	 * loss.
	 *
	 * @param askingToResume
	 *            whether to ask, {@code true} by default
	 * @return {@code this}
	 */
	public ResumableUploadField setAskingToResume(boolean askingToResume)
	{
		this.askingToResume = askingToResume;
		return this;
	}

	/**
	 * @return how long an upload keeps trying again by itself before it takes the connection for
	 *         lost
	 */
	public Duration getRetryTimeout()
	{
		return retryTimeout;
	}

	/**
	 * Sets how long an upload keeps trying again by itself after a request got no answer, or a
	 * busy one (5xx, 409, 423 or 429), waiting longer before each try: at once, then after 1, 3,
	 * 5, 10, 20 and then 30 seconds. Also how long it waits for the browser to be online again.
	 * After that, an upload without answers takes the connection for lost: it waits for the server
	 * to answer again and the field asks whether to resume it, see
	 * {@link #setAskingToResume(boolean)}; one with busy answers fails.
	 *
	 * @param retryTimeout
	 *            the timeout, {@link #DEFAULT_RETRY_TIMEOUT} by default; {@link Duration#ZERO} to
	 *            take the connection for lost at the first request without an answer
	 * @return {@code this}
	 */
	public ResumableUploadField setRetryTimeout(Duration retryTimeout)
	{
		Args.notNull(retryTimeout, "retryTimeout");
		Args.isTrue(!retryTimeout.isNegative(), "retryTimeout must not be negative");
		this.retryTimeout = retryTimeout;
		return this;
	}

	/**
	 * @return how often at most an upload that lost the connection looks whether the server
	 *         answers again
	 */
	public Duration getReconnectCheckInterval()
	{
		return reconnectCheckInterval;
	}

	/**
	 * Sets how often at most the field looks whether the server answers again, once the connection
	 * of its uploads is lost: after 5, 10 and 20 seconds, as long as they are shorter, and then at
	 * this interval. It also looks whenever the browser is online again or the page gets the focus.
	 *
	 * @param reconnectCheckInterval
	 *            the interval, {@link #DEFAULT_RECONNECT_CHECK_INTERVAL} by default
	 * @return {@code this}
	 */
	public ResumableUploadField setReconnectCheckInterval(Duration reconnectCheckInterval)
	{
		Args.notNull(reconnectCheckInterval, "reconnectCheckInterval");
		Args.isTrue(!reconnectCheckInterval.isNegative() && !reconnectCheckInterval.isZero(),
			"reconnectCheckInterval must be positive");
		this.reconnectCheckInterval = reconnectCheckInterval;
		return this;
	}

	/**
	 * @return the waits before each look whether the server answers again in milliseconds, the
	 *         last one again and again
	 */
	static List<Long> reconnectCheckDelaysOf(Duration interval)
	{
		List<Long> delays = new ArrayList<>();
		for (long step : RECONNECT_CHECK_STEPS)
		{
			if (step < interval.toMillis())
			{
				delays.add(step);
			}
		}
		delays.add(interval.toMillis());
		return delays;
	}

	/**
	 * @return how long a request may send or get nothing before it is given up
	 */
	public Duration getStallTimeout()
	{
		return stallTimeout;
	}

	/**
	 * Sets how long a request may send nothing, or get no answer, before it is given up and tried
	 * again, as after a broken connection: a connection that went away without a word answers
	 * never. A chunk whose bytes all left the browser waits for its answer as long as the server
	 * answers a {@code HEAD} every so often.
	 *
	 * @param stallTimeout
	 *            the timeout, {@link #DEFAULT_STALL_TIMEOUT} by default; {@link Duration#ZERO} never
	 *            to give a request up
	 * @return {@code this}
	 */
	public ResumableUploadField setStallTimeout(Duration stallTimeout)
	{
		Args.notNull(stallTimeout, "stallTimeout");
		Args.isTrue(!stallTimeout.isNegative(), "stallTimeout must not be negative");
		this.stallTimeout = stallTimeout;
		return this;
	}

	/**
	 * @return whether the field lists, as the page loads, the uploads an earlier page left
	 *         unfinished, and asks whether to go on with them
	 */
	public boolean isRecalling()
	{
		return recalling;
	}

	/**
	 * Sets whether the field lists, as the page loads, the uploads an earlier page of the same place
	 * left unfinished, and asks whether to go on with them. Without, an upload still goes on once
	 * the user happens to choose the same file again, as long as the field is
	 * {@link #setRemembering(boolean) remembering}.
	 *
	 * @param recalling
	 *            whether to list and ask, {@code true} by default
	 * @return {@code this}
	 */
	public ResumableUploadField setRecalling(boolean recalling)
	{
		this.recalling = recalling;
		return this;
	}

	/**
	 * Lists the uploads the server keeps that the field offers to go on with as the page loads,
	 * besides those the browser remembers: with the uploads belonging to the user, see
	 * {@link ResumableUploadResourceReference#getCurrentOwner()}, those started in another browser,
	 * or, overridden, those started anywhere else, on a page listing the unfinished uploads of the
	 * user or in a step of a wizard for example. The user goes on with one by choosing its file again,
	 * which has to have the same name, size and date. The field asks only while it is
	 * {@link #setRemembering(boolean) remembering} and {@link #setRecalling(boolean) recalling}.
	 * <p>
	 * By default these are the unfinished uploads of the current owner in the
	 * {@link #setUploadContext(String) context} of the field, wherever they started, or, without a
	 * context, those a field in the same place started, {@link #getRememberedAs()}, as the
	 * {@link ResumableUploadResourceReference#getUploadsTracker() tracker of the uploads} lists them,
	 * whose files are no larger than the field takes.
	 *
	 * @return the uploads to offer
	 */
	protected List<TrackedUpload> getRecalledUploads()
	{
		ResumableUploadResourceReference reference = getResumableUploadResourceReference();
		String owner = reference.getCurrentOwner();
		if (owner == null)
		{
			return List.of();
		}
		String place = getRememberedAs();
		long fileMaxSize = getFileMaxSize().bytes();
		return reference.getUploadsTracker()
			.getUploads(uploadContext, owner)
			.stream()
			.filter(upload -> !upload.isStored() && !upload.isComplete() &&
				upload.length() <= fileMaxSize &&
				(uploadContext != null || place.equals(upload.place())))
			.toList();
	}

	/**
	 * @return the part of the application the uploads of the field belong to, {@code null} for
	 *         none
	 */
	public String getUploadContext()
	{
		return uploadContext;
	}

	/**
	 * Sets the part of the application the uploads of the field belong to: the attachments of a
	 * process, videos, and so on. The grant of the field signs it, and the
	 * {@link ResumableUploadResourceReference#getUploadsTracker() tracker of the uploads} hears of
	 * each upload with it, so that the application keeps the uploads of each part apart, or not. A
	 * field with a context offers to go on with the unfinished uploads of its owner in the same
	 * context, wherever they started, see {@link #getRecalledUploads()}.
	 *
	 * @param uploadContext
	 *            the context, a single line, {@code null} by default for none
	 * @return {@code this}
	 */
	public ResumableUploadField setUploadContext(String uploadContext)
	{
		Args.isTrue(UploadGrant.isLine(uploadContext), "uploadContext must be a single line");
		this.uploadContext = uploadContext;
		return this;
	}

	/**
	 * @return the uploads to offer, as the script of the field takes them
	 */
	private JSONArray recalledSettings()
	{
		JSONArray recalled = new JSONArray();
		if (!remembering || !recalling)
		{
			return recalled;
		}
		for (TrackedUpload upload : getRecalledUploads())
		{
			if (upload.isStored() || upload.isComplete())
			{
				continue;
			}
			JSONObject each = new JSONObject();
			each.put("id", upload.id());
			each.put("name", upload.clientFileName());
			each.put("size", upload.length());
			each.put("lastModified", upload.lastModified());
			each.put("type", upload.contentType());
			each.put("sent", upload.offset());
			recalled.put(each);
		}
		return recalled;
	}

	/**
	 * @return how long after an upload last sent something another page takes it for in use
	 */
	public Duration getInUseTimeout()
	{
		return inUseTimeout;
	}

	/**
	 * Sets how long after an upload last sent something another page of the field, in another tab
	 * for example, takes it for in use: it leaves it alone rather than offering to go on with it. A
	 * page sending an upload tells so in the local storage four times as often. A page that is
	 * left tells at once that it no longer sends.
	 *
	 * @param inUseTimeout
	 *            the timeout, {@link #DEFAULT_IN_USE_TIMEOUT} by default
	 * @return {@code this}
	 */
	public ResumableUploadField setInUseTimeout(Duration inUseTimeout)
	{
		Args.notNull(inUseTimeout, "inUseTimeout");
		Args.isTrue(!inUseTimeout.isNegative() && !inUseTimeout.isZero(),
			"inUseTimeout must be positive");
		this.inUseTimeout = inUseTimeout;
		return this;
	}

	/**
	 * @return the waits before the tries again in milliseconds, which together take a timeout
	 */
	static List<Long> retryDelaysOf(Duration timeout)
	{
		List<Long> delays = new ArrayList<>();
		long left = timeout.toMillis();
		for (int step = 0; left > 0; step++)
		{
			long delay = Math.min(RETRY_STEPS[Math.min(step, RETRY_STEPS.length - 1)], left);
			delays.add(delay);
			left -= delay;
		}
		return delays;
	}

	/**
	 * @return whether several files can be chosen at once
	 */
	public boolean isMultiple()
	{
		return multiple;
	}

	/**
	 * @param multiple
	 *            whether several files can be chosen at once, true by default
	 * @return {@code this}
	 */
	public ResumableUploadField setMultiple(boolean multiple)
	{
		this.multiple = multiple;
		return this;
	}

	/**
	 * @return the files the dialog offers, as the accept attribute of an input says
	 */
	public String getAccept()
	{
		return accept;
	}

	/**
	 * @param accept
	 *            the files the dialog offers and a drop takes, as the accept attribute of an input
	 *            says: {@code video/*,.zip} for example; all by default
	 * @return {@code this}
	 */
	public ResumableUploadField setAccept(String accept)
	{
		this.accept = accept;
		return this;
	}

	/**
	 * @return the design of the picker
	 */
	public Design getDesign()
	{
		return design;
	}

	/**
	 * @param design
	 *            the design of the picker, a zone to drop files on by default
	 * @return {@code this}
	 */
	public ResumableUploadField setDesign(Design design)
	{
		this.design = Args.notNull(design, "design");
		return this;
	}

	/**
	 * @return whether files start as soon as they are chosen
	 */
	public boolean isAutoStart()
	{
		return autoStart;
	}

	/**
	 * @param autoStart
	 *            whether files start as soon as they are chosen, true by default; otherwise each
	 *            has its start button
	 * @return {@code this}
	 */
	public ResumableUploadField setAutoStart(boolean autoStart)
	{
		this.autoStart = autoStart;
		return this;
	}

	/**
	 * @return whether the user is asked before leaving the page while files are being sent
	 */
	public boolean isKeepingOnPage()
	{
		return keepingOnPage;
	}

	/**
	 * @param keepingOnPage
	 *            whether the user is asked before leaving the page while files are being sent,
	 *            true by default
	 * @return {@code this}
	 */
	public ResumableUploadField setKeepingOnPage(boolean keepingOnPage)
	{
		this.keepingOnPage = keepingOnPage;
		return this;
	}

	/**
	 * @return whether the chunks go by {@code POST} with {@code X-HTTP-Method-Override: PATCH}
	 */
	public boolean isOverridingPatchMethod()
	{
		return overridingPatchMethod;
	}

	/**
	 * @param overridingPatchMethod
	 *            whether the chunks go by {@code POST} with {@code X-HTTP-Method-Override: PATCH},
	 *            for a proxy refusing {@code PATCH}; false by default
	 * @return {@code this}
	 */
	public ResumableUploadField setOverridingPatchMethod(boolean overridingPatchMethod)
	{
		this.overridingPatchMethod = overridingPatchMethod;
		return this;
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		if (multiple)
		{
			tag.put("multiple", "multiple");
		}
		else
		{
			tag.remove("multiple");
		}
		if (accept != null)
		{
			tag.put("accept", accept);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		CoreLibrariesContributor.contributeAjax(getApplication(), response);
		Uploads.renderHeadItems(response);
		Looks.of(this).renderHead(response);
		// the uploader of the theme rather than the script of Wicket's field
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Upload.resumable(" +
			ScriptJson.quote(getMarkupId()) + ", " + ScriptJson.of(newClientSettings()) + ", " +
			callback.renderAttributes() + ", " + getClientSideResumeQuestionCallBack() + ");"));
	}

	/**
	 * Lets the page ask the user whether to go on with uploads, in a dialog of its own for example,
	 * instead of the field asking above its rows. The function is called with a question:
	 * <ul>
	 * <li>{@code reason}: {@code 'reconnected'} when the server answers again after the connection
	 * was lost for long, {@code 'recalled'} when the page found uploads of an earlier visit that
	 * did not end;</li>
	 * <li>{@code files}: the files, each with its {@code name}, its {@code size} and the bytes the
	 * server holds, {@code sent};</li>
	 * <li>{@code resume()}: goes on with the uploads, for {@code 'recalled'} by opening the
	 * browser's dialog to choose their files again, which only works while the user clicks;</li>
	 * <li>{@code cancel()}: cancels the uploads, deleting them on the server.</li>
	 * </ul>
	 * It returns {@code true} when it asked the user, else the field asks. By default it returns
	 * {@code false}.
	 *
	 * @return the JavaScript function
	 */
	protected CharSequence getClientSideResumeQuestionCallBack()
	{
		return "function (question) { return false; }";
	}

	/**
	 * Creates the settings of the script of the field: where the files go, the limits of the field
	 * in its grant, signed by the reference's
	 * {@link ResumableUploadResourceReference#getGrantSigner() signer} for the
	 * {@link ResumableUploadResourceReference#getCurrentOwner() current owner}, with
	 * {@code grantExpires}, when the grant expires, in milliseconds since the epoch, -1 for never,
	 * how the files go, the picker and the texts. The grant signs the size of each file at most,
	 * the only limit the server holds a request to: the size of the files chosen at once and their
	 * number are checked in the browser only, every file being an upload of its own.
	 *
	 * @return the settings
	 */
	protected JSONObject newClientSettings()
	{
		ResumableUploadResourceReference reference = getResumableUploadResourceReference();
		UploadGrant grant = reference.newGrant(getMarkupId(), uploadContext, getFileMaxSize());
		JSONObject settings = new JSONObject();
		settings.put("inputName", getMarkupId());
		settings.put("resourceUrl", Strings.stripEnding(
			urlFor(reference, new PageParameters()).toString(), "/"));
		settings.put("ajaxCallBackUrl", callback.getCallbackUrl());
		// as large as a script holds exactly
		settings.put("maxSize", Math.min(getMaxSize().bytes(), UploadGrant.MAX_SAFE_INTEGER));
		settings.put("fileMaxSize", grant.fileMaxSize());
		settings.put("fileCountMax", getFileCountMax());
		settings.put("uploadToken",
			grant.token(reference.getGrantSigner(), reference.getCurrentOwner()));
		settings.put("grantExpires", grant.expires());
		settings.put("chunkSize",
			Math.min(chunkSize.bytes(), reference.getMaxChunkSize().bytes()));
		settings.put("parallelUploads", parallelUploads);
		settings.put("remembering", remembering);
		settings.put("rememberedAs", getRememberedAs());
		settings.put("askingToResume", askingToResume);
		settings.put("retryDelays", new JSONArray(retryDelaysOf(retryTimeout)));
		settings.put("probeDelays",
			new JSONArray(reconnectCheckDelaysOf(reconnectCheckInterval)));
		settings.put("stallMillis", stallTimeout.toMillis());
		settings.put("recalling", recalling);
		settings.put("recalled", recalledSettings());
		if (uploadContext != null)
		{
			settings.put("uploadContext", uploadContext);
		}
		settings.put("inUseMillis", inUseTimeout.toMillis());
		settings.put("multiple", multiple);
		if (accept != null)
		{
			settings.put("accept", accept);
		}
		settings.put("autoStart", autoStart);
		settings.put("keepingOnPage", keepingOnPage);
		settings.put("overridePatchMethod", overridingPatchMethod);
		settings.put("pollMillis", PROGRESS_POLL_MILLIS);
		settings.put("design", design.getName());
		settings.put("locale", getLocale().toLanguageTag());
		settings.put("disabled", !isEnabledInHierarchy());
		settings.put("texts", texts(this));
		settings.put("pickerTexts", pickerTexts(this));
		JSONObject errorTexts = new JSONObject();
		for (String key : ERROR_KEYS)
		{
			errorTexts.put(key, getUploadErrorMessage(key));
		}
		settings.put("errorTexts", errorTexts);
		return settings;
	}

	/**
	 * Words the error of an upload, as the browser told its resource key: only those of the field
	 * and of the upload to a resource, any other as a failed upload.
	 *
	 * @param errorKey
	 *            the key
	 * @return the message
	 */
	protected String getUploadErrorMessage(String errorKey)
	{
		Map<String, Object> model = new HashMap<>();
		model.put("maxSize", getMaxSize());
		model.put("fileMaxSize", getFileMaxSize());
		model.put("fileCountMax", getFileCountMax());
		if (errorKey != null && RESUMABLE_ERROR_KEYS.contains(errorKey))
		{
			ResumableUploadResourceReference reference = getResumableUploadResourceReference();
			model.put("maxUploadsPerOwner", reference.getMaxUploadsPerOwner());
			model.put("maxBytesPerOwner", reference.getMaxBytesPerOwner());
			return getString(errorKey, Model.ofMap(model));
		}
		String key = errorKey != null && ERROR_RESOURCE_KEYS.contains(errorKey) ? errorKey
			: UPLOAD_FAILED_RESOURCE_KEY;
		return getString(key, Model.ofMap(model));
	}

	/**
	 * @return the files a request of the browser tells the server stored: their names on the
	 *         server, those the browser reported and their types, as JSON; those whose names are
	 *         no plain file names are left out
	 */
	private List<UploadInfo> uploadInfosOf(String filesInfo)
	{
		List<UploadInfo> infos = new ArrayList<>();
		JSONArray files;
		try
		{
			files = new JSONArray(filesInfo);
		}
		catch (JSONException e)
		{
			return infos;
		}
		for (int index = 0; index < files.length(); index++)
		{
			JSONObject file = files.optJSONObject(index);
			String name = file != null ? file.optString("fileName", null) : null;
			if (Strings.isEmpty(name) || name.contains("/") || name.contains("\\") ||
				name.equals("..") || name.equals("."))
			{
				continue;
			}
			File stored = fileManager().getFile(getMarkupId(), name);
			UploadInfo info = new UploadInfo(name, file.optString("clientFileName", name),
				file.optString("contentType", "application/octet-stream"));
			info.setFile(stored);
			infos.add(info);
		}
		return infos;
	}

	/**
	 * @return the JavaScript expression pausing the uploads of the field
	 */
	public String getTriggerPauseUploadScript()
	{
		return "Wicket.Timer." + getMarkupId() + ".pause();";
	}

	/**
	 * Pauses the uploads of the field via an Ajax request.
	 *
	 * @param target
	 *            the {@link AjaxRequestTarget}
	 */
	public void pauseUpload(IPartialPageRequestHandler target)
	{
		target.appendJavaScript(getTriggerPauseUploadScript());
	}

	/**
	 * @return the JavaScript expression resuming the paused uploads of the field
	 */
	public String getTriggerResumeUploadScript()
	{
		return "Wicket.Timer." + getMarkupId() + ".resume();";
	}

	/**
	 * Resumes the paused uploads of the field via an Ajax request.
	 *
	 * @param target
	 *            the {@link AjaxRequestTarget}
	 */
	public void resumeUpload(IPartialPageRequestHandler target)
	{
		target.appendJavaScript(getTriggerResumeUploadScript());
	}

	/**
	 * @param component
	 *            a component, whose language they are in
	 * @return the texts of the uploader, as the uploaders of the core, React and Svelte take them:
	 *         for those an application shows in its Wicket pages
	 */
	public static JSONObject texts(Component component)
	{
		JSONObject texts = new JSONObject();
		for (String key : TEXTS)
		{
			texts.put(key, component.getString("ResumableUploader." + key));
		}
		return texts;
	}

	/**
	 * @param component
	 *            a component, whose language they are in
	 * @return the texts of the picker of the uploader, as the uploaders of the core, React and
	 *         Svelte take them
	 */
	public static JSONObject pickerTexts(Component component)
	{
		JSONObject texts = new JSONObject();
		for (String key : PICKER_TEXTS)
		{
			texts.put(key, component.getString("FilePicker." + key));
		}
		return texts;
	}

	/**
	 * Changes the attributes of the requests telling the field how an upload ended: the extra
	 * parameters a page veil leaves the requests alone by, for example. The requests go to the
	 * callback of the field, by POST. Does nothing by default.
	 *
	 * @param attributes
	 *            the attributes of the requests
	 */
	protected void updateCallbackAjaxAttributes(AjaxRequestAttributes attributes)
	{
	}

	/**
	 * Hears how each upload ended, as the script of the field tells: the files the server stored,
	 * or the key of an error, or a cancellation. Wicket 11.0 gives a subclass no address of its own
	 * callback of the field to tell it to.
	 */
	private final class Callback extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			attributes.setMethod(AjaxRequestAttributes.Method.POST);
			updateCallbackAjaxAttributes(attributes);
		}

		@Override
		protected void postprocessConfiguration(JSONObject attributesJson, Component component)
		{
			super.postprocessConfiguration(attributesJson, component);
			// the script sends to ajaxCallBackUrl, and no field with the requests
			attributesJson.remove(AjaxAttributeName.URL.jsonName());
			attributesJson.remove(AjaxAttributeName.MARKUP_ID.jsonName());
		}

		/**
		 * @return the attributes, as the script of the field takes them
		 */
		CharSequence renderAttributes()
		{
			return renderAjaxAttributes(getComponent());
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			IRequestParameters parameters = getComponent().getRequest().getRequestParameters();
			if (!parameters.getParameterValue("error").toBoolean(true))
			{
				onUploadSuccess(target,
					uploadInfosOf(parameters.getParameterValue("filesInfo").toString("[]")));
				return;
			}
			String errorKey = parameters.getParameterValue("errorMessage").toOptionalString();
			if (UPLOAD_CANCELED.equals(errorKey))
			{
				onUploadCanceled(target);
				return;
			}
			String message = getUploadErrorMessage(errorKey);
			error(message);
			onUploadFailure(target, message);
		}
	}
}
