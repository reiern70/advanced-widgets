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

import { Desktop, desktop, setHost, standaloneHost, type Host, type WindowState }
	from '../src/index.js';

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

function windowMarkup(id: string, attributes = ''): string {
	return '<section class="antilia-window" id="' + id + '" ' + attributes + '>' +
		'<header class="antilia-window-header"><span class="antilia-window-icon"><svg></svg></span>' +
		'<span class="antilia-window-title">Title ' + id + '</span></header>' +
		'<div class="antilia-window-body"><input id="' + id + '-field"></div></section>';
}

/**
 * @returns the element of a desktop of 1000 by 600 pixels with three windows, one, two and three,
 *          and a taskbar unless left out
 */
function element(taskbar = true, extra: Record<string, string> = {}): HTMLElement {
	document.body.innerHTML = '<div id="d"><div class="antilia-desktop-area">' +
		windowMarkup('one', 'data-x="10" data-y="20" data-width="300" data-height="200" ' +
			(extra.one ?? '')) +
		windowMarkup('two', 'data-x="100" data-y="80" data-width="320" data-height="220" ' +
			(extra.two ?? '')) +
		windowMarkup('three', extra.three ?? '') +
		'</div>' + (taskbar ? '<div class="antilia-desktop-taskbar"></div>' : '') + '</div>';
	const area = document.querySelector('.antilia-desktop-area')!;
	Object.defineProperty(area, 'clientWidth', { value: 1000, configurable: true });
	Object.defineProperty(area, 'clientHeight', { value: 600, configurable: true });
	return document.getElementById('d')!;
}

function win(id: string): HTMLElement {
	return document.getElementById(id)!;
}

function header(id: string): HTMLElement {
	return win(id).querySelector('.antilia-window-header')!;
}

function control(id: string, name: string): HTMLButtonElement {
	return win(id).querySelector('.antilia-window-control-' + name)!;
}

function task(id: string): HTMLButtonElement {
	return document.querySelector('.antilia-desktop-task[data-window="' + id + '"]')!;
}

function place(id: string): [string, string, string, string, string] {
	const style = win(id).style;
	return [style.getPropertyValue('--antilia-window-x'),
		style.getPropertyValue('--antilia-window-y'),
		style.getPropertyValue('--antilia-window-width'),
		style.getPropertyValue('--antilia-window-height'),
		style.getPropertyValue('--antilia-window-z')];
}

/** Lets the mutation observers run. */
function settle(): Promise<void> {
	return new Promise((done) => setTimeout(done, 0));
}

afterEach(() => {
	document.body.innerHTML = '';
	setHost(standaloneHost);
});

describe('Desktop', () => {
	it('places the windows where their attributes say, the last one in front', () => {
		const desk = new Desktop(element());

		expect(desk.element.className).toBe('antilia-desktop antilia-desktop-taskbar-bottom');
		expect(desk.ids()).toEqual(['one', 'two', 'three']);
		expect(place('one')).toEqual(['10px', '20px', '300px', '200px', '1']);
		expect(place('two')).toEqual(['100px', '80px', '320px', '220px', '2']);
		// the third has none: it cascades, as large as by default
		expect(place('three')).toEqual(['70px', '70px', '420px', '280px', '3']);
		expect(desk.activeId()).toBe('three');
		expect(win('three').hasAttribute('data-active')).toBe(true);
		expect(win('one').hasAttribute('data-active')).toBe(false);
		expect(Desktop.of('d')).toBe(desk);
	});

	it('labels the windows by their titles and gives them buttons and handles', () => {
		new Desktop(element(true, { three: 'data-closable="true" data-resizable="false"' }));

		expect(win('one').getAttribute('role')).toBe('region');
		expect(win('one').getAttribute('aria-labelledby')).toBe('one-title');
		expect(win('one').tabIndex).toBe(-1);
		expect(Array.from(win('one').querySelectorAll('.antilia-window-control'))
			.map((button) => button.getAttribute('aria-label')))
			.toEqual(['Move to another pane', 'Minimize', 'Maximize']);
		expect(Array.from(win('three').querySelectorAll('.antilia-window-control'))
			.map((button) => button.getAttribute('aria-label')))
			.toEqual(['Move to another pane', 'Minimize', 'Maximize', 'Close']);
		expect(win('one').querySelectorAll('.antilia-window-handle')).toHaveLength(8);
		expect(win('three').querySelectorAll('.antilia-window-handle')).toHaveLength(0);
	});

	it('has a button in the taskbar for each window, with its icon and title', () => {
		const desk = new Desktop(element(), { texts: { taskbar: 'Open windows' } });

		const tasks = Array.from(document.querySelectorAll<HTMLElement>('.antilia-desktop-task'));
		expect(tasks.map((task) => task.textContent)).toEqual(['Title one', 'Title two',
			'Title three']);
		expect(tasks[0].querySelector('.antilia-desktop-task-icon svg')).not.toBeNull();
		expect(tasks.map((task) => task.getAttribute('aria-pressed'))).toEqual(['false', 'false',
			'true']);
		expect(desk.taskbar.getAttribute('role')).toBe('toolbar');
		expect(desk.taskbar.getAttribute('aria-label')).toBe('Open windows');
		expect(Array.from(document.querySelectorAll('.antilia-desktop-tile'))
			.map((tile) => tile.getAttribute('title'))).toEqual(['Tile the windows',
			'Windows side by side', 'Windows one above the other', 'Cascade the windows']);
	});

	it('adds a taskbar when the desktop has none, and the tiling only when wanted', () => {
		const desk = new Desktop(element(false), { taskbar: 'left', tiling: false });

		expect(desk.taskbar.parentElement).toBe(desk.element);
		expect(desk.element.classList.contains('antilia-desktop-taskbar-left')).toBe(true);
		expect(desk.taskbar.getAttribute('aria-orientation')).toBe('vertical');
		expect(document.querySelector('.antilia-desktop-tile')).toBeNull();

		desk.destroy();
		expect(document.querySelector('.antilia-desktop-taskbar')).toBeNull();
		expect(win('one').getAttribute('style')).toBeNull();
		expect(win('one').querySelector('.antilia-window-controls')).toBeNull();
		expect(Desktop.of('d')).toBeNull();
	});

	it('needs an area for the windows', () => {
		document.body.innerHTML = '<div id="d"></div>';

		expect(() => new Desktop(document.getElementById('d')!)).toThrow(/antilia-desktop-area/);
		expect(Desktop.attach('none')).toBeNull();
	});

	it('takes the state the windows start in from their attributes', () => {
		new Desktop(element(true, { one: 'data-minimized', two: 'data-maximized',
			three: 'data-closed data-closable="true"' }));

		expect(win('one').hidden).toBe(true);
		expect(task('one').hasAttribute('data-minimized')).toBe(true);
		expect(win('two').hasAttribute('data-maximized')).toBe(true);
		expect(win('three').hidden).toBe(true);
		expect(task('three').hidden).toBe(true);
		expect(win('two').hasAttribute('data-active')).toBe(true);
	});

	it('brings a window the user points at to the front, the others staying as they were', () => {
		const onActivate = vi.fn();
		const onChange = vi.fn();
		const desk = new Desktop(element(), { onActivate, onChange });

		pointer(win('one').querySelector('.antilia-window-body')!, 'pointerdown', 50, 50);

		expect(desk.activeId()).toBe('one');
		expect(place('one')[4]).toBe('3');
		expect(place('two')[4]).toBe('1');
		expect(place('three')[4]).toBe('2');
		expect(task('one').getAttribute('aria-pressed')).toBe('true');
		expect(onActivate).toHaveBeenCalledWith('one');
		expect(onChange).toHaveBeenCalledTimes(1);
		expect(onChange.mock.calls[0][0].one.order).toBe(3);

		pointer(win('one'), 'pointerdown', 50, 50);
		expect(onChange).toHaveBeenCalledTimes(1);
	});

	it('brings the window the focus goes into to the front', () => {
		const desk = new Desktop(element());

		document.getElementById('one-field')!.focus();

		expect(desk.activeId()).toBe('one');
	});

	it('moves a window by its title bar, keeping it on the desktop', () => {
		const onChange = vi.fn();
		const desk = new Desktop(element(), { onChange });

		pointer(header('one'), 'pointerdown', 100, 30);
		expect(desk.element.classList.contains('antilia-desktop-moving')).toBe(true);
		pointer(header('one'), 'pointermove', 160, 70);
		expect(place('one').slice(0, 2)).toEqual(['70px', '60px']);
		expect(onChange).toHaveBeenCalledTimes(1);

		pointer(header('one'), 'pointermove', -2000, -500);
		// 80 pixels stay on the desktop, the title bar below its top
		expect(place('one').slice(0, 2)).toEqual(['-220px', '0px']);
		pointer(header('one'), 'pointermove', 5000, 5000);
		expect(place('one').slice(0, 2)).toEqual(['920px', '568px']);

		pointer(header('one'), 'pointerup', 5000, 5000);
		expect(desk.element.classList.contains('antilia-desktop-moving')).toBe(false);
		expect(desk.state('one')).toMatchObject({ x: 920, y: 568 });
		expect(onChange).toHaveBeenCalledTimes(2);
		expect(onChange.mock.calls[1][0].one).toMatchObject({ x: 920, y: 568 });
	});

	it('does not move a window from the buttons of its title bar, nor a maximized one', () => {
		const desk = new Desktop(element());

		pointer(control('one', 'minimize'), 'pointerdown', 100, 30);
		pointer(control('one', 'minimize'), 'pointermove', 300, 300);
		expect(desk.state('one')).toMatchObject({ x: 10, y: 20 });

		desk.maximize('one');
		pointer(header('one'), 'pointerdown', 100, 30);
		pointer(header('one'), 'pointermove', 300, 300);
		expect(desk.state('one')).toMatchObject({ x: 10, y: 20 });
	});

	it('resizes a window by its edges and corners, down to its smallest size', () => {
		const onChange = vi.fn();
		const desk = new Desktop(element(true, { one: 'data-min-width="200"' }), { onChange });
		const handle = (side: string) => win('one').querySelector('.antilia-window-handle-' + side)!;

		pointer(handle('se'), 'pointerdown', 310, 220);
		pointer(handle('se'), 'pointermove', 360, 250);
		pointer(handle('se'), 'pointerup', 360, 250);
		expect(desk.state('one')).toMatchObject({ x: 10, y: 20, width: 350, height: 230 });

		pointer(handle('nw'), 'pointerdown', 10, 20);
		pointer(handle('nw'), 'pointermove', 400, -100);
		pointer(handle('nw'), 'pointerup', 400, -100);
		// the width stops at 200, the top at the top of the desktop
		expect(desk.state('one')).toMatchObject({ x: 160, y: 0, width: 200, height: 250 });
		expect(onChange).toHaveBeenCalledTimes(3);
	});

	it('folds a window to its title bar with a double click, and unfolds it', () => {
		const onChange = vi.fn();
		const desk = new Desktop(element(true, { two: 'data-foldable="false"' }), { onChange });

		header('one').dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(win('one').hasAttribute('data-folded')).toBe(true);
		expect(onChange).toHaveBeenCalledTimes(1);

		header('one').dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(win('one').hasAttribute('data-folded')).toBe(false);

		header('two').dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(desk.state('two')!.folded).toBe(false);
		control('one', 'maximize').dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(desk.state('one')!.folded).toBe(false);
	});

	it('maximizes a window with its button and restores it', () => {
		const onChange = vi.fn();
		const desk = new Desktop(element(), { onChange, texts: { restore: 'Back' } });

		control('one', 'maximize').click();
		expect(win('one').hasAttribute('data-maximized')).toBe(true);
		expect(desk.activeId()).toBe('one');
		expect(control('one', 'maximize').getAttribute('aria-label')).toBe('Back');

		control('one', 'maximize').click();
		expect(win('one').hasAttribute('data-maximized')).toBe(false);
		expect(control('one', 'maximize').getAttribute('aria-label')).toBe('Maximize');
		expect(onChange).toHaveBeenCalledTimes(2);
	});

	it('maximizes the window the focus is in with F11, and restores it', () => {
		const desk = new Desktop(element(true, { two: 'data-maximizable="false"' }));
		const field = document.getElementById('one-field')!;

		const event = key(field, 'F11');
		expect(event.defaultPrevented).toBe(true);
		expect(desk.state('one')!.maximized).toBe(true);
		key(field, 'F11');
		expect(desk.state('one')!.maximized).toBe(false);

		// a window that does not maximize leaves F11 to the browser
		expect(key(document.getElementById('two-field')!, 'F11').defaultPrevented).toBe(false);
		expect(key(desk.taskbar, 'F11').defaultPrevented).toBe(false);
		expect(control('two', 'maximize')).toBeNull();
	});

	it('minimizes a window to the taskbar, the one behind coming to the front', () => {
		const onChange = vi.fn();
		const desk = new Desktop(element(), { onChange });
		document.getElementById('three-field')!.focus();

		control('three', 'minimize').click();

		expect(win('three').hidden).toBe(true);
		expect(task('three').hasAttribute('data-minimized')).toBe(true);
		expect(desk.activeId()).toBe('two');
		expect(win('two').contains(document.activeElement)).toBe(true);
		expect(onChange).toHaveBeenCalledTimes(1);
	});

	it('brings a window to the front from the taskbar, minimizing the one already there', () => {
		const onActivate = vi.fn();
		const desk = new Desktop(element(), { onActivate });

		task('one').click();
		expect(desk.activeId()).toBe('one');
		expect(onActivate).toHaveBeenCalledWith('one');
		expect(win('one').contains(document.activeElement)).toBe(true);

		task('one').click();
		expect(desk.state('one')!.minimized).toBe(true);
		expect(desk.activeId()).toBe('three');

		task('one').click();
		expect(desk.state('one')!.minimized).toBe(false);
		expect(desk.activeId()).toBe('one');
	});

	it('gives the focus back to where it was in a window brought to the front', () => {
		new Desktop(element());
		const field = document.getElementById('one-field')!;
		field.focus();
		document.getElementById('two-field')!.focus();

		task('one').click();

		expect(document.activeElement).toBe(field);
	});

	it('maximizes the window brought to the front while the one in front is maximized', () => {
		const desk = new Desktop(element(true, { two: 'data-maximizable="false"' }));
		desk.maximize('three');

		task('one').click();
		expect(desk.state('one')).toMatchObject({ maximized: true, minimized: false });
		expect(desk.activeId()).toBe('one');
		// the other one stays maximized behind it
		expect(desk.state('three')!.maximized).toBe(true);

		desk.minimize('two');
		task('two').click();
		expect(desk.state('two')!.maximized).toBe(false);
	});

	it('goes round the windows with Ctrl+F6 and Ctrl+Shift+F6, or Ctrl+Tab', () => {
		const desk = new Desktop(element());
		desk.minimize('one');
		const field = document.getElementById('three-field')!;

		expect(key(field, 'F6', { ctrlKey: true }).defaultPrevented).toBe(true);
		expect(desk.activeId()).toBe('one');
		expect(desk.state('one')!.minimized).toBe(false);
		key(document.activeElement!, 'Tab', { ctrlKey: true });
		expect(desk.activeId()).toBe('two');
		key(document.activeElement!, 'F6', { ctrlKey: true, shiftKey: true });
		expect(desk.activeId()).toBe('one');
		key(document.activeElement!, 'Tab', { ctrlKey: true, shiftKey: true });
		expect(desk.activeId()).toBe('three');

		// without the Ctrl key, Tab moves the focus as always
		expect(key(field, 'Tab').defaultPrevented).toBe(false);
	});

	it('closes a closable window and opens it again', () => {
		const onClose = vi.fn();
		const desk = new Desktop(element(true, { three: 'data-closable="true"' }), { onClose });

		control('three', 'close').click();
		expect(win('three').hidden).toBe(true);
		expect(win('three').hasAttribute('data-closed')).toBe(true);
		expect(task('three').hidden).toBe(true);
		expect(onClose).toHaveBeenCalledWith('three');
		expect(desk.activeId()).toBe('two');

		desk.activate('three');
		expect(desk.state('three')!.closed).toBe(false);
		expect(task('three').hidden).toBe(false);
		expect(desk.activeId()).toBe('three');
	});

	it('tiles the windows shown in a grid, side by side, one above the other, or cascading', () => {
		const onChange = vi.fn();
		const desk = new Desktop(element(), { onChange });
		desk.maximize('two');
		const geometry = (id: string) => {
			const state = desk.state(id)!;
			return [state.x, state.y, state.width, state.height];
		};

		(document.querySelector('.antilia-desktop-tile-grid') as HTMLElement).click();
		expect(geometry('one')).toEqual([0, 0, 500, 300]);
		expect(geometry('two')).toEqual([500, 0, 500, 300]);
		expect(geometry('three')).toEqual([0, 300, 1000, 300]);
		expect(desk.state('two')!.maximized).toBe(false);
		expect(onChange).toHaveBeenCalledTimes(1);

		desk.tile('columns');
		expect(geometry('one')).toEqual([0, 0, 333, 600]);
		expect(geometry('two')).toEqual([333, 0, 334, 600]);
		expect(geometry('three')).toEqual([667, 0, 333, 600]);

		desk.minimize('three');
		desk.tile('rows');
		expect(geometry('one')).toEqual([0, 0, 1000, 300]);
		expect(geometry('two')).toEqual([0, 300, 1000, 300]);

		desk.tile('cascade');
		expect(geometry('one')).toEqual([0, 0, 600, 360]);
		expect(geometry('two')).toEqual([28, 28, 600, 360]);
		expect(onChange).toHaveBeenCalledTimes(1);
	});

	it('follows the windows the page adds and removes, giving one back its state', async () => {
		const desk = new Desktop(element());
		desk.setState('one', { x: 400, y: 300, folded: true });
		const area = desk.area;

		// rendered again, as Wicket does by Ajax
		const again = document.createElement('div');
		again.innerHTML = windowMarkup('one', 'data-x="10" data-y="20"');
		win('one').replaceWith(again.firstElementChild!);
		const added = document.createElement('div');
		added.innerHTML = windowMarkup('four', 'data-x="5" data-y="6"');
		area.append(added.firstElementChild!);
		await settle();

		expect(desk.ids()).toEqual(['one', 'two', 'three', 'four']);
		expect(place('one').slice(0, 2)).toEqual(['400px', '300px']);
		expect(win('one').hasAttribute('data-folded')).toBe(true);
		expect(win('one').querySelector('.antilia-window-controls')).not.toBeNull();
		expect(task('four').textContent).toBe('Title four');
		expect(desk.activeId()).toBe('four');

		win('two').remove();
		await settle();
		expect(desk.ids()).toEqual(['one', 'three', 'four']);
		expect(task('two')).toBeNull();
	});

	it('follows the attributes of a window and its title as the page changes them', async () => {
		const desk = new Desktop(element());

		win('one').setAttribute('data-x', '222');
		win('one').setAttribute('data-closable', 'true');
		win('one').querySelector('.antilia-window-title')!.textContent = 'Renamed';
		await settle();

		expect(desk.state('one')!.x).toBe(222);
		expect(control('one', 'close')).not.toBeNull();
		expect(task('one').textContent).toBe('Renamed');
	});

	it('changes the state of a window the page sets, without calling back', () => {
		const onChange = vi.fn();
		const desk = new Desktop(element(), { onChange });

		desk.setState('two', { order: 99, width: 10 });
		desk.restore('two');
		desk.fold('two');
		desk.close('two');
		desk.open('two');

		expect(desk.activeId()).toBe('two');
		// no smaller than the smallest size
		expect(desk.state('two')!.width).toBe(180);
		expect(onChange).not.toHaveBeenCalled();

		desk.setState('later', { x: 1, y: 2 });
		const added = document.createElement('div');
		added.innerHTML = windowMarkup('later');
		desk.area.append(added.firstElementChild!);
		desk.refresh();
		expect(desk.state('later')).toMatchObject({ x: 1, y: 2 });
	});

	it('takes other texts and options', () => {
		const desk = new Desktop(element());

		desk.configure({ taskbar: 'top', texts: { minimize: 'Kleiner' } });

		expect(desk.element.classList.contains('antilia-desktop-taskbar-top')).toBe(true);
		expect(desk.element.classList.contains('antilia-desktop-taskbar-bottom')).toBe(false);
		expect(control('one', 'minimize').title).toBe('Kleiner');
	});

	it('keeps the windows on the desktop when it shrinks', () => {
		const observers: Array<() => void> = [];
		vi.stubGlobal('ResizeObserver', class {
			constructor(callback: () => void) {
				observers.push(callback);
			}

			observe(): void {
				// called back by hand
			}

			disconnect(): void {
				// nothing to stop
			}
		});
		try {
			const desk = new Desktop(element());
			Object.defineProperty(desk.area, 'clientWidth', { value: 100 });
			observers.forEach((observer) => observer());

			expect(desk.state('two')!.x).toBe(20);
		} finally {
			vi.unstubAllGlobals();
		}
	});

	it('leaves the events of a desktop in a window to it', () => {
		const outer = new Desktop(element());
		const body = win('one').querySelector('.antilia-window-body')!;
		body.innerHTML = '<div id="inner"><div class="antilia-desktop-area">' +
			windowMarkup('nested') + '</div></div>';
		const inner = new Desktop(document.getElementById('inner')!);
		outer.activate('two');

		pointer(win('nested'), 'pointerdown', 1, 1);
		expect(outer.activeId()).toBe('two');
		expect(inner.activeId()).toBe('nested');
	});

	it('stops when an update of the page removes it', () => {
		let removing: ((element: Element) => void) | undefined = undefined;
		const host: Host = { ...standaloneHost, onNodeRemoving: (listener) => {
			removing = listener;
			return () => undefined;
		} };
		setHost(host);
		const desk = new Desktop(element());

		removing!(desk.element);
		pointer(win('one'), 'pointerdown', 1, 1);

		expect(desk.activeId()).toBe('three');
	});

	it('works as a Svelte action', () => {
		const onChange = vi.fn<(states: Record<string, WindowState>) => void>();
		const action = desktop(element(), { onChange });

		action.update({ taskbar: 'right' });
		expect(document.getElementById('d')!.classList.contains('antilia-desktop-taskbar-right'))
			.toBe(true);
		action.destroy();
		expect(Desktop.of('d')).toBeNull();
	});

	it('has a grip moving the taskbar and a button letting the desktop fill the page', () => {
		new Desktop(element());
		const taskbar = document.querySelector('.antilia-desktop-taskbar')!;

		expect(taskbar.firstElementChild!.classList.contains('antilia-desktop-grip')).toBe(true);
		expect(taskbar.lastElementChild!.classList.contains('antilia-desktop-expand')).toBe(true);
		expect(document.querySelector('.antilia-desktop-expand')!.getAttribute('aria-pressed'))
			.toBe('false');
		expect(document.getElementById('d')!.hasAttribute('data-movable-taskbar')).toBe(true);
	});

	it('leaves the grip and the button out when not wanted', () => {
		new Desktop(element(), { movableTaskbar: false, expandable: false });

		expect(document.querySelector('.antilia-desktop-grip')).toBeNull();
		expect(document.querySelector('.antilia-desktop-expand')).toBeNull();
		expect(document.getElementById('d')!.hasAttribute('data-movable-taskbar')).toBe(false);
	});

	it('fills the page with its button and goes back into it, with Escape on the taskbar too',
		() => {
			const onChange = vi.fn<(states: Record<string, WindowState>) => void>();
			const onExpand = vi.fn<(expanded: boolean) => void>();
			const desk = new Desktop(element(), { onChange, onExpand,
				texts: { expand: 'Fill', collapse: 'Back' } });
			const expand = () => document.querySelector<HTMLButtonElement>('.antilia-desktop-expand')!;
			expect(expand().title).toBe('Fill');

			expand().click();
			expect(desk.isExpanded()).toBe(true);
			expect(desk.element.hasAttribute('data-expanded')).toBe(true);
			expect(document.documentElement.classList.contains('antilia-desktop-expanded')).toBe(true);
			expect(expand().title).toBe('Back');
			expect(expand().getAttribute('aria-pressed')).toBe('true');
			expect(onExpand).toHaveBeenLastCalledWith(true);
			expect(onChange).toHaveBeenCalledTimes(1);

			key(win('one'), 'Escape');
			expect(desk.isExpanded()).toBe(true);
			key(expand(), 'Escape');
			expect(desk.isExpanded()).toBe(false);
			expect(desk.element.hasAttribute('data-expanded')).toBe(false);
			expect(document.documentElement.classList.contains('antilia-desktop-expanded')).toBe(false);
			expect(onExpand).toHaveBeenLastCalledWith(false);
		});

	it('starts filling the page as told, and lets the page go when it goes', () => {
		const desk = new Desktop(element(), { expanded: true });
		expect(desk.element.hasAttribute('data-expanded')).toBe(true);
		expect(document.documentElement.classList.contains('antilia-desktop-expanded')).toBe(true);

		desk.destroy();
		expect(desk.element.hasAttribute('data-expanded')).toBe(false);
		expect(document.documentElement.classList.contains('antilia-desktop-expanded')).toBe(false);
	});

	it('moves the taskbar with the arrow keys on its grip, the windows staying on the desktop',
		() => {
			const onChange = vi.fn<(states: Record<string, WindowState>) => void>();
			const onTaskbar = vi.fn<(position: string) => void>();
			const desk = new Desktop(element(), { onChange, onTaskbar });
			const grip = document.querySelector<HTMLButtonElement>('.antilia-desktop-grip')!;
			Object.defineProperty(desk.area, 'clientWidth', { value: 300 });

			const event = key(grip, 'ArrowLeft');

			expect(event.defaultPrevented).toBe(true);
			expect(desk.position()).toBe('left');
			expect(desk.element.classList.contains('antilia-desktop-taskbar-left')).toBe(true);
			expect(desk.taskbar.getAttribute('aria-orientation')).toBe('vertical');
			expect(onTaskbar).toHaveBeenLastCalledWith('left');
			expect(onChange).toHaveBeenCalledTimes(1);
			// as wide as the desktop at most, and wholly on it
			expect(desk.state('two')).toMatchObject({ x: 0, y: 80, width: 300, height: 220 });
			expect(desk.state('one')).toMatchObject({ x: 0, y: 20, width: 300 });
			expect(document.activeElement).toBe(document.querySelector('.antilia-desktop-grip'));

			key(document.querySelector('.antilia-desktop-grip')!, 'ArrowUp');
			expect(desk.position()).toBe('top');
		});

	it('moves the taskbar to the side of the desktop it is dragged to', () => {
		const onTaskbar = vi.fn<(position: string) => void>();
		const desk = new Desktop(element(), { onTaskbar });
		Object.defineProperty(desk.element, 'getBoundingClientRect', { value: () => ({
			left: 0, top: 0, right: 1000, bottom: 640, width: 1000, height: 640, x: 0, y: 0 }) });
		const grip = document.querySelector('.antilia-desktop-grip')!;

		pointer(grip, 'pointerdown', 500, 630);
		pointer(grip, 'pointermove', 502, 628);
		expect(desk.element.dataset.taskbarTarget).toBeUndefined();
		pointer(grip, 'pointermove', 980, 300);
		expect(desk.element.dataset.taskbarTarget).toBe('right');
		pointer(grip, 'pointermove', 500, 10);
		expect(desk.element.dataset.taskbarTarget).toBe('top');
		pointer(grip, 'pointerup', 500, 10);

		expect(desk.position()).toBe('top');
		expect(desk.element.dataset.taskbarTarget).toBeUndefined();
		expect(onTaskbar).toHaveBeenCalledWith('top');

		// the buttons of the taskbar do not drag it
		pointer(task('one'), 'pointerdown', 500, 10);
		pointer(task('one'), 'pointermove', 500, 630);
		pointer(task('one'), 'pointerup', 500, 630);
		expect(desk.position()).toBe('top');
	});

	it('does not move the taskbar when told so', () => {
		const desk = new Desktop(element(), { movableTaskbar: false });
		Object.defineProperty(desk.element, 'getBoundingClientRect', { value: () => ({
			left: 0, top: 0, right: 1000, bottom: 640, width: 1000, height: 640, x: 0, y: 0 }) });

		pointer(desk.taskbar, 'pointerdown', 500, 630);
		pointer(desk.taskbar, 'pointermove', 500, 10);
		pointer(desk.taskbar, 'pointerup', 500, 10);

		expect(desk.position()).toBe('bottom');
	});

	it('fits the windows when it takes less of the page again', () => {
		const desk = new Desktop(element(), { expanded: true });
		desk.setState('two', { x: 900, y: 500 });
		Object.defineProperty(desk.area, 'clientWidth', { value: 800 });
		Object.defineProperty(desk.area, 'clientHeight', { value: 400 });

		desk.expand(false);

		expect(desk.state('two')).toMatchObject({ x: 480, y: 180, width: 320, height: 220 });
	});

	it('lists the windows open and those closed in the menu of the taskbar', () => {
		const desk = new Desktop(element(false, { three: 'data-closed' }));
		const menuButton = desk.taskbar.querySelector<HTMLButtonElement>(
			'.antilia-desktop-menu-button')!;
		expect(menuButton.previousElementSibling?.className).toBe('antilia-desktop-grip');
		expect(menuButton.getAttribute('aria-haspopup')).toBe('menu');
		expect(menuButton.title).toBe('All windows');

		menuButton.click();

		const menu = desk.element.querySelector('.antilia-desktop-menu')!;
		expect(menuButton.getAttribute('aria-expanded')).toBe('true');
		expect(menuButton.getAttribute('aria-controls')).toBe(menu.id);
		const groups = Array.from(menu.querySelectorAll('[role="group"]')).map((group) =>
			[group.querySelector('.antilia-desktop-menu-group-heading')!.textContent,
				...Array.from(group.querySelectorAll('[role="menuitem"]'))
					.map((item) => item.textContent)]);
		expect(groups).toEqual([['Open', 'Title one', 'Title two'], ['Closed', 'Title three']]);
		expect(document.activeElement?.textContent).toBe('Title one');

		key(document.activeElement!, 'ArrowDown');
		expect(document.activeElement?.textContent).toBe('Title two');
		key(document.activeElement!, 'ArrowDown');
		expect(document.activeElement?.textContent).toBe('Title three');
		key(document.activeElement!, 'ArrowDown');
		expect(document.activeElement?.textContent).toBe('Title one');
		key(document.activeElement!, 'End');
		expect(document.activeElement?.textContent).toBe('Title three');

		(document.activeElement as HTMLElement).click();

		expect(desk.isMenuOpen()).toBe(false);
		expect(desk.element.querySelector('.antilia-desktop-menu')).toBeNull();
		expect(desk.state('three')).toMatchObject({ closed: false });
		expect(desk.activeId()).toBe('three');
		expect(win('three').contains(document.activeElement)).toBe(true);
	});

	it('closes its menu with Escape, giving its button the focus, and with a press outside', () => {
		const desk = new Desktop(element());
		const menuButton = desk.taskbar.querySelector<HTMLButtonElement>(
			'.antilia-desktop-menu-button')!;
		desk.expand(true);

		key(menuButton, 'ArrowDown');
		expect(desk.isMenuOpen()).toBe(true);
		key(document.activeElement!, 'Escape');

		expect(desk.isMenuOpen()).toBe(false);
		expect(document.activeElement).toBe(menuButton);
		// Escape was the menu's: the desktop still fills the page
		expect(desk.isExpanded()).toBe(true);

		desk.openMenu();
		pointer(win('two'), 'pointerdown', 200, 200);
		expect(desk.isMenuOpen()).toBe(false);

		desk.openMenu();
		desk.moveTaskbar('left');
		expect(desk.isMenuOpen()).toBe(false);
		expect(menuButton.getAttribute('aria-expanded')).toBe('false');
	});

	it('leaves the menu out when not wanted', () => {
		const desk = new Desktop(element(), { menu: false });
		expect(desk.taskbar.querySelector('.antilia-desktop-menu-button')).toBeNull();
		desk.configure({ menu: true });
		expect(desk.taskbar.querySelector('.antilia-desktop-menu-button')).not.toBeNull();
	});

	it('closes a window without changes at once', async () => {
		const onClose = vi.fn();
		const desk = new Desktop(element(true, { one: 'data-closable="true"' }), { onClose });

		control('one', 'close').click();

		expect(desk.state('one')?.closed).toBe(true);
		expect(onClose).toHaveBeenCalledWith('one');
		expect(document.querySelector('.antilia-desktop-confirm')).toBeNull();
	});

	it('asks before closing a window with changes, and puts its fields back', async () => {
		const onClose = vi.fn();
		const desk = new Desktop(element(true, { one: 'data-closable="true"' }), { onClose });
		const field = document.getElementById('one-field') as HTMLInputElement;
		const changed = vi.fn();
		field.addEventListener('change', changed);
		const discarded = vi.fn();
		win('one').addEventListener('antilia-desktop:discard', discarded);
		field.value = 'typed';
		expect(desk.isModified('one')).toBe(true);
		expect(desk.isModified('two')).toBe(false);

		control('one', 'close').click();

		const confirm = document.querySelector('.antilia-desktop-confirm')!;
		expect(confirm.classList.contains('dialog-theme-default')).toBe(true);
		const dialog = confirm.querySelector('.modal-dialog')!;
		expect(dialog.getAttribute('role')).toBe('alertdialog');
		expect(dialog.querySelector('.modal-dialog-title')?.textContent).toBe('Close Title one?');
		expect(dialog.querySelector('.modal-dialog-body')?.textContent)
			.toBe('The changes in Title one are not saved. Close the window and lose them?');
		const keep = dialog.querySelector<HTMLButtonElement>('[data-confirm="keep"]')!;
		const discard = dialog.querySelector<HTMLButtonElement>('[data-confirm="discard"]')!;
		expect(keep.textContent).toBe('Keep it open');
		expect(discard.textContent).toBe('Close and lose them');
		expect(discard.className).toContain('ant-button--danger');
		expect(desk.state('one')?.closed).toBe(false);

		keep.click();
		await settle();

		expect(document.querySelector('.antilia-desktop-confirm')).toBeNull();
		expect(desk.state('one')?.closed).toBe(false);
		expect(field.value).toBe('typed');
		expect(onClose).not.toHaveBeenCalled();

		control('one', 'close').click();
		document.querySelector<HTMLButtonElement>('[data-confirm="discard"]')!.click();
		await settle();

		expect(document.querySelector('.antilia-desktop-confirm')).toBeNull();
		expect(desk.state('one')?.closed).toBe(true);
		expect(onClose).toHaveBeenCalledWith('one');
		expect(field.value).toBe('');
		expect(changed).toHaveBeenCalledTimes(1);
		expect(discarded).toHaveBeenCalledTimes(1);
		expect((discarded.mock.calls[0][0] as CustomEvent).detail).toEqual({ id: 'one' });
		expect(desk.isModified('one')).toBe(false);
	});

	it('asks in the look of the page around the desktop', async () => {
		document.body.setAttribute('data-antilia-look', 'bootstrap');
		const desk = new Desktop(element(true, { one: 'data-closable="true"' }));
		(document.getElementById('one-field') as HTMLInputElement).value = 'typed';

		const closing = desk.requestClose('one');

		expect(document.querySelector('.antilia-desktop-confirm .modal-dialog-header')!.className)
			.toBe('modal-dialog-header modal-header');
		expect(document.querySelector('.antilia-desktop-confirm .modal-dialog')!.className)
			.toContain('modal-dialog-small modal-sm');
		expect(document.querySelector('[data-confirm="keep"]')!.className)
			.toBe('btn btn-secondary btn-sm');
		expect(document.querySelector('[data-confirm="discard"]')!.className)
			.toBe('btn btn-danger btn-sm');
		document.querySelector<HTMLButtonElement>('[data-confirm="keep"]')!.click();
		await expect(closing).resolves.toBe(false);
		document.body.removeAttribute('data-antilia-look');
	});

	it('keeps a window with changes open on Escape in the dialog', async () => {
		const desk = new Desktop(element(true, { one: 'data-closable="true"' }));
		(document.getElementById('one-field') as HTMLInputElement).value = 'typed';

		const closing = desk.requestClose('one');
		key(document.querySelector('.antilia-desktop-confirm .modal-dialog')!, 'Escape');

		await expect(closing).resolves.toBe(false);
		expect(desk.state('one')?.closed).toBe(false);
		expect(document.querySelector('.antilia-desktop-confirm')).toBeNull();
	});

	it('takes whether a window has changes from data-modified', () => {
		const desk = new Desktop(element(true, { one: 'data-modified', two: 'data-modified="false"',
			three: 'data-modified="true"' }));
		expect(desk.isModified('one')).toBe(true);
		expect(desk.isModified('three')).toBe(true);
		(document.getElementById('two-field') as HTMLInputElement).value = 'typed';
		expect(desk.isModified('two')).toBe(false);

		win('two').querySelector('.antilia-window-body')!.insertAdjacentHTML('beforeend',
			'<div data-modified="auto"><textarea id="note"></textarea></div>');
		expect(desk.isModified('two')).toBe(false);
		(document.getElementById('note') as HTMLTextAreaElement).value = 'a note';
		expect(desk.isModified('two')).toBe(true);
	});

	it('asks the page instead, when told so', async () => {
		const confirmClose = vi.fn(() => Promise.resolve(true));
		const desk = new Desktop(element(true, { one: 'data-modified' }), { confirmClose });

		await expect(desk.requestClose('one')).resolves.toBe(true);

		expect(confirmClose).toHaveBeenCalledWith('one', 'Title one');
		expect(document.querySelector('.antilia-desktop-confirm')).toBeNull();
		expect(desk.state('one')?.closed).toBe(true);
	});
});

describe('Desktop panes', () => {
	const PANES = [{ id: '0', name: 'Grids' }, { id: '1', name: 'Dropdowns' }];

	function panes(): string[] {
		return Array.from(document.querySelectorAll<HTMLElement>('.antilia-desktop-pane'))
			.map((pane) => pane.textContent + (pane.getAttribute('aria-pressed') === 'true' ? '*' :
				''));
	}

	function paneButton(id: string): HTMLButtonElement {
		return document.querySelector('.antilia-desktop-pane[data-pane="' + id + '"]')!;
	}

	function paneAction(name: string): HTMLButtonElement | null {
		return document.querySelector('.antilia-desktop-pane-action-' + name);
	}

	function shownTasks(): string[] {
		return Array.from(document.querySelectorAll<HTMLElement>('.antilia-desktop-task'))
			.filter((task) => !task.hidden).map((task) => task.dataset.window!);
	}

	/** Types a name in the dialog asking for one and confirms it with Enter. */
	function answer(name: string): void {
		const input = document.querySelector<HTMLInputElement>('.antilia-desktop-prompt input')!;
		expect(document.activeElement).toBe(input);
		input.value = name;
		key(input, 'Enter');
	}

	it('shows the windows of the pane shown only, and their buttons in the taskbar', () => {
		const desk = new Desktop(element(true, { two: 'data-pane="1"' }), { panes: PANES });

		expect(panes()).toEqual(['Grids*', 'Dropdowns']);
		expect(desk.activePane()).toBe('0');
		expect(desk.state('one')?.pane).toBe('0');
		expect(desk.state('two')?.pane).toBe('1');
		expect(win('two').hidden).toBe(true);
		expect(shownTasks()).toEqual(['one', 'three']);
		expect(desk.activeId()).toBe('three');

		paneButton('1').click();

		expect(panes()).toEqual(['Grids', 'Dropdowns*']);
		expect(win('one').hidden).toBe(true);
		expect(win('three').hidden).toBe(true);
		expect(win('two').hidden).toBe(false);
		expect(shownTasks()).toEqual(['two']);
		expect(desk.activeId()).toBe('two');
	});

	it('names the default pane as the texts do, and adds it when missing', () => {
		const desk = new Desktop(element(), { panes: [{ id: 'x', name: 'Other' }] });

		expect(desk.panes()).toEqual([{ id: '0', name: '' }, { id: 'x', name: 'Other' }]);
		expect(panes()).toEqual(['Main*', 'Other']);
		// the default pane cannot be removed: no button for it
		expect(paneAction('remove')).toBeNull();
		expect(desk.removePane('0')).toBe(false);
	});

	it('puts a window in a pane there is not in the default one', () => {
		const desk = new Desktop(element(true, { one: 'data-pane="9"' }), { panes: PANES,
			activePane: '1' });

		expect(desk.state('one')?.pane).toBe('0');
		// without data-pane, in the pane shown
		expect(desk.state('two')?.pane).toBe('1');
	});

	it('goes from pane to pane with Ctrl+Shift and the arrow keys, not while writing', () => {
		const onPanes = vi.fn();
		const onChange = vi.fn();
		const desk = new Desktop(element(true, { two: 'data-pane="1"' }), { panes: PANES, onPanes,
			onChange });
		win('one').focus();

		key(win('one'), 'ArrowRight', { ctrlKey: true, shiftKey: true });

		expect(desk.activePane()).toBe('1');
		expect(onPanes).toHaveBeenCalledWith(PANES, '1');
		expect(onChange).toHaveBeenCalled();
		// the window in front of the pane takes the focus
		expect(document.activeElement).toBe(win('two'));

		key(win('two'), 'ArrowRight', { ctrlKey: true, shiftKey: true });
		expect(desk.activePane()).toBe('0');
		key(win('three'), 'ArrowUp', { ctrlKey: true, shiftKey: true });
		expect(desk.activePane()).toBe('1');

		const field = document.getElementById('two-field')!;
		const event = key(field, 'ArrowLeft', { ctrlKey: true, shiftKey: true });
		expect(desk.activePane()).toBe('1');
		expect(event.defaultPrevented).toBe(false);
	});

	it('moves a window to another pane from the menu of its title bar', () => {
		const onChange = vi.fn();
		const desk = new Desktop(element(), { panes: PANES, onChange });
		const move = control('three', 'pane');
		expect(move.getAttribute('aria-haspopup')).toBe('menu');

		move.click();

		const items = Array.from(document.querySelectorAll<HTMLElement>(
			'.antilia-desktop-menu-item')).map((item) => item.textContent);
		expect(items).toEqual(['Dropdowns', 'New pane…']);
		expect(move.getAttribute('aria-expanded')).toBe('true');
		(document.activeElement as HTMLElement).click();

		expect(desk.state('three')?.pane).toBe('1');
		expect(win('three').getAttribute('data-pane')).toBe('1');
		expect(win('three').hidden).toBe(true);
		expect(desk.activePane()).toBe('0');
		expect(desk.activeId()).toBe('two');
		expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
			three: expect.objectContaining({ pane: '1' }) }));
	});

	it('moves a window to a new pane, asking for its name', async () => {
		const desk = new Desktop(element(), { panes: PANES });

		control('one', 'pane').click();
		(document.querySelector('.antilia-desktop-menu-item[data-pane=""]') as HTMLElement).click();
		const dialog = document.querySelector('.antilia-desktop-prompt [role="dialog"]')!;
		expect(dialog.querySelector('h1')?.textContent).toBe('New pane');
		expect((document.activeElement as HTMLInputElement).value).toBe('Pane 3');
		answer('Charts');
		await settle();

		expect(desk.panes()).toEqual([...PANES, { id: '2', name: 'Charts' }]);
		expect(desk.state('one')?.pane).toBe('2');
		expect(desk.activePane()).toBe('0');
		expect(document.querySelector('.antilia-desktop-prompt')).toBeNull();
	});

	it('adds a pane and shows it, renames it and removes it from the taskbar', async () => {
		const onPanes = vi.fn();
		const desk = new Desktop(element(), { onPanes });
		expect(panes()).toEqual(['Main*']);

		paneAction('add')!.click();
		answer('Second');
		await settle();

		expect(panes()).toEqual(['Main', 'Second*']);
		expect(onPanes).toHaveBeenLastCalledWith([{ id: '0', name: '' }, { id: '1',
			name: 'Second' }], '1');
		expect(desk.activeId()).toBeNull();
		desk.moveToPane('one', '1');

		paneAction('rename')!.click();
		expect(document.querySelector('.antilia-desktop-prompt h1')?.textContent)
			.toBe('Rename Second');
		answer('  Later  ');
		await settle();
		expect(panes()).toEqual(['Main', 'Later*']);

		paneAction('remove')!.click();

		expect(panes()).toEqual(['Main*']);
		expect(desk.state('one')?.pane).toBe('0');
		expect(win('one').hidden).toBe(false);
		expect(onPanes).toHaveBeenLastCalledWith([{ id: '0', name: '' }], '0');
	});

	it('renames a pane with F2 or a double click on its button, removes it with Delete',
		async () => {
			const desk = new Desktop(element(), { panes: PANES });

			key(paneButton('0'), 'F2');
			answer('Tables');
			await settle();
			expect(desk.panes()[0]).toEqual({ id: '0', name: 'Tables' });

			paneButton('1').dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
			document.querySelector<HTMLElement>('.antilia-desktop-prompt [data-confirm="cancel"]')!
				.click();
			await settle();
			expect(desk.panes()[1].name).toBe('Dropdowns');

			key(paneButton('1'), 'Delete');
			expect(desk.panes()).toEqual([{ id: '0', name: 'Tables' }]);
		});

	it('leaves the name of a pane as it was when the new one is empty', async () => {
		const desk = new Desktop(element(), { panes: PANES });

		void desk.requestRenamePane('1');
		answer('   ');
		await settle();

		expect(desk.panes()[1].name).toBe('Dropdowns');
	});

	it('does not let the user change the panes when told so', () => {
		const desk = new Desktop(element(), { panes: PANES, editablePanes: false });

		expect(paneAction('add')).toBeNull();
		expect(paneAction('rename')).toBeNull();
		key(paneButton('1'), 'Delete');
		expect(desk.panes()).toHaveLength(2);
		control('one', 'pane').click();
		expect(Array.from(document.querySelectorAll('.antilia-desktop-menu-item'))
			.map((item) => item.textContent)).toEqual(['Dropdowns']);
	});

	it('leaves the panes out of the taskbar and the title bars when told so', () => {
		const desk = new Desktop(element(), { paneSwitcher: false });

		expect(document.querySelector('.antilia-desktop-panes')).toBeNull();
		expect(control('one', 'pane')).toBeNull();
		desk.configure({ paneSwitcher: true });
		expect(document.querySelector('.antilia-desktop-panes')).not.toBeNull();
		expect(control('one', 'pane')).not.toBeNull();
	});

	it('shows the pane of a window brought to the front from elsewhere', () => {
		const desk = new Desktop(element(true, { two: 'data-pane="1"' }), { panes: PANES });

		desk.activate('two', true);

		expect(desk.activePane()).toBe('1');
		expect(desk.activeId()).toBe('two');
		expect(win('two').hidden).toBe(false);
	});

	it('lists the windows of the other panes in the menu of the taskbar', () => {
		const desk = new Desktop(element(true, { one: 'data-pane="0"', two: 'data-pane="1"',
			three: 'data-closed' }),
			{ panes: PANES, activePane: '1' });

		desk.openMenu();

		const groups = Array.from(document.querySelectorAll('.antilia-desktop-menu [role="group"]')).map((group) =>
			[group.querySelector('.antilia-desktop-menu-group-heading')!.textContent,
				...Array.from(group.querySelectorAll('[role="menuitem"]'))
					.map((item) => item.textContent)]);
		expect(groups).toEqual([['Open', 'Title two'], ['Grids', 'Title one'],
			['Closed', 'Title three']]);

		// a closed window opens in the pane shown
		(document.querySelector('[data-group="closed"] [role="menuitem"]') as HTMLElement).click();
		expect(desk.state('three')).toMatchObject({ closed: false, pane: '1' });
		expect(desk.activePane()).toBe('1');

		desk.openMenu();
		(document.querySelector('[data-group="pane"] [role="menuitem"]') as HTMLElement).click();
		expect(desk.activePane()).toBe('0');
		expect(desk.activeId()).toBe('one');
	});

	it('tiles and goes round the windows of the pane shown only', () => {
		const desk = new Desktop(element(true, { two: 'data-pane="1"' }), { panes: PANES });

		desk.tile('columns');
		expect(place('one').slice(0, 4)).toEqual(['0px', '0px', '500px', '600px']);
		expect(place('three').slice(0, 4)).toEqual(['500px', '0px', '500px', '600px']);
		expect(place('two').slice(0, 4)).toEqual(['100px', '80px', '320px', '220px']);

		desk.cycle(1);
		expect(desk.activeId()).toBe('one');
		desk.cycle(1);
		expect(desk.activeId()).toBe('three');
	});

	it('follows the pane the page gives a window', async () => {
		const desk = new Desktop(element(), { panes: PANES });

		win('one').setAttribute('data-pane', '1');
		await settle();

		expect(desk.state('one')?.pane).toBe('1');
		expect(win('one').hidden).toBe(true);
	});

	it('takes other panes, its windows in panes gone going to the default one', () => {
		const desk = new Desktop(element(true, { two: 'data-pane="1"' }), { panes: PANES,
			activePane: '1' });

		desk.configure({ panes: [{ id: '0', name: 'Only' }] });

		expect(desk.activePane()).toBe('0');
		expect(desk.state('two')?.pane).toBe('0');
		expect(panes()).toEqual(['Only*']);
	});
});

describe('Desktop window types', () => {
	function notes(id: string): HTMLElement {
		const section = document.createElement('section');
		section.innerHTML = '<header class="antilia-window-header"><span class="antilia-window-title">' +
			'Notes</span></header><div class="antilia-window-body"><textarea></textarea></div>';
		section.dataset.made = id;
		return section;
	}

	it('opens a window of a type from the launcher, in the pane shown, and takes it away closed',
		() => {
			const onChange = vi.fn();
			const desk = new Desktop(element(), { panes: [{ id: '0', name: '' },
				{ id: '1', name: 'Other' }], activePane: '1', onChange,
			windowTypes: [{ type: 'notes', title: 'Notes', icon: '<svg class="n"></svg>',
				create: notes }] });
			const launcher = desk.taskbar.querySelector<HTMLButtonElement>(
				'.antilia-desktop-launcher')!;
			expect(launcher.previousElementSibling?.className).toBe('antilia-desktop-menu-button');
			expect(launcher.title).toBe('Open a window');

			key(launcher, 'ArrowDown');
			const item = document.activeElement as HTMLElement;
			expect(item.textContent).toBe('Notes');
			expect(item.querySelector('svg.n')).not.toBeNull();
			item.click();

			const id = 'd-notes-1';
			expect(desk.ids()).toContain(id);
			expect(win(id).dataset.made).toBe(id);
			expect(desk.state(id)).toMatchObject({ pane: '1', closed: false });
			expect(desk.activeId()).toBe(id);
			expect(onChange).toHaveBeenCalled();

			control(id, 'close').click();

			expect(document.getElementById(id)).toBeNull();
			expect(desk.ids()).not.toContain(id);
		});

	it('asks the page for a window of a type it does not make', () => {
		const onOpenWindow = vi.fn();
		const desk = new Desktop(element(), { onOpenWindow });
		expect(desk.taskbar.querySelector('.antilia-desktop-launcher')).toBeNull();

		desk.registerWindowType({ type: 'chart', title: 'Chart' });
		desk.taskbar.querySelector<HTMLButtonElement>('.antilia-desktop-launcher')!.click();
		(document.activeElement as HTMLElement).click();

		expect(onOpenWindow).toHaveBeenCalledWith('chart', '0');
		expect(desk.openWindow('none')).toBeNull();

		desk.unregisterWindowType('chart');
		expect(desk.taskbar.querySelector('.antilia-desktop-launcher')).toBeNull();
	});
});
