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
package com.antiliacode.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

class ScriptJsonTest
{
	/** A backslash and a u, which the source cannot write next to each other. */
	private static final String U = "\\" + "u";

	@Test
	void writesTheCharactersOfMarkupAndTheLineSeparatorsAsUnicodeEscapes()
	{
		assertEquals("\"" + U + "003c!--" + U + "003cscript" + U + "003e " + U + "0026 " + U +
			"2028" + U + "2029\"", ScriptJson.quote("<!--<script> & " + (char)0x2028 +
				(char)0x2029));
		assertEquals("\"" + U + "003c\\/script" + U + "003e\"", ScriptJson.quote("</script>"));
		assertEquals("\"\"", ScriptJson.quote(null));
	}

	@Test
	void leavesTheValueAsTheScriptReadsIt()
	{
		JSONObject json = new JSONObject().put("a", "<b>x</b> & ]]>").put("n", 1);
		String escaped = ScriptJson.of(json);
		assertEquals("{\"a\":\"" + U + "003cb" + U + "003ex" + U + "003c\\/b" + U + "003e " + U +
			"0026 ]]" + U + "003e\",\"n\":1}", escaped);
		assertEquals(json.toString(), new JSONObject(escaped).toString());
		assertEquals("[\"" + U + "003c\"]", ScriptJson.of(new JSONArray().put("<")));
		assertEquals("null", ScriptJson.of((JSONObject)null));
		assertEquals("null", ScriptJson.of((JSONArray)null));
	}

	@Test
	void escapesTheJsonOfOtherSerializers()
	{
		String json = "{\"a\":\"plain\"}";
		assertSame(json, ScriptJson.escape(json), "nothing to escape, the same text");
		assertEquals("{\"a\":\"" + U + "003c!--\"}", ScriptJson.escape("{\"a\":\"<!--\"}"));
		assertNull(ScriptJson.escape(null));
	}
}
