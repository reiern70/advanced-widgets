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

	it('comes in a size, or as wide as told', () => {
		const { container, rerender } = render(<Modal open onClose={() => undefined}
			size="small">Body</Modal>);
		const dialog = container.querySelector<HTMLElement>('.modal-dialog')!;
		expect(dialog.classList.contains('modal-dialog-small')).toBe(true);

		rerender(<Modal open onClose={() => undefined} size="large" width="40em">Body</Modal>);
		const resized = container.querySelector<HTMLElement>('.modal-dialog')!;
		expect(resized.classList.contains('modal-dialog-small')).toBe(false);
		expect(resized.classList.contains('modal-dialog-large')).toBe(true);
		expect(resized.style.width).toBe('40em');
	});

	it('opens a dialog nested in another over it, Escape closing the one on top', () => {
		function Nested() {
			const [inner, setInner] = useState(false);
			const [outer, setOuter] = useState(true);
			return (
				<Modal open={outer} onClose={() => setOuter(false)} title="Outer">
					<button type="button" onClick={() => setInner(true)}>More</button>
					<Modal open={inner} onClose={() => setInner(false)} title="Inner">
						<input name="inner" />
					</Modal>
				</Modal>
			);
		}
		const { container, getByText } = render(<Nested />);
		const more = getByText('More');
		fireEvent.click(more);
		expect(container.querySelectorAll('.modal-dialog')).toHaveLength(2);
		expect(document.activeElement).toBe(container.querySelector('input[name="inner"]'));

		fireEvent.keyDown(document.activeElement!, { key: 'Escape' });
		expect(container.querySelectorAll('.modal-dialog')).toHaveLength(1);
		expect(document.activeElement).toBe(more);

		fireEvent.keyDown(more, { key: 'Escape' });
		expect(container.querySelector('.modal-dialog')).toBeNull();
	});
});
