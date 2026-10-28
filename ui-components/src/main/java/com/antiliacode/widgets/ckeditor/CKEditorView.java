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

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.richtext.IRichTextSanitizer;
import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;

/**
 * The HTML of a {@link CKEditor} as a page shows it, read only, as in the editor: its element has
 * the class {@code ck-content}, which the style sheet of the text of the
 * {@link CKEditorLibrary library}, {@code ckeditor5-content.css}, styles, and the colors of the
 * theme come from {@link CKEditor#CSS}. Attach it to a {@code <div>}, whose body it replaces.
 * <p>
 * The HTML is {@link #setSanitizer(IRichTextSanitizer) cleaned} before it is written into the page,
 * with {@link CKEditorPolicy#ALL} by default, whatever the model holds: HTML saved before the
 * editor cleaned it, or from somewhere else, shows no script.
 *
 * @since 11.0.0
 */
public class CKEditorView extends WebComponent
{
	private static final long serialVersionUID = 1L;

	private IRichTextSanitizer sanitizer = CKEditorPolicy.ALL;

	private CKEditorLibrary library;

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the HTML
	 */
	public CKEditorView(String id, IModel<String> model)
	{
		super(id, model);
		add(new LookBehavior(LookPart.CK_EDITOR_VIEW));
	}

	/**
	 * @return what cleans the HTML before the page shows it
	 */
	public IRichTextSanitizer getSanitizer()
	{
		return sanitizer;
	}

	/**
	 * @param sanitizer
	 *            what cleans the HTML before the page shows it: that of the editor the HTML comes
	 *            from, for one keeping what its features write
	 * @return {@code this}
	 */
	public CKEditorView setSanitizer(IRichTextSanitizer sanitizer)
	{
		this.sanitizer = Args.notNull(sanitizer, "sanitizer");
		return this;
	}

	/**
	 * @return the library whose style sheet of the text the view renders: the one set, the
	 *         application's, or {@code null} for none
	 */
	public CKEditorLibrary getLibrary()
	{
		if (library != null)
		{
			return library;
		}
		return CKEditorLibrary.isSet(getApplication()) ? CKEditorLibrary.get(getApplication()) :
			null;
	}

	/**
	 * @param library
	 *            CKEditor as this view has it, for its style sheet of the text; {@code null} for
	 *            the application's, as by default
	 * @return {@code this}
	 */
	public CKEditorView setLibrary(CKEditorLibrary library)
	{
		this.library = library;
		return this;
	}

	/**
	 * @return the HTML of the model, cleaned; empty for none
	 */
	public String getHtml()
	{
		Object html = getDefaultModelObject();
		String clean = html == null ? null : sanitizer.sanitize(html.toString());
		return clean == null ? "" : clean;
	}

	@Override
	public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		replaceComponentTagBody(markupStream, openTag, getHtml());
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		if (tag.isOpenClose())
		{
			tag.setType(XmlTag.TagType.OPEN);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		CKEditorLibrary library = getLibrary();
		if (library != null && library.getContentCss() != null)
		{
			response.render(CssHeaderItem.forReference(library.getContentCss()));
		}
		response.render(CssHeaderItem.forReference(CKEditor.CSS));
	}
}
