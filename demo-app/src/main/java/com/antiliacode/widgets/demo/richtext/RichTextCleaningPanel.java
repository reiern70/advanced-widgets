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
package com.antiliacode.widgets.demo.richtext;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.danekja.java.util.function.serializable.SerializableBiFunction;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.notification.Notification;
import com.antiliacode.widgets.richtext.AbstractRichTextEditor;
import com.antiliacode.widgets.richtext.RichTextCleanedEvent;
import com.antiliacode.widgets.richtext.RichTextCleaning;
import com.antiliacode.widgets.richtext.RichTextCleaningReport;
import com.antiliacode.widgets.richtext.RichTextEditorPasteCleanedEvent;

/**
 * What the editors tell of their cleaning, in the case HTML from anywhere of the demos of the
 * rich text editors: an editor to paste into, which tells what a paste lost, as a notification
 * from the server and, once chosen, in a line under its toolbar; and what the server does when its
 * cleaning removes something from the text it gets, as chosen: nothing more, a notification
 * ({@link RichTextCleaning#REPORT}, heard as a {@link RichTextCleanedEvent}), or an error
 * ({@link RichTextCleaning#REJECT}), which the notifications of the page show. A button sends the
 * HTML from anywhere as the text of the editor, as a request made by hand would, past the cleaning
 * in the browser.
 */
public class RichTextCleaningPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final AbstractRichTextEditor<?> editor;

	private final Component kept;

	/** What the server does when its cleaning removes something. */
	private RichTextCleaning cleaning = RichTextCleaning.REPORT;

	/** Whether the editor tells what a paste lost under its toolbar. */
	private boolean pasteNotice = true;

	/** The text the server kept. */
	private String text;

	/**
	 * @param id
	 *            the component id
	 * @param newEditor
	 *            makes the editor, of an id and a model
	 * @param unsafeId
	 *            the id of the field holding the HTML from anywhere in the page
	 */
	public RichTextCleaningPanel(String id,
		SerializableBiFunction<String, IModel<String>, AbstractRichTextEditor<?>> newEditor,
		String unsafeId)
	{
		super(id);
		Form<Void> form = new Form<>("form");
		add(form);
		editor = newEditor.apply("editor", new PropertyModel<>(this, "text"));
		editor.setLabel(new StringResourceModel("pasteLabel", this));
		editor.setCleaning(cleaning).setPasteNotice(pasteNotice);
		editor.addEvent(RichTextEditorPasteCleanedEvent.of((report, target) -> notify(
			Notification.info(format("pasteLost", report)).setTitle(getString("pasteTitle")))));
		form.add(editor);

		form.add(new RadioChoice<>("cleaning", new PropertyModel<>(this, "cleaning"),
			List.of(RichTextCleaning.values()), new IChoiceRenderer<RichTextCleaning>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(RichTextCleaning each)
				{
					return getString("cleaning." + each.name());
				}

				@Override
				public String getIdValue(RichTextCleaning each, int index)
				{
					return each.name();
				}
			}).add(new AjaxFormChoiceComponentUpdatingBehavior()
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					// the server alone: nothing to render
					editor.setCleaning(cleaning);
				}
			}));
		form.add(new CheckBox("pasteNotice", new PropertyModel<>(this, "pasteNotice"))
			.add(new AjaxFormComponentUpdatingBehavior("change")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					// an option of the editor in the browser: rendered anew
					editor.setPasteNotice(pasteNotice);
					target.add(editor);
				}
			}));

		WebMarkupContainer keptBox = new WebMarkupContainer("kept");
		keptBox.setOutputMarkupId(true);
		keptBox.add(new Label("html", () -> text == null ? "" : text));
		add(keptBox);
		kept = keptBox;

		form.add(newSend("send", null));
		form.add(newSend("sendUnsafe", unsafeId));
	}

	/**
	 * @return a button sending the form; with the id of the field of the HTML from anywhere, as
	 *         the text of the editor, as a request made by hand would
	 */
	private AjaxButton newSend(String id, String unsafeId)
	{
		AjaxButton send = new AjaxButton(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				if (unsafeId != null)
				{
					// before the form is read: the text area of the editor takes the HTML as it is
					attributes.getAjaxCallListeners().add(new AjaxCallListener().onBefore(
						"var unsafe=document.getElementById(" + ScriptJson.quote(unsafeId) +
						"),field=document.getElementById(" + ScriptJson.quote(editor.getMarkupId()) +
						");if(unsafe&&field){field.value=unsafe.value;}"));
				}
			}

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				target.add(editor, kept);
			}

			@Override
			protected void onError(AjaxRequestTarget target)
			{
				// the editor shows what the user had, cleaned by the browser again
				target.add(editor);
			}
		};
		send.add(new ButtonBehavior(unsafeId == null ? ButtonBehavior.Variant.PRIMARY :
			ButtonBehavior.Variant.SECONDARY, ButtonBehavior.Size.SMALL));
		return send;
	}

	/**
	 * Shows what the server's cleaning removed, as the editor reports it in
	 * {@link RichTextCleaning#REPORT}.
	 */
	@Override
	public void onEvent(IEvent<?> event)
	{
		super.onEvent(event);
		if (event.getPayload() instanceof RichTextCleanedEvent cleaned &&
			cleaned.getEditor() == editor)
		{
			notify(Notification.warning(format("serverRemoved", cleaned.getReport()))
				.setTitle(getString("serverTitle")));
		}
	}

	private String format(String key, RichTextCleaningReport report)
	{
		return getString(key).replace("{0}", report.toString());
	}

	private void notify(Notification notification)
	{
		if (getPage() instanceof BasePage page)
		{
			page.getNotifications().show(notification);
		}
	}

	/**
	 * @return what the server does when its cleaning removes something
	 */
	public RichTextCleaning getCleaning()
	{
		return cleaning;
	}

	/**
	 * @param cleaning
	 *            what the server does when its cleaning removes something
	 */
	public void setCleaning(RichTextCleaning cleaning)
	{
		this.cleaning = cleaning == null ? RichTextCleaning.SILENT : cleaning;
	}

	/**
	 * @return whether the editor tells what a paste lost under its toolbar
	 */
	public boolean isPasteNotice()
	{
		return pasteNotice;
	}

	/**
	 * @param pasteNotice
	 *            whether the editor tells what a paste lost under its toolbar
	 */
	public void setPasteNotice(boolean pasteNotice)
	{
		this.pasteNotice = pasteNotice;
	}

	/**
	 * @return the text the server kept
	 */
	public String getText()
	{
		return text;
	}

	/**
	 * @param text
	 *            the text the server kept
	 */
	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * @return the editor
	 */
	public AbstractRichTextEditor<?> getEditor()
	{
		return editor;
	}
}
