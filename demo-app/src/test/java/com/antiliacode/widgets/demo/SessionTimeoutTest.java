/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antiliacode.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * A session ends after {@link WicketApplication#SESSION_TIMEOUT} without a request, as the
 * {@code web.xml} the servlet container reads sets it.
 */
class SessionTimeoutTest
{
	@Test
	void aSessionEndsAfterFiveMinutesWithoutARequest() throws Exception
	{
		Document webXml = DocumentBuilderFactory.newInstance()
			.newDocumentBuilder()
			.parse(new File("src/main/webapp/WEB-INF/web.xml"));
		String minutes = webXml.getElementsByTagName("session-timeout").item(0).getTextContent();

		assertEquals(5, WicketApplication.SESSION_TIMEOUT.toMinutes());
		assertEquals(WicketApplication.SESSION_TIMEOUT.toMinutes(), Long.parseLong(minutes.strip()));
	}
}
