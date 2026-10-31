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
package com.antiliacode.widgets.demo.wizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractChoice.LabelPosition;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;

import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.upload.FilePickerBehavior;
import com.antiliacode.widgets.upload.FilePickerBehavior.Design;
import com.antiliacode.widgets.wizard.IWizardButtonFactory;
import com.antiliacode.widgets.wizard.IWizardStep;
import com.antiliacode.widgets.wizard.Wizard;

/**
 * An image card made in a {@link Wizard}: an image uploaded in the form of the wizard, by Ajax,
 * then a choice between two ways, describing the image or writing a text over it, and a preview.
 * The titles of the steps name the file; the step of the text has a button of its own, which
 * suggests a text from the image. The image is checked by its first bytes, whatever its name and
 * the type the browser gives it, and is kept in the object, 2 MB at most.
 */
public class ImageCardPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The most an image weighs. */
	static final Bytes MAX_SIZE = Bytes.megabytes(2);

	/** What to do with the image. */
	enum Way
	{
		/** A description, which is its alternative text too. */
		DESCRIBE,
		/** A text over the image. */
		CAPTION
	}

	/** Where the text over the image is. */
	enum Position
	{
		TOP,
		BOTTOM
	}

	/**
	 * What the wizard fills in.
	 */
	static class ImageCard implements Serializable
	{
		private static final long serialVersionUID = 1L;

		byte[] image;

		String contentType;

		String fileName;

		/** The size of the image in pixels, {@code null} when it could not be read. */
		Integer width;

		Integer height;

		Way way;

		String description;

		String caption;

		Position position = Position.TOP;
	}

	private final Wizard<ImageCard> wizard;

	/**
	 * @param id
	 *            component id
	 */
	public ImageCardPanel(String id)
	{
		super(id);
		wizard = new Wizard<>("wizard", Model.of(new ImageCard()), new UploadStep())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onFinish(AjaxRequestTarget target)
			{
				success(ImageCardPanel.this.getString("ready").replace("{0}",
					getModelObject().fileName));
				startAgain(target);
			}

			@Override
			protected void onCancel(AjaxRequestTarget target)
			{
				info(ImageCardPanel.this.getString("cancelled"));
				startAgain(target);
			}
		};
		wizard.setTitleLevel(4);
		wizard.getForm().setMultiPart(true);
		wizard.getForm().setFileMaxSize(MAX_SIZE);
		add(wizard);
	}

	private void startAgain(AjaxRequestTarget target)
	{
		wizard.setModelObject(new ImageCard());
		wizard.restart(target);
	}

	/**
	 * @return the wizard
	 */
	public Wizard<?> getWizard()
	{
		return wizard;
	}

	/**
	 * @return the type of the image by its first bytes: PNG, JPEG, GIF or WebP; {@code null} for
	 *         none of them
	 */
	static String imageType(byte[] bytes)
	{
		if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' &&
			bytes[3] == 'G')
		{
			return "image/png";
		}
		if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 &&
			(bytes[2] & 0xff) == 0xff)
		{
			return "image/jpeg";
		}
		if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' &&
			bytes[3] == '8')
		{
			return "image/gif";
		}
		if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' &&
			bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' &&
			bytes[11] == 'P')
		{
			return "image/webp";
		}
		return null;
	}

	/**
	 * Reads the size of the image from its header, without decoding its pixels.
	 */
	private static void readSize(ImageCard card)
	{
		card.width = null;
		card.height = null;
		try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(
			card.image)))
		{
			Iterator<ImageReader> readers = in != null ? ImageIO.getImageReaders(in) : null;
			if (readers != null && readers.hasNext())
			{
				ImageReader reader = readers.next();
				try
				{
					reader.setInput(in);
					card.width = reader.getWidth(0);
					card.height = reader.getHeight(0);
				}
				finally
				{
					reader.dispose();
				}
			}
		}
		catch (IOException e)
		{
			// the size is left out
		}
	}

	/**
	 * @return the image of the card, shown small or large by its class
	 */
	private static Component newImage(String id, ImageCard card, String alt)
	{
		return new NonCachingImage(id, new ByteArrayResource(card.contentType, card.image))
			.add(AttributeModifier.replace("alt", alt != null ? alt : ""));
	}

	/**
	 * The upload of the image, by Ajax with the form of the wizard; required until there is one.
	 */
	private class UploadStep implements IWizardStep<ImageCard>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IModel<String> getTitle(IModel<ImageCard> model)
		{
			return () -> model.getObject().fileName == null ? getString("upload") :
				getString("uploaded").replace("{0}", model.getObject().fileName);
		}

		@Override
		public Component newBody(String id, IModel<ImageCard> model)
		{
			Fragment body = new Fragment(id, "upload", ImageCardPanel.this);
			FileUploadField file = new FileUploadField("file")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onConfigure()
				{
					super.onConfigure();
					setRequired(model.getObject().image == null);
				}

				@Override
				public void updateModel()
				{
					FileUpload upload = getFileUpload();
					if (upload != null)
					{
						ImageCard card = model.getObject();
						card.image = upload.getBytes();
						card.contentType = imageType(card.image);
						card.fileName = upload.getClientFileName();
						readSize(card);
					}
				}
			};
			file.setLabel(new ResourceModel("image"));
			file.add(new FilePickerBehavior().setDesign(Design.DROP_ZONE));
			file.add(validatable -> {
				for (FileUpload upload : validatable.getValue())
				{
					if (upload.getSize() > MAX_SIZE.bytes())
					{
						validatable.error(new ValidationError(getString("tooLarge")));
					}
					else if (imageType(upload.getBytes()) == null)
					{
						validatable.error(new ValidationError(getString("notAnImage")));
					}
				}
			});
			body.add(file);
			body.add(new Label("current", () -> getString("current").replace("{0}",
				model.getObject().fileName != null ? model.getObject().fileName : ""))
				.setVisible(model.getObject().image != null));
			return body;
		}

		@Override
		public IWizardStep<ImageCard> getNextStep(IModel<ImageCard> model)
		{
			return new WayStep();
		}

		@Override
		public boolean isLastEnabled(IModel<ImageCard> model)
		{
			// without a way chosen, the steps go no further than the next one
			return model.getObject().way != null;
		}
	}

	/**
	 * The choice of what to do with the image, which the next step depends on.
	 */
	private class WayStep implements IWizardStep<ImageCard>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IModel<String> getTitle(IModel<ImageCard> model)
		{
			return () -> getString("way").replace("{0}", model.getObject().fileName);
		}

		@Override
		public Component newBody(String id, IModel<ImageCard> model)
		{
			Fragment body = new Fragment(id, "way", ImageCardPanel.this);
			body.add(newImage("thumbnail", model.getObject(), null));
			body.add(new RadioChoice<Way>("choice", new PropertyModel<Way>(model, "way"),
				List.of(Way.values()), new EnumChoiceRenderer<Way>(ImageCardPanel.this))
				.setLabelPosition(LabelPosition.WRAP_AFTER).setRequired(true)
				.setLabel(new ResourceModel("wayLabel")));
			return body;
		}

		@Override
		public IWizardStep<ImageCard> getNextStep(IModel<ImageCard> model)
		{
			Way way = model.getObject().way;
			return way == Way.DESCRIBE ? new DescribeStep() : way == Way.CAPTION ?
				new CaptionStep() : null;
		}

		@Override
		public boolean isFinish(IModel<ImageCard> model)
		{
			return false;
		}
	}

	/**
	 * A description of the image.
	 */
	private class DescribeStep implements IWizardStep<ImageCard>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IModel<String> getTitle(IModel<ImageCard> model)
		{
			return () -> getString("describe").replace("{0}", model.getObject().fileName);
		}

		@Override
		public Component newBody(String id, IModel<ImageCard> model)
		{
			Fragment body = new Fragment(id, "describe", ImageCardPanel.this);
			body.add(new TextArea<>("description", new PropertyModel<String>(model, "description"))
				.setRequired(true).add(StringValidator.maximumLength(500))
				.setLabel(new ResourceModel("description")));
			return body;
		}

		@Override
		public IWizardStep<ImageCard> getNextStep(IModel<ImageCard> model)
		{
			return new PreviewStep();
		}
	}

	/**
	 * A text over the image, at the top or at the bottom; a button of the step suggests one.
	 */
	private class CaptionStep implements IWizardStep<ImageCard>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IModel<String> getTitle(IModel<ImageCard> model)
		{
			return () -> getString("caption").replace("{0}", model.getObject().fileName);
		}

		@Override
		public Component newBody(String id, IModel<ImageCard> model)
		{
			Fragment body = new Fragment(id, "caption", ImageCardPanel.this);
			body.add(new TextField<>("text", new PropertyModel<String>(model, "caption"))
				.setRequired(true).add(StringValidator.maximumLength(80))
				.setLabel(new ResourceModel("captionLabel")));
			body.add(new RadioChoice<Position>("position",
				new PropertyModel<Position>(model, "position"), List.of(Position.values()),
				new EnumChoiceRenderer<Position>(ImageCardPanel.this))
				.setLabelPosition(LabelPosition.WRAP_AFTER).setRequired(true)
				.setLabel(new ResourceModel("position")));
			return body;
		}

		@Override
		public IWizardStep<ImageCard> getNextStep(IModel<ImageCard> model)
		{
			return new PreviewStep();
		}

		@Override
		public List<IWizardButtonFactory<ImageCard>> getButtons()
		{
			List<IWizardButtonFactory<ImageCard>> buttons = new ArrayList<>(
				IWizardButtonFactory.defaults());
			buttons.add(1, IWizardButtonFactory.button(new ResourceModel("suggest"),
				SvgIcon.WAND_MAGIC_SPARKLES, (target, wizard) -> {
					wizard.getModelObject().caption = suggest(wizard.getModelObject());
					wizard.refresh(target);
				}));
			return buttons;
		}
	}

	/**
	 * @return a text from what is known of the image: its name, its size and the day
	 */
	private String suggest(ImageCard card)
	{
		String name = card.fileName;
		int dot = name.lastIndexOf('.');
		if (dot > 0)
		{
			name = name.substring(0, dot);
		}
		name = name.replace('_', ' ').replace('-', ' ').trim();
		String day = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
			.withLocale(getLocale()));
		String text = card.width != null ? getString("suggestion") : getString(
			"suggestionNoSize");
		return text.replace("{0}", name).replace("{1}", String.valueOf(card.width))
			.replace("{2}", String.valueOf(card.height)).replace("{3}", day);
	}

	/**
	 * The card as it is: the image with its description below it, or with its text over it.
	 */
	private class PreviewStep implements IWizardStep<ImageCard>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IModel<String> getTitle(IModel<ImageCard> model)
		{
			return () -> getString("preview").replace("{0}", model.getObject().fileName);
		}

		@Override
		public Component newBody(String id, IModel<ImageCard> model)
		{
			ImageCard card = model.getObject();
			boolean caption = card.way == Way.CAPTION;
			Fragment body = new Fragment(id, "preview", ImageCardPanel.this);
			body.add(newImage("image", card, caption ? card.caption : card.description));
			body.add(new Label("text", card.caption).setVisible(caption).add(AttributeModifier
				.append("class", card.position == Position.BOTTOM ? "wizard-demo__text--bottom" :
					"wizard-demo__text--top")));
			body.add(new Label("description", card.description).setVisible(!caption));
			return body;
		}

		@Override
		public IWizardStep<ImageCard> getNextStep(IModel<ImageCard> model)
		{
			return null;
		}

		@Override
		public boolean hasNext(IModel<ImageCard> model)
		{
			return false;
		}

		@Override
		public boolean hasLast(IModel<ImageCard> model)
		{
			return false;
		}
	}
}
