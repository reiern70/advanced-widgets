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
package com.antiliacode.widgets.breadcrumb;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.menu.AjaxMenuItem;
import com.antiliacode.widgets.menu.BookmarkableMenuItem;
import com.antiliacode.widgets.menu.IMenuItem;
import com.antiliacode.widgets.menu.LinkMenuItem;
import com.antiliacode.widgets.menu.SubMenu;
import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;
import com.github.openjson.JSONObject;

/**
 * A breadcrumb: where the page is in the hierarchy of the application, a row of crumbs from the
 * home page down to the page shown. The crumbs are the {@link IMenuItem items} of the menus: a
 * {@link BookmarkableMenuItem} leads to a page, an {@link AjaxMenuItem} calls the server via Ajax
 * and a {@link LinkMenuItem} with a request of its own; an item without a link, such as a
 * {@link SubMenu}, is a crumb leading nowhere, its label alone. Their icons and tooltips show,
 * the items that do not {@link IMenuItem#isVisible() show} are left out and those the user may
 * not {@link IMenuItem#isEnabled() choose} are dimmed. The last crumb is the page shown: its
 * label, not a link, marked {@code aria-current="page"}.
 * <p>
 * The WAI-ARIA breadcrumb pattern: a {@code nav} named "Breadcrumb", in the language of the page
 * or as {@link #setLabel(IModel) given}, around an ordered list. The style sheet draws the
 * {@link #setSeparator(BreadcrumbSeparator) separators} between the crumbs, which screen readers
 * do not read. The breadcrumb is a {@code nav} of its own: put it in a {@code div}, not in another
 * {@code nav}.
 * <p>
 * A long trail collapses: with {@link #setMaxItems(int)}, a trail of more crumbs shows its first
 * crumb and its last ones, and those between them hide behind a button, "…", which
 * {@code Wicket.Breadcrumb} of {@link #JS} makes show them in place, without calling the server.
 * The server renders them hidden already, so that nothing moves while the page loads. On a narrow
 * screen the trail wraps onto more lines.
 * <p>
 * In the colors of the {@link com.antiliacode.widgets.theme.Theme theme} it is in and with the
 * classes of its {@link com.antiliacode.widgets.theme.ILook look}: in Bootstrap's, the list is a
 * breadcrumb, the last crumb the active one. The crumbs are those of the model, read each time the
 * breadcrumb renders:
 *
 * <pre>
 * add(new Breadcrumb("breadcrumb", List.of(
 * 	new BookmarkableMenuItem(Model.of("Home"), HomePage.class).setIcon(SvgIcon.HOUSE),
 * 	new BookmarkableMenuItem(Model.of("Products"), ProductsPage.class),
 * 	new BookmarkableMenuItem(Model.of("Laptops"), ProductsPage.class,
 * 		new PageParameters().add("category", "laptops")))).setMaxItems(4));
 * </pre>
 *
 * @since 11.0.0
 */
public class Breadcrumb extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The script of the breadcrumbs, {@code Wicket.Breadcrumb}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Breadcrumb.class,
		"antilia-breadcrumb.js");

	/** The style sheet of the breadcrumbs, with the separators of {@link BreadcrumbSeparator}. */
	public static final ResourceReference CSS = new CssResourceReference(Breadcrumb.class,
		"antilia-breadcrumb.css");

	/** The attribute marking the crumbs a collapsed trail hides, by which the script finds them. */
	public static final String COLLAPSED_ATTRIBUTE = "data-antilia-collapsed";

	/** Stands for the button "…" among the places of the crumbs. */
	private static final int MORE = -1;

	private final WebMarkupContainer element;

	private BreadcrumbSeparator separator = BreadcrumbSeparator.CHEVRON;

	private int maxItems;

	private IModel<String> label;

	/** The crumbs shown, those of the items that show, as the breadcrumb renders. */
	private transient List<IMenuItem> shown;

	/**
	 * @param id
	 *            component id
	 * @param items
	 *            the crumbs, from the home page down to the page shown, copied
	 */
	public Breadcrumb(String id, List<? extends IMenuItem> items)
	{
		this(id, new ListModel<IMenuItem>(new ArrayList<>(items)));
	}

	/**
	 * @param id
	 *            component id
	 * @param items
	 *            the crumbs, from the home page down to the page shown, read each time the
	 *            breadcrumb renders
	 */
	public Breadcrumb(String id, IModel<? extends List<? extends IMenuItem>> items)
	{
		super(id, items);

		element = new WebMarkupContainer("element");
		element.setOutputMarkupId(true);
		element.add(new LookBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected LookPart[] getParts(Component component)
			{
				return new LookPart[] { LookPart.BREADCRUMB, separator.getPart() };
			}
		});
		element.add(AttributeModifier.replace("aria-label", this::getLabelText));
		add(element);

		WebMarkupContainer list = new WebMarkupContainer("list");
		list.add(new LookBehavior(LookPart.BREADCRUMB_LIST));
		element.add(list);
		IModel<List<Integer>> places = this::places;
		list.add(new ListView<>("crumbs", places)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Integer> listItem)
			{
				populate(listItem, listItem.getModelObject());
			}
		});
	}

	/**
	 * @return the places of the crumbs shown, among the items that show, and {@link #MORE} before
	 *         the first crumb a collapsed trail hides
	 */
	private List<Integer> places()
	{
		int count = getShownItems().size();
		int[] range = collapsed(count, maxItems);
		List<Integer> places = new ArrayList<>();
		for (int index = 0; index < count; index++)
		{
			if (range != null && index == range[0])
			{
				places.add(MORE);
			}
			places.add(index);
		}
		return places;
	}

	/**
	 * Fills the element of a crumb, or of the button "…".
	 */
	private void populate(ListItem<Integer> listItem, int place)
	{
		List<IMenuItem> items = getShownItems();
		int[] range = collapsed(items.size(), maxItems);
		if (place == MORE)
		{
			listItem.add(new LookBehavior(LookPart.BREADCRUMB_ITEM, LookPart.BREADCRUMB_MORE));
			listItem.add(newMore("crumb", range[1] - range[0]));
			return;
		}
		IMenuItem item = items.get(place);
		boolean current = place == items.size() - 1;
		listItem.add(new LookBehavior(LookPart.BREADCRUMB_ITEM,
			current ? LookPart.BREADCRUMB_CURRENT : null));
		if (range != null && place >= range[0] && place < range[1])
		{
			listItem.add(AttributeModifier.replace("hidden", "hidden"));
			listItem.add(AttributeModifier.replace(COLLAPSED_ATTRIBUTE, ""));
		}
		Component link = current ? null : item.getLink("link");
		listItem.add(link != null ? newLink("crumb", item, link) : newText("crumb", item, current));
	}

	/**
	 * @return the link of a crumb, with its icon and its label in it and its tooltip as its title
	 */
	private Component newLink(String id, IMenuItem item, Component link)
	{
		if (!(link instanceof MarkupContainer container))
		{
			throw new IllegalArgumentException("The link of the item " + item +
				" must be a container, to hold its icon and its label");
		}
		Fragment fragment = new Fragment(id, "link", this);
		fragment.add(link);
		container.add(newIcon("icon", item));
		container.add(newLabel("label", item));
		link.add(new LookBehavior(LookPart.BREADCRUMB_LINK));
		link.add(new Behavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onConfigure(Component component)
			{
				component.setEnabled(item.isEnabled());
			}

			@Override
			public void onComponentTag(Component component, ComponentTag tag)
			{
				if (!component.isEnabledInHierarchy())
				{
					tag.put("aria-disabled", "true");
				}
			}
		});
		addTooltip(link, item);
		return fragment;
	}

	/**
	 * @return the label of a crumb leading nowhere, or of the page shown, with its icon
	 */
	private Component newText(String id, IMenuItem item, boolean current)
	{
		Fragment fragment = new Fragment(id, "text", this);
		WebMarkupContainer text = new WebMarkupContainer("text");
		text.add(new LookBehavior(current ? LookPart.BREADCRUMB_PAGE : LookPart.BREADCRUMB_TEXT));
		if (current)
		{
			text.add(AttributeModifier.replace("aria-current", "page"));
		}
		text.add(newIcon("icon", item));
		text.add(newLabel("label", item));
		addTooltip(text, item);
		fragment.add(text);
		return fragment;
	}

	/**
	 * @return the button showing the crumbs hidden
	 */
	private Component newMore(String id, int hidden)
	{
		Fragment fragment = new Fragment(id, "more", this);
		WebMarkupContainer button = new WebMarkupContainer("button");
		button.add(new LookBehavior(LookPart.BREADCRUMB_MORE_BUTTON));
		IModel<String> text = () -> getMoreText(hidden);
		button.add(AttributeModifier.replace("aria-label", text));
		button.add(AttributeModifier.replace("title", text));
		fragment.add(button);
		return fragment;
	}

	private static void addTooltip(Component component, IMenuItem item)
	{
		IModel<String> tooltip = item.getTooltip();
		if (tooltip != null)
		{
			component.add(AttributeModifier.replace("title", assigned(tooltip, component)));
		}
	}

	/**
	 * @return the icon of an item, its markup as is, read each time it renders; hidden when the
	 *         item has none
	 */
	private static Component newIcon(String id, IMenuItem item)
	{
		return new Label(id, () -> {
			IIcon icon = iconOf(item);
			return icon != null ? icon.getMarkup() : null;
		})
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(iconOf(item) != null);
			}
		}.setEscapeModelStrings(false).add(new LookBehavior(LookPart.BREADCRUMB_ICON));
	}

	private static IIcon iconOf(IMenuItem item)
	{
		IModel<IIcon> icon = item.getIcon();
		return icon != null ? icon.getObject() : null;
	}

	private static Component newLabel(String id, IMenuItem item)
	{
		return new Label(id, item.getLabel()).add(new LookBehavior(LookPart.BREADCRUMB_LABEL));
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
	 * The crumbs a trail hides, as the script of the browser has them: those after the first, up
	 * to the last {@code maxItems - 1}.
	 *
	 * @param count
	 *            how many crumbs the trail has
	 * @param maxItems
	 *            how many show at most, 2 at least; every crumb shows for 0
	 * @return the place of the first crumb hidden and that of the crumb after the last one,
	 *         {@code null} when every crumb shows
	 */
	static int[] collapsed(int count, int maxItems)
	{
		if (maxItems <= 0)
		{
			return null;
		}
		int shown = Math.max(2, maxItems);
		return count > shown ? new int[] { 1, count - (shown - 1) } : null;
	}

	/**
	 * @return the items of the model, the crumbs
	 */
	@SuppressWarnings("unchecked")
	public List<? extends IMenuItem> getItems()
	{
		List<? extends IMenuItem> items = (List<? extends IMenuItem>)getDefaultModelObject();
		return items != null ? items : List.of();
	}

	/**
	 * @return the crumbs shown: the items that show, read once as the breadcrumb renders
	 */
	public List<IMenuItem> getShownItems()
	{
		if (shown == null)
		{
			shown = getItems().stream().filter(IMenuItem::isVisible).map(IMenuItem.class::cast)
				.toList();
		}
		return shown;
	}

	/**
	 * @return the separator drawn between the crumbs, {@link BreadcrumbSeparator#CHEVRON} by
	 *         default
	 */
	public BreadcrumbSeparator getSeparator()
	{
		return separator;
	}

	/**
	 * @param separator
	 *            the separator drawn between the crumbs
	 * @return {@code this}
	 */
	public Breadcrumb setSeparator(BreadcrumbSeparator separator)
	{
		this.separator = separator == null ? BreadcrumbSeparator.CHEVRON : separator;
		return this;
	}

	/**
	 * @return how many crumbs show at most, {@code 0}, every one, by default
	 */
	public int getMaxItems()
	{
		return maxItems;
	}

	/**
	 * How many crumbs show at most: a longer trail shows its first crumb and its last
	 * {@code maxItems - 1}, those between them behind a button, "…", that shows them. Every crumb
	 * shows for {@code 0}, the default; 2 show at least otherwise, the first and the page shown.
	 *
	 * @param maxItems
	 *            how many crumbs show at most, {@code 0} for every one
	 * @return {@code this}
	 */
	public Breadcrumb setMaxItems(int maxItems)
	{
		this.maxItems = Math.max(0, maxItems);
		return this;
	}

	/**
	 * @param label
	 *            the accessible name of the breadcrumb, {@code null} for "Breadcrumb" in the
	 *            language of the page
	 * @return {@code this}
	 */
	public Breadcrumb setLabel(IModel<String> label)
	{
		this.label = label == null ? null : assigned(label, this);
		return this;
	}

	/**
	 * @return the accessible name of the breadcrumb
	 */
	public String getLabelText()
	{
		String text = label != null ? label.getObject() : null;
		return text != null ? text : getString("Breadcrumb.label");
	}

	/**
	 * @param hidden
	 *            how many crumbs the trail hides
	 * @return the label of the button showing them
	 */
	private String getMoreText(int hidden)
	{
		return getString("Breadcrumb.more").replace("{0}", String.valueOf(hidden));
	}

	/**
	 * @return the markup id of the element of the breadcrumb, its {@code nav}, which the script
	 *         works on
	 */
	public String getBreadcrumbMarkupId()
	{
		return element.getMarkupId();
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		shown = null;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderHeadItems(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Breadcrumb.attach(" +
			ScriptJson.quote(element.getMarkupId()) + ", " + getOptions() + ");"));
	}

	/**
	 * @return the options of {@code Wicket.Breadcrumb.attach}, as a JavaScript object
	 */
	private String getOptions()
	{
		return ScriptJson.of(new JSONObject()
			.put("separator", separator.cssName())
			.put("maxItems", maxItems)
			.put("texts", new JSONObject()
				.put("label", getLabelText())
				.put("more", getString("Breadcrumb.more"))));
	}

	@Override
	protected void onDetach()
	{
		shown = null;
		if (label != null)
		{
			label.detach();
		}
		// the items of a model already detached would load again
		if (!(getDefaultModel() instanceof LoadableDetachableModel<?> loadable) ||
			loadable.isAttached())
		{
			getItems().forEach(IMenuItem::detach);
		}
		super.onDetach();
	}

	/**
	 * Renders what the breadcrumbs need in the browser: {@link #JS} and {@link #CSS}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
