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

import { createRef, StrictMode } from 'react';
import { cleanup, render } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

import { Scrollbar } from '@antilia/core';

import { Scrollable, type ScrollableHandle } from '../src/index.js';

afterEach(cleanup);

function tracks(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('.antilia-scrollbar-track'),
		(track) => track.className);
}

describe('Scrollable', () => {
	it('draws the bars of the axes asked for inside the wrapper React renders', () => {
		const { container } = render(<StrictMode><Scrollable axis="y" className="list">
			<p>content</p></Scrollable></StrictMode>);

		const wrapper = container.firstElementChild!;
		expect(wrapper.className).toBe('antilia-scrollbar');
		const element = wrapper.firstElementChild!;
		expect(element.classList.contains('antilia-scrollbar-host')).toBe(true);
		expect(element.classList.contains('list')).toBe(true);
		expect(wrapper.querySelector('p')?.textContent).toBe('content');
		expect(tracks(container)).toEqual(['antilia-scrollbar-track antilia-scrollbar-track--y']);
	});

	it('draws other bars when the axis changes', () => {
		const { container, rerender } = render(<Scrollable axis="y" />);
		rerender(<Scrollable axis="both" />);

		expect(tracks(container).length).toBe(2);
		expect(container.firstElementChild?.classList.contains('antilia-scrollbar--both')).toBe(true);
	});

	it('scrolls through its ref', () => {
		const ref = createRef<ScrollableHandle>();
		render(<Scrollable ref={ref} axis="y"><p data-line="1">1</p></Scrollable>);
		const element = ref.current!.element!;
		expect(element.classList.contains('antilia-scrollbar-host')).toBe(true);

		ref.current!.scrollTo({ top: 40 });
		expect(element.scrollTop).toBe(40);
		ref.current!.scrollToTop();
		expect(element.scrollTop).toBe(0);
		expect(ref.current!.scrollToElement('[data-line="1"]')).toBe(true);
		expect(ref.current!.scrollToElement('[data-line="2"]')).toBe(false);
	});

	it('removes the bars when unmounted', () => {
		const { container, unmount } = render(<Scrollable axis="x" />);
		unmount();

		expect(container.innerHTML).toBe('');
		expect(Scrollbar.instances).toEqual([]);
	});
});
