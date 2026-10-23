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

import { StrictMode, useState } from 'react';
import { cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { SplitPanel } from '../src/index.js';

function split(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-split')!;
}

function divider(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-split-divider')!;
}

function shares(container: HTMLElement): [string, string] {
	const style = split(container).style;
	return [style.getPropertyValue('--antilia-split-first'),
		style.getPropertyValue('--antilia-split-second')];
}

afterEach(cleanup);

describe('SplitPanel', () => {
	it('renders the panes and the divider, which the core makes a separator', () => {
		const { container } = render(<SplitPanel id="s" position={30} label="Resize">
			<p>One</p>
			<p>Two</p>
		</SplitPanel>);

		expect(split(container).className).toBe('antilia-split antilia-split-horizontal');
		expect(Array.from(split(container).children, (child) => child.className)).toEqual([
			'antilia-split-pane antilia-split-first', 'antilia-split-divider',
			'antilia-split-pane antilia-split-second']);
		expect(container.querySelector('.antilia-split-first')?.textContent).toBe('One');
		expect(container.querySelector('.antilia-split-second')?.textContent).toBe('Two');
		expect(divider(container).getAttribute('role')).toBe('separator');
		expect(divider(container).getAttribute('aria-label')).toBe('Resize');
		expect(divider(container).getAttribute('aria-valuenow')).toBe('30');
		expect(shares(container)).toEqual(['30', '70']);
	});

	it('calls back when the user moves the divider, with the latest callbacks', () => {
		const first = vi.fn();
		const onResized = vi.fn();
		const { container, rerender } = render(<SplitPanel onResized={first}>
			<p>One</p>
			<p>Two</p>
		</SplitPanel>);
		rerender(<SplitPanel onResized={onResized}>
			<p>One</p>
			<p>Two</p>
		</SplitPanel>);

		fireEvent.keyDown(divider(container), { key: 'ArrowRight' });

		expect(onResized).toHaveBeenCalledWith(55);
		expect(first).not.toHaveBeenCalled();
		expect(shares(container)).toEqual(['55', '45']);
	});

	it('changes its orientation, keeping the position the user gave it', () => {
		const { container, rerender } = render(<SplitPanel>
			<p>One</p>
			<p>Two</p>
		</SplitPanel>);
		fireEvent.keyDown(divider(container), { key: 'End' });

		rerender(<SplitPanel orientation="vertical" resizable={false} className="mine">
			<p>One</p>
			<p>Two</p>
		</SplitPanel>);

		expect(split(container).className).toBe(
			'antilia-split antilia-split-vertical antilia-split-fixed mine');
		expect(divider(container).getAttribute('aria-orientation')).toBe('horizontal');
		expect(divider(container).getAttribute('aria-disabled')).toBe('true');
		expect(shares(container)).toEqual(['100', '0']);
	});

	it('moves the divider when the position changes', () => {
		function Controlled() {
			const [position, setPosition] = useState(40);
			return <>
				<SplitPanel position={position} onResized={(next) => setPosition(Number(next))}>
					<p>One</p>
					<p>Two</p>
				</SplitPanel>
				<button type="button" onClick={() => setPosition(20)}>reset</button>
				<output>{position}</output>
			</>;
		}
		const { container } = render(<Controlled />);

		fireEvent.keyDown(divider(container), { key: 'ArrowLeft' });
		expect(container.querySelector('output')?.textContent).toBe('35');

		fireEvent.click(container.querySelector('button')!);
		expect(shares(container)).toEqual(['20', '80']);
	});

	it('survives the double effects of StrictMode', () => {
		const { container } = render(<StrictMode>
			<SplitPanel position={25}>
				<p>One</p>
				<p>Two</p>
			</SplitPanel>
		</StrictMode>);

		expect(split(container).classList.contains('antilia-split')).toBe(true);
		fireEvent.keyDown(divider(container), { key: 'ArrowRight' });
		expect(shares(container)).toEqual(['30', '70']);
	});

	it('collapses a pane with the buttons on the divider, keeping it through a new render', () => {
		const onCollapse = vi.fn();
		const { container, rerender } = render(<SplitPanel collapsible collapseFirstLabel="Hide"
			onCollapse={onCollapse}>
			<p>One</p>
			<p>Two</p>
		</SplitPanel>);
		const button = container.querySelector<HTMLElement>('.antilia-split-collapse-first')!;
		expect(button.title).toBe('Hide');

		fireEvent.click(button);
		expect(onCollapse).toHaveBeenCalledWith('first');
		expect(split(container).classList.contains('antilia-split-collapsed-first')).toBe(true);

		// React writes its classes over the element's, and the core puts the collapsed one back
		rerender(<SplitPanel collapsible className="mine" onCollapse={onCollapse}>
			<p>One</p>
			<p>Two</p>
		</SplitPanel>);
		expect(split(container).className).toBe('antilia-split antilia-split-horizontal ' +
			'antilia-split-collapsible mine antilia-split-collapsed-first');
	});

	it('collapses the pane the page says', () => {
		const { container, rerender } = render(<SplitPanel collapsible collapsed="second">
			<p>One</p>
			<p>Two</p>
		</SplitPanel>);
		expect(split(container).classList.contains('antilia-split-collapsed-second')).toBe(true);

		rerender(<SplitPanel collapsible collapsed={null}>
			<p>One</p>
			<p>Two</p>
		</SplitPanel>);
		expect(split(container).classList.contains('antilia-split-collapsed-second')).toBe(false);
	});

	it('keeps the first pane in pixels', () => {
		const onResized = vi.fn();
		const { container } = render(<SplitPanel position="250px" step="10px" min="100px"
			onResized={onResized}>
			<p>One</p>
			<p>Two</p>
		</SplitPanel>);

		expect(split(container).classList.contains('antilia-split-pixels')).toBe(true);
		expect(split(container).style.getPropertyValue('--antilia-split-first-size'))
			.toBe('250px');
		fireEvent.keyDown(divider(container), { key: 'ArrowRight' });
		expect(onResized).toHaveBeenCalledWith('260px');
	});
});
