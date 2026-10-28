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
package com.antiliacode.widgets.demo.breadcrumb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.breadcrumb.Breadcrumb;
import com.antiliacode.widgets.breadcrumb.BreadcrumbSeparator;
import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.icon.SvgIcon;
import com.antiliacode.widgets.menu.AjaxMenuItem;
import com.antiliacode.widgets.menu.BookmarkableMenuItem;
import com.antiliacode.widgets.menu.IMenuItem;
import com.antiliacode.widgets.menu.MenuItem;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * {@link Breadcrumb}: the path of a site, its crumbs leading to this page with the parameter
 * {@value #PATH}, as its pages would; a drill-down through folders via Ajax, the server adding a
 * crumb for the folder opened and going back to the one of a crumb chosen, at most
 * {@value #FOLDERS_MAX_ITEMS} crumbs shown; and a long trail whose separator and number of crumbs
 * shown a form chooses. The React and Svelte breadcrumbs show the same in those frameworks.
 */
public class BreadcrumbsPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The parameter naming the page of the site, the names of the places down to it. */
	public static final String PATH = "path";

	/** The pages of a site, under its home page. */
	static final Place SITE = Place.of("home", SvgIcon.HOUSE,
		Place.of("products", SvgIcon.STORE,
			Place.of("computers", SvgIcon.COMPUTER,
				Place.of("laptops", SvgIcon.LAPTOP),
				Place.of("desktops", SvgIcon.DESKTOP)),
			Place.of("phones", SvgIcon.MOBILE_SCREEN,
				Place.of("smartphones", SvgIcon.MOBILE),
				Place.of("phoneCases", SvgIcon.BOX))),
		Place.of("services", SvgIcon.HEADSET,
			Place.of("support", SvgIcon.SCREWDRIVER_WRENCH),
			Place.of("training", SvgIcon.GRADUATION_CAP)));

	/** The folders of a drive. */
	static final Place FILES = Place.of("files", SvgIcon.HARD_DRIVE,
		Place.of("documents", SvgIcon.FOLDER,
			Place.of("projects", SvgIcon.FOLDER,
				Place.of("widgets", SvgIcon.FOLDER,
					Place.of("src", SvgIcon.FOLDER,
						Place.of("main", SvgIcon.FOLDER,
							Place.of("java", SvgIcon.FILE_CODE)),
						Place.of("test", SvgIcon.FOLDER))),
				Place.of("website", SvgIcon.GLOBE)),
			Place.of("invoices", SvgIcon.FILE_INVOICE,
				Place.of("year2025", SvgIcon.FOLDER),
				Place.of("year2026", SvgIcon.FOLDER))),
		Place.of("pictures", SvgIcon.IMAGES,
			Place.of("holidays", SvgIcon.FOLDER,
				Place.of("beach", SvgIcon.UMBRELLA_BEACH),
				Place.of("mountains", SvgIcon.MOUNTAIN))),
		Place.of("music", SvgIcon.MUSIC));

	/** The folder the drill-down starts at, deep enough for its trail to collapse. */
	static final List<String> FIRST_FOLDER = List.of("documents", "projects", "widgets", "src");

	/** How many crumbs the trail of the drill-down shows at most. */
	static final int FOLDERS_MAX_ITEMS = 4;

	/** The trail of the settings: down to the deepest folder. */
	static final List<String> LONG_TRAIL = List.of("documents", "projects", "widgets", "src",
		"main", "java");

	/** How many crumbs the settings may show at most, 0 for all. */
	static final List<Integer> MAX_ITEMS = List.of(0, 2, 3, 4, 5);

	private final WebMarkupContainer folderView;

	private final Label settingsStatus;

	private List<String> folder = new ArrayList<>(FIRST_FOLDER);

	private BreadcrumbSeparator separator = BreadcrumbSeparator.CHEVRON;

	private Integer maxItems = 4;

	private String settingsMessage;

	/**
	 * @param parameters
	 *            the page of the site the path is at, {@value #PATH}
	 */
	public BreadcrumbsPage(PageParameters parameters)
	{
		super(parameters);
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		DemoCases cases = new DemoCases("cases", this);
		themed.add(cases);
		Fragment siteCase = cases.addCase("siteTitle", "siteCase");
		Fragment foldersCase = cases.addCase("foldersTitle", "foldersCase");
		Fragment settingsCase = cases.addCase("settingsTitle", "settingsCase");
		// a page of the site: the one the path led to
		if (!parameters.get(PATH).isEmpty())
		{
			cases.setModelObject("siteTitle");
		}

		siteCase.add(new Breadcrumb("siteTrail", this::siteCrumbs));
		siteCase.add(new Label("siteHeading", () -> label(sitePlace())));
		siteCase.add(new Label("siteBelow", () -> getString(sitePlace().children().isEmpty() ?
			"siteLeaf" : "siteBelow").replace("{0}", label(sitePlace()))));
		IModel<List<Place>> pages = () -> sitePlace().children();
		siteCase.add(new ListView<>("sitePages", pages)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Place> item)
			{
				Place place = item.getModelObject();
				List<String> path = new ArrayList<>(sitePath());
				path.add(place.name());
				BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("link",
					BreadcrumbsPage.class, parametersOf(path));
				link.add(newIcon("icon", place), new Label("label", label(place)));
				item.add(link);
			}
		});

		folderView = new WebMarkupContainer("folderView");
		folderView.setOutputMarkupId(true);
		foldersCase.add(folderView);
		folderView.add(new Breadcrumb("folderTrail", this::folderCrumbs)
			.setMaxItems(FOLDERS_MAX_ITEMS).setLabel(new ResourceModel("folders")));
		folderView.add(new Label("folderHeading", () -> label(folderPlace())));
		IModel<List<Place>> subfolders = () -> folderPlace().children();
		folderView.add(new ListView<>("subfolders", subfolders)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Place> item)
			{
				Place place = item.getModelObject();
				AjaxLink<Void> link = new AjaxLink<>("link")
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						folder.add(place.name());
						target.add(folderView);
					}
				};
				link.add(newIcon("icon", place), new Label("label", label(place)));
				item.add(link);
			}
		});
		folderView.add(new Label("emptyFolder", new ResourceModel("empty"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(folderPlace().children().isEmpty());
			}
		});

		settingsStatus = new Label("settingsStatus", () -> settingsMessage != null ?
			settingsMessage : getString("settingsStart"));
		settingsStatus.setOutputMarkupId(true);
		settingsCase.add(settingsStatus);
		WebMarkupContainer preview = new WebMarkupContainer("preview");
		preview.setOutputMarkupId(true);
		settingsCase.add(preview);
		preview.add(new Breadcrumb("settingsTrail", this::settingsCrumbs)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setSeparator(separator).setMaxItems(maxItems != null ? maxItems : 0);
			}
		});
		Form<Void> form = new Form<>("form");
		settingsCase.add(form);
		form.add(new Dropdown<>("separator", new PropertyModel<>(this, "separator"),
			Arrays.asList(BreadcrumbSeparator.values()), new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(BreadcrumbSeparator shown)
				{
					return getString("separator." + shown.name());
				}

				@Override
				public String getIdValue(BreadcrumbSeparator shown, int index)
				{
					return shown.name();
				}
			}).setRequired(true).add(rerender(preview)));
		form.add(new Dropdown<>("maxItems", new PropertyModel<>(this, "maxItems"), MAX_ITEMS,
			new IChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(Integer shown)
				{
					return shown == 0 ? getString("all") : String.valueOf(shown);
				}

				@Override
				public String getIdValue(Integer shown, int index)
				{
					return String.valueOf(shown);
				}
			}).setRequired(true).add(rerender(preview)));
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

	private static Component newIcon(String id, Place place)
	{
		return new Label(id, place.icon() != null ? place.icon().getMarkup() : "")
			.setEscapeModelStrings(false).setVisible(place.icon() != null);
	}

	/**
	 * @return the label of a place, in the language of the page
	 */
	String label(Place place)
	{
		return getString("place." + place.name());
	}

	private static PageParameters parametersOf(List<String> path)
	{
		return path.isEmpty() ? null : new PageParameters().add(PATH, String.join("/", path));
	}

	/**
	 * @return the names of the places down to the page of the site the path is at; none for the
	 *         home page, or for a path leading nowhere
	 */
	List<String> sitePath()
	{
		String path = getPageParameters().get(PATH).toString("");
		List<String> names = path.isEmpty() ? List.of() : List.of(path.split("/"));
		return SITE.placesAt(names) != null ? names : List.of();
	}

	private Place sitePlace()
	{
		List<Place> places = SITE.placesAt(sitePath());
		return places.get(places.size() - 1);
	}

	private Place folderPlace()
	{
		List<Place> places = FILES.placesAt(folder);
		return places.get(places.size() - 1);
	}

	/**
	 * @return the crumbs of the site: the pages down to the one shown, bookmarkable
	 */
	private List<IMenuItem> siteCrumbs()
	{
		List<String> path = sitePath();
		List<Place> places = SITE.placesAt(path);
		List<IMenuItem> crumbs = new ArrayList<>();
		for (int index = 0; index < places.size(); index++)
		{
			Place place = places.get(index);
			MenuItem crumb = new BookmarkableMenuItem(new ResourceModel("place." + place.name()),
				BreadcrumbsPage.class, parametersOf(path.subList(0, index)));
			crumbs.add(index == 0 ? crumb.setIcon(place.icon()) : crumb);
		}
		return crumbs;
	}

	/**
	 * @return the crumbs of the drill-down: the folders down to the one open, each going back to
	 *         its folder via Ajax
	 */
	private List<IMenuItem> folderCrumbs()
	{
		List<Place> places = FILES.placesAt(folder);
		List<IMenuItem> crumbs = new ArrayList<>();
		for (int index = 0; index < places.size(); index++)
		{
			Place place = places.get(index);
			int depth = index;
			MenuItem crumb = AjaxMenuItem.of(new ResourceModel("place." + place.name()), target -> {
				folder = new ArrayList<>(folder.subList(0, depth));
				target.add(folderView);
			});
			crumbs.add(index == 0 ? crumb.setIcon(place.icon()) : crumb);
		}
		return crumbs;
	}

	/**
	 * @return the crumbs of the settings: the long trail, each saying it was chosen
	 */
	private List<IMenuItem> settingsCrumbs()
	{
		List<IMenuItem> crumbs = new ArrayList<>();
		for (Place place : FILES.placesAt(LONG_TRAIL))
		{
			MenuItem crumb = AjaxMenuItem.of(new ResourceModel("place." + place.name()), target -> {
				settingsMessage = getString("chose").replace("{0}", label(place));
				target.add(settingsStatus);
			});
			crumbs.add(crumbs.isEmpty() ? crumb.setIcon(place.icon()) : crumb);
		}
		return crumbs;
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "breadcrumbs";
	}

	/**
	 * @return the texts, the site and the folders with their icons, and the settings as the page
	 *         has them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "cases", "siteTitle", "siteBelow", "siteLeaf",
			"foldersTitle", "folders", "empty", "settingsTitle", "separator", "maxItems", "all",
			"siteAppIntro", "foldersAppIntro", "settingsAppIntro", "settingsAppStart", "picked");
		for (BreadcrumbSeparator each : BreadcrumbSeparator.values())
		{
			texts.put("separator." + each.name(), getString("separator." + each.name()));
		}
		Set<String> names = SITE.names();
		names.addAll(FILES.names());
		for (String name : names)
		{
			texts.put("place." + name, getString("place." + name));
		}
		return new JSONObject()
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("breadcrumbTexts", new JSONObject()
				.put("label", getString("Breadcrumb.label"))
				.put("more", getString("Breadcrumb.more")))
			.put("site", SITE.toJson())
			.put("files", FILES.toJson())
			.put("folder", new JSONArray(FIRST_FOLDER))
			.put("foldersMaxItems", FOLDERS_MAX_ITEMS)
			.put("longTrail", new JSONArray(LONG_TRAIL))
			.put("separator", separator.cssName())
			.put("maxItems", maxItems)
			.put("separators", new JSONArray(Arrays.stream(BreadcrumbSeparator.values())
				.map(BreadcrumbSeparator::cssName).toList()))
			.put("maxItemsChoices", new JSONArray(MAX_ITEMS));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		Breadcrumb.renderHeadItems(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(BreadcrumbsPage.class, "BreadcrumbsPage.css")));
	}

	public BreadcrumbSeparator getSeparator()
	{
		return separator;
	}

	public void setSeparator(BreadcrumbSeparator separator)
	{
		this.separator = separator;
	}

	public Integer getMaxItems()
	{
		return maxItems;
	}

	public void setMaxItems(Integer maxItems)
	{
		this.maxItems = maxItems;
	}

	/**
	 * @return the names of the folders down to the one the drill-down has open
	 */
	public List<String> getFolder()
	{
		return List.copyOf(folder);
	}

	/**
	 * A place of a tree, a page of the site or a folder: its name, the key of its label under
	 * {@code place.}, its icon and the places under it.
	 */
	record Place(String name, IIcon icon, List<Place> children) implements Serializable
	{
		static Place of(String name, IIcon icon, Place... children)
		{
			return new Place(name, icon, List.of(children));
		}

		/**
		 * @param path
		 *            the names of the places down from this one
		 * @return this place and those of the path, down to the last; {@code null} when there is
		 *         no such place
		 */
		List<Place> placesAt(List<String> path)
		{
			List<Place> places = new ArrayList<>();
			places.add(this);
			Place current = this;
			for (String name : path)
			{
				Place next = current.children().stream().filter(child -> child.name().equals(name))
					.findFirst().orElse(null);
				if (next == null)
				{
					return null;
				}
				places.add(next);
				current = next;
			}
			return places;
		}

		/**
		 * @return the names of this place and of those under it
		 */
		Set<String> names()
		{
			Set<String> names = new LinkedHashSet<>();
			names.add(name);
			children.forEach(child -> names.addAll(child.names()));
			return names;
		}

		JSONObject toJson()
		{
			JSONObject json = new JSONObject().put("name", name);
			if (icon != null)
			{
				json.put("icon", icon.getMarkup());
			}
			if (!children.isEmpty())
			{
				json.put("children", new JSONArray(children.stream().map(Place::toJson).toList()));
			}
			return json;
		}
	}
}
