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
package com.antiliacode.widgets.demo.upload;

import java.io.IOException;
import java.io.Serializable;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.feedback.ErrorLevelFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackCollector;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileDescription;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.upload.FilesSelectedBehavior;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.lang.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.upload.FilePickerBehavior;
import com.antiliacode.widgets.upload.FilePickerBehavior.Design;
import com.antiliacode.widgets.upload.UploadProgressBar;
import com.antiliacode.widgets.throttle.ThrottlePanel;

/**
 * Uploads in Wicket, drawn by the components of ui-components, as cases of one page:
 * <ul>
 * <li>a form posting its files the usual way or via Ajax, which the browser tells of the files
 * chosen before it sends them ({@link FilesSelectedBehavior});</li>
 * <li>a zone to drop files on, in a form sent via Ajax.</li>
 * </ul>
 * The files are chosen with a {@link FilePickerBehavior}, a button or a zone of the theme rather
 * than the browser's own field, and an {@link UploadProgressBar} of the theme follows each upload.
 * The {@link ThrottlePanel} slows the server down reading them, to watch the progress on a fast
 * network, and waiting before the Ajax requests. The server tells what it received below the
 * cases, and keeps no file.
 * <p>
 * The uploads to a resource, without a form, need the FileUploadToResourceField of Wicket 11 and
 * are not part of the 10.x branch.
 */
public class UploadPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(UploadPage.class);

	/** The most a form takes at once. */
	static final Bytes MAX_SIZE = Bytes.megabytes(20);

	/** The most each file may weigh. */
	static final Bytes FILE_MAX_SIZE = Bytes.megabytes(10);

	/** The most files sent at once. */
	static final int FILE_COUNT_MAX = 10;

	/** The files received the page lists, the latest first. */
	private static final int RECEIVED_MAX = 12;

	/** How the server received a file. */
	enum Way
	{
		/** In a form posted the usual way. */
		FORM,
		/** In a form sent via Ajax. */
		AJAX_FORM,
		/** Dropped into a form sent via Ajax. */
		DROPPED_IN_FORM
	}

	/** A file the server received, of which it keeps only what it tells. */
	record ReceivedFile(String name, long size, String type, Way way) implements Serializable
	{
	}

	private final List<ReceivedFile> received = new ArrayList<>();

	private final WebMarkupContainer receivedBox;

	private String description;

	/** What the browser told of the files chosen in the form, before sending them. */
	private String selection;

	/**
	 * Constructor.
	 */
	public UploadPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);
		themed.add(new ThrottlePanel("speed"));

		DemoCases cases = new DemoCases("cases", this);
		themed.add(cases);
		formCase(cases.addCase("form", "formCase"));
		dropCase(cases.addCase("drop", "dropCase"));

		receivedBox = new WebMarkupContainer("received");
		receivedBox.setOutputMarkupId(true);
		themed.add(receivedBox);
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
		table.add(new ListView<ReceivedFile>("files", () -> received)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<ReceivedFile> item)
			{
				ReceivedFile file = item.getModelObject();
				item.add(new Label("name", file.name()));
				item.add(new Label("size", sizeOf(file.size())));
				item.add(new Label("type", file.type()));
				item.add(new Label("way", new ResourceModel("way." + file.way().name())));
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
	 * A form with a text and files, posted the usual way or via Ajax.
	 */
	private void formCase(Fragment fragment)
	{
		Form<Void> form = newUploadForm("form");
		fragment.add(form);
		form.add(new TextField<>("description", LambdaModel.of(() -> description,
			text -> description = text)));

		FileUploadField files = new FileUploadField("files");
		files.add(new FilePickerBehavior());
		form.add(severalFiles("several", files));
		Label selected = new Label("selection", () -> selection)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(selection != null);
			}
		};
		selected.setOutputMarkupPlaceholderTag(true);
		files.add(FilesSelectedBehavior.onSelected((target, chosen) -> {
			selection = describe(chosen);
			target.add(selected);
		}));
		form.add(files, selected);
		form.add(new Label("limits", limits()));

		UploadProgressBar progress = new UploadProgressBar("progress", form);
		form.add(progress);
		form.add(new Button("submit")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit()
			{
				receive(files, Way.FORM);
			}
		}.add(new ButtonBehavior(Variant.PRIMARY, Size.MEDIUM)));
		form.add(new AjaxButton("ajaxSubmit")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				receive(files, Way.AJAX_FORM);
				selection = null;
				FilePickerBehavior.clear(files, target);
				target.add(selected, receivedBox);
			}

			@Override
			protected void onError(AjaxRequestTarget target)
			{
				progress.fail(target, refusal(form));
			}
		}.add(new ButtonBehavior(Variant.SECONDARY, Size.MEDIUM)));
	}

	/**
	 * A zone to drop files on, in a form sent via Ajax.
	 */
	private void dropCase(Fragment fragment)
	{
		Form<Void> form = newUploadForm("dropForm");
		fragment.add(form);
		FileUploadField images = new FileUploadField("images");
		images.add(new FilePickerBehavior().setDesign(Design.DROP_ZONE));
		UploadProgressBar progress = new UploadProgressBar("dropFormProgress", form);
		form.add(images, progress);
		fragment.add(severalFiles("severalImages", images));
		form.add(new AjaxButton("send")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				receive(images, Way.DROPPED_IN_FORM);
				FilePickerBehavior.clear(images, target);
				target.add(receivedBox);
			}

			@Override
			protected void onError(AjaxRequestTarget target)
			{
				progress.fail(target, refusal(form));
			}
		}.add(new ButtonBehavior(Variant.PRIMARY, Size.MEDIUM)));
	}

	/**
	 * Has a file input take one file or several, several to begin with, as a check box says: the
	 * input renders anew, without the files chosen, when it changes.
	 *
	 * @return the check box
	 */
	private static AjaxCheckBox severalFiles(String id, FileUploadField input)
	{
		IModel<Boolean> several = Model.of(true);
		input.add(AttributeModifier.replace("multiple",
			() -> Boolean.TRUE.equals(several.getObject()) ? "multiple" : null));
		input.setOutputMarkupId(true);
		return new AjaxCheckBox(id, several)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(input);
			}
		};
	}

	/**
	 * @return a multipart form taking at most {@link #MAX_SIZE}
	 */
	private static Form<Void> newUploadForm(String id)
	{
		Form<Void> form = new Form<>(id);
		form.setMultiPart(true);
		form.setMaxSize(MAX_SIZE);
		form.setFileMaxSize(FILE_MAX_SIZE);
		form.setFileCountMax(FILE_COUNT_MAX);
		return form;
	}

	/**
	 * @return why the server refused what a form sent: the first error of the form or of its
	 *         fields, else the text refused
	 */
	private String refusal(Form<?> form)
	{
		List<FeedbackMessage> errors = new FeedbackCollector(form, false).collect(
			new ErrorLevelFeedbackMessageFilter(FeedbackMessage.ERROR));
		return errors.isEmpty() ? getString("refused")
			: String.valueOf(errors.getFirst().getMessage());
	}

	/**
	 * @return what a form takes at most
	 */
	private String limits()
	{
		return getString("limits", Model.ofMap(Map.of("count", FILE_COUNT_MAX, "fileMaxSize",
			sizeOf(FILE_MAX_SIZE.bytes()), "maxSize", sizeOf(MAX_SIZE.bytes()))));
	}

	/**
	 * @return what the browser told of the files chosen, and whether the form takes them
	 */
	private String describe(List<FileDescription> chosen)
	{
		long size = chosen.stream().mapToLong(FileDescription::getFileSize).sum();
		boolean tooMany = chosen.size() > FILE_COUNT_MAX || size > MAX_SIZE.bytes() ||
			chosen.stream().anyMatch(file -> file.getFileSize() > FILE_MAX_SIZE.bytes());
		return getString(tooMany ? "selection.tooMuch" : "selection", Model.ofMap(Map.of("count",
			chosen.size(), "size", sizeOf(size))));
	}

	/**
	 * @return bytes as the file pickers write them in the browser: in the largest unit under which
	 *         they are at least one, 1024 of a unit making the next, with at most one decimal
	 */
	String sizeOf(long bytes)
	{
		String[] units = { getString("unit.byte"), "kB", "MB", "GB" };
		double value = Math.max(0, bytes);
		int unit = 0;
		while (value >= 1024 && unit < units.length - 1)
		{
			value /= 1024;
			unit++;
		}
		NumberFormat format = NumberFormat.getNumberInstance(getLocale());
		format.setMaximumFractionDigits(unit == 0 ? 0 : 1);
		// as Intl.NumberFormat rounds in the browser
		format.setRoundingMode(RoundingMode.HALF_UP);
		return format.format(value) + " " + units[unit];
	}

	/**
	 * Lists the files a form sent and lets them go.
	 */
	private void receive(FileUploadField field, Way way)
	{
		List<FileUpload> uploads = field.getFileUploads();
		for (FileUpload upload : uploads)
		{
			record(new ReceivedFile(upload.getClientFileName(), upload.getSize(),
				upload.getContentType(), way));
			try
			{
				upload.delete();
			}
			catch (IOException e)
			{
				LOG.warn("Could not delete the upload {}", upload.getClientFileName(), e);
			}
		}
		info(uploads.isEmpty() ? getString("nothingSent")
			: getString("received.count", Model.ofMap(Map.of("count", uploads.size()))));
	}

	private void record(ReceivedFile file)
	{
		received.addFirst(file);
		while (received.size() > RECEIVED_MAX)
		{
			received.removeLast();
		}
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(UploadPage.class,
			"UploadPage.css")));
	}
}
