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
package com.antiliacode.widgets.modal;

import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;

/**
 * A button of the footer of a {@link Modal} doing something in an Ajax request; see
 * {@link IModalButtonFactory}. It is an {@link AjaxActionButton}, small by default: a text, an
 * icon and a text, or an icon only, in a variant.
 *
 * @since 11.0.0
 */
public abstract class ModalButton extends AjaxActionButton
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            the component id
	 * @param label
	 *            the text of the button
	 */
	public ModalButton(String id, IModel<String> label)
	{
		super(id, label);
		setSize(Size.SMALL);
	}

	/**
	 * @param primary
	 *            whether this is the primary button, in the primary color of the theme
	 * @return {@code this}
	 */
	public ModalButton setPrimary(boolean primary)
	{
		setVariant(primary ? Variant.PRIMARY : Variant.SECONDARY);
		return this;
	}

	/**
	 * @return whether this is the primary button, in the primary color of the theme
	 */
	public boolean isPrimary()
	{
		return getButtonBehavior().getVariant() == Variant.PRIMARY;
	}
}
