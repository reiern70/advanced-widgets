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

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { NOTIFICATION_ICONS, NOTIFICATION_LEAVE_MS, NOTIFICATION_TIMEOUT_PROPERTY, Notifications,
	setHost, standaloneHost, type Host } from '../src/index.js';

function host(): HTMLElement {
	document.body.innerHTML = '<button id="before">Before</button><div id="n"></div>';
	return document.getElementById('n')!;
}

function stack(position: string): HTMLElement | null {
	return document.querySelector('.antilia-notifications-stack-' + position);
}

function titles(position = 'top-right'): string[] {
	return Array.from(stack(position)?.querySelectorAll('.antilia-notification:not(' +
		'.antilia-notification-leaving) .antilia-notification-title') ?? [])
		.map((title) => title.textContent ?? '');
}

function key(target: Element, name: string): KeyboardEvent {
	const event = new KeyboardEvent('keydown', { key: name, bubbles: true, cancelable: true });
	target.dispatchEvent(event);
	return event;
}

beforeEach(() => {
	vi.useFakeTimers();
});

afterEach(() => {
	vi.useRealTimers();
	document.body.innerHTML = '';
	setHost(standaloneHost);
});

describe('Notifications', () => {
	it('shows a notification with a header, a body and a button closing it', () => {
		const notifications = new Notifications(host());
		const shown = notifications.show({ type: 'success', title: 'Saved', body: 'The user was saved.' });

		const element = shown.element;
		expect(notifications.element.classList.contains('antilia-notifications')).toBe(true);
		expect(element.parentElement).toBe(stack('top-right'));
		expect(element.className).toBe('antilia-notification antilia-notification-success ' +
			'antilia-notification-timed');
		expect(element.getAttribute('role')).toBe('status');
		expect(element.dataset.type).toBe('success');
		expect(element.querySelector('.antilia-notification-title')!.textContent).toBe('Saved');
		expect(element.getAttribute('aria-labelledby')).toBe(
			element.querySelector('.antilia-notification-title')!.id);
		expect(element.querySelector('.antilia-notification-body')!.textContent).toBe(
			'The user was saved.');
		expect(element.querySelector('.antilia-notification-icon path')!.getAttribute('d')).toBe(
			NOTIFICATION_ICONS.success);
		const close = element.querySelector<HTMLButtonElement>('.antilia-notification-close')!;
		expect(close.title).toBe('Close');
		expect(close.getAttribute('aria-label')).toBe('Close');
		const region = stack('top-right')!;
		expect(region.tagName).toBe('SECTION');
		expect(region.getAttribute('aria-label')).toBe('Notifications');
		expect(region.getAttribute('aria-live')).toBe('polite');
	});

	it('writes the body as text, or appends the page\'s content', () => {
		const notifications = new Notifications(host());
		const text = notifications.show({ body: '<b>bold</b>' });
		const strong = document.createElement('strong');
		strong.textContent = 'Mine';
		const node = notifications.show({ body: strong });
		const empty = notifications.show({ title: 'Only a title' });

		expect(text.element.querySelector('.antilia-notification-body')!.innerHTML).toBe(
			'&lt;b&gt;bold&lt;/b&gt;');
		expect(node.element.querySelector('.antilia-notification-body strong')).toBe(strong);
		expect(empty.element.querySelector('.antilia-notification-body')).toBeNull();
	});

	it('makes errors and warnings alerts, and info the default type', () => {
		const notifications = new Notifications(host());

		expect(notifications.show({ type: 'error' }).element.getAttribute('role')).toBe('alert');
		expect(notifications.show({ type: 'warning' }).element.getAttribute('role')).toBe('alert');
		const info = notifications.show({});
		expect(info.element.getAttribute('role')).toBe('status');
		expect(info.element.classList.contains('antilia-notification-info')).toBe(true);
		expect(info.element.hasAttribute('aria-labelledby')).toBe(false);
	});

	it('gives a type of the page\'s own its class, and an icon only when given one', () => {
		const notifications = new Notifications(host());
		const note = notifications.show({ type: 'Release Note', className: 'mine' });
		const icon = notifications.show({ type: 'note', icon: 'M0 0h24v24H0z' });
		const none = notifications.show({ type: 'error', icon: false });

		expect(note.element.className).toContain('antilia-notification-release-note mine');
		expect(note.element.dataset.type).toBe('Release Note');
		expect(note.element.querySelector('.antilia-notification-icon')).toBeNull();
		expect(icon.element.querySelector('.antilia-notification-icon path')!.getAttribute('d'))
			.toBe('M0 0h24v24H0z');
		expect(none.element.querySelector('.antilia-notification-icon')).toBeNull();
	});

	it('goes after the timeout, which the countdown takes', () => {
		const onClose = vi.fn();
		const notifications = new Notifications(host(), { timeout: 3000 });
		const shown = notifications.show({ title: 'Soon gone', onClose });

		expect(shown.element.style.getPropertyValue(NOTIFICATION_TIMEOUT_PROPERTY)).toBe('3000ms');
		expect(shown.element.querySelector('.antilia-notification-countdown')).not.toBeNull();
		vi.advanceTimersByTime(2999);
		expect(shown.closed).toBe(false);
		vi.advanceTimersByTime(1);
		expect(shown.closed).toBe(true);
		expect(onClose).toHaveBeenCalledWith('timeout');
		expect(shown.element.classList.contains('antilia-notification-leaving')).toBe(true);
		expect(shown.element.isConnected).toBe(true);
		vi.advanceTimersByTime(NOTIFICATION_LEAVE_MS);
		expect(shown.element.isConnected).toBe(false);
		expect(notifications.notifications()).toEqual([]);
	});

	it('keeps a notification of timeout 0 until the user closes it', () => {
		const onClose = vi.fn();
		const notifications = new Notifications(host());
		const shown = notifications.show({ title: 'Stays', timeout: 0, onClose });

		expect(shown.element.classList.contains('antilia-notification-timed')).toBe(false);
		expect(shown.element.querySelector('.antilia-notification-countdown')).toBeNull();
		vi.advanceTimersByTime(60000);
		expect(shown.closed).toBe(false);

		shown.element.querySelector<HTMLButtonElement>('.antilia-notification-close')!.click();
		expect(shown.closed).toBe(true);
		expect(onClose).toHaveBeenCalledWith('user');
		shown.element.querySelector<HTMLButtonElement>('.antilia-notification-close')!.click();
		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it('keeps all notifications when the timeout of the notifications is 0', () => {
		const notifications = new Notifications(host(), { timeout: 0 });
		const shown = notifications.show({});

		vi.advanceTimersByTime(60000);
		expect(shown.closed).toBe(false);
		expect(notifications.show({ timeout: 100 }).element.classList.contains(
			'antilia-notification-timed')).toBe(true);
	});

	it('keeps errors until the user closes them, unless told otherwise', () => {
		const notifications = new Notifications(host(), { timeout: 1000 });
		const error = notifications.show({ type: 'error', title: 'Broken' });
		const timed = notifications.show({ type: 'error', title: 'Timed', timeout: 500 });
		const info = notifications.show({ title: 'Told' });

		expect(error.element.classList.contains('antilia-notification-timed')).toBe(false);
		vi.advanceTimersByTime(60000);
		expect(error.closed).toBe(false);
		expect(timed.closed).toBe(true);
		expect(info.closed).toBe(true);

		notifications.configure({ errorTimeout: 2000 });
		const later = notifications.show({ type: ' error ' });
		vi.advanceTimersByTime(2000);
		expect(later.closed).toBe(true);
		expect(error.closed).toBe(false);
	});

	it('takes a body as text, unless told it is markup', () => {
		const notifications = new Notifications(host());
		const text = notifications.show({ body: '<strong>Saved</strong>' });
		const markup = notifications.show({ body: 'The user <strong>Ada</strong> was saved.',
			html: true });

		expect(text.element.querySelector('.antilia-notification-body')!.textContent).toBe(
			'<strong>Saved</strong>');
		expect(text.element.querySelector('strong')).toBeNull();
		expect(markup.element.querySelector('.antilia-notification-body strong')!.textContent)
			.toBe('Ada');
		expect(markup.element.querySelector('.antilia-notification-body')!.textContent).toBe(
			'The user Ada was saved.');
	});

	it('has no close button when not closable, and Escape does not close it', () => {
		const notifications = new Notifications(host());
		const shown = notifications.show({ closable: false, timeout: 0 });

		expect(shown.element.querySelector('.antilia-notification-close')).toBeNull();
		key(shown.element, 'Escape');
		expect(shown.closed).toBe(false);
		shown.close();
		expect(shown.closed).toBe(true);
	});

	it('closes the notification that has the focus on Escape, and gives the focus to the next', () => {
		const notifications = new Notifications(host());
		const first = notifications.show({ title: 'First', timeout: 0 });
		const second = notifications.show({ title: 'Second', timeout: 0 });
		const close = second.element.querySelector<HTMLButtonElement>('.antilia-notification-close')!;
		close.focus();

		const event = key(close, 'Escape');
		expect(event.defaultPrevented).toBe(true);
		expect(second.closed).toBe(true);
		expect(document.activeElement).toBe(
			first.element.querySelector('.antilia-notification-close'));
		key(document.activeElement!, 'Enter');
		expect(first.closed).toBe(false);

		key(document.activeElement!, 'Escape');
		expect(first.closed).toBe(true);
		expect(document.activeElement).toBe(document.body);
	});

	it('waits with the countdown while the pointer is over the notification', () => {
		const notifications = new Notifications(host());
		const shown = notifications.show({ timeout: 1000 });

		vi.advanceTimersByTime(600);
		shown.element.dispatchEvent(new Event('pointerenter'));
		expect(shown.element.classList.contains('antilia-notification-paused')).toBe(true);
		vi.advanceTimersByTime(5000);
		expect(shown.closed).toBe(false);
		shown.element.dispatchEvent(new Event('pointerleave'));
		expect(shown.element.classList.contains('antilia-notification-paused')).toBe(false);
		vi.advanceTimersByTime(399);
		expect(shown.closed).toBe(false);
		vi.advanceTimersByTime(1);
		expect(shown.closed).toBe(true);
	});

	it('goes on counting under the pointer when told not to pause on hover', () => {
		const notifications = new Notifications(host(), { pauseOnHover: false });
		const shown = notifications.show({ timeout: 1000 });

		shown.element.dispatchEvent(new Event('pointerenter'));
		expect(shown.element.classList.contains('antilia-notification-paused')).toBe(false);
		vi.advanceTimersByTime(1000);
		expect(shown.closed).toBe(true);
	});

	it('waits with the countdown while the focus is in the notification', () => {
		const notifications = new Notifications(host(), { pauseOnHover: false });
		const shown = notifications.show({ timeout: 1000 });
		const close = shown.element.querySelector<HTMLButtonElement>('.antilia-notification-close')!;

		close.focus();
		vi.advanceTimersByTime(5000);
		expect(shown.closed).toBe(false);
		// the pointer leaving does not start it while the focus is in it
		shown.element.dispatchEvent(new Event('pointerleave'));
		vi.advanceTimersByTime(5000);
		expect(shown.closed).toBe(false);
		document.getElementById('before')!.focus();
		vi.advanceTimersByTime(1000);
		expect(shown.closed).toBe(true);
	});

	it('stacks the newest next to the edge: on top at the top, below at the bottom', () => {
		const notifications = new Notifications(host(), { position: 'bottom-left' });
		notifications.show({ title: 'One' });
		notifications.show({ title: 'Two' });
		notifications.show({ title: 'Three', position: 'top-center' });
		notifications.show({ title: 'Four', position: 'top-center' });

		expect(titles('bottom-left')).toEqual(['One', 'Two']);
		expect(titles('top-center')).toEqual(['Four', 'Three']);
		expect(notifications.element.children).toHaveLength(2);
	});

	it('takes the default position for one it does not know', () => {
		const notifications = new Notifications(host(), {
			position: 'middle' as never });
		notifications.show({ title: 'Here', position: 'nowhere' as never });

		expect(titles('top-right')).toEqual(['Here']);
	});

	it('pushes the oldest out of a full stack', () => {
		const closed: string[] = [];
		const notifications = new Notifications(host(), { max: 2 });
		for (const title of ['A', 'B', 'C']) {
			notifications.show({ title, onClose: (reason) => closed.push(title + ':' + reason) });
		}
		notifications.show({ title: 'Elsewhere', position: 'bottom-right' });

		expect(titles()).toEqual(['C', 'B']);
		expect(closed).toEqual(['A:overflow']);
		expect(notifications.notifications().map((shown) =>
			shown.element.querySelector('.antilia-notification-title')!.textContent))
			.toEqual(['B', 'C', 'Elsewhere']);
	});

	it('closes all notifications', () => {
		const onClose = vi.fn();
		const notifications = new Notifications(host());
		notifications.show({ onClose });
		notifications.show({ onClose, position: 'bottom-center' });

		notifications.closeAll();
		expect(onClose).toHaveBeenCalledTimes(2);
		expect(onClose).toHaveBeenCalledWith('page');
		expect(notifications.notifications()).toEqual([]);
	});

	it('changes the texts of the notifications shown, and where new ones go', () => {
		const notifications = new Notifications(host());
		const shown = notifications.show({ title: 'Old' });

		notifications.configure({ closeLabel: 'Schließen', label: 'Mitteilungen',
			position: 'bottom-right' });
		notifications.show({ title: 'New' });

		const close = shown.element.querySelector<HTMLButtonElement>('.antilia-notification-close')!;
		expect(close.title).toBe('Schließen');
		expect(close.getAttribute('aria-label')).toBe('Schließen');
		expect(stack('top-right')!.getAttribute('aria-label')).toBe('Mitteilungen');
		expect(titles('top-right')).toEqual(['Old']);
		expect(titles('bottom-right')).toEqual(['New']);
	});

	it('takes everything away at once when destroyed, without telling', () => {
		const onClose = vi.fn();
		const notifications = new Notifications(host());
		const shown = notifications.show({ onClose });

		notifications.destroy();
		notifications.destroy();
		expect(onClose).not.toHaveBeenCalled();
		expect(shown.element.isConnected).toBe(false);
		expect(notifications.element.children).toHaveLength(0);
		vi.advanceTimersByTime(10000);
		expect(onClose).not.toHaveBeenCalled();

		const late = notifications.show({ title: 'Too late' });
		expect(late.closed).toBe(true);
		expect(late.element.isConnected).toBe(false);
	});

	it('goes when an update of the page removes its element', () => {
		let removing: ((element: Element) => void) | undefined = undefined;
		setHost({ ...standaloneHost, onNodeRemoving: (listener) => {
			removing = listener;
			return () => {
				removing = undefined;
			};
		} } as Host);
		const element = host();
		const notifications = new Notifications(element);
		const shown = notifications.show({});

		removing!(document.body);
		expect(shown.element.isConnected).toBe(false);
		expect(removing).toBeUndefined();
	});
});
