/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
});
