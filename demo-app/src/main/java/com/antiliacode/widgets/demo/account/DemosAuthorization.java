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
import org.apache.wicket.authorization.IUnauthorizedResourceRequestListener;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestHandlerDelegate;
import org.apache.wicket.request.component.IRequestableComponent;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.http.handler.ErrorCodeRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.DevelopmentSwitches;
import com.antiliacode.widgets.demo.LiveResource;
import com.antiliacode.widgets.demo.admin.AccessTimeResource;
import com.antiliacode.widgets.demo.admin.AccessesApiResource;
import com.antiliacode.widgets.demo.admin.AdminPage;
import com.antiliacode.widgets.demo.admin.UsersApiResource;
import com.antiliacode.widgets.demo.datatable.FontAwesomeResourceReference;
import com.antiliacode.widgets.demo.theme.BootstrapResourceReference;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Keeps the demos, the private part of the application, for the visitors who signed in: anybody
 * else opening one is sent to the {@link SignInPage} and back to the demo after signing in. The
 * other pages, the home page with what the components are and do among them, are public, but
 * for the {@link AdminPage}, which only administrators see, also in development. An
 * administration opened before the session stopped being an administrator's, by signing out say,
 * neither renders again nor answers: Wicket checks a page when it makes one, not when it shows it
 * again.
 * <p>
 * The resources the application serves on their own, mounted or shared, such as the JSON of the
 * React and Svelte demos at {@code /api/...}, follow the demos: only visitors who signed in get
 * them, unless the demos are open. A visitor who did not is answered 403. The public pages need
 * a few of them, which everybody gets: the scripts, style sheets and images of the components and
 * the pages (package resources), the style sheets of Bootstrap and Font Awesome, the visitor's
 * clock ({@link AccessTimeResource}) and the websocket of the home page ({@link LiveResource}).
 * So does the API of the administration ({@link UsersApiResource}, {@link AccessesApiResource}),
 * which lets in administrators itself, and every resource a component of a page serves, such as
 * the image of a {@link Captcha}: the page was let in already.
 * <p>
 * The demos are open to everybody in development, and when the web application has the init
 * parameter {@value #OPEN_PARAMETER} set to {@code true}, as the browser tests have it: they run
 * in deployment, as production does, whose {@code web.xml} never sets it. The system property of
 * that name is ignored in deployment, see {@link DevelopmentSwitches}.
 */
public class DemosAuthorization extends IAuthorizationStrategy.AllowAllAuthorizationStrategy
	implements IUnauthorizedComponentInstantiationListener, IUnauthorizedResourceRequestListener
{
	/** The init parameter of the web application opening the demos to everybody. */
	public static final String OPEN_PARAMETER = "antilia-widgets.open-demos";

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
	public boolean isResourceAuthorized(IResource resource, PageParameters parameters)
	{
		return isPublic(resource) || isServedByPage() || isOpen() ||
			DemoSession.get().isSignedIn();
	}

	/**
	 * @return whether everybody gets the resource, as the public pages need it or as it lets in
	 *         whom it serves itself
	 */
	private static boolean isPublic(IResource resource)
	{
		if (resource instanceof PackageResource || resource instanceof AccessTimeResource ||
			resource instanceof LiveResource || resource instanceof UsersApiResource ||
			resource instanceof AccessesApiResource)
		{
			return true;
		}
		// the style sheets of the web jars are served by lambdas: told apart by their references
		IRequestHandler handler = activeHandler();
		if (handler instanceof ResourceReferenceRequestHandler reference)
		{
			ResourceReference served = reference.getResourceReference();
			return served instanceof PackageResourceReference ||
				served instanceof BootstrapResourceReference ||
				served instanceof FontAwesomeResourceReference;
		}
		return false;
	}

	/**
	 * @return whether a component of a page serves the resource, such as an image: the page was
	 *         let in when it was made
	 */
	private static boolean isServedByPage()
	{
		return activeHandler() instanceof IPageRequestHandler;
	}

	/**
	 * @return the handler of the request that asks for the resource, {@code null} if none
	 */
	private static IRequestHandler activeHandler()
	{
		RequestCycle cycle = RequestCycle.get();
		IRequestHandler handler = cycle != null ? cycle.getActiveRequestHandler() : null;
		while (handler instanceof IRequestHandlerDelegate delegate)
		{
			handler = delegate.getDelegateHandler();
		}
		return handler;
	}

	@Override
	public void onUnauthorizedRequest(IResource resource, PageParameters parameters)
	{
		// without telling what was asked for, as Wicket's default listener does
		RequestCycle.get().replaceAllRequestHandlers(
			new ErrorCodeRequestHandler(HttpServletResponse.SC_FORBIDDEN, "Sign in first"));
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
	 * @return whether the demos are open to everybody, signed in or not: in development, or with
	 *         the init parameter {@value #OPEN_PARAMETER} of the web application
	 */
	public static boolean isOpen()
	{
		Application application = Application.get();
		return application.getConfigurationType() == RuntimeConfigurationType.DEVELOPMENT ||
			application instanceof WebApplication web && web.getServletContext() != null &&
				Boolean.parseBoolean(web.getServletContext().getInitParameter(OPEN_PARAMETER));
	}
}
