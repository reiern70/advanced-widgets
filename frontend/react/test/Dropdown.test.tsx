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

import { StrictMode, useState } from 'react';
import { act, cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { Dropdown, type DropdownOption } from '../src/index.js';

const FRUITS: DropdownOption[] = ['Apple', 'Banana', 'Cherry'].map((name) => ({
	value: name.toLowerCase(),
	label: name
}));

function toggle(container: HTMLElement): HTMLButtonElement {
	return container.querySelector('.antilia-dropdown-toggle')!;
}

function items(container: HTMLElement): HTMLLIElement[] {
	return Array.from(container.querySelectorAll('.antilia-dropdown-option'));
}

function chips(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('.antilia-chip'),
		(chip) => chip.textContent ?? '');
}

afterEach(cleanup);

describe('Dropdown', () => {
	it('draws the core drop-down inside the wrapper React renders', () => {
		const { container } = render(<Dropdown options={FRUITS} value="banana" onChange={() => {}} />);

		const wrapper = container.firstElementChild!;
		expect(wrapper.className).toBe('antilia-dropdown');
		expect(wrapper.firstElementChild?.tagName).toBe('SELECT');
		expect(toggle(container).textContent).toBe('Banana');
		expect(items(container).map((item) => item.textContent)).toEqual(['Apple', 'Banana', 'Cherry']);
	});

	it('shows the value once the options arrive after it, as when they are loaded', () => {
		const { container, rerender } = render(<Dropdown options={[]} value="banana"
			onChange={() => {}} />);

		rerender(<Dropdown options={FRUITS} value="banana" onChange={() => {}} />);

		expect(toggle(container).textContent).toBe('Banana');
		expect(container.querySelector('select')?.value).toBe('banana');
	});

	it('shows the values of a multiple drop-down once its options arrive', () => {
		const { container, rerender } = render(<Dropdown multiple options={[]} value={['cherry']}
			onChange={() => {}} />);

		rerender(<Dropdown multiple options={FRUITS} value={['cherry']} onChange={() => {}} />);

		expect(chips(container)).toEqual(['Cherry']);
	});

	it('reports a choice to onChange and shows the value it gets back', () => {
		function Controlled() {
			const [fruit, setFruit] = useState('apple');
			return <><Dropdown options={FRUITS} value={fruit} onChange={setFruit} /><p>{fruit}</p></>;
		}
		const { container } = render(<Controlled />);

		act(() => {
			toggle(container).click();
		});
		act(() => {
			items(container)[2].click();
		});

		expect(container.querySelector('p')?.textContent).toBe('cherry');
		expect(toggle(container).textContent).toBe('Cherry');
		expect(container.querySelector('select')?.value).toBe('cherry');
	});

	it('keeps the choice when the parent does not take it', () => {
		const onChange = vi.fn();
		const { container } = render(<Dropdown options={FRUITS} value="apple" onChange={onChange} />);

		act(() => {
			items(container)[1].click();
		});

		expect(onChange).toHaveBeenCalledWith('banana');
		expect(toggle(container).textContent).toBe('Apple');
	});

	it('toggles many values and keeps the list open while the parent re-renders', () => {
		function Multiple() {
			const [fruits, setFruits] = useState<string[]>([]);
			// new option objects on every render
			return <Dropdown multiple options={FRUITS.map((fruit) => ({ ...fruit }))} value={fruits}
				onChange={setFruits} />;
		}
		const { container } = render(<Multiple />);

		act(() => {
			toggle(container).click();
		});
		act(() => {
			items(container)[0].click();
		});
		act(() => {
			items(container)[2].click();
		});

		expect(chips(container)).toEqual(['Apple', 'Cherry']);
		expect(container.querySelector('.antilia-dropdown-menu')?.hasAttribute('hidden')).toBe(false);
		expect(Array.from(container.querySelector('select')!.selectedOptions, (o) => o.value))
			.toEqual(['apple', 'cherry']);
	});

	it('removes a value with the x of its chip, labelled with the text given', () => {
		const onChange = vi.fn();
		const { container } = render(<Dropdown multiple options={FRUITS} value={['apple', 'cherry']}
			onChange={onChange} remove="Drop {0}" />);

		const remove = container.querySelector<HTMLElement>('.antilia-chip-remove')!;
		expect(remove.getAttribute('aria-label')).toBe('Drop Apple');
		act(() => {
			remove.click();
		});

		expect(onChange).toHaveBeenCalledWith(['cherry']);
	});

	it('works without a value, keeping the choice in the select', () => {
		const onChange = vi.fn();
		const { container } = render(<Dropdown options={FRUITS} onChange={onChange}
			placeholder="Choose" />);

		act(() => {
			items(container)[1].click();
		});

		expect(onChange).toHaveBeenCalledWith('banana');
		expect(toggle(container).textContent).toBe('Banana');
	});

	it('draws options and texts that change', () => {
		const { container, rerender } = render(<Dropdown options={FRUITS} filterThreshold={10} />);
		expect(container.querySelector('.antilia-dropdown-filter')).toBeNull();

		rerender(<Dropdown options={[...FRUITS, { value: 'date', label: 'Date' }]}
			filterThreshold={0} filter="Search" />);

		expect(items(container).map((item) => item.textContent)).toContain('Date');
		expect(container.querySelector<HTMLInputElement>('.antilia-dropdown-filter')?.placeholder)
			.toBe('Search');
	});

	it('shows a disabled select as disabled', () => {
		const { container, rerender } = render(<Dropdown options={FRUITS} />);
		rerender(<Dropdown options={FRUITS} disabled />);

		expect(toggle(container).disabled).toBe(true);
	});

	it('survives the double effects of StrictMode with one button', () => {
		const { container } = render(<StrictMode><Dropdown options={FRUITS} /></StrictMode>);

		expect(container.querySelectorAll('.antilia-dropdown-toggle').length).toBe(1);
		act(() => {
			toggle(container).click();
		});
		expect(container.querySelector('.antilia-dropdown-open')).not.toBeNull();
	});

	it('leaves nothing behind when unmounted', () => {
		const { container, unmount } = render(<Dropdown options={FRUITS} />);
		const listeners = vi.spyOn(document, 'removeEventListener');
		unmount();

		expect(container.innerHTML).toBe('');
		expect(listeners).toHaveBeenCalledWith('mousedown', expect.any(Function));
	});

	it('closes on Escape with the keyboard', () => {
		const { container } = render(<Dropdown options={FRUITS} />);
		act(() => {
			fireEvent.keyDown(toggle(container), { key: 'ArrowDown' });
		});
		const list = container.querySelector('.antilia-dropdown-list')!;
		act(() => {
			fireEvent.keyDown(list, { key: 'Escape' });
		});

		expect(container.querySelector('.antilia-dropdown-open')).toBeNull();
		expect(document.activeElement).toBe(toggle(container));
	});
});
