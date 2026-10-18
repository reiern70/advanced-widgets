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
package com.antilia.widgets.throttle;

import java.util.Locale;

import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes.Method;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.AbstractRequestWrapperFactory;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.util.lang.Args;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Slows the server down for a session, to see a page as on a slow network or with a busy server:
 * it waits for an {@link AjaxDelay} before taking on each Wicket Ajax request, and reads the files
 * the session sends at an {@link UploadSpeed}. Nothing slows down for the other sessions. The
 * settings are kept in the HTTP session, as the body of an upload is slowed down before Wicket
 * knows the session of the request; {@link ThrottlePanel} and {@link ThrottleButton} let the user
 * change them.
 * <p>
 * The application installs the throttle in its {@code init()}:
 *
 * <pre>
 * Throttle.install(this);
 * </pre>
 *
 * which adds a listener of the request cycles waiting before the Ajax requests, and a factory of
 * {@link WebApplication#getFilterFactoryManager() request wrappers} reading the uploads slowly.
 * Without it, the settings change nothing. The requests of the throttle's own components never
 * wait: they carry the parameter {@value #EXEMPT_PARAMETER}, see
 * {@link #exempt(AjaxRequestAttributes)}.
 * <p>
 * A slowed down request holds a thread of the server while it waits: an application open to
 * anybody shows the components to those trusted with it only.
 *
 * @since 11.0.0
 */
public final class Throttle
{
	/** The parameter of the Ajax requests that never wait. */
	public static final String EXEMPT_PARAMETER = "antilia-throttle-exempt";

	/** The attribute of the HTTP session holding the {@link AjaxDelay}. */
	static final String AJAX_DELAY = AjaxDelay.class.getName();

	/** The attribute of the HTTP session holding the {@link UploadSpeed}. */
	static final String UPLOAD_SPEED = UploadSpeed.class.getName();

	private static final MetaDataKey<Boolean> INSTALLED = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	private Throttle()
	{
	}

	/**
	 * Has the application slow down the sessions as their settings say. Installing it again does
	 * nothing.
	 *
	 * @param application
	 *            the application
	 */
	public static void install(WebApplication application)
	{
		if (isInstalled(application))
		{
			return;
		}
		application.setMetaData(INSTALLED, Boolean.TRUE);
		application.getRequestCycleListeners().add(new AjaxDelayListener());
		application.getFilterFactoryManager().add(new SlowUploads());
	}

	/**
	 * @param application
	 *            the application
	 * @return whether {@link #install(WebApplication)} installed the throttle in the application
	 */
	public static boolean isInstalled(Application application)
	{
		return Boolean.TRUE.equals(application.getMetaData(INSTALLED));
	}

	/**
	 * @param request
	 *            a request of the session
	 * @return how long the server waits before each Ajax request of the session, none by default
	 */
	public static AjaxDelay getAjaxDelay(HttpServletRequest request)
	{
		return get(request, AJAX_DELAY, AjaxDelay.class, AjaxDelay.NONE);
	}

	/**
	 * @param request
	 *            a request of the session
	 * @param delay
	 *            how long the server waits before each Ajax request of the session
	 */
	public static void setAjaxDelay(HttpServletRequest request, AjaxDelay delay)
	{
		set(request, AJAX_DELAY, Args.notNull(delay, "delay"), AjaxDelay.NONE);
	}

	/**
	 * @param request
	 *            a request of the session
	 * @return how fast the server reads the files the session sends, at full speed by default
	 */
	public static UploadSpeed getUploadSpeed(HttpServletRequest request)
	{
		return get(request, UPLOAD_SPEED, UploadSpeed.class, UploadSpeed.FULL);
	}

	/**
	 * @param request
	 *            a request of the session
	 * @param speed
	 *            how fast the server reads the files the session sends
	 */
	public static void setUploadSpeed(HttpServletRequest request, UploadSpeed speed)
	{
		set(request, UPLOAD_SPEED, Args.notNull(speed, "speed"), UploadSpeed.FULL);
	}

	/**
	 * @param request
	 *            a request of the session
	 * @return whether the server slows down for the session
	 */
	public static boolean isSlowingDown(HttpServletRequest request)
	{
		return getAjaxDelay(request) != AjaxDelay.NONE || getUploadSpeed(request) != UploadSpeed.FULL;
	}

	/**
	 * Has an Ajax request never wait: it goes by GET, carrying {@value #EXEMPT_PARAMETER}, as
	 * those of the throttle's own components do, so that slowing down can be undone at once.
	 *
	 * @param attributes
	 *            the attributes of the request
	 */
	public static void exempt(AjaxRequestAttributes attributes)
	{
		attributes.setMethod(Method.GET);
		attributes.getExtraParameters().put(EXEMPT_PARAMETER, "true");
	}

	private static <T extends Enum<T>> T get(HttpServletRequest request, String name,
		Class<T> type, T none)
	{
		HttpSession session = request.getSession(false);
		Object value = session != null ? session.getAttribute(name) : null;
		return type.isInstance(value) ? type.cast(value) : none;
	}

	private static <T extends Enum<T>> void set(HttpServletRequest request, String name, T value,
		T none)
	{
		if (value != none)
		{
			request.getSession(true).setAttribute(name, value);
			return;
		}
		HttpSession session = request.getSession(false);
		if (session != null)
		{
			session.removeAttribute(name);
		}
	}

	/**
	 * @param request
	 *            a request
	 * @return the request, its body read at the upload speed of the session when it sends files
	 */
	static HttpServletRequest slowDown(HttpServletRequest request)
	{
		if (!isUpload(request))
		{
			return request;
		}
		UploadSpeed speed = getUploadSpeed(request);
		return speed == UploadSpeed.FULL ? request
			: new UploadSpeed.SlowRequest(request, speed.getBytesPerSecond());
	}

	private static boolean isUpload(HttpServletRequest request)
	{
		String type = request.getContentType();
		return "POST".equals(request.getMethod()) && type != null &&
			type.toLowerCase(Locale.ROOT).startsWith("multipart/");
	}

	/**
	 * Waits for the {@link AjaxDelay} of the session before taking on each of its Ajax requests,
	 * but those exempted.
	 */
	static final class AjaxDelayListener implements IRequestCycleListener
	{
		@Override
		public void onBeginRequest(RequestCycle cycle)
		{
			Request request = cycle.getRequest();
			if (!(request instanceof WebRequest web) || !web.isAjax() ||
				!(web.getContainerRequest() instanceof HttpServletRequest http) ||
				!web.getQueryParameters().getParameterValue(EXEMPT_PARAMETER).isEmpty())
			{
				return;
			}
			long millis = getAjaxDelay(http).getDuration().toMillis();
			if (millis > 0)
			{
				try
				{
					Thread.sleep(millis);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	/**
	 * Wraps the requests sending files, their bodies read at the upload speed of the session.
	 */
	static final class SlowUploads extends AbstractRequestWrapperFactory
	{
		@Override
		protected boolean needsWrapper(HttpServletRequest request)
		{
			return isUpload(request) && getUploadSpeed(request) != UploadSpeed.FULL;
		}

		@Override
		public HttpServletRequest newRequestWrapper(HttpServletRequest request)
		{
			return slowDown(request);
		}
	}
}
