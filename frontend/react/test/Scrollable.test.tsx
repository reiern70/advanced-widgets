/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { StrictMode } from 'react';
import { cleanup, render } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

import { Scrollbar } from '@advanced-widgets/core';

import { Scrollable } from '../src/index.js';

afterEach(cleanup);

function tracks(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('.wicket-scrollbar-track'),
		(track) => track.className);
}

describe('Scrollable', () => {
	it('draws the bars of the axes asked for inside the wrapper React renders', () => {
		const { container } = render(<StrictMode><Scrollable axis="y" className="list">
			<p>content</p></Scrollable></StrictMode>);

		const wrapper = container.firstElementChild!;
		expect(wrapper.className).toBe('wicket-scrollbar');
		const element = wrapper.firstElementChild!;
		expect(element.classList.contains('wicket-scrollbar-host')).toBe(true);
		expect(element.classList.contains('list')).toBe(true);
		expect(wrapper.querySelector('p')?.textContent).toBe('content');
		expect(tracks(container)).toEqual(['wicket-scrollbar-track wicket-scrollbar-track--y']);
	});

	it('draws other bars when the axis changes', () => {
		const { container, rerender } = render(<Scrollable axis="y" />);
		rerender(<Scrollable axis="both" />);

		expect(tracks(container).length).toBe(2);
		expect(container.firstElementChild?.classList.contains('wicket-scrollbar--both')).toBe(true);
	});

	it('removes the bars when unmounted', () => {
		const { container, unmount } = render(<Scrollable axis="x" />);
		unmount();

		expect(container.innerHTML).toBe('');
		expect(Scrollbar.instances).toEqual([]);
	});
});
