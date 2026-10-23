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
package com.antiliacode.widgets.demo.button;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.button.AjaxActionButton;
import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.datatable.ActionIcons;
import com.antiliacode.widgets.demo.datatable.FontAwesomeResourceReference;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.icon.FontAwesomeIcon;
import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.scrollbar.ScrollbarBehavior;
import com.antiliacode.widgets.scrollbar.ScrollbarBehavior.Axis;
import com.github.openjson.JSONObject;

/**
 * The buttons of the theme: every {@link Variant} in every {@link Size}, with a text, an icon and a
 * text, or an icon only, as {@link AjaxActionButton}s telling which one was clicked; disabled ones;
 * and a link drawn as a button by a {@link ButtonBehavior}. A drop-down chooses their icons: the
 * Font Awesome icons as inline SVG ({@link SvgIcon}) or the same drawn by the Font Awesome font
 * ({@link FontAwesomeIcon}). The React and Svelte tabs show the same buttons with their
 * {@code Button}.
 */
public class ButtonsPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The icon of the buttons of a variant, and the key of their text. */
	private static final List<VariantSample> SAMPLES = List.of(
		new VariantSample(Variant.PRIMARY, SvgIcon.FLOPPY_DISK, "save"),
		new VariantSample(Variant.SECONDARY, SvgIcon.ARROWS_ROTATE, "refresh"),
		new VariantSample(Variant.GHOST, SvgIcon.GEAR, "settings"),
		new VariantSample(Variant.DANGER, SvgIcon.TRASH_CAN, "delete"));

	/** The icons the drop-down chooses among. */
	private static final List<ActionIcons> ICON_SETS = List.of(ActionIcons.SVG,
		ActionIcons.FONT_AWESOME);

	private String clicked;

	/** The icons of the buttons. */
	private ActionIcons icons = ActionIcons.SVG;

	/**
	 * Constructor.
	 */
	public ButtonsPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		Label clickedLabel = new Label("clicked", () -> clicked != null
			? getString("clicked").replace("{0}", clicked) : getString("clicked.none"));
		themed.add(clickedLabel.setOutputMarkupId(true));
		themed.add(iconsChoice("icons"));

		// the table of the buttons scrolls sideways when the page is narrow
		WebMarkupContainer scroll = new WebMarkupContainer("scroll");
		scroll.add(new ScrollbarBehavior(Axis.X));
		themed.add(scroll);

		scroll.add(new ListView<>("sizes", List.of(Size.values()))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Size> item)
			{
				item.add(new Label("size", new ResourceModel(sizeKey(item.getModelObject()))));
			}
		});
		scroll.add(new ListView<>("variants", SAMPLES)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<VariantSample> row)
			{
				VariantSample sample = row.getModelObject();
				row.add(new Label("variant", new ResourceModel(variantKey(sample.variant()))));
				row.add(new ListView<>("cells", List.of(Size.values()))
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(ListItem<Size> cell)
					{
						Size size = cell.getModelObject();
						cell.add(newButton("text", sample, size, null, false, clickedLabel));
						cell.add(newButton("iconText", sample, size, iconOf(sample, icons),
							false, clickedLabel));
						cell.add(newButton("icon", sample, size, iconOf(sample, icons), true,
							clickedLabel));
					}
				});
			}
		});

		WebMarkupContainer disabled = new WebMarkupContainer("disabled");
		themed.add(disabled);
		disabled.add(new ListView<>("buttons", SAMPLES)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<VariantSample> item)
			{
				item.add(newButton("button", item.getModelObject(), Size.MEDIUM,
					iconOf(item.getModelObject(), icons), false, clickedLabel).setEnabled(false));
			}
		});

		themed.add(new ExternalLink("link", Model.of("#buttons-as-links"))
			.setBody(new ResourceModel("asLink.text"))
			.add(new ButtonBehavior(Variant.GHOST, Size.SMALL)));
	}

	/**
	 * @return the drop-down choosing the icons of the buttons; the page is rendered anew with them
	 */
	private Dropdown<ActionIcons> iconsChoice(String id)
	{
		Dropdown<ActionIcons> choice = new Dropdown<>(id, LambdaModel.of(() -> icons,
			set -> icons = set), ICON_SETS, new EnumChoiceRenderer<>(this));
		choice.setRequired(true);
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				setResponsePage(getPage());
			}
		});
		return choice;
	}

	/**
	 * @return the icon of the buttons of a variant in a set: the inline SVG, or the same icon of
	 *         the Font Awesome font
	 */
	private static IIcon iconOf(VariantSample sample, ActionIcons set)
	{
		return set.pick(sample.icon(), sample.icon(), FontAwesomeIcon.valueOf(sample.icon().name()));
	}

	private AjaxActionButton newButton(String id, VariantSample sample, Size size, IIcon icon,
		boolean iconOnly, Label clickedLabel)
	{
		return new AjaxActionButton(id, new ResourceModel(sample.textKey()))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				clicked = getString(variantKey(sample.variant())) + " · " +
					getString(sizeKey(size)) + " · " + getString(sample.textKey());
				target.add(clickedLabel);
			}
		}.setIcon(icon).setIconOnly(iconOnly).setVariant(sample.variant()).setSize(size);
	}

	private static String variantKey(Variant variant)
	{
		return "variant." + variant.name().toLowerCase(Locale.ROOT);
	}

	private static String sizeKey(Size size)
	{
		return "size." + size.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(ButtonBehavior.CSS));
		response.render(FontAwesomeResourceReference.styleSheet());
		response.render(CssHeaderItem.forReference(new CssResourceReference(ButtonsPage.class,
			"ButtonsPage.css")));
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "buttons";
	}

	/**
	 * @return the texts, those of the drop-down choosing the icons too, per variant its name, the
	 *         text of its buttons and the markup of their icon in each set, by the name of the
	 *         set, and the set chosen
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "variant", "clicked", "clicked.none",
			"disabled", "asLink", "asLink.text", "size.small", "size.medium", "size.large",
			"icons");
		for (ActionIcons set : ICON_SETS)
		{
			texts.put("icons." + set.name(), getString(ActionIcons.class.getSimpleName() + "." +
				set.name()));
		}
		FrameworkTabs.withDropdownTexts(this, texts);
		JSONObject variants = new JSONObject();
		for (VariantSample sample : SAMPLES)
		{
			JSONObject markup = new JSONObject();
			for (ActionIcons set : ICON_SETS)
			{
				markup.put(set.name(), iconOf(sample, set).getMarkup());
			}
			variants.put(sample.variant().name().toLowerCase(Locale.ROOT), new JSONObject()
				.put("name", getString(variantKey(sample.variant())))
				.put("text", getString(sample.textKey()))
				.put("icons", markup));
		}
		return new JSONObject().put("texts", texts).put("variants", variants)
			.put("icons", icons.name());
	}

	/**
	 * The buttons of a variant: their icon and the key of their text.
	 */
	private record VariantSample(Variant variant, SvgIcon icon, String textKey)
		implements Serializable
	{
	}
}
