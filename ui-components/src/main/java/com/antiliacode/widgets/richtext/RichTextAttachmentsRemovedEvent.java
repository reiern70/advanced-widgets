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
 * The event of Wicket a rich text editor with a store of files attached sends,
 * {@link RichTextEditor} or {@link com.antiliacode.widgets.ckeditor.CKEditor}, bubbling from the
 * editor to the page, when the browser told it the user took files out of the text: deleted by a
 * key, cut, taken out by the list of the files of the toolbar, or by the code of a page. Only the
 * files the editor knows are told of: those of the text it was shown with, and those uploaded into
 * it since. Whether they are deleted now or once the text is saved is
 * {@link AbstractRichTextEditor#getAttachmentDeletion() the editor's mode for the files}, the same
 * as for the images unless set.
 *
 * <pre>
 * &#64;Override
 * public void onEvent(IEvent&lt;?&gt; event)
 * {
 *     if (event.getPayload() instanceof RichTextAttachmentsRemovedEvent removed)
 *     {
 *         removed.getTarget().add(fileCount);
 *     }
 * }
 * </pre>
 *
 * @since 11.0.0
 */
public final class RichTextAttachmentsRemovedEvent
{
	private final AbstractRichTextEditor<?> editor;

	private final Set<String> urls;

	private final boolean deleted;

	private final AjaxRequestTarget target;

	/**
	 * @param editor
	 *            the editor
	 * @param urls
	 *            the addresses of the files taken out
	 * @param deleted
	 *            whether they are deleted already, {@link RichTextImageDeletion#AT_ONCE}
	 * @param target
	 *            the Ajax request target
	 */
	public RichTextAttachmentsRemovedEvent(AbstractRichTextEditor<?> editor, Set<String> urls,
		boolean deleted, AjaxRequestTarget target)
	{
		this.editor = Args.notNull(editor, "editor");
		this.urls = Set.copyOf(Args.notNull(urls, "urls"));
		this.deleted = deleted;
		this.target = Args.notNull(target, "target");
	}

	/**
	 * @return the editor whose files were taken out
	 */
	public AbstractRichTextEditor<?> getEditor()
	{
		return editor;
	}

	/**
	 * @return the addresses of the files taken out
	 */
	public Set<String> getUrls()
	{
		return urls;
	}

	/**
	 * @return whether the files are deleted already, as in {@link RichTextImageDeletion#AT_ONCE};
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
