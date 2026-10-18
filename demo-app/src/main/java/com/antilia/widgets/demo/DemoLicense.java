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
package com.antilia.widgets.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import com.antilia.widgets.license.Licensing;

/**
 * The license key of the demo application, which activates every private component: the demo
 * always shows all of them, wherever it runs and however it is opened. The license only limits the
 * other applications using the library, which need a key of their own.
 * <p>
 * The key is the one bundled with the demo, {@value Licensing#RESOURCE}. The application installs
 * it at start-up, over any key of the system property or the environment, and gives it to the
 * React and Svelte apps of every demo, which activate it in the browser.
 */
public final class DemoLicense
{
	private DemoLicense()
	{
	}

	/**
	 * @return the key bundled with the demo application
	 * @throws IllegalStateException
	 *             if the demo application has no key
	 */
	public static String key()
	{
		try (InputStream key = DemoLicense.class.getClassLoader()
			.getResourceAsStream(Licensing.RESOURCE))
		{
			if (key == null)
			{
				throw new IllegalStateException("The demo application has no license key in " +
					Licensing.RESOURCE + ": it has to show every private component");
			}
			return new String(key.readAllBytes(), StandardCharsets.UTF_8).strip();
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
}
