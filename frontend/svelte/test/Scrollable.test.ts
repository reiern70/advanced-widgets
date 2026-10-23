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

import { Scrollbar } from '@antilia/core';

import { Scrollable } from '../src/index.js';

afterEach(cleanup);

function tracks(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('.antilia-scrollbar-track'),
		(track) => track.className);
}

describe('Scrollable', () => {
	it('draws the bars of the axes asked for inside the wrapper Svelte renders', () => {
		const { container } = render(Scrollable, { axis: 'y', class: 'list' });

		const wrapper = container.querySelector('.antilia-scrollbar')!;
		expect(wrapper.firstElementChild?.className).toBe('antilia-scrollbar-host list');
		expect(tracks(container)).toEqual(['antilia-scrollbar-track antilia-scrollbar-track--y']);
	});

	it('draws other bars when the axis changes', async () => {
		const { container, rerender } = render(Scrollable, { axis: 'y' });
		await rerender({ axis: 'both' });

		expect(tracks(container).length).toBe(2);
	});

	it('scrolls through the functions of the component', () => {
		const { component, container } = render(Scrollable, { axis: 'y' });
		const element = container.querySelector<HTMLElement>('.antilia-scrollbar-host')!;
		expect(component.getElement()).toBe(element);

		component.scrollTo({ top: 40 });
		expect(element.scrollTop).toBe(40);
		component.scrollToTop();
		expect(element.scrollTop).toBe(0);
		expect(component.scrollToElement('p')).toBe(false);
	});

	it('removes the bars when unmounted', () => {
		const { container, unmount } = render(Scrollable, { axis: 'x' });
		unmount();

		expect(container.querySelector('.antilia-scrollbar')).toBeNull();
		expect(Scrollbar.instances).toEqual([]);
	});
});
