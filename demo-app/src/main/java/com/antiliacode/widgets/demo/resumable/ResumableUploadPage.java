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
package com.antiliacode.widgets.demo.resumable;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.throttle.ThrottlePanel;
import com.antiliacode.widgets.upload.ResumableUploadField;
import com.github.openjson.JSONObject;

/**
 * The demo of the resumable uploads: a {@link ResumableUploadField}, over Wicket's
 * ResumableUploadToResourceField, sends large files in chunks over the tus protocol to the
 * {@link DemoResumableUploads} of the application, Wicket's resource of them, each paused, resumed
 * and canceled by the user, going on after a broken connection, which the server makes now and
 * then when asked to, and after a reload of the page. The {@link ThrottlePanel} slows the chunks
 * down, to watch them on a fast network. The server tells what it received, the SHA-256 of each
 * file among it, and deletes the files; the React and Svelte apps hand theirs over to the
 * {@link ReceiptsResource}.
 */
public class ResumableUploadPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The files received the page lists, the latest first. */
	private static final int RECEIVED_MAX = 12;

	private final List<Receipt> received = new ArrayList<>();

	private final WebMarkupContainer receivedBox;

	/**
	 * Constructor.
	 */
	public ResumableUploadPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);
		themed.add(new ThrottlePanel("speed"));
		themed.add(new AjaxCheckBox("breaking", LambdaModel.of(DemoResumableUploads::isBreaking,
			DemoResumableUploads::setBreaking))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
			}
		});
		themed.add(new Label("limits", limits()));

		receivedBox = new WebMarkupContainer("received");
		receivedBox.setOutputMarkupId(true);
		themed.add(receivedBox);
		themed.add(new ResumableUploadField("uploads")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUploadSuccess(AjaxRequestTarget target, List<UploadInfo> infos)
			{
				for (UploadInfo info : infos)
				{
					try
					{
						received.add(0, Receipt.of(info.getClientFileName(), info.getFile()));
					}
					catch (IOException e)
					{
						error(e.getMessage());
					}
					finally
					{
						// the demo keeps nothing of what it received
						info.getFile().delete();
					}
				}
				while (received.size() > RECEIVED_MAX)
				{
					received.remove(received.size() - 1);
				}
				target.add(receivedBox);
			}
		}.setParallelUploads(2));

		WebMarkupContainer table = new WebMarkupContainer("table")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!received.isEmpty());
			}
		};
		receivedBox.add(table);
		table.add(new ListView<Receipt>("files", () -> received)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Receipt> item)
			{
				Receipt receipt = item.getModelObject();
				item.add(new Label("name", receipt.name()));
				item.add(new Label("size", sizeOf(receipt.size(), getLocale())));
				item.add(new Label("sha256", receipt.sha256()));
			}
		});
		receivedBox.add(new WebMarkupContainer("none")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(received.isEmpty());
			}
		});
	}

	/**
	 * @return the limits of the uploads of the demo, in words
	 */
	private StringResourceModel limits()
	{
		DemoResumableUploads uploads = DemoResumableUploads.get();
		Locale locale = getLocale();
		return new StringResourceModel("limits", this).setParameters(
			sizeOf(uploads.getMaxFileSize().bytes(), locale),
			sizeOf(uploads.getMaxChunkSize().bytes(), locale), uploads.getMaxUploadsPerSession(),
			uploads.getExpiry().toMinutes());
	}

	/**
	 * @return a size as the apps of React and Svelte write it: bytes, KB, MB or GB, with one
	 *         decimal
	 */
	static String sizeOf(long bytes, Locale locale)
	{
		String[] units = { "bytes", "KB", "MB", "GB" };
		double value = Math.max(0, bytes);
		int unit = 0;
		while (value >= 1024 && unit < units.length - 1)
		{
			value /= 1024;
			unit++;
		}
		NumberFormat format = NumberFormat.getNumberInstance(locale);
		format.setMaximumFractionDigits(unit == 0 ? 0 : 1);
		return format.format(value) + " " + units[unit];
	}

	/**
	 * @return the files the page lists, the latest first
	 */
	List<Receipt> getReceived()
	{
		return received;
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "resumableUpload";
	}

	@Override
	protected JSONObject getFrameworkConfig()
	{
		DemoResumableUploads uploads = DemoResumableUploads.get();
		return new JSONObject()
			.put("endpoint", urlFor(uploads, null).toString().replaceAll("/+$", ""))
			.put("query", uploads.newQuery(DemoResumableUploads.clientId(),
				uploads.getMaxFileSize(), null, -1))
			.put("receiptsUrl", urlFor(ReceiptsResource.REFERENCE, null).toString())
			.put("fileMaxSize", uploads.getMaxFileSize().bytes())
			.put("chunkSize", uploads.getMaxChunkSize().bytes())
			.put("locale", getLocale().toLanguageTag())
			.put("uploaderTexts", ResumableUploadField.texts(this))
			.put("pickerTexts", ResumableUploadField.pickerTexts(this))
			.put("texts", FrameworkTabs.texts(this, "received", "received.name", "received.size",
				"received.sha256", "received.none", "received.failed"));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(
			ResumableUploadPage.class, "ResumableUploadPage.css")));
	}
}
