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
package com.antiliacode.widgets.demo.dropdown;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.modal.IModalButtonFactory;
import com.antiliacode.widgets.modal.ModalSubmitButton;

/**
 * The form of the dialog of the {@link DropdownPage}, its body: a drop-down of the long list of
 * {@link UnitsOfMeasure units of measure}, long options in groups, the few used most first. It is
 * the last thing in the body, so its open list reaches over the footer of the dialog. The Choose
 * button of the footer, {@link #chooseButton()}, takes the unit.
 */
public abstract class UnitFormPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final Form<Void> form;

	private final Dropdown<String> unit;

	/**
	 * @param id
	 *            the component id, {@code Modal.BODY_ID}
	 * @param chosen
	 *            the code of the unit chosen so far
	 */
	public UnitFormPanel(String id, String chosen)
	{
		super(id);
		form = new Form<>("form");
		add(form);
		unit = new Dropdown<>("unit", Model.of(chosen), UnitsOfMeasure.codes(),
			UnitsOfMeasure.renderer(this));
		unit.setGroup(code -> UnitsOfMeasure.group(this, code));
		unit.setNullValid(false);
		form.add(unit.setRequired(true).setLabel(new ResourceModel("units.unit")));
	}

	/**
	 * @return the factory of the Choose button of the footer of the dialog, submitting the form,
	 *         also on Enter
	 */
	public IModalButtonFactory chooseButton()
	{
		return id -> new ModalSubmitButton(id, new ResourceModel("units.ok"), form)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				onChosen(unit.getModelObject(), target);
			}
		}.setPrimary(true).setDefault(true).setIcon(SvgIcon.CHECK);
	}

	/**
	 * Called once a unit is chosen.
	 *
	 * @param code
	 *            the code of the unit
	 */
	protected abstract void onChosen(String code, AjaxRequestTarget target);
}
