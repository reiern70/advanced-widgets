/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.button;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.advanced.widgets.button.AjaxActionButton;
import com.advanced.widgets.button.ButtonBehavior;
import com.advanced.widgets.button.ButtonBehavior.Size;
import com.advanced.widgets.button.ButtonBehavior.Variant;
import com.advanced.widgets.demo.DemoPage;
import com.advanced.widgets.demo.frameworks.FrameworkTabs;
import com.advanced.widgets.icon.IIcon;
import com.advanced.widgets.icon.SvgIcon;
import com.advanced.widgets.scrollbar.ScrollbarBehavior;
import com.advanced.widgets.scrollbar.ScrollbarBehavior.Axis;
import com.github.openjson.JSONObject;

/**
 * The buttons of the theme: every {@link Variant} in every {@link Size}, with a text, an icon and a
 * text, or an icon only, as {@link AjaxActionButton}s telling which one was clicked; disabled ones;
 * and a link drawn as a button by a {@link ButtonBehavior}. The React and Svelte tabs show the same
 * buttons with their {@code Button}.
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

	private String clicked;

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
						cell.add(newButton("iconText", sample, size, sample.icon(), false,
							clickedLabel));
						cell.add(newButton("icon", sample, size, sample.icon(), true,
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
					item.getModelObject().icon(), false, clickedLabel).setEnabled(false));
			}
		});

		themed.add(new ExternalLink("link", Model.of("#buttons-as-links"))
			.setBody(new ResourceModel("asLink.text"))
			.add(new ButtonBehavior(Variant.GHOST, Size.SMALL)));
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
		response.render(CssHeaderItem.forReference(new CssResourceReference(ButtonsPage.class,
			"ButtonsPage.css")));
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "buttons";
	}

	/**
	 * @return the texts, and per variant its name, the text of its buttons and the markup of
	 *         their icon
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "variant", "clicked", "clicked.none",
			"disabled", "asLink", "asLink.text", "size.small", "size.medium", "size.large");
		JSONObject variants = new JSONObject();
		for (VariantSample sample : SAMPLES)
		{
			variants.put(sample.variant().name().toLowerCase(Locale.ROOT), new JSONObject()
				.put("name", getString(variantKey(sample.variant())))
				.put("text", getString(sample.textKey()))
				.put("icon", sample.icon().getMarkup()));
		}
		return new JSONObject().put("texts", texts).put("variants", variants);
	}

	/**
	 * The buttons of a variant: their icon and the key of their text.
	 */
	private record VariantSample(Variant variant, SvgIcon icon, String textKey)
		implements Serializable
	{
	}
}
