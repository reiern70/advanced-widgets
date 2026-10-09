/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, describe, expect, it } from 'vitest';

import { FloatingPanel } from '../src/index.js';

function box(element: Element, rect: Partial<DOMRect>): void {
	const full = { left: 0, top: 0, width: 0, height: 0, right: 0, bottom: 0, x: 0, y: 0, ...rect };
	element.getBoundingClientRect = () => full as DOMRect;
}

function viewport(width: number, height: number): void {
	Object.defineProperty(document.documentElement, 'clientWidth', { configurable: true,
		value: width });
	Object.defineProperty(document.documentElement, 'clientHeight', { configurable: true,
		value: height });
}

function pointer(target: Element, type: string, x: number, y: number): void {
	const event = new MouseEvent(type, { bubbles: true, clientX: x, clientY: y, button: 0 });
	Object.defineProperty(event, 'pointerId', { value: 1 });
	target.dispatchEvent(event);
}

function panel(draggable = true): { panel: HTMLElement; header: HTMLElement } {
	document.body.innerHTML = `<div ${draggable ? 'data-antilia-floating-draggable' : ''}>` +
		'<div class="antilia-floating-panel"><div class="antilia-floating-panel-header">Title</div>' +
		'</div></div>';
	const element = document.querySelector<HTMLElement>('.antilia-floating-panel')!;
	const header = element.querySelector<HTMLElement>('.antilia-floating-panel-header')!;
	box(element, { left: 100, top: 100, width: 200, height: 150 });
	box(header, { bottom: 130 });
	return { panel: element, header };
}

afterEach(() => {
	document.body.innerHTML = '';
});

describe('FloatingPanel', () => {
	it('moves by the distance, keeping the title bar in the window', () => {
		viewport(1000, 800);
		const { panel: element } = panel();

		FloatingPanel.moveBy(element, 50, -20);
		expect(element.style.translate).toBe('50px -20px');

		FloatingPanel.moveBy(element, 5000, 5000);
		// its box did not move in jsdom: from 100,100 at most to 1000 - 200 and 800 - 30
		expect(element.style.translate).toBe('750px 650px');
	});

	it('moves the dialog it is shown in', () => {
		document.body.innerHTML = '<div role="dialog"><div class="antilia-floating-panel"></div></div>';
		const element = document.querySelector<HTMLElement>('.antilia-floating-panel')!;

		expect(FloatingPanel.movedElement(element)).toBe(document.querySelector('[role="dialog"]'));
	});

	it('is dragged by its title bar once installed', () => {
		viewport(1000, 800);
		const { panel: element, header } = panel();
		header.setPointerCapture = () => undefined;
		const uninstall = FloatingPanel.install();

		pointer(header, 'pointerdown', 10, 10);
		expect(element.classList.contains('antilia-floating-panel-dragging')).toBe(true);
		pointer(header, 'pointermove', 40, 30);
		pointer(header, 'pointerup', 40, 30);
		uninstall();

		expect(element.style.translate).toBe('30px 20px');
		expect(element.classList.contains('antilia-floating-panel-dragging')).toBe(false);
	});

	it('stays put when not in a draggable element', () => {
		const { panel: element, header } = panel(false);
		const uninstall = FloatingPanel.install();

		pointer(header, 'pointerdown', 10, 10);
		pointer(header, 'pointermove', 40, 30);
		uninstall();

		expect(element.style.translate).toBe('');
	});
});
