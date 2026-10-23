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
package com.antiliacode.widgets.richtext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptPackageResource;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.Strings;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.codeeditor.CodeEditor;
import com.antiliacode.widgets.popover.Popover;
import com.antiliacode.widgets.resizable.ResizableBehavior;
import com.antiliacode.widgets.resizable.ResizableSize;
import com.antiliacode.widgets.resizable.ResizeAxis;

/**
 * A text area shown as a rich text editor, Tiptap, in the colors of the
 * {@link com.antiliacode.widgets.theme.Theme theme} it is in and the look of the page: a toolbar of
 * the components' own formats the text, with headings, bold, italic, underlined, struck and code
 * text, links, lists, task lists, quotes, code blocks, rules, tables, images and alignment, the
 * {@link RichTextFeature features}, all of them unless {@link #setFeatures(Set) told otherwise}.
 * The usual keys format too (Ctrl+B, Ctrl+I...), Markdown's marks do as the user types (# a
 * heading, - a list, **bold**), Ctrl+K edits the link and Ctrl+S tells the page to save. Attach it
 * to a {@code <textarea>}.
 * <p>
 * The model is the text, HTML by default, or Markdown or Tiptap's JSON
 * ({@link #setFormat(RichTextFormat)}). The HTML comes from the browser and is never trusted: the
 * editor {@link #setSanitizer(RichTextSanitizer) cleans} it before the model gets it, keeping what
 * its features write, with {@link RichTextPolicy} by default; {@link RichTextView} shows it in a
 * page, cleaned the same way. The HTML has no style attribute, which a Content Security Policy
 * without {@code 'unsafe-inline'} would block: text aligns by classes.
 * <p>
 * The text area stays in the page, hidden, and holds the text: a form posts it as always, and it
 * fires {@code input} as the user types and {@code change} once the editor loses the focus, as a
 * text area does, so the Ajax behaviors listening to them work as with one. The editor fires its
 * own events too, {@link RichTextEditorEvent}, which {@link RichTextEditorEventBehavior}s hear of:
 * the focus got and lost, the selection, Ctrl+S, the size changed. Until the script makes the
 * editor, the text area is hidden in the room the editor takes ({@link #LOADING_CLASS}), so that a
 * page arriving slowly does not show the HTML of the text. Without JavaScript the text area is
 * shown.
 * <p>
 * Code blocks are in a language, {@code <pre><code class="language-java">}, which the menu of the
 * tool code block chooses among {@link #setCodeLanguages(String...) the languages of the editor}.
 * {@link #setHighlighting(boolean) Highlighted}, they show in the colors of their language, those of
 * the {@link CodeEditor code editor}, with its grammars of Java, JavaScript, TypeScript, JSON and
 * Markdown: the editor then renders {@link #HIGHLIGHT_JS}, which holds them, and the style sheet of
 * the code editor. The HTML stays plain: the colors are the editor's alone.
 * <p>
 * With {@link #setImageStore(RichTextImageStore) a store of images}, the user uploads images: chosen
 * in the form of the image, dropped into the text or pasted. A behavior of the editor takes each,
 * no larger than {@link #setImageMaxSize(Bytes) allowed} and of {@link #setImageTypes(Set) a type
 * allowed} as its bytes say, has the store keep it and answers the address it is served at, which
 * the image takes and the cleaning checks as any other (see {@link RichTextImageUploadBehavior}).
 * <p>
 * The editor tells the {@link com.antiliacode.widgets.tabs.Tabs tabs} and the
 * {@link com.antiliacode.widgets.desktop.Desktop desktop} it is in whether its text has changes not
 * saved, against the text it was rendered with or {@link #setSavedModel(IModel) the text last
 * saved}, and its tab or its window shows a mark meanwhile; closing them anyway puts the saved text
 * back. {@link #markSaved(AjaxRequestTarget)} tells the browser the text was saved.
 * <p>
 * What is selected, how far the text is scrolled and the size the user gave the editor, by the bar
 * along its bottom ({@link #setResizable(ResizeAxis)}), are its {@link RichTextEditorState state},
 * kept in the state model: by default in the editor, and so in the page. The browser sends the
 * server the state, a moment after the user changed it, as the editor loses the focus and, in a
 * beacon, as the page goes ({@link #setSendToServer(boolean)}, on by default), with the text the
 * user changed, which the editor keeps as its input until the form is sent, as a text area keeps
 * what the user typed when the form did not validate. Rendered again, or shown again after the user
 * came back to the page with a state model of the session, the editor takes the state up again.
 * <p>
 * {@code Wicket.RichTextEditor} from {@link #JS} makes the editor in the browser, with Tiptap and
 * ProseMirror; its sizes are CSS custom properties set through the CSSOM, and it writes no style
 * element. The look comes from {@link #CSS}.
 *
 * @since 11.0.0
 */
public class RichTextEditor extends TextArea<String>
{
	private static final long serialVersionUID = 1L;

	/**
	 * The script of the editor, {@code Wicket.RichTextEditor}, with Tiptap and ProseMirror. The
	 * build minifies it already: Wicket's own compressor, which in deployment mode strips the
	 * scripts named without {@code .min.}, would break its regular expressions.
	 */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		RichTextEditor.class, "antilia-rich-text-editor.js")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public JavaScriptPackageResource getResource()
		{
			JavaScriptPackageResource resource = super.getResource();
			resource.setCompress(false);
			return resource;
		}
	};

	/** The style sheet of the editor, and of the text a {@link RichTextView} shows. */
	public static final ResourceReference CSS = new CssResourceReference(RichTextEditor.class,
		"antilia-rich-text-editor.css");

	/**
	 * The grammars highlighting the code blocks, Lezer's parsers of the code editor's languages,
	 * which a page renders only where an editor {@link #setHighlighting(boolean) highlights}:
	 * {@link #JS} does not hold them. Minified by the build, as {@link #JS}.
	 */
	public static final ResourceReference HIGHLIGHT_JS = new JavaScriptResourceReference(
		RichTextEditor.class, "antilia-rich-text-highlight.js")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public JavaScriptPackageResource getResource()
		{
			JavaScriptPackageResource resource = super.getResource();
			resource.setCompress(false);
			return resource;
		}

		@Override
		public List<HeaderItem> getDependencies()
		{
			List<HeaderItem> dependencies = new ArrayList<>(super.getDependencies());
			dependencies.add(JavaScriptHeaderItem.forReference(JS));
			return dependencies;
		}
	};

	/**
	 * The class of the text area until the script makes the editor of it. While scripts run,
	 * {@link #CSS} hides a text area of this class and keeps the room of the editor for it, so that
	 * a page arriving slowly shows no HTML; without scripts the text area is shown.
	 */
	public static final String LOADING_CLASS = "antilia-rich-text-editor-loading";

	/** The height the user resizes the editor from at least, in pixels, unless set otherwise. */
	public static final int DEFAULT_MIN_HEIGHT = 96;

	/** The levels of the headings, unless set otherwise. */
	public static final List<Integer> DEFAULT_HEADING_LEVELS = List.of(1, 2, 3, 4);

	private static final String STATE_PARAMETER = "state";

	/** What the name of a language of code blocks may be, as their class holds it. */
	private static final Pattern LANGUAGE = Pattern.compile("[A-Za-z0-9_+#.-]{1,40}");

	/** The texts of the editor in the browser, by the keys of their translations in the bundles. */
	private static final Map<String, String> TEXTS = new LinkedHashMap<>();

	static
	{
		String[][] texts = { { "toolbar", "Formatting" },
				{ "bubble", "Formatting of the selection" }, { "heading", "Text style" },
				{ "bold", "Bold" }, { "italic", "Italic" }, { "underline", "Underline" },
				{ "strike", "Strikethrough" }, { "code", "Code" }, { "link", "Link" },
				{ "bulletList", "Bulleted list" }, { "orderedList", "Numbered list" },
				{ "taskList", "Task list" }, { "blockquote", "Quote" },
				{ "codeBlock", "Code block" }, { "horizontalRule", "Horizontal line" },
				{ "table", "Table" }, { "image", "Image" }, { "alignLeft", "Align left" },
				{ "alignCenter", "Center" }, { "alignRight", "Align right" },
				{ "alignJustify", "Justify" }, { "clearFormatting", "Clear formatting" },
				{ "undo", "Undo" }, { "redo", "Redo" }, { "paragraph", "Paragraph" },
				{ "headingLevel", "Heading {0}" }, { "insertTable", "Insert a table" },
				{ "addRowBefore", "Add a row above" }, { "addRowAfter", "Add a row below" },
				{ "addColumnBefore", "Add a column before" },
				{ "addColumnAfter", "Add a column after" }, { "deleteRow", "Delete the row" },
				{ "deleteColumn", "Delete the column" }, { "toggleHeaderRow", "Header row" },
				{ "deleteTable", "Delete the table" }, { "linkUrl", "Address" },
				{ "linkNewTab", "Open in a new tab" }, { "removeLink", "Remove the link" },
				{ "imageUrl", "Address of the image" },
				{ "imageAlt", "Text in place of the image" }, { "apply", "Apply" },
				{ "close", "Close" },
				{ "invalidUrl",
						"Give an address starting with http://, https:// or mailto:, or a relative one." },
				{ "characters", "{0} characters" }, { "character", "{0} character" },
				{ "charactersOfLimit", "{0} of {1} characters" }, { "words", "{0} words" },
				{ "word", "{0} word" },
				{ "resize", "Drag, or use the arrow keys, to resize the editor" },
				{ "plainText", "Plain text" }, { "uploadImage", "Upload an image…" },
				{ "uploading", "Uploading {0}…" }, { "theImage", "The image" },
				{ "uploadFailed", "{0} could not be uploaded." },
				{ "uploadTooLarge", "{0} is larger than {1}." },
				{ "uploadType", "{0} is not an image of a type the editor takes: {1}." } };
		for (String[] text : texts)
		{
			TEXTS.put(text[0], text[1]);
		}
	}

	private final AbstractDefaultAjaxBehavior stateBehavior;

	private IModel<RichTextEditorState> stateModel;

	private IModel<String> savedModel;

	private IModel<String> placeholder;

	private RichTextFormat format = RichTextFormat.HTML;

	private Set<RichTextFeature> features;

	private List<Integer> headingLevels = DEFAULT_HEADING_LEVELS;

	private List<List<RichTextTool>> toolbar = RichTextTool.DEFAULT_TOOLBAR;

	private boolean bubbleMenu;

	private boolean readOnly;

	private Integer limit;

	private boolean count;

	private ResizeAxis resizable = ResizeAxis.VERTICAL;

	private Integer minHeight;

	private Integer maxHeight;

	private Integer minWidth;

	private Integer maxWidth;

	private boolean sendToServer = true;

	private List<RichTextExtension> extensions = List.of();

	private RichTextSanitizer sanitizer;

	private boolean highlighting;

	private List<String> codeLanguages;

	private RichTextImageStore imageStore;

	/** The behavior taking the images, made with the first store. */
	private RichTextImageUploadBehavior imageUpload;

	private Bytes imageMaxSize = RichTextImageUploadBehavior.DEFAULT_MAX_SIZE;

	private Set<RichTextImageType> imageTypes = EnumSet.allOf(RichTextImageType.class);

	/**
	 * An editor whose model is inherited, for example from a {@code CompoundPropertyModel}, keeping
	 * its state in the page.
	 *
	 * @param id
	 *            component id
	 */
	public RichTextEditor(String id)
	{
		this(id, null, null);
	}

	/**
	 * An editor keeping its state in the page.
	 *
	 * @param id
	 *            component id
	 * @param model
	 *            the text
	 */
	public RichTextEditor(String id, IModel<String> model)
	{
		this(id, model, null);
	}

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the text; inherited when {@code null}
	 * @param stateModel
	 *            where the editor keeps the selection, the scrolling and the size; in the page when
	 *            {@code null}
	 */
	public RichTextEditor(String id, IModel<String> model, IModel<RichTextEditorState> stateModel)
	{
		super(id, model);
		this.stateModel = stateModel == null ? Model.of((RichTextEditorState)null) : stateModel;
		setOutputMarkupId(true);
		stateBehavior = new AbstractDefaultAjaxBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void respond(AjaxRequestTarget target)
			{
				onStateSent(target, getRequest().getRequestParameters());
			}
		};
		add(stateBehavior);
	}

	/**
	 * Takes what the browser sent: the state, which the state model gets, and the text when the
	 * user changed it, which the editor keeps as its input.
	 */
	private void onStateSent(AjaxRequestTarget target, IRequestParameters parameters)
	{
		if (!sendToServer)
		{
			return;
		}
		RichTextEditorState state = RichTextEditorState.parse(parameters.getParameterValue(
			STATE_PARAMETER).toOptionalString());
		if (state != null)
		{
			state = state.withSize(resizable == null ? ResizableSize.NONE :
				state.getSize().along(resizable));
			if (!state.equals(stateModel.getObject()))
			{
				stateModel.setObject(state);
			}
		}
		// read where the text area's input is: among what the form posts
		String[] input = getInputAsArray();
		if (input != null && input.length > 0 && input[0] != null && !readOnly)
		{
			inputChanged();
		}
		onStateChanged(target);
	}

	/**
	 * Cleans the HTML the browser sent, before it is validated and the model gets it.
	 */
	@Override
	public void convertInput()
	{
		super.convertInput();
		if (format == RichTextFormat.HTML && isValid())
		{
			String html = getConvertedInput();
			if (html != null)
			{
				String clean = getSanitizer().sanitize(html);
				setConvertedInput(Strings.isEmpty(clean) ? null : clean);
			}
		}
	}

	/**
	 * Called when the browser sent the state, and maybe the text, after the state model took it.
	 * Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onStateChanged(AjaxRequestTarget target)
	{
	}

	/**
	 * @return the model keeping the selection, the scrolling and the size
	 */
	public IModel<RichTextEditorState> getStateModel()
	{
		return stateModel;
	}

	/**
	 * @param stateModel
	 *            where the editor keeps the selection, the scrolling and the size; in the editor
	 *            when {@code null}
	 * @return {@code this}
	 */
	public RichTextEditor setStateModel(IModel<RichTextEditorState> stateModel)
	{
		this.stateModel = stateModel == null ? Model.of((RichTextEditorState)null) : stateModel;
		return this;
	}

	/**
	 * @return the selection, the scrolling and the size, as the state model has them;
	 *         {@link RichTextEditorState#NONE} while it is empty
	 */
	public RichTextEditorState getState()
	{
		RichTextEditorState state = stateModel.getObject();
		return state == null ? RichTextEditorState.NONE : state;
	}

	/**
	 * @param state
	 *            the state the state model gets, which the editor shows when rendered again
	 * @return {@code this}
	 */
	public RichTextEditor setState(RichTextEditorState state)
	{
		stateModel.setObject(state);
		return this;
	}

	/**
	 * @return the model of the text last saved, {@code null} for the text the editor is rendered
	 *         with
	 */
	public IModel<String> getSavedModel()
	{
		return savedModel;
	}

	/**
	 * @param savedModel
	 *            the text last saved, against which the editor tells whether the text has changes;
	 *            {@code null} for the text the editor is rendered with, as by default
	 * @return {@code this}
	 */
	public RichTextEditor setSavedModel(IModel<String> savedModel)
	{
		this.savedModel = savedModel;
		return this;
	}

	/**
	 * Tells the browser the text of the model was saved: the editor no longer has changes, unless
	 * the user changed the text since. The model of the text last saved, if any, takes it.
	 *
	 * @param target
	 *            the Ajax request target
	 * @return {@code this}
	 */
	public RichTextEditor markSaved(AjaxRequestTarget target)
	{
		String text = getModelObject();
		if (savedModel != null)
		{
			savedModel.setObject(text);
		}
		target.appendJavaScript(script("markSaved(" + JSONObject.quote(text == null ? "" : text) +
			")"));
		return this;
	}

	/**
	 * Puts the focus in the editor, where the cursor is.
	 *
	 * @param target
	 *            the Ajax request target
	 * @return {@code this}
	 */
	public RichTextEditor focus(AjaxRequestTarget target)
	{
		target.appendJavaScript(script("focus()"));
		return this;
	}

	/**
	 * @return a script calling the editor in the browser, when it is there
	 */
	private String script(String call)
	{
		return "(function(){var e=Wicket.RichTextEditor&&Wicket.RichTextEditor.of(" +
			JSONObject.quote(getMarkupId()) + ");if(e){e." + call + ";}})();";
	}

	/**
	 * @return how the editor keeps the text
	 */
	public RichTextFormat getFormat()
	{
		return format;
	}

	/**
	 * @param format
	 *            how the editor keeps the text: {@link RichTextFormat#HTML} by default
	 * @return {@code this}
	 */
	public RichTextEditor setFormat(RichTextFormat format)
	{
		this.format = Args.notNull(format, "format");
		return this;
	}

	/**
	 * @return what the text may hold: those set, or the {@link RichTextFeature#defaults(RichTextFormat)
	 *         defaults of the format}
	 */
	public Set<RichTextFeature> getFeatures()
	{
		return features == null ? RichTextFeature.defaults(format) :
			Collections.unmodifiableSet(features);
	}

	/**
	 * @param features
	 *            what the text may hold; {@code null} for the defaults of the format, all the
	 *            features for HTML
	 * @return {@code this}
	 */
	public RichTextEditor setFeatures(Set<RichTextFeature> features)
	{
		this.features = features == null ? null : features.isEmpty() ?
			EnumSet.noneOf(RichTextFeature.class) : EnumSet.copyOf(features);
		return this;
	}

	/**
	 * @return the levels of the headings
	 */
	public List<Integer> getHeadingLevels()
	{
		return headingLevels;
	}

	/**
	 * @param levels
	 *            the levels of the headings, from 1 to 6; 1 to 4 by default
	 * @return {@code this}
	 */
	public RichTextEditor setHeadingLevels(int... levels)
	{
		List<Integer> list = new ArrayList<>();
		Arrays.stream(levels).filter(level -> level >= 1 && level <= 6).distinct().sorted()
			.forEach(list::add);
		this.headingLevels = List.copyOf(list);
		return this;
	}

	/**
	 * @return the tools of the toolbar, in groups; empty for no toolbar
	 */
	public List<List<RichTextTool>> getToolbar()
	{
		return toolbar;
	}

	/**
	 * @param toolbar
	 *            the tools of the toolbar, in groups; {@link RichTextTool#DEFAULT_TOOLBAR} by
	 *            default, {@code null} or empty for no toolbar. The tools of features left out do
	 *            not show.
	 * @return {@code this}
	 */
	public RichTextEditor setToolbar(List<List<RichTextTool>> toolbar)
	{
		List<List<RichTextTool>> groups = new ArrayList<>();
		if (toolbar != null)
		{
			toolbar.stream().filter(group -> group != null && !group.isEmpty())
				.forEach(group -> groups.add(List.copyOf(group)));
		}
		this.toolbar = List.copyOf(groups);
		return this;
	}

	/**
	 * @return whether a menu of the inline formats shows over the text selected
	 */
	public boolean isBubbleMenu()
	{
		return bubbleMenu;
	}

	/**
	 * @param bubbleMenu
	 *            whether a menu of the inline formats (bold, italic, underline, strike, code and
	 *            link) shows over the text selected, {@code false} by default
	 * @return {@code this}
	 */
	public RichTextEditor setBubbleMenu(boolean bubbleMenu)
	{
		this.bubbleMenu = bubbleMenu;
		return this;
	}

	/**
	 * @return whether the user cannot change the text, {@code false} unless set otherwise
	 */
	public boolean isReadOnly()
	{
		return readOnly;
	}

	/**
	 * @param readOnly
	 *            whether the user cannot change the text, but can read, select and copy it; the
	 *            toolbar does not show then
	 * @return {@code this}
	 */
	public RichTextEditor setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
		return this;
	}

	/**
	 * @param placeholder
	 *            the text shown while the editor is empty, {@code null} for none
	 * @return {@code this}
	 */
	public RichTextEditor setPlaceholder(IModel<String> placeholder)
	{
		this.placeholder = placeholder;
		return this;
	}

	/**
	 * @return the most characters the user can give the text, {@code null} for no limit
	 */
	public Integer getLimit()
	{
		return limit;
	}

	/**
	 * @param limit
	 *            the most characters the user can give the text, which the editor counts below
	 *            it; {@code null} for no limit, as by default. The browser keeps to it: a
	 *            validator of the page's own is the server's limit.
	 * @return {@code this}
	 */
	public RichTextEditor setLimit(Integer limit)
	{
		this.limit = limit == null ? null : Math.max(limit, 0);
		return this;
	}

	/**
	 * @return whether the editor counts the characters and the words below the text
	 */
	public boolean isCount()
	{
		return count;
	}

	/**
	 * @param count
	 *            whether the editor counts the characters and the words below the text,
	 *            {@code false} by default; it does with a {@link #setLimit(Integer) limit}
	 * @return {@code this}
	 */
	public RichTextEditor setCount(boolean count)
	{
		this.count = count;
		return this;
	}

	/**
	 * @return the dimensions the user resizes the editor in, {@code null} for none
	 */
	public ResizeAxis getResizable()
	{
		return resizable;
	}

	/**
	 * @param resizable
	 *            the dimensions the user resizes the editor in: {@link ResizeAxis#VERTICAL} by
	 *            default, by the bar along its bottom; {@code null} for none
	 * @return {@code this}
	 */
	public RichTextEditor setResizable(ResizeAxis resizable)
	{
		this.resizable = resizable;
		return this;
	}

	/**
	 * @param minHeight
	 *            the smallest height the user gives the editor, in pixels; {@code null} for
	 *            {@value #DEFAULT_MIN_HEIGHT}
	 * @return {@code this}
	 */
	public RichTextEditor setMinHeight(Integer minHeight)
	{
		this.minHeight = minHeight;
		return this;
	}

	/**
	 * @param maxHeight
	 *            the largest height the user gives the editor, in pixels; {@code null} for none
	 * @return {@code this}
	 */
	public RichTextEditor setMaxHeight(Integer maxHeight)
	{
		this.maxHeight = maxHeight;
		return this;
	}

	/**
	 * @param minWidth
	 *            the smallest width the user gives the editor, in pixels; {@code null} for 200
	 * @return {@code this}
	 */
	public RichTextEditor setMinWidth(Integer minWidth)
	{
		this.minWidth = minWidth;
		return this;
	}

	/**
	 * @param maxWidth
	 *            the largest width the user gives the editor, in pixels; {@code null} for none
	 * @return {@code this}
	 */
	public RichTextEditor setMaxWidth(Integer maxWidth)
	{
		this.maxWidth = maxWidth;
		return this;
	}

	/**
	 * @return whether the browser sends the server the state and the text the user changed,
	 *         {@code true} unless set otherwise
	 */
	public boolean isSendToServer()
	{
		return sendToServer;
	}

	/**
	 * @param sendToServer
	 *            whether the browser sends the server the selection, the scrolling, the size and
	 *            the text the user changed, which the state model and the input of the editor
	 *            take
	 * @return {@code this}
	 */
	public RichTextEditor setSendToServer(boolean sendToServer)
	{
		this.sendToServer = sendToServer;
		return this;
	}

	/**
	 * @return the extensions of the page's own
	 */
	public List<RichTextExtension> getExtensions()
	{
		return extensions;
	}

	/**
	 * @param extensions
	 *            extensions of Tiptap of the page's own, which their scripts register in the
	 *            browser; none by default
	 * @return {@code this}
	 */
	public RichTextEditor setExtensions(RichTextExtension... extensions)
	{
		this.extensions = List.of(extensions);
		return this;
	}

	/**
	 * @return whether the code blocks show in the colors of their language
	 */
	public boolean isHighlighting()
	{
		return highlighting;
	}

	/**
	 * @param highlighting
	 *            whether the code blocks show in the colors of their language, those of the code
	 *            editor, {@code false} by default: the editor then renders {@link #HIGHLIGHT_JS}
	 *            and {@link CodeEditor#CSS}, and the menu of the tool code block offers the
	 *            languages of the code editor, unless {@link #setCodeLanguages(String...) set
	 *            otherwise}
	 * @return {@code this}
	 */
	public RichTextEditor setHighlighting(boolean highlighting)
	{
		this.highlighting = highlighting;
		return this;
	}

	/**
	 * @return the languages of the menu of the tool code block, {@code null} for those of the
	 *         highlighting, or none without it
	 */
	public List<String> getCodeLanguages()
	{
		return codeLanguages;
	}

	/**
	 * @param languages
	 *            the languages of code blocks the menu of the tool code block offers, by their
	 *            names: {@code java}, {@code javascript}, {@code typescript}, {@code json},
	 *            {@code markdown} or any other a class may hold; none for no menu, the tool then
	 *            makes a code block without a language; {@code null} for those of the code editor
	 *            while {@link #setHighlighting(boolean) highlighting}, and none otherwise, as by
	 *            default
	 * @return {@code this}
	 */
	public RichTextEditor setCodeLanguages(String... languages)
	{
		if (languages == null)
		{
			this.codeLanguages = null;
		}
		else
		{
			this.codeLanguages = Arrays.stream(languages).filter(language -> language != null &&
				LANGUAGE.matcher(language.trim()).matches()).map(String::trim).distinct().toList();
		}
		return this;
	}

	/**
	 * @return what keeps the images the user uploads, {@code null} for none
	 */
	public RichTextImageStore getImageStore()
	{
		return imageStore;
	}

	/**
	 * @param imageStore
	 *            what keeps the images the user uploads and tells their addresses; {@code null}
	 *            for no uploads, as by default: an image is an address the user gives
	 * @return {@code this}
	 */
	public RichTextEditor setImageStore(RichTextImageStore imageStore)
	{
		this.imageStore = imageStore;
		if (imageStore != null && imageUpload == null)
		{
			imageUpload = new RichTextImageUploadBehavior(this::storeImage)
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean isUploadAllowed()
				{
					return isImageUploading();
				}
			};
			imageUpload.setMaxSize(imageMaxSize).setTypes(imageTypes);
			add(imageUpload);
		}
		return this;
	}

	private String storeImage(FileUpload image, RichTextImageType type) throws IOException
	{
		if (imageStore == null)
		{
			throw new IOException("The editor keeps no images");
		}
		return imageStore.store(image, type);
	}

	/**
	 * @return the largest image the user uploads
	 */
	public Bytes getImageMaxSize()
	{
		return imageMaxSize;
	}

	/**
	 * @param imageMaxSize
	 *            the largest image the user uploads,
	 *            {@link RichTextImageUploadBehavior#DEFAULT_MAX_SIZE} by default
	 * @return {@code this}
	 */
	public RichTextEditor setImageMaxSize(Bytes imageMaxSize)
	{
		this.imageMaxSize = Args.notNull(imageMaxSize, "imageMaxSize");
		if (imageUpload != null)
		{
			imageUpload.setMaxSize(imageMaxSize);
		}
		return this;
	}

	/**
	 * @return the types of the images the user uploads
	 */
	public Set<RichTextImageType> getImageTypes()
	{
		return Collections.unmodifiableSet(imageTypes);
	}

	/**
	 * @param imageTypes
	 *            the types of the images the user uploads, as their bytes say; all of them by
	 *            default
	 * @return {@code this}
	 */
	public RichTextEditor setImageTypes(Set<RichTextImageType> imageTypes)
	{
		Args.notEmpty(imageTypes, "imageTypes");
		this.imageTypes = EnumSet.copyOf(imageTypes);
		if (imageUpload != null)
		{
			imageUpload.setTypes(imageTypes);
		}
		return this;
	}

	/**
	 * @return whether the user uploads images now: the editor has a store, and is enabled, not read
	 *         only, with the feature image
	 */
	public boolean isImageUploading()
	{
		return imageStore != null && !readOnly && getFeatures().contains(RichTextFeature.IMAGE) &&
			isEnabledInHierarchy();
	}

	/**
	 * @return what cleans the HTML: the one set, or the {@link RichTextPolicy} of the features
	 */
	public RichTextSanitizer getSanitizer()
	{
		return sanitizer != null ? sanitizer : RichTextPolicy.of(getFeatures());
	}

	/**
	 * @param sanitizer
	 *            what cleans the HTML the browser sends, before the model gets it; {@code null}
	 *            for the {@link RichTextPolicy} of the features, as by default. An extension
	 *            writing elements of its own needs one letting them through.
	 * @return {@code this}
	 */
	public RichTextEditor setSanitizer(RichTextSanitizer sanitizer)
	{
		this.sanitizer = sanitizer;
		return this;
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		tag.append("class", LOADING_CLASS, " ");
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderHeadItems(response);
		if (highlighting)
		{
			renderHighlightItems(response);
		}
		for (RichTextExtension extension : extensions)
		{
			if (extension.getScript() != null)
			{
				response.render(JavaScriptHeaderItem.forReference(extension.getScript()));
			}
		}
		response.render(OnDomReadyHeaderItem.forScript("Wicket.RichTextEditor.attach(" +
			JSONObject.quote(getMarkupId()) + ", " + getOptions() + ");"));
	}

	/**
	 * @return the options of {@code Wicket.RichTextEditor.attach}
	 */
	JSONObject getOptions()
	{
		RichTextEditorState state = getState();
		JSONArray featureNames = new JSONArray();
		getFeatures().forEach(feature -> featureNames.put(feature.getName()));
		JSONObject options = new JSONObject()
			.put("format", format.getName())
			.put("features", featureNames)
			.put("headingLevels", new JSONArray(headingLevels))
			.put("bubbleMenu", bubbleMenu)
			.put("readOnly", readOnly)
			.put("count", count)
			.put("state", state.withSize(resizable == null ? ResizableSize.NONE :
				state.getSize().along(resizable)).toJson());
		if (toolbar.isEmpty())
		{
			options.put("toolbar", false);
		}
		else
		{
			JSONArray groups = new JSONArray();
			toolbar.forEach(group -> {
				JSONArray tools = new JSONArray();
				group.forEach(tool -> tools.put(tool.getName()));
				groups.put(tools);
			});
			options.put("toolbar", groups);
		}
		if (limit != null)
		{
			options.put("limit", limit.intValue());
		}
		if (savedModel != null)
		{
			String saved = savedModel.getObject();
			options.put("saved", saved == null ? "" : saved);
		}
		String text = placeholder == null ? null : placeholder.getObject();
		if (!Strings.isEmpty(text))
		{
			options.put("placeholder", text);
		}
		IModel<String> label = getLabel();
		String name = label == null ? null : label.getObject();
		if (!Strings.isEmpty(name))
		{
			options.put("label", name);
		}
		if (resizable == null)
		{
			options.put("resizable", false);
		}
		else
		{
			options.put("resizable", resizable.cssName());
			options.put("minHeight", minHeight == null ? DEFAULT_MIN_HEIGHT : minHeight);
			putPixels(options, "maxHeight", maxHeight);
			putPixels(options, "minWidth", minWidth);
			putPixels(options, "maxWidth", maxWidth);
		}
		if (!extensions.isEmpty())
		{
			JSONArray names = new JSONArray();
			extensions.forEach(extension -> names.put(extension.getName()));
			options.put("extensions", names);
		}
		if (highlighting)
		{
			options.put("highlight", true);
		}
		if (codeLanguages != null)
		{
			options.put("codeLanguages", codeLanguages.isEmpty() ? (Object)false :
				new JSONArray(codeLanguages));
		}
		if (isImageUploading())
		{
			options.put("uploadUrl", imageUpload.getUploadUrl());
			options.put("uploadTypes", new JSONArray(imageUpload.getMimeTypes()));
			options.put("uploadMaxSize", imageMaxSize.bytes());
		}
		options.put("texts", texts(this));
		if (sendToServer)
		{
			options.put("stateUrl", stateBehavior.getCallbackUrl().toString());
			options.put("inputName", getInputName());
		}
		return options;
	}

	/**
	 * What the editor says, in the language of a component: the names of the tools, the items of
	 * its menus, its forms, the count and the bar resizing it, those translated. The texts of this
	 * editor, and those a page hands to the React and Svelte editors.
	 *
	 * @param component
	 *            the component whose resources translate the texts
	 * @return the texts, by their names in the browser
	 */
	public static JSONObject texts(Component component)
	{
		JSONObject texts = new JSONObject();
		TEXTS.keySet().forEach(name -> texts.put(name, text(component, name)));
		return texts;
	}

	/**
	 * @param component
	 *            the component whose resources translate the text, {@code null} for English
	 * @param name
	 *            the name of a text of the editor, {@code uploadFailed} for example
	 * @return the text, in the language of the component
	 */
	static String text(Component component, String name)
	{
		String english = TEXTS.getOrDefault(name, name);
		if (component == null)
		{
			return english;
		}
		String translation = component.getString("RichTextEditor." + name, null, english);
		return Strings.isEmpty(translation) ? english : translation;
	}

	private static void putPixels(JSONObject options, String name, Integer pixels)
	{
		if (pixels != null)
		{
			options.put(name, pixels.intValue());
		}
	}

	@Override
	protected void onDetach()
	{
		super.onDetach();
		stateModel.detach();
		if (savedModel != null)
		{
			savedModel.detach();
		}
		if (placeholder != null)
		{
			placeholder.detach();
		}
	}

	/**
	 * Renders what the editor needs in the browser: {@link #JS} and {@link #CSS}, the style sheet
	 * of the bar resizing it, {@link ResizableBehavior#CSS}, and those of the buttons and the
	 * popovers of its forms.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(CssHeaderItem.forReference(ResizableBehavior.CSS));
		response.render(CssHeaderItem.forReference(ButtonBehavior.CSS));
		response.render(CssHeaderItem.forReference(Popover.CSS));
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}

	/**
	 * Renders what highlighting the code blocks needs in the browser: {@link #HIGHLIGHT_JS}, after
	 * {@link #JS}, and the colors of the code editor, {@link CodeEditor#CSS}. A page with editors
	 * of React or Svelte highlighting renders it too.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHighlightItems(IHeaderResponse response)
	{
		response.render(CssHeaderItem.forReference(CodeEditor.CSS));
		response.render(JavaScriptHeaderItem.forReference(HIGHLIGHT_JS));
	}
}
