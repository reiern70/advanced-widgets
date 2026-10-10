/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.split;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.string.Strings;

import com.github.openjson.JSONObject;

/**
 * Two panes, side by side or one above the other, with a divider between them the user drags, with
 * the mouse, a pen or a finger, to share the space differently. The divider is a WAI-ARIA
 * separator: it takes the focus, the arrow keys move it, Home and End take it to its
 * {@link #setMin(SplitSize) limits}, and a double click takes it back to where it started. A
 * {@link #setCollapsible(boolean) collapsible} split panel has two buttons on its divider, which
 * collapse a pane, giving the other all the space, and restore it; Enter does so from the keyboard.
 * <p>
 * Sizes are {@link SplitSize}s, in percent of the space the divider leaves or in pixels. The
 * {@link #setPosition(SplitSize) position} is the size of the first pane: in percent, the panes
 * keep their shares when the split panel grows or shrinks; in pixels, the first pane keeps its
 * size. The limits, {@link #setMin(SplitSize)} and {@link #setMax(SplitSize)} of the first pane
 * and {@link #setMinSecond(SplitSize)} of the second, may be in either.
 * <p>
 * {@code Wicket.SplitPanel} from {@link #JS} moves the divider in the browser; it sets the sizes
 * of the panes as CSS custom properties through the CSSOM, without an inline style, so the split
 * panel works under a strict Content Security Policy. Until the script runs, and without
 * JavaScript, the panes share the space equally. The look comes from {@link #CSS}, in the colors
 * of the {@link com.antilia.widgets.theme.Theme theme} the panel is in.
 * <p>
 * Where the divider is and which pane is collapsed, the {@link SplitState}, is the panel's model.
 * By default the model is kept in the panel, and so in the page, and starts empty: the panel shows
 * the position and the collapsed pane it was given. The browser sends the server what the user
 * changes only with {@link #setSendToServer(boolean)}: via Ajax, each time the user has moved the
 * divider, collapsed a pane or restored it. The panel then gives its model the new state, renders
 * it there again when it or its page is rendered again, and calls
 * {@link #onResized(AjaxRequestTarget)} or {@link #onCollapsed(AjaxRequestTarget)}. To keep the
 * state elsewhere, in the session or with the user's preferences, give the panel a model that
 * stores what it is given.
 * <p>
 * The panes are the components returned by {@link #newFirstPane(String)} and
 * {@link #newSecondPane(String)}. A vertical split panel needs a height, from the CSS of the page
 * or of the box it is in; split panels can be put in each other's panes.
 *
 * @since 11.0.0
 */
public abstract class SplitPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The script of the split panel, {@code Wicket.SplitPanel}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(SplitPanel.class,
		"antilia-split-panel.js");

	/** The style sheet of the split panel. */
	public static final ResourceReference CSS = new CssResourceReference(SplitPanel.class,
		"antilia-split-panel.css");

	private static final String POSITION_PARAMETER = "position";

	private static final String COLLAPSED_PARAMETER = "collapsed";

	private final WebMarkupContainer split;

	private final WebMarkupContainer firstPane;

	private final AbstractDefaultAjaxBehavior changed;

	private SplitOrientation orientation;

	private SplitSize position = SplitSize.percent(50);

	private SplitPane collapsed;

	private SplitSize min;

	private SplitSize max;

	private SplitSize minSecond;

	private SplitSize step;

	private boolean resizable = true;

	private boolean collapsible;

	private boolean sendToServer;

	/**
	 * A split panel with the panes side by side, keeping its state in the page.
	 *
	 * @param id
	 *            component id
	 */
	public SplitPanel(String id)
	{
		this(id, SplitOrientation.HORIZONTAL, null);
	}

	/**
	 * A split panel keeping its state in the page.
	 *
	 * @param id
	 *            component id
	 * @param orientation
	 *            how the panes are laid out
	 */
	public SplitPanel(String id, SplitOrientation orientation)
	{
		this(id, orientation, null);
	}

	/**
	 * A split panel with the panes side by side.
	 *
	 * @param id
	 *            component id
	 * @param model
	 *            where the panel keeps where the divider is and which pane is collapsed; empty
	 *            until the user changes them, with {@link #setSendToServer(boolean)}
	 */
	public SplitPanel(String id, IModel<SplitState> model)
	{
		this(id, SplitOrientation.HORIZONTAL, model);
	}

	/**
	 * @param id
	 *            component id
	 * @param orientation
	 *            how the panes are laid out
	 * @param model
	 *            where the panel keeps where the divider is and which pane is collapsed; empty
	 *            until the user changes them, with {@link #setSendToServer(boolean)}; in the
	 *            page when {@code null}
	 */
	public SplitPanel(String id, SplitOrientation orientation, IModel<SplitState> model)
	{
		super(id, model == null ? Model.of((SplitState)null) : model);
		setOrientation(orientation);

		split = new WebMarkupContainer("split");
		split.setOutputMarkupId(true);
		split.add(AttributeModifier.append("class", this::getCssClasses));
		add(split);

		firstPane = new WebMarkupContainer("firstPane");
		firstPane.setOutputMarkupId(true);
		split.add(firstPane);

		WebMarkupContainer divider = new WebMarkupContainer("divider");
		// what the script sets too, right before it runs
		divider.add(AttributeModifier.replace("aria-orientation",
			() -> this.orientation == SplitOrientation.HORIZONTAL ? "vertical" : "horizontal"));
		divider.add(AttributeModifier.replace("aria-controls", firstPane::getMarkupId));
		divider.add(AttributeModifier.replace("aria-label", () -> getString("SplitPanel.divider")));
		divider.add(AttributeModifier.replace("aria-valuenow", this::getValueNow));
		divider.add(AttributeModifier.replace("aria-valuemin", () -> getValueLimit(min, 0)));
		divider.add(AttributeModifier.replace("aria-valuemax", () -> getValueLimit(max, 100)));
		divider.add(AttributeModifier.replace("tabindex", () -> resizable ? "0" : null));
		divider.add(AttributeModifier.replace("aria-disabled", () -> resizable ? null : "true"));
		split.add(divider);

		changed = new AbstractDefaultAjaxBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled(Component component)
			{
				return sendToServer && super.isEnabled(component);
			}

			@Override
			protected void respond(AjaxRequestTarget target)
			{
				onChanged(target, getRequest().getRequestParameters());
			}
		};
		// on the element of the panes, which has an id even when the panel's tag is not rendered,
		// as Wicket's Ajax needs
		split.add(changed);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		firstPane.add(newFirstPane("first"));
		split.add(newSecondPane("second"));
	}

	/**
	 * Creates the first pane, the left or the top one.
	 *
	 * @param id
	 *            the id the pane must have
	 * @return the pane
	 */
	protected abstract Component newFirstPane(String id);

	/**
	 * Creates the second pane, the right or the bottom one.
	 *
	 * @param id
	 *            the id the pane must have
	 * @return the pane
	 */
	protected abstract Component newSecondPane(String id);

	/**
	 * Takes what the browser sent: gives the model the new state and calls back.
	 */
	private void onChanged(AjaxRequestTarget target, IRequestParameters parameters)
	{
		if (!resizable)
		{
			return;
		}
		SplitState before = getState();
		SplitSize reported = SplitSize.parse(parameters.getParameterValue(POSITION_PARAMETER)
			.toOptionalString());
		SplitState after = new SplitState(reported == null ? before.getPosition() : limit(reported),
			collapsible ? SplitPane.fromCssName(parameters.getParameterValue(COLLAPSED_PARAMETER)
				.toOptionalString()) : null);
		if (after.equals(before))
		{
			return;
		}
		setModelObject(after);
		if (!after.getPosition().equals(before.getPosition()))
		{
			onResized(target);
		}
		if (after.getCollapsed() != before.getCollapsed())
		{
			onCollapsed(target);
		}
	}

	/**
	 * @return the model keeping where the divider is and which pane is collapsed
	 */
	@SuppressWarnings("unchecked")
	public IModel<SplitState> getModel()
	{
		return (IModel<SplitState>)getDefaultModel();
	}

	/**
	 * @return the state the model has, {@code null} until the user has changed it
	 */
	public SplitState getModelObject()
	{
		return getModel().getObject();
	}

	/**
	 * @param state
	 *            the state the model gets, {@code null} for the position and the collapsed pane
	 *            the panel was given
	 * @return {@code this}
	 */
	public SplitPanel setModelObject(SplitState state)
	{
		setDefaultModelObject(state);
		return this;
	}

	/**
	 * @return where the divider is and which pane is collapsed: as the model has them, or as the
	 *         panel was given them while the model is empty
	 */
	public SplitState getState()
	{
		SplitState state = getModelObject();
		return state != null ? state : new SplitState(position, collapsible ? collapsed : null);
	}

	/**
	 * @return how the panes are laid out
	 */
	public SplitOrientation getOrientation()
	{
		return orientation;
	}

	/**
	 * @param orientation
	 *            how the panes are laid out
	 * @return {@code this}
	 */
	public SplitPanel setOrientation(SplitOrientation orientation)
	{
		this.orientation = orientation == null ? SplitOrientation.HORIZONTAL : orientation;
		return this;
	}

	/**
	 * @return where the divider is: as the model has it, or as the panel was given it
	 */
	public SplitSize getPosition()
	{
		return getState().getPosition();
	}

	/**
	 * @param position
	 *            the size of the first pane, in percent of the space the divider leaves
	 * @return {@code this}
	 * @see #setPosition(SplitSize)
	 */
	public SplitPanel setPosition(double position)
	{
		return setPosition(SplitSize.percent(position));
	}

	/**
	 * @param position
	 *            the size of the first pane while the model is empty, in the middle unless set
	 *            otherwise; kept within the limits when rendered. In pixels, the first pane
	 *            keeps its size when the split panel grows or shrinks.
	 * @return {@code this}
	 */
	public SplitPanel setPosition(SplitSize position)
	{
		this.position = position == null ? SplitSize.percent(50) : position;
		return this;
	}

	/**
	 * @return the collapsed pane: as the model has it, or as the panel was given it; {@code null}
	 *         for none
	 */
	public SplitPane getCollapsed()
	{
		return getState().getCollapsed();
	}

	/**
	 * @param collapsed
	 *            the pane collapsed while the model is empty, {@code null} for none, as by default;
	 *            only when {@link #setCollapsible(boolean) collapsible}
	 * @return {@code this}
	 */
	public SplitPanel setCollapsed(SplitPane collapsed)
	{
		this.collapsed = collapsed;
		return this;
	}

	/**
	 * @return the smallest size of the first pane, {@code null} for none
	 */
	public SplitSize getMin()
	{
		return min;
	}

	/**
	 * @param min
	 *            the smallest size of the first pane, in percent
	 * @return {@code this}
	 */
	public SplitPanel setMin(double min)
	{
		return setMin(SplitSize.percent(min));
	}

	/**
	 * @param min
	 *            the smallest size of the first pane, {@code null} for none, as by default
	 * @return {@code this}
	 */
	public SplitPanel setMin(SplitSize min)
	{
		this.min = min;
		return this;
	}

	/**
	 * @return the largest size of the first pane, {@code null} for none
	 */
	public SplitSize getMax()
	{
		return max;
	}

	/**
	 * @param max
	 *            the largest size of the first pane, in percent
	 * @return {@code this}
	 */
	public SplitPanel setMax(double max)
	{
		return setMax(SplitSize.percent(max));
	}

	/**
	 * @param max
	 *            the largest size of the first pane, {@code null} for none, as by default
	 * @return {@code this}
	 */
	public SplitPanel setMax(SplitSize max)
	{
		this.max = max;
		return this;
	}

	/**
	 * @return the smallest size of the second pane, {@code null} for none
	 */
	public SplitSize getMinSecond()
	{
		return minSecond;
	}

	/**
	 * @param minSecond
	 *            the smallest size of the second pane, in percent
	 * @return {@code this}
	 */
	public SplitPanel setMinSecond(double minSecond)
	{
		return setMinSecond(SplitSize.percent(minSecond));
	}

	/**
	 * @param minSecond
	 *            the smallest size of the second pane, {@code null} for none, as by default
	 * @return {@code this}
	 */
	public SplitPanel setMinSecond(SplitSize minSecond)
	{
		this.minSecond = minSecond;
		return this;
	}

	/**
	 * @return how far an arrow key moves the divider, {@code null} for 5 percent
	 */
	public SplitSize getStep()
	{
		return step;
	}

	/**
	 * @param step
	 *            how far an arrow key moves the divider, in percent
	 * @return {@code this}
	 */
	public SplitPanel setStep(double step)
	{
		return setStep(SplitSize.percent(step));
	}

	/**
	 * @param step
	 *            how far an arrow key moves the divider, {@code null} for 5 percent, as by default
	 * @return {@code this}
	 */
	public SplitPanel setStep(SplitSize step)
	{
		this.step = step;
		return this;
	}

	/**
	 * @return whether the user can move the divider, {@code true} unless set otherwise
	 */
	public boolean isResizable()
	{
		return resizable;
	}

	/**
	 * @param resizable
	 *            whether the user can move the divider
	 * @return {@code this}
	 */
	public SplitPanel setResizable(boolean resizable)
	{
		this.resizable = resizable;
		return this;
	}

	/**
	 * @return whether the divider has buttons collapsing a pane and restoring it, {@code false}
	 *         by default
	 */
	public boolean isCollapsible()
	{
		return collapsible;
	}

	/**
	 * @param collapsible
	 *            whether the divider has buttons collapsing a pane and restoring it
	 * @return {@code this}
	 */
	public SplitPanel setCollapsible(boolean collapsible)
	{
		this.collapsible = collapsible;
		return this;
	}

	/**
	 * @return whether the browser sends the server what the user changes, {@code false} by
	 *         default
	 */
	public boolean isSendToServer()
	{
		return sendToServer;
	}

	/**
	 * @param sendToServer
	 *            whether the browser sends the server via Ajax where the user put the divider and
	 *            which pane they collapsed, which the panel gives its model
	 * @return {@code this}
	 */
	public SplitPanel setSendToServer(boolean sendToServer)
	{
		this.sendToServer = sendToServer;
		return this;
	}

	/**
	 * Called when the user has moved the divider and {@link #isSendToServer()} is on, with
	 * {@link #getPosition()} where they put it. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onResized(AjaxRequestTarget target)
	{
	}

	/**
	 * Called when the user has collapsed a pane or restored it and {@link #isSendToServer()} is
	 * on, with {@link #getCollapsed()} the pane they collapsed. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onCollapsed(AjaxRequestTarget target)
	{
	}

	/**
	 * @return the markup id of the element of the panes, which the script works on
	 */
	public String getSplitMarkupId()
	{
		return split.getMarkupId();
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderHeadItems(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.SplitPanel.attach(" +
			JSONObject.quote(split.getMarkupId()) + ", " + getOptions() + ");"));
	}

	/**
	 * @return the classes of the element of the panes, which the script gives it too
	 */
	private String getCssClasses()
	{
		List<String> classes = new ArrayList<>();
		classes.add("antilia-split-" + orientation.cssName());
		if (!resizable)
		{
			classes.add("antilia-split-fixed");
		}
		SplitState state = getState();
		if (state.getPosition().isPixels())
		{
			classes.add("antilia-split-pixels");
		}
		if (collapsible)
		{
			classes.add("antilia-split-collapsible");
			if (state.getCollapsed() != null)
			{
				classes.add("antilia-split-collapsed-" + state.getCollapsed().cssName());
			}
		}
		return String.join(" ", classes);
	}

	/**
	 * @return the value of the separator, in percent, as the script works it out; none in
	 *         pixels, which the script works out from the space
	 */
	private String getValueNow()
	{
		SplitState state = getState();
		if (collapsible && state.getCollapsed() != null)
		{
			return state.getCollapsed() == SplitPane.FIRST ? "0" : "100";
		}
		SplitSize shown = limit(state.getPosition());
		return shown.isPixels() ? null : String.valueOf(Math.round(shown.getValue()));
	}

	/**
	 * @return the limit of the separator's value, in percent, as the script works it out
	 */
	private String getValueLimit(SplitSize limit, long collapsedValue)
	{
		if (!resizable)
		{
			return null;
		}
		if (collapsible)
		{
			return String.valueOf(collapsedValue);
		}
		if (getPosition().isPixels())
		{
			return null;
		}
		double value = limit != null && !limit.isPixels() ? limit.getValue() : collapsedValue;
		if (collapsedValue == 100 && minSecond != null && !minSecond.isPixels())
		{
			value = Math.min(value, 100 - minSecond.getValue());
		}
		return format(value);
	}

	/**
	 * @return the options of {@code Wicket.SplitPanel.attach}, as a JavaScript object
	 */
	private String getOptions()
	{
		SplitState state = getState();
		JSONObject options = new JSONObject()
			.put("orientation", orientation.cssName())
			.put("position", limit(state.getPosition()).toJson())
			.put("resizable", resizable)
			.put("collapsible", collapsible);
		putSize(options, "min", min);
		putSize(options, "max", max);
		putSize(options, "minSecond", minSecond);
		putSize(options, "step", step);
		if (collapsible)
		{
			if (state.getCollapsed() != null)
			{
				options.put("collapsed", state.getCollapsed().cssName());
			}
			boolean horizontal = orientation == SplitOrientation.HORIZONTAL;
			putLabel(options, "collapseFirstLabel", horizontal ? "SplitPanel.collapseLeft" :
				"SplitPanel.collapseTop");
			putLabel(options, "collapseSecondLabel", horizontal ? "SplitPanel.collapseRight" :
				"SplitPanel.collapseBottom");
			putLabel(options, "restoreLabel", "SplitPanel.restore");
		}
		putLabel(options, "label", "SplitPanel.divider");
		String json = options.toString();
		if (!sendToServer || !resizable)
		{
			return json;
		}
		return json.substring(0, json.length() - 1) + ",\"onChange\":" +
			changed.getCallbackFunction(CallbackParameter.explicit(POSITION_PARAMETER),
				CallbackParameter.explicit(COLLAPSED_PARAMETER)) + "}";
	}

	private static void putSize(JSONObject options, String name, SplitSize size)
	{
		if (size != null)
		{
			options.put(name, size.toJson());
		}
	}

	private void putLabel(JSONObject options, String name, String key)
	{
		String label = getString(key, null, "");
		if (!Strings.isEmpty(label))
		{
			options.put(name, label);
		}
	}

	/**
	 * @return the position within the limits in its unit; those in the other unit need the space
	 *         the panes have in the browser, where the script keeps them
	 */
	private SplitSize limit(SplitSize size)
	{
		double low = 0;
		double high = size.isPixels() ? Double.MAX_VALUE : 100;
		if (size.sameUnit(min))
		{
			low = min.getValue();
		}
		if (size.sameUnit(max))
		{
			high = Math.min(high, max.getValue());
		}
		if (!size.isPixels() && size.sameUnit(minSecond))
		{
			high = Math.min(high, 100 - minSecond.getValue());
		}
		double value = Math.min(Math.max(size.getValue(), low), Math.max(low, high));
		if (value == size.getValue())
		{
			return size;
		}
		return size.isPixels() ? SplitSize.pixels(value) : SplitSize.percent(value);
	}

	private static String format(double value)
	{
		return value == Math.rint(value) ? String.valueOf((long)value) : String.valueOf(value);
	}

	/**
	 * Renders what the split panel needs in the browser: {@link #JS} and {@link #CSS}.
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
}
