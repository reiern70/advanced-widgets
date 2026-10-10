/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.account;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.request.component.IRequestableComponent;

import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.admin.AccessesPage;

/**
 * Keeps the demos, the private part of the application, for the visitors who signed in: anybody
 * else opening one is sent to the {@link SignInPage} and back to the demo after signing in. The
 * other pages, the home page with what the components are and do among them, are public, but
 * for the {@link AccessesPage}, which only administrators see, also in development.
 * <p>
 * The demos are open to everybody in development, and when the server is started with
 * {@code -D}{@value #OPEN_PROPERTY}{@code =true}, as the browser tests are.
 */
public class DemosAuthorization extends IAuthorizationStrategy.AllowAllAuthorizationStrategy
	implements IUnauthorizedComponentInstantiationListener
{
	/** The system property opening the demos to everybody. */
	public static final String OPEN_PROPERTY = "antilia-widgets.open-demos";

	@Override
	public <T extends IRequestableComponent> boolean isInstantiationAuthorized(
		Class<T> componentClass)
	{
		if (AccessesPage.class.isAssignableFrom(componentClass))
		{
			return DemoSession.get().isAdmin();
		}
		return !DemoPage.class.isAssignableFrom(componentClass) || isOpen()
			|| DemoSession.get().isSignedIn();
	}

	@Override
	public void onUnauthorizedInstantiation(Component component)
	{
		if (DemoSession.get().isSignedIn())
		{
			// signed in, but not as an administrator
			throw new RestartResponseException(
				Application.get().getApplicationSettings().getAccessDeniedPage());
		}
		throw new RestartResponseAtInterceptPageException(SignInPage.class);
	}

	/**
	 * @return whether the demos are open to everybody, signed in or not
	 */
	public static boolean isOpen()
	{
		return Application.get().getConfigurationType() == RuntimeConfigurationType.DEVELOPMENT
			|| Boolean.getBoolean(OPEN_PROPERTY);
	}
}
