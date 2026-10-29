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
package com.antiliacode.widgets.dynamicdatatable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxUtils;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes.Method;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.modal.TrapFocusBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.resource.AlreadyCompressedJavaScriptResourceReference;
import com.antiliacode.widgets.scrollbar.ScrollbarBehavior;
import com.antiliacode.widgets.table.IMovableColumn;
import com.antiliacode.widgets.table.IMovableColumnsTable;
import com.antiliacode.widgets.table.IResizableColumnsTable;
import com.antiliacode.widgets.table.MovableColumnsBehavior;
import com.antiliacode.widgets.table.ResizableColumnsBehavior;
import com.antiliacode.widgets.template.Templates;
import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookContainer;
import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;
import com.antiliacode.widgets.veil.LocalVeilBehavior;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The common part of the dynamic data tables: a table with toolbars, paging, sorting, selection,
 * an overlay, a body of a fixed height, and columns the user can resize, move, show and hide.
 * How the rows are rendered is up to the subclass: {@link JsonBasedDynamicDataTable} renders them
 * in the browser from JSON, {@link BeanBasedDynamicDataTable} on the server, with Wicket
 * components. Both look and behave the same.
 * <p>
 * The rows come from an {@link IDynamicDataProvider}, which knows the key of every row. The key
 * ties a row in the browser to a row on the server: an element in a row with a
 * {@code data-dt-action} attribute sends the action and the row's key to the table, which resolves
 * the row through {@link IDynamicDataProvider#findByKey(Serializable)} and hands it to the
 * {@link IAjaxActionColumnContributor} registered for the action. A click on a row that has
 * changed or gone since it was rendered therefore still reaches the table, rather than a component
 * that no longer exists, and ends up in
 * {@link #onNotFoundAction(AjaxRequestTarget, String, Serializable, Object)}.
 * <p>
 * The table is paged like a {@link DataTable}: a {@link DynamicNavigationToolbar navigation}, in
 * the footer, above the headers (the default) or both ({@link #setNavigationPosition(ToolbarPosition)}),
 * switches pages via Ajax.
 * <p>
 * Above and below the rows, the table renders toolbars: its navigation, its
 * {@link DynamicSelectionToolbar selection bar}, and toolbars of the application's own, added
 * with {@link #addTopToolbar(AbstractDynamicToolbar)} and
 * {@link #addBottomToolbar(AbstractDynamicToolbar)}. The built-in toolbars come from factory
 * methods an application can override. {@link IDynamicToolbarAction Actions} on the whole table,
 * such as a {@link CsvExportToolbarAction}, are shown in the navigation.
 * <p>
 * With an {@link ISortableDynamicDataProvider}, the header of every column with a
 * {@link #getSortProperty(IDetachable) sort property} is a link sorting the rows by the column,
 * followed by an up and a down arrow, the one of the current order highlighted. Sorting has the
 * provider query the rows again, shows the first page, repaints all its rows and clears the
 * selection.
 * <p>
 * With a {@link ResizableColumnsBehavior} added, the user resizes the columns by dragging the
 * right edge of their header. With a {@link MovableColumnsBehavior} added, the user drags a column
 * marked {@link IMovableColumn} by the handle in its header to another place, a marker showing
 * where it will land, or moves it with the arrow keys on that handle; the other columns keep
 * their places. Columns marked {@link IHideableColumn} can be hidden by the user with a
 * {@link ColumnChooserToolbarAction}; the application can hide any column with
 * {@link #setColumnVisible(IDetachable, boolean)}. The table keeps the order, the visibility and
 * the widths of its columns, re-renders itself when they change and saves them in its
 * {@link #setColumnStateStore(IColumnStateStore) column state store}, if it has one, for example
 * {@link SessionColumnStateStore}, so a table built again, for example after reloading the page,
 * shows its columns as the user left them.
 * <p>
 * With a selection column, the user selects rows with checkboxes. The selection is kept on the
 * server, by key, and survives paging and refreshing, unless the table
 * {@link #setClearSelectionOnNavigation(boolean) clears it on navigation}; see
 * {@link #getSelection()}. Once every row of a page is selected, a bar below the top navigation,
 * above the bottom one or both ({@link #setSelectionToolbarPosition(ToolbarPosition)}) offers to
 * select every row of the provider; checking or unchecking a row afterwards turns the selection
 * back into the rows the page shows checked.
 * <p>
 * The table carries the CSS class {@value #CSS_CLASS}, and takes its colors from the
 * {@link com.antiliacode.widgets.theme.Theme theme} it is rendered in, if any.
 * <p>
 * A column implementing {@link IHeaderContributor} renders its header items, for example a style
 * sheet, along with the table.
 * <p>
 * The table must be attached to a {@code <table>} tag.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys of the rows
 * @param <C>
 *            the type of the columns
 * @since 11.0.0
 */
public abstract class BaseDynamicDataTable<T, K extends Serializable, C extends IDetachable>
	extends
		Panel
	implements
		IPageableItems,
		IResizableColumnsTable,
		IMovableColumnsTable
{
	private static final long serialVersionUID = 1L;

	private static final ResourceReference JAVASCRIPT =
		new AlreadyCompressedJavaScriptResourceReference(BaseDynamicDataTable.class,
			"ant-data-table.js", Templates.JS);

	private static final String ACTION_PARAMETER = "action";

	private static final String KEY_PARAMETER = "key";

	private static final ResourceReference CSS = new CssResourceReference(
		BaseDynamicDataTable.class, "ant-data-table.css");

	private static final Map<SortOrder, LookPart> SORT_PARTS = new EnumMap<>(
		Map.of(SortOrder.ASCENDING, LookPart.TABLE_HEADER_ASCENDING, SortOrder.DESCENDING,
			LookPart.TABLE_HEADER_DESCENDING, SortOrder.NONE, LookPart.TABLE_HEADER_UNSORTED));

	private static final Map<SortOrder, String> ARIA_SORT = new EnumMap<>(Map.of(
		SortOrder.ASCENDING, "ascending", SortOrder.DESCENDING, "descending", SortOrder.NONE,
		"none"));

	/**
	 * The prefix of the actions the table handles itself; an action contributor must not use it.
	 */
	public static final String RESERVED_ACTION_PREFIX = "_antilia";

	/**
	 * The action selecting the row of the key sent with it.
	 */
	public static final String SELECT_ACTION = RESERVED_ACTION_PREFIX + "-select";

	/**
	 * The action deselecting the row of the key sent with it.
	 */
	public static final String DESELECT_ACTION = RESERVED_ACTION_PREFIX + "-deselect";

	/**
	 * The action selecting every row of the current page.
	 */
	public static final String SELECT_PAGE_ACTION = RESERVED_ACTION_PREFIX + "-select-page";

	/**
	 * The action deselecting every row of the current page.
	 */
	public static final String DESELECT_PAGE_ACTION = RESERVED_ACTION_PREFIX + "-deselect-page";

	/**
	 * The action the browser sends when the user presses Escape in the table's overlay.
	 */
	public static final String CLOSE_OVERLAY_ACTION = RESERVED_ACTION_PREFIX + "-close-overlay";

	/**
	 * The action a {@link ColumnChooserToolbarAction} sends the columns to show with: their
	 * indexes in {@link #getColumns()}, separated by commas.
	 */
	public static final String SHOW_COLUMNS_ACTION = RESERVED_ACTION_PREFIX + "-show-columns";

	/**
	 * The id a component shown with {@link #showOverlay(Component, IPartialPageRequestHandler)}
	 * has to carry.
	 */
	public static final String OVERLAY_CONTENT_ID = "content";

	/** The CSS class of the table. */
	public static final String CSS_CLASS = "ant-data-table";

	/** The id of the component holding the rows, see {@link #newRowsBody(String)}. */
	public static final String ROWS_BODY_ID = "body";

	private static final int FETCH_CHUNK = 100;

	private static final double MAX_COLUMN_WIDTH = 100_000;

	/**
	 * Where a {@link BaseDynamicDataTable} shows a toolbar.
	 */
	public enum ToolbarPosition
	{
		/** Above the headers. */
		TOP,

		/** In the footer. */
		BOTTOM,

		/** Above the headers and in the footer. */
		BOTH
	}

	/**
	 * Where a {@link BaseDynamicDataTable} shows its overlay, see
	 * {@link BaseDynamicDataTable#showOverlay(Component, IPartialPageRequestHandler)}.
	 */
	public enum OverlayPlacement
	{
		/** Over the table: the veil covers the table only and the window is shown in it. */
		LOCAL,

		/**
		 * Over the whole page: the veil covers the browser window and the window is shown in its
		 * middle, whatever part of the table is in view. The overlay is fixed to the browser
		 * window, unless an element around the table has a {@code transform}, {@code filter} or
		 * {@code contain} style, which CSS makes it relative to instead.
		 */
		BODY
	}

	/**
	 * The unit of the height of the body of a {@link BaseDynamicDataTable}, see
	 * {@link BaseDynamicDataTable#setBodyHeight(int, BodyHeightUnits)}.
	 */
	public enum BodyHeightUnits
	{
		/** Pixels. */
		PIXELS("px"),

		/**
		 * Percent of the height of the element the table is in. As for any percentage height in
		 * CSS, that element needs a height of its own; without one the body takes the height of
		 * its rows.
		 */
		PERCENT("%"),

		/** Percent of the height of the browser's viewport. */
		VIEWPORT_HEIGHT("vh");

		private final String cssUnit;

		BodyHeightUnits(String cssUnit)
		{
			this.cssUnit = cssUnit;
		}

		/**
		 * @return the unit in CSS, such as {@code px}
		 */
		public String getCssUnit()
		{
			return cssUnit;
		}
	}

	private final List<C> columns;

	private final List<C> initialColumns;

	private IColumnStateStore columnStateStore;

	private final Set<C> hiddenColumns = Collections
		.newSetFromMap(new IdentityHashMap<>());

	private final IDynamicDataProvider<T, K> provider;

	private final Map<String, IAjaxActionColumnContributor<T>> actionContributors =
		new LinkedHashMap<>();

	private final ActionBehavior actionBehavior;

	private final NavigationBehavior navigationBehavior;

	private final WebMarkupContainer headersRow;

	private final RepeatingView topToolbars;

	private final RepeatingView bottomToolbars;

	private final WebMarkupContainer overlay;

	private final WebMarkupContainer overlayWindow;

	private boolean overlayShown;

	private OverlayPlacement overlayPlacement = OverlayPlacement.LOCAL;

	private final List<IDynamicToolbarAction<T, K>> toolbarActions = new ArrayList<>();

	private int toolbarCount;

	private ToolbarPosition selectionToolbarPosition = ToolbarPosition.TOP;

	private SelectionMode selectionMode = SelectionMode.MANY;

	private boolean allSelected;

	private boolean clearSelectionOnNavigation;

	private final LinkedHashSet<K> selectedKeys = new LinkedHashSet<>();

	private ToolbarPosition navigationPosition = ToolbarPosition.TOP;

	private List<Long> itemsPerPageOptions = new ArrayList<>();

	private double[] columnWidths;

	private double[] columnProportions;

	private double[] columnMinWidths;

	private boolean adjustToParentWidth;

	private int bodyHeight = -1;

	private BodyHeightUnits bodyHeightUnits = BodyHeightUnits.PIXELS;

	private long itemsPerPage;

	private long currentPage;

	private boolean infiniteScroll;

	private long loadedPages = 1;

	private boolean scrollToTop;

	private transient Long itemCount;

	private transient List<T> currentPageRows;

	private Component rowsBody;

	private transient Map<String, String> templates;

	/**
	 * @param id
	 *            component id
	 * @param columns
	 *            the columns, in display order, all of them shown
	 * @param provider
	 *            supplies the rows and resolves them back from their keys
	 * @param rowsPerPage
	 *            the number of rows shown on a page
	 */
	protected BaseDynamicDataTable(String id, List<? extends C> columns,
		IDynamicDataProvider<T, K> provider, long rowsPerPage)
	{
		super(id);
		this.columns = new ArrayList<>(Args.notEmpty(columns, "columns"));
		initialColumns = List.copyOf(this.columns);
		checkColumnIds();
		this.provider = Args.notNull(provider, "provider");
		setItemsPerPage(rowsPerPage);

		setOutputMarkupId(true);

		add(topToolbars = new RepeatingView("topToolbars"));
		add(bottomToolbars = new RepeatingView("bottomToolbars"));

		overlay = new WebMarkupContainer("overlay")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(overlayShown);
			}

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				if (overlayPlacement == OverlayPlacement.BODY)
				{
					Looks.addClasses(tag, Looks.of(this), LookPart.TABLE_OVERLAY_OVER_ROWS);
				}
			}
		};
		overlay.setOutputMarkupPlaceholderTag(true);
		overlay.add(new LookBehavior(LookPart.TABLE_OVERLAY));
		overlay.add(new LookContainer("veil", LookPart.TABLE_OVERLAY_VEIL));
		add(overlay);
		overlayWindow = new WebMarkupContainer("window")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void renderHead(IHeaderResponse response)
			{
				super.renderHead(response);
				response.render(JavaScriptHeaderItem.forReference(JAVASCRIPT));
				response.render(OnDomReadyHeaderItem.forScript(
					"Wicket.DynamicDataTable.placeOverlay(" + ScriptJson.quote(getMarkupId()) +
						");"));
			}
		};
		overlayWindow.setOutputMarkupId(true);
		overlayWindow.add(new TrapFocusBehavior(), new LookBehavior(LookPart.TABLE_OVERLAY_WINDOW));
		overlayWindow.add(new WebMarkupContainer(OVERLAY_CONTENT_ID));
		overlay.add(overlayWindow);

		headersRow = new WebMarkupContainer("headersRow");
		headersRow.setOutputMarkupId(true);
		headersRow.add(new LookBehavior(LookPart.TABLE_HEADERS));
		add(headersRow);
		headersRow.add(new ListView<>("headers", this::getVisibleColumns)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<C> item)
			{
				populateHeader(item, item.getModelObject());
			}
		});
		headersRow.add(new WebMarkupContainer("filler")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(hasFiller());
			}
		}.add(new LookBehavior(LookPart.TABLE_FILLER)));

		add(actionBehavior = new ActionBehavior());
		add(navigationBehavior = new NavigationBehavior());
	}

	/**
	 * Registers the contributor handling an action. Subclasses register the contributors of the
	 * columns they are given, such as those of a {@link CompoundDynamicColumn}.
	 *
	 * @param contributor
	 *            the contributor
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             if another contributor is registered for the same action, or the action
	 *             starts with {@value #RESERVED_ACTION_PREFIX}
	 */
	public BaseDynamicDataTable<T, K, C> addActionContributor(
		IAjaxActionColumnContributor<T> contributor)
	{
		Args.notNull(contributor, "contributor");
		String actionId = Args.notEmpty(contributor.getActionId(), "actionId");
		if (actionId.startsWith(RESERVED_ACTION_PREFIX))
		{
			throw new IllegalArgumentException("The action '" + actionId +
				"' starts with the reserved prefix '" + RESERVED_ACTION_PREFIX + "'");
		}
		IAjaxActionColumnContributor<T> registered = actionContributors.putIfAbsent(actionId,
			contributor);
		if (registered != null && registered != contributor)
		{
			throw new IllegalArgumentException(
				"Another contributor is already registered for action '" + actionId + "'");
		}
		if (registered == null)
		{
			contributor.bind(this);
		}
		return this;
	}

	/**
	 * Dispatches a click on an element with a {@code data-dt-action} attribute to the
	 * {@link IAjaxActionColumnContributor} registered for the action, or to
	 * {@link #onNotFoundAction(AjaxRequestTarget, String, Serializable, Object)} if there is none
	 * or the row no longer exists.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param action
	 *            the value of the clicked element's {@code data-dt-action} attribute
	 * @param key
	 *            the key of the row the element belongs to, or {@code null} if it is not in a row
	 *            or the browser sent no valid key
	 * @param row
	 *            the row the element belongs to, or {@code null} if it no longer exists
	 */
	protected final void onAction(AjaxRequestTarget target, String action, K key, T row)
	{
		IAjaxActionColumnContributor<T> contributor = actionContributors.get(action);
		if (contributor == null || row == null)
		{
			onNotFoundAction(target, action, key, row);
		}
		else
		{
			contributor.onAction(row, target);
		}
	}

	/**
	 * Called when a click cannot be dispatched: the row no longer exists, for example because it
	 * was deleted after it was rendered, or no contributor is registered for the action. Does
	 * nothing by default.
	 * <p>
	 * Both the action and the key come from the browser.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param action
	 *            the value of the clicked element's {@code data-dt-action} attribute
	 * @param key
	 *            the key the row was looked up by, or {@code null} if the element is not in a row
	 *            or the browser sent no valid key
	 * @param row
	 *            the row, or {@code null} if it no longer exists
	 */
	protected void onNotFoundAction(AjaxRequestTarget target, String action, K key, T row)
	{
	}

	/**
	 * Shows a component in a window on top of the table, over a veil that blocks the table until
	 * {@link #closeOverlay(IPartialPageRequestHandler)} is called or the user presses Escape, or on
	 * top of the whole page, see {@link #setOverlayPlacement(OverlayPlacement)}. The
	 * focus moves into the window and stays there while it is shown. Showing another component
	 * replaces the current one.
	 *
	 * @param content
	 *            the component to show, with the id {@value #OVERLAY_CONTENT_ID}, for example a
	 *            panel with a form editing a row
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> showOverlay(Component content,
		IPartialPageRequestHandler target)
	{
		Args.notNull(content, "content");
		Args.isTrue(OVERLAY_CONTENT_ID.equals(content.getId()),
			"the content of the overlay must have the id '" + OVERLAY_CONTENT_ID + "'");
		overlayWindow.replace(content);
		overlayShown = true;
		target.add(overlay);
		return this;
	}

	/**
	 * Closes the overlay, if it is shown, giving the table back to the user.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> closeOverlay(IPartialPageRequestHandler target)
	{
		if (overlayShown)
		{
			overlayShown = false;
			overlayWindow.replace(new WebMarkupContainer(OVERLAY_CONTENT_ID));
			target.add(overlay);
			onOverlayClosed(target);
		}
		return this;
	}

	/**
	 * @return where the overlay is shown, {@link OverlayPlacement#LOCAL} by default
	 * @since 11.0.0
	 */
	public OverlayPlacement getOverlayPlacement()
	{
		return overlayPlacement;
	}

	/**
	 * Sets where the overlay is shown: over the table, or over the whole page.
	 *
	 * @param placement
	 *            where the overlay is shown
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> setOverlayPlacement(OverlayPlacement placement)
	{
		overlayPlacement = Args.notNull(placement, "placement");
		return this;
	}

	/**
	 * @return whether the overlay is shown
	 * @since 11.0.0
	 */
	public boolean isOverlayShown()
	{
		return overlayShown;
	}

	/**
	 * Called after the overlay was closed, by {@link #closeOverlay(IPartialPageRequestHandler)} or
	 * by the user pressing Escape. Does nothing by default.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 * @since 11.0.0
	 */
	protected void onOverlayClosed(IPartialPageRequestHandler target)
	{
	}

	/**
	 * Queries the rows of the current page again, has the browser repaint all of them and
	 * re-renders the toolbars.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 */
	public void refresh(IPartialPageRequestHandler target)
	{
		forgetRows();
		refreshToolbars(target);
		refreshRows(target);
	}

	/**
	 * Has the browser repaint the rows of the current page.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 */
	protected abstract void refreshRows(IPartialPageRequestHandler target);

	/**
	 * Forgets the rows of the current page and their count, so they are queried again.
	 */
	protected final void forgetRows()
	{
		itemCount = null;
		currentPageRows = null;
	}

	void refreshToolbars(IPartialPageRequestHandler target)
	{
		for (AbstractDynamicToolbar toolbar : getToolbars())
		{
			toolbar.refresh(target);
		}
	}

	private List<AbstractDynamicToolbar> getToolbars()
	{
		List<AbstractDynamicToolbar> toolbars = new ArrayList<>();
		for (String id : List.of("topNavigation", "topSelection", "bottomSelection",
			"bottomNavigation"))
		{
			if (get(id) instanceof AbstractDynamicToolbar toolbar)
			{
				toolbars.add(toolbar);
			}
		}
		for (Component toolbar : topToolbars)
		{
			toolbars.add((AbstractDynamicToolbar)toolbar);
		}
		for (Component toolbar : bottomToolbars)
		{
			toolbars.add((AbstractDynamicToolbar)toolbar);
		}
		return toolbars;
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		rowsBody = newRowsBody(ROWS_BODY_ID);
		Args.isTrue(ROWS_BODY_ID.equals(rowsBody.getId()),
			"the rows body must have the id '" + ROWS_BODY_ID + "'");
		add(rowsBody);
		Behavior veil = newVeilBehavior();
		if (veil != null)
		{
			add(veil);
		}
		add(checkToolbar(newNavigationToolbar("topNavigation", ToolbarPosition.TOP),
			"topNavigation").add(new PositionBehavior(true, ToolbarPosition.TOP)));
		add(checkToolbar(newSelectionToolbar("topSelection", ToolbarPosition.TOP), "topSelection")
			.add(new PositionBehavior(false, ToolbarPosition.TOP)));
		add(checkToolbar(newSelectionToolbar("bottomSelection", ToolbarPosition.BOTTOM),
			"bottomSelection").add(new PositionBehavior(false, ToolbarPosition.BOTTOM)));
		add(checkToolbar(newNavigationToolbar("bottomNavigation", ToolbarPosition.BOTTOM),
			"bottomNavigation").add(new PositionBehavior(true, ToolbarPosition.BOTTOM)));
		if (columnStateStore != null)
		{
			ColumnState state = columnStateStore.load(getColumnStateKey());
			if (state != null)
			{
				setColumnState(state);
			}
		}
	}

	/**
	 * Creates the component holding the rows, attached to the table's {@code <tbody>}.
	 *
	 * @param id
	 *            the id the component must have, {@value #ROWS_BODY_ID}
	 * @return the component
	 */
	protected abstract Component newRowsBody(String id);

	/**
	 * @return the component holding the rows, created by {@link #newRowsBody(String)}
	 */
	protected final Component getRowsBody()
	{
		return rowsBody;
	}

	/**
	 * Creates the behavior blocking the table while one of its Ajax requests runs, for example
	 * while a row is saved or deleted from the overlay, or a page is fetched: a
	 * {@link LocalVeilBehavior} by default, which shows a spinner over the table when a request
	 * takes a while.
	 *
	 * @return the behavior, or {@code null} to leave the table usable during its requests
	 * @since 11.0.0
	 */
	protected Behavior newVeilBehavior()
	{
		return new LocalVeilBehavior();
	}

	/**
	 * Allows a built-in toolbar to be shown only where the table's settings want it.
	 */
	private class PositionBehavior extends Behavior
	{
		private static final long serialVersionUID = 1L;

		private final boolean navigation;

		private final ToolbarPosition position;

		PositionBehavior(boolean navigation, ToolbarPosition position)
		{
			this.navigation = navigation;
			this.position = position;
		}

		@Override
		public void onConfigure(Component component)
		{
			component.setVisibilityAllowed(isShown(
				navigation ? navigationPosition : selectionToolbarPosition, position));
		}
	}

	private static AbstractDynamicToolbar checkToolbar(AbstractDynamicToolbar toolbar, String id)
	{
		Args.notNull(toolbar, "toolbar");
		if (id.equals(toolbar.getId()) == false)
		{
			throw new IllegalStateException("The toolbar must have the id '" + id + "'");
		}
		return toolbar;
	}

	private static boolean isShown(ToolbarPosition setting, ToolbarPosition position)
	{
		return setting == position || setting == ToolbarPosition.BOTH;
	}

	/**
	 * Creates a navigation toolbar of the table, a {@link DynamicNavigationToolbar} by default.
	 * Override it to customize the navigation, for example with a filter in front of it.
	 *
	 * @param id
	 *            the id the toolbar must have
	 * @param position
	 *            whether the toolbar is above or below the rows, {@link ToolbarPosition#TOP} or
	 *            {@link ToolbarPosition#BOTTOM}
	 * @return the toolbar
	 */
	protected AbstractDynamicToolbar newNavigationToolbar(String id, ToolbarPosition position)
	{
		return new DynamicNavigationToolbar<>(id, this, position);
	}

	/**
	 * Creates a selection bar of the table, a {@link DynamicSelectionToolbar} by default.
	 *
	 * @param id
	 *            the id the toolbar must have
	 * @param position
	 *            whether the toolbar is above or below the rows, {@link ToolbarPosition#TOP} or
	 *            {@link ToolbarPosition#BOTTOM}
	 * @return the toolbar
	 */
	protected AbstractDynamicToolbar newSelectionToolbar(String id, ToolbarPosition position)
	{
		return new DynamicSelectionToolbar(id, this);
	}

	/**
	 * Adds a toolbar above the headers, below the built-in ones.
	 *
	 * @param toolbar
	 *            the toolbar, created with {@link AbstractDynamicToolbar#AbstractDynamicToolbar(
	 *            BaseDynamicDataTable) the table}
	 * @return {@code this}
	 */
	public BaseDynamicDataTable<T, K, C> addTopToolbar(AbstractDynamicToolbar toolbar)
	{
		topToolbars.add(Args.notNull(toolbar, "toolbar"));
		return this;
	}

	/**
	 * Adds a toolbar below the rows, above the built-in ones.
	 *
	 * @param toolbar
	 *            the toolbar, created with {@link AbstractDynamicToolbar#AbstractDynamicToolbar(
	 *            BaseDynamicDataTable) the table}
	 * @return {@code this}
	 */
	public BaseDynamicDataTable<T, K, C> addBottomToolbar(AbstractDynamicToolbar toolbar)
	{
		bottomToolbars.add(Args.notNull(toolbar, "toolbar"));
		return this;
	}

	/**
	 * @return a new id for a toolbar of this table
	 */
	public String newToolbarId()
	{
		return "toolbar" + toolbarCount++;
	}

	/**
	 * Adds an action on the whole table to its navigation, after the paging links.
	 *
	 * @param action
	 *            the action
	 * @return {@code this}
	 */
	public BaseDynamicDataTable<T, K, C> addToolbarAction(IDynamicToolbarAction<T, K> action)
	{
		toolbarActions.add(Args.notNull(action, "action"));
		return this;
	}

	/**
	 * @return the actions on the whole table
	 */
	public List<IDynamicToolbarAction<T, K>> getToolbarActions()
	{
		return List.copyOf(toolbarActions);
	}

	/**
	 * @return every column, shown or hidden, in display order
	 */
	public List<C> getColumns()
	{
		return List.copyOf(columns);
	}

	/**
	 * @return the columns the table shows, in display order
	 * @since 11.0.0
	 */
	public List<C> getVisibleColumns()
	{
		List<C> visible = new ArrayList<>(columns.size());
		for (C column : columns)
		{
			if (hiddenColumns.contains(column) == false &&
				(selectionMode != SelectionMode.NONE || isSelectionColumn(column) == false))
			{
				visible.add(column);
			}
		}
		return visible;
	}

	/**
	 * @param column
	 *            a column of the table
	 * @return whether the table shows the column
	 * @since 11.0.0
	 */
	public boolean isColumnVisible(C column)
	{
		return hiddenColumns.contains(column) == false;
	}

	/**
	 * Shows or hides a column, whether or not it is {@link #isHideable(IDetachable)
	 * hideable}. Re-render the table for the change to show.
	 *
	 * @param column
	 *            a column of the table
	 * @param visible
	 *            whether the table shows the column
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> setColumnVisible(C column, boolean visible)
	{
		indexOfColumn(column);
		if (visible)
		{
			hiddenColumns.remove(column);
		}
		else
		{
			hiddenColumns.add(column);
		}
		return this;
	}

	/**
	 * @param column
	 *            a column of the table
	 * @return whether the user can hide the column: whether it implements
	 *         {@link IHideableColumn}
	 * @since 11.0.0
	 */
	public boolean isHideable(C column)
	{
		return column instanceof IHideableColumn;
	}

	/**
	 * @param column
	 *            a column of the table
	 * @return whether the user can move the column: whether it implements {@link IMovableColumn}
	 * @since 11.0.0
	 */
	public boolean isMovable(C column)
	{
		return column instanceof IMovableColumn;
	}

	/**
	 * @param column
	 *            a column of the table
	 * @return the id the column declares, or {@code null} if it has none, see
	 *         {@link #getColumnId(IDetachable)}
	 */
	protected abstract String getDeclaredColumnId(C column);

	/**
	 * @param id
	 *            the id the header has to carry
	 * @param column
	 *            a column of the table
	 * @return the header of the column, inside the sorting link if the column is sortable
	 */
	protected abstract Component newHeader(String id, C column);

	/**
	 * @param column
	 *            a column of the table
	 * @return the title of the column as text, for example for a
	 *         {@link ColumnChooserToolbarAction} and the header line of a CSV export
	 */
	protected abstract IModel<String> getColumnTitle(C column);

	/**
	 * @param column
	 *            a column of the table
	 * @return the CSS class of the column's header and cells, or {@code null}
	 */
	protected abstract String getColumnCssClass(C column);

	/**
	 * @param column
	 *            a column of the table
	 * @return whether the user can resize the column, while the table has a
	 *         {@link ResizableColumnsBehavior}
	 */
	protected abstract boolean isColumnResizable(C column);

	/**
	 * @param column
	 *            a column of the table
	 * @return the property the column sorts the rows by, or {@code null} if it does not sort them
	 */
	protected abstract Object getSortProperty(C column);

	/**
	 * @param column
	 *            a column of the table
	 * @return whether a CSV export writes the column
	 */
	protected abstract boolean isColumnExportable(C column);

	/**
	 * @param column
	 *            an {@link #isColumnExportable(IDetachable) exportable} column of the table
	 * @param row
	 *            a row
	 * @return the value a CSV export writes for the row in the column
	 */
	protected abstract Object getColumnExportValue(C column, T row);


	/**
	 * Moves a column to another place, whether or not it is {@link #isMovable(IDetachable)
	 * movable}, taking its width, proportion and minimum width along. Re-render the table for the change to show.
	 *
	 * @param column
	 *            a column of the table
	 * @param index
	 *            the index the column has in {@link #getColumns()} afterwards
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> moveColumn(C column, int index)
	{
		int from = indexOfColumn(column);
		Args.withinRange(0, columns.size() - 1, index, "index");
		columns.add(index, columns.remove(from));
		columnWidths = move(columnWidths, from, index);
		columnProportions = move(columnProportions, from, index);
		columnMinWidths = move(columnMinWidths, from, index);
		return this;
	}

	/**
	 * @return the values given for every column, in the order of the columns arranged
	 */
	private double[] arrange(double[] values, List<C> arranged)
	{
		if (values == null)
		{
			return null;
		}
		double[] result = new double[columns.size()];
		for (int i = 0; i < arranged.size(); i++)
		{
			result[i] = values[indexOfColumn(arranged.get(i))];
		}
		return result;
	}

	private static double[] move(double[] values, int from, int to)
	{
		if (values == null)
		{
			return null;
		}
		List<Double> list = new ArrayList<>(values.length);
		for (double value : values)
		{
			list.add(value);
		}
		list.add(to, list.remove(from));
		return list.stream().mapToDouble(Double::doubleValue).toArray();
	}

	/**
	 * @return the index of the column in {@link #getColumns()}
	 * @throws IllegalArgumentException
	 *             if the column is not one of the table
	 */
	private int indexOfColumn(C column)
	{
		for (int i = 0; i < columns.size(); i++)
		{
			if (columns.get(i) == column)
			{
				return i;
			}
		}
		throw new IllegalArgumentException("The column is not one of the table");
	}

	/**
	 * @param column
	 *            a column of the table
	 * @return the id of the column in a {@link ColumnState}: its
	 *         {@link #getDeclaredColumnId(IDetachable) declared id}, or else its index in the list
	 *         of columns the table was constructed with. Give the columns ids when their states
	 *         are kept, for example in a database, across changes to the columns of the table.
	 * @throws IllegalArgumentException
	 *             if the column is not one of the table
	 * @since 11.0.0
	 */
	public final String getColumnId(C column)
	{
		String declared = getDeclaredColumnId(column);
		if (declared != null)
		{
			indexOfColumn(column);
			return declared;
		}
		for (int i = 0; i < initialColumns.size(); i++)
		{
			if (initialColumns.get(i) == column)
			{
				return String.valueOf(i);
			}
		}
		throw new IllegalArgumentException("The column is not one of the table");
	}

	/**
	 * @throws IllegalArgumentException
	 *             if two columns have the same id
	 */
	private void checkColumnIds()
	{
		Set<String> ids = new HashSet<>();
		for (C column : initialColumns)
		{
			String columnId = getColumnId(column);
			Args.isTrue(ids.add(columnId), "The id of every column has to be unique, '%s' is not",
				columnId);
		}
	}

	/**
	 * @return the key the table's {@link ColumnState} is saved under in its
	 *         {@link #setColumnStateStore(IColumnStateStore) store}: by default the class of the
	 *         page and the path of the table in it. Override to tell apart tables a page shows
	 *         for different purposes, or to share a state between pages.
	 * @since 11.0.0
	 */
	public String getColumnStateKey()
	{
		return getPage().getClass().getName() + ":" + getPageRelativePath();
	}

	/**
	 * @return the store keeping the table's {@link ColumnState}, or {@code null} if it is kept
	 *         only as long as the table
	 * @since 11.0.0
	 */
	public IColumnStateStore getColumnStateStore()
	{
		return columnStateStore;
	}

	/**
	 * Sets the store keeping the table's {@link ColumnState}. The table loads its state from the
	 * store when it is initialized, so the store has to be set before, and saves it whenever the
	 * user moves, shows, hides or resizes a column.
	 *
	 * @param columnStateStore
	 *            the store, for example {@link SessionColumnStateStore}; {@code null}, the
	 *            default, to keep the state only as long as the table
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> setColumnStateStore(IColumnStateStore columnStateStore)
	{
		this.columnStateStore = columnStateStore;
		return this;
	}

	/**
	 * @return the order, the visibility and the known widths of the columns
	 * @throws IllegalArgumentException
	 *             if two columns have the same id
	 * @since 11.0.0
	 */
	public ColumnState getColumnState()
	{
		checkColumnIds();
		List<String> order = new ArrayList<>(columns.size());
		List<String> hidden = new ArrayList<>();
		Map<String, Double> widths = new LinkedHashMap<>();
		for (int i = 0; i < columns.size(); i++)
		{
			C column = columns.get(i);
			String id = getColumnId(column);
			order.add(id);
			if (hiddenColumns.contains(column))
			{
				hidden.add(id);
			}
			if (columnWidths != null && columnWidths[i] > 0)
			{
				widths.put(id, columnWidths[i]);
			}
		}
		return new ColumnState(order, hidden, widths);
	}

	/**
	 * Arranges the columns as a state says, as far as the user could have: the
	 * {@link #isMovable(IDetachable) movable} columns take the order of the state among the
	 * places of the movable columns, the {@link #isHideable(IDetachable) hideable} ones are
	 * shown or hidden, and the columns get the widths of the state, which must be positive and at
	 * most {@value #MAX_COLUMN_WIDTH} pixels. Ids the table does not know are ignored, and columns
	 * the state does not know keep their places after the known ones. Re-render the table for the
	 * change to show.
	 *
	 * @param state
	 *            the state
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             if two columns have the same id
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> setColumnState(ColumnState state)
	{
		Args.notNull(state, "state");
		checkColumnIds();
		List<String> order = state.getOrder();
		List<C> movable = new ArrayList<>(columns.stream().filter(this::isMovable)
			.toList());
		movable.sort(Comparator.comparingInt(column -> {
			int rank = order.indexOf(getColumnId(column));
			return rank >= 0 ? rank : order.size();
		}));
		Iterator<C> placed = movable.iterator();
		List<C> arranged = new ArrayList<>(columns.size());
		for (C column : columns)
		{
			arranged.add(isMovable(column) ? placed.next() : column);
		}
		double[] widths = new double[columns.size()];
		boolean anyWidth = false;
		for (int i = 0; i < arranged.size(); i++)
		{
			C column = arranged.get(i);
			Double width = state.getWidths().get(getColumnId(column));
			if (width != null && Double.isFinite(width) && width > 0 && width <= MAX_COLUMN_WIDTH)
			{
				widths[i] = width;
				anyWidth = true;
			}
			else if (columnWidths != null)
			{
				widths[i] = columnWidths[indexOfColumn(column)];
				anyWidth |= widths[i] > 0;
			}
		}
		columnProportions = arrange(columnProportions, arranged);
		columnMinWidths = arrange(columnMinWidths, arranged);
		columnWidths = anyWidth ? widths : null;
		columns.clear();
		columns.addAll(arranged);
		for (C column : columns)
		{
			if (isHideable(column))
			{
				setColumnVisible(column, state.getHidden().contains(getColumnId(column)) == false);
			}
		}
		if (getVisibleColumns().isEmpty())
		{
			hiddenColumns.clear();
		}
		return this;
	}

	/**
	 * Saves the state of the columns in the store, if any.
	 */
	private void saveColumnState()
	{
		if (columnStateStore != null)
		{
			columnStateStore.save(getColumnStateKey(), getColumnState());
		}
	}

	/**
	 * Called after the user moved, showed or hid columns, before the table is re-rendered. Does
	 * nothing by default; override to keep the columns' order and visibility elsewhere, such as in
	 * a user's preferences.
	 *
	 * @param target
	 *            the Ajax request target
	 * @since 11.0.0
	 */
	protected void onColumnsChanged(AjaxRequestTarget target)
	{
	}

	/**
	 * @return whether the rows end with an empty cell, leaving room after the last column, which
	 *         is the case while the columns can be resized or moved, and for the vertical scroll
	 *         bar of a body with a height
	 */
	protected final boolean hasFiller()
	{
		return isResizable() || (isMoving() && hasMovableColumns()) || bodyHeight > 0;
	}

	/**
	 * @return the number of cells in a row: one per shown column, and the empty one at the end
	 *         while the columns can be resized or moved or the body has a height; the
	 *         {@code colspan} of a cell spanning a whole row, as in a toolbar
	 * @since 11.0.0
	 */
	public int getCellCount()
	{
		return getVisibleColumns().size() + (hasFiller() ? 1 : 0);
	}

	/**
	 * @return whether the table has a {@link ResizableColumnsBehavior}
	 */
	private boolean isResizable()
	{
		return getBehaviors(ResizableColumnsBehavior.class).isEmpty() == false;
	}

	/**
	 * @return whether the table has a {@link MovableColumnsBehavior}
	 */
	private boolean isMoving()
	{
		return getBehaviors(MovableColumnsBehavior.class).isEmpty() == false;
	}

	/**
	 * @return whether a column the table shows can be moved
	 */
	private boolean hasMovableColumns()
	{
		return getVisibleColumns().stream().anyMatch(this::isMovable);
	}

	/**
	 * Moves a column as the browser reported it, as two indexes separated by a comma.
	 *
	 * @return whether the column was moved
	 */
	/**
	 * Moves the column shown at {@code from} in front of the one shown at {@code to}, or to the end
	 * if {@code to} is the number of shown columns, as long as it is movable and the columns that
	 * are not keep their places.
	 *
	 * @return whether the column was moved
	 */
	private boolean moveVisibleColumn(int from, int to)
	{
		List<C> visible = getVisibleColumns();
		if (from < 0 || from >= visible.size() || to < 0 || to > visible.size() || to == from ||
			to == from + 1 || isMovable(visible.get(from)) == false)
		{
			return false;
		}
		C column = visible.get(from);
		List<C> moved = new ArrayList<>(visible);
		moved.remove(from);
		moved.add(to > from ? to - 1 : to, column);
		for (int i = 0; i < visible.size(); i++)
		{
			if (isMovable(visible.get(i)) == false && moved.get(i) != visible.get(i))
			{
				return false;
			}
		}
		int index = to < visible.size() ? indexOfColumn(visible.get(to))
			: indexOfColumn(visible.get(visible.size() - 1)) + 1;
		moveColumn(column, index > indexOfColumn(column) ? index - 1 : index);
		return true;
	}

	/**
	 * Shows the columns at the given indexes of {@link #getColumns()} and hides the other
	 * {@link #isHideable(IDetachable) hideable} ones, unless that leaves no column shown.
	 *
	 * @return whether the columns were changed
	 */
	private boolean showColumns(String indexes)
	{
		Set<C> shown = Collections.newSetFromMap(new IdentityHashMap<>());
		for (String index : Strings.isEmpty(indexes) ? new String[0] : indexes.split(","))
		{
			try
			{
				shown.add(columns.get(Integer.parseInt(index.trim())));
			}
			catch (NumberFormatException | IndexOutOfBoundsException e)
			{
				return false;
			}
		}
		Set<C> hidden = Collections.newSetFromMap(new IdentityHashMap<>());
		for (C column : columns)
		{
			boolean hide = isHideable(column) ? shown.contains(column) == false
				: hiddenColumns.contains(column);
			if (hide)
			{
				hidden.add(column);
			}
		}
		if (hidden.size() == columns.size() || hidden.equals(hiddenColumns))
		{
			return false;
		}
		hiddenColumns.clear();
		hiddenColumns.addAll(hidden);
		return true;
	}

	/**
	 * @return how many rows the user can select, {@link SelectionMode#MANY} by default
	 */
	public SelectionMode getSelectionMode()
	{
		return selectionMode;
	}

	/**
	 * Sets how many rows the user can select. {@link SelectionMode#NONE} deselects every row and
	 * leaves the selection column out; {@link SelectionMode#ONE} keeps at most one of the selected
	 * rows. The browser shows it once the table is rendered again.
	 *
	 * @param selectionMode
	 *            how many rows the user can select
	 * @return {@code this}
	 */
	public BaseDynamicDataTable<T, K, C> setSelectionMode(SelectionMode selectionMode)
	{
		this.selectionMode = Args.notNull(selectionMode, "selectionMode");
		if (selectionMode == SelectionMode.NONE)
		{
			clearSelection();
		}
		else if (selectionMode == SelectionMode.ONE && (allSelected || selectedKeys.size() > 1))
		{
			K first = allSelected ? null : selectedKeys.iterator().next();
			clearSelection();
			if (first != null)
			{
				selectedKeys.add(first);
			}
		}
		return this;
	}

	/**
	 * @return whether the column selects rows, a {@link SelectionColumn} or a
	 *         {@link BeanSelectionColumn}, which {@link SelectionMode#NONE} leaves out
	 */
	protected boolean isSelectionColumn(C column)
	{
		return column instanceof SelectionColumn || column instanceof BeanSelectionColumn;
	}

	/**
	 * @return where the selection bar is shown, {@link ToolbarPosition#TOP} by default
	 */
	public ToolbarPosition getSelectionToolbarPosition()
	{
		return selectionToolbarPosition;
	}

	/**
	 * @param selectionToolbarPosition
	 *            where the selection bar is shown, when it is
	 * @return {@code this}
	 */
	public BaseDynamicDataTable<T, K, C> setSelectionToolbarPosition(
		ToolbarPosition selectionToolbarPosition)
	{
		this.selectionToolbarPosition = Args.notNull(selectionToolbarPosition,
			"selectionToolbarPosition");
		return this;
	}

	/**
	 * @return where the navigation is shown, {@link ToolbarPosition#TOP} by default
	 */
	public ToolbarPosition getNavigationPosition()
	{
		return navigationPosition;
	}

	/**
	 * @param navigationPosition
	 *            where the navigation is shown when there is more than one page
	 * @return {@code this}
	 */
	public BaseDynamicDataTable<T, K, C> setNavigationPosition(
		ToolbarPosition navigationPosition)
	{
		this.navigationPosition = Args.notNull(navigationPosition, "navigationPosition");
		return this;
	}

	/**
	 * @return the numbers of rows per page the user can choose from, empty if there is no choice
	 */
	public List<Long> getItemsPerPageOptions()
	{
		return List.copyOf(itemsPerPageOptions);
	}

	/**
	 * Lets the user choose the number of rows per page from a drop-down in the navigation, which
	 * is then shown even when there is only one page.
	 *
	 * @param options
	 *            the numbers of rows per page to choose from, positive; none for no choice
	 * @return {@code this}
	 */
	public BaseDynamicDataTable<T, K, C> setItemsPerPageOptions(Long... options)
	{
		List<Long> checked = new ArrayList<>();
		for (Long option : options)
		{
			checked.add(Args.withinRange(1L, Long.MAX_VALUE, option, "option"));
		}
		itemsPerPageOptions = checked;
		return this;
	}

	/**
	 * @return the widths of the columns in pixels, one per column of {@link #getColumns()}, as last
	 *         resized by the user, or {@code null} if they were not resized; 0 for a column whose
	 *         width is not known, because it was hidden when the user resized the others
	 */
	public double[] getColumnWidths()
	{
		return columnWidths != null ? columnWidths.clone() : null;
	}

	/**
	 * @param widths
	 *            the widths of the columns in pixels, one per column of {@link #getColumns()}, for
	 *            example restored from a user's preferences; {@code null} to let the browser lay
	 *            out the columns. The widths are applied while every shown column has a positive
	 *            one.
	 * @return {@code this}
	 */
	public BaseDynamicDataTable<T, K, C> setColumnWidths(double... widths)
	{
		if (widths != null)
		{
			Args.isTrue(widths.length == columns.size(), "one width per column is needed");
		}
		columnWidths = widths != null ? widths.clone() : null;
		return this;
	}

	/**
	 * @return the relative widths of the columns, or {@code null} to size them by their content
	 * @since 11.0.0
	 */
	public double[] getColumnProportions()
	{
		return columnProportions != null ? columnProportions.clone() : null;
	}

	/**
	 * Sizes the columns in proportion to each other, for example {@code 1, 2, 1} to make the
	 * second column twice as wide as the others. The proportions set the initial layout: widths
	 * the user resized the columns to, or set with {@link #setColumnWidths(double...)}, take
	 * precedence.
	 *
	 * @param proportions
	 *            one positive number per column of {@link #getColumns()}; {@code null} to size the
	 *            columns by their content
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> setColumnProportions(double... proportions)
	{
		if (proportions != null)
		{
			Args.isTrue(proportions.length == columns.size(), "one proportion per column is needed");
			for (double proportion : proportions)
			{
				Args.isTrue(Double.isFinite(proportion) && proportion > 0,
					"proportions have to be positive numbers");
			}
		}
		columnProportions = proportions != null ? proportions.clone() : null;
		return this;
	}

	/**
	 * @return the narrowest each column gets when laid out, in pixels, 0 for no minimum, or
	 *         {@code null} for none at all
	 * @since 11.0.0
	 */
	public double[] getColumnMinWidths()
	{
		return columnMinWidths != null ? columnMinWidths.clone() : null;
	}

	/**
	 * Keeps the columns from getting narrower than the given widths when they are laid out for
	 * the width of the parent, with {@link #setAdjustToParentWidth(boolean)} and columns resized
	 * by a {@link com.antiliacode.widgets.table.ResizableColumnsBehavior} in
	 * {@link com.antiliacode.widgets.table.ResizableColumnsBehavior.Mode#STRETCH STRETCH} mode. On
	 * a narrow screen, a phone for example, the table is then wider than its parent, rather than
	 * squeezing every column to a letter, and the page scrolls it sideways, its navigation staying
	 * in view; give the parent {@code overflow-x: auto} to scroll it there instead. The columns
	 * given a minimum take what they need from the others. Each header cell carries its column's
	 * in {@code data-min-width}.
	 *
	 * @param minWidths
	 *            one number of pixels per column of {@link #getColumns()}, 0 for no minimum;
	 *            {@code null} for none at all
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> setColumnMinWidths(double... minWidths)
	{
		if (minWidths != null)
		{
			Args.isTrue(minWidths.length == columns.size(), "one minimum width per column is needed");
			for (double minWidth : minWidths)
			{
				Args.isTrue(Double.isFinite(minWidth) && minWidth >= 0,
					"minimum widths have to be positive numbers or 0");
			}
		}
		columnMinWidths = minWidths != null ? minWidths.clone() : null;
		return this;
	}

	/**
	 * @return whether the table initially takes the full width of its parent, {@code false} by
	 *         default
	 * @since 11.0.0
	 */
	public boolean isAdjustToParentWidth()
	{
		return adjustToParentWidth;
	}

	/**
	 * Makes the table take the full width of the element it is in, sharing it among the columns
	 * by their {@link #setColumnProportions(double...) proportions} or, without proportions, by
	 * their content. While the user has not resized a column, the table follows changes of the
	 * window's size.
	 *
	 * @param adjustToParentWidth
	 *            whether the table takes the full width of its parent
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> setAdjustToParentWidth(boolean adjustToParentWidth)
	{
		this.adjustToParentWidth = adjustToParentWidth;
		return this;
	}

	/**
	 * @return the height of the body of the table in {@link #getBodyHeightUnits()}, or a negative
	 *         number, {@code -1} by default, when the body is as high as its rows
	 * @since 11.0.0
	 */
	public int getBodyHeight()
	{
		return bodyHeight;
	}

	/**
	 * @return the unit of {@link #getBodyHeight()}, {@link BodyHeightUnits#PIXELS} by default
	 * @since 11.0.0
	 */
	public BodyHeightUnits getBodyHeightUnits()
	{
		return bodyHeightUnits;
	}

	/**
	 * Gives the body of the table, the rows between the headers and the footer, a fixed height.
	 * Only the body scrolls, vertically, its scroll bar next to the rows, while the headers, the
	 * toolbars and the footer stay where they are. Fewer rows leave the rest of the height empty,
	 * so the table keeps its height whatever the page shows. A table wider than its space is as
	 * wide as its columns, as without a body height. In the browser, the rows are put into a
	 * table of their own, in a {@code div} with the class {@code ant-data-table-body} in the
	 * table's body, and they end with an empty cell, which leaves room for the vertical scroll
	 * bar.
	 *
	 * @param height
	 *            the height of the body in the given units; any negative number, such as
	 *            {@code -1}, to let the body take the height of its rows, as by default
	 * @param units
	 *            the units of the height
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> setBodyHeight(int height, BodyHeightUnits units)
	{
		Args.isTrue(height != 0, "the body height has to be positive, or negative for none");
		bodyHeightUnits = Args.notNull(units, "units");
		bodyHeight = height;
		return this;
	}

	/**
	 * Called after the user resized a column and the new widths are stored. Does nothing by
	 * default; override to keep the widths elsewhere, such as in a user's preferences.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param widths
	 *            the widths of the columns in pixels, as returned by {@link #getColumnWidths()}
	 */
	protected void onColumnsResized(AjaxRequestTarget target, double[] widths)
	{
	}

	/**
	 * Stores the widths the user resized the shown columns to, as reported by a
	 * {@link ResizableColumnsBehavior}, in {@link #getColumnWidths()} and the column state, unless
	 * there is not one for each shown column, and calls
	 * {@link #onColumnsResized(AjaxRequestTarget, double[])}.
	 */
	@Override
	public void columnsResized(AjaxRequestTarget target, double[] widths)
	{
		List<C> visible = getVisibleColumns();
		if (widths == null || widths.length != visible.size())
		{
			return;
		}
		double[] all = columnWidths != null ? columnWidths.clone() : new double[columns.size()];
		for (int i = 0; i < widths.length; i++)
		{
			all[indexOfColumn(visible.get(i))] = widths[i];
		}
		columnWidths = all;
		saveColumnState();
		onColumnsResized(target, all.clone());
	}

	/**
	 * @return the widths of the shown columns, from {@link #getColumnWidths()}, or {@code null}
	 *         if not all of them are known
	 */
	@Override
	public double[] getShownColumnWidths()
	{
		double[] widths = visibleValues(columnWidths, getVisibleColumns());
		return widths != null && Arrays.stream(widths).allMatch(width -> width > 0) ? widths
			: null;
	}

	/**
	 * @return whether each shown column is {@link #isMovable(IDetachable) movable}
	 */
	@Override
	public boolean[] getMovableColumns()
	{
		List<C> visible = getVisibleColumns();
		boolean[] movable = new boolean[visible.size()];
		for (int i = 0; i < movable.length; i++)
		{
			movable[i] = isMovable(visible.get(i));
		}
		return movable;
	}

	/**
	 * Moves a shown column as reported by a {@link MovableColumnsBehavior}, saves the column state,
	 * calls {@link #onColumnsChanged(AjaxRequestTarget)} and renders the table again.
	 */
	@Override
	public void moveColumn(AjaxRequestTarget target, int from, int to)
	{
		if (moveVisibleColumn(from, to))
		{
			saveColumnState();
			onColumnsChanged(target);
			target.add(this);
		}
	}

	/**
	 * @return the rows the user selected, a view that follows the selection as it changes
	 */
	public ISelection<T, K> getSelection()
	{
		return new Selection();
	}

	/**
	 * Deselects every row. The browser shows it once the table is {@link #refresh refreshed}.
	 *
	 * @return {@code this}
	 */
	public BaseDynamicDataTable<T, K, C> clearSelection()
	{
		allSelected = false;
		selectedKeys.clear();
		return this;
	}

	/**
	 * Deselects every row and has the browser show it: the checkboxes of the rows shown are
	 * unchecked, the selection bars and the {@link ISelectionView}s, such as the
	 * {@link SelectionCountToolbarAction count}, are rendered again and
	 * {@link #onSelectionChanged(AjaxRequestTarget)} is called. Does nothing when no row is
	 * selected.
	 *
	 * @param target
	 *            the Ajax request target
	 * @since 11.0.0
	 */
	public void clearSelection(AjaxRequestTarget target)
	{
		if (getSelection().isEmpty())
		{
			return;
		}
		clearSelection();
		target.appendJavaScript("if (" + getVarName() + ") { " + getVarName() +
			".selectRows(false); }");
		selectionChanged(target);
	}

	/**
	 * @return whether going to another page, or choosing another number of rows per page,
	 *         deselects every row; {@code false} by default, the selection surviving paging
	 * @since 11.0.0
	 */
	public boolean isClearSelectionOnNavigation()
	{
		return clearSelectionOnNavigation;
	}

	/**
	 * Sets whether going to another page, or choosing another number of rows per page,
	 * deselects every row, so that the selection is always part of the page shown. Sorting
	 * deselects every row anyway.
	 *
	 * @param clearSelectionOnNavigation
	 *            whether the navigation deselects every row
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public BaseDynamicDataTable<T, K, C> setClearSelectionOnNavigation(
		boolean clearSelectionOnNavigation)
	{
		this.clearSelectionOnNavigation = clearSelectionOnNavigation;
		return this;
	}

	/**
	 * Called by the navigation when the user went to another page or chose another number of
	 * rows per page, before the rows are rendered again: deselects every row when the table
	 * {@link #isClearSelectionOnNavigation() clears its selection on navigation}.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onNavigation(AjaxRequestTarget target)
	{
		if (clearSelectionOnNavigation && getSelection().isEmpty() == false)
		{
			clearSelection();
			selectionChanged(target);
		}
	}

	/**
	 * Called after the user changed the selection. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onSelectionChanged(AjaxRequestTarget target)
	{
	}

	/**
	 * @param row
	 *            a row
	 * @return the key of the row as the browser shows it, {@link IDynamicDataProvider#formatKey
	 *         formatted} by the provider
	 */
	public String getRowKey(T row)
	{
		return provider.formatKey(provider.keyOf(row));
	}

	/**
	 * Resolves a row from a key as the browser shows it, for example one sent with a request.
	 *
	 * @param key
	 *            the key as the browser shows it, possibly not a key at all
	 * @return the row, or {@code null} if there is none for the key
	 */
	public T findRow(String key)
	{
		K parsed = provider.parseKey(key);
		return parsed != null ? provider.findByKey(parsed) : null;
	}

	/**
	 * @param key
	 *            the key of a row
	 * @return whether the row is selected
	 */
	protected final boolean isSelected(K key)
	{
		return allSelected || selectedKeys.contains(key);
	}

	private void selectionAction(AjaxRequestTarget target, String action, K key)
	{
		if (selectionMode == SelectionMode.NONE)
		{
			return;
		}
		if (SELECT_ACTION.equals(action) || DESELECT_ACTION.equals(action))
		{
			if (key == null || provider.findByKey(key) == null)
			{
				return;
			}
			leaveAllSelected();
			if (SELECT_ACTION.equals(action))
			{
				if (selectionMode == SelectionMode.ONE)
				{
					selectedKeys.clear();
				}
				selectedKeys.add(key);
			}
			else
			{
				selectedKeys.remove(key);
			}
		}
		else if (selectionMode != SelectionMode.MANY)
		{
			return;
		}
		else if (SELECT_PAGE_ACTION.equals(action))
		{
			leaveAllSelected();
			selectedKeys.addAll(getCurrentPageKeys());
		}
		else if (DESELECT_PAGE_ACTION.equals(action))
		{
			leaveAllSelected();
			getCurrentPageKeys().forEach(selectedKeys::remove);
		}
		else
		{
			return;
		}
		selectionChanged(target);
	}

	/**
	 * Turns a selection of all rows into one of the rows the browser shows checked, the rows of
	 * the current page.
	 */
	private void leaveAllSelected()
	{
		if (allSelected)
		{
			allSelected = false;
			selectedKeys.clear();
			selectedKeys.addAll(getCurrentPageKeys());
		}
	}

	void selectAll(AjaxRequestTarget target, boolean all)
	{
		if (selectionMode != SelectionMode.MANY)
		{
			return;
		}
		allSelected = all;
		selectedKeys.clear();
		target.appendJavaScript("if (" + getVarName() + ") { " + getVarName() + ".selectRows(" +
			all + "); }");
		selectionChanged(target);
	}

	private void selectionChanged(AjaxRequestTarget target)
	{
		((AbstractDynamicToolbar)get("topSelection")).refresh(target);
		((AbstractDynamicToolbar)get("bottomSelection")).refresh(target);
		refreshSelectionViews(target);
		onSelectionChanged(target);
	}

	/**
	 * Re-renders the components of the table showing its selection, the {@link ISelectionView}s.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 */
	public void refreshSelectionViews(IPartialPageRequestHandler target)
	{
		visitChildren(ISelectionView.class, (component, visit) -> {
			// a view in a toolbar that is not shown, such as the count of the navigation below
			// the rows when only the one above shows, is not in the page, not even as a placeholder
			Component parent = component.getParent();
			boolean inPage = parent == null || parent.isVisibleInHierarchy();
			if (inPage &&
				(component.isVisibleInHierarchy() || component.getOutputMarkupPlaceholderTag()))
			{
				target.add(component);
			}
			visit.dontGoDeeper();
		});
	}

	boolean isAllSelected()
	{
		return allSelected;
	}

	/**
	 * @return whether the table shows its rows in one list that grows by a page of rows each time
	 *         the user scrolls to its end, instead of a page at a time, {@code false} by default
	 * @see JsonBasedDynamicDataTable#setInfiniteScroll(boolean)
	 * @since 11.0.0
	 */
	public boolean isInfiniteScroll()
	{
		return infiniteScroll;
	}

	/**
	 * Switches infinite scrolling on or off; either way the table shows its first page of rows
	 * again.
	 *
	 * @param infiniteScroll
	 *            whether the table shows a growing list of rows
	 */
	void setInfiniteScrollEnabled(boolean infiniteScroll)
	{
		this.infiniteScroll = infiniteScroll;
		setCurrentPage(0);
	}

	/**
	 * @return the number of rows the browser shows: those of the current page, or, scrolling
	 *         infinitely, those of every page loaded so far
	 */
	long getShownRowCount()
	{
		long first = getCurrentPage() * itemsPerPage;
		return Math.max(0, Math.min(infiniteScroll ? loadedPages * itemsPerPage : itemsPerPage,
			getItemCount() - first));
	}

	/**
	 * Scrolling infinitely, adds the next page of rows to those the browser shows, if there is one.
	 *
	 * @return the rows added, empty if there are no more
	 */
	List<T> loadMoreRows()
	{
		long shown = getShownRowCount();
		if (infiniteScroll == false || shown >= getItemCount())
		{
			return List.of();
		}
		loadedPages = shown / itemsPerPage + 1;
		currentPageRows = null;
		return rows(shown, Math.min(itemsPerPage, getItemCount() - shown));
	}

	/**
	 * @return whether the rows were set back to the first page since this was last asked, so the
	 *         browser scrolls back to the top; scrolling infinitely only
	 */
	boolean takeScrollToTop()
	{
		boolean top = scrollToTop;
		scrollToTop = false;
		return top;
	}

	/**
	 * @return the rows of the current page or, scrolling infinitely, of the pages loaded so far,
	 *         queried once per request
	 */
	private List<T> getCurrentPageRows()
	{
		if (currentPageRows == null)
		{
			long first = getCurrentPage() * itemsPerPage;
			long count = getShownRowCount();
			currentPageRows = count > 0 ? rows(first, count) : List.of();
		}
		return currentPageRows;
	}

	/**
	 * @return the rows of the current page, queried once per request
	 */
	protected final List<T> getCurrentPageRowList()
	{
		return getCurrentPageRows();
	}

	/**
	 * @return the number of the first row of the current page, counting from 1
	 */
	protected final long getFirstRowNumber()
	{
		return getCurrentPage() * itemsPerPage + 1;
	}

	private List<T> rows(long first, long count)
	{
		List<T> rows = new ArrayList<>();
		provider.iterator(first, count).forEachRemaining(rows::add);
		return rows;
	}

	List<K> getCurrentPageKeys()
	{
		List<K> keys = new ArrayList<>();
		for (T row : getCurrentPageRows())
		{
			keys.add(provider.keyOf(row));
		}
		return keys;
	}

	/**
	 * Returns every row of the provider in its order, read lazily a chunk at a time while
	 * iterating. Iterate it within the request that fetched it.
	 *
	 * @return the rows
	 */
	public Iterable<T> fetchAllRows()
	{
		return AllRowsIterator::new;
	}

	/**
	 * @return whether every row of the current page is selected and there are more rows than the
	 *         page shows, so that selecting all of them is worth offering
	 */
	boolean isPageSelected()
	{
		if (selectionMode != SelectionMode.MANY || selectedKeys.isEmpty())
		{
			return false;
		}
		List<K> keys = getCurrentPageKeys();
		return keys.isEmpty() == false && getItemCount() > keys.size() &&
			selectedKeys.containsAll(keys);
	}

	private void populateHeader(ListItem<C> item, C column)
	{
		AjaxLink<Void> sort = new AjaxLink<>("sort")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				boolean sortable = isSortable(column);
				setEnabled(sortable);
				setOutputMarkupId(sortable);
				setRenderBodyOnly(sortable == false);
			}

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				sort(column, target);
			}
		};
		item.add(sort.add(new LookBehavior(LookPart.TABLE_SORT)));
		sort.add(newHeader("header", column).add(new LookBehavior(LookPart.TABLE_SORT_TITLE)));
		item.add(new LookContainer("frame", LookPart.TABLE_HEADER));
		item.add(new WebMarkupContainer("move")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(isMoving() && isMovable(column));
			}

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				String label = getString("MovableColumnsBehavior.moveColumn", null,
					"Move the column");
				tag.put("title", label);
				tag.put("aria-label", label);
			}
		}.add(new LookBehavior(LookPart.COLUMN_MOVER)));
		AjaxLink<Void> arrows = new AjaxLink<>("arrows")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(isSortable(column));
			}

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				sort(column, target);
			}
		};
		arrows.add(new LookBehavior(LookPart.TABLE_SORT_ARROWS));
		arrows.add(new LookContainer("ascending", LookPart.TABLE_SORT_ASCENDING),
			new LookContainer("descending", LookPart.TABLE_SORT_DESCENDING));
		item.add(arrows);

		String cssClass = getColumnCssClass(column);
		if (cssClass != null)
		{
			item.add(AttributeModifier.append("class", cssClass));
		}
		item.add(new LookBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected LookPart[] getParts(Component component)
			{
				return new LookPart[] { SORT_PARTS.get(getSortOrder(column)) };
			}
		});
		item.add(AttributeModifier.replace("data-resizable",
			() -> isColumnResizable(column) ? null : "false"));
		item.add(AttributeModifier.replace("data-min-width", () -> getColumnMinWidth(column)));
		item.add(AttributeModifier.replace("data-movable",
			() -> isMoving() && isMovable(column) ? "true" : null));
		item.add(AttributeModifier.replace("aria-sort",
			() -> ARIA_SORT.get(getSortOrder(column))));
	}

	/**
	 * @return the narrowest the column gets, as {@code data-min-width} of its header cell, or
	 *         {@code null} for no minimum
	 */
	private String getColumnMinWidth(C column)
	{
		if (columnMinWidths == null)
		{
			return null;
		}
		double minWidth = columnMinWidths[indexOfColumn(column)];
		if (minWidth <= 0)
		{
			return null;
		}
		return minWidth == Math.rint(minWidth) ? String.valueOf((long)minWidth)
			: String.valueOf(minWidth);
	}

	/**
	 * @return the sort state of the provider, {@code null} if it does not sort
	 */
	@SuppressWarnings("unchecked")
	ISortState<Object> getSortState()
	{
		return provider instanceof ISortStateLocator<?> locator
			? (ISortState<Object>)locator.getSortState() : null;
	}

	private boolean isSortable(C column)
	{
		return getSortState() != null && getSortProperty(column) != null;
	}

	/**
	 * @return the order the rows are sorted in by the column, or {@code null} if the column is
	 *         not sortable
	 */
	private SortOrder getSortOrder(C column)
	{
		if (isSortable(column) == false)
		{
			return null;
		}
		return getSortState().getPropertySortOrder(getSortProperty(column));
	}

	private void sort(C column, AjaxRequestTarget target)
	{
		if (isSortable(column) == false)
		{
			return;
		}
		getSortState().setPropertySortOrder(getSortProperty(column),
			getSortOrder(column) == SortOrder.ASCENDING ? SortOrder.DESCENDING
				: SortOrder.ASCENDING);
		setCurrentPage(0);
		boolean selected = getSelection().isEmpty() == false;
		clearSelection();
		target.add(headersRow);
		refresh(target);
		if (selected)
		{
			refreshSelectionViews(target);
			onSelectionChanged(target);
		}
	}

	/**
	 * @return the current page, always the first one while {@link #isInfiniteScroll() scrolling
	 *         infinitely}
	 */
	@Override
	public long getCurrentPage()
	{
		return infiniteScroll ? 0 : Math.max(0, Math.min(currentPage, getPageCount() - 1));
	}

	/**
	 * Sets the current page. While {@link #isInfiniteScroll() scrolling infinitely}, the browser
	 * shows the first page of rows again instead, scrolled to the top.
	 */
	@Override
	public void setCurrentPage(long page)
	{
		currentPage = infiniteScroll ? 0 : Math.max(0, page);
		loadedPages = 1;
		scrollToTop = infiniteScroll;
		currentPageRows = null;
	}

	@Override
	public long getPageCount()
	{
		return (getItemCount() + itemsPerPage - 1) / itemsPerPage;
	}

	@Override
	public long getItemCount()
	{
		if (itemCount == null)
		{
			itemCount = provider.size();
		}
		return itemCount;
	}

	@Override
	public long getItemsPerPage()
	{
		return itemsPerPage;
	}

	@Override
	public void setItemsPerPage(long itemsPerPage)
	{
		this.itemsPerPage = Args.withinRange(1L, Long.MAX_VALUE, itemsPerPage, "itemsPerPage");
		currentPageRows = null;
	}

	/**
	 * @return the provider of the rows
	 */
	public IDynamicDataProvider<T, K> getProvider()
	{
		return provider;
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		checkComponentTag(tag, "table");
		super.onComponentTag(tag);
		Looks.addClasses(tag, Looks.of(this), LookPart.TABLE);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);

		CoreLibrariesContributor.contributeAjax(getApplication(), response);
		ScrollbarBehavior.renderHeadItems(response);
		response.render(JavaScriptHeaderItem.forReference(JAVASCRIPT));
		response.render(CssHeaderItem.forReference(CSS));
		for (C column : getVisibleColumns())
		{
			if (column instanceof IHeaderContributor contributor)
			{
				contributor.renderHead(response);
			}
		}

		JSONObject config = buildConfig();
		configure(config);
		response.render(OnDomReadyHeaderItem.forScript("new Wicket.DynamicDataTable(" +
			ScriptJson.of(config) + ", " +
			actionBehavior.getCallbackFunction(CallbackParameter.explicit(ACTION_PARAMETER),
				CallbackParameter.explicit(KEY_PARAMETER)) +
			", " + navigationBehavior.getCallbackFunction() + getScriptArguments() + ");"));
	}

	/**
	 * Adds the settings of the subclass to the configuration of the table's script.
	 *
	 * @param config
	 *            the configuration, holding the settings of this class
	 */
	protected void configure(JSONObject config)
	{
	}

	/**
	 * @return further arguments of the table's script, each preceded by a comma, or an empty
	 *         string
	 */
	protected String getScriptArguments()
	{
		return "";
	}

	/**
	 * Reads a resource template for this table's style, variation and locale. Outside of
	 * development mode the template is read once and cached.
	 */
	String getTemplate(ResourceTemplate template)
	{
		String style = getStyle();
		String variation = getVariation();
		Locale locale = getLocale();
		if (getApplication().usesDevelopmentConfig())
		{
			return template.load(style, variation, locale);
		}
		if (templates == null)
		{
			templates = new HashMap<>();
		}
		return templates.computeIfAbsent(template.getCacheKey(style, variation, locale),
			key -> template.load(style, variation, locale));
	}

	@Override
	protected void onRemove()
	{
		super.onRemove();
		AjaxUtils.executeIfAjaxOrWebSockets(target -> target.appendJavaScript(
			"if (" + getVarName() + ") { " + getVarName() + ".destroy(); }"));
	}

	/**
	 * @return the expression evaluating to the table's client-side instance
	 */
	protected final String getVarName()
	{
		return "Wicket.DynamicDataTable.instances[" + ScriptJson.quote(getMarkupId()) + "]";
	}

	private JSONObject buildConfig()
	{
		List<C> visible = getVisibleColumns();
		JSONObject config = new JSONObject();
		config.put("id", getMarkupId());
		config.put("itemCount", getItemCount());
		config.put("currentPage", getCurrentPage());
		config.put("filler", hasFiller());
		config.put("selection", selectionMode.name().toLowerCase(Locale.ROOT));
		if (bodyHeight > 0)
		{
			JSONObject height = new JSONObject();
			height.put("value", bodyHeight);
			height.put("unit", bodyHeightUnits.getCssUnit());
			config.put("bodyHeight", height);
		}
		if (columnProportions != null || adjustToParentWidth)
		{
			JSONObject layout = new JSONObject();
			if (columnProportions != null)
			{
				layout.put("proportions", new JSONArray(visibleValues(columnProportions, visible)));
			}
			layout.put("adjustToParent", adjustToParentWidth);
			config.put("layout", layout);
		}
		if (isResizable())
		{
			JSONObject resize = new JSONObject();
			double[] widths = getShownColumnWidths();
			if (widths != null)
			{
				resize.put("widths", new JSONArray(widths));
			}
			config.put("resize", resize);
		}
		return config;
	}

	/**
	 * @return the values of the shown columns, from values given for every column
	 */
	private double[] visibleValues(double[] values, List<C> visible)
	{
		if (values == null)
		{
			return null;
		}
		return visible.stream().mapToDouble(column -> values[indexOfColumn(column)]).toArray();
	}

	@Override
	protected void onDetach()
	{
		forgetRows();
		provider.detach();
		for (C column : columns)
		{
			column.detach();
		}
		for (IAjaxActionColumnContributor<T> contributor : actionContributors.values())
		{
			contributor.detach();
		}
		super.onDetach();
	}

	private class Selection implements ISelection<T, K>
	{
		@Override
		public SelectionType getType()
		{
			return allSelected ? SelectionType.ALL : SelectionType.RANGE;
		}

		@Override
		public List<K> getKeys()
		{
			return allSelected ? List.of() : List.copyOf(selectedKeys);
		}

		@Override
		public boolean isSelected(K key)
		{
			return BaseDynamicDataTable.this.isSelected(key);
		}

		@Override
		public long size()
		{
			return allSelected ? provider.size() : selectedKeys.size();
		}

		@Override
		public Iterable<T> fetch()
		{
			if (allSelected)
			{
				return fetchAllRows();
			}
			List<K> keys = List.copyOf(selectedKeys);
			return () -> keys.stream().map(provider::findByKey).filter(Objects::nonNull).iterator();
		}
	}

	private class AllRowsIterator implements Iterator<T>
	{
		private final long size = provider.size();

		private long next;

		private Iterator<T> chunk = List.<T> of().iterator();

		@Override
		public boolean hasNext()
		{
			if (chunk.hasNext() == false && next < size)
			{
				List<T> rows = rows(next, Math.min(FETCH_CHUNK, size - next));
				next = rows.isEmpty() ? size : next + rows.size();
				chunk = rows.iterator();
			}
			return chunk.hasNext();
		}

		@Override
		public T next()
		{
			if (hasNext() == false)
			{
				throw new NoSuchElementException();
			}
			return chunk.next();
		}
	}

	private class NavigationBehavior extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			attributes.setMethod(Method.POST);
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			refreshToolbars(target);
		}
	}

	private class ActionBehavior extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);
			attributes.setMethod(Method.POST);
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			IRequestParameters parameters = getComponent().getRequest().getRequestParameters();
			String action = parameters.getParameterValue(ACTION_PARAMETER).toOptionalString();
			String key = parameters.getParameterValue(KEY_PARAMETER).toOptionalString();
			if (action == null)
			{
				return;
			}
			if (CLOSE_OVERLAY_ACTION.equals(action))
			{
				closeOverlay(target);
				return;
			}
			if (SHOW_COLUMNS_ACTION.equals(action))
			{
				if (showColumns(key))
				{
					saveColumnState();
					onColumnsChanged(target);
					target.add(BaseDynamicDataTable.this);
				}
				return;
			}
			K parsedKey = provider.parseKey(key);
			if (action.startsWith(RESERVED_ACTION_PREFIX))
			{
				selectionAction(target, action, parsedKey);
				return;
			}
			T row = parsedKey != null ? provider.findByKey(parsedKey) : null;
			onAction(target, action, parsedKey, row);
		}
	}
}
