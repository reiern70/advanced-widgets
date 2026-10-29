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
package com.antiliacode.widgets.demo;

import java.util.List;

import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.handler.ErrorCodeRequestHandler;
import org.apache.wicket.util.string.StringValueConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONException;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Answers 400 to a request whose parameters or body the application cannot read: a number or a
 * yes or no that is none, or JSON that is not JSON, or is nested too deep. Such requests are the
 * client's mistake, not the server's: they are logged in a line, without the stack, instead of
 * as an unexpected error, and the client gets no page of an internal error.
 */
public class BadRequests implements IRequestCycleListener
{
	private static final Logger LOG = LoggerFactory.getLogger(BadRequests.class);

	/** What reading a request throws when the request is wrong. */
	private static final List<Class<? extends Throwable>> BAD = List.of(
		StringValueConversionException.class, NumberFormatException.class, JSONException.class,
		StreamReadException.class, StreamConstraintsException.class,
		MismatchedInputException.class);

	@Override
	public IRequestHandler onException(RequestCycle cycle, Exception exception)
	{
		Throwable bad = cause(exception);
		if (bad == null)
		{
			return null;
		}
		if (LOG.isInfoEnabled())
		{
			LOG.info("Bad request {}: {}", cycle.getRequest().getUrl().getPath(),
				DemoSession.firstLine(bad));
		}
		return new ErrorCodeRequestHandler(HttpServletResponse.SC_BAD_REQUEST);
	}

	/**
	 * @return what in the causes of the exception says the request is wrong, {@code null} if
	 *         nothing does
	 */
	static Throwable cause(Throwable exception)
	{
		for (Throwable cause = exception; cause != null; cause = cause.getCause())
		{
			for (Class<? extends Throwable> bad : BAD)
			{
				if (bad.isInstance(cause))
				{
					return cause;
				}
			}
			if (cause.getCause() == cause)
			{
				break;
			}
		}
		return null;
	}
}
