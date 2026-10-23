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
package com.antiliacode.widgets.demo.account;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.request.component.IRequestableComponent;

import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.admin.AdminPage;

/**
 * Keeps the demos, the private part of the application, for the visitors who signed in: anybody
 * else opening one is sent to the {@link SignInPage} and back to the demo after signing in. The
 * other pages, the home page with what the components are and do among them, are public, but
 * for the {@link AdminPage}, which only administrators see, also in development. An
 * administration opened before the session stopped being an administrator's, by signing out say,
 * neither renders again nor answers: Wicket checks a page when it makes one, not when it shows it
 * again.
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
		if (AdminPage.class.isAssignableFrom(componentClass))
		{
			return DemoSession.get().isAdmin();
		}
		return !DemoPage.class.isAssignableFrom(componentClass) || isOpen()
			|| DemoSession.get().isSignedIn();
	}

	@Override
	public boolean isActionAuthorized(Component component, Action action)
	{
		// the page itself: whatever is in it renders and answers only when it does
		return !(component instanceof AdminPage) || DemoSession.get().isAdmin();
	}

	@Override
	public void onUnauthorizedInstantiation(Component component)
	{
		turnAway();
	}

	/**
	 * Sends a visitor who did not sign in to the {@link SignInPage}, and back after signing in,
	 * and a user who did, but may not see the page, to the page saying so.
	 */
	public static void turnAway()
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
