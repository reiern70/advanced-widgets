/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.demo.richtext;

import java.util.List;

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
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.demo.DemoCases;
import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.antilia.widgets.dropdown.Dropdown;
import com.antilia.widgets.richtext.RichTextEditor;
import com.antilia.widgets.richtext.RichTextEditorEvent;
import com.antilia.widgets.richtext.RichTextEditorEventBehavior;
import com.antilia.widgets.richtext.RichTextEditorState;
import com.antilia.widgets.richtext.RichTextFormat;
import com.antilia.widgets.richtext.RichTextPolicy;
import com.antilia.widgets.richtext.RichTextTool;
import com.antilia.widgets.richtext.RichTextView;
import com.antilia.widgets.resizable.ResizableSize;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link RichTextEditor}: an article in HTML, which the server keeps in the session with the state
 * of its editor, where the cursor is, what is selected, how far it is scrolled and its size, and
 * cleans before it keeps it; below it, the article as a page shows it, a {@link RichTextView}, and
 * the HTML the server keeps. Check boxes and a drop-down switch the toolbar, the bubble menu,
 * reading only and the count of the words, rendering the editor again via Ajax. Ctrl+S, and the
 * button, save the article.
 * <p>
 * Notes in Markdown: the editor keeps Markdown, which shows beside it as the server keeps it.
 * <p>
 * The cleaning on the server: HTML from anywhere, with scripts, handlers and styles, and what
 * {@link RichTextPolicy} leaves of it. The React and Svelte tabs show the editor in those
 * frameworks.
 */
public class RichTextEditorPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The session attribute keeping the article, its text saved and the state of its editor. */
	static final String ARTICLE_ATTRIBUTE = "rich-text-demo.article";

	/** The session attribute keeping the notes in Markdown. */
	static final String NOTES_ATTRIBUTE = "rich-text-demo.notes";

	/** The toolbars of the drop-down. */
	static final List<String> TOOLBARS = List.of("full", "compact", "none");

	/** The compact toolbar. */
	static final List<List<RichTextTool>> COMPACT = List.of(List.of(RichTextTool.HEADING),
		List.of(RichTextTool.BOLD, RichTextTool.ITALIC, RichTextTool.LINK),
		List.of(RichTextTool.BULLET_LIST, RichTextTool.ORDERED_LIST),
		List.of(RichTextTool.UNDO, RichTextTool.REDO));

	private final RichTextEditor editor;

	private final Label known;

	private final Label heard;

	private final Component saved;

	private final RichTextEditor notesEditor;

	private final Label notesStatus;

	private final Component notesKept;

	private final Component cleaned;

	private String toolbar = "full";

	private boolean bubbleMenu = true;

	private boolean readOnly;

	private boolean count = true;

	private String heardMessage;

	private String notesMessage;

	private String unsafe = RichTextSamples.UNSAFE;

	/**
	 * Constructor.
	 */
	public RichTextEditorPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		DemoCases cases = new DemoCases("cases", this);
		themed.add(cases);
		Fragment articleCase = cases.addCase("articleTitle", "articleCase");
		Fragment notesCase = cases.addCase("notesTitle", "notesCase");
		Fragment cleaningCase = cases.addCase("cleaningTitle", "cleaningCase");

		known = new Label("known", this::describe);
		known.setOutputMarkupId(true);
		articleCase.add(known);

		Form<Void> editorForm = new Form<>("editorForm");
		editorForm.setOutputMarkupId(true);
		articleCase.add(editorForm);

		heard = new Label("heard", () -> heardMessage != null ? heardMessage : getString("start"));
		heard.setOutputMarkupId(true);
		editorForm.add(heard);

		IModel<RichTextDocument> article = RichTextEditorPage::article;
		editor = new RichTextEditor("editor", LambdaModel.of(article, RichTextDocument::getText,
			(document, text) -> {
				document.setText(text);
				Session.get().dirty();
			}), LambdaModel.of(article, RichTextDocument::getState, (document, state) -> {
				document.setState(state);
				Session.get().dirty();
			}))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setToolbar(toolbarOf(toolbar));
				setBubbleMenu(bubbleMenu);
				setReadOnly(readOnly);
				setCount(count);
			}

			@Override
			protected void onStateChanged(AjaxRequestTarget target)
			{
				target.add(known);
			}
		};
		editor.setSavedModel(LambdaModel.of(article, RichTextDocument::getSaved,
			RichTextDocument::setSaved));
		editor.setPlaceholder(Model.of(getString("placeholder")));
		editor.setLabel(Model.of(getString("articleLabel")));
		editor.add(new RichTextEditorEventBehavior(RichTextEditorEvent.FOCUS)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				hear(target, "heardFocus");
			}
		});
		editor.add(new RichTextEditorEventBehavior(RichTextEditorEvent.BLUR)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				hear(target, "heardBlur");
			}
		});
		editor.add(new RichTextEditorEventBehavior(RichTextEditorEvent.SAVE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				save(target);
			}
		});
		// the server follows the text as the user types, a moment after each pause
		editor.add(new RichTextEditorEventBehavior(RichTextEditorEvent.INPUT)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(known);
			}
		});
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
		savedBox.add(new RichTextView("view", () -> article().getSaved()));
		savedBox.add(new Label("html", () -> article().getSaved()));
		articleCase.add(savedBox);
		saved = savedBox;

		Form<Void> controls = new Form<>("controls");
		articleCase.add(controls);
		controls.add(new Dropdown<>("toolbar", new PropertyModel<>(this, "toolbar"), TOOLBARS,
			new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(String each)
				{
					return getString("toolbar." + each);
				}

				@Override
				public String getIdValue(String each, int index)
				{
					return each;
				}
			}).setRequired(true).add(rerender(editorForm)));
		for (String option : List.of("bubbleMenu", "readOnly", "count"))
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
		IModel<RichTextDocument> notes = RichTextEditorPage::notes;
		notesEditor = new RichTextEditor("editor", LambdaModel.of(notes, RichTextDocument::getText,
			(document, text) -> {
				document.setText(text);
				Session.get().dirty();
			}), LambdaModel.of(notes, RichTextDocument::getState, (document, state) -> {
				document.setState(state);
				Session.get().dirty();
			}));
		notesEditor.setFormat(RichTextFormat.MARKDOWN).setBubbleMenu(true)
			.setSavedModel(LambdaModel.of(notes, RichTextDocument::getSaved,
				RichTextDocument::setSaved))
			.setLabel(Model.of(getString("notesLabel")));
		notesEditor.add(new RichTextEditorEventBehavior(RichTextEditorEvent.SAVE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				saveNotes(target);
			}
		});
		notesEditor.add(new RichTextEditorEventBehavior(RichTextEditorEvent.INPUT)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(notesKept);
			}
		});
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
		TextArea<String> unsafeField = new TextArea<>("unsafe", new PropertyModel<>(this,
			"unsafe"));
		cleaningForm.add(unsafeField);
		WebMarkupContainer cleanedBox = new WebMarkupContainer("cleaned");
		cleanedBox.setOutputMarkupId(true);
		cleanedBox.add(new Label("html", () -> RichTextPolicy.ALL.sanitize(unsafe)));
		cleanedBox.add(new RichTextView("view", new PropertyModel<>(this, "unsafe")));
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

	/**
	 * @return the tools of a toolbar of the drop-down
	 */
	static List<List<RichTextTool>> toolbarOf(String name)
	{
		return switch (name == null ? "full" : name)
		{
			case "compact" -> COMPACT;
			case "none" -> List.of();
			default -> RichTextTool.DEFAULT_TOOLBAR;
		};
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
	static RichTextDocument article()
	{
		return document(ARTICLE_ATTRIBUTE, RichTextSamples.ARTICLE, true);
	}

	/**
	 * @return the notes of the session
	 */
	static RichTextDocument notes()
	{
		return document(NOTES_ATTRIBUTE, RichTextSamples.NOTES, false);
	}

	private static RichTextDocument document(String attribute, String text, boolean html)
	{
		Session session = Session.get();
		RichTextDocument document = (RichTextDocument)session.getAttribute(attribute);
		if (document == null)
		{
			document = new RichTextDocument(text, html);
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
	 * @return what the server knows of the article: its length and words, the selection, the size
	 *         of the editor and whether all is saved, in words
	 */
	String describe()
	{
		RichTextDocument document = article();
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
			.replace("{1}", String.valueOf(RichTextDocument.words(text).size()))
			.replace("{2}", selection)
			.replace("{3}", sized)
			.replace("{4}", getString(document.isModified() ? "unsaved" : "allSaved"));
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "richTextEditor";
	}

	/**
	 * @return the texts, the article, the notes and the HTML to clean as the page has them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "cases", "articleTitle", "notesTitle",
			"articleLabel", "notesLabel", "placeholder", "toolbar", "bubbleMenu", "readOnly",
			"count", "save", "unsaved", "allSaved", "appArticleTitle", "appHtml", "appMarkdown",
			"appKnown", "appSaved", "appNotClean", "appStart");
		TOOLBARS.forEach(each -> texts.put("toolbar." + each, getString("toolbar." + each)));
		return new JSONObject()
			.put("article", RichTextSamples.ARTICLE)
			.put("notes", RichTextSamples.NOTES)
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("editor", RichTextEditor.texts(this))
			.put("preview", new JSONObject().put("label", getString("MarkdownPreview.label"))
				.put("placeholder", getString("MarkdownPreview.placeholder")))
			.put("toolbars", new JSONArray(TOOLBARS));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		RichTextEditor.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(RichTextEditorPage.class, "RichTextEditorPage.css")));
	}

	public String getToolbar()
	{
		return toolbar;
	}

	public void setToolbar(String toolbar)
	{
		this.toolbar = toolbar;
	}

	public boolean isBubbleMenu()
	{
		return bubbleMenu;
	}

	public void setBubbleMenu(boolean bubbleMenu)
	{
		this.bubbleMenu = bubbleMenu;
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
