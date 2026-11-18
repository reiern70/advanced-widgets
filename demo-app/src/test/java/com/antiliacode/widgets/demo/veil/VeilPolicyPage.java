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
package com.antiliacode.widgets.demo.veil;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.veil.PageVeilBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.codeeditor.CodeEditor;
import com.antiliacode.widgets.desktop.Desktop;
import com.antiliacode.widgets.desktop.DesktopWindow;
import com.antiliacode.widgets.filter.FilterBehavior;
import com.antiliacode.widgets.richtext.RichTextEditor;
import com.antiliacode.widgets.split.SplitPanel;

/**
 * A page with Wicket's {@link PageVeilBehavior} and the components whose requests report in the
 * background while the user goes on, which the page veil leaves alone: a filter, a code editor, a
 * rich text editor, a split panel and a desktop telling the server where the user left them. The
 * link {@code act} is what the user does, which the veil covers. The filter's runs are counted by
 * {@link VeilRace}.
 */
public class VeilPolicyPage extends WebPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public VeilPolicyPage()
	{
		add(new PageVeilBehavior());

		Form<Void> form = new Form<>("form");
		add(form);
		TextField<String> filter = new TextField<>("filter", Model.of(""));
		filter.add(FilterBehavior.onFilter(target -> VeilRace.run("filter")));
		form.add(filter);
		form.add(new CodeEditor("code", Model.of("let tide = 'high';")));
		form.add(new RichTextEditor("rich", Model.of("<p>The tides of the bay.</p>")));

		add(new SplitPanel("split")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newFirstPane(String id)
			{
				return new Label(id, "First");
			}

			@Override
			protected Component newSecondPane(String id)
			{
				return new Label(id, "Second");
			}
		}.setSendToServer(true));

		Desktop desktop = new Desktop("desktop");
		desktop.setSendToServer(true);
		desktop.addWindow(new DesktopWindow(desktop.newWindowId(), Model.of("Notes"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newContent(String id)
			{
				return new Label(id, "Notes of the tides");
			}
		}.setPosition(16, 16).setSize(320, 160));
		add(desktop);

		add(new AjaxLink<Void>("act")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				VeilRace.run("act");
			}
		});
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(VeilPolicyPage.class,
			"VeilPolicyPage.css")));
	}
}
