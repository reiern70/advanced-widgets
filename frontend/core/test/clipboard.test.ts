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
		expect(cell.classList.contains('antilia-clipboard-host')).toBe(true);
		expect(cell.querySelectorAll('.antilia-clipboard-copied').length).toBe(1);
		expect(document.getElementById('antilia-clipboard-status')?.textContent).toBe('Copied');

		vi.advanceTimersByTime(Clipboard.iconMillis);
		expect(cell.querySelector('.antilia-clipboard-copied')).toBeNull();
		expect(cell.classList.contains('antilia-clipboard-host')).toBe(false);
	});

	it('copies the data-antilia-copy of a clicked element once installed', async () => {
		document.body.innerHTML = '<p data-antilia-copy="one"><b>click</b></p>' +
			'<p data-antilia-copy="two" data-antilia-copy-on="dblclick">twice</p>';
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

	it('copies only for the elements attached, past those inside them that were not', async () => {
		document.body.innerHTML = '<p id="own" data-antilia-copy="own"><b>own</b>' +
			'<i data-antilia-copy="inner">inner</i></p>' +
			'<p id="other" data-antilia-copy="other">x</p>';
		const own = document.getElementById('own')!;

		document.getElementById('other')!.click();
		own.click();
		await settle();
		expect(written).toEqual([]);

		const detach = Clipboard.attach(own);
		document.getElementById('other')!.click();
		document.querySelector('b')!.click();
		document.querySelector('i')!.click();
		await settle();
		expect(written).toEqual(['own', 'own']);

		detach();
		own.click();
		await settle();
		expect(written).toEqual(['own', 'own']);
		expect(Clipboard.attach(null)).toBeTypeOf('function');
	});

	it('copies for the elements inside a container attached, also those rendered later',
		async () => {
			document.body.innerHTML = '<div id="table">' +
				'<span data-antilia-copy="one">one</span></div>' +
				'<span id="outside" data-antilia-copy="outside">outside</span>';
			const table = document.getElementById('table')!;
			const detach = Clipboard.attachWithin(table);
			table.insertAdjacentHTML('beforeend', '<span data-antilia-copy="two" ' +
				'data-antilia-copy-on="dblclick"><b>two</b></span>');

			table.querySelector('span')!.click();
			document.getElementById('outside')!.click();
			table.querySelector('b')!.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
			await settle();
			expect(written).toEqual(['one', 'two']);

			detach();
			table.querySelector('span')!.click();
			await settle();
			expect(written).toEqual(['one', 'two']);
			expect(Clipboard.attachWithin(undefined)).toBeTypeOf('function');
		});

	it('logs a failed copy through the host', async () => {
		Object.defineProperty(navigator, 'clipboard', {
			configurable: true,
			value: { writeText: () => Promise.reject(new Error('denied')) }
		});
		const error = vi.fn();
		setHost({ ...standaloneHost, log: { error } });
		document.body.innerHTML = '<p data-antilia-copy="x">x</p>';
		const uninstall = Clipboard.install();

		document.querySelector('p')!.click();
		await settle();
		uninstall();

		expect(error).toHaveBeenCalledWith('Wicket.Clipboard: copying failed', expect.any(Error));
	});
});
