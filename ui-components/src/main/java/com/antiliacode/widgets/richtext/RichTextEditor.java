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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
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
import com.antiliacode.widgets.resizable.ResizeAxis;
import com.antiliacode.widgets.resource.AlreadyCompressedJavaScriptResourceReference;

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
 * The editor tells the {@link com.antiliacode.widgets.tabs.TabbedPanel tabs} and the
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
 * <p>
 * What it shares with {@link com.antiliacode.widgets.ckeditor.CKEditor}, made with CKEditor 5, is
 * {@link AbstractRichTextEditor}'s: the model, the cleaning, the state, the events and the text
 * saved, so that a page can switch between the two.
 *
 * @since 11.0.0
 */
public class RichTextEditor extends AbstractRichTextEditor<RichTextEditor>
{
	private static final long serialVersionUID = 1L;

	/**
	 * The script of the editor, {@code Wicket.RichTextEditor}, with Tiptap and ProseMirror. The
	 * build minifies it already: Wicket's own compressor, which in deployment mode strips the
	 * scripts named without {@code .min.}, would break its regular expressions.
	 */
	public static final ResourceReference JS = new AlreadyCompressedJavaScriptResourceReference(
		RichTextEditor.class, "antilia-rich-text-editor.js");

	/** The style sheet of the editor, and of the text a {@link RichTextView} shows. */
	public static final ResourceReference CSS = new CssResourceReference(RichTextEditor.class,
		"antilia-rich-text-editor.css");

	/**
	 * The grammars highlighting the code blocks, Lezer's parsers of the code editor's languages,
	 * which a page renders only where an editor {@link #setHighlighting(boolean) highlights}:
	 * {@link #JS} does not hold them. Minified by the build, as {@link #JS}.
	 */
	public static final ResourceReference HIGHLIGHT_JS =
		new AlreadyCompressedJavaScriptResourceReference(RichTextEditor.class,
			"antilia-rich-text-highlight.js", JS);

	/**
	 * The class of the text area until the script makes the editor of it. While scripts run,
	 * {@link #CSS} hides a text area of this class and keeps the room of the editor for it, so that
	 * a page arriving slowly shows no HTML; without scripts the text area is shown.
	 */
	public static final String LOADING_CLASS = "antilia-rich-text-editor-loading";

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

	private List<List<RichTextTool>> toolbar = RichTextTool.DEFAULT_TOOLBAR;

	private boolean bubbleMenu;

	private Integer limit;

	private List<RichTextExtension> extensions = List.of();

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
		super(id, model, stateModel);
	}

	@Override
	protected String getScriptObject()
	{
		return "Wicket.RichTextEditor";
	}

	@Override
	public String getLoadingClass()
	{
		return LOADING_CLASS;
	}

	@Override
	protected RichTextSanitizer getDefaultSanitizer()
	{
		return RichTextPolicy.of(getFeatures());
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
		return imageStore != null && !isReadOnly() && getFeatures().contains(RichTextFeature.IMAGE) &&
			isEnabledInHierarchy();
	}

	@Override
	protected void renderEditorHead(IHeaderResponse response)
	{
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
	}

	/**
	 * Puts the options of Tiptap's editor: the toolbar, the bubble menu, the limit, the
	 * extensions, the highlighting, the languages of the code blocks and the uploads of images.
	 */
	@Override
	protected void putEditorOptions(JSONObject options)
	{
		options.put("bubbleMenu", bubbleMenu);
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
	}

	@Override
	protected JSONObject getTexts()
	{
		return texts(this);
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
