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

/*
 * A desktop: floating windows that are not modal, many at once, as on the desktop of an operating
 * system. A click in a window brings it to the front, the others staying behind as they were.
 * Dragging the title bar moves a window, with the mouse, a pen or a finger, and a double click on
 * it folds the window to its title bar and unfolds it. The buttons of the title bar minimize a
 * window, maximize it to the whole desktop and restore it, and close it, as each window allows;
 * a resizable window has handles on its edges and corners. While the focus is in a window, F11
 * maximizes it and restores it, and Ctrl+F6 and Ctrl+Shift+F6 go round the windows, as do
 * Ctrl+Tab and Ctrl+Shift+Tab where the browser lets the page have them.
 *
 * The taskbar, below the desktop or on one of its other sides, has a button for each window that is
 * not closed: it brings the window to the front, restoring it when minimized, and minimizes the
 * one already in front. While the window in front is maximized, the one brought to the front is
 * maximized too, taking its place. The buttons at the end of the taskbar tile the windows in a
 * grid, side by side or one above the other, or cascade them, and the last one lets the desktop
 * fill the whole page, and takes it back into the page. The user moves the taskbar to another
 * side by dragging it, or with the arrow keys on its grip, the first button of the taskbar; the
 * windows move and shrink to stay on the desktop, as they do when it takes less of the page again.
 * The menu of the taskbar, the button after the grip, lists the windows open, to bring one to the
 * front, those of the other panes, and those closed, to open one again in the pane shown.
 *
 * The windows are in panes, as in the workspaces of the desktop of an operating system: the
 * desktop shows the windows of one pane at a time, and the taskbar the buttons of those windows
 * only. The default pane, the first, is always there; the taskbar shows the panes, to show
 * another, and lets the user add one, asking for its name in a dialog, rename the one shown, with
 * a double click or F2 on its button too, and remove it, its windows going to the default pane.
 * Ctrl+Shift+Arrow Left and Up show the pane before, Ctrl+Shift+Arrow Right and Down the one after.
 * A button on the title bar of a window, the first on the right, moves the window to another pane,
 * or to a new one. A window is in the pane its attribute data-pane says, which the desktop keeps;
 * in the pane shown when it has none.
 *
 * The page registers types of windows, which the launcher of the taskbar, the button after its
 * menu, opens: in the pane shown, either by the function of the type making the element of the
 * window, which the desktop takes away again when the user closes it, or by the page, which
 * onOpenWindow of the options tells.
 *
 * Closing a window whose changes are not saved asks first, in a dialog of the markup of modal.ts
 * that its style sheet shows: closing it then puts its fields back as they were rendered. A window
 * has changes when a field in it holds another value than the one it was rendered with, see
 * isModified() of modal.ts, or when the page says so with data-modified; data-modified="false"
 * says it never has. The window gets the event antilia-desktop:discard then, for the page to drop
 * changes it keeps itself, in the state of a component for example.
 *
 * new Desktop(element, options) works on markup the page already has:
 *
 *   <div class="antilia-desktop">
 *     <div class="antilia-desktop-area">
 *       <section class="antilia-window" id="notes" data-x="40" data-y="30" data-width="420"
 *           data-height="280">
 *         <header class="antilia-window-header">
 *           <span class="antilia-window-icon"><svg>…</svg></span>
 *           <span class="antilia-window-title">Notes</span>
 *         </header>
 *         <div class="antilia-window-body">…</div>
 *       </section>
 *     </div>
 *     <div class="antilia-desktop-taskbar"></div>
 *   </div>
 *
 * Each window needs an id. Its data attributes give where it starts, data-x, data-y, data-width
 * and data-height in pixels, its smallest size, data-min-width and data-min-height, and what the
 * user may do with it: data-resizable, data-maximizable, data-minimizable and data-foldable are
 * on unless "false", data-closable is off unless "true". A window may start minimized, maximized,
 * folded or closed with the attributes data-minimized, data-maximized, data-folded or data-closed,
 * which the desktop keeps as the user changes them, with data-active on the window in front.
 * React, Svelte and Wicket render the windows, and add and remove them as they please: the
 * desktop finds them as they come and go, and gives a window that comes back, rendered again,
 * the state it had. It adds the buttons to the title bars, the handles to the windows and the
 * buttons to the taskbar, which it owns. Where the windows are and their sizes are CSS custom
 * properties set through the CSSOM, so the desktop works under a strict Content Security Policy.
 */

import { buttonClasses } from './button.js';
import { onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, type Look, type LookPart } from './look.js';
import { isFieldModified, ModalDialog, resetField } from './modal.js';

/** Where a window is, its size, and what the user made of it. */
export interface WindowState {
	/** Where the window is, from the left of the desktop, in pixels. */
	x: number;
	/** Where the window is, from the top of the desktop, in pixels. */
	y: number;
	/** The width of the window, in pixels, when not maximized. */
	width: number;
	/** The height of the window, in pixels, when neither maximized nor folded. */
	height: number;
	/** Whether the window is minimized to the taskbar. */
	minimized: boolean;
	/** Whether the window fills the desktop. */
	maximized: boolean;
	/** Whether the window is folded to its title bar. */
	folded: boolean;
	/** Whether the window is closed, gone from the desktop and from the taskbar. */
	closed: boolean;
	/** Where the window is stacked, 1 for the one at the back. */
	order: number;
	/** The id of the pane the window is in. */
	pane: string;
}

/** A pane of the desktop: windows shown together, as a workspace of an operating system. */
export interface DesktopPane {
	/** The id of the pane, which the windows in it have in data-pane. */
	id: string;
	/** The name of the pane, in the taskbar; empty for the default pane, as the texts name it. */
	name: string;
}

/** The id of the default pane, which is always there and the first. */
export const DEFAULT_PANE = '0';

/** A type of window, which the launcher of the taskbar opens. */
export interface WindowType {
	/** The name of the type. */
	type: string;
	/** What the launcher says for it. */
	title: string;
	/** The markup of its icon in the launcher, an svg, for example. */
	icon?: string;
	/**
	 * Makes the element of a new window, which the desktop adds to its area and takes away again
	 * when the user closes it; when left out, onOpenWindow of the options asks the page to add it.
	 *
	 * @param id the id the window gets, unique in the page
	 * @param pane the pane it opens in
	 */
	create?: (id: string, pane: string) => HTMLElement;
}

/** The side of the desktop the taskbar is on. */
export type TaskbarPosition = 'bottom' | 'top' | 'left' | 'right';

/**
 * How the windows are laid out by tile(): in a grid, side by side, one above the other, or
 * cascading from the top left corner.
 */
export type TileLayout = 'grid' | 'columns' | 'rows' | 'cascade';

/** What the desktop says: the titles of its buttons and the name of the taskbar. */
export interface DesktopTexts {
	minimize: string;
	maximize: string;
	restore: string;
	close: string;
	taskbar: string;
	grid: string;
	columns: string;
	rows: string;
	cascade: string;
	/** The title of the button letting the desktop fill the whole page. */
	expand: string;
	/** The title of the button taking the desktop back into the page. */
	collapse: string;
	/** The title of the grip of the taskbar, which moves it to another side. */
	move: string;
	/** The title of the button of the menu listing the windows. */
	menu: string;
	/** The heading of the windows open in the menu. */
	openWindows: string;
	/** The heading of the windows closed in the menu, which it opens again. */
	closedWindows: string;
	/** The title of the dialog asking before a window with changes closes; {0} is its title. */
	closeModifiedTitle: string;
	/** What the dialog asking before a window with changes closes says; {0} is its title. */
	closeModified: string;
	/** The button closing the window and losing its changes. */
	discard: string;
	/** The button keeping the window open. */
	keep: string;
	/** The name of the panes in the taskbar. */
	panes: string;
	/** The name of the default pane, until the user renames it. */
	defaultPane: string;
	/** The title of the button adding a pane, and of the dialog asking for its name. */
	newPane: string;
	/** The name a new pane is offered; {0} is its number. */
	newPaneName: string;
	/** The title of the button renaming the pane shown. */
	renamePane: string;
	/** The title of the dialog asking for the new name of a pane; {0} is its name. */
	renamePaneTitle: string;
	/** The title of the button removing the pane shown. */
	removePane: string;
	/** The label of the field of the name of a pane. */
	paneName: string;
	/** The title of the button of a window moving it to another pane. */
	moveToPane: string;
	/** The item of that menu moving the window to a new pane. */
	moveToNewPane: string;
	/** The title of the launcher, opening a window of a type. */
	newWindow: string;
	/** The button saying yes in a dialog. */
	ok: string;
	/** The button saying no in a dialog. */
	cancel: string;
}

/**
 * How a desktop shows its taskbar, what it says, and the callbacks.
 */
export interface DesktopOptions {
	/** The side of the desktop the taskbar is on, bottom when left out. */
	taskbar?: TaskbarPosition;
	/** The look of the desktop; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/** Whether the taskbar has the buttons tiling the windows, true when left out. */
	tiling?: boolean;
	/**
	 * Whether the taskbar has the button letting the desktop fill the whole page, true when left
	 * out.
	 */
	expandable?: boolean;
	/** Whether the desktop fills the whole page, false when left out. */
	expanded?: boolean;
	/**
	 * Whether the taskbar has the menu listing the windows open and closed, true when left out.
	 */
	menu?: boolean;
	/**
	 * Asked before the user closes a window with changes not saved, instead of the dialog of the
	 * desktop; resolving to true closes it. A page asks in a dialog of its own, for example.
	 */
	confirmClose?: (id: string, title: string) => Promise<boolean> | boolean;
	/**
	 * Whether the user may move the taskbar to another side, by dragging it or with the arrow keys
	 * on its grip, true when left out.
	 */
	movableTaskbar?: boolean;
	/**
	 * The panes, the default one, of the id DEFAULT_PANE, first; that one alone when left out,
	 * and added when missing.
	 */
	panes?: DesktopPane[];
	/** The id of the pane shown, the default one when left out. */
	activePane?: string;
	/** Whether the taskbar shows the panes, true when left out. */
	paneSwitcher?: boolean;
	/** Whether the user may add, rename and remove panes, true when left out. */
	editablePanes?: boolean;
	/** The types of windows the launcher of the taskbar opens; none when left out. */
	windowTypes?: WindowType[];
	/** What the desktop says, in English when left out. */
	texts?: Partial<DesktopTexts>;
	/**
	 * Called with the states of all windows, by their ids, once the user has changed any: moved,
	 * resized, minimized, maximized, folded, closed or brought one to the front, or tiled them; or
	 * moved the taskbar, or let the desktop fill the page or taken it back.
	 */
	onChange?: (states: Record<string, WindowState>) => void;
	/** Called with the side the user moved the taskbar to. */
	onTaskbar?: (position: TaskbarPosition) => void;
	/** Called with whether the desktop fills the whole page, once the user changed it. */
	onExpand?: (expanded: boolean) => void;
	/** Called with the id of the window the user brought to the front. */
	onActivate?: (id: string) => void;
	/** Called with the id of the window the user closed. */
	onClose?: (id: string) => void;
	/**
	 * Called with the panes and the id of the one shown, once the user added, renamed or removed
	 * one, or showed another.
	 */
	onPanes?: (panes: DesktopPane[], active: string) => void;
	/**
	 * Called when the user opens a window of a type without a function making it, with the type and
	 * the pane: the page adds the window, with that pane in data-pane.
	 */
	onOpenWindow?: (type: string, pane: string) => void;
}

export const DEFAULT_DESKTOP_TEXTS: DesktopTexts = {
	minimize: 'Minimize', maximize: 'Maximize', restore: 'Restore', close: 'Close',
	taskbar: 'Windows', grid: 'Tile the windows', columns: 'Windows side by side',
	rows: 'Windows one above the other', cascade: 'Cascade the windows',
	expand: 'Fill the page', collapse: 'Back into the page',
	move: 'Move the taskbar: drag it, or press an arrow key',
	menu: 'All windows', openWindows: 'Open', closedWindows: 'Closed',
	closeModifiedTitle: 'Close {0}?',
	closeModified: 'The changes in {0} are not saved. Close the window and lose them?',
	discard: 'Close and lose them', keep: 'Keep it open',
	panes: 'Panes', defaultPane: 'Main', newPane: 'New pane', newPaneName: 'Pane {0}',
	renamePane: 'Rename the pane', renamePaneTitle: 'Rename {0}', removePane: 'Remove the pane',
	paneName: 'Name', moveToPane: 'Move to another pane', moveToNewPane: 'New pane…',
	newWindow: 'Open a window', ok: 'OK', cancel: 'Cancel'
};

/** The classes of the parts of a desktop and of its windows. */
export const DESKTOP_CLASSES = {
	desktop: 'antilia-desktop',
	area: 'antilia-desktop-area',
	taskbar: 'antilia-desktop-taskbar',
	tasks: 'antilia-desktop-tasks',
	task: 'antilia-desktop-task',
	taskIcon: 'antilia-desktop-task-icon',
	taskTitle: 'antilia-desktop-task-title',
	tiling: 'antilia-desktop-tiling',
	tile: 'antilia-desktop-tile',
	grip: 'antilia-desktop-grip',
	expand: 'antilia-desktop-expand',
	menuButton: 'antilia-desktop-menu-button',
	menu: 'antilia-desktop-menu',
	menuGroup: 'antilia-desktop-menu-group',
	menuItem: 'antilia-desktop-menu-item',
	/** On the wrapper of the dialog asking before a window with changes closes. */
	confirm: 'antilia-desktop-confirm',
	/** On the wrapper of the dialog asking for the name of a pane. */
	prompt: 'antilia-desktop-prompt',
	launcher: 'antilia-desktop-launcher',
	panes: 'antilia-desktop-panes',
	pane: 'antilia-desktop-pane',
	paneAction: 'antilia-desktop-pane-action',
	window: 'antilia-window',
	header: 'antilia-window-header',
	icon: 'antilia-window-icon',
	title: 'antilia-window-title',
	body: 'antilia-window-body',
	controls: 'antilia-window-controls',
	control: 'antilia-window-control',
	handle: 'antilia-window-handle',
	moving: 'antilia-desktop-moving',
	/** On the root element of the page while a desktop fills it. */
	expandedPage: 'antilia-desktop-expanded'
} as const;

/** The custom properties holding where a window is, its size and where it is stacked. */
export const WINDOW_X_PROPERTY = '--antilia-window-x';
export const WINDOW_Y_PROPERTY = '--antilia-window-y';
export const WINDOW_WIDTH_PROPERTY = '--antilia-window-width';
export const WINDOW_HEIGHT_PROPERTY = '--antilia-window-height';
export const WINDOW_Z_PROPERTY = '--antilia-window-z';

const PROPERTIES = [WINDOW_X_PROPERTY, WINDOW_Y_PROPERTY, WINDOW_WIDTH_PROPERTY,
	WINDOW_HEIGHT_PROPERTY, WINDOW_Z_PROPERTY];

/** The attributes of a window's state, which the desktop keeps. */
const STATE_ATTRIBUTES = ['minimized', 'maximized', 'folded', 'closed'] as const;

/** The attributes the page sets on a window, which the desktop follows as they change. */
const OPTION_ATTRIBUTES = ['data-x', 'data-y', 'data-width', 'data-height', 'data-min-width',
	'data-min-height', 'data-resizable', 'data-maximizable', 'data-minimizable', 'data-foldable',
	'data-closable', 'data-modified', 'data-pane'];

/** The edges and corners of a window that resize it, and the sides they move. */
const HANDLES = ['n', 'e', 's', 'w', 'ne', 'se', 'sw', 'nw'] as const;

type Handle = typeof HANDLES[number];

/** The controls in a title bar that neither drag nor fold the window. */
const CONTROLS = 'button, a[href], input, select, textarea, [contenteditable]';

const DEFAULTS = { width: 420, height: 280, minWidth: 180, minHeight: 100, cascade: 28,
	/** How much of a window stays on the desktop, in pixels, however far it is dragged. */
	visible: 80 };

/** The paths of the icons of the buttons, in a 24 by 24 box. */
const ICONS: Record<'minimize' | 'maximize' | 'restore' | 'close' | 'expand' | 'collapse' |
	'grip' | 'menu' | 'pane' | 'launcher' | 'add' | 'rename' | 'remove' | TileLayout, string> = {
	minimize: 'M6 18h12',
	maximize: 'M5 5h14v14H5z',
	restore: 'M8 8h11v11H8z M5 16V5h11',
	close: 'M6 6l12 12 M18 6L6 18',
	grid: 'M4 4h7v7H4z M13 4h7v7h-7z M4 13h7v7H4z M13 13h7v7h-7z',
	columns: 'M4 4h7v16H4z M13 4h7v16h-7z',
	rows: 'M4 4h16v7H4z M4 13h16v7H4z',
	cascade: 'M3 3h11v8H3z M7 7h11v8H7z M11 11h10v9H11z',
	expand: 'M4 9V4h5 M15 4h5v5 M20 15v5h-5 M9 20H4v-5',
	collapse: 'M9 4v5H4 M15 4v5h5 M20 15h-5v5 M4 15h5v5',
	grip: 'M9 5h.01 M15 5h.01 M9 12h.01 M15 12h.01 M9 19h.01 M15 19h.01',
	menu: 'M4 6h16 M4 12h16 M4 18h16',
	pane: 'M14 4h6v16h-6 M4 12h11 M11 8l4 4-4 4',
	launcher: 'M4 4h16v16H4z M12 8v8 M8 12h8',
	add: 'M12 5v14 M5 12h14',
	rename: 'M4 20h4L19 9l-4-4L4 16z M13 7l4 4',
	remove: 'M5 7h14 M10 7V4h4v3 M7 7l1 13h8l1-13'
};

/** The sides the arrow keys on the grip move the taskbar to. */
const ARROWS: Record<string, TaskbarPosition> = {
	ArrowUp: 'top', ArrowDown: 'bottom', ArrowLeft: 'left', ArrowRight: 'right'
};

/** The arrow keys that, with Ctrl and Shift, show the pane before or the one after. */
const PANE_KEYS: Record<string, 1 | -1> = {
	ArrowLeft: -1, ArrowUp: -1, ArrowRight: 1, ArrowDown: 1
};

/** The fields whose changes a window may lose when it closes. */
const FIELDS = 'input, textarea, select';

/** The attribute saying whether a window, or a part of it, has changes not saved. */
const MODIFIED_ATTRIBUTE = 'data-modified';

/**
 * The event fired on a window whose changes the user chose to lose, closing it: its fields are
 * back as they were rendered, the page drops the changes it keeps itself.
 */
export const DESKTOP_DISCARD_EVENT = 'antilia-desktop:discard';

/** How far the pointer goes before the taskbar is dragged, in pixels. */
const DRAG_THRESHOLD = 6;

const SVG = 'http://www.w3.org/2000/svg';

/** The desktops by their element. */
const instances = new WeakMap<Element, Desktop>();

/**
 * @returns a text with its {0} replaced
 */
function format(text: string, value: string): string {
	return text.split('{0}').join(value);
}

function clamp(value: number, min: number, max: number): number {
	return Math.min(Math.max(value, min), Math.max(min, max));
}

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

/**
 * @returns the number of a data attribute, null when the element has none or it is not a number
 */
function numberOf(element: HTMLElement, name: string): number | null {
	const value = element.getAttribute(name);
	if (value === null || value.trim() === '') {
		return null;
	}
	const number = Number(value);
	return Number.isFinite(number) ? number : null;
}

/**
 * @returns whether a flag of a data attribute is on: "false" turns it off, "true" or an empty
 *          value on, and none leaves it as by default
 */
function flagOf(element: HTMLElement, name: string, fallback: boolean): boolean {
	const value = element.getAttribute(name);
	return value === null ? fallback : value !== 'false';
}

function icon(path: string): SVGSVGElement {
	const svg = document.createElementNS(SVG, 'svg');
	svg.setAttribute('viewBox', '0 0 24 24');
	svg.setAttribute('width', '14');
	svg.setAttribute('height', '14');
	svg.setAttribute('aria-hidden', 'true');
	svg.setAttribute('focusable', 'false');
	const shape = document.createElementNS(SVG, 'path');
	shape.setAttribute('d', path);
	shape.setAttribute('fill', 'none');
	shape.setAttribute('stroke', 'currentColor');
	shape.setAttribute('stroke-width', '2');
	shape.setAttribute('stroke-linejoin', 'round');
	shape.setAttribute('stroke-linecap', 'round');
	svg.append(shape);
	return svg;
}

function button(className: string, path: string, label: string): HTMLButtonElement {
	const element = document.createElement('button');
	element.type = 'button';
	element.className = className;
	element.append(icon(path));
	element.title = label;
	element.setAttribute('aria-label', label);
	return element;
}

/**
 * A window of the desktop: its element and the parts the desktop added to it.
 */
interface DesktopWindow {
	element: HTMLElement;
	state: WindowState;
	controls: HTMLElement | null;
	buttons: Partial<Record<'pane' | 'minimize' | 'maximize' | 'close', HTMLButtonElement>>;
	handles: HTMLElement[];
	task: HTMLButtonElement | null;
	/** Watches the title, which the taskbar shows. */
	titleObserver: MutationObserver | null;
	/** The element in the window that had the focus last, which gets it back. */
	focused: HTMLElement | null;
}

/** An item of a menu of the desktop: what it says, its icon, and what it does. */
interface MenuItem {
	label: string;
	/** The markup of the icon, or the element whose children are the icon. */
	icon?: string | Element | null;
	/** The data attributes of the item. */
	data: Record<string, string>;
	active?: boolean;
	minimized?: boolean;
	action: () => void;
}

/** The items of a menu under a heading. */
interface MenuGroup {
	name: string;
	/** The pane the items are in, for the windows of another pane. */
	pane?: string;
	heading: string;
	items: MenuItem[];
}

/**
 * @returns whether the user writes in an element, where the arrow keys move in the text
 */
function writing(target: Element | null): boolean {
	const field = target?.closest?.('input, textarea, select, [contenteditable]');
	if (!field || field.getAttribute('contenteditable') === 'false') {
		return false;
	}
	return !(field instanceof HTMLInputElement) || !['button', 'checkbox', 'radio', 'reset',
		'submit', 'image', 'range', 'color', 'file'].includes(field.type);
}

export class Desktop {
	readonly element: HTMLElement;

	/** The look of the desktop, whose classes the elements it makes get. */
	readonly look: Look;

	readonly area: HTMLElement;

	readonly taskbar: HTMLElement;

	options: DesktopOptions;

	/** The windows by their ids, in the order of the page. */
	private windows = new Map<string, DesktopWindow>();

	/** The states of the windows that were taken away, given back if they come again. */
	private remembered = new Map<string, WindowState>();

	private tasks: HTMLElement;

	private tiling: HTMLElement | null = null;

	/** The grip of the taskbar, which moves it to another side. */
	private grip: HTMLButtonElement | null = null;

	/** The button letting the desktop fill the page, and taking it back. */
	private expandButton: HTMLButtonElement | null = null;

	/** The button of the menu listing the windows. */
	private menuButton: HTMLButtonElement | null = null;

	/** The launcher, opening windows of the types the page registered. */
	private launcher: HTMLButtonElement | null = null;

	/** The panes in the taskbar. */
	private paneSwitcher: HTMLElement | null = null;

	/** The open menu: of the taskbar, of the launcher or of a window. */
	private menu: HTMLElement | null = null;

	/** The button the open menu belongs to. */
	private menuAnchor: HTMLButtonElement | null = null;

	/** What the items of the open menu do. */
	private menuActions = new Map<Element, () => void>();

	/** The ids of the windows the desktop made, of a type, which it takes away when closed. */
	private created = new Set<string>();

	/** How many windows of types the desktop made, for their ids. */
	private createdCount = 0;

	/** Stops listening for a press outside the open menu. */
	private stopWatchingMenu: (() => void) | null = null;

	/**
	 * Takes away the dialog asking before a window with changes closes, or for the name of a
	 * pane, while it is open.
	 */
	private cancelDialog: (() => void) | null = null;

	/** Whether the taskbar was added by the desktop, and is taken away again by destroy(). */
	private ownTaskbar = false;

	private listeners: Array<[EventTarget, string, EventListener, boolean]> = [];

	private observer: MutationObserver | null = null;

	private resizeObserver: ResizeObserver | null = null;

	private stopMoving: (() => void) | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** The id of the window in front, if any. */
	private active: string | null = null;

	/**
	 * @param element the element of the desktop: a child of the class antilia-desktop-area holds
	 *        the windows, and one of the class antilia-desktop-taskbar, added when missing, the
	 *        taskbar
	 * @param options the side of the taskbar, the texts and the callbacks
	 */
	constructor(element: HTMLElement, options: DesktopOptions = {}) {
		this.element = element;
		this.options = { ...options };
		this.look = options.look ?? lookOf(element);
		this.options.panes = this.normalizePanes(options.panes);
		this.options.activePane = this.paneOf(options.activePane) ?? DEFAULT_PANE;
		instances.get(element)?.destroy();
		instances.set(element, this);
		const children = Array.from(element.children) as HTMLElement[];
		const area = children.find((child) => child.classList.contains(DESKTOP_CLASSES.area));
		if (!area) {
			throw new Error('A desktop needs an element of the class ' + DESKTOP_CLASSES.area +
				', ' + element.id + ' has none');
		}
		this.area = area;
		let taskbar = children.find((child) => child.classList.contains(DESKTOP_CLASSES.taskbar));
		if (!taskbar) {
			taskbar = document.createElement('div');
			taskbar.className = this.css('desktop-taskbar');
			element.append(taskbar);
			this.ownTaskbar = true;
		}
		this.taskbar = taskbar;
		this.taskbar.replaceChildren();
		this.tasks = document.createElement('div');
		this.tasks.className = this.css('desktop-tasks');
		this.taskbar.append(this.tasks);
		if (typeof MutationObserver !== 'undefined') {
			this.observer = new MutationObserver((records) => this.onMutations(records));
			this.observer.observe(this.area, { childList: true });
		}
		this.render();
		this.findWindows();
		this.bind();
		this.renderExpanded();
		this.stopWatchingRemoval = onRemoving(element, () => this.detach());
	}

	/**
	 * Makes a desktop of the element, as Wicket does.
	 *
	 * @param element the element or its id
	 * @param options the side of the taskbar, the texts and the callbacks
	 * @returns the desktop, null when there is no such element
	 */
	static attach(element: HTMLElement | string, options?: DesktopOptions): Desktop | null {
		const resolved = resolve(element);
		return resolved ? new Desktop(resolved, options) : null;
	}

	/**
	 * @param target the element of the desktop or its id
	 * @returns the desktop, if there is one
	 */
	static of(target: HTMLElement | string | null | undefined): Desktop | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		return (element && instances.get(element)) || null;
	}

	private texts(): DesktopTexts {
		return { ...DEFAULT_DESKTOP_TEXTS, ...this.options.texts };
	}

	/**
	 * @returns the side of the taskbar
	 */
	/**
	 * @returns the classes of parts of the desktop in its look
	 */
	private css(...parts: LookPart[]): string {
		return partClasses(this.look, ...parts);
	}

	/**
	 * Gives an element of the desktop the classes of parts in its look.
	 */
	private add(element: Element, ...parts: LookPart[]): void {
		element.classList.add(...this.css(...parts).split(' '));
	}

	position(): TaskbarPosition {
		const position = this.options.taskbar;
		return position === 'top' || position === 'left' || position === 'right' ? position :
			'bottom';
	}

	private render(): void {
		const element = this.element;
		const position = this.position();
		this.add(element, 'desktop');
		for (const side of ['bottom', 'top', 'left', 'right']) {
			element.classList.toggle('antilia-desktop-taskbar-' + side, side === position);
		}
		const texts = this.texts();
		this.taskbar.setAttribute('role', 'toolbar');
		this.taskbar.setAttribute('aria-label', texts.taskbar);
		this.taskbar.setAttribute('aria-orientation', position === 'left' || position === 'right' ?
			'vertical' : 'horizontal');
		this.closeMenu(false);
		this.renderGrip(texts);
		this.renderMenuButton(texts);
		this.renderLauncher(texts);
		this.renderPanes(texts);
		this.renderTiling(texts);
		this.renderExpandButton(texts);
		this.windows.forEach((window) => this.renderWindow(window));
	}

	private renderGrip(texts: DesktopTexts): void {
		this.grip?.remove();
		this.grip = null;
		this.element.toggleAttribute('data-movable-taskbar', this.options.movableTaskbar !== false);
		if (this.options.movableTaskbar === false) {
			return;
		}
		this.grip = button(this.css('desktop-grip'), ICONS.grip, texts.move);
		this.grip.setAttribute('aria-keyshortcuts', 'ArrowUp ArrowDown ArrowLeft ArrowRight');
		this.taskbar.prepend(this.grip);
	}

	private renderMenuButton(texts: DesktopTexts): void {
		this.menuButton?.remove();
		this.menuButton = null;
		if (this.options.menu === false) {
			return;
		}
		this.menuButton = button(this.css('desktop-menu-button'), ICONS.menu, texts.menu);
		this.menuButton.setAttribute('aria-haspopup', 'menu');
		this.menuButton.setAttribute('aria-expanded', 'false');
		// after the grip, before the buttons of the windows
		this.tasks.before(this.menuButton);
	}

	private renderLauncher(texts: DesktopTexts): void {
		this.launcher?.remove();
		this.launcher = null;
		if (!this.windowTypes().length) {
			return;
		}
		this.launcher = button(this.css('desktop-launcher'), ICONS.launcher, texts.newWindow);
		this.launcher.setAttribute('aria-haspopup', 'menu');
		this.launcher.setAttribute('aria-expanded', 'false');
		// after the menu, before the buttons of the windows
		this.tasks.before(this.launcher);
	}

	/**
	 * Shows the panes in the taskbar, after the buttons of the windows: a button for each, then
	 * those adding a pane, renaming the one shown and removing it, as the user may.
	 */
	private renderPanes(texts: DesktopTexts): void {
		const hadFocus = this.paneSwitcher?.contains(document.activeElement) ?
			(document.activeElement as HTMLElement) : null;
		const focused = hadFocus ? hadFocus.dataset.pane ?? hadFocus.dataset.action ?? null : null;
		this.paneSwitcher?.remove();
		this.paneSwitcher = null;
		if (this.options.paneSwitcher === false) {
			return;
		}
		const switcher = document.createElement('div');
		switcher.className = this.css('desktop-panes');
		switcher.setAttribute('role', 'group');
		switcher.setAttribute('aria-label', texts.panes);
		const active = this.activePane();
		for (const pane of this.panes()) {
			const element = document.createElement('button');
			element.type = 'button';
			element.className = this.css('desktop-pane');
			element.dataset.pane = pane.id;
			element.textContent = this.paneName(pane);
			element.title = this.paneName(pane);
			element.setAttribute('aria-pressed', String(pane.id === active));
			if (this.isEditable()) {
				element.setAttribute('aria-keyshortcuts', 'F2' + (pane.id === DEFAULT_PANE ? '' :
					' Delete'));
			}
			switcher.append(element);
		}
		if (this.isEditable()) {
			const actions: Array<['add' | 'rename' | 'remove', string]> = [['add', texts.newPane],
				['rename', texts.renamePane]];
			if (active !== DEFAULT_PANE) {
				actions.push(['remove', texts.removePane]);
			}
			for (const [action, label] of actions) {
				const element = button(this.css('desktop-pane-action') + ' ' + DESKTOP_CLASSES.paneAction + '-' + action, ICONS[action], label);
				element.dataset.action = action;
				switcher.append(element);
			}
		}
		this.paneSwitcher = switcher;
		this.tasks.after(switcher);
		if (focused !== null) {
			(Array.from(switcher.querySelectorAll<HTMLElement>('button')).find((element) =>
				element.dataset.pane === focused || element.dataset.action === focused) ??
				switcher.querySelector<HTMLElement>('[aria-pressed="true"]'))?.focus();
		}
	}

	/**
	 * @returns whether the menu of the taskbar, of its launcher or of a window is open
	 */
	isMenuOpen(): boolean {
		return this.menu !== null;
	}

	/**
	 * Opens the menu listing the windows: those open in the pane shown, to bring one to the front,
	 * those of each other pane, showing it, then those closed, to open one again in the pane shown.
	 * The focus goes to its first item.
	 */
	openMenu(): void {
		if (!this.menuButton || this.menu) {
			return;
		}
		const texts = this.texts();
		const windows = Array.from(this.windows.values());
		const active = this.activePane();
		const item = (window: DesktopWindow): MenuItem => ({
			label: this.titleOf(window), icon: window.element.querySelector('.' +
				DESKTOP_CLASSES.icon), data: { window: window.element.id },
			active: this.active === window.element.id, minimized: window.state.minimized,
			// opened again when closed, restored when minimized, in front with the focus
			action: () => {
				this.activate(window.element.id, true);
				this.focus(window);
			}
		});
		const groups: MenuGroup[] = [{ name: 'open', heading: texts.openWindows,
			items: windows.filter((window) => !window.state.closed && window.state.pane === active)
				.map(item) }];
		for (const pane of this.panes()) {
			if (pane.id !== active) {
				groups.push({ name: 'pane', pane: pane.id, heading: this.paneName(pane),
					items: windows.filter((window) => !window.state.closed &&
						window.state.pane === pane.id).map(item) });
			}
		}
		groups.push({ name: 'closed', heading: texts.closedWindows,
			items: windows.filter((window) => window.state.closed).map(item) });
		this.showMenu(this.menuButton, texts.menu, groups);
	}

	/**
	 * Opens the menu of the launcher, listing the types of windows to open one in the pane shown.
	 */
	openLauncher(): void {
		if (!this.launcher || this.menu) {
			return;
		}
		const texts = this.texts();
		this.showMenu(this.launcher, texts.newWindow, [{ name: 'types', heading: texts.newWindow,
			items: this.windowTypes().map((type) => ({ label: type.title, icon: type.icon ?? null,
				data: { type: type.type }, action: () => {
					this.openWindow(type.type, this.activePane(), true);
				} })) }]);
	}

	/**
	 * Opens the menu of a window moving it to another pane, or to a new one.
	 *
	 * @param id the id of the window
	 */
	openPaneMenu(id: string): void {
		const window = this.windows.get(id);
		const anchor = window?.buttons.pane;
		if (!window || !anchor || this.menu) {
			return;
		}
		const texts = this.texts();
		const items: MenuItem[] = this.panes().filter((pane) => pane.id !== window.state.pane)
			.map((pane) => ({ label: this.paneName(pane), data: { pane: pane.id },
				action: () => this.moveToPane(id, pane.id, true) }));
		if (this.isEditable()) {
			items.push({ label: texts.moveToNewPane, data: { pane: '' }, action: () => {
				void this.requestNewPane().then((pane) => {
					if (pane) {
						this.moveToPane(id, pane.id, true);
					}
				});
			} });
		}
		this.showMenu(anchor, texts.moveToPane, [{ name: 'panes', heading: texts.moveToPane,
			items }]);
	}

	/**
	 * Opens a menu next to the button it belongs to, its items in groups with headings, those
	 * without items left out. The focus goes to its first item.
	 */
	private showMenu(anchor: HTMLButtonElement, label: string, groups: MenuGroup[]): void {
		const menu = document.createElement('div');
		menu.className = this.css('desktop-menu');
		menu.id = (this.element.id || 'antilia-desktop') + '-menu';
		menu.setAttribute('role', 'menu');
		menu.setAttribute('aria-label', label);
		this.menuActions.clear();
		groups.forEach((members, index) => {
			if (!members.items.length) {
				return;
			}
			const group = document.createElement('div');
			group.className = this.css('desktop-menu-group');
			group.dataset.group = members.name;
			if (members.pane !== undefined) {
				group.dataset.pane = members.pane;
			}
			group.setAttribute('role', 'group');
			const heading = document.createElement('div');
			heading.className = this.css('desktop-menu-group-heading');
			heading.id = menu.id + '-' + members.name + (members.pane !== undefined ? '-' + index :
				'');
			heading.textContent = members.heading;
			group.setAttribute('aria-labelledby', heading.id);
			group.append(heading);
			members.items.forEach((entry) => group.append(this.menuItem(entry)));
			menu.append(group);
		});
		this.menu = menu;
		this.menuAnchor = anchor;
		this.element.append(menu);
		this.placeMenu();
		anchor.setAttribute('aria-expanded', 'true');
		anchor.setAttribute('aria-controls', menu.id);
		const outside = (event: Event) => {
			const target = event.target as Node;
			if (!menu.contains(target) && !anchor.contains(target)) {
				this.closeMenu(false);
			}
		};
		document.addEventListener('pointerdown', outside, true);
		this.stopWatchingMenu = () => document.removeEventListener('pointerdown', outside, true);
		this.menuItems()[0]?.focus();
	}

	private menuItem(entry: MenuItem): HTMLButtonElement {
		const item = document.createElement('button');
		item.type = 'button';
		item.className = this.css('desktop-menu-item');
		item.setAttribute('role', 'menuitem');
		item.tabIndex = -1;
		Object.entries(entry.data).forEach(([name, value]) => {
			item.dataset[name] = value;
		});
		item.toggleAttribute('data-active', !!entry.active);
		item.toggleAttribute('data-minimized', !!entry.minimized);
		const itemIcon = document.createElement('span');
		itemIcon.className = this.css('desktop-task-icon');
		itemIcon.setAttribute('aria-hidden', 'true');
		if (typeof entry.icon === 'string') {
			const template = document.createElement('template');
			template.innerHTML = entry.icon;
			itemIcon.append(template.content);
		} else {
			entry.icon?.childNodes.forEach((node) => itemIcon.append(node.cloneNode(true)));
		}
		const text = document.createElement('span');
		text.className = this.css('desktop-task-title');
		text.textContent = entry.label;
		item.append(itemIcon, text);
		this.menuActions.set(item, entry.action);
		return item;
	}

	/**
	 * Places the open menu next to its button: on the side of the desktop away from the taskbar
	 * for those of the taskbar, below it for that of a window.
	 */
	private placeMenu(): void {
		const menu = this.menu;
		const anchor = this.menuAnchor;
		if (!menu || !anchor) {
			return;
		}
		const desktop = this.element.getBoundingClientRect();
		const rect = anchor.getBoundingClientRect();
		const position = this.taskbar.contains(anchor) ? this.position() : null;
		const gap = 4;
		const place = { left: 'auto', top: 'auto', right: 'auto', bottom: 'auto', 'max-height': '' };
		if (position === null || position === 'top') {
			if (position === null) {
				place.right = (desktop.right - rect.right) + 'px';
			} else {
				place.left = (rect.left - desktop.left) + 'px';
			}
			place.top = (rect.bottom - desktop.top + gap) + 'px';
			place['max-height'] = (desktop.bottom - rect.bottom - 2 * gap) + 'px';
		} else if (position === 'bottom') {
			place.left = (rect.left - desktop.left) + 'px';
			place.bottom = (desktop.bottom - rect.top + gap) + 'px';
			place['max-height'] = (rect.top - desktop.top - 2 * gap) + 'px';
		} else {
			if (position === 'left') {
				place.left = (rect.right - desktop.left + gap) + 'px';
			} else {
				place.right = (desktop.right - rect.left + gap) + 'px';
			}
			place.top = (rect.top - desktop.top) + 'px';
			place['max-height'] = (desktop.bottom - rect.top - gap) + 'px';
		}
		for (const [side, value] of Object.entries(place)) {
			menu.style.setProperty('--antilia-desktop-menu-' + side, value);
		}
	}

	/**
	 * Closes the open menu.
	 *
	 * @param focus whether its button takes the focus
	 */
	closeMenu(focus = true): void {
		const menu = this.menu;
		if (!menu) {
			return;
		}
		const hadFocus = menu.contains(document.activeElement);
		const anchor = this.menuAnchor;
		this.menu = null;
		this.menuAnchor = null;
		this.menuActions.clear();
		this.stopWatchingMenu?.();
		this.stopWatchingMenu = null;
		menu.remove();
		anchor?.setAttribute('aria-expanded', 'false');
		anchor?.removeAttribute('aria-controls');
		if ((focus || hadFocus) && anchor?.isConnected) {
			anchor.focus();
		}
	}

	private menuItems(): HTMLButtonElement[] {
		return this.menu ? Array.from(this.menu.querySelectorAll<HTMLButtonElement>('.' +
			DESKTOP_CLASSES.menuItem)) : [];
	}

	/**
	 * The arrow keys, Home and End go through the items of the open menu, Escape closes it and
	 * Tab leaves it.
	 *
	 * @returns whether the key was the menu's
	 */
	private onMenuKeyDown(event: KeyboardEvent): boolean {
		const items = this.menuItems();
		const index = items.indexOf(document.activeElement as HTMLButtonElement);
		let next: number | null = null;
		switch (event.key) {
			case 'ArrowDown':
			case 'ArrowRight':
				next = (index + 1) % items.length;
				break;
			case 'ArrowUp':
			case 'ArrowLeft':
				next = (index - 1 + items.length) % items.length;
				break;
			case 'Home':
				next = 0;
				break;
			case 'End':
				next = items.length - 1;
				break;
			case 'Escape':
				event.preventDefault();
				this.closeMenu(true);
				return true;
			case 'Tab':
				this.closeMenu(false);
				return false;
			default:
				return false;
		}
		event.preventDefault();
		items[next]?.focus();
		return true;
	}

	private onMenuClick(event: MouseEvent): void {
		const item = (event.target as Element).closest<HTMLElement>('.' + DESKTOP_CLASSES.menuItem);
		const action = item ? this.menuActions.get(item) : undefined;
		if (!action) {
			return;
		}
		this.closeMenu(false);
		action();
	}

	private renderExpandButton(texts: DesktopTexts): void {
		this.expandButton?.remove();
		this.expandButton = null;
		if (this.options.expandable === false) {
			return;
		}
		const expanded = this.isExpanded();
		const label = expanded ? texts.collapse : texts.expand;
		this.expandButton = button(this.css('desktop-expand'), expanded ? ICONS.collapse : ICONS.expand,
			label);
		this.expandButton.setAttribute('aria-pressed', String(expanded));
		this.taskbar.append(this.expandButton);
	}

	/**
	 * @returns whether the desktop fills the whole page
	 */
	isExpanded(): boolean {
		return this.options.expanded === true;
	}

	/**
	 * Shows the desktop filling the whole page, or in it, as the options say.
	 */
	private renderExpanded(): void {
		const expanded = this.isExpanded();
		this.element.toggleAttribute('data-expanded', expanded);
		this.markPage(expanded);
	}

	/**
	 * Marks the page while a desktop fills it, so that the page does not scroll under it.
	 *
	 * @param expanded whether this desktop fills the page
	 */
	private markPage(expanded: boolean): void {
		document.documentElement.classList.toggle(DESKTOP_CLASSES.expandedPage, expanded ||
			Array.from(document.querySelectorAll('.' + DESKTOP_CLASSES.desktop + '[data-expanded]'))
				.some((other) => other !== this.element));
	}

	private renderTiling(texts: DesktopTexts): void {
		this.tiling?.remove();
		this.tiling = null;
		if (this.options.tiling === false) {
			return;
		}
		this.tiling = document.createElement('div');
		this.tiling.className = this.css('desktop-tiling');
		for (const layout of ['grid', 'columns', 'rows', 'cascade'] as TileLayout[]) {
			const tile = button(this.css('desktop-tile') + ' ' + DESKTOP_CLASSES.tile + '-' + layout,
				ICONS[layout], texts[layout]);
			tile.dataset.layout = layout;
			this.tiling.append(tile);
		}
		this.taskbar.append(this.tiling);
	}

	/**
	 * Finds the windows the area has: takes in those that came, forgets those that went, keeping
	 * their states for when they come back.
	 */
	private findWindows(): void {
		const elements = (Array.from(this.area.children) as HTMLElement[])
			.filter((child) => child.classList.contains(DESKTOP_CLASSES.window));
		const found = new Set<string>();
		let added = false;
		elements.forEach((element) => {
			if (!element.id) {
				return;
			}
			found.add(element.id);
			const known = this.windows.get(element.id);
			if (known?.element === element) {
				return;
			}
			if (known) {
				this.forget(known);
			}
			this.windows.set(element.id, this.take(element));
			added = true;
		});
		let removed = false;
		Array.from(this.windows.values()).forEach((window) => {
			if (!found.has(window.element.id) || window.element.parentElement !== this.area) {
				this.forget(window);
				this.windows.delete(window.element.id);
				removed = true;
			}
		});
		// in the order of the page
		const ordered = elements.map((element) => this.windows.get(element.id))
			.filter((window): window is DesktopWindow => window !== undefined);
		this.windows = new Map(ordered.map((window) => [window.element.id, window]));
		if (added || removed) {
			this.normalizeOrder();
		}
		this.renderTasks();
		this.findActive();
	}

	/**
	 * Takes in a window: its state as the desktop had it, or as its attributes say.
	 */
	private take(element: HTMLElement): DesktopWindow {
		const remembered = this.remembered.get(element.id);
		this.remembered.delete(element.id);
		const index = this.windows.size;
		// in the pane shown when it says none, in the default one when it says one there is not
		const attribute = element.getAttribute('data-pane');
		const pane = attribute === null ? this.activePane() : this.paneOf(attribute) ?? DEFAULT_PANE;
		const state: WindowState = remembered ? { ...remembered,
			pane: this.paneOf(remembered.pane) ?? pane } : {
			x: numberOf(element, 'data-x') ?? DEFAULTS.cascade * (index + 1) - DEFAULTS.cascade / 2,
			y: numberOf(element, 'data-y') ?? DEFAULTS.cascade * (index + 1) - DEFAULTS.cascade / 2,
			width: numberOf(element, 'data-width') ?? DEFAULTS.width,
			height: numberOf(element, 'data-height') ?? DEFAULTS.height,
			minimized: element.hasAttribute('data-minimized'),
			maximized: element.hasAttribute('data-maximized'),
			folded: element.hasAttribute('data-folded'),
			closed: element.hasAttribute('data-closed'),
			order: numberOf(element, 'data-order') ?? Number.MAX_SAFE_INTEGER - 1000 + index,
			pane
		};
		const window: DesktopWindow = { element, state, controls: null, buttons: {}, handles: [],
			task: null, titleObserver: null, focused: null };
		if (!element.hasAttribute('tabindex')) {
			// the window takes the focus when brought to the front from the keyboard
			element.tabIndex = -1;
		}
		const title = element.querySelector<HTMLElement>('.' + DESKTOP_CLASSES.title);
		if (title) {
			if (!title.id) {
				title.id = element.id + '-title';
			}
			if (!element.hasAttribute('aria-labelledby')) {
				element.setAttribute('aria-labelledby', title.id);
			}
			if (typeof MutationObserver !== 'undefined') {
				window.titleObserver = new MutationObserver(() => this.renderTask(window));
				window.titleObserver.observe(title, { childList: true, characterData: true,
					subtree: true });
			}
		}
		if (!element.hasAttribute('role')) {
			element.setAttribute('role', 'region');
		}
		this.observer?.observe(element, { attributes: true, attributeFilter: OPTION_ATTRIBUTES });
		this.limitSize(window);
		this.renderWindow(window);
		this.renderTask(window);
		return window;
	}

	/**
	 * Lets a window go: takes away what the desktop added to it and remembers its state.
	 */
	private forget(window: DesktopWindow): void {
		this.remembered.set(window.element.id, { ...window.state });
		window.titleObserver?.disconnect();
		window.controls?.remove();
		window.handles.forEach((handle) => handle.remove());
		window.task?.remove();
		if (this.active === window.element.id) {
			this.active = null;
		}
	}

	private flags(window: DesktopWindow): { resizable: boolean; maximizable: boolean;
		minimizable: boolean; foldable: boolean; closable: boolean } {
		const element = window.element;
		return {
			resizable: flagOf(element, 'data-resizable', true),
			maximizable: flagOf(element, 'data-maximizable', true),
			minimizable: flagOf(element, 'data-minimizable', true),
			foldable: flagOf(element, 'data-foldable', true),
			closable: flagOf(element, 'data-closable', false)
		};
	}

	private minSize(window: DesktopWindow): { width: number; height: number } {
		return {
			width: numberOf(window.element, 'data-min-width') ?? DEFAULTS.minWidth,
			height: numberOf(window.element, 'data-min-height') ?? DEFAULTS.minHeight
		};
	}

	private limitSize(window: DesktopWindow): void {
		const min = this.minSize(window);
		window.state.width = Math.max(window.state.width, min.width);
		window.state.height = Math.max(window.state.height, min.height);
	}

	/**
	 * Shows a window as its state says, with the buttons and handles it allows.
	 */
	private renderWindow(window: DesktopWindow): void {
		const element = window.element;
		const state = window.state;
		const flags = this.flags(window);
		if (!flags.maximizable) {
			state.maximized = false;
		}
		if (!flags.minimizable) {
			state.minimized = false;
		}
		if (!flags.foldable) {
			state.folded = false;
		}
		this.add(element, 'window');
		this.place(window);
		STATE_ATTRIBUTES.forEach((name) => {
			element.toggleAttribute('data-' + name, state[name]);
		});
		element.toggleAttribute('data-active', this.active === element.id);
		if (element.getAttribute('data-pane') !== state.pane) {
			element.setAttribute('data-pane', state.pane);
		}
		element.hidden = state.minimized || state.closed || state.pane !== this.activePane();
		this.renderControls(window, flags);
		this.renderHandles(window, flags.resizable);
		this.renderTaskState(window);
	}

	/**
	 * Shows where a window is, its size and where it is stacked.
	 */
	private place(window: DesktopWindow): void {
		const style = window.element.style;
		const state = window.state;
		style.setProperty(WINDOW_X_PROPERTY, Math.round(state.x) + 'px');
		style.setProperty(WINDOW_Y_PROPERTY, Math.round(state.y) + 'px');
		style.setProperty(WINDOW_WIDTH_PROPERTY, Math.round(state.width) + 'px');
		style.setProperty(WINDOW_HEIGHT_PROPERTY, Math.round(state.height) + 'px');
		style.setProperty(WINDOW_Z_PROPERTY, String(state.order));
	}

	private renderControls(window: DesktopWindow, flags: ReturnType<Desktop['flags']>): void {
		const header = window.element.querySelector<HTMLElement>('.' + DESKTOP_CLASSES.header);
		if (!header) {
			return;
		}
		const texts = this.texts();
		if (!window.controls || window.controls.parentElement !== header) {
			window.controls?.remove();
			window.controls = document.createElement('span');
			window.controls.className = this.css('window-controls');
			window.buttons = {};
			header.append(window.controls);
		}
		const controls = window.controls;
		const wanted: Array<['pane' | 'minimize' | 'maximize' | 'close', boolean]> = [
			['pane', this.options.paneSwitcher !== false && (this.panes().length > 1 ||
				this.isEditable())],
			['minimize', flags.minimizable], ['maximize', flags.maximizable],
			['close', flags.closable]];
		for (const [name, shown] of wanted) {
			let control = window.buttons[name];
			if (!shown) {
				control?.remove();
				delete window.buttons[name];
				continue;
			}
			if (!control) {
				control = button(this.css('window-control') + ' ' + DESKTOP_CLASSES.control + '-' + name,
					ICONS[name], name === 'pane' ? texts.moveToPane : texts[name]);
				control.dataset.control = name;
				if (name === 'pane') {
					control.setAttribute('aria-haspopup', 'menu');
					control.setAttribute('aria-expanded', 'false');
				}
				window.buttons[name] = control;
			}
			// in the order of the title bar: pane, minimize, maximize, close
			controls.append(control);
		}
		const maximize = window.buttons.maximize;
		if (maximize) {
			const label = window.state.maximized ? texts.restore : texts.maximize;
			maximize.title = label;
			maximize.setAttribute('aria-label', label);
			maximize.replaceChildren(icon(window.state.maximized ? ICONS.restore : ICONS.maximize));
		}
		if (window.buttons.minimize) {
			window.buttons.minimize.title = texts.minimize;
			window.buttons.minimize.setAttribute('aria-label', texts.minimize);
		}
		if (window.buttons.close) {
			window.buttons.close.title = texts.close;
			window.buttons.close.setAttribute('aria-label', texts.close);
		}
		if (window.buttons.pane) {
			window.buttons.pane.title = texts.moveToPane;
			window.buttons.pane.setAttribute('aria-label', texts.moveToPane);
		}
	}

	private renderHandles(window: DesktopWindow, resizable: boolean): void {
		if (!resizable) {
			window.handles.forEach((handle) => handle.remove());
			window.handles = [];
			return;
		}
		if (window.handles.length && window.handles[0].parentElement === window.element) {
			return;
		}
		window.handles = HANDLES.map((side) => {
			const handle = document.createElement('span');
			handle.className = this.css('window-handle') + ' ' + DESKTOP_CLASSES.handle + '-' + side;
			handle.dataset.handle = side;
			handle.setAttribute('aria-hidden', 'true');
			window.element.append(handle);
			return handle;
		});
	}

	/**
	 * Gives the taskbar a button for each window, in the order of the page.
	 */
	private renderTasks(): void {
		this.windows.forEach((window) => {
			if (window.task) {
				this.tasks.append(window.task);
			}
		});
	}

	private renderTask(window: DesktopWindow): void {
		if (!window.task) {
			window.task = document.createElement('button');
			window.task.type = 'button';
			window.task.className = this.css('desktop-task');
			window.task.dataset.window = window.element.id;
			window.task.setAttribute('aria-controls', window.element.id);
		}
		const task = window.task;
		const title = this.titleOf(window);
		const source = window.element.querySelector('.' + DESKTOP_CLASSES.icon);
		const taskIcon = document.createElement('span');
		taskIcon.className = this.css('desktop-task-icon');
		taskIcon.setAttribute('aria-hidden', 'true');
		if (source) {
			source.childNodes.forEach((node) => taskIcon.append(node.cloneNode(true)));
		}
		const text = document.createElement('span');
		text.className = this.css('desktop-task-title');
		text.textContent = title;
		task.replaceChildren(taskIcon, text);
		task.title = title;
		this.renderTaskState(window);
	}

	/**
	 * @returns the title of a window, its id when it has none
	 */
	private titleOf(window: DesktopWindow): string {
		return window.element.querySelector('.' + DESKTOP_CLASSES.title)?.textContent?.trim() ||
			window.element.id;
	}

	private renderTaskState(window: DesktopWindow): void {
		const task = window.task;
		if (!task) {
			return;
		}
		task.hidden = window.state.closed || window.state.pane !== this.activePane();
		task.setAttribute('aria-pressed', String(this.active === window.element.id));
		task.toggleAttribute('data-minimized', window.state.minimized);
	}

	/**
	 * Gives the windows the orders 1, 2, 3… as they are stacked, keeping the stacking.
	 */
	private normalizeOrder(): void {
		const stacked = Array.from(this.windows.values()).sort((a, b) =>
			a.state.order - b.state.order);
		stacked.forEach((window, index) => {
			window.state.order = index + 1;
			window.element.style.setProperty(WINDOW_Z_PROPERTY, String(index + 1));
		});
	}

	/**
	 * @returns the windows shown, in the pane shown, neither minimized nor closed, from the back
	 *          to the front
	 */
	private shown(): DesktopWindow[] {
		const pane = this.activePane();
		return Array.from(this.windows.values())
			.filter((window) => !window.state.minimized && !window.state.closed &&
				window.state.pane === pane)
			.sort((a, b) => a.state.order - b.state.order);
	}

	/**
	 * Takes the window in front as the active one.
	 */
	private findActive(): void {
		const shown = this.shown();
		const front = shown.length ? shown[shown.length - 1].element.id : null;
		this.setActive(front);
	}

	private setActive(id: string | null): void {
		const previous = this.active;
		this.active = id;
		this.windows.forEach((window) => {
			const active = window.element.id === id;
			window.element.toggleAttribute('data-active', active);
			window.task?.setAttribute('aria-pressed', String(active));
		});
		if (previous !== id) {
			this.element.dispatchEvent(new CustomEvent('antilia-desktop:activate', {
				detail: { id } }));
		}
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void, capture = false): void {
		target.addEventListener(type, listener as EventListener, capture);
		this.listeners.push([target, type, listener as EventListener, capture]);
	}

	private bind(): void {
		this.listen<PointerEvent>(this.area, 'pointerdown', (event) => this.onPointerDown(event),
			true);
		this.listen<MouseEvent>(this.area, 'click', (event) => this.onClick(event));
		this.listen<MouseEvent>(this.area, 'dblclick', (event) => this.onDoubleClick(event));
		this.listen<FocusEvent>(this.area, 'focusin', (event) => this.onFocusIn(event));
		this.listen<MouseEvent>(this.taskbar, 'click', (event) => this.onTaskbarClick(event));
		this.listen<MouseEvent>(this.taskbar, 'dblclick', (event) => {
			const pane = (event.target as Element).closest<HTMLElement>('.' + DESKTOP_CLASSES.pane);
			if (pane && this.paneSwitcher?.contains(pane) && this.isEditable()) {
				event.preventDefault();
				void this.requestRenamePane(pane.dataset.pane ?? DEFAULT_PANE);
			}
		});
		this.listen<PointerEvent>(this.taskbar, 'pointerdown',
			(event) => this.onTaskbarPointerDown(event));
		this.listen<KeyboardEvent>(this.element, 'keydown', (event) => this.onKeyDown(event));
		this.listen<MouseEvent>(this.element, 'click', (event) => {
			if (this.menu && this.menu.contains(event.target as Node)) {
				this.onMenuClick(event);
			}
		});
		if (typeof ResizeObserver !== 'undefined') {
			this.resizeObserver = new ResizeObserver(() => this.keepInside());
			this.resizeObserver.observe(this.area);
		}
	}

	private onMutations(records: MutationRecord[]): void {
		let find = false;
		const changed = new Set<DesktopWindow>();
		for (const record of records) {
			if (record.type === 'childList') {
				find = find || record.target === this.area;
			} else {
				const window = this.windowOf(record.target as Element);
				if (window && window.element === record.target) {
					changed.add(window);
				}
			}
		}
		if (find) {
			this.findWindows();
		}
		changed.forEach((window) => this.onOptionsChanged(window, records));
	}

	/**
	 * Follows the attributes of a window the page changed: where it is, its size and what the
	 * user may do with it.
	 */
	private onOptionsChanged(window: DesktopWindow, records: MutationRecord[]): void {
		const names = new Set(records.filter((record) => record.target === window.element)
			.map((record) => record.attributeName));
		const element = window.element;
		const state = window.state;
		state.x = names.has('data-x') ? numberOf(element, 'data-x') ?? state.x : state.x;
		state.y = names.has('data-y') ? numberOf(element, 'data-y') ?? state.y : state.y;
		state.width = names.has('data-width') ? numberOf(element, 'data-width') ?? state.width :
			state.width;
		state.height = names.has('data-height') ? numberOf(element, 'data-height') ??
			state.height : state.height;
		if (names.has('data-pane')) {
			state.pane = this.paneOf(element.getAttribute('data-pane')) ?? state.pane;
		}
		this.limitSize(window);
		this.renderWindow(window);
		this.findActive();
	}

	/**
	 * @returns the window of the desktop an element is in, not one of a desktop in it
	 */
	private windowOf(target: Element | null): DesktopWindow | null {
		let element: Element | null = target;
		while (element && element !== this.area) {
			if (element.parentElement === this.area && element.id) {
				const window = this.windows.get(element.id);
				return window?.element === element ? window : null;
			}
			element = element.parentElement;
		}
		return null;
	}

	/**
	 * @returns whether the event happened in a desktop inside one of the windows, which handles
	 *          it itself
	 */
	private nested(event: Event): boolean {
		const target = event.target as Element | null;
		const desktop = target?.closest?.('.' + DESKTOP_CLASSES.desktop);
		return !!desktop && desktop !== this.element;
	}

	private onPointerDown(event: PointerEvent): void {
		if (this.nested(event)) {
			return;
		}
		const target = event.target as Element;
		const window = this.windowOf(target);
		if (!window) {
			return;
		}
		this.activate(window.element.id, true);
		if (event.button !== 0) {
			return;
		}
		const handle = (target.closest('.' + DESKTOP_CLASSES.handle) as HTMLElement | null)
			?.dataset.handle as Handle | undefined;
		if (handle && window.handles.length && !window.state.maximized) {
			event.preventDefault();
			this.track(window, event, handle);
			return;
		}
		const header = target.closest('.' + DESKTOP_CLASSES.header);
		if (header && window.element.contains(header) && !target.closest(CONTROLS) &&
			!window.state.maximized) {
			event.preventDefault();
			this.track(window, event, null);
		}
	}

	/**
	 * Follows the pointer moving a window, by its title bar, or resizing it, by a handle.
	 *
	 * @param handle the edge or corner taken, null to move the window
	 */
	private track(window: DesktopWindow, event: PointerEvent, handle: Handle | null): void {
		this.stopMoving?.();
		const start = { ...window.state };
		const startX = event.clientX;
		const startY = event.clientY;
		const min = this.minSize(window);
		const area = this.areaSize();
		const capture = event.target as Element;
		(capture as HTMLElement).setPointerCapture?.(event.pointerId);
		this.element.classList.add(DESKTOP_CLASSES.moving);
		if (handle) {
			this.element.dataset.resizing = handle;
		}

		const move = (moveEvent: PointerEvent) => {
			const dx = moveEvent.clientX - startX;
			const dy = moveEvent.clientY - startY;
			const state = window.state;
			if (!handle) {
				const position = this.limitPosition(start.x + dx, start.y + dy, start.width, area);
				state.x = position.x;
				state.y = position.y;
			} else {
				if (handle.includes('e')) {
					state.width = Math.max(min.width, start.width + dx);
				}
				if (handle.includes('s')) {
					state.height = Math.max(min.height, start.height + dy);
				}
				if (handle.includes('w')) {
					const width = Math.max(min.width, start.width - dx);
					state.x = start.x + start.width - width;
					state.width = width;
				}
				if (handle.includes('n')) {
					// the title bar stays on the desktop
					const height = Math.max(min.height, start.height - Math.max(dy, -start.y));
					state.y = start.y + start.height - height;
					state.height = height;
				}
			}
			this.place(window);
		};
		const end = () => {
			this.stopMoving = null;
			this.element.classList.remove(DESKTOP_CLASSES.moving);
			delete this.element.dataset.resizing;
			capture.removeEventListener('pointermove', move as EventListener);
			capture.removeEventListener('pointerup', end);
			capture.removeEventListener('pointercancel', end);
			capture.removeEventListener('lostpointercapture', end);
			const state = window.state;
			if (state.x !== start.x || state.y !== start.y || state.width !== start.width ||
				state.height !== start.height) {
				this.changed();
			}
		};
		capture.addEventListener('pointermove', move as EventListener);
		capture.addEventListener('pointerup', end);
		capture.addEventListener('pointercancel', end);
		capture.addEventListener('lostpointercapture', end);
		this.stopMoving = end;
	}

	/**
	 * @returns the size of the area the windows are in, null when it is not laid out
	 */
	private areaSize(): { width: number; height: number } | null {
		const width = this.area.clientWidth;
		const height = this.area.clientHeight;
		return width > 0 && height > 0 ? { width, height } : null;
	}

	/**
	 * @returns where a window may be: its title bar on the desktop, at least a part of it
	 */
	private limitPosition(x: number, y: number, width: number,
		area: { width: number; height: number } | null): { x: number; y: number } {
		if (!area) {
			return { x, y };
		}
		const visible = Math.min(DEFAULTS.visible, width);
		return {
			x: Math.round(clamp(x, visible - width, area.width - visible)),
			y: Math.round(clamp(y, 0, area.height - 32))
		};
	}

	/**
	 * Keeps the windows on the desktop when it shrinks.
	 */
	private keepInside(): void {
		const area = this.areaSize();
		if (!area) {
			return;
		}
		this.windows.forEach((window) => {
			const state = window.state;
			const position = this.limitPosition(state.x, state.y, state.width, area);
			if (position.x !== state.x || position.y !== state.y) {
				state.x = position.x;
				state.y = position.y;
				this.renderWindow(window);
			}
		});
	}

	private onClick(event: MouseEvent): void {
		if (this.nested(event)) {
			return;
		}
		const target = event.target as Element;
		const control = target.closest<HTMLElement>('.' + DESKTOP_CLASSES.control);
		const window = this.windowOf(target);
		if (!control || !window || !window.controls?.contains(control)) {
			return;
		}
		event.preventDefault();
		const id = window.element.id;
		switch (control.dataset.control) {
			case 'minimize':
				this.minimize(id, true);
				break;
			case 'maximize':
				this.maximize(id, !window.state.maximized, true);
				break;
			case 'close':
				void this.requestClose(id);
				break;
			case 'pane':
				if (this.menuAnchor === control) {
					this.closeMenu(true);
				} else {
					this.closeMenu(false);
					this.openPaneMenu(id);
				}
				break;
		}
	}

	private onDoubleClick(event: MouseEvent): void {
		if (this.nested(event)) {
			return;
		}
		const target = event.target as Element;
		const window = this.windowOf(target);
		const header = target.closest('.' + DESKTOP_CLASSES.header);
		if (!window || !header || !window.element.contains(header) || target.closest(CONTROLS)) {
			return;
		}
		event.preventDefault();
		this.fold(window.element.id, !window.state.folded, true);
	}

	private onFocusIn(event: FocusEvent): void {
		if (this.nested(event)) {
			return;
		}
		const window = this.windowOf(event.target as Element);
		if (!window) {
			return;
		}
		window.focused = event.target as HTMLElement;
		if (this.active !== window.element.id) {
			this.activate(window.element.id, true);
		}
	}

	private onTaskbarClick(event: MouseEvent): void {
		const target = event.target as Element;
		const tile = target.closest<HTMLElement>('.' + DESKTOP_CLASSES.tile);
		if (tile && this.taskbar.contains(tile)) {
			this.tile(tile.dataset.layout as TileLayout, true);
			return;
		}
		if (this.expandButton && this.expandButton.contains(target)) {
			this.expand(!this.isExpanded(), true);
			return;
		}
		for (const [anchor, open] of [[this.menuButton, () => this.openMenu()],
			[this.launcher, () => this.openLauncher()]] as const) {
			if (anchor && anchor.contains(target)) {
				if (this.menuAnchor === anchor) {
					this.closeMenu(true);
				} else {
					this.closeMenu(false);
					open();
				}
				return;
			}
		}
		const pane = target.closest<HTMLElement>('.' + DESKTOP_CLASSES.pane);
		if (pane && this.paneSwitcher?.contains(pane)) {
			this.showPane(pane.dataset.pane ?? DEFAULT_PANE, true);
			return;
		}
		const action = target.closest<HTMLElement>('.' + DESKTOP_CLASSES.paneAction);
		if (action && this.paneSwitcher?.contains(action)) {
			switch (action.dataset.action) {
				case 'add':
					void this.requestNewPane().then((added) => {
						if (added) {
							this.showPane(added.id, true);
						}
					});
					break;
				case 'rename':
					void this.requestRenamePane(this.activePane());
					break;
				case 'remove':
					this.removePane(this.activePane(), true);
					break;
			}
			return;
		}
		const task = target.closest<HTMLElement>('.' + DESKTOP_CLASSES.task);
		const id = task?.dataset.window;
		const window = id ? this.windows.get(id) : undefined;
		if (!id || !window || !this.taskbar.contains(task)) {
			return;
		}
		if (this.active === id && !window.state.minimized) {
			this.minimize(id, true);
		} else {
			this.bringForward(id, true);
		}
	}

	/**
	 * Drags the taskbar, by its grip or where it has no button, to the side of the desktop the
	 * pointer is nearest to.
	 */
	private onTaskbarPointerDown(event: PointerEvent): void {
		const target = event.target as Element;
		if (event.button !== 0 || this.options.movableTaskbar === false ||
			(target.closest('button') && !this.grip?.contains(target))) {
			return;
		}
		event.preventDefault();
		this.stopMoving?.();
		const startX = event.clientX;
		const startY = event.clientY;
		const capture = target as HTMLElement;
		capture.setPointerCapture?.(event.pointerId);
		let side: TaskbarPosition | null = null;

		const move = (moveEvent: PointerEvent) => {
			if (side === null && Math.hypot(moveEvent.clientX - startX,
				moveEvent.clientY - startY) < DRAG_THRESHOLD) {
				return;
			}
			side = this.nearestSide(moveEvent.clientX, moveEvent.clientY);
			this.element.classList.add(DESKTOP_CLASSES.moving);
			this.element.dataset.taskbarTarget = side;
		};
		const end = (endEvent: Event) => {
			this.stopMoving = null;
			this.element.classList.remove(DESKTOP_CLASSES.moving);
			delete this.element.dataset.taskbarTarget;
			capture.removeEventListener('pointermove', move as EventListener);
			capture.removeEventListener('pointerup', end);
			capture.removeEventListener('pointercancel', end);
			capture.removeEventListener('lostpointercapture', end);
			if (side !== null && endEvent.type === 'pointerup') {
				this.moveTaskbar(side, true);
			}
		};
		capture.addEventListener('pointermove', move as EventListener);
		capture.addEventListener('pointerup', end);
		capture.addEventListener('pointercancel', end);
		capture.addEventListener('lostpointercapture', end);
		this.stopMoving = () => end(new Event('cancel'));
	}

	/**
	 * @returns the side of the desktop nearest to a point of the page
	 */
	private nearestSide(x: number, y: number): TaskbarPosition {
		const rect = this.element.getBoundingClientRect();
		const distances: Array<[TaskbarPosition, number]> = [
			['top', Math.abs(y - rect.top)], ['bottom', Math.abs(rect.bottom - y)],
			['left', Math.abs(x - rect.left)], ['right', Math.abs(rect.right - x)]];
		return distances.reduce((nearest, next) => next[1] < nearest[1] ? next : nearest)[0];
	}

	private onKeyDown(event: KeyboardEvent): void {
		if (event.defaultPrevented || this.nested(event)) {
			return;
		}
		if (this.menu && this.menu.contains(event.target as Node)) {
			this.onMenuKeyDown(event);
			return;
		}
		if ((event.key === 'ArrowDown' || event.key === 'ArrowUp') && !this.menu && !event.ctrlKey &&
			!event.altKey && !event.metaKey) {
			const target = event.target as Element;
			const window = this.windowOf(target);
			const open = target === this.menuButton ? () => this.openMenu() :
				target === this.launcher ? () => this.openLauncher() :
					window && target === window.buttons.pane ?
						() => this.openPaneMenu(window.element.id) : null;
			if (open) {
				event.preventDefault();
				open();
				return;
			}
		}
		const pane = (event.target as Element).closest?.<HTMLElement>('.' + DESKTOP_CLASSES.pane);
		if (pane && this.paneSwitcher?.contains(pane) && this.isEditable() && !event.ctrlKey &&
			!event.altKey && !event.metaKey && !event.shiftKey &&
			(event.key === 'F2' || event.key === 'Delete')) {
			event.preventDefault();
			const id = pane.dataset.pane ?? DEFAULT_PANE;
			if (event.key === 'F2') {
				void this.requestRenamePane(id);
			} else {
				this.removePane(id, true);
			}
			return;
		}
		const step = PANE_KEYS[event.key];
		if (step && event.ctrlKey && event.shiftKey && !event.altKey && !event.metaKey &&
			!writing(event.target as Element) && this.panes().length > 1) {
			event.preventDefault();
			this.nextPane(step, true);
			return;
		}
		if (this.grip && event.target === this.grip && ARROWS[event.key] && !event.ctrlKey &&
			!event.altKey && !event.metaKey) {
			event.preventDefault();
			this.moveTaskbar(ARROWS[event.key], true);
			this.grip?.focus();
			return;
		}
		if (event.key === 'Escape' && this.isExpanded() && !this.windowOf(event.target as Element)) {
			event.preventDefault();
			this.expand(false, true);
			return;
		}
		const window = this.windowOf(event.target as Element);
		if (event.key === 'F11' && !event.ctrlKey && !event.altKey && !event.metaKey &&
			!event.shiftKey) {
			if (window && this.flags(window).maximizable) {
				event.preventDefault();
				this.maximize(window.element.id, !window.state.maximized, true);
			}
			return;
		}
		const cycle = event.ctrlKey && !event.altKey && !event.metaKey &&
			(event.key === 'F6' || event.key === 'Tab');
		if (cycle) {
			event.preventDefault();
			this.cycle(event.shiftKey ? -1 : 1, true);
		}
	}

	/**
	 * Brings a window to the front, restoring it when minimized and showing its pane; while the
	 * window in front is maximized, it is maximized too. The window takes the focus.
	 */
	private bringForward(id: string, user: boolean): void {
		const window = this.windows.get(id);
		if (!window || window.state.closed) {
			return;
		}
		const paneChanged = window.state.pane !== this.activePane();
		if (paneChanged) {
			this.applyPane(window.state.pane);
			this.findActive();
		}
		const front = this.active ? this.windows.get(this.active) : undefined;
		if (front && front !== window && front.state.maximized && !front.state.minimized &&
			this.flags(window).maximizable) {
			window.state.maximized = true;
		}
		window.state.minimized = false;
		this.raise(window);
		this.renderWindow(window);
		this.setActive(id);
		this.focus(window);
		if (user) {
			this.options.onActivate?.(id);
			if (paneChanged) {
				this.options.onPanes?.(this.panes(), this.activePane());
			}
			this.changed();
		}
	}

	private raise(window: DesktopWindow): void {
		const top = Math.max(0, ...Array.from(this.windows.values()).map((other) =>
			other.state.order));
		if (window.state.order !== top || Array.from(this.windows.values()).some((other) =>
			other !== window && other.state.order === top)) {
			window.state.order = top + 1;
		}
		this.normalizeOrder();
	}

	private focus(window: DesktopWindow): void {
		if (window.element.contains(document.activeElement)) {
			return;
		}
		const focused = window.focused;
		const target = focused && window.element.contains(focused) && !focused.closest('[hidden]') ?
			focused : window.element;
		target.focus({ preventScroll: true });
	}

	/**
	 * Calls back with the states, once the user changed them.
	 */
	private changed(): void {
		this.options.onChange?.(this.states());
	}

	/**
	 * @returns the states of all windows, by their ids
	 */
	states(): Record<string, WindowState> {
		const states: Record<string, WindowState> = {};
		this.windows.forEach((window, id) => {
			states[id] = { ...window.state };
		});
		return states;
	}

	/**
	 * @returns the state of a window, null when the desktop has no such window
	 */
	state(id: string): WindowState | null {
		const window = this.windows.get(id);
		return window ? { ...window.state } : null;
	}

	/**
	 * @returns the ids of the windows, in the order of the page
	 */
	ids(): string[] {
		return Array.from(this.windows.keys());
	}

	/**
	 * @returns the id of the window in front, null when none is shown
	 */
	activeId(): string | null {
		return this.active;
	}

	/**
	 * Brings a window to the front, restoring it when minimized, showing its pane, and opening it
	 * in the pane shown when closed.
	 *
	 * @param id the id of the window
	 * @param user whether the user did, calling back
	 */
	activate(id: string, user = false): void {
		const window = this.windows.get(id);
		if (!window) {
			return;
		}
		if (window.state.closed) {
			this.open(id, user);
			return;
		}
		if (window.state.minimized || window.state.pane !== this.activePane()) {
			this.bringForward(id, user);
			return;
		}
		const raised = window.state.order !== this.windows.size;
		this.raise(window);
		const wasActive = this.active === id;
		this.setActive(id);
		if (user && !wasActive) {
			this.options.onActivate?.(id);
		}
		if (user && raised) {
			this.changed();
		}
	}

	/**
	 * Minimizes a window to the taskbar; the window behind it comes to the front.
	 *
	 * @param id the id of the window
	 * @param user whether the user did, calling back
	 */
	minimize(id: string, user = false): void {
		const window = this.windows.get(id);
		if (!window || window.state.minimized || window.state.closed ||
			!this.flags(window).minimizable) {
			return;
		}
		const hadFocus = window.element.contains(document.activeElement);
		window.state.minimized = true;
		this.renderWindow(window);
		this.findActive();
		if (hadFocus) {
			const front = this.active ? this.windows.get(this.active) : undefined;
			if (front) {
				this.focus(front);
			} else {
				window.task?.focus();
			}
		}
		if (user) {
			this.changed();
		}
	}

	/**
	 * Maximizes a window to the whole desktop, or restores it to its size; a minimized window
	 * comes back.
	 *
	 * @param id the id of the window
	 * @param maximized whether to maximize it
	 * @param user whether the user did, calling back
	 */
	maximize(id: string, maximized = true, user = false): void {
		const window = this.windows.get(id);
		if (!window || window.state.closed || !this.flags(window).maximizable) {
			return;
		}
		if (window.state.maximized === maximized && !window.state.minimized) {
			return;
		}
		window.state.maximized = maximized;
		window.state.minimized = false;
		if (maximized) {
			window.state.folded = false;
		}
		this.raise(window);
		this.renderWindow(window);
		this.setActive(id);
		if (user) {
			this.changed();
		}
	}

	/**
	 * Restores a window: neither minimized, nor maximized, nor folded.
	 *
	 * @param id the id of the window
	 * @param user whether the user did, calling back
	 */
	restore(id: string, user = false): void {
		const window = this.windows.get(id);
		if (!window || window.state.closed) {
			return;
		}
		window.state.minimized = false;
		window.state.maximized = false;
		window.state.folded = false;
		this.raise(window);
		this.renderWindow(window);
		this.setActive(id);
		if (user) {
			this.changed();
		}
	}

	/**
	 * Folds a window to its title bar, or unfolds it.
	 *
	 * @param id the id of the window
	 * @param folded whether to fold it
	 * @param user whether the user did, calling back
	 */
	fold(id: string, folded = true, user = false): void {
		const window = this.windows.get(id);
		if (!window || window.state.folded === folded || !this.flags(window).foldable) {
			return;
		}
		window.state.folded = folded;
		if (folded) {
			window.state.maximized = false;
		}
		this.renderWindow(window);
		if (user) {
			this.changed();
		}
	}

	/**
	 * Closes a window: gone from the desktop and from the taskbar until opened again; one the
	 * desktop made, of a type, is taken away.
	 *
	 * @param id the id of the window
	 * @param user whether the user did, calling back
	 */
	close(id: string, user = false): void {
		const window = this.windows.get(id);
		if (!window || window.state.closed) {
			return;
		}
		const hadFocus = window.element.contains(document.activeElement);
		window.state.closed = true;
		this.renderWindow(window);
		this.findActive();
		if (hadFocus) {
			const front = this.active ? this.windows.get(this.active) : undefined;
			if (front) {
				this.focus(front);
			}
		}
		if (this.created.delete(id)) {
			// made by the desktop, of a type: gone for good
			window.element.remove();
			this.findWindows();
			this.remembered.delete(id);
		}
		if (user) {
			this.options.onClose?.(id);
			this.changed();
		}
	}

	/**
	 * Whether a window has changes not saved, which closing it loses: as data-modified says on it
	 * or on an element in it, "true" or empty for changes, "false" for none; else whether a field
	 * in it holds another value than the one it was rendered with. The fields in an element
	 * marked data-modified="false" do not count, unless in one marked data-modified="auto" in it.
	 *
	 * @param id the id of the window
	 */
	isModified(id: string): boolean {
		const window = this.windows.get(id);
		return !!window && this.modifiedFields(window) !== null;
	}

	/**
	 * @returns the fields of a window the user changed, empty when the page says it has changes
	 *          of its own; null when it has none
	 */
	private modifiedFields(window: DesktopWindow): Element[] | null {
		const element = window.element;
		const marks = [element, ...Array.from(element.querySelectorAll('[' + MODIFIED_ATTRIBUTE +
			']'))];
		if (marks.some((mark) => mark.hasAttribute(MODIFIED_ATTRIBUTE) &&
			['', 'true'].includes(mark.getAttribute(MODIFIED_ATTRIBUTE) ?? 'false'))) {
			return [];
		}
		const fields = Array.from(element.querySelectorAll(FIELDS)).filter((field) =>
			field.closest('[' + MODIFIED_ATTRIBUTE + ']')?.getAttribute(MODIFIED_ATTRIBUTE) !==
				'false' && isFieldModified(field));
		return fields.length ? fields : null;
	}

	/**
	 * Closes a window as its close button does: one with changes not saved asks first, in the
	 * dialog of the desktop or as confirmClose of the options does, and closing it puts its fields
	 * back as they were rendered.
	 *
	 * @param id the id of the window
	 * @returns whether the window closed
	 */
	async requestClose(id: string): Promise<boolean> {
		const window = this.windows.get(id);
		if (!window || window.state.closed) {
			return false;
		}
		const fields = this.modifiedFields(window);
		if (fields !== null) {
			const title = this.titleOf(window);
			const confirmed = this.options.confirmClose ?
				await this.options.confirmClose(id, title) : await this.askToClose(title);
			if (!confirmed || this.windows.get(id) !== window || window.state.closed) {
				return false;
			}
			fields.forEach((field) => resetField(field));
			// for the page to drop the changes it keeps itself
			window.element.dispatchEvent(new CustomEvent(DESKTOP_DISCARD_EVENT, { bubbles: true,
				detail: { id } }));
		}
		this.close(id, true);
		return true;
	}

	/**
	 * Asks whether to close a window and lose its changes, in the dialog of the desktop, of the
	 * markup of modal.ts; what requestClose() does unless confirmClose of the options asks.
	 *
	 * @param title the title of the window
	 * @returns whether to close it
	 */
	askToClose(title: string): Promise<boolean> {
		const texts = this.texts();
		const text = document.createElement('p');
		text.textContent = format(texts.closeModified, title);
		return this.showDialog(DESKTOP_CLASSES.confirm, format(texts.closeModifiedTitle, title),
			text, [texts.keep, 'keep'], [texts.discard, 'discard', 'danger']);
	}

	/**
	 * Asks for the name of a pane, in a dialog of the markup of modal.ts.
	 *
	 * @param title the title of the dialog
	 * @param value the name the field starts with
	 * @returns the name, trimmed; null when the user cancelled or left it empty
	 */
	askPaneName(title: string, value = ''): Promise<string | null> {
		const texts = this.texts();
		const field = document.createElement('div');
		field.className = this.css('desktop-prompt-field');
		const label = document.createElement('label');
		const input = document.createElement('input');
		input.type = 'text';
		input.id = (this.element.id || 'antilia-desktop') + '-prompt-name';
		input.className = this.css('desktop-prompt-input');
		input.value = value;
		input.setAttribute('autocomplete', 'off');
		label.htmlFor = input.id;
		label.textContent = texts.paneName;
		field.append(label, input);
		return this.showDialog(DESKTOP_CLASSES.prompt, title, field, [texts.cancel, 'cancel'],
			[texts.ok, 'ok', 'primary'], input).then((confirmed) => {
			const name = input.value.trim();
			return confirmed && name ? name : null;
		});
	}

	/**
	 * Shows a dialog of the markup of modal.ts, over the page: Escape and the first button close
	 * it, the second one confirms it, as Enter in its field does.
	 *
	 * @param className the class of its wrapper
	 * @param title its title
	 * @param body what it shows
	 * @param cancel what the button closing it says, and its name
	 * @param confirm what the button confirming it says, its name and variant
	 * @param input the field in it, if any, which takes the focus
	 * @returns whether the user confirmed it
	 */
	private showDialog(className: string, title: string, body: HTMLElement,
		cancel: [string, string], confirm: [string, string, 'danger' | 'primary'],
		input?: HTMLInputElement): Promise<boolean> {
		this.cancelDialog?.();
		return new Promise((resolve) => {
			const prefix = (this.element.id || 'antilia-desktop') + '-' +
				className.replace('antilia-desktop-', '');
			const wrapper = document.createElement('div');
			wrapper.className = this.css('dialog-theme', className === DESKTOP_CLASSES.confirm ?
				'desktop-confirm' : 'desktop-prompt');
			const overlay = document.createElement('div');
			overlay.className = this.css('dialog-overlay');
			const dialog = document.createElement('div');
			dialog.className = this.css('dialog');
			dialog.setAttribute('role', input ? 'dialog' : 'alertdialog');
			dialog.setAttribute('aria-modal', 'true');
			dialog.setAttribute('aria-labelledby', prefix + '-title');
			if (!input) {
				body.id = prefix + '-text';
				dialog.setAttribute('aria-describedby', body.id);
			}
			const content = document.createElement('div');
			content.className = this.css('dialog-content');
			const header = document.createElement('div');
			header.className = this.css('dialog-header');
			const heading = document.createElement('h1');
			heading.className = this.css('dialog-title');
			heading.id = prefix + '-title';
			heading.textContent = title;
			header.append(heading);
			const main = document.createElement('div');
			main.className = this.css('dialog-body');
			main.append(body);
			const footer = document.createElement('div');
			footer.className = this.css('dialog-footer');
			const no = this.dialogButton(cancel[0], 'secondary', cancel[1]);
			const yes = this.dialogButton(confirm[0], confirm[2], confirm[1]);
			footer.append(no, yes);
			content.append(header, main, footer);
			dialog.append(content);
			overlay.append(dialog);
			wrapper.append(overlay);
			document.body.append(wrapper);
			let modal: ModalDialog | null = null;
			const finish = (value: boolean) => {
				if (this.cancelDialog !== close) {
					return;
				}
				this.cancelDialog = null;
				modal?.destroy();
				wrapper.remove();
				resolve(value);
			};
			const close = () => finish(false);
			this.cancelDialog = close;
			no.addEventListener('click', close);
			yes.addEventListener('click', () => finish(true));
			input?.addEventListener('keydown', (event) => {
				if (event.key === 'Enter' && !event.isComposing) {
					event.preventDefault();
					finish(true);
				}
			});
			modal = new ModalDialog(overlay, { size: 'small', foldable: false, onClose: close,
				look: this.look });
			if (input) {
				input.focus();
				input.select();
			}
		});
	}

	private dialogButton(label: string, variant: 'secondary' | 'danger' | 'primary',
		name: string): HTMLButtonElement {
		const element = document.createElement('button');
		element.type = 'button';
		// in the look of the desktop, as the buttons rendered with it
		element.className = buttonClasses({ variant, size: 'small', look: this.look });
		element.dataset.confirm = name;
		const text = document.createElement('span');
		text.className = this.css('button-label');
		text.textContent = label;
		element.append(text);
		return element;
	}

	/**
	 * @returns the panes, the default one first
	 */
	panes(): DesktopPane[] {
		return (this.options.panes ?? []).map((pane) => ({ ...pane }));
	}

	/**
	 * @returns the id of the pane shown
	 */
	activePane(): string {
		return this.options.activePane ?? DEFAULT_PANE;
	}

	/**
	 * @returns the id of a pane of the desktop, null when it has no such pane
	 */
	private paneOf(id: string | null | undefined): string | null {
		return id !== null && id !== undefined && (this.options.panes ?? [])
			.some((pane) => pane.id === id) ? id : null;
	}

	/**
	 * @returns the name of a pane, as the texts name the default one while it has none
	 */
	private paneName(pane: DesktopPane): string {
		return pane.name.trim() || (pane.id === DEFAULT_PANE ? this.texts().defaultPane : pane.id);
	}

	/**
	 * @returns the panes as given, each once, the default one first, added when missing
	 */
	private normalizePanes(panes: DesktopPane[] | undefined): DesktopPane[] {
		const seen = new Set<string>();
		const normalized = (panes ?? []).filter((pane) => pane && typeof pane.id === 'string' &&
			pane.id !== '' && !seen.has(pane.id) && seen.add(pane.id))
			.map((pane) => ({ id: pane.id, name: String(pane.name ?? '') }));
		const index = normalized.findIndex((pane) => pane.id === DEFAULT_PANE);
		const first = index < 0 ? { id: DEFAULT_PANE, name: '' } : normalized.splice(index, 1)[0];
		return [first, ...normalized];
	}

	/**
	 * @returns whether the user may add, rename and remove panes
	 */
	private isEditable(): boolean {
		return this.options.editablePanes !== false;
	}

	/**
	 * Shows a pane: its windows, and the buttons of the taskbar for them.
	 */
	private applyPane(id: string): void {
		this.closeMenu(false);
		this.options.activePane = id;
		this.windows.forEach((window) => this.renderWindow(window));
		this.renderPanes(this.texts());
	}

	/**
	 * Calls back with the panes, once the user changed them, and with the states of the windows.
	 */
	private panesChanged(): void {
		this.options.onPanes?.(this.panes(), this.activePane());
		this.changed();
	}

	/**
	 * Shows another pane, its window in front taking the focus when the focus was in a window.
	 *
	 * @param id the id of the pane
	 * @param user whether the user did, calling back
	 * @returns whether the pane shown changed
	 */
	showPane(id: string, user = false): boolean {
		if (this.paneOf(id) === null || id === this.activePane()) {
			return false;
		}
		const hadFocus = this.area.contains(document.activeElement);
		this.applyPane(id);
		this.findActive();
		if (hadFocus) {
			const front = this.active ? this.windows.get(this.active) : undefined;
			if (front) {
				this.focus(front);
			} else {
				this.paneSwitcher?.querySelector<HTMLElement>('[aria-pressed="true"]')?.focus();
			}
		}
		if (user) {
			this.panesChanged();
		}
		return true;
	}

	/**
	 * Shows the pane after the one shown, or the one before, going round.
	 *
	 * @param direction 1 for the pane after, -1 for the one before
	 * @param user whether the user did, calling back
	 */
	nextPane(direction: 1 | -1 = 1, user = false): void {
		const panes = this.panes();
		const index = panes.findIndex((pane) => pane.id === this.activePane());
		this.showPane(panes[(index + direction + panes.length) % panes.length].id, user);
	}

	/**
	 * Adds a pane, after the others, without showing it.
	 *
	 * @param name the name of the pane
	 * @param user whether the user did, calling back
	 * @returns the pane
	 */
	addPane(name: string, user = false): DesktopPane {
		const panes = this.options.panes ?? [];
		const pane = { id: String(Math.max(0, ...panes.map((other) => Number(other.id))
			.filter(Number.isFinite)) + 1), name: name.trim() };
		this.options.panes = [...panes, pane];
		this.renderPanes(this.texts());
		this.windows.forEach((window) => this.renderWindow(window));
		if (user) {
			this.panesChanged();
		}
		return { ...pane };
	}

	/**
	 * Renames a pane.
	 *
	 * @param id the id of the pane
	 * @param name its new name; an empty one names the default pane as the texts do
	 * @param user whether the user did, calling back
	 * @returns whether there is such a pane
	 */
	renamePane(id: string, name: string, user = false): boolean {
		const pane = (this.options.panes ?? []).find((other) => other.id === id);
		if (!pane) {
			return false;
		}
		pane.name = name.trim();
		this.renderPanes(this.texts());
		if (user) {
			this.panesChanged();
		}
		return true;
	}

	/**
	 * Removes a pane, its windows going to the default pane, which is shown when the pane was.
	 * The default pane cannot be removed.
	 *
	 * @param id the id of the pane
	 * @param user whether the user did, calling back
	 * @returns whether the pane was removed
	 */
	removePane(id: string, user = false): boolean {
		if (id === DEFAULT_PANE || this.paneOf(id) === null) {
			return false;
		}
		const hadFocus = this.paneSwitcher?.contains(document.activeElement) ?? false;
		this.closeMenu(false);
		this.options.panes = (this.options.panes ?? []).filter((pane) => pane.id !== id);
		this.windows.forEach((window) => {
			if (window.state.pane === id) {
				window.state.pane = DEFAULT_PANE;
			}
		});
		if (this.activePane() === id) {
			this.options.activePane = DEFAULT_PANE;
		}
		this.renderPanes(this.texts());
		this.windows.forEach((window) => this.renderWindow(window));
		this.findActive();
		if (hadFocus) {
			this.paneSwitcher?.querySelector<HTMLElement>('[aria-pressed="true"]')?.focus();
		}
		if (user) {
			this.panesChanged();
		}
		return true;
	}

	/**
	 * Moves a window to another pane, leaving the pane shown as it is.
	 *
	 * @param id the id of the window
	 * @param pane the id of the pane
	 * @param user whether the user did, calling back
	 * @returns whether the window moved
	 */
	moveToPane(id: string, pane: string, user = false): boolean {
		const window = this.windows.get(id);
		if (!window || this.paneOf(pane) === null || window.state.pane === pane) {
			return false;
		}
		const hadFocus = window.element.contains(document.activeElement);
		window.state.pane = pane;
		this.renderWindow(window);
		this.findActive();
		if (hadFocus) {
			const front = this.active ? this.windows.get(this.active) : undefined;
			if (front) {
				this.focus(front);
			} else {
				this.paneSwitcher?.querySelector<HTMLElement>('[data-pane="' + pane + '"]')?.focus();
			}
		}
		if (user) {
			this.changed();
		}
		return true;
	}

	/**
	 * Asks the user for the name of a new pane and adds it, without showing it.
	 *
	 * @returns the pane, null when the user cancelled
	 */
	async requestNewPane(): Promise<DesktopPane | null> {
		const texts = this.texts();
		const name = await this.askPaneName(texts.newPane, format(texts.newPaneName,
			String(this.panes().length + 1)));
		return name === null ? null : this.addPane(name, true);
	}

	/**
	 * Asks the user for the new name of a pane and renames it.
	 *
	 * @param id the id of the pane, the one shown when left out
	 * @returns whether the user renamed it
	 */
	async requestRenamePane(id = this.activePane()): Promise<boolean> {
		const pane = this.panes().find((other) => other.id === id);
		if (!pane) {
			return false;
		}
		const name = await this.askPaneName(format(this.texts().renamePaneTitle,
			this.paneName(pane)), this.paneName(pane));
		return name !== null && this.renamePane(id, name, true);
	}

	/**
	 * @returns the types of windows the launcher opens
	 */
	windowTypes(): WindowType[] {
		return [...this.options.windowTypes ?? []];
	}

	/**
	 * Registers a type of window, which the launcher opens; one of the same name goes.
	 *
	 * @param type the type
	 */
	registerWindowType(type: WindowType): void {
		this.options.windowTypes = [...this.windowTypes().filter((other) =>
			other.type !== type.type), type];
		this.closeMenu(false);
		this.renderLauncher(this.texts());
	}

	/**
	 * Takes a type of window out of the launcher.
	 *
	 * @param type the name of the type
	 */
	unregisterWindowType(type: string): void {
		this.options.windowTypes = this.windowTypes().filter((other) => other.type !== type);
		this.closeMenu(false);
		this.renderLauncher(this.texts());
	}

	/**
	 * Opens a window of a type, in front: one the type makes, closable unless it says otherwise,
	 * or one the page adds, which onOpenWindow of the options asks for.
	 *
	 * @param type the name of the type
	 * @param pane the id of the pane, the one shown when left out
	 * @param user whether the user did, calling back
	 * @returns the id of the window the type made, null when the page adds it or there is no
	 *          such type
	 */
	openWindow(type: string, pane = this.activePane(), user = false): string | null {
		const windowType = this.windowTypes().find((other) => other.type === type);
		if (!windowType) {
			return null;
		}
		const target = this.paneOf(pane) ?? this.activePane();
		if (!windowType.create) {
			this.options.onOpenWindow?.(type, target);
			return null;
		}
		let id: string;
		do {
			id = (this.element.id || 'antilia-desktop') + '-' + type + '-' + (++this.createdCount);
		} while (document.getElementById(id));
		const element = windowType.create(id, target);
		element.id = id;
		this.add(element, 'window');
		element.setAttribute('data-pane', target);
		if (!element.hasAttribute('data-closable')) {
			element.setAttribute('data-closable', 'true');
		}
		this.created.add(id);
		this.area.append(element);
		this.findWindows();
		this.bringForward(id, user);
		return id;
	}

	/**
	 * Opens a closed window again, in front, in the pane shown.
	 *
	 * @param id the id of the window
	 * @param user whether the user did, calling back
	 */
	open(id: string, user = false): void {
		const window = this.windows.get(id);
		if (!window) {
			return;
		}
		if (window.state.closed) {
			window.state.pane = this.activePane();
		}
		window.state.closed = false;
		this.bringForward(id, user);
	}

	/**
	 * Brings the next window of the pane shown to the front, or the previous one, in the order of
	 * the taskbar, restoring it when minimized.
	 *
	 * @param direction 1 for the next window, -1 for the previous one
	 * @param user whether the user did, calling back
	 */
	cycle(direction: 1 | -1 = 1, user = false): void {
		const pane = this.activePane();
		const open = Array.from(this.windows.values()).filter((window) => !window.state.closed &&
			window.state.pane === pane);
		if (!open.length) {
			return;
		}
		const index = open.findIndex((window) => window.element.id === this.active);
		const next = open[((index < 0 ? (direction > 0 ? -1 : 0) : index) + direction +
			open.length) % open.length];
		this.bringForward(next.element.id, user);
	}

	/**
	 * Lays out the windows shown, in the pane shown, neither minimized nor closed, restoring those
	 * maximized or folded, in the order of the page.
	 *
	 * @param layout in a grid, side by side, one above the other, or cascading
	 * @param user whether the user did, calling back
	 */
	tile(layout: TileLayout = 'grid', user = false): void {
		const area = this.areaSize();
		const pane = this.activePane();
		const windows = Array.from(this.windows.values()).filter((window) =>
			!window.state.minimized && !window.state.closed && window.state.pane === pane);
		if (!area || !windows.length) {
			return;
		}
		const count = windows.length;
		windows.forEach((window, index) => {
			const state = window.state;
			state.maximized = false;
			state.folded = false;
			if (layout === 'cascade') {
				const step = DEFAULTS.cascade;
				state.width = Math.max(Math.round(area.width * 0.6), this.minSize(window).width);
				state.height = Math.max(Math.round(area.height * 0.6), this.minSize(window).height);
				const room = Math.max(1, Math.floor(Math.min(area.width - state.width,
					area.height - state.height) / step) + 1);
				state.x = (index % room) * step;
				state.y = (index % room) * step;
				return;
			}
			const columns = layout === 'columns' ? count : layout === 'rows' ? 1 :
				Math.ceil(Math.sqrt(count));
			const rows = Math.ceil(count / columns);
			const row = Math.floor(index / columns);
			// the last row shares its width among the windows it has
			const inRow = row === rows - 1 ? count - row * columns : columns;
			const column = index - row * columns;
			const left = Math.round(area.width * column / inRow);
			const top = Math.round(area.height * row / rows);
			state.x = left;
			state.y = top;
			state.width = Math.round(area.width * (column + 1) / inRow) - left;
			state.height = Math.round(area.height * (row + 1) / rows) - top;
		});
		windows.forEach((window) => this.renderWindow(window));
		if (user) {
			this.changed();
		}
	}

	/**
	 * Lets the desktop fill the whole page, over all else, or takes it back into the page; the
	 * windows move and shrink to stay on the desktop when it gets smaller.
	 *
	 * @param expanded whether the desktop fills the page
	 * @param user whether the user did, calling back
	 */
	expand(expanded = true, user = false): void {
		if (this.isExpanded() === expanded) {
			return;
		}
		const hadFocus = this.expandButton !== null && this.expandButton === document.activeElement;
		this.options.expanded = expanded;
		this.renderExpanded();
		this.renderExpandButton(this.texts());
		if (hadFocus) {
			this.expandButton?.focus();
		}
		const fitted = expanded ? false : this.fit();
		if (user) {
			this.options.onExpand?.(expanded);
			this.changed();
		} else if (fitted) {
			this.changed();
		}
	}

	/**
	 * Moves the taskbar to another side of the desktop; the windows move and shrink to stay on
	 * the desktop.
	 *
	 * @param position the side of the desktop
	 * @param user whether the user did, calling back
	 */
	moveTaskbar(position: TaskbarPosition, user = false): void {
		if (this.position() === position) {
			return;
		}
		this.options.taskbar = position;
		this.render();
		const fitted = this.fit();
		if (user) {
			this.options.onTaskbar?.(position);
			this.changed();
		} else if (fitted) {
			this.changed();
		}
	}

	/**
	 * Moves the windows that are not maximized onto the desktop, wholly where they fit, shrinking
	 * those larger than it down to their smallest size.
	 *
	 * @returns whether a window moved or changed its size
	 */
	fit(): boolean {
		const area = this.areaSize();
		if (!area) {
			return false;
		}
		let fitted = false;
		this.windows.forEach((window) => {
			const state = window.state;
			const min = this.minSize(window);
			const width = Math.max(min.width, Math.min(state.width, area.width));
			const height = Math.max(min.height, Math.min(state.height, area.height));
			const x = Math.round(clamp(state.x, 0, area.width - width));
			const y = Math.round(clamp(state.y, 0, area.height - height));
			if (x !== state.x || y !== state.y || width !== state.width || height !== state.height) {
				Object.assign(state, { x, y, width, height });
				this.renderWindow(window);
				fitted = true;
			}
		});
		return fitted;
	}

	/**
	 * Changes the state of a window, without calling back: the page decides.
	 *
	 * @param id the id of the window
	 * @param state what changes; the rest stays
	 */
	setState(id: string, state: Partial<WindowState>): void {
		const window = this.windows.get(id);
		if (!window) {
			this.remembered.set(id, { ...this.remembered.get(id) ?? {
				x: 0, y: 0, width: DEFAULTS.width, height: DEFAULTS.height, minimized: false,
				maximized: false, folded: false, closed: false, order: this.windows.size + 1,
				pane: '' },
			...state });
			return;
		}
		Object.assign(window.state, Object.fromEntries(Object.entries(state)
			.filter(([, value]) => value !== undefined)));
		window.state.pane = this.paneOf(window.state.pane) ?? DEFAULT_PANE;
		this.limitSize(window);
		if (state.order !== undefined) {
			this.normalizeOrder();
		}
		this.renderWindow(window);
		this.findActive();
	}

	/**
	 * Takes other options; those left out stay as they were.
	 */
	configure(options: DesktopOptions = {}): void {
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as DesktopOptions;
		const position = this.position();
		const expanded = this.isExpanded();
		const pane = this.activePane();
		this.options = { ...this.options, ...defined };
		this.options.panes = this.normalizePanes(this.options.panes);
		this.options.activePane = this.paneOf(this.options.activePane) ?? DEFAULT_PANE;
		this.windows.forEach((window) => {
			window.state.pane = this.paneOf(window.state.pane) ?? DEFAULT_PANE;
		});
		this.render();
		if (this.activePane() !== pane) {
			this.findActive();
		}
		this.renderExpanded();
		this.windows.forEach((window) => this.renderTask(window));
		if (this.position() !== position || (expanded && !this.isExpanded())) {
			this.fit();
		}
	}

	/**
	 * Finds the windows again, after the page added or removed some, where the browser does not
	 * tell the desktop.
	 */
	refresh(): void {
		this.findWindows();
		this.windows.forEach((window) => {
			this.renderWindow(window);
			this.renderTask(window);
		});
	}

	/**
	 * Takes the desktop away, leaving the windows where the page has them; a taskbar it added
	 * goes too.
	 */
	destroy(): void {
		this.stopMoving?.();
		this.detach();
		this.windows.forEach((window) => {
			this.forget(window);
			PROPERTIES.forEach((property) => window.element.style.removeProperty(property));
			if (!window.element.getAttribute('style')) {
				window.element.removeAttribute('style');
			}
		});
		this.windows.clear();
		this.closeMenu(false);
		this.cancelDialog?.();
		this.menuButton?.remove();
		this.launcher?.remove();
		this.paneSwitcher?.remove();
		this.tiling?.remove();
		this.grip?.remove();
		this.expandButton?.remove();
		this.tasks.remove();
		if (this.isExpanded()) {
			this.options.expanded = false;
			this.renderExpanded();
		}
		if (this.ownTaskbar) {
			this.taskbar.remove();
		}
		if (instances.get(this.element) === this) {
			instances.delete(this.element);
		}
	}

	/**
	 * Stops listening, leaving the elements in place.
	 */
	private detach(): void {
		this.listeners.forEach(([target, type, listener, capture]) => {
			target.removeEventListener(type, listener, capture);
		});
		this.listeners = [];
		this.observer?.disconnect();
		this.observer = null;
		this.resizeObserver?.disconnect();
		this.resizeObserver = null;
		this.windows.forEach((window) => window.titleObserver?.disconnect());
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
		if (this.isExpanded()) {
			this.markPage(false);
		}
	}
}

/**
 * Makes a desktop of an element, in the shape of a Svelte action: use:desktop={options}.
 *
 * @param element the element of the desktop
 * @param options the side of the taskbar, the texts and the callbacks
 * @returns update() taking other options, destroy() taking the desktop away
 */
export function desktop(element: HTMLElement, options?: DesktopOptions): {
	update(options?: DesktopOptions): void;
	destroy(): void;
} {
	const instance = new Desktop(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
