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
package com.antiliacode.widgets.demo.richtext;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.antiliacode.widgets.richtext.IRichTextFeature;
import com.antiliacode.widgets.richtext.IRichTextTool;
import com.antiliacode.widgets.richtext.RichTextHtmlRules;

/**
 * A feature of the demo's own, which {@link com.antiliacode.widgets.richtext.RichTextEditor} has
 * not: keys of the keyboard, {@code <kbd>Ctrl</kbd>}, which a tool of the toolbar, {@link #TOOL},
 * turns on and off, as Ctrl+Shift+K does. Its script, {@code kbd-feature.js}, registers the mark
 * and the tool in the browser; the cleaning lets {@code kbd} through.
 */
public record KbdFeature() implements IRichTextFeature
{
	private static final long serialVersionUID = 1L;

	/** The feature. */
	public static final KbdFeature KBD = new KbdFeature();

	/** Its tool, which the toolbar names by RichTextEditor.kbd. */
	public static final IRichTextTool TOOL = IRichTextTool.of("kbd", KBD);

	/** The script registering the feature in the browser. */
	private static final ResourceReference SCRIPT = new JavaScriptResourceReference(
		KbdFeature.class, "kbd-feature.js");

	@Override
	public String getName()
	{
		return "kbd";
	}

	/**
	 * @return {@code false}: Markdown has no syntax for keys
	 */
	@Override
	public boolean isInMarkdown()
	{
		return false;
	}

	@Override
	public void allowHtml(RichTextHtmlRules rules)
	{
		rules.allowElements("kbd");
	}

	@Override
	public void renderHead(Component editor, IHeaderResponse response)
	{
		response.render(JavaScriptHeaderItem.forReference(SCRIPT));
	}
}
