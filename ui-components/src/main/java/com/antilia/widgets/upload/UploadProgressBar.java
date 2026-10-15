/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.upload;

import org.apache.wicket.Component;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField;
import org.apache.wicket.util.lang.Args;

import com.antilia.widgets.theme.Looks;
import com.github.openjson.JSONObject;

/**
 * The progress of the uploads of a form or of a field uploading to a resource, in the colors of the
 * theme it is in: a {@link com.antilia.widgets.progress.ProgressBar ProgressBar}, then what was
 * sent of how much, how fast and how long it still takes, and once the upload ended whether it went
 * well. It is hidden until an upload starts, and stays as the upload left it until the next one.
 * <p>
 * Everything happens in the browser: {@code Wicket.Upload.UploadProgress} from {@link Uploads#JS}
 * draws the bar into the component's element and follows the uploads of its source:
 * <ul>
 * <li>a multipart form, posted the usual way or sent by Wicket's Ajax, such as by an
 * {@link org.apache.wicket.ajax.markup.html.form.AjaxButton AjaxButton}, when it holds files;</li>
 * <li>an {@link UploadToResourceField}, which tells its uploads by itself; a field of Wicket's own,
 * a {@link FileUploadToResourceField}, does not.</li>
 * </ul>
 * The bar asks the server every {@link #setPollMillis(int) half a second} how far Wicket got
 * reading the upload (see {@link UploadStatusResource}), which Wicket knows with
 * {@link org.apache.wicket.settings.ApplicationSettings#setUploadProgressUpdatesEnabled(boolean)}:
 * that is what arrived. Without it, or {@link #setFollowingServer(boolean) if told not to ask},
 * the bar follows the progress events of the request sending the files, where the browser has
 * them: those of an {@link UploadToResourceField}, and of a form sent by Wicket's Ajax in plain
 * JavaScript. They count what the browser handed to the network, whose buffers may hold megabytes
 * the server did not read yet. Without either, the bar only shows that something is going on.
 * <p>
 * Attach it to a {@code <div>}, which the bar fills. The texts are those of the resources
 * {@code UploadProgressBar.*}.
 *
 * @since 11.0.0
 */
public class UploadProgressBar extends WebComponent
{
	private static final long serialVersionUID = 1L;

	private final Form<?> form;

	private final FileUploadField field;

	private int pollMillis = 500;

	private boolean followingServer = true;

	/**
	 * The progress of the uploads of a form.
	 *
	 * @param id
	 *            component id
	 * @param form
	 *            the form, whose root form sends the files
	 */
	public UploadProgressBar(String id, Form<?> form)
	{
		this(id, Args.notNull(form, "form"), null);
	}

	/**
	 * The progress of the uploads of a field: of an {@link UploadToResourceField}, or of the form
	 * of any other {@link FileUploadField}.
	 *
	 * @param id
	 *            component id
	 * @param field
	 *            the field
	 */
	public UploadProgressBar(String id, FileUploadField field)
	{
		this(id, null, Args.notNull(field, "field"));
	}

	private UploadProgressBar(String id, Form<?> form, FileUploadField field)
	{
		super(id);
		this.form = form;
		this.field = field;
		setOutputMarkupId(true);
		if (form != null)
		{
			form.setOutputMarkupId(true);
		}
		else
		{
			field.setOutputMarkupId(true);
		}
	}

	/**
	 * @return how often the bar asks the server how far an upload got, in milliseconds
	 */
	public int getPollMillis()
	{
		return pollMillis;
	}

	/**
	 * @param pollMillis
	 *            how often the bar asks the server how far an upload got, in milliseconds, when
	 *            the request sending the files does not tell; 500 by default
	 * @return {@code this}
	 */
	public UploadProgressBar setPollMillis(int pollMillis)
	{
		this.pollMillis = Math.max(100, pollMillis);
		return this;
	}

	/**
	 * @return whether the bar asks the server how far it got reading an upload
	 */
	public boolean isFollowingServer()
	{
		return followingServer;
	}

	/**
	 * @param followingServer
	 *            whether the bar asks the server how far it got reading an upload, when Wicket
	 *            keeps the progress of uploads, rather than following the request sending it;
	 *            true by default
	 * @return {@code this}
	 */
	public UploadProgressBar setFollowingServer(boolean followingServer)
	{
		this.followingServer = followingServer;
		return this;
	}

	/**
	 * Shows that the upload failed, as when the server refused the files of a form sent by Ajax,
	 * whose request ended well all the same: in the form's
	 * {@link org.apache.wicket.ajax.markup.html.form.AjaxButton#onError(org.apache.wicket.ajax.AjaxRequestTarget)
	 * onError}, say.
	 *
	 * @param target
	 *            the Ajax request
	 * @param message
	 *            why it failed, {@code null} for the text {@code UploadProgressBar.failed}
	 */
	public void fail(IPartialPageRequestHandler target, String message)
	{
		target.appendJavaScript("Wicket.Upload.UploadProgress.of(" +
			JSONObject.quote(getMarkupId()) + ")?.fail(" +
			(message != null ? JSONObject.quote(message) : "") + ");");
	}

	/**
	 * @return whether the source is a field uploading to a resource, rather than a form
	 */
	private boolean isResourceUpload()
	{
		return field instanceof FileUploadToResourceField;
	}

	/**
	 * @return the form or the field whose uploads the bar shows in the browser: the root form, or
	 *         the field uploading to a resource
	 */
	protected Component getSource()
	{
		if (isResourceUpload())
		{
			return field;
		}
		return (form != null ? form : field.getForm()).getRootForm();
	}

	/**
	 * @return the id Wicket keeps the progress of an upload of the source under: the markup id of
	 *         a field uploading to a resource, else the id of the page
	 */
	protected String getUploadId()
	{
		return isResourceUpload() ? field.getMarkupId() : getPage().getId();
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		// before anything renders: the root form of a field is known once the page is complete
		getSource().setOutputMarkupId(true);
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		checkComponentTag(tag, "div");
		super.onComponentTag(tag);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Uploads.renderHeadItems(response);
		Looks.of(this).renderHead(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Upload.UploadProgress.attach(" +
			JSONObject.quote(getMarkupId()) + ", " + newOptions() + ");"));
	}

	/**
	 * @return the options of the script: the source, where the server tells the progress, and the
	 *         texts
	 */
	protected JSONObject newOptions()
	{
		JSONObject options = new JSONObject();
		options.put("source", getSource().getMarkupId());
		if (followingServer &&
			getApplication().getApplicationSettings().isUploadProgressUpdatesEnabled())
		{
			options.put("statusUrl", urlFor(UploadStatusResource.REFERENCE,
				UploadStatusResource.parametersOf(getUploadId())).toString());
		}
		options.put("pollMillis", pollMillis);
		options.put("locale", getLocale().toLanguageTag());
		JSONObject texts = new JSONObject();
		for (String key : new String[] { "starting", "sent", "rate", "remaining", "processing",
			"done", "canceled", "failed", "lost", "serverError", "blocked" })
		{
			texts.put(key, getString("UploadProgressBar." + key));
		}
		options.put("texts", texts);
		return options;
	}
}
