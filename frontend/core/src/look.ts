/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The looks of the components: which CSS classes the parts of a component get. A theme (see
 * antilia-theme.css) gives the colors, a look the classes: the Antilia look draws the components
 * with their own style sheets, the Bootstrap look gives their parts the classes of Bootstrap too, so
 * a page with Bootstrap's style sheet shows them as Bootstrap does, in the colors of the theme. Only
 * the style sheet of Bootstrap is needed, never its script: the behavior stays the components'.
 *
 * Every element of a component a style sheet draws is a part, in the table below: a dialog's
 * header, a table's header, a dropdown's option... An element keeps its own classes, those of the
 * Antilia look, which its script and its style sheet find it by; another look adds its classes to
 * them (partClasses). Buttons alone have none of their own: their classes are those of their look
 * (lookClasses). The markers of kinds and states a script toggles, such as an open dropdown or a
 * column being dragged, are behavior, not look, and stay the components' own.
 *
 * Each part is of an aspect: what the element is (base), its colors (color), its size (size) or a
 * state it is in (state). A look can be made of others, an aspect from each: composeLook(BOOTSTRAP,
 * { color: ANTILIA, size: ANTILIA }) gives the parts Bootstrap's classes but the colors and the sizes
 * of Antilia's.
 *
 * A look is on an element, and on everything inside it, as data-antilia-look, which Wicket's
 * ThemeBehavior writes: the name of the look, then the aspects taken from other looks, as in
 * data-antilia-look="bootstrap color:antilia size:antilia". lookOf() finds it, so markup made in
 * the browser, such as the buttons of a dialog, gets the classes of the look it is in. React's
 * LookProvider and Svelte's setLook give it to the components they render.
 *
 * The table is the one of Java's LookPart and Look as well: the build writes it into
 * antilia-looks.json, which they read.
 */

/** What a part of a component says about it: what it is, its colors, its size or its state. */
export type LookAspect = 'base' | 'color' | 'size' | 'state';

/** The aspects, in the order a compound look names them. */
export const LOOK_ASPECTS: readonly LookAspect[] = Object.freeze(['base', 'color', 'size', 'state']);

/*
 * The parts: [their aspect, the classes of the Antilia look, the classes of the Bootstrap look]. The
 * classes of the Antilia look are the element's own, but for the buttons; none ('') for an element
 * whose own classes are not the components' (the links of Wicket's paging navigator for example).
 */
const PARTS = {
	// buttons: their classes are those of their look alone
	'button': ['base', 'ant-button', 'btn'],
	'button-primary': ['color', 'ant-button--primary', 'btn-primary'],
	'button-secondary': ['color', 'ant-button--secondary', 'btn-secondary'],
	'button-ghost': ['color', 'ant-button--ghost', 'btn-link'],
	'button-danger': ['color', 'ant-button--danger', 'btn-danger'],
	'button-small': ['size', 'ant-button--small', 'btn-sm'],
	'button-medium': ['size', 'ant-button--medium', ''],
	'button-large': ['size', 'ant-button--large', 'btn-lg'],
	'button-icon-only': ['size', 'ant-button--icon-only', 'ant-button--icon-only'],
	// the parts of a button and a row of buttons
	'button-icon': ['base', 'ant-button__icon', ''],
	'button-label': ['base', 'ant-button__label', ''],
	'button-bar': ['base', 'ant-button-bar', 'btn-toolbar'],

	// dialogs: Modal, ModalDialog of Wicket's default theme
	'dialog-theme': ['base', 'dialog-theme-default', ''],
	'dialog-overlay': ['base', 'modal-dialog-overlay', ''],
	'dialog': ['base', 'modal-dialog', ''],
	'dialog-small': ['size', 'modal-dialog-small', 'modal-sm'],
	'dialog-medium': ['size', 'modal-dialog-medium', ''],
	'dialog-large': ['size', 'modal-dialog-large', 'modal-lg'],
	'dialog-content': ['base', 'modal-dialog-content', 'modal-content'],
	'dialog-header': ['base', 'modal-dialog-header', 'modal-header'],
	'dialog-title': ['base', 'modal-dialog-title', 'modal-title fs-5'],
	'dialog-close': ['base', 'modal-dialog-close', 'btn-close'],
	'dialog-body': ['base', 'modal-dialog-body', 'modal-body'],
	'dialog-footer': ['base', 'modal-dialog-footer', 'modal-footer'],

	// data tables
	'table-host': ['base', 'ant-data-table-host', ''],
	'table': ['base', 'ant-data-table', 'table table-hover'],
	'table-veil': ['base', 'ant-data-table-veil', ''],
	'table-headers': ['base', 'ant-data-table-headers', ''],
	'table-header': ['base', 'ant-data-table-header', ''],
	'table-header-ascending': ['state', 'wicket_orderUp', ''],
	'table-header-descending': ['state', 'wicket_orderDown', ''],
	'table-header-unsorted': ['state', 'wicket_orderNone', ''],
	'table-sort': ['base', 'ant-data-table-sort', ''],
	'table-sort-title': ['base', 'ant-data-table-sort-title', ''],
	'table-sort-arrows': ['base', 'ant-data-table-sort-arrows', ''],
	'table-sort-ascending': ['base', 'ant-data-table-sort-asc', ''],
	'table-sort-descending': ['base', 'ant-data-table-sort-desc', ''],
	'table-filler': ['base', 'ant-data-table-filler', ''],
	'table-selection': ['base', 'ant-data-table-selection', ''],
	'table-select': ['base', 'ant-data-table-select', 'form-check-input'],
	'table-select-page': ['base', 'ant-data-table-select-page', 'form-check-input'],
	'table-body': ['base', 'ant-data-table-body', ''],
	'table-body-row': ['base', 'ant-data-table-body-row', ''],
	'table-body-cell': ['base', 'ant-data-table-body-cell', ''],
	'table-rows': ['base', 'ant-data-table-rows', ''],
	'table-row-odd': ['base', 'odd', ''],
	'table-row-even': ['base', 'even', ''],
	'table-row-selected': ['state', 'selected', 'table-active'],
	'table-navigation-row': ['base', 'navigation', ''],
	'table-navigation': ['base', 'ant-data-table-navigation', ''],
	'table-navigation-prefix': ['base', 'ant-data-table-navigation-prefix', ''],
	'table-paging': ['base', 'ant-data-table-paging', ''],
	'table-navigator-label': ['base', 'navigatorLabel', 'text-body-secondary'],
	'table-navigator': ['base', 'navigator', ''],
	'table-pages': ['base', '', 'pagination pagination-sm'],
	'table-page': ['base', 'goto', 'page-item'],
	'table-page-link': ['base', '', 'page-link'],
	'table-actions': ['base', 'ant-data-table-actions', ''],
	'table-action': ['base', 'ant-data-table-action', 'btn btn-sm'],
	'table-icon-action': ['base', 'ant-data-table-icon-action', ''],
	'table-export': ['base', 'ant-data-table-export-csv', ''],
	'table-page-size': ['base', 'ant-data-table-page-size', ''],
	'table-selection-toolbar': ['base', 'ant-data-table-selection-toolbar', ''],
	'table-selection-count': ['base', 'ant-data-table-selection-count', ''],
	'table-selection-clear': ['base', 'ant-data-table-selection-clear', ''],
	'table-column-chooser': ['base', 'ant-data-table-column-chooser', 'dropdown'],
	'table-column-chooser-menu': ['base', 'ant-data-table-column-chooser-menu', 'dropdown-menu'],
	'table-column-chooser-column': ['base', 'ant-data-table-column-chooser-column', 'dropdown-item'],
	'table-column-chooser-tick': ['base', 'ant-data-table-column-chooser-tick', ''],
	'table-button': ['base', 'ant-data-table-button', 'btn btn-sm'],
	'table-button-primary': ['color', 'ant-data-table-button-primary', 'btn-primary'],
	'table-button-secondary': ['color', '', 'btn-secondary'],
	'table-overlay': ['base', 'ant-data-table-overlay', ''],
	'table-overlay-veil': ['base', 'ant-data-table-overlay-veil', ''],
	'table-overlay-over-rows': ['base', 'ant-data-table-overlay-body', ''],
	'table-overlay-window': ['base', 'ant-data-table-overlay-window', 'modal-content'],
	'table-overlay-buttons': ['base', 'ant-data-table-overlay-buttons', 'modal-footer'],
	'table-confirmation-question': ['base', 'ant-data-table-confirmation-question', ''],

	// the columns of a table that move and resize
	'column-mover': ['base', 'antilia-col-mover', ''],
	'column-move-ghost': ['base', 'antilia-col-move-ghost', 'shadow'],
	'column-drop-marker': ['base', 'antilia-col-drop-marker', ''],
	'column-resizer': ['base', 'antilia-col-resizer', ''],
	'column-resize-guide': ['base', 'antilia-col-resize-guide', ''],
	'column-resize-band': ['base', 'antilia-col-resize-band', ''],
	'column-resize-label': ['base', 'antilia-col-resize-label', 'badge text-bg-dark'],

	// dropdowns
	'dropdown': ['base', 'antilia-dropdown', ''],
	'dropdown-toggle': ['base', 'antilia-dropdown-toggle', 'form-select'],
	'dropdown-value': ['base', 'antilia-dropdown-value', ''],
	'dropdown-placeholder': ['state', 'antilia-dropdown-placeholder', 'text-body-secondary'],
	'dropdown-arrow': ['base', 'antilia-dropdown-arrow', ''],
	'dropdown-menu': ['base', 'antilia-dropdown-menu', 'dropdown-menu'],
	'dropdown-filter': ['base', 'antilia-dropdown-filter', 'form-control'],
	'dropdown-list': ['base', 'antilia-dropdown-list', ''],
	'dropdown-option': ['base', 'antilia-dropdown-option', 'dropdown-item'],
	'dropdown-option-none': ['base', 'antilia-dropdown-option-none', ''],
	'dropdown-option-active': ['state', 'antilia-dropdown-active', ''],
	'dropdown-check': ['base', 'antilia-dropdown-check', ''],
	'dropdown-text': ['base', 'antilia-dropdown-text', ''],
	'dropdown-empty': ['base', 'antilia-dropdown-empty', 'dropdown-item-text'],

	// date pickers
	'datepicker': ['base', 'antilia-datepicker', ''],
	'datepicker-field': ['base', 'antilia-datepicker-field', 'input-group'],
	'datepicker-text': ['base', 'antilia-datepicker-text', 'form-control'],
	'datepicker-toggle': ['base', 'antilia-datepicker-toggle', 'btn btn-outline-secondary'],
	'datepicker-icon': ['base', 'antilia-datepicker-icon', ''],
	'datepicker-popup': ['base', 'antilia-datepicker-popup', 'dropdown-menu'],
	'datepicker-header': ['base', 'antilia-datepicker-header', ''],
	'datepicker-previous': ['base', 'antilia-datepicker-previous', 'btn btn-sm'],
	'datepicker-title': ['base', 'antilia-datepicker-title', 'btn btn-sm'],
	'datepicker-next': ['base', 'antilia-datepicker-next', 'btn btn-sm'],
	'datepicker-body': ['base', 'antilia-datepicker-body', ''],
	'datepicker-days': ['base', 'antilia-datepicker-days', ''],
	'datepicker-day': ['base', 'antilia-datepicker-day', 'btn btn-sm'],
	'datepicker-day-outside': ['state', 'antilia-datepicker-outside', 'text-body-tertiary'],
	'datepicker-months': ['base', 'antilia-datepicker-months', ''],
	'datepicker-month': ['base', 'antilia-datepicker-month', 'btn btn-sm'],
	'datepicker-years': ['base', 'antilia-datepicker-years', ''],
	'datepicker-year': ['base', 'antilia-datepicker-year', 'btn btn-sm'],
	'datepicker-clock': ['base', 'antilia-datepicker-clock', ''],
	'datepicker-hour': ['base', 'antilia-datepicker-hour', 'form-control form-control-sm'],
	'datepicker-minute': ['base', 'antilia-datepicker-minute', 'form-control form-control-sm'],
	'datepicker-colon': ['base', 'antilia-datepicker-colon', ''],
	'datepicker-period': ['base', 'antilia-datepicker-period', 'btn btn-sm btn-outline-secondary'],
	'datepicker-footer': ['base', 'antilia-datepicker-footer', ''],
	'datepicker-today': ['base', 'antilia-datepicker-today', 'btn btn-sm btn-link'],
	'datepicker-clear': ['base', 'antilia-datepicker-clear', 'btn btn-sm btn-link'],
	'datepicker-close': ['base', 'antilia-datepicker-close', 'btn btn-sm btn-link'],

	// autocompletes, and the chips of the values they pick
	'autocomplete': ['base', 'antilia-autocomplete', ''],
	'autocomplete-field': ['base', 'antilia-autocomplete-field', 'form-control'],
	'autocomplete-chips': ['base', 'antilia-autocomplete-chips', ''],
	'autocomplete-text': ['base', 'antilia-autocomplete-text', ''],
	'autocomplete-menu': ['base', 'antilia-autocomplete-menu', 'dropdown-menu'],
	'autocomplete-list': ['base', 'antilia-autocomplete-list', ''],
	'autocomplete-option': ['base', 'antilia-autocomplete-option', 'dropdown-item'],
	'autocomplete-option-active': ['state', 'antilia-autocomplete-active', ''],
	'autocomplete-content': ['base', 'antilia-autocomplete-content', ''],
	'autocomplete-actions': ['base', 'antilia-autocomplete-actions', ''],
	'autocomplete-action': ['base', 'antilia-autocomplete-action', ''],
	'autocomplete-status': ['base', 'antilia-autocomplete-status', 'dropdown-item-text'],
	'autocomplete-tooltip': ['base', 'antilia-autocomplete-tooltip', ''],
	'chip': ['base', 'antilia-chip', 'badge rounded-pill'],
	'chip-more': ['base', 'antilia-chip-more', ''],
	'chip-text': ['base', 'antilia-chip-text', ''],
	'chip-remove': ['base', 'antilia-chip-remove', ''],

	// the buttons copying to the clipboard
	'clipboard-host': ['base', 'antilia-clipboard-host', ''],
	'clipboard-copied': ['base', 'antilia-clipboard-copied', ''],
	'clipboard-status': ['base', 'antilia-clipboard-status', ''],

	// infinite lists
	'list': ['base', 'json-infinite-list', ''],
	'list-item': ['base', 'json-infinite-list-item', ''],
	'list-status': ['base', 'json-infinite-list-status', 'text-body-secondary'],

	// notifications
	'notifications': ['base', 'antilia-notifications', ''],
	'notification-stack': ['base', 'antilia-notifications-stack', ''],
	'notification': ['base', 'antilia-notification', 'alert'],
	'notification-success': ['color', 'antilia-notification-success', 'alert-success'],
	'notification-info': ['color', 'antilia-notification-info', 'alert-info'],
	'notification-warning': ['color', 'antilia-notification-warning', 'alert-warning'],
	'notification-error': ['color', 'antilia-notification-error', 'alert-danger'],
	'notification-note': ['color', 'antilia-notification-note', 'alert-secondary'],
	'notification-header': ['base', 'antilia-notification-header', ''],
	'notification-icon': ['base', 'antilia-notification-icon', ''],
	'notification-title': ['base', 'antilia-notification-title', 'alert-heading'],
	'notification-close': ['base', 'antilia-notification-close', 'btn-close'],
	'notification-body': ['base', 'antilia-notification-body', ''],
	'notification-countdown': ['base', 'antilia-notification-countdown', ''],

	// palettes: two lists to move options between
	'palette': ['base', 'antilia-palette', ''],
	'palette-side': ['base', 'antilia-palette-side', 'card'],
	'palette-available': ['base', 'antilia-palette-available', ''],
	'palette-selected': ['base', 'antilia-palette-selected', ''],
	'palette-title': ['base', 'antilia-palette-title', 'card-header'],
	'palette-filter': ['base', 'antilia-palette-filter', 'form-control'],
	'palette-list': ['base', 'antilia-palette-list', 'list-group list-group-flush'],
	'palette-item': ['base', 'antilia-palette-item', 'list-group-item list-group-item-action'],
	'palette-item-active': ['state', 'antilia-palette-active', 'active'],
	'palette-number': ['base', 'antilia-palette-number', ''],
	'palette-text': ['base', 'antilia-palette-text', ''],
	'palette-match': ['base', 'antilia-palette-match', 'mark'],
	'palette-empty': ['base', 'antilia-palette-empty', 'text-body-secondary'],
	'palette-buttons': ['base', 'antilia-palette-buttons', ''],
	'palette-order': ['base', 'antilia-palette-order', ''],
	'palette-button': ['base', 'antilia-palette-button', 'btn btn-outline-secondary'],
	'palette-arrow': ['base', 'antilia-palette-arrow', ''],
	'palette-ghost': ['base', 'antilia-palette-ghost', 'shadow'],
	'palette-marker': ['base', 'antilia-palette-marker', ''],

	// progress bars
	'progress': ['base', 'antilia-progress-bar', 'progress'],
	'progress-value': ['base', 'antilia-progress-bar-value', ''],
	'progress-label': ['base', 'antilia-progress-bar-label', ''],

	// collapsible panels
	'collapsible-frame': ['base', 'antilia-collapsible-frame', 'card'],
	'collapsible': ['base', 'antilia-collapsible', ''],
	'collapsible-title': ['base', 'antilia-collapsible-title', 'card-header'],
	'collapsible-body': ['base', 'antilia-collapsible-body', 'card-body'],
	'collapsible-actions': ['base', 'antilia-collapsible-actions', ''],

	// floating panels
	'floating-panel': ['base', 'antilia-floating-panel', 'card'],
	'floating-panel-header': ['base', 'antilia-floating-panel-header', 'card-header'],
	'floating-panel-title': ['base', 'antilia-floating-panel-title', ''],
	'floating-panel-body': ['base', 'antilia-floating-panel-body', 'card-body'],
	'floating-panel-close': ['base', 'antilia-floating-panel-close', 'btn-close'],

	// Wicket's TabbedPanel in a style of TabsStyle, and the row of its tabs, whose links keep
	// Wicket's markup: the panel makes them anew each time it renders
	'tabs': ['base', 'antilia-tabs', ''],
	'tabs-tabs': ['base', 'antilia-tabs-tabs', ''],
	'tabs-pills': ['base', 'antilia-tabs-pills', ''],
	'tabs-underline': ['base', 'antilia-tabs-underline', ''],
	'tab-row': ['base', 'tab-row', 'nav'],
	'tab-row-tabs': ['base', '', 'nav-tabs'],
	'tab-row-pills': ['base', '', 'nav-pills'],
	'tab-row-underline': ['base', '', 'nav-underline'],

	// Tabs, along the top of their panels or down their side, in a design of TabsDesign: tabs,
	// pills and underline (above, as TabbedPanel's) are Bootstrap's navs, the others the
	// components' own; the selected tab is marked data-selected, which the look's style sheet
	// shows as Bootstrap's active link
	'tabs-segmented': ['base', 'antilia-tabs-segmented', ''],
	'tabs-folder': ['base', 'antilia-tabs-folder', ''],
	'tabs-lifted': ['base', 'antilia-tabs-lifted', ''],
	'tabs-sidebar': ['base', 'antilia-tabs-sidebar', ''],
	'tabs-steps': ['base', 'antilia-tabs-steps', ''],
	'tabs-cards': ['base', 'antilia-tabs-cards', ''],
	'tabs-horizontal': ['base', 'antilia-tabs-horizontal', ''],
	'tabs-vertical': ['base', 'antilia-tabs-vertical', ''],
	'tabs-list': ['base', 'antilia-tabs-list', ''],
	'tabs-list-tabs': ['base', '', 'nav nav-tabs'],
	'tabs-list-pills': ['base', '', 'nav nav-pills'],
	'tabs-list-underline': ['base', '', 'nav nav-underline'],
	'tab': ['base', 'antilia-tab', 'nav-item'],
	'tab-button': ['base', 'antilia-tab-button', ''],
	'tab-icon': ['base', 'antilia-tab-icon', ''],
	'tab-title': ['base', 'antilia-tab-title', ''],
	'tab-actions': ['base', 'antilia-tab-actions', ''],
	'tab-close': ['base', 'antilia-tab-close', 'btn-close'],
	'tab-mark': ['base', 'antilia-tab-mark', ''],
	'tab-modified': ['state', 'antilia-tab-modified', ''],
	'tabs-panels': ['base', 'antilia-tabs-panels', ''],
	'tabs-panel': ['base', 'antilia-tabs-panel', ''],
	'tabs-confirm': ['base', 'antilia-tabs-confirm', ''],

	// popovers: a bubble pointing at its trigger, in a design of PopoverDesign and the color of a
	// variant; its header and body are Bootstrap's popover's, its arrow its own
	'popover': ['base', 'antilia-popover', ''],
	'popover-default': ['color', 'antilia-popover-default', ''],
	'popover-info': ['color', 'antilia-popover-info', ''],
	'popover-success': ['color', 'antilia-popover-success', ''],
	'popover-warning': ['color', 'antilia-popover-warning', ''],
	'popover-error': ['color', 'antilia-popover-error', ''],
	'popover-card': ['base', 'antilia-popover-card', ''],
	'popover-solid': ['base', 'antilia-popover-solid', ''],
	'popover-outline': ['base', 'antilia-popover-outline', ''],
	'popover-glass': ['base', 'antilia-popover-glass', ''],
	'popover-inverse': ['base', 'antilia-popover-inverse', ''],
	'popover-callout': ['base', 'antilia-popover-callout', ''],
	'popover-aurora': ['base', 'antilia-popover-aurora', ''],
	'popover-comic': ['base', 'antilia-popover-comic', ''],
	'popover-neon': ['base', 'antilia-popover-neon', ''],
	'popover-arrow': ['base', 'antilia-popover-arrow', ''],
	'popover-header': ['base', 'antilia-popover-header', 'popover-header'],
	'popover-title': ['base', 'antilia-popover-title', ''],
	'popover-close': ['base', 'antilia-popover-close', 'btn-close'],
	'popover-body': ['base', 'antilia-popover-body', 'popover-body'],
	'popover-content': ['base', 'antilia-popover-content', ''],

	// trees and tree grids
	'tree': ['base', 'antilia-tree', ''],
	'tree-json': ['base', 'json-tree', ''],
	'tree-grid': ['base', 'antilia-tree-grid', ''],
	'tree-grid-grid': ['base', 'antilia-tree-grid-grid', 'table table-sm table-hover'],
	'tree-grid-row': ['base', 'antilia-tree-grid-row', ''],
	'tree-grid-tree-cell': ['base', 'antilia-tree-grid-tree-cell', ''],
	'tree-group': ['base', 'antilia-tree-group', ''],
	'tree-roots': ['base', 'antilia-tree-roots', ''],
	'tree-item': ['base', 'antilia-tree-item', ''],
	'tree-item-leaf': ['state', 'antilia-tree-item--leaf', ''],
	'tree-item-selected': ['state', 'antilia-tree-item--selected', ''],
	'tree-row': ['base', 'antilia-tree-row', ''],
	'tree-toggle': ['base', 'antilia-tree-toggle', ''],
	'tree-icon': ['base', 'antilia-tree-icon', ''],
	'tree-content': ['base', 'antilia-tree-content', ''],
	'tree-status': ['base', 'antilia-tree-status', 'text-body-secondary'],
	'tree-node': ['base', 'antilia-tree-node', ''],
	'tree-node-leaf': ['state', 'antilia-tree-node--leaf', ''],
	'tree-node-selected': ['state', 'antilia-tree-node--selected', ''],
	'tree-node-button': ['base', 'antilia-tree-node-button', ''],
	'tree-junction': ['base', 'antilia-tree-junction', ''],

	// split panels
	'split': ['base', 'antilia-split', ''],
	'split-pane': ['base', 'antilia-split-pane', ''],
	'split-first': ['base', 'antilia-split-first', ''],
	'split-second': ['base', 'antilia-split-second', ''],
	'split-divider': ['base', 'antilia-split-divider', ''],
	'split-collapse': ['base', 'antilia-split-collapse', ''],

	// desktops and their windows
	'desktop': ['base', 'antilia-desktop', ''],
	'desktop-area': ['base', 'antilia-desktop-area', ''],
	'desktop-taskbar': ['base', 'antilia-desktop-taskbar', ''],
	'desktop-tasks': ['base', 'antilia-desktop-tasks', ''],
	'desktop-task': ['base', 'antilia-desktop-task', ''],
	'desktop-task-icon': ['base', 'antilia-desktop-task-icon', ''],
	'desktop-task-title': ['base', 'antilia-desktop-task-title', ''],
	'desktop-tiling': ['base', 'antilia-desktop-tiling', ''],
	'desktop-tile': ['base', 'antilia-desktop-tile', ''],
	'desktop-grip': ['base', 'antilia-desktop-grip', ''],
	'desktop-expand': ['base', 'antilia-desktop-expand', ''],
	'desktop-menu-button': ['base', 'antilia-desktop-menu-button', ''],
	'desktop-menu': ['base', 'antilia-desktop-menu', 'dropdown-menu'],
	'desktop-menu-group': ['base', 'antilia-desktop-menu-group', ''],
	'desktop-menu-group-heading': ['base', 'antilia-desktop-menu-group-heading', 'dropdown-header'],
	'desktop-menu-item': ['base', 'antilia-desktop-menu-item', 'dropdown-item'],
	'desktop-launcher': ['base', 'antilia-desktop-launcher', ''],
	'desktop-panes': ['base', 'antilia-desktop-panes', ''],
	'desktop-pane': ['base', 'antilia-desktop-pane', ''],
	'desktop-pane-action': ['base', 'antilia-desktop-pane-action', ''],
	'desktop-confirm': ['base', 'antilia-desktop-confirm', ''],
	'desktop-prompt': ['base', 'antilia-desktop-prompt', ''],
	'desktop-prompt-field': ['base', 'antilia-desktop-prompt-field', ''],
	'desktop-prompt-input': ['base', 'antilia-desktop-prompt-input', 'form-control'],
	'window': ['base', 'antilia-window', 'card'],
	'window-header': ['base', 'antilia-window-header', 'card-header'],
	'window-icon': ['base', 'antilia-window-icon', ''],
	'window-title': ['base', 'antilia-window-title', ''],
	'window-body': ['base', 'antilia-window-body', ''],
	'window-controls': ['base', 'antilia-window-controls', ''],
	'window-control': ['base', 'antilia-window-control', ''],
	'window-handle': ['base', 'antilia-window-handle', ''],

	// the slim scroll bars
	'scrollbar': ['base', 'antilia-scrollbar', ''],
	'scrollbar-host': ['base', 'antilia-scrollbar-host', ''],
	'scrollbar-track': ['base', 'antilia-scrollbar-track', ''],
	'scrollbar-rail': ['base', 'antilia-scrollbar-rail', ''],
	'scrollbar-thumb': ['base', 'antilia-scrollbar-thumb', ''],

	// the theme editor
	'theme-editor': ['base', 'theme-editor-properties', ''],
	'theme-editor-grid': ['base', 'theme-editor-grid', ''],
	'theme-editor-property': ['base', 'theme-editor-property', ''],
	'theme-editor-property-text': ['base', 'theme-editor-property-text', ''],
	'theme-editor-property-input': ['base', 'theme-editor-property-input', ''],
	'theme-editor-field': ['base', '', 'form-control'],
	'theme-editor-output': ['base', 'theme-editor-output', ''],
	'theme-editor-class': ['base', 'theme-editor-class', 'form-control'],
	'theme-editor-css': ['base', 'theme-editor-css', 'form-control font-monospace'],
	'theme-editor-copy': ['base', 'theme-editor-copy', 'btn btn-secondary'],

	// the note standing for a component a license key has not activated
	'unlicensed': ['base', 'antilia-unlicensed', 'alert alert-warning']
} as const satisfies Record<string, readonly [LookAspect, string, string]>;

/** The parts of the components a look gives classes to. */
export type LookPart = keyof typeof PARTS;

/** The aspect of each part. */
export const LOOK_PARTS: Readonly<Record<LookPart, LookAspect>> = Object.freeze(
	Object.fromEntries(Object.entries(PARTS).map(([part, [aspect]]) => [part, aspect])) as
		Record<LookPart, LookAspect>);

/** A look: its name, as data-antilia-look has it, and the classes of the parts. */
export interface Look {
	readonly name: string;
	/** The classes of each part, separated by spaces; none for a part missing or empty. */
	readonly classes: Readonly<Partial<Record<LookPart, string>>>;
}

/** The attribute naming the look of an element and of everything inside it. */
export const LOOK_ATTRIBUTE = 'data-antilia-look';

function column(name: string, index: 1 | 2): Look {
	return Object.freeze({
		name,
		classes: Object.freeze(Object.fromEntries(Object.entries(PARTS)
			.filter(([, row]) => row[index])
			.map(([part, row]) => [part, row[index]])))
	});
}

/** The look of the components' own style sheets, the default: the elements' own classes. */
export const ANTILIA_LOOK: Look = column('antilia', 1);

/**
 * The look of Bootstrap 5.3: the classes of its buttons, cards, dialogs, tables, menus and fields.
 * antilia-look-bootstrap.css gives them the colors of the theme and keeps what Bootstrap has not,
 * such as the square buttons showing only an icon.
 */
export const BOOTSTRAP_LOOK: Look = column('bootstrap', 2);

const looks = new Map<string, Look>([ANTILIA_LOOK, BOOTSTRAP_LOOK].map((look) => [look.name, look]));

const compounds = new Map<string, Look>();

/**
 * Makes a look of one's own known to lookOf(), under its name; it replaces a look of that name. The
 * name is a word: no spaces, no colons.
 */
export function registerLook(look: Look): void {
	looks.set(look.name, look);
	compounds.clear();
}

/**
 * @returns a look made of others: the parts of an aspect have the classes of the look given for
 * it, the others those of the base; named as data-antilia-look names it, "bootstrap color:antilia"
 * for example. The looks are simple ones, not made of others themselves.
 */
export function composeLook(base: Look, aspects: Partial<Record<LookAspect, Look>>): Look {
	const taken = LOOK_ASPECTS.filter((aspect) => aspects[aspect] && aspects[aspect] !== base);
	if (!taken.length) {
		return base;
	}
	const classes: Partial<Record<LookPart, string>> = {};
	for (const part of Object.keys(PARTS) as LookPart[]) {
		const css = (aspects[LOOK_PARTS[part]] ?? base).classes[part];
		if (css) {
			classes[part] = css;
		}
	}
	return Object.freeze({
		name: [base.name, ...taken.map((aspect) => aspect + ':' + aspects[aspect]!.name)].join(' '),
		classes: Object.freeze(classes)
	});
}

/**
 * @returns the look of the name, the Antilia look for none or a name not known; a name such as
 * "bootstrap color:antilia" makes Bootstrap's look with the colors of Antilia's
 */
export function lookNamed(name: string | null | undefined): Look {
	const words = name?.trim().split(/\s+/) ?? [];
	const base = (words[0] && looks.get(words[0])) || ANTILIA_LOOK;
	if (words.length < 2) {
		return base;
	}
	const key = words.join(' ');
	let look = compounds.get(key);
	if (!look) {
		const aspects: Partial<Record<LookAspect, Look>> = {};
		for (const word of words.slice(1)) {
			const [aspect, other] = word.split(':');
			const known = other ? looks.get(other) : undefined;
			if (known && LOOK_ASPECTS.includes(aspect as LookAspect)) {
				aspects[aspect as LookAspect] = known;
			}
		}
		look = composeLook(base, aspects);
		compounds.set(key, look);
	}
	return look;
}

/**
 * @returns the look an element is in: that of the closest element with data-antilia-look, itself
 * included; the Antilia look for none
 */
export function lookOf(element: Element | null | undefined): Look {
	return lookNamed(element?.closest(`[${LOOK_ATTRIBUTE}]`)?.getAttribute(LOOK_ATTRIBUTE));
}

type Parts = (LookPart | false | null | undefined)[];

function distinct(classes: (string | undefined)[]): string {
	return [...new Set(classes.flatMap((css) => css ? css.split(' ') : []))].join(' ');
}

/**
 * @returns the classes of the parts in a look, in their order, separated by spaces, each once and
 * without the parts the look gives no class: the classes of a button
 */
export function lookClasses(look: Look, ...parts: Parts): string {
	return distinct(parts.map((part) => (part ? look.classes[part] : undefined)));
}

/**
 * @returns the classes of an element that is the parts: its own, those of the Antilia look, then
 * those the look adds, separated by spaces and each once
 */
export function partClasses(look: Look, ...parts: Parts): string {
	return distinct(parts.flatMap((part) => (part ? [ANTILIA_LOOK.classes[part],
		look.classes[part]] : [])));
}

/**
 * Gives an element the classes of parts in a look, or takes them away: for a state such as the
 * active option of a list.
 */
export function toggleParts(element: Element, look: Look, on: boolean, ...parts: Parts): void {
	const classes = partClasses(look, ...parts);
	if (classes) {
		element.classList[on ? 'add' : 'remove'](...classes.split(' '));
	}
}
