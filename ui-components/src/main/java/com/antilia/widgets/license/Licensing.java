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
package com.antilia.widgets.license;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antilia.widgets.theme.LookPart;
import com.antilia.widgets.theme.Looks;

/**
 * The license key of an application, which activates the private components of ui-components.
 * Public components work without a key; a private one without a key that activates it renders a
 * placeholder telling so, in place of itself.
 * <p>
 * The key is the first found of:
 * <ol>
 * <li>the one given to {@link #install(Application, String)}, for example in
 * {@code Application#init()};</li>
 * <li>the system property {@value #PROPERTY};</li>
 * <li>the environment variable {@value #ENVIRONMENT_VARIABLE};</li>
 * <li>the resource {@value #RESOURCE} on the class path.</li>
 * </ol>
 * A key that is wrong or expired is logged and activates nothing.
 * <p>
 * For now the key is not enforced: every private component renders, with or without a key, unless
 * the system property {@value #ENFORCED_PROPERTY} is {@code true}, as in the tests of the library.
 *
 * @since 11.0.0
 */
public final class Licensing
{
	/** The system property holding the key. */
	public static final String PROPERTY = "antilia-widgets.license";

	/** The environment variable holding the key. */
	public static final String ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_LICENSE";

	/** The resource on the class path holding the key. */
	public static final String RESOURCE = "META-INF/antilia-widgets.license";

	/**
	 * The system property that, {@code true}, makes the private components need a key again. For
	 * now they don't need one without it.
	 */
	public static final String ENFORCED_PROPERTY = "antilia-widgets.license.enforced";

	/** The CSS class of the placeholder of a private component. */
	public static final String PLACEHOLDER_CSS_CLASS = "antilia-unlicensed";

	private static final Logger LOG = LoggerFactory.getLogger(Licensing.class);

	private static final MetaDataKey<Licensing> KEY = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	private final LicenseKey key;

	private final Clock clock;

	private final boolean enforced;

	private Licensing(LicenseKey key, Clock clock)
	{
		this.key = key;
		this.clock = clock;
		enforced = Boolean.getBoolean(ENFORCED_PROPERTY);
	}

	/**
	 * Gives the application a key, replacing the one it had.
	 *
	 * @param application
	 *            the application
	 * @param key
	 *            the text of the key, {@code null} for none
	 * @return the licensing of the application
	 */
	public static Licensing install(Application application, String key)
	{
		Licensing licensing = new Licensing(read(key, "the application"), Clock.systemDefaultZone());
		application.setMetaData(KEY, licensing);
		return licensing;
	}

	/**
	 * @return the licensing of the application, looking up its key the first time
	 */
	public static Licensing get(Application application)
	{
		Licensing licensing = application.getMetaData(KEY);
		if (licensing == null)
		{
			licensing = lookUp(application);
			application.setMetaData(KEY, licensing);
		}
		return licensing;
	}

	/**
	 * @return the licensing of the current application
	 */
	public static Licensing get()
	{
		return get(Application.get());
	}

	/**
	 * @return the key, {@code null} if there is none or it is wrong
	 */
	public LicenseKey getKey()
	{
		return key;
	}

	/**
	 * @return whether the private component class is active today: always while the key is not
	 *         enforced, else if the key activates it
	 */
	public boolean activates(Class<?> componentClass)
	{
		return !enforced || key != null && key.isValidOn(LocalDate.now(clock)) &&
			key.activates(componentClass);
	}

	/**
	 * Lets a private component render only if the key of the application activates it, else its
	 * placeholder: call it from the component's {@code onConfigure()}. A component that is not
	 * rendered contributes no script or style to the page and renders none of its children.
	 *
	 * @param component
	 *            the private component
	 */
	public static void configurePrivate(Component component)
	{
		boolean active = get(component.getApplication()).activates(component.getClass());
		if (!active)
		{
			component.setOutputMarkupPlaceholderTag(true);
		}
		component.setVisibilityAllowed(active);
	}

	/**
	 * Renders the placeholder of a private component the key does not activate: call it from the
	 * component's {@code renderPlaceholderTag(ComponentTag, Response)}.
	 *
	 * @param component
	 *            the private component
	 * @param tag
	 *            the tag of the component
	 * @param response
	 *            the response to write to
	 * @return whether the placeholder was rendered; if not, the component renders the usual
	 *         placeholder of an invisible component
	 */
	public static boolean renderPlaceholder(Component component, ComponentTag tag,
		Response response)
	{
		if (component.isVisibilityAllowed())
		{
			return false;
		}
		String text = component.getString("License.placeholder", null,
			"This component needs a license key.");
		response.write("<div id=\"" + component.getAjaxRegionMarkupId() + "\" class=\"" +
			Looks.of(component).getPartClasses(LookPart.UNLICENSED) +
			"\" role=\"note\" data-component=\"" +
			Strings.escapeMarkup(namedClass(component.getClass()).getSimpleName()) + "\">" +
			Strings.escapeMarkup(text) + "</div>");
		return true;
	}

	/**
	 * @return the class, or the first of its superclasses that has a name, as an anonymous
	 *         subclass of a component has none
	 */
	private static Class<?> namedClass(Class<?> type)
	{
		Class<?> named = type;
		while (named.isAnonymousClass())
		{
			named = named.getSuperclass();
		}
		return named;
	}

	private static Licensing lookUp(Application application)
	{
		String key = System.getProperty(PROPERTY);
		String source = "the system property " + PROPERTY;
		if (Strings.isEmpty(key))
		{
			key = System.getenv(ENVIRONMENT_VARIABLE);
			source = "the environment variable " + ENVIRONMENT_VARIABLE;
		}
		if (Strings.isEmpty(key))
		{
			key = readResource(application);
			source = "the resource " + RESOURCE;
		}
		return new Licensing(Strings.isEmpty(key) ? null : read(key, source),
			Clock.systemDefaultZone());
	}

	private static String readResource(Application application)
	{
		ClassLoader loader = application.getApplicationSettings().getClassResolver()
			.getClassLoader();
		try (InputStream in = loader.getResourceAsStream(RESOURCE))
		{
			return in != null ? new String(in.readAllBytes(), StandardCharsets.UTF_8) : null;
		}
		catch (IOException e)
		{
			LOG.warn("Reading the license key from {} failed", RESOURCE, e);
			return null;
		}
	}

	private static LicenseKey read(String text, String source)
	{
		if (Strings.isEmpty(text))
		{
			return null;
		}
		try
		{
			LicenseKey key = LicenseKey.parse(text);
			LOG.info("Private components of ui-components activated by {} from {}", key, source);
			return key;
		}
		catch (InvalidLicenseKeyException e)
		{
			LOG.warn("The license key from {} is not valid, private components stay inactive: {}",
				source, e.getMessage());
			return null;
		}
	}
}
