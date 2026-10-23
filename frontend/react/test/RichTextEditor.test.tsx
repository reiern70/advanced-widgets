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

import { lezerHighlighter } from '@antilia/core';
import { Slice } from '@tiptap/pm/model';

import { BOOTSTRAP_LOOK, LookProvider, RichTextEditor,
	type RichTextEditorHandle } from '../src/index.js';

beforeAll(() => {
	// jsdom lays nothing out: ProseMirror measures nothing
	Range.prototype.getClientRects = () => [] as unknown as DOMRectList;
	Range.prototype.getBoundingClientRect = () => ({ left: 0, top: 0, right: 0, bottom: 0,
		width: 0, height: 0, x: 0, y: 0, toJSON: () => ({}) }) as DOMRect;
	document.elementFromPoint = () => null;
});

/** A moment, for the state reported after a delay of 0. */
const tick = () => act(() => new Promise((resolve) => setTimeout(resolve, 20)));

function editorOf(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-rich-text-editor')!;
}

/**
 * Types as the user does, at the cursor.
 */
function type(ref: React.RefObject<RichTextEditorHandle | null>, text: string): void {
	const tiptap = ref.current!.editor!.editor;
	act(() => tiptap.view.dispatch(tiptap.state.tr.insertText(text)));
}

afterEach(cleanup);

describe('RichTextEditor', () => {
	it('renders an element the core makes an editor of, with its toolbar', () => {
		const ref = createRef<RichTextEditorHandle>();
		const { container } = render(<StrictMode><RichTextEditor ref={ref} id="e" className="rich"
			value="<p>Hello</p>" label="Notes" /></StrictMode>);
		const element = editorOf(container);

		expect(element.id).toBe('e');
		expect(element.className.split(' ')).toEqual(['rich', 'antilia-rich-text-editor',
			'antilia-resizable', 'antilia-resizable-vertical']);
		expect(element.querySelectorAll('.ProseMirror')).toHaveLength(1);
		expect(element.querySelectorAll('[role="toolbar"]')).toHaveLength(1);
		expect(element.querySelector('.ProseMirror')?.getAttribute('aria-label')).toBe('Notes');
		expect(element.getAttribute('data-modified')).toBe('false');
		expect(ref.current?.getValue()).toBe('<p>Hello</p>');
		expect(ref.current?.editor?.element).toBe(element);
		expect(element.querySelector('textarea')).toBeNull();
	});

	it('takes the look of the page', () => {
		const { container } = render(<LookProvider look={BOOTSTRAP_LOOK}>
			<RichTextEditor value="<p>a</p>" />
		</LookProvider>);
		expect(editorOf(container).classList.contains('form-control')).toBe(true);
	});

	it('tells of the user changing the text, and of changes not saved', () => {
		const onChange = vi.fn();
		const onModifiedChange = vi.fn();
		const ref = createRef<RichTextEditorHandle>();
		render(<RichTextEditor ref={ref} value="<p>one</p>" onChange={onChange}
			onModifiedChange={onModifiedChange} />);

		act(() => ref.current!.editor!.editor.commands.setTextSelection(4));
		type(ref, '!');
		expect(onChange).toHaveBeenCalledWith('<p>one!</p>');
		expect(onModifiedChange).toHaveBeenCalledWith(true);
		expect(ref.current?.isModified()).toBe(true);

		act(() => ref.current!.markSaved());
		expect(ref.current?.isModified()).toBe(false);
	});

	it('shows the text the page gives it, and the text saved', () => {
		function Page() {
			const [value, setValue] = useState('<p>one</p>');
			const [saved, setSaved] = useState<string | undefined>(undefined);
			return <>
				<RichTextEditor value={value} saved={saved} onChange={setValue} />
				<button type="button" className="reset" onClick={() => setValue('<p>two</p>')}>reset</button>
				<button type="button" className="save" onClick={() => setSaved(value)}>save</button>
			</>;
		}
		const { container } = render(<Page />);

		fireEvent.click(container.querySelector('.reset')!);
		expect(editorOf(container).querySelector('.ProseMirror')?.textContent).toBe('two');
		expect(editorOf(container).getAttribute('data-modified')).toBe('true');
		fireEvent.click(container.querySelector('.save')!);
		expect(editorOf(container).getAttribute('data-modified')).toBe('false');
	});

	it('starts in the state given and reports the one the user leaves it in', async () => {
		const onStateChange = vi.fn();
		const onSelectionChange = vi.fn();
		const ref = createRef<RichTextEditorHandle>();
		render(<RichTextEditor ref={ref} value="<p>Hello world</p>" stateDelay={0}
			state={{ selection: { anchor: 2, head: 4 } }} onStateChange={onStateChange}
			onSelectionChange={onSelectionChange} />);

		expect(ref.current?.getState()?.selection).toEqual({ anchor: 2, head: 4 });
		act(() => ref.current!.editor!.editor.commands.setTextSelection(6));
		expect(onSelectionChange).toHaveBeenCalledWith({ anchor: 6, head: 6 });
		await tick();
		expect(onStateChange.mock.calls.at(-1)![0].selection).toEqual({ anchor: 6, head: 6 });

		act(() => ref.current!.setState({ selection: { anchor: 1, head: 1 } }));
		expect(ref.current?.getState()?.selection).toEqual({ anchor: 1, head: 1 });
	});

	it('calls back on Ctrl+S, the focus and the size', async () => {
		const onSave = vi.fn();
		const onFocus = vi.fn();
		const onBlur = vi.fn();
		const onResize = vi.fn();
		const ref = createRef<RichTextEditorHandle>();
		const { container } = render(<><RichTextEditor ref={ref} value="<p>a</p>" onSave={onSave}
			onFocus={onFocus} onBlur={onBlur} onResize={onResize} minHeight={100} /><input id="other" />
		</>);

		fireEvent.keyDown(container.querySelector('.ProseMirror')!, { key: 's', ctrlKey: true });
		expect(onSave).toHaveBeenCalledWith('<p>a</p>');
		act(() => ref.current!.focus());
		await tick();
		expect(onFocus).toHaveBeenCalledTimes(1);
		act(() => (container.querySelector('#other') as HTMLInputElement).focus());
		await tick();
		expect(onBlur).toHaveBeenCalledWith('<p>a</p>');
		fireEvent.keyDown(container.querySelector('.antilia-resizable-handle')!, { key: 'ArrowDown' });
		expect(onResize).toHaveBeenCalledWith({ width: null, height: 100 });
	});

	it('takes other options, keeping the classes of the core', () => {
		const { container, rerender } = render(<RichTextEditor value="<p>a</p>" className="one" />);
		rerender(<RichTextEditor value="<p>a</p>" className="two" readOnly toolbar={[['bold']]}
			format="markdown" features={['bold']} count />);
		const element = editorOf(container);

		expect(element.classList.contains('two')).toBe(true);
		expect(element.classList.contains('antilia-rich-text-editor-readonly')).toBe(true);
		expect(element.classList.contains('antilia-resizable')).toBe(true);
		expect(element.querySelector<HTMLElement>('[role="toolbar"]')?.hidden).toBe(true);
		expect(element.querySelector('.antilia-rich-text-status')?.textContent)
			.toBe('1 character · 1 word');
	});

	it('posts the text in a hidden text area under a name', () => {
		const ref = createRef<RichTextEditorHandle>();
		const { container } = render(<form><RichTextEditor ref={ref} name="body"
			value="<p>Hi</p>" /></form>);
		const field = container.querySelector<HTMLTextAreaElement>('textarea[name="body"]')!;
		expect(field.hidden).toBe(true);
		expect(field.value).toBe('<p>Hi</p>');
		type(ref, 'x');
		expect(field.value).toBe('<p>xHi</p>');
	});

	it('takes extensions of its own', () => {
		const ref = createRef<RichTextEditorHandle>();
		const extensions = ['missing'];
		const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);
		render(<RichTextEditor ref={ref} value="<p>a</p>" extensions={extensions} />);
		expect(error).toHaveBeenCalled();
		expect(ref.current?.getValue()).toBe('<p>a</p>');
		error.mockRestore();
	});

	it('highlights its code blocks with the grammars of the code editor, or none', async () => {
		await lezerHighlighter.load('java');
		const value = '<pre><code class="language-java">class A {}</code></pre>';
		const { container, rerender } = render(<RichTextEditor value={value} highlight />);
		const keyword = () => container.querySelector('pre span.tok-keyword')?.textContent;
		const codeBlock = () => container.querySelector('[data-tool="codeBlock"]')!;
		expect(keyword()).toBe('class');
		expect(codeBlock().getAttribute('aria-haspopup')).toBe('menu');

		rerender(<RichTextEditor value={value} codeLanguages={['java']} />);
		expect(keyword()).toBeUndefined();
		expect(container.querySelector('pre')?.getAttribute('data-antilia-language')).toBe('Java');
		rerender(<RichTextEditor value={value} codeLanguages={false} />);
		expect(codeBlock().getAttribute('aria-haspopup')).toBeNull();
	});

	it('uploads the images the user drops, by onUpload', async () => {
		const onUpload = vi.fn(() => Promise.resolve('/bird.png'));
		const onChange = vi.fn();
		const ref = createRef<RichTextEditorHandle>();
		const { rerender } = render(<RichTextEditor ref={ref} value="<p>a</p>" onUpload={onUpload}
			onChange={onChange} uploadMaxSize={1024} />);
		const view = ref.current!.editor!.editor.view;
		vi.spyOn(view, 'posAtCoords').mockReturnValue(null);
		const drop = (size = 1) => {
			const event = new Event('drop', { cancelable: true });
			Object.assign(event, { clientX: 0, clientY: 0, dataTransfer: { types: ['Files'],
				files: [new File([new Uint8Array(size)], 'bird.png', { type: 'image/png' })],
				getData: () => '' } });
			return view.someProp('handleDrop', (handle) => handle(view, event as DragEvent,
				Slice.empty, false));
		};
		act(() => {
			ref.current!.editor!.editor.commands.setTextSelection(2);
		});
		expect(drop(2048)).toBe(true);
		expect(onUpload).not.toHaveBeenCalled();
		expect(drop()).toBe(true);
		await tick();
		expect(onUpload).toHaveBeenCalledTimes(1);
		expect(onChange).toHaveBeenLastCalledWith('<p>a</p><img src="/bird.png">');

		rerender(<RichTextEditor ref={ref} value="<p>a</p>" />);
		expect(drop()).toBeFalsy();
	});

	it('answers its handle before the editor is made, and after it went', () => {
		const ref = createRef<RichTextEditorHandle>();
		const { unmount } = render(<RichTextEditor ref={ref} value="<p>a</p>" />);
		const handle = ref.current!;
		unmount();
		expect(handle.editor).toBeNull();
		expect(handle.getState()).toBeNull();
		expect(handle.isModified()).toBe(false);
		expect(handle.getValue()).toBe('<p>a</p>');
	});
});
