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
package com.antiliacode.widgets.ckeditor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.modal.Modal;
import com.antiliacode.widgets.resizable.ResizableBehavior;
import com.antiliacode.widgets.resource.AlreadyCompressedJavaScriptResourceReference;
import com.antiliacode.widgets.richtext.AbstractRichTextEditor;
import com.antiliacode.widgets.richtext.IRichTextImageStore;
import com.antiliacode.widgets.richtext.IRichTextSanitizer;
import com.antiliacode.widgets.richtext.RichTextEditor;
import com.antiliacode.widgets.richtext.RichTextEditorState;
import com.antiliacode.widgets.richtext.RichTextFormat;
import com.antiliacode.widgets.richtext.RichTextImageDimensions;
import com.antiliacode.widgets.richtext.RichTextImageUploadBehavior;
import com.antiliacode.widgets.scrollbar.ScrollbarBehavior;

/**
 * A text area shown as CKEditor 5, a rich text editor with its own toolbar, balloons and dialogs,
 * in the colors of the {@link com.antiliacode.widgets.theme.Theme theme} it is in: the
 * {@link CKEditorFeature features} of the text, all of them ({@link #FEATURES}) unless
 * {@link #setFeatures(Collection) told otherwise}, the files attached with a store of files alone,
 * are CKEditor's plugins, and its
 * toolbar has their tools unless {@link #setToolbar(List) set otherwise}. Attach it to a
 * {@code <textarea>}.
 * <p>
 * ui-components holds no code of CKEditor, which is licensed under the GPL 2 or later, or under the
 * commercial terms of CKSource: the application brings it, its style sheet, its translations and
 * its license key, as a {@link CKEditorLibrary}, which it sets once as it starts. The editor speaks
 * the language of the page when the library has a translation of it, English otherwise.
 * <p>
 * It has the contract of {@link RichTextEditor}, Tiptap's, so that a page can switch between the
 * two, from {@link AbstractRichTextEditor}: the model is the text, HTML by default, or Markdown
 * with CKEditor's Markdown plugin ({@link #setFormat(RichTextFormat)}); the HTML is
 * {@link #setSanitizer(IRichTextSanitizer) cleaned} before the model gets it, with the
 * {@link CKEditorPolicy} of the features by default, and {@link CKEditorView} shows it in a page,
 * cleaned the same way; the Markdown is cleaned of the addresses of links and images that are not
 * safe, not of HTML ({@link #setMarkdownSanitizer(IRichTextSanitizer)}). The text area stays in
 * the page, hidden, and holds the text; the editor
 * fires the events of {@link com.antiliacode.widgets.richtext.RichTextEditorEvents} and listens to
 * those it is {@link #addEvent(com.antiliacode.widgets.event.IClientEvent) given}; it keeps its
 * {@link RichTextEditorState state} on the server; it tells the tabs and the desktop it is in
 * whether its text has changes not saved.
 * <p>
 * The HTML has no style attribute, which a Content Security Policy without {@code 'unsafe-inline'}
 * would block: text aligns by classes, those of {@link RichTextEditor}, and images have no width
 * or height. More plugins of the build ({@link #setPlugins(String...)}) and CKEditor's
 * configuration of the page's own ({@link #setConfig(JSONObject)}) may write more: a
 * {@link #setSanitizer(IRichTextSanitizer) sanitizer} of the page lets it through. Those writing
 * styles, {@code FontFamily}, {@code FontSize} in pixels, {@code FontColor},
 * {@code FontBackgroundColor}, {@code ListProperties}, {@code ImageResize},
 * {@code TableProperties} and {@code TableCellProperties}, are left out by default: they need
 * {@code 'unsafe-inline'} in the {@code style-src} of the page's Content Security Policy, and a
 * sanitizer keeping their styles.
 * <p>
 * Given {@link #setImageStore(IRichTextImageStore) a store}, the editor uploads the images the user
 * chooses, pastes or drops, as {@link RichTextEditor} does: CKEditor's {@code ImageUpload}, with
 * an upload adapter of the editor's own posting each image to a
 * {@link RichTextImageUploadBehavior}, which checks its type by its bytes, its size and its pixels
 * ({@link #setImageTypes(Collection)}, {@link #setImageMaxSize(Bytes)},
 * {@link #setImageMaxDimensions(RichTextImageDimensions)}), has the store keep it and answers its
 * address, or why not in the language of the page. The browser checks the image first, and shows
 * the progress on it, without a style attribute.
 * <p>
 * The rest of the uploads of {@link RichTextEditor} are CKEditor's too, from
 * {@link AbstractRichTextEditor}, with tools of the editor's own in CKEditor's toolbar, which a
 * {@link #setToolbar(List) toolbar of the page's own} names too: how many images one choice or
 * drop takes, and the pictures too large scaled down when the user ticks them
 * ({@link #setImageMaxCount(Integer)}, {@link #setImageScaling(boolean)}); the dialog of the
 * uploads, the tool {@code uploadImages} ({@link #setImageUploadDialog(boolean)}); the images kept
 * permanently, ticked in that dialog and inserted by the tool {@code storedImage}
 * ({@link #setPermanentImages(boolean)}); the tool {@code images} listing the images of the text,
 * and the deletion of those the user took out ({@link #setImageDeletion(
 * com.antiliacode.widgets.richtext.RichTextImageDeletion)}); the resource of the uploads of the
 * application ({@link #setUploadResource(com.antiliacode.widgets.richtext.RichTextFiles)}); and the
 * files attached, {@link CKEditorFeature#ATTACHMENT}, an inline widget, with the tools
 * {@code attachment} and {@code attachments} ({@link #setAttachmentStore(
 * com.antiliacode.widgets.richtext.IRichTextAttachmentStore)}).
 * <p>
 * CKEditor 49 has no Watchdog: after an error escaping it, CKEditor keeps running, but may be left
 * in a state of its own. The editor hears of those errors by CKEditor's {@code onEditorError}, and
 * starts CKEditor again, as the Watchdog did, with the text it last had and the selection, saying
 * so below the text; errors coming too often, it gives up and leaves CKEditor running, telling the
 * user to save the text and reload the page ({@link #setRecovery(CKEditorRecovery)}). It fires
 * {@link CKEditorEvents#ERROR} either way, which {@link CKEditorErrorEvent} hears of on the
 * server, logging it:
 *
 * <pre>
 * editor.addEvent(new CKEditorErrorEvent());
 * </pre>
 * <p>
 * {@code Wicket.CKEditor} from {@link #JS} makes the editor in the browser, with CKEditor's
 * {@code ClassicEditor} of the library; {@link #CSS} gives CKEditor the colors of the theme.
 *
 * @since 11.0.0
 */
public class CKEditor extends AbstractRichTextEditor<CKEditor>
{
	private static final long serialVersionUID = 1L;

	/**
	 * The script making CKEditor of a text area, {@code Wicket.CKEditor}, with no code of CKEditor:
	 * the page renders the library's before. The build minifies it already.
	 */
	public static final ResourceReference JS = new AlreadyCompressedJavaScriptResourceReference(
		CKEditor.class, "antilia-ckeditor.js");

	/**
	 * The style sheet giving CKEditor the colors of the theme and laying the editor out, and the
	 * text a {@link CKEditorView} shows.
	 */
	public static final ResourceReference CSS = new CssResourceReference(CKEditor.class,
		"antilia-ckeditor.css");

	/**
	 * The class of the text area until the script makes the editor of it. While scripts run,
	 * {@link #CSS} hides a text area of this class and keeps the room of the editor for it, so that
	 * a page arriving slowly shows no HTML; without scripts the text area is shown.
	 */
	public static final String LOADING_CLASS = "antilia-ck-editor-loading";

	/**
	 * The features CKEditor has: those of {@link RichTextEditor} but the colors of the text and of
	 * its background, which CKEditor's {@code FontColor} and {@code FontBackgroundColor} write as
	 * styles, where {@link RichTextEditor} writes the classes of its palette.
	 */
	public static final Set<CKEditorFeature> FEATURES = Collections.unmodifiableSet(
		EnumSet.allOf(CKEditorFeature.class));

	/** The texts of the editor's own, those of {@link RichTextEditor} of the same names. */
	private static final List<String> TEXTS = List.of("characters", "character", "words", "word",
		"resize", "linkNewTab", "errorRestarted", "errorGaveUp", "pasteCleaned", "pasteScripts",
		"pasteEmbedded", "pasteForms", "pasteImages", "pasteTables", "pasteLinks",
		"pasteFormatting",
		// those of the uploads
		"storedImage", "images", "attachment", "attachments", "uploadImage", "dropImages",
		"chooseImages", "uploadImagesTitle", "uploadDone", "removeFile", "insertImages", "cancel",
		"close", "noImages", "deleteImage", "uploading", "theImage", "noAttachments",
		"deleteAttachment", "dropFiles", "chooseFiles", "attachFiles", "attachFilesTitle",
		"insertFiles", "attachmentType", "attachTooMany", "theFile", "attaching", "uploadFailed",
		"uploadTooLarge", "uploadType", "uploadTooMany", "uploadTooBig", "scaleTitle", "scaleText",
		"scaleDown", "upload", "cancelUpload", "storePermanently", "permanentFailed", "findImages",
		"findingImages", "noStoredImages");

	/** The features set, {@code null} for those of the format; never the files attached. */
	private Set<CKEditorFeature> features;

	/**
	 * Whether the text holds files attached, {@link CKEditorFeature#ATTACHMENT}: {@code null} for
	 * while the editor has a store of files.
	 */
	private Boolean attachments;

	private CKEditorLibrary library;

	private List<String> toolbar;

	private List<String> plugins = List.of();

	/** CKEditor's configuration of the page's own, as JSON: a JSON object is not serializable. */
	private String config;

	/** How CKEditor starts again after an error, {@code null} for not at all. */
	private CKEditorRecovery recovery = CKEditorRecovery.DEFAULT;

	/**
	 * An editor whose model is inherited, for example from a {@code CompoundPropertyModel}, keeping
	 * its state in the page.
	 *
	 * @param id
	 *            component id
	 */
	public CKEditor(String id)
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
	public CKEditor(String id, IModel<String> model)
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
	public CKEditor(String id, IModel<String> model, IModel<RichTextEditorState> stateModel)
	{
		super(id, model, stateModel);
	}

	@Override
	protected String getScriptObject()
	{
		return "Wicket.CKEditor";
	}

	@Override
	public String getLoadingClass()
	{
		return LOADING_CLASS;
	}

	@Override
	protected IRichTextSanitizer getDefaultSanitizer()
	{
		return CKEditorPolicy.of(getFeatures()).withImagesFromSiteOnly(isImagesFromSiteOnly());
	}

	/**
	 * @return what the text may hold: the features set, or
	 *         {@link CKEditorFeature#defaults(RichTextFormat) those of the format}, all of them for
	 *         HTML but the files attached; and these, {@link CKEditorFeature#ATTACHMENT}, while the
	 *         editor has a {@link #setAttachmentStore(
	 *         com.antiliacode.widgets.richtext.IRichTextAttachmentStore) store of files}, unless
	 *         taken away, or when added
	 */
	@Override
	public Set<CKEditorFeature> getFeatures()
	{
		Set<CKEditorFeature> all = EnumSet.noneOf(CKEditorFeature.class);
		all.addAll(features == null ? CKEditorFeature.defaults(getFormat()) : features);
		if (attachments == null ? getAttachmentStore() != null : attachments)
		{
			all.add(CKEditorFeature.ATTACHMENT);
		}
		return Collections.unmodifiableSet(all);
	}

	/**
	 * @param features
	 *            what the text may hold; {@code null} for the features of the format, all of them
	 *            for HTML, with the files attached while the editor has a store of files. A list
	 *            set holds the files attached, {@link CKEditorFeature#ATTACHMENT}, or keeps them
	 *            out, store or not. {@link CKEditorFeature#of(String)} gives CKEditor's feature of
	 *            the name of one of {@link RichTextEditor}.
	 * @return {@code this}
	 */
	public CKEditor setFeatures(Collection<CKEditorFeature> features)
	{
		if (features == null)
		{
			this.features = null;
			attachments = null;
			return this;
		}
		this.features = EnumSet.noneOf(CKEditorFeature.class);
		this.features.addAll(features);
		attachments = this.features.remove(CKEditorFeature.ATTACHMENT);
		return this;
	}

	/**
	 * @param feature
	 *            a feature the editor gets, besides those of the format until the page sets
	 *            others; the files attached, {@link CKEditorFeature#ATTACHMENT}, stay then, the
	 *            editor with a store of files or not: their links are kept, and the user attaches
	 *            files with a store
	 * @return {@code this}
	 */
	public CKEditor addFeature(CKEditorFeature feature)
	{
		Args.notNull(feature, "feature");
		if (feature == CKEditorFeature.ATTACHMENT)
		{
			attachments = true;
			return this;
		}
		Set<CKEditorFeature> all = withoutAttachments();
		all.add(feature);
		features = all;
		return this;
	}

	/**
	 * @param feature
	 *            a feature the editor no longer has; the files attached,
	 *            {@link CKEditorFeature#ATTACHMENT}, go then, the editor with a store of files or not
	 * @return {@code this}
	 */
	public CKEditor removeFeature(CKEditorFeature feature)
	{
		if (feature == CKEditorFeature.ATTACHMENT)
		{
			attachments = false;
			return this;
		}
		Set<CKEditorFeature> all = withoutAttachments();
		all.remove(feature);
		features = all;
		return this;
	}

	/**
	 * @return the features of the editor but the files attached, which {@link #attachments}
	 *         decides: a set of the editor's to change
	 */
	private Set<CKEditorFeature> withoutAttachments()
	{
		Set<CKEditorFeature> all = EnumSet.noneOf(CKEditorFeature.class);
		all.addAll(features == null ? CKEditorFeature.defaults(getFormat()) : features);
		return all;
	}

	/**
	 * @param format
	 *            how the editor keeps the text: {@link RichTextFormat#HTML} by default, or
	 *            {@link RichTextFormat#MARKDOWN}; CKEditor has no JSON of its own
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for {@link RichTextFormat#JSON}
	 */
	@Override
	public CKEditor setFormat(RichTextFormat format)
	{
		if (format == RichTextFormat.JSON)
		{
			throw new IllegalArgumentException("CKEditor keeps its text as HTML or Markdown, " +
				"not as Tiptap's JSON");
		}
		return super.setFormat(format);
	}

	/**
	 * @return the library of CKEditor the editor uses: the one set, or the application's
	 * @throws IllegalStateException
	 *             when neither the editor nor the application has one
	 */
	public CKEditorLibrary getLibrary()
	{
		return library != null ? library : CKEditorLibrary.get(getApplication());
	}

	/**
	 * @param library
	 *            CKEditor as this editor has it; {@code null} for the application's, as by default
	 * @return {@code this}
	 */
	public CKEditor setLibrary(CKEditorLibrary library)
	{
		this.library = library;
		return this;
	}

	/**
	 * @return the tools of CKEditor's toolbar, as CKEditor names them; {@code null} for those of
	 *         the features, empty for no toolbar
	 */
	public List<String> getToolbar()
	{
		return toolbar;
	}

	/**
	 * @param toolbar
	 *            the tools of CKEditor's toolbar, as CKEditor names them ({@code bold},
	 *            {@code insertTable}...), {@code "|"} between groups; {@code null} for those of
	 *            the features, as by default, empty for no toolbar
	 * @return {@code this}
	 */
	public CKEditor setToolbar(List<String> toolbar)
	{
		this.toolbar = toolbar == null ? null : toolbar.stream().filter(Objects::nonNull)
			.map(String::trim).filter(item -> !item.isEmpty()).toList();
		return this;
	}

	/**
	 * @param toolbar
	 *            the tools of CKEditor's toolbar, {@code "|"} between groups
	 * @return {@code this}
	 * @see #setToolbar(List)
	 */
	public CKEditor setToolbar(String... toolbar)
	{
		return setToolbar(toolbar == null ? null : Arrays.asList(toolbar));
	}

	/**
	 * @return the plugins of the build besides those of the features
	 */
	public List<String> getPlugins()
	{
		return plugins;
	}

	/**
	 * @param plugins
	 *            the names of plugins of CKEditor's build besides those of the features,
	 *            {@code FindAndReplace} for example; none by default. What they write needs a
	 *            {@link #setSanitizer(IRichTextSanitizer) sanitizer} letting it through.
	 * @return {@code this}
	 */
	public CKEditor setPlugins(String... plugins)
	{
		this.plugins = plugins == null ? List.of() : Arrays.stream(plugins)
			.filter(Objects::nonNull).map(String::trim).filter(name -> !name.isEmpty()).distinct()
			.toList();
		return this;
	}

	/**
	 * @return CKEditor's configuration of the page's own, a copy; {@code null} for none
	 */
	public JSONObject getConfig()
	{
		return config == null ? null : new JSONObject(config);
	}

	/**
	 * @param config
	 *            CKEditor's configuration of the page's own, which takes the place of the editor's
	 *            own keys, {@code {"codeBlock": {"languages": [...]}}} for example; {@code null} for
	 *            none, as by default
	 * @return {@code this}
	 */
	public CKEditor setConfig(JSONObject config)
	{
		this.config = config == null ? null : config.toString();
		return this;
	}

	/**
	 * @return how CKEditor starts again in the browser after an error escaped it, {@code null} for
	 *         not at all
	 */
	public CKEditorRecovery getRecovery()
	{
		return recovery;
	}

	/**
	 * @param recovery
	 *            how CKEditor starts again in the browser after an error escaped it, as CKEditor's
	 *            Watchdog did: {@link CKEditorRecovery#DEFAULT} by default; {@code null} to leave
	 *            CKEditor running as it is, as CKEditor 49 does, the editor telling of the error
	 *            alone ({@link CKEditorEvents#ERROR})
	 * @return {@code this}
	 */
	public CKEditor setRecovery(CKEditorRecovery recovery)
	{
		this.recovery = recovery;
		return this;
	}

	/**
	 * @return the language of CKEditor's texts in this page: that of the page's locale when the
	 *         library has a translation of it; {@code null} for English
	 */
	public String getLanguage()
	{
		return getLibrary().getLanguage(getLocale());
	}

	@Override
	protected void renderEditorHead(IHeaderResponse response)
	{
		renderHeadItems(response, getLibrary(), getLanguage());
		if (isImageUploading() && isPermanentImages())
		{
			// the dialog of the uploads, where the user ticks the images to store permanently
			response.render(CssHeaderItem.forReference(Modal.CSS));
		}
	}

	/**
	 * Puts the options of CKEditor: the license key, the language, the toolbar, the plugins, the
	 * configuration of the page's own and the recovery after an error.
	 */
	@Override
	protected void putEditorOptions(JSONObject options)
	{
		options.put("licenseKey", getLibrary().getLicenseKey());
		options.put("recovery", recovery == null ? (Object)false : new JSONObject()
			.put("crashes", recovery.crashes()).put("interval", recovery.interval().toMillis()));
		String language = getLanguage();
		if (language != null)
		{
			options.put("language", language);
		}
		if (toolbar != null)
		{
			options.put("toolbar", toolbar.isEmpty() ? (Object)false : new JSONArray(toolbar));
		}
		if (!plugins.isEmpty())
		{
			options.put("plugins", new JSONArray(plugins));
		}
		if (config != null)
		{
			options.put("config", new JSONObject(config));
		}
	}

	@Override
	protected JSONObject getTexts()
	{
		return texts(this);
	}

	/**
	 * What the editor says of its own, in the language of a component: the count and the bar
	 * resizing it, the switch opening a link in a new tab, the line telling of an error and the
	 * notice of what a paste lost; CKEditor translates the rest. The
	 * texts of this editor, and those a page hands to the React and Svelte editors.
	 *
	 * @param component
	 *            the component whose resources translate the texts, those of
	 *            {@link RichTextEditor}
	 * @return the texts, by their names in the browser
	 */
	public static JSONObject texts(Component component)
	{
		JSONObject all = RichTextEditor.texts(component);
		JSONObject texts = new JSONObject();
		TEXTS.forEach(name -> texts.put(name, all.getString(name)));
		return texts;
	}

	/**
	 * Renders what the editor needs in the browser, in this order: Wicket's Ajax, the library's
	 * style sheet, {@link #CSS} and that of the bar resizing it, {@link ResizableBehavior#CSS}, and
	 * those of the uploads, the files attached and the lists and the forms of their tools, which
	 * are {@link RichTextEditor#CSS} and those of its buttons and scroll bars; the library's script,
	 * the translation of a language, and {@link #JS}. A page with editors of React or Svelte
	 * renders them too.
	 *
	 * @param response
	 *            the header response
	 * @param library
	 *            CKEditor as the application brings it
	 * @param language
	 *            the language of CKEditor's texts, whose translation the library has; {@code null}
	 *            for English
	 */
	public static void renderHeadItems(IHeaderResponse response, CKEditorLibrary library,
		String language)
	{
		Args.notNull(library, "library");
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(CssHeaderItem.forReference(library.getCss()));
		response.render(CssHeaderItem.forReference(ResizableBehavior.CSS));
		// the uploads: the files attached, the lists of the images and of the files, the dialogs
		response.render(CssHeaderItem.forReference(ButtonBehavior.CSS));
		response.render(CssHeaderItem.forReference(ScrollbarBehavior.CSS));
		response.render(CssHeaderItem.forReference(RichTextEditor.CSS));
		response.render(CssHeaderItem.forReference(CSS));
		response.render(JavaScriptHeaderItem.forReference(library.getScript()));
		ResourceReference translation = library.getTranslation(language);
		if (translation != null)
		{
			response.render(JavaScriptHeaderItem.forReference(translation));
		}
		response.render(JavaScriptHeaderItem.forReference(JS));
	}
}
