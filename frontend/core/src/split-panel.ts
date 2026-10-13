/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * A split panel: two panes, side by side or one above the other, with a divider between them the
 * user drags, with the mouse, a pen or a finger, to share the space differently. The divider is a
 * WAI-ARIA separator: it takes the focus, the arrow keys move it, Home and End take it to its
 * limits, Enter collapses the first pane and restores it, and a double click takes it back to
 * where it started. A collapsible split panel has two buttons on its divider, which collapse a
 * pane, giving the other all the space, and restore it.
 *
 * Sizes are numbers, in percent, or strings in percent or in pixels: 30, '30%' or '250px'. The
 * position, the size of the first pane, is in percent of the space the divider leaves, and the
 * panes keep their shares when the split panel grows or shrinks; or in pixels, and the first pane
 * keeps its size. The limits, min and max of the first pane and minSecond of the second, may be
 * in either.
 *
 * new SplitPanel(element, options) works on markup the page already has: an element with two
 * panes and, between them, the divider, which it adds when the element has none. React, Svelte
 * and Wicket render all three, so the split panel never changes the elements they own; it adds
 * the collapse buttons to the divider. The sizes are set as CSS custom properties through the
 * CSSOM, so the split panel works under a strict Content Security Policy.
 */

import { onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, type Look, type LookPart } from './look.js';

/**
 * horizontal: the panes side by side, the divider standing between them; vertical: the panes one
 * above the other, the divider lying between them.
 */
export type SplitOrientation = 'horizontal' | 'vertical';

/** A size: a number in percent, or a string in percent or pixels, such as '30%' or '250px'. */
export type SplitSize = number | string;

/** A pane of a split panel: the first, left or top, or the second, right or bottom. */
export type SplitPane = 'first' | 'second';

/**
 * How a split panel shows its panes and how its divider behaves.
 */
export interface SplitPanelOptions {
	/** horizontal (the default) for the panes side by side, vertical for one above the other. */
	orientation?: SplitOrientation;
	/** The look of the panel; that of the element it is in when left out (see lookOf). */
	look?: Look;
	/**
	 * The size of the first pane, 50 (percent of the space the divider leaves) when left out. In
	 * pixels, '250px', the first pane keeps its size when the split panel grows or shrinks.
	 */
	position?: SplitSize;
	/** The smallest size of the first pane, 0 when left out. */
	min?: SplitSize;
	/** The largest size of the first pane, 100 (percent) when left out. */
	max?: SplitSize;
	/** The smallest size of the second pane, 0 when left out. */
	minSecond?: SplitSize;
	/** How far an arrow key moves the divider, 5 (percent) when left out. */
	step?: SplitSize;
	/** Whether the user can move the divider, true when left out. */
	resizable?: boolean;
	/** Whether the divider has buttons collapsing a pane and restoring it, false when left out. */
	collapsible?: boolean;
	/** The collapsed pane, if any, null when left out. */
	collapsed?: SplitPane | null;
	/** The accessible name of the divider. */
	label?: string;
	/** The title of the button collapsing the first pane. */
	collapseFirstLabel?: string;
	/** The title of the button collapsing the second pane. */
	collapseSecondLabel?: string;
	/** The title of the button restoring a collapsed pane. */
	restoreLabel?: string;
	/**
	 * Called with the position while the user moves the divider: a number in percent, or a
	 * string in pixels such as '260px' when the position was given in pixels.
	 */
	onResize?: (position: SplitSize) => void;
	/** Called with the position once the user has moved the divider: dropped it, or used a key. */
	onResized?: (position: SplitSize) => void;
	/** Called with the collapsed pane, or null, once the user has collapsed or restored a pane. */
	onCollapse?: (collapsed: SplitPane | null) => void;
	/**
	 * Called with the position and the collapsed pane once the user has changed either, for a
	 * page that keeps them, as Wicket does.
	 */
	onChange?: (position: SplitSize, collapsed: SplitPane | null) => void;
}

type Unit = '%' | 'px';

interface Length {
	value: number;
	unit: Unit;
}

const CLASS = 'antilia-split';
const DIVIDER_CLASS = 'antilia-split-divider';
const DRAGGING_CLASS = 'antilia-split-dragging';
const FIXED_CLASS = 'antilia-split-fixed';
const PIXELS_CLASS = 'antilia-split-pixels';
const COLLAPSIBLE_CLASS = 'antilia-split-collapsible';
const BUTTON_CLASS = 'antilia-split-collapse';
const ORIENTATION_CLASSES: Record<SplitOrientation, string> = {
	horizontal: 'antilia-split-horizontal',
	vertical: 'antilia-split-vertical'
};
const COLLAPSED_CLASSES: Record<SplitPane, string> = {
	first: 'antilia-split-collapsed-first',
	second: 'antilia-split-collapsed-second'
};

/** The custom properties holding the shares of the panes, as flex-grow factors. */
export const SPLIT_FIRST_PROPERTY = '--antilia-split-first';
export const SPLIT_SECOND_PROPERTY = '--antilia-split-second';
/** The custom property holding the size of the first pane, when in pixels. */
export const SPLIT_FIRST_SIZE_PROPERTY = '--antilia-split-first-size';
/** The custom properties holding the limits the style sheet keeps when the split panel resizes. */
export const SPLIT_FIRST_MIN_PROPERTY = '--antilia-split-first-min';
export const SPLIT_FIRST_MAX_PROPERTY = '--antilia-split-first-max';
export const SPLIT_SECOND_MIN_PROPERTY = '--antilia-split-second-min';

const PROPERTIES = [SPLIT_FIRST_PROPERTY, SPLIT_SECOND_PROPERTY, SPLIT_FIRST_SIZE_PROPERTY,
	SPLIT_FIRST_MIN_PROPERTY, SPLIT_FIRST_MAX_PROPERTY, SPLIT_SECOND_MIN_PROPERTY];

const DEFAULTS = { orientation: 'horizontal' as SplitOrientation, position: 50, step: 5,
	pixelStep: 20, resizable: true, collapsible: false, collapseFirstLabel: 'Collapse the first pane',
	collapseSecondLabel: 'Collapse the second pane', restoreLabel: 'Restore both panes' };

/** The split panels by their element. */
const instances = new WeakMap<Element, SplitPanel>();

let counter = 0;

function clamp(value: number, min: number, max: number): number {
	return Math.min(Math.max(value, min), Math.max(min, max));
}

/**
 * @returns the number, rounded to two decimals, as the split panel keeps sizes
 */
function round(value: number): number {
	return Math.round(value * 100) / 100;
}

/**
 * @returns the size as a value and its unit, null when it is not one
 */
export function parseSplitSize(size: SplitSize | null | undefined): Length | null {
	if (typeof size === 'number') {
		return Number.isFinite(size) ? { value: size, unit: '%' } : null;
	}
	const match = /^\s*(-?\d*\.?\d+)\s*(px|%)?\s*$/.exec(size ?? '');
	return match ? { value: Number(match[1]), unit: match[2] === 'px' ? 'px' : '%' } : null;
}

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

export class SplitPanel {
	readonly element: HTMLElement;

	options: SplitPanelOptions;

	first!: HTMLElement;

	second!: HTMLElement;

	divider!: HTMLElement;

	/** The size of the first pane, in percent or in pixels as the unit says, when not collapsed. */
	position = DEFAULTS.position;

	/** The unit of the position. */
	unit: Unit = '%';

	/** The collapsed pane, if any. */
	collapsed: SplitPane | null = null;

	/** Where the divider was at first, where a double click takes it back to. */
	private initial = DEFAULTS.position;

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private stopDragging: (() => void) | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** Whether the divider was added by the split panel, and is taken away again by destroy(). */
	private ownDivider = false;

	/** The look of the panel, whose classes the elements it makes get. */
	private readonly look: Look;

	/** The collapse buttons on the divider, when collapsible. */
	private buttons: Record<SplitPane, HTMLElement> | null = null;

	/**
	 * @param element the element of the panes: its first and last child elements are the panes,
	 *        and one between them with the class antilia-split-divider, if any, the divider
	 * @param options the orientation, the position, its limits and the callbacks
	 */
	constructor(element: HTMLElement, options: SplitPanelOptions = {}) {
		this.element = element;
		this.options = { ...options };
		this.look = options.look ?? lookOf(element);
		instances.get(element)?.destroy();
		instances.set(element, this);
		this.findParts();
		this.place(options.position);
		this.collapsed = this.collapsible() ? options.collapsed ?? null : null;
		this.render();
		this.bind();
		this.stopWatchingRemoval = onRemoving(element, () => this.detach());
	}

	/**
	 * Makes a split panel of the element, as Wicket does.
	 *
	 * @param element the element or its id
	 * @param options the orientation, the position, its limits and the callbacks
	 * @returns the split panel, null when there is no such element
	 */
	static attach(element: HTMLElement | string, options?: SplitPanelOptions): SplitPanel | null {
		const resolved = resolve(element);
		return resolved ? new SplitPanel(resolved, options) : null;
	}

	/**
	 * @param target the element of the split panel or its id
	 * @returns the split panel, if there is one
	 */
	static of(target: HTMLElement | string | null | undefined): SplitPanel | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		return (element && instances.get(element)) || null;
	}

	private findParts(): void {
		const children = Array.from(this.element.children) as HTMLElement[];
		const divider = children.find((child) => child.classList.contains(DIVIDER_CLASS));
		const panes = children.filter((child) => child !== divider);
		if (panes.length < 2) {
			throw new Error('A split panel needs two panes, ' + this.element.id + ' has ' +
				panes.length);
		}
		this.first = panes[0];
		this.second = panes[panes.length - 1];
		if (divider) {
			this.divider = divider;
		} else {
			this.divider = document.createElement('div');
			this.divider.className = this.css('split-divider');
			this.element.insertBefore(this.divider, this.second);
			this.ownDivider = true;
		}
	}

	/**
	 * Takes a position as where the divider is and where a double click takes it back to, in its
	 * unit.
	 */
	private place(position: SplitSize | undefined): void {
		const parsed = parseSplitSize(position) ?? { value: DEFAULTS.position, unit: '%' };
		this.unit = parsed.unit;
		this.initial = this.limit(parsed.value);
		this.position = this.initial;
	}

	orientation(): SplitOrientation {
		return this.options.orientation === 'vertical' ? 'vertical' : 'horizontal';
	}

	isResizable(): boolean {
		return this.options.resizable ?? DEFAULTS.resizable;
	}

	private collapsible(): boolean {
		return this.options.collapsible ?? DEFAULTS.collapsible;
	}

	/**
	 * @returns the position as the page gave it: a number in percent, or a string in pixels
	 */
	value(): SplitSize {
		return this.unit === 'px' ? this.position + 'px' : this.position;
	}

	/**
	 * @returns the size of the space the panes share, without the divider, and where it starts,
	 *          in pixels along the orientation; 0 when the split panel is not laid out
	 */
	private space(): { start: number; size: number } {
		const rect = this.element.getBoundingClientRect();
		const divider = this.divider.getBoundingClientRect();
		// within the border of the split panel
		const style = getComputedStyle(this.element);
		const width = (side: string) => parseFloat(style.getPropertyValue('border-' + side +
			'-width')) || 0;
		return this.orientation() === 'horizontal' ?
			{ start: rect.left + width('left'), size: Math.max(rect.width - width('left') -
				width('right') - divider.width, 0) } :
			{ start: rect.top + width('top'), size: Math.max(rect.height - width('top') -
				width('bottom') - divider.height, 0) };
	}

	/**
	 * @returns the size in the unit of the position, null when it is none or needs the space the
	 *          split panel has not
	 */
	private convert(size: SplitSize | undefined, space: number): number | null {
		const length = parseSplitSize(size);
		if (!length) {
			return null;
		}
		if (length.unit === this.unit) {
			return length.value;
		}
		if (space <= 0) {
			return null;
		}
		return length.unit === 'px' ? length.value / space * 100 : length.value / 100 * space;
	}

	/**
	 * @returns how far the first pane goes, in the unit of the position
	 */
	private bounds(space = this.space().size): { min: number; max: number } {
		const whole = this.unit === '%' ? 100 : space > 0 ? space : Infinity;
		const min = Math.max(this.convert(this.options.min, space) ?? 0, 0);
		let max = Math.min(this.convert(this.options.max, space) ?? whole, whole);
		const second = this.convert(this.options.minSecond, space);
		if (second !== null && whole !== Infinity) {
			max = Math.min(max, whole - second);
		}
		return { min, max: Math.max(min, max) };
	}

	/**
	 * @returns the position within the limits, rounded to two decimals
	 */
	private limit(position: number): number {
		const { min, max } = this.bounds();
		return round(clamp(Number.isFinite(position) ? position : min, min, max));
	}

	/**
	 * @returns a value in the unit of the position as a percentage of the space, null when it
	 *          needs the space the split panel has not
	 */
	private percent(value: number, space: number): number | null {
		if (this.unit === '%') {
			return value;
		}
		return space > 0 ? value / space * 100 : null;
	}

	/**
	 * @returns the classes of parts of the panel in its look
	 */
	private css(...parts: LookPart[]): string {
		return partClasses(this.look, ...parts);
	}

	/**
	 * Gives an element of the panel the classes of parts in its look.
	 */
	private add(element: Element, ...parts: LookPart[]): void {
		element.classList.add(...this.css(...parts).split(' '));
	}

	private render(): void {
		const element = this.element;
		const orientation = this.orientation();
		this.add(element, 'split');
		element.classList.toggle(ORIENTATION_CLASSES.horizontal, orientation === 'horizontal');
		element.classList.toggle(ORIENTATION_CLASSES.vertical, orientation === 'vertical');
		element.classList.toggle(FIXED_CLASS, !this.isResizable());
		element.classList.toggle(PIXELS_CLASS, this.unit === 'px');
		element.classList.toggle(COLLAPSIBLE_CLASS, this.collapsible());
		element.classList.toggle(DRAGGING_CLASS, this.stopDragging !== null);
		this.add(this.first, 'split-pane', 'split-first');
		this.add(this.second, 'split-pane', 'split-second');
		if (!this.first.id) {
			this.first.id = (element.id || 'antilia-split-' + (++counter)) + '-first';
		}

		const divider = this.divider;
		this.add(divider, 'split-divider');
		divider.setAttribute('role', 'separator');
		// the separator stands upright between panes side by side
		divider.setAttribute('aria-orientation', orientation === 'horizontal' ? 'vertical' :
			'horizontal');
		divider.setAttribute('aria-controls', this.first.id);
		if (this.options.label) {
			divider.setAttribute('aria-label', this.options.label);
		} else {
			divider.removeAttribute('aria-label');
		}
		if (this.isResizable()) {
			divider.tabIndex = 0;
			divider.removeAttribute('aria-disabled');
		} else {
			divider.removeAttribute('tabindex');
			divider.setAttribute('aria-disabled', 'true');
		}
		this.renderButtons();
		this.renderLimits();
		this.show();
	}

	/**
	 * Adds the collapse buttons to the divider, or takes them away. They are for the pointer: the
	 * keyboard collapses and restores with Enter, so they take no focus and are hidden from
	 * assistive technologies, as anything in a separator is.
	 */
	private renderButtons(): void {
		const show = this.collapsible() && this.isResizable();
		if (!show) {
			this.removeButtons();
			return;
		}
		if (!this.buttons) {
			const button = (pane: SplitPane) => {
				const element = document.createElement('span');
				element.className = this.css('split-collapse') + ' ' + BUTTON_CLASS + '-' + pane;
				element.setAttribute('aria-hidden', 'true');
				return element;
			};
			this.buttons = { first: button('first'), second: button('second') };
			this.divider.append(this.buttons.first, this.buttons.second);
		}
	}

	private removeButtons(): void {
		if (this.buttons) {
			this.buttons.first.remove();
			this.buttons.second.remove();
			this.buttons = null;
		}
	}

	/**
	 * Gives the style sheet the limits it keeps when the split panel grows or shrinks: those in
	 * pixels, and with the position in pixels those in percent too. Within the shares of the
	 * panes, the script keeps those in percent. Each split panel sets all three, so that one in
	 * a pane of another does not inherit the other's.
	 */
	private renderLimits(): void {
		const style = this.element.style;
		const css = (size: SplitSize | undefined, property: string, none: string) => {
			const length = parseSplitSize(size);
			style.setProperty(property, length && length.value > 0 &&
				(length.unit === 'px' || this.unit === 'px') ? length.value + length.unit : none);
		};
		css(this.options.min, SPLIT_FIRST_MIN_PROPERTY, '0');
		css(this.options.max, SPLIT_FIRST_MAX_PROPERTY, 'none');
		css(this.options.minSecond, SPLIT_SECOND_MIN_PROPERTY, '0');
	}

	/**
	 * Shows the position: the sizes of the panes, the collapsed one, the buttons and the value of
	 * the separator.
	 */
	private show(): void {
		const style = this.element.style;
		if (this.unit === 'px') {
			style.setProperty(SPLIT_FIRST_SIZE_PROPERTY, this.position + 'px');
			style.removeProperty(SPLIT_FIRST_PROPERTY);
			style.removeProperty(SPLIT_SECOND_PROPERTY);
		} else {
			style.setProperty(SPLIT_FIRST_PROPERTY, String(this.position));
			style.setProperty(SPLIT_SECOND_PROPERTY, String(round(100 - this.position)));
			style.removeProperty(SPLIT_FIRST_SIZE_PROPERTY);
		}
		this.element.classList.toggle(COLLAPSED_CLASSES.first, this.collapsed === 'first');
		this.element.classList.toggle(COLLAPSED_CLASSES.second, this.collapsed === 'second');
		if (this.buttons) {
			const labels = this.options;
			// a button towards a collapsed pane restores it, and the other one is not needed
			this.buttons.first.title = this.collapsed === 'second' ?
				labels.restoreLabel ?? DEFAULTS.restoreLabel :
				labels.collapseFirstLabel ?? DEFAULTS.collapseFirstLabel;
			this.buttons.second.title = this.collapsed === 'first' ?
				labels.restoreLabel ?? DEFAULTS.restoreLabel :
				labels.collapseSecondLabel ?? DEFAULTS.collapseSecondLabel;
			this.buttons.first.hidden = this.collapsed === 'first';
			this.buttons.second.hidden = this.collapsed === 'second';
		}

		const space = this.space().size;
		const divider = this.divider;
		const bounds = this.bounds(space);
		const collapsible = this.collapsible();
		const min = collapsible ? 0 : this.percent(bounds.min, space);
		const max = collapsible ? 100 : this.percent(bounds.max, space);
		const now = this.collapsed === 'first' ? 0 : this.collapsed === 'second' ? 100 :
			this.percent(this.position, space);
		const attribute = (name: string, value: number | null) => {
			if (value === null || !Number.isFinite(value)) {
				divider.removeAttribute(name);
			} else {
				divider.setAttribute(name, String(Math.round(value)));
			}
		};
		attribute('aria-valuenow', now);
		attribute('aria-valuemin', this.isResizable() ? min : null);
		attribute('aria-valuemax', this.isResizable() ? max : null);
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void): void {
		target.addEventListener(type, listener as EventListener);
		this.listeners.push([target, type, listener as EventListener]);
	}

	private bind(): void {
		this.listen<PointerEvent>(this.divider, 'pointerdown', (event) => this.onPointerDown(event));
		this.listen<KeyboardEvent>(this.divider, 'keydown', (event) => this.onKeyDown(event));
		this.listen<MouseEvent>(this.divider, 'click', (event) => this.onClick(event));
		this.listen<MouseEvent>(this.divider, 'dblclick', (event) => {
			if (this.isResizable() && !this.button(event)) {
				this.moveTo(this.initial, true);
			}
		});
	}

	/**
	 * @returns the pane of the collapse button the event happened on, if any
	 */
	private button(event: Event): SplitPane | null {
		const target = event.target as Element | null;
		if (!this.buttons || !target) {
			return null;
		}
		return this.buttons.first.contains(target) ? 'first' :
			this.buttons.second.contains(target) ? 'second' : null;
	}

	private onClick(event: MouseEvent): void {
		const pane = this.button(event);
		if (!pane) {
			return;
		}
		event.preventDefault();
		// towards a collapsed pane, the button restores it
		const other: SplitPane = pane === 'first' ? 'second' : 'first';
		this.collapse(this.collapsed === other ? null : pane, true);
	}

	private onPointerDown(event: PointerEvent): void {
		if (!this.isResizable() || event.button !== 0 || this.button(event)) {
			return;
		}
		event.preventDefault();
		this.stopDragging?.();
		const horizontal = this.orientation() === 'horizontal';
		const dividerRect = this.divider.getBoundingClientRect();
		// where the pointer took the divider, so that it does not jump under it
		const grip = horizontal ? event.clientX - dividerRect.left : event.clientY - dividerRect.top;
		const divider = this.divider;
		divider.setPointerCapture?.(event.pointerId);
		divider.focus({ preventScroll: true });
		this.element.classList.add(DRAGGING_CLASS);
		const start = this.position;
		const collapsed = this.collapsed;

		const move = (moveEvent: PointerEvent) => {
			const space = this.space();
			if (space.size <= 0) {
				return;
			}
			const offset = (horizontal ? moveEvent.clientX : moveEvent.clientY) - space.start - grip;
			this.moveTo(this.unit === 'px' ? offset : offset / space.size * 100, false);
		};
		const end = () => {
			this.stopDragging = null;
			this.element.classList.remove(DRAGGING_CLASS);
			divider.removeEventListener('pointermove', move);
			divider.removeEventListener('pointerup', end);
			divider.removeEventListener('pointercancel', end);
			divider.removeEventListener('lostpointercapture', end);
			if (this.position !== start) {
				this.options.onResized?.(this.value());
			}
			if (this.position !== start || this.collapsed !== collapsed) {
				this.options.onChange?.(this.value(), this.collapsed);
			}
		};
		divider.addEventListener('pointermove', move);
		divider.addEventListener('pointerup', end);
		divider.addEventListener('pointercancel', end);
		divider.addEventListener('lostpointercapture', end);
		this.stopDragging = end;
	}

	/**
	 * @returns how far an arrow key moves the divider, in the unit of the position
	 */
	private step(): number {
		const step = this.convert(this.options.step ?? DEFAULTS.step, this.space().size);
		return step ?? (this.unit === 'px' ? DEFAULTS.pixelStep : DEFAULTS.step);
	}

	/**
	 * @returns where the divider shows, in the unit of the position: at an end when a pane is
	 *          collapsed
	 */
	private shown(): number {
		if (this.collapsed === 'first') {
			return 0;
		}
		if (this.collapsed === 'second') {
			return this.unit === '%' ? 100 : this.space().size;
		}
		return this.position;
	}

	private onKeyDown(event: KeyboardEvent): void {
		if (!this.isResizable() || event.altKey || event.ctrlKey || event.metaKey) {
			return;
		}
		const horizontal = this.orientation() === 'horizontal';
		let position: number;
		switch (event.key) {
			case horizontal ? 'ArrowLeft' : 'ArrowUp':
				position = this.shown() - this.step();
				break;
			case horizontal ? 'ArrowRight' : 'ArrowDown':
				position = this.shown() + this.step();
				break;
			case 'Home':
				position = this.bounds().min;
				break;
			case 'End':
				position = this.bounds().max;
				break;
			case 'Enter':
				if (!this.collapsible()) {
					return;
				}
				event.preventDefault();
				this.collapse(this.collapsed ? null : 'first', true);
				return;
			default:
				return;
		}
		event.preventDefault();
		this.moveTo(position, true);
	}

	/**
	 * Moves the divider, restoring a collapsed pane, calling onResize and, when done, onResized
	 * if it moved.
	 *
	 * @param position the size of the first pane in the unit of the position, kept within the
	 *        limits
	 * @param done whether the user is done moving it
	 */
	private moveTo(position: number, done: boolean): void {
		const next = this.limit(position);
		const restored = this.collapsed !== null;
		if (next === this.position && !restored) {
			return;
		}
		const moved = next !== this.position;
		this.position = next;
		this.collapsed = null;
		this.show();
		if (restored) {
			this.options.onCollapse?.(null);
		}
		if (moved) {
			this.options.onResize?.(this.value());
		}
		if (done) {
			if (moved) {
				this.options.onResized?.(this.value());
			}
			this.options.onChange?.(this.value(), null);
		}
	}

	/**
	 * Collapses a pane, giving the other all the space, or restores it.
	 *
	 * @param pane the pane to collapse, null to restore both
	 * @param user whether the user did, calling back
	 */
	private collapse(pane: SplitPane | null, user: boolean): void {
		if (pane === this.collapsed) {
			return;
		}
		this.collapsed = pane;
		this.show();
		if (user) {
			this.options.onCollapse?.(pane);
			this.options.onChange?.(this.value(), pane);
		}
	}

	/**
	 * Moves the divider, without calling back: the page decides where it is. A pane collapsed
	 * stays so.
	 *
	 * @param position the size of the first pane, kept within the limits; in pixels, '250px',
	 *        the first pane keeps its size when the split panel grows or shrinks
	 */
	setPosition(position: SplitSize): void {
		const parsed = parseSplitSize(position);
		if (!parsed) {
			return;
		}
		if (parsed.unit !== this.unit) {
			this.unit = parsed.unit;
			this.element.classList.toggle(PIXELS_CLASS, this.unit === 'px');
			this.initial = this.limit(parsed.value);
			this.renderLimits();
		}
		this.position = this.limit(parsed.value);
		this.show();
	}

	/**
	 * Collapses a pane or restores it, without calling back: the page decides.
	 *
	 * @param pane the pane to collapse, null to restore both; a split panel that is not
	 *        collapsible collapses none
	 */
	setCollapsed(pane: SplitPane | null): void {
		this.collapse(this.collapsible() ? pane : null, false);
	}

	/**
	 * Takes other options; those left out stay as they were. A new position moves the divider,
	 * and is where a double click takes it back to.
	 */
	configure(options: SplitPanelOptions = {}): void {
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as SplitPanelOptions;
		this.options = { ...this.options, ...defined };
		if (!this.isResizable()) {
			this.stopDragging?.();
		}
		if (defined.position !== undefined) {
			this.place(defined.position);
		} else {
			this.position = this.limit(this.position);
		}
		if (!this.collapsible()) {
			this.collapsed = null;
		} else if (options.collapsed !== undefined) {
			this.collapsed = options.collapsed;
		}
		this.render();
	}

	/**
	 * Takes the split panel away, leaving the panes; a divider it added goes too.
	 */
	destroy(): void {
		this.stopDragging?.();
		this.detach();
		this.removeButtons();
		const element = this.element;
		element.classList.remove(CLASS, FIXED_CLASS, DRAGGING_CLASS, PIXELS_CLASS, COLLAPSIBLE_CLASS,
			...Object.values(ORIENTATION_CLASSES), ...Object.values(COLLAPSED_CLASSES));
		if (!element.classList.length) {
			element.removeAttribute('class');
		}
		PROPERTIES.forEach((property) => element.style.removeProperty(property));
		if (!element.getAttribute('style')) {
			element.removeAttribute('style');
		}
		if (this.ownDivider) {
			this.divider.remove();
		}
		if (instances.get(element) === this) {
			instances.delete(element);
		}
	}

	/**
	 * Stops listening, leaving the elements in place.
	 */
	private detach(): void {
		this.listeners.forEach(([target, type, listener]) => {
			target.removeEventListener(type, listener);
		});
		this.listeners = [];
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
	}
}

/**
 * Makes a split panel of an element, in the shape of a Svelte action: use:splitPanel={options}.
 *
 * @param element the element of the panes
 * @param options the orientation, the position, its limits and the callbacks
 * @returns update() taking other options, destroy() taking the split panel away
 */
export function splitPanel(element: HTMLElement, options?: SplitPanelOptions): {
	update(options?: SplitPanelOptions): void;
	destroy(): void;
} {
	const instance = new SplitPanel(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
