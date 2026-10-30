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
package com.antiliacode.widgets.throttle;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes.Method;
import org.apache.wicket.core.request.handler.ListenerRequestHandler;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.AbstractRequestWrapperFactory;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.mapper.info.PageComponentInfo;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;

import jakarta.servlet.ServletRequestWrapper;
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
 * Only the Ajax requests of the session's pages and their behaviors wait, the requests of the
 * components and behaviors the session uses; not those of resources, websockets or pages
 * rendered. A slowed down request holds a thread of the server while it waits, so at most
 * {@value #MAX_SLOWED} requests of a session are slowed down at once, waiting or sending files;
 * the others go at full speed. A request waits before Wicket takes the lock of its page, so that
 * the other requests of the page do not wait for it. An application open to anybody shows the components to those
 * trusted with it only, and {@link #reset(HttpServletRequest)} undoes the settings, when the user
 * signs out for example.
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

	/** How many requests of a session are slowed down at once, at most. */
	public static final int MAX_SLOWED = 2;

	/** The attribute of the HTTP session holding the permits of its requests to slow down. */
	static final String PERMITS = Permits.class.getName();

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
	 * Undoes the settings of the session: nothing slows down any more.
	 *
	 * @param request
	 *            a request of the session
	 */
	public static void reset(HttpServletRequest request)
	{
		setAjaxDelay(request, AjaxDelay.NONE);
		setUploadSpeed(request, UploadSpeed.FULL);
	}

	/**
	 * @param request
	 *            a request of the session
	 * @return the permits of the session's requests to slow down, {@value #MAX_SLOWED} at first;
	 *         {@code null} without a session
	 */
	static Semaphore permits(HttpServletRequest request)
	{
		HttpSession session = request.getSession(false);
		if (session == null)
		{
			return null;
		}
		Object permits = session.getAttribute(PERMITS);
		if (!(permits instanceof Permits))
		{
			// the requests of a session making them at once share the first
			synchronized (Permits.class)
			{
				permits = session.getAttribute(PERMITS);
				if (!(permits instanceof Permits))
				{
					permits = new Permits();
					session.setAttribute(PERMITS, permits);
				}
			}
		}
		return ((Permits)permits).get();
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
	 * @return the request, its body read at the upload speed of the session when it sends files,
	 *         unless {@value #MAX_SLOWED} requests of the session are slowed down already
	 */
	static HttpServletRequest slowDown(HttpServletRequest request)
	{
		if (!isUpload(request))
		{
			return request;
		}
		UploadSpeed speed = getUploadSpeed(request);
		Semaphore permits = speed == UploadSpeed.FULL ? null : permits(request);
		if (permits == null || !permits.tryAcquire())
		{
			return request;
		}
		return new UploadSpeed.SlowRequest(request, speed.getBytesPerSecond(), permits);
	}

	/**
	 * @return the request reading its body slowly, which holds a permit, {@code null} if none
	 */
	static UploadSpeed.SlowRequest slowRequest(Request request)
	{
		Object container = request != null ? request.getContainerRequest() : null;
		while (container instanceof ServletRequestWrapper wrapper)
		{
			if (wrapper instanceof UploadSpeed.SlowRequest slow)
			{
				return slow;
			}
			container = wrapper.getRequest();
		}
		return null;
	}

	/**
	 * @param url
	 *            the address of a request
	 * @return whether the request calls a component or a behavior of a page: one of its query
	 *         parameters without a value names the page and the component, {@code ?3-1.0-form-save}
	 *         say, as the addresses of a {@link ListenerRequestHandler}, and not only the page, as
	 *         those of a page rendered, nor nothing, as those of a resource
	 */
	static boolean callsComponent(Url url)
	{
		for (Url.QueryParameter parameter : url.getQueryParameters())
		{
			if (Strings.isEmpty(parameter.getValue()))
			{
				PageComponentInfo info = PageComponentInfo.parse(parameter.getName());
				if (info != null)
				{
					return info.getComponentInfo() != null;
				}
			}
		}
		return false;
	}

	private static boolean isUpload(HttpServletRequest request)
	{
		String type = request.getContentType();
		return "POST".equals(request.getMethod()) && type != null &&
			type.toLowerCase(Locale.ROOT).startsWith("multipart/");
	}

	/**
	 * Waits for the {@link AjaxDelay} of the session at the beginning of each of its Ajax requests
	 * calling a component or behavior, but those exempted, and frees the permits of the requests
	 * slowed down once they are done.
	 * <p>
	 * It waits before Wicket resolves the request, which takes the lock of the page: a request
	 * waiting with the lock would keep every other request of the page waiting for it, each
	 * holding a thread of the server, however few requests the permits let wait. So it tells a
	 * request calling a component by its address alone, as Wicket's mappers do, see
	 * {@link #callsComponent(Url)}.
	 */
	static final class AjaxDelayListener implements IRequestCycleListener
	{
		@Override
		public void onBeginRequest(RequestCycle cycle)
		{
			Request request = cycle.getRequest();
			if (!(request instanceof WebRequest web) || !web.isAjax() ||
				!(web.getContainerRequest() instanceof HttpServletRequest http) ||
				!web.getQueryParameters().getParameterValue(EXEMPT_PARAMETER).isEmpty() ||
				!callsComponent(web.getUrl()))
			{
				return;
			}
			long millis = getAjaxDelay(http).getDuration().toMillis();
			if (millis <= 0)
			{
				return;
			}
			// a request sending files slowly holds a permit already, for its thread
			UploadSpeed.SlowRequest upload = slowRequest(request);
			boolean holding = upload != null && upload.holdsPermit();
			Semaphore permits = holding ? null : permits(http);
			if (!holding && (permits == null || !permits.tryAcquire()))
			{
				return;
			}
			try
			{
				Thread.sleep(millis);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			finally
			{
				if (permits != null)
				{
					permits.release();
				}
			}
		}

		@Override
		public void onDetach(RequestCycle cycle)
		{
			UploadSpeed.SlowRequest upload = slowRequest(cycle.getRequest());
			if (upload != null)
			{
				upload.release();
			}
		}
	}

	/**
	 * The permits of the requests of a session to slow down, kept in the session: made anew when
	 * the session is.
	 */
	static final class Permits implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private transient Semaphore semaphore;

		synchronized Semaphore get()
		{
			if (semaphore == null)
			{
				semaphore = new Semaphore(MAX_SLOWED);
			}
			return semaphore;
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
