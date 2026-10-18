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

import { afterEach, describe, expect, it, vi } from 'vitest';

import { Resizable, resizable, RESIZABLE_HEIGHT_PROPERTY, RESIZABLE_WIDTH_PROPERTY, setHost,
	standaloneHost, type Host } from '../src/index.js';

function box(element: Element, width: number, height: number): void {
	element.getBoundingClientRect = () => ({ left: 0, top: 0, width, height, right: width,
		bottom: height, x: 0, y: 0, toJSON: () => ({}) }) as DOMRect;
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
 * @returns an element 400 pixels wide and 200 high, as the page lays it out
 */
function element(): HTMLElement {
	document.body.innerHTML = '<div id="r"><p>Text</p></div>';
	const div = document.getElementById('r')!;
	box(div, 400, 200);
	return div;
}

function sizes(resizable: Resizable): [string, string] {
	return [resizable.element.style.getPropertyValue(RESIZABLE_WIDTH_PROPERTY),
		resizable.element.style.getPropertyValue(RESIZABLE_HEIGHT_PROPERTY)];
}

afterEach(() => {
	document.body.innerHTML = '';
	setHost(standaloneHost);
});

describe('Resizable', () => {
	it('adds a handle to the corner, leaving the size to the page until the user resizes it', () => {
		const resizable = new Resizable(element(), { label: 'Resize the box' });
		const handle = resizable.handle;

		expect(resizable.element.className).toBe('antilia-resizable antilia-resizable-both');
		expect(resizable.element.lastElementChild).toBe(handle);
		expect(handle.className).toBe('antilia-resizable-handle');
		expect(handle.getAttribute('role')).toBe('separator');
		expect(handle.getAttribute('aria-label')).toBe('Resize the box');
		expect(handle.getAttribute('aria-controls')).toBe('r');
		expect(handle.hasAttribute('aria-orientation')).toBe(false);
		expect(handle.getAttribute('aria-valuetext')).toBe('400 × 200 px');
		expect(handle.tabIndex).toBe(0);
		expect(sizes(resizable)).toEqual(['', '']);
		expect(resizable.element.hasAttribute('style')).toBe(false);
		expect(resizable.getSize()).toEqual({ width: null, height: null });
		expect(Resizable.of('r')).toBe(resizable);
	});

	it('resizes the height alone by the bottom edge, a horizontal separator', () => {
		const resizable = new Resizable(element(), { axis: 'vertical', height: 150, width: 300,
			minHeight: 100, maxHeight: 500 });
		const handle = resizable.handle;

		expect(resizable.element.classList.contains('antilia-resizable-vertical')).toBe(true);
		expect(resizable.element.classList.contains('antilia-resizable-sized-height')).toBe(true);
		expect(resizable.element.classList.contains('antilia-resizable-sized-width')).toBe(false);
		expect(sizes(resizable)).toEqual(['', '150px']);
		expect(resizable.getSize()).toEqual({ width: null, height: 150 });
		expect(handle.getAttribute('aria-orientation')).toBe('horizontal');
		expect(handle.getAttribute('aria-valuenow')).toBe('150');
		expect(handle.getAttribute('aria-valuemin')).toBe('100');
		expect(handle.getAttribute('aria-valuemax')).toBe('500');
		expect(handle.getAttribute('aria-valuetext')).toBe('150 px');
	});

	it('resizes the width alone by the right edge, a vertical separator', () => {
		const resizable = new Resizable(element(), { axis: 'horizontal' });

		expect(resizable.handle.getAttribute('aria-orientation')).toBe('vertical');
		expect(resizable.handle.getAttribute('aria-valuenow')).toBe('400');
		expect(resizable.handle.hasAttribute('aria-valuemax')).toBe(false);
		key(resizable.handle, 'ArrowDown');
		expect(resizable.getSize()).toEqual({ width: null, height: null });
		key(resizable.handle, 'ArrowRight');
		expect(resizable.getSize()).toEqual({ width: 410, height: null });
	});

	it('follows the pointer dragging the handle, telling where it was dropped', () => {
		const onResize = vi.fn();
		const onResized = vi.fn();
		const resizable = new Resizable(element(), { onResize, onResized });
		resizable.handle.setPointerCapture = () => undefined;

		pointer(resizable.handle, 'pointerdown', 400, 200);
		expect(resizable.element.classList.contains('antilia-resizable-resizing')).toBe(true);
		pointer(resizable.handle, 'pointermove', 450, 180);
		expect(sizes(resizable)).toEqual(['450px', '180px']);
		expect(onResize).toHaveBeenLastCalledWith({ width: 450, height: 180 });
		expect(onResized).not.toHaveBeenCalled();
		pointer(resizable.handle, 'pointermove', 500.4, 260.6);
		pointer(resizable.handle, 'pointerup', 500, 261);

		expect(resizable.element.classList.contains('antilia-resizable-resizing')).toBe(false);
		expect(onResized).toHaveBeenCalledTimes(1);
		expect(onResized).toHaveBeenCalledWith({ width: 500, height: 261 });
		expect(resizable.getSize()).toEqual({ width: 500, height: 261 });
	});

	it('keeps the size within the limits', () => {
		const resizable = new Resizable(element(), { minWidth: 350, maxWidth: 420, minHeight: 150 });
		resizable.handle.setPointerCapture = () => undefined;

		pointer(resizable.handle, 'pointerdown', 400, 200);
		pointer(resizable.handle, 'pointermove', 100, 0);
		expect(resizable.getSize()).toEqual({ width: 350, height: 150 });
		pointer(resizable.handle, 'pointermove', 900, 900);
		expect(resizable.getSize()).toEqual({ width: 420, height: 900 });
		pointer(resizable.handle, 'pointerup', 900, 900);
	});

	it('puts the size back where the drag began on Escape', () => {
		const onResized = vi.fn();
		const onResize = vi.fn();
		const resizable = new Resizable(element(), { height: 120, onResize, onResized });
		resizable.handle.setPointerCapture = () => undefined;

		pointer(resizable.handle, 'pointerdown', 400, 120);
		pointer(resizable.handle, 'pointermove', 420, 160);
		key(resizable.handle, 'Escape');

		expect(resizable.getSize()).toEqual({ width: null, height: 120 });
		expect(onResize).toHaveBeenLastCalledWith({ width: null, height: 120 });
		expect(onResized).not.toHaveBeenCalled();
		pointer(resizable.handle, 'pointermove', 480, 200);
		expect(resizable.getSize()).toEqual({ width: null, height: 120 });
	});

	it('resizes with the arrow keys, more with Shift, and to its limits with Home and End', () => {
		const onResized = vi.fn();
		const resizable = new Resizable(element(), { minWidth: 100, minHeight: 50, maxWidth: 600,
			step: 20, onResized });
		const handle = resizable.handle;

		expect(key(handle, 'ArrowDown').defaultPrevented).toBe(true);
		expect(resizable.getSize()).toEqual({ width: null, height: 220 });
		key(handle, 'ArrowLeft', { shiftKey: true });
		expect(resizable.getSize()).toEqual({ width: 300, height: 220 });
		key(handle, 'Home');
		expect(resizable.getSize()).toEqual({ width: 100, height: 50 });
		key(handle, 'End');
		// no largest height: the height stays
		expect(resizable.getSize()).toEqual({ width: 600, height: 50 });
		expect(onResized).toHaveBeenCalledTimes(4);
		expect(key(handle, 'ArrowUp', { ctrlKey: true }).defaultPrevented).toBe(false);
		expect(key(handle, 'a').defaultPrevented).toBe(false);
	});

	it('goes back to the size it started with on a double click', () => {
		const onResized = vi.fn();
		const resizable = new Resizable(element(), { axis: 'vertical', onResized });

		key(resizable.handle, 'ArrowDown');
		expect(resizable.getSize().height).toBe(210);
		resizable.handle.dispatchEvent(new MouseEvent('dblclick', { bubbles: true,
			cancelable: true }));

		expect(resizable.getSize()).toEqual({ width: null, height: null });
		expect(sizes(resizable)).toEqual(['', '']);
		expect(onResized).toHaveBeenLastCalledWith({ width: null, height: null });
	});

	it('takes sizes from the page without calling back, within the limits', () => {
		const onResized = vi.fn();
		const resizable = new Resizable(element(), { axis: 'vertical', maxHeight: 300, onResized });

		resizable.setSize({ width: 250, height: 450 });
		expect(resizable.getSize()).toEqual({ width: null, height: 300 });
		resizable.setSize(null);
		expect(resizable.getSize()).toEqual({ width: null, height: null });
		expect(onResized).not.toHaveBeenCalled();
	});

	it('takes other options, a size given becoming the one a double click goes back to', () => {
		const resizable = new Resizable(element(), { axis: 'vertical', height: 100 });

		resizable.configure({ axis: 'both', width: 320 });
		expect(resizable.element.className.split(' ').sort()).toEqual(['antilia-resizable',
			'antilia-resizable-both', 'antilia-resizable-sized-height',
			'antilia-resizable-sized-width']);
		expect(resizable.getSize()).toEqual({ width: 320, height: 100 });
		key(resizable.handle, 'ArrowRight');
		resizable.handle.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(resizable.getSize()).toEqual({ width: 320, height: 100 });

		resizable.configure({ axis: 'horizontal' });
		expect(resizable.getSize()).toEqual({ width: 320, height: null });
	});

	it('lets the user resize no more while not enabled', () => {
		const onResized = vi.fn();
		const resizable = new Resizable(element(), { enabled: false, onResized });

		expect(resizable.handle.hidden).toBe(true);
		expect(resizable.handle.getAttribute('aria-disabled')).toBe('true');
		expect(resizable.handle.hasAttribute('tabindex')).toBe(false);
		expect(resizable.element.classList.contains('antilia-resizable-disabled')).toBe(true);
		key(resizable.handle, 'ArrowDown');
		pointer(resizable.handle, 'pointerdown', 400, 200);
		resizable.handle.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(onResized).not.toHaveBeenCalled();

		resizable.configure({ enabled: true });
		expect(resizable.handle.hidden).toBe(false);
		expect(resizable.handle.tabIndex).toBe(0);
	});

	it('takes itself away, leaving the element as it was', () => {
		document.body.innerHTML = '<div id="r" class="box"></div>';
		const div = document.getElementById('r')!;
		const resizable = new Resizable(div, { width: 200, height: 100 });

		resizable.destroy();
		expect(div.outerHTML).toBe('<div id="r" class="box"></div>');
		expect(Resizable.of(div)).toBeNull();
	});

	it('stops listening when the host removes its element', () => {
		let removing: ((element: Element) => void) | undefined;
		const host: Host = { ...standaloneHost, onNodeRemoving: (listener) => {
			removing = listener;
			return () => undefined;
		} };
		setHost(host);
		const onResized = vi.fn();
		const resizable = new Resizable(element(), { onResized });

		removing!(document.body);
		key(resizable.handle, 'ArrowDown');
		expect(onResized).not.toHaveBeenCalled();
	});

	it('works as a Svelte action, and as Wicket attaches it', () => {
		const div = element();
		const action = resizable(div, { axis: 'vertical' });

		expect(div.classList.contains('antilia-resizable-vertical')).toBe(true);
		action.update({ axis: 'horizontal' });
		expect(div.classList.contains('antilia-resizable-horizontal')).toBe(true);
		action.destroy();
		expect(div.hasAttribute('class')).toBe(false);

		expect(Resizable.attach('r', { height: 90 })?.getSize()).toEqual({ width: null, height: 90 });
		expect(Resizable.attach('nothing')).toBeNull();
		expect(Resizable.of('nothing')).toBeNull();
	});
});
