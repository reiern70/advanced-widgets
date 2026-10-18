/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
 * The options example of the modal dialog demo: a form setting what closes a dialog, dragging,
 * folding and its size, as the Wicket one does, and dialogs nested in it, three deep.
 */

import type { ModalSize } from '@antilia/core';

/** The options of the dialogs, as the form sets them. */
export interface DialogOptions {
	closeOnEscape: boolean;
	closeButton: boolean;
	closeOnClick: boolean;
	draggable: boolean;
	foldable: boolean;
	/** A size, or fitting the content. */
	size: 'fit' | ModalSize;
	/** A width of its own, a CSS length; none when empty. */
	width: string;
}

/** The options a dialog has at first, those of the Wicket form. */
export const DEFAULT_DIALOG_OPTIONS: DialogOptions = { closeOnEscape: true, closeButton: true,
	closeOnClick: false, draggable: true, foldable: true, size: 'medium', width: '' };

/** The check boxes of the form, in their order. */
export const DIALOG_FLAGS = ['closeOnEscape', 'closeButton', 'closeOnClick', 'draggable',
	'foldable'] as const;

/** The sizes the form offers, in their order. */
export const DIALOG_SIZES: DialogOptions['size'][] = ['fit', 'small', 'medium', 'large'];

/** The widths the form takes, as the Wicket form validates them. */
const WIDTH = /^\d+(\.\d+)?(em|rem|px|%|vw|ch)$/;

/**
 * @returns whether a width is fit for the dialog: none, or a number and a CSS unit
 */
export function isValidWidth(width: string): boolean {
	return width === '' || WIDTH.test(width);
}

/**
 * @returns the props of the Modal of React and Svelte for the options; a width not valid is left
 *          out
 */
export function modalOptions(options: DialogOptions): Omit<DialogOptions, 'size' | 'width'> &
	{ size?: ModalSize; width?: string } {
	const { size, width, ...flags } = options;
	return { ...flags, size: size === 'fit' ? undefined : size,
		width: width !== '' && isValidWidth(width) ? width : undefined };
}

/** The icons of the buttons of the demo, as inline SVG the page wrote. */
export interface ModalDemoIcons {
	add: string;
	edit: string;
	delete: string;
	cancel: string;
	save: string;
	open: string;
	nested: string;
	ok: string;
}

/** The texts of the options example, by the keys of the page's bundles. */
export interface DialogOptionsTexts {
	options: string;
	'options.behavior': string;
	'option.closeOnEscape': string;
	'option.closeButton': string;
	'option.closeOnClick': string;
	'option.draggable': string;
	'option.foldable': string;
	size: string;
	'size.fit': string;
	'size.small': string;
	'size.medium': string;
	'size.large': string;
	width: string;
	open: string;
	/** The title of a dialog, {0} standing for how deep it is. */
	'dialog.title': string;
	'dialog.text': string;
	'dialog.deepest': string;
	'dialog.nested': string;
	'dialog.ok': string;
	close: string;
}
