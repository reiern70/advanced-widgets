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
package com.antiliacode.widgets.popover;

import java.io.Serializable;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.theme.ILook;
import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;
import com.antiliacode.widgets.theme.ThemeBehavior;
import com.github.openjson.JSONObject;

/**
 * A popover: a bubble with an arrow pointing at a component, its trigger, telling more about it
 * in a title and a body, or showing a component of the page, its content. The server
 * {@link #show(AjaxRequestTarget, Component) shows} it in the response to an Ajax request, from the
 * click of an icon or a button for example, so what it says can come from the server as the user
 * asks for it.
 * <p>
 * The {@link #Popover(Component) content} is any component of the page, a panel with a form for
 * example: the page renders it hidden, and it moves into the popover while it is open and back
 * when it closes, so the server may render it again via Ajax just before showing it, or while it
 * is open; a component with its own form, out of the forms of the page, as the popover is out of
 * them. Its element with {@code autofocus} takes the focus as it opens. {@link #close} closes it
 * from the server.
 * <p>
 * {@code Wicket.Popover} from {@link #JS} opens it next to the trigger, where its
 * {@link #setPlacement(PopoverPlacement) placement} says, or on another side where it fits in the
 * window; it follows the trigger as the page scrolls. It closes with its close button,
 * Escape, a click outside it, or a click on its trigger again, which then does not reach the
 * trigger: an Ajax link showing a popover toggles it. One popover is open at a time. It is a
 * non-modal dialog labelled by its title, taking the focus and giving it back to the trigger.
 * <p>
 * The popover is in the {@link com.antiliacode.widgets.theme.Theme theme} its trigger is in, in the
 * colors of its {@link #setVariant(PopoverVariant) variant} and its
 * {@link #setDesign(PopoverDesign) design} from {@link #CSS}, and in the
 * {@link com.antiliacode.widgets.theme.ILook look} of its trigger too, whose classes its parts take:
 * in Bootstrap's, its header and its body are Bootstrap's popover's. Where it is is set through
 * the CSSOM, without an inline style, so it works under a strict Content Security Policy. The
 * page renders
 * what it needs with {@link #renderHeadItems(IHeaderResponse)}.
 *
 * @since 11.0.0
 */
public class Popover implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The script of the popovers, {@code Wicket.Popover}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Popover.class,
		"antilia-popover.js");

	/** The style sheet of the popovers. */
	public static final ResourceReference CSS = new CssResourceReference(Popover.class,
		"antilia-popover.css");

	private final String body;

	private final Component content;

	private String title;

	private boolean escapeBody = true;

	private PopoverPlacement placement = PopoverPlacement.BOTTOM_CENTER;

	private PopoverVariant variant = PopoverVariant.DEFAULT;

	private PopoverDesign design = PopoverDesign.CARD;

	private boolean closable = true;

	private String maxWidth;

	/**
	 * @param body
	 *            what the popover says, a text unless {@link #setEscapeBody(boolean)} says it is
	 *            markup
	 */
	public Popover(String body)
	{
		this(Args.notNull(body, "body"), null);
	}

	/**
	 * A popover showing a component of the page, which the page renders hidden from then on: the
	 * popover is made before the page renders it, or the component is made
	 * {@link #asContent(Component) content} then.
	 *
	 * @param content
	 *            the component the popover shows
	 */
	public Popover(Component content)
	{
		this(null, Args.notNull(content, "content"));
	}

	/**
	 * A popover saying something before a component of the page, which the page renders hidden
	 * from then on: the popover is made before the page renders it, or the component is made
	 * {@link #asContent(Component) content} then.
	 *
	 * @param body
	 *            what the popover says, {@code null} for nothing
	 * @param content
	 *            the component the popover shows after it, {@code null} for none
	 */
	public Popover(String body, Component content)
	{
		this.body = body;
		this.content = content == null ? null : asContent(content);
	}

	/**
	 * Makes a component the content of popovers: it renders its markup id, and hidden, out of a
	 * popover. A page showing a component in popovers made as the user asks makes it content
	 * before rendering it.
	 *
	 * @param <T>
	 *            the type of the component
	 * @param content
	 *            the component
	 * @return the component
	 */
	public static <T extends Component> T asContent(T content)
	{
		if (content.getBehaviors(ContentBehavior.class).isEmpty())
		{
			content.add(new ContentBehavior());
		}
		return content;
	}

	/**
	 * @return what the popover says, {@code null} for nothing
	 */
	public String getBody()
	{
		return body;
	}

	/**
	 * @return the component the popover shows, {@code null} for none
	 */
	public Component getContent()
	{
		return content;
	}

	/**
	 * @return the title, {@code null} for none
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * @param title
	 *            the title, in the header of the popover; {@code null} for none
	 * @return {@code this}
	 */
	public Popover setTitle(String title)
	{
		this.title = title;
		return this;
	}

	/**
	 * @return whether the body is a text, escaped, {@code true} by default
	 */
	public boolean isEscapeBody()
	{
		return escapeBody;
	}

	/**
	 * @param escapeBody
	 *            whether the body is a text; {@code false} for markup, which then must never hold
	 *            text from users
	 * @return {@code this}
	 */
	public Popover setEscapeBody(boolean escapeBody)
	{
		this.escapeBody = escapeBody;
		return this;
	}

	/**
	 * @return where the popover opens, {@link PopoverPlacement#BOTTOM_CENTER} by default
	 */
	public PopoverPlacement getPlacement()
	{
		return placement;
	}

	/**
	 * @param placement
	 *            where the popover opens: a side of the trigger, and where along it
	 * @return {@code this}
	 */
	public Popover setPlacement(PopoverPlacement placement)
	{
		this.placement = placement == null ? PopoverPlacement.BOTTOM_CENTER : placement;
		return this;
	}

	/**
	 * @return what the popover stands for, {@link PopoverVariant#DEFAULT} by default
	 */
	public PopoverVariant getVariant()
	{
		return variant;
	}

	/**
	 * @param variant
	 *            what the popover stands for, which sets its color
	 * @return {@code this}
	 */
	public Popover setVariant(PopoverVariant variant)
	{
		this.variant = variant == null ? PopoverVariant.DEFAULT : variant;
		return this;
	}

	/**
	 * @return the design of the popover, {@link PopoverDesign#CARD} by default
	 */
	public PopoverDesign getDesign()
	{
		return design;
	}

	/**
	 * @param design
	 *            the design of the popover
	 * @return {@code this}
	 */
	public Popover setDesign(PopoverDesign design)
	{
		this.design = design == null ? PopoverDesign.CARD : design;
		return this;
	}

	/**
	 * @return whether the popover has a close button, {@code true} by default
	 */
	public boolean isClosable()
	{
		return closable;
	}

	/**
	 * @param closable
	 *            whether the popover has a close button
	 * @return {@code this}
	 */
	public Popover setClosable(boolean closable)
	{
		this.closable = closable;
		return this;
	}

	/**
	 * @param maxWidth
	 *            the widest the popover gets, a CSS length; {@code null} for 20rem
	 * @return {@code this}
	 */
	public Popover setMaxWidth(String maxWidth)
	{
		this.maxWidth = maxWidth;
		return this;
	}

	/**
	 * Shows the popover pointing at a component, closing the one open, in the response to an Ajax
	 * request.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param trigger
	 *            the component the popover points at, which renders its markup id
	 * @return {@code this}
	 */
	public Popover show(AjaxRequestTarget target, Component trigger)
	{
		target.appendJavaScript("Wicket.Popover.show(" + JSONObject.quote(trigger.getMarkupId()) +
			", " + toJson(trigger) + ");");
		return this;
	}

	/**
	 * @param context
	 *            the component whose bundles have the title of the close button
	 * @return the options of {@code Wicket.Popover.show}, as a JavaScript object
	 */
	public String toJson(Component context)
	{
		JSONObject options = new JSONObject()
			.put("html", !escapeBody)
			.put("placement", placement.cssName())
			.put("variant", variant.cssName())
			.put("design", design.cssName())
			.put("closable", closable);
		if (body != null)
		{
			options.put("body", body);
		}
		if (content != null)
		{
			options.put("content", content.getMarkupId());
		}
		if (!Strings.isEmpty(title))
		{
			options.put("title", title);
		}
		if (!Strings.isEmpty(maxWidth))
		{
			options.put("maxWidth", maxWidth);
		}
		String close = context.getString("Popover.close", null, "");
		if (!Strings.isEmpty(close))
		{
			options.put("closeLabel", close);
		}
		return options.toString();
	}

	/**
	 * Closes the popover open in the page, if any, in the response to an Ajax request: its content
	 * goes back to its place, hidden.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	public static void close(AjaxRequestTarget target)
	{
		target.appendJavaScript("Wicket.Popover.closeAll();");
	}

	/**
	 * Renders what the popovers need in the browser, {@link #JS} and {@link #CSS}: in the page of
	 * a component showing them, before it does.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}

	/**
	 * @param look
	 *            the look of the popover
	 * @param design
	 *            its design
	 * @param variant
	 *            its variant
	 * @return the CSS classes of the bubble of a popover, as the script gives them: those of its
	 *         parts in its look, then {@code antilia-look-<name>} of each look it is made of, which
	 *         it carries out of its trigger; for a popover drawn in the page, as a gallery shows
	 *         one
	 */
	public static String getCssClasses(ILook look, PopoverDesign design, PopoverVariant variant)
	{
		StringBuilder classes = new StringBuilder(look.getPartClasses(LookPart.POPOVER,
			variant.getPart(), design.getPart()));
		for (ILook each : look.getLooks())
		{
			classes.append(' ').append(ThemeBehavior.LOOK_CLASS_PREFIX).append(each.getName());
		}
		return classes.toString();
	}

	/**
	 * Has the content of a popover render its markup id, and hidden, out of the popover.
	 */
	private static class ContentBehavior extends Behavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		public void bind(Component component)
		{
			component.setOutputMarkupId(true);
		}

		@Override
		public void onComponentTag(Component component, ComponentTag tag)
		{
			Looks.addClasses(tag, Looks.of(component), LookPart.POPOVER_CONTENT);
		}
	}
}
