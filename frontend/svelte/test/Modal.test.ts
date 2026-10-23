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

import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { Modal } from '../src/index.js';

beforeAll(() => {
	// jsdom lays nothing out: every element counts as shown
	vi.spyOn(HTMLElement.prototype, 'getClientRects').mockReturnValue(
		[{}] as unknown as DOMRectList);
});

afterEach(cleanup);

describe('Modal', () => {
	it('renders the markup of Wicket\'s ModalDialog and closes on Escape', async () => {
		const onclose = vi.fn();
		const { container, rerender } = render(Modal, { open: true, onclose, title: 'Edit Ada' });
		flushSync();

		const dialog = container.querySelector('.dialog-theme-default .modal-dialog-overlay > ' +
			'.modal-dialog')!;
		expect(dialog.getAttribute('role')).toBe('dialog');
		const title = dialog.querySelector('.modal-dialog-header > .modal-dialog-title')!;
		expect(title.textContent).toBe('Edit Ada');
		expect(dialog.getAttribute('aria-labelledby')).toBe(title.id);
		expect(dialog.querySelector('.modal-dialog-header > .modal-dialog-close')!
			.getAttribute('aria-label')).toBe('Close');

		await fireEvent.keyDown(dialog, { key: 'Escape' });
		expect(onclose).toHaveBeenCalledTimes(1);

		await rerender({ open: false });
		expect(container.querySelector('.modal-dialog')).toBeNull();
	});

	it('closes on a click on the overlay only when told to', async () => {
		const onclose = vi.fn();
		const { container } = render(Modal, { open: true, onclose, closeOnClick: true });
		flushSync();

		await fireEvent.click(container.querySelector('.modal-dialog')!);
		expect(onclose).not.toHaveBeenCalled();
		await fireEvent.click(container.querySelector('.modal-dialog-overlay')!);
		expect(onclose).toHaveBeenCalledTimes(1);
	});

	it('closes with its close button when canClose lets it', async () => {
		const onclose = vi.fn();
		let allowed = false;
		const { container } = render(Modal, { open: true, onclose, title: 'Edit',
			closeLabel: 'Schließen', canClose: () => allowed });
		flushSync();

		const close = container.querySelector<HTMLElement>('.modal-dialog-close')!;
		expect(close.getAttribute('aria-label')).toBe('Schließen');
		await fireEvent.click(close);
		expect(onclose).not.toHaveBeenCalled();
		allowed = true;
		await fireEvent.click(close);
		expect(onclose).toHaveBeenCalledTimes(1);
	});

	it('leaves out the close button, dragging and folding when told to', () => {
		const { container } = render(Modal, { open: true, onclose: () => undefined,
			title: 'Edit', closeButton: false, draggable: false, foldable: false });
		flushSync();

		expect(container.querySelector('.modal-dialog-close')).toBeNull();
		const dialog = container.querySelector('.modal-dialog')!;
		expect(dialog.classList.contains('modal-dialog-draggable')).toBe(false);
		expect(dialog.classList.contains('modal-dialog-foldable')).toBe(false);
	});

	it('comes in a size, or as wide as told', () => {
		const { container } = render(Modal, { open: true, onclose: () => undefined,
			size: 'medium', width: '30em' });
		flushSync();

		const dialog = container.querySelector<HTMLElement>('.modal-dialog')!;
		expect(dialog.classList.contains('modal-dialog-medium')).toBe(true);
		expect(dialog.classList.contains('modal-dialog-sized')).toBe(true);
		expect(dialog.style.width).toBe('30em');
	});
});
