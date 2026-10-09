/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import java.util.Map;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * The file selected in a tree of the tree examples: its name, path, kind, size and when it was
 * changed, in the language of the session; or, while none is selected, an invitation to select
 * one. The trees repaint it through Ajax when the user selects a file.
 */
public class FileDetailsPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param file
	 *            the file shown, {@code null} for none
	 */
	public FileDetailsPanel(String id, IModel<ProjectFile> file)
	{
		super(id, file);
		setOutputMarkupId(true);

		WebMarkupContainer details = new WebMarkupContainer("details")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(getFile() != null);
			}
		};
		add(details);
		for (String key : new String[] { "name", "path", "kind", "size", "modified" })
		{
			details.add(new Label(key, () -> json().get(key)));
		}
		add(new WebMarkupContainer("none")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(getFile() == null);
			}
		});
	}

	private ProjectFile getFile()
	{
		return (ProjectFile)getDefaultModelObject();
	}

	private Map<String, Object> json()
	{
		return ProjectFiles.toJson(getFile(), getLocale());
	}
}
