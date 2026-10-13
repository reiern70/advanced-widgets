/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.popover;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.string.Strings;

import com.antilia.widgets.button.AjaxActionButton;
import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.icon.SvgIcon;
import com.antilia.widgets.popover.Popover;
import com.antilia.widgets.popover.PopoverDesign;
import com.antilia.widgets.popover.PopoverPlacement;
import com.antilia.widgets.popover.PopoverVariant;
import com.antilia.widgets.theme.LookBehavior;
import com.antilia.widgets.theme.LookContainer;
import com.antilia.widgets.theme.LookPart;
import com.antilia.widgets.theme.Looks;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link Popover}s: bubbles with an arrow pointing at the button that opened them. Twelve buttons
 * around a target open one on each side of it, from either edge or centered, the one in the
 * middle where there is most room, in the design and the variant the drop-downs choose; a gallery
 * shows every design drawn open where its drop-down says, each opening there for real. The server fills a popover with components: facts it
 * renders as the popover opens, and a form renaming a project, its errors shown in it. Popovers
 * give help on the fields of a form and tell more about the rows of a table. The React and Svelte
 * tabs show the same in those frameworks, their content rendered by them.
 */
public class PopoversPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The placements, in the order of the buttons around the target, auto in the middle. */
	static final List<PopoverPlacement> PLACEMENTS = Arrays.asList(PopoverPlacement.values());

	/** The widest the popovers around the target get. */
	static final String PLACEMENT_WIDTH = "16rem";

	/** The positions of the gallery: every placement but auto, which has no side of its own. */
	static final List<PopoverPlacement> POSITIONS = Arrays.stream(PopoverPlacement.values())
		.filter(placement -> placement != PopoverPlacement.AUTO).toList();

	/** The widest the popovers of the gallery get, drawn open or opened. */
	static final String GALLERY_WIDTH = "14rem";

	/** The fields of the form, the keys of their labels; their help under key.help. */
	static final List<String> FIELDS = List.of("username", "password", "code");

	/** The countries of the table: their codes and their languages. */
	static final List<Country> COUNTRIES = List.of(new Country("DE", "de"), new Country("ES", "es"),
		new Country("JP", "ja"), new Country("BR", "pt"), new Country("CA", "en"),
		new Country("AU", "en"));

	/** The icons of the demo. */
	static final IIcon INFO_ICON = SvgIcon.CIRCLE_INFO;

	static final IIcon HELP_ICON = SvgIcon.CIRCLE_QUESTION;

	static final IIcon RENAME_ICON = SvgIcon.PEN_TO_SQUARE;

	static final IIcon STATUS_ICON = SvgIcon.SERVER;

	static final IIcon AUTO_ICON = SvgIcon.BULLSEYE;

	/** The name of the project the page starts with. */
	static final String PROJECT = "Apollo";

	private PopoverDesign design = PopoverDesign.CARD;

	private PopoverVariant variant = PopoverVariant.DEFAULT;

	/** Where the popovers of the gallery are, drawn open and opened. */
	private PopoverPlacement position = PopoverPlacement.BOTTOM_CENTER;

	private String project = PROJECT;

	/** The name in the field of the form renaming the project. */
	private String draft;

	/** Why the name in the field is not taken, {@code null} while it is fine. */
	private String error;

	/**
	 * Constructor.
	 */
	public PopoversPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		WebMarkupContainer gallery = new WebMarkupContainer("gallery");
		gallery.setOutputMarkupId(true);

		Form<Void> controls = new Form<>("controls");
		themed.add(controls);
		controls.add(choice("design", "design", Arrays.asList(PopoverDesign.values()), gallery));
		controls.add(choice("variant", "variant", Arrays.asList(PopoverVariant.values()), gallery));

		themed.add(new ListView<>("placements", PLACEMENTS)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<PopoverPlacement> item)
			{
				PopoverPlacement placement = item.getModelObject();
				String name = getString("placement." + placement.name());
				item.add(new AjaxActionButton("button", Model.of(name))
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						chosen(new Popover(getString("placementText").replace("{0}", name)))
							.setTitle(name).setPlacement(placement).setMaxWidth(PLACEMENT_WIDTH)
							.show(target, this);
					}
				}.setIcon(placement == PopoverPlacement.AUTO ? AUTO_ICON : null)
					.setSize(ButtonBehavior.Size.SMALL)
					.setVariant(placement == PopoverPlacement.AUTO ? ButtonBehavior.Variant.PRIMARY :
						ButtonBehavior.Variant.SECONDARY)
					.add(AttributeModifier.append("class", "popovers-demo__place popovers-demo__place--" +
						placement.cssName())));
			}
		});

		Form<Void> galleryControls = new Form<>("galleryControls");
		themed.add(galleryControls);
		galleryControls.add(choice("position", "placement", POSITIONS, gallery));
		themed.add(gallery);
		gallery.add(new ListView<>("designs", Arrays.asList(PopoverDesign.values()))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<PopoverDesign> item)
			{
				PopoverDesign shown = item.getModelObject();
				String name = getString("design." + shown.name());
				String text = getString("design." + shown.name() + ".text");
				// in the classes the popover opening has, its look's too
				WebMarkupContainer preview = new WebMarkupContainer("preview");
				preview.add(AttributeModifier.replace("class", () -> Popover.getCssClasses(
					Looks.of(item), shown, variant)));
				// on the side and along the edge of the position, as it opens
				preview.add(AttributeModifier.replace("data-side", () -> sideOf(position)));
				preview.add(AttributeModifier.replace("data-align", () -> alignmentOf(position)));
				preview.add(new LookContainer("arrow", LookPart.POPOVER_ARROW));
				WebMarkupContainer header = new WebMarkupContainer("header");
				header.add(new LookBehavior(LookPart.POPOVER_HEADER));
				preview.add(header);
				header.add(new Label("title", name).add(new LookBehavior(LookPart.POPOVER_TITLE)));
				header.add(new LookContainer("close", LookPart.POPOVER_CLOSE));
				preview.add(new Label("text", text).add(new LookBehavior(LookPart.POPOVER_BODY)));
				item.add(preview);
				item.add(new AjaxActionButton("try", new ResourceModel("try"))
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						new Popover(text).setTitle(name).setDesign(shown).setVariant(variant)
							.setPlacement(position).setMaxWidth(GALLERY_WIDTH).show(target, this);
					}
				}.setSize(ButtonBehavior.Size.SMALL));
			}
		});

		addStatus(themed);
		addRename(themed);
		addFields(themed);
		addCountries(themed);
	}

	/**
	 * @return the side of its trigger a popover of a placement other than auto opens on, with room
	 */
	static String sideOf(PopoverPlacement placement)
	{
		return placement.cssName().split("-")[0];
	}

	/**
	 * @return where along its side a popover of a placement other than auto is: from the start
	 *         edge of its trigger, centered, or from the end edge
	 */
	static String alignmentOf(PopoverPlacement placement)
	{
		String edge = placement.cssName().split("-")[1];
		return switch (edge)
		{
			case "left", "top" -> "start";
			case "right", "bottom" -> "end";
			default -> "center";
		};
	}

	/**
	 * @return a drop-down of the values of an enum, named by the texts under a prefix, rendering a
	 *         component again on change
	 */
	private <T extends Enum<T>> DropDownChoice<T> choice(String id, String prefix, List<T> values,
		Component changed)
	{
		DropDownChoice<T> choice = new DropDownChoice<>(id, new PropertyModel<>(this, id), values,
			new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(T value)
				{
					return getString(prefix + "." + value.name());
				}

				@Override
				public String getIdValue(T value, int index)
				{
					return value.name();
				}
			});
		choice.setRequired(true).add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(changed);
			}
		});
		return choice;
	}

	/**
	 * @return a popover in the design and the variant chosen
	 */
	private Popover chosen(Popover popover)
	{
		return popover.setDesign(design).setVariant(variant);
	}

	/**
	 * A button asking the server how it is: the facts, rendered as the popover opens.
	 */
	private void addStatus(WebMarkupContainer themed)
	{
		WebMarkupContainer status = Popover.asContent(new WebMarkupContainer("statusContent"));
		themed.add(status);
		status.add(new ListView<>("facts", this::facts)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Map.Entry<String, String>> item)
			{
				item.add(new Label("name", getString("fact." + item.getModelObject().getKey())));
				item.add(new Label("value", item.getModelObject().getValue()));
			}
		});
		themed.add(new AjaxActionButton("status", new ResourceModel("status.wicket"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				// the facts as they are now, then the popover showing them
				target.add(status);
				new Popover(getString("statusText"), status).setTitle(getString("statusTitle"))
					.setPlacement(PopoverPlacement.BOTTOM_LEFT)
					.setMaxWidth("24rem").show(target, this);
			}
		}.setIcon(STATUS_ICON));
	}

	/**
	 * @return what the server says of itself, by the keys of their names
	 */
	private List<Map.Entry<String, String>> facts()
	{
		Locale locale = getLocale();
		Runtime runtime = Runtime.getRuntime();
		long mega = 1024 * 1024;
		Map<String, String> facts = new LinkedHashMap<>();
		facts.put("time", DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(locale)
			.format(LocalTime.now()));
		facts.put("wicket", Application.get().getFrameworkSettings().getVersion());
		facts.put("java", System.getProperty("java.version"));
		facts.put("uptime", getString("minutes").replace("{0}",
			String.valueOf(ManagementFactory.getRuntimeMXBean().getUptime() / 60000)));
		facts.put("memory", getString("megabytes")
			.replace("{0}", String.valueOf((runtime.totalMemory() - runtime.freeMemory()) / mega))
			.replace("{1}", String.valueOf(runtime.maxMemory() / mega)));
		return new ArrayList<>(facts.entrySet());
	}

	/**
	 * The name of a project and a button renaming it in a form in a popover, which shows what is
	 * wrong with the name, and closes once it is taken.
	 */
	private void addRename(WebMarkupContainer themed)
	{
		Label name = new Label("project", () -> project);
		name.setOutputMarkupId(true);
		themed.add(name);

		WebMarkupContainer content = Popover.asContent(new WebMarkupContainer("renameContent"));
		themed.add(content);
		Form<Void> form = new Form<>("renameForm");
		content.add(form);
		form.add(new TextField<>("name", new PropertyModel<String>(this, "draft"))
			.setLabel(new ResourceModel("name")));
		form.add(new Label("error", () -> error).add(AttributeModifier.replace("hidden",
			() -> error == null ? AttributeModifier.VALUELESS_ATTRIBUTE_ADD : null)));
		form.add(new AjaxButton("save")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				if (Strings.isEmpty(draft) || draft.isBlank())
				{
					error = getString("required");
					target.add(content);
					return;
				}
				project = draft.strip();
				error = null;
				target.add(name);
				Popover.close(target);
			}
		}.add(new ButtonBehavior(ButtonBehavior.Variant.PRIMARY, ButtonBehavior.Size.SMALL)));
		form.add(new AjaxActionButton("cancel", new ResourceModel("cancel"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				Popover.close(target);
			}
		}.setSize(ButtonBehavior.Size.SMALL).setVariant(ButtonBehavior.Variant.GHOST));

		themed.add(new AjaxActionButton("rename", new ResourceModel("rename"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				// the form as it starts, with the name of the project
				draft = project;
				error = null;
				target.add(content);
				new Popover(content).setTitle(getString("renameTitle"))
					.setPlacement(PopoverPlacement.RIGHT_TOP).setDesign(PopoverDesign.AURORA)
					.show(target, this);
			}
		}.setIcon(RENAME_ICON).setIconOnly(true).setSize(ButtonBehavior.Size.SMALL)
			.setVariant(ButtonBehavior.Variant.GHOST));
	}

	/**
	 * A form whose fields have a button giving help on them beside their labels.
	 */
	private void addFields(WebMarkupContainer themed)
	{
		Form<Void> form = new Form<>("fields");
		themed.add(form);
		form.add(new ListView<>("field", FIELDS)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<String> item)
			{
				String key = item.getModelObject();
				String label = getString("field." + key);
				TextField<String> input = new TextField<>("input", Model.of(""));
				input.setLabel(Model.of(label)).setOutputMarkupId(true);
				input.add(AttributeModifier.replace("type", "password".equals(key) ? "password" :
					"text"));
				item.add(new Label("label", label)
					.add(AttributeModifier.replace("for", input::getMarkupId)));
				item.add(input);
				item.add(new AjaxActionButton("help", Model.of(getString("help").replace("{0}",
					label)))
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						new Popover(getString("field." + key + ".help")).setTitle(label)
							.setPlacement(PopoverPlacement.RIGHT_CENTER).setDesign(PopoverDesign.INVERSE)
							.show(target, this);
					}
				}.setIcon(HELP_ICON).setIconOnly(true).setSize(ButtonBehavior.Size.SMALL)
					.setVariant(ButtonBehavior.Variant.GHOST));
			}
		});
	}

	/**
	 * A table of countries, a button in each row telling more of the country.
	 */
	private void addCountries(WebMarkupContainer themed)
	{
		themed.add(new ListView<>("countries", COUNTRIES)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Country> item)
			{
				Country country = item.getModelObject();
				Locale locale = getLocale();
				String name = country.name(locale);
				item.add(new Label("name", name));
				item.add(new Label("language", country.language(locale)));
				item.add(new Label("currency", country.currency(locale)));
				item.add(new AjaxActionButton("about", Model.of(getString("aboutCountry")
					.replace("{0}", name)))
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						new Popover(country.describe(PopoversPage.this, locale)).setTitle(name)
							.setPlacement(PopoverPlacement.LEFT_CENTER).setDesign(PopoverDesign.CALLOUT)
							.show(target, this);
					}
				}.setIcon(INFO_ICON).setIconOnly(true).setSize(ButtonBehavior.Size.SMALL)
					.setVariant(ButtonBehavior.Variant.GHOST));
			}
		});
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "popovers";
	}

	/**
	 * @return the texts, the placements, the designs, the variants, the fields, the countries and the
	 *         icons as the page has them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "placementsTitle", "placementsText", "design",
			"variant", "position", "placementText", "galleryTitle", "try", "richTitle", "status.app",
			"statusTitle", "statusText", "fact.time", "fact.window", "fact.language", "fact.opened",
			"project", "rename", "renameTitle", "name", "save", "cancel", "required", "fieldsTitle",
			"help", "tableTitle", "country", "language", "currency", "aboutCountry");
		texts.put("close", getString("Popover.close"));
		for (PopoverPlacement each : PopoverPlacement.values())
		{
			texts.put("placement." + each.name(), getString("placement." + each.name()));
		}
		for (PopoverDesign each : PopoverDesign.values())
		{
			texts.put("design." + each.name(), getString("design." + each.name()))
				.put("design." + each.name() + ".text", getString("design." + each.name() + ".text"));
		}
		for (PopoverVariant each : PopoverVariant.values())
		{
			texts.put("variant." + each.name(), getString("variant." + each.name()));
		}
		JSONArray fields = new JSONArray();
		FIELDS.forEach(key -> fields.put(new JSONObject().put("key", key)
			.put("label", getString("field." + key)).put("help", getString("field." + key + ".help"))));
		Locale locale = getLocale();
		JSONArray countries = new JSONArray();
		COUNTRIES.forEach(country -> countries.put(new JSONObject().put("code", country.code())
			.put("name", country.name(locale)).put("language", country.language(locale))
			.put("currency", country.currency(locale)).put("about", country.describe(this, locale))));
		return new JSONObject()
			.put("texts", texts)
			.put("placements", new JSONArray(PLACEMENTS.stream().map(PopoverPlacement::cssName)
				.toList()))
			.put("designs", new JSONArray(Arrays.stream(PopoverDesign.values()).map(PopoverDesign::cssName)
				.toList()))
			.put("variants", new JSONArray(Arrays.stream(PopoverVariant.values())
				.map(PopoverVariant::cssName).toList()))
			.put("design", design.cssName())
			.put("variant", variant.cssName())
			.put("placementWidth", PLACEMENT_WIDTH)
			.put("positions", new JSONArray(POSITIONS.stream().map(PopoverPlacement::cssName)
				.toList()))
			.put("position", position.cssName())
			.put("galleryWidth", GALLERY_WIDTH)
			.put("project", project)
			.put("fields", fields)
			.put("countries", countries)
			.put("icons", new JSONObject()
				.put("info", INFO_ICON.getMarkup())
				.put("help", HELP_ICON.getMarkup())
				.put("rename", RENAME_ICON.getMarkup())
				.put("status", STATUS_ICON.getMarkup())
				.put("auto", AUTO_ICON.getMarkup()));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Popover.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(PopoversPage.class, "PopoversPage.css")));
	}

	/**
	 * @return the design of the popovers around the target and of the gallery
	 */
	public PopoverDesign getDesign()
	{
		return design;
	}

	public void setDesign(PopoverDesign design)
	{
		this.design = design;
	}

	/**
	 * @return the variant of the popovers around the target and of the gallery
	 */
	public PopoverVariant getVariant()
	{
		return variant;
	}

	public void setVariant(PopoverVariant variant)
	{
		this.variant = variant;
	}

	/**
	 * @return where the popovers of the gallery are
	 */
	public PopoverPlacement getPosition()
	{
		return position;
	}

	public void setPosition(PopoverPlacement position)
	{
		this.position = position;
	}

	/**
	 * @return the name of the project
	 */
	public String getProject()
	{
		return project;
	}

	public String getDraft()
	{
		return draft;
	}

	public void setDraft(String draft)
	{
		this.draft = draft;
	}

	/**
	 * A country of the table: its code and its language, which the JDK names in any language.
	 *
	 * @param code
	 *            its ISO 3166 code
	 * @param language
	 *            the ISO 639 code of its language
	 */
	record Country(String code, String language) implements Serializable
	{
		private Locale own()
		{
			return Locale.of(language, code);
		}

		String name(Locale locale)
		{
			return own().getDisplayCountry(locale);
		}

		String language(Locale locale)
		{
			return own().getDisplayLanguage(locale);
		}

		String currency(Locale locale)
		{
			return Currency.getInstance(own()).getDisplayName(locale);
		}

		/**
		 * @return more of the country, in words: its name in its own language, its ISO codes, and
		 *         the symbol and code of its currency
		 */
		String describe(Component component, Locale locale)
		{
			Currency currency = Currency.getInstance(own());
			return component.getString("countryInfo")
				.replace("{0}", own().getDisplayCountry(own()))
				.replace("{1}", code + " / " + own().getISO3Country())
				.replace("{2}", currency.getSymbol(own()) + " " + currency.getCurrencyCode());
		}
	}
}
