/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { Button } from '../src/index.js';

afterEach(cleanup);

describe('Button', () => {
	it('renders the markup of the Wicket button: icon, text, variant and size', () => {
		const click = vi.fn();
		const { container } = render(<Button label="Save" icon={<svg />} variant="primary"
			size="large" onClick={click} />);

		expect(container.innerHTML).toBe('<button type="button" ' +
			'class="aw-button aw-button--primary aw-button--large">' +
			'<span class="aw-button__icon" aria-hidden="true"><svg></svg></span>' +
			'<span class="aw-button__label">Save</span></button>');
		fireEvent.click(container.querySelector('button')!);
		expect(click).toHaveBeenCalledTimes(1);
	});

	it('shows only its icon, its text as tooltip and name, or submits when told', () => {
		const { container } = render(<><Button label="Delete" icon={<svg />} iconOnly />
			<Button type="submit" label="Send" disabled /></>);

		const [icon, submit] = Array.from(container.querySelectorAll('button'));
		expect(icon.className).toBe('aw-button aw-button--secondary aw-button--medium ' +
			'aw-button--icon-only');
		expect(icon.title).toBe('Delete');
		expect(icon.getAttribute('aria-label')).toBe('Delete');
		expect(icon.querySelector('.aw-button__label')).toBeNull();
		expect(submit.type).toBe('submit');
		expect(submit.disabled).toBe(true);
	});
});
