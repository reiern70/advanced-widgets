/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useState } from 'react';
import { cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { Modal } from '../src/index.js';

beforeAll(() => {
	// jsdom lays nothing out: every element counts as shown
	vi.spyOn(HTMLElement.prototype, 'getClientRects').mockReturnValue(
		[{}] as unknown as DOMRectList);
});

afterEach(cleanup);

function Opener() {
	const [open, setOpen] = useState(false);
	return (
		<>
			<button type="button" onClick={() => setOpen(true)}>Edit</button>
			<Modal open={open} onClose={() => setOpen(false)} title="Edit Ada"
				footer={<button type="button" onClick={() => setOpen(false)}>Save</button>}>
				<input name="name" />
			</Modal>
		</>
	);
}

describe('Modal', () => {
	it('renders the markup of Wicket\'s ModalDialog, focused, and closes on Escape', () => {
		const { container, getByText } = render(<Opener />);
		const opener = getByText('Edit');
		opener.focus();
		fireEvent.click(opener);

		const theme = container.querySelector('.dialog-theme-default')!;
		const dialog = theme.querySelector('.modal-dialog-overlay > .modal-dialog')!;
		expect(dialog.getAttribute('role')).toBe('dialog');
		expect(dialog.getAttribute('aria-modal')).toBe('true');
		const title = dialog.querySelector(
			'.modal-dialog-content > .modal-dialog-header > .modal-dialog-title')!;
		expect(title.textContent).toBe('Edit Ada');
		expect(dialog.getAttribute('aria-labelledby')).toBe(title.id);
		expect(dialog.querySelector('.modal-dialog-footer button')!.textContent).toBe('Save');
		expect(document.activeElement).toBe(dialog.querySelector('input'));

		fireEvent.keyDown(document.activeElement!, { key: 'Escape' });
		expect(container.querySelector('.modal-dialog')).toBeNull();
		expect(document.activeElement).toBe(opener);
	});

	it('closes on a click on the overlay only when told to', () => {
		const onClose = vi.fn();
		const { container, rerender } = render(<Modal open onClose={onClose}>Body</Modal>);
		fireEvent.click(container.querySelector('.modal-dialog-overlay')!);
		expect(onClose).not.toHaveBeenCalled();

		rerender(<Modal open onClose={onClose} closeOnClick>Body</Modal>);
		fireEvent.click(container.querySelector('.modal-dialog')!);
		expect(onClose).not.toHaveBeenCalled();
		fireEvent.click(container.querySelector('.modal-dialog-overlay')!);
		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it('closes with its close button when canClose lets it', () => {
		const onClose = vi.fn();
		let allowed = false;
		const { container } = render(<Modal open onClose={onClose} title="Edit"
			closeLabel="Schließen" canClose={() => allowed}>Body</Modal>);

		const close = container.querySelector<HTMLElement>('.modal-dialog-close')!;
		expect(close.getAttribute('aria-label')).toBe('Schließen');
		fireEvent.click(close);
		expect(onClose).not.toHaveBeenCalled();
		allowed = true;
		fireEvent.click(close);
		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it('leaves out the close button, dragging and folding when told to', () => {
		const { container } = render(<Modal open onClose={() => undefined} title="Edit"
			closeButton={false} draggable={false} foldable={false}>Body</Modal>);

		expect(container.querySelector('.modal-dialog-close')).toBeNull();
		const dialog = container.querySelector('.modal-dialog')!;
		expect(dialog.classList.contains('modal-dialog-draggable')).toBe(false);
		expect(dialog.classList.contains('modal-dialog-foldable')).toBe(false);
	});
});
