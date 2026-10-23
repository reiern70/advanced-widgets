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

import { cleanup, render } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

import { ProgressBar } from '../src/index.js';

afterEach(cleanup);

describe('ProgressBar', () => {
	it('renders the markup of the Wicket bar, the value clamped and rounded', () => {
		const { container, rerender } = render(<ProgressBar value={42.4} />);

		expect(container.innerHTML).toBe('<div class="antilia-progress-bar">' +
			'<progress class="antilia-progress-bar-value" max="100" value="42">42%</progress>' +
			'<span class="antilia-progress-bar-label" aria-hidden="true">42%</span></div>');

		rerender(<ProgressBar value={130} />);
		expect(container.querySelector('progress')?.getAttribute('value')).toBe('100');
	});

	it('is indeterminate without a value', () => {
		const { container } = render(<ProgressBar className="big" />);

		expect(container.innerHTML).toBe('<div class="antilia-progress-bar big">' +
			'<progress class="antilia-progress-bar-value" max="100"></progress></div>');
	});
});
