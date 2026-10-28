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
package com.antiliacode.widgets.demo.ckeditor;

import java.util.List;
import java.util.Objects;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.ckeditor.CKEditor;
import com.antiliacode.widgets.ckeditor.CKEditorLibrary;
import com.antiliacode.widgets.ckeditor.CKEditorPolicy;
import com.antiliacode.widgets.ckeditor.CKEditorView;
import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.demo.richtext.OutlineEvent;
import com.antiliacode.widgets.demo.richtext.RichTextEditorPage;
import com.antiliacode.widgets.event.AjaxEvent;
import com.antiliacode.widgets.event.AjaxSubmitEvent;
import com.antiliacode.widgets.richtext.RichTextEditorChangeEvent;
import com.antiliacode.widgets.richtext.RichTextEditorEvents;
import com.antiliacode.widgets.richtext.RichTextEditorResizeEvent;
import com.antiliacode.widgets.richtext.RichTextEditorSelectedTextEvent;
import com.antiliacode.widgets.richtext.RichTextEditorState;
import com.antiliacode.widgets.richtext.RichTextFormat;
import com.antiliacode.widgets.resizable.ResizableSize;
import com.github.openjson.JSONObject;

/**
 * {@link CKEditor}: CKEditor 5, which the application brings with its license key
 * ({@link CKEditorAssets}), with the contract of the rich text editor. An article in HTML, which
 * the server keeps in the session with the state of its editor, where the cursor is, what is
 * selected, how far it is scrolled and its size, and cleans before it keeps it; below it, the
 * article as a page shows it, a {@link CKEditorView}, and the HTML the server keeps. Check boxes
 * switch reading only and the count of the words, rendering the editor again via Ajax. Ctrl+S,
 * and the button, save the article.
 * <p>
 * Notes in Markdown: the editor keeps Markdown, which shows beside it as the server keeps it.
 * <p>
 * The cleaning on the server: HTML from anywhere, with scripts, handlers and styles, and what
 * {@link CKEditorPolicy} leaves of it. The React and Svelte tabs show the editor in those
 * frameworks, with the CKEditor the page loaded.
 */
public class CKEditorPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The session attribute keeping the article, its text saved and the state of its editor. */
	static final String ARTICLE_ATTRIBUTE = "ckeditor-demo.article";

	/** The session attribute keeping the notes in Markdown. */
	static final String NOTES_ATTRIBUTE = "ckeditor-demo.notes";

	private final CKEditor editor;

	private final Label known;

	private final Label heard;

	private final Label selection;

	private final Label outline;

	private final Component saved;

	private final CKEditor notesEditor;

	private final Label notesStatus;

	private final Component notesKept;

	private final Component cleaned;

	private boolean readOnly;

	private boolean count = true;

	private String heardMessage;

	private String selectionMessage;

	private String outlineMessage;

	private String notesMessage;

	private String unsafe = CKEditorSamples.UNSAFE;

	/**
	 * Constructor.
	 */
	public CKEditorPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);
		// which CKEditor, and under which license
		CKEditorAssets.Release release = CKEditorAssets.release();
		WebMarkupContainer license = new WebMarkupContainer("license");
		themed.add(license);
		license.add(new ExternalLink("library", release.url(), release.name() + " " +
			release.version()));
		license.add(new Label("terms", () -> getString(CKEditorLibrary.get().isGpl() ?
			"license.gpl" : "license.commercial")).setRenderBodyOnly(true));

		DemoCases cases = new DemoCases("cases", this);
		themed.add(cases);
		Fragment articleCase = cases.addCase("articleTitle", "articleCase");
		Fragment notesCase = cases.addCase("notesTitle", "notesCase");
		Fragment cleaningCase = cases.addCase("cleaningTitle", "cleaningCase");

		known = new Label("known", this::describe);
		known.setOutputMarkupId(true);
		articleCase.add(known);
		selection = new Label("selection", () -> selectionMessage != null ? selectionMessage :
			getString("selectionStart"));
		selection.setOutputMarkupId(true);
		articleCase.add(selection);
		outline = new Label("outline", () -> outlineMessage != null ? outlineMessage :
			getString("outlineStart"));
		outline.setOutputMarkupId(true);
		articleCase.add(outline);

		Form<Void> editorForm = new Form<>("editorForm");
		editorForm.setOutputMarkupId(true);
		articleCase.add(editorForm);

		heard = new Label("heard", () -> heardMessage != null ? heardMessage : getString("start"));
		heard.setOutputMarkupId(true);
		editorForm.add(heard);

		IModel<CKEditorDocument> article = CKEditorPage::article;
		editor = new CKEditor("editor", LambdaModel.of(article, CKEditorDocument::getText,
			(document, text) -> {
				document.setText(text);
				Session.get().dirty();
			}), LambdaModel.of(article, CKEditorDocument::getState, (document, state) -> {
				document.setState(state);
				Session.get().dirty();
			}))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setReadOnly(readOnly);
				setCount(count);
			}

			@Override
			protected void onStateChanged(AjaxRequestTarget target)
			{
				target.add(known);
			}
		};
		editor.setSavedModel(LambdaModel.of(article, CKEditorDocument::getSaved,
			CKEditorDocument::setSaved));
		editor.setPlaceholder(Model.of(getString("placeholder")));
		editor.setLabel(Model.of(getString("articleLabel")));
		editor.addEvent(AjaxEvent.of(RichTextEditorEvents.FOCUS,
			(eventId, values, target) -> hear(target, "heardFocus")));
		// the text before and the text now, which the model gets, without the rest of the form
		editor.addEvent(RichTextEditorChangeEvent.of(editor, RichTextEditorEvents.BLUR,
			(previousText, currentText, target) -> hearBlur(target, previousText, currentText)));
		editor.addEvent(AjaxSubmitEvent.of(RichTextEditorEvents.SAVE,
			(eventId, values, form, target) -> save(target)));
		// the server follows the text as the user types, a moment after each pause
		editor.addEvent(AjaxSubmitEvent.of(RichTextEditorEvents.INPUT,
			(eventId, values, form, target) -> target.add(known))
			.setDelay(RichTextEditorEvents.TYPING_DELAY));
		// the size the user gave the article, which the browser sends once they let go
		editor.addEvent(RichTextEditorResizeEvent.of((width, height, target) -> hearSize(target,
			width, height)));
		// the types of events of the Tiptap editor, which CKEditor fires too: the text the user
		// selected, built in, and the headings, of the page's own
		editor.addEvent(RichTextEditorSelectedTextEvent.of((text, target) -> hearSelection(target,
			text)).setDelay(RichTextEditorEvents.TYPING_DELAY));
		editor.addEvent(OutlineEvent.of((headings, target) -> hearOutline(target, headings)));
		editorForm.add(editor);
		// a lambda, not editor::getMarkupId: see CodeEditorPage
		editorForm.add(new Label("articleName", getString("articleLabel"))
			.add(AttributeModifier.replace("for", () -> editor.getMarkupId())));
		editorForm.add(new AjaxButton("save")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				save(target);
			}
		}.add(new ButtonBehavior(ButtonBehavior.Variant.PRIMARY, ButtonBehavior.Size.SMALL)));

		WebMarkupContainer savedBox = new WebMarkupContainer("saved");
		savedBox.setOutputMarkupId(true);
		savedBox.add(new CKEditorView("view", () -> article().getSaved()));
		savedBox.add(new Label("html", () -> article().getSaved()));
		articleCase.add(savedBox);
		saved = savedBox;

		Form<Void> controls = new Form<>("controls");
		articleCase.add(controls);
		for (String option : List.of("readOnly", "count"))
		{
			controls.add(new CheckBox(option, new PropertyModel<>(this, option))
				.add(rerender(editorForm)));
		}

		notesStatus = new Label("notesStatus", () -> notesMessage != null ? notesMessage :
			getString("notesStart"));
		notesStatus.setOutputMarkupId(true);
		Form<Void> notesForm = new Form<>("notesForm");
		notesCase.add(notesForm);
		notesForm.add(notesStatus);
		IModel<CKEditorDocument> notes = CKEditorPage::notes;
		notesEditor = new CKEditor("editor", LambdaModel.of(notes, CKEditorDocument::getText,
			(document, text) -> {
				document.setText(text);
				Session.get().dirty();
			}), LambdaModel.of(notes, CKEditorDocument::getState, (document, state) -> {
				document.setState(state);
				Session.get().dirty();
			}));
		notesEditor.setFormat(RichTextFormat.MARKDOWN)
			.setSavedModel(LambdaModel.of(notes, CKEditorDocument::getSaved,
				CKEditorDocument::setSaved))
			.setLabel(Model.of(getString("notesLabel")));
		notesEditor.addEvent(AjaxSubmitEvent.of(RichTextEditorEvents.SAVE,
			(eventId, values, form, target) -> saveNotes(target)));
		notesEditor.addEvent(AjaxSubmitEvent.of(RichTextEditorEvents.INPUT,
			(eventId, values, form, target) -> showNotesKept(target))
			.setDelay(RichTextEditorEvents.TYPING_DELAY));
		notesForm.add(notesEditor);
		notesForm.add(new Label("notesName", getString("notesLabel"))
			.add(AttributeModifier.replace("for", () -> notesEditor.getMarkupId())));
		notesForm.add(new AjaxButton("save")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				saveNotes(target);
			}
		}.add(new ButtonBehavior(ButtonBehavior.Variant.PRIMARY, ButtonBehavior.Size.SMALL)));
		WebMarkupContainer kept = new WebMarkupContainer("kept");
		kept.setOutputMarkupId(true);
		kept.add(new Label("markdown", () -> notes().getText()));
		notesCase.add(kept);
		notesKept = kept;

		Form<Void> cleaningForm = new Form<>("cleaningForm");
		cleaningCase.add(cleaningForm);
		cleaningForm.add(new TextArea<>("unsafe", new PropertyModel<>(this, "unsafe")));
		WebMarkupContainer cleanedBox = new WebMarkupContainer("cleaned");
		cleanedBox.setOutputMarkupId(true);
		cleanedBox.add(new Label("html", () -> CKEditorPolicy.ALL.sanitize(unsafe)));
		cleanedBox.add(new CKEditorView("view", new PropertyModel<>(this, "unsafe")));
		cleaningCase.add(cleanedBox);
		cleaned = cleanedBox;
		cleaningForm.add(new AjaxButton("clean")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				target.add(cleaned);
			}
		}.add(new ButtonBehavior(ButtonBehavior.Variant.PRIMARY, ButtonBehavior.Size.SMALL)));
	}

	private AjaxFormComponentUpdatingBehavior rerender(Component component)
	{
		return new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(component, known);
			}
		};
	}

	/**
	 * @return the article of the session, its text saved and the state of its editor
	 */
	static CKEditorDocument article()
	{
		return document(ARTICLE_ATTRIBUTE, CKEditorSamples.ARTICLE, true);
	}

	/**
	 * @return the notes of the session
	 */
	static CKEditorDocument notes()
	{
		return document(NOTES_ATTRIBUTE, CKEditorSamples.NOTES, false);
	}

	private static CKEditorDocument document(String attribute, String text, boolean html)
	{
		Session session = Session.get();
		CKEditorDocument document = (CKEditorDocument)session.getAttribute(attribute);
		if (document == null)
		{
			document = new CKEditorDocument(text, html);
			session.setAttribute(attribute, document);
		}
		return document;
	}

	/**
	 * Saves the article, telling the editor and saying so.
	 */
	private void save(AjaxRequestTarget target)
	{
		article().save();
		Session.get().dirty();
		editor.markSaved(target);
		target.add(saved);
		hear(target, "heardSave");
	}

	private void saveNotes(AjaxRequestTarget target)
	{
		notes().save();
		Session.get().dirty();
		notesEditor.markSaved(target);
		notesMessage = getString("notesSaved");
		target.add(notesStatus, notesKept);
	}

	/**
	 * Says what the server heard last, and what it knows.
	 */
	private void hear(AjaxRequestTarget target, String key)
	{
		heardMessage = getString(key);
		target.add(heard, known);
	}

	/**
	 * Shows what the server keeps of the notes, as the user types.
	 */
	private void showNotesKept(AjaxRequestTarget target)
	{
		// a method, not a lambda: the field is set after the event is made
		target.add(notesKept);
	}

	/**
	 * Says the user left the article, and whether its text changed since the server last had it.
	 */
	private void hearBlur(AjaxRequestTarget target, String previousText, String currentText)
	{
		if (Objects.equals(previousText, currentText))
		{
			hear(target, "heardBlur");
			return;
		}
		heardMessage = getString("heardBlurChanged")
			.replace("{0}", String.valueOf(previousText == null ? 0 : previousText.length()))
			.replace("{1}", String.valueOf(currentText == null ? 0 : currentText.length()));
		target.add(heard, known);
	}

	/**
	 * Shows the text the user selected, which the browser sent: the start of it, escaped as the
	 * label escapes what it shows.
	 */
	private void hearSelection(AjaxRequestTarget target, String text)
	{
		String shown = text.replaceAll("\\s+", " ").strip();
		selectionMessage = shown.isEmpty() ? getString("selectionNone") :
			getString("selectionHeard").replace("{0}", shown.length() > RichTextEditorPage.SHOWN_SELECTION ?
				shown.substring(0, RichTextEditorPage.SHOWN_SELECTION) + "…" : shown);
		target.add(selection);
	}

	/**
	 * Shows the headings of the article, which the browser sent each time they changed.
	 */
	private void hearOutline(AjaxRequestTarget target, List<String> headings)
	{
		outlineMessage = headings.isEmpty() ? getString("outlineNone") :
			getString("outlineHeard").replace("{0}", String.join(" · ", headings));
		target.add(outline);
	}

	/**
	 * Says the size the user resized the article to, which the browser sent with the event.
	 */
	private void hearSize(AjaxRequestTarget target, int width, int height)
	{
		heardMessage = getString("heardResize").replace("{0}", getString("sizeGiven")
			.replace("{0}", width < 0 ? "auto" : String.valueOf(width))
			.replace("{1}", height < 0 ? "auto" : String.valueOf(height)));
		target.add(heard);
	}

	/**
	 * @return what the server knows of the article: its length and words, the selection, the size
	 *         of the editor and whether all is saved, in words
	 */
	String describe()
	{
		CKEditorDocument document = article();
		String text = document.getText() == null ? "" : document.getText();
		RichTextEditorState state = document.getState() == null ? RichTextEditorState.NONE :
			document.getState();
		String selection = state.isCursor() ? getString("cursorAt").replace("{0}",
			String.valueOf(state.getHead())) : getString("selected")
				.replace("{0}", String.valueOf(Math.min(state.getAnchor(), state.getHead())))
				.replace("{1}", String.valueOf(Math.max(state.getAnchor(), state.getHead())));
		ResizableSize size = state.getSize();
		String sized = size.getHeight() == null && size.getWidth() == null ?
			getString("sizeOfPage") : getString("sizeGiven")
				.replace("{0}", size.getWidth() == null ? "auto" : String.valueOf(size.getWidth()))
				.replace("{1}", size.getHeight() == null ? "auto" : String.valueOf(size.getHeight()));
		return getString("known")
			.replace("{0}", String.valueOf(text.length()))
			.replace("{1}", String.valueOf(CKEditorDocument.words(text).size()))
			.replace("{2}", selection)
			.replace("{3}", sized)
			.replace("{4}", getString(document.isModified() ? "unsaved" : "allSaved"));
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "ckEditor";
	}

	/**
	 * @return the texts, the article, the notes, and CKEditor's license key and language, as the
	 *         page has them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		CKEditorLibrary library = CKEditorLibrary.get();
		JSONObject config = new JSONObject()
			.put("article", CKEditorSamples.ARTICLE)
			.put("notes", CKEditorSamples.NOTES)
			.put("texts", FrameworkTabs.texts(this, "cases", "notesTitle", "articleLabel",
				"notesLabel", "placeholder", "readOnly", "count", "save", "unsaved", "allSaved",
				"appArticleTitle", "appHtml", "markdownKept", "appKnown", "appStart", "appSaved",
				"appNotClean", "appLicense"))
			.put("editor", CKEditor.texts(this))
			.put("licenseKey", library.getLicenseKey());
		String language = library.getLanguage(getLocale());
		if (language != null)
		{
			config.put("language", language);
		}
		return config;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		// CKEditor for the editors of the React and Svelte apps too
		CKEditorLibrary library = CKEditorLibrary.get();
		CKEditor.renderHeadItems(response, library, library.getLanguage(getLocale()));
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(RichTextEditorPage.class, "RichTextEditorPage.css")));
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(CKEditorPage.class, "CKEditorPage.css")));
	}

	public boolean isReadOnly()
	{
		return readOnly;
	}

	public void setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
	}

	public boolean isCount()
	{
		return count;
	}

	public void setCount(boolean count)
	{
		this.count = count;
	}

	public String getUnsafe()
	{
		return unsafe;
	}

	public void setUnsafe(String unsafe)
	{
		this.unsafe = unsafe;
	}
}
