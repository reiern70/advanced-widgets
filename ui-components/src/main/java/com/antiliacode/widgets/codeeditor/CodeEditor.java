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
package com.antiliacode.widgets.codeeditor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.csp.ContentSecurityPolicySettings;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.string.Strings;

import com.github.openjson.JSONObject;

import com.antiliacode.widgets.resizable.ResizableBehavior;
import com.antiliacode.widgets.resizable.ResizableSize;
import com.antiliacode.widgets.resizable.ResizeAxis;
import com.antiliacode.widgets.resource.AlreadyCompressedJavaScriptResourceReference;

/**
 * A text area shown as a code editor, CodeMirror, in the colors of the
 * {@link com.antiliacode.widgets.theme.Theme theme} it is in and the look of the page: the grammar of a
 * {@link CodeEditorLanguage language} highlights the text, the lines show their numbers, blocks
 * fold, Ctrl+F searches, Ctrl+Z undoes, brackets close and match, and Ctrl+S (Cmd+S) tells the page
 * to save. Attach it to a {@code <textarea>}.
 * <p>
 * The text area stays in the page, hidden, and holds the text: a form posts it as always, and it
 * fires {@code input} as the user types and {@code change} once the editor loses the focus, as a
 * text area does, so the Ajax behaviors listening to them work as with one. The editor fires its
 * own events too, {@link CodeEditorEvent}, which {@link CodeEditorEventBehavior}s hear of: the
 * focus got and lost, the selection, Ctrl+S, the size changed. Without JavaScript the text area is
 * shown.
 * <p>
 * The editor tells the {@link com.antiliacode.widgets.tabs.TabbedPanel tabs} and the
 * {@link com.antiliacode.widgets.desktop.Desktop desktop} it is in whether its text has changes not
 * saved, against the text it was rendered with or {@link #setSavedModel(IModel) the text last
 * saved}, and its tab or its window shows a mark meanwhile; closing them anyway puts the saved text
 * back. {@link #markSaved(AjaxRequestTarget)} tells the browser the text was saved.
 * <p>
 * Where the cursor is, what is selected, how far the text is scrolled and the size the user gave
 * the editor, by the bar along its bottom ({@link #setResizable(ResizeAxis)}), are its
 * {@link CodeEditorState state}, kept in the state model: by default in the editor, and so in the
 * page. The browser sends the server the state, a moment after the user changed it, as the editor
 * loses the focus and, in a beacon, as the page goes ({@link #setSendToServer(boolean)}, on by
 * default), with the text the user changed, which the editor keeps as its input until the form
 * is sent, as a text area keeps what the user typed when the form did not validate. Rendered again,
 * or shown again after the user came back to the page with a state model of the session, the
 * editor takes the state up again: the cursor, the selection, the scrolling and the size. In a tab
 * of {@link com.antiliacode.widgets.tabs.TabbedPanel} not selected, the editor keeps them in the
 * browser.
 * <p>
 * {@code Wicket.CodeEditor} from {@link #JS} makes the editor in the browser; its sizes are CSS
 * custom properties set through the CSSOM. CodeMirror writes its style sheet into the page in a
 * style element, which the nonce of the page lets in under the Content Security Policy of the
 * application, when it has one. The look comes from {@link #CSS}.
 *
 * @since 11.0.0
 */
public class CodeEditor extends TextArea<String>
{
	private static final long serialVersionUID = 1L;

	/**
	 * The script of the editor, {@code Wicket.CodeEditor}, with CodeMirror and its grammars. The
	 * build minifies it already: Wicket's own compressor, which in deployment mode strips the
	 * scripts named without {@code .min.}, would break the regular expressions of the grammars.
	 */
	public static final ResourceReference JS = new AlreadyCompressedJavaScriptResourceReference(
		CodeEditor.class, "antilia-code-editor.js");

	/** The style sheet of the editor. */
	public static final ResourceReference CSS = new CssResourceReference(CodeEditor.class,
		"antilia-code-editor.css");

	/** The height the user resizes the editor from at least, in pixels, unless set otherwise. */
	public static final int DEFAULT_MIN_HEIGHT = 64;

	private static final String STATE_PARAMETER = "state";

	/** CodeMirror's phrases, by the keys of their translations in the bundles. */
	private static final Map<String, String> PHRASES = new LinkedHashMap<>();

	static
	{
		String[][] phrases = { { "Find", "find" }, { "Replace", "replace" }, { "next", "next" },
				{ "previous", "previous" }, { "all", "all" }, { "match case", "matchCase" },
				{ "regexp", "regexp" }, { "by word", "byWord" }, { "replace", "replaceOne" },
				{ "replace all", "replaceAll" }, { "close", "close" },
				{ "current match", "currentMatch" }, { "on line", "onLine" },
				{ "replaced match on line $", "replacedMatch" },
				{ "replaced $ matches", "replacedMatches" }, { "Go to line", "goToLine" },
				{ "go", "go" }, { "Folded lines", "foldedLines" },
				{ "Unfolded lines", "unfoldedLines" }, { "to", "to" },
				{ "folded code", "foldedCode" }, { "unfold", "unfold" },
				{ "Fold line", "foldLine" }, { "Unfold line", "unfoldLine" },
				{ "Selection deleted", "selectionDeleted" },
				{ "Control character", "controlCharacter" } };
		for (String[] phrase : phrases)
		{
			PHRASES.put(phrase[0], "CodeEditor." + phrase[1]);
		}
	}

	private final AbstractDefaultAjaxBehavior stateBehavior;

	private IModel<CodeEditorState> stateModel;

	private IModel<String> savedModel;

	private IModel<String> placeholder;

	private CodeEditorLanguage language;

	private boolean lineNumbers = true;

	private boolean folding = true;

	private boolean readOnly;

	private boolean selectable = true;

	private boolean lineWrapping;

	private int tabSize = 4;

	private ResizeAxis resizable = ResizeAxis.VERTICAL;

	private Integer minHeight;

	private Integer maxHeight;

	private Integer minWidth;

	private Integer maxWidth;

	private boolean sendToServer = true;

	/**
	 * An editor whose model is inherited, for example from a {@code CompoundPropertyModel}, keeping
	 * its state in the page.
	 *
	 * @param id
	 *            component id
	 */
	public CodeEditor(String id)
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
	public CodeEditor(String id, IModel<String> model)
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
	public CodeEditor(String id, IModel<String> model, IModel<CodeEditorState> stateModel)
	{
		super(id, model);
		this.stateModel = stateModel == null ? Model.of((CodeEditorState)null) : stateModel;
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
		CodeEditorState state = CodeEditorState.parse(parameters.getParameterValue(
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
	 * @return {@code false}: the white space of code, the indentation and the lines at its end,
	 *         is part of it
	 */
	@Override
	protected boolean shouldTrimInput()
	{
		return false;
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
	public IModel<CodeEditorState> getStateModel()
	{
		return stateModel;
	}

	/**
	 * @param stateModel
	 *            where the editor keeps the selection, the scrolling and the size; in the editor
	 *            when {@code null}
	 * @return {@code this}
	 */
	public CodeEditor setStateModel(IModel<CodeEditorState> stateModel)
	{
		this.stateModel = stateModel == null ? Model.of((CodeEditorState)null) : stateModel;
		return this;
	}

	/**
	 * @return the selection, the scrolling and the size, as the state model has them;
	 *         {@link CodeEditorState#NONE} while it is empty
	 */
	public CodeEditorState getState()
	{
		CodeEditorState state = stateModel.getObject();
		return state == null ? CodeEditorState.NONE : state;
	}

	/**
	 * @param state
	 *            the state the state model gets, which the editor shows when rendered again
	 * @return {@code this}
	 */
	public CodeEditor setState(CodeEditorState state)
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
	public CodeEditor setSavedModel(IModel<String> savedModel)
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
	public CodeEditor markSaved(AjaxRequestTarget target)
	{
		String text = normalize(getModelObject());
		if (savedModel != null)
		{
			savedModel.setObject(text);
		}
		target.appendJavaScript(script("markSaved(" + JSONObject.quote(text) + ")"));
		return this;
	}

	/**
	 * Puts the focus in the editor, where the cursor is.
	 *
	 * @param target
	 *            the Ajax request target
	 * @return {@code this}
	 */
	public CodeEditor focus(AjaxRequestTarget target)
	{
		target.appendJavaScript(script("focus()"));
		return this;
	}

	/**
	 * @return a script calling the editor in the browser, when it is there
	 */
	private String script(String call)
	{
		return "(function(){var e=Wicket.CodeEditor&&Wicket.CodeEditor.of(" +
			JSONObject.quote(getMarkupId()) + ");if(e){e." + call + ";}})();";
	}

	/**
	 * @return the language of the text, {@code null} for plain text
	 */
	public CodeEditorLanguage getLanguage()
	{
		return language;
	}

	/**
	 * @param language
	 *            the language of the text, whose grammar highlights it; {@code null} for plain
	 *            text, as by default
	 * @return {@code this}
	 */
	public CodeEditor setLanguage(CodeEditorLanguage language)
	{
		this.language = language;
		return this;
	}

	/**
	 * @return whether the lines show their numbers, {@code true} unless set otherwise
	 */
	public boolean isLineNumbers()
	{
		return lineNumbers;
	}

	/**
	 * @param lineNumbers
	 *            whether the lines show their numbers
	 * @return {@code this}
	 */
	public CodeEditor setLineNumbers(boolean lineNumbers)
	{
		this.lineNumbers = lineNumbers;
		return this;
	}

	/**
	 * @return whether blocks fold, {@code true} unless set otherwise
	 */
	public boolean isFolding()
	{
		return folding;
	}

	/**
	 * @param folding
	 *            whether blocks fold, by the arrows in the gutter and Ctrl+Shift+[ and ]
	 * @return {@code this}
	 */
	public CodeEditor setFolding(boolean folding)
	{
		this.folding = folding;
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
	 *            whether the user cannot change the text, but can read, select and copy it
	 * @return {@code this}
	 */
	public CodeEditor setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
		return this;
	}

	/**
	 * @return whether the user can put the cursor in the text and select it, {@code true} unless
	 *         set otherwise
	 */
	public boolean isSelectable()
	{
		return selectable;
	}

	/**
	 * @param selectable
	 *            whether the user can put the cursor in the text and select it; {@code false}
	 *            shows the text as a picture, read only
	 * @return {@code this}
	 */
	public CodeEditor setSelectable(boolean selectable)
	{
		this.selectable = selectable;
		return this;
	}

	/**
	 * @return whether long lines wrap, {@code false} unless set otherwise
	 */
	public boolean isLineWrapping()
	{
		return lineWrapping;
	}

	/**
	 * @param lineWrapping
	 *            whether long lines wrap
	 * @return {@code this}
	 */
	public CodeEditor setLineWrapping(boolean lineWrapping)
	{
		this.lineWrapping = lineWrapping;
		return this;
	}

	/**
	 * @return the width of a tab and of a level of indentation, in spaces
	 */
	public int getTabSize()
	{
		return tabSize;
	}

	/**
	 * @param tabSize
	 *            the width of a tab and of a level of indentation, in spaces, 4 by default
	 * @return {@code this}
	 */
	public CodeEditor setTabSize(int tabSize)
	{
		this.tabSize = Math.min(Math.max(tabSize, 1), 16);
		return this;
	}

	/**
	 * @param placeholder
	 *            the text shown while the editor is empty, {@code null} for none
	 * @return {@code this}
	 */
	public CodeEditor setPlaceholder(IModel<String> placeholder)
	{
		this.placeholder = placeholder;
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
	public CodeEditor setResizable(ResizeAxis resizable)
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
	public CodeEditor setMinHeight(Integer minHeight)
	{
		this.minHeight = minHeight;
		return this;
	}

	/**
	 * @param maxHeight
	 *            the largest height the user gives the editor, in pixels; {@code null} for none
	 * @return {@code this}
	 */
	public CodeEditor setMaxHeight(Integer maxHeight)
	{
		this.maxHeight = maxHeight;
		return this;
	}

	/**
	 * @param minWidth
	 *            the smallest width the user gives the editor, in pixels; {@code null} for 160
	 * @return {@code this}
	 */
	public CodeEditor setMinWidth(Integer minWidth)
	{
		this.minWidth = minWidth;
		return this;
	}

	/**
	 * @param maxWidth
	 *            the largest width the user gives the editor, in pixels; {@code null} for none
	 * @return {@code this}
	 */
	public CodeEditor setMaxWidth(Integer maxWidth)
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
	public CodeEditor setSendToServer(boolean sendToServer)
	{
		this.sendToServer = sendToServer;
		return this;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderHeadItems(response);
		if (language != null && language.getScript() != null)
		{
			response.render(JavaScriptHeaderItem.forReference(language.getScript()));
		}
		response.render(OnDomReadyHeaderItem.forScript("Wicket.CodeEditor.attach(" +
			JSONObject.quote(getMarkupId()) + ", " + getOptions() + ");"));
	}

	/**
	 * @return the options of {@code Wicket.CodeEditor.attach}
	 */
	JSONObject getOptions()
	{
		CodeEditorState state = getState();
		JSONObject options = new JSONObject()
			.put("lineNumbers", lineNumbers)
			.put("folding", folding)
			.put("readOnly", readOnly)
			.put("selectable", selectable)
			.put("lineWrapping", lineWrapping)
			.put("tabSize", tabSize)
			.put("state", state.withSize(resizable == null ? ResizableSize.NONE :
				state.getSize().along(resizable)).toJson());
		if (language != null)
		{
			options.put("language", language.getName());
		}
		if (savedModel != null)
		{
			options.put("saved", normalize(savedModel.getObject()));
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
		options.put("texts", texts(this));
		String nonce = getNonce();
		if (nonce != null)
		{
			options.put("nonce", nonce);
		}
		if (sendToServer)
		{
			options.put("stateUrl", stateBehavior.getCallbackUrl().toString());
			options.put("inputName", getInputName());
		}
		return options;
	}

	/**
	 * What the editor says, in the language of a component: the name of the bar resizing it and
	 * CodeMirror's phrases, those translated. The texts of this editor, and those a page hands to the
	 * React and Svelte editors.
	 *
	 * @param component
	 *            the component whose resources translate the texts
	 * @return the texts, {@code resize} and {@code phrases}
	 */
	public static JSONObject texts(Component component)
	{
		JSONObject phrases = new JSONObject();
		PHRASES.forEach((phrase, key) -> {
			String translation = component.getString(key, null, "");
			if (!Strings.isEmpty(translation) && !translation.equals(phrase))
			{
				phrases.put(phrase, translation);
			}
		});
		return new JSONObject()
			.put("resize", component.getString("CodeEditor.resize", null,
				"Drag, or use the arrow keys, to resize the editor"))
			.put("phrases", phrases);
	}

	/**
	 * @return the nonce of the page under the Content Security Policy of the application, for the
	 *         style element of CodeMirror; {@code null} without one
	 */
	private String getNonce()
	{
		Application application = getApplication();
		if (!(application instanceof WebApplication web))
		{
			return null;
		}
		ContentSecurityPolicySettings csp = web.getCspSettings();
		return csp.isEnabled() && csp.isNonceEnabled() ? csp.getNonce(RequestCycle.get()) : null;
	}

	private static void putPixels(JSONObject options, String name, Integer pixels)
	{
		if (pixels != null)
		{
			options.put(name, pixels.intValue());
		}
	}

	/**
	 * @return the text as the browser has it, its lines separated by a single {@code \n}
	 */
	private static String normalize(String text)
	{
		return text == null ? "" : text.replace("\r\n", "\n");
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
	 * Renders what the editor needs in the browser: {@link #JS} and {@link #CSS}, and the style
	 * sheet of the bar resizing it, {@link ResizableBehavior#CSS}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(CssHeaderItem.forReference(ResizableBehavior.CSS));
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
