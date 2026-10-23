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
package com.antiliacode.widgets.modal;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableConsumer;

import com.antiliacode.widgets.icon.IIcon;

/**
 * Makes a button of the footer of a {@link Modal}, opened with
 * {@link Modal#open(IModel, Component, AjaxRequestTarget, IModalButtonFactory...)}. The button is
 * rendered on a {@code <button type="button">}: a {@link ModalButton}, a {@link ModalSubmitButton}
 * submitting a form of the body, a {@link ModalCloseButton}, or any component fit for that tag.
 * The first three are the buttons of the theme
 * ({@link com.antiliacode.widgets.button.ButtonBehavior}), small, with an optional icon. The factories
 * here make the common ones:
 *
 * <pre>
 * modal.open(title, form, target,
 * 	IModalButtonFactory.close(new ResourceModel("cancel"), SvgIcon.XMARK),
 * 	id -&gt; new ModalSubmitButton(id, new ResourceModel("save"), form) {...}
 * 		.setIcon(SvgIcon.FLOPPY_DISK).setPrimary(true));
 * </pre>
 *
 * @since 11.0.0
 */
@FunctionalInterface
public interface IModalButtonFactory extends IClusterable
{
	/**
	 * @param id
	 *            the id the button must have
	 * @return the button
	 */
	Component createButton(String id);

	/**
	 * @param label
	 *            the text of the button
	 * @return the factory of a button closing the dialog as its close button does, if the
	 *         function of {@link Modal#setCanClose(String)} lets it, in the browser
	 */
	static IModalButtonFactory close(IModel<String> label)
	{
		return close(label, null);
	}

	/**
	 * @param label
	 *            the text of the button
	 * @param icon
	 *            the icon before the text, {@code null} for none
	 * @return the factory of a button closing the dialog as its close button does, if the
	 *         function of {@link Modal#setCanClose(String)} lets it, in the browser
	 */
	static IModalButtonFactory close(IModel<String> label, IIcon icon)
	{
		Args.notNull(label, "label");
		return id -> new ModalCloseButton(id, label).setIcon(icon);
	}

	/**
	 * @param label
	 *            the text of the button
	 * @param onClick
	 *            what a click on the button does, in an Ajax request
	 * @return the factory of a button
	 */
	static IModalButtonFactory button(IModel<String> label,
		SerializableConsumer<AjaxRequestTarget> onClick)
	{
		return button(label, null, false, onClick);
	}

	/**
	 * @param label
	 *            the text of the button
	 * @param icon
	 *            the icon before the text, {@code null} for none
	 * @param onClick
	 *            what a click on the button does, in an Ajax request
	 * @return the factory of a button
	 */
	static IModalButtonFactory button(IModel<String> label, IIcon icon,
		SerializableConsumer<AjaxRequestTarget> onClick)
	{
		return button(label, icon, false, onClick);
	}

	/**
	 * @param label
	 *            the text of the button
	 * @param onClick
	 *            what a click on the button does, in an Ajax request
	 * @return the factory of the primary button, in the primary color of the theme
	 */
	static IModalButtonFactory primary(IModel<String> label,
		SerializableConsumer<AjaxRequestTarget> onClick)
	{
		return button(label, null, true, onClick);
	}

	/**
	 * @param label
	 *            the text of the button
	 * @param icon
	 *            the icon before the text, {@code null} for none
	 * @param onClick
	 *            what a click on the button does, in an Ajax request
	 * @return the factory of the primary button, in the primary color of the theme
	 */
	static IModalButtonFactory primary(IModel<String> label, IIcon icon,
		SerializableConsumer<AjaxRequestTarget> onClick)
	{
		return button(label, icon, true, onClick);
	}

	private static IModalButtonFactory button(IModel<String> label, IIcon icon, boolean primary,
		SerializableConsumer<AjaxRequestTarget> onClick)
	{
		Args.notNull(label, "label");
		Args.notNull(onClick, "onClick");
		return id -> new ModalButton(id, label)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				onClick.accept(target);
			}
		}.setPrimary(primary).setIcon(icon);
	}
}
