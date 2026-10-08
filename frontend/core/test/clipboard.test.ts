/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { Clipboard, setHost, standaloneHost } from '../src/index.js';

let written: string[];

beforeEach(() => {
	written = [];
	Object.defineProperty(navigator, 'clipboard', {
		configurable: true,
		value: { writeText: (text: string) => (written.push(text), Promise.resolve()) }
	});
	vi.useFakeTimers();
});

afterEach(() => {
	vi.useRealTimers();
	setHost();
	document.body.innerHTML = '';
});

async function settle(): Promise<void> {
	await Promise.resolve();
	await Promise.resolve();
}

describe('Clipboard', () => {
	it('copies, shows the check icon for a while and announces it', async () => {
		document.body.innerHTML = '<span id="cell">text</span>';
		const cell = document.getElementById('cell')!;

		await Clipboard.copy(42, cell);

		expect(written).toEqual(['42']);
		expect(cell.classList.contains('wicket-clipboard-host')).toBe(true);
		expect(cell.querySelectorAll('.wicket-clipboard-copied').length).toBe(1);
		expect(document.getElementById('wicket-clipboard-status')?.textContent).toBe('Copied');

		vi.advanceTimersByTime(Clipboard.iconMillis);
		expect(cell.querySelector('.wicket-clipboard-copied')).toBeNull();
		expect(cell.classList.contains('wicket-clipboard-host')).toBe(false);
	});

	it('copies the data-wicket-copy of a clicked element once installed', async () => {
		document.body.innerHTML = '<p data-wicket-copy="one"><b>click</b></p>' +
			'<p data-wicket-copy="two" data-wicket-copy-on="dblclick">twice</p>';
		const uninstall = Clipboard.install();
		const again = Clipboard.install();

		document.querySelector('b')!.click();
		document.querySelectorAll('p')[1].click();
		document.querySelectorAll('p')[1].dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		await settle();
		expect(written).toEqual(['one', 'two']);

		again();
		document.querySelector('b')!.click();
		await settle();
		expect(written).toEqual(['one', 'two', 'one']);

		uninstall();
		document.querySelector('b')!.click();
		await settle();
		expect(written.length).toBe(3);
	});

	it('logs a failed copy through the host', async () => {
		Object.defineProperty(navigator, 'clipboard', {
			configurable: true,
			value: { writeText: () => Promise.reject(new Error('denied')) }
		});
		const error = vi.fn();
		setHost({ ...standaloneHost, log: { error } });
		document.body.innerHTML = '<p data-wicket-copy="x">x</p>';
		const uninstall = Clipboard.install();

		document.querySelector('p')!.click();
		await settle();
		uninstall();

		expect(error).toHaveBeenCalledWith('Wicket.Clipboard: copying failed', expect.any(Error));
	});
});
