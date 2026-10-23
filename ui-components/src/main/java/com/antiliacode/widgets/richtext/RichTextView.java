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

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;

/**
 * The HTML of a {@link RichTextEditor} as a page shows it, read only: its headings, lists, quotes,
 * code, tables and alignment drawn as in the editor, by {@link RichTextEditor#CSS}. Attach it to a
 * {@code <div>}, whose body it replaces.
 * <p>
 * The HTML is {@link #setSanitizer(RichTextSanitizer) cleaned} before it is written into the page,
 * with {@link RichTextPolicy#ALL} by default, whatever the model holds: HTML saved before the
 * editor cleaned it, or from somewhere else, shows no script.
 *
 * @since 11.0.0
 */
public class RichTextView extends WebComponent
{
	private static final long serialVersionUID = 1L;

	private RichTextSanitizer sanitizer = RichTextPolicy.ALL;

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the HTML
	 */
	public RichTextView(String id, IModel<String> model)
	{
		super(id, model);
		add(new LookBehavior(LookPart.RICH_TEXT_VIEW));
	}

	/**
	 * @return what cleans the HTML before the page shows it
	 */
	public RichTextSanitizer getSanitizer()
	{
		return sanitizer;
	}

	/**
	 * @param sanitizer
	 *            what cleans the HTML before the page shows it: that of the editor the HTML comes
	 *            from, for one keeping what its features write
	 * @return {@code this}
	 */
	public RichTextView setSanitizer(RichTextSanitizer sanitizer)
	{
		this.sanitizer = Args.notNull(sanitizer, "sanitizer");
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
			tag.setType(org.apache.wicket.markup.parser.XmlTag.TagType.OPEN);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(RichTextEditor.CSS));
	}
}
