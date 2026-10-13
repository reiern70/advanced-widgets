/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, describe, expect, it, vi } from 'vitest';

import { ANTILIA_LOOK, BOOTSTRAP_LOOK, Popover, popoverClasses, POPOVER_DESIGNS, POPOVER_PLACEMENTS, type PopoverDesign,
	type PopoverPlacement } from '../src/index.js';

/** Lets the mutation observers run. */
function settle(): Promise<void> {
	return new Promise((done) => setTimeout(done, 0));
}

/**
 * @returns a trigger in a box with a theme, the trigger at the rectangle given in a window of 1000
 *          by 800 pixels
 */
function trigger(rect = { left: 400, top: 300, width: 20, height: 20 }): HTMLButtonElement {
	document.body.innerHTML = '<div id="themed"><button id="t" type="button">i</button>' +
		'<button id="other" type="button">other</button></div>';
	const themed = document.getElementById('themed')!;
	themed.style.setProperty('--antilia-theme-primary', '#123456');
	themed.style.setProperty('--antilia-theme-surface', '#101010');
	Object.defineProperty(document.documentElement, 'clientWidth', { value: 1000, configurable: true });
	Object.defineProperty(document.documentElement, 'clientHeight', { value: 800, configurable: true });
	const button = document.getElementById('t') as HTMLButtonElement;
	button.getBoundingClientRect = () => ({ ...rect, right: rect.left + rect.width,
		bottom: rect.top + rect.height, x: rect.left, y: rect.top, toJSON: () => rect }) as DOMRect;
	return button;
}

/** Gives every popover a size: jsdom lays nothing out. */
function sized(width = 200, height = 100): void {
	Object.defineProperty(HTMLElement.prototype, 'offsetWidth', { configurable: true,
		get(this: HTMLElement) {
			return this.classList.contains('antilia-popover') ? width : 0;
		} });
	Object.defineProperty(HTMLElement.prototype, 'offsetHeight', { configurable: true,
		get(this: HTMLElement) {
			return this.classList.contains('antilia-popover') ? height : 0;
		} });
}

function popover(): HTMLElement | null {
	return document.querySelector('.antilia-popover');
}

function where(element: HTMLElement): [string, string, string] {
	return [element.style.getPropertyValue('--antilia-popover-x'),
		element.style.getPropertyValue('--antilia-popover-y'),
		element.style.getPropertyValue('--antilia-popover-arrow')];
}

afterEach(() => {
	Popover.closeAll();
	document.body.innerHTML = '';
});

describe('Popover', () => {
	it('opens below its trigger, with its title, its body and a close button', () => {
		sized();
		const button = trigger();
		const shown = Popover.show(button, { title: 'notes.txt', body: '3 lines', variant: 'info',
			closeLabel: 'Schließen', className: 'mine' })!;

		const element = popover()!;
		expect(element).toBe(shown.element);
		expect(element.parentElement).toBe(document.body);
		expect(element.className).toBe('antilia-popover antilia-popover-info antilia-popover-card ' +
			'antilia-look-antilia mine');
		expect(element.getAttribute('data-antilia-look')).toBe('antilia');
		expect(element.getAttribute('role')).toBe('dialog');
		expect(element.getAttribute('data-side')).toBe('bottom');
		const title = element.querySelector('.antilia-popover-title')!;
		expect(title.textContent).toBe('notes.txt');
		expect(element.getAttribute('aria-labelledby')).toBe(title.id);
		expect(element.querySelector('.antilia-popover-body')!.textContent).toBe('3 lines');
		expect(element.querySelector<HTMLElement>('.antilia-popover-close')!.title).toBe('Schließen');
		expect(document.activeElement).toBe(element);
		expect(button.getAttribute('aria-expanded')).toBe('true');
		expect(button.getAttribute('aria-controls')).toBe(element.id);
		expect(button.classList.contains('antilia-popover-open')).toBe(true);
		expect(Popover.current()).toBe(shown);
		// centered under the trigger, the arrow at its middle
		expect(where(element)).toEqual(['310px', '330px', '100px']);
		// in the theme of its trigger, out of it in the body
		expect(element.style.getPropertyValue('--antilia-theme-primary')).toBe('#123456');
		expect(element.getAttribute('style')).not.toContain('left:');
	});

	it('takes markup or a node as its body, and goes without a header', () => {
		trigger();
		Popover.show('t', { body: '<b>bold</b>', html: true, closable: false });
		expect(popover()!.querySelector('.antilia-popover-body b')?.textContent).toBe('bold');
		expect(popover()!.querySelector('.antilia-popover-header')).toBeNull();

		const node = document.createElement('em');
		node.textContent = 'node';
		Popover.show('t', { body: node });
		expect(document.querySelectorAll('.antilia-popover').length).toBe(1);
		expect(popover()!.querySelector('em')).toBe(node);
		expect(Popover.show('missing')).toBeNull();
		expect(Popover.toggle('missing')).toBeNull();
	});

	it('moves to the other side without room, and stays in the window', () => {
		sized(200, 100);
		// near the bottom right corner: no room below, the popover goes above, shifted left
		Popover.show(trigger({ left: 950, top: 740, width: 20, height: 20 }), { placement: 'bottom-center' });
		expect(popover()!.getAttribute('data-side')).toBe('top');
		expect(where(popover()!)).toEqual(['792px', '630px', '168px']);

		Popover.show(trigger({ left: 10, top: 300, width: 20, height: 20 }), { placement: 'left-center' });
		expect(popover()!.getAttribute('data-side')).toBe('right');
		expect(where(popover()!)).toEqual(['40px', '260px', '50px']);

		Popover.show(trigger({ left: 400, top: 20, width: 20, height: 20 }), { placement: 'auto' });
		expect(popover()!.getAttribute('data-side')).toBe('bottom');
		Popover.show(trigger({ left: 400, top: 300, width: 20, height: 20 }), { placement: 'top-center' });
		expect(popover()!.getAttribute('data-side')).toBe('top');
	});

	it('opens along its trigger from one of its edges, its arrow at the middle of the trigger', () => {
		sized(200, 100);
		const opened = (placement: PopoverPlacement, rect?: { left: number; top: number;
			width: number; height: number }) => {
			Popover.show(trigger(rect), { placement });
			return [popover()!.getAttribute('data-side'), popover()!.getAttribute('data-align'),
				...where(popover()!)];
		};
		// a small trigger: the popover starts before its edge, so that the arrow points at it
		expect(opened('bottom-left')).toEqual(['bottom', 'start', '390px', '330px', '20px']);
		expect(opened('bottom-right')).toEqual(['bottom', 'end', '230px', '330px', '180px']);
		expect(opened('right-top')).toEqual(['right', 'start', '430px', '290px', '20px']);
		expect(opened('left-bottom')).toEqual(['left', 'end', '190px', '230px', '80px']);
		expect(opened('left-center')).toEqual(['left', 'center', '190px', '260px', '50px']);
		// a wide one: the popover starts at its edge
		const wide = { left: 100, top: 300, width: 300, height: 20 };
		expect(opened('top-left', wide)).toEqual(['top', 'start', '100px', '190px', '150px']);
		expect(opened('top-right', wide)).toEqual(['top', 'end', '200px', '190px', '50px']);
		// an edge across the side means nothing
		expect(opened('top-top' as PopoverPlacement)).toEqual(['top', 'center', '310px', '190px',
			'100px']);
		expect(POPOVER_PLACEMENTS.length).toBe(13);
	});

	it('opens on a side across without room on the side asked nor on the opposite one', () => {
		sized(200, 100);
		const button = trigger({ left: 140, top: 300, width: 20, height: 20 });
		Object.defineProperty(document.documentElement, 'clientWidth', { value: 300,
			configurable: true });
		Popover.show(button, { placement: 'left-center' });
		expect(popover()!.getAttribute('data-side')).toBe('bottom');
		expect(where(popover()!)).toEqual(['50px', '330px', '100px']);
		expect(popover()!.hasAttribute('data-cramped')).toBe(false);
	});

	it('stays in the window without room on any side, over its trigger, without arrow', () => {
		sized(200, 100);
		const button = trigger({ left: 100, top: 55, width: 20, height: 20 });
		Object.defineProperty(document.documentElement, 'clientWidth', { value: 220,
			configurable: true });
		Object.defineProperty(document.documentElement, 'clientHeight', { value: 130,
			configurable: true });
		Popover.show(button, { placement: 'top-center' });
		expect(popover()!.getAttribute('data-side')).toBe('top');
		expect(where(popover()!)).toEqual(['10px', '8px', '100px']);
		expect(popover()!.hasAttribute('data-cramped')).toBe(true);
	});

	it('takes its design, card by default', () => {
		trigger();
		POPOVER_DESIGNS.forEach((design) => {
			Popover.show('t', { design, variant: 'warning' });
			expect(popover()!.className).toBe('antilia-popover antilia-popover-warning ' +
				'antilia-popover-' + design + ' antilia-look-antilia');
		});
		Popover.show('t', { design: 'nope' as PopoverDesign });
		expect(popover()!.className).toBe('antilia-popover antilia-popover-default ' +
			'antilia-popover-card antilia-look-antilia');
	});

	it('takes the look of its trigger, carrying it into the body', () => {
		sized();
		trigger();
		document.getElementById('themed')!.setAttribute('data-antilia-look',
			'bootstrap color:antilia');
		Popover.show('t', { title: 'notes.txt', body: '3 lines', variant: 'error' });

		const element = popover()!;
		expect(element.className).toBe('antilia-popover antilia-popover-error ' +
			'antilia-popover-card antilia-look-bootstrap antilia-look-antilia');
		expect(element.getAttribute('data-antilia-look')).toBe('bootstrap color:antilia');
		expect(element.querySelector('.antilia-popover-header')!.className)
			.toBe('antilia-popover-header popover-header');
		expect(element.querySelector('.antilia-popover-body')!.className)
			.toBe('antilia-popover-body popover-body');
		expect(element.querySelector('.antilia-popover-close')!.className)
			.toBe('antilia-popover-close btn-close');
		expect(element.querySelector('.antilia-popover-arrow')!.className)
			.toBe('antilia-popover-arrow');
	});

	it('takes the look it is given over that of its trigger, and draws one as it would', () => {
		trigger();
		document.getElementById('themed')!.setAttribute('data-antilia-look', 'bootstrap');
		Popover.show('t', { look: ANTILIA_LOOK });

		expect(popover()!.className).toBe(popoverClasses(ANTILIA_LOOK));
		expect(popover()!.querySelector('.antilia-popover-close')!.className)
			.toBe('antilia-popover-close');
		expect(popoverClasses(BOOTSTRAP_LOOK, 'glass', 'info')).toBe('antilia-popover ' +
			'antilia-popover-info antilia-popover-glass antilia-look-bootstrap');
	});

	it('shows an element of the page while open, then puts it back in its place', () => {
		const button = trigger();
		document.body.insertAdjacentHTML('beforeend', '<p id="before">a</p>' +
			'<div id="rich" class="antilia-popover-content"><input id="field" autofocus></div>' +
			'<p id="after">b</p>');
		const rich = document.getElementById('rich')!;

		Popover.show(button, { body: 'Text', content: 'rich' });

		const body = popover()!.querySelector('.antilia-popover-body')!;
		expect(body.firstChild!.textContent).toBe('Text');
		expect(body.lastChild).toBe(rich);
		expect(document.activeElement).toBe(document.getElementById('field'));
		expect(document.getElementById('before')!.nextSibling!.nodeType).toBe(Node.COMMENT_NODE);

		Popover.closeAll();
		expect(document.getElementById('before')!.nextElementSibling).toBe(rich);
		expect(rich.nextElementSibling!.id).toBe('after');
		expect(document.querySelectorAll('#rich').length).toBe(1);
	});

	it('puts back the content as the page rendered it again while open', () => {
		const button = trigger();
		document.body.insertAdjacentHTML('beforeend',
			'<p id="before">a</p><div id="rich" class="antilia-popover-content">old</div>');
		const rich = document.getElementById('rich')!;
		Popover.show(button, { content: rich });

		// as Wicket replaces a component it renders via Ajax
		const fresh = document.createElement('div');
		fresh.id = 'rich';
		fresh.textContent = 'new';
		rich.replaceWith(fresh);
		Popover.closeAll();

		expect(document.getElementById('before')!.nextElementSibling).toBe(fresh);
		expect(rich.isConnected).toBe(false);
	});

	it('closes on Escape, giving the trigger the focus back', () => {
		const onClose = vi.fn();
		const button = trigger();
		Popover.show(button, { title: 'T', onClose });

		popover()!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));

		expect(popover()).toBeNull();
		expect(onClose).toHaveBeenCalledTimes(1);
		expect(document.activeElement).toBe(button);
		expect(button.getAttribute('aria-expanded')).toBe('false');
		expect(button.hasAttribute('aria-controls')).toBe(false);
		expect(Popover.current()).toBeNull();
	});

	it('closes on a click outside, but not on one in it', () => {
		trigger();
		const shown = Popover.show('t', { title: 'T' })!;
		popover()!.querySelector('.antilia-popover-body')!.dispatchEvent(
			new MouseEvent('pointerdown', { bubbles: true }));
		expect(shown.isOpen()).toBe(true);

		document.getElementById('other')!.dispatchEvent(new MouseEvent('pointerdown', { bubbles: true }));
		expect(shown.isOpen()).toBe(false);
		expect(popover()).toBeNull();
	});

	it('closes with its close button', () => {
		const button = trigger();
		Popover.show(button, { title: 'T' });
		popover()!.querySelector<HTMLButtonElement>('.antilia-popover-close')!.click();
		expect(popover()).toBeNull();
		expect(document.activeElement).toBe(button);
	});

	it('closes on a click on its trigger, which the trigger does not get', () => {
		const button = trigger();
		const opened = vi.fn();
		button.addEventListener('click', opened);
		Popover.show(button, { title: 'T' });

		button.click();

		expect(popover()).toBeNull();
		expect(opened).not.toHaveBeenCalled();
		button.click();
		expect(opened).toHaveBeenCalledTimes(1);
	});

	it('toggles: opening one at the trigger already open closes it', () => {
		const button = trigger();
		expect(Popover.toggle(button, { title: 'T' })).not.toBeNull();
		expect(Popover.toggle(button, { title: 'T' })).toBeNull();
		expect(popover()).toBeNull();
	});

	it('closes when its trigger leaves the page', async () => {
		const button = trigger();
		Popover.show(button, { title: 'T' });
		button.remove();
		await settle();
		expect(popover()).toBeNull();
	});

	it('follows its trigger as the page scrolls', async () => {
		sized();
		const button = trigger();
		Popover.show(button, { maxWidth: '30rem' });
		expect(popover()!.style.getPropertyValue('--antilia-popover-max-width')).toBe('30rem');
		button.getBoundingClientRect = () => ({ left: 400, top: 100, width: 20, height: 20,
			right: 420, bottom: 120 }) as DOMRect;

		window.dispatchEvent(new Event('scroll'));
		await new Promise((done) => requestAnimationFrame(() => done(undefined)));

		expect(where(popover()!)[1]).toBe('130px');
	});
});
