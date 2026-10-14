/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

export { ACTION_EVENT, Autocomplete } from './autocomplete.js';
export type { AutocompleteAction, AutocompleteActionDetail, AutocompleteOptions,
	AutocompleteSuggestion, AutocompleteValue } from './autocomplete.js';
export { Chips } from './chips.js';
export { Clipboard } from './clipboard.js';
export { DynamicDataTable } from './data-table.js';
export type { Bars, DataTableColumn, DataTableConfig, DataTableData, DataTableLayout, DataTableMessage,
	DataTableRow, ResizableColumnsRegistry, Veil } from './data-table.js';
export { CLEAR_SELECTION_ICON, DATA_TABLE_COMPONENTS, DATA_TABLE_TEXTS, localRows, PAGE_LINKS,
	pageLinks, SELECTION_ACTIONS, selectionColumn, SHOW_COLUMNS } from './data-table-model.js';
export type { DataTableColumnDef, DataTableFilter, DataTablePush, DataTableQuery, DataTableRows,
	DataTableSort, DataTableTexts } from './data-table-model.js';
export { FloatingPanel } from './floating-panel.js';
export { handlebarsEngine, registerHandlebars } from './handlebars.js';
export type { HandlebarsLike } from './handlebars.js';
export { MovableColumns, headerCells } from './movable-columns.js';
export type { MovableColumnsOptions } from './movable-columns.js';
export { ResizableColumns } from './resizable-columns.js';
export type { ResizableColumnsOptions, ResizeMode } from './resizable-columns.js';
export { getHost, onRemoving, setHost, standaloneHost } from './host.js';
export type { Host, Unsubscribe } from './host.js';
export { DateFormat, DatePicker, datePicker, daysInMonth, firstDayOfWeek, formatIso,
	parseIso } from './date-picker.js';
export type { DateParts, DatePickerOptions } from './date-picker.js';
export { DEFAULT_DESKTOP_TEXTS, DEFAULT_PANE, Desktop, desktop, DESKTOP_CLASSES, DESKTOP_COMPONENTS,
	DESKTOP_DISCARD_EVENT,
	WINDOW_HEIGHT_PROPERTY, WINDOW_WIDTH_PROPERTY, WINDOW_X_PROPERTY, WINDOW_Y_PROPERTY, WINDOW_Z_PROPERTY } from './desktop.js';
export type { DesktopOptions, DesktopPane, DesktopTexts, TaskbarPosition, TileLayout, WindowState,
	WindowType } from './desktop.js';
export { DEFAULT_FILTER_THRESHOLD, Dropdown, dropdown } from './dropdown.js';
export type { DropdownOptions } from './dropdown.js';
export { Menu, menu, MENU_CLASSES, MENU_DESIGN_CLASSES, MENU_DESIGNS, MENU_MAX_HEIGHT_PROPERTY,
	MENU_ORIENTATION_CLASSES, MENU_X_PROPERTY, MENU_Y_PROPERTY } from './menu.js';
export type { MenuDesign, MenuOptions, MenuOrientation } from './menu.js';
export { insertAt, moveBy, Palette, palette, PALETTE_FILTER_THRESHOLD,
	PALETTE_MATCHERS } from './palette.js';
export type { PaletteChoice, PaletteMatch, PaletteMatcher, PaletteOptions,
	PaletteTexts } from './palette.js';
export { Popover, popoverClasses, POPOVER_ARROW_PROPERTY, POPOVER_CLASSES, POPOVER_DESIGNS, POPOVER_PLACEMENTS,
	POPOVER_VARIANTS, POPOVER_X_PROPERTY, POPOVER_Y_PROPERTY, THEME_PROPERTIES } from './popover.js';
export type { PopoverDesign, PopoverOptions, PopoverPlacement, PopoverSide,
	PopoverVariant } from './popover.js';
export { Scrollbar, scrollbar, SCROLLBAR_WRAPPER_CLASS } from './scrollbar.js';
export type { ScrollbarHandle, ScrollbarOptions, ScrollPosition } from './scrollbar.js';
export { JsonInfiniteList } from './infinite-list.js';
export { activate, activates, isLicensed, LICENSE_PREFIX, PUBLIC_KEY, UNLICENSED_CLASS,
	verifyLicense } from './license.js';
export type { License } from './license.js';
export type { InfiniteListConfig, InfiniteListData, InfiniteListEntry, InfiniteListMessage } from './infinite-list.js';
export { CLOSE_ATTRIBUTE, focusableIn, isFieldModified, isModified, MODAL_CLASSES, MODAL_SIZES,
	MODAL_THEME_CLASS, ModalDialog, modalSizeClass, resetField } from './modal.js';
export type { ModalOptions, ModalSize } from './modal.js';
export { NOTIFICATION_CLASSES, NOTIFICATION_ICONS, NOTIFICATION_LEAVE_MS, NOTIFICATION_POSITIONS,
	NOTIFICATION_TIMEOUT_PROPERTY, NOTIFICATION_TYPES, Notifications } from './notifications.js';
export type { NotificationCloseReason, NotificationHandle, NotificationOptions, NotificationPosition,
	NotificationsOptions, NotificationType } from './notifications.js';
export { parseSplitSize, SPLIT_FIRST_MAX_PROPERTY, SPLIT_FIRST_MIN_PROPERTY, SPLIT_FIRST_PROPERTY,
	SPLIT_FIRST_SIZE_PROPERTY, SPLIT_SECOND_MIN_PROPERTY, SPLIT_SECOND_PROPERTY, SplitPanel,
	splitPanel } from './split-panel.js';
export type { SplitOrientation, SplitPane, SplitPanelOptions, SplitSize } from './split-panel.js';
export { DEFAULT_TABS_TEXTS, Tabs, tabs, TABS_CLASSES, TABS_DISCARD_EVENT, TABS_DESIGN_CLASSES, TABS_DESIGNS,
	TABS_ORIENTATION_CLASSES } from './tabs.js';
export type { TabsDesign, TabsOptions, TabsOrientation, TabsTexts } from './tabs.js';
export { askToDiscard, MODIFIED_ATTRIBUTE, modifiedFields } from './unsaved.js';
export type { DiscardQuestion, DiscardTexts } from './unsaved.js';
export { Templates, templates, useTemplates } from './templates.js';
export { JsonTree, TREE_COMPONENTS, TREE_ICONS, TREE_GRID_COMPONENTS, TREE_TEXTS } from './tree.js';
export type { TreeCallbacks, TreeColumn, TreeColumnsRegistry, TreeConfig, TreeIcons, TreeNode,
	TreeTexts } from './tree.js';
export type { CompiledTemplate, TemplateEngine, TemplateRegistry } from './templates.js';
export { BUTTON_SIZES, BUTTON_VARIANTS, buttonClasses } from './button.js';
export type { ButtonSize, ButtonVariant } from './button.js';
export { ANTILIA_LOOK, BOOTSTRAP_LOOK, composeLook, LOOK_ASPECTS, LOOK_ATTRIBUTE, LOOK_PARTS,
	lookClasses, lookNamed, lookOf, partClasses, registerLook, toggleParts } from './look.js';
export type { Look, LookAspect, LookPart } from './look.js';
