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
package com.antiliacode.widgets.veil;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.extensions.ajax.veil.AbstractVeilBehavior;
import org.apache.wicket.extensions.ajax.veil.LocalVeilBehavior;
import org.apache.wicket.extensions.ajax.veil.PageVeilBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.DecoratingHeaderResponse;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

/**
 * Closes three gaps of Wicket's veil, {@link PageVeilBehavior} and {@link LocalVeilBehavior},
 * with an add-on to its script: {@code antilia-veil-gaps.js} ({@code Wicket.VeilGaps}), which
 * the library's {@link com.antiliacode.widgets.Initializer} renders after Wicket's
 * {@code wicket-veil.js} wherever a page renders that. The veils of a page need no change.
 * <ul>
 * <li><b>The keyboard.</b> Wicket's veil blocks only the mouse: Enter or Space pressed twice on a
 * focused button sends two requests. While a veil covers the element a key goes to, Enter and
 * Space are dropped, but where they type: Space in a text field, Enter in a text area.</li>
 * <li><b>The queue.</b> Wicket's veil goes up as a request is sent, but a request waits in the
 * queue of its channel while another one runs, one the veil leaves alone such as a filter's or a
 * timer's: a second click meanwhile is queued too, and the action runs twice. The script veils the
 * page, or the element of the local veil, from {@code /ajax/call/init} on, which Wicket's Ajax
 * publishes within the event's handler, before it throttles or queues the request, until the
 * request is done, or until it is plain that it will never run: a throttled request a later one
 * replaced, one its channel drops or ignores, one whose precondition stops it. Wicket's Ajax
 * publishes no topic as a request waits in the queue of its channel, nor for one dropped, throttled
 * away or ignored by its channel
 * (<a href="https://github.com/apache/wicket/issues/1942">apache/wicket#1942</a>): the script reads
 * Wicket's channels and throttler to tell. While a request waits its turn, the page has a
 * transparent veil of its own, {@code antilia-veil-guard}; the spinner comes with Wicket's veil,
 * once the request is sent.</li>
 * <li><b>The click the veil catches.</b> The mouse goes down on a button while a field's change
 * goes to the server: the veil comes up before the mouse does, and the browser has no click for
 * the button. The script gives the click back to the button once the veil is down, if it is still
 * in the page, shown and enabled.</li>
 * </ul>
 * {@link #setEnabled(Application, boolean)} leaves an application's veils as Wicket has them.
 *
 * @since 11.0.0
 */
public final class VeilGaps
{
	/** The script of Wicket's veil, which the add-on follows. */
	static final ResourceReference WICKET_VEIL = new JavaScriptResourceReference(
		AbstractVeilBehavior.class, "wicket-veil.js");

	/** The look of the veil over the page while a request waits its turn. */
	public static final ResourceReference CSS = new CssResourceReference(VeilGaps.class,
		"antilia-veil-gaps.css");

	/** The add-on to Wicket's veil, after Wicket's script, with its style sheet. */
	public static final ResourceReference JS = new JavaScriptResourceReference(VeilGaps.class,
		"antilia-veil-gaps.js")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public List<HeaderItem> getDependencies()
		{
			List<HeaderItem> dependencies = new ArrayList<>(super.getDependencies());
			dependencies.add(JavaScriptHeaderItem.forReference(WICKET_VEIL));
			dependencies.add(CssHeaderItem.forReference(CSS));
			return dependencies;
		}
	};

	private static final MetaDataKey<Boolean> DISABLED = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	private VeilGaps()
	{
	}

	/**
	 * Has the pages of an application render the add-on after Wicket's veil script, by a decorator
	 * of their header responses, Ajax ones too. The library's
	 * {@link com.antiliacode.widgets.Initializer} calls it.
	 *
	 * @param application
	 *            the application
	 */
	public static void install(Application application)
	{
		application.getHeaderResponseDecorators()
			.addPostProcessingDecorator(response -> new DecoratingHeaderResponse(response)
			{
				@Override
				public void render(HeaderItem item)
				{
					super.render(item);
					if (isWicketVeil(item) && isEnabled(Application.get()))
					{
						super.render(JavaScriptHeaderItem.forReference(JS));
					}
				}
			});
	}

	/**
	 * Turns the add-on on or off for an application; it is on by default.
	 *
	 * @param application
	 *            the application
	 * @param enabled
	 *            whether its pages render the add-on after Wicket's veil script
	 */
	public static void setEnabled(Application application, boolean enabled)
	{
		Args.notNull(application, "application");
		application.setMetaData(DISABLED, enabled ? null : Boolean.TRUE);
	}

	/**
	 * @param application
	 *            the application
	 * @return whether its pages render the add-on after Wicket's veil script
	 */
	public static boolean isEnabled(Application application)
	{
		return !Boolean.TRUE.equals(application.getMetaData(DISABLED));
	}

	private static boolean isWicketVeil(HeaderItem item)
	{
		return item instanceof JavaScriptReferenceHeaderItem reference &&
			WICKET_VEIL.equals(reference.getReference());
	}
}
