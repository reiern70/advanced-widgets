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

/*
 * Scroll bars that look the same in every browser: the native scroll bars of an element are
 * hidden and slim bars of its own, a rail with a thumb in the colors of the theme, are drawn over
 * its edges, on a strip in the background of the element that hides the content scrolling below
 * them. The element keeps scrolling natively, with the wheel, the keys and touch; the thumb can be
 * dragged and a click on the track pages towards the click. Scripts scroll it with scrollToTop(),
 * scrollToBottom(), scrollTo({top, left}) and scrollToElement(elementOrSelector).
 *
 * Scrollbar.attach(elementOrId, options) puts the element into a wrapper, a div.antilia-scrollbar
 * holding the bars, which do not scroll with the content. Attaching an element that is in a
 * wrapper already, for example one Ajax put in place of the previous one, reuses it; a framework
 * renders the wrapper itself: <div class="antilia-scrollbar"><div>...</div></div>.
 */

import { onRemoving, type Unsubscribe } from './host.js';
import { lookOf, partClasses, type Look } from './look.js';

/**
 * The axes of the scroll bars: by default those along which the CSS of the element lets it
 * scroll.
 */
export interface ScrollbarOptions {
	axis?: 'x' | 'y' | 'both';
	/** The look of the bars; that of the element they are in when left out (see lookOf). */
	look?: Look;
	/**
	 * A CSS selector of an element at the top of the scrolling one that stays in place while the
	 * rest scrolls, such as the sticky head of a table: the vertical bar runs below it.
	 */
	header?: string;
}

/**
 * Where to scroll an element to, in pixels from its start; an axis left out stays where it is.
 */
export interface ScrollPosition {
	top?: number;
	left?: number;
}

/** What a script keeps of the scroll bars it gave an element. */
export interface ScrollbarHandle {
	update(): void;
	destroy(): void;
}

/** The CSS class of the wrapper of an element with scroll bars. */
export const SCROLLBAR_WRAPPER_CLASS = 'antilia-scrollbar';

interface Bar {
	vertical: boolean;
	track: HTMLDivElement;
	rail: HTMLDivElement;
	thumb: HTMLDivElement;
	range?: { length: number; thumb: number; scroll: number };
}

const WRAPPER_CLASS = SCROLLBAR_WRAPPER_CLASS;

const HOST_CLASS = 'antilia-scrollbar-host';

const TRACK_CLASS = 'antilia-scrollbar-track';

const MIN_THUMB = 24;

/** The custom properties laying the bars inside the border of the element, on its background. */
const FRAME = ['background', 'inset-top', 'inset-right', 'inset-bottom', 'inset-left',
	'radius-top-right', 'radius-bottom-right', 'radius-bottom-left']
	.map((name) => '--antilia-scrollbar-' + name);

function scrolls(overflow: string): boolean {
	return overflow === 'auto' || overflow === 'scroll';
}

function transparent(color: string): boolean {
	return !color || color === 'transparent' || /^rgba\(.*,\s*0\)$/.test(color);
}

/**
 * @returns the color the element is painted on: its own background color or else that of the
 *          nearest ancestor having one
 */
function backgroundOf(element: Element): string {
	for (let current: Element | null = element; current; current = current.parentElement) {
		const color = window.getComputedStyle(current).backgroundColor;
		if (!transparent(color)) {
			return color;
		}
	}
	return 'transparent';
}

/**
 * @returns the size of a radius of the inner edge of a border, as CSS
 */
function innerRadius(radius: string, border: string): string {
	const value = parseFloat(radius) - parseFloat(border);
	return (value > 0 ? value : 0) + 'px';
}

/**
 * The scroll bars of an element.
 */
export class Scrollbar {
	/** The live scroll bars. */
	static readonly instances: Scrollbar[] = [];

	readonly element: HTMLElement;

	readonly options: ScrollbarOptions;

	wrapper!: HTMLElement;

	bars: Bar[] = [];

	private destroyed = false;

	private readonly scrollHandler = () => this.update();

	/** The custom properties last given to the wrapper, to set them again only when they change. */
	private frame = '';

	private resizeObserver: ResizeObserver | null = null;

	private mutationObserver: MutationObserver | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

	/** The look of the bars, whose classes the elements they are made of get. */
	private look!: Look;

	/**
	 * @param element the scrolling element
	 * @param options the axes of the bars
	 */
	constructor(element: HTMLElement, options?: ScrollbarOptions) {
		this.element = element;
		this.options = { ...options };
		this.attach();
	}

	/**
	 * Gives an element the scroll bars of this script, replacing those it had already.
	 *
	 * @param element the element or its id
	 * @param options the axes of the bars
	 * @returns the scroll bars, or undefined if there is no such element
	 */
	static attach(element: HTMLElement | string, options?: ScrollbarOptions): Scrollbar | undefined {
		const target = typeof element === 'string' ? document.getElementById(element) : element;
		if (!target) {
			return undefined;
		}
		Scrollbar.detach(target);
		return new Scrollbar(target, options);
	}

	/**
	 * @param element the element or its id
	 * @returns the scroll bars of the element, or undefined if it has none
	 */
	static of(element: Element | string): Scrollbar | undefined {
		const target = typeof element === 'string' ? document.getElementById(element) : element;
		return Scrollbar.instances.find((instance) => instance.element === target);
	}

	/**
	 * Removes the scroll bars of this script from an element, if it has them.
	 *
	 * @param element the element
	 */
	static detach(element: Element): void {
		Scrollbar.instances.slice().forEach((instance) => {
			if (instance.element === element) {
				instance.destroy();
			}
		});
	}

	private attach(): void {
		const element = this.element;
		this.look = this.options.look ?? lookOf(element);
		let wrapper = element.parentElement;
		if (!wrapper || !wrapper.classList.contains(WRAPPER_CLASS)) {
			wrapper = document.createElement('div');
			wrapper.className = partClasses(this.look, 'scrollbar');
			element.parentNode?.insertBefore(wrapper, element);
			wrapper.appendChild(element);
		}
		Array.from(wrapper.children).forEach((child) => {
			if (child.classList.contains(TRACK_CLASS)) {
				child.remove();
			}
		});
		this.wrapper = wrapper;
		element.classList.add(...partClasses(this.look, 'scrollbar-host').split(' '));

		const style = window.getComputedStyle(element);
		const axis = this.options.axis;
		if (axis === 'y' || axis === 'both' || (!axis && scrolls(style.overflowY))) {
			this.bars.push(this.newBar(true));
		}
		if (axis === 'x' || axis === 'both' || (!axis && scrolls(style.overflowX))) {
			this.bars.push(this.newBar(false));
		}
		wrapper.classList.toggle(WRAPPER_CLASS + '--both', this.bars.length === 2);

		element.addEventListener('scroll', this.scrollHandler, { passive: true });
		if (typeof ResizeObserver !== 'undefined') {
			this.resizeObserver = new ResizeObserver(() => this.update());
			this.observeSizes();
		}
		if (typeof MutationObserver !== 'undefined') {
			this.mutationObserver = new MutationObserver(() => {
				this.observeSizes();
				this.update();
			});
			this.mutationObserver.observe(element, { childList: true });
		}
		this.stopWatchingRemoval = onRemoving(element, () => this.destroy());
		Scrollbar.instances.push(this);
		this.update();
	}

	/**
	 * Watches the size of the element and of its children, whose sizes make the size of what it
	 * scrolls.
	 */
	private observeSizes(): void {
		const observer = this.resizeObserver;
		if (!observer) {
			return;
		}
		observer.disconnect();
		observer.observe(this.element);
		Array.from(this.element.children).forEach((child) => observer.observe(child));
	}

	private newBar(vertical: boolean): Bar {
		const track = document.createElement('div');
		track.className = partClasses(this.look, 'scrollbar-track') + ' ' + TRACK_CLASS +
			(vertical ? '--y' : '--x');
		track.setAttribute('aria-hidden', 'true');
		const rail = document.createElement('div');
		rail.className = partClasses(this.look, 'scrollbar-rail');
		track.appendChild(rail);
		const thumb = document.createElement('div');
		thumb.className = partClasses(this.look, 'scrollbar-thumb');
		rail.appendChild(thumb);
		this.wrapper.appendChild(track);
		const bar: Bar = { vertical, track, rail, thumb };
		track.addEventListener('pointerdown', (event) => this.onPointerDown(bar, event));
		return bar;
	}

	/**
	 * Sizes and places the thumbs for the current scroll position, and hides the bar of an axis
	 * along which nothing scrolls.
	 */
	update(): void {
		const element = this.element;
		if (!element.isConnected) {
			this.destroy();
			return;
		}
		this.fitFrame();
		this.bars.forEach((bar) => {
			const client = bar.vertical ? element.clientHeight : element.clientWidth;
			const size = bar.vertical ? element.scrollHeight : element.scrollWidth;
			const position = bar.vertical ? element.scrollTop : element.scrollLeft;
			const overflows = size - client > 1;
			bar.track.hidden = !overflows;
			if (!overflows) {
				return;
			}
			const length = bar.vertical ? bar.rail.clientHeight : bar.rail.clientWidth;
			const thumb = Math.min(length, Math.max(MIN_THUMB, length * client / size));
			const offset = (length - thumb) * position / (size - client);
			bar.range = { length, thumb, scroll: size - client };
			if (bar.vertical) {
				bar.thumb.style.height = thumb + 'px';
				bar.thumb.style.transform = 'translateY(' + offset + 'px)';
			} else {
				bar.thumb.style.width = thumb + 'px';
				bar.thumb.style.transform = 'translateX(' + offset + 'px)';
			}
		});
	}

	/**
	 * Lays the bars inside the border of the element, with its rounded corners, on its background,
	 * so that the content scrolling below them does not show through; the vertical bar starts below
	 * the header, if any.
	 */
	private fitFrame(): void {
		const style = window.getComputedStyle(this.element);
		const header = this.headerHeight();
		const values = [
			backgroundOf(this.element),
			header ? (parseFloat(style.borderTopWidth) || 0) + header + 'px' :
				style.borderTopWidth || '0px',
			style.borderRightWidth || '0px',
			style.borderBottomWidth || '0px',
			style.borderLeftWidth || '0px',
			header ? '0px' : innerRadius(style.borderTopRightRadius, style.borderRightWidth),
			innerRadius(style.borderBottomRightRadius, style.borderRightWidth),
			innerRadius(style.borderBottomLeftRadius, style.borderLeftWidth)
		];
		const frame = values.join(' ');
		if (frame === this.frame) {
			return;
		}
		this.frame = frame;
		FRAME.forEach((name, index) => this.wrapper.style.setProperty(name, values[index]));
	}

	/**
	 * @returns the height of the header staying at the top of the element, 0 if it has none
	 */
	private headerHeight(): number {
		const header = this.options.header ?
			this.element.querySelector<HTMLElement>(this.options.header) : null;
		return header ? header.offsetHeight : 0;
	}

	/**
	 * Scrolls the element to its start.
	 *
	 * @param smooth whether to scroll smoothly rather than at once
	 */
	scrollToTop(smooth = false): void {
		this.scrollTo({ top: 0 }, smooth);
	}

	/**
	 * Scrolls the element to its end.
	 *
	 * @param smooth whether to scroll smoothly rather than at once
	 */
	scrollToBottom(smooth = false): void {
		this.scrollTo({ top: this.element.scrollHeight - this.element.clientHeight }, smooth);
	}

	/**
	 * Scrolls the element to a position.
	 *
	 * @param position the pixels from the start of the element, along either axis or both
	 * @param smooth whether to scroll smoothly rather than at once
	 */
	scrollTo(position: ScrollPosition, smooth = false): void {
		const element = this.element;
		if (smooth && typeof element.scrollTo === 'function') {
			element.scrollTo({ ...position, behavior: 'smooth' });
			return;
		}
		if (position.top !== undefined) {
			element.scrollTop = position.top;
		}
		if (position.left !== undefined) {
			element.scrollLeft = position.left;
		}
		this.update();
	}

	/**
	 * Scrolls the element so that one inside it starts at its top, and horizontally into view.
	 *
	 * @param target the element, or a CSS selector of it, relative to the scrolling element:
	 *            ':scope > p:nth-child(3)'
	 * @param smooth whether to scroll smoothly rather than at once
	 * @returns whether there is such an element to scroll to
	 */
	scrollToElement(target: Element | string, smooth = false): boolean {
		const element = this.element;
		const found = typeof target === 'string' ? element.querySelector(target) : target;
		if (!found || !element.contains(found) || found === element) {
			return false;
		}
		const box = element.getBoundingClientRect();
		const rect = found.getBoundingClientRect();
		const position: ScrollPosition = {
			top: element.scrollTop + rect.top - box.top - element.clientTop
		};
		const left = rect.left - box.left - element.clientLeft;
		if (left < 0 || left + rect.width > element.clientWidth) {
			position.left = element.scrollLeft + left;
		}
		this.scrollTo(position, smooth);
		return true;
	}

	/**
	 * Starts dragging the thumb, or pages towards a click on the track.
	 */
	private onPointerDown(bar: Bar, event: PointerEvent): void {
		if (event.button !== 0 || !bar.range) {
			return;
		}
		event.preventDefault();
		const element = this.element;
		const vertical = bar.vertical;
		if (event.target !== bar.thumb) {
			const rect = bar.thumb.getBoundingClientRect();
			const before = vertical ? event.clientY < rect.top : event.clientX < rect.left;
			const page = (vertical ? element.clientHeight - this.headerHeight() :
				element.clientWidth) * 0.9;
			element.scrollBy({
				top: vertical ? (before ? -page : page) : 0,
				left: vertical ? 0 : (before ? -page : page),
				behavior: 'smooth'
			});
			return;
		}
		const start = vertical ? event.clientY : event.clientX;
		const from = vertical ? element.scrollTop : element.scrollLeft;
		const ratio = bar.range.scroll / Math.max(1, bar.range.length - bar.range.thumb);
		const thumb = bar.thumb;
		const wrapper = this.wrapper;
		thumb.setPointerCapture?.(event.pointerId);
		wrapper.classList.add(WRAPPER_CLASS + '--dragging');
		const move = (moveEvent: PointerEvent) => {
			const delta = ((vertical ? moveEvent.clientY : moveEvent.clientX) - start) * ratio;
			if (vertical) {
				element.scrollTop = from + delta;
			} else {
				element.scrollLeft = from + delta;
			}
		};
		const end = () => {
			thumb.removeEventListener('pointermove', move);
			thumb.removeEventListener('pointerup', end);
			thumb.removeEventListener('pointercancel', end);
			wrapper.classList.remove(WRAPPER_CLASS + '--dragging');
		};
		thumb.addEventListener('pointermove', move);
		thumb.addEventListener('pointerup', end);
		thumb.addEventListener('pointercancel', end);
	}

	/**
	 * Removes the bars, giving the element its native scroll bars back; the element stays in the
	 * wrapper.
	 */
	destroy(): void {
		if (this.destroyed) {
			return;
		}
		this.destroyed = true;
		this.element.removeEventListener('scroll', this.scrollHandler);
		this.resizeObserver?.disconnect();
		this.mutationObserver?.disconnect();
		this.stopWatchingRemoval?.();
		this.bars.forEach((bar) => bar.track.remove());
		this.wrapper.classList.remove(WRAPPER_CLASS + '--both', WRAPPER_CLASS + '--dragging');
		FRAME.forEach((name) => this.wrapper.style.removeProperty(name));
		this.element.classList.remove(HOST_CLASS);
		const index = Scrollbar.instances.indexOf(this);
		if (index >= 0) {
			Scrollbar.instances.splice(index, 1);
		}
	}
}

/**
 * Draws the scroll bars of an element, in the shape of a Svelte action: use:scrollbar={options}.
 *
 * @param element the scrolling element
 * @param options the axes of the bars
 * @returns update() drawing the bars along other axes, destroy() removing them
 */
export function scrollbar(element: HTMLElement, options?: ScrollbarOptions): {
	update(options?: ScrollbarOptions): void;
	destroy(): void;
} {
	let instance = Scrollbar.attach(element, options);
	return {
		update: (next) => {
			instance = Scrollbar.attach(element, next);
		},
		destroy: () => instance?.destroy()
	};
}
