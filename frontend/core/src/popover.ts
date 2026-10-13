/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Popovers: a bubble with an arrow pointing at the element that opened it, its trigger, showing
 * more about it: a title and a body of text, of markup or of a node of the page. It opens next to
 * the trigger, on the side the page asks for (placement) or the one with the most room (auto).
 * Without room there it opens where it fits in the window: on the opposite side, else on the side
 * across with the most room, else on the side with the most room, pushed into the window. It
 * slides along the trigger to stay in the window, its arrow still pointing at the trigger, and
 * follows the trigger as the page scrolls or resizes.
 *
 * A popover closes with its close button, Escape, a click outside it, or a click on its trigger
 * again, which never reaches the trigger's own listeners then: a trigger opening a popover on
 * click toggles it. It closes too when its trigger leaves the page. One popover is open at a time
 * in a page, opening another closes it. The popover is a non-modal dialog labelled by its title;
 * it takes the focus when it opens and gives it back to its trigger when it closes from the
 * keyboard, while the trigger says aria-expanded and aria-controls.
 *
 * The bubble is appended to the body of the page, out of the boxes that would clip it, and takes
 * the theme its trigger is in: the custom properties of the theme (--antilia-theme-*) as the
 * trigger has them, set on the bubble through the CSSOM like where it is and its arrow, so the
 * popover works under a strict Content Security Policy. It is drawn by popover.css, in one of
 * POPOVER_DESIGNS, and takes the look of its trigger too (see lookOf), whose classes its parts
 * get and which it carries as Wicket's ThemeBehavior writes it, data-antilia-look and the class
 * antilia-look-<name> of each look it is made of, so the style sheet of the look reaches it.
 *
 * Besides a text or markup, a popover may show an element of the page, its content: one the page
 * rendered hidden (antilia-popover-content), a Wicket panel or what React or Svelte render, which
 * moves into the bubble while it is open, keeping its listeners and its state, and back to its
 * place when it closes. The page may render it again meanwhile, as Wicket does via Ajax: the
 * popover takes the element of that id back then, and places itself again as its size changes.
 */

import { LOOK_ATTRIBUTE, lookOf, partClasses, type Look, type LookPart } from './look.js';

/** A side of its trigger a popover opens on. */
export type PopoverSide = 'top' | 'bottom' | 'left' | 'right';

/**
 * Where a popover opens: on a side of its trigger, centered on it (top-center), or along it from
 * one of its edges: top-left starts at the left edge of the trigger and grows to the right,
 * left-top starts at its top edge and grows down; auto for the side with the most room.
 */
export type PopoverPlacement = 'top-left' | 'top-center' | 'top-right' | 'right-top' |
	'right-center' | 'right-bottom' | 'bottom-right' | 'bottom-center' | 'bottom-left' |
	'left-bottom' | 'left-center' | 'left-top' | 'auto';

/** The design of a popover; card when left out. */
export type PopoverDesign = 'card' | 'solid' | 'outline' | 'glass' | 'inverse' | 'callout' |
	'aurora' | 'comic' | 'neon';

/** What a popover stands for, which sets the color of its header. */
export type PopoverVariant = 'default' | 'info' | 'success' | 'warning' | 'error';

/**
 * What a popover shows, where, and the callback.
 */
export interface PopoverOptions {
	/** The title, in the header of the popover; none when left out. */
	title?: string;
	/** The body: a text, markup with html true, or a node, which moves into it. */
	body?: string | Node;
	/**
	 * An element of the page, or its id, shown after the body while the popover is open and back
	 * in its place, as it was, once it closed: rich content the page renders, hidden by its class
	 * antilia-popover-content until then. Its element with [autofocus] takes the focus.
	 */
	content?: HTMLElement | string;
	/** Whether the body is markup, false when left out; the page answers for what it holds. */
	html?: boolean;
	/** Where the popover opens, bottom-center when left out. */
	placement?: PopoverPlacement;
	/** What the popover stands for, default when left out. */
	variant?: PopoverVariant;
	/** The design of the popover, card when left out. */
	design?: PopoverDesign;
	/**
	 * The look of the popover, whose classes its parts get; that of its trigger when left out
	 * (see lookOf).
	 */
	look?: Look;
	/** Whether the popover has a close button, true when left out. */
	closable?: boolean;
	/** The title of the close button, Close when left out. */
	closeLabel?: string;
	/** The widest the popover gets, a CSS length, 20rem when left out. */
	maxWidth?: string;
	/** CSS classes of the popover, next to antilia-popover. */
	className?: string;
	/** Called once the popover closed. */
	onClose?: () => void;
}

/** The classes of the parts of a popover. */
export const POPOVER_CLASSES = {
	popover: 'antilia-popover',
	arrow: 'antilia-popover-arrow',
	header: 'antilia-popover-header',
	title: 'antilia-popover-title',
	close: 'antilia-popover-close',
	body: 'antilia-popover-body',
	/** On an element of the page shown by a popover, hidden out of it. */
	content: 'antilia-popover-content',
	/** On a trigger while its popover is open. */
	open: 'antilia-popover-open'
} as const;

export const POPOVER_VARIANTS: readonly PopoverVariant[] = ['default', 'info', 'success',
	'warning', 'error'];

export const POPOVER_DESIGNS: readonly PopoverDesign[] = ['card', 'solid', 'outline', 'glass',
	'inverse', 'callout', 'aurora', 'comic', 'neon'];

export const POPOVER_PLACEMENTS: readonly PopoverPlacement[] = ['top-left', 'top-center',
	'top-right', 'right-top', 'right-center', 'right-bottom', 'bottom-right', 'bottom-center',
	'bottom-left', 'left-bottom', 'left-center', 'left-top', 'auto'];

/** The custom properties holding where a popover is and where its arrow points, in pixels. */
export const POPOVER_X_PROPERTY = '--antilia-popover-x';
export const POPOVER_Y_PROPERTY = '--antilia-popover-y';
export const POPOVER_ARROW_PROPERTY = '--antilia-popover-arrow';
const MAX_WIDTH_PROPERTY = '--antilia-popover-max-width';

/** The custom properties of the themes, which a popover takes from its trigger. */
export const THEME_PROPERTIES = ['primary', 'primary-dark', 'on-primary', 'accent', 'text',
	'surface', 'surface-alt', 'hover', 'selected', 'highlight', 'toolbar', 'border', 'progress',
	'progress-dark', 'track', 'veil', 'color-scheme', 'success', 'error', 'warning', 'info']
	.map((name) => '--antilia-theme-' + name);

/** How far the popover stays from its trigger, beyond its arrow, and from the edges, in pixels. */
const GAP = 10;
const MARGIN = 8;
/** How close to a corner of the popover its arrow may come, in pixels. */
const ARROW_INSET = 14;
/**
 * How far from its corner the arrow of a popover along an edge of a small trigger is, in pixels:
 * the popover starts at that edge, or before it so that the arrow points at the middle.
 */
const ARROW_EDGE = 20;

const OPPOSITE: Record<PopoverSide, PopoverSide> = {
	top: 'bottom', bottom: 'top', left: 'right', right: 'left'
};

/** Where along its side a popover is: from the start edge of the trigger, centered, or the end. */
type Alignment = 'start' | 'center' | 'end';

const ALIGNMENTS: Record<string, Alignment> = { left: 'start', top: 'start', right: 'end',
	bottom: 'end' };

/**
 * @returns the side and the alignment of a placement other than auto, bottom-center for one
 *          unknown
 */
function parse(placement: string): [PopoverSide, Alignment] {
	const [side, edge] = placement.split('-');
	if (!(side in OPPOSITE)) {
		return ['bottom', 'center'];
	}
	const vertical = side === 'top' || side === 'bottom';
	const along = edge && (vertical ? edge === 'left' || edge === 'right' :
		edge === 'top' || edge === 'bottom') ? ALIGNMENTS[edge] : 'center';
	return [side as PopoverSide, along];
}

const CLOSE_ICON = 'M6 6l12 12 M18 6L6 18';

const SVG = 'http://www.w3.org/2000/svg';

let counter = 0;

/** The popover open in the page, if any. */
let current: Popover | null = null;

function resolve(target: HTMLElement | string): HTMLElement | null {
	return typeof target === 'string' ? document.getElementById(target) : target;
}

function closeIcon(): SVGSVGElement {
	const svg = document.createElementNS(SVG, 'svg');
	svg.setAttribute('viewBox', '0 0 24 24');
	svg.setAttribute('width', '12');
	svg.setAttribute('height', '12');
	svg.setAttribute('aria-hidden', 'true');
	svg.setAttribute('focusable', 'false');
	const shape = document.createElementNS(SVG, 'path');
	shape.setAttribute('d', CLOSE_ICON);
	shape.setAttribute('fill', 'none');
	shape.setAttribute('stroke', 'currentColor');
	shape.setAttribute('stroke-width', '2.5');
	shape.setAttribute('stroke-linecap', 'round');
	svg.append(shape);
	return svg;
}

/**
 * @returns the classes naming the looks a look is made of, antilia-look-<name>, as Wicket's
 *          ThemeBehavior gives them: "bootstrap color:antilia" is made of Bootstrap's and Antilia's
 */
function lookClassNames(look: Look): string[] {
	const names = look.name.split(/\s+/).filter(Boolean)
		.map((word) => word.slice(word.indexOf(':') + 1));
	return [...new Set(names)].map((name) => 'antilia-look-' + name);
}

/**
 * @returns the classes of the bubble of a popover: those of its parts in its look, of its variant
 *          and design, and those naming its look, as a popover gets them; for a popover drawn in the
 *          page, as a gallery shows them
 */
export function popoverClasses(look: Look, design: PopoverDesign = 'card',
	variant: PopoverVariant = 'default'): string {
	return [partClasses(look, 'popover', `popover-${variant}`, `popover-${design}`),
		...lookClassNames(look)].join(' ');
}

export class Popover {
	/** The element the popover points at. */
	readonly trigger: HTMLElement;

	/** The bubble. */
	readonly element: HTMLElement;

	readonly options: PopoverOptions;

	/** The look of the popover, whose classes its parts get. */
	readonly look: Look;

	/** The side of the trigger the popover is on now. */
	side: PopoverSide = 'bottom';

	private arrow: HTMLElement;

	/** The element of the page the popover shows, and where it goes back to. */
	private content: HTMLElement | null = null;

	private placeholder: Comment | null = null;

	/** Places the popover again as its size changes. */
	private resizer: ResizeObserver | null = null;

	private listeners: Array<[EventTarget, string, EventListener, boolean]> = [];

	/** Watches the page for the trigger leaving it. */
	private observer: MutationObserver | null = null;

	private frame = 0;

	private closed = false;

	/**
	 * Opens a popover pointing at its trigger, closing the one open; one open at the trigger
	 * already closes instead, as a click on the trigger does.
	 *
	 * @param trigger the element the popover points at, or its id
	 * @param options what the popover shows and where
	 * @returns the popover open, null when it closed or there is no such trigger
	 */
	static toggle(trigger: HTMLElement | string, options: PopoverOptions = {}): Popover | null {
		const element = resolve(trigger);
		if (!element) {
			return null;
		}
		if (current && current.trigger === element) {
			current.close();
			return null;
		}
		return Popover.show(element, options);
	}

	/**
	 * Opens a popover pointing at its trigger, closing the one open.
	 *
	 * @param trigger the element the popover points at, or its id
	 * @param options what the popover shows and where
	 * @returns the popover, null when there is no such trigger
	 */
	static show(trigger: HTMLElement | string, options: PopoverOptions = {}): Popover | null {
		const element = resolve(trigger);
		return element ? new Popover(element, options) : null;
	}

	/**
	 * @returns the popover open in the page, if any
	 */
	static current(): Popover | null {
		return current;
	}

	/**
	 * Closes the popover open in the page, if any.
	 */
	static closeAll(): void {
		current?.close();
	}

	/**
	 * @param trigger the element the popover points at
	 * @param options what the popover shows and where
	 */
	constructor(trigger: HTMLElement, options: PopoverOptions = {}) {
		current?.close();
		this.trigger = trigger;
		this.options = { ...options };
		const id = 'antilia-popover-' + (++counter);
		const element = document.createElement('div');
		element.id = id;
		const variant = options.variant && POPOVER_VARIANTS.includes(options.variant) ?
			options.variant : 'default';
		const design = options.design && POPOVER_DESIGNS.includes(options.design) ?
			options.design : 'card';
		const look = options.look ?? lookOf(trigger);
		this.look = look;
		element.className = [popoverClasses(look, design, variant), options.className]
			.filter(Boolean).join(' ');
		element.setAttribute(LOOK_ATTRIBUTE, look.name);
		element.setAttribute('role', 'dialog');
		element.tabIndex = -1;
		this.element = element;
		this.arrow = document.createElement('div');
		this.arrow.className = this.css('popover-arrow');
		element.append(this.arrow);
		if (options.title || options.closable !== false) {
			element.append(this.header(id));
		}
		const body = document.createElement('div');
		body.className = this.css('popover-body');
		if (options.body instanceof Node) {
			body.append(options.body);
		} else if (options.html) {
			body.innerHTML = options.body ?? '';
		} else {
			body.textContent = options.body ?? '';
		}
		this.content = options.content ? resolve(options.content) : null;
		if (this.content) {
			this.placeholder = document.createComment(POPOVER_CLASSES.content);
			this.content.replaceWith(this.placeholder);
			body.append(this.content);
		}
		element.append(body);
		this.takeTheme();
		if (options.maxWidth) {
			element.style.setProperty(MAX_WIDTH_PROPERTY, options.maxWidth);
		}
		document.body.append(element);

		trigger.setAttribute('aria-expanded', 'true');
		trigger.setAttribute('aria-controls', id);
		trigger.classList.add(POPOVER_CLASSES.open);
		current = this;
		this.place();
		this.bind();
		const focus = this.content?.querySelector<HTMLElement>('[autofocus]') ?? element;
		focus.focus({ preventScroll: true });
	}

	/**
	 * @returns the classes of parts of the popover in its look
	 */
	private css(...parts: LookPart[]): string {
		return partClasses(this.look, ...parts);
	}

	private header(id: string): HTMLElement {
		const header = document.createElement('div');
		header.className = this.css('popover-header');
		if (this.options.title) {
			const title = document.createElement('div');
			title.className = this.css('popover-title');
			title.id = id + '-title';
			title.textContent = this.options.title;
			header.append(title);
			this.element.setAttribute('aria-labelledby', title.id);
		}
		if (this.options.closable !== false) {
			const close = document.createElement('button');
			close.type = 'button';
			close.className = this.css('popover-close');
			const label = this.options.closeLabel ?? 'Close';
			close.title = label;
			close.setAttribute('aria-label', label);
			close.append(closeIcon());
			close.addEventListener('click', () => this.close(true));
			header.append(close);
		}
		return header;
	}

	/**
	 * Gives the bubble the theme of its trigger, which it is out of in the body of the page.
	 */
	private takeTheme(): void {
		const style = getComputedStyle(this.trigger);
		THEME_PROPERTIES.forEach((property) => {
			const value = style.getPropertyValue(property).trim();
			if (value) {
				this.element.style.setProperty(property, value);
			}
		});
	}

	/**
	 * Places the popover next to its trigger, on the side asked or one where it fits, within the
	 * window, its arrow pointing at the trigger.
	 */
	place(): void {
		const anchor = this.trigger.getBoundingClientRect();
		const element = this.element;
		const width = element.offsetWidth;
		const height = element.offsetHeight;
		const viewport = { width: document.documentElement.clientWidth || window.innerWidth,
			height: document.documentElement.clientHeight || window.innerHeight };
		const room: Record<PopoverSide, number> = {
			top: anchor.top - MARGIN,
			bottom: viewport.height - anchor.bottom - MARGIN,
			left: anchor.left - MARGIN,
			right: viewport.width - anchor.right - MARGIN
		};
		const vertical = (side: PopoverSide) => side === 'top' || side === 'bottom';
		const needs = (side: PopoverSide) => (vertical(side) ? height : width) + GAP;
		// on a side, the popover fits with room beside the trigger and room along it
		const fits = (side: PopoverSide) => room[side] >= needs(side) && (vertical(side) ?
			width <= viewport.width - 2 * MARGIN : height <= viewport.height - 2 * MARGIN);
		const byRoom = (sides: PopoverSide[]) => [...sides].sort((a, b) => room[b] - room[a]);
		const asked = this.options.placement ?? 'bottom-center';
		let candidates: PopoverSide[];
		let along: Alignment;
		if (asked === 'auto') {
			candidates = byRoom(['bottom', 'top', 'right', 'left']);
			along = 'center';
		} else {
			let first: PopoverSide;
			[first, along] = parse(asked);
			// the side asked, the opposite one, then those across
			candidates = [first, OPPOSITE[first],
				...byRoom(vertical(first) ? ['left', 'right'] : ['top', 'bottom'])];
		}
		// where it fits, else where it lacks the least room
		const side = candidates.find(fits) ?? candidates.reduce((best, each) =>
			room[each] - needs(each) > room[best] - needs(best) ? each : best);
		const clamp = (value: number, min: number, max: number) =>
			Math.min(Math.max(value, min), Math.max(min, max));
		// where the popover starts along its side, from where the trigger does, and its length
		const upright = vertical(side);
		const start = upright ? anchor.left : anchor.top;
		const length = upright ? anchor.width : anchor.height;
		const size = upright ? width : height;
		const limit = (upright ? viewport.width : viewport.height) - size - MARGIN;
		const middle = start + length / 2;
		const offset = along === 'start' ? Math.min(start, middle - ARROW_EDGE) :
			along === 'end' ? Math.max(start + length - size, middle + ARROW_EDGE - size) :
				middle - size / 2;
		const position = clamp(offset, MARGIN, limit);
		const arrow = clamp(middle - position, ARROW_INSET, size - ARROW_INSET);
		const beside = side === 'bottom' ? anchor.bottom + GAP : side === 'top' ?
			anchor.top - GAP - height : side === 'right' ? anchor.right + GAP :
				anchor.left - GAP - width;
		// without room anywhere, in the window all the same, over the trigger, without an arrow
		const across = clamp(beside, MARGIN, (upright ? viewport.height - height :
			viewport.width - width) - MARGIN);
		const [x, y] = upright ? [position, across] : [across, position];
		this.side = side;
		element.setAttribute('data-side', side);
		element.setAttribute('data-align', along);
		element.toggleAttribute('data-cramped', Math.round(across) !== Math.round(beside));
		element.style.setProperty(POPOVER_X_PROPERTY, Math.round(x) + 'px');
		element.style.setProperty(POPOVER_Y_PROPERTY, Math.round(y) + 'px');
		element.style.setProperty(POPOVER_ARROW_PROPERTY, Math.round(arrow) + 'px');
	}

	private listen<E extends Event>(target: EventTarget, type: string,
		listener: (event: E) => void, capture = false): void {
		target.addEventListener(type, listener as EventListener, capture);
		this.listeners.push([target, type, listener as EventListener, capture]);
	}

	private bind(): void {
		this.listen<PointerEvent>(document, 'pointerdown', (event) => {
			const target = event.target as Node;
			if (!this.element.contains(target) && !this.trigger.contains(target)) {
				this.close();
			}
		}, true);
		// a click on the trigger closes its popover, and stops there: it does not open it again
		this.listen<MouseEvent>(document, 'click', (event) => {
			if (this.trigger.contains(event.target as Node)) {
				event.preventDefault();
				event.stopPropagation();
				this.close(true);
			}
		}, true);
		this.listen<KeyboardEvent>(document, 'keydown', (event) => {
			if (event.key === 'Escape' && (this.element.contains(event.target as Node) ||
				this.trigger.contains(event.target as Node))) {
				event.preventDefault();
				event.stopPropagation();
				this.close(true);
			}
		}, true);
		const follow = () => {
			cancelAnimationFrame(this.frame);
			this.frame = requestAnimationFrame(() => this.place());
		};
		this.listen(window, 'scroll', follow, true);
		this.listen(window, 'resize', follow);
		// content loaded or rendered again, the popover growing or shrinking
		if (typeof ResizeObserver !== 'undefined') {
			this.resizer = new ResizeObserver(follow);
			this.resizer.observe(this.element);
		}
		// in any framework, the page taking the trigger away, or rendering it again
		this.observer = new MutationObserver(() => {
			if (!this.trigger.isConnected) {
				this.close();
			}
		});
		this.observer.observe(document.body, { childList: true, subtree: true });
	}

	/**
	 * @returns whether the popover is open
	 */
	isOpen(): boolean {
		return !this.closed;
	}

	/**
	 * Closes the popover.
	 *
	 * @param refocus whether the trigger gets the focus back, as when the user closed it from the
	 *        popover or the trigger
	 */
	close(refocus = false): void {
		if (this.closed) {
			return;
		}
		this.closed = true;
		cancelAnimationFrame(this.frame);
		this.listeners.forEach(([target, type, listener, capture]) =>
			target.removeEventListener(type, listener, capture));
		this.listeners = [];
		this.observer?.disconnect();
		this.observer = null;
		this.resizer?.disconnect();
		this.resizer = null;
		const hadFocus = this.element.contains(document.activeElement);
		this.giveBack();
		this.element.remove();
		this.trigger.setAttribute('aria-expanded', 'false');
		this.trigger.removeAttribute('aria-controls');
		this.trigger.classList.remove(POPOVER_CLASSES.open);
		if (current === this) {
			current = null;
		}
		if ((refocus || hadFocus) && this.trigger.isConnected) {
			this.trigger.focus({ preventScroll: true });
		}
		this.options.onClose?.();
	}

	/**
	 * Puts the content back in its place in the page: the element of its id as the page has it
	 * now, which may have rendered it again in the popover.
	 */
	private giveBack(): void {
		const placeholder = this.placeholder;
		if (!placeholder || !this.content) {
			return;
		}
		const id = typeof this.options.content === 'string' ? this.options.content : this.content.id;
		const now = (id && document.getElementById(id)) || this.content;
		placeholder.replaceWith(now);
		this.placeholder = null;
	}
}
