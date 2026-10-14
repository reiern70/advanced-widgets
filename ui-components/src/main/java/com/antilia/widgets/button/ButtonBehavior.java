/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.button;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;

import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.theme.ILook;
import com.antilia.widgets.theme.LookPart;
import com.antilia.widgets.theme.Looks;

/**
 * Draws the element of a component as a button in the colors of the theme: a {@code <button>},
 * an {@code <a>} or a submit {@code <input>}, whatever component it belongs to, such as a link or
 * a form's button. The look comes from {@link #CSS}, the same style sheet the React and Svelte
 * buttons use; the {@link Variant} and the {@link Size} each add a CSS class. In another
 * {@link ILook}, such as Bootstrap's, the button gets the classes of that look instead (see
 * {@link Looks#of(Component)}).
 *
 * <pre>
 * add(new Link&lt;Void&gt;("help") { ... }.add(new ButtonBehavior(Variant.GHOST, Size.SMALL)));
 * </pre>
 *
 * @see AjaxActionButton
 * @since 11.0.0
 */
public class ButtonBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The style sheet of the buttons. */
	public static final ResourceReference CSS = new CssResourceReference(ButtonBehavior.class,
		"antilia-button.css");

	/**
	 * The script marking a button busy, {@code Wicket.Button}; a busy button ignores clicks.
	 *
	 * @see ButtonDownloadBehavior
	 */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		ButtonBehavior.class, "antilia-button.js");

	/** The CSS class of every button. */
	public static final String CSS_CLASS = "ant-button";

	/**
	 * What a button stands for, which sets its colors.
	 */
	public enum Variant
	{
		/** The main action of a place, filled with the primary color. */
		PRIMARY,
		/** Any other action, in the color of the surface with a border; the default. */
		SECONDARY,
		/** A minor action, text in the accent color without a border. */
		GHOST,
		/** An action destroying something, filled in red. */
		DANGER;

		/**
		 * @return the CSS class of the variant
		 */
		public String cssClass()
		{
			return CSS_CLASS + "--" + name().toLowerCase();
		}

		/**
		 * @return the part of a look the variant is
		 */
		public LookPart part()
		{
			return LookPart.valueOf("BUTTON_" + name());
		}
	}

	/**
	 * How big a button is.
	 */
	public enum Size
	{
		/** For toolbars and dense places. */
		SMALL,
		/** The default. */
		MEDIUM,
		/** For the one action a page is about. */
		LARGE;

		/**
		 * @return the CSS class of the size
		 */
		public String cssClass()
		{
			return CSS_CLASS + "--" + name().toLowerCase();
		}

		/**
		 * @return the part of a look the size is
		 */
		public LookPart part()
		{
			return LookPart.valueOf("BUTTON_" + name());
		}
	}

	private Variant variant;

	private Size size;

	private boolean iconOnly;

	/**
	 * A secondary button of medium size.
	 */
	public ButtonBehavior()
	{
		this(Variant.SECONDARY, Size.MEDIUM);
	}

	/**
	 * @param variant
	 *            what the button stands for
	 * @param size
	 *            how big it is
	 */
	public ButtonBehavior(Variant variant, Size size)
	{
		this.variant = Args.notNull(variant, "variant");
		this.size = Args.notNull(size, "size");
	}

	/**
	 * @return what the button stands for
	 */
	public Variant getVariant()
	{
		return variant;
	}

	/**
	 * @param variant
	 *            what the button stands for
	 * @return this
	 */
	public ButtonBehavior setVariant(Variant variant)
	{
		this.variant = Args.notNull(variant, "variant");
		return this;
	}

	/**
	 * @return how big the button is
	 */
	public Size getSize()
	{
		return size;
	}

	/**
	 * @param size
	 *            how big the button is
	 * @return this
	 */
	public ButtonBehavior setSize(Size size)
	{
		this.size = Args.notNull(size, "size");
		return this;
	}

	/**
	 * @return whether the button shows only an icon, and is square
	 */
	public boolean isIconOnly()
	{
		return iconOnly;
	}

	/**
	 * @param iconOnly
	 *            whether the button shows only an icon, and is square
	 * @return this
	 */
	public ButtonBehavior setIconOnly(boolean iconOnly)
	{
		this.iconOnly = iconOnly;
		return this;
	}

	/**
	 * @return the CSS classes of the button: the base class, the variant's, the size's and, for
	 *         an icon only, {@code ant-button--icon-only}
	 */
	public String getCssClasses()
	{
		return CSS_CLASS + " " + variant.cssClass() + " " + size.cssClass() +
			(iconOnly ? " " + CSS_CLASS + "--icon-only" : "");
	}

	/**
	 * @param look
	 *            a look
	 * @return the CSS classes of the button in the look: those of a button, of the variant, of the
	 *         size and, for an icon only, of a button showing only its icon
	 */
	public String getCssClasses(ILook look)
	{
		return look.getCssClasses(LookPart.BUTTON, variant.part(), size.part(),
			iconOnly ? LookPart.BUTTON_ICON_ONLY : null);
	}

	/**
	 * The body of a button, as {@link AjaxActionButton} writes it: the icon, if any, then the
	 * text, unless the button shows only its icon.
	 *
	 * @param icon
	 *            the icon shown before the text, {@code null} for none
	 * @param text
	 *            the text, escaped here
	 * @param iconOnly
	 *            whether only the icon is shown
	 * @return the markup of the body
	 */
	public static CharSequence body(IIcon icon, String text, boolean iconOnly)
	{
		StringBuilder body = new StringBuilder();
		if (icon != null)
		{
			body.append("<span class=\"ant-button__icon\" aria-hidden=\"true\">")
				.append(icon.getMarkup())
				.append("</span>");
		}
		if (iconOnly == false)
		{
			body.append("<span class=\"ant-button__label\">")
				.append(Strings.escapeMarkup(text != null ? text : ""))
				.append("</span>");
		}
		return body;
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		super.onComponentTag(component, tag);
		tag.append("class", getCssClasses(Looks.of(component)), " ");
		if ("button".equalsIgnoreCase(tag.getName()) && tag.getAttribute("type") == null &&
			component instanceof IFormSubmittingComponent == false)
		{
			// a button of a link or an Ajax link never submits the form it is in
			tag.put("type", "button");
		}
		if (component.isEnabledInHierarchy() == false && "button".equalsIgnoreCase(
			tag.getName()) == false)
		{
			tag.put("aria-disabled", "true");
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		response.render(CssHeaderItem.forReference(CSS));
		Looks.of(component).renderHead(response);
	}
}
