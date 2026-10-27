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
package com.antiliacode.widgets.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.codeeditor.CodeEditor;
import com.antiliacode.widgets.template.Templates;

class AlreadyCompressedJavaScriptResourceReferenceTest extends WicketTestCase
{
	@Test
	void servesTheScriptWithoutCompressingIt()
	{
		AlreadyCompressedJavaScriptResourceReference reference =
			new AlreadyCompressedJavaScriptResourceReference(CodeEditor.class,
				"antilia-code-editor.js");

		assertSame(CodeEditor.class, reference.getScope());
		assertEquals("antilia-code-editor.js", reference.getName());
		assertFalse(reference.getResource().getCompress());
		assertTrue(reference.getDependencies().isEmpty());
		// unlike Wicket's own reference
		assertTrue(new JavaScriptResourceReference(CodeEditor.class, "antilia-code-editor.js")
			.getResource().getCompress());
	}

	@Test
	void dependsOnTheScriptsItIsGiven()
	{
		ResourceReference other = new JavaScriptResourceReference(CodeEditor.class, "other.js");
		AlreadyCompressedJavaScriptResourceReference reference =
			new AlreadyCompressedJavaScriptResourceReference(CodeEditor.class,
				"antilia-code-editor.js", Templates.JS, other);

		List<HeaderItem> dependencies = reference.getDependencies();
		assertEquals(List.of(JavaScriptHeaderItem.forReference(Templates.JS),
			JavaScriptHeaderItem.forReference(other)), dependencies);
		// a list of its own each time
		dependencies.clear();
		assertEquals(2, reference.getDependencies().size());
	}

	@Test
	void takesNoMissingDependency()
	{
		assertThrows(IllegalArgumentException.class,
			() -> new AlreadyCompressedJavaScriptResourceReference(CodeEditor.class, "a.js",
				(ResourceReference[])null));
		assertThrows(IllegalArgumentException.class,
			() -> new AlreadyCompressedJavaScriptResourceReference(CodeEditor.class, "a.js",
				Templates.JS, null));
	}
}
