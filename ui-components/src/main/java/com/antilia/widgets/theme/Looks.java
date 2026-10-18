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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.danekja.java.util.function.serializable.SerializableSupplier;

/**
 * Finds the {@link ILook} of a component: that of the closest {@link ThemeBehavior} with a look,
 * on the component or a parent of it; else the look of the session; else the default look of the
 * application; else {@link Look#ANTILIA}.
 * <p>
 * The look of the session is that of every page the user sees, once chosen on one of them: any
 * look, those of ui-components, one made of others or one of the application's own.
 *
 * <pre>
 * body.add(new ThemeBehavior(themeModel, Looks.sessionLook()));
 * form.add(new LookChoice("look", Looks.sessionLook()) { ... });
 * </pre>
 *
 * The default look of the application serves the sessions with none. The look of the session, or
 * the default, serves also the components that are not inside the component with the
 * {@link ThemeBehavior} in Wicket's hierarchy, though they are in its markup, as the components of
 * a page are inside a {@code <body>} that is a
 * {@link org.apache.wicket.markup.html.TransparentWebMarkupContainer}.
 *
 * @since 11.0.0
 */
public final class Looks
{
	private static final MetaDataKey<SerializableSupplier<? extends ILook>> DEFAULT =
		new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	private static final MetaDataKey<ILook> SESSION = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	private Looks()
	{
	}

	/**
	 * Sets the look of every page of the session, binding the session if it is temporary, so it
	 * lasts.
	 *
	 * @param look
	 *            the look, any; {@code null} for the default of the application
	 */
	public static void setSessionLook(ILook look)
	{
		Session session = Session.get();
		if (session.isTemporary())
		{
			session.bind();
		}
		session.setMetaData(SESSION, look);
	}

	/**
	 * @return the look of the session, {@code null} for none or outside a request
	 */
	public static ILook getSessionLook()
	{
		return Session.exists() ? Session.get().getMetaData(SESSION) : null;
	}

	/**
	 * @return the look of the session, else the default of the application, else
	 *         {@link Look#ANTILIA}: the look of the components outside any {@link ThemeBehavior}
	 *         with a look
	 */
	public static ILook current()
	{
		ILook session = getSessionLook();
		if (session != null)
		{
			return session;
		}
		SerializableSupplier<? extends ILook> look = Application.exists()
			? Application.get().getMetaData(DEFAULT) : null;
		ILook current = look != null ? look.get() : null;
		return current != null ? current : Look.ANTILIA;
	}

	/**
	 * @return a model of the look of the session: it reads {@link #current()} and sets the look of
	 *         the session, for a {@link ThemeBehavior} and a {@link LookChoice}
	 */
	public static IModel<ILook> sessionLook()
	{
		return new IModel<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public ILook getObject()
			{
				return current();
			}

			@Override
			public void setObject(ILook object)
			{
				setSessionLook(object);
			}
		};
	}

	/**
	 * @param application
	 *            the application
	 * @param look
	 *            the look of the sessions with none, asked each time; {@code null} for
	 *            {@link Look#ANTILIA}
	 */
	public static void setDefault(Application application,
		SerializableSupplier<? extends ILook> look)
	{
		application.setMetaData(DEFAULT, look);
	}

	/**
	 * @param component
	 *            a component
	 * @return the look the component is in
	 */
	public static ILook of(Component component)
	{
		for (Component current = component; current != null; current = current.getParent())
		{
			for (ThemeBehavior theme : current.getBehaviors(ThemeBehavior.class))
			{
				ILook look = theme.getLook();
				if (look != null)
				{
					return look;
				}
			}
		}
		return current();
	}

	/**
	 * Gives a tag the classes of the parts its element is: its own, those of {@link Look#ANTILIA},
	 * then those the look adds; each once, after the classes the tag has.
	 *
	 * @param tag
	 *            the tag of the element
	 * @param look
	 *            the look the element is in
	 * @param parts
	 *            the parts the element is, {@code null}s left out
	 */
	public static void addClasses(ComponentTag tag, ILook look, LookPart... parts)
	{
		String classes = look.getPartClasses(parts);
		if (classes.isEmpty())
		{
			return;
		}
		String current = tag.getAttribute("class");
		List<String> all = new ArrayList<>();
		if (current != null)
		{
			all.add(current);
		}
		all.add(classes);
		String merged = distinct(all);
		if (merged.equals(current) == false)
		{
			tag.put("class", merged);
		}
	}

	/**
	 * @param classes
	 *            CSS classes, each a word or several separated by spaces, {@code null}s left out
	 * @return the classes separated by spaces, each once, in their order
	 */
	static String distinct(List<String> classes)
	{
		Set<String> words = new LinkedHashSet<>();
		for (String css : classes)
		{
			if (Strings.isEmpty(css) == false)
			{
				words.addAll(Arrays.asList(css.trim().split("\\s+")));
			}
		}
		return String.join(" ", words);
	}
}
