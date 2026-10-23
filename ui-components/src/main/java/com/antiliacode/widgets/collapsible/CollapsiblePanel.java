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
package com.antiliacode.widgets.collapsible;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.danekja.java.util.function.serializable.SerializableConsumer;

import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookContainer;
import com.antiliacode.widgets.theme.LookPart;

/**
 * A title and a body the user can expand and collapse, collapsed by default.
 * <p>
 * The panel renders the native {@code <details>} and {@code <summary>} elements: expanding and
 * collapsing happens in the browser, without JavaScript, is keyboard accessible and needs no
 * inline script or style, so it works under a strict Content Security Policy. The look comes from
 * {@link #CSS}. The color of the body's text is the CSS custom property
 * {@code --antilia-collapsible-text} if it is set, for example on the page, else the text color of
 * the {@link com.antiliacode.widgets.theme.Theme theme} the panel is in, else the color of
 * the panel's parent.
 * <p>
 * By default the server does not learn when the user expands or collapses the panel, and renders
 * it again in the state of {@link #setExpanded(boolean)}; the panel then leaves a stateless page
 * stateless. With {@link #setRememberExpanded(boolean)} every toggle is reported via Ajax, so the
 * panel keeps the user's choice when it or its page is rendered again, and its page is stateful.
 * <p>
 * The title is escaped. The body is the component returned by {@link #newBody(String)}, for
 * example a label or a panel.
 * <p>
 * The header can hold actions, buttons on its right, such as one clearing what the body holds:
 *
 * <pre>
 * panel.addAction(new ResourceModel("clear"), SvgIcon.ERASER, target -&gt; {...});
 * </pre>
 *
 * They are not in the {@code <summary>}, which is a button itself, but over its right end: a
 * click on one runs its action without expanding or collapsing the panel, and they are reached
 * with the Tab key after the title. {@link #updateHeader(AjaxRequestTarget)} renders the title and
 * the actions again, for example when an action shows only while there is something to clear.
 *
 * @since 11.0.0
 */
public abstract class CollapsiblePanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The style sheet of the panel. */
	public static final ResourceReference CSS = new CssResourceReference(CollapsiblePanel.class,
		"antilia-collapsible.css");

	private static final String OPEN_PARAMETER = "open";

	private final WebMarkupContainer details;

	private final Label title;

	private final WebMarkupContainer actions;

	private final RepeatingView action;

	private boolean expanded;

	private boolean rememberExpanded;

	/**
	 * @param id
	 *            component id
	 * @param title
	 *            the title, always shown, which the user clicks to expand or collapse the body
	 */
	public CollapsiblePanel(String id, IModel<String> title)
	{
		super(id);

		details = new WebMarkupContainer("details");
		details.setOutputMarkupId(true);
		details.add(AttributeModifier.replace("open", () -> expanded ? "open" : null));
		details.add(new LookBehavior(LookPart.COLLAPSIBLE));
		add(new LookContainer("frame", LookPart.COLLAPSIBLE_FRAME));
		add(details);
		this.title = new Label("title", title);
		this.title.setOutputMarkupId(true);
		this.title.add(new LookBehavior(LookPart.COLLAPSIBLE_TITLE));
		details.add(this.title);
		details.add(new LookContainer("bodyFrame", LookPart.COLLAPSIBLE_BODY));

		actions = new WebMarkupContainer("actions")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(action.size() > 0);
			}
		};
		actions.setOutputMarkupPlaceholderTag(true);
		actions.add(new LookBehavior(LookPart.COLLAPSIBLE_ACTIONS));
		add(actions);
		action = new RepeatingView("action");
		actions.add(action);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		details.add(newBody("body"));
		details.add(new ToggleBehavior());
	}

	/**
	 * Creates the body of the panel, shown when it is expanded.
	 *
	 * @param id
	 *            the id the body must have
	 * @return the body
	 */
	protected abstract Component newBody(String id);

	/**
	 * @return the id the next action has to have, see {@link #addAction(Component)}
	 */
	public String newActionId()
	{
		return action.newChildId();
	}

	/**
	 * Adds an action to the header, on its right, after those added before.
	 *
	 * @param component
	 *            the action, with an id from {@link #newActionId()}, attached to a
	 *            {@code <button type="button">}: an {@link AjaxActionButton}, for example
	 * @return {@code this}
	 */
	public CollapsiblePanel addAction(Component component)
	{
		action.add(component);
		return this;
	}

	/**
	 * Adds a small button to the header, on its right, showing only its icon.
	 *
	 * @param label
	 *            the tooltip of the button and the name screen readers say
	 * @param icon
	 *            the icon of the button
	 * @param onClick
	 *            what a click on the button does, in an Ajax request
	 * @return the button, to set more of it, or to hide it while it has nothing to do
	 */
	public AjaxActionButton addAction(IModel<String> label, IIcon icon,
		SerializableConsumer<AjaxRequestTarget> onClick)
	{
		AjaxActionButton button = new AjaxActionButton(newActionId(), label)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				onClick.accept(target);
			}
		};
		button.setIcon(icon).setIconOnly(true).setSize(Size.SMALL);
		addAction(button);
		return button;
	}

	/**
	 * Renders the title and the actions again, leaving the body and whether the panel is expanded
	 * alone.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	public void updateHeader(AjaxRequestTarget target)
	{
		target.add(title, actions);
	}

	/**
	 * @return whether the panel is rendered expanded, {@code false} by default
	 */
	public boolean isExpanded()
	{
		return expanded;
	}

	/**
	 * @param expanded
	 *            whether the panel is rendered expanded
	 * @return {@code this}
	 */
	public CollapsiblePanel setExpanded(boolean expanded)
	{
		this.expanded = expanded;
		return this;
	}

	/**
	 * @return whether the browser reports every toggle to the server, {@code false} by default
	 */
	public boolean isRememberExpanded()
	{
		return rememberExpanded;
	}

	/**
	 * @param rememberExpanded
	 *            whether the browser reports every toggle to the server via Ajax, updating
	 *            {@link #isExpanded()}
	 * @return {@code this}
	 */
	public CollapsiblePanel setRememberExpanded(boolean rememberExpanded)
	{
		this.rememberExpanded = rememberExpanded;
		return this;
	}

	/**
	 * Called when the user expanded or collapsed the panel and {@link #isRememberExpanded()} is
	 * on. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onToggle(AjaxRequestTarget target)
	{
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(CSS));
	}

	private class ToggleBehavior extends AjaxEventBehavior
	{
		private static final long serialVersionUID = 1L;

		ToggleBehavior()
		{
			super("toggle");
		}

		@Override
		public boolean isEnabled(Component component)
		{
			return rememberExpanded && super.isEnabled(component);
		}

		/**
		 * @return whether the behavior leaves the page stateless: it does while it reports nothing
		 */
		@Override
		public boolean getStatelessHint(Component component)
		{
			return !rememberExpanded;
		}

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			attributes.getDynamicExtraParameters()
				.add("return {'" + OPEN_PARAMETER + "': Wicket.$(attrs.c).open};");
		}

		@Override
		protected void onEvent(AjaxRequestTarget target)
		{
			expanded = getRequest().getRequestParameters()
				.getParameterValue(OPEN_PARAMETER)
				.toBoolean(expanded);
			onToggle(target);
		}
	}
}
