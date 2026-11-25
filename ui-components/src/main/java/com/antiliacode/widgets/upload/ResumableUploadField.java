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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.resumable.ResumableUploadResourceReference;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.resumable.ResumableUploadToResourceField;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.theme.Looks;
import com.antiliacode.widgets.upload.FilePickerBehavior.Design;
import com.github.openjson.JSONObject;

/**
 * Wicket's {@link ResumableUploadToResourceField} with the uploader of the theme: files sent in
 * chunks over the tus protocol to Wicket's {@link ResumableUploadResourceReference}, each paused,
 * resumed or canceled by the user, and going on after a broken connection or a reload of the page
 * without sending again what the server holds. Around the field's input it draws the file picker
 * of the theme, as a zone to drop files on or a button, and a row of each file: its name, its
 * size, its progress bar telling what was sent, how fast and how long it still takes, and its
 * buttons pausing, resuming and canceling it; then buttons pausing and resuming all.
 * <p>
 * Everything else is Wicket's: the application mounts the reference in its {@code init()}, with
 * the {@link org.apache.wicket.markup.html.form.upload.resource.IUploadsFileManager} the whole
 * files go to, and its limits; the field tells the browser those it has to know. Once the server
 * stored a whole file, {@link #onUploadSuccess(org.apache.wicket.ajax.AjaxRequestTarget, java.util.List)}
 * gets it; {@link #onUploadFailure(org.apache.wicket.ajax.AjaxRequestTarget, String)} and
 * {@link #onUploadCanceled(org.apache.wicket.ajax.AjaxRequestTarget)} hear of the others, as for
 * any {@link FileUploadToResourceField}. The field's markup is an {@code <input type="file">}.
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
 * After a broken connection a file goes on by itself; once the connection is lost for longer, a VPN
 * that went down for example, the field waits for the server to answer again and then asks above
 * its rows whether to resume the files or cancel them, unless it is not
 * {@link #setAskingToResume(boolean) asking}.
 * <p>
 * The browser keeps the address of each upload not finished in its {@code localStorage}, with the
 * place of the field: after a reload, the field in the same place shows them, how far each got,
 * and asks whether to go on with them, which they do once their files are chosen again;
 * {@link #setRemembering(boolean)} turns that off. With the uploads belonging to the user rather
 * than the session, see {@link ResumableUploadResourceReference#getCurrentOwner()}, that holds after
 * the session expired too. {@link #getClientSideResumeQuestionCallBack()} lets the page ask in a
 * dialog of its own. A field rendered again via Ajax stops
 * the files it was sending: render it once, and update the components around it. The guard of the
 * uploads asks before the page is left while files are being sent, unless
 * {@link #setKeepingOnPage(boolean)} says otherwise.
 *
 * @since 11.0.0
 */
public abstract class ResumableUploadField extends ResumableUploadToResourceField
{
	private static final long serialVersionUID = 1L;

	/**
	 * How often the browser asks the server how many bytes it holds while a chunk is being sent,
	 * for the progress: the bytes the browser handed to the network may be megabytes ahead.
	 */
	public static final int PROGRESS_POLL_MILLIS = 1000;

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
			UPLOAD_LOST_RESOURCE_KEY, FileUploadToResourceField.UPLOAD_FAILED_RESOURCE_KEY };

	private boolean multiple = true;

	private String accept;

	private Design design = Design.DROP_ZONE;

	private boolean autoStart = true;

	private boolean keepingOnPage = true;

	private boolean overridingPatchMethod;

	private final FieldCallbackAttributes callbackAttributes;

	/**
	 * @param id
	 *            component id
	 */
	public ResumableUploadField(String id)
	{
		super(id);
		callbackAttributes = new FieldCallbackAttributes(this::updateCallbackAjaxAttributes);
		add(callbackAttributes);
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
			callbackAttributes.renderAttributes() + ", " + getClientSideResumeQuestionCallBack() +
			");"));
	}

	/**
	 * @return the settings of Wicket's field, with the options of the uploader of the theme: how
	 *         the files go, the picker and the texts
	 */
	@Override
	protected JSONObject newClientSettings()
	{
		JSONObject settings = super.newClientSettings();
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
	 * Changes the attributes of the requests telling Wicket's field how an upload ended: the extra
	 * parameters a page veil leaves the requests alone by, for example. The requests go to the
	 * callback of Wicket's field, by POST. Does nothing by default.
	 *
	 * @param attributes
	 *            the attributes of the requests
	 */
	protected void updateCallbackAjaxAttributes(AjaxRequestAttributes attributes)
	{
	}
}
