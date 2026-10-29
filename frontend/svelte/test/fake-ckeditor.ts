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

/*
 * A build of CKEditor for the tests, without CKEditor: a ClassicEditor whose editors have what the
 * core's glue uses of them, their model a root of blocks of plain text, read from the HTML.
 */

import type { CKEditorBuild, CKEditorInstance, CKEditorViewWriter } from '@antilia/core';

type Callback = (...args: any[]) => void;

class Emitter {
	private readonly handlers = new Map<string, Callback[]>();

	on(event: string, callback: Callback): void {
		this.handlers.set(event, [...(this.handlers.get(event) ?? []), callback]);
	}

	fire(event: string, ...args: unknown[]): void {
		(this.handlers.get(event) ?? []).forEach((callback) => callback({}, ...args));
	}
}

class FakeText {
	constructor(readonly data: string, readonly startOffset: number) {}

	get offsetSize(): number {
		return this.data.length;
	}

	is(type: string): boolean {
		return type === '$text';
	}
}

class FakeElement {
	constructor(readonly children: (FakeText | FakeElement)[], readonly startOffset: number | null) {}

	get offsetSize(): number {
		return 1;
	}

	get maxOffset(): number {
		return this.children.reduce((size, child) => size + child.offsetSize, 0);
	}

	is(type: string): boolean {
		return type === 'element';
	}

	getChildren(): Iterable<FakeText | FakeElement> {
		return this.children;
	}

	getChild(index: number): FakeText | FakeElement | null {
		return this.children[index] ?? null;
	}

	offsetToIndex(offset: number): number {
		let start = 0;
		for (let index = 0; index < this.children.length; index++) {
			start += this.children[index]!.offsetSize;
			if (offset < start) {
				return index;
			}
		}
		return this.children.length;
	}
}

class FakePosition {
	constructor(readonly path: readonly number[]) {}

	isBefore(other: FakePosition): boolean {
		for (let level = 0; level < Math.min(this.path.length, other.path.length); level++) {
			if (this.path[level] !== other.path[level]) {
				return this.path[level]! < other.path[level]!;
			}
		}
		return this.path.length < other.path.length;
	}
}

/**
 * @returns the root of a model of HTML: a block of plain text for each element
 */
function rootOf(html: string): FakeElement {
	const template = document.createElement('template');
	template.innerHTML = html;
	const blocks = Array.from(template.content.children).map((block, index) => {
		const text = block.textContent ?? '';
		return new FakeElement(text ? [new FakeText(text, 0)] : [], index);
	});
	return new FakeElement(blocks, null);
}

/** An editor of the fake build. */
export class FakeEditor {
	readonly config: Record<string, unknown>;

	readonly source: HTMLElement;

	readonly locks = new Set<string>();

	readonly keys = new Map<string, (data: unknown, cancel: () => void) => void>();

	readonly checks: ((context: { endsWith(query: string): boolean },
		attribute: string) => boolean | undefined)[] = [];

	destroyed = false;

	private html = '';

	private root = rootOf('');

	private readonly documentEmitter = new Emitter();

	private readonly selectionEmitter = new Emitter();

	private readonly focusEmitter = new Emitter();

	private readonly viewEmitter = new Emitter();

	private anchor = new FakePosition([0, 0]);

	private head = new FakePosition([0, 0]);

	readonly ui: CKEditorInstance['ui'];

	readonly model: CKEditorInstance['model'];

	constructor(source: HTMLElement, config: Record<string, unknown>) {
		this.source = source;
		this.config = config;
		const element = document.createElement('div');
		element.className = 'ck ck-editor';
		const editable = document.createElement('div');
		editable.className = 'ck-editor__editable';
		element.append(editable);
		source.after(element);
		const focusEmitter = this.focusEmitter;
		this.ui = {
			element,
			view: { editable: { element: editable } },
			focusTracker: { on: (event, callback) => focusEmitter.on(event, callback), isFocused: false }
		};
		const editor = this;
		const selectionEmitter = this.selectionEmitter;
		const documentEmitter = this.documentEmitter;
		this.model = {
			document: {
				on: (event, callback) => documentEmitter.on(event, callback),
				selection: {
					on: (event, callback) => selectionEmitter.on(event, callback),
					get anchor() {
						return editor.anchor;
					},
					get focus() {
						return editor.head;
					}
				},
				getRoot: () => this.root as never
			},
			schema: {
				addAttributeCheck: (check) => {
					this.checks.push(check);
				},
				getNearestSelectionRange: (position) => ({ start: position, end: position })
			},
			change: (callback) => callback({
				setSelection: (range, options) => {
					this.anchor = (options?.backward ? range.end : range.start) as FakePosition;
					this.head = (options?.backward ? range.start : range.end) as FakePosition;
					selectionEmitter.fire('change:range');
				}
			}),
			createPositionFromPath: (_root, path) => new FakePosition(path),
			createRange: (start, end) => ({ start, end: end ?? start })
		};
	}

	readonly data = {
		parse: (data: string) => data,
		stringify: (fragment: unknown) => String(fragment)
	};

	readonly editing = {
		view: {
			focus: () => this.setFocused(true),
			document: { on: (event: string, callback: Callback) => this.viewEmitter.on(event, callback),
				getRoot: () => this.ui.view.editable.element },
			change: (callback: (writer: CKEditorViewWriter) => void) => callback({
				setAttribute: (key, value, element) => (element as HTMLElement).setAttribute(key, value),
				removeAttribute: (key, element) => (element as HTMLElement).removeAttribute(key)
			})
		}
	};

	readonly conversion = { for: () => ({ add: () => undefined }) };

	readonly keystrokes = {
		set: (keystroke: string, callback: (data: unknown, cancel: () => void) => void) => {
			this.keys.set(keystroke, callback);
		}
	};

	get isReadOnly(): boolean {
		return this.locks.size > 0;
	}

	getData(): string {
		return this.html;
	}

	setData(data: string): void {
		this.html = data;
		this.root = rootOf(data);
		this.anchor = new FakePosition([0, 0]);
		this.head = new FakePosition([0, 0]);
		this.documentEmitter.fire('change:data');
	}

	/** The user changes the text. */
	type(html: string): void {
		this.setData(html);
	}

	/** The user selects in a block, from an offset to another. */
	select(block: number, anchor: number, head = anchor): void {
		this.anchor = new FakePosition([block, anchor]);
		this.head = new FakePosition([block, head]);
		this.selectionEmitter.fire('change:range');
	}

	/** The focus comes to the editor, or goes. */
	setFocused(focused: boolean): void {
		this.focusEmitter.fire('change:isFocused', 'isFocused', focused);
	}

	/** The user presses keys CKEditor has a handler of, Ctrl+S for example. */
	press(keystroke: string): void {
		this.keys.get(keystroke)?.({}, () => undefined);
	}

	enableReadOnlyMode(lock: string): void {
		this.locks.add(lock);
	}

	disableReadOnlyMode(lock: string): void {
		this.locks.delete(lock);
	}

	destroy(): Promise<unknown> {
		this.destroyed = true;
		this.ui.element?.remove();
		return Promise.resolve();
	}
}

/** A build of CKEditor, and the editors it made. */
export interface FakeBuild extends CKEditorBuild {
	readonly editors: FakeEditor[];
}

/**
 * @returns a build of CKEditor making fake editors, with the plugins of the features by their names
 */
export function fakeBuild(): FakeBuild {
	const editors: FakeEditor[] = [];
	const plugin = (name: string) => ({ pluginName: name });
	const names = ['Essentials', 'Paragraph', 'Autoformat', 'PasteFromOffice', 'Heading', 'Bold',
		'Italic', 'Underline', 'Strikethrough', 'Code', 'Superscript', 'Subscript', 'Link',
		'AutoLink', 'List', 'TodoList', 'BlockQuote', 'CodeBlock', 'HorizontalLine', 'Table',
		'TableToolbar', 'Image', 'ImageToolbar', 'ImageCaption', 'ImageTextAlternative',
		'ImageInsertViaUrl', 'AutoImage', 'Alignment', 'Markdown'];
	return {
		...Object.fromEntries(names.map((name) => [name, plugin(name)])),
		editors,
		ClassicEditor: {
			create: (element: HTMLElement, config: Record<string, unknown>) => {
				const editor = new FakeEditor(element, config);
				editors.push(editor);
				return Promise.resolve(editor as unknown as CKEditorInstance);
			}
		}
	};
}
