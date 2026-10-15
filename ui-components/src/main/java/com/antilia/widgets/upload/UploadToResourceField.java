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
package com.antilia.widgets.upload;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.resource.AbstractFileUploadResource;
import org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField;
import org.apache.wicket.model.Model;

import com.github.openjson.JSONObject;

/**
 * Wicket's {@link FileUploadToResourceField}, which sends its files to a mounted resource without
 * a form, telling its uploads to the page: an {@link UploadProgressBar} of the field follows the
 * progress events of the request, and a guard keeps the user on the page while an upload runs.
 * The hooks of Wicket's field stay as they are: on the server
 * {@link #onUploadSuccess(org.apache.wicket.ajax.AjaxRequestTarget, java.util.List)},
 * {@link #onUploadFailure(org.apache.wicket.ajax.AjaxRequestTarget, String)} and
 * {@link #onUploadCanceled(org.apache.wicket.ajax.AjaxRequestTarget)}, in the browser the functions
 * of {@link #getClientBeforeSendCallBack()}, {@link #getClientSideSuccessCallBack()},
 * {@link #getClientSideUploadErrorCallBack()} and {@link #getClientSideCancelCallBack()}, which the
 * script of the field wraps rather than replaces: a subclass overrides any of them as it would
 * Wicket's.
 * <p>
 * The guard asks before the page is left, unless {@link #setKeepingOnPage(boolean)} says
 * otherwise, and with {@link #setBlockingRequests(boolean)} stops the other Ajax requests of the
 * page until the upload ends. Those of the components marked by {@link UploadGuard#bypass()}, and
 * of the Ajax behaviors marked by {@link UploadGuard#bypass(org.apache.wicket.ajax.attributes.AjaxRequestAttributes)},
 * go on: the button canceling the upload via Ajax needs to. The field's own callback, which Wicket
 * calls once the upload ended, goes on too.
 * <p>
 * The guard is set as the upload starts, by {@link #startUpload(org.apache.wicket.core.request.handler.IPartialPageRequestHandler)}
 * or the script of {@link #getTriggerUploadScript()}, so a change of it needs no new rendering of
 * the field, which would lose the files chosen.
 * <p>
 * The field sends any number of files at once, up to {@link #getFileCountMax()}, files of one name
 * too: Wicket's field keeps them apart.
 * <p>
 * The field sends one upload at a time, and none whose files break its limits, as the
 * {@link FilePickerBehavior} of the field knows them, nor one without files: those reach
 * {@link #onUploadFailure(org.apache.wicket.ajax.AjaxRequestTarget, String)} as if the server
 * refused them, without being sent. The resources of this class word the errors, in English,
 * German and Spanish, and the bar of the field tells the same words; when the request got no
 * answer, the bar tells that the connection broke off ({@value #CONNECTION_LOST_KEY}), and the
 * server, which hears of it as of any failure, says
 * {@value FileUploadToResourceField#UPLOAD_FAILED_RESOURCE_KEY}.
 *
 * @since 11.0.0
 */
public abstract class UploadToResourceField extends FileUploadToResourceField
{
	private static final long serialVersionUID = 1L;

	/** The key of the words the bar of the field tells when the connection broke off. */
	public static final String CONNECTION_LOST_KEY = "uploadConnectionLost";

	private boolean keepingOnPage = true;

	private boolean blockingRequests;

	/**
	 * @param id
	 *            component id
	 */
	public UploadToResourceField(String id)
	{
		super(id);
	}

	/**
	 * @return whether the user is asked before leaving the page while an upload runs
	 */
	public boolean isKeepingOnPage()
	{
		return keepingOnPage;
	}

	/**
	 * @param keepingOnPage
	 *            whether the user is asked before leaving the page while an upload runs; true by
	 *            default
	 * @return {@code this}
	 */
	public UploadToResourceField setKeepingOnPage(boolean keepingOnPage)
	{
		this.keepingOnPage = keepingOnPage;
		return this;
	}

	/**
	 * @return whether the other Ajax requests of the page are stopped while an upload runs
	 */
	public boolean isBlockingRequests()
	{
		return blockingRequests;
	}

	/**
	 * @param blockingRequests
	 *            whether the other Ajax requests of the page are stopped while an upload runs, but
	 *            those that bypass the guard (see {@link UploadGuard}); false by default
	 * @return {@code this}
	 */
	public UploadToResourceField setBlockingRequests(boolean blockingRequests)
	{
		this.blockingRequests = blockingRequests;
		return this;
	}

	/**
	 * @return the script setting what guards the next uploads of the field
	 */
	protected String getGuardScript()
	{
		JSONObject guard = new JSONObject();
		guard.put("leaving", keepingOnPage);
		guard.put("requests", blockingRequests);
		return "Wicket.Upload.guard(" + JSONObject.quote(getMarkupId()) + ", " +
			(keepingOnPage || blockingRequests ? guard.toString() : "null") + ");";
	}

	/**
	 * @return the script starting an upload: unless one runs, or the files break the limits, or
	 *         there are none, which the server hears of as of files it refused
	 */
	@Override
	public String getTriggerUploadScript()
	{
		return getGuardScript() + "Wicket.Upload.upload(" + JSONObject.quote(getMarkupId()) + ");";
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Uploads.renderHeadItems(response);
		// after Wicket's field is made, whose hooks it wraps
		response.render(OnDomReadyHeaderItem.forScript(getGuardScript() +
			"Wicket.Upload.field(" + JSONObject.quote(getMarkupId()) + ", " + newMessages() + ");"));
	}

	/**
	 * @return the words of the errors of the uploads, as the server words them, by the key of the
	 *         error the script of Wicket's field tells
	 */
	protected JSONObject newMessages()
	{
		Map<String, Object> model = new HashMap<>();
		model.put("maxSize", getMaxSize());
		model.put("fileMaxSize", getFileMaxSize());
		model.put("fileCountMax", getFileCountMax());
		JSONObject messages = new JSONObject();
		for (String key : new String[] { AbstractFileUploadResource.NO_FILE_SELECTED,
			Form.UPLOAD_TOO_LARGE_RESOURCE_KEY, Form.UPLOAD_SINGLE_FILE_TOO_LARGE_RESOURCE_KEY,
			Form.UPLOAD_TOO_MANY_FILES_RESOURCE_KEY, CONNECTION_LOST_KEY })
		{
			messages.put(key, getString(key, Model.ofMap(model)));
		}
		// the script says uploadFailed, which the server words with a key of its own
		messages.put(Form.UPLOAD_FAILED_RESOURCE_KEY,
			getString(FileUploadToResourceField.UPLOAD_FAILED_RESOURCE_KEY));
		return messages;
	}
}
