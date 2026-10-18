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
package com.antilia.widgets.theme;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * The looks of ui-components, the same as those of the browser's {@code look.ts}: the classes they
 * give the parts are read from {@value #TABLE}, which the build writes from it.
 *
 * @since 11.0.0
 */
public enum Look implements ILook
{
	/** The components' own style sheets; the default. */
	ANTILIA,

	/**
	 * Bootstrap 5.3: its classes, such as {@code btn btn-primary btn-sm} or {@code modal-header},
	 * with {@link #BOOTSTRAP_CSS} bridging them to the components. The page brings the style sheet
	 * of Bootstrap, any build of it from 5.3 on, such as a theme of Bootswatch; never its script.
	 */
	BOOTSTRAP
	{
		@Override
		public void renderHead(IHeaderResponse response)
		{
			response.render(CssHeaderItem.forReference(BOOTSTRAP_CSS));
		}

		@Override
		public void onComponentTag(ComponentTag tag, Theme theme)
		{
			if (theme != null)
			{
				// Bootstrap's own components in the color mode of the theme
				tag.put(Theme.BOOTSTRAP_MODE_ATTRIBUTE, theme.isDark() ? "dark" : "light");
			}
		}
	};

	/**
	 * The table of the looks, next to this class: the aspect of each part, as
	 * {@code aspect.<part>}, and the classes of the parts in each look, as {@code <look>.<part>}.
	 */
	public static final String TABLE = "antilia-looks.properties";

	/**
	 * The style sheet of the Bootstrap look: Bootstrap's shapes for the components, and the colors
	 * of the theme for Bootstrap's components and custom properties.
	 */
	public static final ResourceReference BOOTSTRAP_CSS = new CssResourceReference(Look.class,
		"antilia-look-bootstrap.css");

	@Override
	public String getName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	@Override
	public String getCssClass(LookPart part)
	{
		return Table.CLASSES.get(this).get(part);
	}

	/** The table, read once. */
	static final class Table
	{
		/** The table as the build wrote it. */
		static final Properties PROPERTIES = new Properties();

		/** The classes of the parts in each look. */
		static final Map<Look, Map<LookPart, String>> CLASSES = new EnumMap<>(Look.class);

		static
		{
			try (InputStream in = Look.class.getResourceAsStream(TABLE))
			{
				if (in == null)
				{
					throw new WicketRuntimeException(TABLE + " is missing: build frontend/core");
				}
				PROPERTIES.load(in);
			}
			catch (IOException e)
			{
				throw new WicketRuntimeException("Cannot read " + TABLE, e);
			}
			for (Look look : Look.values())
			{
				Map<LookPart, String> classes = new EnumMap<>(LookPart.class);
				for (LookPart part : LookPart.values())
				{
					String css = PROPERTIES.getProperty(look.getName() + "." + part.getName());
					if (css != null)
					{
						classes.put(part, css);
					}
				}
				CLASSES.put(look, classes);
			}
		}

		private Table()
		{
		}
	}
}
