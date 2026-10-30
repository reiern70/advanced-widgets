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

import { afterEach, describe, expect, it, vi } from 'vitest';

import { CK_EDITOR_CLASSES, CKEditor, ckEditor, registerRichTextEvent, RICH_TEXT_BUILT_IN_EVENTS,
	RICH_TEXT_EDITOR_EVENTS, RICH_TEXT_EVENT_TRIGGER, setHost, standaloneHost, type CKEditorBuild,
	type CKEditorInstance, type CKEditorViewWriter, type Host } from '../src/index.js';
import { offsetOfPath, pathOfOffset, type CKEditorModelElement, type CKEditorModelPosition,
	type CKEditorModelWriter } from '../src/ck-editor.js';

/*
 * A CKEditor of the tests: what the glue uses of CKEditor, as small as it goes. Its model is a
 * root of blocks, each holding its text; its HTML is what the page gave, with the ids CKEditor
 * gives the items of lists.
 */

type Callback = (...args: any[]) => void;

class Emitter {
	private callbacks = new Map<string, Callback[]>();

	on(event: string, callback: Callback): void {
		this.callbacks.set(event, [...(this.callbacks.get(event) ?? []), callback]);
	}

	fire(event: string, ...args: unknown[]): void {
		(this.callbacks.get(event) ?? []).forEach((callback) => callback({ name: event }, ...args));
	}
}

class FakeText {
	constructor(readonly data: string, readonly parent: FakeElement) {}

	is(type: string): boolean {
		return type === '$text';
	}

	get offsetSize(): number {
		return this.data.length;
	}

	get startOffset(): number {
		return this.parent.offsetOf(this);
	}
}

class FakeElement extends Emitter implements CKEditorModelElement {
	children: (FakeElement | FakeText)[] = [];

	constructor(readonly name: string, readonly parent: FakeElement | null) {
		super();
	}

	is(type: string): boolean {
		return type === 'element' || type === this.name;
	}

	get offsetSize(): number {
		return 1;
	}

	get startOffset(): number | null {
		return this.parent ? this.parent.offsetOf(this) : null;
	}

	get maxOffset(): number {
		return this.children.reduce((size, child) => size + child.offsetSize, 0);
	}

	offsetOf(child: FakeElement | FakeText): number {
		let offset = 0;
		for (const each of this.children) {
			if (each === child) {
				return offset;
			}
			offset += each.offsetSize;
		}
		return offset;
	}

	getChildren(): Iterable<FakeElement | FakeText> {
		return this.children;
	}

	getChild(index: number): FakeElement | FakeText | null {
		return this.children[index] ?? null;
	}

	offsetToIndex(offset: number): number {
		let at = 0;
		for (let index = 0; index < this.children.length; index++) {
			const size = this.children[index]!.offsetSize;
			if (offset < at + size) {
				return index;
			}
			at += size;
		}
		return this.children.length;
	}
}

class Position implements CKEditorModelPosition {
	constructor(readonly path: readonly number[]) {}

	isBefore(other: CKEditorModelPosition): boolean {
		for (let index = 0; index < Math.min(this.path.length, other.path.length); index++) {
			if (this.path[index] !== other.path[index]) {
				return this.path[index]! < other.path[index]!;
			}
		}
		return this.path.length < other.path.length;
	}
}

/**
 * @returns a root of the blocks of some HTML, each with its text, as CKEditor's model has lists:
 *          their items are blocks of the root
 */
function rootOf(html: string): FakeElement {
	const root = new FakeElement('$root', null);
	const template = document.createElement('template');
	template.innerHTML = html;
	const blocks = Array.from(template.content.querySelectorAll('p, h1, h2, h3, li, pre'));
	blocks.forEach((block) => {
		const element = new FakeElement('paragraph', root);
		const text = block.textContent ?? '';
		if (text) {
			element.children.push(new FakeText(text, element));
		}
		root.children.push(element);
	});
	if (!root.children.length) {
		root.children.push(new FakeElement('paragraph', root));
	}
	return root;
}

/** The HTML as CKEditor writes it: '' for nothing, the items of lists with ids. */
function written(html: string): string {
	const trimmed = html.trim();
	return trimmed === '<p></p>' ? '' : trimmed.replace(/<li>/g, '<li data-list-item-id="e1">');
}

/**
 * A command setting a mark of text, as CKEditor's AttributeCommand: execute() fires its event, the
 * listeners of a high priority first, which may stop it; then it sets the mark, or takes it away.
 */
class FakeCommand {
	value = false;
	private readonly first: Callback[] = [];

	constructor(private readonly done: string[], readonly name: string) {}

	on(_event: string, callback: Callback, options?: { priority?: string | number }): void {
		expect(options?.priority).toBe('high');
		this.first.push(callback);
	}

	execute(...args: unknown[]): void {
		let stopped = false;
		const event = { name: 'execute', stop: () => {
			stopped = true;
		} };
		for (const callback of this.first) {
			callback(event, args);
			if (stopped) {
				return;
			}
		}
		const forced = (args[0] as { forceValue?: boolean } | undefined)?.forceValue;
		this.value = forced ?? !this.value;
		this.done.push(this.name + (this.value ? ' on' : ' off'));
	}
}

class FakeEditor implements CKEditorInstance {
	html = '';
	root: FakeElement;
	destroyed = false;
	readonly locks = new Set<string>();
	readonly keys = new Map<string, (data: unknown, cancel: () => void) => void>();
	/** What the commands did, in order, and the changes of the model, the outer ones. */
	readonly done: string[] = [];
	changes = 0;
	private changing = false;
	readonly superscript = new FakeCommand(this.done, 'superscript');
	readonly subscript = new FakeCommand(this.done, 'subscript');
	readonly commands = {
		get: (name: string) => name === 'superscript' ? this.superscript :
			name === 'subscript' ? this.subscript : undefined
	};
	readonly checks: Array<(context: { endsWith(query: string): boolean },
		attribute: string) => boolean | undefined> = [];
	readonly selection = Object.assign(new Emitter(), {
		anchor: new Position([0, 0]) as CKEditorModelPosition | null,
		focus: new Position([0, 0]) as CKEditorModelPosition | null
	});
	readonly modelDocument = Object.assign(new Emitter(), {
		selection: this.selection,
		getRoot: () => this.root
	});
	/** The dispatchers of the downcast, by their groups. */
	readonly downcast = new Emitter();
	readonly conversion = {
		for: (group: string) => ({
			add: (helper: (dispatcher: Emitter) => void) => {
				if (group === 'downcast') {
					helper(this.downcast);
				}
			}
		})
	};
	readonly model = {
		document: this.modelDocument,
		schema: {
			addAttributeCheck: (check: FakeEditor['checks'][number]) => {
				this.checks.push(check);
			},
			getNearestSelectionRange: (position: CKEditorModelPosition) => {
				// between blocks: into the next one, or the last one
				const near = position.path.length > 1 ? position :
					new Position([Math.min(position.path[0]!, this.root.children.length - 1), 0]);
				return { start: near, end: near };
			}
		},
		change: (callback: (writer: CKEditorModelWriter) => void) => {
			// a change in a change is part of it
			const outer = !this.changing;
			this.changes += outer ? 1 : 0;
			this.changing = true;
			try {
				callback({
					setSelection: (range, options) => {
						this.selection.anchor = options?.backward ? range.end : range.start;
						this.selection.focus = options?.backward ? range.start : range.end;
						this.selection.fire('change:range');
					}
				});
			} finally {
				this.changing = !outer;
			}
		},
		createPositionFromPath: (_root: CKEditorModelElement, path: readonly number[]) =>
			new Position(path),
		createRange: (start: CKEditorModelPosition, end?: CKEditorModelPosition) =>
			({ start, end: end ?? start })
	};
	readonly data = {
		parse: (data: string) => ({ data }),
		stringify: (fragment: unknown) => written((fragment as { data: string }).data)
	};
	readonly viewDocument = new Emitter();
	readonly focusTracker = Object.assign(new Emitter(), { isFocused: false });
	readonly editing = {
		view: {
			focus: () => {
				if (!this.focusTracker.isFocused) {
					this.focusTracker.isFocused = true;
					this.focusTracker.fire('change:isFocused', 'isFocused', true);
				}
			},
			document: Object.assign(this.viewDocument, { getRoot: () => this.editable }),
			change: (callback: (writer: CKEditorViewWriter) => void) => callback({
				setAttribute: (key, value, element) => (element as HTMLElement).setAttribute(key, value),
				removeAttribute: (key, element) => (element as HTMLElement).removeAttribute(key)
			})
		}
	};
	readonly uiElement = document.createElement('div');
	readonly editable = document.createElement('div');
	readonly ui = {
		element: this.uiElement,
		view: { editable: { element: this.editable } },
		focusTracker: this.focusTracker
	};
	readonly keystrokes = {
		set: (keystroke: string, callback: (data: unknown, cancel: () => void) => void) => {
			this.keys.set(keystroke, callback);
		}
	};
	/** The clipboard's pipeline, which fires contentInsertion with what a paste inserts. */
	readonly pipeline = new Emitter();
	readonly plugins = {
		has: (name: string) => name === 'ClipboardPipeline',
		get: (name: string) => name === 'ClipboardPipeline' ? this.pipeline : undefined
	};

	constructor(readonly source: HTMLElement, readonly config: Record<string, unknown>) {
		this.root = rootOf('');
		this.uiElement.className = 'ck ck-editor';
		this.editable.className = 'ck-editor__editable';
		// the toolbar above the text, as ClassicEditor has it
		const top = document.createElement('div');
		top.className = 'ck-editor__top';
		const main = document.createElement('div');
		main.className = 'ck-editor__main';
		main.append(this.editable);
		this.uiElement.append(top, main);
		source.after(this.uiElement);
	}

	/**
	 * Pastes as the user does: the clipboard's input, then what CKEditor read of it inserted,
	 * which changes the text.
	 */
	paste(html: string, kept: string): void {
		const transfer = { getData: (type: string) => type === 'text/html' ? html : '' };
		const data = { dataTransfer: transfer, content: html as unknown };
		this.viewDocument.fire('clipboardInput', data);
		this.pipeline.fire('contentInsertion', { content: { data: kept } });
		this.type(kept.replace(/<[^>]*>/g, ''));
	}

	get isReadOnly(): boolean {
		return this.locks.size > 0;
	}

	getData(): string {
		return written(this.html);
	}

	setData(data: string): void {
		this.html = data;
		this.root = rootOf(data);
		this.selection.anchor = new Position([0, 0]);
		this.selection.focus = new Position([0, 0]);
		this.modelDocument.fire('change:data');
	}

	/** Changes the text as the user does. */
	type(text: string): void {
		this.html = this.html.includes('</p>') ? this.html.replace('</p>', text + '</p>') :
			'<p>' + text + '</p>';
		this.root = rootOf(this.html);
		this.modelDocument.fire('change:data');
	}

	/** Moves the focus out, as the user does. */
	blur(): void {
		this.focusTracker.isFocused = false;
		this.focusTracker.fire('change:isFocused', 'isFocused', false);
	}

	enableReadOnlyMode(lockId: string): void {
		this.locks.add(lockId);
	}

	disableReadOnlyMode(lockId: string): void {
		this.locks.delete(lockId);
	}

	async destroy(): Promise<void> {
		this.destroyed = true;
		this.uiElement.remove();
	}
}

/** A build of the tests, with ClassicEditor, the plugins by their names and the editors made. */
function fakeBuild(fail?: unknown) {
	const editors: FakeEditor[] = [];
	const plugin = (name: string) => ({ pluginName: name });
	const names = ['Essentials', 'Paragraph', 'Autoformat', 'PasteFromOffice', 'Heading', 'Bold',
		'Italic', 'Underline', 'Strikethrough', 'Code', 'Superscript', 'Subscript', 'Link',
		'AutoLink', 'List', 'TodoList', 'BlockQuote', 'CodeBlock', 'HorizontalLine', 'Table',
		'TableToolbar', 'Image', 'ImageToolbar', 'ImageCaption', 'ImageTextAlternative',
		'ImageInsertViaUrl', 'AutoImage', 'Alignment', 'Markdown', 'Highlight'];
	const build: CKEditorBuild & { editors: FakeEditor[] } = Object.assign(
		Object.fromEntries(names.map((name) => [name, plugin(name)])), {
			editors,
			ClassicEditor: {
				create: async (element: HTMLElement, config: Record<string, unknown>) => {
					if (fail) {
						throw fail;
					}
					const editor = new FakeEditor(element, config);
					editors.push(editor);
					return editor;
				}
			}
		});
	return build;
}

/** The names of the plugins of a configuration. */
function pluginNames(config: Record<string, unknown>): string[] {
	return (config.plugins as { pluginName: string }[]).map((plugin) => plugin.pluginName);
}

/**
 * @returns a text area holding some HTML, with a label, in a form
 */
function textArea(html = '<p>Hello</p>', attributes = ''): HTMLTextAreaElement {
	document.body.innerHTML = '<form><label for="notes">Notes</label><textarea id="notes" ' +
		'name="notes"' + attributes + '></textarea><input id="other"></form>';
	const field = document.getElementById('notes') as HTMLTextAreaElement;
	field.defaultValue = html;
	return field;
}

const tick = (ms = 20) => new Promise((resolve) => setTimeout(resolve, ms));

afterEach(() => {
	document.body.innerHTML = '';
	setHost();
	vi.useRealTimers();
});

describe('the places of the selection', () => {
	/** <p>ab</p><li>cd</li>, the item of a list nested as an element */
	function tree(): FakeElement {
		const root = new FakeElement('$root', null);
		const paragraph = new FakeElement('paragraph', root);
		paragraph.children.push(new FakeText('ab', paragraph));
		const list = new FakeElement('list', root);
		const item = new FakeElement('item', list);
		item.children.push(new FakeText('cd', item));
		list.children.push(item);
		root.children.push(paragraph, list);
		return root;
	}

	it('counts the places between the nodes, as ProseMirror does', () => {
		const root = tree();
		expect(offsetOfPath(root, [0])).toBe(0);
		expect(offsetOfPath(root, [0, 0])).toBe(1);
		expect(offsetOfPath(root, [0, 2])).toBe(3);
		expect(offsetOfPath(root, [1])).toBe(4);
		expect(offsetOfPath(root, [1, 0, 1])).toBe(7);
		expect(offsetOfPath(root, [2])).toBe(10);
	});

	it('finds the path of each place again', () => {
		const root = tree();
		for (const path of [[0], [0, 0], [0, 1], [0, 2], [1, 0], [1, 0, 0], [1, 0, 2], [2]]) {
			expect(pathOfOffset(root, offsetOfPath(root, path))).toEqual(path);
		}
		expect(pathOfOffset(root, 99)).toEqual([2]);
		expect(pathOfOffset(root, -3)).toEqual([0]);
	});
});

describe('CKEditor', () => {
	it('starts CKEditor of the build with the license key, the language and the features', async () => {
		const build = fakeBuild();
		const field = textArea('<p>Hello <b style="font-weight:700">there</b></p>',
			' class="antilia-ck-editor-loading"');
		const ready = vi.fn();
		const editor = new CKEditor(field, { build, licenseKey: 'GPL', language: 'de',
			translations: [{ de: {} }], placeholder: 'Write', onReady: ready });
		expect(editor.element.classList.contains(CK_EDITOR_CLASSES.editor)).toBe(true);
		expect(field.hidden).toBe(true);
		expect(field.classList.contains(CK_EDITOR_CLASSES.loading)).toBe(false);
		await editor.ready;
		const fake = build.editors[0]!;
		expect(ready).toHaveBeenCalledWith(fake);
		expect(editor.editor).toBe(fake);
		expect(fake.config.licenseKey).toBe('GPL');
		expect(fake.config.language).toBe('de');
		expect(fake.config.translations).toEqual([{ de: {} }]);
		expect(fake.config.placeholder).toBe('Write');
		expect(fake.config.label).toBe('Notes');
		expect(pluginNames(fake.config)).toEqual(expect.arrayContaining(['Essentials', 'Paragraph',
			'Bold', 'Table', 'TodoList', 'Alignment', 'Image']));
		expect(pluginNames(fake.config)).not.toContain('Markdown');
		expect((fake.config.toolbar as { items: string[] }).items).toEqual(['heading', '|', 'bold',
			'italic', 'underline', 'strikethrough', 'code', 'superscript', 'subscript', '|', 'link',
			'|', 'bulletedList', 'numberedList', 'todoList', '|', 'blockQuote', 'codeBlock',
			'horizontalLine', 'insertTable', 'insertImage', '|', 'alignment', '|', 'undo', 'redo']);
		expect((fake.config.alignment as { options: unknown[] }).options).toContainEqual(
			{ name: 'center', className: 'antilia-text-center' });
		// the style moved aside, which a Content Security Policy would report
		expect(fake.html).toBe('<p>Hello <b data-antilia-style="font-weight:700"><strong>there' +
			'</strong></b></p>');
		expect(editor.element.querySelector(':scope > .ck-editor')).toBe(fake.uiElement);
		expect(editor.element.getAttribute('data-modified')).toBe('false');
	});

	it('gives the text the spellcheck and the language of the options, its texts staying in theirs',
		async () => {
			const build = fakeBuild();
			const plain = new CKEditor(textArea(), { build, language: 'de' });
			await plain.ready;
			expect(build.editors[0]!.config.language).toBe('de');
			expect(build.editors[0]!.editable.hasAttribute('spellcheck')).toBe(false);
			plain.destroy();

			const editor = new CKEditor(textArea(), { build, spellcheck: false,
				textLanguage: ' en-GB ' });
			await editor.ready;
			const fake = build.editors[1]!;
			expect(fake.config.language).toEqual({ ui: 'en', content: 'en-GB' });
			expect(fake.editable.getAttribute('spellcheck')).toBe('false');

			// without starting CKEditor anew
			editor.configure({ spellcheck: true });
			expect(editor.editor).toBe(fake);
			expect(fake.editable.getAttribute('spellcheck')).toBe('true');
			editor.configure({ spellcheck: null });
			expect(fake.editable.hasAttribute('spellcheck')).toBe(false);

			// another language starts it anew; no language tag is none
			editor.configure({ textLanguage: 'es', language: 'de' });
			await editor.ready;
			expect(build.editors[2]!.config.language).toEqual({ ui: 'de', content: 'es' });
			editor.configure({ textLanguage: '"><script>' });
			await editor.ready;
			expect(build.editors[3]!.config.language).toBe('de');
		});

	it('leaves out what Markdown has no syntax for, and makes the toolbar and the headings given',
		async () => {
			const build = fakeBuild();
			const editor = new CKEditor(textArea('# Title'), { build, format: 'markdown',
				toolbar: ['bold', '|', 'italic'], headingLevels: [2, 1, 9 as 1],
				features: ['heading', 'bold', 'italic', 'underline'] });
			await editor.ready;
			const config = build.editors[0]!.config;
			expect(pluginNames(config)).toEqual(['Essentials', 'Paragraph', 'Autoformat',
				'PasteFromOffice', 'Heading', 'Bold', 'Italic', 'Underline', 'Markdown']);
			expect((config.toolbar as { items: string[] }).items).toEqual(['bold', '|', 'italic']);
			expect((config.heading as { options: { model: string }[] }).options.map((option) =>
				option.model)).toEqual(['paragraph', 'heading1', 'heading2']);
			expect(editor.getValue()).toBe('# Title');

			const markdown = new CKEditor(textArea('x'), { build, format: 'markdown' });
			await markdown.ready;
			expect([...markdown.features()]).not.toContain('underline');
			expect([...markdown.features()]).not.toContain('superscript');
			expect([...markdown.features()]).not.toContain('subscript');
			expect([...markdown.features()]).not.toContain('textAlign');
			expect(pluginNames(build.editors[1]!.config)).not.toContain('Alignment');
		});

	it('takes subscript away with superscript, and the other way round, in one change', async () => {
		const build = fakeBuild();
		const editor = new CKEditor(textArea(), { build });
		await editor.ready;
		const fake = build.editors[0]!;
		const changes = fake.changes;

		fake.subscript.execute();
		expect(fake.done).toEqual(['superscript off', 'subscript on']);
		fake.superscript.execute();
		expect(fake.done.slice(2)).toEqual(['subscript off', 'superscript on']);
		expect(fake.changes - changes).toBe(2);
		// taking one away leaves the other alone
		fake.superscript.execute();
		fake.subscript.execute({ forceValue: false });
		expect(fake.done.slice(4)).toEqual(['superscript off', 'subscript off']);
		expect(fake.changes - changes).toBe(2);
		fake.superscript.execute({ forceValue: true });
		expect(fake.done.slice(6)).toEqual(['subscript off', 'superscript on']);
	});

	it('takes the plugins and the configuration of the page, and tells of plugins not there',
		async () => {
			const errors: unknown[] = [];
			setHost({ ...standaloneHost, log: { error: (message) => errors.push(message) } } as Host);
			const build = fakeBuild();
			const own = { pluginName: 'Own' };
			const editor = new CKEditor(textArea(), { build, plugins: ['Highlight', own, 'Nothing'],
				features: ['bold'], toolbar: false, config: { highlight: { options: [] } } });
			await editor.ready;
			const config = build.editors[0]!.config;
			expect(pluginNames(config)).toEqual(['Essentials', 'Paragraph', 'Autoformat',
				'PasteFromOffice', 'Bold', 'Highlight', 'Own']);
			expect(config.heading).toBeUndefined();
			expect((config.toolbar as { items: string[] }).items).toEqual([]);
			expect(config.highlight).toEqual({ options: [] });
			expect(errors).toEqual(['CKEditor has no plugin Nothing']);
		});

	it('shows the text area again when CKEditor cannot start', async () => {
		const errors: unknown[] = [];
		setHost({ ...standaloneHost, log: { error: (message) => errors.push(message) } } as Host);
		const field = textArea();
		const onError = vi.fn();
		const editor = new CKEditor(field, { onError });
		await expect(editor.ready).rejects.toThrow(/no CKEditor in the page/);
		expect(onError).toHaveBeenCalled();
		expect(field.hidden).toBe(false);
		expect(field.parentElement?.tagName).toBe('FORM');
		expect(errors).toEqual(['CKEditor could not start']);

		const refused = new CKEditor(textArea(), { build: fakeBuild(new Error('license-key-invalid')),
			onError });
		await expect(refused.ready).rejects.toThrow('license-key-invalid');
		expect(onError).toHaveBeenLastCalledWith(new Error('license-key-invalid'));
		// gone, it takes no more options: the text area stays shown
		refused.configure({ readOnly: true });
		refused.setValue('<p>x</p>');
		refused.markSaved();
		expect(refused.field!.hidden).toBe(false);
		expect(refused.getValue()).toBe('<p>Hello</p>');
	});

	it('starts CKEditor anew for options changed while it starts', async () => {
		const build = fakeBuild();
		const editor = new CKEditor(textArea(), { build });
		const first = editor.ready;
		editor.configure({ features: ['bold'] });
		await editor.ready;
		await first;
		expect(build.editors).toHaveLength(2);
		expect(build.editors[0]!.destroyed).toBe(true);
		expect(editor.editor).toBe(build.editors[1]);
		expect(pluginNames(build.editors[1]!.config)).toContain('Bold');
		expect(pluginNames(build.editors[1]!.config)).not.toContain('Italic');
	});

	it('keeps the text in the text area and tells of the user\'s changes as a text area does',
		async () => {
			const build = fakeBuild();
			const field = textArea('<ul><li>one</li></ul>');
			const changes: string[] = [];
			const events: string[] = [];
			Object.entries(RICH_TEXT_EDITOR_EVENTS).forEach(([name, type]) =>
				document.addEventListener(type, (event) => events.push(name +
					((event as CustomEvent).detail.modified === undefined ? '' :
						'=' + (event as CustomEvent).detail.modified))));
			field.addEventListener('input', () => events.push('input'));
			field.addEventListener('change', () => events.push('change-of-field'));
			const editor = new CKEditor(field, { build, onChange: (value) => changes.push(value),
				stateDelay: 0 });
			await editor.ready;
			const fake = build.editors[0]!;
			// the ids of the items are CKEditor's, new each time: not the text's
			expect(editor.getValue()).toBe('<ul><li>one</li></ul>');
			expect(editor.isModified()).toBe(false);
			events.length = 0;

			editor.focus();
			fake.html = '<p>x</p>';
			fake.type(' more');
			expect(field.value).toBe('<p>x more</p>');
			expect(changes).toEqual(['<p>x more</p>']);
			expect(editor.isModified()).toBe(true);
			expect(editor.element.getAttribute('data-modified')).toBe('true');
			fake.blur();
			expect(events).toEqual(['focus', 'modified=true', 'text', 'input', 'change',
				'change-of-field', 'blur', 'state']);
			// the same text again: no change
			fake.modelDocument.fire('change:data');
			expect(changes).toHaveLength(1);
		});

	it('takes the text of the page without telling, and as saved', async () => {
		const build = fakeBuild();
		const field = textArea('<p>one</p>');
		const changes = vi.fn();
		const modified: boolean[] = [];
		const editor = new CKEditor(field, { build, onChange: changes,
			onModifiedChange: (value) => modified.push(value) });
		// before CKEditor started, the editor keeps it
		editor.setValue('<p>two</p>');
		expect(editor.getValue()).toBe('<p>two</p>');
		expect(field.value).toBe('<p>two</p>');
		await editor.ready;
		expect(build.editors[0]!.html).toBe('<p>two</p>');
		editor.setValue('<p>three</p>');
		expect(field.value).toBe('<p>three</p>');
		expect(changes).not.toHaveBeenCalled();
		expect(editor.isModified()).toBe(true);
		editor.markSaved();
		expect(editor.isModified()).toBe(false);
		expect(field.defaultValue).toBe('<p>three</p>');
		editor.setValue('<p>four</p>', true);
		expect(editor.isModified()).toBe(false);
		// the same text: nothing changes
		editor.setValue('<p>four</p>');
		expect(modified).toEqual([true, false]);
	});

	it('puts the saved text back when its tab or its window is closed anyway', async () => {
		const build = fakeBuild();
		const field = textArea('<p>saved</p>');
		const editor = new CKEditor(field, { build });
		await editor.ready;
		build.editors[0]!.type('!');
		expect(editor.isModified()).toBe(true);
		document.body.dispatchEvent(new CustomEvent('antilia-tabs:discard', { bubbles: true }));
		expect(editor.getValue()).toBe('<p>saved</p>');
		expect(editor.isModified()).toBe(false);
		// the page puts the text area back as it was
		build.editors[0]!.type('?');
		field.value = '<p>back</p>';
		field.dispatchEvent(new Event('input'));
		expect(editor.getValue()).toBe('<p>back</p>');
	});

	it('tells the page to save on Ctrl+S', async () => {
		const build = fakeBuild();
		const onSave = vi.fn();
		const field = textArea('<p>text</p>');
		const saved = vi.fn();
		field.addEventListener(RICH_TEXT_EDITOR_EVENTS.save, saved);
		const editor = new CKEditor(field, { build, onSave });
		await editor.ready;
		const cancel = vi.fn();
		build.editors[0]!.keys.get('Ctrl+S')!({}, cancel);
		expect(onSave).toHaveBeenCalledWith('<p>text</p>');
		expect(saved).toHaveBeenCalled();
		expect(cancel).toHaveBeenCalled();
	});

	it('leaves the width and the height of images out, and pastes quiet HTML', async () => {
		const build = fakeBuild();
		const editor = new CKEditor(textArea(), { build });
		await editor.ready;
		const fake = build.editors[0]!;
		const context = (name: string) => ({ endsWith: (query: string) => query === name });
		expect(fake.checks.some((check) => check(context('imageBlock'), 'width') === false)).toBe(true);
		expect(fake.checks.some((check) => check(context('imageInline'), 'height') === false))
			.toBe(true);
		expect(fake.checks.every((check) => check(context('paragraph'), 'alignment') === undefined))
			.toBe(true);
		// the natural size CKEditor gives an image once it loaded is consumed before its
		// converters write it
		const consumed: unknown[][] = [];
		const api = { consumable: { consume: (item: unknown, type: string) => {
			consumed.push([item, type]);
			return true;
		} } };
		for (const event of ['attribute:width:imageInline', 'attribute:height:imageBlock']) {
			(fake.downcast as unknown as { fire(event: string, ...args: unknown[]): void }).fire(event,
				{ item: 'an image' }, api);
		}
		expect(consumed).toEqual([['an image', 'attribute:width:imageInline'],
			['an image', 'attribute:height:imageBlock']]);
		const transfer = { files: ['a file'], getData: (type: string) =>
			type === 'text/html' ? '<p style="color:red">x</p>' : 'x' };
		const data: { dataTransfer: typeof transfer; content: unknown } = { dataTransfer: transfer,
			content: '<p style="color:red">x</p>' };
		fake.viewDocument.fire('clipboardInput', data);
		expect(data.content).toBe('<p data-antilia-style="color:red">x</p>');
		expect(data.dataTransfer.getData('text/html')).toBe('<p data-antilia-style="color:red">x</p>');
		expect(data.dataTransfer.getData('text/plain')).toBe('x');
		expect(data.dataTransfer.files).toEqual(['a file']);
	});

	it('keeps its state, reports it and starts in it', async () => {
		vi.useFakeTimers();
		const build = fakeBuild();
		const reports: unknown[] = [];
		const field = textArea('<p>Hello</p><p>World</p>');
		const editor = new CKEditor(field, { build, state: { selection: { anchor: 8, head: 10 },
			height: 300, scrollTop: 20 }, onStateChange: (state, change) => reports.push([state,
			change]) });
		await vi.waitFor(() => expect(editor.editor).not.toBeNull());
		const fake = build.editors[0]!;
		expect([fake.selection.anchor?.path, fake.selection.focus?.path]).toEqual([[1, 0], [1, 2]]);
		expect(editor.getState()).toEqual({ selection: { anchor: 8, head: 10 }, scrollTop: 20,
			scrollLeft: 0, width: null, height: 300 });
		expect(editor.element.style.getPropertyValue('--antilia-resizable-height')).toBe('300px');

		// the user selects backwards: reported a moment later
		fake.model.change((writer) => writer.setSelection(fake.model.createRange(new Position([0, 1]),
			new Position([0, 4])), { backward: true }));
		expect(reports).toEqual([]);
		vi.advanceTimersByTime(400);
		expect(reports).toEqual([[{ selection: { anchor: 5, head: 2 }, scrollTop: 20,
			scrollLeft: 0, width: null, height: 300 }, { leaving: false, changed: false }]]);
		// the same again: not reported
		window.dispatchEvent(new Event('pagehide'));
		expect(reports).toHaveLength(1);

		editor.setState({ selection: { anchor: 1, head: 1 }, width: 500, height: 250 });
		expect(fake.selection.anchor?.path).toEqual([0, 0]);
		// resized along its height alone
		expect(editor.getState()).toMatchObject({ width: null, height: 250 });
		// a place between blocks is one CKEditor selects near
		editor.setState({ selection: { anchor: 7, head: 7 } });
		expect(fake.selection.anchor?.path).toEqual([1, 0]);
	});

	it('reports the text it was not told of as the page goes', async () => {
		const build = fakeBuild();
		const reports: unknown[] = [];
		const editor = new CKEditor(textArea(), { build, onStateChange: (_state, change) =>
			reports.push(change) });
		await editor.ready;
		build.editors[0]!.type('!');
		window.dispatchEvent(new Event('pagehide'));
		expect(reports).toEqual([{ leaving: true, changed: true }]);
	});

	it('is read only as told, or as its text area is', async () => {
		const build = fakeBuild();
		const editor = new CKEditor(textArea('<p>x</p>', ' readonly'), { build });
		await editor.ready;
		const fake = build.editors[0]!;
		expect(fake.isReadOnly).toBe(true);
		expect(editor.element.classList.contains(CK_EDITOR_CLASSES.readOnly)).toBe(true);
		editor.field!.readOnly = false;
		editor.configure({});
		expect(fake.isReadOnly).toBe(false);
		editor.configure({ readOnly: true });
		expect(fake.isReadOnly).toBe(true);
		expect(build.editors).toHaveLength(1);
	});

	it('counts the characters and the words', async () => {
		const build = fakeBuild();
		const editor = new CKEditor(textArea('<p>Hello world</p><p>again</p>'), { build, count: true,
			texts: { words: '{0} Wörter' } });
		const status = () => editor.element.querySelector('.antilia-ck-editor-status')?.textContent;
		// counted once CKEditor started
		expect(status()).toBe('');
		await editor.ready;
		expect(status()).toBe('16 characters · 3 Wörter');
		build.editors[0]!.setData('<p>a</p>');
		expect(status()).toBe('1 character · 1 word');
		editor.configure({ count: false });
		expect(status()).toBeUndefined();
	});

	it('makes CKEditor anew for other features, with the text it had', async () => {
		const build = fakeBuild();
		const editor = new CKEditor(textArea('<p>one</p>'), { build });
		await editor.ready;
		build.editors[0]!.type(' two');
		editor.configure({ features: ['bold'] });
		expect(build.editors[0]!.destroyed).toBe(true);
		await editor.ready;
		expect(build.editors).toHaveLength(2);
		expect(build.editors[1]!.html).toBe('<p>one two</p>');
		expect(editor.isModified()).toBe(true);
		expect(editor.element.querySelectorAll('.ck-editor')).toHaveLength(1);
		// a format: the text is read anew, and saved as it is
		editor.configure({ format: 'markdown' });
		await editor.ready;
		expect(editor.isModified()).toBe(false);
	});

	it('goes on as it was when the page renders it anew in the same place', async () => {
		const build = fakeBuild();
		const removing: Array<(element: Element) => void> = [];
		setHost({ ...standaloneHost, onNodeRemoving: (listener) => {
			removing.push(listener);
			return () => undefined;
		} } as Host);
		const field = textArea('<p>Hello</p>');
		const editor = new CKEditor(field, { build });
		await editor.ready;
		build.editors[0]!.type('!');
		removing.forEach((listener) => listener(field));
		expect(build.editors[0]!.destroyed).toBe(true);
		expect(field.hidden).toBe(false);
		// Wicket's new text area, with the text the editor sent
		const again = document.createElement('textarea');
		again.id = 'notes';
		again.defaultValue = '<p>Hello!</p>';
		field.replaceWith(again);
		const next = new CKEditor(again, { build });
		await next.ready;
		expect(next.getValue()).toBe('<p>Hello!</p>');
		expect(next.isModified()).toBe(true);
	});

	it('stops CKEditor that starts after the editor went', async () => {
		const build = fakeBuild();
		const editor = new CKEditor(textArea(), { build });
		editor.destroy();
		await expect(editor.ready).rejects.toThrow(/destroyed/);
		expect(build.editors[0]!.destroyed).toBe(true);
		expect(CKEditor.of('notes')).toBeNull();
	});

	it('is made by attach, found by of, and is a Svelte action', async () => {
		const build = fakeBuild();
		textArea();
		const editor = CKEditor.attach('notes', { build })!;
		expect(CKEditor.of('notes')).toBe(editor);
		expect(CKEditor.attach('nothing')).toBeNull();
		await editor.ready;
		editor.destroy();

		const element = document.createElement('div');
		document.body.append(element);
		const action = ckEditor(element, { build, value: '<p>x</p>' });
		await tick();
		expect(element.classList.contains(CK_EDITOR_CLASSES.editor)).toBe(true);
		action.update({ readOnly: true });
		expect(build.editors[1]!.isReadOnly).toBe(true);
		action.destroy();
		expect(element.classList.contains(CK_EDITOR_CLASSES.editor)).toBe(false);
		expect(element.hasAttribute('data-modified')).toBe(false);
	});

	it('focuses CKEditor once it started, and the label focuses it too', async () => {
		const build = fakeBuild();
		const field = textArea();
		const editor = new CKEditor(field, { build });
		editor.focus();
		await editor.ready;
		await tick();
		expect(build.editors[0]!.focusTracker.isFocused).toBe(true);
		build.editors[0]!.blur();
		(document.querySelector('label') as HTMLLabelElement).click();
		expect(build.editors[0]!.focusTracker.isFocused).toBe(true);
	});
});

describe('the types of events of CKEditor', () => {
	/** The details of the events of a name the text area fired, without the editor. */
	function heard(field: HTMLElement, name: string): Array<Record<string, unknown>> {
		const details: Array<Record<string, unknown>> = [];
		field.addEventListener(name, (event) => {
			const { editor, ...rest } = (event as CustomEvent<Record<string, unknown>>).detail;
			expect(editor).toBe(CKEditor.of(field as HTMLTextAreaElement));
			details.push(rest);
		});
		return details;
	}

	it('fire from the events of the editor as the Tiptap editor\'s do, once CKEditor started',
		async () => {
			registerRichTextEvent<{ previous: string }>('test-ck:left', {
				from: ['change', RICH_TEXT_EDITOR_EVENTS.blur],
				init: (editor) => ({ previous: editor.getValue() }),
				fire(editor, state, cause) {
					const current = editor.getValue();
					if (current === state.previous) {
						return undefined;
					}
					const detail = { previous: state.previous, current, cause: cause.type,
						text: editor.getText() };
					state.previous = current;
					return detail;
				}
			});
			const build = fakeBuild();
			const field = textArea('<h2>Title</h2><p>Hello world</p>');
			const left = heard(field, 'test-ck:left');
			const editor = new CKEditor(field, { build });
			expect(editor.getText()).toBe('');
			await editor.ready;
			const fake = build.editors[0]!;
			expect(editor.getText()).toBe('Title\n\nHello world');

			editor.focus();
			fake.type('!');
			fake.blur();
			expect(left).toEqual([{ previous: '<h2>Title</h2><p>Hello world</p>',
				current: '<h2>Title</h2><p>Hello world!</p>', cause: 'change',
				text: 'Title\n\nHello world!' }]);
			editor.focus();
			fake.blur();
			expect(left).toHaveLength(1);
		});

	it('fire by trigger(), not once the editor is gone', async () => {
		registerRichTextEvent('test-ck:asked', {});
		const build = fakeBuild();
		const field = textArea();
		const asked = heard(field, 'test-ck:asked');
		const editor = new CKEditor(field, { build });
		await editor.ready;
		expect(editor.trigger('test-ck:asked', { a: 1 })).toBe(true);
		expect(asked).toEqual([{ a: 1 }]);
		editor.destroy();
		expect(editor.trigger('test-ck:asked')).toBe(false);
		expect(asked).toHaveLength(1);
		expect(RICH_TEXT_EVENT_TRIGGER).toBe('trigger');
	});

	it('include the text selected, once for each text', async () => {
		const build = fakeBuild();
		const field = textArea('<h2>Title</h2><p>Hello world</p>');
		const selected = heard(field, RICH_TEXT_BUILT_IN_EVENTS.selectedText);
		const editor = new CKEditor(field, { build });
		expect(editor.getSelectedText()).toBe('');
		await editor.ready;
		const fake = build.editors[0]!;
		const select = (anchor: number[], head: number[]) => fake.model.change((writer) =>
			writer.setSelection(fake.model.createRange(new Position(anchor), new Position(head))));

		select([1, 0], [1, 5]);
		expect(editor.getSelectedText()).toBe('Hello');
		select([0, 2], [1, 5]);
		expect(editor.getSelectedText()).toBe('tle\nHello');
		select([1, 1], [1, 1]);
		select([1, 2], [1, 2]);
		expect(selected).toEqual([{ text: 'Hello' }, { text: 'tle\nHello' }, { text: '' }]);
	});
});

describe('what a paste into CKEditor lost', () => {
	/** A script, a handler, a link to javascript: and a frame: what CKEditor does not read. */
	const UNSAFE = '<p onclick="steal()">Hi <a href="javascript:steal()">there</a></p>' +
		'<script>steal()</script><iframe src="https://example.org"></iframe>';

	it('is told to onPasteCleaned, with the event paste-cleaned, and in the notice', async () => {
		const build = fakeBuild();
		const field = textArea();
		const reports: unknown[] = [];
		const details: Array<Record<string, unknown>> = [];
		field.addEventListener(RICH_TEXT_BUILT_IN_EVENTS.pasteCleaned, (event) => {
			const { editor: firing, ...rest } = (event as CustomEvent<Record<string, unknown>>).detail;
			expect(firing).toBe(CKEditor.of(field));
			details.push(rest);
		});
		const editor = new CKEditor(field, { build, pasteNotice: true,
			onPasteCleaned: (report) => reports.push(report) });
		await editor.ready;
		const fake = build.editors[0]!;
		// under the toolbar, before the text, empty
		const notice = editor.element.querySelector<HTMLElement>('.antilia-rich-text-notice')!;
		expect(notice.getAttribute('role')).toBe('status');
		expect(notice.nextElementSibling?.classList.contains('ck-editor__main')).toBe(true);
		expect(notice.textContent).toBe('');

		fake.paste(UNSAFE, '<p>Hi there</p>');
		// told once the paste is in the text
		expect(reports).toEqual([]);
		await Promise.resolve();
		const report = { elements: { script: 1, iframe: 1, a: 1 }, attributes: { onclick: 1 } };
		expect(reports).toEqual([report]);
		expect(details).toEqual([report]);
		expect(notice.textContent).toBe('Part of the paste was left out: scripts, embedded ' +
			'content, links.');

		// gone with the next change
		fake.type('!');
		expect(notice.textContent).toBe('');
	});

	it('reads the HTML of a paste of the browser from the clipboard, and stays through changes ' +
		'of the model alone', async () => {
		const build = fakeBuild();
		const onPasteCleaned = vi.fn();
		const editor = new CKEditor(textArea(), { build, pasteNotice: true, onPasteCleaned });
		await editor.ready;
		const fake = build.editors[0]!;
		// a paste event of the browser: CKEditor reads the content from the clipboard after the
		// editor heard of the paste
		fake.viewDocument.fire('clipboardInput', { dataTransfer: { getData: (type: string) =>
			type === 'text/html' ? '<p onclick="steal()">Hi</p><script>steal()</script>' : '' } });
		fake.pipeline.fire('contentInsertion', { content: { data: '<p>Hi</p>' } });
		fake.type('Hi');
		await Promise.resolve();
		expect(onPasteCleaned).toHaveBeenCalledWith({ elements: { script: 1 },
			attributes: { onclick: 1 } });
		const notice = editor.element.querySelector<HTMLElement>('.antilia-rich-text-notice')!;
		expect(notice.textContent).toBe('Part of the paste was left out: scripts.');

		// the model changed, not the text: an image given its natural size once it loaded
		fake.modelDocument.fire('change:data');
		expect(notice.textContent).toBe('Part of the paste was left out: scripts.');
		fake.type('!');
		expect(notice.textContent).toBe('');
	});

	it('is nothing for what CKEditor keeps, for text, and without the clipboard\'s pipeline',
		async () => {
			const build = fakeBuild();
			const onPasteCleaned = vi.fn();
			const editor = new CKEditor(textArea(), { build, pasteNotice: true, onPasteCleaned });
			await editor.ready;
			const fake = build.editors[0]!;
			// what CKEditor writes of a paste of Word: italic as i, no spans, no paragraphs' classes
			fake.paste('<p class="MsoNormal"><b><span lang="EN">Bold</span></b> <em>it</em>' +
				'<o:p></o:p></p><ul><li>One</li></ul>', '<p><strong>Bold</strong> <i>it</i></p>' +
				'<ul><li data-list-item-id="e1">One</li></ul>');
			// text, which CKEditor makes HTML of
			fake.viewDocument.fire('clipboardInput', { dataTransfer: { getData: (type: string) =>
				type === 'text/plain' ? '<script>' : '' }, content: '<p>&lt;script&gt;</p>' });
			fake.pipeline.fire('contentInsertion', { content: { data: '<p>&lt;script&gt;</p>' } });
			await Promise.resolve();
			expect(onPasteCleaned).not.toHaveBeenCalled();
			expect(editor.element.querySelector('.antilia-rich-text-notice')!.textContent).toBe('');

			// a CKEditor without the clipboard's pipeline pastes as it does
			const plain = fakeBuild();
			const create = plain.ClassicEditor.create;
			(plain as { ClassicEditor: unknown }).ClassicEditor = {
				create: async (element: HTMLElement, config: Record<string, unknown>) => Object.assign(
					await create(element, config), { plugins: undefined }) };
			const other = new CKEditor(textArea(), { build: plain, onPasteCleaned });
			await other.ready;
			plain.editors[0]!.paste(UNSAFE, '<p>Hi there</p>');
			await Promise.resolve();
			expect(onPasteCleaned).not.toHaveBeenCalled();
		});

	it('shows the notice as the options say, in the texts given, with each CKEditor', async () => {
		const build = fakeBuild();
		const editor = new CKEditor(textArea(), { build, texts: { pasteCleaned:
			'Ein Teil fehlt: {0}.', pasteScripts: 'Skripte' } });
		await editor.ready;
		expect(editor.element.querySelector('.antilia-rich-text-notice')).toBeNull();
		editor.configure({ pasteNotice: true });
		build.editors[0]!.paste('<p onmouseover="x()">x</p>', '<p>x</p>');
		await Promise.resolve();
		expect(editor.element.querySelector('.antilia-rich-text-notice')!.textContent)
			.toBe('Ein Teil fehlt: Skripte.');

		// CKEditor made anew, the text the same: the notice under its toolbar, as it was
		editor.configure({ features: ['bold'] });
		await vi.waitFor(() => expect(build.editors[1]?.uiElement.isConnected).toBe(true));
		await editor.ready;
		const notice = editor.element.querySelector<HTMLElement>('.antilia-rich-text-notice')!;
		expect(notice.nextElementSibling).toBe(build.editors[1]!.uiElement
			.querySelector('.ck-editor__main'));
		expect(notice.textContent).toBe('Ein Teil fehlt: Skripte.');
		build.editors[1]!.type('!');
		expect(notice.textContent).toBe('');
		editor.configure({ pasteNotice: false });
		expect(editor.element.querySelector('.antilia-rich-text-notice')).toBeNull();
	});
});
