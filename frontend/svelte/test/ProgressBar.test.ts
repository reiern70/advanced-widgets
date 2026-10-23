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

import { cleanup, render } from '@testing-library/svelte';
import { afterEach, describe, expect, it } from 'vitest';

import { ProgressBar } from '../src/index.js';

afterEach(cleanup);

describe('ProgressBar', () => {
	it('renders the markup of the Wicket bar, the value clamped and rounded', async () => {
		const { container, rerender } = render(ProgressBar, { value: 42.4 });

		const bar = container.querySelector('.antilia-progress-bar')!;
		expect(bar.querySelector('progress')?.getAttribute('value')).toBe('42');
		expect(bar.querySelector('progress')?.textContent).toBe('42%');
		expect(bar.querySelector('.antilia-progress-bar-label')?.textContent).toBe('42%');
		expect(bar.querySelector('.antilia-progress-bar-label')?.getAttribute('aria-hidden'))
			.toBe('true');

		await rerender({ value: -5 });
		expect(bar.querySelector('progress')?.getAttribute('value')).toBe('0');
	});

	it('is indeterminate without a value', () => {
		const { container } = render(ProgressBar, { value: null, class: 'big' });

		const bar = container.querySelector('.antilia-progress-bar')!;
		expect(bar.className).toBe('antilia-progress-bar big');
		expect(bar.querySelector('progress')?.hasAttribute('value')).toBe(false);
		expect(bar.querySelector('.antilia-progress-bar-label')).toBeNull();
	});
});
