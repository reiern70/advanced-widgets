/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The buttons of the theme: their look is the style sheet button.css, the same for Wicket's
 * ButtonBehavior and AjaxActionButton and for the React and Svelte Button, or the classes of
 * another look, such as Bootstrap's (see look.ts). A button is an element with these classes;
 * this module only names them.
 */

import { ANTILIA_LOOK, type Look, lookClasses } from './look.js';

/** What a button stands for, which sets its colors. */
export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

/** How big a button is. */
export type ButtonSize = 'small' | 'medium' | 'large';

/** The variants, in the order a demo shows them. */
export const BUTTON_VARIANTS: readonly ButtonVariant[] = ['primary', 'secondary', 'ghost', 'danger'];

/** The sizes, from the smallest. */
export const BUTTON_SIZES: readonly ButtonSize[] = ['small', 'medium', 'large'];

/**
 * @returns the CSS classes of a button in a look, the Antilia look by default: there ant-button,
 * its variant's and its size's, and ant-button--icon-only for a button showing its icon only;
 * secondary and medium by default
 */
export function buttonClasses(options: { variant?: ButtonVariant; size?: ButtonSize;
	iconOnly?: boolean; className?: string; look?: Look } = {}): string {
	const { variant = 'secondary', size = 'medium', iconOnly = false, className,
		look = ANTILIA_LOOK } = options;
	return [lookClasses(look, 'button', `button-${variant}`, `button-${size}`,
		iconOnly && 'button-icon-only'), className ?? ''].filter(Boolean).join(' ');
}
