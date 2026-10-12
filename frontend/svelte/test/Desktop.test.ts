/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, fireEvent, render, waitFor } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { activate } from '../src/index.js';
import BoundDesktop from './BoundDesktop.svelte';
import PanedDesktop from './PanedDesktop.svelte';
// the key the demo application installs, which activates every private component
import demoKey from '../../../demo-app/src/main/resources/META-INF/antilia-widgets.license?raw';

function win(container: HTMLElement, id: string): HTMLElement {
	return container.querySelector('#' + id)!;
}

function tasks(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('.antilia-desktop-task'),
		(task) => task.textContent ?? '');
}

function settle(): Promise<void> {
	return new Promise((done) => setTimeout(done, 0));
}

/**
 * Renders the desktop once its license key is checked.
 */
async function shown(props: Record<string, unknown> = {}) {
	const result = render(BoundDesktop, props);
	await waitFor(() => expect(result.container.querySelector('.antilia-desktop')).not.toBeNull());
	flushSync();
	return result;
}

beforeAll(async () => {
	await activate(demoKey);
});

afterEach(cleanup);

describe('Desktop', () => {
	it('shows the placeholder of a private component without a license key', async () => {
		const { container } = render(BoundDesktop, { licenseKey: 'AW1.not.valid' });

		await waitFor(() => expect(container.querySelector('.antilia-unlicensed')?.textContent?.trim())
			.toBe('Needs a key'));
		expect(container.querySelector('#w0')).toBeNull();
	});

	it('renders the windows, which the core places, with their buttons and a taskbar', async () => {
		const { container } = await shown();

		const desktop = container.querySelector('#d')!;
		expect(desktop.className).toBe('antilia-desktop antilia-desktop-taskbar-bottom tall');
		expect(win(container, 'w1').style.getPropertyValue('--antilia-window-x')).toBe('10px');
		expect(win(container, 'w0').querySelector('.antilia-window-body')?.textContent?.trim())
			.toBe('Body 0');
		expect(win(container, 'w1').querySelector('.antilia-window-control-close')).not.toBeNull();
		expect(win(container, 'w0').querySelector('.antilia-window-control-close')).toBeNull();
		expect(win(container, 'w1').hasAttribute('data-active')).toBe(true);
		expect(tasks(container)).toEqual(['Window 0', 'Window 1']);
		expect(container.querySelector('.antilia-desktop-tile-grid')?.getAttribute('title'))
			.toBe('Grid');
	});

	it('calls back when the user changes the windows', async () => {
		const onchange = vi.fn();
		const onactivate = vi.fn();
		const { container } = await shown({ onchange, onactivate });

		fireEvent.click(container.querySelector('.antilia-desktop-task[data-window="w0"]')!);
		expect(onactivate).toHaveBeenCalledWith('w0');
		expect(onchange).toHaveBeenCalledTimes(1);
		expect(onchange.mock.calls[0][0].w0.order).toBe(2);

		fireEvent.click(win(container, 'w1').querySelector('.antilia-window-control-close')!);
		expect(win(container, 'w1').hidden).toBe(true);
	});

	it('follows the windows Svelte adds and removes, and takes other options', async () => {
		const { container, rerender } = await shown({ count: 1 });

		await rerender({ count: 3, taskbar: 'left' });
		flushSync();
		await settle();

		expect(tasks(container)).toEqual(['Window 0', 'Window 1', 'Window 2']);
		expect(win(container, 'w2').hidden).toBe(true);
		expect(container.querySelector('.antilia-desktop')!.classList
			.contains('antilia-desktop-taskbar-left')).toBe(true);
	});

	it('changes the windows through the functions it exports', async () => {
		let desktop: { states(): Record<string, { minimized: boolean; folded: boolean }> } &
			Record<string, (...args: unknown[]) => void> | undefined = undefined;
		const { container } = await shown({ count: 3, get desktop() {
			return desktop;
		}, set desktop(value) {
			desktop = value as typeof desktop;
		} } as never);
		const handle = desktop!;

		expect(handle.desktop()).not.toBeNull();
		handle.activate('w2');
		expect(handle.states().w2.minimized).toBe(false);
		handle.fold('w2');
		expect(win(container, 'w2').hasAttribute('data-folded')).toBe(true);
		handle.maximize('w2');
		handle.restore('w2');
		handle.minimize('w2');
		handle.close('w1');
		handle.open('w1');
		handle.cycle(-1);
		handle.tile('columns');
		expect(handle.states().w2.minimized).toBe(true);
		handle.expand();
		expect(container.querySelector('#d')!.hasAttribute('data-expanded')).toBe(true);
		handle.expand(false);
		expect(container.querySelector('#d')!.hasAttribute('data-expanded')).toBe(false);
	});

	it('keeps the side the user moved the taskbar to, and whether it fills the page', async () => {
		const ontaskbar = vi.fn();
		const onexpand = vi.fn();
		const { container, rerender } = await shown({ ontaskbar, onexpand });
		const desktop = container.querySelector('#d')!;

		await fireEvent.keyDown(container.querySelector('.antilia-desktop-grip')!,
			{ key: 'ArrowLeft' });
		await fireEvent.click(container.querySelector('.antilia-desktop-expand')!);
		flushSync();

		expect(ontaskbar).toHaveBeenCalledWith('left');
		expect(onexpand).toHaveBeenCalledWith(true);
		expect(desktop.classList.contains('antilia-desktop-taskbar-left')).toBe(true);
		await rerender({ count: 3 });
		flushSync();
		expect(desktop.classList.contains('antilia-desktop-taskbar-left')).toBe(true);
		expect(desktop.hasAttribute('data-expanded')).toBe(true);
	});

	it('asks before closing a window with changes, the page or the dialog of the desktop', async () => {
		const confirmClose = vi.fn(() => false);
		const { container, rerender } = await shown({ modified: true, confirmClose });
		expect(win(container, 'w1').getAttribute('data-modified')).toBe('true');

		await fireEvent.click(win(container, 'w1').querySelector('.antilia-window-control-close')!);
		await settle();
		expect(confirmClose).toHaveBeenCalledWith('w1', 'Window 1');
		expect(win(container, 'w1').hidden).toBe(false);

		await rerender({ modified: true, confirmClose: undefined });
		flushSync();
		await fireEvent.click(win(container, 'w1').querySelector('.antilia-window-control-close')!);
		await settle();
		const discard = document.querySelector<HTMLButtonElement>(
			'.antilia-desktop-confirm [data-confirm="discard"]')!;
		await fireEvent.click(discard);
		await settle();
		expect(win(container, 'w1').hidden).toBe(true);
		expect(document.querySelector('.antilia-desktop-confirm')).toBeNull();
	});

	it('has the menu listing the windows, unless told otherwise', async () => {
		const { container, rerender } = await shown();

		await fireEvent.click(container.querySelector('.antilia-desktop-menu-button')!);
		expect(Array.from(container.querySelectorAll('.antilia-desktop-menu-item'),
			(item) => item.textContent)).toEqual(['Window 0', 'Window 1']);

		await rerender({ menu: false });
		flushSync();
		expect(container.querySelector('.antilia-desktop-menu-button')).toBeNull();
	});
});

describe('Desktop panes and window types', () => {
	async function paned(props: Record<string, unknown> = {}) {
		const result = render(PanedDesktop, props);
		await waitFor(() => expect(result.container.querySelector('.antilia-desktop')).not
			.toBeNull());
		flushSync();
		return result;
	}

	it('shows the windows of the pane shown, following the user and the props', async () => {
		const onpanes = vi.fn();
		const { container, rerender } = await paned({ onpanes });

		expect(win(container, 'two').hidden).toBe(true);
		const buttons = Array.from(container.querySelectorAll<HTMLElement>('.antilia-desktop-pane'));
		expect(buttons.map((button) => button.textContent)).toEqual(['Grids', 'Dropdowns']);

		buttons[1].click();
		flushSync();

		expect(onpanes).toHaveBeenCalledWith([{ id: '0', name: 'Grids' },
			{ id: '1', name: 'Dropdowns' }], '1');
		expect(win(container, 'two').hidden).toBe(false);
		expect(win(container, 'one').hidden).toBe(true);

		await rerender({ onpanes, activePane: '0' });
		flushSync();
		expect(win(container, 'one').hidden).toBe(false);
		expect(win(container, 'two').hidden).toBe(true);
	});

	it('renders the windows of a type the user opens, and takes them away closed', async () => {
		const onopenwindow = vi.fn();
		const { container } = await paned({ onopenwindow });

		container.querySelector<HTMLElement>('.antilia-desktop-launcher')!.click();
		const item = container.querySelector<HTMLElement>('.antilia-desktop-menu-item')!;
		expect(item.textContent).toBe('Notes');
		item.click();
		flushSync();

		expect(onopenwindow).toHaveBeenCalledWith('notes', '0');
		const opened = await waitFor(() => {
			const element = container.querySelector<HTMLElement>('#d-notes-1');
			expect(element).not.toBeNull();
			return element!;
		});
		expect(opened.querySelector('.antilia-window-body')?.textContent?.trim())
			.toBe('Notes of d-notes-1');
		expect(opened.querySelector('.antilia-window-icon svg.notes')).not.toBeNull();
		expect(opened.getAttribute('data-width')).toBe('300');
		expect(opened.getAttribute('data-pane')).toBe('0');
		await waitFor(() => expect(tasks(container)).toEqual(['One', 'Two', 'Notes']));

		opened.querySelector<HTMLElement>('.antilia-window-control-close')!.click();
		flushSync();

		await waitFor(() => expect(container.querySelector('#d-notes-1')).toBeNull());
		await settle();
		expect(tasks(container)).toEqual(['One', 'Two']);
	});
});

