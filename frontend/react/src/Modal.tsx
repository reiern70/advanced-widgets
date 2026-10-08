/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useId, useLayoutEffect, useRef, type ReactNode } from 'react';

import { MODAL_CLASSES, MODAL_THEME_CLASS, ModalDialog } from '@advanced-widgets/core';

export interface ModalProps {
	/** Whether the dialog is open; nothing is rendered while it is closed. */
	open: boolean;
	/** Called when the user asks to close the dialog: Escape, or a click on the overlay. */
	onClose(): void;
	/** The title, in the header of the dialog; none when left out. */
	title?: ReactNode;
	/** The footer of the dialog, its buttons for example; none when left out. */
	footer?: ReactNode;
	/** Whether Escape closes the dialog; true by default. */
	closeOnEscape?: boolean;
	/** Whether a click on the overlay around the dialog closes it; false by default. */
	closeOnClick?: boolean;
	/** Whether the dialog keeps the focus while open; true by default. */
	trapFocus?: boolean;
	/** CSS classes next to dialog-theme-default, of another theme for example. */
	className?: string;
	/** The body of the dialog. */
	children?: ReactNode;
}

/**
 * A modal dialog that behaves and looks like Wicket's ModalDialog with its default theme: the
 * same markup, so the same style sheets show it (the core's styles/modal.css, or Wicket's and
 * the theme's in a Wicket page), Escape and a click on the overlay closing it, and the focus kept
 * in it while it is open and given back when it closes. A dialog rendered in another one, or
 * after it, opens over it.
 */
export function Modal({ open, onClose, title, footer, closeOnEscape = true,
	closeOnClick = false, trapFocus = true, className, children }: ModalProps) {
	const overlay = useRef<HTMLDivElement>(null);
	const titleId = useId();
	const latest = useRef(onClose);
	latest.current = onClose;

	useLayoutEffect(() => {
		if (!open || !overlay.current) {
			return;
		}
		const modal = new ModalDialog(overlay.current, { closeOnEscape, closeOnClick, trapFocus,
			onClose: () => latest.current() });
		return () => modal.destroy();
	}, [open, closeOnEscape, closeOnClick, trapFocus]);

	if (!open) {
		return null;
	}
	return (
		<div className={[MODAL_THEME_CLASS, className].filter(Boolean).join(' ')}>
			<div ref={overlay} className={MODAL_CLASSES.overlay}>
				<div className={MODAL_CLASSES.dialog} role="dialog" aria-modal="true"
					aria-labelledby={title ? titleId : undefined}>
					<div className={MODAL_CLASSES.content}>
						{title ? <h1 className={MODAL_CLASSES.header} id={titleId}>{title}</h1> : null}
						<div className={MODAL_CLASSES.body}>{children}</div>
						{footer ? <div className={MODAL_CLASSES.footer}>{footer}</div> : null}
					</div>
				</div>
			</div>
		</div>
	);
}
