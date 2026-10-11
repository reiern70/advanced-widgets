/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { createRef, StrictMode } from 'react';
import { act, cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { Notifications, NotificationsProvider, useNotifications,
	type NotificationsHandle } from '../src/index.js';

afterEach(() => {
	cleanup();
	vi.useRealTimers();
});

function titles(container: ParentNode, position = 'top-right'): string[] {
	return Array.from(container.querySelectorAll('.antilia-notifications-stack-' + position +
		' .antilia-notification:not(.antilia-notification-leaving) .antilia-notification-title'),
	(title) => title.textContent ?? '');
}

describe('Notifications', () => {
	it('renders an empty element the core stacks the notifications in', () => {
		const ref = createRef<NotificationsHandle>();
		const { container } = render(<StrictMode>
			<Notifications ref={ref} id="n" className="mine" closeLabel="Zu" />
		</StrictMode>);

		const element = container.querySelector('#n')!;
		expect(element.className).toBe('antilia-notifications mine');
		expect(element.children).toHaveLength(0);

		act(() => {
			ref.current!.show({ type: 'success', title: 'Saved', body: 'Done.' });
		});
		expect(titles(element)).toEqual(['Saved']);
		expect(element.querySelector('.antilia-notification-success .antilia-notification-body')
			?.textContent).toBe('Done.');
		expect(element.querySelector<HTMLButtonElement>('.antilia-notification-close')!.title)
			.toBe('Zu');
	});

	it('takes the position, timeout and texts as they change', () => {
		vi.useFakeTimers();
		const ref = createRef<NotificationsHandle>();
		const { container, rerender } = render(<Notifications ref={ref} timeout={0} />);
		act(() => {
			ref.current!.show({ title: 'Stays' });
		});
		rerender(<Notifications ref={ref} position="bottom-left" timeout={1000} errorTimeout={2000}
			max={1} closeLabel="Cerrar" />);
		act(() => {
			ref.current!.show({ title: 'Goes' });
			ref.current!.show({ type: 'error', title: 'Later', position: 'top-left' });
		});

		expect(titles(container)).toEqual(['Stays']);
		expect(titles(container, 'bottom-left')).toEqual(['Goes']);
		expect(container.querySelector<HTMLButtonElement>('.antilia-notification-close')!.title)
			.toBe('Cerrar');
		act(() => {
			vi.advanceTimersByTime(1000);
		});
		expect(titles(container, 'bottom-left')).toEqual([]);
		expect(titles(container)).toEqual(['Stays']);
		expect(titles(container, 'top-left')).toEqual(['Later']);
		act(() => {
			vi.advanceTimersByTime(1000);
		});
		expect(titles(container, 'top-left')).toEqual([]);

		act(() => {
			ref.current!.closeAll();
		});
		expect(titles(container)).toEqual([]);
	});

	it('takes the notifications away when unmounted', () => {
		const ref = createRef<NotificationsHandle>();
		const onClose = vi.fn();
		const { container, unmount } = render(<Notifications ref={ref} />);
		let shown = undefined as ReturnType<NotificationsHandle['show']> | undefined;
		act(() => {
			shown = ref.current!.show({ title: 'Here', onClose });
		});
		const handle = ref.current!;

		unmount();
		expect(container.querySelector('.antilia-notification')).toBeNull();
		expect(shown!.element.isConnected).toBe(false);
		expect(onClose).not.toHaveBeenCalled();
		expect(handle.show({ title: 'Too late' }).closed).toBe(true);
	});

	it('gives useNotifications the notifications of the provider', () => {
		function Save() {
			const notifications = useNotifications();
			return <button onClick={() => notifications.show({ type: 'success', title: 'Saved' })}>
				Save
			</button>;
		}
		function CloseAll() {
			const notifications = useNotifications();
			return <button onClick={() => notifications.closeAll()}>Close all</button>;
		}
		const { container, getByText } = render(<NotificationsProvider position="top-center">
			<Save />
			<CloseAll />
		</NotificationsProvider>);

		fireEvent.click(getByText('Save'));
		fireEvent.click(getByText('Save'));
		expect(titles(container, 'top-center')).toEqual(['Saved', 'Saved']);
		expect(container.lastElementChild?.className).toBe('antilia-notifications');
		fireEvent.click(getByText('Close all'));
		expect(titles(container, 'top-center')).toEqual([]);
	});

	it('says useNotifications needs a provider', () => {
		function Lonely() {
			useNotifications();
			return null;
		}
		vi.spyOn(console, 'error').mockImplementation(() => undefined);

		expect(() => render(<Lonely />)).toThrow(/NotificationsProvider/);
	});
});
