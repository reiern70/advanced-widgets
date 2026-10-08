/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, render } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

import { ProgressBar } from '../src/index.js';

afterEach(cleanup);

describe('ProgressBar', () => {
	it('renders the markup of the Wicket bar, the value clamped and rounded', () => {
		const { container, rerender } = render(<ProgressBar value={42.4} />);

		expect(container.innerHTML).toBe('<div class="wicket-progress-bar">' +
			'<progress class="wicket-progress-bar-value" max="100" value="42">42%</progress>' +
			'<span class="wicket-progress-bar-label" aria-hidden="true">42%</span></div>');

		rerender(<ProgressBar value={130} />);
		expect(container.querySelector('progress')?.getAttribute('value')).toBe('100');
	});

	it('is indeterminate without a value', () => {
		const { container } = render(<ProgressBar className="big" />);

		expect(container.innerHTML).toBe('<div class="wicket-progress-bar big">' +
			'<progress class="wicket-progress-bar-value" max="100"></progress></div>');
	});
});
