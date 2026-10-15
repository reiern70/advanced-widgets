/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

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
