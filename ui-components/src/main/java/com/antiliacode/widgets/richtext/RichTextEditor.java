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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
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
import com.antiliacode.widgets.modal.Modal;
import com.antiliacode.widgets.popover.Popover;
import com.antiliacode.widgets.resizable.ResizableBehavior;
import com.antiliacode.widgets.resizable.ResizeAxis;
import com.antiliacode.widgets.resource.AlreadyCompressedJavaScriptResourceReference;

/**
 * A text area shown as a rich text editor, Tiptap ({@link RichTextLibrary}), in the colors of the
 * {@link com.antiliacode.widgets.theme.Theme theme} it is in and the look of the page: a toolbar of
 * the components' own formats the text, with headings, bold, italic, underlined, struck and code
 * text, superscript and subscript, the colors of the text and of its background, links, lists,
 * task lists, quotes, code blocks, rules, tables, images and alignment, the
 * {@link RichTextFeature features}, all of them unless {@link #setFeatures(Collection) told
 * otherwise}. The page chooses the tools of the toolbar, and their groups, with
 * {@link #setToolbar(List)}.
 * The usual keys format too (Ctrl+B, Ctrl+I..., Ctrl+Shift+. raises and Ctrl+Shift+, lowers),
 * Markdown's marks do as the user types (# a heading, - a list, **bold**), Ctrl+K edits the link
 * and Ctrl+S tells the page to save. Attach it to a {@code <textarea>}.
 * <p>
 * The model is the text, HTML by default, or Markdown or Tiptap's JSON
 * ({@link #setFormat(RichTextFormat)}). The HTML comes from the browser and is never trusted: the
 * editor {@link #setSanitizer(IRichTextSanitizer) cleans} it before the model gets it, keeping what
 * its features write, with {@link RichTextPolicy} by default; {@link RichTextView} shows it in a
 * page, cleaned the same way. The HTML has no style attribute, which a Content Security Policy
 * without {@code 'unsafe-inline'} would block: text aligns by classes, and takes colors by classes
 * too, from two palettes apart: the {@link IRichTextColor colors of the text},
 * {@link RichTextColor#PALETTE} or the page's own ({@link #setColors(Collection)}), and the
 * {@link IRichTextBackgroundColor colors of its background},
 * {@link RichTextBackgroundColor#PALETTE} or the page's own
 * ({@link #setBackgroundColors(Collection)}). The cleaning keeps the classes of those palettes
 * alone.
 * <p>
 * The text area stays in the page, hidden, and holds the text: a form posts it as always, and it
 * fires {@code input} as the user types and {@code change} once the editor loses the focus, as a
 * text area does, so the Ajax behaviors listening to them work as with one. The editor fires its
 * own events too, {@link RichTextEditorEvents}: the focus got and lost, the selection, Ctrl+S, the
 * size changed. It listens to the {@link #addEvent(com.antiliacode.widgets.event.IClientEvent)
 * events it is given}, in the browser or on the server, with the form or without, and to those
 * of its features, through two Ajax behaviors whatever their number. Until the script makes the
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
 * With {@link #setImageStore(IRichTextImageStore) a store of images}, the user uploads images:
 * dropped on the zone of the form of the image or chosen there, several at once, or, with
 * {@link #setImageUploadDialog(boolean) the dialog}, in a dialog listing the files as they upload;
 * dropped into the text or pasted. A behavior of the editor takes each, no larger than
 * {@link #setImageMaxSize(Bytes) allowed}, of {@link #setImageTypes(Collection) a type allowed} as
 * its bytes say, and no wider and no higher than {@link #setImageMaxDimensions(
 * RichTextImageDimensions) allowed} as its header says, has the store keep it and answers the
 * address it is served at, which the image takes and the cleaning checks as any other (see
 * {@link RichTextImageUploadBehavior}). The browser checks the same before sending a file, and
 * {@link #setImageMaxCount(Integer) how many} one drop or choice takes; it scales a picture too
 * large down instead, when {@link #setImageScaling(boolean) allowed} and the user ticks it.
 * <p>
 * The tool {@link RichTextTool#IMAGES} lists the images of the text, each taking it out. The
 * browser tells the server the images the user took out of the text, at once, via Ajax:
 * {@link #onImagesRemoved(AjaxRequestTarget, Set)} and a {@link RichTextImagesRemovedEvent} hear of
 * those the editor knows, those of the text it was shown with and those uploaded since, and
 * {@link #deleteImages(Set)}, by the store, deletes them, at once or once the text is saved
 * ({@link #markSaved(AjaxRequestTarget)}, {@link #deleteRemovedImages()}), as
 * {@link #setImageDeletion(RichTextImageDeletion) the mode} says.
 * <p>
 * The browser checks the spelling of the text unless {@link #setSpellcheck(Boolean) told
 * otherwise}, in {@link #setTextLanguage(Locale) its language}. With
 * {@link #setProofreader(IRichTextProofreader) a proofreader}, such as
 * {@link LanguageToolProofreader LanguageTool}, the server checks the spelling, the grammar and the
 * style of the text instead: the editor underlines what it finds, and offers the replacements in a
 * popover, by a click or Ctrl+. (see {@link RichTextProofreadBehavior}).
 * <p>
 * With {@link #addSuggester(IRichTextSuggester) suggesters}, the server offers items for what the
 * user types, in a list under the cursor: the users to mention after {@code @}, the words
 * completing the one before the cursor on Ctrl+Space (see {@link RichTextSuggester} and
 * {@link RichTextSuggestBehavior}). A mention keeps the id of its item
 * ({@link RichTextFeature#MENTION}, {@link RichTextMention}).
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

	/**
	 * The names of the texts of the editor in the browser: each the translation of
	 * {@code RichTextEditor.} followed by its name, in the bundles of {@code Initializer}, English
	 * among them.
	 */
	static final List<String> TEXT_NAMES = List.of(
		"toolbar", "bubble", "heading", "bold", "italic", "underline", "strike", "code",
		"superscript", "subscript", "textColor", "textBackground", "link", "bulletList",
		"orderedList", "taskList", "blockquote", "codeBlock", "horizontalRule", "table", "image",
		"alignLeft", "alignCenter", "alignRight", "alignJustify", "clearFormatting", "undo", "redo",
		"paragraph", "headingLevel", "insertTable", "addRowBefore", "addRowAfter",
		"addColumnBefore", "addColumnAfter", "deleteRow", "deleteColumn", "toggleHeaderRow",
		"deleteTable", "linkUrl", "linkNewTab", "removeLink", "imageUrl", "imageAlt", "apply",
		"close", "invalidUrl", "characters", "character", "charactersOfLimit", "words", "word",
		"resize", "plainText", "defaultColor", "noBackground", "recentColors", "colorGray",
		"colorBrown", "colorRed", "colorOrange", "colorYellow", "colorLime", "colorGreen",
		"colorTeal", "colorCyan", "colorBlue", "colorIndigo", "colorPurple", "colorPink",
		"uploadImage", "uploading", "theImage", "uploadFailed", "uploadTooLarge", "uploadType",
		"images", "noImages", "deleteImage", "dropImages", "chooseImages", "uploadImagesTitle",
		"uploadDone", "removeFile", "insertImages", "cancel", "upload", "uploadTooMany",
		"uploadTooBig", "scaleTitle", "scaleText", "scaleDown",
		"proofreadSpelling", "proofreadGrammar", "proofreadStyle", "proofreadReplacements",
		"proofreadNoSuggestions", "proofreadIgnore", "suggestions", "suggestLoading", "suggestNone",
		"pasteCleaned", "pasteScripts", "pasteEmbedded", "pasteForms", "pasteImages", "pasteTables",
		"pasteLinks", "pasteFormatting");

	/** The features set, {@code null} for those of the format. */
	private Set<IRichTextFeature> features;

	private List<List<IRichTextTool>> toolbar = groups(RichTextTool.DEFAULT_TOOLBAR);

	private boolean bubbleMenu;

	private Integer limit;

	private List<RichTextExtension> extensions = List.of();

	private boolean highlighting;

	private List<String> codeLanguages;

	private IRichTextImageStore imageStore;

	/** The behavior taking the images, made with the first store. */
	private RichTextImageUploadBehavior imageUpload;

	private Bytes imageMaxSize = RichTextImageUploadBehavior.DEFAULT_MAX_SIZE;

	private Set<IRichTextImageType> imageTypes = new LinkedHashSet<>(RichTextImageType.ALL);

	private RichTextImageDimensions imageMaxDimensions;

	private Integer imageMaxCount;

	private boolean imageUploadDialog;

	private boolean imageScaling;

	private RichTextImageDeletion imageDeletion = RichTextImageDeletion.ON_SAVE;

	/** The behavior hearing of the images the user took out of the text, made with the first store. */
	private AbstractDefaultAjaxBehavior imagesRemoval;

	/** The images of the text of the model the editor was last shown with. */
	private Set<String> imagesShown = new LinkedHashSet<>();

	/** The hash of that text, to tell another text from it. */
	private int shownHash;

	/** The images the user took out of the text since it was last saved, to delete once it is. */
	private final Set<String> imagesRemoved = new LinkedHashSet<>();

	private IRichTextProofreader proofreader;

	/** The behavior checking the texts, made with the first proofreader. */
	private RichTextProofreadBehavior proofreading;

	/** The behavior answering the suggesters, made with the first of them. */
	private RichTextSuggestBehavior suggesting;

	private Set<IRichTextColor> colors = RichTextColor.PALETTE;

	private Set<IRichTextBackgroundColor> backgroundColors = RichTextBackgroundColor.PALETTE;

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
	protected IRichTextSanitizer getDefaultSanitizer()
	{
		return RichTextPolicy.of(getFeatures(), colors, backgroundColors)
			.withImagesFromSiteOnly(isImagesFromSiteOnly());
	}

	/**
	 * @return what the text may hold: the features set, or those of
	 *         {@link RichTextFeature#defaults(RichTextFormat) the format}, all the built-in ones for
	 *         HTML
	 */
	@Override
	public Set<IRichTextFeature> getFeatures()
	{
		return features == null ? RichTextFeature.defaults(getFormat()) : features;
	}

	/**
	 * @param features
	 *            what the text may hold: built-in {@link RichTextFeature}s and features of the
	 *            page's own, each by a name of its own; {@code null} for those of the format
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for features {@link IRichTextFeature#checked(Collection)} does not take: one
	 *             missing, or two of the same name
	 */
	public RichTextEditor setFeatures(Collection<? extends IRichTextFeature> features)
	{
		this.features = features == null ? null : IRichTextFeature.checked(features);
		return this;
	}

	/**
	 * Adds a feature to those of the editor, the features of the format until the page sets
	 * others: its tools show, and the cleaning lets through what it writes.
	 *
	 * @param feature
	 *            the feature, by a name no other feature of the editor has
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for a feature missing, or of the name of another
	 */
	public RichTextEditor addFeature(IRichTextFeature feature)
	{
		List<IRichTextFeature> all = new ArrayList<>(getFeatures());
		all.add(feature);
		return setFeatures(all);
	}

	/**
	 * Takes a feature away from those of the editor, the features of the format until the page
	 * sets others: its tools no longer show, and the cleaning drops what it writes, but for its
	 * text.
	 *
	 * @param feature
	 *            the feature
	 * @return {@code this}
	 */
	public RichTextEditor removeFeature(IRichTextFeature feature)
	{
		List<IRichTextFeature> all = new ArrayList<>(getFeatures());
		all.remove(feature);
		return setFeatures(all);
	}

	/**
	 * @return the palette of the colors of the text, in the order of its menu; unmodifiable:
	 *         {@link #setColors(Collection)} and {@link #addColor(IRichTextColor)} change it
	 */
	public Set<IRichTextColor> getColors()
	{
		return colors;
	}

	/**
	 * Adds a color at the end of the palette of the colors of the text, {@link RichTextColor#PALETTE}
	 * until the page sets one, or none. The page's style sheets color its class; the
	 * {@link #getDefaultSanitizer() cleaning} keeps it. An editor already in the page shows it once
	 * rendered again.
	 *
	 * @param color
	 *            the color, by a name no other color of the palette has
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for a color {@link IRichTextColor#palette(Collection)} does not take: missing, a
	 *             name or a class not allowed, or one another color of the palette has
	 */
	public RichTextEditor addColor(IRichTextColor color)
	{
		List<IRichTextColor> palette = new ArrayList<>(colors);
		palette.add(color);
		return setColors(palette);
	}

	/**
	 * @param colors
	 *            the palette of the colors of the text, in the order of its menu (a {@link List} or
	 *            a {@link java.util.LinkedHashSet} keeps it), each by a name unique in it,
	 *            {@link RichTextColor#PALETTE} by default; {@code null} or empty for none, the editor
	 *            then without the tool of the color of the text. The page's style sheets color the
	 *            classes of colors of its own; the {@link #getDefaultSanitizer() cleaning} keeps
	 *            those classes, and no others.
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for a palette {@link IRichTextColor#palette(Collection)} does not take: a color
	 *             missing, a name or a class not allowed, or one twice
	 */
	public RichTextEditor setColors(Collection<? extends IRichTextColor> colors)
	{
		this.colors = colors == null ? Collections.emptySet() : IRichTextColor.palette(colors);
		return this;
	}

	/**
	 * @return the palette of the colors of the background, in the order of its menu; unmodifiable:
	 *         {@link #setBackgroundColors(Collection)} and
	 *         {@link #addBackgroundColor(IRichTextBackgroundColor)} change it
	 */
	public Set<IRichTextBackgroundColor> getBackgroundColors()
	{
		return backgroundColors;
	}

	/**
	 * Adds a color at the end of the palette of the colors of the background,
	 * {@link RichTextBackgroundColor#PALETTE} until the page sets one, or none. The page's style
	 * sheets color its class; the {@link #getDefaultSanitizer() cleaning} keeps it. An editor
	 * already in the page shows it once rendered again.
	 *
	 * @param color
	 *            the color, by a name no other color of the palette has
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for a color {@link IRichTextBackgroundColor#palette(Collection)} does not take:
	 *             missing, a name or a class not allowed, or one another color of the palette has
	 */
	public RichTextEditor addBackgroundColor(IRichTextBackgroundColor color)
	{
		List<IRichTextBackgroundColor> palette = new ArrayList<>(backgroundColors);
		palette.add(color);
		return setBackgroundColors(palette);
	}

	/**
	 * @param backgroundColors
	 *            the palette of the colors of the background, in the order of its menu (a
	 *            {@link List} or a {@link java.util.LinkedHashSet} keeps it), each by a name unique
	 *            in it, {@link RichTextBackgroundColor#PALETTE} by default; {@code null} or empty
	 *            for none, the editor then without the tool of the background. The page's style
	 *            sheets color the classes of colors of its own; the
	 *            {@link #getDefaultSanitizer() cleaning} keeps those classes, and no others.
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for a palette {@link IRichTextBackgroundColor#palette(Collection)} does not take
	 */
	public RichTextEditor setBackgroundColors(
		Collection<? extends IRichTextBackgroundColor> backgroundColors)
	{
		this.backgroundColors = backgroundColors == null ? Collections.emptySet() :
			IRichTextBackgroundColor.palette(backgroundColors);
		return this;
	}

	/**
	 * @return the tools of the toolbar, in groups; empty for no toolbar
	 */
	public List<List<IRichTextTool>> getToolbar()
	{
		return toolbar;
	}

	/**
	 * @param toolbar
	 *            the tools of the toolbar, in groups: built-in {@link RichTextTool}s and those of
	 *            the features of the page's own; {@link RichTextTool#DEFAULT_TOOLBAR} by default,
	 *            {@code null} or empty for no toolbar. The tools of features left out do not show.
	 * @return {@code this}
	 */
	public RichTextEditor setToolbar(List<? extends List<? extends IRichTextTool>> toolbar)
	{
		this.toolbar = groups(toolbar);
		return this;
	}

	private static List<List<IRichTextTool>> groups(
		List<? extends List<? extends IRichTextTool>> toolbar)
	{
		List<List<IRichTextTool>> groups = new ArrayList<>();
		if (toolbar != null)
		{
			toolbar.stream().filter(group -> group != null && !group.isEmpty())
				.forEach(group -> groups.add(List.<IRichTextTool>copyOf(group)));
		}
		return List.copyOf(groups);
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
	 *            whether a menu of the inline formats (bold, italic, underline, strike, code,
	 *            superscript, subscript and link) shows over the text selected, {@code false} by
	 *            default
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
	public IRichTextImageStore getImageStore()
	{
		return imageStore;
	}

	/**
	 * @param imageStore
	 *            what keeps the images the user uploads and tells their addresses; {@code null}
	 *            for no uploads, as by default: an image is an address the user gives
	 * @return {@code this}
	 */
	public RichTextEditor setImageStore(IRichTextImageStore imageStore)
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
			imageUpload.setMaxSize(imageMaxSize).setTypes(imageTypes)
				.setMaxDimensions(imageMaxDimensions);
			add(imageUpload);
			imagesRemoval = new AbstractDefaultAjaxBehavior()
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void respond(AjaxRequestTarget target)
				{
					onImagesRemovedSent(target, RichTextImageUploadBehavior.urlsOf(getRequest()
						.getRequestParameters().getParameterValue(
							RichTextImageUploadBehavior.REMOVED_PARAMETER).toString("[]")));
				}
			};
			add(imagesRemoval);
		}
		return this;
	}

	private String storeImage(FileUpload image, IRichTextImageType type) throws IOException
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
	 * @return the types of the images the user uploads, in their order
	 */
	public Set<IRichTextImageType> getImageTypes()
	{
		return Collections.unmodifiableSet(imageTypes);
	}

	/**
	 * @param imageTypes
	 *            the types of the images the user uploads, as their bytes say, in the order the
	 *            messages name them: those of {@link RichTextImageType} and those of the
	 *            application's own ({@link IRichTextImageType}); all those built in by default
	 * @return {@code this}
	 */
	public RichTextEditor setImageTypes(Collection<? extends IRichTextImageType> imageTypes)
	{
		this.imageTypes = RichTextImageUploadBehavior.checkedTypes(imageTypes);
		if (imageUpload != null)
		{
			imageUpload.setTypes(this.imageTypes);
		}
		return this;
	}

	/**
	 * @return the widest and the highest image the user uploads, {@code null} for no limit
	 * @since 10.11.0
	 */
	public RichTextImageDimensions getImageMaxDimensions()
	{
		return imageMaxDimensions;
	}

	/**
	 * @param imageMaxDimensions
	 *            the widest and the highest image the user uploads, in pixels; {@code null} for no
	 *            limit, as by default. The browser reads them from the file before sending it, and
	 *            the server again from its header, without decoding the picture: a file of a few
	 *            kilobytes may hold a picture of many millions of pixels, which every browser
	 *            showing the text would decode. An image larger is refused, or scaled down in the
	 *            browser while {@link #setImageScaling(boolean) the user may}.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public RichTextEditor setImageMaxDimensions(RichTextImageDimensions imageMaxDimensions)
	{
		this.imageMaxDimensions = imageMaxDimensions;
		if (imageUpload != null)
		{
			imageUpload.setMaxDimensions(imageMaxDimensions);
		}
		return this;
	}

	/**
	 * @param maxWidth
	 *            the widest image the user uploads, in pixels
	 * @param maxHeight
	 *            the highest image the user uploads, in pixels
	 * @return {@code this}
	 * @see #setImageMaxDimensions(RichTextImageDimensions)
	 * @since 10.11.0
	 */
	public RichTextEditor setImageMaxDimensions(int maxWidth, int maxHeight)
	{
		return setImageMaxDimensions(new RichTextImageDimensions(maxWidth, maxHeight));
	}

	/**
	 * @return how many images one drop or choice of the user takes at most, {@code null} for no
	 *         limit
	 * @since 10.11.0
	 */
	public Integer getImageMaxCount()
	{
		return imageMaxCount;
	}

	/**
	 * @param imageMaxCount
	 *            how many images one drop, choice or paste of the user takes at most, the first
	 *            ones, the others left out with a message; {@code null} for no limit, as by
	 *            default. A limit of the browser alone: each image is a request of its own, which
	 *            the server checks as any other; a store limits how many a user keeps.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public RichTextEditor setImageMaxCount(Integer imageMaxCount)
	{
		this.imageMaxCount = imageMaxCount == null ? null : Math.max(imageMaxCount, 1);
		return this;
	}

	/**
	 * @return whether the form of the image opens a dialog to upload images in
	 * @since 10.11.0
	 */
	public boolean isImageUploadDialog()
	{
		return imageUploadDialog;
	}

	/**
	 * @param imageUploadDialog
	 *            whether the form of the image has a button opening a dialog to upload images in:
	 *            a zone to drop them on or choose them, and the list of the files, each uploading,
	 *            uploaded or refused and why, which the user takes out before the images go into
	 *            the text; {@code false}, the default, for the zone in the form itself, each image
	 *            going into the text as soon as it is uploaded
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public RichTextEditor setImageUploadDialog(boolean imageUploadDialog)
	{
		this.imageUploadDialog = imageUploadDialog;
		return this;
	}

	/**
	 * @return whether the user may have a picture too large scaled down rather than refused
	 * @since 10.11.0
	 */
	public boolean isImageScaling()
	{
		return imageScaling;
	}

	/**
	 * @param imageScaling
	 *            whether a dialog offers the user to scale down the pictures larger than
	 *            {@link #setImageMaxDimensions(RichTextImageDimensions) the limit}, each by a box to
	 *            tick, rather than refusing them; {@code false} by default. The browser scales a
	 *            picture down before sending it, in its type when it can write it, else in PNG: an
	 *            animated GIF loses its animation. The server checks the picture it gets as any
	 *            other.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public RichTextEditor setImageScaling(boolean imageScaling)
	{
		this.imageScaling = imageScaling;
		return this;
	}

	/**
	 * @return when the images the user took out of the text are deleted
	 * @since 10.11.0
	 */
	public RichTextImageDeletion getImageDeletion()
	{
		return imageDeletion;
	}

	/**
	 * @param imageDeletion
	 *            when the images the user took out of the text are deleted: at once, or once the
	 *            text is saved, {@link RichTextImageDeletion#ON_SAVE} by default
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public RichTextEditor setImageDeletion(RichTextImageDeletion imageDeletion)
	{
		this.imageDeletion = Args.notNull(imageDeletion, "imageDeletion");
		return this;
	}

	/**
	 * @return the Ajax behavior hearing of the images taken out of the text, {@code null} until the
	 *         editor has a store
	 */
	AbstractDefaultAjaxBehavior getImagesRemoval()
	{
		return imagesRemoval;
	}

	/**
	 * @return the images of the text of the model, as its format writes them
	 */
	private Set<String> modelImages()
	{
		return RichTextImageAddresses.of(getModelObject(), getFormat());
	}

	/**
	 * @return the images the editor knows: those of the text of the model it was shown with, and
	 *         those uploaded since it was last saved
	 */
	private Set<String> knownImages()
	{
		Set<String> known = new LinkedHashSet<>(imagesShown);
		if (imageUpload != null)
		{
			known.addAll(imageUpload.getUploaded());
		}
		return known;
	}

	/**
	 * The browser told of images the user took out of the text: those the editor knows are
	 * deleted now, or once the text is saved, then told of.
	 */
	private void onImagesRemovedSent(AjaxRequestTarget target, List<String> urls)
	{
		if (imageStore == null || !isImageEditing())
		{
			return;
		}
		Set<String> known = knownImages();
		Set<String> removed = new LinkedHashSet<>();
		urls.stream().filter(known::contains).forEach(removed::add);
		if (removed.isEmpty())
		{
			return;
		}
		boolean now = imageDeletion == RichTextImageDeletion.AT_ONCE;
		if (now)
		{
			deleteImages(Collections.unmodifiableSet(removed));
			imagesShown.removeAll(removed);
			imageUpload.forget(removed);
		}
		else
		{
			imagesRemoved.addAll(removed);
		}
		Set<String> told = Collections.unmodifiableSet(removed);
		onImagesRemoved(target, told);
		send(this, Broadcast.BUBBLE, new RichTextImagesRemovedEvent(this, told, now, target));
	}

	/**
	 * Called when the browser told that the user took images out of the text, those the editor
	 * knows, after they were deleted in {@link RichTextImageDeletion#AT_ONCE}, before the
	 * {@link RichTextImagesRemovedEvent}. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param urls
	 *            the addresses of the images
	 * @since 10.11.0
	 */
	protected void onImagesRemoved(AjaxRequestTarget target, Set<String> urls)
	{
	}

	/**
	 * Deletes images the user took out of the text, at once or once the text is saved, as
	 * {@link #getImageDeletion() the mode} says: each by {@link IRichTextImageStore#delete(String)}
	 * by default. A page overrides it to decide which, knowing its texts: an image pasted into
	 * another one stays, for example.
	 *
	 * @param urls
	 *            the addresses of the images, those of the text the editor was shown with and those
	 *            uploaded into it, never others
	 * @since 10.11.0
	 */
	protected void deleteImages(Set<String> urls)
	{
		IRichTextImageStore store = imageStore;
		if (store != null)
		{
			urls.forEach(url -> RichTextImageUploadBehavior.delete(store, url));
		}
	}

	/**
	 * Gives the model the text, which the images the user took out of are still told of against,
	 * until it is saved.
	 */
	@Override
	public void updateModel()
	{
		super.updateModel();
		shownHash = hash(getModelObject());
	}

	/**
	 * Tells the browser the text of the model was saved, then deletes the images the user took
	 * out of it, as {@link #deleteRemovedImages()} does.
	 */
	@Override
	public RichTextEditor markSaved(AjaxRequestTarget target)
	{
		super.markSaved(target);
		return deleteRemovedImages();
	}

	/**
	 * Deletes, by {@link #deleteImages(Set)}, the images the user took out of the text since it was
	 * last saved, and those uploaded since that it does not hold: the text of the model is saved
	 * without them. {@link #markSaved(AjaxRequestTarget)} calls it; call it once the text is saved
	 * otherwise, by a form sent without Ajax for example. A text the page put in the model since
	 * the editor was shown, not the user's, deletes nothing.
	 *
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public RichTextEditor deleteRemovedImages()
	{
		if (imageStore == null || imageUpload == null)
		{
			return this;
		}
		String text = getModelObject();
		Set<String> saved = modelImages();
		Set<String> gone = new LinkedHashSet<>();
		if (hash(text) == shownHash)
		{
			gone.addAll(imagesRemoved);
			gone.addAll(imageUpload.getUploaded());
			gone.removeAll(saved);
		}
		imageUpload.forget(imageUpload.getUploaded());
		imagesRemoved.clear();
		imagesShown = saved;
		shownHash = hash(text);
		if (!gone.isEmpty())
		{
			deleteImages(Collections.unmodifiableSet(gone));
		}
		return this;
	}

	/**
	 * Remembers the images of the text the editor is shown with. Another text than before, which
	 * the page put in the model, forgets the images the user took out of the one before.
	 */
	private void showImages()
	{
		String text = getModelObject();
		int hash = hash(text);
		if (hash != shownHash)
		{
			shownHash = hash;
			imagesRemoved.clear();
		}
		imagesShown = modelImages();
	}

	private static int hash(String text)
	{
		return text == null ? 0 : text.hashCode();
	}

	/**
	 * @return whether the user changes the images of the text now: the editor is enabled, not read
	 *         only, with the feature image
	 */
	private boolean isImageEditing()
	{
		return !isReadOnly() && getFeatures().contains(RichTextFeature.IMAGE) &&
			isEnabledInHierarchy();
	}

	/**
	 * @return what checks the spelling, the grammar and the style of the text, {@code null} for none
	 */
	public IRichTextProofreader getProofreader()
	{
		return proofreader;
	}

	/**
	 * @param proofreader
	 *            what checks the spelling, the grammar and the style of the text, through the
	 *            server; {@code null}, the default, for none: the browser checks the spelling then,
	 *            unless {@link #setSpellcheck(Boolean) told otherwise}
	 * @return {@code this}
	 */
	public RichTextEditor setProofreader(IRichTextProofreader proofreader)
	{
		this.proofreader = proofreader;
		if (proofreader != null && proofreading == null)
		{
			proofreading = new RichTextProofreadBehavior(this::proofread)
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean isProofreadAllowed()
				{
					return isProofreading();
				}

				@Override
				protected Locale getLanguage(Locale requested)
				{
					Locale own = getTextLanguage();
					return own == null ? requested : own;
				}
			};
			add(proofreading);
		}
		return this;
	}

	/**
	 * @return the behavior checking the texts, {@code null} until the editor has a proofreader:
	 *         its limits are set there
	 */
	public RichTextProofreadBehavior getProofreadBehavior()
	{
		return proofreading;
	}

	private List<List<RichTextIssue>> proofread(List<String> texts, Locale language)
		throws IOException
	{
		if (proofreader == null)
		{
			throw new IOException("The editor has no proofreader");
		}
		return proofreader.check(texts, language);
	}

	/**
	 * @return whether the server checks the text now: the editor has a proofreader, and is
	 *         enabled, not read only
	 */
	public boolean isProofreading()
	{
		return proofreader != null && !isReadOnly() && isEnabledInHierarchy();
	}

	/**
	 * @param suggester
	 *            what offers items for what the user types, of a name of its own in the editor
	 * @return {@code this}
	 */
	public RichTextEditor addSuggester(IRichTextSuggester suggester)
	{
		if (suggesting == null)
		{
			suggesting = new RichTextSuggestBehavior()
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean isSuggestAllowed()
				{
					return isSuggesting();
				}
			};
			add(suggesting);
		}
		suggesting.add(suggester);
		return this;
	}

	/**
	 * @param suggester
	 *            a suggester to take away
	 * @return {@code this}
	 */
	public RichTextEditor removeSuggester(IRichTextSuggester suggester)
	{
		if (suggesting != null)
		{
			suggesting.remove(suggester);
		}
		return this;
	}

	/**
	 * @return the suggesters, in their order
	 */
	public List<IRichTextSuggester> getSuggesters()
	{
		return suggesting == null ? List.of() : suggesting.getSuggesters();
	}

	/**
	 * @return the behavior answering the suggesters, {@code null} until the editor has one
	 */
	public RichTextSuggestBehavior getSuggestBehavior()
	{
		return suggesting;
	}

	/**
	 * @return whether the server offers items now: the editor has suggesters, and is enabled, not
	 *         read only
	 */
	public boolean isSuggesting()
	{
		return !getSuggesters().isEmpty() && !isReadOnly() && isEnabledInHierarchy();
	}

	/**
	 * @return whether the user uploads images now: the editor has a store, and is enabled, not read
	 *         only, with the feature image
	 */
	public boolean isImageUploading()
	{
		return imageStore != null && isImageEditing();
	}

	@Override
	protected void renderEditorHead(IHeaderResponse response)
	{
		renderHeadItems(response);
		if (isImageUploading() && (imageUploadDialog || imageScaling && imageMaxDimensions != null))
		{
			response.render(CssHeaderItem.forReference(Modal.CSS));
		}
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
	 * extensions, the palettes, the highlighting, the languages of the code blocks and the uploads
	 * of images.
	 */
	@Override
	protected void putEditorOptions(JSONObject options)
	{
		options.put("bubbleMenu", bubbleMenu);
		Set<IRichTextFeature> features = getFeatures();
		if (features.contains(RichTextFeature.TEXT_COLOR))
		{
			options.put("colors", colors(this, colors));
		}
		if (features.contains(RichTextFeature.TEXT_BACKGROUND))
		{
			options.put("backgroundColors", backgroundColors(this, backgroundColors));
		}
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
			if (imageMaxDimensions != null)
			{
				options.put("uploadMaxWidth", imageMaxDimensions.width());
				options.put("uploadMaxHeight", imageMaxDimensions.height());
				options.put("uploadScaling", imageScaling);
			}
			if (imageMaxCount != null)
			{
				options.put("uploadMaxCount", imageMaxCount.intValue());
			}
			if (imageUploadDialog)
			{
				options.put("uploadDialog", true);
			}
			showImages();
			options.put("imagesUrl", imagesRemoval.getCallbackUrl().toString());
		}
		if (isProofreading())
		{
			options.put("proofreadUrl", proofreading.getProofreadUrl());
		}
		if (isSuggesting())
		{
			options.put("suggestUrl", suggesting.getSuggestUrl());
			options.put("suggesters", suggesting.toJson());
		}
	}

	@Override
	protected JSONObject getTexts()
	{
		JSONObject texts = texts(this);
		// the tools of the features of the page's own, named as their scripts do unless translated
		toolbar.forEach(group -> group.forEach(tool -> {
			if (!(tool instanceof RichTextTool))
			{
				String name = getString("RichTextEditor." + tool.getName(), null, "");
				if (!Strings.isEmpty(name))
				{
					texts.put(tool.getName(), name);
				}
			}
		}));
		return texts;
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
		TEXT_NAMES.forEach(name -> texts.put(name, text(component, name)));
		return texts;
	}

	/**
	 * A palette of the colors of the text as the editors in the browser take it, its colors named in
	 * the language of a component: the option {@code colors} of this editor, and of the React and
	 * Svelte editors.
	 *
	 * @param component
	 *            the component whose resources translate the names of the colors,
	 *            {@code RichTextEditor.colorRed} for {@code red}; {@code null} for English
	 * @param colors
	 *            the palette, checked as {@link IRichTextColor#palette(Collection)} does
	 * @return the colors, each with its name, its class and its label
	 */
	public static JSONArray colors(Component component, Collection<? extends IRichTextColor> colors)
	{
		JSONArray palette = new JSONArray();
		for (IRichTextColor color : IRichTextColor.palette(colors))
		{
			JSONObject json = new JSONObject();
			json.put("name", color.getName());
			json.put("colorClass", color.getColorClass());
			json.put("label", colorLabel(component, color.getName()));
			palette.put(json);
		}
		return palette;
	}

	/**
	 * A palette of the colors of the background as the editors in the browser take it, its colors
	 * named in the language of a component: the option {@code backgroundColors} of this editor, and
	 * of the React and Svelte editors.
	 *
	 * @param component
	 *            the component whose resources translate the names of the colors,
	 *            {@code RichTextEditor.colorRed} for {@code red}; {@code null} for English
	 * @param colors
	 *            the palette, checked as {@link IRichTextBackgroundColor#palette(Collection)} does
	 * @return the colors, each with its name, its class and its label
	 */
	public static JSONArray backgroundColors(Component component,
		Collection<? extends IRichTextBackgroundColor> colors)
	{
		JSONArray palette = new JSONArray();
		for (IRichTextBackgroundColor color : IRichTextBackgroundColor.palette(colors))
		{
			JSONObject json = new JSONObject();
			json.put("name", color.getName());
			json.put("backgroundClass", color.getBackgroundClass());
			json.put("label", colorLabel(component, color.getName()));
			palette.put(json);
		}
		return palette;
	}

	/**
	 * @return what the menus call a color of that name: the translation of
	 *         {@code RichTextEditor.colorRed} for red, else its name
	 */
	private static String colorLabel(Component component, String name)
	{
		String label = translation(component, "color" + Character.toUpperCase(name.charAt(0)) +
			name.substring(1));
		return label == null ? name : label;
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
		String text = translation(component, name);
		return text == null ? name : text;
	}

	/**
	 * @param component
	 *            the component whose resources translate the text, {@code null} for English
	 * @param name
	 *            the name of a text of the editor
	 * @return the translation of {@code RichTextEditor.} followed by the name, in the language of
	 *         the component, else in English; {@code null} for none, or outside an application
	 */
	private static String translation(Component component, String name)
	{
		String key = "RichTextEditor." + name;
		String text;
		if (component != null)
		{
			text = component.getString(key, null, "");
		}
		else if (Application.exists())
		{
			text = Application.get().getResourceSettings().getLocalizer().getString(key, null, null,
				Locale.ENGLISH, null, "");
		}
		else
		{
			text = null;
		}
		return Strings.isEmpty(text) ? null : text;
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
