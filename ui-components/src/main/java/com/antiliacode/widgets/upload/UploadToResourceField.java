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

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.AjaxAttributeName;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.resource.AbstractFileUploadResource;
import org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField;
import org.apache.wicket.model.Model;

import com.antiliacode.widgets.ScriptJson;
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
 * German and Spanish, and the bar of the field tells the same words: when the request got no
 * answer, both the bar and the server tell that the connection broke off
 * ({@value FileUploadToResourceField#CONNECTION_LOST_RESOURCE_KEY}).
 * <p>
 * Wicket's field tells its callback how an upload ended by a request of Wicket's Ajax, which the
 * field sends with attributes of its own:
 * {@link #updateCallbackAjaxAttributes(AjaxRequestAttributes)} changes them, those of the uploads
 * it refuses before sending them too, the extra parameters a page veil leaves the requests alone
 * by, for example.
 *
 * @since 11.0.0
 */
public abstract class UploadToResourceField extends FileUploadToResourceField
{
	private static final long serialVersionUID = 1L;

	private boolean keepingOnPage = true;

	private boolean blockingRequests;

	private final CallbackAttributes callbackAttributes;

	/**
	 * @param id
	 *            component id
	 */
	public UploadToResourceField(String id)
	{
		super(id);
		callbackAttributes = new CallbackAttributes();
		add(callbackAttributes);
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
		return "Wicket.Upload.guard(" + ScriptJson.quote(getMarkupId()) + ", " +
			(keepingOnPage || blockingRequests ? ScriptJson.of(guard) : "null") + ");";
	}

	/**
	 * @return the script starting an upload: unless one runs, or the files break the limits, or
	 *         there are none, which the server hears of as of files it refused
	 */
	@Override
	public String getTriggerUploadScript()
	{
		return getGuardScript() + "Wicket.Upload.upload(" + ScriptJson.quote(getMarkupId()) + ");";
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Uploads.renderHeadItems(response);
		// after Wicket's field is made, whose hooks it wraps
		response.render(OnDomReadyHeaderItem.forScript(getGuardScript() +
			"Wicket.Upload.field(" + ScriptJson.quote(getMarkupId()) + ", " +
			ScriptJson.of(newMessages()) + ", " + callbackAttributes.renderAttributes() + ");"));
	}

	/**
	 * Changes the attributes of the requests telling the server how an upload ended, which Wicket's
	 * field sends to its callback, and the script of the field for the uploads it refuses before
	 * sending them: the {@link AjaxRequestAttributes#getExtraParameters() extra parameters} a page
	 * veil leaves the requests alone by
	 * ({@link org.apache.wicket.extensions.ajax.veil.PageVeilBehavior#noVeil(
	 * AjaxRequestAttributes)}), the dynamic extra parameters or the listeners
	 * of the call, for example. The requests go to the callback of Wicket's field, by POST, which
	 * carries what the server needs of the files in the body. Does nothing by default.
	 *
	 * @param attributes
	 *            the attributes of the requests
	 */
	protected void updateCallbackAjaxAttributes(AjaxRequestAttributes attributes)
	{
	}

	/**
	 * Renders the attributes of the requests to the callback of Wicket's field, whose URL its
	 * script knows: it hears of no request of its own.
	 */
	private final class CallbackAttributes extends AbstractDefaultAjaxBehavior
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
			// the URL is the callback's, and Wicket's field sends no field with the requests
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
		public boolean canCallListener(Component component)
		{
			// the requests go to the callback of Wicket's field
			return false;
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
		}
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
			Form.UPLOAD_TOO_MANY_FILES_RESOURCE_KEY, CONNECTION_LOST_RESOURCE_KEY })
		{
			messages.put(key, getString(key, Model.ofMap(model)));
		}
		// the script says uploadFailed, which the server words with a key of its own
		messages.put(Form.UPLOAD_FAILED_RESOURCE_KEY,
			getString(FileUploadToResourceField.UPLOAD_FAILED_RESOURCE_KEY));
		return messages;
	}
}
