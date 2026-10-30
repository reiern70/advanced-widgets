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

import org.apache.wicket.util.lang.Args;

/**
 * A text a {@link IRichTextSanitizer} cleaned, and what the cleaning removed from it.
 *
 * @param text
 *            the text cleaned, {@code null} for none
 * @param report
 *            what was removed, {@link RichTextCleaningReport#EMPTY} for nothing
 * @since 11.0.0
 */
public record RichTextSanitized(String text, RichTextCleaningReport report)
{
	/**
	 * Checks the report.
	 */
	public RichTextSanitized
	{
		Args.notNull(report, "report");
	}
}
