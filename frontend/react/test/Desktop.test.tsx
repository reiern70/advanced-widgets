/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { createRef, useState, type ReactElement } from 'react';
import { act, cleanup, fireEvent, render, waitFor } from '@testing-library/react';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { activate } from '@antilia/core';

import { Desktop, DesktopWindow, type DesktopHandle } from '../src/index.js';
// the key the demo application installs, which activates every private component
import demoKey from '../../../demo-app/src/main/resources/META-INF/antilia-widgets.license?raw';

function win(container: HTMLElement, id: string): HTMLElement {
	return container.querySelector('#' + id)!;
}

function tasks(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('.antilia-desktop-task'),
		(task) => task.textContent ?? '');
}

/**
 * Renders a desktop once its license key is checked.
 */
async function shown(ui: ReactElement): Promise<ReturnType<typeof render>> {
	const result = render(ui);
	await waitFor(() => expect(result.container.querySelector('.antilia-desktop')).not.toBeNull());
	return result;
}

beforeAll(async () => {
	await activate(demoKey);
});

afterEach(cleanup);

describe('Desktop', () => {
	it('shows the placeholder of a private component without a license key', async () => {
		const { container } = render(<Desktop licenseKey="AW1.not.valid"
			texts={{ unlicensed: 'Needs a key' }}>
			<DesktopWindow id="one" title="One" />
		</Desktop>);

		await waitFor(() => expect(container.querySelector('.antilia-unlicensed')?.textContent)
			.toBe('Needs a key'));
		expect(container.querySelector('#one')).toBeNull();
	});

	it('renders the windows, which the core places, with their buttons and a taskbar', async () => {
		const { container } = await shown(<Desktop id="d" className="tall">
			<DesktopWindow id="one" title="One" x={10} y={20} width={300} height={200}
				icon={<svg />}>
				<p>First</p>
			</DesktopWindow>
			<DesktopWindow id="two" title="Two" closable resizable={false}>
				<p>Second</p>
			</DesktopWindow>
		</Desktop>);

		const desktop = container.querySelector('#d')!;
		expect(desktop.className).toBe('antilia-desktop antilia-desktop-taskbar-bottom tall');
		expect(win(container, 'one').style.getPropertyValue('--antilia-window-x')).toBe('10px');
		expect(win(container, 'one').style.getPropertyValue('--antilia-window-width'))
			.toBe('300px');
		expect(win(container, 'one').querySelector('.antilia-window-body')?.textContent)
			.toBe('First');
		expect(win(container, 'two').querySelector('.antilia-window-control-close')).not.toBeNull();
		expect(win(container, 'two').querySelectorAll('.antilia-window-handle')).toHaveLength(0);
		expect(win(container, 'two').hasAttribute('data-active')).toBe(true);
		expect(tasks(container)).toEqual(['One', 'Two']);
		expect(container.querySelector('.antilia-desktop-task-icon svg')).not.toBeNull();
	});

	it('calls back with the latest callbacks when the user changes the windows', async () => {
		const first = vi.fn();
		const onChange = vi.fn();
		const onActivate = vi.fn();
		const markup = (callback: typeof first) => <Desktop onChange={callback}
			onActivate={onActivate}>
			<DesktopWindow id="one" title="One" />
			<DesktopWindow id="two" title="Two" />
		</Desktop>;
		const { container, rerender } = await shown(markup(first));
		rerender(markup(onChange));

		fireEvent.click(container.querySelector('.antilia-window-control-maximize')!);

		expect(first).not.toHaveBeenCalled();
		expect(onChange).toHaveBeenCalledTimes(1);
		expect(onChange.mock.calls[0][0].one).toMatchObject({ maximized: true, order: 2 });
		expect(win(container, 'one').hasAttribute('data-maximized')).toBe(true);

		fireEvent.click(container.querySelector('.antilia-desktop-task[data-window="two"]')!);
		expect(onActivate).toHaveBeenCalledWith('two');
		// two takes the place of the maximized window, maximized too
		expect(win(container, 'two').hasAttribute('data-maximized')).toBe(true);
	});

	it('follows the windows React adds and removes, and takes other options', async () => {
		function Shown({ count, taskbar }: { count: number; taskbar: 'bottom' | 'left' }) {
			return <Desktop taskbar={taskbar} texts={{ grid: 'Grid' }}>
				{Array.from({ length: count }, (_, index) =>
					<DesktopWindow key={index} id={'w' + index} title={'Window ' + index} />)}
			</Desktop>;
		}
		const { container, rerender } = await shown(<Shown count={1} taskbar="bottom" />);
		expect(tasks(container)).toEqual(['Window 0']);

		rerender(<Shown count={3} taskbar="left" />);
		const desktop = container.querySelector('.antilia-desktop')!;
		return new Promise<void>((done) => setTimeout(() => {
			expect(tasks(container)).toEqual(['Window 0', 'Window 1', 'Window 2']);
			expect(desktop.classList.contains('antilia-desktop-taskbar-left')).toBe(true);
			expect(container.querySelector('.antilia-desktop-tile-grid')?.getAttribute('title'))
				.toBe('Grid');
			done();
		}, 0));
	});

	it('changes the windows through its handle', async () => {
		const ref = createRef<DesktopHandle>();
		const { container } = await shown(<Desktop ref={ref}>
			<DesktopWindow id="one" title="One" minimized />
			<DesktopWindow id="two" title="Two" closable />
		</Desktop>);

		expect(ref.current!.desktop).not.toBeNull();
		expect(win(container, 'one').hidden).toBe(true);
		act(() => ref.current!.activate('one'));
		expect(ref.current!.states().one.minimized).toBe(false);
		act(() => ref.current!.close('two'));
		expect(win(container, 'two').hidden).toBe(true);
		act(() => ref.current!.open('two'));
		act(() => ref.current!.maximize('two'));
		act(() => ref.current!.restore('two'));
		act(() => ref.current!.fold('two'));
		expect(ref.current!.states().two.folded).toBe(true);
		act(() => ref.current!.minimize('two'));
		act(() => ref.current!.cycle(1));
		act(() => ref.current!.tile('rows'));
		expect(ref.current!.states().two.minimized).toBe(false);
		act(() => ref.current!.expand());
		expect(container.querySelector('.antilia-desktop')!.hasAttribute('data-expanded')).toBe(true);
		act(() => ref.current!.expand(false));
		expect(container.querySelector('.antilia-desktop')!.hasAttribute('data-expanded'))
			.toBe(false);
	});

	it('keeps the side the user moved the taskbar to, and whether it fills the page', async () => {
		const onTaskbar = vi.fn();
		const onExpand = vi.fn();
		const { container, rerender } = await shown(<Desktop onTaskbar={onTaskbar}
			onExpand={onExpand}>
			<DesktopWindow id="one" title="One" />
		</Desktop>);
		const desktop = container.querySelector('.antilia-desktop')!;

		act(() => {
			fireEvent.keyDown(container.querySelector('.antilia-desktop-grip')!, { key: 'ArrowRight' });
		});
		expect(onTaskbar).toHaveBeenCalledWith('right');
		expect(desktop.classList.contains('antilia-desktop-taskbar-right')).toBe(true);
		act(() => {
			fireEvent.click(container.querySelector('.antilia-desktop-expand')!);
		});
		expect(onExpand).toHaveBeenCalledWith(true);

		// rendering again keeps both
		rerender(<Desktop onTaskbar={onTaskbar} onExpand={onExpand}>
			<DesktopWindow id="one" title="Window one" />
		</Desktop>);
		expect(desktop.classList.contains('antilia-desktop-taskbar-right')).toBe(true);
		expect(desktop.hasAttribute('data-expanded')).toBe(true);
	});

	it('keeps the state the core gave a window when React renders it again', async () => {
		function Counter() {
			const [count, setCount] = useState(0);
			return <Desktop>
				<DesktopWindow id="one" title={'Clicked ' + count}>
					<button type="button" className="more" onClick={() => setCount(count + 1)}>
						more</button>
				</DesktopWindow>
			</Desktop>;
		}
		const { container } = await shown(<Counter />);
		fireEvent.doubleClick(container.querySelector('.antilia-window-header')!);

		fireEvent.click(container.querySelector('.more')!);

		expect(win(container, 'one').hasAttribute('data-folded')).toBe(true);
		expect(win(container, 'one').querySelector('.antilia-window-controls')).not.toBeNull();
		return new Promise<void>((done) => setTimeout(() => {
			expect(tasks(container)).toEqual(['Clicked 1']);
			done();
		}, 0));
	});

	it('asks before closing a window with changes, the page or the dialog of the desktop', async () => {
		const confirmClose = vi.fn(() => false);
		const ref = createRef<DesktopHandle>();
		const { container, rerender } = await shown(<Desktop ref={ref} confirmClose={confirmClose}>
			<DesktopWindow id="one" title="One" closable modified />
		</Desktop>);
		expect(win(container, 'one').getAttribute('data-modified')).toBe('true');

		fireEvent.click(container.querySelector('.antilia-window-control-close')!);
		await waitFor(() => expect(confirmClose).toHaveBeenCalledWith('one', 'One'));
		expect(ref.current!.states().one.closed).toBe(false);

		rerender(<Desktop ref={ref}>
			<DesktopWindow id="one" title="One" closable modified />
		</Desktop>);
		let closed: Promise<boolean> | undefined = undefined;
		act(() => {
			closed = ref.current!.requestClose('one');
		});
		const discard = document.querySelector<HTMLButtonElement>(
			'.antilia-desktop-confirm [data-confirm="discard"]')!;
		expect(discard.textContent).toBe('Close and lose them');
		act(() => discard.click());
		await expect(closed).resolves.toBe(true);
		expect(ref.current!.states().one.closed).toBe(true);

		rerender(<Desktop ref={ref}>
			<DesktopWindow id="one" title="One" closable modified={false} />
		</Desktop>);
		expect(win(container, 'one').getAttribute('data-modified')).toBe('false');
	});

	it('has the menu listing the windows, unless told otherwise', async () => {
		const { container, rerender } = await shown(<Desktop>
			<DesktopWindow id="one" title="One" />
			<DesktopWindow id="two" title="Two" closed />
		</Desktop>);
		act(() => {
			fireEvent.click(container.querySelector('.antilia-desktop-menu-button')!);
		});
		expect(Array.from(container.querySelectorAll('.antilia-desktop-menu-item'),
			(item) => item.textContent)).toEqual(['One', 'Two']);
		act(() => {
			fireEvent.click(container.querySelectorAll('.antilia-desktop-menu-item')[1]);
		});
		expect(win(container, 'two').hidden).toBe(false);

		rerender(<Desktop menu={false}>
			<DesktopWindow id="one" title="One" />
			<DesktopWindow id="two" title="Two" closed />
		</Desktop>);
		expect(container.querySelector('.antilia-desktop-menu-button')).toBeNull();
	});
});

describe('Desktop panes and window types', () => {
	it('shows the windows of the pane shown, following the user and the props', async () => {
		const onPanes = vi.fn();
		const panes = [{ id: '0', name: 'Grids' }, { id: '1', name: 'Dropdowns' }];
		const ui = (active?: string) => <Desktop id="d" panes={panes} activePane={active}
			onPanes={onPanes}>
			<DesktopWindow id="one" title="One" pane="0" />
			<DesktopWindow id="two" title="Two" pane="1" />
		</Desktop>;
		const { container, rerender } = await shown(ui());

		expect(win(container, 'two').hidden).toBe(true);
		const buttons = Array.from(container.querySelectorAll<HTMLElement>('.antilia-desktop-pane'));
		expect(buttons.map((button) => button.textContent)).toEqual(['Grids', 'Dropdowns']);

		act(() => buttons[1].click());

		expect(onPanes).toHaveBeenCalledWith(panes, '1');
		expect(win(container, 'two').hidden).toBe(false);
		expect(win(container, 'one').hidden).toBe(true);
		expect(container.querySelector('.antilia-desktop-pane[aria-pressed="true"]')?.textContent)
			.toBe('Dropdowns');

		rerender(ui('0'));
		expect(win(container, 'one').hidden).toBe(false);
		expect(win(container, 'two').hidden).toBe(true);
	});

	it('renders the windows of a type the user opens, and takes them away closed', async () => {
		const onOpenWindow = vi.fn();
		const ref = createRef<DesktopHandle>();
		const { container } = await shown(<Desktop id="d" ref={ref} onOpenWindow={onOpenWindow}
			windowTypes={[{ type: 'notes', title: 'Notes', icon: '<svg class="notes"></svg>',
				render: (window) => <p>Notes of {window}</p>, window: { width: 300 } }]}>
			<DesktopWindow id="one" title="One" />
		</Desktop>);

		act(() => container.querySelector<HTMLElement>('.antilia-desktop-launcher')!.click());
		const item = container.querySelector<HTMLElement>('.antilia-desktop-menu-item')!;
		expect(item.textContent).toBe('Notes');
		act(() => item.click());

		expect(onOpenWindow).toHaveBeenCalledWith('notes', '0');
		const opened = await waitFor(() => {
			const element = container.querySelector<HTMLElement>('#d-notes-1');
			expect(element).not.toBeNull();
			return element!;
		});
		expect(opened.querySelector('.antilia-window-body')?.textContent).toBe('Notes of d-notes-1');
		expect(opened.querySelector('.antilia-window-icon svg.notes')).not.toBeNull();
		expect(opened.getAttribute('data-width')).toBe('300');
		await waitFor(() => expect(tasks(container)).toEqual(['One', 'Notes']));

		act(() => opened.querySelector<HTMLElement>('.antilia-window-control-close')!.click());

		await waitFor(() => expect(container.querySelector('#d-notes-1')).toBeNull());
		await waitFor(() => expect(tasks(container)).toEqual(['One']));

		act(() => ref.current!.openWindow('notes'));
		await waitFor(() => expect(container.querySelector('#d-notes-2')).not.toBeNull());
	});
});

