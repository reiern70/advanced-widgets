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

import org.apache.wicket.util.io.IClusterable;

/**
 * Cleans the HTML of a {@link RichTextEditor}, which comes from the browser and is never to be
 * trusted: what it gives back is what the model gets and a page shows. {@link RichTextPolicy}
 * keeps what the features of the editor write.
 *
 * @since 11.0.0
 */
@FunctionalInterface
public interface RichTextSanitizer extends IClusterable
{
	/**
	 * @param html
	 *            the HTML, maybe {@code null}
	 * @return the HTML cleaned, {@code null} for {@code null}
	 */
	String sanitize(String html);
}
