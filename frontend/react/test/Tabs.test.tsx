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

import { createRef, useState } from 'react';
import { act, cleanup, fireEvent, render, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { LookProvider, Tab, Tabs, type TabsHandle } from '../src/index.js';

function tab(container: HTMLElement, name: string): HTMLElement {
	return container.querySelector('.antilia-tab[data-tab="' + name + '"]')!;
}

function button(container: HTMLElement, name: string): HTMLButtonElement {
	return tab(container, name).querySelector('.antilia-tab-button')!;
}

function panel(container: HTMLElement, name: string): HTMLElement {
	return container.querySelector('.antilia-tabs-panel[data-tab="' + name + '"]')!;
}

afterEach(() => {
	cleanup();
	document.querySelectorAll('.antilia-tabs-confirm').forEach((confirm) => confirm.remove());
});

describe('Tabs', () => {
	it('renders the tabs and their panels, the first one shown', () => {
		const { container } = render(<Tabs id="t" label="Documents" className="mine">
			<Tab id="one" title="One" icon={<svg />}><p>First</p></Tab>
			<Tab id="two" title="Two" closable panelClassName="padded"><p>Second</p></Tab>
			{null}
		</Tabs>);

		const root = container.querySelector('#t')!;
		expect(root.className).toContain('antilia-tabs antilia-tabs-horizontal antilia-tabs-tabs mine');
		expect(root.querySelector('.antilia-tabs-list')!.getAttribute('role')).toBe('tablist');
		expect(root.querySelector('.antilia-tabs-list')!.getAttribute('aria-label')).toBe('Documents');
		expect(button(container, 'one').getAttribute('aria-selected')).toBe('true');
		expect(button(container, 'one').querySelector('.antilia-tab-icon svg')).not.toBeNull();
		expect(button(container, 'one').textContent).toBe('One');
		expect(panel(container, 'one').hidden).toBe(false);
		expect(panel(container, 'two').hidden).toBe(true);
		expect(panel(container, 'two').className).toBe('antilia-tabs-panel padded');
		expect(tab(container, 'two').querySelector('.antilia-tab-close')).not.toBeNull();
		expect(tab(container, 'one').querySelector('.antilia-tab-close')).toBeNull();
	});

	it('selects the tab the props say, and tells the page what the user selects', () => {
		const onSelect = vi.fn();
		const { container, rerender } = render(<Tabs selected="two" onSelect={onSelect}
			orientation="vertical" design="pills">
			<Tab id="one" title="One">First</Tab>
			<Tab id="two" title="Two">Second</Tab>
		</Tabs>);
		expect(panel(container, 'two').hidden).toBe(false);
		expect(container.querySelector('.antilia-tabs-vertical.antilia-tabs-pills')).not.toBeNull();

		fireEvent.click(button(container, 'one'));
		expect(onSelect).toHaveBeenCalledWith('one');
		expect(panel(container, 'one').hidden).toBe(false);

		rerender(<Tabs selected="one" onSelect={onSelect} design="underline">
			<Tab id="one" title="One">First</Tab>
			<Tab id="two" title="Two">Second</Tab>
		</Tabs>);
		expect(panel(container, 'one').hidden).toBe(false);
		expect(container.querySelector('.antilia-tabs-horizontal.antilia-tabs-underline'))
			.not.toBeNull();
	});

	it('lets the page take a closed tab away, and add one it selects', async () => {
		function Documents() {
			const [names, setNames] = useState(['one', 'two']);
			const [selected, setSelected] = useState('one');
			return <>
				<button type="button" className="add" onClick={() => {
					setNames([...names, 'three']);
					setSelected('three');
				}}>Add</button>
				<Tabs selected={selected} onSelect={setSelected}
					onClose={(name) => setNames(names.filter((each) => each !== name))}>
					{names.map((name) => <Tab key={name} id={name} title={name} closable>{name}</Tab>)}
				</Tabs>
			</>;
		}
		const { container } = render(<Documents />);

		fireEvent.click(tab(container, 'one').querySelector('.antilia-tab-close')!);
		await waitFor(() => expect(tab(container, 'one')).toBeNull());
		expect(button(container, 'two').getAttribute('aria-selected')).toBe('true');

		fireEvent.click(container.querySelector('.add')!);
		expect(button(container, 'three').getAttribute('aria-selected')).toBe('true');
		expect(panel(container, 'three').hidden).toBe(false);
		expect(panel(container, 'two').hidden).toBe(true);
		expect(tab(container, 'three').querySelector('.antilia-tab-close')).not.toBeNull();
	});

	it('asks before closing a tab the page says has changes, or as the page asks', async () => {
		const onClose = vi.fn();
		const confirmClose = vi.fn(() => false);
		const { container, rerender } = render(<Tabs onClose={onClose}>
			<Tab id="one" title="Notes" closable modified>Notes</Tab>
		</Tabs>);
		expect(tab(container, 'one').querySelector('.antilia-tab-mark')).not.toBeNull();
		expect(panel(container, 'one').getAttribute('data-modified')).toBe('true');

		fireEvent.click(tab(container, 'one').querySelector('.antilia-tab-close')!);
		expect(document.querySelector('.antilia-tabs-confirm .modal-dialog-title')?.textContent)
			.toBe('Close Notes?');
		fireEvent.click(document.querySelector('[data-confirm="discard"]')!);
		await waitFor(() => expect(onClose).toHaveBeenCalledWith('one'));

		rerender(<Tabs onClose={onClose} confirmClose={confirmClose}>
			<Tab id="two" title="Draft" closable modified>Draft</Tab>
		</Tabs>);
		fireEvent.click(tab(container, 'two').querySelector('.antilia-tab-close')!);
		await waitFor(() => expect(confirmClose).toHaveBeenCalledWith('two', 'Draft'));
		expect(onClose).toHaveBeenCalledTimes(1);
		expect(document.querySelector('.antilia-tabs-confirm')).toBeNull();
	});

	it('renders the actions of a tab before its close button', () => {
		const info = vi.fn();
		const { container } = render(<Tabs selected="two">
			<Tab id="one" title="One" closable
				actions={<button type="button" className="info" onClick={info}>i</button>}>First</Tab>
			<Tab id="two" title="Two">Second</Tab>
		</Tabs>);

		const parts = Array.from(tab(container, 'one').children, (child) => child.className);
		expect(parts).toEqual(['antilia-tab-button', 'antilia-tab-actions', 'antilia-tab-close']);
		fireEvent.click(container.querySelector('.info')!);
		expect(info).toHaveBeenCalled();
		expect(panel(container, 'two').hidden).toBe(false);
	});

	it('gives a ref the tabs to change by their names', async () => {
		const ref = createRef<TabsHandle>();
		const { container } = render(<Tabs ref={ref}>
			<Tab id="one" title="One" closable><input defaultValue="" className="field" /></Tab>
			<Tab id="two" title="Two">Second</Tab>
		</Tabs>);

		expect(ref.current!.tabs).not.toBeNull();
		act(() => ref.current!.select('two'));
		expect(ref.current!.selected()).toBe('two');
		expect(ref.current!.isModified('one')).toBe(false);
		(container.querySelector('.field') as HTMLInputElement).value = 'typed';
		expect(ref.current!.isModified('one')).toBe(true);
		act(() => ref.current!.close('one'));
		expect(tab(container, 'one').hidden).toBe(true);
		act(() => ref.current!.open('one'));
		expect(ref.current!.selected()).toBe('one');
		await expect(ref.current!.requestClose('two')).resolves.toBe(true);
	});

	it('renders its parts in the look of its provider', () => {
		const { container } = render(<LookProvider look="bootstrap">
			<Tabs design="underline">
				<Tab id="one" title="One" closable>First</Tab>
				<Tab id="two" title="Two">Second</Tab>
			</Tabs>
		</LookProvider>);

		expect(container.querySelector('.antilia-tabs-list')!.className)
			.toBe('antilia-tabs-list nav nav-underline');
		expect(tab(container, 'one').className).toBe('antilia-tab nav-item');
		expect(tab(container, 'one').querySelector('.antilia-tab-close')!.className)
			.toBe('antilia-tab-close btn-close');
	});
});
