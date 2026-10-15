/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { forwardRef, useImperativeHandle, useLayoutEffect, useRef } from 'react';

import { CodeEditor as CoreCodeEditor, type CodeEditorLanguage, type CodeEditorSelection,
	type CodeEditorState, type CodeEditorStateChange, type CodeEditorTexts, type Extension,
	type ResizableSize, type ResizeAxis } from '@antilia/core';

import { useLook } from './Look.js';

export interface CodeEditorProps {
	/**
	 * The text: what the editor starts with, and what it shows whenever this changes; the user's
	 * changes come with onChange.
	 */
	value?: string;
	/** The text last saved, against which the editor tells of changes; the first text by default. */
	saved?: string;
	/** The grammar: the name of a language the editor knows or one registered, or an extension. */
	language?: CodeEditorLanguage | null;
	/** Whether the lines show their numbers, true when left out. */
	lineNumbers?: boolean;
	/** Whether blocks fold, true when left out. */
	folding?: boolean;
	/** Whether the user cannot change the text, false when left out. */
	readOnly?: boolean;
	/** Whether the user can put the cursor in the text and select it, true when left out. */
	selectable?: boolean;
	/** Whether long lines wrap, false when left out. */
	lineWrapping?: boolean;
	/** The width of a tab and of a level of indentation, in spaces: 4 when left out. */
	tabSize?: number;
	/** Whether Tab indents, true when left out. */
	indentWithTab?: boolean;
	/** The text shown while the editor is empty. */
	placeholder?: string;
	/** The accessible name of the text. */
	label?: string;
	/** The dimensions the user resizes the editor in, vertical when left out; false for none. */
	resizable?: ResizeAxis | false;
	minWidth?: number;
	minHeight?: number;
	maxWidth?: number | null;
	maxHeight?: number | null;
	/** The state the editor starts in: the selection, the scroll position and the size. */
	state?: CodeEditorState;
	/** How long after the user changed the state onStateChange reports it, 400 ms when left out. */
	stateDelay?: number;
	/** More CodeMirror extensions. */
	extensions?: Extension[];
	/** What the editor says, in English when left out. */
	texts?: Partial<CodeEditorTexts>;
	/** The name a form posts the text under, in a hidden text area; none when left out. */
	name?: string;
	id?: string;
	/** CSS classes of the editor, next to antilia-code-editor. */
	className?: string;
	/** Called with the text each time the user changed it. */
	onChange?(value: string): void;
	onFocus?(): void;
	onBlur?(value: string): void;
	onSelectionChange?(selection: CodeEditorSelection): void;
	/** Called with the state a moment after the user changed it, for the page to keep it. */
	onStateChange?(state: CodeEditorState, change: CodeEditorStateChange): void;
	/** Called when the text came to have changes not saved, or no longer has. */
	onModifiedChange?(modified: boolean): void;
	/** Called with the text when the user pressed Ctrl+S, or Cmd+S. */
	onSave?(value: string): void;
	onResize?(size: ResizableSize): void;
}

/**
 * What a ref to a {@link CodeEditor} can do with it.
 */
export interface CodeEditorHandle {
	/** The editor of the core, null until it is made. */
	readonly editor: CoreCodeEditor | null;
	/** @returns the text */
	getValue(): string;
	/** Puts the focus in the text. */
	focus(): void;
	/** @returns the selection, the scroll position and the size */
	getState(): CodeEditorState | null;
	/** Puts the editor in a state. */
	setState(state: CodeEditorState): void;
	/** Takes the text, or another, as the text last saved. */
	markSaved(value?: string): void;
	/** @returns whether the text has changes not saved */
	isModified(): boolean;
}

/**
 * The code editor of the core, CodeMirror, in the theme and the look of the page: the grammar of
 * a language highlights the text, Ctrl+F searches, Ctrl+S calls onSave; it is resizable by the
 * bar along its bottom unless told otherwise. React renders its element, the core the editor in
 * it. It tells whether the text has changes not saved, with data-modified, which the tabs it is in
 * show. Give it a state to start in, one onStateChange reported, to show it again as the user left
 * it. A ref gets a {@link CodeEditorHandle}.
 */
export const CodeEditor = forwardRef<CodeEditorHandle, CodeEditorProps>(function CodeEditor(
	props, ref) {
	const { value, saved, language, lineNumbers, folding, readOnly, selectable, lineWrapping,
		tabSize, indentWithTab, placeholder, label, resizable, minWidth, minHeight, maxWidth,
		maxHeight, stateDelay, extensions, texts, name, id, className } = props;
	const element = useRef<HTMLDivElement>(null);
	const instance = useRef<CoreCodeEditor | null>(null);
	const look = useLook();
	const latest = useRef(props);
	latest.current = props;
	const rendered = useRef(className);

	useImperativeHandle(ref, () => ({
		get editor() {
			return instance.current;
		},
		getValue: () => instance.current?.getValue() ?? latest.current.value ?? '',
		focus: () => instance.current?.focus(),
		getState: () => instance.current?.getState() ?? null,
		setState: (state) => instance.current?.setState(state),
		markSaved: (text) => instance.current?.markSaved(text),
		isModified: () => instance.current?.isModified() ?? false
	}), []);

	useLayoutEffect(() => {
		const current = latest.current;
		const editor = new CoreCodeEditor(element.current!, {
			value: current.value ?? '', saved: current.saved, language: current.language,
			lineNumbers: current.lineNumbers, folding: current.folding, readOnly: current.readOnly,
			selectable: current.selectable, lineWrapping: current.lineWrapping,
			tabSize: current.tabSize, indentWithTab: current.indentWithTab,
			placeholder: current.placeholder, label: current.label, resizable: current.resizable,
			minWidth: current.minWidth, minHeight: current.minHeight, maxWidth: current.maxWidth,
			maxHeight: current.maxHeight, state: current.state, stateDelay: current.stateDelay,
			extensions: current.extensions, texts: current.texts, look,
			onChange: (next) => latest.current.onChange?.(next),
			onFocus: () => latest.current.onFocus?.(),
			onBlur: (next) => latest.current.onBlur?.(next),
			onSelectionChange: (next) => latest.current.onSelectionChange?.(next),
			onStateChange: (next, change) => latest.current.onStateChange?.(next, change),
			onModifiedChange: (next) => latest.current.onModifiedChange?.(next),
			onSave: (next) => latest.current.onSave?.(next),
			onResize: (next) => latest.current.onResize?.(next)
		});
		instance.current = editor;
		return () => {
			editor.destroy();
			instance.current = null;
		};
	}, [look, name]);

	useLayoutEffect(() => {
		instance.current?.configure({ language: language ?? null, lineNumbers, folding, readOnly,
			selectable, lineWrapping, tabSize, indentWithTab, placeholder, label, resizable,
			minWidth, minHeight, maxWidth, maxHeight, stateDelay, texts });
	}, [language, lineNumbers, folding, readOnly, selectable, lineWrapping, tabSize, indentWithTab,
		placeholder, label, resizable, minWidth, minHeight, maxWidth, maxHeight, stateDelay, texts]);

	useLayoutEffect(() => {
		if (extensions !== undefined) {
			instance.current?.configure({ extensions });
		}
	}, [extensions]);

	useLayoutEffect(() => {
		const editor = instance.current;
		if (editor && value !== undefined && value !== editor.getValue()) {
			editor.setValue(value);
		}
	}, [value]);

	useLayoutEffect(() => {
		if (saved !== undefined) {
			instance.current?.markSaved(saved);
		}
	}, [saved]);

	// React wrote other classes over the element's: those the core keeps come back
	useLayoutEffect(() => {
		if (rendered.current !== className) {
			rendered.current = className;
			instance.current?.configure({});
		}
	}, [className]);

	return (
		<div ref={element} id={id} className={className}>
			{name !== undefined && <textarea name={name} defaultValue={value ?? ''} />}
		</div>
	);
});
