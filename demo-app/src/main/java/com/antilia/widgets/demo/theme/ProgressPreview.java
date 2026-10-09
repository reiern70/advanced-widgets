/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.theme;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.antilia.widgets.progress.ProgressBar;

/**
 * Progress bars of the theme editor's preview: running, finished and of unknown progress.
 */
class ProgressPreview extends Panel
{
	private static final long serialVersionUID = 1L;

	ProgressPreview(String id)
	{
		super(id);
		add(new ProgressBar("running", Model.of(62)));
		add(new ProgressBar("finished", Model.of(100)));
		add(new ProgressBar("unknown", Model.of((Integer)null)));
	}
}
