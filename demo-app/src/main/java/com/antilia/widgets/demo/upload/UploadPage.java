/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.upload;

import java.io.IOException;
import java.io.Serializable;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.feedback.ErrorLevelFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackCollector;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnEventHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileDescription;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.upload.FilesSelectedBehavior;
import org.apache.wicket.markup.html.form.upload.resource.IUploadsFileManager;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.lang.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antilia.widgets.button.AjaxActionButton;
import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.button.ButtonBehavior.Variant;
import com.antilia.widgets.demo.DemoCases;
import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.icon.SvgIcon;
import com.antilia.widgets.upload.FilePickerBehavior;
import com.antilia.widgets.upload.FilePickerBehavior.Design;
import com.antilia.widgets.upload.UploadGuard;
import com.antilia.widgets.upload.UploadProgressBar;
import com.antilia.widgets.throttle.ThrottlePanel;
import com.antilia.widgets.upload.UploadToResourceField;
import com.github.openjson.JSONObject;

/**
 * Uploads in Wicket, each way Wicket has, drawn by the components of ui-components, as cases of one
 * page:
 * <ul>
 * <li>a form posting its files the usual way or via Ajax, which the browser tells of the files
 * chosen before it sends them ({@link FilesSelectedBehavior});</li>
 * <li>a field sending its files to a mounted resource without a form, as Wicket's
 * {@link org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField} does: the
 * user is asked before leaving the page while an upload runs and, if chosen, the other Ajax
 * requests are stopped meanwhile, but those marked to go on, such as the button canceling it;
 * every hook of the field, in the browser and on the server, tells when it is called;</li>
 * <li>zones to drop files on, one sending them at once to a resource, one into a form.</li>
 * </ul>
 * The files are chosen with a {@link FilePickerBehavior}, a button or a zone of the theme rather
 * than the browser's own field, and an {@link UploadProgressBar} of the theme follows each upload.
 * The {@link ThrottlePanel} slows the server down reading them, to watch the progress on a fast
 * network, and waiting before the Ajax requests. The server tells what it received below the
 * cases, and keeps no file.
 */
public class UploadPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(UploadPage.class);

	/** The most a form or a field takes at once. */
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
		/** Sent to a resource. */
		RESOURCE,
		/** Dropped on a zone sending it to a resource at once. */
		DROPPED,
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

	/** Whether the field of the resource case sends its files. */
	private boolean uploading;

	private int blockedClicks;

	private int bypassingClicks;

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
		resourceCase(cases.addCase("resource", "resourceCase"));
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
	 * A field sending its files to a resource, guarding the page meanwhile, with buttons whose Ajax
	 * requests the guard stops or lets go, and the log of the hooks of the field.
	 */
	private void resourceCase(Fragment fragment)
	{
		WebMarkupContainer buttons = new WebMarkupContainer("buttons");
		buttons.setOutputMarkupId(true);
		WebMarkupContainer hookLog = new WebMarkupContainer("hookLog");
		hookLog.setOutputMarkupId(true);
		UploadToResourceField field = new HookedUploadField("resourceFiles", hookLog)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUploaded(AjaxRequestTarget target)
			{
				uploading = false;
				target.add(buttons);
			}
		};
		field.add(new FilePickerBehavior());
		fragment.add(field, new Label("resourceLimits", limits()), severalFiles("several", field));

		fragment.add(new AjaxCheckBox("keepOnPage", LambdaModel.of(field::isKeepingOnPage,
			field::setKeepingOnPage))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
			}
		});
		fragment.add(new AjaxCheckBox("blockRequests", LambdaModel.of(field::isBlockingRequests,
			field::setBlockingRequests))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
			}
		});
		fragment.add(new UploadProgressBar("resourceProgress", field));

		fragment.add(buttons, hookLog);
		buttons.add(new AjaxActionButton("upload", new ResourceModel("upload"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				uploading = true;
				// the hooks of this upload only
				target.appendJavaScript("UploadDemo.clear(" +
					JSONObject.quote(hookLog.getMarkupId()) + ");");
				field.startUpload(target);
				target.add(buttons);
			}

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setEnabled(!uploading);
			}
		}.setIcon(SvgIcon.UPLOAD).setVariant(Variant.PRIMARY));
		buttons.add(new AjaxActionButton("cancel", new ResourceModel("cancel"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				field.cancelUpload(target);
			}

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setEnabled(uploading);
			}
		}.setIcon(SvgIcon.XMARK).setVariant(Variant.DANGER).add(UploadGuard.bypass()));

		Label blockedCount = new Label("blockedCount", () -> clicks(blockedClicks));
		Label bypassingCount = new Label("bypassingCount", () -> clicks(bypassingClicks));
		fragment.add(blockedCount.setOutputMarkupId(true),
			bypassingCount.setOutputMarkupId(true));
		fragment.add(new AjaxActionButton("blocked", new ResourceModel("blocked"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				blockedClicks++;
				target.add(blockedCount);
			}
		}.setSize(Size.SMALL));
		fragment.add(new AjaxActionButton("bypassing", new ResourceModel("bypassing"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				bypassingClicks++;
				target.add(bypassingCount);
			}
		}.setSize(Size.SMALL).add(UploadGuard.bypass()));
	}

	/**
	 * Zones to drop files on: one sending them to a resource as soon as they are dropped, one in a
	 * form sent via Ajax.
	 */
	private void dropCase(Fragment fragment)
	{
		UploadToResourceField dropped = new DemoUploadField("dropFiles", Way.DROPPED);
		dropped.add(new FilePickerBehavior().setDesign(Design.DROP_ZONE));
		dropped.add(new UploadOnChoice());
		fragment.add(dropped, new UploadProgressBar("dropProgress", dropped),
			severalFiles("severalDropped", dropped));

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
	 * @return what a form or a field takes at most
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

	private String clicks(int count)
	{
		return getString("clicks", Model.ofMap(Map.of("count", count)));
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
		response.render(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(
			UploadPage.class, "UploadPage.js")));
	}

	/**
	 * A field of the demo sending its files to the resource of the application: it lists what
	 * the server received, deletes the files, and clears the files chosen.
	 */
	private class DemoUploadField extends UploadToResourceField
	{
		private static final long serialVersionUID = 1L;

		private final Way way;

		DemoUploadField(String id, Way way)
		{
			super(id);
			this.way = way;
			setMaxSize(MAX_SIZE);
			setFileMaxSize(FILE_MAX_SIZE);
			setFileCountMax(FILE_COUNT_MAX);
		}

		@Override
		protected void onUploadSuccess(AjaxRequestTarget target, List<UploadInfo> infos)
		{
			IUploadsFileManager files = fileManager();
			for (UploadInfo info : infos)
			{
				record(new ReceivedFile(info.getClientFileName(), info.getSize(),
					info.getContentType(), way));
				if (files instanceof DemoUploadsFileManager demo)
				{
					demo.delete(info.getFile());
				}
			}
			info(UploadPage.this.getString("received.count", Model.ofMap(Map.of("count",
				infos.size()))));
			FilePickerBehavior.clear(this, target);
			target.add(receivedBox);
			hooked(target, "onUploadSuccess(target, fileInfos)", UploadPage.this.getString(
				"hook.uploadSuccess").replace("{0}", String.valueOf(infos.size())));
			onUploaded(target);
		}

		@Override
		protected void onUploadFailure(AjaxRequestTarget target, String errorInfo)
		{
			hooked(target, "onUploadFailure(target, errorInfo)", UploadPage.this.getString(
				"hook.uploadFailure").replace("{0}", String.valueOf(errorInfo)));
			onUploaded(target);
		}

		@Override
		protected void onUploadCanceled(AjaxRequestTarget target)
		{
			info(UploadPage.this.getString("canceled"));
			hooked(target, "onUploadCanceled(target)",
				UploadPage.this.getString("hook.uploadCanceled"));
			onUploaded(target);
		}

		/**
		 * Called once an upload ended, however it did.
		 */
		protected void onUploaded(AjaxRequestTarget target)
		{
		}

		/**
		 * Called once a hook of the field was called on the server.
		 *
		 * @param hook
		 *            the hook
		 * @param text
		 *            what happened
		 */
		protected void hooked(AjaxRequestTarget target, String hook, String text)
		{
		}
	}

	/**
	 * The field of the resource case, logging every hook of Wicket's field as it is called: those
	 * of its script in the browser, which Wicket calls as the request is about to be sent, once the
	 * files arrived, once the upload failed or was canceled, and those on the server, once the
	 * page heard of it.
	 */
	private class HookedUploadField extends DemoUploadField
	{
		private static final long serialVersionUID = 1L;

		private final Component log;

		HookedUploadField(String id, Component log)
		{
			super(id, Way.RESOURCE);
			this.log = log;
		}

		/**
		 * @param side
		 *            where the hook was called, browser or server
		 * @param text
		 *            the script of what happened
		 * @return the script logging that a hook was called
		 */
		private String log(String side, String hook, String text)
		{
			return "UploadDemo.hook(" + JSONObject.quote(log.getMarkupId()) + ", '" + side + "', " +
				JSONObject.quote(UploadPage.this.getString("hook." + side)) + ", " +
				JSONObject.quote(hook) + ", " + text + ");";
		}

		private String text(String key)
		{
			return JSONObject.quote(UploadPage.this.getString(key));
		}

		@Override
		protected CharSequence getClientBeforeSendCallBack()
		{
			return "function (xhr, settings) { " + log("browser", "getClientBeforeSendCallBack()",
				text("hook.beforeSend")) + " return true; }";
		}

		@Override
		protected CharSequence getClientSideSuccessCallBack()
		{
			return "function () { " + log("browser", "getClientSideSuccessCallBack()",
				text("hook.success")) + " }";
		}

		@Override
		protected CharSequence getClientSideUploadErrorCallBack()
		{
			// the error the server is told next
			return "function (res) { " + log("browser", "getClientSideUploadErrorCallBack()",
				text("hook.error") + ".replace('{0}', res.errorMessage)") + " }";
		}

		@Override
		protected CharSequence getClientSideCancelCallBack()
		{
			return "function () { " + log("browser", "getClientSideCancelCallBack()",
				text("hook.cancel")) + " }";
		}

		@Override
		protected void hooked(AjaxRequestTarget target, String hook, String text)
		{
			target.appendJavaScript(log("server", hook, JSONObject.quote(text)));
		}
	}

	/**
	 * Starts the upload of a field as soon as files are chosen or dropped.
	 */
	private static class UploadOnChoice extends Behavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void renderHead(Component component, IHeaderResponse response)
		{
			super.renderHead(component, response);
			response.render(OnEventHeaderItem.forComponent(component, "change",
				"if (event.target.files && event.target.files.length) { " +
					((UploadToResourceField)component).getTriggerUploadScript() + " }"));
		}
	}
}
