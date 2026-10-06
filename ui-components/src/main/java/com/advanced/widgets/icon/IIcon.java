/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.icon;

import org.apache.wicket.util.io.IClusterable;

/**
 * An icon, written into the page as markup.
 * <p>
 * The markup is written as is, without escaping, so it has to be authored by the developer and
 * never contain text from users. This library ships {@link SvgIcon}, which needs nothing else, and
 * {@link FontAwesomeIcon}, which needs the Font Awesome style sheet and fonts on the page.
 *
 * @since 11.0.0
 */
@FunctionalInterface
public interface IIcon extends IClusterable
{
	/**
	 * @return the markup of the icon, written into the page as is
	 */
	String getMarkup();
}
