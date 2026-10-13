/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, LOOK_CONTEXT, Popover } from '../src/index.js';
import PopoverButtonHost from './PopoverButtonHost.svelte';

afterEach(() => {
	Popover.closeAll();
	cleanup();
});

function popover(): HTMLElement | null {
	return document.querySelector('.antilia-popover');
}

describe('PopoverButton', () => {
	it('opens a popover pointing at it, with its content, which Svelte goes on updating', async () => {
		const opened = vi.fn();
		const closed = vi.fn();
		const { container } = render(PopoverButtonHost, { popover: { title: 'Details',
			body: 'Some text', placement: 'left-bottom', design: 'neon', onClose: closed },
		onopen: opened });
		const button = container.querySelector<HTMLButtonElement>('button.mine')!;
		expect(button.type).toBe('button');
		const holder = container.querySelector('span[hidden] > .antilia-popover-content')!;
		expect(holder.querySelector('.count')).not.toBeNull();

		await fireEvent.click(button);

		const element = popover()!;
		expect(element.className).toContain('antilia-popover-neon');
		expect(element.getAttribute('data-align')).toBe('end');
		expect(element.querySelector('.antilia-popover-title')!.textContent).toBe('Details');
		const body = element.querySelector('.antilia-popover-body')!;
		expect(body.firstChild!.textContent).toBe('Some text');
		expect(body.lastChild).toBe(holder);
		expect(opened).toHaveBeenCalledWith(Popover.current());
		// events of the content still reach Svelte out of its tree
		await fireEvent.click(element.querySelector('.count')!);
		expect(element.querySelector('.count')!.textContent).toBe('1');

		await fireEvent.click(button);
		expect(popover()).toBeNull();
		expect(closed).toHaveBeenCalledTimes(1);
		expect(container.querySelector('span[hidden] > .antilia-popover-content')).toBe(holder);
		expect(holder.querySelector('.count')!.textContent).toBe('1');
	});

	it('opens a popover of text only', async () => {
		const { container } = render(PopoverButtonHost, { popover: { body: 'Text' }, rich: false });
		expect(container.querySelector('span[hidden]')).toBeNull();
		await fireEvent.click(container.querySelector('button')!);
		expect(popover()!.querySelector('.antilia-popover-body')!.textContent).toBe('Text');
	});

	it('closes its popover as it goes away', async () => {
		const { container, rerender } = render(PopoverButtonHost, { popover: { body: 'Text' } });
		await fireEvent.click(container.querySelector('button')!);
		expect(popover()).not.toBeNull();
		await rerender({ shown: false });
		expect(popover()).toBeNull();
		expect(document.querySelector('.count')).toBeNull();
		expect(container.querySelector('#before')!.nextElementSibling!.id).toBe('after');
	});

	it('opens its popover in the look of its context', async () => {
		const { container } = render(PopoverButtonHost, { props: { popover: { title: 'Details' } },
			context: new Map([[LOOK_CONTEXT, BOOTSTRAP_LOOK]]) });
		await fireEvent.click(container.querySelector('button.mine')!);

		const element = popover()!;
		expect(element.classList.contains('antilia-look-bootstrap')).toBe(true);
		expect(element.querySelector('.antilia-popover-body')!.classList.contains('popover-body'))
			.toBe(true);
	});
});
