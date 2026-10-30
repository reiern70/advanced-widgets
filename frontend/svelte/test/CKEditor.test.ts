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

import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { CKEditor, setHost, standaloneHost } from '@antilia/core';

import BoundCKEditor from './BoundCKEditor.svelte';
import { fakeBuild, type FakeEditor } from './fake-ckeditor.js';

/** A moment, for the state reported after a delay of 0. */
const tick = () => new Promise((resolve) => setTimeout(resolve, 20));

function editorOf(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-ck-editor')!;
}

/**
 * @returns the CKEditor in the container, once it started
 */
async function started(container: HTMLElement): Promise<FakeEditor> {
	const core = CKEditor.of(editorOf(container))!;
	await core.ready;
	flushSync();
	return core.editor as unknown as FakeEditor;
}

afterEach(() => {
	cleanup();
	setHost(standaloneHost);
});

describe('CKEditor', () => {
	it('renders an element the core starts CKEditor in, with the license key and the text',
		async () => {
			const build = fakeBuild();
			const onready = vi.fn();
			const { container } = render(BoundCKEditor, { build, onready });
			const fake = await started(container);
			const element = editorOf(container);

			expect(element.id).toBe('e');
			expect(element.className.split(' ')).toEqual(['rich', 'antilia-ck-editor',
				'antilia-resizable', 'antilia-resizable-vertical']);
			expect(fake.config.licenseKey).toBe('GPL');
			expect(fake.config.language).toBe('de');
			expect(fake.config.label).toBe('Notes');
			expect(fake.getData()).toBe('<p>Hello world</p>');
			expect(onready).toHaveBeenCalledWith(fake);
			expect(container.querySelector('.modified')?.textContent).toBe('false');
		});

	it('binds the text the user changes, and whether it has changes not saved', async () => {
		const { container } = render(BoundCKEditor, { build: fakeBuild() });
		const fake = await started(container);
		fake.type('<p>Oh, Hello world</p>');
		flushSync();
		expect(container.querySelector('output')?.textContent).toBe('<p>Oh, Hello world</p>');
		expect(container.querySelector('.modified')?.textContent).toBe('true');
		expect(editorOf(container).getAttribute('data-modified')).toBe('true');
	});

	it('shows the text the page gives it, and takes the text saved', async () => {
		const { container } = render(BoundCKEditor, { build: fakeBuild() });
		const fake = await started(container);
		await fireEvent.click(container.querySelector('.reset')!);
		flushSync();
		expect(fake.getData()).toBe('<p>Reset</p>');
		expect(container.querySelector('.modified')?.textContent).toBe('true');

		const other = render(BoundCKEditor, { build: fakeBuild(), saved: '<p>Other</p>' });
		await started(other.container);
		expect(other.container.querySelector('.modified')?.textContent).toBe('true');
		await other.rerender({ saved: '<p>Hello world</p>' });
		flushSync();
		expect(other.container.querySelector('.modified')?.textContent).toBe('false');
	});

	it('binds the state the user leaves it in, and calls back', async () => {
		const onsave = vi.fn();
		const onfocus = vi.fn();
		const onblur = vi.fn();
		const onselectionchange = vi.fn();
		const { container } = render(BoundCKEditor, { build: fakeBuild(), onsave, onfocus, onblur,
			onselectionchange });
		const fake = await started(container);
		fake.setFocused(true);
		fake.select(0, 2, 5);
		fake.press('Ctrl+S');
		await tick();
		flushSync();
		expect(onfocus).toHaveBeenCalled();
		expect(onselectionchange).toHaveBeenCalledWith({ anchor: 3, head: 6 });
		expect(onsave).toHaveBeenCalledWith('<p>Hello world</p>');
		expect(container.querySelector('.head')?.textContent).toBe('6');
		fake.setFocused(false);
		expect(onblur).toHaveBeenCalledWith('<p>Hello world</p>');
	});

	it('takes other options, and starts CKEditor anew for another configuration', async () => {
		const build = fakeBuild();
		const { container, rerender } = render(BoundCKEditor, { build });
		const first = await started(container);

		await rerender({ readOnly: true, count: true });
		expect(first.isReadOnly).toBe(true);
		expect(editorOf(container).querySelector('.antilia-ck-editor-status')).not.toBeNull();
		expect(build.editors).toHaveLength(1);

		await rerender({ toolbar: ['bold', 'italic'] });
		const second = await started(container);
		expect(first.destroyed).toBe(true);
		expect(second.config.toolbar).toMatchObject({ items: ['bold', 'italic'] });
		expect(second.isReadOnly).toBe(true);

		await rerender({ plugins: ['Bold'] });
		const third = await started(container);
		expect(second.destroyed).toBe(true);
		expect(third.config.plugins).toContain(build.Bold);
	});

	it('tells what a paste lost, to onpastecleaned and in a line while asked', async () => {
		const onpastecleaned = vi.fn();
		const { container, rerender } = render(BoundCKEditor, { build: fakeBuild(),
			onpastecleaned });
		const fake = await started(container);
		expect(container.querySelector('.antilia-rich-text-notice')).toBeNull();
		await rerender({ pasteNotice: true });
		flushSync();
		fake.paste('<p>b</p><object data="x.swf"></object>', '<p>b</p>');
		await Promise.resolve();
		expect(onpastecleaned).toHaveBeenCalledWith({ elements: { object: 1 }, attributes: {} });
		expect(container.querySelector('.antilia-rich-text-notice')!.textContent)
			.toBe('Part of the paste was left out: embedded content.');

		await rerender({ pasteNotice: false });
		flushSync();
		expect(container.querySelector('.antilia-rich-text-notice')).toBeNull();
	});

	it('posts the text under a name, in a hidden text area', async () => {
		const { container } = render(BoundCKEditor, { build: fakeBuild(), name: 'notes' });
		const fake = await started(container);
		const field = container.querySelector<HTMLTextAreaElement>('textarea[name="notes"]')!;
		expect(field.hidden).toBe(true);
		fake.type('<p>b</p>');
		expect(field.value).toBe('<p>b</p>');
	});

	it('keeps the classes of the core when Svelte writes others', async () => {
		const { container, rerender } = render(BoundCKEditor, { build: fakeBuild() });
		await started(container);
		await rerender({ className: 'other' });
		flushSync();
		const element = container.querySelector('.other')!;
		expect(element.classList.contains('antilia-ck-editor')).toBe(true);
	});

	it('destroys CKEditor when unmounted', async () => {
		const { container, unmount } = render(BoundCKEditor, { build: fakeBuild() });
		const fake = await started(container);
		unmount();
		expect(fake.destroyed).toBe(true);
	});

	it('tells why CKEditor could not start, without a build', async () => {
		const error = vi.fn();
		setHost({ ...standaloneHost, log: { error } });
		const onerror = vi.fn();
		const { container } = render(BoundCKEditor, { name: 'notes', onerror });
		expect(onerror).toHaveBeenCalledWith(expect.any(Error));
		expect(error).toHaveBeenCalledWith('CKEditor could not start', expect.any(Error));
		// the text area shows, to type the text in
		expect(container.querySelector<HTMLTextAreaElement>('textarea')?.hidden).toBe(false);
	});
});
