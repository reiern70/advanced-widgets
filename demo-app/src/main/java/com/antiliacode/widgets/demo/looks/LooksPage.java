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
package com.antiliacode.widgets.demo.looks;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.theme.BootstrapResourceReference;
import com.antiliacode.widgets.theme.ILook;
import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.LookAspect;
import com.antiliacode.widgets.theme.ThemeBehavior;

/**
 * The looks side by side, in the theme of the page: the same components in the {@link Look#ANTILIA
 * Antilia look}, in the {@link Look#BOOTSTRAP Bootstrap look} and in a look made of both,
 * Bootstrap's with the colors and the sizes of Antilia's, and markup of Bootstrap's own, which the
 * theme colors too. The page has Bootstrap's style sheet whatever the look of the session,
 * so the Antilia look shows on a page with Bootstrap as well.
 */
public class LooksPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public LooksPage()
	{
		add(column("antilia", Look.ANTILIA).add(new LookSample("sample")));
		add(column("bootstrap", Look.BOOTSTRAP).add(new LookSample("sample")));
		ILook mixed = Look.BOOTSTRAP.with(LookAspect.COLOR, Look.ANTILIA).with(LookAspect.SIZE,
			Look.ANTILIA);
		add(column("mixed", mixed).add(new LookSample("sample")));
		add(column("native", Look.BOOTSTRAP));
	}

	/**
	 * @return a column of the page in the theme of the page and in a look of its own
	 */
	private WebMarkupContainer column(String id, ILook look)
	{
		WebMarkupContainer column = new WebMarkupContainer(id);
		column.add(new ThemeBehavior(getThemeModel(), Model.of(look)));
		return column;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(BootstrapResourceReference.styleSheet());
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(LooksPage.class, "LooksPage.css")));
	}
}
