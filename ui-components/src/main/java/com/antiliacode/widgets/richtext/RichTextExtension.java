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

import java.io.Serializable;
import java.util.Locale;

import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

/**
 * An extension of a {@link RichTextEditor}, of Tiptap, which a script of the page's own
 * registers in the browser under the extension's name, before the editor starts:
 *
 * <pre>
 * Wicket.RichTextEditor.registerExtension('highlight', function (kit) {
 *     return kit.Mark.create({
 *         name: 'highlight',
 *         parseHTML: function () { return [{ tag: 'mark' }]; },
 *         renderHTML: function (props) { return ['mark', props.HTMLAttributes, 0]; }
 *     });
 * });
 * </pre>
 *
 * The kit holds the editor's classes of Tiptap and ProseMirror ({@code Extension}, {@code Node},
 * {@code Mark}, {@code mergeAttributes}, the input rules, {@code Plugin}...), so a page needs none of
 * its own. An extension writing elements of its own into the HTML needs a
 * {@link RichTextEditor#setSanitizer(RichTextSanitizer) cleaning} letting them through.
 *
 * @since 11.0.0
 */
public final class RichTextExtension implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String name;

	private final ResourceReference script;

	private RichTextExtension(String name, ResourceReference script)
	{
		this.name = Args.notEmpty(name, "name").trim().toLowerCase(Locale.ROOT);
		this.script = script;
	}

	/**
	 * @param name
	 *            the name the script registers the extension under; the case does not matter
	 * @param script
	 *            the script registering it, which the page renders before the editor; {@code null}
	 *            for one the page renders itself
	 * @return the extension
	 */
	public static RichTextExtension of(String name, ResourceReference script)
	{
		return new RichTextExtension(name, script);
	}

	/**
	 * @return the name of the extension, in lower case
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the script registering the extension, {@code null} for none
	 */
	public ResourceReference getScript()
	{
		return script;
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof RichTextExtension extension && extension.name.equals(name);
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public String toString()
	{
		return name;
	}
}
