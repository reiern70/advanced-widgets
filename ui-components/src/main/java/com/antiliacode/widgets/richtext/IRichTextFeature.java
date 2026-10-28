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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.util.io.IClusterable;

import com.github.openjson.JSONObject;

import com.antiliacode.widgets.event.IClientEvent;

/**
 * What the text of a {@link RichTextEditor} may hold: headings, bold text, links, tables... An
 * editor has the features it is {@link AbstractRichTextEditor#setFeatures(java.util.Collection)
 * given}, those of {@link RichTextFeature} by default; a page adds features of its own, or takes
 * some away ({@link AbstractRichTextEditor#addFeature(IRichTextFeature)},
 * {@link AbstractRichTextEditor#removeFeature(IRichTextFeature)}).
 * <p>
 * A feature is three things, by its {@link #getName() name}:
 * <ul>
 * <li>its script, in the browser: the Tiptap extensions making it, and the tools of the toolbar
 * acting on it, which a script the feature {@link #renderHead(Component, IHeaderResponse) renders}
 * registers under the name of the feature, before the editor starts:
 *
 * <pre>
 * Wicket.RichTextEditor.registerFeature('kbd', {
 *     extensions: function (context) {
 *         return context.kit.Mark.create({ name: 'kbd', ... });
 *     },
 *     tools: {
 *         kbd: { label: 'Keyboard key', icon: '&lt;svg ...&gt;', kind: 'toggle',
 *             active: function (editor) { return editor.isActive('kbd'); },
 *             run: function (editor) { editor.chain().focus().toggleMark('kbd').run(); } }
 *     }
 * });
 * </pre>
 *
 * The context holds the kit, the editor's classes of Tiptap and ProseMirror ({@code Mark},
 * {@code Node}, {@code mergeAttributes}, the input rules...), so a script needs none of its own,
 * and the options of the feature ({@link #putOptions(Component, JSONObject)}). The built-in
 * features are in the script of the editor; one registered under a built-in name replaces it.</li>
 * <li>the HTML it writes, which the cleaning of the editor lets through
 * ({@link #allowHtml(RichTextHtmlRules)}), and nothing else;</li>
 * <li>the {@link IRichTextTool tools} of the toolbar needing it, which do not show without it.</li>
 * </ul>
 * A feature may also bring {@link #getEvents(Component) events} the editor listens to while it
 * has the feature: those its script fires, for example, which the server hears of.
 * A feature is kept in the page with the editor: it is serializable, and two features of the same
 * name are the same feature, as a {@code record} or an {@code enum} is.
 *
 * @since 11.0.0
 */
public interface IRichTextFeature extends IClusterable
{
	/**
	 * @return the name of the feature in the browser, unique among the features of the editor:
	 *         what its script registers it under
	 */
	String getName();

	/**
	 * @return whether Markdown has a syntax for what the feature writes, so that an editor of
	 *         Markdown has it by default; {@code true} unless told otherwise
	 */
	default boolean isInMarkdown()
	{
		return true;
	}

	/**
	 * Lets through the cleaning of the HTML the elements and the attributes the feature writes,
	 * and those alone. Does nothing by default: the HTML of the feature is dropped, but for its
	 * text.
	 *
	 * @param rules
	 *            what the cleaning lets through
	 */
	default void allowHtml(RichTextHtmlRules rules)
	{
	}

	/**
	 * Renders what the feature needs in the browser: the script registering it, its style sheet.
	 * The editor calls it after rendering its own script, so that {@code Wicket.RichTextEditor} is
	 * there; the page renders each script once, also when an editor comes in an Ajax response.
	 * Does nothing by default, as for the built-in features.
	 *
	 * @param editor
	 *            the editor
	 * @param response
	 *            the header response
	 */
	default void renderHead(Component editor, IHeaderResponse response)
	{
	}

	/**
	 * Puts the options of the feature for an editor, which its script gets in the browser
	 * ({@code context.options}). Puts none by default.
	 *
	 * @param editor
	 *            the editor
	 * @param options
	 *            the options of the feature, sent when not empty
	 */
	default void putOptions(Component editor, JSONObject options)
	{
	}

	/**
	 * @param editor
	 *            the editor
	 * @return the events the editor listens to while it has the feature, after its own, in the
	 *         same order each time: the server finds them by it. None by default.
	 */
	default List<? extends IClientEvent> getEvents(Component editor)
	{
		return List.of();
	}

	/**
	 * Features, checked: none missing, each with a name, no two of the same name.
	 *
	 * @param <F>
	 *            the type of the features
	 * @param features
	 *            the features
	 * @return the features, in their order, unmodifiable and serializable
	 * @throws IllegalArgumentException
	 *             for a feature missing, without a name, or of the name of another
	 */
	static <F extends IRichTextFeature> Set<F> checked(Collection<? extends F> features)
	{
		if (features == null)
		{
			throw new IllegalArgumentException("The features are missing");
		}
		Set<String> names = new HashSet<>();
		Set<F> checked = new LinkedHashSet<>();
		for (F feature : features)
		{
			if (feature == null)
			{
				throw new IllegalArgumentException("A feature is missing");
			}
			String name = feature.getName();
			if (name == null || name.isBlank())
			{
				throw new IllegalArgumentException("A feature has no name: " + feature);
			}
			if (!names.add(name) && !checked.contains(feature))
			{
				throw new IllegalArgumentException("Two features are named " + name);
			}
			checked.add(feature);
		}
		return Collections.unmodifiableSet(checked);
	}
}
