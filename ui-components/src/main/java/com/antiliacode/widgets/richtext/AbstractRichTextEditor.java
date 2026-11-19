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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.json.JSONFunction;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.ScriptJson;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import com.antiliacode.widgets.event.ClientEventsBehavior;
import com.antiliacode.widgets.event.IAjaxRoundTripEvent;
import com.antiliacode.widgets.event.IClientEvent;
import com.antiliacode.widgets.event.IClientEventName;
import com.antiliacode.widgets.event.SendStateEvent;
import com.antiliacode.widgets.modal.Modal;
import com.antiliacode.widgets.resizable.ResizableSize;
import com.antiliacode.widgets.resizable.ResizeAxis;
import com.antiliacode.widgets.upload.IUploadFileFormat;
import com.antiliacode.widgets.upload.IUploadScanner;
import com.antiliacode.widgets.upload.UploadFileFormat;
import com.antiliacode.widgets.veil.PageVeilBehavior;

/**
 * What the rich text editors share, {@link RichTextEditor}, made with Tiptap, and
 * {@link com.antiliacode.widgets.ckeditor.CKEditor}, made with CKEditor 5, so that a page can
 * switch between them: a text area shown as an editor in the browser, whose model is the text,
 * HTML or Markdown, which a form posts as always.
 * <p>
 * The text comes from the browser and is never trusted: the editor cleans it before the model
 * gets it, by its {@link #getFormat() format}.
 * <ul>
 * <li>HTML: {@link #setSanitizer(IRichTextSanitizer) the sanitizer} keeps what the
 * {@link #getFeatures() features} write, and nothing else.</li>
 * <li>Markdown: {@link #setMarkdownSanitizer(IRichTextSanitizer) the sanitizer of Markdown},
 * {@link RichTextMarkdownSanitizer} by default, takes out the addresses of links and images that
 * are not safe, {@code javascript:} for example. It leaves HTML written in the Markdown as it is:
 * render the Markdown with a renderer that shows HTML as text or leaves it out, or set a sanitizer
 * that cleans it.</li>
 * <li>Tiptap's JSON: {@link #setJsonSanitizer(IRichTextSanitizer) the sanitizer of JSON},
 * {@link RichTextJsonSanitizer} by default, takes out the links and images to such addresses, and
 * gives nothing for a text that is no document.</li>
 * </ul>
 * {@link #setImagesFromSiteOnly(boolean)} keeps the images of the site alone, in the three
 * formats: an image from another site, which shows its viewers to that site, goes. It holds for the
 * sanitizers of the editor; one a page sets cleans as it does.
 * By default the cleaning is silent: the model gets the clean text, and no one hears of what was
 * removed. {@link #setCleaning(RichTextCleaning)} makes the editor {@link RichTextCleaning#REPORT
 * report} it, to {@link #onCleaned(AjaxRequestTarget, RichTextCleaningReport)} and in a
 * {@link RichTextCleanedEvent} bubbling to the page, or {@link RichTextCleaning#REJECT reject} the
 * text with an error, as a validator would; {@link RichTextCleaning#setDefault(
 * org.apache.wicket.Application, RichTextCleaning)} sets the mode of every editor of the
 * application. The sanitizers of the editors tell what they removed
 * ({@link IRichTextSanitizer#sanitizeAndReport(String)}): the elements and the attributes of the
 * HTML, the links and the images of Markdown and of JSON whose addresses are not safe; HTML left in
 * Markdown is not told of. A sanitizer of a page's own tells of nothing unless it overrides that
 * method.
 * <p>
 * The editor in the browser cleans what the user pastes too, by the features it has. It tells of
 * what a paste lost with the event {@link RichTextEditorEvents#PASTE_CLEANED}, which
 * {@link RichTextEditorPasteCleanedEvent} hears on the server, and, once
 * {@link #setPasteNotice(boolean) asked}, in a line of its own under the toolbar.
 * <p>
 * Each editor has features of its own kind,
 * {@link IRichTextFeature}s: {@link RichTextFeature} and those of a page for {@link RichTextEditor},
 * {@link com.antiliacode.widgets.ckeditor.CKEditorFeature} for CKEditor, of the same names.
 * <p>
 * The text area stays in the page, hidden, and holds the text: it fires {@code input} as the user
 * types and {@code change} once the editor loses the focus, as a text area does, and the editors
 * fire the same events of their own, {@link RichTextEditorEvents}. The editor listens to the
 * {@link IClientEvent events} it is {@link #addEvent(IClientEvent) given}, of those names or of
 * any other, and to those of its {@link IRichTextFeature#getEvents(org.apache.wicket.Component)
 * features}: each runs its function in the browser, and goes to the server when it is an
 * {@link com.antiliacode.widgets.event.IAjaxEvent}, alone, or an
 * {@link com.antiliacode.widgets.event.IAjaxSubmitEvent}, submitting the form, each through an
 * Ajax behavior of its own, which a {@link ClientEventsBehavior} adds to the editor:
 *
 * <pre>
 * editor.addEvent(new AjaxSubmitEvent(RichTextEditorEvents.SAVE)
 * {
 *     public void onEvent(JSONObject values, Form&lt;?&gt; form,
 *         AjaxRequestTarget target)
 *     {
 *         articles.save(editor.getModelObject());
 *         editor.markSaved(target);
 *     }
 * });
 * editor.addEvent(RichTextEditorResizeEvent.of((width, height, target) -&gt; ...));
 * </pre>
 *
 * The editors fire the events of the types of events registered in the browser too, by the page or
 * a library, from their own events or by hand, and the built-in ones,
 * {@link RichTextEditorEvents#SELECTED_TEXT} for example: see {@link RichTextEditorEvents}.
 *
 * Until the script makes the editor, the text area is hidden in the room the editor takes
 * ({@link #getLoadingClass()}). The editor is attached to a {@code <textarea>}.
 * <p>
 * The editor tells the {@link com.antiliacode.widgets.tabs.TabbedPanel tabs} and the
 * {@link com.antiliacode.widgets.desktop.Desktop desktop} it is in whether its text has changes not
 * saved, against the text it was rendered with or {@link #setSavedModel(IModel) the text last
 * saved}; {@link #markSaved(AjaxRequestTarget)} tells the browser the text was saved.
 * <p>
 * What is selected, how far the text is scrolled and the size the user gave the editor, by the bar
 * along its bottom ({@link #setResizable(ResizeAxis)}), are its {@link RichTextEditorState state},
 * kept in the state model: by default in the editor, and so in the page. The browser sends the
 * server the state with each event going to the server, with the text when the user changed it,
 * which the editor keeps as its input until the form is sent, before the event hears of it. By
 * default the editor listens to six {@link SendStateEvent}s, sending them when they changed since
 * last sent: {@link RichTextEditorEvents#INPUT}, {@link RichTextEditorEvents#SELECT},
 * {@link RichTextEditorEvents#SCROLL} and {@link RichTextEditorEvents#RESIZE} a moment after the
 * user paused ({@link SendStateEvent#DEFAULT_DELAY}), {@link RichTextEditorEvents#BLUR} at once and
 * {@link RichTextEditorEvents#LEAVE}, in a beacon, as the page goes. The requests of the events
 * of the first four names carry {@link PageVeilBehavior#NO_VEIL_PARAMETER}, which a page veil
 * leaves alone, the user going on as they run, whoever gave the editor the event, the page too;
 * those of the blur, the focus, Ctrl+S and the other names are veiled, the page waiting for the
 * server once the editor lost the focus: {@link #isVeiled(IAjaxRoundTripEvent)}. They are events
 * as any other:
 * {@link #removeEvents(IClientEventName...)} takes those of names away,
 * {@link #setEvent(IClientEvent)} puts another in their place, {@link #addEvent(IClientEvent)}
 * adds one, of any name. Besides, the page's other Ajax requests carry the state when the editor
 * did not send it yet: the user scrolled the editor without focusing it, the page not sending
 * the state as the user scrolls, then the request of a check box redraws the form, for example.
 * The text goes with the editor's own events alone, the others not sending the text area.
 *
 * <pre>
 * // the state and the text go as the editor loses the focus and as the page goes, no more as
 * // the user types, moves the cursor, scrolls or resizes
 * editor.removeEvents(RichTextEditorEvents.INPUT, RichTextEditorEvents.SELECT,
 *     RichTextEditorEvents.SCROLL, RichTextEditorEvents.RESIZE);
 * // or as the user types, but not as the editor loses the focus
 * editor.setEvent(ClientEvent.nothing(RichTextEditorEvents.BLUR));
 * </pre>
 *
 * The requests of the events are Wicket's Ajax requests:
 * {@link #updateEventAjaxAttributes(AjaxRequestAttributes)} changes the attributes of all of them,
 * and each event those of its own, by
 * {@link IAjaxRoundTripEvent#updateAjaxAttributes(AjaxRequestAttributes)}: the method, the extra
 * parameters, the dynamic extra parameters and the listeners of the calls, which the built-in
 * events take by their setters:
 *
 * <pre>
 * // the page veil leaves the request sending the state on blur alone
 * editor.setEvent(new SendStateEvent(RichTextEditorEvents.BLUR)
 * 	.setExtraParameter("wicket_nb", "true"));
 * </pre>
 *
 * Rendered again, or shown again after the user came back to the page with a state model of the
 * session, the editor takes the state up again.
 * <p>
 * Given {@link #setImageStore(IRichTextImageStore) a store}, the editor uploads the images the user
 * chooses, pastes or drops, through a {@link RichTextImageUploadBehavior}, which checks their type
 * by their bytes, their size and their pixels as the editor tells it
 * ({@link #setImageTypes(Collection)}, {@link #setImageMaxSize(Bytes)},
 * {@link #setImageMaxDimensions(RichTextImageDimensions)}), the browser checking them first, and
 * how many one drop or choice takes ({@link #setImageMaxCount(Integer)}); it scales a picture too
 * large down instead, when {@link #setImageScaling(boolean) allowed} and the user ticks it, and
 * lists the files as they upload in {@link #setImageUploadDialog(boolean) a dialog}.
 * {@link #setUploadResource(RichTextFiles) The resource of the uploads} of the application takes
 * them instead, with the same checks. The browser tells the server the images the user took out
 * of the text, which the editor deletes by the store, at once or once the text is saved
 * ({@link #setImageDeletion(RichTextImageDeletion)}, {@link #onImagesRemoved(AjaxRequestTarget,
 * Set)}, {@link RichTextImagesRemovedEvent}). With {@link #setPermanentImages(boolean) permanent
 * images}, the user keeps images to insert into any text later. Given
 * {@link #setAttachmentStore(IRichTextAttachmentStore) a store of files}, the user attaches files
 * that are no images, PDFs or spreadsheets, as links with their names and sizes, deleted as the
 * images are ({@link #setAttachmentDeletion(RichTextImageDeletion)}), and
 * {@link #setUploadScanner(IUploadScanner) a scanner} checks each file before a store keeps it. The
 * two editors share all of it, its options going to the browser under the same names.
 *
 * @param <T>
 *            the editor, which its setters return
 * @since 11.0.0
 */
public abstract class AbstractRichTextEditor<T extends AbstractRichTextEditor<T>>
	extends TextArea<String>
{
	private static final long serialVersionUID = 1L;

	/** The height the user resizes the editor from at least, in pixels, unless set otherwise. */
	public static final int DEFAULT_MIN_HEIGHT = 96;

	/** The levels of the headings, unless set otherwise. */
	public static final List<Integer> DEFAULT_HEADING_LEVELS = List.of(1, 2, 3, 4);

	private static final Logger LOG = LoggerFactory.getLogger(AbstractRichTextEditor.class);

	/**
	 * The parameter of the request telling the server of the files the user took out of the text,
	 * their addresses as a JSON array, beside those of the images.
	 */
	static final String ATTACHMENTS_REMOVED_PARAMETER = "removedAttachments";

	private final ClientEventsBehavior events;

	private IModel<RichTextEditorState> stateModel;

	private IModel<String> savedModel;

	private IModel<String> placeholder;

	private Boolean spellcheck;

	private Locale textLanguage;

	private RichTextFormat format = RichTextFormat.HTML;

	private List<Integer> headingLevels = DEFAULT_HEADING_LEVELS;

	private boolean readOnly;

	private boolean count;

	private ResizeAxis resizable = ResizeAxis.VERTICAL;

	private Integer minHeight;

	private Integer maxHeight;

	private Integer minWidth;

	private Integer maxWidth;


	private IRichTextSanitizer sanitizer;

	private IRichTextSanitizer markdownSanitizer;

	private IRichTextSanitizer jsonSanitizer;

	/** Whether the default sanitizers keep the images of the site alone. */
	private boolean imagesFromSiteOnly;

	/** What the editor does when the cleaning removed something, {@code null} for the default. */
	private RichTextCleaning cleaning;

	private boolean pasteNotice;

	/** What the cleaning removed from the text the model is about to get, to report then. */
	private transient RichTextCleaningReport cleaned;

	private IRichTextImageStore imageStore;

	/** The behavior taking the images, made with the first store, unless they go to the resource. */
	private RichTextImageUploadBehavior imageUpload;

	/** The path of the resource of the uploads taking the images, {@code null} for the behavior. */
	private String uploadResourcePath;

	/** The key of the editor's uploads at the resource, made as the editor first renders there. */
	private String uploadKey;

	private Bytes imageMaxSize = RichTextImageUploadBehavior.DEFAULT_MAX_SIZE;

	private Set<IRichTextImageType> imageTypes = new LinkedHashSet<>(RichTextImageType.ALL);

	private RichTextImageDimensions imageMaxDimensions;

	private Integer imageMaxCount;

	private boolean imageUploadDialog;

	private boolean imageScaling;

	private RichTextImageDeletion imageDeletion = RichTextImageDeletion.ON_SAVE;

	/** When the files taken out are deleted, {@code null} for as the images are. */
	private RichTextImageDeletion attachmentDeletion;

	private boolean permanentImages;

	/**
	 * The behavior hearing of the images and the files the user took out of the text, made with the
	 * first store.
	 */
	private ImagesRemoval imagesRemoval;

	private IRichTextAttachmentStore attachmentStore;

	/** The behavior taking the files attached, made with the first store, unless they go to the resource. */
	private RichTextAttachmentUploadBehavior attachmentUpload;

	private Bytes attachmentMaxSize = RichTextAttachmentUploadBehavior.DEFAULT_MAX_SIZE;

	private Set<IUploadFileFormat> attachmentFormats = new LinkedHashSet<>(UploadFileFormat.ALL);

	private Integer attachmentMaxCount;

	private boolean attachmentUploadDialog;

	private IUploadScanner uploadScanner;

	/** The files attached to the text of the model the editor was last shown with. */
	private Set<String> attachmentsShown = new LinkedHashSet<>();

	/** The files the user took out of the text since it was last saved, to delete once it is. */
	private final Set<String> attachmentsRemoved = new LinkedHashSet<>();

	/** The images of the text of the model the editor was last shown with. */
	private Set<String> imagesShown = new LinkedHashSet<>();

	/** The hash of that text, to tell another text from it. */
	private int shownHash;

	/** The images the user took out of the text since it was last saved, to delete once it is. */
	private final Set<String> imagesRemoved = new LinkedHashSet<>();

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the text; inherited when {@code null}
	 * @param stateModel
	 *            where the editor keeps the selection, the scrolling and the size; in the page when
	 *            {@code null}
	 */
	protected AbstractRichTextEditor(String id, IModel<String> model,
		IModel<RichTextEditorState> stateModel)
	{
		super(id, model);
		this.stateModel = stateModel == null ? Model.of((RichTextEditorState)null) : stateModel;
		setOutputMarkupId(true);
		events = new ClientEventsBehavior()
		{
			private static final long serialVersionUID = 1L;

			/**
			 * The events of each feature by its name, asked once while the editor has the feature:
			 * the same events each time, each with its Ajax behavior.
			 */
			private final Map<String, FeatureEvents> featureEvents = new LinkedHashMap<>();

			@Override
			public List<IClientEvent> getEvents()
			{
				List<IClientEvent> all = new ArrayList<>(getAddedEvents());
				Set<? extends IRichTextFeature> features = getFeatures();
				featureEvents.keySet().removeIf(name -> features.stream().noneMatch(
					feature -> feature.getName().equals(name)));
				for (IRichTextFeature feature : features)
				{
					FeatureEvents asked = featureEvents.get(feature.getName());
					if (asked == null || !asked.feature().equals(feature))
					{
						asked = new FeatureEvents(feature, List.copyOf(feature.getEvents(
							AbstractRichTextEditor.this)));
						featureEvents.put(feature.getName(), asked);
					}
					all.addAll(asked.events());
				}
				return all;
			}

			@Override
			protected String getClientObject()
			{
				// the editor, once its script made it
				String global = getScriptObject();
				return "function(element){return " + global + "&&" + global +
					".of(element.id);}";
			}

			@Override
			protected String getClientValues()
			{
				// the state, and whether the text changed, with each event
				String global = getScriptObject();
				return "function(editor){return " + global + "?" + global +
					".stateParameters(editor):{};}";
			}

			@Override
			protected String getClientPendingValues()
			{
				// the state, when not sent yet, with the page's other requests
				String global = getScriptObject();
				return "function(editor){return " + global + "?" + global +
					".pendingParameters(editor):null;}";
			}

			@Override
			protected void onEventSent(AjaxRequestTarget target, IRequestParameters parameters)
			{
				onStateSent(target, parameters, true);
			}

			@Override
			protected void onPendingValuesSent(AjaxRequestTarget target,
				IRequestParameters values)
			{
				// the state alone: the text goes with the editor's own events
				onStateSent(target, values, false);
			}

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				updateEventAjaxAttributes(attributes);
			}

			@Override
			protected boolean isVeiled(IAjaxRoundTripEvent event)
			{
				return AbstractRichTextEditor.this.isVeiled(event);
			}
		};
		add(events);
		for (RichTextEditorEvents name : List.of(RichTextEditorEvents.INPUT,
			RichTextEditorEvents.SELECT, RichTextEditorEvents.SCROLL, RichTextEditorEvents.RESIZE))
		{
			// they report while the user goes on: the veils leave them alone (isVeiled), not the
			// blur
			events.add(new SendStateEvent(name).setDelay(SendStateEvent.DEFAULT_DELAY));
		}
		events.add(new SendStateEvent(RichTextEditorEvents.BLUR));
		events.add(new SendStateEvent(RichTextEditorEvents.LEAVE));
	}

	/**
	 * @return {@code this}, as the editor it is
	 */
	@SuppressWarnings("unchecked")
	protected final T self()
	{
		return (T)this;
	}

	/**
	 * @return the global of the editor's script in the browser, {@code Wicket.RichTextEditor} for
	 *         example, with {@code attach(id, options)} and {@code of(id)}
	 */
	protected abstract String getScriptObject();

	/**
	 * @return the class of the text area until the script makes the editor of it, which the style
	 *         sheet of the editor hides in the room of the editor while scripts run
	 */
	public abstract String getLoadingClass();

	/**
	 * @return what cleans the HTML unless {@link #setSanitizer(IRichTextSanitizer) set otherwise}:
	 *         the policy of the features of the editor, keeping the images of the site alone if
	 *         {@link #isImagesFromSiteOnly() asked}
	 */
	protected abstract IRichTextSanitizer getDefaultSanitizer();

	/**
	 * Renders what the editor needs in the browser, before the script making it.
	 *
	 * @param response
	 *            the header response
	 */
	protected abstract void renderEditorHead(IHeaderResponse response);

	/**
	 * @return what the editor says, in the language of the editor, by the names of the texts in the
	 *         browser
	 */
	protected abstract JSONObject getTexts();

	/**
	 * Puts the options of the editor's own, those the others do not have, into the options it is
	 * attached with. Does nothing by default.
	 *
	 * @param options
	 *            the options
	 */
	protected void putEditorOptions(JSONObject options)
	{
	}

	/**
	 * Changes the attributes of the requests of all the events of the editor going to the server,
	 * those sending its state among them: the
	 * {@link AjaxRequestAttributes#getExtraParameters() extra parameters} a page veil leaves the
	 * requests alone by, or the method, for example. Each event changes those of its requests
	 * after, in its
	 * {@link IAjaxRoundTripEvent#updateAjaxAttributes(AjaxRequestAttributes) own}. Does nothing by
	 * default.
	 *
	 * @param attributes
	 *            the attributes of the requests, with what the events need already: keep it
	 * @see ClientEventsBehavior#updateAjaxAttributes(AjaxRequestAttributes)
	 */
	protected void updateEventAjaxAttributes(AjaxRequestAttributes attributes)
	{
	}

	/**
	 * Says whether the requests of an event of the editor going to the server are veiled, as
	 * Wicket's veils veil a request: by a page veil, or by the local veil of a component around the
	 * editor. Those of the events coming while the user goes on, as they type, move the cursor,
	 * select, scroll or resize the editor, are not, whoever gave the editor the event
	 * ({@link RichTextEditorEvents#isBackground()}): they carry
	 * {@link PageVeilBehavior#NO_VEIL_PARAMETER}, which the veils leave alone, so that the page
	 * goes on taking the user's clicks and keys. Those of the other events, the blur, the focus,
	 * Ctrl+S, the events of other names, are veiled, as what the user does: the page waits for the
	 * server once the editor lost the focus, for example. An editor overriding this decides otherwise,
	 * event by event; an event leaves the veils alone by
	 * {@code setExtraParameter(PageVeilBehavior.NO_VEIL_PARAMETER, "true")} too.
	 *
	 * @param event
	 *            an event of the editor going to the server
	 * @return whether its requests are veiled
	 * @see ClientEventsBehavior#isVeiled(IAjaxRoundTripEvent)
	 */
	protected boolean isVeiled(IAjaxRoundTripEvent event)
	{
		String name = event.getEventName();
		for (RichTextEditorEvents each : RichTextEditorEvents.values())
		{
			if (each.isBackground() && each.getEventName().equals(name))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Takes what the browser sent with an event, or with another request of the page: the state,
	 * which the state model gets, and, with an event, the text when the user changed it, which
	 * the editor keeps as its input.
	 *
	 * @param withText
	 *            whether the editor takes the text the request tells it changed: not from the
	 *            page's other requests, which do not send it
	 */
	private void onStateSent(AjaxRequestTarget target, IRequestParameters parameters,
		boolean withText)
	{
		String sent = parameters.getParameterValue(SendStateEvent.STATE_PARAMETER)
			.toOptionalString();
		if (sent == null)
		{
			// an event of the editor's, without the state: before the script made the editor
			return;
		}
		RichTextEditorState state = RichTextEditorState.parse(sent);
		if (state != null)
		{
			state = state.withSize(resizable == null ? ResizableSize.NONE :
				state.getSize().along(resizable));
			if (!state.equals(stateModel.getObject()))
			{
				stateModel.setObject(state);
			}
		}
		// read where the text area's input is: among what the form posts, which Wicket's Ajax
		// sends with each event
		boolean textChanged = withText && parameters.getParameterValue(
			SendStateEvent.TEXT_CHANGED_PARAMETER).toBoolean(false);
		String[] input = getInputAsArray();
		if (textChanged && input != null && input.length > 0 && input[0] != null && !readOnly)
		{
			inputChanged();
		}
		onStateChanged(target);
	}

	/**
	 * Cleans the text the browser sent, before it is validated and the model gets it, with the
	 * sanitizer of its format: {@link #getSanitizer()} for HTML, {@link #getMarkdownSanitizer()} for
	 * Markdown and {@link #getJsonSanitizer()} for JSON. When the cleaning removed something, the
	 * {@link #getCleaning() mode} decides: nothing more, a report once the model got the text, or
	 * an error, {@code RichTextEditor.rejected}, with the names removed as {@code ${removed}}.
	 */
	@Override
	public void convertInput()
	{
		cleaned = null;
		super.convertInput();
		String text = getConvertedInput();
		if (text != null && isValid())
		{
			IRichTextSanitizer sanitizer = switch (format)
			{
				case HTML -> getSanitizer();
				case MARKDOWN -> getMarkdownSanitizer();
				case JSON -> getJsonSanitizer();
			};
			RichTextCleaning mode = getCleaning();
			String clean;
			if (mode == RichTextCleaning.SILENT)
			{
				clean = sanitizer.sanitize(text);
			}
			else
			{
				RichTextSanitized sanitized = sanitizer.sanitizeAndReport(text);
				RichTextCleaningReport report = sanitized.report();
				if (!report.isEmpty())
				{
					LOG.warn("The cleaning of the rich text editor {} on {} {}: {}",
						getPageRelativePath(), getPage().getClass().getName(),
						mode == RichTextCleaning.REJECT ? "rejected the text, which holds" :
							"removed", report);
					if (mode == RichTextCleaning.REJECT)
					{
						error(new ValidationError().addKey("RichTextEditor.rejected")
							.setVariable("removed", report.toString()));
						return;
					}
					cleaned = report;
				}
				clean = sanitized.text();
			}
			setConvertedInput(Strings.isEmpty(clean) ? null : clean);
		}
	}

	/**
	 * Gives the model the clean text, then tells what the cleaning removed from it, if anything,
	 * in {@link RichTextCleaning#REPORT}: {@link #onCleaned(AjaxRequestTarget,
	 * RichTextCleaningReport)}, then a {@link RichTextCleanedEvent} bubbling to the page.
	 */
	@Override
	public void updateModel()
	{
		super.updateModel();
		// the images the user took out of the text are told of against it, until it is saved
		shownHash = hash(getModelObject());
		RichTextCleaningReport report = cleaned;
		cleaned = null;
		if (report != null)
		{
			AjaxRequestTarget target = getRequestCycle().find(AjaxRequestTarget.class)
				.orElse(null);
			onCleaned(target, report);
			send(this, Broadcast.BUBBLE, new RichTextCleanedEvent(this, report, target));
		}
	}

	/**
	 * Called in {@link RichTextCleaning#REPORT} once the model got the text, when the cleaning
	 * removed something from it, before the {@link RichTextCleanedEvent}. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target, {@code null} when the request is no Ajax one
	 * @param report
	 *            what the cleaning removed
	 */
	protected void onCleaned(AjaxRequestTarget target, RichTextCleaningReport report)
	{
	}

	/**
	 * Called when the browser sent the state, and maybe the text, with an event, after the state
	 * model took it, before the event hears of it. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onStateChanged(AjaxRequestTarget target)
	{
	}

	/**
	 * @param event
	 *            an event the editor listens to, after those it listens to already, of its name and
	 *            of the others: one of {@link RichTextEditorEvents}, or of any other name
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             when the event goes to the server more than one way, or has no handler
	 */
	public T addEvent(IClientEvent event)
	{
		events.add(event);
		return self();
	}

	/**
	 * @param event
	 *            an event the editor listens to in place of those of its name it listens to
	 *            already, the {@link SendStateEvent} of the name by default for example
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             when the event goes to the server more than one way, or has no handler
	 */
	public T setEvent(IClientEvent event)
	{
		events.set(event);
		return self();
	}

	/**
	 * @param event
	 *            an event the editor no longer listens to
	 * @return {@code this}
	 */
	public T removeEvent(IClientEvent event)
	{
		events.remove(event);
		return self();
	}

	/**
	 * @param eventNames
	 *            the names of events the editor no longer listens to, those it listens to by
	 *            default too: the editor then does nothing on them
	 * @return {@code this}
	 */
	public T removeEvents(String... eventNames)
	{
		events.removeEvents(eventNames);
		return self();
	}

	/**
	 * @param eventNames
	 *            the names of events the editor no longer listens to, those it listens to by
	 *            default too: the editor then does nothing on them
	 * @return {@code this}
	 */
	public T removeEvents(IClientEventName... eventNames)
	{
		events.removeEvents(eventNames);
		return self();
	}

	/**
	 * @param eventName
	 *            the name of events
	 * @return the events of the name the editor was given, or listens to by default, in their
	 *         order
	 */
	public List<IClientEvent> getEvents(IClientEventName eventName)
	{
		return events.getEvents(eventName);
	}

	/**
	 * @param eventName
	 *            the name of events
	 * @return the events of the name the editor was given, or listens to by default, in their
	 *         order
	 */
	public List<IClientEvent> getEvents(String eventName)
	{
		return events.getEvents(eventName);
	}

	/**
	 * @return the events the editor listens to: those it was given, and by default, then those of
	 *         its features
	 */
	public List<IClientEvent> getEvents()
	{
		return events.getEvents();
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
	public T setStateModel(IModel<RichTextEditorState> stateModel)
	{
		this.stateModel = stateModel == null ? Model.of((RichTextEditorState)null) : stateModel;
		return self();
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
	public T setState(RichTextEditorState state)
	{
		stateModel.setObject(state);
		return self();
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
	public T setSavedModel(IModel<String> savedModel)
	{
		this.savedModel = savedModel;
		return self();
	}

	/**
	 * Tells the browser the text of the model was saved: the editor no longer has changes, unless
	 * the user changed the text since. The model of the text last saved, if any, takes it. Then
	 * deletes the images and the files the user took out of it, as {@link #deleteRemovedImages()}
	 * does.
	 *
	 * @param target
	 *            the Ajax request target
	 * @return {@code this}
	 */
	public T markSaved(AjaxRequestTarget target)
	{
		String text = getModelObject();
		if (savedModel != null)
		{
			savedModel.setObject(text);
		}
		target.appendJavaScript(script("markSaved(" + ScriptJson.quote(text == null ? "" : text) +
			")"));
		return deleteRemovedImages();
	}

	/**
	 * Puts the focus in the editor, where the cursor is.
	 *
	 * @param target
	 *            the Ajax request target
	 * @return {@code this}
	 */
	public T focus(AjaxRequestTarget target)
	{
		target.appendJavaScript(script("focus()"));
		return self();
	}

	/**
	 * @param call
	 *            a call of a method of the editor in the browser, {@code focus()} for example
	 * @return a script calling the editor in the browser, when it is there
	 */
	protected final String script(String call)
	{
		String global = getScriptObject();
		return "(function(){var e=" + global + "&&" + global + ".of(" +
			ScriptJson.quote(getMarkupId()) + ");if(e){e." + call + ";}})();";
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
	public T setFormat(RichTextFormat format)
	{
		this.format = Args.notNull(format, "format");
		return self();
	}

	/**
	 * @return what the text may hold: the features set, or those of the editor by default for its
	 *         format; their names go to the browser, and each renders what it needs there
	 */
	public abstract Set<? extends IRichTextFeature> getFeatures();

	/**
	 * @param feature
	 *            a feature, of this editor's kind or another's
	 * @return whether the editor has a feature of its name: {@link RichTextFeature#IMAGE} and
	 *         CKEditor's feature image alike
	 */
	protected final boolean hasFeature(IRichTextFeature feature)
	{
		String name = feature.getName();
		return getFeatures().stream().anyMatch(each -> each.getName().equals(name));
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
	public T setImageStore(IRichTextImageStore imageStore)
	{
		this.imageStore = imageStore;
		if (imageStore != null && imagesRemoval == null)
		{
			imagesRemoval = new ImagesRemoval();
			add(imagesRemoval);
		}
		if (imageStore != null && imageUpload == null && uploadResourcePath == null)
		{
			imageUpload = new RichTextImageUploadBehavior(new EditorImageStore())
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean isUploadAllowed()
				{
					return isImageUploading() && uploadResourcePath == null;
				}
			};
			imageUpload.setMaxSize(imageMaxSize).setTypes(imageTypes)
				.setMaxDimensions(imageMaxDimensions).setPermanentImages(permanentImages)
				.setScanner(uploadScanner);
			add(imageUpload);
		}
		return self();
	}

	/**
	 * @return the behavior taking the images the user uploads, {@code null} until the editor has a
	 *         store, and while the images go to {@link #setUploadResource(RichTextFiles) the
	 *         resource of the uploads}
	 * @since 10.11.0
	 */
	public RichTextImageUploadBehavior getImageUploadBehavior()
	{
		return imageUpload;
	}

	/**
	 * @return the behavior taking the files the user attaches, {@code null} until the editor has a
	 *         store of files, and while the files go to {@link #setUploadResource(RichTextFiles) the
	 *         resource of the uploads}
	 * @since 10.11.0
	 */
	public RichTextAttachmentUploadBehavior getAttachmentUploadBehavior()
	{
		return attachmentUpload;
	}

	/**
	 * @return the resource of the uploads the images go to, {@code null} for the editor's own
	 *         behavior
	 * @since 10.11.0
	 */
	public RichTextFiles getUploadResource()
	{
		return uploadResourcePath == null ? null : RichTextFiles.get(uploadResourcePath);
	}

	/**
	 * @param uploadResource
	 *            the resource of the uploads of the application the images go to, mounted once,
	 *            not tied to the page: its store keeps them, which the editor deletes and finds them
	 *            by too, and serves them; {@code null} for a behavior of the editor's own, the page
	 *            the context, with {@link #setImageStore(IRichTextImageStore) the store}, as by
	 *            default. The editor keeps its own rules there, its limits and its types, which it
	 *            registers in the session as it renders, and deletes only the images it took.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T setUploadResource(RichTextFiles uploadResource)
	{
		uploadResourcePath = uploadResource == null ? null : uploadResource.getPath();
		if (uploadResource != null && uploadResource.getAttachmentStore() != null)
		{
			setAttachmentStore(uploadResource.getAttachmentStore());
		}
		else if (attachmentStore != null)
		{
			setAttachmentStore(attachmentStore);
		}
		return setImageStore(uploadResource == null ? imageStore : uploadResource.getImageStore());
	}

	/**
	 * @return what keeps the files the user attaches, {@code null} for none
	 * @since 10.11.0
	 */
	public IRichTextAttachmentStore getAttachmentStore()
	{
		return attachmentStore;
	}

	/**
	 * @param attachmentStore
	 *            what keeps the files the user attaches to the text, PDFs or spreadsheets, which go
	 *            in as links, and tells their addresses; {@code null} for none, as by default. With
	 *            {@link #setUploadResource(RichTextFiles) the resource of the uploads} taking files,
	 *            the resource's.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T setAttachmentStore(IRichTextAttachmentStore attachmentStore)
	{
		this.attachmentStore = attachmentStore;
		if (attachmentStore != null && imagesRemoval == null)
		{
			imagesRemoval = new ImagesRemoval();
			add(imagesRemoval);
		}
		if (attachmentStore != null && attachmentUpload == null && !isAttachmentResource())
		{
			attachmentUpload = new RichTextAttachmentUploadBehavior(new EditorAttachmentStore())
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean isUploadAllowed()
				{
					return isAttachmentUploading() && !isAttachmentResource();
				}
			};
			attachmentUpload.setMaxSize(attachmentMaxSize).setFormats(attachmentFormats)
				.setScanner(uploadScanner);
			add(attachmentUpload);
		}
		return self();
	}

	/**
	 * The store of the editor's behavior of the files: the store the editor has now.
	 */
	private final class EditorAttachmentStore implements IRichTextAttachmentStore
	{
		private static final long serialVersionUID = 1L;

		private IRichTextAttachmentStore current() throws IOException
		{
			if (attachmentStore == null)
			{
				throw new IOException("The editor keeps no files");
			}
			return attachmentStore;
		}

		@Override
		public String store(FileUpload file, IUploadFileFormat format) throws IOException
		{
			return current().store(file, format);
		}

		@Override
		public void delete(String url) throws IOException
		{
			current().delete(url);
		}

		@Override
		public RichTextServedFile open(String name) throws IOException
		{
			return current().open(name);
		}
	}

	/**
	 * @return whether the files go to the resource of the uploads: the editor has one, which takes
	 *         files
	 */
	private boolean isAttachmentResource()
	{
		return uploadResourcePath != null && getUploadResource().getAttachmentStore() != null;
	}

	/**
	 * @return the largest file the user attaches
	 * @since 10.11.0
	 */
	public Bytes getAttachmentMaxSize()
	{
		return attachmentMaxSize;
	}

	/**
	 * @param attachmentMaxSize
	 *            the largest file the user attaches,
	 *            {@link RichTextAttachmentUploadBehavior#DEFAULT_MAX_SIZE} by default
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T setAttachmentMaxSize(Bytes attachmentMaxSize)
	{
		this.attachmentMaxSize = Args.notNull(attachmentMaxSize, "attachmentMaxSize");
		if (attachmentUpload != null)
		{
			attachmentUpload.setMaxSize(attachmentMaxSize);
		}
		return self();
	}

	/**
	 * @return the formats of the files the user attaches, in their order
	 * @since 10.11.0
	 */
	public Set<IUploadFileFormat> getAttachmentFormats()
	{
		return Collections.unmodifiableSet(attachmentFormats);
	}

	/**
	 * @param attachmentFormats
	 *            the formats of the files the user attaches, as their bytes say, in the order a file
	 *            is told by and the messages name them: those of {@link UploadFileFormat} and those
	 *            of the application's own ({@link IUploadFileFormat}); all those built in by default
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T setAttachmentFormats(
		Collection<? extends IUploadFileFormat> attachmentFormats)
	{
		this.attachmentFormats = RichTextAttachmentUploads.checkedFormats(attachmentFormats);
		if (attachmentUpload != null)
		{
			attachmentUpload.setFormats(this.attachmentFormats);
		}
		return self();
	}

	/**
	 * @return how many files one drop or choice of the user takes at most, {@code null} for no
	 *         limit
	 * @since 10.11.0
	 */
	public Integer getAttachmentMaxCount()
	{
		return attachmentMaxCount;
	}

	/**
	 * @param attachmentMaxCount
	 *            how many files one drop, choice or paste of the user takes at most, the first ones,
	 *            the others left out with a message; {@code null} for no limit, as by default. A
	 *            limit of the browser alone: each file is a request of its own, which the server
	 *            checks as any other.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T setAttachmentMaxCount(Integer attachmentMaxCount)
	{
		this.attachmentMaxCount = attachmentMaxCount == null ? null :
			Math.max(attachmentMaxCount, 1);
		return self();
	}

	/**
	 * @return whether the form of the tool attaching files opens a dialog to upload them in
	 * @since 10.11.0
	 */
	public boolean isAttachmentUploadDialog()
	{
		return attachmentUploadDialog;
	}

	/**
	 * @param attachmentUploadDialog
	 *            whether the form of the tool attaching files has a button opening a dialog to
	 *            upload them in, with the list of the files as they upload, which the user takes out
	 *            before they go into the text; {@code false}, the default, for the zone in the form
	 *            itself. In CKEditor the tool opens the dialog, or else the browser's dialog of
	 *            files.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T setAttachmentUploadDialog(boolean attachmentUploadDialog)
	{
		this.attachmentUploadDialog = attachmentUploadDialog;
		return self();
	}

	/**
	 * @return what scans each image and each file uploaded before a store keeps it, {@code null}
	 *         for none
	 * @since 10.11.0
	 */
	public IUploadScanner getUploadScanner()
	{
		return uploadScanner;
	}

	/**
	 * @param uploadScanner
	 *            what scans each image and each file the user uploads, before a store keeps it: an
	 *            antivirus, or a sanitizer of the application's own; it accepts the file, refuses
	 *            it with a message in the language of the user, or answers a cleaned copy.
	 *            {@code null} for none, as by default; at {@link #setUploadResource(RichTextFiles)
	 *            the resource of the uploads}, the resource's when the editor has none.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T setUploadScanner(IUploadScanner uploadScanner)
	{
		this.uploadScanner = uploadScanner;
		if (imageUpload != null)
		{
			imageUpload.setScanner(uploadScanner);
		}
		if (attachmentUpload != null)
		{
			attachmentUpload.setScanner(uploadScanner);
		}
		return self();
	}

	/**
	 * @return whether the user attaches files now: the editor has a store of files, and is enabled,
	 *         not read only, with the feature attachment
	 * @since 10.11.0
	 */
	public boolean isAttachmentUploading()
	{
		return attachmentStore != null && !isReadOnly() &&
			hasFeature(RichTextFeature.ATTACHMENT) && isEnabledInHierarchy();
	}

	/**
	 * @return the files the editor took since it was last saved, by its behavior or at the
	 *         resource of the uploads
	 */
	private Set<String> uploadedAttachments()
	{
		if (isAttachmentResource())
		{
			return uploadKey == null ? Set.of() :
				getUploadResource().getUploadedAttachments(uploadKey);
		}
		return attachmentUpload == null ? Set.of() : attachmentUpload.getUploaded();
	}

	/**
	 * @return whether the address of a link of the text is that of a file attached: one the editor
	 *         took since it was last saved, one of the resource of the uploads, or one its store
	 *         keeps ({@link IRichTextAttachmentStore#isStored(String)})
	 */
	private boolean isAttachmentUrl(String url)
	{
		if (uploadedAttachments().contains(url) ||
			isAttachmentResource() && getUploadResource().isFileUrl(url))
		{
			return true;
		}
		IRichTextAttachmentStore store = attachmentStore;
		return store != null && store.isStored(url);
	}

	/**
	 * Forgets files the editor took: they are no longer deleted when the browser asks to.
	 */
	private void forgetAttachments(Collection<String> urls)
	{
		if (isAttachmentResource())
		{
			if (uploadKey != null)
			{
				getUploadResource().forgetAttachments(uploadKey, urls);
			}
		}
		else if (attachmentUpload != null)
		{
			attachmentUpload.forget(urls);
		}
	}

	/**
	 * @return the rules of the files the editor attaches
	 */
	private RichTextAttachmentUploads.Rules attachmentRules()
	{
		return new RichTextAttachmentUploads.Rules(attachmentMaxSize, attachmentFormats,
			isImagesFromSiteOnly(), uploadScanner);
	}

	/**
	 * @return the address the browser posts the files to: of the editor's behavior, or of the
	 *         resource, with the key of its uploads, its rules registered in the session
	 */
	private String attachmentUploadUrl()
	{
		if (!isAttachmentResource())
		{
			return attachmentUpload.getUploadUrl();
		}
		if (uploadKey == null)
		{
			uploadKey = RichTextFiles.newKey();
		}
		return getUploadResource().registerAttachments(uploadKey, attachmentRules(),
			isAttachmentUploading());
	}

	/**
	 * @return the images the editor took since it was last saved, by its behavior or at the
	 *         resource of the uploads
	 */
	private Set<String> uploadedImages()
	{
		if (uploadResourcePath != null)
		{
			return uploadKey == null ? Set.of() : getUploadResource().getUploaded(uploadKey);
		}
		return imageUpload == null ? Set.of() : imageUpload.getUploaded();
	}

	/**
	 * Forgets images the editor took: they are no longer deleted when the browser asks to.
	 */
	private void forgetUploaded(Collection<String> urls)
	{
		if (uploadResourcePath != null)
		{
			if (uploadKey != null)
			{
				getUploadResource().forget(uploadKey, urls);
			}
		}
		else if (imageUpload != null)
		{
			imageUpload.forget(urls);
		}
	}

	/**
	 * @return the rules of the editor's uploads at the resource
	 */
	private RichTextImageUploads.Rules imageRules()
	{
		return new RichTextImageUploads.Rules(imageMaxSize, imageTypes, imageMaxDimensions,
			isImagesFromSiteOnly(), permanentImages && isImageUploading(),
			RichTextImageUploadBehavior.DEFAULT_MAX_FOUND, uploadScanner);
	}

	/**
	 * @return the address the browser posts the images to: of the editor's behavior, or of the
	 *         resource, with the key of its uploads, its rules registered in the session
	 */
	private String imageUploadUrl()
	{
		if (uploadResourcePath == null)
		{
			return imageUpload.getUploadUrl();
		}
		if (uploadKey == null)
		{
			uploadKey = RichTextFiles.newKey();
		}
		return getUploadResource().register(uploadKey, imageRules(), isImageUploading());
	}

	/**
	 * The store of the editor's behavior: the store the editor has now, which the page may change.
	 */
	private final class EditorImageStore implements IRichTextImageStore
	{
		private static final long serialVersionUID = 1L;

		private IRichTextImageStore current() throws IOException
		{
			if (imageStore == null)
			{
				throw new IOException("The editor keeps no images");
			}
			return imageStore;
		}

		@Override
		public String store(FileUpload image, IRichTextImageType type) throws IOException
		{
			return current().store(image, type);
		}

		@Override
		public void delete(String url) throws IOException
		{
			current().delete(url);
		}

		@Override
		public void storePermanently(String url, String name) throws IOException
		{
			current().storePermanently(url, name);
		}

		@Override
		public boolean isPermanent(String url)
		{
			return imageStore != null && imageStore.isPermanent(url);
		}

		@Override
		public List<RichTextStoredImage> findPermanent(String query, int max) throws IOException
		{
			return current().findPermanent(query, max);
		}

		@Override
		public RichTextServedFile open(String name) throws IOException
		{
			return current().open(name);
		}
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
	public T setImageMaxSize(Bytes imageMaxSize)
	{
		this.imageMaxSize = Args.notNull(imageMaxSize, "imageMaxSize");
		if (imageUpload != null)
		{
			imageUpload.setMaxSize(imageMaxSize);
		}
		return self();
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
	public T setImageTypes(Collection<? extends IRichTextImageType> imageTypes)
	{
		this.imageTypes = RichTextImageUploadBehavior.checkedTypes(imageTypes);
		if (imageUpload != null)
		{
			imageUpload.setTypes(this.imageTypes);
		}
		return self();
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
	public T setImageMaxDimensions(RichTextImageDimensions imageMaxDimensions)
	{
		this.imageMaxDimensions = imageMaxDimensions;
		if (imageUpload != null)
		{
			imageUpload.setMaxDimensions(imageMaxDimensions);
		}
		return self();
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
	public T setImageMaxDimensions(int maxWidth, int maxHeight)
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
	public T setImageMaxCount(Integer imageMaxCount)
	{
		this.imageMaxCount = imageMaxCount == null ? null : Math.max(imageMaxCount, 1);
		return self();
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
	 *            going into the text as soon as it is uploaded. In CKEditor the tool
	 *            {@code uploadImages} opens the dialog, in place of CKEditor's own button.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T setImageUploadDialog(boolean imageUploadDialog)
	{
		this.imageUploadDialog = imageUploadDialog;
		return self();
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
	public T setImageScaling(boolean imageScaling)
	{
		this.imageScaling = imageScaling;
		return self();
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
	public T setImageDeletion(RichTextImageDeletion imageDeletion)
	{
		this.imageDeletion = Args.notNull(imageDeletion, "imageDeletion");
		return self();
	}

	/**
	 * @return when the files the user took out of the text are deleted: as
	 *         {@link #getImageDeletion() the images} unless
	 *         {@link #setAttachmentDeletion(RichTextImageDeletion) set}
	 * @since 11.0.0
	 */
	public RichTextImageDeletion getAttachmentDeletion()
	{
		return attachmentDeletion != null ? attachmentDeletion : imageDeletion;
	}

	/**
	 * @param attachmentDeletion
	 *            when the files the user took out of the text are deleted: at once, or once the
	 *            text is saved; {@code null} for as {@link #getImageDeletion() the images}, as by
	 *            default
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public T setAttachmentDeletion(RichTextImageDeletion attachmentDeletion)
	{
		this.attachmentDeletion = attachmentDeletion;
		return self();
	}

	/**
	 * @return whether the user keeps images permanently, and inserts those kept
	 * @since 10.11.0
	 */
	public boolean isPermanentImages()
	{
		return permanentImages;
	}

	/**
	 * @param permanentImages
	 *            whether the user keeps images permanently, by a box to tick for each in the dialog
	 *            of the uploads, or for those dropped on the form of the image, which
	 *            {@link IRichTextImageStore#storePermanently(String, String) the store keeps}; and
	 *            inserts those kept by the tool {@link RichTextTool#STORED_IMAGE insert image from
	 *            store}, which {@link IRichTextImageStore#findPermanent(String, int) the store
	 *            finds}. {@code false} by default. Needs {@link #setImageStore(IRichTextImageStore)
	 *            a store}. In CKEditor the box is in the dialog of the uploads, which its tool
	 *            {@code uploadImages} opens, and the tool {@code storedImage} inserts those kept.
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T setPermanentImages(boolean permanentImages)
	{
		this.permanentImages = permanentImages;
		if (imageUpload != null)
		{
			imageUpload.setPermanentImages(permanentImages);
		}
		return self();
	}

	/**
	 * @return whether the store keeps an image permanently, which the editor never deletes
	 */
	private boolean isPermanent(String url)
	{
		IRichTextImageStore store = imageStore;
		try
		{
			return store != null && store.isPermanent(url);
		}
		catch (RuntimeException e)
		{
			LOG.warn("Could not tell whether the image {} is kept permanently", url, e);
			// kept: an image deleted is gone for good
			return true;
		}
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
	 * Changes the attributes of the request telling the server of the images the user took out of
	 * the text, which the editor's script sends by Wicket's Ajax: the
	 * {@link AjaxRequestAttributes#getExtraParameters() extra parameters} a page veil leaves the
	 * request alone by, or the listeners of the call, for example. By default the attributes of the
	 * requests of the editor's events, {@link #updateEventAjaxAttributes(AjaxRequestAttributes)}: a
	 * page leaving those alone leaves this one alone too.
	 *
	 * @param attributes
	 *            the attributes of the request, POST already, the images going in its body
	 * @since 10.11.0
	 */
	protected void updateImagesRemovalAjaxAttributes(AjaxRequestAttributes attributes)
	{
		updateEventAjaxAttributes(attributes);
	}

	/**
	 * Hears of the images the user took out of the text, by the request the editor's script sends
	 * with the attributes it renders.
	 */
	private final class ImagesRemoval extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			// in the body: the addresses of the images may be many
			attributes.setMethod(AjaxRequestAttributes.Method.POST);
			updateImagesRemovalAjaxAttributes(attributes);
		}

		/**
		 * @return the attributes of its requests, as the editor's script takes them
		 */
		CharSequence renderAttributes()
		{
			return renderAjaxAttributes(getComponent());
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			String images = getRequest().getRequestParameters().getParameterValue(
				RichTextImageUploadBehavior.REMOVED_PARAMETER).toOptionalString();
			if (images != null)
			{
				onImagesRemovedSent(target, RichTextImageUploadBehavior.urlsOf(images));
			}
			String attachments = getRequest().getRequestParameters().getParameterValue(
				ATTACHMENTS_REMOVED_PARAMETER).toOptionalString();
			if (attachments != null)
			{
				onAttachmentsRemovedSent(target, RichTextAttachmentUploads.urlsOf(attachments));
			}
		}
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
		known.addAll(uploadedImages());
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
		urls.stream().filter(known::contains).filter(url -> !isPermanent(url))
			.forEach(removed::add);
		if (removed.isEmpty())
		{
			return;
		}
		boolean now = imageDeletion == RichTextImageDeletion.AT_ONCE;
		if (now)
		{
			deleteImages(Collections.unmodifiableSet(removed));
			imagesShown.removeAll(removed);
			forgetUploaded(removed);
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
	 * @return the files attached to the text of the model, as its format writes them
	 */
	private Set<String> modelAttachments()
	{
		return RichTextImageAddresses.attachmentsOf(getModelObject(), getFormat());
	}

	/**
	 * The browser told of files the user took out of the text: those the editor knows, those of
	 * the text it was shown with and those uploaded since, are deleted now, or once the text is
	 * saved, then told of.
	 */
	private void onAttachmentsRemovedSent(AjaxRequestTarget target, List<String> urls)
	{
		if (attachmentStore == null || !isAttachmentUploading())
		{
			return;
		}
		Set<String> known = new LinkedHashSet<>(attachmentsShown);
		known.addAll(uploadedAttachments());
		Set<String> removed = new LinkedHashSet<>();
		urls.stream().filter(known::contains).forEach(removed::add);
		if (removed.isEmpty())
		{
			return;
		}
		boolean now = getAttachmentDeletion() == RichTextImageDeletion.AT_ONCE;
		if (now)
		{
			deleteAttachments(Collections.unmodifiableSet(removed));
			attachmentsShown.removeAll(removed);
			forgetAttachments(removed);
		}
		else
		{
			attachmentsRemoved.addAll(removed);
		}
		Set<String> told = Collections.unmodifiableSet(removed);
		onAttachmentsRemoved(target, told);
		send(this, Broadcast.BUBBLE, new RichTextAttachmentsRemovedEvent(this, told, now, target));
	}

	/**
	 * Called when the browser told that the user took files out of the text, those the editor
	 * knows, after they were deleted in {@link RichTextImageDeletion#AT_ONCE}, before the
	 * {@link RichTextAttachmentsRemovedEvent}. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param urls
	 *            the addresses of the files
	 * @since 10.11.0
	 */
	protected void onAttachmentsRemoved(AjaxRequestTarget target, Set<String> urls)
	{
	}

	/**
	 * Deletes files the user took out of the text, at once or once the text is saved, as
	 * {@link #getAttachmentDeletion() the mode} says: each by
	 * {@link IRichTextAttachmentStore#delete(String)} by default. A page overrides it to decide
	 * which, knowing its texts.
	 *
	 * @param urls
	 *            the addresses of the files, those of the text the editor was shown with and those
	 *            uploaded into it, never others
	 * @since 10.11.0
	 */
	protected void deleteAttachments(Set<String> urls)
	{
		IRichTextAttachmentStore store = attachmentStore;
		if (store != null)
		{
			urls.forEach(url -> RichTextAttachmentUploads.delete(store, url));
		}
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
	 * Deletes, by {@link #deleteImages(Set)}, the images the user took out of the text since it was
	 * last saved, and those uploaded since that it does not hold: the text of the model is saved
	 * without them. {@link #markSaved(AjaxRequestTarget)} calls it; call it once the text is saved
	 * otherwise, by a form sent without Ajax for example. A text the page put in the model since
	 * the editor was shown, not the user's, deletes nothing.
	 *
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T deleteRemovedImages()
	{
		String text = getModelObject();
		boolean users = hash(text) == shownHash;
		if (attachmentStore != null)
		{
			Set<String> saved = modelAttachments();
			Set<String> gone = new LinkedHashSet<>();
			Set<String> uploaded = uploadedAttachments();
			if (users)
			{
				gone.addAll(attachmentsRemoved);
				gone.addAll(uploaded);
				gone.removeAll(saved);
			}
			forgetAttachments(uploaded);
			attachmentsRemoved.clear();
			attachmentsShown = saved;
			if (!gone.isEmpty())
			{
				deleteAttachments(Collections.unmodifiableSet(gone));
			}
		}
		if (imageStore == null || imageUpload == null && uploadResourcePath == null)
		{
			shownHash = hash(text);
			return self();
		}
		Set<String> saved = modelImages();
		Set<String> gone = new LinkedHashSet<>();
		Set<String> uploaded = uploadedImages();
		if (users)
		{
			gone.addAll(imagesRemoved);
			gone.addAll(uploaded);
			gone.removeAll(saved);
			gone.removeIf(this::isPermanent);
		}
		forgetUploaded(uploaded);
		imagesRemoved.clear();
		imagesShown = saved;
		shownHash = hash(text);
		if (!gone.isEmpty())
		{
			deleteImages(Collections.unmodifiableSet(gone));
		}
		return self();
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
			attachmentsRemoved.clear();
		}
		imagesShown = modelImages();
		attachmentsShown = modelAttachments();
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
		return !isReadOnly() && hasFeature(RichTextFeature.IMAGE) &&
			isEnabledInHierarchy();
	}

	/**
	 * @return whether the user uploads images now: the editor has a store, and is enabled, not read
	 *         only, with the feature image
	 */
	public boolean isImageUploading()
	{
		return imageStore != null && isImageEditing();
	}

	/**
	 * Puts the options of the uploads, those the editors share: while the user uploads images, where
	 * they go, their limits, the dialog, the scaling, the permanent images and where the browser
	 * tells of those taken out; while the user attaches files, the same of the files.
	 *
	 * @param options
	 *            the options the editor is attached with
	 */
	private void putUploadOptions(JSONObject options)
	{
		if (isImageUploading())
		{
			options.put("uploadUrl", imageUploadUrl());
			options.put("uploadTypes", new JSONArray(imageRules().mimeTypes()));
			options.put("uploadMaxSize", uploadResourcePath == null ? imageMaxSize.bytes() :
				Math.min(imageMaxSize.bytes(), getUploadResource().getImageMaxSize().bytes()));
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
			if (permanentImages)
			{
				options.put("permanentImages", true);
			}
			showImages();
			// the attributes of the request telling of the images taken out: the page's extra
			// parameters go with it
			options.put("imagesAjax", new JSONFunction(imagesRemoval.renderAttributes()));
		}
		else if (uploadKey != null)
		{
			// the resource takes no more uploads of the editor, read only now for example
			imageUploadUrl();
		}
		if (isAttachmentUploading())
		{
			options.put("attachUrl", attachmentUploadUrl());
			options.put("attachTypes", attachmentRules().toJson());
			options.put("attachMaxSize", isAttachmentResource() ? Math.min(
				attachmentMaxSize.bytes(), getUploadResource().getAttachmentMaxSize().bytes()) :
				attachmentMaxSize.bytes());
			if (attachmentMaxCount != null)
			{
				options.put("attachMaxCount", attachmentMaxCount.intValue());
			}
			if (attachmentUploadDialog)
			{
				options.put("attachDialog", true);
			}
			showImages();
			if (!options.has("imagesAjax"))
			{
				options.put("imagesAjax", new JSONFunction(imagesRemoval.renderAttributes()));
			}
			if (getFormat() == RichTextFormat.MARKDOWN)
			{
				// a file is a plain link in Markdown: the links to the files come back as files
				JSONArray urls = new JSONArray();
				attachmentsShown.stream().filter(this::isAttachmentUrl).forEach(urls::put);
				if (urls.length() > 0)
				{
					options.put("attachmentUrls", urls);
				}
			}
		}
		else if (uploadKey != null && isAttachmentResource())
		{
			// the resource takes no more files of the editor
			attachmentUploadUrl();
		}
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
	public T setHeadingLevels(int... levels)
	{
		List<Integer> list = new ArrayList<>();
		Arrays.stream(levels).filter(level -> level >= 1 && level <= 6).distinct().sorted()
			.forEach(list::add);
		this.headingLevels = List.copyOf(list);
		return self();
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
	public T setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
		return self();
	}

	/**
	 * @return the model of the text shown while the editor is empty, {@code null} for none
	 */
	public IModel<String> getPlaceholder()
	{
		return placeholder;
	}

	/**
	 * @param placeholder
	 *            the text shown while the editor is empty, {@code null} for none
	 * @return {@code this}
	 */
	public T setPlaceholder(IModel<String> placeholder)
	{
		this.placeholder = placeholder;
		return self();
	}

	/**
	 * @return whether the browser checks the spelling of the text, {@code null} for its default
	 */
	public Boolean getSpellcheck()
	{
		return spellcheck;
	}

	/**
	 * @param spellcheck
	 *            whether the browser checks the spelling of the text, underlining the words its
	 *            dictionaries do not know; {@code null}, the default, leaves it to the browser, which
	 *            mostly does
	 * @return {@code this}
	 */
	public T setSpellcheck(Boolean spellcheck)
	{
		this.spellcheck = spellcheck;
		return self();
	}

	/**
	 * @return the language of the text, {@code null} for that of the page
	 */
	public Locale getTextLanguage()
	{
		return textLanguage;
	}

	/**
	 * @param textLanguage
	 *            the language of the text, which the text gets as its {@code lang}: the browser
	 *            checks its spelling in it; {@code null}, the default, for that of the page. The
	 *            editor's own texts stay in the language of the page.
	 * @return {@code this}
	 */
	public T setTextLanguage(Locale textLanguage)
	{
		this.textLanguage = textLanguage;
		return self();
	}

	/**
	 * @return the language of the text as a tag of BCP 47, {@code en-GB} for example; {@code null}
	 *         for that of the page
	 */
	protected String getTextLanguageTag()
	{
		String tag = textLanguage == null ? null : textLanguage.toLanguageTag();
		return tag == null || "und".equals(tag) ? null : tag;
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
	 *            {@code false} by default
	 * @return {@code this}
	 */
	public T setCount(boolean count)
	{
		this.count = count;
		return self();
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
	public T setResizable(ResizeAxis resizable)
	{
		this.resizable = resizable;
		return self();
	}

	/**
	 * @param minHeight
	 *            the smallest height the user gives the editor, in pixels; {@code null} for
	 *            {@value #DEFAULT_MIN_HEIGHT}
	 * @return {@code this}
	 */
	public T setMinHeight(Integer minHeight)
	{
		this.minHeight = minHeight;
		return self();
	}

	/**
	 * @param maxHeight
	 *            the largest height the user gives the editor, in pixels; {@code null} for none
	 * @return {@code this}
	 */
	public T setMaxHeight(Integer maxHeight)
	{
		this.maxHeight = maxHeight;
		return self();
	}

	/**
	 * @param minWidth
	 *            the smallest width the user gives the editor, in pixels; {@code null} for 200
	 * @return {@code this}
	 */
	public T setMinWidth(Integer minWidth)
	{
		this.minWidth = minWidth;
		return self();
	}

	/**
	 * @param maxWidth
	 *            the largest width the user gives the editor, in pixels; {@code null} for none
	 * @return {@code this}
	 */
	public T setMaxWidth(Integer maxWidth)
	{
		this.maxWidth = maxWidth;
		return self();
	}

	/**
	 * @return what cleans the HTML: the one set, or the {@link #getDefaultSanitizer() policy of the
	 *         features}
	 */
	public IRichTextSanitizer getSanitizer()
	{
		return sanitizer != null ? sanitizer : getDefaultSanitizer();
	}

	/**
	 * @param sanitizer
	 *            what cleans the HTML the browser sends, before the model gets it; {@code null}
	 *            for the policy of the features, as by default. HTML of the page's own, an
	 *            extension's or a plugin's, needs one letting it through.
	 * @return {@code this}
	 */
	public T setSanitizer(IRichTextSanitizer sanitizer)
	{
		this.sanitizer = sanitizer;
		return self();
	}

	/**
	 * @return whether the editor keeps the images of the site alone
	 * @see #setImagesFromSiteOnly(boolean)
	 * @since 10.11.0
	 */
	public boolean isImagesFromSiteOnly()
	{
		return imagesFromSiteOnly;
	}

	/**
	 * Keeps the images of the site alone, or not, as by default: the default sanitizers of the
	 * editor take out an image whose address is not {@link RichTextPolicy#isSiteUrl(String) the
	 * site's own}, a relative one, in HTML ({@link RichTextPolicy#withImagesFromSiteOnly(boolean)}),
	 * in Markdown ({@link RichTextMarkdownSanitizer#IMAGES_FROM_SITE_ONLY}) and in Tiptap's JSON
	 * ({@link RichTextJsonSanitizer#IMAGES_FROM_SITE_ONLY}). An image from another site shows who
	 * reads the text to that site, a pixel tracking the readers for example. Links go to other
	 * sites as before. The addresses below the base address of an upload resource of the
	 * application, on a domain of its own, are the site's own too ({@link RichTextFiles#isOwnUrl}).
	 * <p>
	 * A sanitizer {@link #setSanitizer(IRichTextSanitizer) the page sets} cleans as it does; give
	 * it the same setting. The editor in the browser still takes an image of another site, which
	 * goes once the text reaches the server; the
	 * {@link RichTextEditor#setImageStore(IRichTextImageStore) store} of the uploaded images must
	 * answer relative addresses. A content security policy of the page with {@code img-src 'self'}
	 * keeps the browser from loading such images, shown before the cleaning or saved before the
	 * setting.
	 *
	 * @param imagesFromSiteOnly
	 *            whether the editor keeps the images of the site alone; {@code false} by default
	 * @return {@code this}
	 * @since 10.11.0
	 */
	public T setImagesFromSiteOnly(boolean imagesFromSiteOnly)
	{
		this.imagesFromSiteOnly = imagesFromSiteOnly;
		return self();
	}

	/**
	 * @return what the editor does when the cleaning removed something: the mode set, or that of
	 *         the application, {@link RichTextCleaning#getDefault()}
	 */
	public RichTextCleaning getCleaning()
	{
		return cleaning != null ? cleaning : RichTextCleaning.getDefault();
	}

	/**
	 * @param cleaning
	 *            what the editor does when the cleaning of the text the browser sent removed
	 *            something: {@link RichTextCleaning#SILENT}, {@link RichTextCleaning#REPORT} or
	 *            {@link RichTextCleaning#REJECT}; {@code null} for that of the application, as by
	 *            default
	 * @return {@code this}
	 */
	public T setCleaning(RichTextCleaning cleaning)
	{
		this.cleaning = cleaning;
		return self();
	}

	/**
	 * @return whether the editor tells the user, under its toolbar, what a paste lost
	 */
	public boolean isPasteNotice()
	{
		return pasteNotice;
	}

	/**
	 * @param pasteNotice
	 *            whether the editor tells the user, in a polite line under its toolbar, what the
	 *            cleaning in the browser left out of a paste, until the next change of the text;
	 *            {@code false} by default. The event {@link RichTextEditorEvents#PASTE_CLEANED}
	 *            fires either way.
	 * @return {@code this}
	 */
	public T setPasteNotice(boolean pasteNotice)
	{
		this.pasteNotice = pasteNotice;
		return self();
	}

	/**
	 * @return what cleans the Markdown: the one set, or {@link RichTextMarkdownSanitizer}, which
	 *         takes out the addresses of links and images that are not safe but leaves HTML in the
	 *         Markdown as it is, and those of images not the site's own if
	 *         {@link #setImagesFromSiteOnly(boolean) asked}
	 */
	public IRichTextSanitizer getMarkdownSanitizer()
	{
		if (markdownSanitizer != null)
		{
			return markdownSanitizer;
		}
		return imagesFromSiteOnly ? RichTextMarkdownSanitizer.IMAGES_FROM_SITE_ONLY :
			RichTextMarkdownSanitizer.INSTANCE;
	}

	/**
	 * @param markdownSanitizer
	 *            what cleans the Markdown the browser sends, in {@link RichTextFormat#MARKDOWN},
	 *            before the model gets it; {@code null} for {@link RichTextMarkdownSanitizer}, as by
	 *            default. One that parses the Markdown, with the renderer of the application for
	 *            example, may clean the HTML in it too.
	 * @return {@code this}
	 */
	public T setMarkdownSanitizer(IRichTextSanitizer markdownSanitizer)
	{
		this.markdownSanitizer = markdownSanitizer;
		return self();
	}

	/**
	 * @return what cleans Tiptap's JSON: the one set, or {@link RichTextJsonSanitizer}, keeping the
	 *         images of the site alone if {@link #setImagesFromSiteOnly(boolean) asked}
	 */
	public IRichTextSanitizer getJsonSanitizer()
	{
		if (jsonSanitizer != null)
		{
			return jsonSanitizer;
		}
		return imagesFromSiteOnly ? RichTextJsonSanitizer.IMAGES_FROM_SITE_ONLY :
			RichTextJsonSanitizer.INSTANCE;
	}

	/**
	 * @param jsonSanitizer
	 *            what cleans the JSON the browser sends, in {@link RichTextFormat#JSON}, before the
	 *            model gets it; {@code null} for {@link RichTextJsonSanitizer}, as by default
	 * @return {@code this}
	 */
	public T setJsonSanitizer(IRichTextSanitizer jsonSanitizer)
	{
		this.jsonSanitizer = jsonSanitizer;
		return self();
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		// also for a tag without a body, which the text area does not check
		checkComponentTag(tag, "textarea");
		super.onComponentTag(tag);
		tag.append("class", getLoadingClass(), " ");
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderEditorHead(response);
		if (isImageUploading() && (imageUploadDialog ||
			imageScaling && imageMaxDimensions != null) ||
			isAttachmentUploading() && attachmentUploadDialog)
		{
			// the dialogs of the uploads
			response.render(CssHeaderItem.forReference(Modal.CSS));
		}
		// after the editor's script, which the scripts of the features register with
		getFeatures().forEach(feature -> feature.renderHead(this, response));
		// the events listen before the editor starts, hearing of its text as it starts too
		events.renderHead(this, response);
		// the options may hold the attributes of Ajax requests, with functions
		response.render(OnDomReadyHeaderItem.forScript(getScriptObject() + ".attach(" +
			ScriptJson.quote(getMarkupId()) + ", " + ScriptJson.withFunctions(getOptions()) +
			");"));
	}

	/**
	 * @return the options the editor is attached with in the browser: those the editors share, the
	 *         names of the features and {@link IRichTextFeature#putOptions(org.apache.wicket.Component,
	 *         JSONObject) their options}, {@link #putEditorOptions(JSONObject) its own}, the texts and
	 *         where the server hears of the state
	 */
	protected JSONObject getOptions()
	{
		RichTextEditorState state = getState();
		JSONArray featureNames = new JSONArray();
		JSONObject featureOptions = new JSONObject();
		getFeatures().forEach(feature -> {
			featureNames.put(feature.getName());
			JSONObject own = new JSONObject();
			feature.putOptions(this, own);
			if (own.length() > 0)
			{
				featureOptions.put(feature.getName(), own);
			}
		});
		JSONObject options = new JSONObject()
			.put("format", format.getName())
			.put("features", featureNames)
			.put("headingLevels", new JSONArray(headingLevels))
			.put("readOnly", readOnly)
			.put("count", count)
			.put("state", state.withSize(resizable == null ? ResizableSize.NONE :
				state.getSize().along(resizable)).toJson());
		if (featureOptions.length() > 0)
		{
			options.put("featureOptions", featureOptions);
		}
		putUploadOptions(options);
		putEditorOptions(options);
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
		if (spellcheck != null)
		{
			options.put("spellcheck", spellcheck.booleanValue());
		}
		String language = getTextLanguageTag();
		if (language != null)
		{
			options.put("textLanguage", language);
		}
		IModel<String> label = getLabel();
		String name = label == null ? null : label.getObject();
		if (!Strings.isEmpty(name))
		{
			options.put("label", name);
		}
		if (pasteNotice)
		{
			options.put("pasteNotice", true);
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
		options.put("texts", getTexts());
		return options;
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
		cleaned = null;
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
	 * The events of a feature, as the editor asked them.
	 *
	 * @param feature
	 *            the feature
	 * @param events
	 *            its events
	 */
	private record FeatureEvents(IRichTextFeature feature, List<IClientEvent> events)
		implements IClusterable
	{
	}
}
