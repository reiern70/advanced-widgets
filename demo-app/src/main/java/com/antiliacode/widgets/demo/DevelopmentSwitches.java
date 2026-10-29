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

import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.demo.account.DemosAuthorization;
import com.antiliacode.widgets.demo.register.RegisterPage;

/**
 * The system properties that spare a development machine signing in and confirming emails:
 * {@value WicketApplication#SIGN_IN_AS_PROPERTY}, {@value DemosAuthorization#OPEN_PARAMETER} and
 * {@value RegisterPage#OPEN_PROPERTY}. On a server they would let anybody in, so they count in
 * development only: in deployment they are ignored, and the application warns of each one set
 * when it starts. Only closing the registration with
 * {@code -D}{@value RegisterPage#OPEN_PROPERTY}{@code =false} counts everywhere.
 */
public final class DevelopmentSwitches
{
	private static final Logger LOG = LoggerFactory.getLogger(DevelopmentSwitches.class);

	/** The system properties counting in development only. */
	static final List<String> PROPERTIES = List.of(WicketApplication.SIGN_IN_AS_PROPERTY,
		DemosAuthorization.OPEN_PARAMETER, RegisterPage.OPEN_PROPERTY);

	private DevelopmentSwitches()
	{
	}

	/**
	 * @param property
	 *            one of the system properties of a development machine
	 * @return its value in development, {@code null} in deployment
	 */
	public static String get(String property)
	{
		return Application.get().usesDevelopmentConfig() ? System.getProperty(property) : null;
	}

	/**
	 * Warns of each system property of a development machine that is set, but ignored, as the
	 * application runs in deployment.
	 *
	 * @param application
	 *            the application starting
	 * @return how many it warned of
	 */
	static int warnOfIgnored(Application application)
	{
		if (application.usesDevelopmentConfig())
		{
			return 0;
		}
		int ignored = 0;
		for (String property : PROPERTIES)
		{
			String value = System.getProperty(property);
			boolean closing = RegisterPage.OPEN_PROPERTY.equals(property) &&
				!Boolean.parseBoolean(value);
			if (!Strings.isEmpty(value) && !closing)
			{
				LOG.warn("-D{} is set, but ignored: it counts in development only", property);
				ignored++;
			}
		}
		return ignored;
	}
}
