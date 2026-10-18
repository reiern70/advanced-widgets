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
