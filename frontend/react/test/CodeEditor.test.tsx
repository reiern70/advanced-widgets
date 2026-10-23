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
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, CODE_EDITOR_KIT, CodeEditor, LookProvider,
	type CodeEditorHandle } from '../src/index.js';

beforeAll(() => {
	// jsdom lays nothing out: CodeMirror measures nothing
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => ({ left: 0, top: 0, right: 0, bottom: 0,
		width: 0, height: 0, x: 0, y: 0, toJSON: () => ({}) }) as DOMRect;
});

/** A moment, for a grammar loading and the state reported after a delay of 0. */
const tick = () => act(() => new Promise((resolve) => setTimeout(resolve, 20)));

function editorOf(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-code-editor')!;
}

afterEach(cleanup);

describe('CodeEditor', () => {
	it('renders an element the core makes an editor of, resizable by its bottom edge', () => {
		const ref = createRef<CodeEditorHandle>();
		const { container } = render(<StrictMode><CodeEditor ref={ref} id="e" className="code"
			value="let a = 1;" label="Script" /></StrictMode>);
		const element = editorOf(container);

		expect(element.id).toBe('e');
		expect(element.className.split(' ')).toEqual(['code', 'antilia-code-editor',
			'antilia-resizable', 'antilia-resizable-vertical']);
		expect(element.querySelectorAll('.cm-editor')).toHaveLength(1);
		expect(element.querySelectorAll('.antilia-resizable-handle')).toHaveLength(1);
		expect(element.querySelector('.cm-content')?.getAttribute('aria-label')).toBe('Script');
		expect(element.getAttribute('data-modified')).toBe('false');
		expect(ref.current?.getValue()).toBe('let a = 1;');
		expect(ref.current?.editor?.element).toBe(element);
		expect(element.querySelector('textarea')).toBeNull();
	});

	it('highlights the text in the grammar of its language, in the look of the page', async () => {
		const { container } = render(<LookProvider look={BOOTSTRAP_LOOK}>
			<CodeEditor value="let a = 1;" language="javascript" />
		</LookProvider>);
		await tick();

		expect(editorOf(container).classList.contains('form-control')).toBe(true);
		expect(container.querySelector('.tok-keyword')?.textContent).toBe('let');
	});

	it('tells of the user changing the text, and of changes not saved', () => {
		const onChange = vi.fn();
		const onModifiedChange = vi.fn();
		const ref = createRef<CodeEditorHandle>();
		const { container } = render(<CodeEditor ref={ref} value="one" onChange={onChange}
			onModifiedChange={onModifiedChange} />);

		act(() => ref.current!.editor!.view.dispatch({ changes: { from: 3, insert: '!' },
			userEvent: 'input.type' }));
		expect(onChange).toHaveBeenCalledWith('one!');
		expect(onModifiedChange).toHaveBeenCalledWith(true);
		expect(ref.current?.isModified()).toBe(true);
		expect(editorOf(container).getAttribute('data-modified')).toBe('true');

		act(() => ref.current!.markSaved());
		expect(ref.current?.isModified()).toBe(false);
	});

	it('shows the text the page gives it, and the text it saved', () => {
		const ref = createRef<CodeEditorHandle>();
		const { rerender } = render(<CodeEditor ref={ref} value="one" />);

		rerender(<CodeEditor ref={ref} value="two" />);
		expect(ref.current?.getValue()).toBe('two');
		expect(ref.current?.isModified()).toBe(true);
		rerender(<CodeEditor ref={ref} value="two" saved="two" />);
		expect(ref.current?.isModified()).toBe(false);
	});

	it('follows a page keeping the text in its state, the cursor staying', () => {
		const ref = createRef<CodeEditorHandle>();
		function Host() {
			const [text, setText] = useState('ab');
			return <><CodeEditor ref={ref} value={text} onChange={setText} />
				<output>{text}</output></>;
		}
		const { container } = render(<Host />);

		act(() => ref.current!.editor!.view.dispatch({ changes: { from: 1, insert: 'x' },
			selection: { anchor: 2 }, userEvent: 'input.type' }));
		expect(container.querySelector('output')?.textContent).toBe('axb');
		expect(ref.current?.getValue()).toBe('axb');
		expect(ref.current?.getState()?.selection?.ranges[0].head).toBe(2);
	});

	it('starts in the state given and reports the state the user left it in', async () => {
		const onStateChange = vi.fn();
		const onSelectionChange = vi.fn();
		const ref = createRef<CodeEditorHandle>();
		render(<CodeEditor ref={ref} value="first line" stateDelay={0}
			state={{ selection: { ranges: [{ anchor: 2, head: 5 }], main: 0 }, height: 200 }}
			onStateChange={onStateChange} onSelectionChange={onSelectionChange} />);

		expect(ref.current?.getState()).toEqual(expect.objectContaining({
			selection: { ranges: [{ anchor: 2, head: 5 }], main: 0 }, height: 200 }));
		act(() => ref.current!.editor!.view.dispatch({ selection: { anchor: 6 },
			userEvent: 'select' }));
		expect(onSelectionChange).toHaveBeenCalledWith({ ranges: [{ anchor: 6, head: 6 }],
			main: 0 });
		await tick();
		expect(onStateChange).toHaveBeenCalledWith(expect.objectContaining({
			selection: { ranges: [{ anchor: 6, head: 6 }], main: 0 } }),
		{ leaving: false, changed: false });

		act(() => ref.current!.setState({ selection: { ranges: [{ anchor: 1, head: 1 }],
			main: 0 } }));
		expect(ref.current?.getState()?.selection?.ranges[0].head).toBe(1);
	});

	it('tells the page to save on Ctrl+S, with the latest callback', () => {
		const first = vi.fn();
		const onSave = vi.fn();
		const { container, rerender } = render(<CodeEditor value="x" onSave={first} />);
		rerender(<CodeEditor value="x" onSave={onSave} />);

		fireEvent.keyDown(container.querySelector('.cm-content')!, { key: 's', ctrlKey: true });
		expect(onSave).toHaveBeenCalledWith('x');
		expect(first).not.toHaveBeenCalled();
	});

	it('tells of the focus and of the size the user gave it', async () => {
		const onFocus = vi.fn();
		const onBlur = vi.fn();
		const onResize = vi.fn();
		const ref = createRef<CodeEditorHandle>();
		const { container } = render(<><CodeEditor ref={ref} value="x" onFocus={onFocus}
			onBlur={onBlur} onResize={onResize} minHeight={100} /><input id="other" /></>);

		act(() => ref.current!.focus());
		await tick();
		expect(onFocus).toHaveBeenCalledTimes(1);
		act(() => (container.querySelector('#other') as HTMLInputElement).focus());
		await tick();
		expect(onBlur).toHaveBeenCalledWith('x');

		fireEvent.keyDown(container.querySelector('.antilia-resizable-handle')!,
			{ key: 'ArrowDown' });
		expect(onResize).toHaveBeenCalledWith({ width: null, height: 100 });
	});

	it('takes other options as the page changes them', async () => {
		const ref = createRef<CodeEditorHandle>();
		const { container, rerender } = render(<CodeEditor ref={ref} value="let a;" />);
		expect(container.querySelector('.cm-lineNumbers')).not.toBeNull();

		const extension = CODE_EDITOR_KIT.EditorView.contentAttributes.of({ 'data-own': 'yes' });
		rerender(<CodeEditor ref={ref} value="let a;" lineNumbers={false} folding={false}
			readOnly language="js" resizable={false} extensions={[extension]}
			className="other" />);
		await tick();
		const element = editorOf(container);
		expect(container.querySelector('.cm-gutters')).toBeNull();
		expect(element.classList.contains('antilia-code-editor-readonly')).toBe(true);
		expect(element.classList.contains('other')).toBe(true);
		expect(element.querySelector('.antilia-resizable-handle')).toBeNull();
		expect(container.querySelector('.tok-keyword')?.textContent).toBe('let');
		expect(container.querySelector('.cm-content')?.getAttribute('data-own')).toBe('yes');
	});

	it('posts the text in a hidden text area under a name', () => {
		const ref = createRef<CodeEditorHandle>();
		const { container } = render(<form><CodeEditor ref={ref} name="script" value="a" />
		</form>);
		const field = container.querySelector('textarea')!;

		expect(field.name).toBe('script');
		expect(field.hidden).toBe(true);
		act(() => ref.current!.editor!.view.dispatch({ changes: { from: 1, insert: 'b' },
			userEvent: 'input.type' }));
		expect(field.value).toBe('ab');
		expect(new FormData(container.querySelector('form')!).get('script')).toBe('ab');
	});

	it('takes itself away when unmounted', () => {
		const ref = createRef<CodeEditorHandle>();
		const { container, unmount } = render(<CodeEditor ref={ref} value="gone" />);
		const editor = ref.current!.editor!;

		unmount();
		expect(container.querySelector('.cm-editor')).toBeNull();
		expect(editor.view.dom.isConnected).toBe(false);
		expect(ref.current).toBeNull();
	});

	it('answers its handle before the editor is made, and after', () => {
		const ref = createRef<CodeEditorHandle>();
		const { unmount } = render(<CodeEditor ref={ref} value="v" />);
		const handle = ref.current!;
		unmount();

		expect(handle.editor).toBeNull();
		expect(handle.getValue()).toBe('v');
		expect(handle.getState()).toBeNull();
		expect(handle.isModified()).toBe(false);
		handle.focus();
		handle.setState({});
		handle.markSaved();
	});
});
