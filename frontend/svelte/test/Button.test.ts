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

import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, Button, LOOK_CONTEXT } from '../src/index.js';
import LookedButtons from './LookedButtons.svelte';

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

	it('has the classes of the look set, or given to the app as it mounts', () => {
		const { container } = render(LookedButtons, { look: 'bootstrap' });

		const [save, undo] = Array.from(container.querySelectorAll('button'));
		expect(save.className).toBe('btn btn-primary btn-sm');
		expect(save.querySelector('.ant-button__label')?.textContent).toBe('Save');
		expect(undo.className).toBe('btn btn-link ant-button--icon-only');

		const mounted = render(Button, { props: { label: 'Go' },
			context: new Map([[LOOK_CONTEXT, BOOTSTRAP_LOOK]]) });
		expect(mounted.container.querySelector('button')!.className).toBe('btn btn-secondary');
	});
});
