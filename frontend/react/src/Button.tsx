/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import type { ButtonHTMLAttributes, ReactNode } from 'react';

import { buttonClasses, partClasses, type ButtonSize, type ButtonVariant } from '@antilia/core';

import { useLook } from './Look.js';

export type { ButtonSize, ButtonVariant };

export interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
	/** The text of the button; with only the icon shown, its tooltip and accessible name. */
	label: string;
	/** The icon before the text, an inline SVG mostly. */
	icon?: ReactNode;
	/** Whether only the icon is shown, the button then square. */
	iconOnly?: boolean;
	/** What the button stands for: secondary by default. */
	variant?: ButtonVariant;
	/** How big the button is: medium by default. */
	size?: ButtonSize;
}

/**
 * A button in the colors of the theme, in the markup of Wicket's AjaxActionButton and shown by the
 * core's style sheet button.css: an icon, a text or both, in a variant and a size. It is a
 * button of type button unless told otherwise, so it never submits a form by accident. In the
 * look of a LookProvider, such as Bootstrap's, it has the classes of that look.
 */
export function Button({ label, icon, iconOnly = false, variant, size, className, type = 'button',
	title, 'aria-label': ariaLabel, ...rest }: ButtonProps) {
	const look = useLook();
	return (
		<button {...rest} type={type}
			className={buttonClasses({ variant, size, iconOnly, className, look })}
			title={title ?? (iconOnly ? label : undefined)}
			aria-label={ariaLabel ?? (iconOnly ? label : undefined)}>
			{icon != null && <span className={partClasses(look, 'button-icon')} aria-hidden="true">{icon}</span>}
			{!iconOnly && <span className={partClasses(look, 'button-label')}>{label}</span>}
		</button>
	);
}
