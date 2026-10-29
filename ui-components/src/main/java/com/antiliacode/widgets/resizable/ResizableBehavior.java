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
package com.antiliacode.widgets.resizable;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.ScriptJson;
import com.github.openjson.JSONObject;

import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;

/**
 * Lets the user resize the element of a component: by its bottom edge ({@link ResizeAxis#VERTICAL}),
 * its right edge ({@link ResizeAxis#HORIZONTAL}) or its bottom right corner ({@link ResizeAxis#BOTH},
 * the default), dragged with the mouse, a pen or a finger. The handle on that edge or corner takes
 * the focus: the arrow keys resize the element, ten pixels at a time or fifty with Shift, Home takes
 * it to its smallest size and End to its largest, and a double click takes it back to the size it
 * started with. The limits are {@link #setMinWidth(Integer)} and the like, in pixels.
 * <p>
 * {@code Wicket.Resizable} from {@link #JS} adds the handle and resizes the element in the browser;
 * it sets the size as CSS custom properties through the CSSOM, without an inline style, so the
 * element resizes under a strict Content Security Policy. The look comes from {@link #CSS}, in the
 * colors of the {@link com.antiliacode.widgets.theme.Theme theme} the element is in. The element
 * becomes the box the handle is positioned in, unless the page positions it otherwise.
 * <p>
 * The size the user gave the element, a {@link ResizableSize}, is the model of the behavior. By
 * default the model is kept in the behavior, and so in the page, and starts empty: the element is as
 * the page's style sheets make it. The browser sends the server the size only with
 * {@link #setSendToServer(boolean)}: via Ajax, each time the user has resized the element. The
 * behavior then gives its model the new size, which the element takes again when rendered again,
 * also after its component was taken out of the page and put back, and calls
 * {@link #onResized(AjaxRequestTarget, ResizableSize)}. To keep the size elsewhere, in the session
 * or with the user's preferences, give the behavior a model that stores what it is given; to start
 * with a size, give it a model holding one.
 *
 * @since 11.0.0
 */
public class ResizableBehavior extends AbstractDefaultAjaxBehavior
{
	private static final long serialVersionUID = 1L;

	/** The script of the resizable elements, {@code Wicket.Resizable}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		ResizableBehavior.class, "antilia-resizable.js");

	/** The style sheet of the resizable elements. */
	public static final ResourceReference CSS = new CssResourceReference(ResizableBehavior.class,
		"antilia-resizable.css");

	private static final String WIDTH_PARAMETER = "width";

	private static final String HEIGHT_PARAMETER = "height";

	private final ResizeAxis axis;

	private final IModel<ResizableSize> model;

	private Integer minWidth;

	private Integer minHeight;

	private Integer maxWidth;

	private Integer maxHeight;

	private Integer step;

	private boolean resizable = true;

	private boolean sendToServer;

	private IModel<String> label;

	/**
	 * A behavior resizing both dimensions, by the bottom right corner, keeping the size in the
	 * page.
	 */
	public ResizableBehavior()
	{
		this(ResizeAxis.BOTH);
	}

	/**
	 * A behavior keeping the size in the page.
	 *
	 * @param axis
	 *            the dimensions the user resizes
	 */
	public ResizableBehavior(ResizeAxis axis)
	{
		this(axis, null);
	}

	/**
	 * @param axis
	 *            the dimensions the user resizes, both when {@code null}
	 * @param model
	 *            where the behavior keeps the size the user gave the element, with
	 *            {@link #setSendToServer(boolean)}; in the page when {@code null}
	 */
	public ResizableBehavior(ResizeAxis axis, IModel<ResizableSize> model)
	{
		this.axis = axis == null ? ResizeAxis.BOTH : axis;
		this.model = model == null ? Model.of((ResizableSize)null) : model;
	}

	@Override
	protected void onBind()
	{
		super.onBind();
		getComponent().setOutputMarkupId(true);
	}

	/**
	 * @return the dimensions the user resizes
	 */
	public ResizeAxis getAxis()
	{
		return axis;
	}

	/**
	 * @return the model keeping the size the user gave the element
	 */
	public IModel<ResizableSize> getModel()
	{
		return model;
	}

	/**
	 * @return the size of the element, in the dimensions the user resizes: as the model has it,
	 *         {@link ResizableSize#NONE} while it is empty
	 */
	public ResizableSize getSize()
	{
		ResizableSize size = model.getObject();
		return size == null ? ResizableSize.NONE : size.along(axis);
	}

	/**
	 * @param size
	 *            the size the model gets, {@code null} for the page's
	 * @return {@code this}
	 */
	public ResizableBehavior setSize(ResizableSize size)
	{
		model.setObject(size);
		return this;
	}

	/**
	 * @param minWidth
	 *            the smallest width, in pixels; {@code null} for 32 pixels, as by default
	 * @return {@code this}
	 */
	public ResizableBehavior setMinWidth(Integer minWidth)
	{
		this.minWidth = minWidth;
		return this;
	}

	/**
	 * @return the smallest width, in pixels, {@code null} for the default
	 */
	public Integer getMinWidth()
	{
		return minWidth;
	}

	/**
	 * @param minHeight
	 *            the smallest height, in pixels; {@code null} for 32 pixels, as by default
	 * @return {@code this}
	 */
	public ResizableBehavior setMinHeight(Integer minHeight)
	{
		this.minHeight = minHeight;
		return this;
	}

	/**
	 * @return the smallest height, in pixels, {@code null} for the default
	 */
	public Integer getMinHeight()
	{
		return minHeight;
	}

	/**
	 * @param maxWidth
	 *            the largest width, in pixels; {@code null} for none, as by default
	 * @return {@code this}
	 */
	public ResizableBehavior setMaxWidth(Integer maxWidth)
	{
		this.maxWidth = maxWidth;
		return this;
	}

	/**
	 * @return the largest width, in pixels, {@code null} for none
	 */
	public Integer getMaxWidth()
	{
		return maxWidth;
	}

	/**
	 * @param maxHeight
	 *            the largest height, in pixels; {@code null} for none, as by default
	 * @return {@code this}
	 */
	public ResizableBehavior setMaxHeight(Integer maxHeight)
	{
		this.maxHeight = maxHeight;
		return this;
	}

	/**
	 * @return the largest height, in pixels, {@code null} for none
	 */
	public Integer getMaxHeight()
	{
		return maxHeight;
	}

	/**
	 * @param step
	 *            how far an arrow key resizes the element, in pixels; {@code null} for 10, as by
	 *            default
	 * @return {@code this}
	 */
	public ResizableBehavior setStep(Integer step)
	{
		this.step = step;
		return this;
	}

	/**
	 * @return how far an arrow key resizes the element, in pixels, {@code null} for the default
	 */
	public Integer getStep()
	{
		return step;
	}

	/**
	 * @return whether the user can resize the element, {@code true} unless set otherwise
	 */
	public boolean isResizable()
	{
		return resizable;
	}

	/**
	 * @param resizable
	 *            whether the user can resize the element; the size it has stays
	 * @return {@code this}
	 */
	public ResizableBehavior setResizable(boolean resizable)
	{
		this.resizable = resizable;
		return this;
	}

	/**
	 * @return whether the browser sends the server the size the user gives the element,
	 *         {@code false} by default
	 */
	public boolean isSendToServer()
	{
		return sendToServer;
	}

	/**
	 * @param sendToServer
	 *            whether the browser sends the server via Ajax the size the user gave the
	 *            element, which the behavior gives its model
	 * @return {@code this}
	 */
	public ResizableBehavior setSendToServer(boolean sendToServer)
	{
		this.sendToServer = sendToServer;
		return this;
	}

	/**
	 * @param label
	 *            the accessible name of the handle, {@code null} for the default, the text of
	 *            {@code Resizable.handle}
	 * @return {@code this}
	 */
	public ResizableBehavior setLabel(IModel<String> label)
	{
		this.label = label;
		return this;
	}

	/**
	 * Called when the user has resized the element and {@link #isSendToServer()} is on, after the
	 * model took the size. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param size
	 *            the size the user gave the element
	 */
	protected void onResized(AjaxRequestTarget target, ResizableSize size)
	{
	}

	@Override
	protected void respond(AjaxRequestTarget target)
	{
		if (!sendToServer || !resizable)
		{
			return;
		}
		IRequestParameters parameters = getComponent().getRequest().getRequestParameters();
		ResizableSize size = ResizableSize.parse(
			parameters.getParameterValue(WIDTH_PARAMETER).toOptionalString(),
			parameters.getParameterValue(HEIGHT_PARAMETER).toOptionalString()).along(axis);
		model.setObject(size);
		onResized(target, size);
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		// what the script gives the element too, right before it runs
		Looks.addClasses(tag, Looks.of(getComponent()), LookPart.RESIZABLE);
		tag.append("class", "antilia-resizable-" + axis.cssName(), " ");
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		renderHeadItems(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Resizable.attach(" +
			ScriptJson.quote(component.getMarkupId()) + ", " +
			ScriptJson.of(getOptions(component)) + ", " +
			(sendToServer && resizable ? getCallbackFunction(CallbackParameter.explicit(
				WIDTH_PARAMETER), CallbackParameter.explicit(HEIGHT_PARAMETER)) : "null") + ");"));
	}

	/**
	 * @return the options of {@code Wicket.Resizable.attach}
	 */
	JSONObject getOptions(Component component)
	{
		JSONObject options = getSize().putInto(new JSONObject()
			.put("axis", axis.cssName())
			.put("enabled", resizable));
		putPixels(options, "minWidth", minWidth);
		putPixels(options, "minHeight", minHeight);
		putPixels(options, "maxWidth", maxWidth);
		putPixels(options, "maxHeight", maxHeight);
		putPixels(options, "step", step);
		String text = label != null ? label.getObject() :
			component.getString("Resizable.handle", null, "");
		if (!Strings.isEmpty(text))
		{
			options.put("label", text);
		}
		return options;
	}

	private static void putPixels(JSONObject options, String name, Integer pixels)
	{
		if (pixels != null)
		{
			options.put(name, pixels.intValue());
		}
	}

	@Override
	public void detach(Component component)
	{
		super.detach(component);
		model.detach();
		if (label != null)
		{
			label.detach();
		}
	}

	/**
	 * Renders what a resizable element needs in the browser: {@link #JS} and {@link #CSS}.
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
