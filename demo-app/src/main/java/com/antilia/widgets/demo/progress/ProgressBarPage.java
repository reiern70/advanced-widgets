/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.progress;

import java.time.Duration;

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.antilia.widgets.progress.ProgressBar;
import com.github.openjson.JSONObject;

/**
 * {@link ProgressBar}s on their own: one advanced by an Ajax timer, and an indeterminate one, in a
 * theme the user chooses. The React and Svelte tabs show the same bars with their
 * {@code ProgressBar}, the first one advanced by a timer of the app.
 */
public class ProgressBarPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private int progress;

	private boolean running;

	/**
	 * Constructor.
	 */
	public ProgressBarPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		ProgressBar bar = new ProgressBar("bar", new PropertyModel<>(this, "progress"));
		bar.setOutputMarkupId(true);
		themed.add(bar);
		themed.add(new ProgressBar("finished", Model.of(100)));
		themed.add(new ProgressBar("indeterminate", Model.of((Integer)null)));

		bar.add(new AbstractAjaxTimerBehavior(Duration.ofMillis(300))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onTimer(AjaxRequestTarget target)
			{
				if (running)
				{
					progress = Math.min(100, progress + 3);
					running = progress < 100;
					target.add(bar);
				}
			}
		});

		themed.add(new AjaxLink<Void>("toggle")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				running = !running && progress < 100;
				target.add(this);
			}
		}.setBody(() -> getString(running ? "pause" : "start")).setOutputMarkupId(true));

		themed.add(new AjaxLink<Void>("reset")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				progress = 0;
				running = false;
				target.add(bar, themed.get("toggle"));
			}
		});
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "progressBar";
	}

	/**
	 * @return the texts and the progress as the page has it
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		return new JSONObject()
			.put("texts", FrameworkTabs.texts(this, "known", "unknown", "finished", "reset",
				"start", "pause"))
			.put("progress", progress);
	}
}
