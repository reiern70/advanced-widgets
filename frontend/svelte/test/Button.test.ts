/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { Button } from '../src/index.js';

afterEach(cleanup);

describe('Button', () => {
	it('renders the markup of the Wicket button: text, variant and size', async () => {
		const click = vi.fn();
		const { container } = render(Button, { label: 'Save', variant: 'primary', size: 'large',
			onclick: click });

		const button = container.querySelector('button')!;
		expect(button.type).toBe('button');
		expect(button.className).toBe('ant-button ant-button--primary ant-button--large');
		expect(button.querySelector('.ant-button__label')?.textContent).toBe('Save');
		expect(button.querySelector('.ant-button__icon')).toBeNull();
		await fireEvent.click(button);
		expect(click).toHaveBeenCalledTimes(1);
	});

	it('shows only its icon, its text as tooltip and name, or submits when told', () => {
		const { container } = render(Button, { label: 'Delete', iconOnly: true, type: 'submit',
			disabled: true });

		const button = container.querySelector('button')!;
		expect(button.className).toBe('ant-button ant-button--secondary ant-button--medium ' +
			'ant-button--icon-only');
		expect(button.title).toBe('Delete');
		expect(button.getAttribute('aria-label')).toBe('Delete');
		expect(button.querySelector('.ant-button__label')).toBeNull();
		expect(button.type).toBe('submit');
		expect(button.disabled).toBe(true);
	});
});
