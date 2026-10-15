/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.upload;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.Behavior;

/**
 * Marks the Ajax requests that go on while an upload stops the others, as an
 * {@link UploadToResourceField} does with {@link UploadToResourceField#setBlockingRequests(boolean)}:
 * the guard of the upload in the browser ({@code Wicket.Upload.UploadGuard}) gives every Wicket
 * Ajax request a precondition, which stops it unless it is marked.
 * <p>
 * Mark a component, and so the requests of its Ajax behaviors and of those of the components in
 * it, with {@link #bypass()}:
 *
 * <pre>
 * add(new AjaxLink&lt;Void&gt;("cancel") { ... }.add(UploadGuard.bypass()));
 * </pre>
 *
 * or the requests of one Ajax behavior with {@link #bypass(AjaxRequestAttributes)}, in its
 * {@code updateAjaxAttributes}. A request stopped marks its element with
 * {@code data-antilia-upload-blocked} for a moment, which the style sheet of the uploads outlines,
 * and the {@link UploadProgressBar} of the upload tells it.
 *
 * @since 11.0.0
 */
public final class UploadGuard
{
	/** The attribute of the elements whose requests, and those of the elements in them, go on. */
	public static final String BYPASS_ATTRIBUTE = "data-antilia-upload-bypass";

	/** The script of the request attributes marking a request that goes on. */
	static final String BYPASS_SCRIPT = "attrs.antiliaUploadBypass = true;";

	private UploadGuard()
	{
	}

	/**
	 * @return a behavior marking a component: its Ajax requests, and those of the components in
	 *         it, go on while an upload stops the others
	 */
	public static Behavior bypass()
	{
		return AttributeModifier.replace(BYPASS_ATTRIBUTE, "");
	}

	/**
	 * Marks the requests of one Ajax behavior: they go on while an upload stops the others.
	 *
	 * @param attributes
	 *            the attributes of the behavior's requests
	 * @return the attributes
	 */
	public static AjaxRequestAttributes bypass(AjaxRequestAttributes attributes)
	{
		attributes.getAjaxCallListeners().add(new AjaxCallListener().onBefore(BYPASS_SCRIPT));
		return attributes;
	}
}
