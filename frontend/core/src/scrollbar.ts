/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Scroll bars that look the same in every browser: the native scroll bars of an element are
 * hidden and slim bars of its own, a track with a thumb in the colors of the theme, are drawn over
 * its edges. The element keeps scrolling natively, with the wheel, the keys and touch; the thumb
 * can be dragged and a click on the track pages towards the click.
 *
 * Scrollbar.attach(elementOrId, options) puts the element into a wrapper, a div.wicket-scrollbar
 * holding the bars, which do not scroll with the content. Attaching an element that is in a
 * wrapper already, for example one Ajax put in place of the previous one, reuses it; a framework
 * renders the wrapper itself: <div class="wicket-scrollbar"><div>...</div></div>.
 */

import { onRemoving, type Unsubscribe } from './host.js';

/**
 * The axes of the scroll bars: by default those along which the CSS of the element lets it
 * scroll.
 */
export interface ScrollbarOptions {
	axis?: 'x' | 'y' | 'both';
}

interface Bar {
	vertical: boolean;
	track: HTMLDivElement;
	thumb: HTMLDivElement;
	range?: { length: number; thumb: number; scroll: number };
}

const WRAPPER_CLASS = 'wicket-scrollbar';

const HOST_CLASS = 'wicket-scrollbar-host';

const TRACK_CLASS = 'wicket-scrollbar-track';

const THUMB_CLASS = 'wicket-scrollbar-thumb';

const MIN_THUMB = 24;

function scrolls(overflow: string): boolean {
	return overflow === 'auto' || overflow === 'scroll';
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

	private resizeObserver: ResizeObserver | null = null;

	private mutationObserver: MutationObserver | null = null;

	private stopWatchingRemoval: Unsubscribe | null = null;

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
		let wrapper = element.parentElement;
		if (!wrapper || !wrapper.classList.contains(WRAPPER_CLASS)) {
			wrapper = document.createElement('div');
			wrapper.className = WRAPPER_CLASS;
			element.parentNode?.insertBefore(wrapper, element);
			wrapper.appendChild(element);
		}
		Array.from(wrapper.children).forEach((child) => {
			if (child.classList.contains(TRACK_CLASS)) {
				child.remove();
			}
		});
		this.wrapper = wrapper;
		element.classList.add(HOST_CLASS);

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
		track.className = TRACK_CLASS + ' ' + TRACK_CLASS + (vertical ? '--y' : '--x');
		track.setAttribute('aria-hidden', 'true');
		const thumb = document.createElement('div');
		thumb.className = THUMB_CLASS;
		track.appendChild(thumb);
		this.wrapper.appendChild(track);
		const bar: Bar = { vertical, track, thumb };
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
		this.bars.forEach((bar) => {
			const client = bar.vertical ? element.clientHeight : element.clientWidth;
			const size = bar.vertical ? element.scrollHeight : element.scrollWidth;
			const position = bar.vertical ? element.scrollTop : element.scrollLeft;
			const overflows = size - client > 1;
			bar.track.hidden = !overflows;
			if (!overflows) {
				return;
			}
			const length = bar.vertical ? bar.track.clientHeight : bar.track.clientWidth;
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
			const page = (vertical ? element.clientHeight : element.clientWidth) * 0.9;
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
