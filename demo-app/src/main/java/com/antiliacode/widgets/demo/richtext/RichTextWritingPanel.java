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

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.event.AjaxSubmitEvent;
import com.antiliacode.widgets.richtext.IRichTextProofreader;
import com.antiliacode.widgets.richtext.LanguageToolProofreader;
import com.antiliacode.widgets.richtext.RichTextEditor;
import com.antiliacode.widgets.richtext.RichTextEditorEvents;
import com.antiliacode.widgets.richtext.RichTextMention;
import com.antiliacode.widgets.richtext.RichTextView;

/**
 * Writing with the rich text editor: the spelling the browser checks, or not, in the language of
 * the text; a proofreader on the server, the demo's own ({@link RichTextWriting}) or LanguageTool
 * where the application is told its address ({@value #LANGUAGE_TOOL_PROPERTY}); the people to
 * mention after {@code @}, the topics after {@code #} and the terms completing a word on
 * Ctrl+Space, all from the server. Below, the text as a page shows it and the mentions the server
 * reads in it.
 */
public class RichTextWritingPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/**
	 * The system property giving the address of LanguageTool's check, such as
	 * {@code http://localhost:8081/v2/check}: the proofreader offers it then. Never the public
	 * server by default, which would hand the visitors' texts to a third party.
	 */
	public static final String LANGUAGE_TOOL_PROPERTY = "antilia-widgets.languagetool";

	/** The session attribute keeping the text. */
	static final String WRITING_ATTRIBUTE = "rich-text-demo.writing";

	static final List<String> SPELLCHECKS = List.of("browser", "on", "off");

	static final List<String> LANGUAGES = List.of("page", "en-GB", "en-US", "de", "es");

	private RichTextEditor editor;

	private Component kept;

	private Label status;

	private String spellcheck = "browser";

	private String textLanguage = "page";

	private String proofreading = "demo";

	private String statusMessage;

	/**
	 * @param id
	 *            component id
	 */
	public RichTextWritingPanel(String id)
	{
		super(id);
	}

	/**
	 * Builds the case once on the page, whose texts it says.
	 */
	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		IRichTextProofreader demo = RichTextWriting.proofreader(getString("misspelled"),
			getString("repeated"), getString("twoBlanks"));
		String languageTool = System.getProperty(LANGUAGE_TOOL_PROPERTY);

		Form<Void> form = new Form<>("writingForm");
		form.setOutputMarkupId(true);
		add(form);
		IModel<RichTextDocument> writing = RichTextWritingPanel::writing;
		editor = new RichTextEditor("editor", LambdaModel.of(writing, RichTextDocument::getText,
			(document, text) -> {
				document.setText(text);
				Session.get().dirty();
			}), LambdaModel.of(writing, RichTextDocument::getState, (document, state) -> {
				document.setState(state);
				Session.get().dirty();
			}))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setSpellcheck("on".equals(spellcheck) ? Boolean.TRUE : "off".equals(spellcheck) ?
					Boolean.FALSE : null);
				setTextLanguage("page".equals(textLanguage) ? null : Locale.forLanguageTag(
					textLanguage));
				setProofreader("demo".equals(proofreading) ? demo :
					"languagetool".equals(proofreading) && languageTool != null ?
						new LanguageToolProofreader(URI.create(languageTool)) : null);
			}
		};
		editor.addSuggester(RichTextWriting.people()).addSuggester(RichTextWriting.topics())
			.addSuggester(RichTextWriting.terms());
		editor.setSavedModel(LambdaModel.of(writing, RichTextDocument::getSaved,
			RichTextDocument::setSaved));
		editor.setLabel(Model.of(getString("writingLabel")));
		editor.setBubbleMenu(true).setCount(true);
		// images of other sites go: they would show the readers to those sites
		editor.setImagesFromSiteOnly(true);
		editor.addEvent(AjaxSubmitEvent.of(RichTextEditorEvents.SAVE,
			(eventId, values, submitted, target) -> save(target)));
		editor.addEvent(AjaxSubmitEvent.of(RichTextEditorEvents.INPUT,
			(eventId, values, submitted, target) -> showKept(target))
			.setDelay(RichTextEditorEvents.TYPING_DELAY));
		form.add(editor);
		form.add(new Label("writingName", getString("writingLabel"))
			.add(AttributeModifier.replace("for", () -> editor.getMarkupId())));
		status = new Label("writingStatus", () -> statusMessage != null ? statusMessage :
			getString("writingStart"));
		status.setOutputMarkupId(true);
		form.add(status);
		form.add(new AjaxButton("save")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				save(target);
			}
		}.add(new ButtonBehavior(ButtonBehavior.Variant.PRIMARY, ButtonBehavior.Size.SMALL)));

		WebMarkupContainer box = new WebMarkupContainer("writingKept");
		box.setOutputMarkupId(true);
		box.add(new RichTextView("view", () -> writing().getText()));
		box.add(new Label("mentions", this::describeMentions));
		add(box);
		kept = box;

		Form<Void> controls = new Form<>("writingControls");
		add(controls);
		controls.add(choice("spellcheck", SPELLCHECKS, form));
		controls.add(choice("textLanguage", LANGUAGES, form));
		controls.add(choice("proofreading", languageTool == null ? List.of("none", "demo") :
			List.of("none", "demo", "languagetool"), form));
	}

	private Dropdown<String> choice(String name, List<String> values, Component rendered)
	{
		Dropdown<String> choice = new Dropdown<>(name, new PropertyModel<>(this, name), values,
			new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(String each)
				{
					return getString(name + "." + each);
				}

				@Override
				public String getIdValue(String each, int index)
				{
					return each;
				}
			});
		choice.setRequired(true);
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(rendered);
			}
		});
		return choice;
	}

	/**
	 * @return the mentions the server reads in the text, or that there are none
	 */
	private String describeMentions()
	{
		List<RichTextMention> mentions = RichTextMention.of(writing().getText());
		return mentions.isEmpty() ? getString("noMentions") : getString("mentions") + " " +
			mentions.stream().map(mention -> mention.label() + " (" + mention.kind() + ": " +
				mention.id() + ")").collect(Collectors.joining(", "));
	}

	private void showKept(AjaxRequestTarget target)
	{
		target.add(kept);
	}

	private void save(AjaxRequestTarget target)
	{
		writing().save();
		Session.get().dirty();
		editor.markSaved(target);
		statusMessage = getString("writingSaved");
		target.add(status, kept);
	}

	/**
	 * @return the text of the session, its text saved and the state of its editor
	 */
	static RichTextDocument writing()
	{
		Session session = Session.get();
		RichTextDocument document = (RichTextDocument)session.getAttribute(WRITING_ATTRIBUTE);
		if (document == null)
		{
			document = new RichTextDocument(RichTextSamples.WRITING, true);
			session.setAttribute(WRITING_ATTRIBUTE, document);
		}
		return document;
	}

	public String getSpellcheck()
	{
		return spellcheck;
	}

	public void setSpellcheck(String spellcheck)
	{
		this.spellcheck = spellcheck;
	}

	public String getTextLanguage()
	{
		return textLanguage;
	}

	public void setTextLanguage(String textLanguage)
	{
		this.textLanguage = textLanguage;
	}

	public String getProofreading()
	{
		return proofreading;
	}

	public void setProofreading(String proofreading)
	{
		this.proofreading = proofreading;
	}
}
