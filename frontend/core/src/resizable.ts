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
 * An element the user resizes: by its bottom edge (vertical), its right edge (horizontal) or its
 * bottom right corner (both), dragged with the mouse, a pen or a finger. The handle on that edge
 * or corner takes the focus: the arrow keys resize the element, ten pixels at a time or fifty
 * with Shift, Home takes it to its smallest size and End to its largest, and a double click takes
 * it back to the size it started with. Escape, while dragging, puts it back where the drag began.
 *
 * Sizes are in pixels, the size of the element's border box; a size of null leaves the element as
 * the style sheets of the page make it, which is how it starts unless given a width or a height.
 * The limits, minWidth, minHeight, maxWidth and maxHeight, are in pixels too.
 *
 * new Resizable(element, options) works on an element the page already has, which it gives the
 * class antilia-resizable and the handle, as its last child; React, Svelte and Wicket render the
 * element, the resizable adds the handle. The sizes are set as CSS custom properties through the
 * CSSOM, so the resizable works under a strict Content Security Policy.
 */

import { onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, type Look, type LookPart } from './look.js';

/**
 * vertical: the height alone, by the bottom edge; horizontal: the width alone, by the right edge;
 * both: both, by the bottom right corner.
 */
export type ResizeAxis = 'vertical' | 'horizontal' | 'both';

/** The axes a resizable may have. */
export const RESIZE_AXES: readonly ResizeAxis[] = Object.freeze(['vertical', 'horizontal', 'both']);

/**
 * The size of a resizable element, its border box in pixels; null for a dimension left as the
 * style sheets of the page make it.
 */
export interface ResizableSize {
	width: number | null;
	height: number | null;
}

/**
 * How an element resizes.
 */
export interface ResizableOptions {
	/** The dimensions the user changes, both when left out. */
	axis?: ResizeAxis;
	/** The width the element starts with, in pixels; as the page makes it when left out or null. */
	width?: number | null;
	/** The height the element starts with, in pixels; as the page makes it when left out or null. */
	height?: number | null;
	/** The smallest width, in pixels, 32 when left out. */
	minWidth?: number;
	/** The smallest height, in pixels, 32 when left out. */
	minHeight?: number;
	/** The largest width, in pixels, none when left out. */
	maxWidth?: number | null;
	/** The largest height, in pixels, none when left out. */
	maxHeight?: number | null;
	/** How far an arrow key resizes the element, in pixels, 10 when left out; five times with Shift. */
	step?: number;
	/** Whether the user can resize the element, true when left out. */
	enabled?: boolean;
	/** The accessible name of the handle. */
	label?: string;
	/** The look of the handle; that of the element when left out (see lookOf). */
	look?: Look;
	/** Called with the size while the user drags the handle. */
	onResize?: (size: ResizableSize) => void;
	/**
	 * Called with the size once the user has resized the element: dropped the handle, used a key
	 * or double clicked it.
	 */
	onResized?: (size: ResizableSize) => void;
}

/** The custom properties holding the size of the element. */
export const RESIZABLE_WIDTH_PROPERTY = '--antilia-resizable-width';
export const RESIZABLE_HEIGHT_PROPERTY = '--antilia-resizable-height';

/** The classes of a resizable element, its handle and their states. */
export const RESIZABLE_CLASSES = Object.freeze({
	resizable: 'antilia-resizable',
	handle: 'antilia-resizable-handle',
	/** The element while the user drags its handle. */
	resizing: 'antilia-resizable-resizing',
	/** The element while its width, or its height, is the user's. */
	width: 'antilia-resizable-sized-width',
	height: 'antilia-resizable-sized-height',
	/** The element while the user cannot resize it. */
	disabled: 'antilia-resizable-disabled'
});

const AXIS_CLASSES: Record<ResizeAxis, string> = {
	vertical: 'antilia-resizable-vertical',
	horizontal: 'antilia-resizable-horizontal',
	both: 'antilia-resizable-both'
};

const DEFAULTS = { axis: 'both' as ResizeAxis, min: 32, step: 10, bigStep: 5,
	label: 'Drag, or use the arrow keys, to resize' };

/** The resizables by their element. */
const instances = new WeakMap<Element, Resizable>();

let counter = 0;

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

/**
 * @returns the size in whole pixels, null for none
 */
function pixels(value: number | null | undefined): number | null {
	return typeof value === 'number' && Number.isFinite(value) ? Math.round(value) : null;
}

export class Resizable {
	readonly element: HTMLElement;

	options: ResizableOptions;

	/** The handle on the edge or corner the user drags. */
	readonly handle: HTMLElement;

	/** The size of the element: its width and height, null where the page's style sheets set it. */
	size: ResizableSize;

	/** The size it started with, where a double click takes it back to. */
	private initial: ResizableSize;

	private readonly look: Look;

	private listeners: Array<[EventTarget, string, EventListener]> = [];

	private stopDragging: ((cancel: boolean) => void) | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/**
	 * @param element the element to resize
	 * @param options the dimensions the user changes, the sizes the element starts with, the
	 *        limits and the callbacks
	 */
	constructor(element: HTMLElement, options: ResizableOptions = {}) {
		this.element = element;
		this.options = { ...options };
		this.look = options.look ?? lookOf(element);
		instances.get(element)?.destroy();
		instances.set(element, this);
		this.size = { width: null, height: null };
		this.initial = this.size;
		this.handle = document.createElement('span');
		this.handle.className = this.css('resizable-handle');
		element.append(this.handle);
		this.place(options);
		this.render();
		this.bind();
		this.stopWatchingRemoval = onRemoving(element, () => this.detach());
	}

	/**
	 * Makes an element resizable, as Wicket does.
	 *
	 * @param element the element or its id
	 * @param options the dimensions the user changes, the sizes, the limits and the callbacks
	 * @returns the resizable, null when there is no such element
	 */
	static attach(element: HTMLElement | string, options?: ResizableOptions): Resizable | null {
		const resolved = resolve(element);
		return resolved ? new Resizable(resolved, options) : null;
	}

	/**
	 * @param target the resizable element or its id
	 * @returns the resizable, if there is one
	 */
	static of(target: HTMLElement | string | null | undefined): Resizable | null {
		const element = typeof target === 'string' ? document.getElementById(target) : target;
		return (element && instances.get(element)) || null;
	}

	axis(): ResizeAxis {
		const axis = this.options.axis;
		return axis && RESIZE_AXES.includes(axis) ? axis : DEFAULTS.axis;
	}

	isEnabled(): boolean {
		return this.options.enabled ?? true;
	}

	/**
	 * @returns whether the user changes the width
	 */
	private horizontal(): boolean {
		return this.axis() !== 'vertical';
	}

	/**
	 * @returns whether the user changes the height
	 */
	private vertical(): boolean {
		return this.axis() !== 'horizontal';
	}

	/**
	 * Takes the sizes of the options as those the element has and starts with, within the limits;
	 * a dimension the user does not change keeps the page's size.
	 */
	private place(options: ResizableOptions): void {
		this.size = this.within({ width: pixels(options.width), height: pixels(options.height) });
		this.initial = { ...this.size };
	}

	/**
	 * @returns the size within the limits, in the dimensions the user changes: null in the others
	 */
	private within(size: ResizableSize): ResizableSize {
		return {
			width: this.horizontal() ? this.limit(size.width, 'width') : null,
			height: this.vertical() ? this.limit(size.height, 'height') : null
		};
	}

	/**
	 * @returns the size within the limits of its dimension, null for none
	 */
	private limit(value: number | null, dimension: 'width' | 'height'): number | null {
		if (value === null) {
			return null;
		}
		const { min, max } = this.bounds(dimension);
		return Math.round(Math.min(Math.max(value, min), Math.max(min, max)));
	}

	/**
	 * @returns how small and how large a dimension goes, in pixels
	 */
	private bounds(dimension: 'width' | 'height'): { min: number; max: number } {
		const options = this.options;
		const min = dimension === 'width' ? options.minWidth : options.minHeight;
		const max = dimension === 'width' ? options.maxWidth : options.maxHeight;
		return {
			min: Math.max(pixels(min) ?? DEFAULTS.min, 0),
			max: pixels(max) ?? Infinity
		};
	}

	/**
	 * @returns the classes of parts in the look of the resizable
	 */
	private css(...parts: LookPart[]): string {
		return partClasses(this.look, ...parts);
	}

	private render(): void {
		const element = this.element;
		const axis = this.axis();
		element.classList.add(...this.css('resizable').split(' '));
		Object.entries(AXIS_CLASSES).forEach(([each, css]) => element.classList.toggle(css,
			each === axis));
		element.classList.toggle(RESIZABLE_CLASSES.disabled, !this.isEnabled());
		if (!element.id) {
			element.id = 'antilia-resizable-' + (++counter);
		}
		const handle = this.handle;
		handle.setAttribute('role', 'separator');
		handle.setAttribute('aria-controls', element.id);
		handle.setAttribute('aria-label', this.options.label ?? DEFAULTS.label);
		// a separator lying under the element moves up and down, one standing beside it left and
		// right; the corner moves both ways, as no separator does
		if (axis === 'both') {
			handle.removeAttribute('aria-orientation');
		} else {
			handle.setAttribute('aria-orientation', axis === 'vertical' ? 'horizontal' : 'vertical');
		}
		if (this.isEnabled()) {
			handle.tabIndex = 0;
			handle.removeAttribute('aria-disabled');
			handle.hidden = false;
		} else {
			handle.removeAttribute('tabindex');
			handle.setAttribute('aria-disabled', 'true');
			handle.hidden = true;
		}
		this.show();
	}

	/**
	 * Shows the size: the custom properties and the classes taking them, and the value of the
	 * separator.
	 */
	private show(): void {
		const element = this.element;
		const set = (value: number | null, property: string, css: string) => {
			if (value === null) {
				element.style.removeProperty(property);
			} else {
				element.style.setProperty(property, value + 'px');
			}
			element.classList.toggle(css, value !== null);
		};
		set(this.size.width, RESIZABLE_WIDTH_PROPERTY, RESIZABLE_CLASSES.width);
		set(this.size.height, RESIZABLE_HEIGHT_PROPERTY, RESIZABLE_CLASSES.height);
		if (!element.getAttribute('style')) {
			element.removeAttribute('style');
		}

		const handle = this.handle;
		const axis = this.axis();
		const shown = this.measure();
		const attribute = (name: string, value: number | null) => {
			if (value === null || !Number.isFinite(value)) {
				handle.removeAttribute(name);
			} else {
				handle.setAttribute(name, String(Math.round(value)));
			}
		};
		if (axis === 'both') {
			['aria-valuenow', 'aria-valuemin', 'aria-valuemax'].forEach((name) =>
				handle.removeAttribute(name));
			handle.setAttribute('aria-valuetext', Math.round(shown.width) + ' × ' +
				Math.round(shown.height) + ' px');
			return;
		}
		const dimension = axis === 'vertical' ? 'height' : 'width';
		const bounds = this.bounds(dimension);
		attribute('aria-valuenow', shown[dimension] || null);
		attribute('aria-valuemin', bounds.min);
		attribute('aria-valuemax', bounds.max);
		handle.setAttribute('aria-valuetext', Math.round(shown[dimension]) + ' px');
	}

	/**
	 * @returns the size the element has: the user's, or that of its border box as laid out
	 */
	private measure(): { width: number; height: number } {
		const rect = this.element.getBoundingClientRect();
		return {
			width: this.size.width ?? rect.width,
			height: this.size.height ?? rect.height
		};
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void): void {
		target.addEventListener(type, listener as EventListener);
		this.listeners.push([target, type, listener as EventListener]);
	}

	private bind(): void {
		this.listen<PointerEvent>(this.handle, 'pointerdown', (event) => this.onPointerDown(event));
		this.listen<KeyboardEvent>(this.handle, 'keydown', (event) => this.onKeyDown(event));
		this.listen<MouseEvent>(this.handle, 'dblclick', (event) => {
			if (this.isEnabled()) {
				event.preventDefault();
				this.resizeTo({ ...this.initial }, true);
			}
		});
	}

	private onPointerDown(event: PointerEvent): void {
		if (!this.isEnabled() || event.button !== 0) {
			return;
		}
		event.preventDefault();
		this.stopDragging?.(false);
		const handle = this.handle;
		handle.setPointerCapture?.(event.pointerId);
		handle.focus({ preventScroll: true });
		this.element.classList.add(RESIZABLE_CLASSES.resizing);
		const before = { ...this.size };
		const start = this.measure();
		const x = event.clientX;
		const y = event.clientY;

		const move = (moveEvent: PointerEvent) => {
			this.resizeTo({
				width: this.horizontal() ? start.width + moveEvent.clientX - x : this.size.width,
				height: this.vertical() ? start.height + moveEvent.clientY - y : this.size.height
			}, false);
		};
		const escape = (keyEvent: KeyboardEvent) => {
			if (keyEvent.key === 'Escape') {
				keyEvent.preventDefault();
				keyEvent.stopPropagation();
				end(true);
			}
		};
		const end = (cancel: boolean) => {
			this.stopDragging = null;
			this.element.classList.remove(RESIZABLE_CLASSES.resizing);
			handle.removeEventListener('pointermove', move);
			handle.removeEventListener('pointerup', up);
			handle.removeEventListener('pointercancel', up);
			handle.removeEventListener('lostpointercapture', up);
			handle.removeEventListener('keydown', escape);
			if (cancel) {
				this.size = before;
				this.show();
				this.options.onResize?.({ ...this.size });
			} else if (!same(this.size, before)) {
				this.options.onResized?.({ ...this.size });
			}
		};
		const up = () => end(false);
		handle.addEventListener('pointermove', move);
		handle.addEventListener('pointerup', up);
		handle.addEventListener('pointercancel', up);
		handle.addEventListener('lostpointercapture', up);
		handle.addEventListener('keydown', escape);
		this.stopDragging = end;
	}

	private onKeyDown(event: KeyboardEvent): void {
		if (!this.isEnabled() || this.stopDragging || event.altKey || event.ctrlKey ||
			event.metaKey) {
			return;
		}
		const step = Math.max(pixels(this.options.step) ?? DEFAULTS.step, 1) *
			(event.shiftKey ? DEFAULTS.bigStep : 1);
		const shown = this.measure();
		let width: number | null = this.size.width;
		let height: number | null = this.size.height;
		switch (event.key) {
			case 'ArrowUp':
			case 'ArrowDown':
				if (!this.vertical()) {
					return;
				}
				height = shown.height + (event.key === 'ArrowDown' ? step : -step);
				break;
			case 'ArrowLeft':
			case 'ArrowRight':
				if (!this.horizontal()) {
					return;
				}
				width = shown.width + (event.key === 'ArrowRight' ? step : -step);
				break;
			case 'Home':
			case 'End': {
				const end = event.key === 'End';
				const to = (dimension: 'width' | 'height') => {
					const bounds = this.bounds(dimension);
					return end ? (Number.isFinite(bounds.max) ? bounds.max : shown[dimension]) :
						bounds.min;
				};
				width = this.horizontal() ? to('width') : width;
				height = this.vertical() ? to('height') : height;
				break;
			}
			default:
				return;
		}
		event.preventDefault();
		this.resizeTo({ width, height }, true);
	}

	/**
	 * Resizes the element, within the limits, calling onResize and, when done, onResized if it
	 * changed.
	 *
	 * @param size the size, null for a dimension left as the page makes it
	 * @param done whether the user is done resizing it
	 */
	private resizeTo(size: ResizableSize, done: boolean): void {
		const next = this.within({ width: pixels(size.width), height: pixels(size.height) });
		if (same(next, this.size)) {
			return;
		}
		this.size = next;
		this.show();
		this.options.onResize?.({ ...next });
		if (done) {
			this.options.onResized?.({ ...next });
		}
	}

	/**
	 * @returns the size of the element, the user's or that the page gave it; null for a dimension
	 *          left as the page's style sheets make it
	 */
	getSize(): ResizableSize {
		return { ...this.size };
	}

	/**
	 * Resizes the element, without calling back: the page decides.
	 *
	 * @param size the width and the height, null or left out to leave a dimension as the page's
	 *        style sheets make it; within the limits
	 */
	setSize(size: Partial<ResizableSize> | null): void {
		this.size = this.within({ width: pixels(size?.width), height: pixels(size?.height) });
		this.show();
	}

	/**
	 * Takes other options; those left out stay as they were. A new width or height resizes the
	 * element, and is what a double click takes it back to.
	 */
	configure(options: ResizableOptions = {}): void {
		const defined = Object.fromEntries(Object.entries(options)
			.filter(([, value]) => value !== undefined)) as ResizableOptions;
		this.options = { ...this.options, ...defined };
		if (!this.isEnabled()) {
			this.stopDragging?.(false);
		}
		const size = { ...this.size };
		const initial = { ...this.initial };
		for (const dimension of ['width', 'height'] as const) {
			if (dimension in defined) {
				size[dimension] = initial[dimension] = pixels(defined[dimension]);
			}
		}
		this.size = this.within(size);
		this.initial = this.within(initial);
		this.render();
	}

	/**
	 * Takes the resizable away: the handle, the classes and the size it gave the element.
	 */
	destroy(): void {
		this.stopDragging?.(false);
		this.detach();
		this.handle.remove();
		const element = this.element;
		element.classList.remove(...this.css('resizable').split(' '), ...Object.values(AXIS_CLASSES),
			RESIZABLE_CLASSES.resizing, RESIZABLE_CLASSES.width, RESIZABLE_CLASSES.height,
			RESIZABLE_CLASSES.disabled);
		if (!element.classList.length) {
			element.removeAttribute('class');
		}
		element.style.removeProperty(RESIZABLE_WIDTH_PROPERTY);
		element.style.removeProperty(RESIZABLE_HEIGHT_PROPERTY);
		if (!element.getAttribute('style')) {
			element.removeAttribute('style');
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

function same(one: ResizableSize, other: ResizableSize): boolean {
	return one.width === other.width && one.height === other.height;
}

/**
 * Makes an element resizable, in the shape of a Svelte action: use:resizable={options}.
 *
 * @param element the element to resize
 * @param options the dimensions the user changes, the sizes, the limits and the callbacks
 * @returns update() taking other options, destroy() taking the resizable away
 */
export function resizable(element: HTMLElement, options?: ResizableOptions): {
	update(options?: ResizableOptions): void;
	destroy(): void;
} {
	const instance = new Resizable(element, options);
	return {
		update: (next) => instance.configure(next),
		destroy: () => instance.destroy()
	};
}
