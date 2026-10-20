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
package com.antilia.widgets.theme;

import java.util.Locale;

/**
 * A part of the components to which an {@link ILook} gives CSS classes: every element of a
 * component a style sheet draws, such as the header of a dialog or of a table. The parts are those
 * of the browser's {@code look.ts}, under their {@link #getName() names}, where the table of the
 * classes each look gives them is.
 * <p>
 * An element keeps its own classes, those of {@link Look#ANTILIA}, by which its script and its
 * style sheet find it; another look adds its classes to them. Buttons alone have none of their own:
 * their classes are those of their look. The markers of kinds and states a script toggles, such as
 * an open dropdown, are behavior, not look, and are no parts.
 * <p>
 * The parts are fixed for now; components of one's own cannot add theirs.
 *
 * @since 11.0.0
 */
public enum LookPart
{
	// buttons: their classes are those of their look alone
	BUTTON(LookAspect.BASE),
	BUTTON_PRIMARY(LookAspect.COLOR),
	BUTTON_SECONDARY(LookAspect.COLOR),
	BUTTON_GHOST(LookAspect.COLOR),
	BUTTON_DANGER(LookAspect.COLOR),
	BUTTON_SMALL(LookAspect.SIZE),
	BUTTON_MEDIUM(LookAspect.SIZE),
	BUTTON_LARGE(LookAspect.SIZE),
	BUTTON_ICON_ONLY(LookAspect.SIZE),
	// the parts of a button and a row of buttons
	BUTTON_ICON(LookAspect.BASE),
	BUTTON_LABEL(LookAspect.BASE),
	BUTTON_BAR(LookAspect.BASE),

	// dialogs: Modal, ModalDialog of Wicket's default theme
	DIALOG_THEME(LookAspect.BASE),
	DIALOG_OVERLAY(LookAspect.BASE),
	DIALOG(LookAspect.BASE),
	DIALOG_SMALL(LookAspect.SIZE),
	DIALOG_MEDIUM(LookAspect.SIZE),
	DIALOG_LARGE(LookAspect.SIZE),
	DIALOG_CONTENT(LookAspect.BASE),
	DIALOG_HEADER(LookAspect.BASE),
	DIALOG_TITLE(LookAspect.BASE),
	DIALOG_CLOSE(LookAspect.BASE),
	DIALOG_BODY(LookAspect.BASE),
	DIALOG_FOOTER(LookAspect.BASE),

	// data tables
	TABLE_HOST(LookAspect.BASE),
	TABLE(LookAspect.BASE),
	TABLE_VEIL(LookAspect.BASE),
	TABLE_HEADERS(LookAspect.BASE),
	TABLE_HEADER(LookAspect.BASE),
	TABLE_HEADER_ASCENDING(LookAspect.STATE),
	TABLE_HEADER_DESCENDING(LookAspect.STATE),
	TABLE_HEADER_UNSORTED(LookAspect.STATE),
	TABLE_SORT(LookAspect.BASE),
	TABLE_SORT_TITLE(LookAspect.BASE),
	TABLE_SORT_ARROWS(LookAspect.BASE),
	TABLE_SORT_ASCENDING(LookAspect.BASE),
	TABLE_SORT_DESCENDING(LookAspect.BASE),
	TABLE_FILLER(LookAspect.BASE),
	TABLE_SELECTION(LookAspect.BASE),
	TABLE_SELECT(LookAspect.BASE),
	TABLE_SELECT_PAGE(LookAspect.BASE),
	TABLE_BODY(LookAspect.BASE),
	TABLE_BODY_ROW(LookAspect.BASE),
	TABLE_BODY_CELL(LookAspect.BASE),
	TABLE_ROWS(LookAspect.BASE),
	TABLE_ROW_ODD(LookAspect.BASE),
	TABLE_ROW_EVEN(LookAspect.BASE),
	TABLE_ROW_SELECTED(LookAspect.STATE),
	TABLE_NAVIGATION_ROW(LookAspect.BASE),
	TABLE_NAVIGATION(LookAspect.BASE),
	TABLE_NAVIGATION_PREFIX(LookAspect.BASE),
	TABLE_PAGING(LookAspect.BASE),
	TABLE_NAVIGATOR_LABEL(LookAspect.BASE),
	TABLE_NAVIGATOR(LookAspect.BASE),
	TABLE_PAGES(LookAspect.BASE),
	TABLE_PAGE(LookAspect.BASE),
	TABLE_PAGE_LINK(LookAspect.BASE),
	TABLE_ACTIONS(LookAspect.BASE),
	TABLE_ACTION(LookAspect.BASE),
	TABLE_ICON_ACTION(LookAspect.BASE),
	TABLE_EXPORT(LookAspect.BASE),
	TABLE_PAGE_SIZE(LookAspect.BASE),
	TABLE_SELECTION_TOOLBAR(LookAspect.BASE),
	TABLE_SELECTION_COUNT(LookAspect.BASE),
	TABLE_SELECTION_CLEAR(LookAspect.BASE),
	TABLE_COLUMN_CHOOSER(LookAspect.BASE),
	TABLE_COLUMN_CHOOSER_MENU(LookAspect.BASE),
	TABLE_COLUMN_CHOOSER_COLUMN(LookAspect.BASE),
	TABLE_COLUMN_CHOOSER_TICK(LookAspect.BASE),
	TABLE_BUTTON(LookAspect.BASE),
	TABLE_BUTTON_PRIMARY(LookAspect.COLOR),
	TABLE_BUTTON_SECONDARY(LookAspect.COLOR),
	TABLE_OVERLAY(LookAspect.BASE),
	TABLE_OVERLAY_VEIL(LookAspect.BASE),
	TABLE_OVERLAY_OVER_ROWS(LookAspect.BASE),
	TABLE_OVERLAY_WINDOW(LookAspect.BASE),
	TABLE_OVERLAY_BUTTONS(LookAspect.BASE),
	TABLE_CONFIRMATION_QUESTION(LookAspect.BASE),

	// the columns of a table that move and resize
	COLUMN_MOVER(LookAspect.BASE),
	COLUMN_MOVE_GHOST(LookAspect.BASE),
	COLUMN_DROP_MARKER(LookAspect.BASE),
	COLUMN_RESIZER(LookAspect.BASE),
	COLUMN_RESIZE_GUIDE(LookAspect.BASE),
	COLUMN_RESIZE_BAND(LookAspect.BASE),
	COLUMN_RESIZE_LABEL(LookAspect.BASE),

	// dropdowns
	DROPDOWN(LookAspect.BASE),
	DROPDOWN_TOGGLE(LookAspect.BASE),
	DROPDOWN_VALUE(LookAspect.BASE),
	DROPDOWN_PLACEHOLDER(LookAspect.STATE),
	DROPDOWN_ARROW(LookAspect.BASE),
	DROPDOWN_MENU(LookAspect.BASE),
	DROPDOWN_FILTER(LookAspect.BASE),
	DROPDOWN_LIST(LookAspect.BASE),
	DROPDOWN_OPTION(LookAspect.BASE),
	DROPDOWN_OPTION_NONE(LookAspect.BASE),
	DROPDOWN_OPTION_ACTIVE(LookAspect.STATE),
	DROPDOWN_CHECK(LookAspect.BASE),
	DROPDOWN_TEXT(LookAspect.BASE),
	DROPDOWN_EMPTY(LookAspect.BASE),

	// date pickers
	DATEPICKER(LookAspect.BASE),
	DATEPICKER_FIELD(LookAspect.BASE),
	DATEPICKER_TEXT(LookAspect.BASE),
	DATEPICKER_TOGGLE(LookAspect.BASE),
	DATEPICKER_ICON(LookAspect.BASE),
	DATEPICKER_POPUP(LookAspect.BASE),
	DATEPICKER_HEADER(LookAspect.BASE),
	DATEPICKER_PREVIOUS(LookAspect.BASE),
	DATEPICKER_TITLE(LookAspect.BASE),
	DATEPICKER_NEXT(LookAspect.BASE),
	DATEPICKER_BODY(LookAspect.BASE),
	DATEPICKER_DAYS(LookAspect.BASE),
	DATEPICKER_DAY(LookAspect.BASE),
	DATEPICKER_DAY_OUTSIDE(LookAspect.STATE),
	DATEPICKER_MONTHS(LookAspect.BASE),
	DATEPICKER_MONTH(LookAspect.BASE),
	DATEPICKER_YEARS(LookAspect.BASE),
	DATEPICKER_YEAR(LookAspect.BASE),
	DATEPICKER_CLOCK(LookAspect.BASE),
	DATEPICKER_HOUR(LookAspect.BASE),
	DATEPICKER_MINUTE(LookAspect.BASE),
	DATEPICKER_COLON(LookAspect.BASE),
	DATEPICKER_PERIOD(LookAspect.BASE),
	DATEPICKER_FOOTER(LookAspect.BASE),
	DATEPICKER_TODAY(LookAspect.BASE),
	DATEPICKER_CLEAR(LookAspect.BASE),
	DATEPICKER_CLOSE(LookAspect.BASE),

	// autocompletes, and the chips of the values they pick
	AUTOCOMPLETE(LookAspect.BASE),
	AUTOCOMPLETE_FIELD(LookAspect.BASE),
	AUTOCOMPLETE_CHIPS(LookAspect.BASE),
	AUTOCOMPLETE_TEXT(LookAspect.BASE),
	AUTOCOMPLETE_MENU(LookAspect.BASE),
	AUTOCOMPLETE_LIST(LookAspect.BASE),
	AUTOCOMPLETE_OPTION(LookAspect.BASE),
	AUTOCOMPLETE_OPTION_ACTIVE(LookAspect.STATE),
	AUTOCOMPLETE_CONTENT(LookAspect.BASE),
	AUTOCOMPLETE_ACTIONS(LookAspect.BASE),
	AUTOCOMPLETE_ACTION(LookAspect.BASE),
	AUTOCOMPLETE_STATUS(LookAspect.BASE),
	AUTOCOMPLETE_TOOLTIP(LookAspect.BASE),
	CHIP(LookAspect.BASE),
	CHIP_MORE(LookAspect.BASE),
	CHIP_TEXT(LookAspect.BASE),
	CHIP_REMOVE(LookAspect.BASE),

	// the buttons copying to the clipboard
	CLIPBOARD_HOST(LookAspect.BASE),
	CLIPBOARD_COPIED(LookAspect.BASE),
	CLIPBOARD_STATUS(LookAspect.BASE),

	// infinite lists
	LIST(LookAspect.BASE),
	LIST_ITEM(LookAspect.BASE),
	LIST_STATUS(LookAspect.BASE),

	// notifications
	NOTIFICATIONS(LookAspect.BASE),
	NOTIFICATION_STACK(LookAspect.BASE),
	NOTIFICATION(LookAspect.BASE),
	NOTIFICATION_SUCCESS(LookAspect.COLOR),
	NOTIFICATION_INFO(LookAspect.COLOR),
	NOTIFICATION_WARNING(LookAspect.COLOR),
	NOTIFICATION_ERROR(LookAspect.COLOR),
	NOTIFICATION_NOTE(LookAspect.COLOR),
	NOTIFICATION_HEADER(LookAspect.BASE),
	NOTIFICATION_ICON(LookAspect.BASE),
	NOTIFICATION_TITLE(LookAspect.BASE),
	NOTIFICATION_CLOSE(LookAspect.BASE),
	NOTIFICATION_BODY(LookAspect.BASE),
	NOTIFICATION_COUNTDOWN(LookAspect.BASE),

	// palettes: two lists to move options between
	PALETTE(LookAspect.BASE),
	PALETTE_SIDE(LookAspect.BASE),
	PALETTE_AVAILABLE(LookAspect.BASE),
	PALETTE_SELECTED(LookAspect.BASE),
	PALETTE_TITLE(LookAspect.BASE),
	PALETTE_FILTER(LookAspect.BASE),
	PALETTE_LIST(LookAspect.BASE),
	PALETTE_ITEM(LookAspect.BASE),
	PALETTE_ITEM_ACTIVE(LookAspect.STATE),
	PALETTE_NUMBER(LookAspect.BASE),
	PALETTE_TEXT(LookAspect.BASE),
	PALETTE_MATCH(LookAspect.BASE),
	PALETTE_EMPTY(LookAspect.BASE),
	PALETTE_BUTTONS(LookAspect.BASE),
	PALETTE_ORDER(LookAspect.BASE),
	PALETTE_BUTTON(LookAspect.BASE),
	PALETTE_ARROW(LookAspect.BASE),
	PALETTE_GHOST(LookAspect.BASE),
	PALETTE_MARKER(LookAspect.BASE),

	// progress bars
	PROGRESS(LookAspect.BASE),
	PROGRESS_VALUE(LookAspect.BASE),
	PROGRESS_LABEL(LookAspect.BASE),

	// uploads: the picker drawn over a file input, as a button or a zone to drop files on, and the
	// progress of an upload
	FILE_PICKER(LookAspect.BASE),
	FILE_PICKER_CHOOSE(LookAspect.BASE),
	FILE_PICKER_ZONE(LookAspect.BASE),
	FILE_PICKER_ZONE_OVER(LookAspect.STATE),
	FILE_PICKER_ICON(LookAspect.BASE),
	FILE_PICKER_PROMPT(LookAspect.BASE),
	FILE_PICKER_SUMMARY(LookAspect.BASE),
	FILE_PICKER_CLEAR(LookAspect.BASE),
	FILE_PICKER_FILES(LookAspect.BASE),
	FILE_PICKER_FILE(LookAspect.BASE),
	FILE_PICKER_NAME(LookAspect.BASE),
	FILE_PICKER_SIZE(LookAspect.BASE),
	FILE_PICKER_REMOVE(LookAspect.BASE),
	FILE_PICKER_FILE_INVALID(LookAspect.STATE),
	FILE_PICKER_ZONE_INVALID(LookAspect.STATE),
	FILE_PICKER_ERROR(LookAspect.BASE),
	UPLOAD_PROGRESS(LookAspect.BASE),
	UPLOAD_STATUS(LookAspect.BASE),
	UPLOAD_STATUS_DONE(LookAspect.STATE),
	UPLOAD_STATUS_FAILED(LookAspect.STATE),

	// collapsible panels
	COLLAPSIBLE_FRAME(LookAspect.BASE),
	COLLAPSIBLE(LookAspect.BASE),
	COLLAPSIBLE_TITLE(LookAspect.BASE),
	COLLAPSIBLE_BODY(LookAspect.BASE),
	COLLAPSIBLE_ACTIONS(LookAspect.BASE),

	// floating panels
	FLOATING_PANEL(LookAspect.BASE),
	FLOATING_PANEL_HEADER(LookAspect.BASE),
	FLOATING_PANEL_TITLE(LookAspect.BASE),
	FLOATING_PANEL_BODY(LookAspect.BASE),
	FLOATING_PANEL_CLOSE(LookAspect.BASE),

	// Wicket's TabbedPanel in a style of TabsStyle, and the row of its tabs, whose links keep
	// Wicket's markup: the panel makes them anew each time it renders
	TABS(LookAspect.BASE),
	TABS_TABS(LookAspect.BASE),
	TABS_PILLS(LookAspect.BASE),
	TABS_UNDERLINE(LookAspect.BASE),
	TAB_ROW(LookAspect.BASE),
	TAB_ROW_TABS(LookAspect.BASE),
	TAB_ROW_PILLS(LookAspect.BASE),
	TAB_ROW_UNDERLINE(LookAspect.BASE),

	// Tabs, along the top of their panels or down their side, in a design of TabsDesign: tabs,
	// pills and underline (above, as TabbedPanel's) are Bootstrap's navs, the others the
	// components' own; the selected tab is marked data-selected, which the look's style sheet
	// shows as Bootstrap's active link
	TABS_SEGMENTED(LookAspect.BASE),
	TABS_FOLDER(LookAspect.BASE),
	TABS_LIFTED(LookAspect.BASE),
	TABS_SIDEBAR(LookAspect.BASE),
	TABS_STEPS(LookAspect.BASE),
	TABS_CARDS(LookAspect.BASE),
	TABS_HORIZONTAL(LookAspect.BASE),
	TABS_VERTICAL(LookAspect.BASE),
	TABS_LIST(LookAspect.BASE),
	TABS_LIST_TABS(LookAspect.BASE),
	TABS_LIST_PILLS(LookAspect.BASE),
	TABS_LIST_UNDERLINE(LookAspect.BASE),
	TAB(LookAspect.BASE),
	TAB_BUTTON(LookAspect.BASE),
	TAB_ICON(LookAspect.BASE),
	TAB_TITLE(LookAspect.BASE),
	TAB_ACTIONS(LookAspect.BASE),
	TAB_CLOSE(LookAspect.BASE),
	TAB_MARK(LookAspect.BASE),
	TAB_MODIFIED(LookAspect.STATE),
	TABS_PANELS(LookAspect.BASE),
	TABS_PANEL(LookAspect.BASE),
	TABS_CONFIRM(LookAspect.BASE),

	// popovers: a bubble pointing at its trigger, in a design of PopoverDesign and the color of a
	// variant; its header and body are Bootstrap's popover's, its arrow its own
	POPOVER(LookAspect.BASE),
	POPOVER_DEFAULT(LookAspect.COLOR),
	POPOVER_INFO(LookAspect.COLOR),
	POPOVER_SUCCESS(LookAspect.COLOR),
	POPOVER_WARNING(LookAspect.COLOR),
	POPOVER_ERROR(LookAspect.COLOR),
	POPOVER_CARD(LookAspect.BASE),
	POPOVER_SOLID(LookAspect.BASE),
	POPOVER_OUTLINE(LookAspect.BASE),
	POPOVER_GLASS(LookAspect.BASE),
	POPOVER_INVERSE(LookAspect.BASE),
	POPOVER_CALLOUT(LookAspect.BASE),
	POPOVER_AURORA(LookAspect.BASE),
	POPOVER_COMIC(LookAspect.BASE),
	POPOVER_NEON(LookAspect.BASE),
	POPOVER_ARROW(LookAspect.BASE),
	POPOVER_HEADER(LookAspect.BASE),
	POPOVER_TITLE(LookAspect.BASE),
	POPOVER_CLOSE(LookAspect.BASE),
	POPOVER_BODY(LookAspect.BASE),
	POPOVER_CONTENT(LookAspect.BASE),

	// menus: a bar of items along the top or down the side, in a design of MenuDesign, whose
	// submenus pop up or open in place; in Bootstrap's look the bar is a nav, its items nav-links,
	// a submenu a dropdown-menu and its items dropdown-items
	MENU(LookAspect.BASE),
	MENU_HORIZONTAL(LookAspect.BASE),
	MENU_VERTICAL(LookAspect.BASE),
	MENU_INLINE(LookAspect.BASE),
	MENU_BAR(LookAspect.BASE),
	MENU_PILLS(LookAspect.BASE),
	MENU_UNDERLINE(LookAspect.BASE),
	MENU_SIDEBAR(LookAspect.BASE),
	MENU_LIST(LookAspect.BASE),
	MENU_LIST_BAR(LookAspect.BASE),
	MENU_LIST_PILLS(LookAspect.BASE),
	MENU_LIST_UNDERLINE(LookAspect.BASE),
	MENU_LIST_SIDEBAR(LookAspect.BASE),
	MENU_ITEM(LookAspect.BASE),
	MENU_LINK(LookAspect.BASE),
	MENU_ICON(LookAspect.BASE),
	MENU_LABEL(LookAspect.BASE),
	MENU_SUBMENU(LookAspect.BASE),
	MENU_SUBMENU_ITEM(LookAspect.BASE),
	MENU_SUBMENU_LINK(LookAspect.BASE),
	MENU_SEPARATOR(LookAspect.BASE),
	MENU_CURRENT(LookAspect.STATE),
	MENU_CURRENT_PARENT(LookAspect.STATE),

	// trees and tree grids
	TREE(LookAspect.BASE),
	TREE_JSON(LookAspect.BASE),
	TREE_GRID(LookAspect.BASE),
	TREE_GRID_GRID(LookAspect.BASE),
	TREE_GRID_ROW(LookAspect.BASE),
	TREE_GRID_TREE_CELL(LookAspect.BASE),
	TREE_GROUP(LookAspect.BASE),
	TREE_ROOTS(LookAspect.BASE),
	TREE_ITEM(LookAspect.BASE),
	TREE_ITEM_LEAF(LookAspect.STATE),
	TREE_ITEM_SELECTED(LookAspect.STATE),
	TREE_ROW(LookAspect.BASE),
	TREE_TOGGLE(LookAspect.BASE),
	TREE_ICON(LookAspect.BASE),
	TREE_CONTENT(LookAspect.BASE),
	TREE_STATUS(LookAspect.BASE),
	TREE_NODE(LookAspect.BASE),
	TREE_NODE_LEAF(LookAspect.STATE),
	TREE_NODE_SELECTED(LookAspect.STATE),
	TREE_NODE_BUTTON(LookAspect.BASE),
	TREE_JUNCTION(LookAspect.BASE),

	// split panels
	SPLIT(LookAspect.BASE),
	SPLIT_PANE(LookAspect.BASE),
	SPLIT_FIRST(LookAspect.BASE),
	SPLIT_SECOND(LookAspect.BASE),
	SPLIT_DIVIDER(LookAspect.BASE),
	SPLIT_COLLAPSE(LookAspect.BASE),

	// the elements the user resizes, by the handle on their edge or corner
	RESIZABLE(LookAspect.BASE),
	RESIZABLE_HANDLE(LookAspect.BASE),

	// code editors: CodeMirror's markup inside is its own
	CODE_EDITOR(LookAspect.BASE),
	CODE_EDITOR_FIELD(LookAspect.BASE),

	// the preview of Markdown, and its tables
	MARKDOWN_PREVIEW(LookAspect.BASE),
	MARKDOWN_PREVIEW_TABLE(LookAspect.BASE),

	// rich text editors: Tiptap's text inside is the editor's own; their toolbars, menus, forms,
	// and the text as a page shows it
	RICH_TEXT_EDITOR(LookAspect.BASE),
	RICH_TEXT_EDITOR_FIELD(LookAspect.BASE),
	RICH_TEXT_TOOLBAR(LookAspect.BASE),
	RICH_TEXT_BUBBLE(LookAspect.BASE),
	RICH_TEXT_GROUP(LookAspect.BASE),
	RICH_TEXT_TOOL(LookAspect.BASE),
	RICH_TEXT_TOOL_ACTIVE(LookAspect.STATE),
	RICH_TEXT_MENU(LookAspect.BASE),
	RICH_TEXT_MENU_ITEM(LookAspect.BASE),
	RICH_TEXT_MENU_ITEM_ACTIVE(LookAspect.STATE),
	RICH_TEXT_MENU_SEPARATOR(LookAspect.BASE),
	RICH_TEXT_CONTENT(LookAspect.BASE),
	RICH_TEXT_STATUS(LookAspect.BASE),
	RICH_TEXT_FORM(LookAspect.BASE),
	RICH_TEXT_FORM_LABEL(LookAspect.BASE),
	RICH_TEXT_FORM_INPUT(LookAspect.BASE),
	RICH_TEXT_FORM_CHECK(LookAspect.BASE),
	RICH_TEXT_FORM_CHECKBOX(LookAspect.BASE),
	RICH_TEXT_FORM_ERROR(LookAspect.BASE),
	RICH_TEXT_FORM_BUTTONS(LookAspect.BASE),
	RICH_TEXT_VIEW(LookAspect.BASE),

	// desktops and their windows
	DESKTOP(LookAspect.BASE),
	DESKTOP_AREA(LookAspect.BASE),
	DESKTOP_TASKBAR(LookAspect.BASE),
	DESKTOP_TASKS(LookAspect.BASE),
	DESKTOP_TASK(LookAspect.BASE),
	DESKTOP_TASK_ICON(LookAspect.BASE),
	DESKTOP_TASK_TITLE(LookAspect.BASE),
	DESKTOP_TILING(LookAspect.BASE),
	DESKTOP_TILE(LookAspect.BASE),
	DESKTOP_GRIP(LookAspect.BASE),
	DESKTOP_EXPAND(LookAspect.BASE),
	DESKTOP_MENU_BUTTON(LookAspect.BASE),
	DESKTOP_MENU(LookAspect.BASE),
	DESKTOP_MENU_GROUP(LookAspect.BASE),
	DESKTOP_MENU_GROUP_HEADING(LookAspect.BASE),
	DESKTOP_MENU_ITEM(LookAspect.BASE),
	DESKTOP_LAUNCHER(LookAspect.BASE),
	DESKTOP_PANES(LookAspect.BASE),
	DESKTOP_PANE(LookAspect.BASE),
	DESKTOP_PANE_ACTION(LookAspect.BASE),
	DESKTOP_CONFIRM(LookAspect.BASE),
	DESKTOP_PROMPT(LookAspect.BASE),
	DESKTOP_PROMPT_FIELD(LookAspect.BASE),
	DESKTOP_PROMPT_INPUT(LookAspect.BASE),
	WINDOW(LookAspect.BASE),
	WINDOW_HEADER(LookAspect.BASE),
	WINDOW_ICON(LookAspect.BASE),
	WINDOW_TITLE(LookAspect.BASE),
	WINDOW_BODY(LookAspect.BASE),
	WINDOW_CONTROLS(LookAspect.BASE),
	WINDOW_CONTROL(LookAspect.BASE),
	WINDOW_HANDLE(LookAspect.BASE),

	// the slim scroll bars
	SCROLLBAR(LookAspect.BASE),
	SCROLLBAR_HOST(LookAspect.BASE),
	SCROLLBAR_TRACK(LookAspect.BASE),
	SCROLLBAR_RAIL(LookAspect.BASE),
	SCROLLBAR_THUMB(LookAspect.BASE),

	// the theme editor
	THEME_EDITOR(LookAspect.BASE),
	THEME_EDITOR_GRID(LookAspect.BASE),
	THEME_EDITOR_PROPERTY(LookAspect.BASE),
	THEME_EDITOR_PROPERTY_TEXT(LookAspect.BASE),
	THEME_EDITOR_PROPERTY_INPUT(LookAspect.BASE),
	THEME_EDITOR_FIELD(LookAspect.BASE),
	THEME_EDITOR_OUTPUT(LookAspect.BASE),
	THEME_EDITOR_CLASS(LookAspect.BASE),
	THEME_EDITOR_CSS(LookAspect.BASE),
	THEME_EDITOR_COPY(LookAspect.BASE);

	private final LookAspect aspect;

	LookPart(LookAspect aspect)
	{
		this.aspect = aspect;
	}

	/**
	 * @return what the part says about its element: what it is, its colors, its size or its state
	 */
	public LookAspect getAspect()
	{
		return aspect;
	}

	/**
	 * @return the name of the part in the browser, such as {@code dialog-header}
	 */
	public String getName()
	{
		return name().toLowerCase(Locale.ROOT).replace('_', '-');
	}
}
