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
package com.antiliacode.widgets.button;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.icon.IIcon;

/**
 * A button in the colors of the theme running an action via Ajax: an icon, a text or both, in a
 * {@link Variant} and a {@link Size}. Attach it to a {@code <button>}; it never submits a form.
 *
 * <pre>
 * add(new AjaxActionButton("save", Model.of("Save"))
 * {
 * 	&#64;Override
 * 	public void onClick(AjaxRequestTarget target)
 * 	{
 * 		...
 * 	}
 * }.setIcon(SvgIcon.FLOPPY_DISK).setVariant(Variant.PRIMARY));
 * </pre>
 *
 * A button showing only its icon ({@link #setIconOnly(boolean)}) has its text as tooltip and as
 * the name screen readers say.
 *
 * @since 11.0.0
 */
public abstract class AjaxActionButton extends AjaxLink<Void>
{
	private static final long serialVersionUID = 1L;

	private final IModel<String> label;

	private final ButtonBehavior button = new ButtonBehavior();

	private IIcon icon;

	/**
	 * @param id
	 *            component id
	 * @param label
	 *            the text of the button; with only an icon shown, its tooltip and accessible name
	 */
	public AjaxActionButton(String id, IModel<String> label)
	{
		super(id);
		this.label = label instanceof IComponentAssignedModel<String> assigned
			? assigned.wrapOnAssignment(this) : label;
		add(button);
	}

	/**
	 * @param icon
	 *            the icon shown before the text, {@code null} for none
	 * @return this
	 */
	public AjaxActionButton setIcon(IIcon icon)
	{
		this.icon = icon;
		return this;
	}

	/**
	 * @return the icon shown before the text, {@code null} if there is none
	 */
	public IIcon getIcon()
	{
		return icon;
	}

	/**
	 * @param variant
	 *            what the button stands for
	 * @return this
	 */
	public AjaxActionButton setVariant(Variant variant)
	{
		button.setVariant(variant);
		return this;
	}

	/**
	 * @param size
	 *            how big the button is
	 * @return this
	 */
	public AjaxActionButton setSize(Size size)
	{
		button.setSize(size);
		return this;
	}

	/**
	 * @param iconOnly
	 *            whether the button shows its icon only, its text being the tooltip
	 * @return this
	 */
	public AjaxActionButton setIconOnly(boolean iconOnly)
	{
		button.setIconOnly(iconOnly);
		return this;
	}

	/**
	 * @return the behavior drawing the button, holding its variant and size
	 */
	public ButtonBehavior getButtonBehavior()
	{
		return button;
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		if (button.isIconOnly())
		{
			String text = text();
			tag.put("title", text);
			tag.put("aria-label", text);
		}
	}

	@Override
	public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		replaceComponentTagBody(markupStream, openTag,
			ButtonBehavior.body(getIcon(), text(), button.isIconOnly()));
	}

	private String text()
	{
		String text = label != null ? label.getObject() : null;
		return text != null ? text : "";
	}

	@Override
	protected void onDetach()
	{
		if (label != null)
		{
			label.detach();
		}
		super.onDetach();
	}
}
