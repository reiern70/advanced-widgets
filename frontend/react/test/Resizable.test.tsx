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

import { createRef } from 'react';
import { cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, LookProvider, Resizable, type ResizableHandle } from '../src/index.js';

function box(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-resizable')!;
}

function handle(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-resizable-handle')!;
}

afterEach(cleanup);

describe('Resizable', () => {
	it('renders a div the core adds a handle to, the size the page\'s', () => {
		const ref = createRef<ResizableHandle>();
		const { container } = render(<LookProvider look={BOOTSTRAP_LOOK}><Resizable ref={ref}
			id="r" className="notes" label="Resize the notes"><p>Text</p></Resizable>
		</LookProvider>);

		expect(box(container).id).toBe('r');
		expect(box(container).className.split(' ')).toEqual(['notes', 'antilia-resizable',
			'antilia-resizable-both']);
		expect(box(container).firstElementChild?.textContent).toBe('Text');
		expect(handle(container).getAttribute('aria-label')).toBe('Resize the notes');
		expect(ref.current?.element).toBe(box(container));
		expect(ref.current?.getSize()).toEqual({ width: null, height: null });
	});

	it('calls back as the user resizes it, with the latest callbacks', () => {
		const first = vi.fn();
		const onResized = vi.fn();
		const onResize = vi.fn();
		const { container, rerender } = render(<Resizable axis="vertical" onResized={first} />);
		rerender(<Resizable axis="vertical" onResized={onResized} onResize={onResize} />);

		fireEvent.keyDown(handle(container), { key: 'ArrowDown' });
		expect(onResize).toHaveBeenCalledWith({ width: null, height: 32 });
		expect(onResized).toHaveBeenCalledWith({ width: null, height: 32 });
		expect(first).not.toHaveBeenCalled();
	});

	it('takes the size and the options the page gives it', () => {
		const ref = createRef<ResizableHandle>();
		const { container, rerender } = render(<Resizable ref={ref} width={300} height={200} />);
		expect(box(container).style.getPropertyValue('--antilia-resizable-width')).toBe('300px');

		rerender(<Resizable ref={ref} width={300} height={250} maxHeight={240}
			className="other" />);
		expect(ref.current?.getSize()).toEqual({ width: 300, height: 240 });
		expect(box(container).classList.contains('other')).toBe(true);
		expect(box(container).classList.contains('antilia-resizable-sized-height')).toBe(true);

		rerender(<Resizable ref={ref} axis="horizontal" enabled={false} />);
		expect(ref.current?.getSize()).toEqual({ width: 300, height: null });
		expect(handle(container).hidden).toBe(true);
	});

	it('takes itself away when unmounted', () => {
		const ref = createRef<ResizableHandle>();
		const { unmount } = render(<Resizable ref={ref} />);
		const handleOf = ref.current!;
		unmount();

		expect(handleOf.element).toBeNull();
		expect(handleOf.getSize()).toEqual({ width: null, height: null });
	});
});
