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
package com.antilia.widgets.menu;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Args;

/**
 * An item leading to a page that can be bookmarked, by a {@link BookmarkablePageLink}. On that page,
 * with the same parameters, the menu marks it as current.
 *
 * @since 11.0.0
 */
public class BookmarkableMenuItem extends MenuItem
{
	private static final long serialVersionUID = 1L;

	private final Class<? extends Page> pageClass;

	private final PageParameters parameters;

	/**
	 * @param label
	 *            the label of the item
	 * @param pageClass
	 *            the page the item leads to
	 */
	public BookmarkableMenuItem(IModel<String> label, Class<? extends Page> pageClass)
	{
		this(label, pageClass, null);
	}

	/**
	 * @param label
	 *            the label of the item
	 * @param pageClass
	 *            the page the item leads to
	 * @param parameters
	 *            the parameters of the page, {@code null} for none
	 */
	public BookmarkableMenuItem(IModel<String> label, Class<? extends Page> pageClass,
		PageParameters parameters)
	{
		super(label);
		this.pageClass = Args.notNull(pageClass, "pageClass");
		this.parameters = parameters;
	}

	/**
	 * @return the page the item leads to
	 */
	public Class<? extends Page> getPageClass()
	{
		return pageClass;
	}

	/**
	 * @return the parameters of the page, {@code null} for none
	 */
	public PageParameters getParameters()
	{
		return parameters;
	}

	@Override
	public AbstractLink getLink(String id)
	{
		return new BookmarkablePageLink<>(id, pageClass, parameters);
	}

	/**
	 * @return whether the page is the one the item leads to, with the same parameters
	 */
	@Override
	public boolean isCurrent(Page page)
	{
		return page != null && page.getClass().equals(pageClass) &&
			PageParameters.equals(parameters, page.getPageParameters());
	}
}
