/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets;

import org.apache.wicket.Application;
import org.apache.wicket.IInitializer;

import com.antilia.widgets.upload.UploadStatusResource;

/**
 * Registered in {@code META-INF/services/org.apache.wicket.IInitializer}, so that Wicket looks up
 * the default texts of the widgets in {@code Initializer.properties} next to this class, whatever
 * the package of the page they are added to. It shares the resources of the widgets, such as the
 * {@link UploadStatusResource}.
 */
public class Initializer implements IInitializer
{
	@Override
	public void init(Application application)
	{
		application.getSharedResources().add(UploadStatusResource.NAME, new UploadStatusResource());
	}

	@Override
	public void destroy(Application application)
	{
	}
}
