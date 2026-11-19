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
package com.antiliacode.widgets.demo.looks;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.theme.BootstrapResourceReference;
import com.antiliacode.widgets.demo.theme.DaisyUiResourceReference;
import com.antiliacode.widgets.demo.theme.WebJarStyleSheetReference;
import com.antiliacode.widgets.theme.ILook;
import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.LookAspect;
import com.antiliacode.widgets.theme.Looks;
import com.antiliacode.widgets.theme.ThemeBehavior;

/**
 * The looks side by side, in the theme of the page, for a framework of CSS: the same components
 * in the {@link Look#ANTILIA Antilia look}, in the look of the framework, {@link Look#BOOTSTRAP
 * Bootstrap} or {@link Look#DAISYUI daisyUI}, and in a look made of both, the framework's with the
 * colors and the sizes of Antilia's, and markup of the framework's own, which the theme colors too.
 * The page has the style sheet of that framework only, whatever the look of the session, as
 * Bootstrap and daisyUI name their components alike; the links above the columns, or the page
 * parameter {@value #FRAMEWORK}, choose the other. Without one the page shows the framework of the
 * look of the session, Bootstrap for a look of neither.
 */
public class LooksPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The page parameter naming the framework of the looks shown: bootstrap or daisyui. */
	public static final String FRAMEWORK = "framework";

	/**
	 * The frameworks of CSS the page shows the looks of.
	 */
	enum Framework
	{
		BOOTSTRAP(Look.BOOTSTRAP, BootstrapResourceReference.INSTANCE),

		DAISYUI(Look.DAISYUI, DaisyUiResourceReference.INSTANCE);

		private final Look look;

		private final WebJarStyleSheetReference styleSheet;

		Framework(Look look, WebJarStyleSheetReference styleSheet)
		{
			this.look = look;
			this.styleSheet = styleSheet;
		}

		/**
		 * @return the name of the framework, as the page parameter and the texts have it
		 */
		String getName()
		{
			return name().toLowerCase(Locale.ROOT);
		}

		/**
		 * @return the framework named, else that of the look of the session, else Bootstrap
		 */
		static Framework of(String name)
		{
			for (Framework framework : values())
			{
				if (framework.getName().equals(name))
				{
					return framework;
				}
			}
			return Looks.current().getLooks().contains(Look.DAISYUI) ? DAISYUI : BOOTSTRAP;
		}
	}

	private final Framework framework;

	/**
	 * Constructor.
	 *
	 * @param parameters
	 *            the parameters of the page: {@value #FRAMEWORK}, the framework of the looks
	 */
	public LooksPage(PageParameters parameters)
	{
		super(parameters);
		framework = Framework.of(parameters.get(FRAMEWORK).toOptionalString());
		for (Framework each : Framework.values())
		{
			add(frameworkLink(each));
		}
		add(column("antilia", Look.ANTILIA).add(new LookSample("sample")));
		String name = framework.getName();
		add(column("framework", framework.look).add(new Label("title", new ResourceModel(name)),
			new Label("help", new ResourceModel(name + ".help")), new LookSample("sample")));
		ILook mixed = framework.look.with(LookAspect.COLOR, Look.ANTILIA).with(LookAspect.SIZE,
			Look.ANTILIA);
		add(column("mixed", mixed).add(new Label("title", new ResourceModel(name + ".mixed")),
			new Label("help", new ResourceModel(name + ".mixed.help")), new LookSample("sample")));
		add(column("native", framework.look).add(new Label("title",
			new ResourceModel(name + ".native")), new Label("help",
				new ResourceModel(name + ".native.help")), new Fragment("markup", name, this)));
	}

	/**
	 * @return the link showing the looks of a framework, marked as the current page for the one
	 *         shown
	 */
	private BookmarkablePageLink<Void> frameworkLink(Framework each)
	{
		BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(each.getName() + "Link",
			LooksPage.class, new PageParameters().set(FRAMEWORK, each.getName()));
		if (each == framework)
		{
			link.add(AttributeModifier.replace("aria-current", "page"));
		}
		return link;
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

	/**
	 * @return the style sheet of the framework shown only, whatever the look of the session
	 */
	@Override
	protected List<WebJarStyleSheetReference> getFrameworks()
	{
		return List.of(framework.styleSheet);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(LooksPage.class, "LooksPage.css")));
	}
}
