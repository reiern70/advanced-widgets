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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.ChoicesResource;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.dropdown.MultiDropdown;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.modal.IModalButtonFactory;
import com.antiliacode.widgets.modal.Modal;
import com.github.openjson.JSONObject;

/**
 * {@link Dropdown} and {@link MultiDropdown}: drop-downs opened, filtered and chosen from in the
 * browser, which post their choice like native selects, in the form of the
 * {@link DropdownExamplesPanel}. Below it, a {@link Modal} chooses a unit of measure in a
 * drop-down of a long list of long options in groups, the few used most first, whose open list
 * reaches over the footer of the dialog ({@link UnitFormPanel}). The React and Svelte tabs show
 * the same in those frameworks, the countries loaded from the {@link ChoicesResource}.
 */
public class DropdownPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private final DropdownExamplesPanel examples;

	private final Label unitLabel;

	private final Modal unitDialog;

	private String unit = UnitsOfMeasure.DEFAULT;

	/**
	 * Constructor.
	 */
	public DropdownPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		examples = new DropdownExamplesPanel("examples");
		themed.add(examples);

		unitLabel = new Label("unit", () -> getString("units.chosen").replace("{0}",
			UnitsOfMeasure.name(this, unit)));
		unitLabel.setOutputMarkupId(true);
		themed.add(unitLabel);
		themed.add(new AjaxActionButton("chooseUnit", new ResourceModel("units.choose"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				openUnitDialog(target);
			}
		}.setIcon(SvgIcon.RULER_COMBINED));

		unitDialog = new Modal("unitDialog");
		add(unitDialog);
	}

	/**
	 * Opens the dialog choosing the unit: Cancel and Choose in its footer.
	 */
	private void openUnitDialog(AjaxRequestTarget target)
	{
		UnitFormPanel form = new UnitFormPanel(Modal.BODY_ID, unit)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onChosen(String code, AjaxRequestTarget target)
			{
				unit = code;
				unitDialog.close(target);
				target.add(unitLabel);
			}
		};
		unitDialog.setSize(Modal.Size.MEDIUM);
		unitDialog.open(new ResourceModel("units.dialog"), form, target,
			IModalButtonFactory.close(new ResourceModel("units.cancel"), SvgIcon.XMARK),
			form.chooseButton());
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "dropdown";
	}

	/**
	 * @return the texts, the options but the countries, which come from the
	 *         {@link ChoicesResource}, and the choices as the examples have them; and under
	 *         {@code units} those of the dialog: its texts, the units of measure in their groups,
	 *         the unit chosen and the icons of the buttons, as inline SVG
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "units.title", "units.chosen", "units.choose",
			"units.dialog", "units.text", "units.unit", "units.ok", "units.cancel", "units.close");
		JSONObject icons = new JSONObject()
			.put("choose", SvgIcon.RULER_COMBINED.getMarkup())
			.put("cancel", SvgIcon.XMARK.getMarkup())
			.put("ok", SvgIcon.CHECK.getMarkup());
		JSONObject units = new JSONObject()
			.put("texts", texts)
			.put("options", UnitsOfMeasure.options(this))
			.put("value", unit)
			.put("icons", icons);
		return examples.getFrameworkConfig().put("units", units);
	}
}
