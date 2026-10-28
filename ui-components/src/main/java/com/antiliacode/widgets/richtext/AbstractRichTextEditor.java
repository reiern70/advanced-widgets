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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import com.antiliacode.widgets.event.ClientEventsBehavior;
import com.antiliacode.widgets.event.IClientEvent;
import com.antiliacode.widgets.resizable.ResizableSize;
import com.antiliacode.widgets.resizable.ResizeAxis;

/**
 * What the rich text editors share, {@link RichTextEditor}, made with Tiptap, and
 * {@link com.antiliacode.widgets.ckeditor.CKEditor}, made with CKEditor 5, so that a page can
 * switch between them: a text area shown as an editor in the browser, whose model is the text,
 * HTML or Markdown, which a form posts as always.
 * <p>
 * The HTML comes from the browser and is never trusted: the editor
 * {@link #setSanitizer(IRichTextSanitizer) cleans} it before the model gets it, keeping what its
 * {@link #getFeatures() features} write. Each editor has features of its own kind,
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
 * {@link com.antiliacode.widgets.event.IAjaxSubmitEvent}, submitting the form, through the two
 * Ajax behaviors of a {@link ClientEventsBehavior}, whatever the number of events:
 *
 * <pre>
 * editor.addEvent(new AjaxSubmitEvent(RichTextEditorEvents.SAVE)
 * {
 *     public void onEvent(String eventId, JSONObject values, Form&lt;?&gt; form,
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
 * server the state, a moment after the user changed it, as the editor loses the focus and, in a
 * beacon, as the page goes ({@link #setSendToServer(boolean)}, on by default), with the text the
 * user changed, which the editor keeps as its input until the form is sent. Rendered again, or
 * shown again after the user came back to the page with a state model of the session, the editor
 * takes the state up again.
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

	private static final String STATE_PARAMETER = "state";

	private final AbstractDefaultAjaxBehavior stateBehavior;

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

	private boolean sendToServer = true;

	private IRichTextSanitizer sanitizer;

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
		events = new ClientEventsBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public List<IClientEvent> getEvents()
			{
				List<IClientEvent> all = new ArrayList<>(getAddedEvents());
				getFeatures().forEach(feature -> all.addAll(feature.getEvents(
					AbstractRichTextEditor.this)));
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
		};
		add(events);
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
	 *         the policy of the features of the editor
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
	 * @param event
	 *            an event the editor listens to, after those it listens to already: one of
	 *            {@link RichTextEditorEvents}, or of any other name
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             when the event goes to the server both ways, or stays in the browser without a
	 *             function
	 */
	public T addEvent(IClientEvent event)
	{
		events.add(event);
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
	 * @return the events the editor listens to: those it was given, then those of its features
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
	 * the user changed the text since. The model of the text last saved, if any, takes it.
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
		target.appendJavaScript(script("markSaved(" + JSONObject.quote(text == null ? "" : text) +
			")"));
		return self();
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
	public T setSendToServer(boolean sendToServer)
	{
		this.sendToServer = sendToServer;
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
		// after the editor's script, which the scripts of the features register with
		getFeatures().forEach(feature -> feature.renderHead(this, response));
		response.render(OnDomReadyHeaderItem.forScript(getScriptObject() + ".attach(" +
			JSONObject.quote(getMarkupId()) + ", " + getOptions() + ");"));
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
		if (sendToServer)
		{
			options.put("stateUrl", stateBehavior.getCallbackUrl().toString());
			options.put("inputName", getInputName());
		}
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
}
