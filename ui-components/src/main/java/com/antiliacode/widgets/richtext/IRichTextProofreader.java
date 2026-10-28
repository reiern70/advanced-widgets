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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.util.io.IClusterable;

/**
 * Checks the spelling, the grammar and the style of the texts of a {@link RichTextEditor}, which
 * underlines what it finds and offers the replacements: a service, such as
 * {@link LanguageToolProofreader LanguageTool}, or a dictionary of the application's own. The
 * editor asks it through the server, {@link RichTextProofreadBehavior}, so that the address of the
 * service and its keys stay there.
 *
 * @see RichTextEditor#setProofreader(IRichTextProofreader)
 * @since 11.0.0
 */
@FunctionalInterface
public interface IRichTextProofreader extends IClusterable
{
	/**
	 * Checks texts: blocks of an editor that changed, a paragraph each for example.
	 *
	 * @param texts
	 *            the texts, from the browser: the user's, not to be trusted
	 * @param language
	 *            their language, {@code null} for the proofreader to find
	 * @return the issues of each text, in the order of the texts
	 * @throws IOException
	 *             when the texts could not be checked: the service did not answer, for example
	 */
	List<List<RichTextIssue>> check(List<String> texts, Locale language) throws IOException;
}
