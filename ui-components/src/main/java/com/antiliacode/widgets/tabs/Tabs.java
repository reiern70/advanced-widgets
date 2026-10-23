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
package com.antiliacode.widgets.tabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.modal.Modal;
import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;
import com.github.openjson.JSONObject;

/**
 * Tabs: a list of {@link Tab}s, {@link #setOrientation(TabsOrientation) along the top of their
 * panels or down their left side}, and the panel of the tab selected. A WAI-ARIA tab list: a click
 * selects a tab, and while the focus is on one the arrow keys select the next or the previous,
 * Home and End the first and the last. The design is one of {@link TabsDesign}, in the colors of
 * the {@link com.antiliacode.widgets.theme.Theme theme} the tabs are in and with the classes of its
 * {@link com.antiliacode.widgets.theme.ILook look}: in Bootstrap's, tabs, pills and underline are its
 * navs.
 * <p>
 * A {@link Tab#setClosable(boolean) closable} tab has a button closing it; Delete closes the tab
 * that has the focus, and so does a click with the middle button. Closing a tab whose panel has
 * changes not saved asks first, in a dialog drawn as {@link Modal} draws its own, in the look of
 * the tabs: the panel has changes
 * when a field in it holds another value than the one it was rendered with, or as
 * {@link Tab#setModified(Boolean)} says, and its tab shows a mark meanwhile. A page hearing of the
 * changes by the Ajax behaviors of its fields tells the browser with
 * {@link #setModified(Tab, Boolean, AjaxRequestTarget)}. Closing the tab anyway puts its fields
 * back as they were rendered. The browser then tells the server via Ajax: the tabs take the tab
 * away and call {@link #onClosed(AjaxRequestTarget, Tab)}.
 * <p>
 * A tab may have {@link TabAction actions}, buttons after its title before the one closing it, each
 * calling back via Ajax: one telling about what the panel holds, for example.
 * <p>
 * {@code Wicket.Tabs} from {@link #JS} does all that in the browser. The panels of all tabs are
 * rendered, those not selected hidden, so that switching tabs needs no request and keeps what the
 * user typed in each. Which tab is selected is the model of the tabs, its name; by default the
 * model is kept in the page, and starts empty for the first tab. With
 * {@link #setSendToServer(boolean)} the browser tells the server each tab the user selects, the
 * model takes its name and the tabs call {@link #onSelected(AjaxRequestTarget, Tab)}. Tabs added or
 * taken away via Ajax, with {@link #addTab(Tab, AjaxRequestTarget)} and
 * {@link #removeTab(Tab, AjaxRequestTarget)}, leave the other panels as they are in the browser.
 *
 * @since 11.0.0
 */
public class Tabs extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The script of the tabs, {@code Wicket.Tabs}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Tabs.class,
		"antilia-tabs.js");

	/** The style sheet of the tabs, with the designs of {@link TabsDesign}. */
	public static final ResourceReference CSS = TabsStyleBehavior.CSS;

	private static final String EVENT_PARAMETER = "event";

	private static final String TAB_PARAMETER = "tab";

	/** The texts of the tabs, by their names in the script; their keys are Tabs.name. */
	private static final String[] TEXTS = { "close", "modified", "closeModifiedTitle",
		"closeModified", "discard", "keep" };

	private final WebMarkupContainer element;

	private final WebMarkupContainer list;

	private final WebMarkupContainer panels;

	private final RepeatingView headers;

	private final RepeatingView items;

	private final AbstractDefaultAjaxBehavior callback;

	private final List<Tab> tabs = new ArrayList<>();

	private TabsOrientation orientation = TabsOrientation.HORIZONTAL;

	private TabsDesign design = TabsDesign.TABS;

	private IModel<String> label;

	private boolean sendToServer;

	/**
	 * Tabs keeping the tab selected in the page.
	 *
	 * @param id
	 *            component id
	 */
	public Tabs(String id)
	{
		this(id, null);
	}

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            where the tabs keep the name of the tab selected, empty for the first one; in the
	 *            page when {@code null}
	 */
	public Tabs(String id, IModel<String> model)
	{
		super(id, model == null ? Model.of((String)null) : model);

		element = new WebMarkupContainer("element");
		element.setOutputMarkupId(true);
		element.add(new LookBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected LookPart[] getParts(Component component)
			{
				return new LookPart[] { LookPart.TABS, orientation.getPart(), design.getPart() };
			}
		});
		add(element);

		list = new WebMarkupContainer("list");
		list.setOutputMarkupId(true);
		list.add(AttributeModifier.replace("aria-orientation", () -> orientation.cssName()));
		list.add(AttributeModifier.replace("aria-label", () -> label != null ? label.getObject() :
			null));
		list.add(new LookBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected LookPart[] getParts(Component component)
			{
				return new LookPart[] { LookPart.TABS_LIST, design.getListPart() };
			}
		});
		element.add(list);
		headers = new RepeatingView("headers");
		list.add(headers);

		panels = new WebMarkupContainer("panels");
		panels.setOutputMarkupId(true);
		panels.add(new LookBehavior(LookPart.TABS_PANELS));
		element.add(panels);
		items = new RepeatingView("items");
		panels.add(items);

		callback = new AbstractDefaultAjaxBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled(Component component)
			{
				return isCalledBack() && super.isEnabled(component);
			}

			@Override
			protected void respond(AjaxRequestTarget target)
			{
				IRequestParameters parameters = getRequest().getRequestParameters();
				onBrowserEvent(target, parameters.getParameterValue(EVENT_PARAMETER).toOptionalString(),
					parameters.getParameterValue(TAB_PARAMETER).toOptionalString());
			}
		};
		// on the element of the tabs, which has an id even when the panel's tag is not rendered
		element.add(callback);
	}

	/**
	 * Adds a tab, after the others; its panel is created now.
	 *
	 * @param tab
	 *            the tab; given a name when it has none
	 * @return {@code this}
	 */
	public Tabs addTab(Tab tab)
	{
		Args.notNull(tab, "tab");
		if (Strings.isEmpty(tab.getName()))
		{
			tab.setName(headers.newChildId());
		}
		if (getTab(tab.getName()) != null)
		{
			throw new IllegalArgumentException("The tabs " + getPageRelativePath() +
				" have a tab named " + tab.getName() + " already");
		}
		tabs.add(tab);
		headers.add(newHeader(tab));
		WebMarkupContainer item = newItem(tab);
		item.add(tab.newPanel("panel"));
		items.add(item);
		return this;
	}

	/**
	 * Adds a tab via Ajax, after the others, rendering only it: the panels of the others stay as
	 * they are in the browser.
	 *
	 * @param tab
	 *            the tab; given a name when it has none
	 * @param target
	 *            the Ajax request target
	 * @return {@code this}
	 */
	public Tabs addTab(Tab tab, AjaxRequestTarget target)
	{
		addTab(tab);
		Component header = headers.get(tab.getName());
		Component item = items.get(tab.getName());
		// elements for Wicket to replace by those of the tab and its panel
		target.prependJavaScript("(function(){" +
			placeholder(list.getMarkupId(), header.getMarkupId()) +
			placeholder(panels.getMarkupId(), item.getMarkupId()) + "})();");
		target.add(header, item);
		return this;
	}

	private static String placeholder(String parent, String id)
	{
		return "var e=document.createElement('div');e.id=" + JSONObject.quote(id) +
			";document.getElementById(" + JSONObject.quote(parent) + ").appendChild(e);";
	}

	/**
	 * Takes a tab away; the next one is selected when it was, or the previous one.
	 *
	 * @param tab
	 *            the tab
	 * @return {@code this}
	 */
	public Tabs removeTab(Tab tab)
	{
		int index = tabs.indexOf(tab);
		if (index < 0)
		{
			return this;
		}
		if (tab == getSelectedTab())
		{
			Tab next = index + 1 < tabs.size() ? tabs.get(index + 1) :
				index > 0 ? tabs.get(index - 1) : null;
			setModelObject(next != null ? next.getName() : null);
		}
		tabs.remove(index);
		headers.remove(tab.getName());
		items.remove(tab.getName());
		return this;
	}

	/**
	 * Takes a tab away via Ajax, without rendering the others again.
	 *
	 * @param tab
	 *            the tab
	 * @param target
	 *            the Ajax request target
	 * @return {@code this}
	 */
	public Tabs removeTab(Tab tab, AjaxRequestTarget target)
	{
		if (!tabs.contains(tab))
		{
			return this;
		}
		String header = headers.get(tab.getName()).getMarkupId();
		String item = items.get(tab.getName()).getMarkupId();
		removeTab(tab);
		target.appendJavaScript("(function(){var r=function(i){var e=document.getElementById(i);" +
			"if(e){e.remove();}};r(" + JSONObject.quote(header) + ");r(" + JSONObject.quote(item) +
			");})();");
		Tab selected = getSelectedTab();
		if (selected != null)
		{
			target.appendJavaScript(script("select(" + JSONObject.quote(selected.getName()) + ")"));
		}
		return this;
	}

	/**
	 * @return the tabs, in order
	 */
	public List<Tab> getTabs()
	{
		return Collections.unmodifiableList(tabs);
	}

	/**
	 * @param name
	 *            the name of a tab
	 * @return the tab of the name, {@code null} when there is none
	 */
	public Tab getTab(String name)
	{
		return tabs.stream().filter(tab -> tab.getName().equals(name)).findFirst().orElse(null);
	}

	/**
	 * @return the model keeping the name of the tab selected
	 */
	@SuppressWarnings("unchecked")
	public IModel<String> getModel()
	{
		return (IModel<String>)getDefaultModel();
	}

	/**
	 * @return the name of the tab selected the model has, {@code null} for the first one
	 */
	public String getModelObject()
	{
		return getModel().getObject();
	}

	/**
	 * @param name
	 *            the name of the tab selected, {@code null} for the first one
	 * @return {@code this}
	 */
	public Tabs setModelObject(String name)
	{
		setDefaultModelObject(name);
		return this;
	}

	/**
	 * @return the tab selected: the one the model names, or the first one; {@code null} when there
	 *         are no tabs
	 */
	public Tab getSelectedTab()
	{
		Tab named = getTab(getModelObject());
		return named != null ? named : tabs.isEmpty() ? null : tabs.get(0);
	}

	/**
	 * Selects a tab, the next time the tabs are rendered.
	 *
	 * @param tab
	 *            the tab
	 * @return {@code this}
	 */
	public Tabs select(Tab tab)
	{
		return setModelObject(tab != null ? tab.getName() : null);
	}

	/**
	 * Selects a tab via Ajax, without rendering the tabs again.
	 *
	 * @param tab
	 *            the tab
	 * @param target
	 *            the Ajax request target
	 * @return {@code this}
	 */
	public Tabs select(Tab tab, AjaxRequestTarget target)
	{
		select(tab);
		if (tab != null)
		{
			target.appendJavaScript(script("select(" + JSONObject.quote(tab.getName()) + ")"));
		}
		return this;
	}

	/**
	 * Says whether the panel of a tab has changes not saved, telling the browser via Ajax: its tab
	 * shows the mark of changes, and the user is asked before it closes.
	 *
	 * @param tab
	 *            the tab
	 * @param modified
	 *            whether the panel has changes not saved; {@code null} for the browser to tell by
	 *            its fields
	 * @param target
	 *            the Ajax request target
	 * @return {@code this}
	 */
	public Tabs setModified(Tab tab, Boolean modified, AjaxRequestTarget target)
	{
		tab.setModified(modified);
		target.appendJavaScript(script("setModified(" + JSONObject.quote(tab.getName()) + "," +
			modified + ")"));
		return this;
	}

	/**
	 * @return a script calling the tabs in the browser, when they are there
	 */
	private String script(String call)
	{
		return "(function(){var t=Wicket.Tabs&&Wicket.Tabs.of(" +
			JSONObject.quote(element.getMarkupId()) + ");if(t){t." + call + ";}})();";
	}

	/**
	 * Takes what the browser sent: the tab the user selected or closed.
	 */
	private void onBrowserEvent(AjaxRequestTarget target, String event, String name)
	{
		Tab tab = getTab(name);
		if (tab == null)
		{
			return;
		}
		if ("select".equals(event) && sendToServer)
		{
			if (!name.equals(getModelObject()))
			{
				setModelObject(name);
				onSelected(target, tab);
			}
		}
		else if ("close".equals(event) && tab.isClosable())
		{
			removeTab(tab);
			onClosed(target, tab);
		}
	}

	/**
	 * Called when the user has selected a tab and {@link #isSendToServer()} is on, with
	 * {@link #getSelectedTab()} the tab. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param tab
	 *            the tab selected
	 */
	protected void onSelected(AjaxRequestTarget target, Tab tab)
	{
	}

	/**
	 * Called when the user has closed a tab, which the tabs have taken away already: gone from the
	 * browser, it is not rendered again. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param tab
	 *            the tab closed
	 */
	protected void onClosed(AjaxRequestTarget target, Tab tab)
	{
	}

	/**
	 * @return where the tabs are, {@link TabsOrientation#HORIZONTAL} by default
	 */
	public TabsOrientation getOrientation()
	{
		return orientation;
	}

	/**
	 * @param orientation
	 *            where the tabs are: along the top of the panels or down their left side
	 * @return {@code this}
	 */
	public Tabs setOrientation(TabsOrientation orientation)
	{
		this.orientation = orientation == null ? TabsOrientation.HORIZONTAL : orientation;
		return this;
	}

	/**
	 * @return the design of the tabs, {@link TabsDesign#TABS} by default
	 */
	public TabsDesign getDesign()
	{
		return design;
	}

	/**
	 * @param design
	 *            the design of the tabs
	 * @return {@code this}
	 */
	public Tabs setDesign(TabsDesign design)
	{
		this.design = design == null ? TabsDesign.TABS : design;
		return this;
	}

	/**
	 * @param label
	 *            the accessible name of the list of tabs, {@code null} for none
	 * @return {@code this}
	 */
	public Tabs setLabel(IModel<String> label)
	{
		this.label = assigned(label, this);
		return this;
	}

	/**
	 * @return the model as a component uses it: one taking the component, such as a
	 *         ResourceModel, given it
	 */
	private static IModel<String> assigned(IModel<String> model, Component component)
	{
		return model instanceof IComponentAssignedModel<String> assigned ?
			assigned.wrapOnAssignment(component) : model;
	}

	/**
	 * @return whether the browser tells the server each tab the user selects, {@code false} by
	 *         default
	 */
	public boolean isSendToServer()
	{
		return sendToServer;
	}

	/**
	 * Whether the browser tells the server via Ajax each tab the user selects, which the model
	 * takes. The tabs the user closes it tells always.
	 *
	 * @param sendToServer
	 *            whether the browser tells the server each tab the user selects
	 * @return {@code this}
	 */
	public Tabs setSendToServer(boolean sendToServer)
	{
		this.sendToServer = sendToServer;
		return this;
	}

	/**
	 * @return the markup id of the element of the tabs, which the script works on
	 */
	public String getTabsMarkupId()
	{
		return element.getMarkupId();
	}

	/**
	 * @return whether the browser calls the server back: to tell the tab the user selects, or one
	 *         they close
	 */
	private boolean isCalledBack()
	{
		return sendToServer || tabs.stream().anyMatch(Tab::isClosable);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderHeadItems(response);
		StringBuilder script = new StringBuilder("Wicket.Tabs.attach(")
			.append(JSONObject.quote(element.getMarkupId())).append(", ").append(getOptions());
		if (isCalledBack())
		{
			script.append(", ").append(sendToServer ? callbackFor("select") : "null")
				.append(", ").append(callbackFor("close"));
		}
		response.render(OnDomReadyHeaderItem.forScript(script.append(");").toString()));
	}

	private CharSequence callbackFor(String event)
	{
		return callback.getCallbackFunction(
			CallbackParameter.resolved(EVENT_PARAMETER, JSONObject.quote(event)),
			CallbackParameter.explicit(TAB_PARAMETER));
	}

	/**
	 * @return the options of {@code Wicket.Tabs.attach}, as a JavaScript object
	 */
	private String getOptions()
	{
		JSONObject texts = new JSONObject();
		for (String name : TEXTS)
		{
			String text = getString("Tabs." + name, null, "");
			if (!Strings.isEmpty(text))
			{
				texts.put(name, text);
			}
		}
		JSONObject options = new JSONObject()
			.put("orientation", orientation.cssName())
			.put("design", design.cssName())
			.put("texts", texts);
		Tab selected = getSelectedTab();
		if (selected != null)
		{
			options.put("selected", selected.getName());
		}
		String text = label != null ? label.getObject() : null;
		if (!Strings.isEmpty(text))
		{
			options.put("label", text);
		}
		return options.toString();
	}

	/**
	 * @return the tab of a tab: its button, with the icon and the title, and its actions
	 */
	private Component newHeader(Tab tab)
	{
		Fragment header = new Fragment(tab.getName(), "header", this)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				Looks.addClasses(tag, Looks.of(this), LookPart.TAB);
				tag.put("role", "presentation");
				tag.put("data-tab", tab.getName());
				if (tab.isClosable())
				{
					tag.put("data-closable", "true");
				}
				if (tab == getSelectedTab())
				{
					tag.put("data-selected", "");
				}
			}
		};
		header.setOutputMarkupId(true);

		WebMarkupContainer button = new WebMarkupContainer("button")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				boolean selected = tab == getSelectedTab();
				tag.put("aria-selected", String.valueOf(selected));
				tag.put("tabindex", selected ? "0" : "-1");
				tag.put("aria-controls", items.get(tab.getName()).getMarkupId());
			}
		};
		button.setOutputMarkupId(true);
		button.add(new LookBehavior(LookPart.TAB_BUTTON));
		header.add(button);
		button.add(new Label("icon", () -> tab.getIcon() != null ? tab.getIcon().getMarkup() : null)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(tab.getIcon() != null);
			}
		}.setEscapeModelStrings(false).add(new LookBehavior(LookPart.TAB_ICON)));
		button.add(new Label("title", tab.getTitle()).add(new LookBehavior(LookPart.TAB_TITLE)));

		WebMarkupContainer actions = new WebMarkupContainer("actions")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!tab.getActions().isEmpty());
			}
		};
		actions.add(new LookBehavior(LookPart.TAB_ACTIONS));
		header.add(actions);
		RepeatingView buttons = new RepeatingView("action");
		actions.add(buttons);
		for (TabAction action : tab.getActions())
		{
			buttons.add(newAction(buttons.newChildId(), tab, action));
		}
		return header;
	}

	/**
	 * @return the button of an action of a tab, calling it back via Ajax
	 */
	private Component newAction(String id, Tab tab, TabAction action)
	{
		Label button = new Label(id, () -> action.getIcon().getMarkup())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				tag.put("tabindex", tab == getSelectedTab() ? "0" : "-1");
			}
		};
		button.setEscapeModelStrings(false);
		button.setOutputMarkupId(true);
		IModel<String> title = assigned(action.getTitle(), button);
		button.add(AttributeModifier.replace("title", title));
		button.add(AttributeModifier.replace("aria-label", title));
		button.add(new AjaxEventBehavior("click")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				action.onClick(target, tab, getComponent());
			}
		});
		return button;
	}

	/**
	 * @return the element of the panel of a tab, hidden unless the tab is selected
	 */
	private WebMarkupContainer newItem(Tab tab)
	{
		WebMarkupContainer item = new WebMarkupContainer(tab.getName())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				Looks.addClasses(tag, Looks.of(this), LookPart.TABS_PANEL);
				tag.put("role", "tabpanel");
				tag.put("data-tab", tab.getName());
				tag.put("aria-labelledby", headers.get(tab.getName() + ":button").getMarkupId());
				if (tab != getSelectedTab())
				{
					tag.put("hidden", "hidden");
				}
				if (tab.getModified() != null)
				{
					tag.put("data-modified", tab.getModified().toString());
				}
			}
		};
		item.setOutputMarkupId(true);
		return item;
	}

	@Override
	protected void onDetach()
	{
		tabs.forEach(Tab::detach);
		if (label != null)
		{
			label.detach();
		}
		super.onDetach();
	}

	/**
	 * Renders what the tabs need in the browser: {@link #JS} and {@link #CSS}, with the style
	 * sheets of the dialog asking before a tab with changes closes, {@link Modal#CSS} and
	 * {@link ButtonBehavior#CSS}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(ButtonBehavior.CSS));
		response.render(CssHeaderItem.forReference(Modal.CSS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
