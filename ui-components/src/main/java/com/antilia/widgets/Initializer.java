/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets;

import org.apache.wicket.Application;
import org.apache.wicket.IInitializer;

/**
 * Registered in {@code META-INF/services/org.apache.wicket.IInitializer}, so that Wicket looks up
 * the default texts of the widgets in {@code Initializer.properties} next to this class, whatever
 * the package of the page they are added to.
 */
public class Initializer implements IInitializer
{
	@Override
	public void init(Application application)
	{
	}

	@Override
	public void destroy(Application application)
	{
	}
}
