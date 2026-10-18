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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.protocol.http.servlet.AbstractRequestWrapperFactory;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebResponse;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Checks {@link Throttle}, its settings kept in the HTTP session, the Ajax requests waiting and the
 * uploads read slowly once installed, and {@link ThrottlePanel} and {@link ThrottleButton} changing
 * the settings.
 */
class ThrottleTest extends WicketTestCase
{
	private MockHttpServletRequest multipart()
	{
		MockHttpServletRequest request = tester.getRequest();
		request.setMethod("POST");
		request.setUseMultiPartContentType(true);
		return request;
	}

	@Test
	void keepsTheSettingsInTheSession()
	{
		MockHttpServletRequest request = tester.getRequest();
		assertSame(AjaxDelay.NONE, Throttle.getAjaxDelay(request));
		assertSame(UploadSpeed.FULL, Throttle.getUploadSpeed(request));
		assertFalse(Throttle.isSlowingDown(request));

		Throttle.setAjaxDelay(request, AjaxDelay.SECOND);
		assertSame(AjaxDelay.SECOND, Throttle.getAjaxDelay(request));
		assertTrue(Throttle.isSlowingDown(request));
		Throttle.setUploadSpeed(request, UploadSpeed.SLOW);
		assertSame(UploadSpeed.SLOW, Throttle.getUploadSpeed(request));
		assertEquals(64 * 1024, UploadSpeed.SLOW.getBytesPerSecond());
		assertEquals(1000, AjaxDelay.SECOND.getDuration().toMillis());

		Throttle.setAjaxDelay(request, AjaxDelay.NONE);
		Throttle.setUploadSpeed(request, UploadSpeed.FULL);
		assertNull(request.getSession().getAttribute(Throttle.AJAX_DELAY), "nothing kept");
		assertNull(request.getSession().getAttribute(Throttle.UPLOAD_SPEED));
		assertFalse(Throttle.isSlowingDown(request));
		request.getSession().setAttribute(Throttle.UPLOAD_SPEED, "fast");
		assertSame(UploadSpeed.FULL, Throttle.getUploadSpeed(request));
		assertThrows(IllegalArgumentException.class, () -> Throttle.setAjaxDelay(request, null));
	}

	@Test
	void installsOnce()
	{
		WebApplication application = tester.getApplication();
		assertFalse(Throttle.isInstalled(application));
		Throttle.install(application);
		Throttle.install(application);
		assertTrue(Throttle.isInstalled(application));
		assertEquals(1, StreamSupport.stream(application.getRequestCycleListeners().spliterator(),
			false).filter(Throttle.AjaxDelayListener.class::isInstance).count());
		assertEquals(1, StreamSupport.stream(application.getFilterFactoryManager().spliterator(),
			false).filter(Throttle.SlowUploads.class::isInstance).count());
	}

	@Test
	void slowsDownOnlyTheRequestsSendingFiles()
	{
		MockHttpServletRequest request = tester.getRequest();
		Throttle.setUploadSpeed(request, UploadSpeed.MEGABYTE);
		AbstractRequestWrapperFactory uploads = new Throttle.SlowUploads();
		assertSame(request, uploads.getWrapper(request), "a GET");
		request.setMethod("POST");
		assertSame(request, uploads.getWrapper(request), "a form without files");

		HttpServletRequest slow = uploads.getWrapper(multipart());
		assertNotSame(request, slow);
		assertInstanceOf(UploadSpeed.SlowRequest.class, slow);
		Throttle.setUploadSpeed(request, UploadSpeed.FULL);
		assertSame(request, uploads.getWrapper(request), "at full speed");
		assertSame(request, Throttle.slowDown(request));
	}

	@Test
	void readsTheBodyAtTheSpeedOfTheSession() throws IOException
	{
		MockHttpServletRequest request = multipart();
		request.addFile("file", null, "text/plain");
		Throttle.setUploadSpeed(request, UploadSpeed.QUARTER_MEGABYTE);
		HttpServletRequest slow = Throttle.slowDown(request);
		ServletInputStream body = slow.getInputStream();
		assertSame(body, slow.getInputStream(), "the same stream every time");
		assertTrue(body.read() >= 0);
		body.readAllBytes();
		assertTrue(body.isFinished() || body.read() < 0);
		body.close();
	}

	@Test
	void keepsTheRate() throws IOException
	{
		byte[] data = new byte[20 * 1024];
		Arrays.fill(data, (byte)7);
		ServletInputStream slow = new UploadSpeed.SlowInputStream(new Body(data), 100 * 1024);

		long started = System.nanoTime();
		ByteArrayOutputStream read = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		for (int count; (count = slow.read(buffer, 0, buffer.length)) >= 0;)
		{
			assertTrue(count <= 100 * 1024 / 20, "a twentieth of a second's bytes at most");
			read.write(buffer, 0, count);
		}
		long millis = (System.nanoTime() - started) / 1_000_000;
		assertArrayEquals(data, read.toByteArray());
		assertTrue(millis >= 150, "20 kB at 100 kB/s take a fifth of a second: " + millis);
	}

	@Test
	void passesTheStateOfTheBodyOn() throws IOException
	{
		Body body = new Body(new byte[] { 1 });
		UploadSpeed.SlowInputStream slow = new UploadSpeed.SlowInputStream(body, 1024);
		assertFalse(slow.isFinished());
		assertTrue(slow.isReady());
		ReadListener listener = new ReadListener()
		{
			@Override
			public void onDataAvailable()
			{
			}

			@Override
			public void onAllDataRead()
			{
			}

			@Override
			public void onError(Throwable error)
			{
			}
		};
		slow.setReadListener(listener);
		assertSame(listener, body.listener);
		assertEquals(1, slow.read());
		assertEquals(-1, slow.read());
		assertTrue(slow.isFinished());
		slow.close();
		assertTrue(body.closed.get());
	}

	@Test
	void stopsReadingWhenInterrupted()
	{
		UploadSpeed.SlowInputStream slow = new UploadSpeed.SlowInputStream(
			new Body(new byte[4096]), 1);
		Thread.currentThread().interrupt();
		try
		{
			assertThrows(IOException.class, () -> {
				slow.read();
				slow.read();
			});
		}
		finally
		{
			Thread.interrupted();
		}
	}

	/**
	 * @return how long the listener of the throttle kept a request waiting, in milliseconds
	 */
	private long waited(MockHttpServletRequest request)
	{
		ServletWebRequest web = new ServletWebRequest(request, "");
		RequestCycle cycle = tester.getApplication().createRequestCycle(web,
			new ServletWebResponse(web, tester.getResponse()));
		long started = System.nanoTime();
		new Throttle.AjaxDelayListener().onBeginRequest(cycle);
		return (System.nanoTime() - started) / 1_000_000;
	}

	@Test
	void keepsTheAjaxRequestsWaitingButThoseExempted()
	{
		MockHttpServletRequest request = tester.getRequest();
		Throttle.setAjaxDelay(request, AjaxDelay.HALF_SECOND);
		assertTrue(waited(request) < 400, "not an Ajax request");

		request.addHeader("Wicket-Ajax", "true");
		assertTrue(waited(request) >= 450, "an Ajax request");
		request.setURL(request.getRequestURL() + "?" + Throttle.EXEMPT_PARAMETER + "=true");
		assertTrue(waited(request) < 400, "the throttle's own");

		Thread.currentThread().interrupt();
		try
		{
			request.setURL(request.getRequestURL().toString());
			assertTrue(waited(request) < 400, "interrupted");
			assertTrue(Thread.currentThread().isInterrupted());
		}
		finally
		{
			Thread.interrupted();
		}
	}

	private String input(String path)
	{
		return ((FormComponent<?>)tester.getComponentFromLastRenderedPage(path)).getInputName();
	}

	@Test
	void changesTheSettingsAndTellsTheOtherComponents()
	{
		tester.startPage(new TestPage());
		tester.assertVisible("panel:notInstalled");
		tester.assertInvisible("button:state");
		String page = tester.getLastResponseAsString();
		assertTrue(page.contains(Throttle.EXEMPT_PARAMETER), page);
		assertTrue(page.contains("antilia-throttle.css"), page);
		assertTrue(page.contains("antilia-popover.js"), page);

		tester.getRequest().getPostParameters().setParameterValue(input("panel:ajaxDelay"), "SECOND");
		tester.executeAjaxEvent("panel:ajaxDelay", "change");
		assertSame(AjaxDelay.SECOND, Throttle.getAjaxDelay(tester.getRequest()));
		tester.assertComponentOnAjaxResponse("button:state");
		tester.assertComponentOnAjaxResponse("button:settings");
		tester.assertLabel("button:state", "Ajax +1 s");

		tester.getRequest().getPostParameters().setParameterValue(input("button:settings:uploadSpeed"), "SLOW");
		tester.executeAjaxEvent("button:settings:uploadSpeed", "change");
		assertSame(UploadSpeed.SLOW, Throttle.getUploadSpeed(tester.getRequest()));
		tester.assertComponentOnAjaxResponse("panel");
		tester.assertLabel("button:state", "Ajax +1 s · Uploads 64 kB/s");
		assertEquals("Ajax +1 s · Uploads 64 kB/s",
			tester.getTagByWicketId("state").getAttribute("title"), "its tooltip");

		Throttle.install(tester.getApplication());
		tester.startPage(new TestPage());
		tester.assertInvisible("panel:notInstalled");
	}

	@Test
	void opensTheSettingsInAPopover()
	{
		tester.startPage(new TestPage());
		tester.clickLink("button:button", true);
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.Popover.show("), response);
		assertTrue(response.contains("\"title\":\"Slow down\""), response);
		tester.assertComponentOnAjaxResponse("button:settings");
	}

	/**
	 * A body of a request, from bytes.
	 */
	private static final class Body extends ServletInputStream
	{
		private final InputStream in;

		private final AtomicBoolean closed = new AtomicBoolean();

		private ReadListener listener;

		private boolean finished;

		Body(byte[] data)
		{
			in = new ByteArrayInputStream(data);
		}

		@Override
		public int read() throws IOException
		{
			int b = in.read();
			finished = b < 0;
			return b;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException
		{
			int count = in.read(buffer, offset, length);
			finished = count < 0;
			return count;
		}

		@Override
		public boolean isFinished()
		{
			return finished;
		}

		@Override
		public boolean isReady()
		{
			return true;
		}

		@Override
		public void setReadListener(ReadListener listener)
		{
			this.listener = listener;
		}

		@Override
		public void close()
		{
			closed.set(true);
		}
	}

	/**
	 * A page with a panel and a button of the throttle.
	 */
	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage()
		{
			add(new ThrottlePanel("panel"), new ThrottleButton("button"));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><div wicket:id=\"panel\">" +
				"</div><div wicket:id=\"button\"></div></body></html>");
		}
	}
}
