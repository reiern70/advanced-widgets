/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.upload;

import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;

import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.button.ButtonBehavior.Variant;
import com.antilia.widgets.theme.Looks;
import com.github.openjson.JSONObject;

/**
 * Draws a file input, such as Wicket's {@link org.apache.wicket.markup.html.form.upload.FileUploadField
 * FileUploadField} or {@link UploadToResourceField}, as a picker that looks the same in every
 * browser and takes the colors of the theme it is in: a button of the theme opening the browser's
 * dialog, or a zone the files are dropped on ({@link Design}), and what was chosen, the name and
 * the size of a file or the list of several.
 * <p>
 * Everything happens in the browser: {@code Wicket.Upload.FilePicker} from {@link Uploads#JS}
 * hides the input and puts the picker right after it. The input stays in the page and holds the
 * files: a form posts them as always, and dropped files go into it and fire its {@code change}
 * event, so an Ajax behavior on it, such as Wicket's
 * {@link org.apache.wicket.markup.html.form.upload.FilesSelectedBehavior FilesSelectedBehavior},
 * hears of them as of files chosen in the dialog. Dropped files the input does not accept, by its
 * {@code accept} attribute, stay out, and so do all but the first unless it is {@code multiple}.
 * A {@code multiple} input keeps the files chosen before: those chosen in the dialog or dropped are
 * added to them, and each can be taken out of the list again.
 * <p>
 * The picker knows the limits the server sets on uploads, those of the form, of its root form, or
 * of an {@link UploadToResourceField}: the most the files weigh together, the most a file weighs
 * and the most files at once, and for a field uploading to a resource that the folder its files go
 * to keeps one file of each name. It tells at once which the files chosen break, and keeps them from
 * being sent, with the form posted the usual way or by Ajax, or by the field, rather than letting
 * the server refuse them once they were sent, which a browser may not even hear of. While its files
 * are being sent it lets nothing change. Without JavaScript the native input is shown.
 * <p>
 * The script needs no inline style, so the picker works under a strict Content Security Policy.
 *
 * <pre>
 * form.add(new FileUploadField("files").add(new FilePickerBehavior().setDesign(Design.DROP_ZONE)));
 * </pre>
 *
 * @since 11.0.0
 */
public class FilePickerBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/**
	 * How a picker looks.
	 */
	public enum Design
	{
		/** A button of the theme, then what was chosen. */
		BUTTON,
		/** A zone to drop files on, with a button in it. */
		DROP_ZONE;

		/**
		 * @return the name of the design in the browser, such as {@code drop-zone}
		 */
		public String getName()
		{
			return name().toLowerCase(Locale.ROOT).replace('_', '-');
		}
	}

	private Design design = Design.BUTTON;

	private Variant variant;

	private Size size;

	/**
	 * @return how the picker looks
	 */
	public Design getDesign()
	{
		return design;
	}

	/**
	 * @param design
	 *            how the picker looks, a button by default
	 * @return {@code this}
	 */
	public FilePickerBehavior setDesign(Design design)
	{
		this.design = Args.notNull(design, "design");
		return this;
	}

	/**
	 * @return the variant of the button choosing files, {@code null} for that of the design:
	 *         secondary for a button, ghost in a zone
	 */
	public Variant getVariant()
	{
		return variant;
	}

	/**
	 * @param variant
	 *            the variant of the button choosing files, {@code null} for that of the design
	 * @return {@code this}
	 */
	public FilePickerBehavior setVariant(Variant variant)
	{
		this.variant = variant;
		return this;
	}

	/**
	 * @return the size of the button choosing files, {@code null} for that of the design: medium
	 *         for a button, small in a zone
	 */
	public Size getSize()
	{
		return size;
	}

	/**
	 * @param size
	 *            the size of the button choosing files, {@code null} for that of the design
	 * @return {@code this}
	 */
	public FilePickerBehavior setSize(Size size)
	{
		this.size = size;
		return this;
	}

	/**
	 * Clears the files chosen in a file input with a picker, firing its {@code change} event, as
	 * after the files were sent.
	 *
	 * @param input
	 *            the input
	 * @param target
	 *            the Ajax request
	 */
	public static void clear(Component input, IPartialPageRequestHandler target)
	{
		target.appendJavaScript("Wicket.Upload.FilePicker.of(" +
			JSONObject.quote(input.getMarkupId()) + ")?.clear();");
	}

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		component.setOutputMarkupId(true);
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		super.onComponentTag(component, tag);
		if (!"input".equalsIgnoreCase(tag.getName()) ||
			!"file".equalsIgnoreCase(tag.getAttribute("type")))
		{
			throw new WicketRuntimeException(
				"A FilePickerBehavior can only be attached to an <input type=\"file\">, not to <" +
					tag.getName() + "> of " + component);
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		Uploads.renderHeadItems(response);
		Looks.of(component).renderHead(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Upload.FilePicker.attach(" +
			JSONObject.quote(component.getMarkupId()) + ", " + newOptions(component) + ");"));
	}

	/**
	 * @return the options of the script: the design, the button, the limits of the uploads and the
	 *         texts
	 */
	protected JSONObject newOptions(Component component)
	{
		JSONObject options = new JSONObject();
		options.put("design", design.getName());
		if (variant != null)
		{
			options.put("variant", variant.name().toLowerCase(Locale.ROOT));
		}
		if (size != null)
		{
			options.put("size", size.name().toLowerCase(Locale.ROOT));
		}
		options.put("locale", component.getLocale().toLanguageTag());
		putLimits(component, options);
		JSONObject texts = new JSONObject();
		for (String key : new String[] { "choose", "chooseMany", "drop", "dropMany", "browse",
			"none", "chosen", "clear", "rejected", "tooLarge", "tooMany", "tooMuch", "sameName",
			"remove" })
		{
			texts.put(key, component.getString("FilePicker." + key));
		}
		options.put("texts", texts);
		return options;
	}

	/**
	 * Puts the limits the server sets on the uploads of the input: those of an
	 * {@link FileUploadToResourceField}, else of the root form of the input, which reads the files.
	 */
	private static void putLimits(Component component, JSONObject options)
	{
		if (component instanceof FileUploadToResourceField field)
		{
			putLimits(options, field.getMaxSize(), field.getFileMaxSize(), field.getFileCountMax());
			// its files go to a folder, by name
			options.put("uniqueNames", true);
			return;
		}
		Form<?> form = Form.findForm(component);
		if (form != null)
		{
			form = form.getRootForm();
			putLimits(options, form.getMaxSize(), form.getFileMaxSize(), form.getFileCountMax());
		}
	}

	private static void putLimits(JSONObject options, Bytes maxSize, Bytes fileMaxSize,
		long fileCountMax)
	{
		if (maxSize != null && maxSize.bytes() < Bytes.MAX.bytes())
		{
			options.put("maxSize", maxSize.bytes());
		}
		if (fileMaxSize != null && fileMaxSize.bytes() < Bytes.MAX.bytes())
		{
			options.put("fileMaxSize", fileMaxSize.bytes());
		}
		if (fileCountMax >= 0)
		{
			options.put("fileCountMax", fileCountMax);
		}
	}
}
