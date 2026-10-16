/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.menu;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.core.request.handler.IPageClassRequestHandler;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Args;

/**
 * An item leading to a page that can be bookmarked, by a {@link BookmarkablePageLink}. It is
 * {@link #getSelected() selected} while that page, with the same parameters, is the one requested,
 * so that the menu marks it as current without being told; {@link #setSelected(IModel)} decides
 * otherwise.
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
		setSelected((IModel<Boolean>)this::leadsToTheRequestedPage);
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
	public Component getLink(String id)
	{
		return new BookmarkablePageLink<>(id, pageClass, parameters);
	}

	/**
	 * @return whether the page requested, the one rendering or called via Ajax, is the one the item
	 *         leads to, with the same parameters
	 */
	private boolean leadsToTheRequestedPage()
	{
		RequestCycle cycle = RequestCycle.get();
		IRequestHandler handler = cycle != null ? cycle.getActiveRequestHandler() : null;
		return handler instanceof IPageClassRequestHandler page &&
			pageClass.equals(page.getPageClass()) &&
			PageParameters.equals(parameters, page.getPageParameters());
	}
}
