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
