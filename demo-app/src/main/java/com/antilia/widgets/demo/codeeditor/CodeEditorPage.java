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
package com.antilia.widgets.demo.codeeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.codeeditor.CodeEditor;
import com.antilia.widgets.codeeditor.CodeEditorEvent;
import com.antilia.widgets.codeeditor.CodeEditorEventBehavior;
import com.antilia.widgets.codeeditor.CodeEditorLanguage;
import com.antilia.widgets.codeeditor.CodeEditorState;
import com.antilia.widgets.codeeditor.MarkdownPreview;
import com.antilia.widgets.demo.DemoCases;
import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.antilia.widgets.dropdown.Dropdown;
import com.antilia.widgets.icon.SvgIcon;
import com.antilia.widgets.resizable.ResizableBehavior;
import com.antilia.widgets.resizable.ResizableSize;
import com.antilia.widgets.resizable.ResizeAxis;
import com.antilia.widgets.split.SplitOrientation;
import com.antilia.widgets.split.SplitPane;
import com.antilia.widgets.split.SplitPanel;
import com.antilia.widgets.split.SplitState;
import com.antilia.widgets.tabs.Tab;
import com.antilia.widgets.tabs.Tabs;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link CodeEditor}: a sample in each language the editor knows, Java, JavaScript, TypeScript,
 * JSON and Markdown, and in that of a log, whose grammar the page plugs in with a script of its own.
 * The texts and the states of the editor, where the cursor is, what is selected, how far the text
 * is scrolled and the size given to the editor, are kept in the session: the server hears each of
 * them as the user goes, and says below the editor what it knows; switching languages, and coming
 * back to the page, the editor takes them up again. Check boxes and drop-downs switch the line
 * numbers, reading only, selecting and wrapping, and how the editor resizes, rendering it again via
 * Ajax. Ctrl+S, and the button, save the text.
 * <p>
 * Notes in Markdown show beside their {@link MarkdownPreview preview}, in a {@link SplitPanel}
 * whose height the user resizes: the preview follows the text as the user types, in the browser,
 * and a button hides it, giving the editor all the room, and shows it again, collapsing the pane
 * from the server without rendering the panes again. The session keeps the notes, the state of
 * their editor, where the divider is, which pane is collapsed and the height given.
 * <p>
 * The files of a small project open in closable {@link Tabs}, an editor in each: a tab is marked
 * while its file has changes not saved, which the editor tells the tabs in the browser, and
 * closing it asks first; switching tabs keeps where the cursor of each was. The React and Svelte
 * tabs show the same in those frameworks.
 */
public class CodeEditorPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The session attribute keeping the samples, their texts and their states. */
	static final String SAMPLES_ATTRIBUTE = "code-editor-demo.samples";

	/** The session attribute keeping the notes in Markdown, and how they show. */
	static final String NOTES_ATTRIBUTE = "code-editor-demo.notes";

	/** How the editor resizes, in the order of the drop-down: the names of the axes, or none. */
	static final List<String> RESIZES = List.of("vertical", "horizontal", "both", "none");

	private final CodeEditor editor;

	private final Label known;

	private final Label heard;

	private final Label filesStatus;

	private final Tabs files;

	private final CodeEditor notesEditor;

	private final SplitPanel notesSplit;

	private final Label notesStatus;

	private final Component togglePreview;

	private CodeEditorLanguage language = CodeEditorLanguage.JAVA;

	private String resize = "vertical";

	private boolean lineNumbers = true;

	private boolean readOnly;

	private boolean selectable = true;

	private boolean lineWrapping;

	private String heardMessage;

	private String filesMessage;

	private String notesMessage;

	/**
	 * Constructor.
	 */
	public CodeEditorPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		DemoCases cases = new DemoCases("cases", this);
		themed.add(cases);
		Fragment editorCase = cases.addCase("editorTitle", "editorCase");
		Fragment notesCase = cases.addCase("notesTitle", "notesCase");
		Fragment filesCase = cases.addCase("filesTitle", "filesCase");

		known = new Label("known", this::describe);
		known.setOutputMarkupId(true);
		editorCase.add(known);

		Form<Void> editorForm = new Form<>("editorForm");
		editorForm.setOutputMarkupId(true);
		editorCase.add(editorForm);

		heard = new Label("heard", () -> heardMessage != null ? heardMessage : getString("start"));
		heard.setOutputMarkupId(true);
		editorForm.add(heard);

		// the sample shown, whose text, state and text saved the session keeps
		IModel<CodeDocument> shown = this::sample;
		editor = new CodeEditor("editor", LambdaModel.of(shown, CodeDocument::getText, (document,
			text) -> {
				document.setText(text);
				Session.get().dirty();
			}), LambdaModel.of(shown, CodeDocument::getState, (document, state) -> {
				document.setState(state);
				Session.get().dirty();
			}))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setLanguage(language);
				setLineNumbers(lineNumbers);
				setReadOnly(readOnly);
				setSelectable(selectable);
				setLineWrapping(lineWrapping);
				setResizable(axisOf(resize));
			}

			@Override
			protected void onStateChanged(AjaxRequestTarget target)
			{
				target.add(known);
			}
		};
		editor.setSavedModel(LambdaModel.of(shown, CodeDocument::getSaved, CodeDocument::setSaved));
		editor.setLabel(() -> sample().getName());
		editor.add(new CodeEditorEventBehavior(CodeEditorEvent.FOCUS)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				hear(target, "heardFocus");
			}
		});
		editor.add(new CodeEditorEventBehavior(CodeEditorEvent.BLUR)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				hear(target, "heardBlur");
			}
		});
		editor.add(new CodeEditorEventBehavior(CodeEditorEvent.SAVE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				save(target);
			}
		});
		// the server follows the text as the user types, a moment after each pause
		editor.add(new CodeEditorEventBehavior(CodeEditorEvent.INPUT)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(known);
			}
		});
		editorForm.add(editor);
		// lambdas, not editor::getMarkupId and preview::getMarkupId: javac deserializes two method
		// references to one inherited method as the first one's, and the page fails to load
		// from the page store
		editorForm.add(new Label("name", () -> sample().getName())
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

		Form<Void> controls = new Form<>("controls");
		editorCase.add(controls);
		controls.add(new Dropdown<>("language", new PropertyModel<>(this, "language"),
			CodeSamples.LANGUAGES, new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(CodeEditorLanguage each)
				{
					return getString("language." + each.getName());
				}

				@Override
				public String getIdValue(CodeEditorLanguage each, int index)
				{
					return each.getName();
				}
			}).setRequired(true).add(rerender(editorForm)));
		controls.add(new Dropdown<>("resize", new PropertyModel<>(this, "resize"), RESIZES,
			new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(String each)
				{
					return getString("resize." + each);
				}

				@Override
				public String getIdValue(String each, int index)
				{
					return each;
				}
			}).setRequired(true).add(rerender(editorForm)));
		for (String option : List.of("lineNumbers", "readOnly", "selectable", "lineWrapping"))
		{
			controls.add(new CheckBox(option, new PropertyModel<>(this, option))
				.add(rerender(editorForm)));
		}

		notesStatus = new Label("notesStatus", () -> notesMessage != null ? notesMessage :
			getString("notesStart"));
		notesStatus.setOutputMarkupId(true);
		notesCase.add(notesStatus);
		Form<Void> notesForm = new Form<>("notesForm");
		notesCase.add(notesForm);
		notesForm.add(new Label("notesName", () -> notes().document.getName()));
		IModel<CodeDocument> notes = () -> notes().document;
		notesEditor = new CodeEditor("editor", LambdaModel.of(notes, CodeDocument::getText,
			(document, text) -> {
				document.setText(text);
				Session.get().dirty();
			}), LambdaModel.of(notes, CodeDocument::getState, (document, state) -> {
				document.setState(state);
				Session.get().dirty();
			}));
		// the editor fills its pane, whose size the split panel and the box give
		notesEditor.setLanguage(CodeEditorLanguage.MARKDOWN).setLineWrapping(true)
			.setResizable(null).setSavedModel(LambdaModel.of(notes, CodeDocument::getSaved,
				CodeDocument::setSaved))
			.setLabel(() -> notes().document.getName());
		notesEditor.add(new CodeEditorEventBehavior(CodeEditorEvent.SAVE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				saveNotes(target);
			}
		});
		// the server follows the text as the user types, a moment after each pause, keeping it in
		// the session; the preview follows it in the browser
		notesEditor.add(new CodeEditorEventBehavior(CodeEditorEvent.INPUT)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
			}
		});
		MarkdownPreview preview = new MarkdownPreview("preview", notesEditor);
		notesSplit = new SplitPanel("notesSplit", SplitOrientation.HORIZONTAL, new IModel<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public SplitState getObject()
			{
				return notes().split;
			}

			@Override
			public void setObject(SplitState split)
			{
				notes().split = split;
				Session.get().dirty();
			}
		})
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newFirstPane(String id)
			{
				return new Fragment(id, "notesEditorPane", CodeEditorPage.this).add(notesEditor);
			}

			@Override
			protected Component newSecondPane(String id)
			{
				return new Fragment(id, "notesPreviewPane", CodeEditorPage.this).add(preview);
			}

			@Override
			protected void onCollapsed(AjaxRequestTarget target)
			{
				SplitPane collapsed = getCollapsed();
				sayAboutNotes(target, collapsed == SplitPane.SECOND ? "previewHidden" :
					collapsed == SplitPane.FIRST ? "editorHidden" : "bothShown");
			}
		};
		notesSplit.setCollapsible(true).setSendToServer(true).setMin(20).setMinSecond(20);
		// the box of the split panel, whose height the user gives by the bar along its bottom
		notesSplit.add(new ResizableBehavior(ResizeAxis.VERTICAL, LambdaModel.of(
			CodeEditorPage::notes, each -> each.size, (each, size) -> {
				each.size = size;
				Session.get().dirty();
			})).setMinHeight(160).setSendToServer(true));
		notesForm.add(notesSplit);
		notesForm.add(new AjaxButton("save")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				saveNotes(target);
			}
		}.add(new ButtonBehavior(ButtonBehavior.Variant.PRIMARY, ButtonBehavior.Size.SMALL)));
		togglePreview = new AjaxLink<Void>("togglePreview")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				// collapsed in the browser, the panes stay as they are: the text, the cursor...
				boolean shown = isPreviewShown();
				notesSplit.collapse(shown ? SplitPane.SECOND : null, target);
				sayAboutNotes(target, shown ? "previewHidden" : "bothShown");
			}
		}.setBody(() -> getString(isPreviewShown() ? "hidePreview" : "showPreview"))
			.add(AttributeModifier.replace("aria-controls", () -> preview.getMarkupId()))
			.add(new ButtonBehavior(ButtonBehavior.Variant.SECONDARY, ButtonBehavior.Size.SMALL))
			.setOutputMarkupId(true);
		notesForm.add(togglePreview);

		filesStatus = new Label("filesStatus", () -> filesMessage != null ? filesMessage :
			getString("filesStart"));
		filesStatus.setOutputMarkupId(true);
		filesCase.add(filesStatus);

		files = new Tabs("files")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClosed(AjaxRequestTarget target, Tab tab)
			{
				sayAboutFiles(target, "closed", ((FileTab)tab).document.getName());
			}
		};
		files.setLabel(Model.of(getString("filesLabel")));
		CodeSamples.files().forEach(document -> files.addTab(new FileTab(document.copy())));
		filesCase.add(files);
	}

	/**
	 * @return how the editor resizes, by the name in the drop-down
	 */
	static ResizeAxis axisOf(String resize)
	{
		return ResizeAxis.fromCssName(resize);
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
	 * @return the samples of the session: their texts and their states, as the user left them
	 */
	@SuppressWarnings("unchecked")
	static Map<String, CodeDocument> samples()
	{
		Session session = Session.get();
		Map<String, CodeDocument> samples = (Map<String, CodeDocument>)session.getAttribute(
			SAMPLES_ATTRIBUTE);
		if (samples == null)
		{
			samples = new LinkedHashMap<>(CodeSamples.samples());
			session.setAttribute(SAMPLES_ATTRIBUTE, (Serializable)samples);
		}
		return samples;
	}

	/**
	 * @return the sample of the language shown
	 */
	CodeDocument sample()
	{
		return samples().get(language.getName());
	}

	/**
	 * Saves the sample shown, telling the editor and saying so.
	 */
	private void save(AjaxRequestTarget target)
	{
		sample().save();
		Session.get().dirty();
		editor.markSaved(target);
		hear(target, "heardSave");
	}

	/**
	 * Says what the server heard last, and what it knows.
	 */
	private void hear(AjaxRequestTarget target, String key)
	{
		heardMessage = getString(key).replace("{0}", sample().getName());
		target.add(heard, known);
	}

	/**
	 * Saves the notes, telling the editor and saying so.
	 */
	private void saveNotes(AjaxRequestTarget target)
	{
		notes().document.save();
		Session.get().dirty();
		notesEditor.markSaved(target);
		sayAboutNotes(target, "saved");
	}

	/**
	 * Says what happened to the notes, renders the button showing and hiding the preview again.
	 */
	private void sayAboutNotes(AjaxRequestTarget target, String key)
	{
		notesMessage = getString(key).replace("{0}", notes().document.getName());
		target.add(notesStatus, togglePreview);
	}

	/**
	 * @return whether the preview of the notes shows, alone or beside the editor
	 */
	public boolean isPreviewShown()
	{
		return notesSplit.getCollapsed() != SplitPane.SECOND;
	}

	/**
	 * @return the notes of the session, and how they show
	 */
	static Notes notes()
	{
		Session session = Session.get();
		Notes notes = (Notes)session.getAttribute(NOTES_ATTRIBUTE);
		if (notes == null)
		{
			notes = new Notes(CodeSamples.notes());
			session.setAttribute(NOTES_ATTRIBUTE, notes);
		}
		return notes;
	}

	private void sayAboutFiles(AjaxRequestTarget target, String key, String name)
	{
		filesMessage = getString(key).replace("{0}", name);
		target.add(filesStatus);
	}

	/**
	 * @return what the server knows of the sample shown: its lines, where the cursor is, what is
	 *         selected, the size of the editor and whether all is saved, in words
	 */
	String describe()
	{
		CodeDocument document = sample();
		return describe(this, document.getName(), document.getText(), document.getState() == null ?
			CodeEditorState.NONE : document.getState(), document.isModified());
	}

	/**
	 * @return what is known of a text and the state of its editor, in words, as the page says it
	 */
	static String describe(Component component, String name, String text, CodeEditorState state,
		boolean modified)
	{
		String normalized = text == null ? "" : text.replace("\r\n", "\n");
		int cursor = Math.min(state.getCursor(), normalized.length());
		String before = normalized.substring(0, cursor);
		int line = (int)before.chars().filter(c -> c == '\n').count() + 1;
		int column = cursor - before.lastIndexOf('\n');
		int selected = state.getRanges().stream().mapToInt(range -> range.to() - range.from()).sum();
		ResizableSize size = state.getSize();
		String sized = size.getHeight() == null && size.getWidth() == null ?
			component.getString("sizeOfPage") : component.getString("sizeGiven")
				.replace("{0}", size.getWidth() == null ? "auto" : String.valueOf(size.getWidth()))
				.replace("{1}", size.getHeight() == null ? "auto" : String.valueOf(size.getHeight()));
		return component.getString("known")
			.replace("{0}", name)
			.replace("{1}", String.valueOf(normalized.isEmpty() ? 0 : normalized.split("\n", -1).length))
			.replace("{2}", String.valueOf(line))
			.replace("{3}", String.valueOf(column))
			.replace("{4}", String.valueOf(selected))
			.replace("{5}", sized)
			.replace("{6}", component.getString(modified ? "unsaved" : "allSaved"));
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "codeEditor";
	}

	/**
	 * @return the texts, the languages and their samples, the files and the options as the page has
	 *         them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "cases", "editorTitle", "notesTitle",
			"filesTitle", "language", "resize", "lineNumbers", "readOnly", "selectable",
			"lineWrapping", "save", "heardSave", "sizeOfPage", "sizeGiven", "unsaved", "allSaved",
			"hidePreview", "showPreview", "previewHidden", "editorHidden", "bothShown", "filesLabel",
			"filesStart", "closed", "saved");
		texts.put("notesStart", getString("appNotesStart"));
		// the app, not the server, keeps the texts and the states
		texts.put("start", getString("appStart")).put("heardFocus", getString("appHeardFocus"))
			.put("heardBlur", getString("appHeardBlur")).put("known", getString("appKnown"));
		JSONArray languages = new JSONArray();
		CodeSamples.samples().values().forEach(sample -> {
			String name = sample.getLanguage().getName();
			texts.put("language." + name, getString("language." + name));
			languages.put(new JSONObject().put("name", name).put("file", sample.getName())
				.put("text", sample.getText()));
		});
		RESIZES.forEach(each -> texts.put("resize." + each, getString("resize." + each)));
		JSONArray filesConfig = new JSONArray();
		CodeSamples.files().forEach(file -> filesConfig.put(new JSONObject()
			.put("name", file.getName()).put("language", file.getLanguage().getName())
			.put("text", file.getText())));
		JSONObject tabs = new JSONObject();
		for (String name : new String[] { "close", "modified", "closeModifiedTitle", "closeModified",
			"discard", "keep" })
		{
			tabs.put(name, getString("Tabs." + name));
		}
		CodeDocument notes = CodeSamples.notes();
		JSONObject split = new JSONObject();
		for (String name : new String[] { "divider", "collapseLeft", "collapseRight", "restore" })
		{
			split.put(name, getString("SplitPanel." + name));
		}
		return new JSONObject()
			.put("notes", new JSONObject().put("file", notes.getName()).put("text", notes.getText()))
			.put("preview", new JSONObject().put("label", getString("MarkdownPreview.label"))
				.put("placeholder", getString("MarkdownPreview.placeholder")))
			.put("split", split)
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("editor", CodeEditor.texts(this))
			.put("handle", getString("Resizable.handle"))
			.put("tabs", tabs)
			.put("languages", languages)
			.put("resizes", new JSONArray(RESIZES))
			.put("files", filesConfig)
			.put("icons", new JSONObject().put("file", SvgIcon.FILE_LINES.getMarkup()));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		CodeEditor.renderHeadItems(response);
		Tabs.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(CodeEditorPage.class, "CodeEditorPage.css")));
	}

	/**
	 * @return the language of the sample shown
	 */
	public CodeEditorLanguage getLanguage()
	{
		return language;
	}

	/**
	 * @param language
	 *            the language of the sample to show
	 */
	public void setLanguage(CodeEditorLanguage language)
	{
		this.language = language;
	}

	/**
	 * @return how the editor resizes: the name of an axis, or none
	 */
	public String getResize()
	{
		return resize;
	}

	/**
	 * @param resize
	 *            how the editor resizes: the name of an axis, or none
	 */
	public void setResize(String resize)
	{
		this.resize = resize;
	}

	/**
	 * @return whether the lines show their numbers
	 */
	public boolean isLineNumbers()
	{
		return lineNumbers;
	}

	/**
	 * @param lineNumbers
	 *            whether the lines show their numbers
	 */
	public void setLineNumbers(boolean lineNumbers)
	{
		this.lineNumbers = lineNumbers;
	}

	/**
	 * @return whether the user cannot change the text
	 */
	public boolean isReadOnly()
	{
		return readOnly;
	}

	/**
	 * @param readOnly
	 *            whether the user cannot change the text
	 */
	public void setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
	}

	/**
	 * @return whether the user can select the text
	 */
	public boolean isSelectable()
	{
		return selectable;
	}

	/**
	 * @param selectable
	 *            whether the user can select the text
	 */
	public void setSelectable(boolean selectable)
	{
		this.selectable = selectable;
	}

	/**
	 * @return whether long lines wrap
	 */
	public boolean isLineWrapping()
	{
		return lineWrapping;
	}

	/**
	 * @param lineWrapping
	 *            whether long lines wrap
	 */
	public void setLineWrapping(boolean lineWrapping)
	{
		this.lineWrapping = lineWrapping;
	}

	/**
	 * @return the files open, in the order of their tabs
	 */
	public List<CodeDocument> getFiles()
	{
		List<CodeDocument> open = new ArrayList<>();
		files.getTabs().forEach(tab -> open.add(((FileTab)tab).document));
		return open;
	}

	/**
	 * The notes in Markdown, the state of their editor, and how they show: where the divider of
	 * the split panel is, which pane is collapsed and the height given to its box.
	 */
	static final class Notes implements Serializable
	{
		private static final long serialVersionUID = 1L;

		final CodeDocument document;

		SplitState split;

		ResizableSize size = ResizableSize.NONE;

		Notes(CodeDocument document)
		{
			this.document = document;
		}
	}

	/**
	 * The tab of a file: its text in an editor, which tells the tabs of its changes not saved, and
	 * a button saving it, as Ctrl+S does.
	 */
	private class FileTab extends Tab
	{
		private static final long serialVersionUID = 1L;

		final CodeDocument document;

		FileTab(CodeDocument document)
		{
			super(Model.of(document.getName()));
			this.document = document;
			setIcon(SvgIcon.FILE_LINES);
			setClosable(true);
		}

		@Override
		protected Component newPanel(String id)
		{
			Fragment panel = new Fragment(id, "filePanel", CodeEditorPage.this);
			Form<Void> form = new Form<>("form");
			panel.add(form);
			CodeEditor fileEditor = new CodeEditor("editor", new PropertyModel<>(document, "text"));
			fileEditor.setLanguage(document.getLanguage())
				.setSavedModel(new PropertyModel<>(document, "saved"))
				.setLabel(Model.of(document.getName()));
			fileEditor.add(new CodeEditorEventBehavior(CodeEditorEvent.SAVE)
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					saveFile(target, fileEditor);
				}
			});
			form.add(fileEditor);
			form.add(new AjaxButton("save")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSubmit(AjaxRequestTarget target)
				{
					saveFile(target, fileEditor);
				}
			}.add(new ButtonBehavior(ButtonBehavior.Variant.PRIMARY, ButtonBehavior.Size.SMALL)));
			return panel;
		}

		private void saveFile(AjaxRequestTarget target, CodeEditor fileEditor)
		{
			document.save();
			fileEditor.markSaved(target);
			sayAboutFiles(target, "saved", document.getName());
		}
	}
}
