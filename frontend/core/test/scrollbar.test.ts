/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, describe, expect, it } from 'vitest';

import { Scrollbar, scrollbar, setHost, standaloneHost } from '../src/index.js';

function box(id = 'box', style = 'overflow-x: auto; overflow-y: auto'): HTMLElement {
	document.body.innerHTML = `<div id="${id}" style="${style}"><p>content</p></div>`;
	return document.getElementById(id)!;
}

function sizes(element: HTMLElement, client: number, scroll: number): void {
	Object.defineProperty(element, 'clientHeight', { configurable: true, value: client });
	Object.defineProperty(element, 'scrollHeight', { configurable: true, value: scroll });
}

function tracks(): Element[] {
	return Array.from(document.querySelectorAll('.wicket-scrollbar-track'));
}

afterEach(() => {
	Scrollbar.instances.slice().forEach((instance) => instance.destroy());
	setHost();
	document.body.innerHTML = '';
});

describe('Scrollbar', () => {
	it('wraps the element and draws bars along the axes its CSS scrolls', () => {
		const element = box();
		const instance = Scrollbar.attach('box')!;

		expect(element.parentElement?.className).toBe('wicket-scrollbar wicket-scrollbar--both');
		expect(element.classList.contains('wicket-scrollbar-host')).toBe(true);
		expect(tracks().map((track) => track.className)).toEqual([
			'wicket-scrollbar-track wicket-scrollbar-track--y',
			'wicket-scrollbar-track wicket-scrollbar-track--x'
		]);
		expect(Scrollbar.instances).toEqual([instance]);
	});

	it('draws only the bars of the axis asked for', () => {
		box();
		Scrollbar.attach('box', { axis: 'y' });

		expect(tracks().length).toBe(1);
		expect(document.querySelector('.wicket-scrollbar--both')).toBeNull();
	});

	it('hides a bar while nothing overflows, sizes its thumb when something does', () => {
		const element = box('box', 'overflow-y: auto');
		sizes(element, 100, 100);
		const instance = new Scrollbar(element);
		const track = tracks()[0] as HTMLElement;
		expect(track.hidden).toBe(true);

		Object.defineProperty(track, 'clientHeight', { configurable: true, value: 100 });
		sizes(element, 100, 400);
		element.scrollTop = 150;
		instance.update();

		expect(track.hidden).toBe(false);
		const thumb = track.firstElementChild as HTMLElement;
		expect(thumb.style.height).toBe('25px');
		expect(thumb.style.transform).toBe('translateY(37.5px)');
	});

	it('reuses the wrapper and replaces the bars when attached again', () => {
		const element = box();
		const first = Scrollbar.attach(element)!;
		const second = Scrollbar.attach(element, { axis: 'x' })!;

		expect(second.wrapper).toBe(first.wrapper);
		expect(tracks().length).toBe(1);
		expect(Scrollbar.instances).toEqual([second]);
	});

	it('gives the native scroll bars back on destroy, leaving the wrapper', () => {
		const element = box();
		Scrollbar.attach(element)!.destroy();

		expect(tracks().length).toBe(0);
		expect(element.classList.contains('wicket-scrollbar-host')).toBe(false);
		expect(element.parentElement?.className).toBe('wicket-scrollbar');
		expect(Scrollbar.instances).toEqual([]);
	});

	it('is destroyed when the host removes the element', () => {
		let removing: ((element: Element) => void) | undefined;
		setHost({
			...standaloneHost,
			onNodeRemoving(listener) {
				removing = listener;
				return () => {
					removing = undefined;
				};
			}
		});
		const element = box();
		Scrollbar.attach(element);

		removing!(element);

		expect(Scrollbar.instances).toEqual([]);
		expect(removing).toBeUndefined();
	});

	it('is a Svelte action', () => {
		const element = box();
		const action = scrollbar(element, { axis: 'y' });
		action.update({ axis: 'both' });
		expect(tracks().length).toBe(2);

		action.destroy();
		expect(tracks().length).toBe(0);
	});
});
