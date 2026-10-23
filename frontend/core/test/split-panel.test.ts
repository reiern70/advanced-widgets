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

import { afterEach, describe, expect, it, vi } from 'vitest';

import { parseSplitSize, setHost, SplitPanel, splitPanel, standaloneHost, type Host }
	from '../src/index.js';

function box(element: Element, rect: Partial<DOMRect>): void {
	const full = { left: 0, top: 0, width: 0, height: 0, right: 0, bottom: 0, x: 0, y: 0, ...rect };
	element.getBoundingClientRect = () => full as DOMRect;
}

function pointer(target: Element, type: string, x: number, y: number, button = 0): void {
	const event = new MouseEvent(type, { bubbles: true, cancelable: true, clientX: x, clientY: y,
		button });
	Object.defineProperty(event, 'pointerId', { value: 1 });
	target.dispatchEvent(event);
}

function key(target: Element, name: string, init: KeyboardEventInit = {}): KeyboardEvent {
	const event = new KeyboardEvent('keydown', { key: name, bubbles: true, cancelable: true,
		...init });
	target.dispatchEvent(event);
	return event;
}

/**
 * @returns the element of a split panel with two panes, and a divider unless left out
 */
function element(divider = true): HTMLElement {
	document.body.innerHTML = '<div id="s"><div>One</div>' +
		(divider ? '<div class="antilia-split-divider"></div>' : '') + '<div>Two</div></div>';
	return document.getElementById('s')!;
}

function shares(split: SplitPanel): [string, string] {
	return [split.element.style.getPropertyValue('--antilia-split-first'),
		split.element.style.getPropertyValue('--antilia-split-second')];
}

/**
 * Gives a split panel side by side 1008 pixels from 100 on, its divider 8 pixels wide, so that the
 * panes share 1000 pixels.
 */
function sized(split: SplitPanel): void {
	box(split.element, { left: 100, top: 50, width: 1008, height: 508 });
	box(split.divider, { left: 100 + split.position * 10, top: 50 + split.position * 5, width: 8,
		height: 8 });
	split.divider.setPointerCapture = () => undefined;
}

afterEach(() => {
	document.body.innerHTML = '';
	setHost(standaloneHost);
});

describe('SplitPanel', () => {
	it('shows the panes side by side, sharing the space equally by default', () => {
		const split = new SplitPanel(element());

		expect(split.element.className).toBe('antilia-split antilia-split-horizontal');
		expect(split.first.className).toBe('antilia-split-pane antilia-split-first');
		expect(split.second.className).toBe('antilia-split-pane antilia-split-second');
		expect(shares(split)).toEqual(['50', '50']);
		expect(SplitPanel.of('s')).toBe(split);
	});

	it('makes the divider a separator standing between the panes', () => {
		const split = new SplitPanel(element(), { position: 30, min: 10, max: 80, label: 'Resize' });
		const divider = split.divider;

		expect(divider.getAttribute('role')).toBe('separator');
		expect(divider.getAttribute('aria-orientation')).toBe('vertical');
		expect(divider.getAttribute('aria-valuenow')).toBe('30');
		expect(divider.getAttribute('aria-valuemin')).toBe('10');
		expect(divider.getAttribute('aria-valuemax')).toBe('80');
		expect(divider.getAttribute('aria-label')).toBe('Resize');
		expect(divider.getAttribute('aria-controls')).toBe('s-first');
		expect(split.first.id).toBe('s-first');
		expect(divider.tabIndex).toBe(0);
		expect(shares(split)).toEqual(['30', '70']);
	});

	it('shows the panes one above the other when vertical', () => {
		const split = new SplitPanel(element(), { orientation: 'vertical' });

		expect(split.element.classList.contains('antilia-split-vertical')).toBe(true);
		expect(split.divider.getAttribute('aria-orientation')).toBe('horizontal');
	});

	it('adds a divider when the markup has none, and takes it away again', () => {
		const target = element(false);
		const split = new SplitPanel(target);

		expect(Array.from(target.children).map((child) => child.className)).toEqual([
			'antilia-split-pane antilia-split-first', 'antilia-split-divider',
			'antilia-split-pane antilia-split-second']);

		split.destroy();
		expect(target.children).toHaveLength(2);
		expect(target.hasAttribute('class')).toBe(false);
		expect(target.hasAttribute('style')).toBe(false);
		expect(SplitPanel.of(target)).toBeNull();
	});

	it('keeps a divider of the markup when destroyed', () => {
		const target = element();
		new SplitPanel(target).destroy();

		expect(target.querySelector('.antilia-split-divider')).not.toBeNull();
	});

	it('needs two panes', () => {
		document.body.innerHTML = '<div id="s"><div>Only</div></div>';

		expect(() => new SplitPanel(document.getElementById('s')!)).toThrow(/two panes/);
	});

	it('keeps the position within its limits', () => {
		expect(new SplitPanel(element(), { position: 95, max: 90 }).position).toBe(90);
		expect(new SplitPanel(element(), { position: -5 }).position).toBe(0);
		expect(new SplitPanel(element(), { position: Number.NaN }).position).toBe(50);
		expect(new SplitPanel(element(), { position: 33.3333 }).position).toBe(33.33);
	});

	it('follows the pointer dragging the divider, where it took it', () => {
		const onResize = vi.fn();
		const onResized = vi.fn();
		const split = new SplitPanel(element(), { position: 50, onResize, onResized });
		sized(split);

		// the divider starts at 600; taken 3 pixels into it
		pointer(split.divider, 'pointerdown', 603, 60);
		expect(split.element.classList.contains('antilia-split-dragging')).toBe(true);

		pointer(split.divider, 'pointermove', 403, 60);
		expect(split.position).toBe(30);
		expect(shares(split)).toEqual(['30', '70']);
		expect(onResize).toHaveBeenLastCalledWith(30);
		expect(onResized).not.toHaveBeenCalled();

		pointer(split.divider, 'pointerup', 403, 60);
		expect(split.element.classList.contains('antilia-split-dragging')).toBe(false);
		expect(onResized).toHaveBeenCalledOnce();
		expect(onResized).toHaveBeenCalledWith(30);

		// no longer dragged
		pointer(split.divider, 'pointermove', 803, 60);
		expect(split.position).toBe(30);
	});

	it('follows the pointer vertically when vertical', () => {
		const split = new SplitPanel(element(), { orientation: 'vertical', max: 75 });
		sized(split);

		// the divider starts at 300 of 50 to 558, the panes share 500 pixels
		pointer(split.divider, 'pointerdown', 10, 300);
		pointer(split.divider, 'pointermove', 10, 150);
		expect(split.position).toBe(20);

		pointer(split.divider, 'pointermove', 10, 2000);
		expect(split.position).toBe(75);
		pointer(split.divider, 'pointercancel', 10, 2000);
		expect(split.element.classList.contains('antilia-split-dragging')).toBe(false);
	});

	it('does not report a drag that did not move the divider', () => {
		const onResized = vi.fn();
		const split = new SplitPanel(element(), { onResized });
		sized(split);

		pointer(split.divider, 'pointerdown', 600, 60);
		pointer(split.divider, 'pointerup', 600, 60);

		expect(onResized).not.toHaveBeenCalled();
	});

	it('is not dragged with another button, or when not resizable', () => {
		const split = new SplitPanel(element(), { resizable: false });
		sized(split);

		pointer(split.divider, 'pointerdown', 600, 60);
		pointer(split.divider, 'pointermove', 300, 60);
		expect(split.position).toBe(50);
		expect(split.element.classList.contains('antilia-split-fixed')).toBe(true);
		expect(split.divider.hasAttribute('tabindex')).toBe(false);
		expect(split.divider.getAttribute('aria-disabled')).toBe('true');

		split.configure({ resizable: true });
		pointer(split.divider, 'pointerdown', 600, 60, 2);
		pointer(split.divider, 'pointermove', 300, 60);
		expect(split.position).toBe(50);
	});

	it('moves with the arrow keys, Home and End', () => {
		const onResized = vi.fn();
		const split = new SplitPanel(element(), { min: 20, max: 90, step: 10, onResized });

		expect(key(split.divider, 'ArrowLeft').defaultPrevented).toBe(true);
		expect(split.position).toBe(40);
		key(split.divider, 'ArrowRight');
		key(split.divider, 'ArrowRight');
		expect(split.position).toBe(60);
		expect(onResized).toHaveBeenLastCalledWith(60);

		key(split.divider, 'Home');
		expect(split.position).toBe(20);
		key(split.divider, 'End');
		expect(split.position).toBe(90);
		expect(split.divider.getAttribute('aria-valuenow')).toBe('90');

		// the other axis, modifiers and other keys leave it
		expect(key(split.divider, 'ArrowUp').defaultPrevented).toBe(false);
		key(split.divider, 'ArrowLeft', { ctrlKey: true });
		key(split.divider, 'a');
		expect(split.position).toBe(90);
		// already at its end: not reported again
		onResized.mockClear();
		key(split.divider, 'End');
		expect(onResized).not.toHaveBeenCalled();
	});

	it('moves with the up and down arrows when vertical', () => {
		const split = new SplitPanel(element(), { orientation: 'vertical' });

		key(split.divider, 'ArrowDown');
		expect(split.position).toBe(55);
		key(split.divider, 'ArrowUp');
		key(split.divider, 'ArrowUp');
		expect(split.position).toBe(45);
	});

	it('goes back to where it started on a double click', () => {
		const onResized = vi.fn();
		const split = new SplitPanel(element(), { position: 25, onResized });
		key(split.divider, 'End');

		split.divider.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));

		expect(split.position).toBe(25);
		expect(onResized).toHaveBeenLastCalledWith(25);
	});

	it('takes other options, keeping those left out', () => {
		const split = new SplitPanel(element(), { position: 40, label: 'Resize' });
		key(split.divider, 'ArrowRight');

		split.configure({ orientation: 'vertical', label: undefined });
		expect(split.element.classList.contains('antilia-split-vertical')).toBe(true);
		expect(split.element.classList.contains('antilia-split-horizontal')).toBe(false);
		expect(split.position).toBe(45);
		expect(split.divider.getAttribute('aria-label')).toBe('Resize');

		split.configure({ max: 30 });
		expect(split.position).toBe(30);

		split.configure({ position: 10 });
		expect(split.position).toBe(10);
		split.divider.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(split.position).toBe(10);
	});

	it('is moved by the page without calling back', () => {
		const onResize = vi.fn();
		const split = new SplitPanel(element(), { onResize, max: 80 });

		split.setPosition(70);
		expect(shares(split)).toEqual(['70', '30']);
		split.setPosition(99);
		expect(split.position).toBe(80);
		expect(onResize).not.toHaveBeenCalled();
	});

	it('takes over from the split panel of the same element', () => {
		const target = element();
		const first = new SplitPanel(target, { position: 20 });
		const second = SplitPanel.attach('s', { position: 60 })!;

		expect(SplitPanel.of(target)).toBe(second);
		key(target.querySelector('.antilia-split-divider')!, 'ArrowRight');
		expect(second.position).toBe(65);
		expect(first.position).toBe(20);
		expect(SplitPanel.attach('missing')).toBeNull();
	});

	it('stops listening when the host removes its element', () => {
		let removing: ((element: Element) => void) | undefined;
		const host: Host = { ...standaloneHost, onNodeRemoving: (listener) => {
			removing = listener;
			return () => undefined;
		} };
		setHost(host);
		const split = new SplitPanel(element());

		removing!(document.body);
		key(split.divider, 'ArrowRight');

		expect(split.position).toBe(50);
	});

	it('works as a Svelte action', () => {
		const target = element();
		const action = splitPanel(target, { position: 30 });
		action.update({ orientation: 'vertical' });

		expect(target.classList.contains('antilia-split-vertical')).toBe(true);
		action.destroy();
		expect(target.classList.contains('antilia-split')).toBe(false);
	});

	it('reads sizes in percent and in pixels', () => {
		expect(parseSplitSize(30)).toEqual({ value: 30, unit: '%' });
		expect(parseSplitSize('30%')).toEqual({ value: 30, unit: '%' });
		expect(parseSplitSize(' 250px ')).toEqual({ value: 250, unit: 'px' });
		expect(parseSplitSize('12.5')).toEqual({ value: 12.5, unit: '%' });
		expect(parseSplitSize('wide')).toBeNull();
		expect(parseSplitSize(Number.POSITIVE_INFINITY)).toBeNull();
		expect(parseSplitSize(undefined)).toBeNull();
	});

	it('keeps the first pane in pixels, reporting where it is in pixels', () => {
		const onResized = vi.fn();
		const onChange = vi.fn();
		const split = new SplitPanel(element(), { position: '250px', min: '100px', max: 50,
			onResized, onChange });
		sized(split);
		split.divider.getBoundingClientRect = () => ({ left: 350, top: 50, width: 8,
			height: 8 }) as DOMRect;

		expect(split.unit).toBe('px');
		expect(split.value()).toBe('250px');
		expect(split.element.classList.contains('antilia-split-pixels')).toBe(true);
		expect(split.element.style.getPropertyValue('--antilia-split-first-size')).toBe('250px');
		expect(shares(split)).toEqual(['', '']);
		// the style sheet keeps both limits when the split panel resizes
		expect(split.element.style.getPropertyValue('--antilia-split-first-min')).toBe('100px');
		expect(split.element.style.getPropertyValue('--antilia-split-first-max')).toBe('50%');

		pointer(split.divider, 'pointerdown', 350, 60);
		pointer(split.divider, 'pointermove', 400, 60);
		expect(split.value()).toBe('300px');
		// half of the 1000 pixels
		pointer(split.divider, 'pointermove', 900, 60);
		expect(split.value()).toBe('500px');
		pointer(split.divider, 'pointermove', 120, 60);
		expect(split.value()).toBe('100px');
		pointer(split.divider, 'pointerup', 120, 60);

		expect(onResized).toHaveBeenCalledWith('100px');
		expect(onChange).toHaveBeenCalledWith('100px', null);

		key(split.divider, 'ArrowRight');
		expect(split.value()).toBe('150px');
		// in percent of the space, as a separator's value is
		expect(split.divider.getAttribute('aria-valuenow')).toBe('15');
		expect(split.divider.getAttribute('aria-valuemin')).toBe('10');
		expect(split.divider.getAttribute('aria-valuemax')).toBe('50');
	});

	it('keeps the second pane from getting smaller than its minimum', () => {
		const split = new SplitPanel(element(), { minSecond: '200px' });
		sized(split);

		key(split.divider, 'End');
		expect(split.position).toBe(80);
		expect(split.element.style.getPropertyValue('--antilia-split-second-min')).toBe('200px');

		split.configure({ minSecond: 30 });
		expect(split.position).toBe(70);
		// within the shares, the script keeps a limit in percent
		expect(split.element.style.getPropertyValue('--antilia-split-second-min')).toBe('0');
		expect(split.element.style.getPropertyValue('--antilia-split-first-max')).toBe('none');
	});

	it('leaves out the limits it cannot work out before it is laid out', () => {
		const split = new SplitPanel(element(), { position: 90, max: '300px' });

		expect(split.position).toBe(90);
		split.setPosition('400px');
		expect(split.value()).toBe('300px');
		expect(split.element.classList.contains('antilia-split-pixels')).toBe(true);
		expect(split.divider.hasAttribute('aria-valuenow')).toBe(false);
		split.setPosition('wide');
		expect(split.value()).toBe('300px');
		split.setPosition(40);
		expect(split.value()).toBe(40);
		expect(split.element.classList.contains('antilia-split-pixels')).toBe(false);
	});

	it('collapses a pane and restores it with the buttons on the divider', () => {
		const onCollapse = vi.fn();
		const onChange = vi.fn();
		const split = new SplitPanel(element(), { position: 30, collapsible: true,
			collapseFirstLabel: 'Hide the files', onCollapse, onChange });
		const [first, second] = Array.from(split.divider.children) as HTMLElement[];

		expect(split.element.classList.contains('antilia-split-collapsible')).toBe(true);
		expect(first.className).toBe('antilia-split-collapse antilia-split-collapse-first');
		expect(first.title).toBe('Hide the files');
		expect(second.title).toBe('Collapse the second pane');
		expect(first.getAttribute('aria-hidden')).toBe('true');
		expect(split.divider.getAttribute('aria-valuemin')).toBe('0');
		expect(split.divider.getAttribute('aria-valuemax')).toBe('100');

		first.click();
		expect(split.collapsed).toBe('first');
		expect(split.element.classList.contains('antilia-split-collapsed-first')).toBe(true);
		expect(first.hidden).toBe(true);
		expect(second.title).toBe('Restore both panes');
		expect(split.divider.getAttribute('aria-valuenow')).toBe('0');
		expect(onCollapse).toHaveBeenLastCalledWith('first');
		expect(onChange).toHaveBeenLastCalledWith(30, 'first');

		// towards the collapsed pane, the other button restores it, where it was
		second.click();
		expect(split.collapsed).toBeNull();
		expect(split.position).toBe(30);
		expect(onCollapse).toHaveBeenLastCalledWith(null);

		second.click();
		expect(split.collapsed).toBe('second');
		expect(split.element.classList.contains('antilia-split-collapsed-second')).toBe(true);
		expect(split.divider.getAttribute('aria-valuenow')).toBe('100');
		first.click();
		expect(split.collapsed).toBeNull();
		expect(onChange).toHaveBeenCalledTimes(4);
	});

	it('does not start dragging or go back from a collapse button', () => {
		const split = new SplitPanel(element(), { position: 30, collapsible: true });
		sized(split);
		const first = split.divider.firstElementChild!;

		pointer(first, 'pointerdown', 400, 60);
		expect(split.element.classList.contains('antilia-split-dragging')).toBe(false);
		key(split.divider, 'End');
		first.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(split.position).toBe(100);
	});

	it('collapses the first pane and restores it with Enter', () => {
		const split = new SplitPanel(element(), { collapsible: true });

		expect(key(split.divider, 'Enter').defaultPrevented).toBe(true);
		expect(split.collapsed).toBe('first');
		key(split.divider, 'Enter');
		expect(split.collapsed).toBeNull();

		split.configure({ collapsible: false });
		expect(split.divider.children).toHaveLength(0);
		expect(key(split.divider, 'Enter').defaultPrevented).toBe(false);
		expect(split.collapsed).toBeNull();
	});

	it('restores a collapsed pane when the divider moves', () => {
		const onCollapse = vi.fn();
		const onResized = vi.fn();
		const split = new SplitPanel(element(), { position: 40, collapsible: true,
			collapsed: 'first', onCollapse, onResized });
		expect(split.element.classList.contains('antilia-split-collapsed-first')).toBe(true);

		key(split.divider, 'ArrowRight');
		expect(split.collapsed).toBeNull();
		expect(split.position).toBe(5);
		expect(onCollapse).toHaveBeenCalledWith(null);

		split.setCollapsed('second');
		key(split.divider, 'ArrowLeft');
		expect(split.position).toBe(95);

		// a double click restores it where the divider started
		split.setCollapsed('first');
		split.divider.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(split.collapsed).toBeNull();
		expect(split.position).toBe(40);

		// dragged out of a collapsed pane
		split.setCollapsed('first');
		sized(split);
		split.divider.getBoundingClientRect = () => ({ left: 100, top: 50, width: 8,
			height: 8 }) as DOMRect;
		pointer(split.divider, 'pointerdown', 100, 60);
		pointer(split.divider, 'pointermove', 300, 60);
		pointer(split.divider, 'pointerup', 300, 60);
		expect(split.collapsed).toBeNull();
		expect(split.position).toBe(20);
		expect(onResized).toHaveBeenLastCalledWith(20);
	});

	it('is collapsed by the page without calling back, only when collapsible', () => {
		const onCollapse = vi.fn();
		const split = new SplitPanel(element(), { collapsed: 'first', onCollapse });

		expect(split.collapsed).toBeNull();
		split.setCollapsed('second');
		expect(split.collapsed).toBeNull();

		split.configure({ collapsible: true, collapsed: 'second' });
		expect(split.collapsed).toBe('second');
		split.setCollapsed(null);
		expect(split.collapsed).toBeNull();
		split.configure({ collapsed: 'first' });
		split.configure({ collapsible: false });
		expect(split.collapsed).toBeNull();
		expect(onCollapse).not.toHaveBeenCalled();
	});

	it('takes away the collapse buttons and the sizes when destroyed', () => {
		const target = element();
		const split = new SplitPanel(target, { position: '200px', min: '50px', collapsible: true,
			collapsed: 'second' });
		split.destroy();

		expect(target.querySelector('.antilia-split-divider')!.children).toHaveLength(0);
		expect(target.hasAttribute('class')).toBe(false);
		expect(target.hasAttribute('style')).toBe(false);
	});

	it('shows no collapse buttons when not resizable', () => {
		const split = new SplitPanel(element(), { collapsible: true, resizable: false });

		expect(split.divider.children).toHaveLength(0);
		expect(split.divider.hasAttribute('aria-valuemin')).toBe(false);
	});
});
