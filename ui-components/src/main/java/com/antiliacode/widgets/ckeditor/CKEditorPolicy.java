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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.owasp.html.PolicyFactory;

import com.antiliacode.widgets.richtext.RichTextHtmlRules;
import com.antiliacode.widgets.richtext.RichTextPolicy;
import com.antiliacode.widgets.richtext.RichTextSanitizer;

/**
 * The cleaning of the HTML of a {@link CKEditor}, with the OWASP Java HTML Sanitizer: it keeps the
 * elements and the attributes CKEditor 5 writes for the {@link CKEditorFeature features} of the
 * editor, each {@link CKEditorFeature#allowHtml(com.antiliacode.widgets.richtext.RichTextHtmlRules)
 * letting through its own}, and nothing else, as {@link RichTextPolicy} does for Tiptap's. No script, no event
 * handler, no style attribute, no element of a feature left out; links and images only to http,
 * https and mailto addresses, or relative ones ({@link RichTextPolicy#isSafeUrl(String)}), links
 * with {@code rel="noopener noreferrer nofollow"}, and images without a width or a height. Classes
 * only where CKEditor writes them:
 * <ul>
 * <li>the alignment of paragraphs, headings and the items of lists, {@code antilia-text-center},
 * {@code -right} and {@code -justify}, those of Tiptap's editor;</li>
 * <li>the language of a code block, {@code <pre><code class="language-java">};</li>
 * <li>the figures holding tables and images, {@code <figure class="table">} and
 * {@code <figure class="image">}, with the caption of an image;</li>
 * <li>the lists of tasks, {@code <ul class="todo-list">}, each item a {@code label} of the class
 * {@code todo-list__label} holding a check box and a {@code span} of the class
 * {@code todo-list__label__description}.</li>
 * </ul>
 * CKEditor's ids of the items of lists, {@code data-list-item-id}, which it makes anew as it reads
 * the HTML, do not stay.
 *
 * @since 11.0.0
 */
public final class CKEditorPolicy implements RichTextSanitizer
{
	private static final long serialVersionUID = 1L;

	/** The policy of all the features of CKEditor, {@link CKEditor#FEATURES}. */
	public static final CKEditorPolicy ALL = new CKEditorPolicy(CKEditor.FEATURES);

	private final Set<CKEditorFeature> features;

	/** Made the first time it cleans: OWASP's policies are not serializable. */
	private transient PolicyFactory factory;

	private CKEditorPolicy(Collection<CKEditorFeature> features)
	{
		this.features = features.isEmpty() ? EnumSet.noneOf(CKEditorFeature.class) :
			EnumSet.copyOf(features);
	}

	/**
	 * @param features
	 *            the features of the editor; {@code null} for all of them
	 * @return the policy keeping what CKEditor writes for those features
	 */
	public static CKEditorPolicy of(Collection<CKEditorFeature> features)
	{
		if (features == null || features.containsAll(ALL.features))
		{
			return ALL;
		}
		return new CKEditorPolicy(features);
	}

	/**
	 * @return the features whose HTML the policy keeps
	 */
	public Set<CKEditorFeature> getFeatures()
	{
		return Collections.unmodifiableSet(features);
	}

	@Override
	public String sanitize(String html)
	{
		if (html == null)
		{
			return null;
		}
		if (factory == null)
		{
			factory = RichTextHtmlRules.toPolicy(features, Set.of(), Set.of());
		}
		return factory.sanitize(html);
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof CKEditorPolicy policy && policy.features.equals(features);
	}

	@Override
	public int hashCode()
	{
		return features.hashCode();
	}

	@Override
	public String toString()
	{
		return "CKEditorPolicy" + features;
	}
}
