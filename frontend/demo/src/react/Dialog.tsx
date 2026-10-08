/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import type { ReactNode } from 'react';

import { Modal } from '@advanced-widgets/react';

/**
 * The dialog of the demos: an open Modal, which behaves and looks like Wicket's ModalDialog with
 * its default theme, as the Wicket pages open theirs: Escape closes it, and it keeps the focus
 * while open.
 */
export function Dialog({ title, onClose, children }: { title: string; onClose(): void;
	children: ReactNode }) {
	return <Modal open title={title} onClose={onClose}>{children}</Modal>;
}
