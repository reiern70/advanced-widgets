/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import org.apache.wicket.ajax.WicketAjaxJQueryResourceReference;
import org.apache.wicket.ajax.WicketAjaxResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * The script Wicket's Ajax runs on in the browser, chosen per session in the top bar. The
 * components of ui-components need neither, so the demos work on both.
 */
public enum AjaxEngine
{
	/** Wicket's Ajax on top of jQuery, {@code wicket-ajax-jquery.js}, and jQuery itself. */
	JQUERY
	{
		@Override
		public ResourceReference getReference()
		{
			return WicketAjaxJQueryResourceReference.get();
		}
	},

	/** Wicket's Ajax in plain JavaScript, {@code wicket-ajax.js}, without jQuery. */
	PLAIN_JAVASCRIPT
	{
		@Override
		public ResourceReference getReference()
		{
			return WicketAjaxResourceReference.get();
		}
	};

	/** The engine of a new session. */
	public static final AjaxEngine DEFAULT = PLAIN_JAVASCRIPT;

	/**
	 * @return the script of Wicket's Ajax in this engine
	 */
	public abstract ResourceReference getReference();
}
