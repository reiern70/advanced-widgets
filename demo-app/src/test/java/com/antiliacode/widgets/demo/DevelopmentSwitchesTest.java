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
package com.antiliacode.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.account.DemosAuthorization;
import com.antiliacode.widgets.demo.mail.LogMailer;
import com.antiliacode.widgets.demo.register.RegisterPage;

/**
 * The system properties of a development machine count in development only, and the application
 * runs in deployment unless told otherwise; see {@link DevelopmentSwitches}.
 */
class DevelopmentSwitchesTest
{
	private WicketTester tester;

	@AfterEach
	void stop()
	{
		if (tester != null)
		{
			tester.destroy();
		}
		DevelopmentSwitches.PROPERTIES.forEach(System::clearProperty);
	}

	private WicketApplication start(RuntimeConfigurationType configuration)
	{
		TestDatabase.clear();
		WicketApplication application = new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return configuration;
			}
		};
		tester = new WicketTester(application);
		application.setMailer(new LogMailer());
		return application;
	}

	@Test
	void inDeploymentNobodyIsLetInByThem()
	{
		System.setProperty(WicketApplication.SIGN_IN_AS_PROPERTY, "admin");
		System.setProperty(DemosAuthorization.OPEN_PARAMETER, "true");
		System.setProperty(RegisterPage.OPEN_PROPERTY, "true");
		WicketApplication application = start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(HomePage.class);
		tester.assertVisible("signIn");
		assertFalse(DemoSession.get().isSignedIn());
		assertFalse(DemosAuthorization.isOpen());
		assertFalse(RegisterPage.isOpen(), "no emails, no registering");
		assertEquals(3, DevelopmentSwitches.warnOfIgnored(application));
	}

	@Test
	void inDevelopmentTheyCount()
	{
		System.setProperty(WicketApplication.SIGN_IN_AS_PROPERTY, "admin");
		System.setProperty(RegisterPage.OPEN_PROPERTY, "true");
		WicketApplication application = start(RuntimeConfigurationType.DEVELOPMENT);

		tester.startPage(HomePage.class);
		assertTrue(DemoSession.get().isAdmin());
		assertTrue(RegisterPage.isOpen());
		assertEquals(0, DevelopmentSwitches.warnOfIgnored(application));
	}

	@Test
	void closingTheRegistrationCountsEverywhere()
	{
		System.setProperty(RegisterPage.OPEN_PROPERTY, "false");
		WicketApplication application = start(RuntimeConfigurationType.DEPLOYMENT);
		RecordingMailer.install(application);

		tester.startPage(HomePage.class);
		assertFalse(RegisterPage.isOpen());
		assertEquals(0, DevelopmentSwitches.warnOfIgnored(application));
	}

	@Test
	void theApplicationRunsInDeploymentUnlessToldOtherwise() throws Exception
	{
		String basedir = System.getProperty("basedir");
		String webXml = Files.readString(Path.of(basedir != null ? basedir : ".",
			"src/main/webapp/WEB-INF/web.xml"));

		assertTrue(webXml.replaceAll("\\s+", "").contains("<context-param><param-name>" +
			"configuration</param-name><param-value>deployment</param-value></context-param>"),
			webXml);
	}
}
