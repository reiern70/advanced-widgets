/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
