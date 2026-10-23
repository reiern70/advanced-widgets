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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.popover.Popover;
import com.antiliacode.widgets.popover.PopoverPlacement;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A button, a gauge, opening the {@link ThrottlePanel} of the session in a {@link Popover}, for a
 * top bar say: a dot on it says that the server slows down, how, such as
 * "Ajax +1 s · Uploads 64 kB/s", to screen readers and as its tooltip. The requests of the button
 * never wait.
 * <p>
 * The texts are those of the resources {@code Throttle.*} of the class, and of
 * {@link ThrottlePanel}.
 *
 * @since 11.0.0
 */
public class ThrottleButton extends Panel
{
	private static final long serialVersionUID = 1L;

	private final ThrottlePanel settings;

	private final Label state;

	/**
	 * @param id
	 *            component id
	 */
	public ThrottleButton(String id)
	{
		super(id);
		settings = Popover.asContent(new ThrottlePanel("settings"));
		add(settings);
		add(new AjaxActionButton("button", new ResourceModel("Throttle.button"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				Throttle.exempt(attributes);
			}

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				// the settings as they are now, then the popover showing them
				target.add(settings);
				new Popover(settings).setTitle(getString("Throttle.title"))
					.setPlacement(PopoverPlacement.BOTTOM_RIGHT).setMaxWidth("22rem")
					.show(target, this);
			}
		}.setIcon(SvgIcon.GAUGE).setIconOnly(true).setVariant(Variant.GHOST)
			.setSize(Size.SMALL));
		state = new Label("state", this::describe)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(Throttle.isSlowingDown(request()));
			}
		};
		state.add(AttributeModifier.replace("title", this::describe));
		add(state.setOutputMarkupPlaceholderTag(true));
	}

	/**
	 * @return how the server slows down, such as "Ajax +1 s · Uploads 64 kB/s"
	 */
	private String describe()
	{
		HttpServletRequest request = request();
		List<String> parts = new ArrayList<>();
		AjaxDelay delay = Throttle.getAjaxDelay(request);
		if (delay != AjaxDelay.NONE)
		{
			parts.add(getString("Throttle.state.ajax").replace("{0}",
				new EnumChoiceRenderer<AjaxDelay>(settings).getDisplayValue(delay).toString()));
		}
		UploadSpeed speed = Throttle.getUploadSpeed(request);
		if (speed != UploadSpeed.FULL)
		{
			parts.add(getString("Throttle.state.upload").replace("{0}",
				new EnumChoiceRenderer<UploadSpeed>(settings).getDisplayValue(speed).toString()));
		}
		return String.join(" · ", parts);
	}

	private HttpServletRequest request()
	{
		return (HttpServletRequest)getRequest().getContainerRequest();
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Popover.renderHeadItems(response);
	}

	@Override
	public void onEvent(IEvent<?> event)
	{
		super.onEvent(event);
		if (event.getPayload() instanceof ThrottlePanel.Change change)
		{
			change.target().add(state);
		}
	}
}
