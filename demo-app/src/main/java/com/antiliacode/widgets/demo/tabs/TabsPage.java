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
package com.antiliacode.widgets.demo.tabs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.popover.Popover;
import com.antiliacode.widgets.popover.PopoverPlacement;
import com.antiliacode.widgets.popover.PopoverVariant;
import com.antiliacode.widgets.tabs.Tab;
import com.antiliacode.widgets.tabs.TabAction;
import com.antiliacode.widgets.tabs.Tabs;
import com.antiliacode.widgets.tabs.TabsOrientation;
import com.antiliacode.widgets.tabs.TabsDesign;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link Tabs}: documents in closable tabs, after a welcome tab that is not. The server hears each
 * change of a document, by the Ajax behavior of its text area, and marks its tab as having changes
 * not saved, which Save takes away again; closing a tab with changes asks first, and the server
 * hears of the tab closed. Each document has an action, before the button closing it, showing what
 * the server knows of it in a popover pointing at the action, and a button opens a new document via Ajax, leaving the
 * others as they are in the browser. A second tabs' orientation and design a check box and a drop-down
 * switch, and the server hears of the tab the user selects. A gallery shows the designs made for tabs
 * along the top and those made for tabs down the side. The React and Svelte tabs show the same in
 * those frameworks.
 */
public class TabsPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The documents the page starts with: their names and their texts. */
	static final List<Document> DOCUMENTS = List.of(new Document("notes.txt",
		"Buy milk\nCall the bank\nBook the flights"), new Document("todo.txt",
		"- Write the tabs demo\n- Test it in three frameworks"));

	/** The sections of the settings, the keys of their titles and texts. */
	static final List<String> SECTIONS = List.of("profile", "alerts", "privacy");

	/** The designs the gallery shows along the top, and the tabs it shows them with. */
	static final List<TabsDesign> TOP_DESIGNS = List.of(TabsDesign.SEGMENTED, TabsDesign.FOLDER,
		TabsDesign.LIFTED);

	static final Map<String, IIcon> TOP_TABS = icons("overview", SvgIcon.EYE, "specs",
		SvgIcon.RULER, "reviews", SvgIcon.STAR);

	/** The designs the gallery shows down the side, the tabs it shows them with and the one selected. */
	static final List<TabsDesign> SIDE_DESIGNS = List.of(TabsDesign.SIDEBAR, TabsDesign.STEPS,
		TabsDesign.CARDS);

	static final Map<String, IIcon> SIDE_TABS = icons("account", SvgIcon.USER, "shipping",
		SvgIcon.TRUCK, "payment", SvgIcon.CREDIT_CARD, "review", SvgIcon.CLIPBOARD_CHECK);

	static final String SIDE_SELECTED = "payment";

	/** The icons of the demo. */
	static final IIcon WELCOME_ICON = SvgIcon.BOOK;

	static final IIcon DOCUMENT_ICON = SvgIcon.FILE_LINES;

	static final IIcon INFO_ICON = SvgIcon.CIRCLE_INFO;

	private final Tabs documents;

	private final Label status;

	private final Tabs settings;

	private String message;

	private int untitled;

	private boolean vertical = true;

	private TabsDesign design = TabsDesign.PILLS;

	/**
	 * Constructor.
	 */
	public TabsPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		DemoCases cases = new DemoCases("cases", this);
		themed.add(cases);
		Fragment documentsCase = cases.addCase("documentsTitle", "documentsCase");
		Fragment settingsCase = cases.addCase("settingsTitle", "settingsCase");
		Fragment galleryCase = cases.addCase("galleryTitle", "galleryCase");

		status = new Label("status", () -> message != null ? message : getString("start"));
		status.setOutputMarkupId(true);
		documentsCase.add(status);

		documents = new Tabs("documents")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClosed(AjaxRequestTarget target, Tab tab)
			{
				say(target, "closed", ((DocumentTab)tab).document.getName());
			}
		};
		documents.setLabel(new ResourceModel("documents"));
		documents.addTab(new Tab("welcome", new ResourceModel("welcome"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newPanel(String id)
			{
				return new Fragment(id, "welcomePanel", TabsPage.this);
			}
		}.setIcon(WELCOME_ICON));
		DOCUMENTS.forEach(document -> documents.addTab(new DocumentTab(document.copy())));
		documentsCase.add(documents);

		documentsCase.add(new AjaxLink<Void>("newDocument")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				Document document = new Document(getString("untitled").replace("{0}",
					String.valueOf(++untitled)), "");
				DocumentTab tab = new DocumentTab(document);
				documents.addTab(tab, target).select(tab, target);
				say(target, "opened", document.getName());
			}
		}.add(new ButtonBehavior(ButtonBehavior.Variant.SECONDARY, ButtonBehavior.Size.SMALL)));

		Label selected = new Label("selected", this::describeSelected);
		selected.setOutputMarkupId(true);
		settingsCase.add(selected);

		settings = new Tabs("settings")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setOrientation(vertical ? TabsOrientation.VERTICAL : TabsOrientation.HORIZONTAL);
				setDesign(design);
			}

			@Override
			protected void onSelected(AjaxRequestTarget target, Tab tab)
			{
				target.add(selected);
			}
		};
		settings.setLabel(new ResourceModel("settings")).setSendToServer(true)
			.setOutputMarkupId(true);
		for (String section : SECTIONS)
		{
			settings.addTab(new Tab(section, new ResourceModel(section))
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected Component newPanel(String id)
				{
					return new Label(id, new ResourceModel(section + ".text"));
				}
			});
		}
		settingsCase.add(settings);

		Form<Void> form = new Form<>("form");
		settingsCase.add(form);
		form.add(new CheckBox("vertical", new PropertyModel<>(this, "vertical"))
			.add(rerender(settings)));
		form.add(new Dropdown<>("design", new PropertyModel<>(this, "design"),
			Arrays.asList(TabsDesign.values()), new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(TabsDesign design)
				{
					return getString("design." + design.name());
				}

				@Override
				public String getIdValue(TabsDesign design, int index)
				{
					return design.name();
				}
			}).setRequired(true).add(rerender(settings)));

		galleryCase.add(gallery("topDesigns", TOP_DESIGNS, TabsOrientation.HORIZONTAL, TOP_TABS,
			null));
		galleryCase.add(gallery("sideDesigns", SIDE_DESIGNS, TabsOrientation.VERTICAL, SIDE_TABS,
			SIDE_SELECTED));
	}

	/**
	 * @return the tabs of the gallery in some designs, each under its name
	 */
	private ListView<TabsDesign> gallery(String id, List<TabsDesign> designs,
		TabsOrientation orientation, Map<String, IIcon> tabs, String selected)
	{
		return new ListView<>(id, designs)
		{
			private static final long serialVersionUID = 1L;

			{
				// the tabs keep the tab the user selected when the page renders again
				setReuseItems(true);
			}

			@Override
			protected void populateItem(ListItem<TabsDesign> item)
			{
				TabsDesign shown = item.getModelObject();
				item.add(new Label("name", getString("design." + shown.name())));
				Tabs example = new Tabs("tabs", Model.of(selected));
				example.setDesign(shown).setOrientation(orientation)
					.setLabel(Model.of(getString("design." + shown.name())));
				tabs.forEach((name, icon) -> example.addTab(new Tab(name, new ResourceModel(name))
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected Component newPanel(String panelId)
					{
						return new Label(panelId, new ResourceModel(name + ".text"));
					}
				}.setIcon(icon)));
				item.add(example);
			}
		};
	}

	private static Map<String, IIcon> icons(Object... namesAndIcons)
	{
		Map<String, IIcon> icons = new LinkedHashMap<>();
		for (int i = 0; i < namesAndIcons.length; i += 2)
		{
			icons.put((String)namesAndIcons[i], (IIcon)namesAndIcons[i + 1]);
		}
		return Collections.unmodifiableMap(icons);
	}

	private static AjaxFormComponentUpdatingBehavior rerender(Component component)
	{
		return new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(component);
			}
		};
	}

	/**
	 * @return the tab of the settings selected, in words
	 */
	private String describeSelected()
	{
		return getString("selected").replace("{0}", getString(settings.getSelectedTab().getName()));
	}

	/**
	 * Says what happened below the documents.
	 */
	private void say(AjaxRequestTarget target, String key, String name)
	{
		message = getString(key).replace("{0}", name);
		target.add(status);
	}

	/**
	 * @return what the server knows of a document, in words
	 */
	String describe(Document document)
	{
		String text = document.getText();
		long lines = text.isEmpty() ? 0 : text.lines().count();
		return getString("info").replace("{0}", document.getName())
			.replace("{1}", String.valueOf(lines))
			.replace("{2}", String.valueOf(text.length()))
			.replace("{3}", getString(document.isModified() ? "unsaved" : "allSaved"));
	}

	/**
	 * @return the documents, in the order of their tabs
	 */
	public List<Document> getDocuments()
	{
		return documents.getTabs().stream().filter(DocumentTab.class::isInstance)
			.map(tab -> ((DocumentTab)tab).document).toList();
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "tabs";
	}

	/**
	 * @return the texts, the documents, the sections and the icons as the page has them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "cases", "documentsTitle", "documents",
			"welcome", "welcomeText", "newDocument", "save", "about", "info", "unsaved", "allSaved",
			"untitled", "closed", "saved", "opened", "start", "settingsTitle", "settings", "vertical",
			"design", "selected", "galleryTitle", "alongTheTop", "downTheSide");
		for (TabsDesign each : TabsDesign.values())
		{
			texts.put("design." + each.name(), getString("design." + each.name()));
		}
		List<String> names = new ArrayList<>(SECTIONS);
		names.addAll(TOP_TABS.keySet());
		names.addAll(SIDE_TABS.keySet());
		for (String name : names)
		{
			texts.put(name, getString(name)).put(name + ".text", getString(name + ".text"));
		}
		JSONObject tabs = new JSONObject();
		for (String name : new String[] { "close", "modified", "closeModifiedTitle", "closeModified",
			"discard", "keep" })
		{
			tabs.put(name, getString("Tabs." + name));
		}
		JSONArray documentsConfig = new JSONArray();
		DOCUMENTS.forEach(document -> documentsConfig.put(new JSONObject()
			.put("name", document.getName()).put("text", document.getText())));
		return new JSONObject()
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("tabs", tabs)
			.put("documents", documentsConfig)
			.put("sections", new JSONArray(SECTIONS))
			.put("icons", new JSONObject()
				.put("welcome", WELCOME_ICON.getMarkup())
				.put("document", DOCUMENT_ICON.getMarkup())
				.put("info", INFO_ICON.getMarkup()))
			.put("vertical", vertical)
			.put("design", design.cssName())
			.put("designs", new JSONArray(Arrays.stream(TabsDesign.values()).map(TabsDesign::cssName)
				.toList()))
			.put("gallery", new JSONObject()
				.put("top", gallery(TOP_DESIGNS, TOP_TABS, null))
				.put("side", gallery(SIDE_DESIGNS, SIDE_TABS, SIDE_SELECTED)));
	}

	/**
	 * @return a part of the gallery as the apps take it: its designs, its tabs with their icons and
	 *         the tab selected
	 */
	private static JSONObject gallery(List<TabsDesign> designs, Map<String, IIcon> tabs,
		String selected)
	{
		JSONArray tabsConfig = new JSONArray();
		tabs.forEach((name, icon) -> tabsConfig.put(new JSONObject().put("name", name)
			.put("icon", icon.getMarkup())));
		JSONObject config = new JSONObject()
			.put("designs", new JSONArray(designs.stream().map(TabsDesign::cssName).toList()))
			.put("tabs", tabsConfig);
		if (selected != null)
		{
			config.put("selected", selected);
		}
		return config;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Tabs.renderHeadItems(response);
		Popover.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(TabsPage.class, "TabsPage.css")));
	}

	/**
	 * @return whether the second tabs are down the side
	 */
	public boolean isVertical()
	{
		return vertical;
	}

	/**
	 * @param vertical
	 *            whether the second tabs are down the side
	 */
	public void setVertical(boolean vertical)
	{
		this.vertical = vertical;
	}

	/**
	 * @return the design of the second tabs
	 */
	public TabsDesign getDesign()
	{
		return design;
	}

	/**
	 * @param design
	 *            the design of the second tabs
	 */
	public void setDesign(TabsDesign design)
	{
		this.design = design;
	}

	/**
	 * The tab of a document: its text in a text area whose changes the server hears, marking the
	 * tab, and a button saving it; an action shows what the server knows of it.
	 */
	private class DocumentTab extends Tab
	{
		private static final long serialVersionUID = 1L;

		final Document document;

		DocumentTab(Document document)
		{
			super(Model.of(document.getName()));
			this.document = document;
			setIcon(DOCUMENT_ICON);
			setClosable(true);
			// the server tells, as it hears the changes
			setModified(false);
			addAction(new TabAction(INFO_ICON, new ResourceModel("about"))
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onClick(AjaxRequestTarget target, Tab tab, Component button)
				{
					// what the server knows, above the tab, pointing at the button
					new Popover(describe(document)).setTitle(document.getName())
						.setPlacement(PopoverPlacement.TOP_CENTER)
						.setVariant(document.isModified() ? PopoverVariant.WARNING :
							PopoverVariant.DEFAULT)
						.show(target, button);
				}
			});
		}

		@Override
		protected Component newPanel(String id)
		{
			Fragment panel = new Fragment(id, "documentPanel", TabsPage.this);
			Form<Void> form = new Form<>("form");
			panel.add(form);
			TextArea<String> text = new TextArea<>("text", new PropertyModel<>(document, "text"));
			text.setLabel(Model.of(document.getName()));
			text.add(new OnChangeAjaxBehavior()
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					if (!Boolean.valueOf(document.isModified()).equals(DocumentTab.this.getModified()))
					{
						documents.setModified(DocumentTab.this, document.isModified(), target);
					}
				}
			});
			form.add(text);
			form.add(new AjaxButton("save")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSubmit(AjaxRequestTarget target)
				{
					document.save();
					documents.setModified(DocumentTab.this, false, target);
					say(target, "saved", document.getName());
				}
			}.add(new ButtonBehavior(ButtonBehavior.Variant.PRIMARY, ButtonBehavior.Size.SMALL)));
			return panel;
		}
	}

	/**
	 * A document: its name, its text and the text it was saved with.
	 */
	public static class Document implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private final String name;

		private String text;

		private String saved;

		/**
		 * @param name
		 *            the name of the document
		 * @param text
		 *            its text, saved
		 */
		public Document(String name, String text)
		{
			this.name = name;
			this.text = text;
			this.saved = text;
		}

		/**
		 * @return a document of the same name and text, for a page of its own
		 */
		Document copy()
		{
			return new Document(name, text);
		}

		public String getName()
		{
			return name;
		}

		public String getText()
		{
			return text;
		}

		/**
		 * @param text
		 *            the text as the user changed it, {@code null} for none
		 */
		public void setText(String text)
		{
			this.text = text == null ? "" : text.replace("\r\n", "\n");
		}

		/**
		 * @return whether the text has changes not saved
		 */
		public boolean isModified()
		{
			return !text.equals(saved);
		}

		/**
		 * Saves the text.
		 */
		public void save()
		{
			saved = text;
		}
	}
}
