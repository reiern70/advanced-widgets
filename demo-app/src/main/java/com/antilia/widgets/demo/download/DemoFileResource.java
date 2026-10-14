/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.download;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ContentDisposition;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Serves a file of the downloads demo, made when the browser asks for it: the
 * {@link CountriesReport} a subclass gives, in the language of the session, in the format of the
 * {@link DemoFile} it gives, as an attachment the browser saves or inline for the browser to show.
 * The server answers at once, slowly or with an error, as {@link #getAnswer()} says.
 */
abstract class DemoFileResource extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param attributes
	 *            the attributes of the request
	 * @return the kind of file to serve, {@code null} for none, which is answered with 404
	 */
	protected abstract DemoFile getFile(Attributes attributes);

	/**
	 * @param locale
	 *            the language of the session
	 * @return the report to serve, all countries under the default title by default
	 */
	protected CountriesReport getReport(Locale locale)
	{
		return CountriesReport.of(locale);
	}

	/**
	 * @return the name of the file without its extension, {@code countries} by default
	 */
	protected String getName()
	{
		return "countries";
	}

	/**
	 * @param attributes
	 *            the attributes of the request
	 * @return how the file is sent, as an attachment by default
	 */
	protected ContentDisposition getContentDisposition(Attributes attributes)
	{
		return ContentDisposition.ATTACHMENT;
	}

	/**
	 * @return how the server answers, at once by default
	 */
	protected Answer getAnswer()
	{
		return Answer.AT_ONCE;
	}

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		ResourceResponse response = new ResourceResponse();
		response.disableCaching();
		Answer answer = getAnswer();
		answer.await();
		DemoFile file = getFile(attributes);
		if (file == null)
		{
			response.setError(HttpServletResponse.SC_NOT_FOUND);
			return response;
		}
		if (answer == Answer.FAILS)
		{
			response.setError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"The server of the demo fails on purpose");
			return response;
		}

		byte[] content = file.write(getReport(Session.get().getLocale()), getName());
		response.setContentType(file.getContentType());
		if (file.isText())
		{
			response.setTextEncoding(StandardCharsets.UTF_8.name());
		}
		response.setFileName(file.fileName(getName()));
		response.setContentDisposition(getContentDisposition(attributes));
		response.setContentLength(content.length);
		response.setWriteCallback(new WriteCallback()
		{
			@Override
			public void writeData(Attributes attributes)
			{
				attributes.getResponse().write(content);
			}
		});
		return response;
	}
}
