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

import { createRef, StrictMode, useState } from 'react';
import { act, cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { setHost, standaloneHost } from '@antilia/core';

import { BOOTSTRAP_LOOK, CKEditor, LookProvider, type CKEditorHandle } from '../src/index.js';
import { fakeBuild, type FakeEditor } from './fake-ckeditor.js';

/** A moment, for the state reported after a delay of 0. */
const tick = () => act(() => new Promise((resolve) => setTimeout(resolve, 20)));

function editorOf(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-ck-editor')!;
}

/**
 * @returns the CKEditor of a handle, once it started
 */
async function started(ref: React.RefObject<CKEditorHandle | null>): Promise<FakeEditor> {
	await act(async () => {
		await ref.current!.editor!.ready;
	});
	return ref.current!.editor!.editor as unknown as FakeEditor;
}

afterEach(() => {
	cleanup();
	setHost(standaloneHost);
	delete (globalThis as { CKEDITOR?: unknown }).CKEDITOR;
});

describe('CKEditor', () => {
	it('renders an element the core starts CKEditor in, with the license key and the text',
		async () => {
			const build = fakeBuild();
			const ref = createRef<CKEditorHandle>();
			const onReady = vi.fn();
			const { container } = render(<StrictMode><CKEditor ref={ref} build={build} id="e"
				className="rich" licenseKey="GPL" language="de" value="<p>Hello</p>" label="Notes"
				onReady={onReady} /></StrictMode>);
			const fake = await started(ref);
			const element = editorOf(container);

			expect(element.id).toBe('e');
			expect(element.className.split(' ')).toEqual(['rich', 'antilia-ck-editor',
				'antilia-resizable', 'antilia-resizable-vertical']);
			expect(fake.config.licenseKey).toBe('GPL');
			expect(fake.config.language).toBe('de');
			expect(fake.config.label).toBe('Notes');
			expect(fake.getData()).toBe('<p>Hello</p>');
			expect(element.querySelectorAll('.ck-editor')).toHaveLength(1);
			expect(onReady).toHaveBeenCalledWith(fake);
			expect(ref.current?.getValue()).toBe('<p>Hello</p>');
			expect(element.getAttribute('data-modified')).toBe('false');
			expect(element.querySelector('textarea')).toBeNull();
			// StrictMode made one and destroyed it, then made the one shown
			expect(build.editors.filter((editor) => !editor.destroyed)).toEqual([fake]);
		});

	it('uses the global CKEDITOR of the browser build without one given', async () => {
		const build = fakeBuild();
		(globalThis as { CKEDITOR?: unknown }).CKEDITOR = build;
		const ref = createRef<CKEditorHandle>();
		render(<CKEditor ref={ref} licenseKey="GPL" value="<p>a</p>" />);
		expect(await started(ref)).toBe(build.editors[0]);
	});

	it('takes the look of the page', () => {
		const { container } = render(<LookProvider look={BOOTSTRAP_LOOK}>
			<CKEditor build={fakeBuild()} value="<p>a</p>" count />
		</LookProvider>);
		expect(editorOf(container).querySelector('.antilia-ck-editor-status')?.classList
			.contains('form-text')).toBe(true);
	});

	it('tells of the user changing the text, and of changes not saved', async () => {
		const onChange = vi.fn();
		const onModifiedChange = vi.fn();
		const ref = createRef<CKEditorHandle>();
		render(<CKEditor ref={ref} build={fakeBuild()} value="<p>one</p>" onChange={onChange}
			onModifiedChange={onModifiedChange} />);
		const fake = await started(ref);

		act(() => fake.type('<p>one!</p>'));
		expect(onChange).toHaveBeenCalledWith('<p>one!</p>');
		expect(onModifiedChange).toHaveBeenCalledWith(true);
		expect(ref.current?.isModified()).toBe(true);

		act(() => ref.current!.markSaved());
		expect(ref.current?.isModified()).toBe(false);
		expect(onModifiedChange).toHaveBeenLastCalledWith(false);
	});

	it('shows the text the page gives it, and takes the text saved', async () => {
		function Page() {
			const [value, setValue] = useState('<p>one</p>');
			const [saved, setSaved] = useState<string | undefined>(undefined);
			return <>
				<CKEditor ref={ref} build={build} value={value} onChange={setValue} saved={saved} />
				<button type="button" className="reset" onClick={() => setValue('<p>two</p>')}>reset</button>
				<button type="button" className="save" onClick={() => setSaved(value)}>save</button>
			</>;
		}
		const build = fakeBuild();
		const ref = createRef<CKEditorHandle>();
		const { container } = render(<Page />);
		const fake = await started(ref);

		fireEvent.click(container.querySelector('.reset')!);
		expect(fake.getData()).toBe('<p>two</p>');
		expect(ref.current?.isModified()).toBe(true);

		fireEvent.click(container.querySelector('.save')!);
		expect(ref.current?.isModified()).toBe(false);
		expect(build.editors).toHaveLength(1);
	});

	it('takes other options, and starts CKEditor anew for another configuration', async () => {
		const build = fakeBuild();
		const ref = createRef<CKEditorHandle>();
		const { rerender } = render(<CKEditor ref={ref} build={build} licenseKey="GPL"
			value="<p>a</p>" />);
		const first = await started(ref);
		expect(first.config.toolbar).toMatchObject({ items: expect.arrayContaining(['bold']) });

		rerender(<CKEditor ref={ref} build={build} licenseKey="GPL" value="<p>a</p>" readOnly
			count />);
		expect(first.isReadOnly).toBe(true);
		expect(ref.current!.editor!.element.querySelector('.antilia-ck-editor-status')).not.toBeNull();
		expect(build.editors).toHaveLength(1);

		rerender(<CKEditor ref={ref} build={build} licenseKey="GPL" value="<p>a</p>" readOnly
			count toolbar={['bold', 'italic']} />);
		const second = await started(ref);
		expect(first.destroyed).toBe(true);
		expect(second).not.toBe(first);
		expect(second.config.toolbar).toMatchObject({ items: ['bold', 'italic'] });
		expect(second.isReadOnly).toBe(true);
		expect(second.getData()).toBe('<p>a</p>');

		const plugins = ['Bold'];
		rerender(<CKEditor ref={ref} build={build} licenseKey="GPL" value="<p>a</p>" readOnly
			count toolbar={['bold', 'italic']} plugins={plugins} />);
		const third = await started(ref);
		expect(second.destroyed).toBe(true);
		expect(third.config.plugins).toContain(build.Bold);
	});

	it('calls back the latest callbacks: focus, blur, selection, Ctrl+S and the state',
		async () => {
			const onFocus = vi.fn();
			const onBlur = vi.fn();
			const onSave = vi.fn();
			const onSelectionChange = vi.fn();
			const onStateChange = vi.fn();
			const ref = createRef<CKEditorHandle>();
			render(<CKEditor ref={ref} build={fakeBuild()} value="<p>Hello</p>" stateDelay={0}
				onFocus={onFocus} onBlur={onBlur} onSave={onSave}
				onSelectionChange={onSelectionChange} onStateChange={onStateChange} />);
			const fake = await started(ref);

			act(() => ref.current!.focus());
			expect(onFocus).toHaveBeenCalledTimes(1);
			act(() => fake.select(0, 1, 3));
			expect(onSelectionChange).toHaveBeenCalledWith({ anchor: 2, head: 4 });
			act(() => fake.press('Ctrl+S'));
			expect(onSave).toHaveBeenCalledWith('<p>Hello</p>');
			await tick();
			expect(onStateChange).toHaveBeenCalledWith(expect.objectContaining({
				selection: { anchor: 2, head: 4 } }), { leaving: false, changed: false });
			expect(ref.current?.getState()?.selection).toEqual({ anchor: 2, head: 4 });

			act(() => ref.current!.setState({ selection: { anchor: 1, head: 1 } }));
			expect(ref.current?.getState()?.selection).toEqual({ anchor: 1, head: 1 });

			act(() => fake.setFocused(false));
			expect(onBlur).toHaveBeenCalledWith('<p>Hello</p>');
		});

	it('posts the text under a name, in a hidden text area', async () => {
		const ref = createRef<CKEditorHandle>();
		const { container } = render(<form><CKEditor ref={ref} build={fakeBuild()} name="notes"
			value="<p>a</p>" /></form>);
		const fake = await started(ref);
		const field = container.querySelector<HTMLTextAreaElement>('textarea[name="notes"]')!;
		expect(field.hidden).toBe(true);
		act(() => fake.type('<p>b</p>'));
		expect(field.value).toBe('<p>b</p>');
	});

	it('keeps the classes of the core when React writes others', async () => {
		const ref = createRef<CKEditorHandle>();
		const { container, rerender } = render(<CKEditor ref={ref} build={fakeBuild()}
			className="one" value="<p>a</p>" />);
		await started(ref);
		rerender(<CKEditor ref={ref} build={fakeBuild()} className="two" value="<p>a</p>" />);
		expect(editorOf(container)?.classList.contains('two')).toBe(true);
		expect(container.querySelector('.two')?.classList.contains('antilia-ck-editor')).toBe(true);
	});

	it('destroys CKEditor when unmounted', async () => {
		const ref = createRef<CKEditorHandle>();
		const { unmount } = render(<CKEditor ref={ref} build={fakeBuild()} value="<p>a</p>" />);
		const fake = await started(ref);
		unmount();
		expect(fake.destroyed).toBe(true);
		expect(ref.current).toBeNull();
	});

	it('tells why CKEditor could not start, without a build', async () => {
		const error = vi.fn();
		setHost({ ...standaloneHost, log: { error } });
		const onError = vi.fn();
		const ref = createRef<CKEditorHandle>();
		const { container } = render(<CKEditor ref={ref} name="notes" value="<p>a</p>"
			onError={onError} />);
		await act(async () => {
			await ref.current!.editor!.ready.catch(() => undefined);
		});
		expect(onError).toHaveBeenCalledWith(expect.any(Error));
		expect(error).toHaveBeenCalledWith('CKEditor could not start', expect.any(Error));
		// the text area shows, to type the text in
		expect(container.querySelector<HTMLTextAreaElement>('textarea')?.hidden).toBe(false);
		expect(ref.current?.getValue()).toBe('<p>a</p>');
	});

	it('answers its handle before the editor is made, or after it went', () => {
		const ref = createRef<CKEditorHandle>();
		const { unmount } = render(<CKEditor ref={ref} build={fakeBuild()} value="<p>a</p>" />);
		const handle = ref.current!;
		unmount();
		expect(handle.editor).toBeNull();
		expect(handle.getValue()).toBe('<p>a</p>');
		expect(handle.getState()).toBeNull();
		expect(handle.isModified()).toBe(false);
		handle.focus();
		handle.setState({});
		handle.markSaved();
	});
});
