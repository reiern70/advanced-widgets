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
package com.antiliacode.widgets.codeeditor;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;

import com.github.openjson.JSONObject;

import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;

/**
 * A preview of Markdown: the text as a page shows it, with GitHub's tables, task lists and
 * strikethrough, its fenced code blocks highlighted by the grammars of the {@link CodeEditor}, in
 * the colors of the {@link com.antiliacode.widgets.theme.Theme theme} it is in. Attach it to a
 * {@code <div>}.
 * <p>
 * Following a {@link CodeEditor}, the preview shows its text and renders it again in the browser,
 * a frame later, each time it changes, without asking the server; the editor may be rendered
 * again via Ajax, and the preview follows it. Given a model instead, it shows the model's text.
 * <p>
 * {@link #setScrollWithEditor(boolean) Scrolling with the editor}, the preview keeps in step with
 * it: as the user moves the cursor, types or scrolls the editor, the preview scrolls, in the
 * browser, to the part matching the place in the editor; not while either is hidden, in a
 * collapsed pane of a {@link com.antiliacode.widgets.split.SplitPanel} for example.
 * <p>
 * {@code Wicket.MarkdownPreview} from {@link CodeEditor#JS} renders the text with the grammar of
 * the editor, an element at a time, never parsing HTML: HTML in the text shows as text, and links
 * and pictures take only web addresses, those of the application, http, https and mailto, and
 * pictures in data: addresses. So the preview shows safely what anyone wrote, and works under a
 * strict Content Security Policy. Links open in a new tab, but for those to places in the page.
 * The code blocks of a {@link CodeEditorLanguage#of language of the page's own} are highlighted
 * once its script is in the page, which an editor showing the language renders. Without
 * JavaScript the preview is empty.
 * <p>
 * The preview is a region of the page, named by its {@link #setLabel(IModel) label}, which takes
 * the focus so that the keyboard scrolls it; give it a height to scroll. The look comes from
 * {@link CodeEditor#CSS}.
 *
 * @since 11.0.0
 */
public class MarkdownPreview extends WebMarkupContainer
{
	private static final long serialVersionUID = 1L;

	private final CodeEditor editor;

	private IModel<String> label;

	private IModel<String> placeholder;

	private boolean scrollWithEditor;

	/**
	 * A preview of the text of a model.
	 *
	 * @param id
	 *            component id
	 * @param model
	 *            the text, in Markdown
	 */
	public MarkdownPreview(String id, IModel<String> model)
	{
		super(id, model);
		this.editor = null;
		init();
	}

	/**
	 * A preview following a code editor.
	 *
	 * @param id
	 *            component id
	 * @param editor
	 *            the editor whose text the preview shows
	 */
	public MarkdownPreview(String id, CodeEditor editor)
	{
		super(id);
		this.editor = Args.notNull(editor, "editor");
		init();
	}

	private void init()
	{
		setOutputMarkupId(true);
		add(new LookBehavior(LookPart.MARKDOWN_PREVIEW));
	}

	/**
	 * @return the editor whose text the preview shows, {@code null} for one showing its model's
	 */
	public CodeEditor getEditor()
	{
		return editor;
	}

	/**
	 * @return the text shown: the editor's, as the user left it, or the model's
	 */
	public String getText()
	{
		if (editor != null)
		{
			// as the user left it, not escaped as markup
			String value = editor.hasRawInput() ? editor.getRawInput() : editor.getModelObject();
			return value == null ? "" : value;
		}
		Object text = getDefaultModelObject();
		return text == null ? "" : text.toString();
	}

	/**
	 * @return the accessible name of the preview, a region of the page; "Preview" in the
	 *         language of the user when not set
	 */
	public IModel<String> getLabel()
	{
		return label;
	}

	/**
	 * @param label
	 *            the accessible name of the preview, {@code null} for "Preview" in the language
	 *            of the user
	 * @return {@code this}
	 */
	public MarkdownPreview setLabel(IModel<String> label)
	{
		this.label = wrap(label);
		return this;
	}

	/**
	 * @return what the preview says while the text is empty; "Nothing to preview" in the
	 *         language of the user when not set
	 */
	public IModel<String> getPlaceholder()
	{
		return placeholder;
	}

	/**
	 * @param placeholder
	 *            what the preview says while the text is empty, {@code null} for "Nothing to
	 *            preview" in the language of the user; empty for nothing
	 * @return {@code this}
	 */
	public MarkdownPreview setPlaceholder(IModel<String> placeholder)
	{
		this.placeholder = wrap(placeholder);
		return this;
	}

	/**
	 * @return whether the preview keeps in step with its editor
	 */
	public boolean isScrollWithEditor()
	{
		return scrollWithEditor;
	}

	/**
	 * @param scrollWithEditor
	 *            whether the preview keeps in step with its editor: as the user moves the cursor,
	 *            types or scrolls the editor, the preview scrolls to the part matching the place
	 *            in the editor; {@code false} by default, each scrolling on its own. For a preview
	 *            following an editor only.
	 * @return {@code this}
	 */
	public MarkdownPreview setScrollWithEditor(boolean scrollWithEditor)
	{
		this.scrollWithEditor = scrollWithEditor;
		return this;
	}

	/**
	 * Keeps the preview in step with its editor, or not, from the server: tells the preview in the
	 * browser, without rendering it again, so that it stays where the user scrolled it. Kept in
	 * step, it comes in step at once.
	 *
	 * @param scrollWithEditor
	 *            whether the preview keeps in step with its editor
	 * @param target
	 *            the Ajax request target
	 * @return {@code this}
	 * @see #setScrollWithEditor(boolean)
	 */
	public MarkdownPreview setScrollWithEditor(boolean scrollWithEditor, AjaxRequestTarget target)
	{
		setScrollWithEditor(scrollWithEditor);
		target.appendJavaScript("(function(){var p=Wicket.MarkdownPreview&&" +
			"Wicket.MarkdownPreview.of(" + JSONObject.quote(getMarkupId()) + ");if(p){p.configure(" +
			new JSONObject().put("scrollWithEditor", scrollWithEditor) + ");}})();");
		return this;
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		// a region the keyboard scrolls, as the script makes it
		tag.put("tabindex", "0");
	}

	@Override
	public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		// the script renders the text
		replaceComponentTagBody(markupStream, openTag, "");
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		CodeEditor.renderHeadItems(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.MarkdownPreview.attach(" +
			JSONObject.quote(getMarkupId()) + ", " + getOptions() + ");"));
	}

	/**
	 * @return the options of {@code Wicket.MarkdownPreview.attach}
	 */
	JSONObject getOptions()
	{
		JSONObject options = new JSONObject().put("value", getText())
			.put("label", text(label, "MarkdownPreview.label", "Preview"))
			.put("placeholder", text(placeholder, "MarkdownPreview.placeholder",
				"Nothing to preview"));
		if (editor != null)
		{
			options.put("editor", editor.getMarkupId()).put("scrollWithEditor", scrollWithEditor);
		}
		return options;
	}

	private String text(IModel<String> model, String key, String fallback)
	{
		if (model != null)
		{
			String text = model.getObject();
			return Strings.isEmpty(text) ? "" : text;
		}
		return getString(key, null, fallback);
	}

	@Override
	protected void onDetach()
	{
		super.onDetach();
		if (label != null)
		{
			label.detach();
		}
		if (placeholder != null)
		{
			placeholder.detach();
		}
	}
}
