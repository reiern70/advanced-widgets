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
	return Array.from(document.querySelectorAll('.antilia-scrollbar-track'));
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

		expect(element.parentElement?.className).toBe('antilia-scrollbar antilia-scrollbar--both');
		expect(element.classList.contains('antilia-scrollbar-host')).toBe(true);
		expect(tracks().map((track) => track.className)).toEqual([
			'antilia-scrollbar-track antilia-scrollbar-track--y',
			'antilia-scrollbar-track antilia-scrollbar-track--x'
		]);
		expect(Scrollbar.instances).toEqual([instance]);
	});

	it('draws only the bars of the axis asked for', () => {
		box();
		Scrollbar.attach('box', { axis: 'y' });

		expect(tracks().length).toBe(1);
		expect(document.querySelector('.antilia-scrollbar--both')).toBeNull();
	});

	it('hides a bar while nothing overflows, sizes its thumb when something does', () => {
		const element = box('box', 'overflow-y: auto');
		sizes(element, 100, 100);
		const instance = new Scrollbar(element);
		const track = tracks()[0] as HTMLElement;
		expect(track.hidden).toBe(true);

		const rail = track.firstElementChild as HTMLElement;
		expect(rail.className).toBe('antilia-scrollbar-rail');
		Object.defineProperty(rail, 'clientHeight', { configurable: true, value: 100 });
		sizes(element, 100, 400);
		element.scrollTop = 150;
		instance.update();

		expect(track.hidden).toBe(false);
		const thumb = rail.firstElementChild as HTMLElement;
		expect(thumb.style.height).toBe('25px');
		expect(thumb.style.transform).toBe('translateY(37.5px)');
	});

	it('lays the bars inside the border of the element, on the background it is painted on', () => {
		document.body.innerHTML = '<div style="background-color: rgb(1, 2, 3)"><div id="box" ' +
			'style="overflow-y: auto; border: 3px solid; border-top-right-radius: 10px; border-bottom-right-radius: 2px"></div></div>';
		const instance = Scrollbar.attach('box')!;
		const style = instance.wrapper.style;

		expect(style.getPropertyValue('--antilia-scrollbar-background')).toBe('rgb(1, 2, 3)');
		expect(style.getPropertyValue('--antilia-scrollbar-inset-right')).toBe('3px');
		expect(style.getPropertyValue('--antilia-scrollbar-radius-top-right')).toBe('7px');
		expect(style.getPropertyValue('--antilia-scrollbar-radius-bottom-right')).toBe('0px');

		instance.destroy();
		expect(style.getPropertyValue('--antilia-scrollbar-background')).toBe('');
	});

	it('runs the vertical bar below a header staying in place', () => {
		document.body.innerHTML = '<div id="box" style="overflow-y: auto; border: 3px solid; ' +
			'border-top-right-radius: 10px"><table><thead><tr><th>A</th></tr></thead></table></div>';
		const head = document.querySelector('thead')!;
		Object.defineProperty(head, 'offsetHeight', { configurable: true, value: 30 });
		const style = Scrollbar.attach('box', { header: ':scope > table > thead' })!.wrapper.style;

		expect(style.getPropertyValue('--antilia-scrollbar-inset-top')).toBe('33px');
		expect(style.getPropertyValue('--antilia-scrollbar-radius-top-right')).toBe('0px');
	});

	it('scrolls to the top, the bottom, a position and an element', () => {
		document.body.innerHTML = '<div id="box" style="overflow: auto">' +
			'<p data-line="1">1</p><p data-line="2">2</p></div>';
		const element = document.getElementById('box')!;
		sizes(element, 100, 400);
		const instance = Scrollbar.attach('box')!;
		expect(Scrollbar.of('box')).toBe(instance);
		expect(Scrollbar.of(document.body)).toBeUndefined();

		instance.scrollToBottom();
		expect(element.scrollTop).toBe(300);
		instance.scrollToTop();
		expect(element.scrollTop).toBe(0);
		instance.scrollTo({ top: 120, left: 7 });
		expect([element.scrollTop, element.scrollLeft]).toEqual([120, 7]);

		const line = element.querySelector('[data-line="2"]')!;
		element.getBoundingClientRect = () => new DOMRect(0, 50, 200, 100);
		line.getBoundingClientRect = () => new DOMRect(0, 80, 100, 20);
		expect(instance.scrollToElement('[data-line="2"]')).toBe(true);
		expect([element.scrollTop, element.scrollLeft]).toEqual([150, 7]);
		expect(instance.scrollToElement('[data-line="3"]')).toBe(false);
		expect(instance.scrollToElement(document.body)).toBe(false);
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
		expect(element.classList.contains('antilia-scrollbar-host')).toBe(false);
		expect(element.parentElement?.className).toBe('antilia-scrollbar');
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
