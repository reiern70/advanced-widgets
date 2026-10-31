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

import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.lang.Args;

/**
 * The event of Wicket a {@link RichTextEditor} with a store of images sends, bubbling from the
 * editor to the page, when the browser told it the user took images out of the text: deleted by a
 * key, cut, taken out by the list of the images of the toolbar, or by the code of a page. Only the
 * images the editor knows are told of: those of the text it was shown with, and those uploaded into
 * it since. Whether they are deleted now or once the text is saved is
 * {@link RichTextEditor#getImageDeletion() the editor's mode}.
 *
 * <pre>
 * &#64;Override
 * public void onEvent(IEvent&lt;?&gt; event)
 * {
 *     if (event.getPayload() instanceof RichTextImagesRemovedEvent removed)
 *     {
 *         removed.getTarget().add(imageCount);
 *     }
 * }
 * </pre>
 *
 * @since 10.11.0
 */
public final class RichTextImagesRemovedEvent
{
	private final RichTextEditor editor;

	private final Set<String> urls;

	private final boolean deleted;

	private final AjaxRequestTarget target;

	/**
	 * @param editor
	 *            the editor
	 * @param urls
	 *            the addresses of the images taken out
	 * @param deleted
	 *            whether they are deleted already, {@link RichTextImageDeletion#AT_ONCE}
	 * @param target
	 *            the Ajax request target
	 */
	public RichTextImagesRemovedEvent(RichTextEditor editor, Set<String> urls, boolean deleted,
		AjaxRequestTarget target)
	{
		this.editor = Args.notNull(editor, "editor");
		this.urls = Set.copyOf(Args.notNull(urls, "urls"));
		this.deleted = deleted;
		this.target = Args.notNull(target, "target");
	}

	/**
	 * @return the editor whose images were taken out
	 */
	public RichTextEditor getEditor()
	{
		return editor;
	}

	/**
	 * @return the addresses of the images taken out
	 */
	public Set<String> getUrls()
	{
		return urls;
	}

	/**
	 * @return whether the images are deleted already, as in {@link RichTextImageDeletion#AT_ONCE};
	 *         otherwise they will be once the text is saved without them
	 */
	public boolean isDeleted()
	{
		return deleted;
	}

	/**
	 * @return the Ajax request target
	 */
	public AjaxRequestTarget getTarget()
	{
		return target;
	}
}
