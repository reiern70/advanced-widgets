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
package com.antiliacode.widgets.throttle;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.antiliacode.widgets.dropdown.Dropdown;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The settings of the {@link Throttle} of the session, which take effect at once: how long the
 * server waits before each Wicket Ajax request ({@link AjaxDelay}) and how fast it reads the files
 * sent ({@link UploadSpeed}), each a {@link Dropdown} in the look of the page. A change tells the
 * other throttle components of the page, which render again: a {@link ThrottleButton} in a top bar
 * and a panel in the page say the same. The requests of the panel never wait.
 * <p>
 * Without {@link Throttle#install(org.apache.wicket.protocol.http.WebApplication)}, the panel says
 * that nothing slows down. The texts are those of the resources {@code Throttle.*},
 * {@code AjaxDelay.*} and {@code UploadSpeed.*} of the class.
 *
 * @since 11.0.0
 */
public class ThrottlePanel extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The style sheet of the throttle components. */
	public static final ResourceReference CSS = new CssResourceReference(ThrottlePanel.class,
		"antilia-throttle.css");

	/**
	 * What a throttle component of the page sends the others once the settings changed.
	 *
	 * @param target
	 *            the Ajax request changing them
	 * @param source
	 *            the component changing them, which needs no new rendering
	 */
	public record Change(IPartialPageRequestHandler target, Object source)
	{
	}

	/**
	 * @param id
	 *            component id
	 */
	public ThrottlePanel(String id)
	{
		super(id);
		setOutputMarkupId(true);
		add(newChoice("ajaxDelay", AjaxDelay.values(), LambdaModel.of(
			() -> Throttle.getAjaxDelay(request()), delay -> Throttle.setAjaxDelay(request(), delay))));
		add(newChoice("uploadSpeed", UploadSpeed.values(), LambdaModel.of(
			() -> Throttle.getUploadSpeed(request()),
			speed -> Throttle.setUploadSpeed(request(), speed))));
		add(new WebMarkupContainer("notInstalled")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!Throttle.isInstalled(getApplication()));
			}
		});
	}

	private <T extends Enum<T>> Dropdown<T> newChoice(String id, T[] values, IModel<T> model)
	{
		Dropdown<T> choice = new Dropdown<>(id, model, List.of(values),
			new EnumChoiceRenderer<>(this));
		choice.setRequired(true);
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				Throttle.exempt(attributes);
			}

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				send(getPage(), Broadcast.BREADTH, new Change(target, ThrottlePanel.this));
			}
		});
		return choice;
	}

	private HttpServletRequest request()
	{
		return (HttpServletRequest)getRequest().getContainerRequest();
	}

	@Override
	public void onEvent(IEvent<?> event)
	{
		super.onEvent(event);
		if (event.getPayload() instanceof Change change && change.source() != this)
		{
			change.target().add(this);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(CSS));
	}
}
