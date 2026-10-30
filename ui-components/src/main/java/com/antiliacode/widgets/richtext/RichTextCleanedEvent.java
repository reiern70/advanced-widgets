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

import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.lang.Args;

/**
 * The event of Wicket a rich text editor in {@link RichTextCleaning#REPORT} sends, bubbling from
 * the editor to the page, once its model got the text the cleaning removed something from: a page
 * shows a notification, or logs it.
 *
 * <pre>
 * &#64;Override
 * public void onEvent(IEvent&lt;?&gt; event)
 * {
 *     if (event.getPayload() instanceof RichTextCleanedEvent cleaned)
 *     {
 *         cleaned.getTarget().ifPresent(target -&gt; ...);
 *     }
 * }
 * </pre>
 *
 * @since 11.0.0
 */
public final class RichTextCleanedEvent
{
	private final AbstractRichTextEditor<?> editor;

	private final RichTextCleaningReport report;

	private final AjaxRequestTarget target;

	/**
	 * @param editor
	 *            the editor
	 * @param report
	 *            what the cleaning removed
	 * @param target
	 *            the Ajax request target, {@code null} when the request is no Ajax one
	 */
	public RichTextCleanedEvent(AbstractRichTextEditor<?> editor, RichTextCleaningReport report,
		AjaxRequestTarget target)
	{
		this.editor = Args.notNull(editor, "editor");
		this.report = Args.notNull(report, "report");
		this.target = target;
	}

	/**
	 * @return the editor whose text was cleaned
	 */
	public AbstractRichTextEditor<?> getEditor()
	{
		return editor;
	}

	/**
	 * @return what the cleaning removed
	 */
	public RichTextCleaningReport getReport()
	{
		return report;
	}

	/**
	 * @return the Ajax request target, none when the request is no Ajax one
	 */
	public Optional<AjaxRequestTarget> getTarget()
	{
		return Optional.ofNullable(target);
	}
}
