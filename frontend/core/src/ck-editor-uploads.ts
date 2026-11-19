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
 * What CKEditor has of the uploads of the Tiptap editor besides its own upload of images
 * (ck-editor.ts), as CKEditor makes it of the classes of the build:
 *
 * - the files attached, an inline object of CKEditor's model, attachment, written as the node of
 *   the Tiptap editor writes them (rich-text-attachments.ts), a link of its own with the class of
 *   its kind and its size; read from that HTML, and, in Markdown, from a link to one of the files
 *   the page tells (attachmentUrls), a text of its own;
 * - the images and the files of the text, as the model has them, which the editor tells the page
 *   of as the user takes them out;
 * - the tools of the toolbar, as CKEditor names its own: a button running something, the dialog
 *   of the uploads for example, or a drop-down of CKEditor whose panel holds a list or a form of
 *   the components', the images of the text each selected by a click and taken out of the text by
 *   its button, or the form finding the images the page stores.
 *
 * Nothing here holds code of CKEditor, which the page brings: the classes come from the build.
 */

import { buttonClasses } from './button.js';
import type { Look } from './look.js';
import { ATTACHMENT_CLASS, attachmentKindOfLink, isAttachmentKind, isAttachmentSize,
	type AttachmentAttributes, type RichTextAttachmentType } from './rich-text-attachments.js';
import { isSafeUrl } from './rich-text-html.js';

/* What the uploads use of CKEditor, as its classes have it: no more, so that any build fits. */

/** A node of CKEditor's model or of its view, as the uploads walk them. */
interface CKNode {
	is(type: string, name?: string): boolean;
	readonly name?: string;
	readonly data?: string;
	readonly parent?: CKNode | null;
	readonly childCount?: number;
	getChildren?(): Iterable<CKNode>;
	getChild?(index: number): CKNode | null;
	getAttribute?(key: string): unknown;
	hasClass?(name: string): boolean;
	getClassNames?(): Iterable<string>;
}

/** What CKEditor's writers of the model and of the view do for the uploads. */
interface CKWriter {
	createElement(name: string, attributes?: Record<string, unknown>): unknown;
	createContainerElement(name: string, attributes?: Record<string, string>,
		options?: Record<string, unknown>): unknown;
	createText(data: string): unknown;
	createPositionAt(parent: unknown, offset: number | 'end'): unknown;
	insert(position: unknown, item: unknown): unknown;
	remove(item: unknown): void;
	setSelection(item: unknown, place?: string): void;
}

/** An observable of CKEditor: its properties set, its events heard. */
interface CKObservable {
	set(values: Record<string, unknown>): void;
	on(event: string, callback: (...args: any[]) => void): void;
}

/** A view of CKEditor's UI. */
interface CKView extends CKObservable {
	readonly element: HTMLElement | null;
	setTemplate?(template: Record<string, unknown>): void;
	focus?(): void;
	isEnabled?: boolean;
}

/** A drop-down of CKEditor's UI: its button, and its panel. */
interface CKDropdown extends CKView {
	readonly buttonView: CKView;
	readonly panelView: { readonly children: { add(view: unknown): void } };
	isOpen: boolean;
}

/** The editor of CKEditor, as the uploads use it. */
export interface CKUploadsEditor {
	readonly locale?: unknown;
	readonly isReadOnly?: boolean;
	readonly model: {
		readonly document: { getRoot(): unknown };
		readonly schema: { register(name: string, definition: Record<string, unknown>): void;
			isRegistered?(name: string): boolean };
		change(callback: (writer: CKWriter) => void): unknown;
		insertContent?(content: unknown, selectable?: unknown): unknown;
	};
	readonly conversion: { for(group: string): {
		elementToElement(definition: Record<string, unknown>): unknown } };
	readonly editing: {
		readonly view: { focus(): void; scrollToTheSelection?(): void };
		readonly mapper?: { on(event: string, callback: (...args: any[]) => void,
			options?: Record<string, unknown>): void };
	};
	readonly ui: { readonly componentFactory?: { add(name: string,
		callback: (locale: unknown) => unknown): void; has?(name: string): boolean } };
	on?(event: string, callback: (...args: any[]) => void): void;
}

/** The classes of the build the uploads use. */
export interface CKUploadsBuild {
	readonly ButtonView?: new (locale: unknown) => CKView;
	readonly View?: new (locale: unknown) => CKView;
	readonly createDropdown?: (locale: unknown) => CKDropdown;
	readonly toWidget?: (element: unknown, writer: unknown, options?: Record<string, unknown>) =>
		unknown;
	readonly viewToModelPositionOutsideModelElement?: (model: unknown,
		test: (element: CKNode) => boolean) => (...args: any[]) => void;
	readonly [name: string]: unknown;
}

/** The name of the element of CKEditor's model of a file attached. */
export const CK_ATTACHMENT = 'attachment';

/** The elements of CKEditor's model of the images. */
const IMAGES = ['imageBlock', 'imageInline'];

/** The marks a link to a file is no text of its own in, as the Tiptap editor takes it. */
const MARKS = ['strong', 'b', 'em', 'i', 'u', 's', 'del', 'code', 'sup', 'sub', 'mark', 'span'];

/**
 * @returns the children of a node, none for text
 */
function childrenOf(node: CKNode): Iterable<CKNode> {
	return node.is('$text') || !node.getChildren ? [] : node.getChildren();
}

/**
 * Walks the elements of the model below a root, depth first, in their order.
 */
function walk(root: unknown, visit: (element: CKNode) => void): void {
	if (!root) {
		return;
	}
	for (const child of childrenOf(root as CKNode)) {
		if (child.is('$text')) {
			continue;
		}
		visit(child);
		walk(child, visit);
	}
}

/**
 * @returns the addresses of the images of CKEditor's model, safe ones, each with how many times
 *          it is in the text; an image uploading, without an address yet, is none
 */
export function ckImagesIn(root: unknown): Map<string, number> {
	const found = new Map<string, number>();
	walk(root, (element) => {
		const src = IMAGES.includes(element.name ?? '') ? element.getAttribute?.('src') : null;
		if (typeof src === 'string' && src && isSafeUrl(src)) {
			found.set(src, (found.get(src) ?? 0) + 1);
		}
	});
	return found;
}

/**
 * @returns the images of CKEditor's model, each once by its address, in their order, with the
 *          text in place of it
 */
export function ckImagesOf(root: unknown): { src: string; alt: string | null }[] {
	const found = new Map<string, string | null>();
	walk(root, (element) => {
		const src = IMAGES.includes(element.name ?? '') ? element.getAttribute?.('src') : null;
		if (typeof src === 'string' && src && isSafeUrl(src) && !found.has(src)) {
			const alt = element.getAttribute?.('alt');
			found.set(src, typeof alt === 'string' ? alt : null);
		}
	});
	return Array.from(found, ([src, alt]) => ({ src, alt }));
}

/**
 * @returns the addresses of the files attached to CKEditor's model, each with how many times it
 *          is in the text
 */
export function ckAttachmentsIn(root: unknown): Map<string, number> {
	const found = new Map<string, number>();
	walk(root, (element) => {
		const href = element.name === CK_ATTACHMENT ? element.getAttribute?.('href') : null;
		if (typeof href === 'string' && href) {
			found.set(href, (found.get(href) ?? 0) + 1);
		}
	});
	return found;
}

/**
 * @returns the files attached to CKEditor's model, each once by its address, in their order
 */
export function ckAttachmentsOf(root: unknown): AttachmentAttributes[] {
	const found = new Map<string, AttachmentAttributes>();
	walk(root, (element) => {
		const href = element.name === CK_ATTACHMENT ? element.getAttribute?.('href') : null;
		if (typeof href === 'string' && href && !found.has(href)) {
			found.set(href, attributesOf(element));
		}
	});
	return Array.from(found.values());
}

/**
 * @returns what an element of the model of a file attached keeps
 */
function attributesOf(element: CKNode): AttachmentAttributes {
	const text = (key: string) => {
		const value = element.getAttribute?.(key);
		return typeof value === 'string' && value ? value : null;
	};
	return { href: text('href') ?? '', name: text('name') ?? text('href') ?? '',
		size: text('size'), kind: text('kind') };
}

/**
 * @returns the elements of the model of the images of an address, or of the files attached of an
 *          address, in their order
 */
function elementsOf(root: unknown, names: readonly string[], key: string,
	value: string): CKNode[] {
	const found: CKNode[] = [];
	walk(root, (element) => {
		if (names.includes(element.name ?? '') && element.getAttribute?.(key) === value) {
			found.push(element);
		}
	});
	return found;
}

/**
 * Takes the images of an address out of the text, every place they are in, in a step of undo.
 *
 * @returns whether the text held any
 */
export function ckRemoveImages(editor: CKUploadsEditor, src: string): boolean {
	return remove(editor, IMAGES, 'src', src);
}

/**
 * Takes the files attached of an address out of the text, every place they are in.
 *
 * @returns whether the text held any
 */
export function ckRemoveAttachments(editor: CKUploadsEditor, href: string): boolean {
	return remove(editor, [CK_ATTACHMENT], 'href', href);
}

function remove(editor: CKUploadsEditor, names: readonly string[], key: string,
	value: string): boolean {
	const found = elementsOf(editor.model.document.getRoot(), names, key, value);
	if (!found.length) {
		return false;
	}
	editor.model.change((writer) => found.reverse().forEach((element) => writer.remove(element)));
	return true;
}

/**
 * Selects the first image of an address, or the first file attached, and shows it.
 */
export function ckSelect(editor: CKUploadsEditor, kind: 'image' | 'attachment',
	value: string): void {
	const [element] = kind === 'image' ?
		elementsOf(editor.model.document.getRoot(), IMAGES, 'src', value) :
		elementsOf(editor.model.document.getRoot(), [CK_ATTACHMENT], 'href', value);
	if (!element) {
		return;
	}
	editor.model.change((writer) => writer.setSelection(element, 'on'));
	editor.editing.view.focus();
	editor.editing.view.scrollToTheSelection?.();
}

/**
 * Inserts files attached where the selection is, one after the other, the selection after them.
 */
export function ckInsertAttachments(editor: CKUploadsEditor,
	attachments: readonly AttachmentAttributes[]): void {
	const insert = editor.model.insertContent;
	if (!attachments.length || typeof insert !== 'function') {
		return;
	}
	editor.model.change((writer) => attachments.forEach((attachment) => {
		const attributes: Record<string, unknown> = { href: attachment.href,
			name: attachment.name || attachment.href };
		if (isAttachmentSize(attachment.size)) {
			attributes.size = attachment.size;
		}
		if (isAttachmentKind(attachment.kind)) {
			attributes.kind = attachment.kind;
		}
		const element = writer.createElement(CK_ATTACHMENT, attributes);
		insert.call(editor.model, element);
		writer.setSelection(element, 'after');
	}));
}

/**
 * @returns the attributes of the link of a file attached, in the order the cleaning of the server
 *          writes them, as the Tiptap editor writes them
 */
function linkAttributes(attachment: AttachmentAttributes): Record<string, string> {
	const kind = isAttachmentKind(attachment.kind) ? attachment.kind : null;
	const attributes: Record<string, string> = {
		class: kind ? ATTACHMENT_CLASS + ' ' + ATTACHMENT_CLASS + '-' + kind : ATTACHMENT_CLASS,
		href: attachment.href,
		target: '_blank'
	};
	if (isAttachmentSize(attachment.size)) {
		attributes['data-size'] = attachment.size;
	}
	attributes.rel = 'noopener noreferrer nofollow';
	return attributes;
}

/**
 * @returns the kind of a file attached as the classes of its link say it, null for none
 */
function kindOfClasses(element: CKNode): string | null {
	for (const name of Array.from(element.getClassNames?.() ?? [])) {
		if (name.startsWith(ATTACHMENT_CLASS + '-')) {
			const kind = name.slice(ATTACHMENT_CLASS.length + 1);
			return isAttachmentKind(kind) ? kind : null;
		}
	}
	return null;
}

/**
 * @returns the text of a link of the view, when it is a text of its own: a single text, no element
 */
function textOf(element: CKNode): string | null {
	if (element.childCount !== 1) {
		return null;
	}
	const child = element.getChild?.(0);
	return child && child.is('$text') ? child.data ?? '' : null;
}

/**
 * Makes CKEditor's model hold the files attached, as an inline object, attachment, of an address,
 * a name, a size and a kind: read from the links of the class antilia-attachment, at a safe
 * address, and from the links to the addresses of the files the page tells, texts of their own
 * without other marks, as Markdown writes a file; written as the Tiptap editor writes them, an
 * inline widget while the user edits. Nothing without the classes of the build it needs.
 *
 * @param isAttachment tells whether a link is to a file attached, by its address; null for none
 * @param types the types of the files the user attaches, which tell a kind by an extension
 */
export function ckAttachmentConversion(editor: CKUploadsEditor, build: CKUploadsBuild,
	isAttachment: () => ((href: string) => boolean) | null,
	types: () => readonly RichTextAttachmentType[]): void {
	const toWidget = build.toWidget;
	if (typeof toWidget !== 'function' || editor.model.schema.isRegistered?.(CK_ATTACHMENT)) {
		return;
	}
	editor.model.schema.register(CK_ATTACHMENT, { inheritAllFrom: '$inlineObject',
		allowAttributes: ['href', 'name', 'size', 'kind'] });
	editor.conversion.for('upcast').elementToElement({
		view: (element: CKNode) => {
			if (!element.is('element', 'a') || element.name !== 'a') {
				return null;
			}
			const href = element.getAttribute?.('href');
			if (typeof href !== 'string' || !href || !isSafeUrl(href)) {
				return null;
			}
			if (element.hasClass?.(ATTACHMENT_CLASS)) {
				return { name: true, attributes: ['href'], classes: [ATTACHMENT_CLASS] };
			}
			const test = isAttachment();
			const parent = element.parent;
			return test && test(href) && textOf(element) !== null &&
				!(parent && MARKS.includes(parent.name ?? '')) ?
				{ name: true, attributes: ['href'] } : null;
		},
		model: (element: CKNode, { writer }: { writer: CKWriter }) => {
			const href = String(element.getAttribute?.('href') ?? '');
			const name = (textOf(element) ?? Array.from(childrenOf(element)).map((child) =>
				child.data ?? '').join('')).trim() || href;
			const size = element.getAttribute?.('data-size');
			const kind = element.hasClass?.(ATTACHMENT_CLASS) ? kindOfClasses(element) :
				attachmentKindOfLink(name, href, types());
			const attributes: Record<string, unknown> = { href, name };
			if (typeof size === 'string' && isAttachmentSize(size)) {
				attributes.size = size;
			}
			if (kind) {
				attributes.kind = kind;
			}
			return writer.createElement(CK_ATTACHMENT, attributes);
		},
		converterPriority: 'highest'
	});
	const link = (element: CKNode, writer: CKWriter) => {
		const attachment = attributesOf(element);
		const view = writer.createContainerElement('a', linkAttributes(attachment),
			{ isAllowedInsideAttributeElement: true });
		writer.insert(writer.createPositionAt(view, 0), writer.createText(attachment.name ||
			attachment.href));
		return view;
	};
	editor.conversion.for('dataDowncast').elementToElement({ model: CK_ATTACHMENT,
		view: (element: CKNode, { writer }: { writer: CKWriter }) => link(element, writer) });
	editor.conversion.for('editingDowncast').elementToElement({ model: CK_ATTACHMENT,
		view: (element: CKNode, { writer }: { writer: CKWriter }) =>
			toWidget(link(element, writer), writer) });
	const outside = build.viewToModelPositionOutsideModelElement;
	if (typeof outside === 'function') {
		editor.editing.mapper?.on('viewToModelPosition', outside(editor.model, (element) =>
			!!element.hasClass?.(ATTACHMENT_CLASS)));
	}
}

/** A tool of the toolbar running something. */
export interface CKButtonTool {
	name: string;
	label: string;
	icon: string;
	run(): void;
}

/** A tool of the toolbar opening a drop-down, whose panel it fills as it opens. */
export interface CKPanelTool {
	name: string;
	label: string;
	icon: string;
	/**
	 * Fills the panel as it opens.
	 *
	 * @param close closes the drop-down, the focus back in the text
	 * @returns what stops the panel once it closed, if anything
	 */
	render(panel: HTMLElement, close: () => void): (() => void) | void;
}

/** The class of the panels of the tools of the uploads, holding a list or a form. */
export const CK_PANEL_CLASS = 'antilia-ck-editor-panel';

/** The keys a panel keeps from CKEditor's toolbar, which would move the focus to its tools. */
const PANEL_KEYS = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Home', 'End', 'PageUp',
	'PageDown'];

/**
 * Registers the tools of the uploads in CKEditor's toolbar, by their names: buttons, and
 * drop-downs whose panels hold the lists and the forms. Disabled while the text is read only.
 * Nothing without the classes of the build they need.
 *
 * @param tools the tools, those of names CKEditor has already left out
 */
export function ckRegisterTools(editor: CKUploadsEditor, build: CKUploadsBuild,
	tools: readonly (CKButtonTool | CKPanelTool)[]): void {
	const factory = editor.ui.componentFactory;
	const ButtonView = build.ButtonView;
	const createDropdown = build.createDropdown;
	const View = build.View;
	if (!factory || typeof ButtonView !== 'function') {
		return;
	}
	const enable = (view: CKView) => {
		view.set({ isEnabled: !editor.isReadOnly });
		editor.on?.('change:isReadOnly', (_event: unknown, _name: string, readOnly: boolean) =>
			view.set({ isEnabled: !readOnly }));
	};
	tools.forEach((tool) => {
		if (factory.has?.(tool.name)) {
			return;
		}
		if ('run' in tool) {
			factory.add(tool.name, (locale) => {
				const button = new ButtonView(locale);
				button.set({ label: tool.label, icon: tool.icon, tooltip: true, withText: false });
				button.on('execute', () => tool.run());
				enable(button);
				return button;
			});
			return;
		}
		if (typeof createDropdown !== 'function' || typeof View !== 'function') {
			return;
		}
		factory.add(tool.name, (locale) => {
			const dropdown = createDropdown(locale);
			dropdown.buttonView.set({ label: tool.label, icon: tool.icon, tooltip: true,
				withText: false });
			const content = new View(locale);
			// out of CKEditor's reset of the styles of its UI: the components' own look
			content.setTemplate?.({ tag: 'div', attributes: { class: [CK_PANEL_CLASS,
				CK_PANEL_CLASS + '-' + tool.name, 'ck-reset_all-excluded'] } });
			// the focus goes to the panel's first control as the drop-down opens by the keyboard
			content.focus = () => content.element?.querySelector<HTMLElement>(
				'input, button:not([disabled])')?.focus();
			dropdown.panelView.children.add(content);
			let stop: (() => void) | void;
			let listening = false;
			dropdown.on('change:isOpen', (_event: unknown, _name: string, open: boolean) => {
				const panel = content.element;
				if (!panel) {
					return;
				}
				if (!listening) {
					listening = true;
					panel.addEventListener('keydown', (event) => {
						if (PANEL_KEYS.includes(event.key)) {
							event.stopPropagation();
						}
					});
				}
				stop?.();
				stop = undefined;
				panel.replaceChildren();
				if (open) {
					stop = tool.render(panel, () => {
						dropdown.isOpen = false;
						editor.editing.view.focus();
					});
				}
			});
			enable(dropdown);
			return dropdown;
		});
	});
}

/** An item of a list of a panel: what it shows, and what its buttons do. */
export interface CKPanelItem {
	label: string;
	/** The address of its thumbnail, for an image. */
	thumbnail?: string;
	/** The size, and the kind, of a file attached. */
	size?: string | null;
	kind?: string | null;
	/** What the button deleting it says. */
	deleteLabel: string;
	choose(): void;
	remove(): void;
}

/** The icon of the button deleting an item of a list: a bin. */
const TRASH = '<svg viewBox="0 0 24 24" width="1em" height="1em" fill="none" ' +
	'stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" ' +
	'aria-hidden="true" focusable="false"><path d="M4 7h16M10 11v6M14 11v6M5.5 7l1 12a2 2 0 0 0 ' +
	'2 2h7a2 2 0 0 0 2-2l1-12M9 7V4h6v3"/></svg>';

/**
 * Fills a panel with a list: a line for each item, its name, with its thumbnail or its size,
 * choosing it, and a button deleting it; or a line saying the list is empty. The arrow keys go
 * through the buttons.
 *
 * @param kind images or attachments, the class of the list
 */
export function ckFillList(panel: HTMLElement, kind: 'images' | 'attachments', look: Look,
	items: readonly CKPanelItem[], none: string): void {
	if (!items.length) {
		const empty = document.createElement('p');
		empty.className = CK_PANEL_CLASS + '-none';
		empty.textContent = none;
		panel.append(empty);
		return;
	}
	const list = document.createElement('ul');
	list.className = CK_PANEL_CLASS + '-list antilia-ck-editor-' + kind;
	items.forEach((item) => {
		const line = document.createElement('li');
		line.className = CK_PANEL_CLASS + '-item';
		const choose = document.createElement('button');
		choose.type = 'button';
		choose.className = CK_PANEL_CLASS + '-choose';
		if (item.thumbnail) {
			const thumbnail = document.createElement('img');
			thumbnail.className = 'antilia-rich-text-images-thumbnail';
			thumbnail.alt = '';
			thumbnail.loading = 'lazy';
			thumbnail.referrerPolicy = 'no-referrer';
			thumbnail.src = item.thumbnail;
			choose.append(thumbnail);
		} else if (item.kind !== undefined) {
			const icon = document.createElement('span');
			icon.className = 'antilia-rich-text-attachments-kind' + (isAttachmentKind(item.kind) ?
				' ' + ATTACHMENT_CLASS + '-' + item.kind : '');
			icon.setAttribute('aria-hidden', 'true');
			choose.append(icon);
		}
		const name = document.createElement('span');
		name.className = CK_PANEL_CLASS + '-name';
		name.textContent = item.label;
		choose.append(name);
		if (item.size) {
			const size = document.createElement('span');
			size.className = CK_PANEL_CLASS + '-size';
			size.textContent = item.size;
			choose.append(size);
		}
		choose.addEventListener('click', () => item.choose());
		const remove = document.createElement('button');
		remove.type = 'button';
		remove.className = buttonClasses({ variant: 'ghost', size: 'small', iconOnly: true, look }) +
			' ' + CK_PANEL_CLASS + '-delete';
		remove.setAttribute('aria-label', item.deleteLabel);
		remove.title = item.deleteLabel;
		remove.insertAdjacentHTML('beforeend', TRASH);
		remove.addEventListener('click', () => item.remove());
		line.append(choose, remove);
		list.append(line);
	});
	list.addEventListener('keydown', (event) => {
		const buttons = Array.from(list.querySelectorAll<HTMLButtonElement>('button'));
		const at = buttons.indexOf(document.activeElement as HTMLButtonElement);
		const next = event.key === 'ArrowDown' ? at + 1 : event.key === 'ArrowUp' ? at - 1 :
			event.key === 'Home' ? 0 : event.key === 'End' ? buttons.length - 1 : null;
		if (next === null || !buttons.length) {
			return;
		}
		event.preventDefault();
		buttons[(next + buttons.length) % buttons.length]!.focus();
	});
	panel.append(list);
}
