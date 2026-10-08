/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

import org.apache.wicket.util.string.Strings;

/**
 * The branch and commit the demo application was built from, which the build writes to
 * {@code build-info.properties}, and whether the demo runs in demo mode, started with
 * {@code -Dwicket.demo=true}, where the footer of every page shows them.
 *
 * @param branch
 *            the branch, empty if not known
 * @param commit
 *            the abbreviated id of the commit, empty if not known
 * @param dirty
 *            whether the build had changes not committed yet
 */
public record BuildInfo(String branch, String commit, boolean dirty) implements Serializable
{
	/** The system property turning the demo mode on. */
	public static final String DEMO_PROPERTY = "wicket.demo";

	private static final String RESOURCE = "/build-info.properties";

	private static final BuildInfo INSTANCE = load();

	/**
	 * @return what the application was built from
	 */
	public static BuildInfo get()
	{
		return INSTANCE;
	}

	/**
	 * @return whether the demo runs in demo mode
	 */
	public static boolean isDemoMode()
	{
		return Boolean.getBoolean(DEMO_PROPERTY);
	}

	/**
	 * @return whether the branch is known
	 */
	public boolean isKnown()
	{
		return !Strings.isEmpty(branch);
	}

	private static BuildInfo load()
	{
		Properties properties = new Properties();
		try (InputStream in = BuildInfo.class.getResourceAsStream(RESOURCE))
		{
			if (in != null)
			{
				properties.load(in);
			}
		}
		catch (IOException e)
		{
			// unknown, the footer shows nothing
		}
		return new BuildInfo(properties.getProperty("branch", "").strip(),
			properties.getProperty("commit", "").strip(),
			Boolean.parseBoolean(properties.getProperty("dirty")));
	}
}
