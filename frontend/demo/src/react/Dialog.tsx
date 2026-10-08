/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useEffect, useId, useRef, type ReactNode } from 'react';

/**
 * A modal dialog in the markup of Wicket's ModalDialog with its default theme, so that the page's
 * style sheet shows it the same. Escape closes it; it takes the focus while open and gives it back
 * when closed.
 */
export function Dialog({ title, onClose, children }: { title: string; onClose(): void;
	children: ReactNode }) {
	const dialog = useRef<HTMLDivElement>(null);
	const titleId = useId();
	const latest = useRef(onClose);
	latest.current = onClose;

	useEffect(() => {
		const opener = document.activeElement as HTMLElement | null;
		const element = dialog.current!;
		(element.querySelector<HTMLElement>('input, select, button') ?? element).focus();
		const keydown = (event: KeyboardEvent) => {
			if (event.key === 'Escape') {
				event.stopPropagation();
				latest.current();
			}
		};
		element.addEventListener('keydown', keydown);
		return () => {
			element.removeEventListener('keydown', keydown);
			opener?.focus?.();
		};
	}, []);

	return (
		<div className="dialog-theme-default">
			<div className="modal-dialog-overlay">
				<div ref={dialog} className="modal-dialog" role="dialog" aria-modal="true"
					aria-labelledby={titleId} tabIndex={-1}>
					<div className="modal-dialog-content">
						<h1 className="modal-dialog-header" id={titleId}>{title}</h1>
						<div className="modal-dialog-body">{children}</div>
					</div>
				</div>
			</div>
		</div>
	);
}
