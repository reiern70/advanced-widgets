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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractChoice.LabelPosition;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.lang.Bytes;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.codeeditor.CodeEditor;
import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.dropdown.MultiDropdown;
import com.antiliacode.widgets.richtext.IRichTextTool;
import com.antiliacode.widgets.richtext.RichTextBackgroundColor;
import com.antiliacode.widgets.richtext.RichTextColor;
import com.antiliacode.widgets.richtext.RichTextEditor;
import com.antiliacode.widgets.richtext.RichTextEditorEvent;
import com.antiliacode.widgets.richtext.RichTextEditorEventBehavior;
import com.antiliacode.widgets.richtext.RichTextEditorState;
import com.antiliacode.widgets.richtext.RichTextFormat;
import com.antiliacode.widgets.richtext.RichTextImageUploadBehavior;
import com.antiliacode.widgets.richtext.RichTextLibrary;
import com.antiliacode.widgets.richtext.RichTextPolicy;
import com.antiliacode.widgets.richtext.RichTextTool;
import com.antiliacode.widgets.richtext.RichTextView;
import com.antiliacode.widgets.resizable.ResizableSize;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link RichTextEditor}: an article in HTML, which the server keeps in the session with the state
 * of its editor, where the cursor is, what is selected, how far it is scrolled and its size, and
 * cleans before it keeps it; below it, the article as a page shows it, a {@link RichTextView}, and
 * the HTML the server keeps. Check boxes and a drop-down switch the toolbar, the bubble menu,
 * reading only, the count of the words and the colors of the code blocks, rendering the editor
 * again via Ajax; the toolbar may be chosen tool by tool, and the Java making it shows below. Two
 * {@link MultiDropdown}s choose the colors of the text and those of the background, palettes
 * apart, of the editor and of the view; none chosen, the editor has no tool for them. A check box
 * adds a feature of the page's own, {@link KbdFeature}: keys of the keyboard, with their tool.
 * Ctrl+S, and the button, save the article. The code blocks are in a language, which the menu of
 * their tool chooses; the images upload, kept by {@link RichTextImages}. Below the title, the
 * library the editor is based on, {@link RichTextLibrary}.
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

	/** The toolbars of the drop-down, those of the React and Svelte apps too. */
	static final List<String> TOOLBARS = List.of("full", "compact", "none");

	/** The toolbar chosen tool by tool, with the check boxes of the tools. */
	static final String CHOSEN = "chosen";

	/** The toolbars of the drop-down of the Wicket editor. */
	static final List<String> CHOICES = List.of("full", "compact", "none", CHOSEN);

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

	/** The tools of the toolbar chosen tool by tool. */
	private List<RichTextTool> tools = tools(RichTextTool.DEFAULT_TOOLBAR);

	private boolean bubbleMenu = true;

	private boolean readOnly;

	private boolean count = true;

	private boolean highlight = true;

	/** Whether the editor has the feature of the page's own, {@link KbdFeature}. */
	private boolean keys;

	/** The colors of the text of the editor and of the view, all of them at first. */
	private List<RichTextColor> colors = new ArrayList<>(List.of(RichTextColor.values()));

	/** The colors of the background of the editor and of the view, all of them at first. */
	private List<RichTextBackgroundColor> backgroundColors = new ArrayList<>(List.of(
		RichTextBackgroundColor.values()));

	/** Takes the images the editors of the React and Svelte apps upload. */
	private final RichTextImageUploadBehavior appUploads;

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
				// the feature of the page's own, and its tool after the inline formats
				List<List<? extends IRichTextTool>> groups = new ArrayList<>(toolbarOf(toolbar,
					tools));
				if (keys)
				{
					addFeature(KbdFeature.KBD);
					int bold = groups.indexOf(groups.stream().filter(group -> group.contains(
						RichTextTool.BOLD)).findFirst().orElse(null));
					groups.add(bold < 0 ? groups.size() : bold + 1, List.of(KbdFeature.TOOL));
				}
				else
				{
					removeFeature(KbdFeature.KBD);
				}
				setToolbar(groups);
				setBubbleMenu(bubbleMenu);
				setReadOnly(readOnly);
				setCount(count);
				setHighlighting(highlight);
				// none chosen, no tool
				setColors(RichTextEditorPage.this.colors);
				setBackgroundColors(RichTextEditorPage.this.backgroundColors);
			}

			@Override
			protected void onStateChanged(AjaxRequestTarget target)
			{
				target.add(known);
			}
		};
		editor.setSavedModel(LambdaModel.of(article, RichTextDocument::getSaved,
			RichTextDocument::setSaved));
		editor.setImageStore(RichTextImages.get()).setImageMaxSize(RichTextImages.MAX_SIZE);
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
		savedBox.add(new RichTextView("view", () -> article().getSaved())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				// the features and the palettes of the editor the HTML comes from
				setFeatures(editor.getFeatures());
				setColors(RichTextEditorPage.this.colors);
				setBackgroundColors(RichTextEditorPage.this.backgroundColors);
			}
		});
		savedBox.add(new Label("html", () -> article().getSaved()));
		articleCase.add(savedBox);
		saved = savedBox;

		Form<Void> controls = new Form<>("controls");
		articleCase.add(controls);
		WebMarkupContainer toolbarJava = new WebMarkupContainer("toolbarJava");
		toolbarJava.setOutputMarkupId(true);
		toolbarJava.add(new Label("java", () -> javaOf(toolbarOf(toolbar, tools))));
		articleCase.add(toolbarJava);
		WebMarkupContainer chooser = new WebMarkupContainer("chooser")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(CHOSEN.equals(toolbar));
			}
		};
		chooser.setOutputMarkupPlaceholderTag(true);
		controls.add(chooser);
		CheckBoxMultipleChoice<RichTextTool> toolChoice = new CheckBoxMultipleChoice<>("tools",
			new PropertyModel<>(this, "tools"), List.of(RichTextTool.values()),
			new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(RichTextTool tool)
				{
					// the name the editor gives the tool, in the language of the page
					return getString("RichTextEditor." + tool.getName(), null, tool.getName());
				}

				@Override
				public String getIdValue(RichTextTool tool, int index)
				{
					return tool.getName();
				}
			});
		toolChoice.setLabelPosition(LabelPosition.WRAP_AFTER)
			.setPrefix("<span class=\"rich-text-demo__tool\">").setSuffix("</span>");
		toolChoice.add(new AjaxFormChoiceComponentUpdatingBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(editorForm, known, toolbarJava);
			}
		});
		chooser.add(toolChoice);
		controls.add(new Dropdown<>("toolbar", new PropertyModel<>(this, "toolbar"), CHOICES,
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
			}).setRequired(true).add(rerender(editorForm, chooser, toolbarJava)));
		controls.add(paletteChoice("colors", List.of(RichTextColor.values()),
			RichTextColor::getName, editorForm, savedBox));
		controls.add(paletteChoice("backgroundColors", List.of(RichTextBackgroundColor.values()),
			RichTextBackgroundColor::getName, editorForm, savedBox));
		articleCase.add(new Label("extras", getString("extras").replace("{0}",
			sizeText(RichTextImages.MAX_SIZE))));
		for (String option : List.of("bubbleMenu", "readOnly", "count", "highlight", "keys"))
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
		notesEditor.setFormat(RichTextFormat.MARKDOWN).setBubbleMenu(true).setHighlighting(true)
			.setImageStore(RichTextImages.get()).setImageMaxSize(RichTextImages.MAX_SIZE)
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

		appUploads = new RichTextImageUploadBehavior(RichTextImages.get())
			.setMaxSize(RichTextImages.MAX_SIZE);
		add(appUploads);
	}

	/**
	 * @return a size as the editor says it: 2 MB
	 */
	private static String sizeText(Bytes size)
	{
		long megabytes = size.bytes() / (1024 * 1024);
		return megabytes * 1024 * 1024 == size.bytes() ? megabytes + " MB" : size.toString();
	}

	/**
	 * @return the tools of a toolbar of the drop-down
	 */
	static List<List<RichTextTool>> toolbarOf(String name)
	{
		return toolbarOf(name, tools(RichTextTool.DEFAULT_TOOLBAR));
	}

	/**
	 * @param name
	 *            a toolbar of the drop-down
	 * @param chosen
	 *            the tools chosen one by one, for the toolbar {@link #CHOSEN}
	 * @return the tools of the toolbar, in groups
	 */
	static List<List<RichTextTool>> toolbarOf(String name, Collection<RichTextTool> chosen)
	{
		return switch (name == null ? "full" : name)
		{
			case "compact" -> COMPACT;
			case "none" -> List.of();
			// in the groups of the default toolbar, those left empty left out
			case CHOSEN -> RichTextTool.DEFAULT_TOOLBAR.stream()
				.map(group -> group.stream().filter(chosen::contains).toList())
				.filter(group -> !group.isEmpty())
				.toList();
			default -> RichTextTool.DEFAULT_TOOLBAR;
		};
	}

	/**
	 * @return the tools of a toolbar, in a list the check boxes change
	 */
	private static List<RichTextTool> tools(List<List<RichTextTool>> toolbar)
	{
		return toolbar.stream().flatMap(List::stream).collect(Collectors.toCollection(
			ArrayList::new));
	}

	/**
	 * @return the Java giving an editor a toolbar
	 */
	static String javaOf(List<List<RichTextTool>> toolbar)
	{
		if (toolbar.equals(RichTextTool.DEFAULT_TOOLBAR))
		{
			return "editor.setToolbar(RichTextTool.DEFAULT_TOOLBAR);";
		}
		if (toolbar.isEmpty())
		{
			return "editor.setToolbar(List.of());";
		}
		return "import static com.antiliacode.widgets.richtext.RichTextTool.*;\n\n" +
			"editor.setToolbar(List.of(\n" + toolbar.stream()
				.map(group -> group.stream().map(RichTextTool::name).collect(Collectors.joining(
					", ", "\tList.of(", ")")))
				.collect(Collectors.joining(",\n")) + "));";
	}

	/**
	 * @return the drop-down choosing the colors of a palette, each named as the menus of the editor
	 *         name it, in the language of the page; none chosen, none in the palette
	 */
	private <C> MultiDropdown<C> paletteChoice(String id, List<C> colors,
		SerializableFunction<C, String> nameOf, Component... rerendered)
	{
		MultiDropdown<C> choice = new MultiDropdown<>(id, new PropertyModel<>(this, id), colors,
			new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(C color)
				{
					String name = nameOf.apply(color);
					String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);
					return getString("RichTextEditor.color" + capitalized, null, capitalized);
				}

				@Override
				public String getIdValue(C color, int index)
				{
					return nameOf.apply(color);
				}
			});
		choice.getDropdownBehavior().setPlaceholder(new ResourceModel("colors.none"));
		choice.add(rerender(rerendered));
		return choice;
	}

	private AjaxFormComponentUpdatingBehavior rerender(Component... components)
	{
		return new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(components);
				target.add(known);
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
	protected BasedOn getBasedOn()
	{
		RichTextLibrary library = RichTextLibrary.get();
		return new BasedOn(library.name(), library.version(), library.url(), "ProseMirror");
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
			"count", "highlight", "save", "unsaved", "allSaved", "appArticleTitle", "appHtml",
			"appMarkdown", "appKnown", "appSaved", "appNotClean", "appStart");
		texts.put("extras", getString("extras").replace("{0}", sizeText(RichTextImages.MAX_SIZE)));
		TOOLBARS.forEach(each -> texts.put("toolbar." + each, getString("toolbar." + each)));
		return new JSONObject()
			.put("article", RichTextSamples.ARTICLE)
			.put("notes", RichTextSamples.NOTES)
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("editor", RichTextEditor.texts(this))
			.put("preview", new JSONObject().put("label", getString("MarkdownPreview.label"))
				.put("placeholder", getString("MarkdownPreview.placeholder")))
			.put("toolbars", new JSONArray(TOOLBARS))
			.put("uploadUrl", appUploads.getUploadUrl())
			.put("uploadTypes", new JSONArray(appUploads.getMimeTypes()))
			.put("uploadMaxSize", appUploads.getMaxSize().bytes());
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		RichTextEditor.renderHeadItems(response);
		// the colors of the code blocks of the apps too, whose grammars their script holds
		response.render(CssHeaderItem.forReference(CodeEditor.CSS));
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(RichTextEditorPage.class, "RichTextEditorPage.css")));
	}

	public String getToolbar()
	{
		return toolbar;
	}

	/**
	 * @param toolbar
	 *            a toolbar of the drop-down; choosing tool by tool starts from the tools of the
	 *            toolbar shown
	 */
	public void setToolbar(String toolbar)
	{
		if (CHOSEN.equals(toolbar) && !CHOSEN.equals(this.toolbar))
		{
			tools = tools(toolbarOf(this.toolbar));
		}
		this.toolbar = toolbar;
	}

	public List<RichTextTool> getTools()
	{
		return tools;
	}

	public void setTools(List<RichTextTool> tools)
	{
		this.tools = tools;
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

	public boolean isHighlight()
	{
		return highlight;
	}

	public void setHighlight(boolean highlight)
	{
		this.highlight = highlight;
	}

	public List<RichTextColor> getColors()
	{
		return colors;
	}

	public void setColors(List<RichTextColor> colors)
	{
		this.colors = colors == null ? new ArrayList<>() : new ArrayList<>(colors);
	}

	public List<RichTextBackgroundColor> getBackgroundColors()
	{
		return backgroundColors;
	}

	public void setBackgroundColors(List<RichTextBackgroundColor> backgroundColors)
	{
		this.backgroundColors = backgroundColors == null ? new ArrayList<>() :
			new ArrayList<>(backgroundColors);
	}

	public boolean isKeys()
	{
		return keys;
	}

	public void setKeys(boolean keys)
	{
		this.keys = keys;
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
