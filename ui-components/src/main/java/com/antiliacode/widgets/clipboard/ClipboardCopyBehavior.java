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
package com.antiliacode.widgets.clipboard;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.ScriptJson;

/**
 * Copies a text to the clipboard when the user clicks, or double clicks, the component, and
 * briefly shows a check mark in it; screen readers announce the copy.
 * <p>
 * The behavior writes the text into the component's {@code data-antilia-copy} attribute, escaped
 * like any attribute value, and the trigger into {@code data-antilia-copy-on}. The copying is done
 * in the browser by {@code Wicket.Clipboard} from {@link #JS}, for the elements it attaches: the
 * component of this behavior ({@code Wicket.Clipboard.attach}), and the elements carrying these
 * attributes inside the component of a {@link #within()} behavior
 * ({@code Wicket.Clipboard.attachWithin}), markup a BaseDynamicDataTable column template renders
 * in the browser for example. Other elements carrying the attributes copy nothing, so that HTML
 * a user wrote, shown in the page, cannot choose what lands on the clipboard.
 * {@code Wicket.Clipboard.copy(text, element)} copies from any script. The browser lets a page
 * write to the clipboard only in a secure context (HTTPS or localhost) and in response to a user
 * action.
 *
 * @since 11.0.0
 */
public class ClipboardCopyBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The script copying to the clipboard, {@code Wicket.Clipboard}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		ClipboardCopyBehavior.class, "antilia-clipboard.js");

	/** The style sheet of the check mark. */
	public static final ResourceReference CSS = new CssResourceReference(
		ClipboardCopyBehavior.class, "antilia-clipboard.css");

	/**
	 * What makes the component copy.
	 */
	public enum Trigger
	{
		/** A click. */
		CLICK("click"),

		/** A double click, leaving single clicks to select text. */
		DOUBLE_CLICK("dblclick");

		private final String event;

		Trigger(String event)
		{
			this.event = event;
		}

		/**
		 * @return the name of the DOM event
		 */
		public String getEvent()
		{
			return event;
		}
	}

	private final IModel<String> text;

	private Trigger trigger = Trigger.CLICK;

	/**
	 * @param text
	 *            the text to copy
	 */
	public ClipboardCopyBehavior(IModel<String> text)
	{
		this.text = Args.notNull(text, "text");
	}

	/**
	 * @param trigger
	 *            what makes the component copy, a click by default
	 * @return {@code this}
	 */
	public ClipboardCopyBehavior setTrigger(Trigger trigger)
	{
		this.trigger = Args.notNull(trigger, "trigger");
		return this;
	}

	/**
	 * @return what makes the component copy
	 */
	public Trigger getTrigger()
	{
		return trigger;
	}

	/**
	 * A behavior making the elements inside its component that carry a {@code data-antilia-copy}
	 * attribute copy, as a ClipboardCopyBehavior makes its own component: for markup of the
	 * page's own that is rendered in the browser, by the column templates of a table for example,
	 * or written as markup by a label. Not for a component showing HTML a user wrote.
	 *
	 * @return the behavior, for a component rendering its markup id
	 */
	public static Behavior within()
	{
		return new Behavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void bind(Component component)
			{
				super.bind(component);
				component.setOutputMarkupId(true);
			}

			@Override
			public void renderHead(Component component, IHeaderResponse response)
			{
				super.renderHead(component, response);
				renderHeadItems(response);
				response.render(OnDomReadyHeaderItem.forScript(attachScript("attachWithin",
					component)));
			}
		};
	}

	/**
	 * @return the script attaching the element of a component to {@code Wicket.Clipboard}
	 */
	private static String attachScript(String function, Component component)
	{
		return "Wicket.Clipboard." + function + "(document.getElementById(" +
			ScriptJson.quote(component.getMarkupId()) + "));";
	}

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		component.setOutputMarkupId(true);
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		super.onComponentTag(component, tag);
		String value = text.getObject();
		tag.put("data-antilia-copy", value != null ? value : "");
		tag.put("data-antilia-copy-on", trigger.getEvent());
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		renderHeadItems(response);
		response.render(OnDomReadyHeaderItem.forScript(attachScript("attach", component)));
	}

	/**
	 * Renders what copying needs in the browser: {@link #JS}, {@link #CSS} and the localized
	 * message read to screen readers after a copy, from the resource
	 * {@code ClipboardCopyBehavior.copied}. Enough for {@code Wicket.Clipboard.copy}; an element
	 * carrying {@code data-antilia-copy} copies once the behavior or {@link #within()} attaches it.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
		String copied = Application.get().getResourceSettings().getLocalizer().getString(
			"ClipboardCopyBehavior.copied", null, "Copied");
		response.render(OnDomReadyHeaderItem.forScript(
			"Wicket.Clipboard.copiedMessage = " + ScriptJson.quote(copied) + ";"));
	}

	@Override
	public void detach(Component component)
	{
		text.detach();
		super.detach(component);
	}
}
